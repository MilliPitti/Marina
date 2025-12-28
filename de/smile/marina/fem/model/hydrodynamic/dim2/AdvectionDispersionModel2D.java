/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2025

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
package de.smile.marina.fem.model.hydrodynamic.dim2;

import bijava.math.ifunction.ZeroFunction1d;
import de.smile.marina.MarinaXML;
import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.*;
import java.util.*;

/** this ODE describe Advection-Dispersion model for depth integrated simulations
 * @version 4.7.0
 * @author Peter Milbradt
 */
public class  AdvectionDispersionModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel  {
    
    public double dispersionCoefficient = 1.1E-9; // Diffusionskoeffizient fuer Salz in m^2/s
    public double degradationRate = 0.;
    
    private double previousTimeStep = 0.0; // Speichert den vorherigen Zeitschritt für das gesamte Modell
    
    private DataOutputStream xf_os = null;
    private FileOutputStream xf_fs = null;
    
    private ArrayList<DOF> initsc = new ArrayList<>();
    
    private ArrayList<BoundaryCondition> bsc  = new ArrayList<>();
    private ArrayList<BoundaryCondition> sQ = new ArrayList<>();
    
    private AdvectionDispersionDat addat;
    
    public AdvectionDispersionModel2D(FEDecomposition fe, AdvectionDispersionDat addata) {
        fenet = fe;
        femodel=this;
        this.addat = addata;
        System.out.println("AdvectionDispersionModel2D initalization");

        this.dispersionCoefficient = addata.dispersionCoefficient;
        this.degradationRate = addata.degradationRate;
        
        setNumberOfThreads(addat.NumberOfThreads);
        
        readBoundCond();
        bsc.stream().forEach((bcond) -> {
            initsc.add(fenet.getDOF(bcond.pointnumber));
        });
//        BoundaryCondition bcond;        
//        Enumeration be = bsc.elements();
//        while (be.hasMoreElements()) {
//            bcond = (BoundaryCondition) be.nextElement();
//            initsc.addElement(fenet.getDOF(bcond.pointnumber));
//        }
        
        // DOFs initialisieren
        initialDOFs();
        // element results initialization
        initialElementModelData();
        
        try {
            xf_fs = new FileOutputStream(addat.xferg_name);
            xf_os = new DataOutputStream(xf_fs);
            // Setzen der Ergebnismaske (Tiefe, Transport x, Transport y)
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ addat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @Override
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske Salz
        return TicadIO.HRES_SALT;
    }
    
    private double initialConcentration(DOF dof, double time) {
        double sc=0., R=0., d;
        
        AdvectionDispersionModel2DData addata = AdvectionDispersionModel2DData.extract(dof);
        if (addata.bsc != null)
            sc = addata.bsc.getValue(time);
        else {
            for (DOF ndof : initsc) {
                AdvectionDispersionModel2DData conc = AdvectionDispersionModel2DData.extract(ndof);
                if ((dof!=ndof) & ( conc.bsc != null )){
                    d = dof.distance(ndof);
                    sc += conc.bsc.getValue(time) / d;
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
    
      /** initialisiert die Konzentration mit einem konstanten Wert
     * @param initalvalue
     * @return 
     */
     public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value "+initalvalue +" mg/l");
        for (DOF dof: fenet.getDOFs()){
           AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);
           adModelData.C = initalvalue;
        }
        return null;
    }
     
    /** Read the start solution from file
     * @param aderg file with simulation results
     * @param record record in the file
     * @return the vector of st
     * @throws java.lang.Exception
     */
    public double[] initialSolutionFromTicadErgFile(String aderg, int record) throws Exception {

        System.out.println("\tread Inital Values from result file " + aderg);
        //erstes Durchscannen
        File sysergFile = new File(aderg);
        try (FileInputStream stream = new FileInputStream(sysergFile); DataInputStream inStream = new DataInputStream(stream)) {

            // Kommentar lesen, bis ASCII-Zeichen 7 kommt
            int c;
            do {
                c = inStream.readByte();
            } while (c != 7);
            // Ende Kommentar

            //Anzahl Elemente, Knoten und Rand lesen
            int anzKnoten = inStream.readInt();
            if (fenet.getNumberofDOFs() != anzKnoten) {
                System.out.println("Die Datei mit den Startwerten hat andere Anzahl von Knoten");
                System.exit(1);
            }
            int anzr = inStream.readInt();
            int anzElemente = inStream.readInt();

            //Ueberlesen folgende Zeilen
            inStream.skip(9 * 4);

            //Ergebnismaske lesen und auswerten
            int ergMaske = inStream.readInt();
            int anzWerte = TicadIO.ergMaskeAuswerten(ergMaske);

            boolean None_gesetzt = ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE);
            boolean Pos_gesetzt = ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS);
            boolean Z_gesetzt = ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z);
            boolean V_gesetzt = ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V);
            boolean Q_gesetzt = ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q);
            boolean H_gesetzt = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);
            boolean SALT_gesetzt = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);
            boolean EDDY_gesetzt = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);
            boolean SHEAR_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR);
            boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL);
            boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
            boolean AH_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);

            inStream.readInt();

            //Elemente, Rand und Knoten ueberlesen
            inStream.skip((anzElemente * 4L + anzr + 3L * anzKnoten) * 4L); //4 Bytes je float und int

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4L) * record);

            float t = inStream.readFloat();
            for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
                DOF dof = fenet.getDOF(i);
                AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);

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
                    inStream.skip(4);
                }

                if (SALT_gesetzt) {
                    adModelData.C = inStream.readFloat();
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
        return null;
    }
    
    
    
    /** the method  initialConcentrationFromSysDat read the datas for skonc 
     *  from a sysdat-file named filename
     *  @param filename  name of the file to be open
     * @param time
     * @return 
     * @throws java.lang.Exception */
    public double[] initialConcentrationFromSysDat(String filename, double time) throws Exception {
        this.time=time;
        int rand_knoten;
        int gebiets_knoten;
        int knoten_nr;
        
        
        double skonc;
        
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
                    strto.nextToken();
                    strto.nextToken();
                    try {
                         skonc=Double.parseDouble(strto.nextToken()); 
                    } catch(NumberFormatException ex) {
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
                    AdvectionDispersionModel2DData.extract(dof).C=skonc;
                    
                    //if(p_count%1000==0) System.out.println(p_count);
                    p_count++;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Salzkonzentration-Datei!");
            System.exit(0);
        }
        
        return null;
        
    }
    
      /** the method initialConcentrationFromJanetBin read the datas for skonc 
     *  from  a JanetBinary-file named filename
     *  @param filename  name of the file to be open 
     * @param time 
     * @return  
     * @throws java.lang.Exception */ 
     public double[] initialConcentrationFromJanetBin(String filename, double time) throws Exception {
        int anzAttributes=0;
        double skonc;
        
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
                    bin_in.fbinreaddouble();
                    bin_in.fbinreaddouble();                    
                    skonc=bin_in.fbinreaddouble(); 
                    
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(skonc) || skonc<0.)
                    	hasValidValues=false;
                 
                    DOF dof=fenet.getDOF(nr);
                    AdvectionDispersionModel2DData.extract(dof).C=skonc;
                    
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
            System.out.println("Fehler beim Lesen der Salzkonzentration-Datei!");
            System.exit(0);
        }
        
        return null;
    }    
    
    
    public double[] initialSolution(double time){
        System.out.println("AdvectionDispersionModel2D - Werte Initialisieren");
        DOF[] dof = fenet.getDOFs();
        for (DOF dof1 : dof) {
            AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof1);
            adModelData.C = initialConcentration(dof1, time);
        }
        initsc=null;
        return null;
    }
    
    
    /**
     * @deprecated
     * @param time
     * @param x
     * @return
     */
    @Override
    @Deprecated
    public double[] getRateofChange(double time, double x[]){
        
        return null;
        
    } // end getRateofChange
    
    //------------------------------------------------------------------------
    // ElementApproximation
    //------------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element){

        double timeStep=Double.POSITIVE_INFINITY;

        FTriangle ele = (FTriangle) element;
        
        Current2DElementData eleCurrentData = Current2DElementData.extract(element);
        if(eleCurrentData != null){
            if(!eleCurrentData.isDry){
                
                final double[][] koeffmat = ele.getkoeffmat();
                
                final double[] terms_AD    = new double[3];
                
                double u_mean     = eleCurrentData.u_mean;
                double v_mean     = eleCurrentData.v_mean;
                
                AdvectionDispersionModel2DElementData eleADData = AdvectionDispersionModel2DElementData.extract(element);
                double absResiduum = 0.;
                
                // compute element derivations
                //-------------------------------------------------------------------
                double dsaltconcdx = 0.;
                double dsaltconcdy = 0.;
                for (int j = 0; j<3; j++) {
                    DOF dof = ele.getDOF(j);
                    AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);
                    
                    dsaltconcdx += adModelData.C * koeffmat[j][1];
                    dsaltconcdy += adModelData.C * koeffmat[j][2];
                } // end for
                
                double current_mean = Function.norm(u_mean, v_mean);
                double elementsize = eleCurrentData.elementsize;
                
                // dispersion
                double astx = eleCurrentData.astx + dispersionCoefficient + eleADData.absRes * elementsize;
                double asty = eleCurrentData.asty + dispersionCoefficient + eleADData.absRes * elementsize;

                
                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);
                    final CurrentModel2DData  cmd  = CurrentModel2DData.extract(dof);
                    
                    terms_AD[j] = (cmd.u * dsaltconcdx + cmd.v * dsaltconcdy)
                                // Diffusionsterm (entspricht ∇⋅(D∇c))    
                                + (koeffmat[j][1] * astx * dsaltconcdx + koeffmat[j][2] * asty * dsaltconcdy) * cmd.wlambda
                                // KORREKTURTERM fuer variable Wassertiefe (entspricht 1/d * (D∇d)⋅(∇c)):  korrigiert die Diffusion, wenn sich die Tiefe aendert.
                                - 1./3. * (1. / Function.max(cmd.totaldepth,CurrentModel2D.WATT)) * (eleCurrentData.ddepthdx * astx * dsaltconcdx + eleCurrentData.ddepthdy * asty * dsaltconcdy) * cmd.wlambda
//                                  - 3. * source_dCdt // wird im Zeitschritt beruecksichtigt
                                + 3. * degradationRate * adModelData.C; 
                    
                    if ((adModelData.bsc == null) && !adModelData.extrapolate/*  && !cmd.boundary*/)
                        Koeq1_mean += 1. / 3. * ( adModelData.dadconcdt + terms_AD[j] ) * cmd.wlambda;
                    
                    absResiduum += Math.abs(adModelData.dadconcdt + terms_AD[j]);
                }
                
                eleADData.absRes = absResiduum;
                
                double tau_konc=0.;
                if (current_mean > 1.E-5) {
                    tau_konc = 0.5 * elementsize / current_mean;
                    timeStep = tau_konc;
//                    double tau = Function.norm(astx,asty);
//                    double a_opt = 1.;
//                    if(tau>0.00001) {
//                        double peclet = current_mean * elementsize / tau;
//                        a_opt = Function.coth(peclet) - 1.0 / peclet;
//                    }
//                    tau_konc *= a_opt;
                }
                
                // Fehlerkorrektur durchfuehren
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);
                    final CurrentModel2DData  cmd  = CurrentModel2DData.extract(dof);
                    
                    double result_ADConc_i = -tau_konc * (koeffmat[j][1] * u_mean + koeffmat[j][2] * v_mean) * Koeq1_mean * ele.area;
                    if(result_ADConc_i>0) result_ADConc_i *= cmd.wlambda; // Konzentration will wachsen, Knoten aber Wattknoten
                    
//                    // tubulenc-term 
//                    result_ADConc_i -= 
//                                // Diffusionsterm (entspricht ∇⋅(D∇c))    
//                                + (koeffmat[j][1] * astx * dsaltconcdx + koeffmat[j][2] * asty * dsaltconcdy) * cmd.wlambda * ele.area
//                                // KORREKTURTERM fuer variable Wassertiefe (entspricht 1/d * (D∇d)⋅(∇c)):  korrigiert die Diffusion, wenn sich die Tiefe aendert.
//                                - 1./3. * (1. / Function.max(cmd.totaldepth,CurrentModel2D.WATT)) * (eleCurrentData.ddepthdx * astx * dsaltconcdx + eleCurrentData.ddepthdy * asty * dsaltconcdy) * cmd.wlambda * ele.area;
                    
                    // Begin standart Galerkin-step
                    for (int l = 0; l < 3; l++) {
                        final double vorfak =  ele.area * ((l == j) ? 1./6. : 1./12.);
                        final double gl = (l == j) ? 1. :  Function.min(CurrentModel2DData.extract(ele.getDOF(l)).wlambda, CurrentModel2DData.extract(ele.getDOF(l)).totaldepth/Function.max(CurrentModel2D.WATT,cmd.totaldepth));
                        result_ADConc_i -= vorfak * terms_AD[l]*gl;
                    }
                    synchronized (adModelData) {
                        adModelData.radconc += result_ADConc_i;
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
        
        AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);
        
        /* prevention of negative concentration */
        if (adModelData.C < 0.)   adModelData.C = 0.;
        
        if (adModelData.bsc != null){
            adModelData.C=adModelData.bsc.getValue(t);
            adModelData.dadconcdt = adModelData.bsc.getDifferential(t);
        }

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        if (adModelData.extrapolate && (cmd.totaldepth >= CurrentModel2D.WATT)) {
            AdvectionDispersionModel2DData tmpdata;
            for (FElement elem : dof.getFElements()) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            CurrentModel2DData tmpcmd = CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3));
                            if (tmpcmd.totaldepth > CurrentModel2D.WATT) {
                                tmpdata = AdvectionDispersionModel2DData.extract(elem.getDOF((ll + ii) % 3));
                                double dC = (adModelData.C - tmpdata.C) / 100.;
                                if (tmpdata.extrapolate) {
                                    dC /= 10.;
                                }
                                double lambda = Math.min(1., tmpcmd.totaldepth/cmd.totaldepth);
                                synchronized (adModelData) {
                                    adModelData.C -= dC*lambda;
                                }
                                synchronized (tmpdata) {
                                    tmpdata.C += dC / elem.getDOF((ll + ii) % 3).getNumberofFElements() * dof.getNumberofFElements();
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
//
//        if ( cmd.totaldepth < CurrentModel2D.WATT) {
//            int anz = 0;
//            double meanC = 0.;
//            FElement[] felem = dof.getFElements();
//            for (int j = 0; j < felem.length; j++) {
//                FElement elem = felem[j];
//                for (int ll = 0; ll < 3; ll++) {
//                    if (elem.getDOF(ll) == dof) {
//                        for (int ii = 1; ii < 3; ii++) {
//                            if (CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3)).totaldepth > CurrentModel2D.WATT) {
//                                meanC += AdvectionDispersionModel2DData.extract(elem.getDOF((ll+ii)%3)).C;
//                                anz++;
//                            }
//                        }
//                    }
//                    break;
//                }
//            }
//            if (anz > 0) {
//                adModelData.C = cmd.w1_lambda * 0. + cmd.wlambda * meanC / anz;
//            } else {
//                adModelData.C = cmd.w1_lambda * 0. + cmd.wlambda * adModelData.C;
//            }
//        }

        adModelData.sourceSink = 0.;
        if (cmd.totaldepth > CurrentModel2D.WATT) {

            if (cmd.sourceQ != null) {
                double area = 0.;
                FElement[] feles = dof.getFElements();
                for (FElement fele : feles) // muss nicht jedes mal neu berechet werden
                {
                    area += fele.getVolume();
                }
                double cquelle = 0.0;
                if (adModelData.sourceQc != null) {
                    cquelle = adModelData.sourceQc.getValue(time);
                }
                double c = adModelData.C;
                double source_dhdt = cmd.sourceQ.getValue(time) / area;
                adModelData.sourceSink = (source_dhdt / (cmd.totaldepth)) * (cquelle - c);
            }
        } else {
            adModelData.sourceSink = dispersionCoefficient/100. * (0. - adModelData.C);
        }

        // Rechte Seite initialisieren
        adModelData.radconc=0.;
    }
    
    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof){
        AdvectionDispersionModel2DData data = new AdvectionDispersionModel2DData();
        int dofnumber = dof.number;
        
        for(BoundaryCondition bcond : bsc){
            if ( dofnumber == bcond.pointnumber ){
                data.bsc = bcond.function;
                bsc.remove(bcond);
                break;
            }
        }
        
        for(BoundaryCondition bcond : sQ){
            if (dofnumber == bcond.pointnumber) {
                data.sourceQc = bcond.function;
                sQ.remove(bcond);
                break;
            }
        }
        
        CurrentModel2DData  current = CurrentModel2DData.extract(dof);
        
        // nicht vollstaendig spezifizierte Randbedingungen schaetzen
        data.extrapolate= ((data.bsc == null) && ( (current.bu instanceof ZeroFunction1d) && (current.bv instanceof ZeroFunction1d) && (current.bh == null)));
        return data;
    }
    
    /**
     * @deprecated
     * @param erg
     * @param t
     */
    @Override
    @Deprecated
    public void write_erg_xf( double[] erg, double t) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            for (DOF dof : fenet.getDOFs()) {
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);
                xf_os.writeFloat((float)adModelData.C);
            }
            xf_os.flush();
        } catch (Exception e) {
            System.out.println(this.getClass()+"\n\ttime="+time+"\n");
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public ModelData genData(FElement felement) {
        AdvectionDispersionModel2DElementData res = new AdvectionDispersionModel2DElementData();
        return res;
    }
    
    /** Neue Einleseroutine readBoundCond 
     * liest die spezifizierten Datensaetze (Randbedingungen) in der boundary_condition_key_mask 
     * aus entsprechenden Randwertedatei (addat.rndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in addat.rndwerteReader)
     */
    public final void readBoundCond() {
               
        String[] boundary_condition_key_mask=new String[2];
       
        boundary_condition_key_mask[0]=BoundaryCondition.concentration_salt;
        boundary_condition_key_mask[1]=BoundaryCondition.pointbased_salt_source;
       
        try{
            for (BoundaryCondition bc : addat.rndwerteReader.readBoundaryConditions(boundary_condition_key_mask)) {
                if (bc.boundary_condition_key.equals(BoundaryCondition.concentration_salt)) {
                    bsc.add(bc);  
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.pointbased_salt_source)) {
                    sQ.add(bc);
                }
            }
        }
        catch(Exception e){
            System.exit(1);
        }
                
    } // end readBoundCond

    @Override
    public void timeStep(double dt) {
        resultIsNaN = false;

        setBoundaryConditions();

        maxTimeStep = 10000.;

        // Elementloop
        performElementLoop();
        
        // Berechne omega und die Koeffizienten fuer Variable Adams-Bashforth 2. Ordnung einmal vor dem parallelen Stream
        final double beta0, beta1;
        if (previousTimeStep == 0.0) {
            // Erster Schritt: Euler-Integration (beta0=1, beta1=0)
            beta0 = 1.0;
            beta1 = 0.0;
        } else {
            double omega = dt / previousTimeStep / 2.;
            beta0 = 1.0 + omega;
            beta1 = -omega;
        }
        
        Arrays.stream(fenet.getDOFs()).parallel().forEach(dof -> {
            AdvectionDispersionModel2DData adModelData = AdvectionDispersionModel2DData.extract(dof);

            adModelData.radconc /= dof.lumpedMass;

            // Variable Adams-Bashforth 2. Ordnung
            double rConcentration = beta0 * adModelData.radconc + beta1 * adModelData.dadconcdt;

            adModelData.dadconcdt = adModelData.radconc;
            
            rConcentration += adModelData.sourceSink; // Stoffquellen und -senken bruecksichtigen

            adModelData.C += dt * rConcentration;
            /* prevention of negative concentration */
            if (adModelData.C < 0.)   adModelData.C = 0.;

            boolean rIsNaN = Double.isNaN(rConcentration);
            if (rIsNaN) System.out.println("AdvectionDispersionTransport is NaN bei " + dof.number + " dconcdt=" + rConcentration);
            resultIsNaN |= rIsNaN;
        });

        this.time += dt;

        if (resultIsNaN) {
            System.out.println("Time=" + this.time + " and timestep is" + dt);
            write_erg_xf();
            try {
                xf_os.close();
            } catch (Exception e) {
            }
            System.exit(0);
        }
    }
}