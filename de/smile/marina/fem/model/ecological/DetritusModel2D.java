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

import de.smile.marina.fem.*;
import de.smile.math.Function;
import bijava.math.ifunction.DiscretScalarFunction1d;
import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.model.hydrodynamic.dim2.Current2DElementData;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2D;
import de.smile.marina.fem.model.meteorology.MeteorologyData2D;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.TicadIO;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 *
 * @version 1.2.15
 */
public class DetritusModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel {
    
    private DataOutputStream xf_os = null;
    
    private Vector<DOF> initsc = new Vector<>();
    
    private Vector<BoundaryCondition> bsc  = new Vector<>();
    
    private int n,DetritConc,numberofdofs;
    
    private DetritusDat detritdat;
    
    private double[] result;// zum speichern der Zeitableitungen
    private double[] x;     // zum Speichern der Zustandsgroessen
    
    // Erdbeschleunigung
    static final double AST      = 0.0012;      // 0.0012 Austauschkoeffizient fuer Stroemung
    
    static final double BATTJESKOEFF=0.3; 	// 0.1 - 0.3 Austauschkoeffizient infolge Wellenbrechens
    
    // hier baruch ich noch eine Gute Idee um die Variable Watt nicht nur implements Strmungsmodell verwenden kann
    private double WATT=0.1;
    
    /** Creates a new instance of DetritusModel2D */
    public DetritusModel2D(FEDecomposition fe, DetritusDat detritdat) {
        fenet = fe;
        femodel=this;
        this.detritdat = detritdat;
        System.out.println("DetritusModel2D initalization");
        
        setNumberOfThreads(detritdat.numberOfThreads);
        
        readBoundCond(detritdat.detrit_rndwerteName);
        
        bsc.forEach((bcond) -> {
            initsc.add(fenet.getDOF(bcond.pointnumber));
        });
        
        // DOFs initialisieren
        initialDOFs();
        
        numberofdofs = fenet.getNumberofDOFs();
        DetritConc = 0;
        n = numberofdofs;
        result = new double[n];
        
        try {
            xf_os = new DataOutputStream(new FileOutputStream(detritdat.xferg_name));
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ detritdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }
    
    
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske Konzentration
        return TicadIO.HRES_H;
    }
     
    /**
     * @deprecated
     */
     @Deprecated
    public int getResultSize() {
        return n;
    }
    
    private double initialDetritConcentration(DOF dof, double time) {
        //    System.out.println("initialSalt");
        double sc=0., R=0., d;
        
        DetritusModel2DData detritdata = DetritusModel2DData.extract(dof);
        if (detritdata.bsc != null)
            sc = detritdata.bsc.getValue(time);
        else {
            for (DOF ndof : initsc) {
                DetritusModel2DData detrit = DetritusModel2DData.extract(ndof);
                if ((dof != ndof) && (detrit.bsc != null)) {
                    d = dof.distance(ndof);
                    sc += detrit.bsc.getValue(time) / d;
                    R += 1. / d;
                }
            }
            if (R != 0.)
                sc /= R;
            else
                sc = 0.;
        }
        return sc;
    }
    
    /** Read the start solution from file
     * @param detriterg file with simulation results
     * @param record record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unused")
    public double[] initialSolutionFromTicadErgFile(String detriterg, int record) throws Exception {
    	
        System.out.println("\t Read inital values from result file "+detriterg);
	//erstes Durchscannen
	File sysergFile=new File(detriterg);
        double[] x;
        try (FileInputStream stream = new FileInputStream(sysergFile); DataInputStream inStream = new DataInputStream(stream)) {
            
            // Kommentar lesen, bis ASCII-Zeichen 7 kommt
            int c;
            do {
                c = inStream.readByte();
            } while (c != 7);
            // Ende Kommentar
            
            //Anzahl Elemente, Knoten und Rand lesen
            int anzKnoten=inStream.readInt();
            if (fenet.getNumberofDOFs()!=anzKnoten) {
                System.out.println("Die Datei mit den Startwerten hat andere Anzahl von Knoten");
                System.exit(1);
            }   int anzr=inStream.readInt();
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
                DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
                
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
                    detritmodeldata.detritconc = x[DetritConc + i] = inStream.readFloat();
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
    
    public double[] readDetritConcentrationFromASCII(String name) {

        System.out.println("DetritusModel2D - Read inital values from ASCII file.");
        double[] x = null;

        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(name)));

            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('C');

            // read Point3ds
            int anzr = TicadIO.NextInt(st);
            int anzk = anzr + TicadIO.NextInt(st);
            if (fenet.getNumberofDOFs() == anzk) {
                x = new double[getResultSize()];
                for (int j = 0; j < anzk; j++) {
                    int pos = TicadIO.NextInt(st);
                    DOF dof = fenet.getDOF(pos);
                    DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
                    TicadIO.NextDouble(st);
                    TicadIO.NextDouble(st);
                    detritmodeldata.detritconc = x[DetritConc + j] = TicadIO.NextDouble(st);
                }
            } else {
                System.out.println("The initial file have a different number of nodes.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("The file " + name + " cannot be opened!");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        return x;
    }
    
    
    public double[] initialSolution(double time){
        
        double x[] = new double[getResultSize()];
        
        System.out.println("DetritusModel2D - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
            detritmodeldata.detritconc = x[DetritConc + i] = initialDetritConcentration(dof, time);
        }
        initsc=null;
        write_erg_xf(x, time);
        return x;
    }
    
    /** initialisiert die Detrituskonzentration mit einem konstanten Wert
     * @param initalvalue
     * @return 
     */
     public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value "+initalvalue +" mg/l");

        for (DOF dof: fenet.getDOFs()){
           DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
           detritmodeldata.detritconc = initalvalue;
        }
        return null;
    }
    
    
    //------------------------------------------------------------------------
    // getRateofChange
    //------------------------------------------------------------------------
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
            DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
            int gamma = dof.getNumberofFElements();
            result[DetritConc+i] *= 3. / gamma;
            detritmodeldata.dDetritConcdt = result[DetritConc+i];
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
                
                final double[] terms_Detrit    = new double[3];
                
                double u_mean       = eleCurrentData.u_mean;
                double v_mean       = eleCurrentData.v_mean;
                
                // compute element derivations
                //-------------------------------------------------------------------
                double dDetritConcdx = 0.;
                double dDetritConcdy = 0.;
                for (int j = 0; j<3; j++) {
                    DOF dof = ele.getDOF(j);
                    DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
                    
                    dDetritConcdx += detritmodeldata.detritconc * koeffmat[j][1];
                    dDetritConcdy += detritmodeldata.detritconc * koeffmat[j][2];
                } // end for
                
                double current_mean = Function.norm(u_mean, v_mean);
                double elementsize = eleCurrentData.elementsize;  // Peter 08.08.08
                
                //eddy viscosity
                double astx = eleCurrentData.astx;
                double asty = eleCurrentData.asty;
                
                // Phytoplankton Quellen und Senken f�rs Element bestimmen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    squelle[j] = getSourceSink(dof);
                            //getSourceSunk(nitratmodeldata.skonc,C_Algen,depth[j]);
                }
                
                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
                    CurrentModel2DData  currentmodeldata  = CurrentModel2DData.extract(dof);
                    
                    terms_Detrit[j] = currentmodeldata.u * dDetritConcdx + currentmodeldata.v * dDetritConcdy
                                        - (astx * eleCurrentData.ddepthdx * dDetritConcdx + asty * eleCurrentData.ddepthdy * dDetritConcdy)/ ((currentmodeldata.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT : currentmodeldata.totaldepth)
                                        + 3. * (koeffmat[j][1] * astx * dDetritConcdx + koeffmat[j][2] * asty * dDetritConcdy)
                                        - squelle[j];
                    
                    Koeq1_mean += 1. / 3. * ( detritmodeldata.dDetritConcdt + terms_Detrit[j] );
                }

                double tau_konc=0.;
                if (current_mean > 1.E-5) {
                    tau_konc = 0.5 * elementsize / current_mean;

                    timeStep = tau_konc;// *1./(1.+Math.abs(Koeq1_mean)); Peter 03.08.2016

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
                    final int i = dof.number;
                    CurrentModel2DData  cmd  = CurrentModel2DData.extract(dof);
                    
                    synchronized (result) {
                        result[DetritConc+i] -= tau_konc * (koeffmat[j][1] * u_mean * Koeq1_mean + koeffmat[j][2] * v_mean * Koeq1_mean);
                    }
                    
                    // Begin standart Galerkin-step
                    for (int l = 0; l < 3; l++) {
                        double vorfak = (l == j) ? 1./6. : 1./12.;
                        final double gl = (l == j) ? 1. :  Math.min(cmd.wlambda, CurrentModel2DData.extract(ele.getDOF(l)).totaldepth/Math.max(CurrentModel2D.WATT,cmd.totaldepth)); // Peter 21.01.2016
                        synchronized (result) {
                            result[DetritConc+i] -= vorfak * terms_Detrit[l]*gl;
                        }
                    }
                }
            }
        }
        return timeStep;
    } // end ElementApproximation
    
    //Calculating the Source and sink terms for phytoplankton
    private double getSourceSink(DOF dof){
        double sourceSink = 0.;
        double d = dof.z + CurrentModel2DData.extract(dof).eta;
        if(d > WATT) {           
            double phytoMortalSource = getSourcePhytoMortal(dof);//[1/sec]
            double zooMortalSource = getSourceZooMortal(dof);//[1/sec]
            double detritMineralization = getSinkMineralization(dof);

            sourceSink = phytoMortalSource
                       + zooMortalSource
                       - detritMineralization;
        }
        
        return sourceSink;
    }
    private double getSourcePhytoMortal(DOF dof) {
        double sourcePhyto = 0.;
        PhytoplanktonModel2DData phytoData = PhytoplanktonModel2DData.extract(dof);
        sourcePhyto = phytoData.phytoMortality;
//        if (dof.number == 248){
//            System.out.println("GrowthRate:::::::::::::" + grossGrowthRate);
////            System.out.println("N---------------------" + nutrientLimitation);
//        }
        return sourcePhyto;
    }

    private double getSourceZooMortal(DOF dof) {
        double sourceZoo = 0.;
        ZooplanktonModel2DData zooData = ZooplanktonModel2DData.extract(dof);            
        sourceZoo = zooData.zooMortality;
//        if (dof.number == 248){
//            System.out.println("GrowthRate:::::::::::::" + grossGrowthRate);
////            System.out.println("N---------------------" + nutrientLimitation);
//        }
        return sourceZoo;
    }
    
    private double getSinkMineralization(DOF dof) {
        DetritusModel2DData detritData = DetritusModel2DData.extract(dof);
        detritData.detritMineral = 0.;
        if (!(detritData.detritconc <= 0.)) {
            detritData.detritMineral  = DetritusDat.DETRITUS_MINERALIZATION_RATE * detritData.detritconc;
        }
        return detritData.detritMineral;
    }
    
    //PHYTO
    private double getPhytoConcentration(DOF dof) {
        PhytoplanktonModel2DData phytoModelData = PhytoplanktonModel2DData.extract(dof);
        if (phytoModelData != null) {       // Ueberpruefen, ob Phytoplanktonmodell vorliegt
            return phytoModelData.phytoconc;
        } else {                              // sonst initialisieren
            return 0.;
        }
    }
    
    //ZOO
    private double getZooConcentration(DOF dof) {
        ZooplanktonModel2DData zooModelData = ZooplanktonModel2DData.extract(dof);
        double zooConc;
        if (zooModelData != null) {       // Ueberpruefen, ob Zooplanktonmodell vorliegt
            return zooModelData.zooconc;
        } else {                              // sonst initialisieren
            return 0.;
        }
    }
    //Nitrate
    private double getNitratConcentration(DOF dof) {
        NitrogenModel2DData nitratModelData = NitrogenModel2DData.extract(dof);
        double nitratConc;
        if (nitratModelData != null) {       // Ueberpruefen, ob nitratmodell vorliegt
            double M = 14.0067 + 3. * 15.9994; //Molare Masse Nitrat
//                double erg3 = erg[SKonc + i]*M/1000*10; //Umrechnung von micromol/l in mg/l und 10-fach �berh�ht
            return nitratModelData.skonc;// * M; //[micro g/l]
        } else {                              // sonst initialisieren
            return 0.;
        }
    }
    
    
    //Temprature (Meteorology)
    private double getTemperature(DOF dof) {
        MeteorologyData2D meteorologyData = MeteorologyData2D.extract(dof);
        if (meteorologyData != null) {       // Ueberpruefen, ob meteorologymodell vorliegt
            return meteorologyData.temperature;
        } else {                              // sonst initialisieren
//            temperature = 0.;
//            light = 0.;
            return 0.;//or someother value
        }
    }
    //Light (Meteorology)
    private double getSurfaceLightIntensity(DOF dof) {
        MeteorologyData2D meteorologyData = MeteorologyData2D.extract(dof);

        if (meteorologyData != null) {       // Ueberpruefen, ob meteorologymodell vorliegt
            return meteorologyData.insolation;
        } else {                              // sonst initialisieren
//            temperature = 0.;
//            light = 0.;
            return 0.;//or someother value
        }
    }
    private double getLightIntensity(DOF dof, double waterDepth) {
        double lightIntensity = 0.;
        double surfaceIntensity = getSurfaceLightIntensity(dof);
        double photoActivityCeoff = PhytoplanktonDat.PHOTOSYNTHETIC_ACTIVITY_COEFFICIENT;
        double lightExtCoeff = PhytoplanktonDat.LIGHT_EXTINCTION_COEFFICIENT;
        lightIntensity = photoActivityCeoff * surfaceIntensity * Math.exp(-lightExtCoeff * waterDepth);
        return lightIntensity;
    }
    
    //------------------------------------------------------------------------
    // setBoundaryCondition
    //------------------------------------------------------------------------
    public void setBoundaryCondition(DOF dof, double t){
        
        int i = dof.number;
        DetritusModel2DData detritmodeldata = DetritusModel2DData.extract(dof);
        
        /* prevention of negative concentration */
        if (x[DetritConc + i] <= 0.)   x[DetritConc + i] = 0.;
        
        if (detritmodeldata.bsc != null){
            x[DetritConc + i]=detritmodeldata.bsc.getValue(t);
            detritmodeldata.dDetritConcdt = detritmodeldata.bsc.getDifferential(t);
        }

        if (detritmodeldata.extrapolate){
            DetritusModel2DData tmpdata;
            FElement[] felem = dof.getFElements();
            for(int j=0; j<felem.length;j++) {
                FElement elem=felem[j];
                for(int ll=0;ll<3;ll++){
                    if(elem.getDOF(ll)==dof){
                        for(int ii=1;ii<3;ii++){
                            tmpdata = DetritusModel2DData.extract(elem.getDOF((ll+ii)%3));
                            int jtmp = elem.getDOF((ll+ii)%3).number;
                            if (!tmpdata.extrapolate){
                                x[DetritConc + i] = (9.*x[DetritConc + i] + 1. * x[DetritConc + jtmp])/10.;
                            }
                        }
                    }
                    break;
                }
            }
        }
        
        detritmodeldata.detritconc = x[DetritConc + i];
    }
    
    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    public ModelData genData(DOF dof){
        DetritusModel2DData data = new DetritusModel2DData();
        int dofnumber = dof.number;
        final ScalarFunction1d[] found = new ScalarFunction1d[1];
        bsc.removeIf(bcond -> {
            if (dofnumber == bcond.pointnumber) {
                found[0] = bcond.function;
                return true;
            }
            return false;
        });
        data.bsc = found[0];
        
        CurrentModel2DData  currentmodeldata = CurrentModel2DData.extract(dof);
        
        // nicht vollstaendig spezifizierte Randbedingungen schaetzen
        data.extrapolate= ((data.bsc == null) && ( (currentmodeldata.bh != null)
                        || (currentmodeldata.bu != null) || (currentmodeldata.bv != null)
                        || (currentmodeldata.bqx != null) || (currentmodeldata.bqy != null)
                        || (currentmodeldata.bQx != null) || (currentmodeldata.bQy != null)
                                                                         ));
        return data;
    }
    
    
    //------------------------------------------------------------------------
    // write_erg_xf
    //------------------------------------------------------------------------
    @Override
    public void write_erg_xf( double[] erg, double t) {
        try {
            xf_os.writeFloat((float) t);
            DOF[] dof = fenet.getDOFs();
            for (int j=0; j<dof.length;j++){
                if(erg[j]<0.) erg[j]=0.;    // Peter 12.03.10
                xf_os.writeFloat((float)erg[j]);
            }
            xf_os.flush();
        } catch (Exception e) {}
    }
    
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
    @SuppressWarnings("unused")
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
    
    
    /**
     * the method readBoundCond read the datas for boudary conditions
     * from a file named nam
     * @param nam  name of the file to be open
     */
    public final void readBoundCond(String nam) {
        int pointer = 0;
        int q_read = 0;
        int anz_identische_Knoten;
        int i1;
        int K; // Knotennur

        int Gesamtzahl; // Gesamtanzahl
        int Anz_Werte; // Anzahl Werte
        byte art_rndwrt = 0;

        double feld[][];
        int KnotenNr[];

        DiscretScalarFunction1d randfkt;

        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(nam)));
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('C');

            int anzrndwrt = TicadIO.NextInt(st); // Gesamtzahl Konten f�r die Randwerte anzugeben sind
            System.out.println("anzrndwrt =" + anzrndwrt);
            KnotenNr = new int[anzrndwrt]; // Knotennr. = Anzahl Knoten mit identischen Randwerten

            while (pointer < anzrndwrt) {
                System.out.println("pointer " + pointer + "   anzrndwrt" + anzrndwrt);
                anz_identische_Knoten = TicadIO.NextInt(st); // Anzahl Knoten mit identischen Randwerten
                System.out.print("anzidentische knoten " + anz_identische_Knoten);
                q_read = 0;

                Anz_Werte = TicadIO.NextInt(st);

                for (K = pointer; K < pointer + anz_identische_Knoten; K++) {
                    i1 = TicadIO.NextInt(st); //Knotennr.
                    KnotenNr[K] = i1;
//                System.out.print(" "+i1);
                }
                System.out.println(" ");

                feld = new double[2][Anz_Werte];

                for (K = 0; K <= Anz_Werte - 1; K++) {
                    feld[0][K] = TicadIO.NextDouble(st);
                    feld[1][K] = TicadIO.NextDouble(st);
//                System.out.println(feld[0][K]+" "+feld[1][K]);
                }

                DiscretScalarFunction1d boundcond = new DiscretScalarFunction1d(feld);
                boundcond.setPeriodic(true);
                for (K = pointer; K < pointer + anz_identische_Knoten; K++) {
                    bsc.addElement(new BoundaryCondition("Concentration", KnotenNr[K], boundcond));
                }
                pointer += anz_identische_Knoten;
//            System.out.println("Test2:	Pointer="+pointer);
            }
            System.out.println("Randwerte gelesen");
        } catch (FileNotFoundException e) {
            System.out.println("The file " + nam + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public ModelData genData(FElement felement) {
        System.out.println("noch nicht genutzt");
        return null;
    }
}
