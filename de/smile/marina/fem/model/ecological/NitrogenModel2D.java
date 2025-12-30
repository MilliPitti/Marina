/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2026

 * This file is part of Marina.

 * Marina is free software: you can redistribute it and/or modify              
 * it under the terms of the GNU Affero General Public License as               
 * published by the Free Software Foundation version 3.
 * 
 * Marina is distributed in the hope that it will be useful,                  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                
 * GNU Affero General Public License for more details.                          
 *                                                                              
 * You should have received a copy of the GNU Affero General Public License     
 * along with Marina.  If not, see <http://www.gnu.org/licenses/>.             
 *                                                                               
 * contact: milbradt@smileconsult.de                                        
 * smile consult GmbH                                                           
 * Schiffgraben 11                                                                 
 * 30159 Hannover, Germany 
 * 
 */
package de.smile.marina.fem.model.ecological;

import java.io.*;
import javax.vecmath.*;
import java.util.*;

import bijava.math.ifunction.*;
import de.smile.marina.fem.DOF;


import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.model.hydrodynamic.BoundaryConditionOld;
import de.smile.marina.fem.model.hydrodynamic.dim2.Current2DElementData;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.marina.fem.TicadModel;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2D;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;

/** this ODE describe nitratModel based on CurrentModel2D
 * @version 2.7.4
 * @author Peter Milbradt
 */
public class  NitrogenModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel  {
    
    private DataOutputStream xf_os = null;
    private FileOutputStream xf_fs = null;
    
    private Vector initsc = new Vector();
    
    private int n,SKonc,numberofdofs;
    private Vector bskonc  = new Vector();
    
    private NitrogenDat nitratdat;
    
    private double[] x;     // zum Speichern der Zustandsgroessen
    double[] result;
    
    // Erdbeschleunigung
    static final double RhoWater = 998.2;       // Water density
    static final double AST      = 0.0012;      // 0.0012 Austauschkoeffizient fuer Stroemung
    
    static final double BATTJESKOEFF=0.3; 	// 0.1 - 0.3 Austauschkoeffizient infolge Wellenbrechens
    
    double WATT=0.1;
    
    //------------------------------------------------------------------------
    // konstruktor
    //------------------------------------------------------------------------
    public NitrogenModel2D(FEDecomposition fe, NitrogenDat _nitratdata) {
        fenet = fe;
        femodel=this;
        nitratdat = _nitratdata;
        
        WATT=nitratdat.watt;
        
        setNumberOfThreads(nitratdat.NumberOfThreads);
        
        readBoundCond(nitratdat.rndwerte_name);
        
        BoundaryCondition bcond;
        Enumeration be = bskonc.elements();
        while (be.hasMoreElements()) {
            bcond = (BoundaryCondition) be.nextElement();
            //and???????
            initsc.addElement(fenet.getDOF(bcond.pointnumber));
        }
        
        // DOFs initialisieren
        initialDOFs();
        
        numberofdofs = fenet.getNumberofDOFs();
        SKonc = 0;
        n = numberofdofs;
        result = new double[n];
        
        try {
            xf_fs = new FileOutputStream(nitratdat.xferg_name);
            xf_os = new DataOutputStream(xf_fs);
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ nitratdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }//NitratModel2D
    
    
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske Konzentration
        return TicadIO.HRES_H;
    }
     
    /**
     * @deprecated
     */
    
    //------------------------------------------------------------------------
    // getResultSize
    //------------------------------------------------------------------------
     @Deprecated
    public int getResultSize() {
        return n;
    }//getResultSize
    
    private double initialNitratConcentration(DOF dof, double time) {
        //    System.out.println("initialSalt");
        double sc=0., R=0., d;
        
        NitrogenModel2DData nitratdata = NitrogenModel2DData.extract(dof);
        if (nitratdata.bsc != null)
            sc = nitratdata.bsc.getValue(time);
        else {
            for (Enumeration e = initsc.elements(); e.hasMoreElements();){
                DOF ndof= (DOF) e.nextElement();
                NitrogenModel2DData nitrat = NitrogenModel2DData.extract(ndof);
                if ((dof!=ndof) & ( nitrat.bsc != null )){
                    d = dof.distance(ndof);
                    sc += nitrat.bsc.getValue(time) / d;
                    R += 1./d;
                }
            }
            if( R != 0. )
                sc /= R;
            else
                sc = 0.;
        }
        return sc;
    }
    
    /** initialisiert die Nitratkonzentration mit einem konstanten Wert
     * @param initalvalue
     * @return 
     */
     public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value "+initalvalue +" mg/l");
        
        for (DOF dof: fenet.getDOFs()){
           NitrogenModel2DData nitratmodeldata = NitrogenModel2DData.extract(dof);
           nitratmodeldata.skonc = initalvalue;
        }
        return null;
    }
    
    //------------------------------------------------------------------------
    // initalSolution
    //------------------------------------------------------------------------
    public double[] initialSolution(double time){
        this.time = time;
        
        double x[] = new double[getResultSize()];
        
        System.out.println("NitratModel - Werte Initialisieren");
        DOF[] dof = fenet.getDOFs();
//        for (int j=0; j<dof.length;j++){
//            int i = dof[j].number;
//            CurrentModel2DData currentmodeldata = CurrentModel2DData.extract(dof[j]);
//            NitratModel2DData nitratmodeldata = NitratModel2DData.extract(dof[j]);
//            
//            if (currentmodeldata!=null) {
//                x[SKonc + i] = 0.;
//            }
//            nitratmodeldata.skonc=x[SKonc + i];
//        }
        for (int j=0; j<dof.length;j++){
            int i = dof[j].number;
            
            NitrogenModel2DData nitratmodeldata = NitrogenModel2DData.extract(dof[j]);
            
            nitratmodeldata.skonc = x[SKonc + i] = initialNitratConcentration(dof[j],time);
        }
        initsc=null;
        write_erg_xf(x, time);
        return x;
    }//end initialSolution
    
     /** Read the start solution from file
     * @param currentergPath file with simulation results
     * @param record record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    public double[] initialSolutionFromTicadErgFile(String currentergPath, int record) throws Exception {
    	
        System.out.println("\t Read inital values from result file "+currentergPath);
	//erstes Durchscannen
	File sysergFile=new File(currentergPath);
        try (FileInputStream stream = new FileInputStream(sysergFile); DataInputStream inStream = new DataInputStream(stream)) {
            
            // Kommentar lesen, bis ASCII-Zeichen 7 kommt
            int c;
            do{
                c = inStream.readByte();
            }while ( c != 7 );
            // Ende Kommentar
            
            //Anzahl Elemente, Knoten und Rand lesen
            int anzKnoten=inStream.readInt();
            if (fenet.getNumberofDOFs()!=anzKnoten) {
                System.out.println("Die Datei mit den Startwerten hat andere Anzahl von Knoten");
                System.exit(1);
            }
            int anzr=inStream.readInt();
            int anzElemente=inStream.readInt();
            
            //überlesen folgende Zeilen
            inStream.skip(9*4);
            
            //Ergebnismaske lesen und auswerten
            int ergMaske = inStream.readInt();
            int anzWerte = TicadIO.ergMaskeAuswerten(ergMaske);
            
            final boolean None_gesetzt = ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE);
            final boolean Pos_gesetzt = ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS);
            final boolean Z_gesetzt = ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z);
            final boolean V_gesetzt = ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V);        
            final boolean Q_gesetzt = ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q);
            final boolean H_gesetzt = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);             
            final boolean SALT_gesetzt = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);
            final boolean EDDY_gesetzt = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);    
            final boolean SHEAR_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR); 
            final boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL);
            final boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
            final boolean AH_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);
            
            inStream.readInt();
            
            //Elemente, Rand und Knoten überlesen
            inStream.skip((anzElemente*4+anzr+3*anzKnoten)*4); //4 Bytes je float und int
            
            // bis zum record-Satz springen
            inStream.skip((4+anzKnoten*anzWerte*4)*record);
            
            x = new double[getResultSize()];
            
            float time=inStream.readFloat();
            for (int i = 0;i<fenet.getNumberofDOFs();i++){
                DOF dof=fenet.getDOF(i);
                NitrogenModel2DData nmd = NitrogenModel2DData.extract(dof);
                
                if (None_gesetzt) {
                    inStream.skip(4);
                }

                if (Pos_gesetzt) {
                    inStream.skip(4);
                }

                if (Z_gesetzt) {
                   inStream.skip(4);
                }

                if (V_gesetzt) {
                    inStream.skip(8);
                }

                if (Q_gesetzt) {
                    inStream.skip(8);
                }

                if (H_gesetzt) {
                    nmd.skonc = x[SKonc + i] = inStream.readFloat();
                }

                if (SALT_gesetzt) {
                    inStream.skip(4);
                }

                if (EDDY_gesetzt) {
                    inStream.skip(4);
                }

                if (SHEAR_gesetzt) {
                    inStream.skip(8);
                }

                if (V_SCAL_gesetzt) {
                    inStream.skip(4);
                }

                if (Q_SCAL_gesetzt) {
                    inStream.skip(4);
                }

                if (AH_gesetzt) {
                    inStream.skip(4);
                }
            }
        }
        return x;
    }
    
    //------------------------------------------------------------------------
    // getRateofChange
    //------------------------------------------------------------------------
    @Override
    public double[] getRateofChange(double time, double x[]){
        this.x = x;
        this.time = time;
        
        for(int i=0;i<result.length;i++) result[i]=0.;

        setBoundaryConditions();
        
        maxTimeStep = 10000.;
        
        // Elementloop
        performElementLoop();
        
        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            NitrogenModel2DData nitmodeldata = NitrogenModel2DData.extract(dof);
            int gamma = dof.getNumberofFElements();
            result[SKonc+i] *= 3. / gamma;
            nitmodeldata.dskoncdt = result[SKonc+i];
        }
        
        return result;        
    } // end getRateofChange
    
    //------------------------------------------------------------------------
    // ElementApproximation
    //------------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element){

        double timeStep=Double.POSITIVE_INFINITY;
        
        Current2DElementData eleCurrentData = Current2DElementData.extract(element);
        if(eleCurrentData != null){
            if(eleCurrentData.iwatt < 3){
                
                final FTriangle ele = (FTriangle) element;
                final double[][] koeffmat = ele.getkoeffmat();
                final double[] squelle    = new double[3];
                
                final double[] terms_Nit    = new double[3];
                
                double u_mean       = eleCurrentData.u_mean;
                double v_mean       = eleCurrentData.v_mean;
                
                // compute element derivations
                //-------------------------------------------------------------------
                double dNitConcdx = 0.;
                double dNitConcdy = 0.;
                for (int j = 0; j<3; j++) {
                    DOF dof = ele.getDOF(j);
                    NitrogenModel2DData nitmodeldata = NitrogenModel2DData.extract(dof);
                    
                    dNitConcdx += nitmodeldata.skonc * koeffmat[j][1];
                    dNitConcdy += nitmodeldata.skonc * koeffmat[j][2];
                } // end for
                
                double current_mean = Function.norm(u_mean, v_mean);
                double elementsize = eleCurrentData.elementsize;  // Peter 05.08.08
                
                //eddy viscosity
                double astx = eleCurrentData.astx;
                double asty = eleCurrentData.asty;
                
                // Nitrogen Quellen und Senken f�rs Element bestimmen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    squelle[j] = getSourceSunk(dof);
                            //getSourceSunk(nitratmodeldata.skonc,C_Algen,depth[j]);
                }
                
                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    NitrogenModel2DData nitmodeldata = NitrogenModel2DData.extract(dof);
                    CurrentModel2DData  currentmodeldata  = CurrentModel2DData.extract(dof);
                    
                    terms_Nit[j] = currentmodeldata.u * dNitConcdx + currentmodeldata.v * dNitConcdy
                                        - (astx * eleCurrentData.ddepthdx * dNitConcdx + asty * eleCurrentData.ddepthdy * dNitConcdy)/ ((currentmodeldata.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT : currentmodeldata.totaldepth)
                                        + 3. * (koeffmat[j][1] * astx * dNitConcdx
                                                    + koeffmat[j][2] * asty * dNitConcdy)
                                                    - squelle[j];
                    
                    Koeq1_mean += 1. / 3. * ( nitmodeldata.dskoncdt + terms_Nit[j] );
                }

                double tau_konc=0.;
                if (current_mean > 1.E-5) {
                    tau_konc = 0.5 * elementsize / current_mean;

                    timeStep = tau_konc*1./(1.+Math.abs(Koeq1_mean));

                    double tau = Function.norm(astx,asty);

                    double a_opt = 1.;
                    if(tau>0.00001) {
                        double peclet = current_mean * elementsize / tau;
                        a_opt = Function.coth(peclet) - 1.0 / peclet;
                    }
                    tau_konc *= a_opt;
                }
                
                // Fehlerkorrektur durchfuehren
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    final int i = dof.number;
                    CurrentModel2DData  cmd  = CurrentModel2DData.extract(dof);
                    
                    synchronized (result) {
                        result[SKonc+i] -= tau_konc * (koeffmat[j][1] * u_mean * Koeq1_mean + koeffmat[j][2] * v_mean * Koeq1_mean);
                    }
                    
                    // Begin standart Galerkin-step
                    for (int l = 0; l < 3; l++) {
                        final double vorfak = (l == j) ? 1./6. : 1./12.;
                        final double gl = (l == j) ? 1. :  Math.min(cmd.wlambda, CurrentModel2DData.extract(ele.getDOF(l)).totaldepth/Math.max(CurrentModel2D.WATT,cmd.totaldepth)); // Peter 21.01.2016
                        synchronized (result) {
                            result[SKonc+i] -= vorfak * terms_Nit[l]*gl;
                        }
                    }
                }
            }
        } 
        return timeStep;
    } // end ElementApproximation
    
    //------------------------------------------------------------------------
    // setBoundaryCondition
    //------------------------------------------------------------------------
    @Override
    public void setBoundaryCondition(DOF dof, double t){
        int i = dof.number;
        NitrogenModel2DData nitmodeldata = NitrogenModel2DData.extract(dof);
        
        /* prevention of negative concentration */
        if (x[SKonc + i] <= 0.)   x[SKonc + i] = 0.;
        
        if (nitmodeldata.bsc != null){
            x[SKonc + i] = nitmodeldata.bsc.getValue(t);
            nitmodeldata.dskoncdt = nitmodeldata.bsc.getDifferential(t);
        }

        if (nitmodeldata.extrapolate){
            NitrogenModel2DData tmpdata;
            FElement[] felem = dof.getFElements();
            for(int j=0; j<felem.length;j++) {
                FElement elem=felem[j];
                for(int ll=0;ll<3;ll++){
                    if(elem.getDOF(ll)==dof){
                        for(int ii=1;ii<3;ii++){
                            tmpdata = NitrogenModel2DData.extract(elem.getDOF((ll+ii)%3));
                            int jtmp = elem.getDOF((ll+ii)%3).number;
                            if (!tmpdata.extrapolate){
                                x[SKonc + i] = (9.*x[SKonc + i] + 1. * x[SKonc + jtmp])/10.;
                            }
                        }
                    }
                    break;
                }
            }
        }
        
        nitmodeldata.skonc = x[SKonc + i];
    }//end setBoundaryCondition
    
    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    public ModelData genData(DOF dof){
        //     System.out.println("DOF "+dof);
        NitrogenModel2DData data = new NitrogenModel2DData();
        
        int dofnumber = (int) dof.number;
        Enumeration b = bskonc.elements();
        while (b.hasMoreElements()) {
            BoundaryCondition bcond = (BoundaryCondition) b.nextElement();
            if ( dofnumber == bcond.pointnumber ){
                data.bsc = bcond.function;
                bskonc.removeElement(bcond);
            }
        }
        
        return data;
    }//end genData
    
    
    //------------------------------------------------------------------------
    // write_erg_xf
    //------------------------------------------------------------------------
    @Override
    public void write_erg_xf(double[] erg, double t) {
        try {
            xf_os.writeFloat((float) t);
            for (DOF dof : fenet.getDOFs()) {
                int i = dof.number;
//                CurrentModel2DData  currentmodeldata = CurrentModel2DData.extract(dof[j]);
//                double erg1=0.0;
//                double erg2=0.0;
                if (erg[SKonc + i] < 0.) {
                    erg[SKonc + i] = 0.;    // Peter 12.03.10
                }
                double M = 14.0067 + 3. * 15.9994; //Molare Masse Nitrat
                double erg3 = erg[SKonc + i];//*M/1000*10; //Umrechnung von micromol/l in mg/l und 10-fach �berh�ht
//                xf_os.writeFloat((float)erg1);
//                xf_os.writeFloat((float)erg2);
                xf_os.writeFloat((float) erg3);
            }
            xf_os.flush();
        } catch (Exception e) {
        }
    }//end write_erg_xf

    //------------------------------------------------------------------------
    // getSourceSunk
    //------------------------------------------------------------------------
    private double getSourceSunk(DOF dof) {
//    private double getSourceSunk(double actualC, double c_Algen, double c_Zoo, double d){
        NitrogenModel2DData nitratmodeldata = NitrogenModel2DData.extract(dof);
        CurrentModel2DData currentmodeldata = CurrentModel2DData.extract(dof);
        PhytoplanktonModel2DData phytoData = PhytoplanktonModel2DData.extract(dof);
        ZooplanktonModel2DData zooData = ZooplanktonModel2DData.extract(dof);
        DetritusModel2DData detritData = DetritusModel2DData.extract(dof);
        
        double d = currentmodeldata.eta + dof.z;
//        OekoModel2DData  oekomodeldata  = OekoModel2DData.extract(dof);
                
        double c_Zoo;
        double c_Detrit;
        double zooResMorRate; //Sterberate Zooplankton
        double detritMineralRate;
        double phytoBackConc;
        double zooBackConc;
//        double m = 14.0067 + 3. * 15.9994; //Molar mass nitrate
        
        double quellterm = 0.;
        if (d > WATT) {
            // Abbau durch chemische Reaktionen
            double chem_Abbau = 0.;
//            if( !(actualC <= 0.)) {
//                double lambda;
//                double T_halb = 365.;// Wert aus Literatur; zwichen 840 und 365
//
//                lambda = 0.6931/T_halb;
//                chem_Abbau = -lambda*actualC;  //[micro gN/l]
//            }
            
            // biologischer Abbau und Remineralisierung
            double bio_Abbau = 0.;

            double R = 106./16.; // Redfieldverh�ltnis C:N
            
            //Aufbau - Mineralization of detritus
            double sourceDetrit = 0.;
            if (detritData != null) 
                sourceDetrit = detritData.detritMineral;
            
            //Aufbau - Respiration of phytoplankton and zooplankton
            double sourcePhyto = 0.;
            if( phytoData != null ) 
                sourcePhyto = phytoData.phytoRespiration;
            
            double sourceZoo = 0.;
            if( zooData != null )
                sourceZoo = zooData.zooRespiration;
            
            //Abbau - Alge
            double sinkPhyto = 0.;
            if ( phytoData != null && !(nitratmodeldata.skonc <= 0.))
                sinkPhyto = phytoData.phytoGrowth;

            bio_Abbau = sourcePhyto + sourceZoo + sourceDetrit - sinkPhyto;
            
            quellterm = /**chem_Abbau +*/ bio_Abbau;
            
        }
        //m: Molar mass of nitrate
        return quellterm;// / m; //[micro mol N/l]                              
        
    }//end getSourceSunk
    
    
    
    
    /** the method readConcentrationDat read the datas for skonc 
     *  from a sysdat-file named filename
     *  @param nam  name of the file to be open */
    //  NEU TINO 14.06.2007
    private void readConcentrationDat(String filename) {
        int rand_knoten=0;
        int gebiets_knoten = 0;
        int knoten_nr;
        
        
        double x,y,skonc;
        
        String line;
        
        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');
            
            System.out.println("\tReading Concentration-File (in TiCAD-System.Dat-Format): "+filename);
            
            do
            {
                line=systemfile.freadLine();
            }while(line.startsWith("C"));
            java.util.StringTokenizer strto=new StringTokenizer(line," \t\n\r\f,");
            rand_knoten=Integer.parseInt(strto.nextToken());
            
            do
            {
                line=systemfile.freadLine();
            }while(line.startsWith("C"));
            
            
            strto=new StringTokenizer(line," \t\n\r\f,");
            gebiets_knoten=Integer.parseInt(strto.nextToken());
            
            if (rand_knoten<0 || rand_knoten>10000000 || gebiets_knoten<0 || gebiets_knoten>10000000 )
                throw new Exception("Fehler");
            
            //System.out.println(""+rand_knoten+" "+gebiets_knoten);
            
            // Knoten einlesen
            // DOF[] dof= new DOF[rand_knoten+gebiets_knoten];
            int p_count=0;
            while( p_count<(rand_knoten+gebiets_knoten) ) {
                line=systemfile.freadLine();
                strto=new StringTokenizer(line," \t\n\r\f,");
                
                //System.out.println(""+line+"\n");
                if (!line.startsWith("C")) {
                    knoten_nr=Integer.parseInt(strto.nextToken());
                    x=Double.parseDouble(strto.nextToken());
                    y=Double.parseDouble(strto.nextToken());
                    try {
                         skonc=Double.parseDouble(strto.nextToken()); 
                         
                    } catch(Exception ex) {
                        skonc=Double.NaN;
                    }
                    
                    if (Double.isNaN(skonc) || skonc<0){
                        
                        System.out.println("");
                        
                        System.out.println("********************************       ERROR         ***********************************");
                        System.out.println("Invalid skonc-value (skonc=NaN or skonc<0.0) in Concentration-File: <"+filename+"> node number <"+p_count+">");
                        System.out.println("To correct this problem ensure that node nr <"+p_count+"> has a correct floating point (greater zero)");
                        System.out.println("Concentration  value");
                        System.out.println("*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                    DOF dof=fenet.getDOF(knoten_nr);
                    NitrogenModel2DData.extract(dof).skonc=skonc;
                     
                    
                    //if(p_count%1000==0) System.out.println(p_count);
                    p_count++;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Konzentration-Datei!");
            System.exit(0);
//            System.out.println("\t\tcannot open file: "+filename);
//            System.out.println("\t\tuse standard value for d50: 0.42 mm");
//            DOF[] dof = fenet.getDOFs();
//            for (int i=0; i<dof.length; i++){
//                SedimentModel2DData.extract(dof[i]).d50 = 0.00042;
//            }
        }
        
        
        
    }
    // ENDE NEU TINO 14.06.2007
    
      /** the method readConcentrationFromJanetBin read the datas for skonc 
     *  from  a JanetBinary-file named filename
     *  @param nam  name of the file to be open */
    //  NEU TINO 14.06.2007    
    private void readConcentrationFromJanetBin(String filename) {
        int anzAttributes=0;
        double x,y,skonc;        
        
        boolean hasValidValues=true;        
        int nr;
        short status,kennung;
        int anzPolys,anzEdges,anzPoints=0,pointsize,trisize,swapMode;
        short sets;
        boolean active,protectBorder,protectConstraints,noPolygon,inPolygon,makeHoles,processFlagsActive;
        boolean noZoom,inZoom,noActive,processActive,processSelected,inPolygonProp,inZoomProp,protectInnerPoints;
        boolean noSelected,closed;
        boolean read_status_byte=false;
        
        FileIO bin_in=new FileIO();
        
        try {
            bin_in.fopenbinary( filename, FileIO.input );
            
            // Netz aus einer Binaerdatei lesen
            
            // Version auslesen
            float version=bin_in.fbinreadfloat();
            if (version<1.5f){
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: "+version+", current version: 1.8");
            }
            
            if (version<1.79)
                read_status_byte=true;
            
            System.out.println("\t Read Concentration-File from "+filename);
            
            // zunaechst den FileHeader lesen
            boolean writePointNumbers=bin_in.fbinreadboolean();
            boolean writePointAttributes=bin_in.fbinreadboolean();
            anzAttributes=bin_in.fbinreadint();
            boolean writePointStatus=bin_in.fbinreadboolean();
            boolean writeConstraintPolygons=bin_in.fbinreadboolean();
            boolean writeConstraintEdges=bin_in.fbinreadboolean();
            boolean writeElements=bin_in.fbinreadboolean();
            boolean writeElementNumbers=bin_in.fbinreadboolean();
            boolean writeElementKennung=bin_in.fbinreadboolean();
            boolean writeAlphaTestRadius=bin_in.fbinreadboolean();
            
            // Layertyp ueberlesen
            int filetype=bin_in.fbinreadint();
            // liegt UnTRIM-Gitetr mit diskreten Kantentiefen vor??
            boolean is_untrim=(filetype==2);
            
            // Anzahl der Punkte lesen
            int anzk=bin_in.fbinreadint();
            if(anzk==fenet.getNumberofDOFs()){
                
                // Punkte lesen
                for (int i=0; i<anzk; i++) {
                    // Punktnummer lesen
                    if (writePointNumbers)
                        nr=bin_in.fbinreadint();
                    else
                        nr=i;
                    
                    // x,y,s lesen
                    x=bin_in.fbinreaddouble();
                    y=bin_in.fbinreaddouble();                    
                    skonc=bin_in.fbinreaddouble(); 
                    
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(skonc) || skonc<=0.)
                    	hasValidValues=false;
                 
                    DOF dof=fenet.getDOF(nr);
                    NitrogenModel2DData.extract(dof).skonc=skonc;
                   
                    
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                    
                }
                
                // Abbruch, wenn Netz nicht ok!
                if(!hasValidValues)
                {
                	System.out.println("***                     WARNUNG                       ***");
                	System.out.println("***   Ueberpruefen Sie die Konzentration-Werte des   ***");
                	System.out.println("***   Konzentrationnetzes. Das verwendetet Netz hat      ***");
                	System.out.println("***   Knoten mit negativen oder nicht definierten     ***");
                	System.out.println("***   Konzentrationen!                       ***");
                	System.out.println("***   Die Simulation wird nicht fortgesetzt                 ***");
                	System.exit(0);
                }
                
            } else System.out.println("system und concentration.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Konzentration-Datei!");
            System.exit(0);
        }
    }    
    // ENDE NEU TINO 14.06.2007
    
    //------------------------------------------------------------------------
    // readBoundCond
    //------------------------------------------------------------------------
    
        /*the method readBoundCond read the datas for boudary conditions
         * from a file named nam
         * @param nam  name of the file to be open */
    public final void readBoundCond(String nam) {
        int   pointer=0;
        int   q_read=0;
        int anz_identische_Knoten;
        int i1;
        int K; // Knotennur
        
        int Gesamtzahl; // Gesamtanzahl
        int Anz_Werte; // Anzahl Werte
        byte art_rndwrt=0;
        
        double feld[][];
        int KnotenNr[];
        
        DiscretScalarFunction1d randfkt;
        
        FileInputStream is=null;
        try {
            is = new FileInputStream(nam);
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ nam + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('C');
        
        int anzrndwrt = TicadIO.NextInt(st); // Gesamtzahl Konten f�r die Randwerte anzugeben sind
        System.out.println("anzrndwrt ="+anzrndwrt );
        KnotenNr = new int[anzrndwrt]; // Knotennr. = Anzahl Knoten mit identischen Randwerten
        
        while(pointer < anzrndwrt){
//            System.out.println("pointer "+pointer+"   anzrndwrt"+anzrndwrt);
            anz_identische_Knoten = TicadIO.NextInt(st); // Anzahl Knoten mit identischen Randwerten
            System.out.print("anzidentische knoten "+anz_identische_Knoten);
            q_read = 0;
            
            Anz_Werte = TicadIO.NextInt(st);
            
            for ( K = pointer; K < pointer+anz_identische_Knoten; K++ ) {
                i1 = TicadIO.NextInt(st); //Knotennr.
                KnotenNr[K] = i1;
//                System.out.print(" "+i1);
            }
            System.out.println(" ");
            
            
            feld = new double [2][Anz_Werte];
            
            for ( K = 0; K <= Anz_Werte-1; K++ ) {
                feld[0][K] = TicadIO.NextDouble(st);
                feld[1][K] = TicadIO.NextDouble(st);
//                System.out.println(feld[0][K]+" "+feld[1][K]);
            }
            
            DiscretScalarFunction1d boundcond = new DiscretScalarFunction1d(feld);
            boundcond.setPeriodic(true);
            for ( K = pointer; K < pointer+anz_identische_Knoten; K++ ){
                Point3d pt = (fenet.getDOF(KnotenNr[K]));
                bskonc.addElement(new BoundaryConditionOld(KnotenNr[K], boundcond));
               // bskonc.addElement(new BoundaryCondition(KnotenNr[K], boundcond));
            }
            pointer += anz_identische_Knoten;
        }
        System.out.println("Randwerte gelesen");
    }//end readBoundCond

    public ModelData genData(FElement felement) {
        System.out.println("noch nicht genutzt");
        return null;
    }
    
}