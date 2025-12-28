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

import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.*;
import javax.vecmath.*;
import java.util.*;
import bijava.math.ifunction.*;
import de.smile.marina.MarinaXML;
import de.smile.marina.fem.TicadModel;
import de.smile.marina.fem.model.hydrodynamic.BoundaryConditionOld;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.SmileIO;

/** this ODE describe groundwater model based on darcy equation
 * @version 1.7.0
 * @author Peter Milbradt
 */
public class  GroundWaterModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel  {
    
    private GroundWater2DData[] dof_data=null;
    private GroundWater2DElementData[] element_data=null;
    
    private DataOutputStream xf_os = null;
    
    private Vector bh  = new Vector();
    
    private Vector inith = new Vector();
    
    private int n,H;
    
    private GroundWaterDat groundwaterdat;
    
    double[] result;// zum speichern der Zeitableitungen
    double[] x;     // zum Speichern der Zustandsgroessen
    private double WATT = 0.1;

    // konstruktor
    public GroundWaterModel2D(FEDecomposition fe, GroundWaterDat _groundwaterdata) {
        System.out.println("GroundWaterModel2D initalization");
        fenet = fe;
        femodel=this;
        groundwaterdat = _groundwaterdata;
        
        dof_data= new GroundWater2DData[fenet.getNumberofDOFs()];
        element_data=new GroundWater2DElementData[fenet.getNumberofFElements()];
        
        setNumberOfThreads(groundwaterdat.NumberOfThreads);
        
       // readBoundCondOld(groundwaterdat.rndwerte_name);
        readBoundCond();
        
        BoundaryCondition bcond;
        Enumeration be = bh.elements();
        while (be.hasMoreElements()) {
            bcond = (BoundaryCondition) be.nextElement();
            inith.addElement(fenet.getDOF(bcond.pointnumber));
        }
        
        // DOFs initialisieren
        initialDOFs();
        
        initialElementModelData();
        
        // read free surface water level als boundary condition
        if (groundwaterdat.surfaceWater_name!=null){
            if(groundwaterdat.surfaceWater_FileType==SmileIO.MeshFileType.SystemDat)
                readSurfaceWaterFromSysDat(groundwaterdat.surfaceWater_name);
            if(groundwaterdat.surfaceWater_FileType==SmileIO.MeshFileType.JanetBin)
                readSurfaceWaterFromJanetBin(groundwaterdat.surfaceWater_name);
        }
        
        // read permeability-dat
        if (groundwaterdat.permeability_name!=null){
            if(groundwaterdat.permeability_FileType==SmileIO.MeshFileType.SystemDat)
                readPermeabilityDat(groundwaterdat.permeability_name);
            if(groundwaterdat.permeability_FileType==SmileIO.MeshFileType.JanetBin)
                readPermeabilityFromJanetBin(groundwaterdat.permeability_name);
        }        
        else{
            for (DOF dof : fenet.getDOFs()) {
                GroundWater2DData.extract(dof).kf = groundwaterdat.standardPermeability;
            }
        }
        
     
        // read impermeability-dat
        if (groundwaterdat.impermeability_name!=null){
            if(groundwaterdat.impermeability_FileType==SmileIO.MeshFileType.SystemDat)
                readImpermeabilityDat(groundwaterdat.impermeability_name);
            if(groundwaterdat.permeability_FileType==SmileIO.MeshFileType.JanetBin)
                readImpermeabilityFromJanetBin(groundwaterdat.impermeability_name);
        }        
        else{
            for (DOF dof : fenet.getDOFs()) {
                GroundWater2DData.extract(dof).zG = groundwaterdat.standardImpermeability;
            }
        }
        
        // read upperImpermeabilityThickness
        if (groundwaterdat.upperImpermeabilityThickness_name!=null){
            if(groundwaterdat.upperImpermeabilityThickness_FileType==SmileIO.MeshFileType.SystemDat)
                readOverlayingStrata(groundwaterdat.upperImpermeabilityThickness_name);
            if(groundwaterdat.upperImpermeabilityThickness_FileType==SmileIO.MeshFileType.JanetBin)
                readOverlayingStrataFromJanetBin(groundwaterdat.upperImpermeabilityThickness_name);
        }        
        else{
            for (DOF dof : fenet.getDOFs()) {
                GroundWater2DData.extract(dof).upperImpermeableLayer = groundwaterdat.standartUpperImpermeabilityThickness;
            }
        }
        
        
        
        final int numberofdofs = fenet.getNumberofDOFs();
        
        H = 0;
        n = numberofdofs;
        result = new double[n];
        
        try {
            xf_os = new DataOutputStream(new FileOutputStream(groundwaterdat.xferg_name));
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ groundwaterdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }
    
    @Override
     public int getTicadErgMask(){
        // Setzen der Ergebnismaske 
        return TicadIO.HRES_V | TicadIO.HRES_H;
    }
    
    /** initialisiert die wasserspiegellage mit einem konstatnten Wert
     * @param initalvalue
     * @return  */
    public double[] ConstantInitialSolution(double initalvalue) {
        System.out.println("\t Set inital value "+initalvalue);
        
        x = new double[fenet.getNumberofDOFs()];
        
        for (int i=0; i<fenet.getNumberofDOFs(); i++){
            x[i+H] = initalvalue;
        }
        
        return x;
    }
    
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
            
            //Ueberlesen folgende Zeilen
            inStream.skip(9L*4L);
            
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
            
            //Elemente, Rand und Knoten Ueberlesen
            inStream.skip((anzElemente*4L+anzr+3L*anzKnoten)*4L); //4 Bytes je float und int
            
            // bis zum record-Satz springen
            inStream.skip((4L+anzKnoten*anzWerte*4L)*record);
            
            x = new double[n];
            
            float t=inStream.readFloat();
            for (int i = 0;i<fenet.getNumberofDOFs();i++){
                if(None_gesetzt)
                    inStream.skip(4);
                
                if(Pos_gesetzt)
                    inStream.skip(4);
                
                if(Z_gesetzt)
                    inStream.skip(4);
                
                if(V_gesetzt){
                    dof_data[i].u = inStream.readFloat()/1000.;
                    dof_data[i].v = inStream.readFloat()/1000.;
                }
                
                if(Q_gesetzt)
                    inStream.skip(8);
                
                if(H_gesetzt){
                    x[H + i] = inStream.readFloat();
                    CurrentModel2DData current2d = CurrentModel2DData.extract(fenet.getDOF(i));
                    if(current2d!=null){
                        if ((current2d.z+current2d.eta>CurrentModel2D.WATT)){
                            if ((current2d.eta-x[H+i])>0.) x[H+i] = current2d.eta;
                        }
                        if ((current2d.z+x[H + i]>CurrentModel2D.WATT)){
                            x[H + i]=current2d.eta=Math.max(x[H + i],current2d.eta);
                        }
                    }
                }
                
                if(SALT_gesetzt)
                    inStream.skip(4);
                
                if(EDDY_gesetzt)
                    inStream.skip(4);
                
                if(SHEAR_gesetzt)
                    inStream.skip(8);
                
                if(V_SCAL_gesetzt)
                    inStream.skip(4);
                
                if(Q_SCAL_gesetzt)
                    inStream.skip(4);
                
                if(AH_gesetzt)
                    inStream.skip(4);
                
            }
        }
        return x;
    }
    
    /** the method initialSolutionFromTicadErgFile compute a startsolution
     * @param time the time to generate a inital solution
     * @return
     */
    public double[] initialSolution(double time){
        this.time=time;
        x = new double[n];
        
//        System.out.println("CurrentModel - Werte Initialisieren");
        int NumberOfThreads=getNumberOfThreads();
        
        initalSolutionLoop[] iloop = new initalSolutionLoop[NumberOfThreads];
        int anzdofs = fenet.getNumberofDOFs();
        for (int ii=0; ii<NumberOfThreads; ii++){
            iloop[ii]= new initalSolutionLoop(anzdofs*ii/NumberOfThreads, anzdofs*(ii+1)/NumberOfThreads,x,time);
            iloop[ii].start();
        }
        for(int ii=0; ii<NumberOfThreads; ii++)
            try{iloop[ii].join();} catch(Exception e){}
        
        inith=null;
        return x;
    }
    
    public final class initalSolutionLoop extends Thread {
        int lo, hi;
        double[] x;
        double time;
        initalSolutionLoop(int lo, int hi, double[] x, double time ){
            this.lo=lo;
            this.hi=hi;
            this.x = x;
            this.time = time;
        }
        @Override
        public void run(){
            for(int ii=lo; ii<hi; ii++){
                DOF dof= fenet.getDOF(ii);
                int i = dof.number;
                GroundWater2DData modeldata = dof_data[i];
                
                x[H + i] = initialH(dof,time);
                
                if ((modeldata.zG + x[H + i]) <= 0.)
                    x[H + i] = -modeldata.zG;
                
                CurrentModel2DData current2d = CurrentModel2DData.extract(dof);
                if(current2d!=null){
                    if ((current2d.z+current2d.eta>CurrentModel2D.WATT)){
                        if ((current2d.eta-x[H+i])>0.) x[H+i] = current2d.eta;
                    }
                }
                
                modeldata.h=x[H + i];
            }
        }
    }
    
    // ----------------------------------------------------------------------
    // initialH
    // ----------------------------------------------------------------------
    private double initialH( DOF dof, double time){
        double h=0., R=0., d;
        GroundWater2DData modeldata = dof_data[dof.number];
        if (modeldata.bh != null)
            h = modeldata.bh.getValue(time);
        else {
            for (Enumeration e = inith.elements(); e.hasMoreElements();){
                DOF ndof= (DOF) e.nextElement();
                GroundWater2DData modeldata1 = dof_data[ndof.number];
                if ((dof!=ndof) & ( modeldata1.bh != null )){
                    d = dof.distance(ndof);
                    h += modeldata1.bh.getValue(time) / d;
                    R += 1./d;
                }
            }
            if( R != 0. )
                h /= R;
            else
                h = 0.;
        }
        return h;
    }
    
   
            
    
    public double[] initialHfromJanetBin(String filename, double time) throws Exception {
        this.time=time;
        x = new double[n];
        
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
                    x[H + nr] = bin_in.fbinreaddouble();
                    
                    DOF dof= fenet.getDOF(nr);
                    
                    
                    GroundWater2DData groundwaterdata = dof_data[dof.number];
                    groundwaterdata.h=x[H + nr];

                
                    
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                    
                }
            } else System.out.println("system and waterlevel.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
            System.exit(0);
        }
        
        inith=null;
        
        return x;
    }
    
    
    
    public double[] initialHfromASCIIFile(String systemDatPath, double time) throws Exception {
        this.time=time;
        x = new double[n];
        
        try {
            InputStream is = new FileInputStream(systemDatPath);
            
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('C');
            
            int anzr = TicadIO.NextInt(st);
            int anzi = TicadIO.NextInt(st);
            int anzk = anzr + anzi;
            if(anzk==fenet.getNumberofDOFs()){
                System.out.println("\t Read initial waterlevel from "+systemDatPath);
                for(int j = 0;j< anzk; j++) {
                    int  pos = TicadIO.NextInt(st);
                    TicadIO.NextDouble(st);
                    TicadIO.NextDouble(st);
                    x[H + pos] = TicadIO.NextDouble(st); // -> eta
                    DOF dof=fenet.getDOF(pos);
                    GroundWater2DData groundwaterdata = dof_data[dof.number];
                    groundwaterdata.h=x[H + pos];
                }
            } else System.out.println("\t different number of nodes");
            
            r.close();            
        } catch (Exception e) {
            System.out.println("\t cannot open file: "+systemDatPath);
        }
        
        inith=null;
        return x;
    }
    
    
    /** Compute the time derivations on each node by the FE-domainapproximation of the FE-Model
     * @param time
     * @param x
     * @return  */
    @Override
    public double[] getRateofChange(double time, double x[]){
        
        this.x=x;
        this.time=time;
        
        java.util.Arrays.fill(result,0.0);
        
        setBoundaryConditions();
        
        maxTimeStep = 10000.;
        
        // Elementloop
        performElementLoop();
        
        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            final int gamma = dof.getNumberofFElements();
            result[H+i] *= 3. / gamma;
            double rh = (3.*result[H+i]-dof_data[i].dhdt)/2.;  // zusaetzlichen Stabilisierung in Anlehnung am expliziten Adams-Bashford 2. Ordnung
            dof_data[i].dhdt=result[H+i];
            result[H+i]=rh;
            dof_data[i].nu /= gamma;
            dof_data[i].nv /= gamma;
            dof_data[i].u = dof_data[i].nu;
            dof_data[i].v = dof_data[i].nv;
        }
        
        return result;
    } // end getRateofChange
    
    
    /**
     * @param element
     * @return   */
    @Override
    public double ElementApproximation(FElement element){

        double timeStep=Double.POSITIVE_INFINITY;
        
        FTriangle ele = (FTriangle) element;
        DOF[] dofs=element.getDOFs();
        
        final double[][] koeffmat = ele.getkoeffmat();
        
        final double[] m = new double[3]; // Maechtigkeit der des Grundwasserleiters
        final double[] h = new double[3]; // Groundwaterlevel
        
        double hdx = 0.;
        double hdy = 0.;
        
        double dqxdx = 0.;
        double dqydy = 0.;
        
        int iwatt = 0;
        int ibound = 0;
        
        double kf=0.; // TODO aus den Elementparametern lesen
        
        // compute element derivations
        for ( int j = 0; j < 3; j++) {
            DOF dof = dofs[j];
            int i = dof.number;
            
            if(dof_data[i].bh!=null) ibound++;
            
            if((x[H+i]+dof_data[i].zG)<WATT) {
                iwatt++;
            } else {
                if(x[H+i]< -dof_data[i].upperImpermeableLayer)
                    m[j] = x[H+i]+dof_data[i].zG;  // freier GW
                else
                    m[j] = -dof_data[i].upperImpermeableLayer+dof_data[i].zG;  // gespannter GW
            }
            
            hdx += x[H+i] * koeffmat[j][1];
            hdy += x[H+i] * koeffmat[j][2];
            
            dqxdx += m[j] * dof_data[i].u * koeffmat[j][1];
            dqydy += m[j] * dof_data[i].v * koeffmat[j][2];
            
            h[j]=x[H+i];
            
            kf+=dof_data[i].kf/3.;
            
//            q[j]=0.01 * dof_data[i].source;
        }

        double S0=0.25;     // effektive Porositaet TODO aus den Elementparametern lesen
        
        if(iwatt!=0){
            hdx=0.;
            hdy=0.;
            if(iwatt==1){
                    for (int j = 0; j < 3; j++)
                        if(m[j] >= WATT){
                            hdx += h[j] * koeffmat[j][1];
                            hdy += h[j] * koeffmat[j][2];
                        } else {
                            if ((h[j] < h[(j+1)%3]) || (h[j] < h[(j+2)%3])){
                                hdx += h[j] * koeffmat[j][1];
                                hdy += h[j] * koeffmat[j][2];
                            } else {
                                hdx += ((1.-m[j]/WATT)*0.5*(h[(j+1)%3]+h[(j+2)%3]) + m[j]/WATT * h[j] ) * koeffmat[j][1];
                                hdy += ((1.-m[j]/WATT)*0.5*(h[(j+1)%3]+h[(j+2)%3]) + m[j]/WATT * h[j] ) * koeffmat[j][2];
                            }
                        }
                }  else if (iwatt==2){
                    for (int j = 0; j < 3; j++)
                        if((m[j] >= WATT)&&(h[j] > 0.5*(h[(j+1)%3]+h[(j+2)%3]))){
                        hdx += h[j] * koeffmat[j][1]
                                + 0.5*(h[(j+1)%3]+h[(j+2)%3]) * koeffmat[(j+1)%3][1]
                                + 0.5*(h[(j+1)%3]+h[(j+2)%3]) * koeffmat[(j+2)%3][1];
                        hdy += h[j] * koeffmat[j][2]
                                + 0.5*(h[(j+1)%3]+h[(j+2)%3]) * koeffmat[(j+1)%3][2]
                                + 0.5*(h[(j+1)%3]+h[(j+2)%3]) * koeffmat[(j+2)%3][2];
                        }
                }
        } 
            
            double u = -kf * hdx; // ToDo in die Elementparametern schreiben
            double v = -kf * hdy; // ToDo in die Elementparametern schreiben
        
            if(ibound!=0){u*=1.-ibound/3.;v*=1.-ibound/3.;}
            
            double tau_cur = 0.;
            if((Function.norm(hdx, hdy)>0.00001)&&(ibound==0)){
                tau_cur = 0.5 * ele.getVectorSize( hdx, hdy ) / Function.norm(u, v);
                timeStep = tau_cur;
            }
            
            double LRes = 0.;
            // Elementfehler berechnen
            for (int j = 0; j < 3; j++) {
                
                DOF dof = ele.getDOF(j);
                int i = dof.number;
                
                LRes +=  1./3. * (dof_data[i].dhdt - 1./S0 *(dqxdx + dqydy));
            }

            for ( int j = 0; j < 3; j++) {
                DOF dof = dofs[j];
                int i = dof.number; 
                synchronized (dof_data[i]) { // Peter 28.08.2010
                    dof_data[i].nu += u;
                    dof_data[i].nv += v;
                }
                double result_H_i = +tau_cur * ( koeffmat[j][1] * u * LRes + koeffmat[j][2] * v * LRes );
                
                double vorfaktor;
                for (int l=0;l<2;l++){
                    if(j==l) vorfaktor=1./3.;
                    else vorfaktor=1./6.;
                        result_H_i += 1./S0 * vorfaktor*(m[l]*(koeffmat[j][1] * u + koeffmat[j][2] * v));
                }
                synchronized (dof){
                        result[H+i] += result_H_i;
                }
        }
            return timeStep;
    } // end ElementApproximation
    
    // ----------------------------------------------------------------------
    // setBoundaryCondition
    // ----------------------------------------------------------------------
    /**
     * @param dof
     * @param t  */
    @Override
    public void setBoundaryCondition(DOF dof, double t){
        
        int i = dof.number;
        GroundWater2DData groundwaterdata = dof_data[i];
        
        if (groundwaterdata.bh != null){
//            System.out.println(i+" bh");
            x[H + i]    = groundwaterdata.bh.getValue(t);
//            groundwaterdata.detadt = groundwaterdata.bh.getDifferential(t);
        }
        
        CurrentModel2DData current2d = CurrentModel2DData.extract(dof);
        if(current2d!=null){
            if ((current2d.z+current2d.eta>CurrentModel2D.WATT)){
                if ((current2d.eta-x[H+i])>0.) x[H+i] = current2d.eta;
//                if ((current2d.eta-x[H+i])>0.) groundwaterdata.source = Function.max(0,current2d.eta-x[H+i]);
            }
        }
        
        if((x[H+i]+dof_data[i].zG)<=0.) x[H+i]=-dof_data[i].zG;
        
        groundwaterdata.h = x[H + i];
        
        groundwaterdata.u = groundwaterdata.v = 0; // initialisieren zum knotenweise bestimmen der Geschwindigkeiten
        
    } // end setBoundaryCondition
    
    @Override
    public ModelData genData(FElement felement) {
        GroundWater2DElementData res = new GroundWater2DElementData();
        element_data[felement.number]=res;
        return res;
    }
    
    /** genData generate the nessecery modeldatas for a DOF
     * @param dof
     * @return  */
    @Override
    public ModelData genData(DOF dof){
        //System.out.println("DOF "+dof);
        GroundWater2DData data = new GroundWater2DData();
        int dofnumber = (int) dof.number;
        dof_data[dofnumber]=data;
        
        Enumeration<BoundaryCondition> b = bh.elements();
        while (b.hasMoreElements()) {
            BoundaryCondition bcond = (BoundaryCondition) b.nextElement();
            if ( dofnumber == bcond.pointnumber ){
                data.bh = bcond.function;
                bh.removeElement(bcond);
            }
        }
        return data;
    } // end genData
    
    
    
    
    /** the method readPermeabilityDat read the datas for kf 
     *  from a sysdat-file named filename
     *  @param nam  name of the file to be open */
    private void readPermeabilityDat(String filename) {
        int rand_knoten=0;
        int gebiets_knoten = 0;
        int knoten_nr;
        
        
        double x,y,kf;
        
        String line;
        
        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');
            
            System.out.println("\tReading Permeability-File (in TiCAD-System.Dat-Format): "+filename);
            
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
                         kf=Double.parseDouble(strto.nextToken()); // -> kf
                         
                    } catch(Exception ex) {
                        kf=Double.NaN;
                    }
                    
                    if (Double.isNaN(kf) || kf<0){
                        
                        System.out.println("");
                        
                        System.out.println("********************************       ERROR         ***********************************");
                        System.out.println("Invalid kf-value (kf=NaN or kf<0.0) in Permeability-File: <"+filename+"> node number <"+p_count+">");
                        System.out.println("To correct this problem ensure that node nr <"+p_count+"> has a correct floating point (greater zero)");
                        System.out.println("Permeability  value");
                        System.out.println("*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                    DOF dof=fenet.getDOF(knoten_nr);
                    GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                    groundwaterdata.kf=kf;
                                         
                    
                    //if(p_count%1000==0) System.out.println(p_count);
                    p_count++;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Permeabilitaets-Datei!");
            System.exit(0);
//            System.out.println("\t\tcannot open file: "+filename);
//            System.out.println("\t\tuse standard value for d50: 0.42 mm");
//            DOF[] dof = fenet.getDOFs();
//            for (int i=0; i<dof.length; i++){
//                SedimentModel2DData.extract(dof[i]).d50 = 0.00042;
//            }
        } 
    }
    
      /** the method eadPermeabilityFromJanetBin read the datas for kf 
     *  from  a JanetBinary-file named filename
     *  @param nam  name of the file to be open */ 
    private void readPermeabilityFromJanetBin(String filename) {
        int anzAttributes=0;
        double x,y,kf;        
        
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
            
            System.out.println("\t Read Permeability-File from "+filename);
            
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
                    kf=bin_in.fbinreaddouble(); 
                    
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(kf) || kf<=0.)
                    	hasValidValues=false;
                 
                    DOF dof=fenet.getDOF(nr);                   
                    
                    
                    GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                    groundwaterdata.kf=kf;
                   
                    
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                    
                }
                
                // Abbruch, wenn Netz nicht ok!
                if(!hasValidValues)
                {
                	System.out.println("***                     WARNUNG                       ***");
                	System.out.println("***   Ueberpruefen Sie die Permeabilitaeten des   ***");
                	System.out.println("***   Permeabilitaetsnetzes. Das verwendetet Netz hat      ***");
                	System.out.println("***   Knoten mit negativen oder nicht definierten     ***");
                	System.out.println("***   Permeabilitaeten!                       ***");
                	System.out.println("***   Die Simulation wird nicht fortgesetzt     !           ***");
                	System.exit(0);
                }
                
            } else System.out.println("system und permeability.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Permeabilitaets-Datei!");
            System.exit(0);
        }
    }    
    
    
    
    /** the method readImpermeabilityDat read the datas for zG 
     *  from a sysdat-file named filename
     *  @param nam  name of the file to be open */
    private void readImpermeabilityDat(String filename) {
        int rand_knoten=0;
        int gebiets_knoten = 0;
        int knoten_nr;
        
        
        double x,y,zG;
        
        String line;
        
        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');
            
            System.out.println("\tReading Impermeability-File (in TiCAD-System.Dat-Format): "+filename);
            
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
                         zG=Double.parseDouble(strto.nextToken()); // -> zG
                         
                    } catch(Exception ex) {
                        zG=Double.NaN;
                    }
                    
//                    if (Double.isNaN(zG) || zG<0){
//                        
//                        System.out.println("");
//                        
//                        System.out.println("********************************       ERROR         ***********************************");
//                        System.out.println("Invalid zG-value (zG=NaN or zG<0.0) in Impermeability-File: <"+filename+"> node number <"+p_count+">");
//                        System.out.println("To correct this problem ensure that node nr <"+p_count+"> has a correct floating point (greater zero)");
//                        System.out.println("impermeability  value");
//                        System.out.println("*****************************************************************************************");
//                        System.out.println("");
//                        System.exit(0);
//                    }
                    DOF dof=fenet.getDOF(knoten_nr);
                    GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                    groundwaterdata.zG=zG;
                                         
                    
                    //if(p_count%1000==0) System.out.println(p_count);
                    p_count++;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Impermeabilitaets-Datei!");
            System.exit(0);
//            System.out.println("\t\tcannot open file: "+filename);
//            System.out.println("\t\tuse standard value for d50: 0.42 mm");
//            DOF[] dof = fenet.getDOFs();
//            for (int i=0; i<dof.length; i++){
//                SedimentModel2DData.extract(dof[i]).d50 = 0.00042;
//            }
        }
    }
    
      /** the method eadPermeabilityFromJanetBin read the datas for kf 
     *  from  a JanetBinary-file named filename
     *  @param nam  name of the file to be open */ 
    private void readImpermeabilityFromJanetBin(String filename) {
        int anzAttributes=0;
        double x,y,zG;        
        
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
            
            System.out.println("\t Read Permeability-File from "+filename);
            
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
                    zG=bin_in.fbinreaddouble(); 
                    
                    // Plausibilitaetskontrolle
//                    if (Double.isNaN(zG) || zG<=0.)
//                    	hasValidValues=false;
                 
                    DOF dof=fenet.getDOF(nr);                   
                    
                    
                    GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                    groundwaterdata.zG=zG;
                   
                    
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                    
                }
                
                // Abbruch, wenn Netz nicht ok!
                if(!hasValidValues)
                {
                	System.out.println("***                     WARNUNG                       ***");
                	System.out.println("***   Ueberpruefen Sie die Impermeabilitaeten des   ***");
                	System.out.println("***   Impermeabilitaetsnetzes. Das verwendetet Netz hat      ***");
                	System.out.println("***   Knoten mit negativen oder nicht definierten     ***");
                	System.out.println("***   Impermeabilitaeten!                       ***");
                	System.out.println("***   Die Simulation wird nicht fortgesetzt     !           ***");
                	System.exit(0);
                }
                
            } else System.out.println("system und impermeability.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Impermeabilitaets-Datei!");
            System.exit(0);
        }
    }    

// *****************    
    /** the method readSurfaceWaterFromSysDat read a stationary free surface water level as boundary condition
     *  from a sysdat-file named filename
     *  @param nam  name of the file to be open */
    private void readSurfaceWaterFromSysDat(String filename) {
        int rand_knoten=0;
        int gebiets_knoten = 0;
        int knoten_nr;
        
        
        double x,y,swl;
        
        String line;
        
        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');
            
            System.out.println("\tReading stationary free surface water level as boundary condition from (in TiCAD-System.Dat-Format): "+filename);
            
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
                if (!line.startsWith("C")) {
                    knoten_nr=Integer.parseInt(strto.nextToken());
                    x=Double.parseDouble(strto.nextToken());
                    y=Double.parseDouble(strto.nextToken());
                    try {
                         swl=Double.parseDouble(strto.nextToken());
                         
                    } catch(Exception ex) {
                        swl=Double.NaN;
                    }
                    if (Double.isNaN(swl)){
                        
                        System.out.println("");
                        
                        System.out.println("********************************       ERROR         ***********************************");
                        System.out.println("Invalid surface water level-value (swl=NaN) in SurfaceWater-File: <"+filename+"> node number <"+p_count+">");
                        System.out.println("To correct this problem ensure that node nr <"+p_count+"> has a correct floating point");
                        System.out.println(" surface water level value");
                        System.out.println("*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                    DOF dof=fenet.getDOF(knoten_nr);
                    if((dof.z+swl)>=WATT){
                        GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                        groundwaterdata.bh = new ConstantFunction1d(swl);
                    }
                    p_count++;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der SurfaceWater-Datei!");
            System.exit(0);
        } 
    }
    
      /** the method readSurfaceWaterFromJanetBin read a stationary free surface water level as boundary condition 
     *  from  a JanetBinary-file named filename
     *  @param nam  name of the file to be open */  
    private void readSurfaceWaterFromJanetBin(String filename) {
        int anzAttributes=0;
        double x,y,swl;        
        
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
            
            System.out.println("\tReading stationary free surface water level as boundary condition from "+filename);
            
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
                    swl=bin_in.fbinreaddouble(); 
                    
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(swl))
                    	hasValidValues=false;
                 
                    DOF dof=fenet.getDOF(nr);
                    if((dof.z+swl)>=WATT){
                        GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                        groundwaterdata.bh = new ConstantFunction1d(swl);
                    }
                   
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                    
                }
                
                // Abbruch, wenn Netz nicht ok!
                if(!hasValidValues)
                {
                	System.out.println("***                     WARNUNG                       ***");
                	System.out.println("***   Ueberpruefen Sie die Oberflächenwasserspiegellage des   ***");
                	System.out.println("***   SurfaceWater-Netzes. Das verwendetet Netz hat      ***");
                	System.out.println("***   Knoten mit nicht definierten     ***");
                	System.out.println("***   Werten!                       ***");
                	System.out.println("***   Die Simulation wird nicht fortgesetzt     !           ***");
                	System.exit(0);
                }
                
            } else System.out.println("system und surfacewater.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der SurfaceWater-Datei!");
            System.exit(0);
        }
    }    
// ****************
    
    
    /**
     * the method readImpermeableLayer read the datas for the lower Impermeable Layer - the Ground of the aquifer
     *  from a file named nam
     *
     * @param nam  name of the file to be open
     */
    public void readImpermeableLayer(String nam) {

        try {
            InputStream is = new FileInputStream(nam);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                StreamTokenizer st = new StreamTokenizer(r);
                st.eolIsSignificant(true);
                st.commentChar('C');
                
                int anzr = TicadIO.NextInt(st);
                int anzi = TicadIO.NextInt(st);
                int anzk = anzr + anzi;
                if(anzk==fenet.getNumberofDOFs()){
                    System.out.println("\t Read lower impermeable layer from "+nam);
                    for(int j = 0;j< anzk; j++) {
                        int  pos = TicadIO.NextInt(st);
                        TicadIO.NextDouble(st);
                        TicadIO.NextDouble(st);
                        double zG = TicadIO.NextDouble(st);
                        DOF dof=fenet.getDOF(pos);
                        GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                        groundwaterdata.zG=zG;
                    }
                } else System.out.println("system.dat und imperLayer.dat different number of nodes");
            }
            
        } catch (Exception e) {
            System.out.println("cannot open file: "+nam);
            System.out.println("use standard value for impermeable Layer: "+GroundWater2DData.extract(fenet.getDOF(0)).zG);
        }
    }
    
    /**
     * the method readOverlayingStrata read the Thickness for the uppere Impermeable Layer - for strained aquifer
     *  from a file named nam
     *
     * @param nam  name of the file to be open
     */
    public final void readOverlayingStrata(String nam) {

        try {
            InputStream is = new FileInputStream(nam);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                StreamTokenizer st = new StreamTokenizer(r);
                st.eolIsSignificant(true);
                st.commentChar('C');
                
                int anzr = TicadIO.NextInt(st);
                int anzi = TicadIO.NextInt(st);
                int anzk = anzr + anzi;
                if(anzk==fenet.getNumberofDOFs()){
                    System.out.println("\t Read upper Impermeable Layer Thickness from "+nam);
                    for(int j = 0;j< anzk; j++) {
                        int  pos = TicadIO.NextInt(st);
                        TicadIO.NextDouble(st);
                        TicadIO.NextDouble(st);
                        double upperImpermeableLayerThickness  = TicadIO.NextDouble(st);
                        DOF dof=fenet.getDOF(pos);
                        GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                        groundwaterdata.upperImpermeableLayer=dof.z+upperImpermeableLayerThickness;
                    }
                } else System.out.println("system.dat und upperimperLayer.dat different number of nodes");
            }
            
        } catch (Exception e) {
            System.out.println("cannot open file: "+nam);
            System.out.println("use standard value for upper Impermeable Layer Thickness: = 0.");
        }
    }
    
    /** the method eadPermeabilityFromJanetBin read the datas for kf 
     *  from  a JanetBinary-file named filename
     *  @param nam  name of the file to be open */  
    private void readOverlayingStrataFromJanetBin(String filename) {
        int anzAttributes=0;
        double upperImpermeableLayerThickness;        
        
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
            
            System.out.println("\t Read upper Impermeable Layer Thickness from "+filename);
            
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
                    upperImpermeableLayerThickness=bin_in.fbinreaddouble(); 
                    
                    // Plausibilitaetskontrolle
//                    if (Double.isNaN(zG) || zG<=0.)
//                    	hasValidValues=false;
                 
                    DOF dof=fenet.getDOF(nr);                   
                    
                    
                    GroundWater2DData groundwaterdata = GroundWater2DData.extract(dof);
                    groundwaterdata.upperImpermeableLayer=dof.z+upperImpermeableLayerThickness;
                   
                    
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                    
                }
                
                // Abbruch, wenn Netz nicht ok!
                if(!hasValidValues)
                {
                	System.out.println("***                     WARNUNG                       ***");
                	System.out.println("***   Ueberpruefen Sie die Impermeabilitaeten des   ***");
                	System.out.println("***   Impermeabilitaetsnetzes. Das verwendetet Netz hat      ***");
                	System.out.println("***   Knoten mit negativen oder nicht definierten     ***");
                	System.out.println("***   Impermeabilitaeten!                       ***");
                	System.out.println("***   Die Simulation wird nicht fortgesetzt     !           ***");
                	System.exit(0);
                }
                
            } else System.out.println("system und upperImperLayer.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der upperImperLayer-Datei!");
            System.exit(0);
        }
    }
    
    /** Neue Einleseroutine readBoundCond 
     * liest die spezifizierten Datensaetze (Randbedingungen) in der boundary_condition_key_mask 
     * aus entsprechenden Randwertedatei (groundwaterdat.rndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in groundwaterdat.rndwerteReader)
     */
    public final void readBoundCond() {

        String[] boundary_condition_key_mask = new String[1];

        boundary_condition_key_mask[0] = BoundaryCondition.free_surface;

        try {
            for (BoundaryCondition bc : groundwaterdat.rndwerteReader.readBoundaryConditions(boundary_condition_key_mask)) {
                if (bc.boundary_condition_key.equals(BoundaryCondition.free_surface)) {
                    bh.add(bc);
                }
            }
        } catch (Exception e) {
            System.exit(1);
        }

    } // end readBoundCond
    
    
    
    /** the method readBoundCond read the datas for boudary conditions
     * from a file named nam in the BAW-format
     * @param nam  name of the file to be open */
    public void readBoundCondOld(String nam) {
        
        int   pointer=0, q_read=0;
        int anz_identische_Knoten, i1, K;
        int Art_Rndwrt, Art_Rndfkt, Gesamtzahl, Anz_Periode,
                Translation, Spreizung, Ordinaten;
        
        double feld[][];
        int KnotenNr[];
        
        DiscretScalarFunction1d randfkt;
        
        FileInputStream is=null;
        try {       
        
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(nam)));
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('C');
        
        System.out.println("\t read boundary conditions from "+nam);
        
        int anzrndwrt = TicadIO.NextInt(st);
//        System.out.println("anzrndwrt ="+anzrndwrt );
        KnotenNr = new int[anzrndwrt];
        int zeitintervall = TicadIO.NextInt(st);
        
        while(pointer < anzrndwrt){
            //System.out.println("pointer "+pointer+"   anzrndwrt"+anzrndwrt);
            anz_identische_Knoten = TicadIO.NextInt(st);
            
            q_read = 0;
            Art_Rndwrt = TicadIO.NextInt(st);
            
            Art_Rndfkt  = TicadIO.NextInt(st);
            
            Anz_Periode = TicadIO.NextInt(st);
            Gesamtzahl  = TicadIO.NextInt(st);
            
            Translation = TicadIO.NextInt(st);
            Spreizung	= TicadIO.NextInt(st);
            Ordinaten	= TicadIO.NextInt(st);
            
            for ( K = pointer; K < pointer+anz_identische_Knoten; K++ ) {
                i1 = TicadIO.NextInt(st);
                KnotenNr[K] = i1;
                //System.out.print(" "+i1);
            }
            //System.out.println(" ");
            
            feld = new double [2][Anz_Periode+1];
            
            switch(Art_Rndfkt) {
                case 1:
                    break;
                default:        /* 2:linear, 3:spline        */
                    for ( K = 0; K <= Anz_Periode; K++ ) {
                        feld[0][K] = TicadIO.NextDouble(st);
                        feld[1][K] = TicadIO.NextDouble(st);
                        //System.out.println(feld[0][K]+" "+feld[1][K]);
                    }
                    break;
            }
            
            DiscretScalarFunction1d boundcond = new DiscretScalarFunction1d(feld);
            boundcond.setPeriodic(true);
            for ( K = pointer; K < pointer+anz_identische_Knoten; K++ ){
                Point3d pt = (fenet.getDOF(KnotenNr[K]));
//                    System.out.print(KnotenNr[K]+" ");
                switch (Art_Rndwrt){
                    case TicadIO.H_GESETZT  :
//                            System.out.println("H Randbedingung gelesen");
                        bh.addElement( new BoundaryConditionOld(KnotenNr[K], boundcond));
                        break;
                }
            }
            pointer += anz_identische_Knoten;
            
        }
        } catch (Exception e) {}
//        System.out.println("Randwerte gelesen");
    } // end readBoundCond
    
    /** The method write_erg_xf
     * @param erg
     * @param t  */
    @Override
    public void write_erg_xf(double[] erg, double t) {
        if (xf_os != null) {
            try {
                xf_os.writeFloat((float) t);
                for (DOF dof : fenet.getDOFs()) {
                    GroundWater2DData modeldata = GroundWater2DData.extract(dof);
                    int i = dof.number;
                    modeldata.h = erg[H + i];
                    if (MarinaXML.release) {
                        setBoundaryCondition(dof, time);
                    }
                    xf_os.writeFloat((float) (modeldata.u * 1000.));        // v1x
                    xf_os.writeFloat((float) (modeldata.v * 1000.));        // v1y
                    xf_os.writeFloat((float) modeldata.h);        // skalar1
                }
                xf_os.flush();
            } catch (IOException e) {
                System.out.println(this.getClass() + "\n\ttime=" + time + "\n");
                e.printStackTrace();
                System.exit(0);
            }
        }
    } // end write_erg_xf

}
