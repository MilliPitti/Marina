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
import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.ecological.SpartinaAlternifloraModel2DData;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import static de.smile.math.Function.norm;
import java.io.*;
import java.util.*;

/**
 * this Class describe a depth integrated fluid mud flow based on depth
 * integrated equations
 * 
 * @version 3.18.0
 * @author Peter Milbradt
 */
public class FluidMudFlowModel2D extends TimeDependentFEApproximation
        implements FEModel, TicadModel, TimeDependentModel {

    FluidMudFlowModel2DData[] dof_data = null;

    private DataOutputStream xf_os = null;

    private Vector<BoundaryCondition> bqx = new Vector<>();
    private Vector<BoundaryCondition> bqy = new Vector<>();
    private Vector<BoundaryCondition> bu = new Vector<>();
    private Vector<BoundaryCondition> bv = new Vector<>();
    private Vector<BoundaryCondition> bh = new Vector<>();
    private Vector<BoundaryCondition> bQx = new Vector<>();
    private Vector<BoundaryCondition> bQy = new Vector<>();
    private Vector<BoundaryCondition> sQ = new Vector<>();
    private Vector<BoundaryCondition> sh = new Vector<>();

    private Vector<BoundaryCondition> bsc = new Vector<>();

    private Vector<DOF> inith = new Vector<>();
    private FluidMudFlowDat fluidmuddat;
    static final double T_0 = 4.; // [C]
    static final double alpha_T = 7.e-6; // [K^-2]
    static final double alpha_S = 750.e-6; // [ppt^-1]
    static final double alpha = .75; // coefficient for secondary flow [0.75 rough bootom, 1. smooth]
    double infiltrationRate = 0.; // 1.e-5; // initial infilration rate of fine sand
    static final double SqrtFrom2 = Math.sqrt(2.);
    static final double NEWTREIB = 0.0012; // 0.0012 Reibungskoeffizient fuer Newton'sche
    static double Coriolis = 0.0001; // Coriolisbeiwert 2*Omega*sin(phi)
    // Omega=2*pi/T mit T=86400s periode der Erddrehung
    // phi geographische Breite ca.54,5 fuer Ostsee und Nordfriesland

    private double WATT = 0.1;
    private double halfWATT = WATT / 2.;

    // konstruktor
    public FluidMudFlowModel2D(FEDecomposition fe, FluidMudFlowDat fluidmuddat) {
        System.out.println("FluidMudFlowModel2D initialization");
        fenet = fe;
        femodel = this;
        this.fluidmuddat = fluidmuddat;

        dof_data = new FluidMudFlowModel2DData[fenet.getNumberofDOFs()];

        setNumberOfThreads(fluidmuddat.NumberOfThreads);

        readBoundCond();

        bh.forEach((bcond) -> {
            inith.add(fenet.getDOF(bcond.pointnumber));
        });

        // DOFs initialisieren
        initialDOFs();
        generateClosedBoundCond();

        try {
            xf_os = new DataOutputStream(new FileOutputStream(fluidmuddat.xferg_name));
            // Setzen der Ergebnismaske
            TicadIO.write_xf(xf_os, this);
        } catch (FileNotFoundException e) {
            System.out.println("The file " + fluidmuddat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * initialisiert die Wasserspiegellage mit einem konstanten Wert
     * 
     * @param initalvalue
     * @return
     */
    public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\t Set initial value " + initalvalue);

        for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
            dof_data[i].m = initalvalue;
        }
        return null;
    }

    public static void setLatitude(double latitude) {
        Coriolis = 4. * Math.PI / 86400 * Math.sin(Math.toRadians(latitude));
    }

    public static double getCoriolisParameter() {
        return Coriolis;
    }

    /**
     * Read the start solution from file
     * 
     * @param currentergPath file with simulation results
     * @param record         record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unused")
    public double[] initialSolutionFromTicadErgFile(String currentergPath, int record) throws Exception {

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
            int anzKnoten = inStream.readInt();
            if (fenet.getNumberofDOFs() != anzKnoten) {
                System.out.println("Die Datei mit den Startwerten hat andere Anzahl von Knoten");
                System.exit(1);
            }
            int anzr = inStream.readInt();
            int anzElemente = inStream.readInt();

            // Ueberlesen folgender Zeilen
            inStream.skip(9 * 4);

            // Ergebnismaske lesen und auswerten
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
                    inStream.skip(4);
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
                    dof_data[i].m = inStream.readFloat();
                    DOF dof = fenet.getDOF(i);
                    double depth;
                    SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
                    if (sedimentmodeldata == null) {
                        depth = dof.z;
                    } else {
                        depth = sedimentmodeldata.z;
                    }
                    dof_data[i].z = depth;
                    if ((depth + dof_data[i].m) < 0.) {
                        dof_data[i].m = -depth;
                    }
                    dof_data[i].thickness = depth + dof_data[i].m;

                }

                if (SALT_gesetzt) {
                    inStream.skip(4);
                }

                if (EDDY_gesetzt) {
                    inStream.skip(4);
                }

                if (SHEAR_gesetzt) {
                    double tau_bx = inStream.readFloat() / PhysicalParameters.RHO_WATER;
                    double tau_by = inStream.readFloat() / PhysicalParameters.RHO_WATER;
                    dof_data[i].tau_b = Function.norm(tau_bx, tau_by);
                    // inStream.skip(8);
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
            } catch (Exception e) {
            }
        }

        inith = null;

        return null;
    }

    @Override
    public ModelData genData(FElement felement) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private final class initalSolutionLoop extends Thread {

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
                FluidMudFlowModel2DData currentmodeldata = dof_data[i];

                double depth;
                SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
                if (sedimentmodeldata == null) {
                    depth = dof.z;
                } else {
                    depth = sedimentmodeldata.z;
                }

                currentmodeldata.z = depth;

                currentmodeldata.m = initialH(dof, time);

                if ((depth + currentmodeldata.m) <= 0.) {
                    currentmodeldata.m = -depth;
                }

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
        FluidMudFlowModel2DData currentdata = dof_data[dof.number];
        if (currentdata.bh != null) {
            h = currentdata.bh.getValue(time);
        } else {
            for (DOF ndof : inith) {
                final FluidMudFlowModel2DData current = dof_data[ndof.number];
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

        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(systemDatPath)))) {
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('C');

            final int anzr = TicadIO.NextInt(st);
            final int anzi = TicadIO.NextInt(st);
            final int anzk = anzr + anzi;
            double value;
            if (anzk == fenet.getNumberofDOFs()) {
                System.out.println("\tRead initial fluid mud level (in Ticad-SysDat-Format): " + systemDatPath);
                for (int j = 0; j < anzk; j++) {
                    int nr = TicadIO.NextInt(st);
                    TicadIO.NextDouble(st);
                    TicadIO.NextDouble(st);
                    value = TicadIO.NextDouble(st); // -> m

                    DOF dof = fenet.getDOF(nr);
                    double depth;
                    SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
                    if (sedimentmodeldata == null) {
                        depth = dof.z;
                    } else {
                        depth = sedimentmodeldata.z;
                    }

                    FluidMudFlowModel2DData currentmodeldata = dof_data[nr];
                    currentmodeldata.z = depth;

                    if ((depth + value) < WATT) {
                        currentmodeldata.m = -depth;
                    } else {
                        currentmodeldata.m = value;
                    }

                    currentmodeldata.u = 0.;
                    currentmodeldata.v = 0.;

                }
            } else {
                System.out.println("\t different number of nodes");

            }

        } catch (Exception e) {
            System.out.println("\t cannot open file: " + systemDatPath);
        }

        inith = null;

        return null;
    }

    @SuppressWarnings("unused")
    public double[] initialHfromJanetBin(String filename, double time) throws Exception {
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

            System.out.println("\tRead initial fluid mud level (in Janet-Binary-Format):  " + filename);

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
                    SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
                    if (sedimentmodeldata == null) {
                        depth = dof.z;
                    } else {
                        depth = sedimentmodeldata.z;
                    }

                    FluidMudFlowModel2DData currentmodeldata = dof_data[nr];
                    currentmodeldata.z = depth;

                    if ((depth + value) < WATT) {
                        currentmodeldata.m = -depth;
                    } else {
                        currentmodeldata.m = value;
                    }

                    currentmodeldata.u = 0.;
                    currentmodeldata.v = 0.;

                    // Status-Flag lesen
                    if (writePointStatus) {
                        bin_in.fbinreadshort();
                    }

                }
            } else {
                System.out.println("system and fluidmudlevel.jbf different number of nodes");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Lesen");
            System.exit(0);
        }

        inith = null;

        return null;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public double[] getRateofChange(double time, double x[]) {
        return null;
    }

    /**
     * @param element
     * @return
     */
    @Override
    public double ElementApproximation(FElement element) {

        double timeStep = Double.POSITIVE_INFINITY;

        FTriangle ele = (FTriangle) element;
        DOF[] dofs = element.getDOFs();

        int iwatt = 0;
        int dry = 0;

        double wlambda = 1.;

        for (int j = 0; j < 3; j++) {
            double totaldepth = dof_data[dofs[j].number].thickness;
            if (totaldepth < WATT) {
                iwatt++;
                wlambda *= dof_data[dofs[j].number].wlambda;
            }
            if (totaldepth < halfWATT) {
                dry++;
            }
        }

        if (3 == dry) { // element is totaly dry

            for (int j = 0; j < 3; j++) {
                int i = dofs[j].number;

                dof_data[i].rm -= 5E-6 * dof_data[i].w1_lambda / 3.;

                dof_data[i].ru -= (5.E-2 * dof_data[i].w1_lambda * dof_data[i].u + dof_data[i].tau_b * dof_data[i].u
                        / ((dof_data[i].thickness < halfWATT) ? halfWATT : dof_data[i].thickness)) / 3.;
                dof_data[i].rv -= (5.E-2 * dof_data[i].w1_lambda * dof_data[i].v + dof_data[i].tau_b * dof_data[i].v
                        / ((dof_data[i].thickness < halfWATT) ? halfWATT : dof_data[i].thickness)) / 3.;
            }
        } else {

            Current2DElementData eleCurrentData = Current2DElementData.extract(element);

            double[][] koeffmat = ele.getkoeffmat();

            double[] terms_C = new double[3];
            double[] terms_u = new double[3];
            double[] terms_v = new double[3];
            double[] terms_h = new double[3];

            double u_mean = 0.;
            double v_mean = 0.;
            double thickness_mean = 0.;

            double udx = 0.;
            double udy = 0.;
            double vdx = 0.;
            double vdy = 0.;
            double dpdx = 0.;
            double dpdy = 0.;
            double depthdx = 0.;
            double depthdy = 0.;

            // sediment
            double dskoncdx = 0.;
            double dskoncdy = 0.;
            double qsxdx = 0.;
            double qsydy = 0.;

            double rhodx = 0.;
            double rhody = 0.;

            double DYNVISCOSITY = 0.;

            // caculate Bottomslope
            double dzdx;
            double dzdy;
            double bottomslope;
            SedimentElementData eleSedimentData = SedimentElementData.extract(ele);
            if (eleSedimentData != null) {
                bottomslope = eleSedimentData.bottomslope;
                dzdx = eleSedimentData.dzdx;
                dzdy = eleSedimentData.dzdy;
            } else {
                bottomslope = ele.bottomslope;
                dzdx = ele.dzdx;
                dzdy = ele.dzdy;
            }

            // Wave relevant parameter
            double wavebreaking = 0.;

            // compute element derivations
            for (int j = 0; j < 3; j++) {
                DOF dof = dofs[j];
                int i = dof.number;
                FluidMudFlowModel2DData fmudmd = dof_data[i];

                rhodx += fmudmd.rho * koeffmat[j][1];
                rhody += fmudmd.rho * koeffmat[j][2];

                u_mean += fmudmd.u / 3.;
                v_mean += fmudmd.v / 3.;
                thickness_mean += fmudmd.thickness / 3.;

                DYNVISCOSITY += fmudmd.viscosity / 3.;

                udx += fmudmd.u * koeffmat[j][1];
                udy += fmudmd.u * koeffmat[j][2];

                vdx += fmudmd.v * koeffmat[j][1];
                vdy += fmudmd.v * koeffmat[j][2];

                dpdx += fmudmd.p * koeffmat[j][1];
                dpdy += fmudmd.p * koeffmat[j][2];

                depthdx += fmudmd.thickness * koeffmat[j][1];
                depthdy += fmudmd.thickness * koeffmat[j][2];

                WaveHYPModel2DData wave = WaveHYPModel2DData.extract(dof);
                if (wave != null) {
                    if (j == 0)
                        wavebreaking = wave.epsilon_b;
                    else
                        wavebreaking = Math.max(wavebreaking, wave.epsilon_b);
                }

                dskoncdx += fmudmd.skonc * koeffmat[j][1];
                dskoncdy += fmudmd.skonc * koeffmat[j][2];

                CurrentModel2DData currentmodeldata = CurrentModel2DData.extract(dof);
                if (null != currentmodeldata) {
                    // Sediment - suspendet load
                    qsxdx += fmudmd.skonc * currentmodeldata.u * currentmodeldata.totaldepth * koeffmat[j][1];
                    qsydy += fmudmd.skonc * currentmodeldata.v * currentmodeldata.totaldepth * koeffmat[j][2];
                }
            }

            if (iwatt != 0) {

                dpdx = 0.;
                dpdy = 0.;

                if (iwatt == 1) {
                    for (int j = 0; j < 3; j++) {
                        int jg = dofs[j].number;
                        if (dof_data[jg].thickness >= WATT) {
                            dpdx += dof_data[jg].p * koeffmat[j][1];
                            dpdy += dof_data[jg].p * koeffmat[j][2];
                        } else {
                            int jg_1 = dofs[(j + 1) % 3].number;
                            int jg_2 = dofs[(j + 2) % 3].number;
                            if ((dof_data[jg].p < dof_data[jg_1].p) || (dof_data[jg].p < dof_data[jg_2].p)) {
                                dpdx += dof_data[jg].p * koeffmat[j][1];
                                dpdy += dof_data[jg].p * koeffmat[j][2];
                            } else {
                                dpdx += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].p + dof_data[jg_2].p)
                                        + dof_data[jg].wlambda * dof_data[jg].p) * koeffmat[j][1];
                                dpdy += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].p + dof_data[jg_2].p)
                                        + dof_data[jg].wlambda * dof_data[jg].p) * koeffmat[j][2];
                            }
                        }
                    }
                }
                if (iwatt == 2) {
                    for (int j = 0; j < 3; j++) {
                        int jg = dofs[j].number;
                        if (dof_data[jg].thickness >= WATT) {
                            dpdx += dof_data[jg].p * koeffmat[j][1];
                            dpdy += dof_data[jg].p * koeffmat[j][2];

                            int jg_1 = dofs[(j + 1) % 3].number;
                            int jg_2 = dofs[(j + 2) % 3].number;

                            if (dof_data[jg].p > dof_data[jg_1].p) {
                                dpdx += dof_data[jg_1].p * koeffmat[(j + 1) % 3][1];
                                dpdy += dof_data[jg_1].p * koeffmat[(j + 1) % 3][2];
                            } else {
                                dpdx += (dof_data[jg_1].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_1].wlambda * dof_data[jg_1].p) * koeffmat[(j + 1) % 3][1];
                                dpdy += (dof_data[jg_1].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_1].wlambda * dof_data[jg_1].p) * koeffmat[(j + 1) % 3][2];
                            }

                            if (dof_data[jg].p > dof_data[jg_2].p) {
                                dpdx += dof_data[jg_2].p * koeffmat[(j + 2) % 3][1];
                                dpdy += dof_data[jg_2].p * koeffmat[(j + 2) % 3][2];
                            } else {
                                dpdx += (dof_data[jg_2].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_2].wlambda * dof_data[jg_2].p) * koeffmat[(j + 2) % 3][1];
                                dpdy += (dof_data[jg_2].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_2].wlambda * dof_data[jg_2].p) * koeffmat[(j + 2) % 3][2];
                            }
                        }
                    }
                }
                if (iwatt == 3) {
                    double dmax = dof_data[dofs[0].number].thickness;
                    int j = 0;
                    if (dof_data[dofs[1].number].thickness > dmax) {
                        j = 1;
                        dmax = dof_data[dofs[1].number].thickness;
                    }
                    if (dof_data[dofs[2].number].thickness > dmax) {
                        j = 2;
                        /* dmax = dof_data[dofs[2].number].totaldepth; **unnoetig** */}

                    int jg = dofs[j].number;
                    dpdx += dof_data[jg].p * koeffmat[j][1];
                    dpdy += dof_data[jg].p * koeffmat[j][2];

                    int jg_1 = dofs[(j + 1) % 3].number;
                    int jg_2 = dofs[(j + 2) % 3].number;

                    if (dof_data[jg].p >= dof_data[jg_1].p) {
                        dpdx += (dof_data[jg].wlambda * dof_data[jg_1].p
                                + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_1].wlambda * dof_data[jg_1].p))
                                * koeffmat[(j + 1) % 3][1];
                        dpdy += (dof_data[jg].wlambda * dof_data[jg_1].p
                                + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_1].wlambda * dof_data[jg_1].p))
                                * koeffmat[(j + 1) % 3][2];
                    } else {
                        dpdx += (dof_data[jg_1].w1_lambda * dof_data[jg].p + dof_data[jg_1].wlambda * dof_data[jg_1].p)
                                * koeffmat[(j + 1) % 3][1];
                        dpdy += (dof_data[jg_1].w1_lambda * dof_data[jg].p + dof_data[jg_1].wlambda * dof_data[jg_1].p)
                                * koeffmat[(j + 1) % 3][2];
                    }

                    if (dof_data[jg].p >= dof_data[jg_2].p) {
                        dpdx += (dof_data[jg].wlambda * dof_data[jg_2].p
                                + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_2].wlambda * dof_data[jg_2].p))
                                * koeffmat[(j + 2) % 3][1];
                        dpdy += (dof_data[jg].wlambda * dof_data[jg_2].p
                                + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].p
                                        + dof_data[jg_2].wlambda * dof_data[jg_2].p))
                                * koeffmat[(j + 2) % 3][2];
                    } else {
                        dpdx += (dof_data[jg_2].w1_lambda * dof_data[jg].p + dof_data[jg_2].wlambda * dof_data[jg_2].p)
                                * koeffmat[(j + 2) % 3][1];
                        dpdy += (dof_data[jg_2].w1_lambda * dof_data[jg].p + dof_data[jg_2].wlambda * dof_data[jg_2].p)
                                * koeffmat[(j + 2) % 3][2];
                    }
                    dpdx *= dof_data[jg].wlambda;
                    dpdy *= dof_data[jg].wlambda;
                }
            } else {
                Current2DElementData elemCurrent = Current2DElementData.extract(element);
                if (null != elemCurrent) {
                    double lambda = elemCurrent.depth_mean / ((thickness_mean < WATT) ? WATT : thickness_mean);
                    dpdx = (dpdx + lambda * elemCurrent.dhdx);
                    dpdy = (dpdy + lambda * elemCurrent.dhdy);
                }
            }

            final double elementsize;
            if (norm(u_mean, v_mean) > WATT / 10.) {
                elementsize = ele.getVectorSize(u_mean, v_mean); // Peter 07.08.2024
            } else
                elementsize = ele.minHight;
            eleCurrentData.elementsize = elementsize;

            // eddy viscosity
            // -----------------------------------------
            // konstant
            double astx = DYNVISCOSITY * bottomslope;
            // Smagorinsky-Ansatz
            astx += (astx * elementsize) * (astx * elementsize)
                    * Math.sqrt(2. * udx * udx + (udy + vdx) * (udy + vdx) + 2. * vdy * vdy);
            /* Elder - Ansatz mit Strickler Bodenschubspannung approximiert */
            astx += DYNVISCOSITY * PhysicalParameters.sqrtG / 30. * Function.norm(u_mean, v_mean) * thickness_mean
                    * bottomslope;

            // Battjes-Ansatz turbulence by wavebreaking
            if (wavebreaking != 0.)
                astx += CurrentModel2D.BATTJESKOEFF * thickness_mean
                        * Math.cbrt(wavebreaking / PhysicalParameters.RHO_WATER);

            double asty = astx;

            double cureq1_mean = 0.;
            double cureq2_mean = 0.;
            double cureq3_mean = 0.;

            double Koeq1_mean = 0.;

            // Elementfehler berechnen
            for (int j = 0; j < 3; j++) {
                int i = dofs[j].number;
                FluidMudFlowModel2DData fmudmd = dof_data[i];
                // Quellen und Senken bestimmen
                double source_dhdt = 0;
                if (fmudmd.sourceh != null) {
                    source_dhdt = fmudmd.sourceh.getValue(time);
                }
                if (fmudmd.sourceQ != null) {
                    double area = 0.;
                    FElement[] feles = dofs[j].getFElements();
                    for (FElement fele : feles) {
                        // muss nicht jedes mal neu berechet werden
                        area += fele.getVolume();
                    }
                    source_dhdt = fmudmd.sourceQ.getValue(time) / area;
                }

                final double nu = DYNVISCOSITY
                        * (1. + PhysicalParameters.sqrtG / fmudmd.kst * fmudmd.cv * fmudmd.thickness); // Elder-Approximation
                final double nonZeroTotalDepth = ((fmudmd.thickness < WATT) ? WATT : fmudmd.thickness);

                terms_u[j] =
                        // pressure
                        PhysicalParameters.G * dpdx
                                // density term
                                + 0.5 * PhysicalParameters.G * rhodx * fmudmd.thickness / fmudmd.rho
                                // Advektive Terme
                                + fmudmd.u * udx + fmudmd.v * udy
                                // Coriolis
                                - fmudmd.v * Coriolis
                                // bottom friction
                                + (fmudmd.tau_b * fmudmd.u + nu * (2. * udx * dzdx + (udy + vdx) * dzdy) * wlambda)
                                        / ((fmudmd.thickness < halfWATT) ? halfWATT : fmudmd.thickness)
                                // Current2D
                                - fmudmd.tau_currentdx / fmudmd.rho / nonZeroTotalDepth * fmudmd.wlambda
                                // turbulence term
                                - (astx * depthdx * udx + asty * depthdy * udy) / nonZeroTotalDepth * wlambda
                                + 3. * (koeffmat[j][1] * astx * udx + koeffmat[j][2] * asty * udy);
                terms_u[j] += 5.E-2 * bottomslope * fmudmd.w1_lambda * fmudmd.u;

                terms_v[j] = (
                // pressure
                PhysicalParameters.G * dpdy
                        // density term
                        + 0.5 * PhysicalParameters.G * rhody * fmudmd.thickness / fmudmd.rho
                        // Advektive Terme
                        + fmudmd.u * vdx + fmudmd.v * vdy
                        // Coriolis
                        + fmudmd.u * Coriolis
                        // bottom friction
                        + (fmudmd.tau_b * fmudmd.v + nu * (2. * vdy * dzdy + (udy + vdx) * dzdx) * wlambda)
                                / ((fmudmd.thickness < halfWATT) ? halfWATT : fmudmd.thickness)
                        // Current2D
                        - fmudmd.tau_currentdy / fmudmd.rho / nonZeroTotalDepth * fmudmd.wlambda
                        // turbulence term
                        - (astx * depthdx * vdx + asty * depthdy * vdy) / nonZeroTotalDepth * wlambda
                        + 3. * (koeffmat[j][1] * astx * vdx + koeffmat[j][2] * asty * vdy)) // turbulence term
                ;
                terms_v[j] += 5.E-2 * bottomslope * fmudmd.w1_lambda * fmudmd.v;

                terms_h[j] = (fmudmd.thickness * (udx + vdy) + fmudmd.u * depthdx + fmudmd.v * depthdy
                        - 3. * source_dhdt + fmudmd.dzdt)
                        + (qsxdx + qsydy);
                terms_h[j] += 5.E-6 * bottomslope * fmudmd.w1_lambda;

                if (fmudmd.wattsickern) {
                    if (dof_data[dofs[(j + 1) % 3].number].m < fmudmd.m) {
                        terms_h[j] += 4.E-5 * bottomslope * fmudmd.w1_lambda
                                * (fmudmd.m - dof_data[dofs[(j + 1) % 3].number].m)
                                / (dofs[(j + 1) % 3].distance(dofs[j]));
                    }
                    if (dof_data[dofs[(j + 2) % 3].number].m < fmudmd.m) {
                        terms_h[j] += 4.E-5 * bottomslope * fmudmd.w1_lambda
                                * (fmudmd.m - dof_data[dofs[(j + 2) % 3].number].m)
                                / (dofs[(j + 2) % 3].distance(dofs[j]));
                    }
                }

                if ((fmudmd.bh == null) && (!fmudmd.extrapolate_h)) {
                    cureq1_mean += 1. / 3. * (fmudmd.dmdt + terms_h[j]);
                }
                if ((fmudmd.bu == null) && (!fmudmd.extrapolate_u)) {
                    cureq2_mean += 1. / 3. * (fmudmd.dudt + terms_u[j]);
                }
                if ((fmudmd.bv == null) && (!fmudmd.extrapolate_v)) {
                    cureq3_mean += 1. / 3. * (fmudmd.dvdt + terms_v[j]);
                }

                CurrentModel2DData cmd = CurrentModel2DData.extract(dofs[j]);
                if (null != cmd) {
                    terms_C[j] = cmd.u * dskoncdx + cmd.v * dskoncdy
                            - fmudmd.sedimentSource
                            - (eleCurrentData.astx * eleCurrentData.ddepthdx * dskoncdx
                                    + eleCurrentData.asty * eleCurrentData.ddepthdy * dskoncdy)
                                    / ((eleCurrentData.depth_mean < CurrentModel2D.WATT) ? CurrentModel2D.WATT
                                            : eleCurrentData.depth_mean)
                                    * eleCurrentData.wlambda
                            + 3. * (koeffmat[j][1] * eleCurrentData.astx * dskoncdx
                                    + koeffmat[j][2] * eleCurrentData.asty * dskoncdy);
                    // ToDo if(fmudmd.bconc==null) // Peter 09.02.2011
                    Koeq1_mean += 1. / 3. * (fmudmd.dCdt + terms_C[j]);
                }

            }

            double c0 = Math.sqrt(PhysicalParameters.G * ((thickness_mean < WATT) ? WATT : thickness_mean)); // shallow
                                                                                                             // water
                                                                                                             // wave
                                                                                                             // velocity
                                                                                                             // // =
                                                                                                             // Math.sqrt(G*Function.max(WATT,thickness_mean));
            double operatornorm1 = Math.abs(u_mean) + c0;
            double operatornorm2 = Math.abs(v_mean) + c0;
            double operatornorm = Math.sqrt(operatornorm1 * operatornorm1 + operatornorm2 * operatornorm2);

            double tau_cur = 0.5 * elementsize / operatornorm;

            timeStep = tau_cur;

            // if (tau > 0.00001) { // to time-consuming
            // double peclet = operatornorm * elementsize / tau;
            // tau_cur *= Function.coth(peclet) - 1.0 / peclet;
            // }

            double tau_C = 0.;
            double current_mean = Function.norm(eleCurrentData.u_mean, eleCurrentData.v_mean);
            if (current_mean > 1.E-5) {
                tau_C = 0.5 * eleCurrentData.elementsize / current_mean;
                timeStep = ((timeStep < tau_C) ? timeStep : tau_C);
                // double tauc = Function.norm(eleCurrentData.astx, eleCurrentData.asty);
                // if (tauc > 0.00001) { // to time-consuming
                // double peclet = current_mean * elementsize / tauc;
                // tau_C *= Function.coth(peclet) - 1.0 / peclet;
                // }
            }

            // Fehlerkorrektur durchfuehren
            // ----------------------------
            for (int j = 0; j < 3; j++) {

                FluidMudFlowModel2DData cmd = dof_data[ele.getDOF(j).number];

                double result_SKonc_i = -tau_C * (koeffmat[j][1] * eleCurrentData.u_mean * Koeq1_mean
                        + koeffmat[j][2] * eleCurrentData.v_mean * Koeq1_mean
                        - (eleCurrentData.astx * eleCurrentData.ddepthdx * koeffmat[j][1] * Koeq1_mean
                                + eleCurrentData.asty * eleCurrentData.ddepthdy * koeffmat[j][2] * Koeq1_mean)
                                / ((eleCurrentData.depth_mean < CurrentModel2D.WATT) ? CurrentModel2D.WATT
                                        : eleCurrentData.depth_mean)
                                * eleCurrentData.wlambda)
                        * ele.area;

                double result_U_i = -tau_cur * (koeffmat[j][1] * u_mean * cureq2_mean
                        + koeffmat[j][1] * PhysicalParameters.G * cureq1_mean + koeffmat[j][2] * v_mean * cureq2_mean
                        - (astx * depthdx * koeffmat[j][1] * cureq2_mean
                                + asty * depthdy * koeffmat[j][2] * cureq2_mean)
                                / ((thickness_mean < WATT) ? WATT : thickness_mean) * wlambda)
                        * ele.area;

                double result_V_i = -tau_cur * (koeffmat[j][1] * u_mean * cureq3_mean
                        + koeffmat[j][2] * PhysicalParameters.G * cureq1_mean + koeffmat[j][2] * v_mean * cureq3_mean
                        - (astx * depthdx * koeffmat[j][1] * cureq3_mean
                                + asty * depthdy * koeffmat[j][2] * cureq3_mean)
                                / ((thickness_mean < WATT) ? WATT : thickness_mean) * wlambda)
                        * ele.area;

                double result_H_i = -tau_cur
                        * (koeffmat[j][1] * cmd.thickness * cureq2_mean + koeffmat[j][1] * u_mean * cureq1_mean
                                + koeffmat[j][2] * cmd.thickness * cureq3_mean + koeffmat[j][2] * v_mean * cureq1_mean)
                        * ele.area;

                // Begin standart Galerkin-step
                for (int l = 0; l < 3; l++) {
                    final double vorfak = ele.area * ((l == j) ? 1. / 6. : 1. / 12.);

                    // Impulse Equations
                    result_U_i -= vorfak * terms_u[l];
                    result_V_i -= vorfak * terms_v[l];

                    // Conti Equation
                    result_H_i -= vorfak * terms_h[l];

                    result_SKonc_i -= vorfak * terms_C[l];
                }

                synchronized (cmd) {
                    cmd.ru += result_U_i;
                    cmd.rv += result_V_i;
                    cmd.rm += result_H_i;

                    cmd.rC += result_SKonc_i;
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
    public void setBoundaryCondition(DOF dof, double t) {

        int i = dof.number;
        FluidMudFlowModel2DData fluidmuddata = dof_data[i];

        fluidmuddata.ru = 0.;
        fluidmuddata.rv = 0.;
        fluidmuddata.rm = 0.;

        fluidmuddata.wattsickern = true;

        SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
        if (sedimentmodeldata == null) {
            fluidmuddata.z = dof.z;
            fluidmuddata.dzdt = 0.;
        } else {
            fluidmuddata.z = sedimentmodeldata.z; // schon im sedimentmodell gesetzt
            fluidmuddata.dzdt = sedimentmodeldata.dzdt; // schon im sedimentmodell gesetzt
        }

        if (fluidmuddata.bh != null) {
            fluidmuddata.m = Math.max(-fluidmuddata.z, fluidmuddata.bh.getValue(t));
            // fluidmuddata.dmdt = fluidmuddata.bh.getDifferential(t);
        }
        fluidmuddata.thickness = Function.max(0., fluidmuddata.z + fluidmuddata.m);

        /* extrapolate no exact defined boundary conditions */
        if ((fluidmuddata.extrapolate_h || fluidmuddata.extrapolate_u || fluidmuddata.extrapolate_v)
                && (fluidmuddata.thickness > WATT)) {
            for (FElement elem : dof.getFElements()) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            int jtmp = elem.getDOF((ll + ii) % 3).number;
                            FluidMudFlowModel2DData tmpcdata = dof_data[jtmp];
                            if (tmpcdata.thickness > WATT) {
                                if (fluidmuddata.extrapolate_h) {
                                    double dh = (fluidmuddata.m - tmpcdata.m) / 10.;
                                    if (tmpcdata.extrapolate_h)
                                        dh /= 10.;
                                    synchronized (fluidmuddata) {
                                        fluidmuddata.m -= dh;
                                        fluidmuddata.thickness = Function.max(0., fluidmuddata.z + fluidmuddata.m); // currentdata.totaldepth
                                                                                                                    // -=
                                                                                                                    // dh;
                                    }
                                    synchronized (tmpcdata) {
                                        tmpcdata.m += dh / elem.getDOF((ll + ii) % 3).getNumberofFElements()
                                                * dof.getNumberofFElements();
                                        tmpcdata.thickness = Function.max(0., tmpcdata.z + tmpcdata.m); // tmpcdata.totaldepth
                                                                                                        // += dh /
                                                                                                        // elem.getDOF((ll
                                                                                                        // + ii) %
                                                                                                        // 3).getNumberofFElements()
                                                                                                        // *
                                                                                                        // dof.getNumberofFElements();
                                    }
                                }
                                if (fluidmuddata.extrapolate_u) {
                                    double du = (fluidmuddata.u - tmpcdata.u) / 10.;
                                    if (tmpcdata.extrapolate_u)
                                        du /= 10.;
                                    synchronized (fluidmuddata) {
                                        fluidmuddata.u -= du;
                                        fluidmuddata.cv = Function.norm(fluidmuddata.u, fluidmuddata.v);
                                    }
                                    synchronized (tmpcdata) {
                                        tmpcdata.u += du / elem.getDOF((ll + ii) % 3).getNumberofFElements()
                                                * dof.getNumberofFElements();
                                        tmpcdata.cv = Function.norm(tmpcdata.u, tmpcdata.v);
                                    }
                                }
                                if (fluidmuddata.extrapolate_v) {
                                    double dv = (fluidmuddata.v - tmpcdata.v) / 10.;
                                    if (tmpcdata.extrapolate_v)
                                        dv /= 10.;
                                    synchronized (fluidmuddata) {
                                        fluidmuddata.v -= dv;
                                        fluidmuddata.cv = Function.norm(fluidmuddata.u, fluidmuddata.v);
                                    }
                                    synchronized (tmpcdata) {
                                        tmpcdata.v += dv / elem.getDOF((ll + ii) % 3).getNumberofFElements()
                                                * dof.getNumberofFElements();
                                        tmpcdata.cv = Function.norm(tmpcdata.u, tmpcdata.v);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        if ((fluidmuddata.z + fluidmuddata.m) <= 0.) {
            fluidmuddata.m = -fluidmuddata.z;
            fluidmuddata.thickness = 0;
            fluidmuddata.u = fluidmuddata.v = 0.;
        }

        // if (fluidmuddata.bQx != null) {
        // fluidmuddata.bQx.update(dof_data, t);
        // fluidmuddata.u = fluidmuddata.bQx.getValueAt(i);
        // // System.out.println("Knoten "+i+" mit Qx nach U "+fluidmuddata.u);
        // }
        // if (fluidmuddata.bQy != null) {
        // fluidmuddata.bQy.update(dof_data, t);
        // fluidmuddata.v = fluidmuddata.bQy.getValueAt(i);
        // // System.out.println("Knoten "+i+" mit Qy nach V "+fluidmuddata.v);
        // }

        fluidmuddata.thickness = fluidmuddata.z + fluidmuddata.m;

        if (fluidmuddata.bu != null) {
            fluidmuddata.u = fluidmuddata.bu.getValue(t);
            // fluidmuddata.dudt = fluidmuddata.bu.getDifferential(t);
        }
        if (fluidmuddata.bv != null) {
            fluidmuddata.v = fluidmuddata.bv.getValue(t);
            // fluidmuddata.dvdt = fluidmuddata.bv.getDifferential(t);
        }

        if (fluidmuddata.bqx != null) {
            if (fluidmuddata.thickness > WATT) {
                fluidmuddata.u = fluidmuddata.bqx.getValue(t) / fluidmuddata.thickness;
            } else {
                fluidmuddata.u = fluidmuddata.thickness / WATT * fluidmuddata.bqx.getValue(t) / WATT;
            }
            // fluidmuddata.dudt = (fluidmuddata.bqx.getDifferential(t) - x[U + i]
            // *(depthdt+fluidmuddata.dmdt))/(fluidmuddata.z+x[H + i]);
        }
        if (fluidmuddata.bqy != null) {
            if (fluidmuddata.thickness > WATT) {
                fluidmuddata.v = fluidmuddata.bqy.getValue(t) / fluidmuddata.thickness;
            } else {
                fluidmuddata.v = fluidmuddata.thickness / WATT * fluidmuddata.bqy.getValue(t) / WATT;
            }
            // fluidmuddata.dvdt = (fluidmuddata.bqy.getDifferential(t) - x[V + i]
            // *(depthdt+fluidmuddata.dmdt))/(fluidmuddata.z+x[H + i]);
        }

        /* Wattstrategie */
        fluidmuddata.wlambda = Function.min(1., fluidmuddata.thickness / WATT);
        fluidmuddata.w1_lambda = 1. - fluidmuddata.wlambda;

        // testen ob ein knoten ein sickerknoten ist
        if ((fluidmuddata.thickness < WATT) /* && !fluidmuddata.boundary */) {
            FElement[] felem = dof.getFElements();
            for (int j = 0; (j < felem.length) && fluidmuddata.wattsickern; j++) {
                FElement elem = felem[j];
                for (int ll = 0; (ll < 3) && fluidmuddata.wattsickern; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        FluidMudFlowModel2DData tmpcmd1 = dof_data[elem.getDOF((ll + 1) % 3).number];
                        FluidMudFlowModel2DData tmpcmd2 = dof_data[elem.getDOF((ll + 2) % 3).number];
                        fluidmuddata.wattsickern &= !(((tmpcmd1.m > fluidmuddata.m)
                                && (tmpcmd1.thickness >= 0.9 * WATT))
                                || ((tmpcmd2.m > fluidmuddata.m) && (tmpcmd2.thickness >= 0.9 * WATT)));
                    }
                }
            }
        } else
            fluidmuddata.wattsickern = false;

        if ((fluidmuddata.thickness < CurrentModel2D.halfWATT / 2.) && fluidmuddata.wattsickern) {
            fluidmuddata.u *= fluidmuddata.thickness / (CurrentModel2D.halfWATT / 2.);
            fluidmuddata.v *= fluidmuddata.thickness / (CurrentModel2D.halfWATT / 2.);
        }
        fluidmuddata.cv = Function.norm(fluidmuddata.u, fluidmuddata.v);

        double kst = fluidmuddata.kst;
        // BewuchsKst
        SpartinaAlternifloraModel2DData samd = SpartinaAlternifloraModel2DData.extract(dof);
        if (samd != null) {
            kst = Function.min(kst, samd.getStrickler(fluidmuddata.thickness));
        }
        // bottom friction coefficient
        // Strickler
        fluidmuddata.tau_b = PhysicalParameters.G
            / Math.cbrt((fluidmuddata.thickness < WATT) ? WATT : fluidmuddata.thickness) * fluidmuddata.cv
            / Function.sqr(kst); // with fast approximation

        fluidmuddata.p = fluidmuddata.m;// * fluidmuddata.rho;
        /* current stress koeffizient in anlehnung an wind stress */
        /* Smith and Banke (1975) */
        if (fluidmuddata.thickness > WATT) {
            CurrentModel2DData current2DData = CurrentModel2DData.extract(dof);
            if (current2DData != null) {
                // double tau_wind = (0.63 + 0.066 * current2DData.cv) * 1.E-3 *
                // current2DData.rho;
                // fluidmuddata.tau_currentdx = tau_wind * current2DData.cv * current2DData.u;
                // fluidmuddata.tau_currentdy = tau_wind * current2DData.cv * current2DData.v;
                fluidmuddata.tau_currentdx = current2DData.tauBx;
                fluidmuddata.tau_currentdy = current2DData.tauBy;

                // ToDo mit Wattstrategie fluidmuddata.p = current2DData.eta;// *
                // current2DData.rho;
            }
        } else {
            fluidmuddata.tau_currentdx = 0.;
            fluidmuddata.tau_currentdy = 0.;
        }

        /* prevention of negative concentration */
        if (fluidmuddata.skonc < 0.)
            fluidmuddata.skonc = 0.;

        fluidmuddata.sedimentSource = getSourceSunk(dof);

    } // end setBoundaryCondition

    /**
     * genData generate the nessecery modeldatas for a DOF
     * 
     * @param dof
     * @return
     */
    @Override
    public ModelData genData(DOF dof) {
        // System.out.println("DOF "+dof);
        FluidMudFlowModel2DData data = new FluidMudFlowModel2DData();
        int dofnumber = dof.number;
        dof_data[dofnumber] = data;

        data.rho = fluidmuddat.constantDensity;
        data.viscosity = fluidmuddat.constantViscosity;

        CurrentModel2DData current2DData = CurrentModel2DData.extract(dof); // ToDo aus dem Bodenmodell holen
        if (current2DData != null) {
            data.kst = current2DData.kst;
            data.ks = CurrentModel2DData.Strickler2Nikuradse(data.kst);
        }

        for (BoundaryCondition bcond : bsc) {
            if (dofnumber == bcond.pointnumber) {
            data.bconc = bcond.function;
            bsc.remove(bcond);
            break;
            }
        }

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
            // System.out.println("Qx gesetzt bei "+ dofnumber);
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

    // ----------------------------------------------------------------------
    // generateClosedBoundCond
    // ----------------------------------------------------------------------
    final void generateClosedBoundCond() {
        // System.out.println(" generateClosedBoundCond ");
        ZeroFunction1d zerofct = new ZeroFunction1d();
        FluidMudFlowModel2DData current;
        for (FElement felem : fenet.getFElements()) {
            FTriangle tele = (FTriangle) felem;
            if (tele.getKennung() != 0) {

                if (tele.getKennung() == FTriangle.bit_kante_jk) {
                    // System.out.println(" "+FTriangle.bit_nr_kante_jk+" bit_kante_jk");
                    current = FluidMudFlowModel2DData.extract(tele.getDOF(1));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;

                    current = FluidMudFlowModel2DData.extract(tele.getDOF(2));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;
                } else if (tele.getKennung() == FTriangle.bit_kante_ki) {
                    // System.out.println(" "+FTriangle.bit_nr_kante_ki+" bit_kante_ki");
                    current = FluidMudFlowModel2DData.extract(tele.getDOF(0));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;

                    current = FluidMudFlowModel2DData.extract(tele.getDOF(2));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;
                } else if (tele.getKennung() == FTriangle.bit_kante_ij) {
                    // System.out.println(" "+FTriangle.bit_nr_kante_ij+" bit_kante_ij");
                    current = FluidMudFlowModel2DData.extract(tele.getDOF(0));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;

                    current = FluidMudFlowModel2DData.extract(tele.getDOF(1));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;
                } else if ((tele.getKennung() == FTriangle.bit_kante_ijk) ||
                        (tele.getKennung() == FTriangle.bit_kante_jki) ||
                        (tele.getKennung() == FTriangle.bit_kante_kij) ||
                        (tele.getKennung() == FTriangle.bit_kante_ijki)) {
                    // System.out.println("alle");
                    current = FluidMudFlowModel2DData.extract(tele.getDOF(0));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;

                    current = FluidMudFlowModel2DData.extract(tele.getDOF(1));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;

                    current = FluidMudFlowModel2DData.extract(tele.getDOF(2));
                    if (current.bu == null) {
                        current.bu = zerofct;
                    }
                    if (current.bv == null) {
                        current.bv = zerofct;
                    }
                    current.boundary = true;
                }
            }
        }
        for (DOF dof : fenet.getDOFs()) {
            current = FluidMudFlowModel2DData.extract(dof);
            current.extrapolate_h = ((current.bu != null) && (current.bv != null) && (current.bh == null))
                    || ((current.bqx != null) && (current.bqy != null) && (current.bh == null)) /*
                                                                                                 * || ((current.bQx !=
                                                                                                 * null) && (current.bQy
                                                                                                 * != null) &&
                                                                                                 * (current.bh == null))
                                                                                                 */;
            current.extrapolate_u = ((current.bh != null) && (current.bu == null))
                    || ((current.bh != null) && (current.bqx == null))
                    || ((current.bh != null) && (current.bQx == null));
            current.extrapolate_v = ((current.bh != null) && (current.bv == null))
                    || ((current.bh != null) && (current.bqy == null))
                    || ((current.bh != null) && (current.bQy == null));
        }
    } // end generateClosedBoundCond

    /**
     * The method write_erg_xf
     * 
     * @param erg
     * @param t
     * @deprecated by write_erg_xf()
     */
    @Override
    @Deprecated
    public void write_erg_xf(double[] erg, double t) {
        throw new UnsupportedOperationException("Not implemented yet");
    } // end write_erg_xf

    @Override
    public int getTicadErgMask() {
        // scalar z, vector v, scalar mud oberflaeche, scalar Concentration
        return TicadIO.HRES_Z | TicadIO.HRES_V | TicadIO.HRES_H | TicadIO.HRES_EDDY;
    }

    /** The method write_erg_xf */
    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);

            for (DOF dof : fenet.getDOFs()) {
                FluidMudFlowModel2DData current = dof_data[dof.number];
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                xf_os.writeFloat((float) current.z);
                if (current.thickness < WATT && MarinaXML.release) {
                    xf_os.writeFloat(0.f);
                    xf_os.writeFloat(0.f);
                } else {
                    xf_os.writeFloat((float) current.u);
                    xf_os.writeFloat((float) current.v);
                }
                xf_os.writeFloat((float) current.m); // skalar1
                xf_os.writeFloat((float) current.skonc); // concentration
            }
            xf_os.flush();
        } catch (Exception e) {
            System.out.println(this.getClass() + "\n\ttime=" + time + "\n");
            e.printStackTrace();
            System.exit(0);
        }
    } // end write_erg_xf

    private double getSourceSunk(DOF dof) {

        double rvalue = 0.;

        double tdm = 1000.;
        double ws = 1.E-3; // 1.
        FluidMudFlowModel2DData fmudd = dof_data[dof.number];
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);

        if (null != cmd) {

            double tauc = Function.norm(fmudd.tau_currentdx, fmudd.tau_currentdy);

            final double taub = 0.2;

            if (tauc < tdm) {
                rvalue = -ws * fmudd.skonc * (1. - tauc / tdm);
            }

            if (tauc > taub) {
                // rvalue += fmudd.wlambda * 7.35E-4 * (tauc / taub - 1.); // Malcharek
                // Unterweser konsoledierte Boeden
                rvalue += fmudd.wlambda * 2.5E-3 * Math.exp(0.13 * (taub - tauc)); // Parchure & Mehta (1986) fuer
                                                                                   // weiche Boeden
                // double u_um = Math.sqrt(Math.pow(fmudd.u - cmd.u,2)+ Math.pow(fmudd.v -
                // cmd.v,2));
                // double usm2 = 0.032*u_um*u_um;
                // double uss = fmudd.cv + Math.sqrt(usm2);
                //// if(u_um > 0.01*CurrentModel2D.WATT*CurrentModel2D.WATT){
                // final double Cm=30.; // kg/m^2
                // final double Cs=0.25;
                // final double Csigma=0.42;
                // rvalue +=
                // fmudd.wlambda*fmudd.wlambda*(2*Cs*Math.max(0.,usm2-taub/fmudd.rho*1.E3)*u_um
                // + Csigma *Math.max(0.,uss*uss-taub/fmudd.rho*1.E3)*uss)/(PhysicalParameters.G
                // * ((cmd.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT :
                // cmd.totaldepth) * (fmudd.rho-cmd.rho)/cmd.rho + Cs *u_um*u_um) *Cm;
                //// }
            }
            rvalue -= ws * 1.E-7; // Sinkgeschwindigkeit mal kleiner Konzentrationsdifferenz

            rvalue /= ((cmd.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT : cmd.totaldepth);
        }
        return rvalue;
    }

    /**
     * Neue Einleseroutine readBoundCond
     * liest die spezifizierten Datensaetze (Randbedingungen) in der
     * boundary_condition_key_mask
     * aus entsprechenden Randwertedatei (fluidmuddat.rndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in fluidmuddat.rndwerteReader)
     */
    public final void readBoundCond() {

        String[] boundary_condition_key_mask = new String[10];
        boundary_condition_key_mask[0] = BoundaryCondition.absolute_flowrate_x;
        boundary_condition_key_mask[1] = BoundaryCondition.absolute_flowrate_y;
        boundary_condition_key_mask[2] = BoundaryCondition.specific_flowrate_x;
        boundary_condition_key_mask[3] = BoundaryCondition.specific_flowrate_y;
        boundary_condition_key_mask[4] = BoundaryCondition.free_surface;
        boundary_condition_key_mask[5] = BoundaryCondition.velocity_u;
        boundary_condition_key_mask[6] = BoundaryCondition.velocity_v;
        boundary_condition_key_mask[7] = BoundaryCondition.pointbased_Q_source;
        boundary_condition_key_mask[8] = BoundaryCondition.pointbased_h_source;
        boundary_condition_key_mask[9] = BoundaryCondition.concentration_sediment;

        try {
            for (BoundaryCondition bc : fluidmuddat.rndwerteReader
                    .readBoundaryConditions(boundary_condition_key_mask)) {
                if (bc.boundary_condition_key.equals(BoundaryCondition.absolute_flowrate_x)) {
                    bQx.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.absolute_flowrate_y)) {
                    bQy.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.specific_flowrate_x)) {
                    bqx.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.specific_flowrate_y)) {
                    bqy.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.free_surface)) {
                    bh.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.velocity_u)) {
                    bu.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.velocity_v)) {
                    bv.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.pointbased_h_source)) {
                    sh.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.pointbased_Q_source)) {
                    sQ.add(bc);
                }
                if (bc.boundary_condition_key.equals(BoundaryCondition.concentration_sediment)) {
                    bsc.add(bc);
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

        timeStepLoop[] iloop = new timeStepLoop[numberOfThreads];
        int anzdofs = fenet.getNumberofDOFs();
        for (int ii = 0; ii < numberOfThreads; ii++) {
            iloop[ii] = new timeStepLoop(anzdofs * ii / numberOfThreads, anzdofs * (ii + 1) / numberOfThreads, dt);
            iloop[ii].start();
        }
        for (int ii = 0; ii < numberOfThreads; ii++) {
            try {
                iloop[ii].join();
            } catch (Exception e) {
            }
        }

        this.time += dt;

        if (resultIsNaN) {
            System.out.println("Time=" + this.time + " and timestep is " + dt);
            write_erg_xf();
            try {
                xf_os.close();
            } catch (Exception e) {
            }
            System.exit(0);
        }
    }

    // innere Klasse
    private final class timeStepLoop extends Thread {

        int lo, hi;
        double dt;

        timeStepLoop(int lo, int hi, double dt) {
            this.lo = lo;
            this.hi = hi;
            this.dt = dt;
        }

        @Override
        public void run() {
            DOF[] dof = fenet.getDOFs();
            for (int j = lo; j < hi; j++) {
                int i = dof[j].number;
                FluidMudFlowModel2DData cmd = dof_data[i];

                cmd.ru /= dof[j].lumpedMass;
                cmd.rv /= dof[j].lumpedMass;
                cmd.rm /= dof[j].lumpedMass;
                cmd.rC /= dof[j].lumpedMass;

                double ru = (3. * cmd.ru - cmd.dudt) / 2.; // zusaetzlichen Stabilisierung in Anlehnung am expliziten
                                                           // Adams-Bashford 2. Ordnung
                double rv = (3. * cmd.rv - cmd.dvdt) / 2.;
                double rh = (3. * cmd.rm - cmd.dmdt) / 2.;
                double rC = (3. * cmd.rC - cmd.dCdt) / 2.;

                cmd.dudt = cmd.ru;
                cmd.dvdt = cmd.rv;
                cmd.dmdt = cmd.rm;
                cmd.dCdt = cmd.rC;

                final GroundWater2DData gwdata = GroundWater2DData.extract(dof[j]);
                if (gwdata != null) {
                    if ((cmd.z + gwdata.h) >= 0.) {
                        rh += gwdata.dhdt;
                    }
                } else {
                    rh -= infiltrationRate;
                }

                if ((cmd.wattsickern) && (rh > 0.)) {
                    rh *= cmd.wlambda;
                }

                cmd.u += dt * ru;
                cmd.v += dt * rv;
                cmd.m += dt * rh;
                cmd.skonc += dt * rC;

                boolean rIsNaN = Double.isNaN(ru) || Double.isNaN(rv) || Double.isNaN(rh);
                if (rIsNaN) {
                    System.out.println(
                            "FluidMudFlowModel2D is NaN bei " + i + " dh/dt=" + rh + " du/dt=" + ru + " dv/dt=" + rv);
                }
                resultIsNaN |= rIsNaN;
            }
        }
    }
}
