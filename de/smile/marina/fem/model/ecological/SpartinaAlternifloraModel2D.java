package de.smile.marina.fem.model.ecological;

import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.fem.TicadModel;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.SmileIO;
import de.smile.marina.io.TicadIO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * benthisches Smooth Cordgrass am Delaware
 * @version 2.0
 */
public class SpartinaAlternifloraModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel {
    
    private DataOutputStream xf_os = null;
    private FileOutputStream xf_fs = null;

    //fuer Wachstum ueber das Jahr
    private final static double T1=365.*24.*3600.;   //1 Jahr
    private final static double w1=(2.*3.1415)/T1;   //Kreisfrequenz bestimmen
    private final static double C1=.55;              // Amplitude Planzenhoehe - max Pflanzenhoehe 1.1 m
    private final static double x1=0.55;              // Mittelwert Planzenhoehe
    private final static double C2=.01;              // Amplitude Planzendurchmesser - max. Planzendurchmesser 2 cm
    private final static double x2=0.01;              // Mittelwert Planzendurchmesser
    
    final static double minKst = 5.; // voller Bewuchs
    final static double maxKst = 48.; // kein Bewuchs

    static double maxdensity = 10.; // 10 Pflanzen pro m^2
    static double maxdiameter = x2+C2; //
    static double maxheight = x1+C1; //

    
    /** Creates a new instance of SpartinaAlternifloraModel2D */
    public SpartinaAlternifloraModel2D(FEDecomposition fe, SpartinaAlternifloraModel2DDat spartinaalternifloradat) {
        fenet = fe;
        femodel=this;
        System.out.println("SpartinaAlternifloraModel2D initalization");
        
        setNumberOfThreads(spartinaalternifloradat.numberOfThreads);
                
        // DOFs initialisieren
        initialDOFs();

        if(!Double.isNaN(spartinaalternifloradat.startDensity))
            initial(spartinaalternifloradat.startDensity);

        if (spartinaalternifloradat.startWerteDatei != null) {
            try {
                initialSolutionFromTicadErgFile(spartinaalternifloradat.startWerteDatei, spartinaalternifloradat.startSatz);
            } catch (Exception ex) {
                Logger.getLogger(SpartinaAlternifloraModel2D.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
            if(spartinaalternifloradat.desity_name != null){
                if(spartinaalternifloradat.densityFileType == SmileIO.MeshFileType.SystemDat)
                    initialFromSysDat(spartinaalternifloradat.desity_name);
                else if(spartinaalternifloradat.densityFileType == SmileIO.MeshFileType.JanetBin)
                    initialFromJanetBin(spartinaalternifloradat.desity_name);
            }
        
        try {
            xf_fs = new FileOutputStream(spartinaalternifloradat.xferg_name);
            xf_os = new DataOutputStream(xf_fs);
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ spartinaalternifloradat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }
    
    
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske dichte, pflanzenhoehe, planzendurchmesser, Schein-StricklerWert
        return TicadIO.HRES_H | TicadIO.HRES_SALT| TicadIO.HRES_EDDY | TicadIO.HRES_AH;
    }
    
    /** Read the start solution from file
     * @param spartinaAlternifloraModel2DErgFileName file with simulation results
     * @param record record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    public final void initialSolutionFromTicadErgFile(String spartinaAlternifloraModel2DErgFileName, int record) throws Exception {
    	
        System.out.println("\t Read inital values from result file "+spartinaAlternifloraModel2DErgFileName);
	//erstes Durchscannen
	File sysergFile=new File(spartinaAlternifloraModel2DErgFileName);
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
            final boolean VegetationDensity = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);             
            final boolean VegetationHeight = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);
            final boolean VegetationDiameter = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);    
            final boolean SHEAR_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR); 
            final boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL);
            final boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
            final boolean AH_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);
            
            inStream.readInt();
            
            //Elemente, Rand und Knoten überlesen
            inStream.skip((anzElemente*4+anzr+3*anzKnoten)*4); //4 Bytes je float und int
            
            // bis zum record-Satz springen
            inStream.skip((4+anzKnoten*anzWerte*4)*record);
            
            float time=inStream.readFloat();
            for (int i = 0;i<fenet.getNumberofDOFs();i++){
                DOF dof=fenet.getDOF(i);
                SpartinaAlternifloraModel2DData safmodeldata = SpartinaAlternifloraModel2DData.extract(dof);
                
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

                if (VegetationDensity) {
                    safmodeldata.density = inStream.readFloat();
                    maxdensity = Math.max(maxdensity, safmodeldata.density);
                }

                if (VegetationHeight) {
                    safmodeldata.height = inStream.readFloat();
                }

                if (VegetationDiameter) {
                    safmodeldata.diameter = inStream.readFloat();
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
    }
    
    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof){
        SpartinaAlternifloraModel2DData data = new SpartinaAlternifloraModel2DData();
        return data;
    }
    
    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            for (DOF dof : fenet.getDOFs()) {
                SpartinaAlternifloraModel2DData data = SpartinaAlternifloraModel2DData.extract(dof);
                float height = (float) data.height;
                float diameter = (float) data.diameter;
                if(data.density<1.){
                    height *= data.density;
                    diameter *= data.density;
                }
                xf_os.writeFloat((float)data.density);
                xf_os.writeFloat(height);
                xf_os.writeFloat(diameter);
                double depth = 1.;
                CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
                if(cmd!=null)
                    depth = cmd.totaldepth;
                float strickler = (float)data.getStrickler(depth);
                xf_os.writeFloat(strickler);
            }
            xf_os.flush();
        } catch (Exception e) {}
    }

    private void initial(double startDensity) {
        maxdensity = Math.max(maxdensity, startDensity);
        for (DOF dof : fenet.getDOFs()) {
            SpartinaAlternifloraModel2DData data = SpartinaAlternifloraModel2DData.extract(dof);
            data.density = startDensity;
        }
    }
    
    /** the method readConcentrationDat read the datas for SpartinaAlterniflora
     *  from a sysdat-file named filename
     *  @param nam  name of the file to be open */
    private void initialFromSysDat(String filename) {
        int rand_knoten=0;
        int gebiets_knoten = 0;
        int knoten_nr;
        
        
        double x,y,density;
        
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
            
            // Knoten einlesen
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
                         density=Double.parseDouble(strto.nextToken());
                         
                    } catch(Exception ex) {
                        density=Double.NaN;
                    }
                    
                    if (Double.isNaN(density) || density<0){
                        
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
                    SpartinaAlternifloraModel2DData.extract(dof).density=density;
                    maxdensity = Math.max(maxdensity, density);
                    
                    p_count++;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Konzentration-Datei!");
            System.exit(0);
//            }
        }
    }

    
      /** the method readConcentrationFromJanetBin read the datas for density
     *  from  a JanetBinary-file named filename
     *  @param nam  name of the file to be open */   
    private void initialFromJanetBin(String filename) {
        int anzAttributes=0;
        double x,y,density;
        
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
            // liegt UnTRIM-Gitter mit diskreten Kantentiefen vor??
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
                    density=bin_in.fbinreaddouble();
                    
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(density) || density<=0.)
                    	hasValidValues=false;
                 
                    DOF dof=fenet.getDOF(nr);
                    SpartinaAlternifloraModel2DData.extract(dof).density=density;
                    maxdensity = Math.max(maxdensity, density);
                    
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

    public void timeStep(double dt) {
        this.time += dt;

        for (DOF dof : fenet.getDOFs()) {
            SpartinaAlternifloraModel2DData data = SpartinaAlternifloraModel2DData.extract(dof);
            data.height=C1*Math.cos(w1*time-Math.PI)+x1; // Jahresschwankung der Planzenhoehe
            data.diameter = C2*Math.cos(w1*time-Math.PI)+x2; // Jahresschwankung der Planzendurchmessers
        }
    }
    
    public ModelData genData(FElement felement) {
        System.out.println("noch nicht genutzt");
        return null;
    }

    public void write_erg_xf(double[] erg, double t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBoundaryCondition(DOF dof, double t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double ElementApproximation(FElement ele) {
        return Double.MAX_VALUE;
    }

    @Override
    public double[] getRateofChange(double time, double[] x) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
