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
package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.MarinaXML;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.ground.SoilModel3DData;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3D;
import de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.SmileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import de.smile.xml.marina.TSSSFileType;
import java.io.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this class describe a depth integrated morphodynamic model including bottom
 * load, suspened transport and bottom evolution
 * 
 * @author Peter Milbradt
 * @version 4.9.3
 */
public class SedimentModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel {

    public static enum InitialValues {
        D50, DMIN, DMAX, SORTING, MAINTAINEDDEPTH, MAXEROSION, NOERODIBLEHORIZON
    };

    private boolean basedOnCurrentModel3D = false;

    private SedimentModel2DData[] dof_data = null;
    private SedimentElementData[] element_data = null;

    private CurrentModel2DData[] dof_currentdata = null;
    private Current2DElementData[] element_currentdata = null;

    private DataOutputStream xf_os = null;

    private final ArrayList<BoundaryCondition> bsc = new ArrayList<>();
    private final ArrayList<BoundaryCondition> bz = new ArrayList<>();
    private final ArrayList<BoundaryCondition> bd50 = new ArrayList<>();

    private BedLoad2DFormulation bl = null;
    private SuspendedLoad2DFormulation sl = null;

    private final SedimentDat sedimentdat;

    protected static double morphFactor = 1.; // morphological Factor
    @SuppressWarnings("unused")
    private double morphologicalTime; // ist startTime + dt*morphFactor

    private double previousTimeStep = 0.0; // Speichert den vorherigen Zeitschritt fuer das gesamte Modell

    public static double WATT = 0.05; // Wattgrenze des SedimentModell, eigentlich abhaengig von den
                                      // Gueltigkeitsgrenzen der Transportformel

    boolean withSoilModel3DData = false;
    int modulo = 1;
    int wrtInt = 0;

    public SedimentModel2D(CurrentModel2D currentmodel, SedimentDat sedimentdat) {
        System.out.println("SedimentModel2D initialization");
        this.referenceDate = sedimentdat.referenceDate;
        this.epsgCode = sedimentdat.epsgCode;
        fenet = currentmodel.fenet;
        dof_currentdata = currentmodel.dof_data;
        element_currentdata = currentmodel.element_data;

        femodel = this;
        this.sedimentdat = sedimentdat;

        initialSedimentModel2D(sedimentdat);
    }

    public SedimentModel2D(CurrentModel3D currentmodel, SedimentDat sedimentdat) {

        this.basedOnCurrentModel3D = true;

        System.out.println("SedimentModel2D initalization");
        this.referenceDate = sedimentdat.referenceDate;
        this.epsgCode = sedimentdat.epsgCode;
        fenet = currentmodel.fenet;

        // initialization of Current2DDate based on the values of the Current3DModel
        dof_currentdata = new CurrentModel2DData[fenet.getNumberofDOFs()];
        DOF[] doffs = fenet.getDOFs();
        for (DOF doff : doffs) {
            CurrentModel3DData c3dData = CurrentModel3DData.extract(doff);
            int dofnumber = doff.number;
            dof_currentdata[dofnumber] = c3dData.getCurrentModel2DData(null);
            doff.addModelData(dof_currentdata[dofnumber]);
        }
        element_currentdata = new Current2DElementData[fenet.getNumberofFElements()];
        for (FElement felement : fenet.getFElements()) {
            FTriangle ele = (FTriangle) felement;
            element_currentdata[ele.number] = CurrentModel3DData.getCurrent2DElementData(ele, null);
        }
        // end of initialization

        femodel = this;
        this.sedimentdat = sedimentdat;

        initialSedimentModel2D(sedimentdat);
    }

    private void initialSedimentModel2D(SedimentDat sedimentdat) {

        morphFactor = this.sedimentdat.morphFactor;
        // ** SoilModel
        withSoilModel3DData = this.sedimentdat.withSoilModel3DData;
        modulo = this.sedimentdat.modulo;
        if (withSoilModel3DData) {
            System.out.println("\t ** with SoilModel3D ** ");
            System.out.println("\t \t A SoilModel result file is written at every " + modulo + "th time step.");
        }

        dof_data = new SedimentModel2DData[fenet.getNumberofDOFs()];
        element_data = new SedimentElementData[fenet.getNumberofFElements()];

        bl = sedimentdat.bl;
        sl = sedimentdat.sl;

        setNumberOfThreads(this.sedimentdat.NumberOfThreads);

        if (this.sedimentdat.rndwerte_name != null)
            readBoundCond();

        // DOFs initialisieren
        initialDOFs();

        // read maxerosion
        boolean maxerosionSet = false;
        if (sedimentdat.noErodibleHorizonFileName != null && !maxerosionSet) {
            if (sedimentdat.noErodibleHorizonFileType == SmileIO.MeshFileType.SystemDat) {
                readInitalValueFromXF4(sedimentdat.noErodibleHorizonFileName, InitialValues.NOERODIBLEHORIZON);
                maxerosionSet = true;
            }
            if (sedimentdat.noErodibleHorizonFileType == SmileIO.MeshFileType.JanetBin) {
                readInitalValueFromJanetBin(sedimentdat.noErodibleHorizonFileName, InitialValues.NOERODIBLEHORIZON);
                maxerosionSet = true;
            }
        }
        if (!Double.isNaN(sedimentdat.noErodibleHorizon) && !maxerosionSet) {
            for (DOF dof : fenet.getDOFs()) {
                SedimentModel2DData.extract(dof).zh = Function.max(dof.z, sedimentdat.noErodibleHorizon);
            }
            maxerosionSet = true;
        }

        /*
         * Wenn sowohl noErodibleHorizon als auch MaxErosionsTiefe gesetzt wurde nehme
         * das Minimum
         */
        if (sedimentdat.maxErosionFileName != null && maxerosionSet) {
            // if(sedimentdat.maxerosionFileType==SmileIO.MeshFileType.SystemDat){
            // readMaxErosion(sedimentdat.maxErosionFileName);}
            if (sedimentdat.maxerosionFileType == SmileIO.MeshFileType.JanetBin) {
                double[] dz = readMaxErosionFromJanetBinAsdoubleArray(sedimentdat.maxErosionFileName);
                for (int i = 0; i < dz.length; i++) {
                    DOF dof = fenet.getDOF(i);
                    dof_data[i].zh = Math.min(dof_data[i].zh, dof.z + dz[i]);
                }

            }
        }

        if (sedimentdat.maxErosionFileName != null && !maxerosionSet) {
            if (sedimentdat.maxerosionFileType == SmileIO.MeshFileType.SystemDat) {
                readInitalValueFromXF4(sedimentdat.maxErosionFileName, InitialValues.MAXEROSION);
                maxerosionSet = true;
            }
            if (sedimentdat.maxerosionFileType == SmileIO.MeshFileType.JanetBin) {
                readInitalValueFromJanetBin(sedimentdat.maxErosionFileName, InitialValues.MAXEROSION);
                maxerosionSet = true;
            }
        }
        if (!maxerosionSet) {
            System.out.println("\tGlobal maximum erodible depths of " + sedimentdat.MaxErosionsTiefe + " m");
            for (DOF dof : fenet.getDOFs()) {
                SedimentModel2DData.extract(dof).zh = dof.z + sedimentdat.MaxErosionsTiefe;
            }
        }

        /* Erosionstiefen an Wehren auf 0 Setzen */
        FElement[] felement = fenet.getFElements();
        for (int j = 0; j < felement.length; j++) {
            if (element_currentdata[j].withWeir) {
                DOF[] dofs = felement[j].getDOFs();
                for (int k = 0; k < 3; k++) {
                    dof_data[dofs[k].number].zh = dofs[k].z;
                }
            }
        }

        // read MaintainedDepth
        if (sedimentdat.maintainedDepthFileName != null) {
            if (sedimentdat.maintainedDepthFileType == SmileIO.MeshFileType.SystemDat)
                readInitalValueFromXF4(sedimentdat.maintainedDepthFileName, InitialValues.MAINTAINEDDEPTH);
            if (sedimentdat.maintainedDepthFileType == SmileIO.MeshFileType.JanetBin)
                readInitalValueFromJanetBin(sedimentdat.maintainedDepthFileName, InitialValues.MAINTAINEDDEPTH);
        } else {
            for (DOF dof : fenet.getDOFs()) {
                SedimentModel2DData.extract(dof).maintainedDepth = sedimentdat.maintainedDepth;
            }
        }

        if (sedimentdat.singleSSPFileName != null) {
            if (sedimentdat.singleSSPFileType == TSSSFileType.CSV) {
                readInitalSedimentParameterFromCSV(sedimentdat.singleSSPFileName);
            }
        }
        // read d50
        if (sedimentdat.d50FileName != null) {
            if (sedimentdat.d50FileType == SmileIO.MeshFileType.SystemDat)
                readInitalValueFromXF4(sedimentdat.d50FileName, InitialValues.D50);
            if (sedimentdat.d50FileType == SmileIO.MeshFileType.JanetBin)
                readInitalValueFromJanetBin(sedimentdat.d50FileName, InitialValues.D50);
        } else if (sedimentdat.singleSSPFileName == null) {
            System.out.println("\tGlobal d50 = " + sedimentdat.standardD50 + " m");
            for (DOF dof : fenet.getDOFs()) {
                SedimentModel2DData.extract(dof).initialD50(sedimentdat.standardD50);
            }
        }

        // read dmin
        if (sedimentdat.dminFileName != null) {
            if (sedimentdat.dminFileType == SmileIO.MeshFileType.SystemDat)
                readInitalValueFromXF4(sedimentdat.dminFileName, InitialValues.DMIN);
            if (sedimentdat.dminFileType == SmileIO.MeshFileType.JanetBin)
                readInitalValueFromJanetBin(sedimentdat.dminFileName, InitialValues.DMIN);
        } else if (sedimentdat.singleSSPFileName == null) {
            final boolean dminIsNaN = Double.isNaN(sedimentdat.lowerGrainSize);
            if (!dminIsNaN)
                System.out.println("\tGlobal dmin = " + sedimentdat.lowerGrainSize + " m");
            else
                System.out.println("\tGlobal dmin = d50/10");
            for (DOF dof : fenet.getDOFs()) {
                if (!dminIsNaN)
                    SedimentModel2DData.extract(dof).dmin = sedimentdat.lowerGrainSize;
                else {
                    SedimentModel2DData smd = SedimentModel2DData.extract(dof);
                    smd.dmin = smd.d50 / 10.;
                }
            }
        }

        // read dmax
        if (sedimentdat.dmaxFileName != null) {
            if (sedimentdat.dmaxFileType == SmileIO.MeshFileType.SystemDat)
                readInitalValueFromXF4(sedimentdat.dmaxFileName, InitialValues.DMAX);
            if (sedimentdat.dmaxFileType == SmileIO.MeshFileType.JanetBin)
                readInitalValueFromJanetBin(sedimentdat.dmaxFileName, InitialValues.DMAX);
        } else if (sedimentdat.singleSSPFileName == null) {
            final boolean dmaxIsNaN = Double.isNaN(sedimentdat.upperGrainSize);
            if (!dmaxIsNaN)
                System.out.println("\tGlobal dmax = " + sedimentdat.upperGrainSize + " m");
            else
                System.out.println("\tGlobal dmax = d50*10");
            for (DOF dof : fenet.getDOFs()) {
                if (!dmaxIsNaN)
                    SedimentModel2DData.extract(dof).dmax = sedimentdat.upperGrainSize;
                else {
                    SedimentModel2DData smd = SedimentModel2DData.extract(dof);
                    smd.dmax = smd.d50 * 10;
                }
            }
        }

        // read initalSorting
        if (sedimentdat.initalSortingFileName != null) {
            if (sedimentdat.initalSortingFileType == SmileIO.MeshFileType.SystemDat)
                readInitalValueFromXF4(sedimentdat.initalSortingFileName, InitialValues.SORTING);
            if (sedimentdat.initalSortingFileType == SmileIO.MeshFileType.JanetBin)
                readInitalValueFromJanetBin(sedimentdat.initalSortingFileName, InitialValues.SORTING);
        } else if (sedimentdat.singleSSPFileName == null) {
            final boolean initalSortingIsNaN = Double.isNaN(sedimentdat.initalSorting);
            if (!initalSortingIsNaN)
                System.out.println("\tGlobal initalSorting = " + sedimentdat.initalSorting);
            else
                System.out.println("\tGlobal initalSorting = 1");
            for (DOF dof : fenet.getDOFs()) {
                if (!initalSortingIsNaN)
                    SedimentModel2DData.extract(dof).initialSorting = sedimentdat.initalSorting;
                else {
                    SedimentModel2DData.extract(dof).initialSorting = 1.;
                }
            }
        }

        for (DOF dof : fenet.getDOFs()) {
            SedimentModel2DData sdata = SedimentModel2DData.extract(dof);
            sdata.cfs = sedimentdat.csf; // set critical Shields Function
            sdata.initalUpdate();
        }

        // element results initialization
        initialElementModelData();

        // initialisieren der punktbezogenen Gradienten
        initialBottomGradientsAtPoints();

        try {
            xf_os = new DataOutputStream(new FileOutputStream(sedimentdat.xferg_name));
            // Setzen der Ergebnismaske
            TicadIO.write_xf(xf_os, this);
        } catch (FileNotFoundException e) {
            System.out.println("The file " + sedimentdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void initialBottomGradientsAtPoints() {

        // Berechnen der Elementgradienten
        for (FElement felement : fenet.getFElements()) {
            FTriangle element = (FTriangle) felement;
            final double[][] koeffmat = element.getkoeffmat();
            SedimentElementData eleSedimentData = element_data[element.number];
            double dzdx = 0.; // Tiefe
            double dzdy = 0.; // Tiefe
            for (int j = 0; j < 3; j++) {
                DOF dof = element.getDOF(j);
                SedimentModel2DData sedimentmodeldata = dof_data[dof.number];
                dzdx += sedimentmodeldata.z * koeffmat[j][1];
                dzdy += sedimentmodeldata.z * koeffmat[j][2];
            }
            eleSedimentData.dzdx = dzdx;
            eleSedimentData.dzdy = dzdy;
            // caculate Bottomslope
            final double bottomslope = Math.sqrt(dzdx * dzdx + dzdy * dzdy + 1.);
            eleSedimentData.bottomslope = bottomslope;
        }

        // initialisieren der punktbezogenen Gradienten
        for (DOF dof : fenet.getDOFs()) {
            SedimentModel2DData data = dof_data[dof.number];
            for (FElement felem : dof.getFElements()) {
                FTriangle element = (FTriangle) felem;
                SedimentElementData eleSedimentData = element_data[element.number];
                data._bottomslope += eleSedimentData.bottomslope * element.area;
            }
            data.bottomslope = data._bottomslope / (dof.lumpedMass * 3.);
        }
    }

    public SuspendedLoad2DFormulation getSuspendedLoad2DFormulation() {
        return sl;
    }

    public void setSuspendedLoad2DFormulation(SuspendedLoad2DFormulation s) {
        this.sl = s;
    }

    @Override
    public int getTicadErgMask() {
        // Setzen der Ergebnismaske (Tiefe, TotalSedimentTransport (qx,qy) ,
        // Konzentration des suspendierten Sedimentes, Korndurchmesser d_50,
        // DuenenHoehe, DuenenLaengenVektor, Porosity)
        return TicadIO.HRES_Z | TicadIO.HRES_V | TicadIO.HRES_H | TicadIO.HRES_SALT | TicadIO.HRES_EDDY
                | TicadIO.HRES_SHEAR | TicadIO.HRES_AH;
    }

    // ------------------------------------------------------------------------
    // initalSolution
    // ------------------------------------------------------------------------
    public double[] initialSolution(double time) {

        System.out.println("\tinitialization of values");
        DOF[] dof = fenet.getDOFs();
        for (int j = 0; j < dof.length; j++) {

            SedimentModel2DData sedimentmodeldata = dof_data[j];

            sedimentmodeldata.z = dof[j].z;

            sedimentmodeldata.sC = sl.getConcentration(dof[j]);

            if (withSoilModel3DData)
                ((SoilModel3DData) sedimentmodeldata).update(time);
        }

        // initialisieren der punktbezogenen Gradienten
        initialBottomGradientsAtPoints();

        return null;
    }

    public void readInitialSoilModel3DDataFromCVS(String csvFileame) {
        readInitialSoilModel3DDataFromCVS(csvFileame, true);
    }

    public void readInitialSoilModel3DDataFromCVS(String csvFileame, boolean coarsen) {
        System.out.println("\tRead inital SoilModel3DData from " + csvFileame);
        try (BufferedReader reader = Files.newBufferedReader(FileSystems.getDefault().getPath(csvFileame))) {
            reader.readLine(); // Kopfzeile lesen
            String line;
            int biggestDofNumber = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] split = line.split(";");
                    int dofNumber = Integer.parseInt(split[0].trim());
                    biggestDofNumber = Math.max(biggestDofNumber, dofNumber);
                    SoilModel3DData sm3d = (SoilModel3DData) dof_data[dofNumber];
                    SoilModel3DData.LayerValues layer = new SoilModel3DData.LayerValues(line);
                    if (Double.isNaN(layer.sorting))
                        layer.sorting = 1.;
                    if (!Double.isNaN(layer.zl))
                        sm3d.layerValues.push(layer);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                } // wenn z.B. keine Daten an einem Knoten vorliegen
            }
            if (biggestDofNumber == 0) {
                System.out.println("!Initialisierung mit nur einer Schichtvorgabe!");
                SoilModel3DData sm3d_0 = (SoilModel3DData) dof_data[0];
                for (DOF dof : fenet.getDOFs()) {
                    if (dof.number > 0) {
                        SoilModel3DData sm3d = (SoilModel3DData) dof_data[dof.number];
                        try {
                            sm3d.layerValues = sm3d_0.clone().layerValues;
                            if (sm3d.layerValues.size() > 1) {
                                Collections.sort(sm3d.layerValues);
                            }
                            sm3d.update(time);
                        } catch (CloneNotSupportedException ex) {
                            Logger.getLogger(SedimentModel2D.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            } else {
                for (DOF dof : fenet.getDOFs()) {
                    SoilModel3DData sm3d = (SoilModel3DData) dof_data[dof.number];
                    // *** heilen !
                    sm3d.checkANDcorrect();
                    // *** vergroebern !
                    if (coarsen)
                        sm3d.layerValues = sm3d.getCoarsenedLayerValuesStack();

                    sm3d.update(time);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SedimentModel2D.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void writeSoilModel3DData2CVS() {
        writeSoilModel3DData2CVS(true);
    }

    public void writeSoilModel3DData2CVS(boolean coarsen) {
        Path path = FileSystems.getDefault().getPath(sedimentdat.xferg_name);
        // call getParent() to get parent path
        Path parentPath = path.getParent();
        Path confDir = FileSystems.getDefault().getPath(parentPath.toString() + "/" + (int) (time * morphFactor) + "/");
        // if the sub-directory doesn't exist then create it
        if (Files.notExists(confDir)) {
            try {
                Files.createDirectory(confDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // create file-in-subdirectory path
        Path outPath = FileSystems.getDefault().getPath(confDir.toString() + "/soilModel3DData.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(outPath, Charset.defaultCharset(),
                StandardOpenOption.CREATE)) {
            writer.write("nodenumber ; x ; y ; " + SoilModel3DData.LayerValues.getASCIIHeaderLine());
            for (DOF dof : fenet.getDOFs()) {
                final int nr = dof.number;
                SoilModel3DData sm3d = (SoilModel3DData) dof_data[nr];

                // *** heilen !
                sm3d.checkANDcorrect();
                // *** vergroebern !
                if (coarsen)
                    sm3d.layerValues = sm3d.getCoarsenedLayerValuesStack();

                double lastLayerZ = Double.NaN;
                boolean firstLayer = true;
                for (SoilModel3DData.LayerValues layer : sm3d.layerValues) { // Tiefste zu erst, hoechste zu letzte
                    if (firstLayer) {
                        writer.write("\n" + nr + " ; " + dof.x + " ; " + dof.y + " ; " + layer.toString());
                        firstLayer = false;
                    } else {
                        if (!(Math.abs(layer.zl - lastLayerZ) < Double.MIN_NORMAL * 128)) // wenn die Schicht nicht
                                                                                          // doppelt ist
                            writer.write("\n" + nr + " ; " + dof.x + " ; " + dof.y + " ; " + layer.toString());
                    }
                    lastLayerZ = layer.zl;
                }
            }
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(SedimentModel2D.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void writeSoilModel3DData2AVSUCD() {
        Path path = FileSystems.getDefault().getPath(sedimentdat.xferg_name);
        // call getParent() to get parent path
        Path parentPath = path.getParent();
        Path confDir = FileSystems.getDefault().getPath(parentPath.toString() + "/" + (int) (time * morphFactor) + "/");
        // if the sub-directory doesn't exist then create it
        if (Files.notExists(confDir)) {
            try {
                Files.createDirectory(confDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // create file-in-subdirectory path
        Path outPathVolume = FileSystems.getDefault().getPath(confDir.toString() + "/soilModel3DVolume.inp");
        Path outPathLayer = FileSystems.getDefault().getPath(confDir.toString() + "/soilModel3DLayer.inp");
        try (BufferedWriter writerVolume = Files.newBufferedWriter(outPathVolume, Charset.defaultCharset(),
                StandardOpenOption.CREATE);
                BufferedWriter writerLayer = Files.newBufferedWriter(outPathLayer, Charset.defaultCharset(),
                        StandardOpenOption.CREATE)) {

            // String headVolume = punktzahl + " " + prismenzahl + " " + punktzahl + " 0 0";
            // String headLayer = punkte_.size() + " " + dreiecke_.size() + " " +
            // punkte_.size() + " 0 0";
            //
            //
            // for (DOF dof : fenet.getDOFs()) {
            // int nr = dof.number;
            // SoilModel3DData sm3d = (SoilModel3DData) dof_data[nr];
            //// *** Reihenfolge ggf. heilen !
            // if (sm3d.layerValues.size() > 1) {
            // Collections.sort(sm3d.layerValues);
            // }
            //// *** Reihenfolge geheilen !
            // for (SoilModel3DData.LayerValues layer : sm3d.layerValues) { // Tiefste zu
            // erst, hoechste zu letzte
            // writerVolume.write("\n" + nr + ";" + layer.toString());
            // }
            // }
            writerVolume.flush();
            writerLayer.flush();
        } catch (IOException ex) {
            Logger.getLogger(SedimentModel2D.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * initialisiert die Sedimentkonzentration mit einem konstanten Wert
     * Tiefen aus den DOFs
     * 
     * @param initalvalue
     * @return
     */
    public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value " + initalvalue + " [g/l]");

        DOF[] dof = fenet.getDOFs();

        for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
            dof_data[i].z = dof[i].z;
            dof_data[i].sC = initalvalue / PhysicalParameters.RHO_SEDIM;

            if (withSoilModel3DData)
                ((SoilModel3DData) dof_data[i]).update(time);
        }

        // initialisieren der punktbezogenen Gradienten
        initialBottomGradientsAtPoints();

        return null;
    }

    /**
     * Read the start solution from file
     * 
     * @param sedimentergPath file with simulation results
     * @param record          record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    public double[] initialSolutionFromTicadErgFile(String sedimentergPath, int record) throws Exception {

        System.out.println("\tRead inital values from result file " + sedimentergPath);
        // erstes Durchscannen
        File sysergFile = new File(sedimentergPath);
        try (FileInputStream stream = new FileInputStream(sysergFile);
                DataInputStream inStream = new DataInputStream(stream)) {

            // Kommentar lesen, bis ASCII-Zeichen 7 kommt
            int c;
            do {
                c = inStream.readByte();
            } while (c != 7);
            // Ende Kommentar

            // Anzahl Elemente, Knoten und Rand lesen
            int anzKnoten = inStream.readInt();
            if (fenet.getNumberofDOFs() != anzKnoten) {
                System.out.println("Die Datei mit den Startwerten hat eine andere Anzahl von Knoten");
                System.exit(1);
            }
            int anzr = inStream.readInt();
            int anzElemente = inStream.readInt();

            // Ueberlesen folgende Zeilen
            inStream.skip(9 * 4);

            // Ergebnismaske lesen und auswerten
            final int ergMaske = inStream.readInt();
            final int anzWerte = TicadIO.ergMaskeAuswerten(ergMaske);

            final boolean None_gesetzt = ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE);
            final boolean Pos_gesetzt = ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS);
            final boolean Z_gesetzt = ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z);
            final boolean V_gesetzt = ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V);
            final boolean Q_gesetzt = ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q);
            final boolean C_gesetzt = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);
            final boolean D50_gesetzt = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);
            final boolean DUNEHight_gesetzt = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);
            final boolean DUNELength_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR);
            final boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL);
            final boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
            final boolean POROSITY_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);

            inStream.readInt();

            // Elemente, Rand und Knoten Ueberlesen
            inStream.skip((anzElemente * 4L + anzr + 3L * anzKnoten) * 4L); // 4 Bytes je float und int

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4L) * record);

            @SuppressWarnings("unused")
            float t = inStream.readFloat();
            for (int i = 0; i < fenet.getNumberofDOFs(); i++) {

                if (None_gesetzt)
                    inStream.skip(4);

                if (Pos_gesetzt)
                    inStream.skip(4);

                if (Z_gesetzt) {
                    dof_data[i].z = inStream.readFloat();
                    CurrentModel2DData currentdata = CurrentModel2DData.extract(fenet.getDOF(i));
                    currentdata.setBottomLevel(dof_data[i].z);
                }

                if (V_gesetzt) // Sedimenttransportrate
                    inStream.skip(8);

                if (Q_gesetzt) // nicht gesetzt
                    inStream.skip(8);

                if (C_gesetzt) {
                    dof_data[i].sC = inStream.readFloat() / PhysicalParameters.RHO_SEDIM;
                }

                if (D50_gesetzt) {
                    dof_data[i].d50 = inStream.readFloat() / 1000.;
                }

                if (DUNEHight_gesetzt)
                    dof_data[i].duneHeight = Function.max(0., inStream.readFloat());

                if (DUNELength_gesetzt) {
                    dof_data[i].duneLengthX = inStream.readFloat();
                    dof_data[i].duneLengthY = inStream.readFloat();
                }

                if (V_SCAL_gesetzt)
                    inStream.skip(4);

                if (Q_SCAL_gesetzt)
                    inStream.skip(4);

                if (POROSITY_gesetzt)
                    dof_data[i].porosity = inStream.readFloat() / 100.;
            }
        }

        // initialisieren der punktbezogenen Gradienten
        initialBottomGradientsAtPoints();

        if (withSoilModel3DData)
            for (DOF dof : fenet.getDOFs()) {
                SoilModel3DData.extract(dof).update(time);
            }

        return null;
    }

    /**
     * the method initialConcentrationFromSysDat read the datas for sC
     * from a sysdat-file named filename
     * 
     * @param filename name of the file to be open
     * @param time
     * @return
     * @throws java.lang.Exception
     */
    // private final void readConcentrationDat(String filename) {
    public double[] initialConcentrationFromSysDat(String filename, double time) throws Exception {
        this.time = time;
        int knoten_nr;

        double skonc;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading Concentration-File (in TiCAD-System.Dat-Format): " + filename);

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));
            java.util.StringTokenizer strto = new StringTokenizer(line, " \t\n\r\f,");
            final int rand_knoten = Integer.parseInt(strto.nextToken());

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));

            strto = new StringTokenizer(line, " \t\n\r\f,");
            final int gebiets_knoten = Integer.parseInt(strto.nextToken());

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000)
                throw new Exception("Fehler");

            // System.out.println(""+rand_knoten+" "+gebiets_knoten);

            // Knoten einlesen
            int p_count = 0;
            while (p_count < (rand_knoten + gebiets_knoten)) {
                line = systemfile.freadLine();
                strto = new StringTokenizer(line, " \t\n\r\f,");

                // System.out.println(""+line+"\n");
                if (!line.startsWith("C")) {
                    knoten_nr = Integer.parseInt(strto.nextToken());
                    strto.nextToken();
                    strto.nextToken();
                    try {
                        skonc = Double.parseDouble(strto.nextToken()) / PhysicalParameters.RHO_SEDIM;
                    } catch (NumberFormatException ex) {
                        skonc = Double.NaN;
                    }

                    if (Double.isNaN(skonc) || skonc < 0) {

                        System.out.println("");

                        System.out.println(
                                "********************************       ERROR         ***********************************");
                        System.out.println("Invalid skonc-value (skonc=NaN or skonc<0.0) in Concentration-File: <"
                                + filename + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count
                                + "> has a correct floating point (greater zero)");
                        System.out.println("Concentration  value");
                        System.out.println(
                                "*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                    DOF dof = fenet.getDOF(knoten_nr);
                    SedimentModel2DData.extract(dof).sC = skonc;
                    dof_data[knoten_nr].z = dof.z;
                    dof_data[knoten_nr].sC = skonc;

                    p_count++;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Sedimentkonzentration-Datei!");
            System.exit(0);
        }

        return null;

    }

    /**
     * the method readConcentrationFromJanetBin read the datas for sC
     * from a JanetBinary-file named filename
     * 
     * @param filename name of the file to be open
     * @param time
     * @return
     * @throws java.lang.Exception
     */
    public double[] initialConcentrationFromJanetBin(String filename, double time) throws Exception {
        @SuppressWarnings("unused")
        int anzAttributes = 0;
        double skonc;

        boolean hasValidValues = true;
        int nr;
        @SuppressWarnings("unused")
        short status, kennung;
        @SuppressWarnings("unused")
        int anzPolys, anzEdges, anzPoints = 0, pointsize, trisize, swapMode;
        @SuppressWarnings("unused")
        short sets;
        @SuppressWarnings("unused")
        boolean active, protectBorder, protectConstraints, noPolygon, inPolygon, makeHoles, processFlagsActive;
        @SuppressWarnings("unused")
        boolean noZoom, inZoom, noActive, processActive, processSelected, inPolygonProp, inZoomProp, protectInnerPoints;
        @SuppressWarnings("unused")
        boolean noSelected, closed;
        @SuppressWarnings("unused")
        boolean read_status_byte = false;

        FileIO bin_in = new FileIO();

        try {
            bin_in.fopenbinary(filename, FileIO.input);

            // Netz aus einer Binaerdatei lesen

            // Version auslesen
            float version = bin_in.fbinreadfloat();
            if (version < 1.5f) {
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version
                        + ", current version: 1.8");
            }

            if (version < 1.79)
                read_status_byte = true;

            System.out.println("\tRead Concentration-File from " + filename);

            // zunaechst den FileHeader lesen
            boolean writePointNumbers = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writePointAttributes = bin_in.fbinreadboolean();
            anzAttributes = bin_in.fbinreadint();
            boolean writePointStatus = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeConstraintPolygons = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeConstraintEdges = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeElements = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeElementNumbers = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeElementKennung = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeAlphaTestRadius = bin_in.fbinreadboolean();

            // Layertyp ueberlesen
            int filetype = bin_in.fbinreadint();
            // liegt UnTRIM-Gitetr mit diskreten Kantentiefen vor??
            @SuppressWarnings("unused")
            boolean is_untrim = (filetype == 2);

            // Anzahl der Punkte lesen
            int anzk = bin_in.fbinreadint();
            if (anzk == fenet.getNumberofDOFs()) {

                // Punkte lesen
                for (int i = 0; i < anzk; i++) {
                    // Punktnummer lesen
                    if (writePointNumbers)
                        nr = bin_in.fbinreadint();
                    else
                        nr = i;

                    // x,y,s lesen
                    bin_in.fbinreaddouble();
                    bin_in.fbinreaddouble();
                    skonc = bin_in.fbinreaddouble() / PhysicalParameters.RHO_SEDIM;

                    // Plausibilitaetskontrolle
                    if (Double.isNaN(skonc) || skonc < 0.)
                        hasValidValues = false;

                    DOF dof = fenet.getDOF(nr);
                    SedimentModel2DData.extract(dof).sC = skonc;
                    dof_data[nr].z = dof.z;
                    dof_data[nr].sC = skonc;

                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();

                }

                // Abbruch, wenn Netz nicht ok!
                if (!hasValidValues) {
                    System.out.println("***                     WARNUNG                       ***");
                    System.out.println("***   Ueberpruefen Sie die Konzentration-Werte des   ***");
                    System.out.println("***   Konzentrationnetzes. Das verwendetet Netz hat      ***");
                    System.out.println("***   Knoten mit negativen oder nicht definierten     ***");
                    System.out.println("***   Konzentrationen!                       ***");
                    System.out.println("***   Die Simulation wird nicht fortgesetzt                 ***");
                    System.exit(0);
                }

            } else
                System.out.println("system und concentration.jbf different number of nodes");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der Sedimentkonzentration-Datei!");
            System.exit(0);
        }

        return null;
    }

    // ------------------------------------------------------------------------
    // getRateofChange
    // ------------------------------------------------------------------------
    @Deprecated
    @Override
    public double[] getRateofChange(double time, double x[]) {

        return null;

    } // end getRateofChange

    // ------------------------------------------------------------------------
    // ElementApproximation
    // ------------------------------------------------------------------------
    @Override
    public final double ElementApproximation(FElement element) {

        double timeStep = Double.POSITIVE_INFINITY;

        final Current2DElementData eleCurrentData = element_currentdata[element.number];
        SedimentElementData eleSedimentData = element_data[element.number];
        final FTriangle ele = (FTriangle) element;

        if (!eleCurrentData.isDry) {

            final double[][] koeffmat = ele.getkoeffmat();

            final double[] terms_C = new double[3];
            final double[] terms_z = new double[3];

            // Current - verschwenkt
            double u_mean = 0.;
            double v_mean = 0.;

            double porosity_mean = 0.;

            final double astx = eleCurrentData.astx;
            final double asty = eleCurrentData.asty;

            // sediment
            double dskoncdx = 0.;
            double dskoncdy = 0.;
            double dzdx = 0.; // Tiefeableitung nach x
            double dzdy = 0.; // Tiefeableitung nach y

            double dQxdx = 0.; // Ableitung des totalen Sedimenttransportes
            double dQydy = 0.;

            double eleEro = 1.; // neuer Indikator mit Test der Turbulenzterme ob diese an einem Knoten
                                // erodieren wollen der aber nichterodierbar ist
            double eleSed = 1.;
            double morph_x = 0.;
            double morph_y = 0.;

            // compute element derivations
            // -------------------------------------------------------------------
            for (int j = 0; j < 3; j++) {
                final SedimentModel2DData smd = dof_data[ele.getDOF(j).number];
                final CurrentModel2DData cmd = dof_currentdata[ele.getDOF(j).number];

                porosity_mean += smd.porosity / 3.;

                dzdx += smd.z * koeffmat[j][1];
                dzdy += smd.z * koeffmat[j][2];

                u_mean += 1. / 3. * cmd.u;
                v_mean += 1. / 3. * cmd.v;

                dskoncdx += smd.sC * koeffmat[j][1];
                dskoncdy += smd.sC * koeffmat[j][2];

                morph_x += 1. / 3. * smd.u_bank; // mittlere Geschwindigkeit mit der sich Sedimentpakete bewegen
                morph_y += 1. / 3. * smd.v_bank;

                dQxdx += smd.qTotal_x * koeffmat[j][1];
                dQydy += smd.qTotal_y * koeffmat[j][2];

                // Indikator fuer nichterodierbare // oder teiltrockene Elemente bestimmen
                eleEro *= smd.lambda;
                eleSed *= cmd.wlambda;

            } // end for

            eleSedimentData.dzdx = dzdx;
            eleSedimentData.dzdy = dzdy;
            // caculate Bottomslope
            final double bs2 = dzdx * dzdx + dzdy * dzdy; // bs2 = (tan(beta))**2 bzw. beta = Math.atan(Math.sqrt(dzdx *
                                                          // dzdx + dzdy * dzdy))
            final double bs = Math.sqrt(bs2);
            final double bottomslope = bs + 1.;
            eleSedimentData.bottomslope = bottomslope;

            final double slope_norm = 2 * bs / (bs2 + 1); // zur Berechnung des gravitationellen Transportes

            final double dQgd = dQxdx + dQydy;

            double lambda_x = -1. / (1. - porosity_mean) * dQgd * dzdx / bs2 * eleEro;
            double lambda_y = -1. / (1. - porosity_mean) * dQgd * dzdy / bs2 * eleEro;
            lambda_x = lambda_x * eleSed + (1 - eleSed) * morph_x;
            lambda_y = lambda_y * eleSed + (1 - eleSed) * morph_y;
            final double uG = 1.E-7;
            final double oG = 1.E-6;
            if (bs2 < oG) {
                if (bs2 > uG) { // Morphen
                    final double lambda = (bs2 - uG) / (oG - uG);
                    lambda_x = lambda * lambda_x + (1 - lambda) * morph_x;
                    lambda_y = lambda * lambda_y + (1 - lambda) * morph_y;
                } else {
                    lambda_x = morph_x;
                    lambda_y = morph_y;
                }
            }

            final double current_mean = Function.norm(u_mean, v_mean);
            double localResC = 0., localResZTransport = 0.;

            double nu_sed = 0.;

            // Elementfehler berechnen
            for (int j = 0; j < 3; j++) {
                final int i = ele.getDOF(j).number;
                final SedimentModel2DData smd = dof_data[i];
                final CurrentModel2DData cmd = dof_currentdata[i];

                terms_C[j] = (cmd.u * dskoncdx + cmd.v * dskoncdy)
                // in timeStep - stoert die Fehlerkorrektur
                // - smd.sedimentSource
                ;
                localResC += 1. / 3. * (smd.dsCdt + terms_C[j]) * cmd.wlambda; // mean local elementresiduum

                terms_z[j] = -1. / (1. - smd.porosity) * (dQgd < 0 ? dQgd * cmd.wlambda : dQgd * smd.lambda);
                localResZTransport += 1. / 3. * (smd.dzTransportdt + terms_z[j]); // mean local elementresiduum
                // fuer gravitioneller Transport, herunter rollern mit max. wc/4 inklusive
                // Projektion in die Ebene
                nu_sed = Math.sqrt(smd.wc / 4.0 * smd.d50 * slope_norm * smd.bedload) * smd.lambda / 3.;
            }
            final double morph_mean = Function.norm(lambda_x, lambda_y);
            double tau_z;
            if (morph_mean > 1.E-7) {
                tau_z = 0.5 * ele.getVectorSize(lambda_x, lambda_y) / morph_mean;
                timeStep = Math.min(timeStep, tau_z);
            } else
                tau_z = 0.;

            final double tauC;
            if (current_mean > WATT / 10. / 3.) {
                tauC = 0.5 * eleCurrentData.elementsize / current_mean;
                timeStep = Math.min(timeStep, tauC);
            } else
                tauC = 0.;

            for (int j = 0; j < 3; j++) {
                final int i = ele.getDOF(j).number;
                SedimentModel2DData smd = dof_data[i];
                final CurrentModel2DData cmd = dof_currentdata[i];
                // Fehlerkorrektur C
                double sKoncCorrect = -tauC * (koeffmat[j][1] * u_mean + koeffmat[j][2] * v_mean) * localResC;
                // Diffusionsterm (entspricht ∇⋅(D∇c))
                sKoncCorrect -= (koeffmat[j][1] * astx * dskoncdx + koeffmat[j][2] * asty * dskoncdy) * cmd.wlambda
                        // KORREKTURTERM fuer variable Wassertiefe (entspricht 1/d * (D∇d)⋅(∇c)):
                        // korrigiert die Diffusion, wenn sich die Tiefe aendert.
                        - 1. / 3. * (1. / Function.max(cmd.totaldepth, CurrentModel2D.WATT))
                                * (eleCurrentData.ddepthdx * astx * dskoncdx
                                        + eleCurrentData.ddepthdy * asty * dskoncdy)
                                * cmd.wlambda;

                // Fehlerkorrektur Z
                double resCorrect = -tau_z * (koeffmat[j][1] * lambda_x + koeffmat[j][2] * lambda_y)
                        * localResZTransport;
                // gravitioneller Transport, herunter rollern mit max. wc/4 inklusive Projektion
                // in die Ebene
                resCorrect -= (koeffmat[j][1] * dzdx + koeffmat[j][2] * dzdy) * nu_sed;

                double result_Z_i = 0;
                double result_SKonc_i = 0.;
                for (int l = 0; l < 3; l++) {
                    final double vorfak = ele.area * ((l == j) ? 1. / 6. : 1. / 12.);

                    double gl = (l == j) ? 1.
                            : Math.min(dof_currentdata[ele.getDOF(l).number].wlambda,
                                    dof_currentdata[ele.getDOF(l).number].totaldepth
                                            / Math.max(CurrentModel2D.WATT, cmd.totaldepth));
                    result_SKonc_i -= vorfak * terms_C[l] * gl;

                    gl = 1.;
                    if (l != j) { // ToDo siehe Marina-Version 2.8.9
                        if (terms_z[l] > 0) { // abgelegener Knoten sedimentiert
                            if (dof_data[ele.getDOF(l).number].z > smd.z) { // abgelegener Knoten liegt unterhalb
                                gl = cmd.wlambda * Function.max(0.,
                                        1. - (dof_data[ele.getDOF(l).number].z - smd.z) / ele.distance[l][j]);
                            } else { // abgelegener Knoten liegt oberhalb
                                gl = dof_currentdata[ele.getDOF(l).number].wlambda;
                            }
                        } else { // abgelegener Knoten erodiert
                            if (dof_data[ele.getDOF(l).number].z > smd.z) { // abgelegener Knoten liegt unterhalb
                                gl = smd.lambdaQs;
                            } else { // abgelegenen Knoten liegt oberhalb
                                gl = smd.lambdaQs * dof_data[ele.getDOF(l).number].lambda * Function.max(0.,
                                        1. - (smd.z - dof_data[ele.getDOF(l).number].z) / ele.distance[l][j]);
                            }
                        }
                    }
                    result_Z_i -= vorfak * terms_z[l] * gl;
                }

                synchronized (smd) {
                    smd.rC += result_SKonc_i;
                    smd.rZTransport += result_Z_i;

                    smd._bottomslope += bottomslope * ele.area;
                    smd.rZCorrect += resCorrect * ele.area / 3.;
                    smd.rSKoncCorrect += sKoncCorrect * ele.area / 3.;

                    // Test ob der Knoten tiefster / hoester im Patch ist
                    smd._isDeepest &= ((smd.z > dof_data[ele.getDOF((j + 1) % 3).number].z + smd.bound)
                            && (smd.z > dof_data[ele.getDOF((j + 2) % 3).number].z + smd.bound));
                    smd._isHighest &= ((smd.z < dof_data[ele.getDOF((j + 1) % 3).number].z - smd.bound)
                            && (smd.z < dof_data[ele.getDOF((j + 2) % 3).number].z - smd.bound));
                }

            }
        } else {
            // dry element
            for (int j = 0; j < 3; j++) {
                SedimentModel2DData smd = dof_data[element.getDOF(j).number];
                synchronized (smd) {
                    smd._bottomslope += eleSedimentData.bottomslope * ele.area;
                }
            }
        }

        return timeStep;
    } // end ElementApproximation

    // ------------------------------------------------------------------------
    // setBoundaryCondition
    // ------------------------------------------------------------------------
    @Override
    public final void setBoundaryCondition(DOF dof, double t) {

        final int i = dof.number;
        SedimentModel2DData smd = dof_data[i];

        final CurrentModel2DData cmd = dof_currentdata[i];

        smd.u = cmd.u;
        smd.v = cmd.v;
        smd.cv = cmd.cv;
        if (!basedOnCurrentModel3D) {
            final double normTauB = Function.norm(cmd.tauBx, cmd.tauBy);
            if (normTauB > 1.E-4) { // Verschwenken der resultirerenden Geschwindigkeiten auf Grund der sekundaer
                                    // Stroemung
                final double lambda = Function.min(1,
                        Function.norm(cmd.tau_bx_extra, cmd.tau_by_extra) / (smd.grainShearStress)); // ??? ToDo
                smd.u = (1. - lambda) * smd.u + lambda * cmd.tauBx / normTauB * cmd.cv;
                smd.v = (1. - lambda) * smd.v + lambda * cmd.tauBy / normTauB * cmd.cv;
            }
        }

        smd.rC = 0.;
        smd.rZTransport = 0.;

        if (smd.bz != null) {
            smd.z = smd.bz.getValue(t);
        } else if (cmd.boundary) {
            smd.z = dof.z;
        }

        if (smd.bd50 != null)
            smd.d50 = smd.bd50.getValue(t);

        if (smd.maintainedDepth > smd.z && smd.maintainedDepth < smd.zh && cmd.totaldepth > CurrentModel2D.WATT) { // einfache
                                                                                                                   // Variante
                                                                                                                   // des
                                                                                                                   // Baggern
                                                                                                                   // //
                                                                                                                   // ToDo
                                                                                                                   // Die
                                                                                                                   // Einschraenkung
                                                                                                                   // auf
                                                                                                                   // nass
                                                                                                                   // Knoten
                                                                                                                   // ist
                                                                                                                   // DOOF
            final double dredge = (smd.maintainedDepth - smd.z);
            final double lambda = Function.max(0., 1. - dredge / Function.max(smd.bound, smd.duneHeight));
            smd.duneLengthX *= lambda;
            smd.duneLengthY *= lambda;
            smd.duneHeight = Function.max(0., smd.duneHeight - dredge);
            smd.d50 -= Math.min(1., dredge) * (smd.d50 - smd.d50init); // beim Baggern wird die Korngroesze auf die
                                                                       // initiale zurueck verschoben
            smd.z = smd.maintainedDepth;
        }

        // prevention of negative duneHeight
        if (smd.duneHeight <= 0.) {
            smd.duneHeight = 0.;
            smd.duneLengthX = 0.;
            smd.duneLengthY = 0.;
        }
        smd.duneLength = Function.norm(smd.duneLengthX, smd.duneLengthY);

        if (smd.zh <= smd.z)
            smd.z = smd.zh;
        smd.lambda = Function.min(1, Function.max(0., smd.zh - smd.z) / smd.bound);

        smd.bedloadVector = bl.getLoadVector(dof);

        if (smd.bconc != null) {
            smd.sC = smd.bconc.getValue(t);
            // smd.dsCdt = smd.bconc.getDifferential(t);
        }

        /* prevention of negative concentration */
        if (smd.sC < 0.)
            smd.sC = 0.;

        smd.sedimentSource = getSourceSunk(dof, smd.sC, sl.getConcentration(dof));

        smd.qsx = smd.sC * cmd.u * cmd.totaldepth; // suspendet Load in Richtung der tiefenintegrierten
                                                   // Stroemungsgeschwindigkeit
        smd.qsy = smd.sC * cmd.v * cmd.totaldepth;

        // totaler Sedimenttransport
        smd.qTotal_x = (smd.bedloadVector[0] + smd.qsx);
        smd.qTotal_y = (smd.bedloadVector[1] + smd.qsy);

        smd.u_bank = 1. / (1. - smd.porosity) * smd.qTotal_x / Math.max(cmd.totaldepth, 0.1) * cmd.wlambda;
        smd.v_bank = 1. / (1. - smd.porosity) * smd.qTotal_y / Math.max(cmd.totaldepth, 0.1) * cmd.wlambda;

        smd.lambdaQs = Function.min(1., (smd.bedload + Function.norm(smd.qsx, smd.qsy)) / (1.E-5 * smd.bound));

        // tau = rho * uStar**2
        // uStar = u_mean * 1/7 * (d50/d)**(1/7)
        // tau = rho * 1/49 * (d50/d)**(2/7) * u_mean * u_mean // Solsby
        smd.grainShearStress = 1. / 49. * Math.pow(smd.d50 / Math.max(CurrentModel2D.WATT, cmd.totaldepth), 2. / 7.)
                * smd.cv;
        // Duenenhoehen beruecksichtigen einfacher Ansatz Van Rijn (1984b) und Engelund
        // & Fredsøe (1982)
        smd.grainShearStress *= (1 + 2.5 * smd.duneHeight / Math.max(0.01, smd.duneLength));

    }

    // ------------------------------------------------------------------------
    // genData
    // ------------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof) {
        SedimentModel2DData data = (withSoilModel3DData ? new SoilModel3DData() : new SedimentModel2DData()); // Peter
                                                                                                              // 26.05.2020
        int dofnumber = dof.number;
        dof_data[dofnumber] = data;
        data.bottomslope = 1.;

        for (BoundaryCondition bcond : bsc) {
            if (dofnumber == bcond.pointnumber) {
                data.bconc = bcond.function;
                bsc.remove(bcond);
                break;
            }
        }

        for (BoundaryCondition bcond : bz) {
            if (dofnumber == bcond.pointnumber) {
                data.bz = bcond.function;
                bz.remove(bcond);
                break;
            }
        }

        for (BoundaryCondition bcond : bd50) {
            if (dofnumber == bcond.pointnumber) {
                data.bd50 = bcond.function;
                bd50.remove(bcond);
                break;
            }
        }

        // ToDo alle randknoten suchen - Christoph fragen
        if (dofnumber < ((FTriangleMesh) fenet).anzr) {
            if (data.bz == null) {
                data.extrapolate_z = true;
            }
            if (data.bconc == null) {
                data.extrapolate_conc = true;
            }
        }

        return data;
    }

    /**
     * the method readMaxErosion read the datas for maxerosion depth
     * from a JanetBinary-file named filename
     * 
     * @param nam name of the file to be open
     */
    private double[] readMaxErosionFromJanetBinAsdoubleArray(String filename) {
        double[] rvalue = null;

        @SuppressWarnings("unused")
        int anzAttributes = 0;
        @SuppressWarnings("unused")
        double x, y, dz;

        boolean hasValidValues = true;
        @SuppressWarnings("unused")
        int nr;
        @SuppressWarnings("unused")
        short status, kennung;
        @SuppressWarnings("unused")
        int anzPolys, anzEdges, anzPoints = 0, pointsize, trisize, swapMode;
        @SuppressWarnings("unused")
        short sets;
        @SuppressWarnings("unused")
        boolean active, protectBorder, protectConstraints, noPolygon, inPolygon, makeHoles, processFlagsActive;
        @SuppressWarnings("unused")
        boolean noZoom, inZoom, noActive, processActive, processSelected, inPolygonProp, inZoomProp, protectInnerPoints;
        @SuppressWarnings("unused")
        boolean noSelected, closed;
        @SuppressWarnings("unused")
        boolean read_status_byte = false;

        FileIO bin_in = new FileIO();

        try {
            bin_in.fopenbinary(filename, FileIO.input);

            // Netz aus einer Binaerdatei lesen

            // Version auslesen
            float version = bin_in.fbinreadfloat();
            if (version < 1.5f) {
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version
                        + ", current version: 1.8");
            }

            if (version < 1.79)
                read_status_byte = true;

            System.out.println("\tRead MaxErosionDepth-File from " + filename);

            // zunaechst den FileHeader lesen
            boolean writePointNumbers = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writePointAttributes = bin_in.fbinreadboolean();
            anzAttributes = bin_in.fbinreadint();
            boolean writePointStatus = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeConstraintPolygons = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeConstraintEdges = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeElements = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeElementNumbers = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeElementKennung = bin_in.fbinreadboolean();
            @SuppressWarnings("unused")
            boolean writeAlphaTestRadius = bin_in.fbinreadboolean();

            // Layertyp ueberlesen
            int filetype = bin_in.fbinreadint();
            // liegt UnTRIM-Gitetr mit diskreten Kantentiefen vor??
            @SuppressWarnings("unused")
            boolean is_untrim = (filetype == 2);

            // Anzahl der Punkte lesen
            int anzk = bin_in.fbinreadint();
            rvalue = new double[anzk];
            if (anzk == fenet.getNumberofDOFs()) {

                // Punkte lesen
                for (int i = 0; i < anzk; i++) {
                    // Punktnummer lesen
                    if (writePointNumbers)
                        nr = bin_in.fbinreadint();
                    else
                        nr = i;

                    // x,y,s lesen
                    x = bin_in.fbinreaddouble();
                    y = bin_in.fbinreaddouble();
                    dz = bin_in.fbinreaddouble(); // -> dz
                    rvalue[i] = dz;
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(dz) || dz < 0.)
                        hasValidValues = false;

                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();

                }

                // Abbruch, wenn Netz nicht ok!
                if (!hasValidValues) {
                    System.out.println("***                     WARNUNG                                ***");
                    System.out.println("***   Ueberpruefen Sie die maximalen erodierbaren Tiefen des   ***");
                    System.out.println("***   MaxErosionsnetzes. Das verwendetet Netz hat              ***");
                    System.out.println("***   Knoten mit negativen oder nicht definierten              ***");
                    System.out.println("***   maximalen erodierbaren Tiefen!                           ***");
                    System.out.println("***   Die Simulation wird fortgesetzt                          ***");
                    System.exit(0);
                }

            } else {
                System.out.println("system und maxerosion.jbf different number of nodes");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der MaxErosion-Datei!");
            System.exit(0);
        }
        return rvalue;
    }

    /**
     * the method readDuneHightFromSysDat read the DuneHight
     * from a sysdat-file named filename
     * 
     * @param filename name of the file to be open
     */
    public void readDuneHightFromSysDat(String filename) {
        int knoten_nr;

        @SuppressWarnings("unused")
        double x, y, dh;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading DuneHight-File (in TiCAD-System.Dat-Format): " + filename);

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));
            java.util.StringTokenizer strto = new StringTokenizer(line, " \t\n\r\f,");
            final int rand_knoten = Integer.parseInt(strto.nextToken());

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));

            strto = new StringTokenizer(line, " \t\n\r\f,");
            final int gebiets_knoten = Integer.parseInt(strto.nextToken());

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000)
                throw new Exception("Fehler");

            if ((rand_knoten + gebiets_knoten) != fenet.getNumberofDOFs()) {
                System.out.println("system und duneHight.dat different number of nodes");
                System.exit(0);
            }

            // Knoten einlesen
            int p_count = 0;
            while (p_count < (rand_knoten + gebiets_knoten)) {
                line = systemfile.freadLine();
                strto = new StringTokenizer(line, " \t\n\r\f,");

                // System.out.println(""+line+"\n");
                if (!line.startsWith("C")) {
                    knoten_nr = Integer.parseInt(strto.nextToken());
                    x = Double.parseDouble(strto.nextToken());
                    y = Double.parseDouble(strto.nextToken());
                    try {
                        dh = Double.parseDouble(strto.nextToken());
                    } catch (NumberFormatException ex) {
                        dh = Double.NaN;
                    }

                    if (Double.isNaN(dh) || dh < 0) {

                        System.out.println("");

                        System.out.println(
                                "********************************       ERROR         ***********************************");
                        System.out.println("Invalid DuneHight-value (dz=NaN or dz<0.0) in DuneHight-File: <" + filename
                                + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count
                                + "> has a correct floating point (greater zero)");
                        System.out.println("DuneHight value");
                        System.out.println(
                                "*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                    SedimentModel2DData smd = dof_data[knoten_nr];
                    smd.duneHeight = dh;
                    CurrentModel2DData currentmodeldata = dof_currentdata[knoten_nr];
                    double dunehight = SedimentModel2DData.getvanRijnDuneHeight(smd.d50,
                            PhysicalParameters.RHO_WATER * smd.grainShearStress * currentmodeldata.cv,
                            currentmodeldata.totaldepth, smd.bottomslope);
                    double dhSource = smd.duneHeight / Function.max(0.01, Function.max(dunehight, smd.duneHeight));
                    smd.duneLength = SedimentModel2DData.getFlemmingDuneLength(dunehight) * dhSource;
                    smd.duneLengthX = smd.duneLength * currentmodeldata.u / (Function.max(currentmodeldata.cv, 0.001));
                    smd.duneLengthY = smd.duneLength * currentmodeldata.v / (Function.max(currentmodeldata.cv, 0.001));

                    p_count++;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der duneHight-Datei!");
            System.exit(0);
        }

    }

    /**
     * the method readDuneHightFromJanetBin read the datas for DuneHight
     * from a JanetBinary-file named filename
     * 
     * @param filename name of the file to be open
     */

    @SuppressWarnings("unused")
    public void readDuneHightFromJanetBin(String filename) {
        int anzAttributes = 0;
        double x, y, dh;
        boolean hasValidValues = true;
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
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version
                        + ", current version: 1.8");
            }

            if (version < 1.79)
                read_status_byte = true;

            System.out.println("\tRead duneHight-File from " + filename);

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
                    if (writePointNumbers)
                        nr = bin_in.fbinreadint();
                    else
                        nr = i;

                    // x,y,s lesen
                    x = bin_in.fbinreaddouble();
                    y = bin_in.fbinreaddouble();
                    dh = bin_in.fbinreaddouble();

                    // Plausibilitaetskontrolle
                    if (Double.isNaN(dh) || dh < 0.)
                        hasValidValues = false;

                    SedimentModel2DData smd = dof_data[nr];
                    smd.duneHeight = dh;
                    CurrentModel2DData currentmodeldata = dof_currentdata[nr];
                    double dunehight = SedimentModel2DData.getvanRijnDuneHeight(smd.d50,
                            PhysicalParameters.RHO_WATER * smd.grainShearStress * currentmodeldata.cv,
                            currentmodeldata.totaldepth, smd.bottomslope);
                    double dhSource = smd.duneHeight / Function.max(0.01, Function.max(dunehight, smd.duneHeight));
                    final double dunelength = SedimentModel2DData.getFlemmingDuneLength(dunehight) * dhSource;
                    smd.duneLengthX = dunelength * currentmodeldata.u / (Function.max(currentmodeldata.cv, 0.001));
                    smd.duneLengthY = dunelength * currentmodeldata.v / (Function.max(currentmodeldata.cv, 0.001));

                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                }

                // Abbruch, wenn Netz nicht ok!
                if (!hasValidValues) {
                    System.out.println("***                     WARNUNG                                ***");
                    System.out.println("***   Ueberpruefen Sie die duneHight des   ***");
                    System.out.println("***   duneHight-Netzes. Das verwendetet Netz hat              ***");
                    System.out.println("***   Knoten mit negativen oder nicht definierten              ***");
                    System.out.println("***   Werten !                           ***");
                    System.out.println("***   Die Simulation wird fortgesetzt                          ***");
                    System.exit(0);
                }

            } else {
                System.out.println("system und duneHight.jbf different number of nodes");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der MaxErosion-Datei!");
            System.exit(0);
        }
    }

    /**
     * kann sowohl marina soilModelResultfiles als auch SubSurfaceSedimentFiles
     * lesen
     */
    private void readInitalSedimentParameterFromCSV(String singleSSPFileName) {
        // new String[] {"nodenumber ; x ; y ; z [m] in depth ; dmax [mm] ; d50 [mm] ;
        // dmin [mm] ; meanInitialSorting ; porosity ; consolidation ; time [s] since
        // 01.01.1970 ; Datum"};

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(singleSSPFileName, FileIO.input, FileIO.noCommentChar);

            System.out.println("\tReading SedimentParameterFromCSV (in MarinaCSV-Format): " + singleSSPFileName);

            final String header = systemfile.freadLine(); // Header
            if (!header.contains(
                    "nodenumber ; x ; y ; z [m] in depth ; dmax [mm] ; d50 [mm] ; dmin [mm] ; meanInitialSorting ; porosity ; consolidation")) {
                System.out.println("Header in der MarinaCSV entspricht nicht dem erwarteten!");
                System.exit(0);
            }

            String line = systemfile.freadLine();
            while (line != null) {
                String[] seperated = line.split(";");
                int nodeNumber = Integer.parseInt(seperated[0].trim());
                SedimentModel2DData smd = dof_data[nodeNumber];
                smd.setProperties(line);
                line = systemfile.freadLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der SedimentParameterFromCSV-Datei!");
            System.exit(0);
        }
    }

    /**
     * the method readInitalSortingFromXF4 read the datas for value from a
     * sysdat-file named filename
     * 
     * @param filename  name of the file to be open
     * @param initValue Konstante die Anzeigt welcher Wert initalisiert werden soll
     */
    private void readInitalValueFromXF4(String filename, InitialValues initValue) {

        double value;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading " + initValue + "-File (in TiCAD-System.Dat-Format): " + filename);

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));
            java.util.StringTokenizer strto = new StringTokenizer(line, " \t\n\r\f,");
            final int rand_knoten = Integer.parseInt(strto.nextToken());

            do {
                line = systemfile.freadLine();
            } while (line.startsWith("C"));

            strto = new StringTokenizer(line, " \t\n\r\f,");
            final int gebiets_knoten = Integer.parseInt(strto.nextToken());

            if (rand_knoten < 0 || gebiets_knoten < 0)
                throw new Exception("Fehler");

            if (fenet.getNumberofDOFs() != (rand_knoten + gebiets_knoten)) {
                System.out.println("!! FE-Mesh and " + initValue + "-File have different number of nodes !!");
                System.exit(0);
            }

            // Knoten einlesen
            int p_count = 0;
            while (p_count < (rand_knoten + gebiets_knoten)) {
                line = systemfile.freadLine();
                strto = new StringTokenizer(line, " \t\n\r\f,");

                if (!line.startsWith("C")) {
                    final int nr = Integer.parseInt(strto.nextToken());
                    /* final double x=Double.parseDouble( */strto.nextToken()/* ) */;
                    // parse x
                    /* final double y=Double.parseDouble( */strto.nextToken()/* ) */; // parse y
                    try {
                        value = Double.parseDouble(strto.nextToken());
                        switch (initValue) { // D50,DMIN,DMAX,SORTING, MAINTAINEDDEPTH, MAXEROSION, NOERODIBLEHORIZON
                            case SORTING:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].initialSorting = value;
                                break;
                            case D50:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].d50 = value / 1000.;
                                dof_data[nr].d50init = value / 1000.;
                                break;
                            case DMIN:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].dmin = value / 1000.;
                                break;
                            case DMAX:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].dmax = value / 1000.;
                                break;
                            case MAINTAINEDDEPTH:
                                dof_data[nr].maintainedDepth = value;
                                break;
                            case MAXEROSION: // nach unten positive
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                } {
                                DOF dof = fenet.getDOF(nr);
                                dof_data[nr].zh = dof.z + value;
                            }
                                break;
                            case NOERODIBLEHORIZON: {
                                DOF dof = fenet.getDOF(nr);
                                dof_data[nr].zh = Function.max(value, dof.z);
                            }
                                break;

                        }
                        p_count++;
                    } catch (NumberFormatException ex) {
                        System.out.println(
                                "********************************       ERROR         ***********************************");
                        System.out.println(ex);
                        System.out.println(
                                "*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der SedimentVarianz-Datei!");
            System.exit(0);
        }

    }

    /**
     * the method readInitalSortingFromJanetBin read the value
     * from a JanetBinary-file named filename
     * 
     * @param nam name of the file to be open
     */
    @SuppressWarnings("unused")
    private void readInitalValueFromJanetBin(String filename, InitialValues initValue) {
        int anzAttributes = 0;
        double x, y, value;

        boolean hasValidValues = true;
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
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version
                        + ", current version: 1.8");
            }

            if (version < 1.79)
                read_status_byte = true;

            System.out.println("\tReading " + initValue + "-File from " + filename);

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
            final boolean hasHeights = bin_in.fbinreadboolean(); // true = Hoehen, false = Tiefen

            // Layertyp ueberlesen
            int filetype = bin_in.fbinreadint();
            // liegt UnTRIM-Gitter mit diskreten Kantentiefen vor??
            boolean is_untrim = (filetype == 2);

            // Anzahl der Punkte lesen
            int anzk = bin_in.fbinreadint();
            if (anzk == fenet.getNumberofDOFs()) {

                // Punkte lesen
                for (int i = 0; i < anzk; i++) {
                    // Punktnummer lesen
                    if (writePointNumbers)
                        nr = bin_in.fbinreadint();
                    else
                        nr = i;

                    // x,y,s lesen
                    x = bin_in.fbinreaddouble();
                    y = bin_in.fbinreaddouble();
                    try {
                        value = bin_in.fbinreaddouble();
                        switch (initValue) { // D50,DMIN,DMAX,SORTING, MAINTAINEDDEPTH, MAXEROSION, NOERODIBLEHORIZON
                            case SORTING:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].initialSorting = value;
                                break;
                            case D50:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].d50 = value / 1000.;
                                dof_data[nr].d50init = value / 1000.;
                                break;
                            case DMIN:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].dmin = value / 1000.;
                                break;
                            case DMAX:
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                }
                                dof_data[nr].dmax = value / 1000.;
                                break;
                            case MAINTAINEDDEPTH:
                                if (hasHeights)
                                    value *= -1.; // wenn Hoehen dann auf Tiefen umrechnen
                                dof_data[nr].maintainedDepth = value;
                                break;
                            case MAXEROSION: // nach unten positive
                                if (value < 0.) {
                                    System.out.println("Value of " + initValue + " is negativ in File: <" + filename
                                            + "> at node number <" + nr + ">");
                                    throw new NumberFormatException("Value of " + initValue + " is negativ in File: <"
                                            + filename + "> at node number <" + nr + ">");
                                } {
                                DOF dof = fenet.getDOF(nr);
                                dof_data[nr].zh = dof.z + value;
                            }
                                break;
                            case NOERODIBLEHORIZON: {
                                if (hasHeights)
                                    value *= -1.; // wenn Hoehen dann auf Tiefen umrechnen
                                DOF dof = fenet.getDOF(nr);
                                dof_data[nr].zh = Function.max(value, dof.z);
                            }
                                break;

                        }
                    } catch (NumberFormatException ex) {
                        System.out.println(
                                "********************************       ERROR         ***********************************");
                        System.out.println(ex);
                        System.out.println(
                                "*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                    // Status-Flag lesen
                    if (writePointStatus)
                        bin_in.fbinreadshort();
                }
            } else {
                System.out.println("system und" + filename + " different number of nodes");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen der SedimentVarianz-Datei!");
            System.exit(0);
        }
    }

    /**
     * Read the start solution from file
     * 
     * @param currentergPath file with simulation results
     * @param record         record in the file
     * @throws java.lang.Exception
     */
    public void readDuneParameterFromTicadErgFile(String currentergPath, int record) throws Exception {

        System.out.println("\tRead DuneParameter from result file " + currentergPath);
        File sysergFile = new File(currentergPath);
        try (FileInputStream stream = new FileInputStream(sysergFile);
                DataInputStream inStream = new DataInputStream(stream)) {

            // Kommentar lesen, bis ASCII-Zeichen 7 kommt
            int c;
            do {
                c = inStream.readByte();
            } while (c != 7);
            // Ende Kommentar

            // Anzahl Elemente, Knoten und Rand lesen
            int anzKnoten = inStream.readInt();
            if (fenet.getNumberofDOFs() != anzKnoten) {
                System.out.println("Die Datei mit den Startwerten hat eine andere Anzahl von Knoten");
                System.exit(1);
            }
            int anzr = inStream.readInt();
            int anzElemente = inStream.readInt();

            // Ueberlesen folgende Zeilen
            inStream.skip(9 * 4);

            // Ergebnismaske lesen und auswerten
            int ergMaske = inStream.readInt();
            int anzWerte = TicadIO.ergMaskeAuswerten(ergMaske);

            boolean None_gesetzt = ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE);
            boolean Pos_gesetzt = ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS);
            boolean Z_gesetzt = ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z);
            boolean V_gesetzt = ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V);
            boolean Q_gesetzt = ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q);
            boolean C_gesetzt = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);
            boolean D50_gesetzt = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);
            boolean DUNEHight_gesetzt = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);
            boolean DUNELength_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR);
            boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL);
            boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
            boolean AH_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);

            inStream.readInt();

            // Elemente, Rand und Knoten Ueberlesen
            inStream.skip((anzElemente * 4L + anzr + 3L * anzKnoten) * 4L); // 4 Bytes je float und int

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4L) * record);

            inStream.readFloat(); // time
            for (int i = 0; i < fenet.getNumberofDOFs(); i++) {

                if (None_gesetzt)
                    inStream.skip(4);

                if (Pos_gesetzt)
                    inStream.skip(4);

                if (Z_gesetzt) {
                    inStream.skip(4);
                }

                if (V_gesetzt)
                    inStream.skip(8);

                if (Q_gesetzt)
                    inStream.skip(8);

                if (C_gesetzt) {
                    inStream.skip(4);
                }

                if (D50_gesetzt) {
                    inStream.skip(4);
                }

                if (DUNEHight_gesetzt)
                    dof_data[i].duneHeight = Function.max(0., inStream.readFloat());

                if (DUNELength_gesetzt) {
                    dof_data[i].duneLengthX = inStream.readFloat();
                    dof_data[i].duneLengthY = inStream.readFloat();
                }

                if (V_SCAL_gesetzt)
                    inStream.skip(4);

                if (Q_SCAL_gesetzt)
                    inStream.skip(4);

                if (AH_gesetzt)
                    inStream.skip(4);
            }
        }
    }

    // ------------------------------------------------------------------------
    // write_erg_xf
    // ------------------------------------------------------------------------
    @Deprecated
    @Override
    public void write_erg_xf(double[] erg, double t) {
    }

    // ------------------------------------------------------------------------
    // write_erg_xf
    // ------------------------------------------------------------------------
    @Override
    public final void write_erg_xf() {
        try {
            xf_os.writeFloat((float) (time * morphFactor));

            for (DOF dof : fenet.getDOFs()) {
                SedimentModel2DData smd = dof_data[dof.number];
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                final double ergZ = smd.z; // Tiefe
                final double ergC = smd.sC * PhysicalParameters.RHO_SEDIM; // in kg/m^3 oder g/l
                double erg1 = smd.qsx + smd.bedloadVector[0];
                double erg2 = smd.qsy + smd.bedloadVector[1];
                erg1 *= 3600.;
                erg2 *= 3600.;
                xf_os.writeFloat((float) ergZ);
                xf_os.writeFloat((float) erg1); // in m^3/(mh)
                xf_os.writeFloat((float) erg2); // in m^3/(mh)
                xf_os.writeFloat((float) ergC); // in g/l
                xf_os.writeFloat((float) (smd.d50 * 1000.)); // in mm
                xf_os.writeFloat((float) (smd.duneHeight)); // in m
                xf_os.writeFloat((float) (smd.duneLengthX)); // in m
                xf_os.writeFloat((float) (smd.duneLengthY)); // in m
                xf_os.writeFloat((float) (smd.porosity * 100.)); // in %
            }
            xf_os.flush();

            // Schreibe die Schichdaten
            if (withSoilModel3DData && wrtInt % modulo == 0)
                writeSoilModel3DData2CVS();

        } catch (IOException e) {
            System.out.println(this.getClass() + "\n\ttime=" + time + "\n");
            e.printStackTrace();
            System.exit(0);
        }
        wrtInt++;
    }

    // ------------------------------------------------------------------------
    // getSourceSunk
    // ------------------------------------------------------------------------
    private double getSourceSunk(DOF dof, double actualC, double maximalC) {

        double diffC = maximalC - actualC; // positive: Erosion, negative: Sedimentation

        SedimentModel2DData smd = dof_data[dof.number];
        CurrentModel2DData cmd = dof_currentdata[dof.number];

        // nicht Null Wassertiefe
        double H = Math.max(cmd.totaldepth, SedimentModel2D.WATT);
        // Schubgeschwindigkeit (aus kornbezogener Schubspannung)
        double ustar = Math.sqrt(smd.grainShearStress); // sqrt(tau/rho)
        // Rouse-Zahl
        final double kappa = 0.41;
        double P = smd.wc / (kappa * Math.max(ustar, 1e-5));
        // Faktor f(P) nach van Rijn (1986): f = 1 + 0.3 * P^2
        double fP = 1.0 + 0.3 * P * P * (H / (H + 0.05));
        // Relaxationszeit (beeinflusst durch Turbulenz)
        final double Ts = H / (smd.wc * fP);

        double rvalue;

        if (diffC > 0.) { // Erosion

            // nicht mehr aufnehmen als ueber dem nicht erodierbarem Horizont vohanden ist
            diffC = min(diffC, max(0., 1. / SedimentModel2D.morphFactor * (smd.zh - smd.z) * (1. - smd.porosity)
                    / max(CurrentModel2D.WATT, cmd.totaldepth) * cmd.wlambda));

            rvalue = diffC / Ts;

        } else { // sedimentation

            rvalue = diffC / Ts;
            // weitere Daempfung durch Boeschungswinkel
            rvalue /= smd.bottomslope;

        }

        return rvalue;
    }

    @Override
    public ModelData genData(FElement felement) {
        FTriangle ele = (FTriangle) felement;
        SedimentElementData res = new SedimentElementData();
        // initalization
        res.bottomslope = ele.bottomslope;
        res.dzdx = ele.dzdx;
        res.dzdy = ele.dzdy;
        element_data[felement.number] = res;
        return res;
    }

    /**
     * Einleseroutine readBoundCond
     * liest die spezifizierten Datensaetze (Randbedingungen) in der
     * boundary_condition_key_mask
     * aus entsprechenden Randwertedatei (sedimentdat.rndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in sedimentdat.rndwerteReader)
     */
    public final void readBoundCond() {

        String[] boundary_condition_key_mask = new String[3];
        boundary_condition_key_mask[0] = BoundaryCondition.concentration_sediment; // in m^3/m^3
        boundary_condition_key_mask[1] = BoundaryCondition.bottom; // in m Tiefe
        boundary_condition_key_mask[2] = BoundaryCondition.d50; // in mm

        try {
            for (BoundaryCondition bc : sedimentdat.rndwerteReader
                    .readBoundaryConditions(boundary_condition_key_mask)) {
                if (bc.boundary_condition_key.equals(BoundaryCondition.concentration_sediment)) {
                    bsc.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.bottom)) {
                    bz.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.d50)) {
                    bd50.add(bc);
                }
            }
        } catch (Exception e) {
            System.exit(1);
        }
    } // end readBoundCond

    @Override
    public final void timeStep(double dt) {

        resultIsNaN = false;

        if (this.basedOnCurrentModel3D) {
            // update of Current2DDate based on the values of the Current3DModel
            for (DOF dof : fenet.getDOFs()) {
                CurrentModel3DData c3dData = CurrentModel3DData.extract(dof);
                int dofnumber = dof.number;
                dof_currentdata[dofnumber] = c3dData.getCurrentModel2DData(dof_currentdata[dofnumber]);
            }
            for (FElement felement : fenet.getFElements()) {
                FTriangle ele = (FTriangle) felement;
                element_currentdata[ele.number] = CurrentModel3DData.getCurrent2DElementData(ele,
                        element_currentdata[ele.number]);
            }
            // end of update
        }

        setBoundaryConditions();

        maxTimeStep = Double.MAX_VALUE;

        // Elementloop
        performElementLoop();

        // Berechne omega und die Koeffizienten fuer Variable Adams-Bashforth 2. Ordnung
        // einmal vor dem parallelen Stream
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
            SedimentModel2DData smd = dof_data[j];
            CurrentModel2DData cmd = dof_currentdata[j];

            smd.bottomslope = smd._bottomslope / (dof.lumpedMass * 3.);
            smd._bottomslope = 0.;

            smd.rC /= dof.lumpedMass;
            smd.rZTransport /= dof.lumpedMass;
            smd.rZCorrect /= dof.lumpedMass;
            smd.rSKoncCorrect /= dof.lumpedMass;

            smd.isDeepest = smd._isDeepest;
            smd._isDeepest = true;

            smd.isHighest = smd._isHighest;
            smd._isHighest = true;

            if (smd.isDeepest && smd.rZCorrect > 0.) // ist der Knoten der tiefste Knoten im Patch und ist der
                                                     // Sedimenttransport fast 0 wird verhindert, dass der Knoten durch
                                                     // Fehlerkorrektur weiter erodiert
                smd.rZCorrect *= smd.lambdaQs;
            if (smd.isHighest && smd.rZCorrect < 0.) // ist der Knoten der hoechste Knoten im Patch und ist der
                                                     // Sedimenttransport fast 0 wird verhindert, dass der Knoten durch
                                                     // Fehlerkorrektur weiter waechst
                smd.rZCorrect *= smd.lambdaQs;

            // keine Fehlerkorrektur bei Boden will aufwachsen
            if (smd.rZCorrect < 0.)
                smd.rZCorrect *= smd.lambda // aber nichterodierbarer Knoten
                        * cmd.wlambda; // an trockenen Knoten

            if (smd.rSKoncCorrect > 0)
                smd.rSKoncCorrect *= cmd.wlambda; // aber Wattknoten

            // Variable Adams-Bashforth 2. Ordnung
            double rC = beta0 * smd.rC + beta1 * smd.dsCdt; // zusaetzlichen Stabilisierung in Anlehnung am expliziten
                                                            // Adams-Bashford 2. Ordnung mit variabler Schrittweite
            rC += smd.rSKoncCorrect;
            smd.rSKoncCorrect = 0;
            double rZ = beta0 * smd.rZTransport + beta1 * smd.dzTransportdt;
            double rZCorrect = beta0 * smd.rZCorrect + beta1 * smd.dzCdt;
            rZ += rZCorrect;
            smd.rZCorrect = 0;

            smd.dzTransportdt = smd.rZTransport;
            smd.dsCdt = smd.rC;

            smd.dzCdt = smd.rZCorrect;
            smd.rZCorrect = 0.;

            if ((cmd.totaldepth < smd.bound) && (rC > 0.))
                rC = 0.;
            if (rC > 0.)
                rC *= cmd.puddleLambda; // in Pfuetzen keine Zunahme der Konzentration

            // Quell- und Senkterm zur Anpassung der Gewaesserbettentwicklung
            // ich kenne den Zeitschritt und den morphologischen Faktor - kann also rZ so
            // anpassen, dass das neu z nicht ueber der Wasseroberflaeche liegt -> dies hat
            // auch eine Wirkung auf die Kornanpassung
            final double newZ = smd.z + dt * rZ * morphFactor;
            if (rZ < 0. && -newZ >= cmd.eta) // sedimentieren
                rZ = (-cmd.eta - smd.z) / dt / morphFactor;
            // ich kenne den Zeitschritt und den morphologischen Faktor - kann also rZ so
            // anpassen, dass das neu z nicht unter dem nichterodierbaren Horizont liegt ->
            // dies hat auch eine Wirkung auf die Kornanpassung
            if (rZ > 0. && newZ > smd.zh) // erodieren
                rZ = (smd.zh - smd.z) / dt / morphFactor;

            smd.dzdt = rZ;

            // Quell- und Senkterm zur Anpassung des d50
            double d50Source = smd.d50 * (1. - smd.porosity) * rZ * smd.initialSorting / smd.bottomslope;
            if (rZ < 0.) { // sedimentation
                d50Source *= (1. - smd.dmin / smd.d50) / (1 - rZ * Function.norm(cmd.tauBx, cmd.tauBy));
            } else { // erosion
                d50Source *= (1. - smd.d50 / smd.dmax) / smd.bottomslope
                        * (1 + rZ * Function.norm(cmd.tauBx, cmd.tauBy) / (1 + Function.norm(cmd.tauBx, cmd.tauBy)));
            }
            smd.setD50(smd.d50 + dt * d50Source * morphFactor);

            smd.z += dt * rZ * morphFactor;
            cmd.setBottomLevel(smd.z);

            smd.sC += dt * (rC + smd.sedimentSource);
            /* prevention of negative concentration */
            if (smd.sC < 0.)
                smd.sC = 0.;
            /* prevention of saturated concentration m**3/m**3 */
            if (smd.sC > .5)
                smd.sC = 0.5;

            double tauBx = cmd.tauBx;
            double tauBy = cmd.tauBy;
            final double waveBreaking; // energyloss by wavebreaking
            WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
            if (wmd != null) {
                double twX = wmd.taubX / 2.; // ! Wave induced mean bottomshearstress = maximal bottomshearstress / 2. !
                double twY = wmd.taubY / 2.;
                // umdrehen der Richtung, wenn notwendig
                if (scalarProduct(tauBx, tauBy, twX, twY) < 0) {
                    twX *= -1.;
                    twY *= -1.;
                }
                tauBx += twX;
                tauBy += twY;
                waveBreaking = wmd.epsilon_b;
            } else {
                waveBreaking = 0.;
            }
            final double taub = Function.norm(tauBx, tauBy);

            // Bodenformen // Yalin, vanRijn, Yalin80
            double predictedDuneHeight = min(smd.getYalinDuneHeight(taub, cmd.totaldepth), max(0., smd.zh - smd.z)); // kann
                                                                                                                     // nicht
                                                                                                                     // hoeher
                                                                                                                     // sein,
                                                                                                                     // als
                                                                                                                     // das
                                                                                                                     // ueber
                                                                                                                     // dem
                                                                                                                     // erodierbaren
                                                                                                                     // Horizont
                                                                                                                     // verfuegbare
                                                                                                                     // Material
            if (rZ > 0)
                predictedDuneHeight *= 1. / (1. + rZ); // bei starker Erosion verschwinden Bodenformen
            predictedDuneHeight *= 1. / (1. + waveBreaking); // bei wavebreaking verschwinden Bodenformen
            double dhSource = ((predictedDuneHeight - smd.duneHeight) > 0)
                    ? 1. / PhysicalParameters.G
                            * Math.min(smd.bedload / (1 + smd.bottomslope), (predictedDuneHeight - smd.duneHeight))
                            / smd.bottomslope
                    : (taub + waveBreaking) / 86400 * smd.bottomslope * (predictedDuneHeight - smd.duneHeight); // Peter
                                                                                                                // 28.01.2025
            // Flemming, Yalin, vanRijn
            final double predictedDuneLength = SedimentModel2DData.getYalinDuneLength(smd.duneHeight);

            if (dhSource > 0) {
                dhSource *= 1. / (1. + Math.abs(rZ)); // bei starker Bodenevolution kleineres Anwachsen der Duehnenhoehe
                smd.duneHeight += dhSource * dt * morphFactor;
                smd.duneHeight = Math.min(smd.duneHeight, predictedDuneHeight);
                // umdrehen der Richtung, wenn notwendig
                if (scalarProduct(tauBx, tauBy, smd.duneLengthX, smd.duneLengthY) < 0) {
                    smd.duneLengthX *= -1;
                    smd.duneLengthY *= -1;
                }
                final double dlSourceX = (predictedDuneLength * tauBx / Function.max(taub, 0.001) - smd.duneLengthX);
                final double dlSourceY = (predictedDuneLength * tauBy / Function.max(taub, 0.001) - smd.duneLengthY);
                smd.duneLengthX += dlSourceX * dhSource * dt * morphFactor;
                smd.duneLengthY += dlSourceY * dhSource * dt * morphFactor;
            } else {
                dhSource *= (1. + Math.abs(rZ)); // bei starker Bodenevolution schnellere Abnahme der Duehnenhoehe
                smd.duneHeight += dhSource * dt * morphFactor;
                smd.duneHeight = Math.max(smd.duneHeight, 0.);
                final double actualDuneLength = Function.norm(smd.duneLengthX, smd.duneLengthY);
                final double newLength = actualDuneLength - (dhSource - taub * PhysicalParameters.KINVISCOSITY_WATER)
                        * (predictedDuneLength - actualDuneLength) * dt * morphFactor; // Betrag von dhSource - deshalb
                                                                                       // -dhSource bei negativem
                                                                                       // dhSource
                smd.duneLengthX = smd.duneLengthX / Function.max(actualDuneLength, 0.01) * newLength;
                smd.duneLengthY = smd.duneLengthY / Function.max(actualDuneLength, 0.01) * newLength;
            }
            smd.duneHeight = min(smd.duneHeight, max(0., smd.zh - smd.z)); // kann nicht hoeher sein, als das ueber dem
                                                                           // erodierbaren Horizont verfuegbare Meterial
            if (smd.duneHeight <= 0.) {
                smd.duneHeight = 0.;
                smd.duneLengthX = 0.;
                smd.duneLengthY = 0.;
            }

            if (withSoilModel3DData) {
                ((SoilModel3DData) smd).timeStep(this.time, dt);
            }
            boolean rIsNaN = Double.isNaN(rZ) || Double.isNaN(rC);
            if (rIsNaN) {
                System.out.println(
                        "SedimentTransportModel is NaN bei " + j + " dzdt=" + rZ + " DCdt=" + rC + " d50=" + smd.d50);
            }
            resultIsNaN |= rIsNaN;
        });

        // Aktualisiere den vorherigen Zeitschritt für das gesamte Modell
        this.previousTimeStep = dt;
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

    private double scalarProduct(double v1x, double v1y, double v2x, double v2y) {
        return v1x * v2x + v1y * v2y;
    }
}
