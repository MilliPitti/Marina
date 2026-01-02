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

import bijava.math.ifunction.*;
import de.smile.marina.MarinaXML;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.ecological.SpartinaAlternifloraModel2DData;
import de.smile.marina.fem.model.ground.BathymetryData2D;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.model.hydrodynamic.dim2.weirs.*;
import de.smile.marina.fem.model.meteorology.MeteorologyData2D;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.SmileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import de.smile.xml.marina.weirs.*;
import java.io.*;
import static java.lang.Math.max;
import java.util.*;
import javax.xml.bind.*;

/**
 * The {@code CurrentModel2D} class extends {@link SurfaceWaterModel} to
 * simulate depth integrated current based on shallow water equations.
 * It manages the flow of water, incorporating factors such as bottom friction,
 * wind stress,
 * and Coriolis force. This model uses finite element methods to solve the
 * governing equations,
 * and it supports various boundary conditions and initial conditions.
 * 
 * @version 4.9.2
 * @author Peter Milbradt
 */
public class CurrentModel2D extends SurfaceWaterModel {

    CurrentModel2DData[] dof_data = null;
    Current2DElementData[] element_data = null;

    private DataOutputStream xf_os = null;

    private final CurrentDat currentdat;

    static private final double ALPHA = .75; // coefficient for secondary flow [0.75 rough bottom, 1. smooth], die
                                             // Beruecksichtigung der Bodenrauheit erfolgt ueber beta in der Formel

    public static final double BATTJESKOEFF = 0.3; // 0.1 - 0.3 Austauschkoeffizient infolge Wellenbrechens

    private double previousTimeStep = 0.0; // Speichert den vorherigen Zeitschritt für das gesamte Modell

    // Konstruktor
    public CurrentModel2D(FEDecomposition fe, CurrentDat currentdat) {
        System.out.println("CurrentModel2D initialization");
        this.referenceDate = currentdat.referenceDate;
        this.epsgCode = currentdat.epsgCode;
        fenet = fe;
        femodel = this;
        this.currentdat = currentdat;
        WATT = Function.max(0.01, currentdat.watt); // verhindert das jemand als Wattgrenze 0 angibt
        halfWATT = WATT / 2.;
        infiltrationRate = currentdat.infiltrationRate;

        dof_data = new CurrentModel2DData[fenet.getNumberofDOFs()];
        element_data = new Current2DElementData[fenet.getNumberofFElements()];

        setNumberOfThreads(currentdat.NumberOfThreads);

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
            // read rauhigkeitsmodell // nikuradse-dat in mm
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

        // read Weirs
        if (currentdat.weirsFileType == CurrentDat.WeirFileType.weirXML) {
            readWeirXML(currentdat.weirsFileName);
        }

        try {
            xf_os = new DataOutputStream(new FileOutputStream(currentdat.xferg_name));
            // Setzen der Ergebnismaske
            TicadIO.write_xf(xf_os, this);
        } catch (FileNotFoundException e) {
            System.out.println("The file " + currentdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialisiert die Wasserspiegellage mit einem konstanten Wert.
     * Die Methode setzt den Wasserspiegel auf den uebergebenen konstanten Wert f�r
     * den gesamten Projektzeitraum.
     *
     * @param initalWaterLevel Der konstante Wert fuer die Wasserspiegellage.
     * @return true, wenn die Initialisierung erfolgreich war, false falls nicht.
     */
    public double[] constantInitialWaterLevel(double initalWaterLevel) {
        System.out.println("\t Set initial value " + initalWaterLevel);

        for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
            DOF dof = fenet.getDOF(i);
            SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
            dof_data[i].z = dof.z;
            if (sedimentmodeldata != null)
                dof_data[i].z = sedimentmodeldata.z;
            if ((dof_data[i].z + initalWaterLevel) > 0.)
                dof_data[i].eta = initalWaterLevel;
            else
                dof_data[i].eta = -dof_data[i].z;
        }
        return null;
    }

    /**
     * Read the start solution from file
     * 
     * @param currentergPath file with simulation results
     * @param record         record in the file
     * @param sysDatZ        is true if the z-Value of the Net is using else the
     *                       z-Value stored in the result file is used
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unused")
    public double[] initialSolutionFromTicadErgFile(String currentergPath, int record, boolean sysDatZ)
            throws Exception {

        System.out.println("\tRead initial values from result file " + currentergPath);
        // erstes Durchscannen
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
            final int anzKnoten = inStream.readInt();
            if (fenet.getNumberofDOFs() != anzKnoten) {
                System.out.println("Die Datei mit den Startwerten hat andere Anzahl von Knoten");
                System.exit(1);
            }
            final int anzr = inStream.readInt();
            final int anzElemente = inStream.readInt();

            // Ueberlesen folgender Zeilen
            inStream.skip(9 * 4);

            // Ergebnismaske lesen und auswerten
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

            // Elemente, Rand und Knoten Ueberlesen
            inStream.skip((anzElemente * 4 + anzr + 3 * anzKnoten) * 4); // 4 Bytes je float und int

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4) * record);

            float t = inStream.readFloat();
            for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
                if (None_gesetzt) {
                    inStream.skip(4);
                }

                if (Pos_gesetzt) {
                    inStream.skip(4);
                }

                if (Z_gesetzt) {
                    if (sysDatZ) {
                        inStream.skip(4);
                        dof_data[i].z = fenet.getDOF(i).z;
                    } else
                        dof_data[i].z = inStream.readFloat();
                }

                if (V_gesetzt) {
                    dof_data[i].u = inStream.readFloat();
                    dof_data[i].v = inStream.readFloat();
                    dof_data[i].cv = Function.norm(dof_data[i].u, dof_data[i].v);
                }

                if (Q_gesetzt) {
                    inStream.skip(8);
                }

                if (H_gesetzt) {
                    dof_data[i].setWaterLevel(inStream.readFloat());
                }

                if (SALT_gesetzt) {
                    inStream.skip(4);
                }

                if (EDDY_gesetzt) {
                    inStream.skip(4);
                }

                if (SHEAR_gesetzt) {
                    dof_data[i].tauBx = inStream.readFloat();
                    dof_data[i].tauBy = inStream.readFloat();
                    dof_data[i].bottomFrictionCoefficient = Function.norm(dof_data[i].tauBx, dof_data[i].tauBy)
                            / dof_data[i].rho;
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
            inStream.close();
            stream.close();
        }
        return null;
    }

    /**
     * the method initialSolution compute a startsolution
     * 
     * @param time the time to generate a initial solution
     * @return
     */
    public double[] initialSolution(double time) {
        this.time = time;

        System.out.println("\tinterpolating initial water level");

        initalSolutionLoop[] iloop = new initalSolutionLoop[numberOfThreads];
        int anzdofs = fenet.getNumberofDOFs();
        for (int ii = 0; ii < numberOfThreads; ii++) {
            iloop[ii] = new initalSolutionLoop(anzdofs * ii / numberOfThreads, anzdofs * (ii + 1) / numberOfThreads,
                    time);
            iloop[ii].start();
        }
        for (int ii = 0; ii < numberOfThreads; ii++) {
            try {
                iloop[ii].join();
            } catch (InterruptedException e) {
            }
        }

        inith = null;

        return null;
    }

    private class initalSolutionLoop extends Thread {

        int lo, hi;
        double time;

        initalSolutionLoop(int lo, int hi, double time) {
            this.lo = lo;
            this.hi = hi;
            this.time = time;
        }

        @Override
        public void run() {
            for (int ii = lo; ii < hi; ii++) {
                DOF dof = fenet.getDOF(ii);
                int i = dof.number;
                CurrentModel2DData currentmodeldata = dof_data[i];

                final SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
                if (sedimentmodeldata == null) {
                    currentmodeldata.z = dof.z;
                } else {
                    currentmodeldata.z = sedimentmodeldata.z;
                }

                currentmodeldata.setWaterLevel(initialH(dof, time));

                currentmodeldata.u = 0.;
                currentmodeldata.v = 0.;
            }
        }
    }

    // ----------------------------------------------------------------------
    // initialH
    // ----------------------------------------------------------------------
    private double initialH(DOF dof, double time) {
        double h = 0., R = 0., d;
        CurrentModel2DData currentdata = dof_data[dof.number];
        if (currentdata.bh != null) {
            h = currentdata.bh.getValue(time);
        } else {
            for (DOF ndof : inith) {
                CurrentModel2DData current = dof_data[ndof.number];
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

    public double[] initialHfromSysDat(String systemDatPath, double time) throws Exception {
        this.time = time;

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
                if (anzk == fenet.getNumberofDOFs()) {
                    System.out.println("\tRead initial waterlevel (in Ticad-SysDat-Format): " + systemDatPath);
                    for (int j = 0; j < anzk; j++) {
                        int nr = TicadIO.NextInt(st);
                        TicadIO.NextDouble(st);
                        TicadIO.NextDouble(st);
                        value = TicadIO.NextDouble(st); // -> eta

                        DOF dof = fenet.getDOF(nr);
                        double depth;
                        final SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
                        if (sedimentmodeldata == null) {
                            depth = dof.z;
                        } else {
                            depth = sedimentmodeldata.z;
                        }

                        CurrentModel2DData currentmodeldata = dof_data[nr];
                        currentmodeldata.z = depth;

                        currentmodeldata.setWaterLevel(value);

                        currentmodeldata.u = 0.;
                        currentmodeldata.v = 0.;

                    }
                } else {
                    System.out.println("\t different number of nodes");

                }
            }

        } catch (IOException e) {
            System.out.println("\t cannot open file: " + systemDatPath);
        }

        inith = null;

        return null;
    }

    @SuppressWarnings("unused")
    public double[] initialHfromJanetBin(String filename, double time) {
        this.time = time;

        int anzAttributes = 0;
        double value;
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

            if (version < 1.79) {
                read_status_byte = true;
            }

            System.out.println("\tRead Waterlevel (in Janet-Binary-Format):  " + filename);

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
                    bin_in.fbinreaddouble();
                    bin_in.fbinreaddouble();
                    value = bin_in.fbinreaddouble();

                    DOF dof = fenet.getDOF(nr);
                    double depth;
                    final SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
                    if (sedimentmodeldata == null) {
                        depth = dof.z;
                    } else {
                        depth = sedimentmodeldata.z;
                    }

                    CurrentModel2DData currentmodeldata = dof_data[nr];
                    currentmodeldata.z = depth;

                    currentmodeldata.setWaterLevel(value);

                    currentmodeldata.u = 0.;
                    currentmodeldata.v = 0.;

                    // Status-Flag lesen
                    if (writePointStatus) {
                        bin_in.fbinreadshort();
                    }

                }
            } else {
                System.out.println("system and waterlevel.jbf different number of nodes");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
            System.exit(1);
        }

        inith = null;

        return null;
    }

    @Override
    @Deprecated
    public double[] getRateofChange(double time, double x[]) {
        return null;
    }

    /**
     * FEM-Approximation for a FElement (FTriangle)
     * 
     * @param element FElement (FTriangle)
     * @return timeStep
     */
    @Override
    public final double ElementApproximation(FElement element) {

        double timeStep = Double.MAX_VALUE;

        final FTriangle ele = (FTriangle) element;
        final DOF[] dofs = element.getDOFs();
        final Current2DElementData eleCurrentData = element_data[ele.number];

        eleCurrentData.deepestTotalDepth = 0.;
        eleCurrentData.iwatt = 0;
        int dry = 0;

        eleCurrentData.wlambda = 1;

        for (int j = 0; j < 3; j++) {
            final CurrentModel2DData cmd = dof_data[dofs[j].number];
            final double totaldepth = cmd.totaldepth;

            if (totaldepth < WATT) {
                eleCurrentData.iwatt++;
                eleCurrentData.wlambda *= cmd.wlambda;
                if (totaldepth < halfWATT) {
                    dry++;
                }
            }

            if (cmd.bWeir != null) { // TODO muss das wirklich in jedem Zeitschritt gemacht werden oder kann dies
                                     // nicht beim einlesen der Wehre geschehen
                eleCurrentData.meanStricklerCoefficient = 20.;
                cmd.kst = 20.;
                dof_data[dofs[(j + 1) % 3].number].kst = 20.;
                dof_data[dofs[(j + 2) % 3].number].kst = 20.;
                cmd.ks = CurrentModel2DData.Strickler2Nikuradse(20.); // in mm
                dof_data[dofs[(j + 1) % 3].number].kst = cmd.ks;
                dof_data[dofs[(j + 2) % 3].number].kst = cmd.ks;
            }
        }

        // caculate Bottomslope
        final SedimentElementData eleSedimentData = SedimentElementData.extract(ele);
        final double bottomslope = (eleSedimentData != null) ? eleSedimentData.bottomslope : ele.bottomslope;

        if (dry == 3) { // element is totaly dry

            eleCurrentData.isDry = true;

            for (int j = 0; j < 3; j++) {
                final CurrentModel2DData cmd = dof_data[ele.getDOF(j).number];
                final double w1_lambda = 1. - cmd.totaldepth / halfWATT;
                synchronized (cmd) {
                    cmd.reta -= (1.E-7 + infiltrationRate) * w1_lambda * bottomslope; // kuenstliches Versickeren auf
                                                                                      // trockenen Elementen zur
                                                                                      // Modellstabilisierung
                }
            }

        } else {

            eleCurrentData.isDry = false;

            final double[][] koeffmat = ele.getkoeffmat();

            final double[] terms_u = new double[3];
            final double[] terms_v = new double[3];
            final double[] terms_eta = new double[3];

            double u_mean = 0.;
            double v_mean = 0.;
            double depth_mean = 0.;

            double udx = 0.;
            double udy = 0.;
            double vdx = 0.;
            double vdy = 0.;
            double detadx = 0.;
            double detady = 0.;
            double depthdx = 0.;
            double depthdy = 0.;

            double rhodx = 0.;
            double rhody = 0.;

            // Wave relevant parameter
            double dsxxdx = 0.;
            double dsxydx = 0.;
            double dsxydy = 0.;
            double dsyydy = 0.;
            double wavebreaking = 0.;

            double elementsize = ele.maxEdgeLength;
            boolean indicator = false;
            // compute element derivations
            for (int j = 0; j < 3; j++) {
                final DOF dof = dofs[j];
                final CurrentModel2DData cmd = dof_data[dof.number];

                rhodx += cmd.rho * koeffmat[j][1];
                rhody += cmd.rho * koeffmat[j][2];

                u_mean += cmd.u / 3.;
                v_mean += cmd.v / 3.;
                depth_mean += cmd.totaldepth / 3.;
                eleCurrentData.deepestTotalDepth = (eleCurrentData.deepestTotalDepth > cmd.totaldepth)
                        ? eleCurrentData.deepestTotalDepth
                        : cmd.totaldepth;// max(eleCurrentData.deepestTotalDepth, cmd.totaldepth);

                udx += cmd.u * koeffmat[j][1];
                udy += cmd.u * koeffmat[j][2];

                vdx += cmd.v * koeffmat[j][1];
                vdy += cmd.v * koeffmat[j][2];

                detadx += cmd.eta * koeffmat[j][1];
                detady += cmd.eta * koeffmat[j][2];

                depthdx += cmd.totaldepth * koeffmat[j][1];
                depthdy += cmd.totaldepth * koeffmat[j][2];

                final WaveHYPModel2DData wave = WaveHYPModel2DData.extract(dof);
                if (wave != null) {
                    dsxxdx += wave.sxx * cmd.rho * PhysicalParameters.G * wave.wa * koeffmat[j][1];
                    dsxydx += wave.sxy * cmd.rho * PhysicalParameters.G * wave.wa * koeffmat[j][1];
                    dsxydy += wave.sxy * cmd.rho * PhysicalParameters.G * wave.wa * koeffmat[j][2];
                    dsyydy += wave.syy * cmd.rho * PhysicalParameters.G * wave.wa * koeffmat[j][2];
                    if (j == 0)
                        wavebreaking = wave.epsilon_b;
                    else
                        wavebreaking = (wavebreaking > wave.epsilon_b) ? wavebreaking : wave.epsilon_b; // max(wavebreaking,wave.epsilon_b);
                }

                if (cmd.cv > WATT / 10.) {
                    elementsize = Function.min(ele.getVectorSize(cmd.u, cmd.v), elementsize);
                    indicator = true;
                }
            }
            if (!indicator)
                elementsize = ele.minHight;
            eleCurrentData.elementsize = elementsize;

            if (ele.getKennung() != 0) { // mind. eine Kante ist geschlossenen, dann keine welleninduzierte Kraefte
                dsxxdx = 0.;
                dsxydx = 0.;
                dsxydy = 0.;
                dsyydy = 0.;
            }

            // update elementdata for currentmodel2d
            eleCurrentData.dudx = udx;
            eleCurrentData.dudy = udy;
            eleCurrentData.dvdx = vdx;
            eleCurrentData.dvdy = vdy;
            eleCurrentData.ddepthdx = depthdx;
            eleCurrentData.ddepthdy = depthdy;

            eleCurrentData.u_mean = u_mean;
            eleCurrentData.v_mean = v_mean;
            eleCurrentData.depth_mean = depth_mean;

            double flood = 1.;

            if (eleCurrentData.iwatt != 0) {

                flood = 0.;

                detadx = 0.;
                detady = 0.;

                switch (eleCurrentData.iwatt) {
                    case 1:
                        for (int j = 0; j < 3; j++) {
                            final int jg = dofs[j].number;
                            if (dof_data[jg].totaldepth >= WATT) {
                                detadx += dof_data[jg].eta * koeffmat[j][1];
                                detady += dof_data[jg].eta * koeffmat[j][2];
                            } else {
                                final int jg_1 = dofs[(j + 1) % 3].number;
                                final int jg_2 = dofs[(j + 2) % 3].number;
                                if ((dof_data[jg].eta < dof_data[jg_1].eta)
                                        || (dof_data[jg].eta < dof_data[jg_2].eta)) {
                                    detadx += dof_data[jg].eta * koeffmat[j][1];
                                    detady += dof_data[jg].eta * koeffmat[j][2];
                                    flood = Function.min(1., Function.max(dof_data[jg_1].eta - dof_data[jg].eta,
                                            dof_data[jg_2].eta - dof_data[jg].eta) / WATT);
                                } else {
                                    detadx += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].eta + dof_data[jg_2].eta)
                                            + dof_data[jg].wlambda * dof_data[jg].eta) * koeffmat[j][1];
                                    detady += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].eta + dof_data[jg_2].eta)
                                            + dof_data[jg].wlambda * dof_data[jg].eta) * koeffmat[j][2];
                                }
                            }
                        }
                        break;

                    case 2:
                        for (int j = 0; j < 3; j++) {
                            final int jg = dofs[j].number;
                            if (dof_data[jg].totaldepth >= WATT) {
                                detadx += dof_data[jg].eta * koeffmat[j][1];
                                detady += dof_data[jg].eta * koeffmat[j][2];

                                final int jg_1 = dofs[(j + 1) % 3].number;
                                final int jg_2 = dofs[(j + 2) % 3].number;

                                if (dof_data[jg].eta > dof_data[jg_1].eta) {
                                    detadx += dof_data[jg_1].eta * koeffmat[(j + 1) % 3][1];
                                    detady += dof_data[jg_1].eta * koeffmat[(j + 1) % 3][2];
                                    flood = Function.min(1., (dof_data[jg].eta - dof_data[jg_1].eta) / WATT);
                                } else {
                                    detadx += (dof_data[jg_1].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][1];
                                    detady += (dof_data[jg_1].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][2];
                                }

                                if (dof_data[jg].eta > dof_data[jg_2].eta) {
                                    detadx += dof_data[jg_2].eta * koeffmat[(j + 2) % 3][1];
                                    detady += dof_data[jg_2].eta * koeffmat[(j + 2) % 3][2];
                                    flood = Function.min(1.,
                                            Function.max(flood, (dof_data[jg].eta - dof_data[jg_2].eta) / WATT));
                                } else {
                                    detadx += (dof_data[jg_2].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][1];
                                    detady += (dof_data[jg_2].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][2];
                                }
                                break;
                            }
                        }
                        break;

                    case 3:
                        double dmax = dof_data[dofs[0].number].totaldepth;
                        int j = 0;
                        if (dof_data[dofs[1].number].totaldepth > dmax) {
                            j = 1;
                            dmax = dof_data[dofs[1].number].totaldepth;
                        }
                        if (dof_data[dofs[2].number].totaldepth > dmax) {
                            j = 2;
                            /* dmax = dof_data[dofs[2].number].totaldepth; **unnoetig** */}
                        /* Knoten j hat groeszte Wassertiefe */
                        final int jg = dofs[j].number;
                        detadx += dof_data[jg].eta * koeffmat[j][1];
                        detady += dof_data[jg].eta * koeffmat[j][2];

                        final int jg_1 = dofs[(j + 1) % 3].number;
                        final int jg_2 = dofs[(j + 2) % 3].number;

                        if (dof_data[jg].eta > dof_data[jg_1].eta) {
                            detadx += (dof_data[jg].wlambda * dof_data[jg_1].eta
                                    + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_1].wlambda * dof_data[jg_1].eta))
                                    * koeffmat[(j + 1) % 3][1];
                            detady += (dof_data[jg].wlambda * dof_data[jg_1].eta
                                    + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_1].wlambda * dof_data[jg_1].eta))
                                    * koeffmat[(j + 1) % 3][2];
                            flood = Function.min(1.,
                                    dof_data[jg].wlambda * (dof_data[jg].eta - dof_data[jg_1].eta) / WATT);
                        } else {
                            detadx += (dof_data[jg_1].w1_lambda * dof_data[jg].eta
                                    + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][1];
                            detady += (dof_data[jg_1].w1_lambda * dof_data[jg].eta
                                    + dof_data[jg_1].wlambda * dof_data[jg_1].eta) * koeffmat[(j + 1) % 3][2];
                        }

                        if (dof_data[jg].eta > dof_data[jg_2].eta) {
                            detadx += (dof_data[jg].wlambda * dof_data[jg_2].eta
                                    + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_2].wlambda * dof_data[jg_2].eta))
                                    * koeffmat[(j + 2) % 3][1];
                            detady += (dof_data[jg].wlambda * dof_data[jg_2].eta
                                    + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].eta
                                            + dof_data[jg_2].wlambda * dof_data[jg_2].eta))
                                    * koeffmat[(j + 2) % 3][2];
                            flood = Function.min(1., Function.max(flood,
                                    dof_data[jg].wlambda * (dof_data[jg].eta - dof_data[jg_2].eta) / WATT));
                        } else {
                            detadx += (dof_data[jg_2].w1_lambda * dof_data[jg].eta
                                    + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][1];
                            detady += (dof_data[jg_2].w1_lambda * dof_data[jg].eta
                                    + dof_data[jg_2].wlambda * dof_data[jg_2].eta) * koeffmat[(j + 2) % 3][2];
                        }
                        break;
                }

                dsxxdx = 0.;
                dsxydx = 0.;
                dsxydy = 0.;
                dsyydy = 0.;

            }

            // eddy viscosity
            // -----------------------------------------
            // konstant
            double astx = PhysicalParameters.DYNVISCOSITY_WATER;
            // Smagorinsky-Ansatz (Konstante zwischen 0.1-0.2)
            final double Cs = 0.1 / 2.; // Smagorinsky Konstante
            astx += (Cs * elementsize) * (Cs * elementsize)
                    * Math.sqrt(2. * udx * udx + (udy + vdx) * (udy + vdx) + 2. * vdy * vdy);
            /*
             * isotropher Elder - Anteil mit Strickler Bodenschubspannung approximiert - ca.
             * 0.06
             */
            final double u_star = Function.norm(u_mean, v_mean) * PhysicalParameters.sqrtG /
                    (eleCurrentData.meanStricklerCoefficient * Math.pow(depth_mean, 1.0 / 6.0));
            // Elder-Koeffizient kappa (ca. 0.6): nu_t = kappa * u* * h
            final double nu_elder = 0.6 / 2. * u_star * depth_mean;
            astx += nu_elder;

            // Battjes-Ansatz turbulence by wavebreaking
            if (wavebreaking > 0.)
                astx += BATTJESKOEFF * depth_mean * Math.cbrt(wavebreaking / PhysicalParameters.RHO_WATER);
            // wave induced turbulence by Radiation-Stresses
            astx += Function.norm((dsxxdx + dsxydy), (dsxydx + dsyydy)) / PhysicalParameters.RHO_WATER;

            if (eleCurrentData.withWeir) {
                int iUnderFlowTopoWeir = 0;
                for (int j = 0; j < 3; j++) {
                    final int jn = dofs[j].number;
                    if (dof_data[jn].bWeir != null) {
                        if ((dof_data[jn].bWeir instanceof BroadCrestedWeir)
                                && !(dof_data[jn].bWeir instanceof BroadCrestedTopoWeir)) {
                            if ((((BroadCrestedWeir) dof_data[jn].bWeir).getCrestLevel()
                                    + dof_data[jn].eta) <= (-WATT)) {
                                detadx = 0.;
                                detady = 0.;
                            } else {
                                double factor = Function.min(1.,
                                        (((BroadCrestedWeir) dof_data[jn].bWeir).getCrestLevel() + dof_data[jn].eta
                                                + WATT) / 6. / WATT);
                                detadx *= factor;
                                detady *= factor;
                            }
                        }
                        if (dof_data[jn].bWeir instanceof NeedleWeir needleWeir) {
                            double factor = needleWeir.getOpening();
                            detadx *= factor;
                            detady *= factor;
                        }
                        if (dof_data[jn].bWeir instanceof UnderFlowWeir underFlowWeir) {
                            if (dof_data[jn].bWeir instanceof UnderFlowTopoWeir underFlowTopoWeir) {
                                if (dof_data[jn].eta + underFlowTopoWeir.getSluiceLevel() >= (-WATT)) {
                                    astx += Math
                                            .abs(Function.norm(detadx - dof_data[jn].dhdx, detady - dof_data[jn].dhdy));
                                    if (iUnderFlowTopoWeir == 0) {
                                        detadx = dof_data[jn].dhdx;
                                        detady = dof_data[jn].dhdy;
                                        iUnderFlowTopoWeir++;
                                    } else {
                                        detadx = 0.5 * (detadx + dof_data[jn].dhdx);
                                        detady = 0.5 * (detady + dof_data[jn].dhdy);
                                        iUnderFlowTopoWeir++;
                                    }
                                }
                            } else {
                                if (dof_data[jn].eta + underFlowWeir.getSluiceLevel() >= (-WATT)) {
                                    double factor = Function.min(1.,
                                            Function.max(0., dof_data[jn].z - underFlowWeir.getSluiceLevel())
                                                    / Function.max(WATT, dof_data[jn].totaldepth));
                                    detadx *= factor;
                                    detady *= factor;
                                }
                            }
                        }
                    }
                }
            }

            eleCurrentData.dhdx = detadx;
            eleCurrentData.dhdy = detady;

            double cureq1_mean = 0.;
            double cureq2_mean = 0.;
            double cureq3_mean = 0.;

            final double dzdx = (eleSedimentData != null) ? eleSedimentData.dzdx : ele.dzdx;
            final double dzdy = (eleSedimentData != null) ? eleSedimentData.dzdy : ele.dzdy;

            double asty = astx;
            eleCurrentData.asty = asty;
            eleCurrentData.astx = astx;

            // Elementfehler der Kontigleichung berechnen
            for (int j = 0; j < 3; j++) {
                final CurrentModel2DData cmd = dof_data[dofs[j].number];
                final double wlambda = (flood > cmd.wlambda ? flood : cmd.wlambda);
                // Kontigleichung
                terms_eta[j] = cmd.totaldepth * (udx + vdy) + (cmd.u * depthdx + cmd.v * depthdy);
                cureq1_mean += 1. / 3. * (cmd.detadt + terms_eta[j]) * wlambda;
            }

            for (int j = 0; j < 3; j++) {
                final SedimentModel2DData smd = SedimentModel2DData.extract(dofs[j]);
                final CurrentModel2DData cmd = dof_data[dofs[j].number];
                final double wlambda = (flood > cmd.wlambda ? flood : cmd.wlambda);
                final double nonZeroTotalDepth = ((cmd.totaldepth < WATT) ? WATT : cmd.totaldepth);

                // Impulsgleichung x
                terms_u[j] =
                        // Druckterm
                        PhysicalParameters.G * detadx * wlambda
                                // density term
                                - 0.5 * PhysicalParameters.G * rhodx * cmd.totaldepth / cmd.rho * wlambda
                                // Advektionsterme
                                + (cmd.u * udx + cmd.v * udy)
                                // Coriolis
                                - cmd.v * Coriolis
                                // bottom friction
                                + cmd.bottomFrictionCoefficient * cmd.u * max(0.1, 1 - dzdx) / nonZeroTotalDepth
                                // wind
                                - cmd.tau_windx / cmd.rho / nonZeroTotalDepth * cmd.wlambda
                                // Radiationstresses
                                + (dsxxdx + dsxydy) / cmd.rho / nonZeroTotalDepth * cmd.wlambda
                                // KopplungsTerm aus der Herleitung der Formulierung von q -> v
                                + cmd.u / nonZeroTotalDepth * cureq1_mean * wlambda // Verbesserung in der
                                                                                    // Dammbruchsimulation / wlamda
                                                                                    // skaliert nonZeroTotalDepth gegen
                                                                                    // Null, falls der Knoten trocken
                                                                                    // faellt
                ;
                /* if (!cmd.boundary) */cureq2_mean += 1. / 3. * (cmd.dudt + terms_u[j]);

                // Impulsgleichung y
                terms_v[j] =
                        // Druckterm
                        PhysicalParameters.G * detady * wlambda
                                // density term
                                - 0.5 * PhysicalParameters.G * rhody * cmd.totaldepth / cmd.rho * wlambda
                                // Advektionsterme
                                + (cmd.u * vdx + cmd.v * vdy)
                                // Coriolis
                                + cmd.u * Coriolis
                                // bottom friction
                                + cmd.bottomFrictionCoefficient * cmd.v * max(0.1, 1 - dzdy) / nonZeroTotalDepth
                                // wind
                                - cmd.tau_windy / cmd.rho / nonZeroTotalDepth * cmd.wlambda
                                // Radiationstresses
                                + (dsxydx + dsyydy) / cmd.rho / nonZeroTotalDepth * cmd.wlambda
                                // KopplungsTerm aus der Herleitung der Formulierung von q -> v
                                + cmd.v / nonZeroTotalDepth * cureq1_mean * wlambda // Verbesserung in der
                                                                                    // Dammbruchsimulation / cmd.wlamda
                                                                                    // skaliert nonZeroTotalDepth gegen
                                                                                    // Null, falls der Knoten trocken
                                                                                    // faellt
                ;
                /* if (!cmd.boundary) */cureq3_mean += 1. / 3. * (cmd.dvdt + terms_v[j]);
                // ToDo ins Sedimentmodell
                if ((eleCurrentData.iwatt == 0) && (cmd.totaldepth > 0.1) && smd != null) { // secondary Current shear
                                                                                            // stress only in wett
                                                                                            // elements
                    double reduceFactor = (Math.abs(cureq1_mean * cmd.totaldepth) + 1.) * bottomslope;
                    reduceFactor *= reduceFactor * reduceFactor;
                    // reduceFactor *= reduceFactor; // hoch 6
                    // final double chezy = cmd.kst * Math.pow(cmd.totaldepth, 1./6.);
                    final double chezy = Math.sqrt(PhysicalParameters.G / smd.grainShearStress); // siehe Berechnung des
                                                                                                 // grainShearStress
                    final double alphaStar = 1; // 1 nach MIKE 21C mit gravitationellem Transport; 0.5 ohne grav.
                                                // Transport;
                    final double beta = alphaStar * 2. / PhysicalParameters.KARMANCONSTANT
                            / PhysicalParameters.KARMANCONSTANT * Function.max(0.,
                                    1. - PhysicalParameters.sqrtG / PhysicalParameters.KARMANCONSTANT / chezy);
                    // final double beta = 7.*0.75;
                    final double cv = (cmd.cv >= 0.1) ? cmd.cv : 0.1;// max(cmd.cv, 0.1);
                    final double r = smd.grainShearStress / cv;
                    final double coeff = beta * r / (ALPHA * cv * cv) * PhysicalParameters.G * cmd.totaldepth
                            * (cmd.u * detady - cmd.v * detadx)
                            / reduceFactor;
                    synchronized (cmd) {
                        cmd._tau_bx_extra -= coeff * (-cmd.v);
                        cmd._tau_by_extra -= coeff * (+cmd.u);
                    }
                }
            }

            final double c0 = Math.sqrt(PhysicalParameters.G * ((depth_mean < WATT) ? WATT : depth_mean)); // the
                                                                                                           // shallow
                                                                                                           // water wave
                                                                                                           // velocity
            // Operatornorm for each direction component and then again as euclidean vector
            // norm
            final double operatornorm_x = c0 + Math.abs(u_mean);
            final double operatornorm_y = c0 + Math.abs(v_mean);
            final double operatornorm = Math.sqrt(operatornorm_x * operatornorm_x + operatornorm_y * operatornorm_y);
            final double tau_cur = 0.5 * elementsize / operatornorm;
            timeStep = tau_cur;

            for (int j = 0; j < 3; j++) {

                final CurrentModel2DData cmd = dof_data[ele.getDOF(j).number];
                final double wlambda = (flood > cmd.wlambda ? flood : cmd.wlambda);

                // Fehlerkorrektur berechhnen
                double uCorrect = -tau_cur * (koeffmat[j][1] * u_mean * cureq2_mean
                        + koeffmat[j][1] * PhysicalParameters.G * cureq1_mean
                        + koeffmat[j][2] * v_mean * cureq2_mean);
                uCorrect -= (koeffmat[j][1] * astx * udx + koeffmat[j][2] * asty * udy);

                double vCorrect = -tau_cur * (koeffmat[j][1] * u_mean * cureq3_mean
                        + koeffmat[j][2] * PhysicalParameters.G * cureq1_mean
                        + koeffmat[j][2] * v_mean * cureq3_mean);
                vCorrect -= (koeffmat[j][1] * astx * vdx + koeffmat[j][2] * asty * vdy);

                double etaCorrect = -tau_cur * (koeffmat[j][1] * depth_mean * cureq2_mean * wlambda
                        + koeffmat[j][1] * u_mean * cureq1_mean
                        + koeffmat[j][2] * depth_mean * cureq3_mean * wlambda
                        + koeffmat[j][2] * v_mean * cureq1_mean);

                double puddleLambda = cmd.puddleLambda;

                double result_U_i = 0.;
                double result_V_i = 0.;
                double result_H_i = 0.;
                // Begin standart Galerkin-step
                for (int l = 0; l < 3; l++) {

                    final double wlambda_l = (flood > dof_data[ele.getDOF(l).number].wlambda ? flood
                            : dof_data[ele.getDOF(l).number].wlambda);

                    final double vorfak = ele.area * ((l == j) ? 1. / 6. : 1. / 12.);
                    double gl = (l == j) ? 1.
                            : Math.min(wlambda_l, dof_data[ele.getDOF(l).number].totaldepth
                                    / Math.max(CurrentModel2D.WATT, cmd.totaldepth));

                    // Impulse Equations
                    result_U_i -= vorfak * terms_u[l] * gl;
                    result_V_i -= vorfak * terms_v[l] * gl;

                    gl = (l == j) ? 1. : wlambda_l;
                    if (l != j && eleCurrentData.iwatt != 0) {
                        if (terms_eta[l] < 0) { // Wasserstand am abgelegenen Knoten will steigen
                            if (dof_data[ele.getDOF(l).number].eta < cmd.eta) { // Wasserstand am abgelegenen Knoten
                                                                                // liegt unterhalb
                                gl = cmd.wlambda * Function.max(0.,
                                        1. - (cmd.eta - dof_data[ele.getDOF(l).number].eta) / ele.distance[l][j]);
                            } else { // Wasserstand am abgelegenen Knoten liegt oberhalb
                                // gl = dof_data[ele.getDOF(l).number].wlambda;
                            }
                        } else { // Wasserstand am abgelegenen Knoten will fallen
                            if (dof_data[ele.getDOF(l).number].eta < cmd.eta) { // Wasserstand am abgelegenen Knoten
                                                                                // liegt unterhalb
                                gl = 1.;
                            } else { // Wasserstand am abgelegenen Knoten liegt oberhalb
                                gl = wlambda * Function.max(0.,
                                        1. - (dof_data[ele.getDOF(l).number].eta - cmd.eta) / ele.distance[l][j]);
                            }
                        }
                    }

                    // Conti Equation
                    result_H_i -= vorfak * terms_eta[l] * gl;

                    // puddledetection
                    if (l != j) {
                        puddleLambda = ((puddleLambda < dof_data[ele.getDOF(l).number].wlambda)
                                ? dof_data[ele.getDOF(l).number].wlambda
                                : puddleLambda);
                    }

                }

                synchronized (cmd) {
                    cmd.ru += result_U_i;
                    cmd.rv += result_V_i;
                    cmd.reta += result_H_i;

                    cmd.puddleLambda = ((cmd.puddleLambda < puddleLambda) ? puddleLambda : cmd.puddleLambda);

                    cmd._dhdx += detadx;
                    cmd._dhdy += detady;

                    cmd.ruCorrection += uCorrect * ele.area / 3.;
                    cmd.rvCorrection += vCorrect * ele.area / 3.;
                    cmd.retaCorrection += etaCorrect * ele.area / 3.;
                }
            }
        }
        return timeStep;
    } // end ElementApproximation

    /**
     * setBoundaryCondition
     * 
     * @param dof
     * @param t
     */
    @Override
    public final void setBoundaryCondition(DOF dof, double t) {

        final int i = dof.number;
        final CurrentModel2DData currentdata = dof_data[i];

        currentdata.puddleLambda = 0.;

        double d50; // in [m]

        final SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
        if (sedimentmodeldata == null) {
            final BathymetryData2D bathymetrymodeldata = BathymetryData2D.extract(dof);
            if (bathymetrymodeldata != null) {
                currentdata.z = bathymetrymodeldata.z;
            }
            // else {
            // currentdata.z = dof.z; // reicht beim initialiseren
            // }
            d50 = 0.0001 * 1.E-3; // [m]
        } else {
            // currentdata.z = sedimentmodeldata.z; // schon im Sedimentmodell gesetzt mit
            // setBottomLevel(z)
            // currentdata.dzdt = sedimentmodeldata.dzdt; // schon im
            // Sedimentmodell.timStep() gesetzt
            d50 = sedimentmodeldata.d50; // [m]
        }

        if (currentdata.bWeir != null) { // ToDo das verstehe ich nicht! Die Geschwindigkeiten werden am ENDE der
                                         // Methode gesetzt
            final FElement[] felem = dof.getFElements();
            for (FElement elem : felem) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            final CurrentModel2DData tmpcdata = dof_data[elem.getDOF((ll + ii) % 3).number];
                            if (tmpcdata.totaldepth > WATT) {
                                currentdata.setWaterLevel_synchronized(
                                        (999. * currentdata.eta + 1. * tmpcdata.eta) / 1000.);
                            }
                        }
                        break;
                    }
                }
            }
        }

        if (currentdata.bh != null) {
            currentdata.setWaterLevel_synchronized(currentdata.bh.getValue(t));
            currentdata.detadt = currentdata.bh.getDifferential(t);
        }

        /* extrapolate no exact defined boundary conditions */
        if ((currentdata.extrapolate_h || currentdata.extrapolate_u || currentdata.extrapolate_v)
                && (currentdata.totaldepth > WATT)) {
            for (FElement elem : dof.getFElements()) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            final CurrentModel2DData tmpcdata = dof_data[elem.getDOF((ll + ii) % 3).number];
                            if (tmpcdata.totaldepth > WATT) {
                                if (currentdata.extrapolate_h) {
                                    double dh = (currentdata.eta - tmpcdata.eta) / 10.;
                                    if (tmpcdata.extrapolate_h) {
                                        dh /= 10.;
                                    }
                                    currentdata.setWaterLevel_synchronized(currentdata.eta - dh);
                                    tmpcdata.setWaterLevel_synchronized(tmpcdata.eta + dh);
                                }
                                if (currentdata.extrapolate_u) {
                                    final double lambda = Function.min(1., currentdata.totaldepth / 5.);
                                    final double lambdaInv = 1. - lambda;
                                    double du = (currentdata.u - tmpcdata.u * (lambda + lambdaInv * tmpcdata.totaldepth
                                            / Function.max(currentdata.totaldepth, 10 * WATT))) / 10.;
                                    if (tmpcdata.extrapolate_u) {
                                        du /= 10.;
                                    }
                                    synchronized (currentdata) {
                                        currentdata.u -= du;
                                    }
                                }
                                if (currentdata.extrapolate_v) {
                                    final double lambda = Function.min(1., currentdata.totaldepth / 5.);
                                    final double lambdaInv = 1. - lambda;
                                    double dv = (currentdata.v - tmpcdata.v * (lambda + lambdaInv * tmpcdata.totaldepth
                                            / Function.max(currentdata.totaldepth, 10 * WATT))) / 10.;
                                    if (tmpcdata.extrapolate_v) {
                                        dv /= 10.;
                                    }
                                    synchronized (currentdata) {
                                        currentdata.v -= dv;
                                    }
                                }
                            } else {
                                if (currentdata.extrapolate_h) {
                                    double dh = (currentdata.eta - tmpcdata.eta) / 10. * tmpcdata.wlambda;
                                    if (tmpcdata.extrapolate_h) {
                                        dh /= 10.;
                                    }
                                    currentdata.setWaterLevel_synchronized(currentdata.eta - dh);
                                    if (dh < 0.)
                                        tmpcdata.setWaterLevel_synchronized(tmpcdata.eta + dh);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        /* extrapolate waterlevel on no exact defined boundary conditions */
        if (currentdata.extrapolate_h && currentdata.totaldepth < WATT) {
            final FElement[] felem = dof.getFElements();
            double minh = currentdata.eta;
            for (FElement elem : felem) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            final CurrentModel2DData tmpcdata = dof_data[elem.getDOF((ll + ii) % 3).number];
                            minh = Function.min(minh, tmpcdata.w1_lambda * minh + tmpcdata.wlambda * tmpcdata.eta); // Morphen
                        }
                        break;
                    }
                }
            }
            final double dh = (currentdata.eta - minh) / 100. * currentdata.w1_lambda;
            currentdata.setWaterLevel_synchronized(currentdata.eta - dh);
        }

        if (currentdata.bQx != null) {
            currentdata.bQx.update(dof_data, t);
            currentdata.u = currentdata.bQx.getValueAt(i);
            // System.out.println("Knoten "+i+" mit Qx nach U "+currentdata.u);
        }
        if (currentdata.bQy != null) {
            currentdata.bQy.update(dof_data, t);
            currentdata.v = currentdata.bQy.getValueAt(i);
            // System.out.println("Knoten "+i+" mit Qy nach V "+currentdata.v);
        }

        currentdata.totaldepth = currentdata.z + currentdata.eta;

        if (currentdata.bu != null) {
            currentdata.u = currentdata.bu.getValue(t);
            currentdata.dudt = currentdata.bu.getDifferential(t); // besonders relevant an geschlossenen Raendern mit
                                                                  // no-slip RB
        }
        if (currentdata.bv != null) {
            currentdata.v = currentdata.bv.getValue(t);
            currentdata.dvdt = currentdata.bv.getDifferential(t);
        }

        // double depthdt = 0.;
        if (currentdata.bqx != null) {
            if (currentdata.totaldepth > WATT) {
                currentdata.u = currentdata.bqx.getValue(t) / currentdata.totaldepth;
            } else {
                currentdata.u = currentdata.totaldepth / WATT * currentdata.bqx.getValue(t) / WATT;
            }
            // currentdata.dudt = (currentdata.bqx.getDifferential(t) - x[U + i]
            // *(depthdt+currentdata.detadt))/(currentdata.z+x[H + i]);
        }
        if (currentdata.bqy != null) {
            if (currentdata.totaldepth > WATT) {
                currentdata.v = currentdata.bqy.getValue(t) / currentdata.totaldepth;
            } else {
                currentdata.v = currentdata.totaldepth / WATT * currentdata.bqy.getValue(t) / WATT;
            }
            // currentdata.dvdt = (currentdata.bqy.getDifferential(t) - x[V + i]
            // *(depthdt+currentdata.detadt))/(currentdata.z+x[H + i]);
        }

        /* Wattstrategie fuer Stroemung */
        currentdata.wlambda = Function.min(1., currentdata.totaldepth / WATT);
        currentdata.w1_lambda = 1. - currentdata.wlambda;

        currentdata.cv = Function.norm(currentdata.u, currentdata.v);

        currentdata.bottomFrictionCoefficient = PhysicalParameters.KINVISCOSITY_WATER; // Bodenreibungskoeffizient

        // Effektive Wassertiefe fuer Reibungsberechnungen (wie in Ihrem Code)
        final double depthForFriction = (currentdata.totaldepth < 0.1) ? 0.1 : currentdata.totaldepth;

        // // bed roughness factor for dunes based on [Karim F. (1995): Bed
        // Configuration and Hydraulic Resistance in Alluvial-Channel Flows, Journal of
        // Hydraulic Engineering, ASCE, Vol.121, No.1, January]
        // double bedRoughnessFactor = 1.;
        // if (sedimentmodeldata != null) {
        // // ToDo vielleicht vorher noch das skalarprodukt mit dem
        // Geschwindigkeitsvektor bilden?
        // final double lambda = Math.min(1.,
        // Function.norm(sedimentmodeldata.duneLengthX,sedimentmodeldata.duneLengthY)/(4.*dof.meanEdgeLength));
        // // Verringerung der Rauheit aus Duehnen, wenn das Netz die Duene mit 4
        // Stuetzstellen selbst Abbilden kann
        // bedRoughnessFactor += (1. - lambda) *
        // 8.92*sedimentmodeldata.duneHeight/((currentdata.totaldepth < 0.1) ? 0.1 :
        // currentdata.totaldepth);
        // }
        double fmudLambda = 0.;
        final FluidMudFlowModel2DData fmuddata = FluidMudFlowModel2DData.extract(dof);
        if (fmuddata != null) {
            currentdata.z = Function.min(-fmuddata.m, currentdata.z);
            fmudLambda = Function.min(1., fmuddata.thickness / (10. * WATT));
            d50 = (1. - fmudLambda) * d50 + fmudLambda * 0.0001E-3; // morphing between d50 and clay [m]
        }
        // ** Nikuradse-Beiwerte
        // Kornrauheit fuer grain Friction (ks= 2 bis 3*d50 in Gaia/Telemac)
        final double ks_grain_effective = 2. * d50; // Peter 03.06.25 von 2 auf 3 gesetzt

        // dune rougness van Rijn 1993
        double ks_dune = 0.; // in [m]
        double duneLength = 0.01;
        if (sedimentmodeldata != null) {
            duneLength = Function.norm(sedimentmodeldata.duneLengthX, sedimentmodeldata.duneLengthY);
            final double lambda = Math.min(1., duneLength / (4. * dof.meanEdgeLength)); // Verringerung der Rauheit aus
                                                                                        // Duehnen, wenn das Netz die
                                                                                        // Duene mit 4 Stuetzstellen
                                                                                        // selbst Abbilden kann
            // ToDo vielleicht vorher noch das Skalarprodukt mit dem Geschwindigkeitsvektor
            // bilden um Lambda weiter zu verrringern
            ks_dune = (1. - lambda) * 1.1 * 0.7 * sedimentmodeldata.duneHeight
                    * (1. - Math.exp(-25 * sedimentmodeldata.duneHeight / Math.max(0.01, duneLength)));
        }

        // Bewuchs
        double ks_benthic = 0.; // in [m]
        final SpartinaAlternifloraModel2DData samd = SpartinaAlternifloraModel2DData.extract(dof);
        if (samd != null) {
            ks_benthic = CurrentModel2DData.Strickler2Nikuradse(samd.getStrickler(currentdata.totaldepth));
        }
        // bottom friction coefficient
        // minimaler Chezybeiwert wird zu 5 gesetzt
        if (nikuradse) {

            double ks = Math.max(currentdata.ks, ks_grain_effective + ks_benthic + ks_dune); // in [m] // Peter
                                                                                             // 16.01.2025
            // Schlammauflage
            if (fmuddata != null)
                ks = Math.max(0., ks - fmuddata.thickness); // Peter 16.01.2025
            // final double k=0.41;// Karman-Konstante (k=0,41)
            // z0 = 0.033*ks bei Re* > 3.3
            currentdata.bottomFrictionCoefficient += PhysicalParameters.G
                    / Function.sqr(18. * Math.log10(12. * depthForFriction / ks)); // Colebrooks / Nikuradse

        } else {

            final double kst = (1. - fmudLambda) * currentdata.kst + fmudLambda * 60; // Erhoehung bei Schlammauflage
                                                                                      // auf sehr glatt
            double ks = ks_grain_effective + ks_benthic + ks_dune; // in [m]
            // Schlammauflage
            if (fmuddata != null)
                ks = Math.max(0., ks - fmuddata.thickness);
            // Strickler
            currentdata.bottomFrictionCoefficient += PhysicalParameters.G / Math.cbrt(depthForFriction) / Function
                    .sqr(Function.min(kst, CurrentModel2DData.Nikuradse2Strickler(ks, currentdata.totaldepth)));
            // double kst_skin = 26 * Math.pow(d50, 1./6.);
            // double Chezy_skin = kst_skin * Math.pow(depthForFriction, 1./6.);
            // currentdata.grainShearStress = PhysicalParameters.G /
            // Function.sqr(Chezy_skin)*currentdata.cv; // * rho spaeter
        }
        currentdata.bottomFrictionCoefficient *= currentdata.cv; // * rho und Richtungsvektor der Geschwindigkeit
                                                                 // spaeter

        /* wind stress coeffizient */
        /* Smith and Banke (1975) */
        if (currentdata.totaldepth > WATT) {
            final MeteorologyData2D meteorologyData2D = MeteorologyData2D.extract(dof);
            if (meteorologyData2D != null) {
                double tau_wind = (0.63 + 0.066 * meteorologyData2D.windspeed) * 1.E-3 * PhysicalParameters.RHO_AIR;
                if (currentdata.bu == null)
                    currentdata.tau_windx = tau_wind * meteorologyData2D.windspeed * meteorologyData2D.windx;
                else
                    currentdata.tau_windx = 0.;
                if (currentdata.bv == null)
                    currentdata.tau_windy = tau_wind * meteorologyData2D.windspeed * meteorologyData2D.windy;
                else
                    currentdata.tau_windy = 0.;
            }
        } else {
            currentdata.tau_windx = 0.;
            currentdata.tau_windy = 0.;
        }

        // aktualisieren der Wasserdichte
        currentdata.rho = PhysicalParameters.RHO_WATER_10;
        // Temperature
        final HeatTransportModel2DData temperature = HeatTransportModel2DData.extract(dof);
        // Saltdata
        final SaltModel2DData saltconcentration = SaltModel2DData.extract(dof);
        if (temperature != null && saltconcentration != null) {
            currentdata.rho = PhysicalParameters.rhoWater(temperature.temperature, saltconcentration.C);
        } else {
            if (temperature != null)
                currentdata.rho = PhysicalParameters.rhoWater(temperature.temperature);
            if (saltconcentration != null)
                currentdata.rho = PhysicalParameters.rhoWater(10, saltconcentration.C);
        }
        // Suspended Load
        if (sedimentmodeldata != null) {
            currentdata.rho += sedimentmodeldata.sC * (PhysicalParameters.RHO_SEDIM - currentdata.rho);
        }

        // Wehrimplementierung
        if (currentdata.bWeir != null) {
            double[] qu = currentdata.bWeir.getV(dof, currentdata.eta, t);

            if (currentdata.cv > Function.norm(qu[0], qu[1]) && !(currentdata.bWeir instanceof BroadCrestedTopoWeir)) {
                currentdata.u = qu[0];
                currentdata.v = qu[1];
                currentdata.cv = Function.norm(qu[0], qu[1]);
            }
        }
    } // end setBoundaryCondition

    @Override
    public ModelData genData(FElement felement) {
        FTriangle ele = (FTriangle) felement;
        Current2DElementData res = new Current2DElementData();
        CurrentModel2DData cmd0 = CurrentModel2DData.extract(ele.getDOF(0));
        CurrentModel2DData cmd1 = CurrentModel2DData.extract(ele.getDOF(1));
        CurrentModel2DData cmd2 = CurrentModel2DData.extract(ele.getDOF(2));

        res.withWeir = (cmd0.bWeir != null) || (cmd1.bWeir != null) || (cmd2.bWeir != null);

        element_data[felement.number] = res;
        return res;
    }

    /**
     * genData generate the nessecery modeldatas for a DOF
     * 
     * @param dof
     * @return
     */
    @Override
    public ModelData genData(DOF dof) {
        // System.out.println("DOF "+dof);
        CurrentModel2DData data = new CurrentModel2DData(dof);
        int dofnumber = dof.number;
        dof_data[dofnumber] = data;

        data.kst = currentdat.constantStrickler;
        data.ks = currentdat.constantNikuradse;

        for (BoundaryCondition bcond : bqx) {
            if (dofnumber == bcond.pointnumber) {
                data.bqx = bcond.function;
                bqx.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for (BoundaryCondition bcond : bqy) {
            if (dofnumber == bcond.pointnumber) {
                data.bqy = bcond.function;
                bqy.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for (BoundaryCondition bcond : bu) {
            if (dofnumber == bcond.pointnumber) {
                data.bu = bcond.function;
                bu.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for (BoundaryCondition bcond : bv) {
            if (dofnumber == bcond.pointnumber) {
                data.bv = bcond.function;
                bv.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for (BoundaryCondition bcond : bh) {
            if (dofnumber == bcond.pointnumber) {
                data.bh = bcond.function;
                bh.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for (BoundaryCondition bcond : bQx) {
            if (dofnumber == bcond.pointnumber) {
                data.bQx = (QSteuerung) bcond.function;
                bQx.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for (BoundaryCondition bcond : bQy) {
            if (dofnumber == bcond.pointnumber) {
                data.bQy = (QSteuerung) bcond.function;
                bQy.remove(bcond);
                data.boundary = true;
                break;
            }
        }

        for (BoundaryCondition bcond : sQ) {
            if (dofnumber == bcond.pointnumber) {
                data.sourceQ = bcond.function;
                sQ.remove(bcond);
                break;
            }
        }

        for (BoundaryCondition bcond : sh) {
            if (dofnumber == bcond.pointnumber) {
                data.sourceh = bcond.function;
                sh.remove(bcond);
                break;
            }
        }

        return data;
    } // end genData

    /**
     * the method readStricklerCoeff read the datas for strickler coefficients
     * from a JanetBinary-file named filename
     * 
     * @param nam name of the file to be open
     */
    @SuppressWarnings("unused")
    private void readStricklerCoeffFromJanetBin(String filename) {
        int anzAttributes = 0;
        double x, y, kst;
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
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version
                        + ", current version: 1.8");
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
                    x = bin_in.fbinreaddouble();
                    y = bin_in.fbinreaddouble();
                    kst = bin_in.fbinreaddouble(); // -> kst
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(kst) || kst <= 0.) {
                        hasValidValues = false;
                    }
                    DOF dof = fenet.getDOF(nr);
                    CurrentModel2DData currentdata = CurrentModel2DData.extract(dof);
                    currentdata.kst = kst;
                    currentdata.ks = CurrentModel2DData.Strickler2Nikuradse(kst);

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
                    System.exit(1);
                }
            } else {
                System.out.println("system und strickler.jbf different number of nodes");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
            System.exit(1);
        }

        // Anzahl der Elemente
        FTriangle[] element = (FTriangle[]) fenet.getFElements();
        for (FTriangle element1 : element) {
            DOF[] dofs = element1.getDOFs();
            double smean = 0.;
            for (int j = 0; j < 3; j++) {
                smean += CurrentModel2DData.extract(dofs[j]).kst;
            }
            Current2DElementData.extract(element1).meanStricklerCoefficient = smean / 3.;
        }
    }

    /**
     * the method readStricklerCoeff read the datas for strickler coefficients
     * from a sysdat-file named nam
     * 
     * @param nam name of the file to be open
     */
    @SuppressWarnings("unused")
    private void readStricklerCoeff(String filename) {
        int knoten_nr;
        double x, y, kst;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading Bottom-Friction-File (in TiCAD-System.Dat-Format): " + filename);

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

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000) {
                throw new Exception("Fehler");
            }

            if ((rand_knoten + gebiets_knoten) != fenet.getNumberofDOFs()) {
                System.out.println("system und strickler.dat different number of nodes");
                System.exit(1);
            }

            // System.out.println(""+rand_knoten+" "+gebiets_knoten);

            // Knoten einlesen
            // DOF[] dof= new DOF[rand_knoten+gebiets_knoten];
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
                        kst = Double.parseDouble(strto.nextToken());
                    } catch (NumberFormatException ex) {
                        kst = Double.NaN;
                    }

                    if (Double.isNaN(kst) || kst < 0) {

                        System.out.println("");

                        System.out.println(
                                "********************************       ERROR         ***********************************");
                        System.out.println("Invalid z-value (z=NaN or z<0.0) in Bottom Friction-Mesh: <" + filename
                                + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count
                                + "> has a correct floating point (greater zero)");
                        System.out.println("bottom friction value");
                        System.out.println(
                                "*****************************************************************************************");
                        System.out.println("");
                        System.exit(1);
                    }
                    DOF dof = fenet.getDOF(knoten_nr);
                    CurrentModel2DData currentdata = CurrentModel2DData.extract(dof);
                    currentdata.kst = kst;
                    currentdata.ks = CurrentModel2DData.Strickler2Nikuradse(kst);

                    p_count++;
                }

            }
        } catch (Exception e) {
            System.out.println("cannot open file: " + filename);
            System.exit(1);
        }

        // Anzahl der Elemente
        FTriangle[] element = (FTriangle[]) fenet.getFElements();
        for (FTriangle element1 : element) {
            DOF[] dofs = element1.getDOFs();
            double smean = 0.;
            for (int j = 0; j < 3; j++) {
                smean += CurrentModel2DData.extract(dofs[j]).kst;
            }
            Current2DElementData.extract(element1).meanStricklerCoefficient = smean / 3.;
        }

    }

    /**
     * the method read Nikuradse coefficients datas in [mm]
     * from a JanetBinary-file named filename
     * 
     * @param nam name of the file to be open
     */
    @SuppressWarnings("unused")
    private void readNikuradseCoeffFromJanetBin(String filename) {

        nikuradse = true;

        int anzAttributes = 0;
        double x, y, ks;
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
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version
                        + ", current version: 1.8");
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
                    x = bin_in.fbinreaddouble();
                    y = bin_in.fbinreaddouble();
                    ks = bin_in.fbinreaddouble(); // -> ks
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(ks) || ks <= 0.) {
                        hasValidValues = false;
                    }

                    DOF dof = fenet.getDOF(nr);
                    CurrentModel2DData currentdata = CurrentModel2DData.extract(dof);
                    currentdata.ks = ks; // [mm]
                    currentdata.kst = CurrentModel2DData.Nikuradse2Strickler(ks); // nach
                                                                                  // http://www.baw.de/vip/abteilungen/wbk/Publikationen/scn/sc1-99a/node21.htm

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
                    System.exit(1);
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

        // Anzahl der Elemente
        FTriangle[] element = (FTriangle[]) fenet.getFElements();
        for (FTriangle element1 : element) {
            DOF[] dofs = element1.getDOFs();
            double smean = 0.;
            for (int j = 0; j < 3; j++) {
                smean += CurrentModel2DData.extract(dofs[j]).kst;
            }
            Current2DElementData.extract(element1).meanStricklerCoefficient = smean / 3.;
        }
    }

    /**
     * the method readNikuradseCoeff read the datas for Nikuradse coefficients in
     * [mm]
     * from a sysdat-file named nam
     * 
     * @param nam name of the file to be open
     */
    @SuppressWarnings("unused")
    private void readNikuradseCoeff(String filename) {

        nikuradse = true;

        int knoten_nr;
        double x, y, ks;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading Nikuradse Coefficients from file (in TiCAD-System.Dat-Format): " + filename);

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

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000) {
                throw new Exception("Fehler");
            }

            // System.out.println(""+rand_knoten+" "+gebiets_knoten);

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
                        ks = Double.parseDouble(strto.nextToken());
                    } catch (NumberFormatException ex) {
                        ks = Double.NaN;
                    }

                    if (Double.isNaN(ks) || ks < 0) {

                        System.out.println("");

                        System.out.println(
                                "********************************       ERROR         ***********************************");
                        System.out.println("Invalid z-value (z=NaN or z<0.0) in Bottom Friction-Mesh: <" + filename
                                + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count
                                + "> has a correct floating point (greater zero)");
                        System.out.println("bottom friction value");
                        System.out.println(
                                "*****************************************************************************************");
                        System.out.println("");
                        System.exit(1);
                    }
                    DOF dof = fenet.getDOF(knoten_nr);
                    CurrentModel2DData currentdata = CurrentModel2DData.extract(dof);
                    currentdata.ks = ks; // [mm]
                    currentdata.kst = CurrentModel2DData.Nikuradse2Strickler(ks); // nach
                                                                                  // http://www.baw.de/vip/abteilungen/wbk/Publikationen/scn/sc1-99a/node21.htm

                    p_count++;
                }

            }
        } catch (Exception e) {
            System.out.println("cannot open file: " + filename);
            System.exit(1);
        }

        // Anzahl der Elemente
        FTriangle[] element = (FTriangle[]) fenet.getFElements();
        for (FTriangle element1 : element) {
            DOF[] dofs = element1.getDOFs();
            double smean = 0.;
            for (int j = 0; j < 3; j++) {
                smean += CurrentModel2DData.extract(dofs[j]).kst;
            }
            Current2DElementData.extract(element1).meanStricklerCoefficient = smean / 3.;
        }

    }

    /**
     * The method write_erg_xf
     * 
     * @param erg
     * @param t
     * @deprecated
     */
    @Override
    @Deprecated
    public void write_erg_xf(double[] erg, double t) {
        throw new UnsupportedOperationException("Not implemented yet");
    } // end write_erg_xf

    @Override
    public int getTicadErgMask() {
        return TicadIO.HRES_Z | TicadIO.HRES_V | TicadIO.HRES_H | TicadIO.HRES_SHEAR;
    }

    /** The method write_erg_xf */
    @Override
    public final void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);

            for (DOF dof : fenet.getDOFs()) {
                CurrentModel2DData current = dof_data[dof.number];
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                xf_os.writeFloat((float) current.z);
                if (current.totaldepth < WATT && MarinaXML.release) {
                    xf_os.writeFloat(0.f);
                    xf_os.writeFloat(0.f);
                } else {
                    xf_os.writeFloat((float) current.u);
                    xf_os.writeFloat((float) current.v);
                }
                xf_os.writeFloat((float) current.eta); // skalar1
                if (current.totaldepth < WATT && MarinaXML.release) {
                    xf_os.writeFloat(0.f);
                    xf_os.writeFloat(0.f);
                } else {
                    double taux = (current.bottomFrictionCoefficient * current.u + current.tau_bx_extra) * current.rho; // ToDo
                                                                                                                        // Schubspannung
                                                                                                                        // aus
                                                                                                                        // der
                                                                                                                        // Orbitalgeschwindigkeit
                    double tauy = (current.bottomFrictionCoefficient * current.v + current.tau_bx_extra) * current.rho;
                    xf_os.writeFloat((float) taux);
                    xf_os.writeFloat((float) tauy);
                }
            }
            xf_os.flush();
        } catch (IOException e) {
            System.out.println(this.getClass() + "\n\ttime=" + time + "\n");
            e.printStackTrace();
            System.exit(1);
        }
    }
    // end readBoundCond
    // end write_erg_xf

    private void readWeirXML(String wehrDateiName) {
        try {
            System.out.println("\tRead weirs parameter from " + wehrDateiName);
            JAXBContext jc = JAXBContext.newInstance("de.smile.xml.marina.weirs");
            Unmarshaller u = jc.createUnmarshaller();
            de.smile.xml.marina.weirs.Weirs weirsList = (de.smile.xml.marina.weirs.Weirs) u
                    .unmarshal(new FileInputStream(wehrDateiName));
            List<TWeir> list = weirsList.getWeir();
            for (TWeir w : list) {
                List<Integer> nodes = w.getListofNodes().getNodeID();
                int[] knotennummern = new int[nodes.size()];
                int i = 0;
                for (int j : nodes) {
                    knotennummern[i] = j;
                    i++;
                }
                // BroadCrestedWeir
                if (w.getWeirType().getBroadCrestedWeir() != null) {
                    if (w.getWeirType().getBroadCrestedWeir().getCrestLevel() != null) {
                        if (w.getWeirType().getBroadCrestedWeir().getCrestLevel().getConstant() != null) {
                            double crestLevel = w.getWeirType().getBroadCrestedWeir().getCrestLevel().getConstant();
                            new BroadCrestedWeir(crestLevel, knotennummern, fenet);
                        }
                        if (w.getWeirType().getBroadCrestedWeir().getCrestLevel().getTimeSeries() != null) {
                            TTimeSeries tsh = w.getWeirType().getBroadCrestedWeir().getCrestLevel().getTimeSeries();
                            List<TItem> timeList = tsh.getItem();
                            int anz = timeList.size();
                            double[][] value = new double[2][anz];
                            i = 0;
                            for (TItem it : timeList) {
                                value[0][i] = it.getTime();
                                value[1][i] = it.getValue();
                                i++;
                            }
                            DiscretScalarFunction1d crestLevelFunnction = new DiscretScalarFunction1d(value);
                            new TimeDependentBroadCrestedWeir(crestLevelFunnction, knotennummern, fenet);
                        }
                        if (w.getWeirType().getBroadCrestedWeir().getCrestLevel().getWaterLevelControlled() != null) { // Wasserspiegelkontrolliert
                            ScalarFunction1d waterLevelFunction = null;
                            if (w.getWeirType().getBroadCrestedWeir().getCrestLevel().getWaterLevelControlled()
                                    .getWaterLevel().getConstant() != null) {
                                double waterLevel = w.getWeirType().getBroadCrestedWeir().getCrestLevel()
                                        .getWaterLevelControlled().getWaterLevel().getConstant();
                                waterLevelFunction = new ConstantFunction1d(waterLevel);
                            }
                            if (w.getWeirType().getBroadCrestedWeir().getCrestLevel().getWaterLevelControlled()
                                    .getWaterLevel().getTimeSeries() != null) {
                                TTimeSeries tsh = w.getWeirType().getBroadCrestedWeir().getCrestLevel()
                                        .getWaterLevelControlled().getWaterLevel().getTimeSeries();
                                List<TItem> timeList = tsh.getItem();
                                int anz = timeList.size();
                                double[][] value = new double[2][anz];
                                i = 0;
                                for (TItem it : timeList) {
                                    value[0][i] = it.getTime();
                                    value[1][i] = it.getValue();
                                    i++;
                                }
                                waterLevelFunction = new DiscretScalarFunction1d(value);
                            }

                            nodes = w.getWeirType().getBroadCrestedWeir().getCrestLevel().getWaterLevelControlled()
                                    .getListofReferenceNodes().getNodeID();
                            int[] mesureNodeID = new int[nodes.size()];
                            i = 0;
                            for (int j : nodes) {
                                mesureNodeID[i] = j;
                                i++;
                            }
                            WaterLevelControlledBroadCrestedWeir weir = new WaterLevelControlledBroadCrestedWeir(
                                    mesureNodeID, waterLevelFunction, knotennummern, fenet);
                            // Faktoren fuer den Regler lesen
                            if (w.getWeirType().getBroadCrestedWeir().getCrestLevel().getWaterLevelControlled()
                                    .getPControllerFactor() != null) {
                                weir.propFactor = w.getWeirType().getBroadCrestedWeir().getCrestLevel()
                                        .getWaterLevelControlled().getPControllerFactor();
                            }
                            if (w.getWeirType().getBroadCrestedWeir().getCrestLevel().getWaterLevelControlled()
                                    .getDControllerFactor() != null) {
                                weir.diffFactor = w.getWeirType().getBroadCrestedWeir().getCrestLevel()
                                        .getWaterLevelControlled().getDControllerFactor();
                            }
                            // initiale Wehrhoehe setzen, wenn vorhanden
                            try {
                                weir.setCrestLevel(w.getWeirType().getBroadCrestedWeir().getCrestLevel()
                                        .getWaterLevelControlled().getInitalCrestLevel());
                            } catch (Exception ex) {
                            }
                        }
                    } else {
                        System.out.println("tag CrestLevel expected");
                    }
                }
                // BroadCrestedTopoWeir
                if (w.getWeirType().getBroadCrestedTopoWeir() != null) {
                    if (w.getWeirType().getBroadCrestedTopoWeir().getCrestLevel() != null) {
                        if (w.getWeirType().getBroadCrestedTopoWeir().getCrestLevel().getConstant() != null) {
                            double crestLevel = w.getWeirType().getBroadCrestedTopoWeir().getCrestLevel().getConstant();
                            new BroadCrestedTopoWeir(crestLevel, knotennummern, fenet);
                        }
                        if (w.getWeirType().getBroadCrestedTopoWeir().getCrestLevel().getTimeSeries() != null) {
                            TTimeSeries tsh = w.getWeirType().getBroadCrestedTopoWeir().getCrestLevel().getTimeSeries();
                            List<TItem> timeList = tsh.getItem();
                            int anz = timeList.size();
                            double[][] value = new double[2][anz];
                            i = 0;
                            for (TItem it : timeList) {
                                value[0][i] = it.getTime();
                                value[1][i] = it.getValue();
                                i++;
                            }
                            DiscretScalarFunction1d crestLevelFunction = new DiscretScalarFunction1d(value);
                            new TimeDependentBroadCrestedTopoWeir(crestLevelFunction, knotennummern, fenet);
                        }
                    } else {
                        System.out.println("tag CrestLevel expected");
                    }
                }

                // UnderFlowTopoWeir
                if (w.getWeirType().getUnderFlowTopoWeir() != null) {
                    if (w.getWeirType().getUnderFlowTopoWeir().getSluiceLevel() != null) {
                        if (w.getWeirType().getUnderFlowTopoWeir().getSluiceLevel().getConstant() != null) {
                            double crestLevel = w.getWeirType().getUnderFlowTopoWeir().getSluiceLevel().getConstant();
                            new UnderFlowTopoWeir(crestLevel, knotennummern, fenet);
                        }
                        if (w.getWeirType().getUnderFlowTopoWeir().getSluiceLevel().getTimeSeries() != null) {
                            TTimeSeries tsh = w.getWeirType().getUnderFlowTopoWeir().getSluiceLevel().getTimeSeries();
                            List<TItem> timeList = tsh.getItem();
                            int anz = timeList.size();
                            double[][] value = new double[2][anz];
                            i = 0;
                            for (TItem it : timeList) {
                                value[0][i] = it.getTime();
                                value[1][i] = it.getValue();
                                i++;
                            }
                            DiscretScalarFunction1d sluiceLevelFunction = new DiscretScalarFunction1d(value);
                            new TimeDependentUnderFlowTopoWeir(sluiceLevelFunction, knotennummern, fenet);
                        }
                    } else {
                        System.out.println("tag SluiceLevel expected");
                    }
                }

                // UnderFlowWeir
                if (w.getWeirType().getUnderFlowWeir() != null) {
                    if (w.getWeirType().getUnderFlowWeir().getSluiceLevel() != null) {
                        if (w.getWeirType().getUnderFlowWeir().getSluiceLevel().getConstant() != null) {
                            double crestLevel = w.getWeirType().getUnderFlowWeir().getSluiceLevel().getConstant();
                            new UnderFlowWeir(crestLevel, knotennummern, fenet);
                        }
                        if (w.getWeirType().getUnderFlowWeir().getSluiceLevel().getTimeSeries() != null) {
                            TTimeSeries tsh = w.getWeirType().getUnderFlowWeir().getSluiceLevel().getTimeSeries();
                            List<TItem> timeList = tsh.getItem();
                            int anz = timeList.size();
                            double[][] value = new double[2][anz];
                            i = 0;
                            for (TItem it : timeList) {
                                value[0][i] = it.getTime();
                                value[1][i] = it.getValue();
                                i++;
                            }
                            DiscretScalarFunction1d sluiceLevelFunction = new DiscretScalarFunction1d(value);
                            new TimeDependentUnderFlowWeir(sluiceLevelFunction, knotennummern, fenet);
                        }
                    } else {
                        System.out.println("tag SluiceLevel expected");
                    }
                }

                // NeedleWeir
                if (w.getWeirType().getNeedleWeir() != null) {
                    if (w.getWeirType().getNeedleWeir().getOpening() != null) {
                        if (w.getWeirType().getNeedleWeir().getOpening().getConstant() != null) {
                            double opening = w.getWeirType().getNeedleWeir().getOpening().getConstant();
                            new NeedleWeir(opening, knotennummern, fenet);
                        }
                        if (w.getWeirType().getNeedleWeir().getOpening().getTimeSeriesOfOpening() != null) {
                            TTimeSeries tsh = w.getWeirType().getNeedleWeir().getOpening().getTimeSeriesOfOpening();
                            List<TItem> timeList = tsh.getItem();
                            int anz = timeList.size();
                            double[][] value = new double[2][anz];
                            i = 0;
                            for (TItem it : timeList) {
                                value[0][i] = it.getTime();
                                value[1][i] = it.getValue();
                                i++;
                            }
                            DiscretScalarFunction1d crestLevelFunnction = new DiscretScalarFunction1d(value);
                            new TimeDependentNeedleWeir(crestLevelFunnction, knotennummern, fenet);
                        }
                        if (w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled() != null) { // Wasserspiegelkontrolliert
                            ScalarFunction1d waterLevelFunction = null;
                            if (w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled().getWaterLevel()
                                    .getConstant() != null) {
                                waterLevelFunction = new ConstantFunction1d(w.getWeirType().getNeedleWeir().getOpening()
                                        .getWaterLevelControlled().getWaterLevel().getConstant());
                            }
                            if (w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled().getWaterLevel()
                                    .getTimeSeries() != null) {
                                TTimeSeries tsh = w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled()
                                        .getWaterLevel().getTimeSeries();
                                List<TItem> timeList = tsh.getItem();
                                int anz = timeList.size();
                                double[][] value = new double[2][anz];
                                i = 0;
                                for (TItem it : timeList) {
                                    value[0][i] = it.getTime();
                                    value[1][i] = it.getValue();
                                    i++;
                                }
                                waterLevelFunction = new DiscretScalarFunction1d(value);
                            }
                            nodes = w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled()
                                    .getListofReferenceNodes().getNodeID();
                            int[] mesureNodeID = new int[nodes.size()];
                            i = 0;
                            for (int j : nodes) {
                                mesureNodeID[i] = j;
                                i++;
                            }
                            WaterLevelControlledNeedleWeir weir = new WaterLevelControlledNeedleWeir(mesureNodeID,
                                    waterLevelFunction, knotennummern, fenet);
                            // Faktoren fuer den Regler lesen
                            if (w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled()
                                    .getPControllerFactor() != null) {
                                weir.propFactor = w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled()
                                        .getPControllerFactor();
                            }
                            if (w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled()
                                    .getDControllerFactor() != null) {
                                weir.diffFactor = w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled()
                                        .getDControllerFactor();
                            }
                            // Initiale Offnungsverhaeltnis setzen [0-geschlossen, 1-Offen]
                            try {
                                weir.setOpening(w.getWeirType().getNeedleWeir().getOpening().getWaterLevelControlled()
                                        .getInitalOpening());
                            } catch (Exception ex) {
                            }
                        }
                    } else {
                        System.out.println("tag Opening expected");
                    }
                }
            }

        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void timeStep(double dt) {

        resultIsNaN = false;

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

            final CurrentModel2DData cmd = dof_data[dof.number];
            final SedimentModel2DData smd = SedimentModel2DData.extract(dof);

            final int gamma = dof.getNumberofFElements();

            cmd.dhdx = cmd._dhdx / gamma;
            cmd._dhdx = 0.;
            cmd.dhdy = cmd._dhdy / gamma;
            cmd._dhdy = 0.;

            cmd.ru /= dof.lumpedMass;
            cmd.rv /= dof.lumpedMass;
            cmd.reta /= dof.lumpedMass;

            cmd.ruCorrection /= dof.lumpedMass;
            cmd.rvCorrection /= dof.lumpedMass;
            cmd.retaCorrection /= dof.lumpedMass;

            cmd.tau_bx_extra = cmd._tau_bx_extra / gamma;
            cmd.tau_by_extra = cmd._tau_by_extra / gamma;
            cmd._tau_bx_extra = 0.;
            cmd._tau_by_extra = 0.;

            double ru = beta0 * cmd.ru + beta1 * cmd.dudt; // zusaetzlichen Stabilisierung in Anlehnung am expliziten
                                                           // Adams-Bashford 2. Ordnung mit variabler Schrittweite
            double ruCorrection = beta0 * cmd.ruCorrection + beta1 * cmd.duCdt;
            ru += ruCorrection;
            double rv = beta0 * cmd.rv + beta1 * cmd.dvdt; // zusaetzlichen Stabilisierung in Anlehnung am expliziten
                                                           // Adams-Bashford 2. Ordnung mit variabler Schrittweite
            double rvCorrection = beta0 * cmd.rvCorrection + beta1 * cmd.dvCdt;
            rv += rvCorrection;
            double reta = beta0 * cmd.reta + beta1 * cmd.detadt; // zusaetzlichen Stabilisierung in Anlehnung am
                                                                 // expliziten Adams-Bashford 2. Ordnung mit variabler
                                                                 // Schrittweite
            double retaCorrection = beta0 * cmd.retaCorrection + beta1 * cmd.detaCdt;
            reta += retaCorrection;

            cmd.dudt = cmd.ru;
            cmd.ru = 0.;
            cmd.dvdt = cmd.rv;
            cmd.rv = 0.;
            cmd.detadt = cmd.reta;
            cmd.reta = 0.;

            cmd.duCdt = cmd.ruCorrection;
            cmd.ruCorrection = 0.;
            cmd.dvCdt = cmd.rvCorrection;
            cmd.rvCorrection = 0.;
            cmd.detaCdt = cmd.retaCorrection;
            cmd.retaCorrection = 0.;

            // Quellen und Senken fuer das Oberflaechenwasser bestimmen
            double source_dhdt = 0;
            if (cmd.sourceh != null) {
                source_dhdt = cmd.sourceh.getValue(time);
            }
            if (cmd.sourceQ != null) {
                double area = 0.;
                FElement[] feles = dof.getFElements();
                for (FElement fele : feles) { // muss nicht jedes mal neu berechet werden
                    area += fele.getVolume();
                }
                source_dhdt = cmd.sourceQ.getValue(time) / area;
            }
            final GroundWater2DData gwdata = GroundWater2DData.extract(dof);
            if (gwdata != null) {
                if ((cmd.z + gwdata.h) >= 0.) {
                    source_dhdt += gwdata.dhdt;
                } else
                    source_dhdt -= gwdata.kf;
            } else {
                source_dhdt -= infiltrationRate;
            }
            reta += source_dhdt;
            reta -= (1.E-6 + infiltrationRate) * (1. - cmd.puddleLambda); // Versickern in Pfuezen

            cmd.u += dt * ru;
            cmd.v += dt * rv;
            cmd.u *= cmd.puddleLambda;
            cmd.v *= cmd.puddleLambda; // in Pfuetzen keine Stroemung

            cmd.setWaterLevel(cmd.eta + dt * reta);
            // ToDo Sedimentmodel?!
            if (smd != null) {
                cmd.tauBx = cmd.rho * (smd.grainShearStress * cmd.u + cmd.tau_bx_extra);
                cmd.tauBy = cmd.rho * (smd.grainShearStress * cmd.v + cmd.tau_by_extra);
            } else {
                cmd.tauBx = cmd.rho * cmd.tau_bx_extra;
                cmd.tauBy = cmd.rho * cmd.tau_by_extra;
            }

            boolean rIsNaN = Double.isNaN(ru) || Double.isNaN(rv) || Double.isNaN(reta);
            if (rIsNaN) {
                System.out.println(
                        "CurrentModel2D is NaN bei " + dof.number + " dh/dt=" + reta + " du/dt=" + ru + " dv/dt=" + rv);
            }
            resultIsNaN |= rIsNaN;
        });

        // Aktualisiere den vorherigen Zeitschritt für das gesamte Modell
        this.previousTimeStep = dt;
        this.time += dt;

        if (resultIsNaN) {
            System.out.println("Time=" + this.time + " and timestep is " + dt);
            write_erg_xf();
            try {
                xf_os.close();
            } catch (IOException e) {
            }
            System.exit(1);
        }
    }
}
