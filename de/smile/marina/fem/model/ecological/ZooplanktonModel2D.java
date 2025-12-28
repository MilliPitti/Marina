package de.smile.marina.fem.model.ecological;

import bijava.math.ifunction.DiscretScalarFunction1d;
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
import de.smile.marina.fem.model.meteorology.MeteorologyData2D;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.vecmath.Point3d;

/**
 *
 * @version 2.7.4
 * @author Peter Milbradt
 */
public class ZooplanktonModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel {
    private DataOutputStream xf_os = null;
    
    private Vector initsc = new Vector();
    
    private Vector bsc  = new Vector();
    
    private int n,ZooConc,numberofdofs;
    
    private ZooplanktonDat zoodat;
    
    private double[] result;// zum speichern der Zeitableitungen
    private double[] x;     // zum Speichern der Zustandsgroessen
    
    // Erdbeschleunigung
    static final double AST      = 0.0012;      // 0.0012 Austauschkoeffizient fuer Stroemung
    
    static final double BATTJESKOEFF=0.3; 	// 0.1 - 0.3 Austauschkoeffizient infolge Wellenbrechens
    
    // hier baruch ich noch eine Gute Idee um die Variable Watt nicht nur implements Strmungsmodell verwenden kann
    private double WATT=0.1;
    
    public ZooplanktonModel2D(FEDecomposition fe, ZooplanktonDat zoodat) {
        fenet = fe;
        femodel=this;
        this.zoodat = zoodat;
        System.out.println("ZooplanktonModel2D initalization");
        
        setNumberOfThreads(zoodat.numberOfThreads);
        
        readBoundCond(zoodat.zoo_rndwerteName);
        BoundaryCondition bcond;
        Enumeration be = bsc.elements();
        while (be.hasMoreElements()) {
            bcond = (BoundaryCondition) be.nextElement();
            initsc.addElement(fenet.getDOF(bcond.pointnumber));
        }
        
        // DOFs initialisieren
        initialDOFs();
        
        numberofdofs = fenet.getNumberofDOFs();
        ZooConc = 0;
        n = numberofdofs;
        result = new double[n];
        
        try {
            xf_os = new DataOutputStream(new FileOutputStream(zoodat.xferg_name));
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ zoodat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }
    
     
    @Override
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske Konzentration
        return TicadIO.HRES_H;
    }
    
    private double initialZooConcentration(DOF dof, double time) {
        //    System.out.println("initialSalt");
        double sc=0., R=0., d;
        
        ZooplanktonModel2DData zoodata = ZooplanktonModel2DData.extract(dof);
        if (zoodata.bsc != null)
            sc = zoodata.bsc.getValue(time);
        else {
            for (Enumeration e = initsc.elements(); e.hasMoreElements();){
                DOF ndof= (DOF) e.nextElement();
                ZooplanktonModel2DData zoo = ZooplanktonModel2DData.extract(ndof);
                if ((dof!=ndof) & ( zoo.bsc != null )){
                    d = dof.distance(ndof);
                    sc += zoo.bsc.getValue(time) / d;
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
    
    /** Read the start solution from file
     * @param zooerg file with simulation results
     * @param record record in the file
     * @return the vector of start solution
     */
    public double[] initialSolutionFromTicadErgFile(String zooerg, int record) throws Exception {
    	
        System.out.println("\t Read inital values from result file "+zooerg);
	//erstes Durchscannen
	File sysergFile=new File(zooerg);
        double[] x;
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
            }   int anzr=inStream.readInt();
            int anzElemente=inStream.readInt();
            //ueberlesen folgende Zeilen
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
            x = new double[n];
            float time=inStream.readFloat();
            for (int i = 0;i<fenet.getNumberofDOFs();i++){
                DOF dof=fenet.getDOF(i);
                ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof);
                
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
                    zoomodeldata.zooconc = x[ZooConc + i] = inStream.readFloat();
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
    
    
    /** the method readConcentrationDat read the datas for skonc 
     *  from a sysdat-file named filename
     *  @param nam  name of the file to be open */
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
    
      /** the method readConcentrationFromJanetBin read the datas for skonc 
     *  from  a JanetBinary-file named filename
     *  @param nam  name of the file to be open */   
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
    
    
    public double[] readZooConcentrationFromASCII(String name){
        
        System.out.println("ZooplanktonModel2D - Read inital values from ASCII file.");
        double[] x = null;
        
        InputStream is = null;
	try
	{
	    is = new FileInputStream(name);
	} catch (Exception e) {}
	    
	BufferedReader r = new BufferedReader(new InputStreamReader(is));

	StreamTokenizer st = new StreamTokenizer(r);
	st.eolIsSignificant (true);
	st.commentChar('C');
		
	// read Point3ds
	int anzr = TicadIO.NextInt(st);
	int anzk = anzr + TicadIO.NextInt(st);
        if (fenet.getNumberofDOFs() == anzk){
            x = new double[n];
            for(int j = 0;j<anzk;j++) {
                int pos=TicadIO.NextInt(st);
                DOF dof=fenet.getDOF(pos);
                ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof);
                TicadIO.NextDouble(st);
                TicadIO.NextDouble(st);
                zoomodeldata.zooconc = x[ZooConc + j] = TicadIO.NextDouble(st);
            }
        } else
            System.out.println("The initial file have a different number of nodes.");
        return x;
    }
    
    
    public double[] initialSolution(double time){
        
        double x[] = new double[n];
        
        System.out.println("ZooplanktonModel2D - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof);
            zoomodeldata.zooconc = x[ZooConc + i] = initialZooConcentration(dof, time);
        }
        initsc=null;
        write_erg_xf(x, time);
        return x;
    }
    
    /** initialisiert die Phytoplanktonkonzentration mit einem konstanten Wert
     * @param initalvalue
     * @return 
     */
     public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value "+initalvalue +" mg/l");
        
        for (DOF dof: fenet.getDOFs()){
           ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof);
           zoomodeldata.zooconc = initalvalue;
        }
        return null;
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
        
        DOF[] dof = fenet.getDOFs();
        for (int j=0; j<dof.length;j++){
            int i = dof[j].number;
            ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof[j]);
            int gamma = dof[j].getNumberofFElements();
            
            result[ZooConc+i] *= 3. / gamma;
            
            zoomodeldata.dZooConcdt = result[ZooConc+i];
        }
        
        return result;
        
    } // end getRateofChange
    
    //------------------------------------------------------------------------
    // ElementApproximation
    //------------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element){
        
        double timeStep=Double.MAX_VALUE;
        
        Current2DElementData eleCurrentData = Current2DElementData.extract(element);
        if(eleCurrentData != null){
            if(eleCurrentData.iwatt < 3){
                
                FTriangle ele = (FTriangle) element;
                double[][] koeffmat = ele.getkoeffmat();
                double[] squelle    = new double[3];
                
                double[] terms_Zoo    = new double[3];
                
                double u_mean       = eleCurrentData.u_mean;
                double v_mean       = eleCurrentData.v_mean;
                
                // compute element derivations
                //-------------------------------------------------------------------
                double dZooConcdx = 0.;
                double dZooConcdy = 0.;
                for (int j = 0; j<3; j++) {
                    DOF dof = ele.getDOF(j);
                    ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof);
                    
                    dZooConcdx += zoomodeldata.zooconc * koeffmat[j][1];
                    dZooConcdy += zoomodeldata.zooconc * koeffmat[j][2];
                } // end for
                
                double current_mean = Function.norm(u_mean, v_mean);
                double elementsize = eleCurrentData.elementsize;  // Peter 05.08.08
                
                //eddy viscosity
                double astx = eleCurrentData.astx;
                double asty = eleCurrentData.asty;
                
                // Zooplankton Quellen und Senken f�rs Element bestimmen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    squelle[j] = getZooSourceSink(dof);
                }
                
                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof);
                    CurrentModel2DData  currentmodeldata  = CurrentModel2DData.extract(dof);
                    
                    terms_Zoo[j] = currentmodeldata.u * dZooConcdx + currentmodeldata.v * dZooConcdy // Peter 02.08.2010
                                        - (astx * eleCurrentData.ddepthdx * dZooConcdx + asty * eleCurrentData.ddepthdy * dZooConcdy)/ ((currentmodeldata.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT : currentmodeldata.totaldepth)
                                        + 3. * (koeffmat[j][1] * astx * dZooConcdx
                                                    + koeffmat[j][2] * asty * dZooConcdy)
                                                    - squelle[j];
                    
                    Koeq1_mean += 1. / 3. * ( zoomodeldata.dZooConcdt + terms_Zoo[j] );
                }

                double tau_konc=0.;
                if (current_mean > 1.E-5) {
                    tau_konc = 0.5 * elementsize / current_mean;

                    timeStep=tau_konc*1./(1.+Math.abs(Koeq1_mean));

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
                        result[ZooConc + i] -= tau_konc * (koeffmat[j][1] * u_mean * Koeq1_mean + koeffmat[j][2] * v_mean * Koeq1_mean );
                    }
                    
                    // Begin standart Galerkin-step
                    for (int l = 0; l < 3; l++) {
                        final double vorfak = (l == j) ? 1./6. : 1./12.;
                        final double gl = (l == j) ? 1. :  Math.min(cmd.wlambda, CurrentModel2DData.extract(ele.getDOF(l)).totaldepth/Math.max(CurrentModel2D.WATT,cmd.totaldepth)); // Peter 21.01.2016
                        synchronized (result) {
                            result[ZooConc+i] -= vorfak * terms_Zoo[l]*gl;
                        }
                    }
                }
            }
        }
        return timeStep;
    } // end ElementApproximation
    
    //Calculating the Source and sink terms for phytoplankton
    private double getZooSourceSink(DOF dof){
        double sourceSink = 0.;
        double d = dof.z + CurrentModel2DData.extract(dof).eta;
        if(d > WATT) {
            ZooplanktonModel2DData zooData = ZooplanktonModel2DData.extract(dof);
            //Concentration of phytoplankton in the previous time step
            double zooConcentration = zooData.zooconc;//[mg/l]
            double zooBackConc = ZooplanktonDat.getBACKGROUND_CONCENTRATION();
            
            //Gross growth rate for phytoplankton
            zooData.zooGrowth = getGrossGrowthRate(dof) * zooConcentration;
            //Respiration, essudation, non-predatory mortality and settling rate
            //PHYTOPLANKTON ---------> NUTRIENTS
            zooData.zooRespiration = getRespireEssudationRate(dof) * (zooConcentration - zooBackConc);
            //non-predatory mortality
            zooData.zooMortality = getMortalityRate(dof) * (zooConcentration - zooBackConc);
            //Grazing loss due to zooplankton existence
            double grazingLoss = getGrazingLoss(dof);//[mg/l]

            sourceSink = zooData.zooGrowth
                        - zooData.zooRespiration
                        - zooData.zooMortality
                        - grazingLoss * (zooConcentration - zooBackConc);
        }
        return sourceSink;
    }
    
    private double getGrossGrowthRate(DOF dof) {
        double grossGrowthRate = 1.;
        double filtrationParameter = ZooplanktonDat.getFILTRATION_PARAMETER();
        double phytoConcentration = getPhytoConcentration(dof);
        double maxPhytoPredation = ZooplanktonDat.getMAX_PHYTOPLANKTON_PREDATION_RATE();
        double ivlevParam = ZooplanktonDat.getIVLEV_PARAMETER();
//        double ingestionRate = filtrationParameter * phytoConcentration;
//        double ingestionRate = ZooplanktonDat.getPHYTOPLANKTON_PREDATION_RATE();
//        double assimilationPara = ZooplanktonDat.ASSIMILATION_PARAMETER;
        double referenceTemp = ZooplanktonDat.REFERENCE_TEMPERATURE;
        double temperature = getTemperature(dof);
        double temperatureEffect = Math.exp((temperature - referenceTemp)/ temperature);
//        double temperatureEffect = Math.pow((temperature / referenceTemp),5);
        if(phytoConcentration <= 0. || ZooplanktonModel2DData.extract(dof).zooconc <= 0.) {
            grossGrowthRate = 0.;
        } else {
//            grossGrowthRate = assimilationPara * ingestionRate;
            grossGrowthRate = filtrationParameter * maxPhytoPredation * (1 - Math.exp(-ivlevParam * phytoConcentration))*temperatureEffect;
//            grossGrowthRate = filtrationParameter * maxPhytoPredation * (phytoConcentration / (1/ivlevParam + phytoConcentration));
        }
        
        return grossGrowthRate;
    }
    
//    private double getTemperatureVariationEffect(DOF dof) {
//        double tempEffect = 1.0;
//        double temperature = getTemperature(dof);
//        double referenceTemp = PhytoplanktonDat.REFERENCE_TEMPERATURE;
//        tempEffect = Math.exp(temperature - referenceTemp);
//        return tempEffect;
//    }
//    private double getLightLimitationEffect(DOF dof, double waterDepth) {
//        double lightLimitation = 1.;
//        double lightIntensity = getLightIntensity(dof, waterDepth);
//        double lightSemisatConstant = PhytoplanktonDat.getLIGHT_SEMISATURATION_CONSTANT();
//        lightLimitation = lightIntensity / (lightSemisatConstant + lightIntensity);
//        return lightLimitation;
//    }
    private double getNutrientLimitationEffect(DOF dof) {
        double nutrientLimitation = 1.0;
        return nutrientLimitation;
    }
    private double getRespireEssudationRate(DOF dof) {
        double rate = 0.;
        double referenceRate = ZooplanktonDat.getRES_ESSUD_MORTAL_REFERENCE_RATE();
        double referenceTemp = ZooplanktonDat.REFERENCE_TEMPERATURE;
        double temperature = getTemperature(dof);
        rate = referenceRate;// * Math.exp((temperature - referenceTemp) / temperature);
        return rate;
    }
    private double getMortalityRate(DOF dof) {
        double rate = 0.;
        double referenceRate = ZooplanktonDat.MORTAL_REFERENCE_RATE;
        double referenceTemp = ZooplanktonDat.REFERENCE_TEMPERATURE;
        double temperature = getTemperature(dof);
        rate = referenceRate;// * Math.exp((temperature - referenceTemp) / temperature);
        return rate;
    }
    private double getGrazingLoss(DOF dof) {
        //Constant because zooplankton is the top level of the modelled food web
        double grazingLoss = 0.;
        if(ZooplanktonModel2DData.extract(dof).zooconc > 0.)
        grazingLoss = ZooplanktonDat.getGRAZING_LOSS();
        return grazingLoss;
    }
    //Phyto
    private double getPhytoConcentration(DOF dof) {
        PhytoplanktonModel2DData phytoModelData = PhytoplanktonModel2DData.extract(dof);
        double phytoConc;
        if (phytoModelData != null) {       // Ueberpruefen, ob Phytoplanktonmodell vorliegt
            return phytoModelData.phytoconc;
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
//    //Light (Meteorology)
//    private double getSurfaceLightIntensity(DOF dof) {
//        MeteorologyData2D meteorologyData = MeteorologyData2D.extract(dof);
//
//        if (meteorologyData != null) {       // Ueberpruefen, ob meteorologymodell vorliegt
//            return meteorologyData.insolation;
//        } else {                              // sonst initialisieren
////            temperature = 0.;
////            light = 0.;
//            return 0.;//or someother value
//        }
//    }
//    private double getLightIntensity(DOF dof, double waterDepth) {
//        double lightIntensity = 0.;
//        double surfaceIntensity = getSurfaceLightIntensity(dof);
//        double photoActivityCeoff = PhytoplanktonDat.PHOTOSYNTHETIC_ACTIVITY_COEFFICIENT;
//        double lightExtCoeff = PhytoplanktonDat.LIGHT_EXTINCTION_COEFFICIENT;
//        lightIntensity = photoActivityCeoff * surfaceIntensity * Math.exp(-lightExtCoeff * waterDepth);
//        return lightIntensity;
//    }
    
    //------------------------------------------------------------------------
    // setBoundaryCondition
    //------------------------------------------------------------------------
    public void setBoundaryCondition(DOF dof, double t){
        
        int i = dof.number;
        ZooplanktonModel2DData zoomodeldata = ZooplanktonModel2DData.extract(dof);
        
        /* prevention of negative concentration */
        if (x[ZooConc + i] <= 0.)   x[ZooConc + i] = 0.;
        
        if (zoomodeldata.bsc != null){
            x[ZooConc + i]=zoomodeldata.bsc.getValue(t);
            zoomodeldata.dZooConcdt = zoomodeldata.bsc.getDifferential(t);
        }

        if (zoomodeldata.extrapolate){
            ZooplanktonModel2DData tmpdata;
            FElement[] felem = dof.getFElements();
            for(int j=0; j<felem.length;j++) {
                FElement elem=felem[j];
                for(int ll=0;ll<3;ll++){
                    if(elem.getDOF(ll)==dof){
                        for(int ii=1;ii<3;ii++){
                            tmpdata = ZooplanktonModel2DData.extract(elem.getDOF((ll+ii)%3));
                            int jtmp = elem.getDOF((ll+ii)%3).number;
                            if (!tmpdata.extrapolate){
                                x[ZooConc + i] = (9.*x[ZooConc + i] + 1. * x[ZooConc + jtmp])/10.;
                            }
                        }
                    }
                    break;
                }
            }
        }
        
        zoomodeldata.zooconc = x[ZooConc + i];
    }
    
    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    public ModelData genData(DOF dof){
        ZooplanktonModel2DData data = new ZooplanktonModel2DData();
        int dofnumber = (int) dof.number;
        Enumeration b = bsc.elements();
        while (b.hasMoreElements()) {
            BoundaryCondition bcond = (BoundaryCondition) b.nextElement();
            if ( dofnumber == bcond.pointnumber ){
                data.bsc = bcond.function;
                bsc.removeElement(bcond);
            }
        }
        
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
    
    /**
     * the method readBoundCond read the datas for boudary conditions
     * from a file named nam
     * @param nam  name of the file to be open
     */
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

        try {
            FileInputStream is = new FileInputStream(nam);
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
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
                    Point3d pt = (fenet.getDOF(KnotenNr[K]));
                    bsc.addElement(new BoundaryConditionOld(KnotenNr[K], boundcond));
                    //bsc.addElement(new BoundaryCondition(KnotenNr[K], boundcond));
                }
                pointer += anz_identische_Knoten;
                System.out.println("Test2:	Pointer=" + pointer);
            }
            System.out.println("Randwerte gelesen");
        } catch (Exception e) {
            System.exit(1);
        }
    }

    public ModelData genData(FElement felement) {
        System.out.println("noch nicht genutzt");
        return null;
    }    
}
