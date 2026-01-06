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
package de.smile.marina.fem.model.hydrodynamic.dim3;

import de.smile.marina.MarinaXML;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.FTriangleMesh;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.model.ground.BathymetryData2D;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentDat;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.marina.fem.model.hydrodynamic.dim2.QSteuerung;
import de.smile.marina.fem.model.hydrodynamic.dim2.SedimentElementData;
import de.smile.marina.fem.model.hydrodynamic.dim2.SedimentModel2DData;
import de.smile.marina.fem.model.hydrodynamic.dim2.SurfaceWaterModel;
import de.smile.marina.fem.model.hydrodynamic.dim2.WaveHYPModel2DData;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._du2dz2;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._dudt;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._dudz;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._dv2dz2;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._dvdt;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._dvdz;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._dwdz;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._layerThickness;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._qx;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._qy;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._ru;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._rv;
import de.smile.marina.fem.model.meteorology.MeteorologyData2D;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.SmileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.*;
import java.util.*;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._u;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._v;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._w;
import static de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData._rw;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

/** 3-dimensional current model with stabilized linear triangle finite elements
 * @version 4.6.0
 * @author Peter Milbradt
 */
public class  CurrentModel3D extends SurfaceWaterModel  {

    private CurrentModel3DData[] dof_data=null;
    
    private DataOutputStream xf_os = null;
    
    private DataOutputStream os = null;
    private FileOutputStream fs = null;
    
    public CurrentDat currentdat;
    
    double[] tiefenverteilung;
    private boolean initialH=false;
    
    
    // ----------------------------------------------------------------------
    // CurrentModel3D
    // ----------------------------------------------------------------------
    public CurrentModel3D(FEDecomposition fe, CurrentDat _currentdata, double[] tiefen) {
        System.out.println("CurrentModel3D initalization");
        fenet = fe;
        femodel=this;
        currentdat = _currentdata;
        this.tiefenverteilung=tiefen;
        setNumberOfThreads(currentdat.NumberOfThreads);


        WATT = Function.max(0.01,currentdat.watt);  // verhindert das jemand als Wattgrenze 0 angibt
        halfWATT = WATT / 2.;
        infiltrationRate = currentdat.infiltrationRate;
        
        dof_data= new CurrentModel3DData[fenet.getNumberofDOFs()];
        
        readBoundCond(currentdat.rndwerteReader);
        
        bh.forEach((bcond) -> {
            inith.add(fenet.getDOF(bcond.pointnumber));
        });
        
        // DOFs initialisieren
        initialDOFs();

        initialElementModelData();
        
        generateClosedBoundCond();

        // Rauhigkeiten lesen
        if (currentdat.bottomFriction == CurrentDat.BottomFriction.ManningStrickler) {
            nikuradse = false;
            // read rauhigkeitsmodell // strickler-dat
            if (currentdat.strickler_name != null) {
                if (currentdat.stricklerFileType == SmileIO.MeshFileType.SystemDat) {
                    readStricklerCoeff(currentdat.strickler_name);
                }
                if (currentdat.stricklerFileType == SmileIO.MeshFileType.JanetBin) {
                    readStricklerCoeffFromJanetBin(currentdat.strickler_name);
                }
            }
        }
        if (currentdat.bottomFriction == CurrentDat.BottomFriction.Nikuradse) {
            nikuradse = true;
            // read rauhigkeitsmodell // nikuradse-dat
            if (currentdat.nikuradse_name != null) {
                if (currentdat.nikuradseFileType == SmileIO.MeshFileType.SystemDat) {
                    readNikuradseCoeff(currentdat.nikuradse_name);
                }
                if (currentdat.nikuradseFileType == SmileIO.MeshFileType.JanetBin) {
                    readNikuradseCoeffFromJanetBin(currentdat.nikuradse_name);
                }
            }
        }
// Ende Rauhigkeiten lesen
        
        try {
            xf_os = new DataOutputStream(new FileOutputStream(currentdat.xferg_name));
            TicadIO.write_xf(xf_os, this);
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ currentdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
        }
        
        try {
            fs = new FileOutputStream(currentdat.current3d_erg_name);
            os = new DataOutputStream(fs);
            write_sys(os,(FTriangleMesh)fenet);
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ currentdat.current3d_erg_name + " cannot be opened");
            System.out.println(e.getMessage());
            System.exit(1);
        }
        
    }
    
    public double[] constantInitialWaterLevel(double initalWaterLevel) {
        System.out.println("\tSet constant inital waterlevel "+initalWaterLevel);
        
        for (int i=0; i<fenet.getNumberofDOFs(); i++){
            
            DOF dof= fenet.getDOF(i);
            CurrentModel3DData cmd = dof_data[i];
            SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
            if(sedimentmodeldata == null)
                cmd.setBottomLevel(dof.z);
            else
                cmd.setBottomLevel(sedimentmodeldata.z);
            
            cmd.setWaterLevel_synchronized(initalWaterLevel);
            
        }
        return null;
    }
    
    public double[] initialHfromSysDat(String systemDatPath, double time) {
        this.time=time;

        try {
            InputStream is = new FileInputStream(systemDatPath);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                StreamTokenizer st = new StreamTokenizer(r);
                st.eolIsSignificant(true);
                st.commentChar('C');
                final int anzr = TicadIO.NextInt(st);
                final int anzi = TicadIO.NextInt(st);
                final int anzk = anzr + anzi;
                double value;
                if(anzk==fenet.getNumberofDOFs()){
                    System.out.println("\tRead initial waterlevel (in Ticad-SysDat-Format) from "+systemDatPath);
                    for(int j = 0;j< anzk; j++) {
                        int  nr = TicadIO.NextInt(st);
                        TicadIO.NextDouble(st);
                        TicadIO.NextDouble(st);
                        value = TicadIO.NextDouble(st); // -> eta

                        DOF dof= fenet.getDOF(nr);
                        
                        double depth;
                        SedimentModel2DData sedimentmodeldata=SedimentModel2DData.extract(dof);
                        if (sedimentmodeldata == null)
                            depth = dof.z;
                        else
                            depth = sedimentmodeldata.z;
               
                        CurrentModel3DData cmd = dof_data[nr];
                        cmd.z = depth;		

                        if ((depth + value) <= 0.)
                            cmd.eta = -depth;
                        else
                            cmd.eta = value;
                    
                        cmd.totaldepth = cmd.z + cmd.eta;

                    }
                    initialH=true;
                } else System.out.println("\t different number of nodes");
            }
        } catch (IOException e) {
            System.out.println("!! cannot open file: "+systemDatPath+" !!");
            System.exit(1);
        }
        
        inith=null;
        
        return null;
    }
    @SuppressWarnings("unused")
    public double[] initialHfromJanetBin(String filename, double time) {
        this.time=time;
        
        int anzAttributes=0;
        double value;
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
            
            System.out.println("\tRead Waterlevel (in Janet-Binary-Format):  "+filename);
            
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
                    value=bin_in.fbinreaddouble();
                    
                    DOF dof= fenet.getDOF(nr);
                    double depth;
                    SedimentModel2DData sedimentmodeldata=SedimentModel2DData.extract(dof);
                    if (sedimentmodeldata == null)
                        depth = dof.z;
                    else
                        depth = sedimentmodeldata.z;
           
                    CurrentModel3DData cmd = dof_data[nr];
                    cmd.z = depth;

                    if ((depth + value) < WATT)
                        cmd.eta = -depth;
                    else
                        cmd.eta = value;
                    
                    cmd.totaldepth = cmd.z + cmd.eta;
                    
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                    
                }
                initialH=true;
            } else System.out.println("system and waterlevel.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
            System.exit(1);
        }
        
        inith=null;
        
        return null;
    }
    
    // ----------------------------------------------------------------------
    // initialSolution
    // ----------------------------------------------------------------------
    public double[] initialSolution(double time){
        
        System.out.println("CurrentModel3D - Werte Initialisieren");
        for (int i=0;i<fenet.getNumberofDOFs();i++) {
            
            DOF dof= fenet.getDOF(i);
            CurrentModel3DData cmd = dof_data[dof.number];
            SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
            
            if(!initialH) cmd.setWaterLevel(initialH(dof,time));
            
            if (sedimentmodeldata != null) 
                cmd.setBottomLevel(sedimentmodeldata.z);
            else
                cmd.setBottomLevel(dof.z);
        }
        inith=null;
        
        return null;
    }
    
    // ----------------------------------------------------------------------
    // initialH
    // ----------------------------------------------------------------------
    private double initialH(DOF dof, double time) {
        double h = 0., R = 0., d;
        CurrentModel3DData currentdata = dof_data[dof.number];
        if (currentdata.bh != null) {
            h = currentdata.bh.getValue(time);
        } else {
            for (DOF ndof : inith) {
                CurrentModel3DData current = dof_data[ndof.number];
                if ((dof != ndof) && (current.bh != null)) {
                    d = dof.distance(ndof);
                    h += current.bh.getValue(time) / d;
                    R += 1. / d;
                }
            }
            if (R != 0.) {
                h /= R;
            } else {
                h = 0.;
            }
        }
        return h;
    }
    
    /** Read the start solution from file
     * @param currentergPath file with simulation results
     * @param record record in the file
     * @param sysDatZ is true if the z-Value of the Net is using else the z-Value stored in the result file is used
     * @return the vector of start solution = null, if depricated interface
     */
    @SuppressWarnings("unused")
    public double[] initialSolutionfromErgFile(String currentergPath, int record, boolean sysDatZ) {

        System.out.println("\tRead inital values from Current3DErg-result file " + currentergPath);
        File ergFile = new File(currentergPath);
        try (FileInputStream stream = new FileInputStream(ergFile); DataInputStream inStream = new DataInputStream(stream)) {

            // Kommentar lesen, bis ASCII-Zeichen 7 kommt
            int c;
            do {
                c = inStream.readByte();
            } while (c != 7);
            // Ende Kommentar

            int anzKnoten = inStream.readInt();
            if (fenet.getNumberofDOFs() != anzKnoten) {
                System.out.println("\t " + anzKnoten);
                System.out.println("\t Die Datei mit den Startwerten hat andere Anzahl von Knoten");
                System.exit(1);
            }
            int anzElemente = inStream.readInt();

            int ansaetze = inStream.readInt();

            //Elemente, Rand und Knoten Ueberlesen
            inStream.skip((anzElemente * 4 + 3 * anzKnoten) * 4); //4 Bytes je float und int    

            // bis zum record-Satz springen
            for (int i = 0; i < record; i++) {
                inStream.skip(4); // time
                for (int k = 0; k < anzKnoten; k++) {
                    inStream.skip(4); // z
                    inStream.skip(4); // eta
                    inStream.skip(3*4); // tau
                    int anzSch = inStream.readInt();
                    inStream.skip(5 * anzSch * 4); // werte
                }
            }

            inStream.skip(4); // time
            for (int k = 0; k < anzKnoten; k++) {
                CurrentModel3DData cmd = dof_data[k];
                
                if(sysDatZ){
                    inStream.skip(4);
                    cmd.setBottomLevel(fenet.getDOF(k).z);
                }else
                    cmd.setBottomLevel(inStream.readFloat()); // z
                
                cmd.setWaterLevel(inStream.readFloat()); // eta
                inStream.skip(3*4); // tau
                DOF dof = fenet.getDOF(k);
                SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);

                if (sedimentmodeldata != null) {
                    cmd.setBottomLevel(sedimentmodeldata.z);
                } else {
                    cmd.setBottomLevel(dof.z);
                }

                int anzSch = inStream.readInt();
                if (anzSch != cmd.f.getSizeOfValues()) {
                    System.out.println("\t Die Datei mit den Startwerten hat andere Anzahl von Schichten");
                    System.exit(1);
                }
                for (int s = 0; s < anzSch; s++) {
                    double hg = inStream.readFloat();
                    cmd.f.setValueAt(_u, s, inStream.readFloat());
                    cmd.f.setValueAt(_v, s, inStream.readFloat());
                    cmd.f.setValueAt(_w, s, inStream.readFloat());
                    inStream.readFloat(); // hydrostatischen Druck ueberlesen
                }
            }
        }catch(IOException e){
            System.out.println("!! cannot open file: "+currentergPath+" !!");
            System.exit(1);
        }

        inith = null;

        return null;
    }
    
    /** Read the start solution from TicadSyserg-file from depth integrated hydrodynamic values
     * @param currentergPath file with simulation results
     * @param record record in the file
     * @param sysDatZ is true if the z-Value of the Net is using else the z-Value stored in the result file is used
     * @return the vector of start solution
     */
    @SuppressWarnings("unused")
    public double[] initialSolutionFromTicadErgFile(String currentergPath, int record, boolean sysDatZ) {

        double u_mean,v_mean;
        
        System.out.println("\tRead inital values from TicadSysErg-result file "+currentergPath+" (Current2D)"+" result record: "+record);
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
            
            //Ueberlesen folgende Zeilen
            inStream.skip(9*4);
            
            //Ergebnismaske lesen und auswerten
            int ergMaske = inStream.readInt();
            int anzWerte=TicadIO.ergMaskeAuswerten(ergMaske);
            
            inStream.readInt();
            
            //Elemente, Rand und Knoten Ueberlesen
            inStream.skip((anzElemente*4L+anzr+3L*anzKnoten)*4L); //4 Bytes je float und int    

            // bis zum record-Satz springen
            inStream.skip((4L+anzKnoten*anzWerte*4L)*record);
            
            float t = inStream.readFloat(); // read time stamp
            for (int i = 0;i<fenet.getNumberofDOFs();i++){
                
                CurrentModel3DData cmd = dof_data[i];
                
                if((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE)
                    inStream.skip(4);
                
                if((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS)
                    inStream.skip(4);
                
                if((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z)
                    if(sysDatZ){
                        inStream.skip(4);
                        dof_data[i].z = fenet.getDOF(i).z;
                    }else
                        cmd.z = inStream.readFloat();
                
                if((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V){
                    u_mean = inStream.readFloat();
                    v_mean = inStream.readFloat();
                    for(int s=0; s<cmd.f.getSizeOfValues();s++) {
                        cmd.f.setValueAt(_u,s,u_mean);
                        cmd.f.setValueAt(_v,s,v_mean);
                    }
                }    
                if((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q)
                    inStream.skip(8);
                
                if ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H) {
                    cmd.setWaterLevel(inStream.readFloat());
                }
                
                if((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT)
                    inStream.skip(4);
                
                if((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY)
                    inStream.skip(4);
                
                if((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR)
                    inStream.skip(8);
                
                if((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL)
                    inStream.skip(4);
                
                if((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL)
                    inStream.skip(4);
                
                if((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH)
                    inStream.skip(4);	  
            }   
        } catch(IOException e){
            e.printStackTrace();
            System.out.println("!! cannot open/read file: "+currentergPath+" result record: "+record+"  !!");
            System.exit(1);
        }
        
        inith=null;
         // Geschwindigkeiten oberhalb des Wasserspiegels und unterhalb des Bodens sowie bei trockenen Knoten auf 0 setzen
        for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
            CurrentModel3DData cmd = dof_data[i];

            if (cmd.totaldepth < WATT / 10.) { // Knoten trocken
                for (int s = 0; s < cmd.f.getSizeOfValues(); s++) {
                    cmd.f.setValueAt(_u, s, 0.);
                    cmd.f.setValueAt(_v, s, 0.);
                    cmd.f.setValueAt(_rw, s, 0.);
                    cmd.f.setValueAt(_w, s, 0.); // Tiefenkoordinaten
                    cmd.f.setValueAt(_qx, s, 0.);
                    cmd.f.setValueAt(_qy, s, 0.);
                }
            } else {
                for (int s = 0; s < cmd.f.getSizeOfValues(); s++) {
                        if (cmd.z <= cmd.f.getxAt(s)) { // Geschwingikeiten unterhalb des Bodens auf 0 setzen
                        cmd.f.setValueAt(_u, s, 0.);
                        cmd.f.setValueAt(_v, s, 0.);
                        cmd.f.setValueAt(_rw, s, 0.);
                        cmd.f.setValueAt(_w, s, 0);
                        cmd.f.setValueAt(_qx, s, 0.);
                        cmd.f.setValueAt(_qy, s, 0.);
                    }
                }
            }
        }        
        
        return null;
    }
    
    
    @Override
    @Deprecated
    public double[] getRateofChange(double time, double x[]) {
        return null;
    }
    
    // ----------------------------------------------------------------------
    // ElementApproximation
    // ----------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element) {
        
        double courant = Double.MAX_VALUE;
        
        final FTriangle ele = (FTriangle) element;

        int iwatt = 0;
        int dry = 0;
        int schichten=0;
        
        for ( int j = 0; j < 3; j++) {
            DOF dof = ele.getDOF(j);
            CurrentModel3DData current = dof_data[dof.number];
            
            schichten=Math.max(schichten,current.f.getSizeOfValues());
            
            if (current.totaldepth < WATT) {
                iwatt++;
                if (current.totaldepth < halfWATT) {
                    dry++;
                }
            }
        }

        if (dry == 3) { // element is totaly dry
            for (int j = 0; j < 3; j++) {
                int i = ele.getDOF(j).number;
                final double w1_lambda = 1.-dof_data[i].totaldepth/halfWATT;
                for (int s = schichten - 1; s >= 0; s--) {
                    dof_data[i].f.setValueAt(_rw, s, dof_data[i].f.getValueAt(_rw, s) + (1.E-6+infiltrationRate) * w1_lambda); // Tiefenkoordinatensystem
                }
            }
        } else {
            
            // caculate Bottomslope
            final double bottomslope;
            final SedimentElementData eleSedimentData = SedimentElementData.extract(ele);
            if (eleSedimentData != null) {
                bottomslope = eleSedimentData.bottomslope;
            } else {
                bottomslope = ele.bottomslope;
            }
            
            final double[][] koeffmat = ele.getkoeffmat();
        
            double kst_mean = 0.;
            
            double dhdx = 0.;
            double dhdy = 0.;

            // Wave relevant parameter
            double dsxxdx = 0.;
            double dsxydx = 0.;
            double dsxydy = 0.;
            double dsyydy = 0.;
            double wavebreaking = 0.;
            int nClosedBoundary = 0;

            for (int j = 0; j < 3; j++) {
                DOF dof = ele.getDOF(j);
                CurrentModel3DData current = dof_data[dof.number];
                
                if(current.closedBoundary) nClosedBoundary++;
           
                dhdx += current.eta * koeffmat[j][1];
                dhdy += current.eta * koeffmat[j][2];
                
                kst_mean += current.kst/3.;
                
                WaveHYPModel2DData wave = WaveHYPModel2DData.extract(dof);
                if (wave != null) {
                    dsxxdx += wave.sxx * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wave.wa * koeffmat[j][1];
                    dsxydx += wave.sxy * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wave.wa * koeffmat[j][1];
                    dsxydy += wave.sxy * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wave.wa * koeffmat[j][2];
                    dsyydy += wave.syy * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wave.wa * koeffmat[j][2];
                    if (j == 0) {
                        wavebreaking = wave.epsilon_b;
                    } else {
                        wavebreaking = Math.max(wavebreaking, wave.epsilon_b);
                    }
                }
            }

            double flood = 1.;
            // Pressure gradient reduce with dry falling
            if (iwatt != 0) {
                
                flood = 0.;

                dhdx = 0.;
                dhdy = 0.;

                switch (iwatt) {
                    case 1:
                        for (int j = 0; j < 3; j++) {
                            final int jg = ele.getDOF(j).number;
                            if (dof_data[jg].totaldepth >= WATT) {
                                dhdx += dof_data[jg].eta * koeffmat[j][1];
                                dhdy += dof_data[jg].eta * koeffmat[j][2];
                            } else {
                                final int jg_1 = ele.getDOF((j + 1) % 3).number;
                                final int jg_2 = ele.getDOF((j + 2) % 3).number;
                                if ((dof_data[jg].eta < dof_data[jg_1].eta) || (dof_data[jg].eta < dof_data[jg_2].eta)) {
                                    dhdx += dof_data[jg].eta * koeffmat[j][1];
                                    dhdy += dof_data[jg].eta * koeffmat[j][2];
                                    flood = Function.min(1.,Function.max(dof_data[jg_1].eta-dof_data[jg].eta,dof_data[jg_2].eta-dof_data[jg].eta)/WATT);
                                } else {
                                    dhdx += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].eta + dof_data[jg_2].eta) + dof_data[jg].wlambda * dof_data[jg].eta) * koeffmat[j][1];
                                    dhdy += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].eta + dof_data[jg_2].eta) + dof_data[jg].wlambda * dof_data[jg].eta) * koeffmat[j][2];
                                }
                            }
                        }
                        break;

                    case 2:
                        for (int j = 0; j < 3; j++) {
                            int jg = ele.getDOF(j).number;
                            if (dof_data[jg].totaldepth >= WATT) {
                                dhdx += dof_data[jg].eta * koeffmat[j][1];
                                dhdy += dof_data[jg].eta * koeffmat[j][2];

                                int jg_1 = ele.getDOF((j + 1) % 3).number;
                                int jg_2 = ele.getDOF((j + 2) % 3).number;

                                if (dof_data[jg].eta > dof_data[jg_1].eta) {
                                    dhdx += dof_data[jg_1].eta * koeffmat[(j + 1) % 3][1];
                                    dhdy += dof_data[jg_1].eta * koeffmat[(j + 1) % 3][2];
                                    flood = Function.min(1.,(dof_data[jg].eta-dof_data[jg_1].eta)/WATT);
                                } else {
                                    dhdx += (dof_data[jg_1].w1_lambda * dof_data[jg].eta + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][1];
                                    dhdy += (dof_data[jg_1].w1_lambda * dof_data[jg].eta + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][2];
                                }

                                if (dof_data[jg].eta > dof_data[jg_2].eta) {
                                    dhdx += dof_data[jg_2].eta * koeffmat[(j + 2) % 3][1];
                                    dhdy += dof_data[jg_2].eta * koeffmat[(j + 2) % 3][2];
                                    flood = Function.min(1.,Function.max(flood,(dof_data[jg].eta-dof_data[jg_2].eta)/WATT));
                                } else {
                                    dhdx += (dof_data[jg_2].w1_lambda * dof_data[jg].eta + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][1];
                                    dhdy += (dof_data[jg_2].w1_lambda * dof_data[jg].eta + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][2];
                                }
                            }
                        }
                        break;

                    case 3:
                        double dmax = dof_data[ele.getDOF(0).number].totaldepth;
                        int j = 0;
                        if (dof_data[ele.getDOF(1).number].totaldepth > dmax) { j = 1; dmax = dof_data[ele.getDOF(1).number].totaldepth;}
                        if (dof_data[ele.getDOF(2).number].totaldepth > dmax) { j = 2; /* dmax = dof_data[ele.getDOF(2).number].totaldepth; **unnoetig** */}

                        int jg = ele.getDOF(j).number;
                        dhdx += dof_data[jg].eta * koeffmat[j][1];
                        dhdy += dof_data[jg].eta * koeffmat[j][2];

                        int jg_1 = ele.getDOF((j + 1) % 3).number;
                        int jg_2 = ele.getDOF((j + 2) % 3).number;

                        if (dof_data[jg].eta >= dof_data[jg_1].eta) {
                            dhdx += (dof_data[jg].wlambda * dof_data[jg_1].eta + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].eta + dof_data[jg_1].wlambda * dof_data[jg_1].eta)) * koeffmat[(j + 1) % 3][1];
                            dhdy += (dof_data[jg].wlambda * dof_data[jg_1].eta + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].eta + dof_data[jg_1].wlambda * dof_data[jg_1].eta)) * koeffmat[(j + 1) % 3][2];
                            flood = Function.min(1.,dof_data[jg].wlambda*(dof_data[jg].eta-dof_data[jg_1].eta)/WATT);
                        } else {
                            dhdx += (dof_data[jg_1].w1_lambda * dof_data[jg].eta + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][1];
                            dhdy += (dof_data[jg_1].w1_lambda * dof_data[jg].eta + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][2];
                        }

                        if (dof_data[jg].eta >= dof_data[jg_2].eta) {
                            dhdx += (dof_data[jg].wlambda * dof_data[jg_2].eta + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].eta + dof_data[jg_2].wlambda * dof_data[jg_2].eta)) * koeffmat[(j + 2) % 3][1];
                            dhdy += (dof_data[jg].wlambda * dof_data[jg_2].eta + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].eta + dof_data[jg_2].wlambda * dof_data[jg_2].eta)) * koeffmat[(j + 2) % 3][2];
                            flood = Function.min(1.,Function.max(flood,dof_data[jg].wlambda*(dof_data[jg].eta-dof_data[jg_2].eta)/WATT));
                        } else {
                            dhdx += (dof_data[jg_2].w1_lambda * dof_data[jg].eta + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][1];
                            dhdy += (dof_data[jg_2].w1_lambda * dof_data[jg].eta + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][2];
                        }
                        break;
                }

                dsxxdx = 0.;
                dsxydx = 0.;
                dsxydy = 0.;
                dsyydy = 0.;
                
            }else      
                if (nClosedBoundary>0) {
                    dsxxdx = 0.;
                    dsxydx = 0.;
                    dsxydy = 0.;
                    dsyydy = 0.;
                }
            
        for (int j = 0; j < 3; j++) {
            CurrentModel3DData current = dof_data[ele.getDOF(j).number];
            // puddledetection
            double puddleLambda = current.puddleLambda;
            for (int l = 0; l < 3; l++) {
                // puddledetection
                if(l!=j){
                    final double wlambda_l = (flood > dof_data[ele.getDOF(l).number].wlambda) ? flood : dof_data[ele.getDOF(l).number].wlambda; // max(flood, cmd.wlambda);
                    puddleLambda = ((puddleLambda < wlambda_l) ? wlambda_l : puddleLambda);
                }
            }
            synchronized (current) {
                current.puddleLambda = ((current.puddleLambda < puddleLambda) ? puddleLambda : current.puddleLambda);
            }
        }

// zwischenspeichern der Elementresiduen je Schicht um fuer die Korrektur von w den Wert ueber die Tiefe zu bekommen
        final double[] resImpulsX_mean = new double[schichten];
        final double[] resImpulsY_mean = new double[schichten];
        final double[] schichtdicke_mean = new double[schichten];

        final double[] u = new double[3];
        final double[] v = new double[3];
        final double[] w = new double[3];
        final double[] aktuelletiefe = new double[3];

        final double[] terms_u = new double[3];
        final double[] terms_v = new double[3];
            
        // compute element derivations per layer
        for(int s=schichten-1;s>=0;s--) {

            double udx = 0.;
            double udy = 0.;
            double vdx = 0.;
            double vdy = 0.;
            double qxdx = 0.;
            double qydy = 0.;
            double u_mean=0.;
            double v_mean=0.;
            double uI_mean = 0.;
            double vI_mean = 0.;
            
            double aktuelletiefemean=0.;

            int unterBoden = 0;
            int ueberWasser=0;

            double elementsize2D = ele.maxEdgeLength;
            boolean indicator2D = false;
            for ( int j = 0; j < 3; j++) {
                DOF dof = ele.getDOF(j);
                CurrentModel3DData current = dof_data[dof.number];
                 
                u[j]=current.f.getValueAt(_u,s);
                v[j]=current.f.getValueAt(_v,s);
                w[j]=current.f.getValueAt(_w,s);
// oberhalb der Wasseroberflaeche 
                if (s + 1< current.f.getSizeOfValues() && current.eta < (-current.f.getxAt(s)) && current.eta <= (-current.f.getxAt(s + 1))) {
                    ueberWasser++;
                }
                // Lage der Schichtobergrenze
                final double dh = Math.max(-current.eta, current.f.getxAt(s));
                
                if(dh>current.z) unterBoden++;
                
                schichtdicke_mean[s]+= current.f.getValueAt(_layerThickness, s)/3.;
                aktuelletiefe[j] = Function.max(0,current.z-dh);
                aktuelletiefemean += aktuelletiefe[j]/3.;
                
                u_mean+=u[j]/3.;
                v_mean+=v[j]/3.;
                
                qxdx += current.f.getValueAt(_qx,s) * koeffmat[j][1];
                qydy += current.f.getValueAt(_qy,s) * koeffmat[j][2];
                
                final double uI = current.f.getValueAt(_qx,s) / ((aktuelletiefe[j] < WATT) ? WATT : aktuelletiefe[j]);
                final double vI = current.f.getValueAt(_qy,s) / ((aktuelletiefe[j] < WATT) ? WATT : aktuelletiefe[j]);
                uI_mean += uI/3.;
                vI_mean += vI/3.;
                
                if (Function.norm(uI,vI) > WATT / 10.) {
                    elementsize2D = Function.min(ele.getVectorSize(uI, vI), elementsize2D);
                    indicator2D=true;
                }  
                
                udx += u[j] * koeffmat[j][1];
                udy += u[j] * koeffmat[j][2];
                vdx += v[j] * koeffmat[j][1];
                vdy += v[j] * koeffmat[j][2];
                
            }
            
if(unterBoden<3 && ueberWasser<3){ // mindestens ein Knoten der Schicht liegt oberhalb des Gewaesserbodens und einer im Bereich der Wasseroberflaeche
            
            if(!indicator2D) elementsize2D = ele.minHight;
            
            final double nonZeroMeanDepth = (aktuelletiefemean < WATT) ? WATT : aktuelletiefemean;
            
            double c0 = sqrt(PhysicalParameters.G * nonZeroMeanDepth); // the shallow water wave velocity
            // Formulierung mit dem betragsgroeszten Eigenwert fuer JEDE Richtungskomponente und dann wiederum als euklidische Vektornorm
            final double operatornorm1 = c0 + abs(uI_mean);
            final double operatornorm2 = c0 + abs(vI_mean);
            // Satbilisierungsparameter fuer Konti-Gleichung
            double operatornorm = Math.sqrt(operatornorm1*operatornorm1 + operatornorm2*operatornorm2);
            final double tau_cur2D = 0.5 * elementsize2D / operatornorm ;
            courant=Function.min(courant, tau_cur2D);

            final double cv_mean = Function.norm(u_mean, v_mean);
            
            double elementsize = ele.maxEdgeLength;
            boolean indicator = false;
            if(Function.norm(u[0], v[0]) > WATT/10.){
                elementsize = Function.min(ele.getVectorSize(u[0],v[0]),elementsize);
                indicator=true;
            }
            if(Function.norm(u[1], v[1]) > WATT/10.){
                elementsize = Function.min(ele.getVectorSize(u[1],v[1]),elementsize);
                indicator=true;
            }
            if(Function.norm(u[2], v[2]) > WATT/10.){
                elementsize = Function.min(ele.getVectorSize(u[2],v[2]),elementsize);
                indicator=true;
            }
            if(!indicator) elementsize = ele.minHight;
            
            //eddy viscosity
            //-----------------------------------------
            // konstant
            double astx = PhysicalParameters.DYNVISCOSITY_WATER;
            // Smagorinsky-Ansatz
            astx += (astx*elementsize)*(astx*elementsize)*Math.sqrt(2.*udx*udx+(udy+vdx)*(udy+vdx)+2.*vdy*vdy);
            // Elder - Anteil mit Strickler Bodenschubspannung approximiert
            final double elderCoeff = PhysicalParameters.DYNVISCOSITY_WATER * PhysicalParameters.sqrtG / kst_mean * schichtdicke_mean[s]/nonZeroMeanDepth;
            astx += 0.6 * bottomslope * elderCoeff * cv_mean;
            double asty = astx;

            double impuls1_mean = 0.;
            double impuls2_mean = 0.;
            double contieq_mean = 0.;

            c0 = sqrt(PhysicalParameters.G * nonZeroMeanDepth); // the shallow water wave velocity
            // Formulierung mit dem betragsgroeszten Eigenwert fuer JEDE Richtungskomponente und dann wiederum als euklidische Vektornorm
            final double operatornorm_x = c0 + abs(u_mean);
            final double operatornorm_y = c0 + abs(v_mean);
            operatornorm= Math.sqrt(operatornorm_x*operatornorm_x + operatornorm_y*operatornorm_x);
            // Stabilisierungsparameter fuer Impuls der Schichten
            double tau_cur = 0.5 * elementsize / operatornorm;

            courant=Function.min(courant, tau_cur);

            // Elementfehler berechnen
            // -----------------------
            for (int j = 0; j < 3; j++) {
                final CurrentModel3DData cmd = dof_data[ele.getDOF(j).number];
                final double slambda =  min(1., cmd.f.getValueAt(_layerThickness,s) / CurrentModel3D.WATT);
                contieq_mean += 1./3. * (-w[j] + (qxdx + qydy)) * slambda;
            }

            for (int j = 0; j < 3; j++) {
                
                final CurrentModel3DData cmd = dof_data[ele.getDOF(j).number];
                final double wlambda = (flood > cmd.wlambda ? flood : cmd.wlambda);
                final double slambda =  min(1., cmd.f.getValueAt(_layerThickness,s) / CurrentModel3D.WATT);

                terms_u[j] = PhysicalParameters.G * dhdx * wlambda
                                - v[j] * Coriolis
                                + (u[j] * udx + v[j] * udy)
                                + w[j] * cmd.f.getValueAt(_dudz,s)
                                // Reibung
                                + cmd.bfcoeff[s] * bottomslope * u[j] / Function.max(WATT, aktuelletiefe[j])
                                // wind
                                - cmd.tau_windx[s] * cmd.wlambda
                                // Radiationstresses
                                + (dsxxdx + dsxydy) / PhysicalParameters.RHO_WATER / (( cmd.totaldepth < WATT) ? WATT : cmd.totaldepth)  * 2 * aktuelletiefe[j]/(( cmd.totaldepth < WATT) ? WATT : cmd.totaldepth) * cmd.wlambda
                                // turbulence term, weiter unten
//                                + 3. * (koeffmat[j][1] * astx * udx + koeffmat[j][2] * asty * udy) * slambda
                                // KopplungsTerm aus der Herleitung der Formulierung von q -> v
                                + u[j]/Function.max(WATT, aktuelletiefe[j]) * contieq_mean * slambda
                                          ;
                
                terms_v[j] = PhysicalParameters.G * dhdy * wlambda
                                + u[j] * Coriolis
                                + (u[j] * vdx + v[j] * vdy)
                                + w[j] * cmd.f.getValueAt(_dvdz,s)
                                // Reibung
                                + cmd.bfcoeff[s] * bottomslope * v[j] / Function.max(WATT, aktuelletiefe[j])
                                // wind
                                - cmd.tau_windy[s] * cmd.wlambda
                                // Radiationstresses
                                + (dsxydx + dsyydy) / PhysicalParameters.RHO_WATER / (( cmd.totaldepth < WATT) ? WATT : cmd.totaldepth) * 2 * aktuelletiefe[j]/(( cmd.totaldepth < WATT) ? WATT : cmd.totaldepth) * cmd.wlambda
                                // turbulence term, weiter unten
//                                + 3. * (koeffmat[j][1] * astx * vdx + koeffmat[j][2] * asty * vdy) * slambda
                                // KopplungsTerm aus der Herleitung der Formulierung von q -> v
                                + v[j]/Function.max(WATT, aktuelletiefe[j]) * contieq_mean * slambda
                                          ;
                
                impuls1_mean += 1./3. * ( cmd.f.getValueAt(_dudt,s) + terms_u[j] ) * slambda;
                
                impuls2_mean += 1./3. * ( cmd.f.getValueAt(_dvdt,s) + terms_v[j] ) * slambda;

            }
            
            resImpulsX_mean[s]=impuls1_mean;
            resImpulsY_mean[s]=impuls2_mean;
            
            double dimpuls1_mean=0., dimpuls2_mean=0.;
            double tiefesum=0.;
            for(int j=schichten-1;j>=s;j--){
                tiefesum+=schichtdicke_mean[j];
                dimpuls1_mean+=resImpulsX_mean[j]*schichtdicke_mean[j];
                dimpuls2_mean+=resImpulsY_mean[j]*schichtdicke_mean[j];
            }
            if(tiefesum>WATT){
                dimpuls1_mean/=tiefesum;
                dimpuls2_mean/=tiefesum;
            } else {
                dimpuls1_mean=0.;
                dimpuls2_mean=0.;
            }
            
            // eddy viscosity for Z
            double astz = PhysicalParameters.DYNVISCOSITY_WATER;
//          bodennahe Turbulenz wenn ein Knoten Bodenknoten ist UNBEDINGT NOTWENDIG - sonst instabil an steilen Gradienten
            if(unterBoden>0)
                astz += (bottomslope-1.) * Function.norm(u_mean, v_mean);
            // residualbased turbulenz
            astz += abs(contieq_mean);

            for (int j = 0; j < 3; j++) {

                final CurrentModel3DData cmd = dof_data[ele.getDOF(j).number];
                final double wlambda = (flood > cmd.wlambda ? flood : cmd.wlambda);
                final double slambda =  min(1., cmd.f.getValueAt(_layerThickness,s) / CurrentModel3D.WATT);
                
                // Fehlerkorrektur durchfuehren
                double ru = -tau_cur *
                                ( koeffmat[j][1] * u_mean * impuls1_mean
                                + koeffmat[j][1] * PhysicalParameters.G * contieq_mean
                                + koeffmat[j][2] * v_mean * impuls1_mean
                                ) * ele.area;
                // tubulence-term
                ru += astz * cmd.f.getValueAt(_du2dz2,s)/3. * ele.area;
                ru -= (koeffmat[j][1] * astx * udx + koeffmat[j][2] * asty * udy) * slambda * ele.area;
                
                double rv = -tau_cur * 
                                ( koeffmat[j][1] * u_mean * impuls2_mean
                                + koeffmat[j][2] * PhysicalParameters.G * contieq_mean
                                + koeffmat[j][2] * v_mean * impuls2_mean
                                ) * ele.area;
                // tubulence-term
                rv += astz * cmd.f.getValueAt(_dv2dz2,s)/3. * ele.area;
                rv -= (koeffmat[j][1] * astx * vdx + koeffmat[j][2] * asty * vdy) * slambda * ele.area;
                
                double rw = tau_cur2D * 
                                ( koeffmat[j][1] * aktuelletiefemean * dimpuls1_mean * wlambda
                                + koeffmat[j][1] * uI_mean * contieq_mean
                                + koeffmat[j][2] * aktuelletiefemean * dimpuls2_mean * wlambda
                                + koeffmat[j][2] * vI_mean * contieq_mean
                                ) * ele.area;
                
                final double terms_h = -(qxdx+qydy);
                
                for (int l = 0; l < 3; l++) {
                    final double wlambda_l = (flood > dof_data[ele.getDOF(l).number].wlambda ? flood : dof_data[ele.getDOF(l).number].wlambda);
                    final double vorfak =  ele.area * ((l == j) ? 1. / 6. : 1. / 12.);
                    double gl = (l == j) ? 1. : Math.min(wlambda_l,dof_data[ele.getDOF(l).number].f.getValueAt(_layerThickness, s)/Math.max(WATT,cmd.f.getValueAt(_layerThickness, s)));
                    
                    ru -= vorfak * terms_u[l]*gl;
                    rv -= vorfak * terms_v[l]*gl;
                    
                    double glH = (l == j) ? 1. : wlambda_l;
// ToDo muss nicht in jeder Schicht berechnet werden, da s gar nicht enthalten - koennte so wie die Koeffmat umgesetzt werden !!
                    if ((l != j) && (iwatt != 0)) {
                        if (terms_h < 0) { // Wasserstand am abgelegenen Knoten will steigen
                            if (dof_data[ele.getDOF(l).number].eta < cmd.eta) { // Wasserstand am abgelegenen Knoten liegt unterhalb
                                glH = cmd.wlambda * Function.max(0., 1. - (cmd.eta - dof_data[ele.getDOF(l).number].eta) / ele.distance[l][j]);
                            } else { // Wasserstand am abgelegenen Knoten liegt oberhalb
//                                glH = dof_data[ele.getDOF(l).number].wlambda;
                            }
                        } else { // Wasserstand am abgelegenen Knoten will fallen
                            if (dof_data[ele.getDOF(l).number].eta < cmd.eta) { // Wasserstand am abgelegenen Knoten liegt unterhalb
                                glH=1.;
                            } else { // Wasserstand am abgelegenen Knoten liegt oberhalb
                                glH = wlambda * Function.max(0., 1. - (dof_data[ele.getDOF(l).number].eta - cmd.eta) / ele.distance[l][j]);
                            }
                        }
                    }
// ToDo  rw -= vorfak * terms_h*glH[s][l]; mit l=s -> 1
                    // Conti Equation
                    rw -= vorfak * terms_h * glH;
                }
                synchronized (cmd) {
                    cmd.f.setValueAt(_ru,s,cmd.f.getValueAt(_ru,s)+ru);
                    cmd.f.setValueAt(_rv,s,cmd.f.getValueAt(_rv,s)+rv);
                    cmd.f.setValueAt(_rw,s,cmd.f.getValueAt(_rw,s)+rw);
                }
            }
            
}
        }
    }
        // Zeitschritt zurueck geben
        return courant;
    }
    // end ElementApproximation
    
    // ----------------------------------------------------------------------
    // setBoundaryCondition
    // ----------------------------------------------------------------------
    @Override
    public void setBoundaryCondition(DOF dof, double t){
        int i = dof.number;

        CurrentModel3DData cmd = dof_data[i];
        for (int s = 0; s < cmd.f.getSizeOfValues(); s++) {
            cmd.f.setValueAt(_ru, s, 0.);
            cmd.f.setValueAt(_rv, s, 0.);
            cmd.f.setValueAt(_rw, s, 0.);
        }

        cmd.puddleLambda = 0.;
        
        cmd.wattsickern=true;
        
        final SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
        if (sedimentmodeldata == null) {
            final BathymetryData2D bathymetrymodeldata = BathymetryData2D.extract(dof);
            if (bathymetrymodeldata != null) {
                cmd.setBottomLevel(bathymetrymodeldata.z);
            } 
//            else {
//                cmd.setBottomLevel(dof.z); // Peter 20.04.2021 reicht eigentlich beim initialiseren
//            }
//            cmd.dzdt = 0.;
        } else {
            cmd.setBottomLevel(sedimentmodeldata.z);
//            cmd.dzdt = sedimentmodeldata.dzdt; 
        }

        if (cmd.bh != null) {
            cmd.setWaterLevel_synchronized(cmd.bh.getValue(t));
            cmd.detadt = cmd.bh.getDifferential(t);
            for(int s=0;s<cmd.f.getSizeOfValues();s++) cmd.f.setValueAt(_w,s,-cmd.detadt * Math.max(0,cmd.z+Math.min(-cmd.f.getxAt(s),cmd.eta)) /Math.max(WATT, cmd.totaldepth));
//            for (int s = 0; s < cmd.f.getSizeOfValues(); s++) cmd.f.setValueAt(_w, s, 0);
        }
        
        if ((cmd.extrapolate_h || cmd.extrapolate_u || cmd.extrapolate_v) && (cmd.totaldepth > WATT)){
            FElement[] felem = dof.getFElements();
            for (FElement elem : felem) {
                for(int ll=0;ll<3;ll++){
                    if(elem.getDOF(ll)==dof){
                        for(int ii=1;ii<3;ii++){
                            int jtmp = elem.getDOF((ll+ii)%3).number;
                            CurrentModel3DData tmpcdata = dof_data[jtmp];
                            if (tmpcdata.totaldepth > WATT){
                                if (cmd.extrapolate_h) {
                                    double dh = (cmd.eta - tmpcdata.eta) / 10.;
                                    if (tmpcdata.extrapolate_h)
                                        dh /= 10.;
                                    cmd.setWaterLevel_synchronized(cmd.eta - dh);
                                    tmpcdata.setWaterLevel_synchronized(tmpcdata.eta + dh);
                                }
                                if(cmd.extrapolate_u){
                                    synchronized (cmd) {
                                        for(int s=0;s<cmd.f.getSizeOfValues();s++) {
                                            double du = (cmd.f.getValueAt(_u,s) - tmpcdata.f.getValueAt(_u,s))/10.;
                                            if (tmpcdata.extrapolate_u)  du /= 10.;
                                            cmd.f.setValueAt(_u,s,cmd.f.getValueAt(_u,s)-du);
                                        }
                                    }
                                }
                                if(cmd.extrapolate_v){
                                    synchronized (cmd) {
                                        for(int s=0;s<cmd.f.getSizeOfValues();s++) {
                                            double dv = (cmd.f.getValueAt(_v,s) - tmpcdata.f.getValueAt(_v,s))/10.;
                                            if (tmpcdata.extrapolate_v)  dv /= 10.;
                                            cmd.f.setValueAt(_v,s,cmd.f.getValueAt(_v,s)-dv);
                                        }
                                    }
                                }
                            } else {
                                if (cmd.extrapolate_h) {
                                    double dh = (cmd.eta - tmpcdata.eta) / 10. * tmpcdata.wlambda;
                                    if (tmpcdata.extrapolate_h) {
                                        dh /= 10.;
                                    }
                                    cmd.setWaterLevel_synchronized(cmd.eta - dh);
                                    if (dh<0.) tmpcdata.setWaterLevel_synchronized(tmpcdata.eta + dh);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        /* extrapolate waterlevel on no exact defined boundary conditions in the case of very small waterdepth*/
        if (cmd.extrapolate_h && cmd.totaldepth < WATT) {
            FElement[] felem = dof.getFElements();
            double minh = cmd.eta;
            for (FElement elem : felem) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            int jtmp = elem.getDOF((ll + ii) % 3).number;
                            CurrentModel3DData tmpcdata = dof_data[jtmp];
                            minh=Function.min(minh,tmpcdata.w1_lambda*minh+tmpcdata.wlambda*tmpcdata.eta);  // Morphen
                        }
                        break;
                    }
                }
            }
            final double dh = (cmd.eta - minh) / 100. * cmd.w1_lambda;
            cmd.setWaterLevel_synchronized(cmd.eta-dh);
        }
        
        // testen ob ein knoten ein Sickerknoten ist
        if (cmd.totaldepth < WATT) {
            FElement[] felem = dof.getFElements();
            for (int j = 0; (j < felem.length) && cmd.wattsickern; j++) {
                FElement elem = felem[j];
                for (int ll = 0; (ll < 3)&& cmd.wattsickern; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        CurrentModel3DData tmpcmd1 = dof_data[elem.getDOF((ll + 1) % 3).number];
                        CurrentModel3DData tmpcmd2 = dof_data[elem.getDOF((ll + 2) % 3).number];
                        cmd.wattsickern &= !(((tmpcmd1.eta > cmd.eta) && (tmpcmd1.totaldepth >= 0.9*WATT)) || ((tmpcmd2.eta > cmd.eta) && (tmpcmd2.totaldepth >= 0.9*WATT)));
                    }
                }
            }
        } else
            cmd.wattsickern = false;

        if (cmd.bQx != null) {
            cmd.bQx.update(dof_data, t); // Methode update ist nun syncronized !

            final double u = cmd.bQx.getValueAt(i);
            for(int s=0;s<cmd.f.getSizeOfValues();s++) {
                cmd.f.setValueAt(_u,s,u);
                cmd.f.setValueAt(_dudt,s,0);
                cmd.f.setValueAt(_w,s,0);
            }
        }
        if (cmd.bQy != null) {
            cmd.bQy.update(dof_data, t); // Methode update ist nun syncronized !

            final double v = cmd.bQy.getValueAt(i);
            for(int s=0;s<cmd.f.getSizeOfValues();s++) {
                cmd.f.setValueAt(_v,s,v);
                cmd.f.setValueAt(_dvdt,s,0);
                cmd.f.setValueAt(_w,s,0);
            }
        }
        
        if (cmd.bu != null) {
            final double u=cmd.bu.getValue(t);
            final double dudt = cmd.bu.getDifferential(t);
            for(int s=0;s<cmd.f.getSizeOfValues();s++) {
                cmd.f.setValueAt(_u,s,u);
                cmd.f.setValueAt(_dudt,s,dudt);
                cmd.f.setValueAt(_w,s,0);
            }
        }
        
        if (cmd.bv != null){
            final double v=cmd.bv.getValue(t);
            final double dvdt = cmd.bv.getDifferential(t);
            for(int s=0;s<cmd.f.getSizeOfValues();s++) {
                cmd.f.setValueAt(_v,s,v);
                cmd.f.setValueAt(_dvdt,s,dvdt);
                cmd.f.setValueAt(_w,s,0);
            }
        }
        
//        double depthdt=0.;
        if (cmd.totaldepth > 5. * WATT) {
            if (cmd.bqx != null) {
                double data_u = cmd.bqx.getValue(t) / cmd.totaldepth;
//                double data_dudt = (cmd.bqx.getDifferential(t) - data_u *(depthdt+cmd.detadt))/data.totaldepth;
                for (int s = 0; s < cmd.f.getSizeOfValues(); s++) {
                    cmd.f.setValueAt(_u, s, data_u);
//                    cmd.f.setValueAt(_dudt,s,data_dudt);
                    cmd.f.setValueAt(_w, s, 0);
                }
            }

            if (cmd.bqy != null) {
                double data_v = cmd.bqy.getValue(t) / cmd.totaldepth;
//                double data_dvdt = (cmd.bqy.getDifferential(t) - data_v *(depthdt+cmd.detadt))/data.totaldepth;
                for (int s = 0; s < cmd.f.getSizeOfValues(); s++) {
                    cmd.f.setValueAt(_v, s, data_v);
//                    cmd.f.setValueAt(_dvdt,s,data_dvdt);
                    cmd.f.setValueAt(_w, s, 0);
                }
            }
        }
        
        /* Wattstrategie fuer Stroemung   */
        cmd.wlambda = Function.min(1., cmd.totaldepth / WATT);
        cmd.w1_lambda = 1. - cmd.wlambda;

        double qx=0, qy=0.;
        double tau_windx = 0., tau_windy = 0.;

        if (cmd.totaldepth < WATT / 10.) { // Knoten trocken

            for (int s = 0; s < cmd.f.getSizeOfValues(); s++) {
                
                cmd.f.setValueAt(_u, s, 0.);
                cmd.f.setValueAt(_v, s, 0.);
                cmd.f.setValueAt(_w, s, 1.E-6); // Tiefenkoordinaten
                cmd.f.setValueAt(_qx, s, 0.);
                cmd.f.setValueAt(_qy, s, 0.);
                cmd.f.setValueAt(_dudz, s, 0);
                cmd.f.setValueAt(_dvdz, s, 0);
                cmd.f.setValueAt(_dwdz, s, 0);
                cmd.f.setValueAt(_du2dz2, s, 0);
                cmd.f.setValueAt(_dv2dz2, s, 0);
                cmd.f.setValueAt(_layerThickness, s, 0);
                
                cmd.tau_windx[s] = 0.;
                cmd.tau_windy[s] = 0.;
                
                cmd.bfcoeff[s] = PhysicalParameters.KINVISCOSITY_WATER;
                
                cmd.tauBx=cmd.tauBy=cmd.tauBz=0.;
            }

        } else { 
           
            /* wind stress koeffizient */
            /* Smith and Banke (1975) */
            if (cmd.totaldepth > WATT) {
                MeteorologyData2D meteorologyData2D = MeteorologyData2D.extract(dof);
                if (meteorologyData2D != null) {
                    double tau_wind = (0.63 + 0.066 * meteorologyData2D.windspeed) * 1.E-3 * PhysicalParameters.RHO_AIR / PhysicalParameters.RHO_WATER;
                    tau_windx = tau_wind * meteorologyData2D.windspeed * meteorologyData2D.windx / cmd.totaldepth;
                    tau_windy = tau_wind * meteorologyData2D.windspeed * meteorologyData2D.windy / cmd.totaldepth;
                }
            }
            
            double u = 0.;
            double v = 0.;
            double w = 0.;
            double sschichten = 0.;
            
            for (int s = cmd.f.getSizeOfValues() - 1; s >= 0; s--) {
                if (cmd.z <= cmd.f.getxAt(s)) {
                    // Geschwingikeiten unterhalb des Bodens auf 0 setzen
                    cmd.f.setValueAt(_u, s, 0.);
                    cmd.f.setValueAt(_v, s, 0.);
                    cmd.f.setValueAt(_w, s, 0.);
                    cmd.f.setValueAt(_qx, s, 0.);
                    cmd.f.setValueAt(_qy, s, 0.);
                    cmd.f.setValueAt(_dudz, s, 0);
                    cmd.f.setValueAt(_dvdz, s, 0);
                    cmd.f.setValueAt(_dwdz, s, 0);
                    cmd.f.setValueAt(_du2dz2, s, 0);
                    cmd.f.setValueAt(_dv2dz2, s, 0);
                    cmd.f.setValueAt(_layerThickness, s, 0);
                    
                    cmd.tau_windx[s] = 0.;
                    cmd.tau_windy[s] = 0.;
                
                    cmd.bfcoeff[s] = PhysicalParameters.KINVISCOSITY_WATER;
                
                } else {
                    
                    final double d;
                    if (s == cmd.f.getSizeOfValues() - 1)
                        d = cmd.z - Math.max(cmd.f.getxAt(s), -cmd.eta);
                    else
                        d = Function.max(0., Function.min(cmd.f.getxAt(s + 1), cmd.z) - Function.max(cmd.f.getxAt(s), -cmd.eta));
                    
                    qx += d * cmd.f.getValueAt(_u, s);
                    qy += d * cmd.f.getValueAt(_v, s);
                    cmd.f.setValueAt(_qx, s, qx);
                    cmd.f.setValueAt(_qy, s, qy);
                    
                    cmd.f.setValueAt(_dudz, s, cmd.f.centralDifference(_u, s));
                    cmd.f.setValueAt(_dvdz, s, cmd.f.centralDifference(_v, s));
                    cmd.f.setValueAt(_dwdz, s, cmd.f.centralDifference(_w, s));
                    cmd.f.setValueAt(_du2dz2, s, cmd.f.secondDifference(_u, s));
                    cmd.f.setValueAt(_dv2dz2, s, cmd.f.secondDifference(_v, s));
                    
                    
                    double dh = cmd.f.getxAt(s);
                    if (-dh > cmd.eta) {
                        dh = -cmd.eta; // Schicht an der Wasseroberflaeche
                    }
                    final double aktuelletiefe = max(0, cmd.z - dh);
                    
                    double schichtdicke;
                    if (s < cmd.f.getSizeOfValues() - 1) {
                        schichtdicke = Math.min(Math.max(0, cmd.f.getxAt(s + 1) - dh), aktuelletiefe);
                    } else {
                        // Unterste Schicht
                        schichtdicke = aktuelletiefe;
                    }
                    cmd.f.setValueAt(_layerThickness, s, schichtdicke);

                    final double uI = qx / ((aktuelletiefe < WATT) ? WATT : aktuelletiefe);
                    final double vI = qy / ((aktuelletiefe < WATT) ? WATT : aktuelletiefe);
                    
                    /* bottom friction coefficient */
                    if (nikuradse){
                        cmd.bfcoeff[s] = (PhysicalParameters.KINVISCOSITY_WATER+PhysicalParameters.G / Function.sqr(Function.max(5.,18.*Math.log10(12.*((aktuelletiefe < 0.1) ? 0.1 : aktuelletiefe)/Function.max(cmd.ks,CurrentModel2DData.Strickler2Nikuradse(cmd.kst))))) * Function.norm(uI,vI)) * Function.pow3(schichtdicke /((aktuelletiefe < WATT) ? WATT : aktuelletiefe)); //Colebrooks / Nikuradse
                    } else{
                        // Strickler
                        cmd.bfcoeff[s] = (PhysicalParameters.KINVISCOSITY_WATER+PhysicalParameters.G / cmd.kst / cmd.kst / Math.cbrt((aktuelletiefe < .1) ? .1 : aktuelletiefe) * Function.norm(uI,vI,cmd.f.getValueAt(_w, s))) * Function.pow4(schichtdicke /((aktuelletiefe < WATT) ? WATT : aktuelletiefe));
                    }
                    /* wind stress koeffizient */
                    if (cmd.totaldepth > WATT) {
                        cmd.tau_windx[s] = tau_windx * 2.* aktuelletiefe / cmd.totaldepth;
                        cmd.tau_windy[s] = tau_windy * 2.* aktuelletiefe / cmd.totaldepth;
                    } else {
                        cmd.tau_windx[s] = 0.;
                        cmd.tau_windy[s] = 0.;
                    }
                    if (sschichten < 1.) {
                        if (sschichten + schichtdicke >= 1.) schichtdicke = 1. - sschichten;
                        sschichten += schichtdicke;
                        u += cmd.f.getValueAt(_u, s) * schichtdicke;
                        v += cmd.f.getValueAt(_v, s) * schichtdicke;
                        w += cmd.f.getValueAt(_w, s) * schichtdicke;
                    }
                }
            }
            if (sschichten > CurrentModel3D.halfWATT) { // Summe der Schichten sollten eigentlich immer <= 1 sein / 1 oder falls kleiner totaldepth
                u /= sschichten;
                v /= sschichten;
                w /= sschichten;
                
                // Mittelwert aus tiefenintegrierter bodennaher Bodenschubspannung und bodennaher - duty trick, da sonst die Bodenschubspannungen zu klein
                final double nonTotalDepth = (cmd.totaldepth < WATT) ? WATT : cmd.totaldepth;
                final double uI = qx/nonTotalDepth;
                final double vI = qy/nonTotalDepth;
                
                final double tau_b = Math.max(PhysicalParameters.DYNVISCOSITY_WATER, PhysicalParameters.G / cmd.kst / cmd.kst / ((sschichten < .1) ? .1 : sschichten)) * Function.norm(u,v,w);
                final double tau_bU = Math.max(PhysicalParameters.DYNVISCOSITY_WATER, PhysicalParameters.G / cmd.kst / cmd.kst / Math.cbrt((cmd.totaldepth < .1) ? .1 : cmd.totaldepth)) * Function.norm(uI,vI);
                cmd.tauBx = PhysicalParameters.RHO_WATER * (tau_b * u + tau_bU*uI)/2.;
                cmd.tauBy = PhysicalParameters.RHO_WATER * (tau_b * v + tau_bU*vI)/2.;
                cmd.tauBz = PhysicalParameters.RHO_WATER * tau_b * w;
            } else {
                cmd.tauBx = 0.;
                cmd.tauBy = 0.;
                cmd.tauBz = 0.;
            }
        }

        cmd.qx = qx;
        cmd.qy = qy;
        
    } // end setBoundaryCondition
    
    // ----------------------------------------------------------------------
    // ToDO
    // ----------------------------------------------------------------------
    @Override
	public ModelData genData(FElement felement){
//            Current3DElementData res = new Current3DElementData();
//            element_data[felement.number] = res;
//            return res;
            return null;
        }

    // ----------------------------------------------------------------------
    // genData
    // ----------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof){
        CurrentModel3DData data = new CurrentModel3DData(dof, this.tiefenverteilung);
        int dofnumber = dof.number;
        dof_data[dofnumber]=data;
        
        for(BoundaryCondition bcond : bqx){
            if (dofnumber == bcond.pointnumber) {
                data.bqx = bcond.function;
                bqx.remove(bcond);
                data.boundary = true;
                break;
            }
        }
        
        for(BoundaryCondition bcond : bqy){
            if (dofnumber == bcond.pointnumber) {
                data.bqy = bcond.function;
                bqy.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for(BoundaryCondition bcond : bu){
            if (dofnumber == bcond.pointnumber) {
                data.bu = bcond.function;
                bu.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for(BoundaryCondition bcond : bv){
            if (dofnumber == bcond.pointnumber) {
                data.bv = bcond.function;
                bv.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for(BoundaryCondition bcond : bh){
            if (dofnumber == bcond.pointnumber) {
                data.bh = bcond.function;
                bh.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for(BoundaryCondition bcond : bQx){
            if (dofnumber == bcond.pointnumber) {
                data.bQx = (QSteuerung) bcond.function;
                bQx.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for(BoundaryCondition bcond : bQy){
            if (dofnumber == bcond.pointnumber) {
                data.bQy = (QSteuerung) bcond.function;
                bQy.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        return data;
    } // end genData    

    /** the method readStricklerCoeff read the datas for strickler coefficients
     *  from a sysdat-file named nam
     *  @param nam  name of the file to be open */
    private void readStricklerCoeff(String filename) {
        int rand_knoten = 0;
        int gebiets_knoten = 0;
        int knoten_nr;


        double kst;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading Bottom-Friction-File (in TiCAD-System.Dat-Format): " + filename);

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));
            java.util.StringTokenizer strto = new StringTokenizer(line, " \t\n\r\f,");
            rand_knoten = Integer.parseInt(strto.nextToken());

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));


            strto = new StringTokenizer(line, " \t\n\r\f,");
            gebiets_knoten = Integer.parseInt(strto.nextToken());

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000) {
                throw new Exception("Fehler");
            }

            //System.out.println(""+rand_knoten+" "+gebiets_knoten);

            // Knoten einlesen
            // DOF[] dof= new DOF[rand_knoten+gebiets_knoten];
            int p_count = 0;
            while (p_count < (rand_knoten + gebiets_knoten)) {
                line = systemfile.freadLine();
                strto = new StringTokenizer(line, " \t\n\r\f,");

                //System.out.println(""+line+"\n");
                if (!line.startsWith("C")) {
                    knoten_nr = Integer.parseInt(strto.nextToken());
                    Double.parseDouble(strto.nextToken()); // read x-coordinate
                    Double.parseDouble(strto.nextToken()); // read y-coordinate
                    try {
                        kst = Double.parseDouble(strto.nextToken());
                    } catch (Exception ex) {
                        kst = Double.NaN;
                    }

                    if (Double.isNaN(kst) || kst < 0) {

                        System.out.println("");

                        System.out.println("********************************       ERROR         ***********************************");
                        System.out.println("Invalid z-value (z=NaN or z<0.0) in Bottom Friction-Mesh: <" + filename + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count + "> has a correct floating point (greater zero)");
                        System.out.println("bottom friction value");
                        System.out.println("*****************************************************************************************");
                        System.out.println("");
                        System.exit(1);
                    }
                    DOF dof = fenet.getDOF(knoten_nr);
                    CurrentModel3DData currentdata =
                            CurrentModel3DData.extract(dof);
                    currentdata.kst = kst;
                    currentdata.ks  = CurrentModel2DData.Strickler2Nikuradse(kst);

                    p_count++;
                }

            }
        } catch (Exception e) {
            System.out.println("cannot open file: " + filename);
            System.exit(1);
        }

        // Anzahl der Elemente
//        FTriangle[] element = (FTriangle[]) fenet.getFElements();
//        double smean = 0.;
//        for (int i = 0; i < element.length; i++) {
//            DOF[] dofs = element[i].getDOFs();
//            smean = 0.;
//            for (int s = 0; s < 3; s++) {
//                smean += CurrentModel2DData.extract(dofs[s]).kst;
//            }
//
//            Current2DElementData.extract(element[i]).meanStricklerCoefficient = smean / 3.;
//        }

    }

    /** the method readStricklerCoeff read the datas for strickler coefficients
     *  from a JanetBinary-file named filename
     *  @param nam  name of the file to be open */
    @SuppressWarnings("unused")
    private void readStricklerCoeffFromJanetBin(String filename) {
        int anzAttributes = 0;
        double kst;
        // NEU WIEBKE 22.02.2007
        boolean hasValidValues = true;
        // ENDE NEU WIEBKE
        int nr;
        short status, kennung;
        int anzPolys, anzEdges, anzPoints = 0, pointsize, trisize, swapMode;
        short sets;
        boolean active, protectBorder, protectConstraints, noPolygon, inPolygon, makeHoles, processFlagsActive;
        boolean noZoom, inZoom, noActive, processActive, processSelected, inPolygonProp, inZoomProp, protectInnerPoints;
        boolean noSelected, closed;
        boolean read_status_byte = false;

        FileIO bin_in = new FileIO();

        try {
            bin_in.fopenbinary(filename, FileIO.input);

            // Netz aus einer Binaerdatei lesen

            // Version auslesen
            float version = bin_in.fbinreadfloat();
            if (version < 1.5f) {
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version + ", current version: 1.8");
            }

            if (version < 1.79) {
                read_status_byte = true;
            }

            System.out.println("\tRead strickler coefficients (in Janet-Binary-Format): " + filename);

            // zunaechst den FileHeader lesen
            boolean writePointNumbers = bin_in.fbinreadboolean();
            boolean writePointAttributes = bin_in.fbinreadboolean();
            anzAttributes = bin_in.fbinreadint();
            boolean writePointStatus = bin_in.fbinreadboolean();
            boolean writeConstraintPolygons = bin_in.fbinreadboolean();
            boolean writeConstraintEdges = bin_in.fbinreadboolean();
            boolean writeElements = bin_in.fbinreadboolean();
            boolean writeElementNumbers = bin_in.fbinreadboolean();
            boolean writeElementKennung = bin_in.fbinreadboolean();
            boolean writeAlphaTestRadius = bin_in.fbinreadboolean();

            // Layertyp ueberlesen
            int filetype = bin_in.fbinreadint();
            // liegt UnTRIM-Gitetr mit diskreten Kantentiefen vor??
            boolean is_untrim = (filetype == 2);

            // Anzahl der Punkte lesen
            int anzk = bin_in.fbinreadint();
            if (anzk == fenet.getNumberofDOFs()) {

                // Punkte lesen
                for (int i = 0; i < anzk; i++) {
                    // Punktnummer lesen
                    if (writePointNumbers) {
                        nr = bin_in.fbinreadint();
                    } else {
                        nr = i;
                    }

                    // x,y,kst lesen
                    bin_in.fbinreaddouble(); // read x-coordinate
                    bin_in.fbinreaddouble(); // read y-coordinate
                    kst = bin_in.fbinreaddouble(); // -> kst
                    // NEU WIEBKE 20.02.2007
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(kst) || kst <= 0.) {
                        hasValidValues = false;
                    }
                    // ENDE NEU WIEBKE 20.02.2007
                    DOF dof = fenet.getDOF(nr);
                    CurrentModel3DData currentdata = CurrentModel3DData.extract(dof);
                    currentdata.kst = kst;
                    currentdata.ks  = CurrentModel2DData.Strickler2Nikuradse(kst);

                    // Status-Flag lesen
                    if (writePointStatus) {
                        bin_in.fbinreadshort();
                    }

                }
                // Abbruch, wenn Netz nicht ok!
                if (!hasValidValues) {
                    System.out.println("***                     WARNUNG                       ***");
                    System.out.println("***   Ueberpruefen Sie die Rauheiten des              ***");
                    System.out.println("***   Rauheitennetzes. Das verwendetet Netz hat       ***");
                    System.out.println("***   Knoten mit negativen oder nicht definierten     ***");
                    System.out.println("***   Rauheiten!                                      ***");
                    System.out.println("***   Die Simulation wird fortgesetzt                 ***");
                //System.exit(0);
                }
            // ENDE NEU WIEBKE 20.02.2007
            } else {
                System.out.println("system und strickler.jbf different number of nodes");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
            System.exit(1);
        }
    }

    /** the method read Nikuradse Coeff read the datas for Nikuradse coefficients
     *  from a JanetBinary-file named filename
     *  @param nam  name of the file to be open */
    @SuppressWarnings("unused")
    private void readNikuradseCoeffFromJanetBin(String filename) {

        nikuradse=true;

        int anzAttributes = 0;
        double ks;
        // NEU WIEBKE 22.02.2007
        boolean hasValidValues = true;
        // ENDE NEU WIEBKE
        int nr;
        short status, kennung;
        int anzPolys, anzEdges, anzPoints = 0, pointsize, trisize, swapMode;
        short sets;
        boolean active, protectBorder, protectConstraints, noPolygon, inPolygon, makeHoles, processFlagsActive;
        boolean noZoom, inZoom, noActive, processActive, processSelected, inPolygonProp, inZoomProp, protectInnerPoints;
        boolean noSelected, closed;
        boolean read_status_byte = false;

        FileIO bin_in = new FileIO();

        try {
            bin_in.fopenbinary(filename, FileIO.input);

            // Netz aus einer Binaerdatei lesen

            // Version auslesen
            float version = bin_in.fbinreadfloat();
            if (version < 1.5f) {
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version + ", current version: 1.8");
            }

            if (version < 1.79) {
                read_status_byte = true;
            }

            System.out.println("\tRead nikuradse coefficients (in Janet-Binary-Format): " + filename);

            // zunaechst den FileHeader lesen
            boolean writePointNumbers = bin_in.fbinreadboolean();
            boolean writePointAttributes = bin_in.fbinreadboolean();
            anzAttributes = bin_in.fbinreadint();
            boolean writePointStatus = bin_in.fbinreadboolean();
            boolean writeConstraintPolygons = bin_in.fbinreadboolean();
            boolean writeConstraintEdges = bin_in.fbinreadboolean();
            boolean writeElements = bin_in.fbinreadboolean();
            boolean writeElementNumbers = bin_in.fbinreadboolean();
            boolean writeElementKennung = bin_in.fbinreadboolean();
            boolean writeAlphaTestRadius = bin_in.fbinreadboolean();

            // Layertyp ueberlesen
            int filetype = bin_in.fbinreadint();
            // liegt UnTRIM-Gitetr mit diskreten Kantentiefen vor??
            boolean is_untrim = (filetype == 2);

            // Anzahl der Punkte lesen
            int anzk = bin_in.fbinreadint();
            if (anzk == fenet.getNumberofDOFs()) {

                // Punkte lesen
                for (int i = 0; i < anzk; i++) {
                    // Punktnummer lesen
                    if (writePointNumbers) {
                        nr = bin_in.fbinreadint();
                    } else {
                        nr = i;
                    }

                    // x,y,ks lesen
                    bin_in.fbinreaddouble(); // read x-coordinate
                    bin_in.fbinreaddouble(); // read y-coordinate
                    ks = bin_in.fbinreaddouble(); // -> ks
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(ks) || ks <= 0.) {
                        hasValidValues = false;
                    }
                    // ENDE NEU WIEBKE 20.02.2007
                    DOF dof = fenet.getDOF(nr);
                    CurrentModel3DData currentdata = CurrentModel3DData.extract(dof);
                    currentdata.ks = ks;
                    currentdata.kst = CurrentModel2DData.Nikuradse2Strickler(ks); // nach http://www.baw.de/vip/abteilungen/wbk/Publikationen/scn/sc1-99a/node21.htm

                    // Status-Flag lesen
                    if (writePointStatus) {
                        bin_in.fbinreadshort();
                    }

                }
                // Abbruch, wenn Netz nicht ok!
                if (!hasValidValues) {
                    System.out.println("***                     WARNUNG                       ***");
                    System.out.println("***   Ueberpruefen Sie die Rauheiten des              ***");
                    System.out.println("***   Rauheitennetzes. Das verwendetet Netz hat       ***");
                    System.out.println("***   Knoten mit negativen oder nicht definierten     ***");
                    System.out.println("***   Rauheiten!                                      ***");
                    System.out.println("***   Die Simulation wird fortgesetzt                 ***");
                //System.exit(0);
                }
            } else {
                System.out.println("system und nikuradse.jbf different number of nodes");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
            System.exit(1);
        }
    }

    /** the method readStricklerCoeff read the datas for strickler coefficients
     *  from a sysdat-file named nam
     *  @param nam  name of the file to be open */
    private void readNikuradseCoeff(String filename) {

        nikuradse=true;

        int rand_knoten = 0;
        int gebiets_knoten = 0;
        int knoten_nr;


        double ks;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading Nikuradse Coefficients from file (in TiCAD-System.Dat-Format): " + filename);

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));
            java.util.StringTokenizer strto = new StringTokenizer(line, " \t\n\r\f,");
            rand_knoten = Integer.parseInt(strto.nextToken());

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));


            strto = new StringTokenizer(line, " \t\n\r\f,");
            gebiets_knoten = Integer.parseInt(strto.nextToken());

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000) {
                throw new Exception("Fehler");
            }

            // Knoten einlesen
            int p_count = 0;
            while (p_count < (rand_knoten + gebiets_knoten)) {
                line = systemfile.freadLine();
                strto = new StringTokenizer(line, " \t\n\r\f,");

                if (!line.startsWith("C")) {
                    knoten_nr = Integer.parseInt(strto.nextToken());
                    Double.parseDouble(strto.nextToken()); // read x coordinate
                    Double.parseDouble(strto.nextToken()); // read y-coordinate
                    try {
                        ks = Double.parseDouble(strto.nextToken());
                    } catch (Exception ex) {
                        ks = Double.NaN;
                    }

                    if (Double.isNaN(ks) || ks < 0) {

                        System.out.println("");

                        System.out.println("********************************       ERROR         ***********************************");
                        System.out.println("Invalid z-value (z=NaN or z<0.0) in Bottom Friction-Mesh: <" + filename + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count + "> has a correct floating point (greater zero)");
                        System.out.println("bottom friction value");
                        System.out.println("*****************************************************************************************");
                        System.out.println("");
                        System.exit(1);
                    }
                    DOF dof = fenet.getDOF(knoten_nr);
                    CurrentModel3DData currentdata = CurrentModel3DData.extract(dof);
                    currentdata.ks = ks;
                    currentdata.kst = CurrentModel2DData.Nikuradse2Strickler(ks); // nach http://www.baw.de/vip/abteilungen/wbk/Publikationen/scn/sc1-99a/node21.htm

                    p_count++;
                }

            }
        } catch (Exception e) {
            System.out.println("!! cannot open file: " + filename+" !!");
            System.exit(1);
        }

//        // Anzahl der Elemente
//        FTriangle[] element = (FTriangle[]) fenet.getFElements();
//        double smean = 0.;
//        for (int i = 0; i < element.length; i++) {
//            DOF[] dofs = element[i].getDOFs();
//            smean = 0.;
//            for (int s = 0; s < 3; s++) {
//                smean += CurrentModel2DData.extract(dofs[s]).kst;
//            }
//
//            Current2DElementData.extract(element[i]).meanStricklerCoefficient = smean / 3.;
//        }

    }
    
    private void write_sys(DataOutputStream os, FTriangleMesh net) {
        try {
            
            os.writeBytes("Datei vom Typ Current3DErg.bin, erzeugt von Marina " + MarinaXML.majorversion + "." + MarinaXML.minorversion + "." + MarinaXML.update + "\n");
            os.writeBytes("ModelComponent " + getClass().getName() + "\n");

            Date date = new Date();
            os.writeBytes("Datum: " + date.toString() + "\n");

            double minX = net.getDOF(0).x;
            double minY = net.getDOF(0).y;
            for (int i = 1; i < net.anzk; i++) {
                minX = Function.min(minX, net.getDOF(i).x);
                minY = Function.min(minY, net.getDOF(i).y);
            }

            final int offSetX = ((int) (minX / 100000.)) * 100000;
            final int offSetY = ((int) (minY / 100000.)) * 100000;

            os.writeBytes("OffSetX " + offSetX + "\n");
            os.writeBytes("OffSetY " + offSetY + "\n");
            os.writeBytes("ReferenceDate " + referenceDate + "\n");

            int c = 7;  // Kennzeichner fuer das Ende der Kommentarzeilen
            os.writeByte(c);
            
            int anzk = net.anzk;
            int anze = net.anze;
            
            //      Dateikopf schreiben
            os.writeInt(anzk);
            os.writeInt(anze);
            os.writeInt(0); // hier Anzahl Ergebniszeiten
            
            //      Knotenverzeichnis
            for( int i=0 ; i<anzk; i++){
                os.writeFloat((float) (net.getDOF(i).x - offSetX));
                os.writeFloat((float) (net.getDOF(i).y - offSetY));
                os.writeFloat((float)net.getDOF(i).z);
            }
            
            //      Elementverzeichnis
//            System.out.println(anze + " Elemente schreiben...");
            FElement[] felem = net.getFElements();
            for (FElement felem1 : felem) {
                FTriangle triangle = (FTriangle) felem1;
                for (int j=0; j<3; j++) {
                    os.writeInt( triangle.getDOF(j).number );
                }
                os.writeInt(0);
            }
            
        } catch (Exception e) {
        e.printStackTrace();}
    }
    
    // ----------------------------------------------------------------------
    // write_erg3D
    // ----------------------------------------------------------------------
    public void write_erg() {
        try {
            os.writeFloat((float) time);
            for (DOF dof : fenet.getDOFs()) {
                CurrentModel3DData current = dof_data[dof.number];
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }

                os.writeFloat((float) current.z);
                os.writeFloat((float) current.eta);
                  
                if (current.totaldepth < WATT && MarinaXML.release) {
                    os.writeFloat(0.f);
                    os.writeFloat(0.f);
                    os.writeFloat(0.f);
                } else {
                    os.writeFloat((float) current.tauBx); // tauX
                    os.writeFloat((float) current.tauBy); // tauY
                    os.writeFloat((float) current.tauBz); // tauZ
                }
                
                int anzWerte=current.f.getSizeOfValues();
                os.writeInt(anzWerte);
                
                double[]  werte;
                for(int j=0;j<anzWerte;j++) {
                    werte=current.f.getValueAt(j);
                    double hg=current.f.getxAt(j); //tiefe
                    if((j!=anzWerte-1) && (-hg>current.eta) && (-current.f.getxAt(j+1)<=current.eta) && MarinaXML.release ) hg=-current.eta;
                    os.writeFloat((float)hg);  // tiefe
                    
                    final double p = (hg+current.eta)*0.0980665 * 100.;
                    
                    if((((j+1<anzWerte) && (-current.f.getxAt(j+1)>current.eta))||(current.totaldepth < WATT)) && MarinaXML.release) {
                        os.writeFloat(0.f);
                        os.writeFloat(0.f);
                        os.writeFloat(0.f);
                        os.writeFloat(0.f);
                    } else {
                        os.writeFloat((float)werte[_u]);
                        os.writeFloat((float)werte[_v]);
                        os.writeFloat((float)werte[_w]);
                        os.writeFloat((float)p); // hydrostatische Druck in [kPa] - ergibt sich je 1m Wassersaeule zu 0,0980665 bar (1 bar = 10^5 Pa = 100 kPa)
                    }
                }
            }
            os.flush();
        } catch (Exception e) {
            System.out.println(this.getClass()+"\n\ttime="+time+"\n");
            e.printStackTrace();
            System.exit(0);
        }
    } // end write_erg

    @Override
    public int getTicadErgMask() {
        // Setzen der Ergebnismaske
        return TicadIO.HRES_Z | TicadIO.HRES_V | TicadIO.HRES_H | TicadIO.HRES_SHEAR;
    }
    
    /**
     * The method write_erg_xf
     * @param erg
     * @param t
     * @deprecated
     */
    @Override
    @Deprecated
    public void write_erg_xf(double[] erg, double t) {
        throw new UnsupportedOperationException("Not implemented yet");
    } // end write_erg_xf

    /** The method write_erg_xf */
    @Override
    public final void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            for (DOF dof : fenet.getDOFs()) {
                CurrentModel3DData current = dof_data[dof.number];
                
                xf_os.writeFloat((float) current.z);

                float u,v;
                if (current.totaldepth<WATT && MarinaXML.release) {
                    u = 0.f;
                    v = 0.f;
                } else {
                    u=(float)(current.qx/Math.max(WATT,current.totaldepth));
                    v=(float)(current.qy/Math.max(WATT,current.totaldepth));
                }

                xf_os.writeFloat(u);        // v1x
                xf_os.writeFloat(v);        // v1y
                xf_os.writeFloat((float)current.eta);        // skalar1
                
                if (current.totaldepth < WATT && MarinaXML.release) {
                    xf_os.writeFloat(0.f);
                    xf_os.writeFloat(0.f);
                } else {
                    xf_os.writeFloat((float) current.tauBx);
                    xf_os.writeFloat((float) current.tauBy);
                }
            }
            xf_os.flush();
        } catch (IOException e) {
            System.out.println(this.getClass()+"\n\ttime="+time+"\n");
            e.printStackTrace();
            System.exit(0);
        }
    } // end write_erg_xf
    
    @Override
    public final void timeStep(double dt) {

        resultIsNaN = false;

        setBoundaryConditions();

        maxTimeStep = Double.MAX_VALUE;

        // Elementloop
        performElementLoop();

        Arrays.stream(fenet.getDOFs()).parallel().forEach(dof -> {

            final CurrentModel3DData currentdata = dof_data[dof.number];

            double u=0., v=0., w=0.;
            double dudt=0., dvdt=0.;
            for (int s = currentdata.f.getSizeOfValues()-1; s >=0 ; s--) {

                if (currentdata.eta >= (-currentdata.f.getxAt(s)) || currentdata.eta <= (-currentdata.f.getxAt(s))&& (s + 1 < currentdata.f.getSizeOfValues() && currentdata.eta > (-currentdata.f.getxAt(s + 1))) || (currentdata.f.getSizeOfValues() < 2)) {
                   
                    if (currentdata.z <= currentdata.f.getxAt(s)) {
                        // Geschwingikeiten unterhalb des Bodens auf 0 setzen
                        currentdata.f.setValueAt(_u, s, 0.);
                        currentdata.f.setValueAt(_v, s, 0.);
                        currentdata.f.setValueAt(_dudt, s, 0);
                        currentdata.f.setValueAt(_dvdt, s, 0);
                        currentdata.f.setValueAt(_rw, s, 0.);
                        currentdata.f.setValueAt(_w, s, 0);
                        currentdata.f.setValueAt(_qx, s, 0.);
                        currentdata.f.setValueAt(_qy, s, 0.);
                    } else {
                        dudt = currentdata.f.getValueAt(_ru, s) / dof.lumpedMass;
                        dvdt = currentdata.f.getValueAt(_rv, s) / dof.lumpedMass;
                        w = currentdata.f.getValueAt(_rw, s) / dof.lumpedMass;
                        // zusaetzlichen Stabilisierung in Anlehnung am expliziten Adams-Bashford 2. Ordnung
                        final double ru = (3. * dudt - currentdata.f.getValueAt(_dudt, s)) / 2.;
                        final double rv = (3. * dvdt - currentdata.f.getValueAt(_dvdt, s)) / 2.;

                        currentdata.f.setValueAt(_dudt, s, dudt);
                        currentdata.f.setValueAt(_dvdt, s, dvdt);
                        currentdata.f.setValueAt(_w, s, w);

                        u = (currentdata.f.getValueAt(_u, s) + dt * ru) * currentdata.puddleLambda;
                        v = (currentdata.f.getValueAt(_v, s) + dt * rv) * currentdata.puddleLambda;

                        // Froude - kostet viel Zeit, ist aber bei Dammbruchsimulationen notwendig 
                        final double aktuelletiefe = Math.max(0, currentdata.z - Math.max(-currentdata.eta, currentdata.f.getxAt(s)));
                        if (aktuelletiefe > halfWATT / 10.) {
                            final double vg = PhysicalParameters.G * aktuelletiefe;
                            final double cv2 = u * u + v * v;
                            final double froud = Math.sqrt(cv2 / vg);
                            if (froud > 1.) {
                                u /= froud;
                                v /= froud;
                            }
                        } else {
                            u *= aktuelletiefe/(halfWATT/10.);
                            v *= aktuelletiefe/(halfWATT/10.);
                        }

                        currentdata.f.setValueAt(_u, s, u);
                        currentdata.f.setValueAt(_v, s, v);

                        if ((currentdata.eta >= (-currentdata.f.getxAt(s))) && (s == 0) || currentdata.eta <= (-currentdata.f.getxAt(s)) && (s + 1 < currentdata.f.getSizeOfValues() && currentdata.eta > (-currentdata.f.getxAt(s + 1)) || (currentdata.f.getSizeOfValues() < 2))) { // Wasseroberflaeche dazwischen

                            currentdata.detadt = w;
                            double rw = -(3. * w - currentdata.f.getValueAt(_w, s)) / 2.; // zusaetzlichen Stabilisierung in Anlehnung am expliziten Adams-Bashford 2. Ordnung

                            if ((currentdata.wattsickern) && (rw > 0.)) {
                                rw *= currentdata.wlambda;
                            }
                            rw -= (1.E-6 + infiltrationRate) * (1. - currentdata.puddleLambda); // Versickern in Pfuezen

                            currentdata.setWaterLevel(currentdata.eta + dt * rw);
                        }
                    }
                   
                } else { // Ueber der Wasseroberflaeche
                    
                    currentdata.f.setValueAt(_u, s, u);
                    currentdata.f.setValueAt(_v, s, v);
                    currentdata.f.setValueAt(_dudt, s, dudt);
                    currentdata.f.setValueAt(_dvdt, s, dvdt);
                    currentdata.f.setValueAt(_w, s, w);
                    
                }
                
                boolean rIsNaN = Double.isNaN(u) ||  Double.isNaN(v) || Double.isNaN(w)
                        || Math.abs(u)>1.E5 || Math.abs(v)>1.E5 || Math.abs(w)>1.E5;
                if (rIsNaN) {
                    System.out.println("CurrentModel3D is NaN at node " + dof.number + " in layer " + s + ": u=" + u + " v=" + v + " w=" + w);
                }
                resultIsNaN |= rIsNaN;
            }

        });

        this.time += dt;

        if (resultIsNaN) {
            System.out.println("Time="+this.time+" and timestep is "+dt);
            write_erg_xf();
            write_erg();
            try {
                xf_os.close();
                os.close();
            } catch (IOException e) {}
            System.exit(1);
        }
    }
}
