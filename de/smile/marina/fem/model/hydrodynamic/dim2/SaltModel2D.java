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

/** this ODE describe salt-transport model for depth integrated simulations
 * @version 4.7.0
 * @author Peter Milbradt
 */
public class  SaltModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel  {
    
    private SaltModel2DData[] dof_data=null;

    private CurrentModel2DData[] dof_currentdata=null;
    private Current2DElementData[] element_currentdata=null;
    
    public final static double dispersionCoefficient = 1.1E-9; // Diffusionskoeffizient fuer Salz in m^2/s
    
    private DataOutputStream xf_os = null;
    
    private ArrayList<DOF> initsc = new ArrayList<>();
    
    private final ArrayList<BoundaryCondition> bsc = new ArrayList<>();
    private final ArrayList<BoundaryCondition> sQ = new ArrayList<>();
    
    private SaltDat saltdat;
    private double previousTimeStep;
    
    public SaltModel2D(CurrentModel2D currentmodel, SaltDat saltdata) {
        System.out.println("SaltModel2D initalization");
        fenet = currentmodel.fenet;        
        dof_currentdata = currentmodel.dof_data;
        element_currentdata = currentmodel.element_data;
        femodel=this;
        this.saltdat = saltdata;

        dof_data= new SaltModel2DData[fenet.getNumberofDOFs()];
        
        setNumberOfThreads(saltdat.NumberOfThreads);
        
        readBoundCond();
        
        bsc.forEach((bcond) -> {
            initsc.add(fenet.getDOF(bcond.pointnumber));
        });
        
        // DOFs initialisieren
        initialDOFs();
        
        try {
            xf_os = new DataOutputStream(new FileOutputStream(saltdat.xferg_name));
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ saltdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }
    
    @Override
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske Salz
        return TicadIO.HRES_SALT;
    }
    
    private double initialSaltConcentration(DOF dof, double time) {
        //    System.out.println("initialSalt");
        double sc=0., R=0., d;
        final int i = dof.number;
        SaltModel2DData saltdata = dof_data[i];
        if (saltdata.bsc != null)
            sc = saltdata.bsc.getValue(time);
        else {
            for (DOF ndof : initsc) {
                final int j = dof.number;
                SaltModel2DData salt = dof_data[j];
                if ((dof!=ndof) & ( salt.bsc != null )){
                    d = dof.distance(ndof);
                    sc += salt.bsc.getValue(time) / d;
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
    
      /** initialisiert die Salzkonzentration mit einem konstanten Wert
     * @param initalvalue
     * @return is null and not used
     */
     public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value "+initalvalue);
        
        for(SaltModel2DData saltmodeldata : dof_data){

            saltmodeldata.C = initalvalue;
        }
        return null;
    }
     
    /** Read the start solution from file
     * @param salterg file with simulation results
     * @param record record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    public double[] initialSolutionFromTicadErgFile(String salterg, int record) throws Exception {

        System.out.println("\tRead inital values from result file " + salterg);
        //erstes Durchscannen
        File sysergFile = new File(salterg);
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

            //Elemente, Rand und Knoten √ºberlesen
            inStream.skip((anzElemente * 4L + anzr + 3L * anzKnoten) * 4L); //4 Bytes je float und int

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4L) * record);

            inStream.readFloat(); // read time in float
            for (int i = 0; i < fenet.getNumberofDOFs(); i++) {

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
                    dof_data[i].C = inStream.readFloat();
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
    
    
    
    /** the method  initialSaltConcentrationFromSysDat read the datas for skonc 
     *  from a sysdat-file named filename
     *  @param filename  name of the file to be open
     * @param time
     * @return is null and not used
     * @throws java.lang.Exception */
    public double[] initialSaltConcentrationFromSysDat(String filename, double time) throws Exception {
        this.time=time;        
        
        double skonc;
        
        String line;
        
        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');
            
            System.out.println("\tReading SaltConcentration-File (in TiCAD-System.Dat-Format): "+filename);
            
            do
            {
                line=systemfile.freadLine();
            }while(line.startsWith("C"));
            java.util.StringTokenizer strto=new StringTokenizer(line," \t\n\r\f,");
            int rand_knoten=Integer.parseInt(strto.nextToken());
            
            do
            {
                line=systemfile.freadLine();
            }while(line.startsWith("C"));
            
            
            strto=new StringTokenizer(line," \t\n\r\f,");
            int gebiets_knoten=Integer.parseInt(strto.nextToken());
            
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
                    int knoten_nr=Integer.parseInt(strto.nextToken());
                    strto.nextToken();
                    strto.nextToken();
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
                    dof_data[knoten_nr].C=skonc;
                    
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
    
      /** the method initialSaltConcentrationFromJanetBin read the datas for skonc 
     *  from  a JanetBinary-file named filename
     *  @param filename  name of the file to be open 
     * @param time 
     * @return is always null and not used
     * @throws java.lang.Exception */ 
     public double[] initialSaltConcentrationFromJanetBin(String filename, double time) throws Exception {
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
                 
                    dof_data[nr].C=skonc;
                    
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
        
        System.out.println("SaltModel2D - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            dof_data[i].C = initialSaltConcentration(dof, time);
        }
        initsc=null;
        return null;
    }
    
    
    @Deprecated
    @Override
    public double[] getRateofChange(double time, double x[]){
        return null;
    }
    
    //------------------------------------------------------------------------
    // ElementApproximation
    //------------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element){

        double timeStep=Double.POSITIVE_INFINITY;
        final Current2DElementData eleCurrentData = element_currentdata[element.number];
        
        FTriangle ele = (FTriangle) element;
        final double[][] koeffmat = ele.getkoeffmat();
        // compute element derivations
        double dsaltconcdx = 0.;
        double dsaltconcdy = 0.;
        for (int j = 0; j < 3; j++) {
            DOF dof = ele.getDOF(j);
            final int i = dof.number;

            dsaltconcdx += dof_data[i].C * koeffmat[j][1];
            dsaltconcdy += dof_data[i].C * koeffmat[j][2];
        }

        if(eleCurrentData != null){
            if(!eleCurrentData.isDry) {

                final double[] terms_Salt    = new double[3];
                
                final double u_mean     = eleCurrentData.u_mean;
                final double v_mean     = eleCurrentData.v_mean;

                final double current_mean = Function.norm(u_mean, v_mean);
                final double elementsize = eleCurrentData.elementsize;
                
                // dispersion
                final double astx = eleCurrentData.astx * eleCurrentData.wlambda + dispersionCoefficient;
                final double asty = eleCurrentData.asty * eleCurrentData.wlambda + dispersionCoefficient;
            
                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    final DOF dof = ele.getDOF(j);
                    final int i = dof.number; 
                    final SaltModel2DData saltmodeldata = dof_data[i];
                    final CurrentModel2DData  currentmodeldata  = dof_currentdata[i];
                    
                    terms_Salt[j] = (currentmodeldata.u * dsaltconcdx + currentmodeldata.v * dsaltconcdy)
//                                        // turbulence term - weiter unten
//                                        - (astx * eleCurrentData.ddepthdx * dsaltconcdx + asty * eleCurrentData.ddepthdy * dsaltconcdy) / Function.max(currentmodeldata.totaldepth,CurrentModel2D.WATT) * currentmodeldata.wlambda
//                                        + 3. * (koeffmat[j][1] * astx * dsaltconcdx + koeffmat[j][2] * asty * dsaltconcdy) * currentmodeldata.wlambda
//                                        - 3. * source_dCdt// wird im Zeitschritt beruecksichtigt
                            ;
                    
                    if ((saltmodeldata.bsc == null) && !saltmodeldata.extrapolate && !currentmodeldata.boundary)
                        Koeq1_mean += 1. / 3. * ( saltmodeldata.dsaltconcdt + terms_Salt[j] ) * currentmodeldata.wlambda;
                }

                double tau_konc=0.;
                if (current_mean > 1.E-5) {
                    tau_konc = 0.5 * elementsize / current_mean;
                    timeStep = tau_konc;
                }
                
                
                for (int j = 0; j < 3; j++) {
                    final DOF dof = ele.getDOF(j);
                    final int i = dof.number;
                    final SaltModel2DData saltmodeldata = dof_data[i];
                    final CurrentModel2DData  cmd  = dof_currentdata[i];
                    // Fehlerkorrektur durchfuehren
                    double result_SaltConc_i = -tau_konc * (koeffmat[j][1] * u_mean + koeffmat[j][2] * v_mean) * Koeq1_mean * ele.area;
                    if(result_SaltConc_i>0) result_SaltConc_i *= cmd.wlambda; // Konzentration will wachsen, Knoten aber Wattknoten
                    // Diffusionsterm (entspricht ∇⋅(D∇c))
                    result_SaltConc_i -= (koeffmat[j][1] * astx * dsaltconcdx + koeffmat[j][2] * asty * dsaltconcdy) * cmd.wlambda * ele.area
                                // KORREKTURTERM fuer variable Wassertiefe (entspricht 1/d * (D∇d)⋅(∇c))
                                // Dieser Term korrigiert die Diffusion, wenn sich die Tiefe aendert.
                                - 1./3. * (1. / Function.max(cmd.totaldepth,CurrentModel2D.WATT)) * (eleCurrentData.ddepthdx * astx * dsaltconcdx + eleCurrentData.ddepthdy * asty * dsaltconcdy) * cmd.wlambda * ele.area;
                    
                    // Begin standart Galerkin-step
                    for (int l = 0; l < 3; l++) {
                        final double vorfak =  ele.area * ((l == j) ? 1./6. : 1./12.);
                        final double gl = (l == j) ? 1. :  Math.min(dof_currentdata[ele.getDOF(l).number].wlambda, dof_currentdata[ele.getDOF(l).number].totaldepth/Math.max(CurrentModel2D.WATT,cmd.totaldepth));
                        result_SaltConc_i -= vorfak * terms_Salt[l]*gl;
                    }
                    synchronized (saltmodeldata) {
                        saltmodeldata.rsaltconc += result_SaltConc_i;
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
        
        final int i = dof.number;
        SaltModel2DData saltmodeldata = dof_data[i];

        final CurrentModel2DData  cmd = dof_currentdata[i];
        
        if (saltmodeldata.bsc != null){
            saltmodeldata.C=saltmodeldata.bsc.getValue(t);
//            saltmodeldata.dsaltconcdt = saltmodeldata.bsc.getDifferential(t);
        }

        if (saltmodeldata.extrapolate) {
            SaltModel2DData tmpdata;
            for (FElement elem : dof.getFElements()) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            CurrentModel2DData tmpcmd = dof_currentdata[elem.getDOF((ll + ii) % 3).number];
                            tmpdata = dof_data[elem.getDOF((ll + ii) % 3).number];
                            double dC = (saltmodeldata.C - tmpdata.C) / 10. * Math.min(1., tmpcmd.totaldepth / Math.max(CurrentModel2D.WATT, cmd.totaldepth));
                            if (tmpdata.extrapolate) {
                                dC /= 10.;
                            }
                            synchronized (saltmodeldata) {
                                saltmodeldata.C -= dC;
                            }
                        }
                        break;
                    }
                }
            }
        }

        saltmodeldata.sourceSink = 0.0;
        if (cmd.totaldepth > CurrentModel2D.WATT) {
            if (cmd.sourceQ != null) {
                double area = 0.;
                FElement[] feles = dof.getFElements();
                for (FElement fele : feles) // muss nicht jedes mal neu berechet werden
                {
                    area += fele.getVolume();
                }
                double cquelle = 0.0;
                if (saltmodeldata.sourceQc != null) {
                    cquelle = saltmodeldata.sourceQc.getValue(time);
                }
                double c = saltmodeldata.C;
                double source_dhdt = cmd.sourceQ.getValue(time) / area;
                saltmodeldata.sourceSink = (source_dhdt / (cmd.totaldepth)) * (cquelle - c);
            }
        } else {
            saltmodeldata.sourceSink = dispersionCoefficient / 100. * (0. - saltmodeldata.C);
        }

        // Rechte Seite initialisieren
        saltmodeldata.rsaltconc=0.;
    }
    
    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof){
        SaltModel2DData data = new SaltModel2DData();
        int dofnumber = dof.number;
        dof_data[dofnumber]=data;
        
        for(BoundaryCondition bcond : bsc){
            if (dofnumber == bcond.pointnumber) {
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
        
        CurrentModel2DData  current = dof_currentdata[dofnumber];
        
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
        System.out.println("deprecated method is called");
    }

    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            for (DOF dof : fenet.getDOFs()) {
                int i = dof.number;
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                SaltModel2DData saltmodeldata = dof_data[i];
                xf_os.writeFloat((float) saltmodeldata.C);
            }
            xf_os.flush();
        } catch (IOException e) {
            System.out.println(this.getClass()+"\n\ttime="+time+"\n");
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public ModelData genData(FElement felement) {
        System.out.println("noch nicht genutzt");
        return null;
    }
    
    /** Neue Einleseroutine readBoundCond 
     * liest die spezifizierten Datensaetze (Randbedingungen) in der boundary_condition_key_mask 
     * aus entsprechenden Randwertedatei (saltdat.rndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in saltdat.rndwerteReader)
     */
    public final void readBoundCond() {

        String[] boundary_condition_key_mask = new String[2];

        boundary_condition_key_mask[0] = BoundaryCondition.concentration_salt;
        boundary_condition_key_mask[1] = BoundaryCondition.pointbased_salt_source;

        try {
            for (BoundaryCondition bc : saltdat.rndwerteReader.readBoundaryConditions(boundary_condition_key_mask)) {
                if (bc.boundary_condition_key.equals(BoundaryCondition.concentration_salt)) {
                    bsc.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.pointbased_salt_source)) {
                    sQ.add(bc);
                }
            }
        } catch (Exception e) {
            System.exit(1);
        }

    } // end readBoundCond

    @Override
    public void timeStep(double dt) {
        resultIsNaN = false;

        setBoundaryConditions();

        maxTimeStep = Double.MAX_VALUE;

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
            final int j = dof.number;
            SaltModel2DData saltmodeldata = dof_data[j];
            CurrentModel2DData currentmodeldata = dof_currentdata[j];

            saltmodeldata.rsaltconc /= dof.lumpedMass;

            // Variable Adams-Bashforth 2. Ordnung
            double rSalt = beta0 * saltmodeldata.rsaltconc + beta1 * saltmodeldata.dsaltconcdt;

            saltmodeldata.dsaltconcdt = saltmodeldata.rsaltconc;

            rSalt *= currentmodeldata.puddleLambda * currentmodeldata.wlambda; // in Pfuetzen und auf trockenen Knoten keine Veraenderung der Konzentration
            
            rSalt += saltmodeldata.sourceSink; // Salzquellen und -senken bruecksichtigen

            saltmodeldata.C += dt * rSalt;
            /* prevention of negative concentration */
            if (saltmodeldata.C < 0.)   saltmodeldata.C = 0.;
            /* prevention of concentration grater then max. of 36% = 360 ppt*/
            if (saltmodeldata.C > 360.)   saltmodeldata.C = 360.; // Peter 12.09.19

            boolean rIsNaN = Double.isNaN(rSalt);
            if (rIsNaN) System.out.println("Salttransport is NaN bei " + dof.number + " dsaltconcdt=" + rSalt);
            resultIsNaN |= rIsNaN;
        });

        this.time += dt;

        if (resultIsNaN) {
            System.out.println("Time=" + this.time + " and timestep is" + dt);
            write_erg_xf();
            try {
                xf_os.close();
            } catch (IOException e) {
            }
            System.exit(0);
        }
    }
}