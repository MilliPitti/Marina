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
import static de.smile.marina.PhysicalParameters.G;
import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.FTriangleMesh;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TicadModel;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.marina.fem.model.hydrodynamic.dim3.CurrentModel3DData;
import de.smile.marina.fem.model.hydrodynamic.wave.WaveFunction;
import de.smile.marina.fem.model.meteorology.MeteorologyData2D;
import de.smile.marina.io.RandN_Reader;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.*;
import static java.lang.Math.PI;
import java.util.*;

/**
 * This class describe the stabilized finite element approximation of the
 * 2-dimensional hyperbolic wave equations
 *
 * @author Peter Milbradt
 * @version 4.6.0
 */
public class WaveHYPModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel {

    // File Streams
    private DataOutputStream xf_os = null;
    WaveHYPModel2DData[] dof_data = null;
    WaveHYPElementData[] element_data = null;
    ArrayList<DOF> bcs = new ArrayList<>();
    private final WaveHYPDat wavehypdat;
    double ddwadx[], ddwady[], ddwa2dx2[], ddwa2dy2[];
    // ist wohl entwas zu gross
    static final double BATTJESKOEFF = 0.3; 	// 0.1 - 0.3 Austauschkoeffizient infolge WaveBreaking
    static final double BRK = 0.15; // WaveBreakingKoefficient
    static final double MICHEKOEFF = 0.78; // Wellenbrechens
    static double WATT = 0.1;
    static final double CWAVE = 0.001 / 2.; // windstress coefficient
    double meanWaterLevelOffSet = 0.;
    
    private double previousTimeStep = 0.0; // Speichert den vorherigen Zeitschritt für das gesamte Modell

    // ----------------------------------------------------------------------
    // WaveHYPModel2D
    // ----------------------------------------------------------------------
    public WaveHYPModel2D(FEDecomposition fe, WaveHYPDat wavehypdata) {
        System.out.println("WaveHYPModel2D initialization");
        this.referenceDate = wavehypdata.referenceDate;
        fenet = fe;
        femodel = this;
        this.wavehypdat = wavehypdata;
        WATT = Math.max(wavehypdata.watt, CurrentModel2D.WATT * 2.);
        meanWaterLevelOffSet = wavehypdata.waterLevelOffSet;

        dof_data = new WaveHYPModel2DData[fenet.getNumberofDOFs()];
        element_data = new WaveHYPElementData[fenet.getNumberofFElements()];

        setNumberOfThreads(wavehypdat.NumberOfThreads);

        // DOFs initialisieren
        initialDOFs();

        initialElementModelData();

        generateClosedBoundCond();

        // Einlesen der Randbedingungen muss noch geaendert werden; Zugehoerige Datei ist randn.dat
        readBoundCond(wavehypdat.randn_name);

        final int numberofdofs = fenet.getNumberofDOFs();

        ddwadx = new double[numberofdofs];
        ddwady = new double[numberofdofs];
        ddwa2dx2 = new double[numberofdofs];
        ddwa2dy2 = new double[numberofdofs];

        try {
            System.out.println("\tOpen result file: "+ wavehypdat.xferg_name);
            xf_os = new DataOutputStream(new FileOutputStream(wavehypdat.xferg_name));
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ wavehypdat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }

    @Override
    public int getTicadErgMask() { 
        // Setzen der Ergebnismaske (depth, vector wave length, wave hight, wave period, vector wave orbial velocity, wavebreaking-Energy)
        return TicadIO.HRES_Z           // Location of the seabed
                | TicadIO.HRES_V        // vector wave length
                | TicadIO.HRES_H        // wave hight
                | TicadIO.HRES_SALT     // wavebreaking-Energy
                | TicadIO.HRES_EDDY     // wave period
//                | TicadIO.HRES_SHEAR    // vector max wave orbial velocity in wavepropagation direction // Peter 27.01.2025
                | TicadIO.HRES_SHEAR    // vector mean wave bottom shear stress in wavepropagation direction // Peter 27.01.2025
                ;
    }

    /**
     * Read the start solution from file
     *
     * @param waveergPath file with simulation results
     * @param record record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    public double[] initialSolutionFromTicadErgFile(String waveergPath, int record) throws Exception {

        System.out.println("\tRead inital values from result file " + waveergPath);
        //erstes Durchscannen
        File sysergFile = new File(waveergPath);
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
            final int anzr = inStream.readInt();
            final int anzElemente = inStream.readInt();

            //ueberlesen folgende Zeilen
            inStream.skip(9 * 4);

            //Ergebnismaske lesen und auswerten
            final int ergMaske = inStream.readInt();
            final int anzWerte = TicadIO.ergMaskeAuswerten(ergMaske);

            final boolean None_gesetzt = ((ergMaske & TicadIO.HRES_NONE) == TicadIO.HRES_NONE);
            final boolean Pos_gesetzt = ((ergMaske & TicadIO.HRES_POS) == TicadIO.HRES_POS);
            final boolean Z_gesetzt = ((ergMaske & TicadIO.HRES_Z) == TicadIO.HRES_Z);
            final boolean V_gesetzt = ((ergMaske & TicadIO.HRES_V) == TicadIO.HRES_V);              // entspricht wellenzahlvektor
            final boolean Q_gesetzt = ((ergMaske & TicadIO.HRES_Q) == TicadIO.HRES_Q);
            final boolean H_gesetzt = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);              // entspricht wellenhoehe
            final boolean SALT_gesetzt = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);     // Energie des Wellenbrechens
            final boolean EDDY_gesetzt = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);     // wellenpriode
            final boolean SHEAR_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR);  // vektor der orbitalgeschwindigkeiten
            final boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL); 
            final boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
            final boolean AH_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);           

            inStream.readInt();

            //Elemente, Rand und Knoten ueerlesen
            inStream.skip((anzElemente * 4L + anzr + 3L * anzKnoten) * 4L); //4 Bytes je float und int    

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4L) * record);

            double wh = 0., wp = 0., wlx = 1., wly = 1.;

            inStream.readFloat(); // read time
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
                    wlx = inStream.readFloat();
                    wly = inStream.readFloat();
                }

                if (Q_gesetzt) {
                    inStream.skip(8);
                }

                if (H_gesetzt) {
                    wh = inStream.readFloat();
                }

                if (SALT_gesetzt) {
                    inStream.skip(4);
                }

                if (EDDY_gesetzt) {
                    wp = inStream.readFloat();
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

                double wl = Math.sqrt(wlx * wlx + wly * wly);
                if (wl < 0.1) {
                    wlx = 1.;
                    wly = -1.;
                    wl = Math.sqrt(2.);
                }

                double kres = (2.0 * Math.PI) / wl;
                dof_data[i].wa = wh / 2.;

                dof_data[i].sigma = (2. * Math.PI) / wp; if(Double.isNaN(dof_data[i].sigma)) System.out.println("Periode des Knoten "+i+"bereits in der Stardatei "+dof_data[i].sigma);

                dof_data[i].kx = wlx / wl * kres;
                dof_data[i].ky = wly / wl * kres;
            }
        }
        
        return null;
    }

    // ----------------------------------------------------------------------
    // initialSolution
    // ----------------------------------------------------------------------
    public double[] initialSolution(double time) {
        this.time = time;

        System.out.println("\tinterpolating initial values");
        DOF[] dof = fenet.getDOFs();
        for (DOF dof1 : dof) {
            int i = dof1.number;
            dof_data[i].wa = initialWa(dof1, time);
            dof_data[i].sigma = initialSigma(dof1, time);
            double[] k = initialWaveNumber(dof1, time);
            dof_data[i].kx = k[0];
            dof_data[i].ky = k[1];
        }

        return null;
    }

    // ----------------------------------------------------------------------
    // initialWa
    // ----------------------------------------------------------------------
    private double initialWa(DOF dof, double time) {
        double wa = 0., R = 0., d;
        WaveHYPModel2DData waveData = dof_data[dof.number];
        if (waveData.bwa != null) {
            wa = waveData.bwa.getValue(time);
        } else {
            for (DOF ndof : bcs) {
                WaveHYPModel2DData wave = dof_data[ndof.number];
                if ((dof != ndof) & (wave.bwa != null)) {
                    d = dof.distance(ndof);
                    wa += wave.bwa.getValue(time) / d;
                    R += 1. / d;
                }
            }
            if (R != 0.) {
                wa /= R;
            } else {
                wa = 0.;
            }
        }
        // Wellenbrechen beachten
        // ----------------------

        double depth;
        SedimentModel2DData sedimentmodeldata = SedimentModel2DData.extract(dof);
        if (sedimentmodeldata == null) {
            depth = dof.z;
        } else {
            depth = sedimentmodeldata.z;
        }


        double totaldepth = Math.max(0., depth + meanWaterLevelOffSet);
        CurrentModel2DData currentmodeldata = CurrentModel2DData.extract(dof);
        if (currentmodeldata != null) {
            totaldepth = Math.max(0., depth + currentmodeldata.eta);
        }
        CurrentModel3DData current3D = CurrentModel3DData.extract(dof);
        if (current3D != null) {
            totaldepth = Math.max(0., depth + current3D.eta);
        }

        // zum Anfahren des Modells im Flachwasser geringere Wellenhoehen vorgeben
        wa *= Function.min(1., totaldepth / 5.);
        if (wa >= totaldepth) {
            wa = totaldepth * 0.5;
        }

        waveData.wa = wa;
        return wa;
    }

    // ----------------------------------------------------------------------
    // initialSigma
    // ----------------------------------------------------------------------
    private double initialSigma(DOF dof, double time) {
        double sigma = 0., R = 0., d;
        WaveHYPModel2DData waveData = WaveHYPModel2DData.extract(dof);
        if (waveData.bsigma != null) {
            sigma = waveData.bsigma.getValue(time);
        } else {
            for (DOF ndof : bcs) {
                WaveHYPModel2DData wave = WaveHYPModel2DData.extract(ndof);
                if ((dof != ndof) & (wave.bsigma != null)) {
                    d = dof.distance(ndof);
                    sigma += wave.bsigma.getValue(time) / d;
                    R += 1. / d;
                }
            }
            if (R != 0.) {
                sigma /= R;
            } else {
                sigma = Math.PI * 2. / 3.;
            }
        }
        waveData.sigma = sigma;
        return sigma;
    }

    // ----------------------------------------------------------------------
    // initialk
    // ----------------------------------------------------------------------
    private double[] initialWaveNumber(DOF dof, double time) {
        
        double sintheta = 0., costheta = 0., R = 0., d, wnumber;
        double[] k = {0., 0.};
        WaveHYPModel2DData waveData = WaveHYPModel2DData.extract(dof);
        if (waveData.bsintheta != null) {
            sintheta = waveData.bsintheta.getValue(time);
            costheta = waveData.bcostheta.getValue(time);
        } else {
            for (DOF ndof : bcs) {
                WaveHYPModel2DData wave = WaveHYPModel2DData.extract(ndof);
                if ((dof != ndof) & (wave.bsigma != null)) {
                    d = dof.distance(ndof);
                    sintheta += wave.bsintheta.getValue(time) / d;
                    costheta += wave.bcostheta.getValue(time) / d;
                    R += 1. / d;
                }
            }
            if (R != 0.) {
                sintheta /= R;
                costheta /= R;
            }
        }

        // Wellenbrechen beachten
        SedimentModel2DData sediment = SedimentModel2DData.extract(dof);
        double depth, totaldepth;

        //...Tiefe............................................................
        if (sediment != null) {
            depth = sediment.z;
        } else {
            depth = dof.z;
        }

        //...absolute Tiefe...................................................
        totaldepth = Math.max(WATT, depth + meanWaterLevelOffSet);
        CurrentModel2DData current = CurrentModel2DData.extract(dof);
        if (current != null) {
            totaldepth = Math.max(WATT, depth + current.eta);
        }
        CurrentModel3DData current3D = CurrentModel3DData.extract(dof);
        if (current3D != null) {
            totaldepth = Math.max(0., depth + current3D.eta);
        }

        wnumber = WaveFunction.WaveNumber(totaldepth, waveData.sigma);

        k[0] = wnumber * costheta;    // kx
        k[1] = wnumber * sintheta;    // ky

        waveData.kx = k[0];
        waveData.ky = k[1];
        return k;
    }

    @Override
    @Deprecated
    public double[] getRateofChange(double time, double x[]) {
        return null;
    } // end getRateofChange

    // ----------------------------------------------------------------------
    // ElementApproximation
    // ----------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element) {

        double timeStep;

        final FTriangle ele = (FTriangle) element;
        final DOF[] dofs = element.getDOFs();
        final double[][] koeffmat = ele.getkoeffmat();

        double[] terms_kx = new double[3];
        double[] terms_ky = new double[3];
        double[] terms_sigma = new double[3];
        double[] terms_wa = new double[3];

        // Stroemungszustandsgroessen
        // --------------------------

        //  Ableitungen fuer die Stroemungsgroessen
        double dudx = 0.;
        double dudy = 0.;
        double dvdx = 0.;
        double dvdy = 0.;

        Current2DElementData eleCurrentData = Current2DElementData.extract(ele);
        if (eleCurrentData != null) {
            dudx = eleCurrentData.dudx;
            dudy = eleCurrentData.dudy;
            dvdx = eleCurrentData.dvdx;
            dvdy = eleCurrentData.dvdy;
        }

        // wave parameter
        // --------------------
        double dkxdy = 0.;
        double dkydx = 0.;
//        double dkxdx = 0.; // kostet nur Zeit
//        double dkydy = 0.; // kostet nur Zeit
//        double dsigmaAdx = 0.;
//        double dsigmaAdy = 0.;
        double dsigmaRdx = 0.;
        double dsigmaRdy = 0.;
        double dwadx = 0.;
        double dwady = 0.;
        double dwa2dx2 = 0.;
        double dwa2dy2 = 0.;
//        double dwa3dx3 = 0.;
//        double dwa3dy3 = 0.;
//        double dwa3dx2dy = 0.;
//        double dwa3dy2dx = 0.;

        double dcgxdx = 0.;
        double dcgydy = 0.;

        double cgx_mean = 0.;
        double cgy_mean = 0.;
        double cg_mean = 0.;
        
        int iwatt = 0; // number of very small wave hight

//        double cgxR_mean = 0.;
//        double cgyR_mean = 0.;

        double wa_min = 0.;

        // Variable fuers Petrow_Galerkin_Verfahren
        //...Elementfehler der Gleichungen fuer PG-Verfahren.................
        double waveKx_mean = 0.0;
        double waveKy_mean = 0.0;
        double waveSigma_mean = 0.0;
        double waveAmplitude_mean = 0.0;

        //--------------------------------------------------------------------
        // compute element derivations
        // ---------------------------
        for (int j = 0; j < 3; j++) {

            DOF dof = ele.getDOF(j);

            // ---------------------------------------------------------------
            // Wellendaten
            // ---------------------------------------------------------------
            WaveHYPModel2DData wavehyp = dof_data[dof.number];

            // small waves
            if(wavehyp.wa < WATT) iwatt++;

            CurrentModel3DData current3D = CurrentModel3DData.extract(dof);
            if (current3D != null) {
                dudx += current3D.getU() * koeffmat[j][1];
                dudy += current3D.getU() * koeffmat[j][2];

                dvdx += current3D.getV() * koeffmat[j][1];
                dvdy += current3D.getV() * koeffmat[j][2];
            }

            //...Ableitungen Wave............................................
            dkxdy += wavehyp.kx * koeffmat[j][2];
            dkydx += wavehyp.ky * koeffmat[j][1];

//            dsigmaAdx += wmd.sigmaA * koeffmat[j][1];
//            dsigmaAdy += wmd.sigmaA * koeffmat[j][2];

            dsigmaRdx += wavehyp.sigma * koeffmat[j][1];
            dsigmaRdy += wavehyp.sigma * koeffmat[j][2];

            dwadx += wavehyp.wa * koeffmat[j][1];
            dwady += wavehyp.wa * koeffmat[j][2];

            dcgxdx += wavehyp.cgx * koeffmat[j][1];
            dcgydy += wavehyp.cgy * koeffmat[j][2];

            cgx_mean += wavehyp.cgx / 3.;
            cgy_mean += wavehyp.cgy / 3.;
            cg_mean += wavehyp.cg / 3.;

            if (j == 0) {
                wa_min = wavehyp.wa;
            } else {
                wa_min = Math.min(wa_min, wavehyp.wa);
            }

            // Der orginale  Radiation-Stress-Tensor ist
            // sxx = stxx * RHO * G * k[0][WA+j_tmp] sxy = stxy * RHO * G * k[0][WA+j_tmp] ...
            // Diese Herangehensweise erleichtert die Wattstrategie fuer die
            // Wellensimulation

            wavehyp.sxx = (wavehyp.cgR / wavehyp.c * (Function.sqr(wavehyp.kx / wavehyp.kres) + 1) - 0.5) * 0.5 * wavehyp.wa;
            wavehyp.syy = (wavehyp.cgR / wavehyp.c * (Function.sqr(wavehyp.ky / wavehyp.kres) + 1) - 0.5) * 0.5 * wavehyp.wa;
            wavehyp.sxy = wavehyp.cgR / wavehyp.c * wavehyp.kx * wavehyp.ky / wavehyp.kres / wavehyp.kres * 0.5 * wavehyp.wa;

        } // end for j (compute element derivations)
        
        double dsigmadx = dsigmaRdx;
        double dsigmady = dsigmaRdy;
        
        if (iwatt != 0) {

                dsigmaRdx = 0.;
                dsigmaRdy = 0.;
                dkxdy = 0.;
                dkydx = 0.;
//                dkxdx = 0.;
//                dkydy = 0.;

                switch (iwatt) {
                    case 1:
                        for (int j = 0; j < 3; j++) {
                            final int jg = dofs[j].number;
                            if (dof_data[jg].wa >= WATT) {
                                dsigmaRdx += dof_data[jg].sigma * koeffmat[j][1];
                                dsigmaRdy += dof_data[jg].sigma * koeffmat[j][2];
                                dkydx += dof_data[jg].ky * koeffmat[j][1];
                                dkxdy += dof_data[jg].kx * koeffmat[j][2];
//                                dkydy += dof_data[jg].ky * koeffmat[j][2];
//                                dkxdx += dof_data[jg].kx * koeffmat[j][1];
                            } else {
                                final int jg_1 = dofs[(j + 1) % 3].number;
                                final int jg_2 = dofs[(j + 2) % 3].number;
                                if ((dof_data[jg].wa < dof_data[jg_1].wa) || (dof_data[jg].wa < dof_data[jg_2].wa)) {
                                    dsigmaRdx += dof_data[jg].sigma * koeffmat[j][1];
                                    dsigmaRdy += dof_data[jg].sigma * koeffmat[j][2];
                                    dkydx += dof_data[jg].ky * koeffmat[j][1];
                                    dkxdy += dof_data[jg].kx * koeffmat[j][2];
//                                    dkydy += dof_data[jg].ky * koeffmat[j][2];
//                                    dkxdx += dof_data[jg].kx * koeffmat[j][1];
                                } else {
                                    dsigmaRdx += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].sigma + dof_data[jg_2].sigma) + dof_data[jg].wlambda * dof_data[jg].sigma) * koeffmat[j][1];
                                    dsigmaRdy += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].sigma + dof_data[jg_2].sigma) + dof_data[jg].wlambda * dof_data[jg].sigma) * koeffmat[j][2];
                                    dkydx += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].ky + dof_data[jg_2].ky) + dof_data[jg].wlambda * dof_data[jg].ky) * koeffmat[j][1];
                                    dkxdy += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].kx + dof_data[jg_2].kx) + dof_data[jg].wlambda * dof_data[jg].kx) * koeffmat[j][2];
//                                    dkydy += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].ky + dof_data[jg_2].ky) + dof_data[jg].wlambda * dof_data[jg].ky) * koeffmat[j][2];
//                                    dkxdx += (dof_data[jg].w1_lambda * 0.5 * (dof_data[jg_1].kx + dof_data[jg_2].kx) + dof_data[jg].wlambda * dof_data[jg].kx) * koeffmat[j][1];
                                }
                            }
                        }
                        break;

                    case 2:
                        for (int j = 0; j < 3; j++) {
                            final int jg = dofs[j].number;
                            if (dof_data[jg].wa >= WATT) {
                                dsigmaRdx += dof_data[jg].sigma * koeffmat[j][1];
                                dsigmaRdy += dof_data[jg].sigma * koeffmat[j][2];
                                dkydx += dof_data[jg].ky * koeffmat[j][1];
                                dkxdy += dof_data[jg].kx * koeffmat[j][2];
//                                dkydy += dof_data[jg].ky * koeffmat[j][2];
//                                dkxdx += dof_data[jg].kx * koeffmat[j][1];

                                final int jg_1 = dofs[(j + 1) % 3].number;
                                final int jg_2 = dofs[(j + 2) % 3].number;

                                if (dof_data[jg].wa > dof_data[jg_1].wa) {
                                    dsigmaRdx += dof_data[jg_1].sigma * koeffmat[(j + 1) % 3][1];
                                    dsigmaRdy += dof_data[jg_1].sigma * koeffmat[(j + 1) % 3][2];
                                    dkydx += dof_data[jg_1].ky * koeffmat[(j + 1) % 3][1];
                                    dkxdy += dof_data[jg_1].kx * koeffmat[(j + 1) % 3][2];
//                                    dkydy += dof_data[jg_1].ky * koeffmat[(j + 1) % 3][2];
//                                    dkxdx += dof_data[jg_1].kx * koeffmat[(j + 1) % 3][1];
                                } else {
                                    dsigmaRdx += (dof_data[jg_1].w1_lambda * dof_data[jg].sigma + dof_data[jg_1].wlambda * dof_data[jg_1].sigma) * koeffmat[(j + 1) % 3][1];
                                    dsigmaRdy += (dof_data[jg_1].w1_lambda * dof_data[jg].sigma + dof_data[jg_1].wlambda * dof_data[jg_1].sigma) * koeffmat[(j + 1) % 3][2];
                                    dkydx += (dof_data[jg_1].w1_lambda * dof_data[jg].ky + dof_data[jg_1].wlambda * dof_data[jg_1].ky) * koeffmat[(j + 1) % 3][1];
                                    dkxdy += (dof_data[jg_1].w1_lambda * dof_data[jg].kx + dof_data[jg_1].wlambda * dof_data[jg_1].kx) * koeffmat[(j + 1) % 3][2];
//                                    dkydy += (dof_data[jg_1].w1_lambda * dof_data[jg].ky + dof_data[jg_1].wlambda * dof_data[jg_1].ky) * koeffmat[(j + 1) % 3][2];
//                                    dkxdx += (dof_data[jg_1].w1_lambda * dof_data[jg].kx + dof_data[jg_1].wlambda * dof_data[jg_1].kx) * koeffmat[(j + 1) % 3][1];
                                }

                                if (dof_data[jg].wa > dof_data[jg_2].wa) {
                                    dsigmaRdx += dof_data[jg_2].sigma * koeffmat[(j + 2) % 3][1];
                                    dsigmaRdy += dof_data[jg_2].sigma * koeffmat[(j + 2) % 3][2];
                                    dkydx += dof_data[jg_2].ky * koeffmat[(j + 2) % 3][1];
                                    dkxdy += dof_data[jg_2].kx * koeffmat[(j + 2) % 3][2];
//                                    dkydy += dof_data[jg_2].ky * koeffmat[(j + 2) % 3][2];
//                                    dkxdx += dof_data[jg_2].kx * koeffmat[(j + 2) % 3][1];
                                } else {
                                    dsigmaRdx += (dof_data[jg_2].w1_lambda * dof_data[jg].sigma + dof_data[jg_2].wlambda * dof_data[jg_2].sigma) * koeffmat[(j + 2) % 3][1];
                                    dsigmaRdy += (dof_data[jg_2].w1_lambda * dof_data[jg].sigma + dof_data[jg_2].wlambda * dof_data[jg_2].sigma) * koeffmat[(j + 2) % 3][2];
                                    dkydx += (dof_data[jg_2].w1_lambda * dof_data[jg].ky + dof_data[jg_2].wlambda * dof_data[jg_2].ky) * koeffmat[(j + 2) % 3][1];
                                    dkxdy += (dof_data[jg_2].w1_lambda * dof_data[jg].kx + dof_data[jg_2].wlambda * dof_data[jg_2].kx) * koeffmat[(j + 2) % 3][2];
//                                    dkydy += (dof_data[jg_2].w1_lambda * dof_data[jg].ky + dof_data[jg_2].wlambda * dof_data[jg_2].ky) * koeffmat[(j + 2) % 3][2];
//                                    dkxdx += (dof_data[jg_2].w1_lambda * dof_data[jg].kx + dof_data[jg_2].wlambda * dof_data[jg_2].kx) * koeffmat[(j + 2) % 3][1];
                                }
                                break;
                            }
                        }
                        break;

                    case 3:
                        double dmax = dof_data[dofs[0].number].wa;
                        int j = 0;
                        if (dof_data[dofs[1].number].wa > dmax) {j = 1; dmax = dof_data[dofs[1].number].wa;}
                        if (dof_data[dofs[2].number].wa > dmax) {j = 2; /* dmax = dof_data[dofs[2].number].wa; **unnoetig** */}

                        final int jg = dofs[j].number;
                        dsigmaRdx += dof_data[jg].sigma * koeffmat[j][1];
                        dsigmaRdy += dof_data[jg].sigma * koeffmat[j][2];
                        dkydx += dof_data[jg].ky * koeffmat[j][1];
                        dkxdy += dof_data[jg].kx * koeffmat[j][2];
//                        dkydy += dof_data[jg].ky * koeffmat[j][2];
//                        dkxdx += dof_data[jg].kx * koeffmat[j][1];

                        final int jg_1 = dofs[(j + 1) % 3].number;
                        final int jg_2 = dofs[(j + 2) % 3].number;

                        if (dof_data[jg].wa > dof_data[jg_1].wa) {
                            dsigmaRdx += (dof_data[jg].wlambda * dof_data[jg_1].sigma + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].sigma + dof_data[jg_1].wlambda * dof_data[jg_1].sigma)) * koeffmat[(j + 1) % 3][1];
                            dsigmaRdy += (dof_data[jg].wlambda * dof_data[jg_1].sigma + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].sigma + dof_data[jg_1].wlambda * dof_data[jg_1].sigma)) * koeffmat[(j + 1) % 3][2];
                            dkydx += (dof_data[jg].wlambda * dof_data[jg_1].ky + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].ky + dof_data[jg_1].wlambda * dof_data[jg_1].ky)) * koeffmat[(j + 1) % 3][1];
                            dkxdy += (dof_data[jg].wlambda * dof_data[jg_1].kx + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].kx + dof_data[jg_1].wlambda * dof_data[jg_1].kx)) * koeffmat[(j + 1) % 3][2];
//                            dkydy += (dof_data[jg].wlambda * dof_data[jg_1].ky + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].ky + dof_data[jg_1].wlambda * dof_data[jg_1].ky)) * koeffmat[(j + 1) % 3][2];
//                            dkxdx += (dof_data[jg].wlambda * dof_data[jg_1].kx + dof_data[jg].w1_lambda * (dof_data[jg_1].w1_lambda * dof_data[jg].kx + dof_data[jg_1].wlambda * dof_data[jg_1].kx)) * koeffmat[(j + 1) % 3][1];
                        } else {
                            dsigmaRdx += (dof_data[jg_1].w1_lambda * dof_data[jg].sigma + dof_data[jg_1].wlambda * dof_data[jg_1].sigma) * koeffmat[(j + 1) % 3][1];
                            dsigmaRdy += (dof_data[jg_1].w1_lambda * dof_data[jg].sigma + dof_data[jg_1].wlambda * dof_data[jg_1].sigma) * koeffmat[(j + 1) % 3][2];
                            dkydx += (dof_data[jg_1].w1_lambda * dof_data[jg].ky + dof_data[jg_1].wlambda * dof_data[jg_1].ky) * koeffmat[(j + 1) % 3][1];
                            dkxdy += (dof_data[jg_1].w1_lambda * dof_data[jg].kx + dof_data[jg_1].wlambda * dof_data[jg_1].kx) * koeffmat[(j + 1) % 3][2];
//                            dkydy += (dof_data[jg_1].w1_lambda * dof_data[jg].ky + dof_data[jg_1].wlambda * dof_data[jg_1].ky) * koeffmat[(j + 1) % 3][2];
//                            dkxdx += (dof_data[jg_1].w1_lambda * dof_data[jg].kx + dof_data[jg_1].wlambda * dof_data[jg_1].kx) * koeffmat[(j + 1) % 3][1];
                        }

                        if (dof_data[jg].wa > dof_data[jg_2].wa) {
                            dsigmaRdx += (dof_data[jg].wlambda * dof_data[jg_2].sigma + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].sigma + dof_data[jg_2].wlambda * dof_data[jg_2].sigma)) * koeffmat[(j + 2) % 3][1];
                            dsigmaRdy += (dof_data[jg].wlambda * dof_data[jg_2].sigma + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].sigma + dof_data[jg_2].wlambda * dof_data[jg_2].sigma)) * koeffmat[(j + 2) % 3][2];
                            dkydx += (dof_data[jg].wlambda * dof_data[jg_2].ky + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].ky + dof_data[jg_2].wlambda * dof_data[jg_2].ky)) * koeffmat[(j + 2) % 3][1];
                            dkxdy += (dof_data[jg].wlambda * dof_data[jg_2].kx + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].kx + dof_data[jg_2].wlambda * dof_data[jg_2].kx)) * koeffmat[(j + 2) % 3][2];
//                            dkydy += (dof_data[jg].wlambda * dof_data[jg_2].ky + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].ky + dof_data[jg_2].wlambda * dof_data[jg_2].ky)) * koeffmat[(j + 2) % 3][2];
//                            dkxdx += (dof_data[jg].wlambda * dof_data[jg_2].kx + dof_data[jg].w1_lambda * (dof_data[jg_2].w1_lambda * dof_data[jg].kx + dof_data[jg_2].wlambda * dof_data[jg_2].kx)) * koeffmat[(j + 2) % 3][1];
                        } else {
                            dsigmaRdx += (dof_data[jg_2].w1_lambda * dof_data[jg].sigma + dof_data[jg_2].wlambda * dof_data[jg_2].sigma) * koeffmat[(j + 2) % 3][1];
                            dsigmaRdy += (dof_data[jg_2].w1_lambda * dof_data[jg].sigma + dof_data[jg_2].wlambda * dof_data[jg_2].sigma) * koeffmat[(j + 2) % 3][2];
                            dkydx += (dof_data[jg_2].w1_lambda * dof_data[jg].ky + dof_data[jg_2].wlambda * dof_data[jg_2].ky) * koeffmat[(j + 2) % 3][1];
                            dkxdy += (dof_data[jg_2].w1_lambda * dof_data[jg].kx + dof_data[jg_2].wlambda * dof_data[jg_2].kx) * koeffmat[(j + 2) % 3][2];
//                            dkydy += (dof_data[jg_2].w1_lambda * dof_data[jg].ky + dof_data[jg_2].wlambda * dof_data[jg_2].ky) * koeffmat[(j + 2) % 3][2];
//                            dkxdx += (dof_data[jg_2].w1_lambda * dof_data[jg].kx + dof_data[jg_2].wlambda * dof_data[jg_2].kx) * koeffmat[(j + 2) % 3][1];
                        }
                        break;
                }
            }

            for (int j = 0; j < 3; j++) {
                int i = ele.getDOF(j).number;
                ddwadx[i] += dwadx;
                ddwady[i] += dwady;
                ddwa2dx2[i] += dwa2dx2;
                ddwa2dy2[i] += dwa2dy2;
            }
        
            double elementsize = ele.maxEdgeLength;
            boolean indicator = false;
            if (Function.norm(dof_data[dofs[0].number].cgx, dof_data[dofs[0].number].cgy) > WATT / 10.) {
                elementsize = Function.min(ele.getVectorSize(dof_data[dofs[0].number].cgx, dof_data[dofs[0].number].cgy), elementsize);
                indicator=true;
            }
            if (Function.norm(dof_data[dofs[1].number].cgx, dof_data[dofs[1].number].cgy) > WATT / 10.) {
                elementsize = Function.min(ele.getVectorSize(dof_data[dofs[1].number].cgx, dof_data[dofs[1].number].cgy), elementsize);
                indicator=true;
            }
            if (Function.norm(dof_data[dofs[2].number].cgx, dof_data[dofs[2].number].cgy) > WATT / 10.) {
                elementsize = Function.min(ele.getVectorSize(dof_data[dofs[2].number].cgx, dof_data[dofs[2].number].cgy), elementsize);
                indicator=true;
            }
            if(!indicator) elementsize = ele.minHight;

            // Elementfehler berechnen
            for (int j = 0; j < 3; j++) {
                DOF dof = ele.getDOF(j);
                WaveHYPModel2DData wmd = dof_data[dof.number];

                terms_kx[j] = dsigmaRdx * wmd.wlambda
                        - wmd.cgy * (dkydx - dkxdy) * wmd.wlambda
                        // change of wave direction by wind
                        - wmd.windchangekx * ((wmd.bkx == null)?1:0)
                        // Diffraktion
                        + wmd.cg / (PhysicalParameters.G * Function.max(WATT, wmd.wa)) * dwadx * ((wmd.bkx == null)?1:0) // Peters einfache Version 
                        ;
                terms_ky[j] = dsigmaRdy * wmd.wlambda
                        + wmd.cgx * (dkydx - dkxdy) * wmd.wlambda
                        // change of wave direction by wind
                        - wmd.windchangeky * ((wmd.bky == null)?1:0)
                        // Diffraktion
                        + wmd.cg / (PhysicalParameters.G * Function.max(WATT, wmd.wa)) * dwady * ((wmd.bky == null)?1:0) // Peters einfache Version
                        ;
                
                terms_sigma[j] = (wmd.cgx * dsigmaRdx + wmd.cgy * dsigmaRdy) * wmd.wlambda
                        // change of wave periode by wind
                        - wmd.windchangeperiode  
                        // Diffraktion funktioniert nicht
//                        - wmd.cg / (2. * wmd.kres * Function.max(WATT, wmd.wa)) * (- wmd.dwadt / Function.max(WATT, wmd.wa) * (dwa2dx2 + dwa2dy2))/* * wmd.wlambda*/
//                                                  + wmd.cg / (2. * wmd.kres * Function.max(WATT, wmd.wa))*( dwa3dx2dt + dwa3dy2dt - wmd.dwadt/Math.max(WATT,wmd.wa)*(dwa2dx2+dwa2dy2))
//                        // Diffusion / Diffraction  - weiter unten
//                        + 3. * (cg_mean / PhysicalParameters.G + Function.norm(dwadx, dwady)) * (koeffmat[j][1] * dsigmadx + koeffmat[j][2] * dsigmady) // second term
                        ;
                terms_wa[j] = (wmd.cgx * dwadx + wmd.cgy * dwady)
                        + wmd.wa / 2. * ((dcgxdx + dcgydy))
                        - (wmd.sxx * dudx + wmd.sxy * dudy + wmd.sxy * dvdx + wmd.syy * dvdy) * wmd.wlambda
                        - wmd.windchangeamplitude  // change of wave amplitude by wind
//                        + 2./ (3. * Math.PI) / (PhysicalParameters.G * Function.max(WATT, wmd.wa)) * Math.max(PhysicalParameters.DYNVISCOSITY_WATER,0.3) * Function.pow3(wmd.bottomvelocity) // urspruengliche Formulierung
                        + 4. / (3. * Math.PI) / (PhysicalParameters.RHO_WATER * PhysicalParameters.G * Function.max(WATT, wmd.wa)) * wmd.bfcoeff * wmd.bottomvelocity // coastalwiki & Bagnold
//                        + Math.max(PhysicalParameters.DYNVISCOSITY_WATER,wmd.bfcoeff) * wmd.wa * wmd.wa // Wave bottom friction, in Anlehnung an Yoo, D.: Mean Bed Friction of Combined Wave/Current Folw, Coastal Engineering, Vol. 12, pp. 1-21, 1988
//                        + 3. * cg_mean / PhysicalParameters.G * (koeffmat[j][1] * dwadx + koeffmat[j][2] * dwady) // Diffusion / Diffraction - weiter unten
                        + wmd.epsilon_b / (PhysicalParameters.RHO_WATER * PhysicalParameters.G * Function.max(WATT, wmd.wa)) // wavebreaking 
                        ;

                if ((wmd.bkx == null)) {
                    waveKx_mean += 1. / 3. * (wmd.dkxdt + terms_kx[j]) * wmd.wlambda;
                }

                if ((wmd.bky == null)) {
                    waveKy_mean += 1. / 3. * (wmd.dkydt + terms_ky[j]) * wmd.wlambda;
                }

                if ((wmd.bsigma == null)) {
                    waveSigma_mean += 1. / 3. * (wmd.dsigmadt + terms_sigma[j]) * wmd.wlambda;
                }

                if ((wmd.bwa == null)) {
                    waveAmplitude_mean += 1. / 3. * (wmd.dwadt + terms_wa[j]) * wmd.wlambda;
                }

            } // Ende Knotenwerte wellen
            
            // Operatornorm 
            final double operatornorm1 = cgx_mean*cgx_mean;
            final double operatornorm2 = cgy_mean*cgy_mean;
            final double operatornorm = Math.sqrt(operatornorm1 + operatornorm2);
            final double tau_wave = 0.5 * elementsize / operatornorm;

            timeStep = tau_wave;

            // Fehlerkorrektur durchfuehren
            for (int j = 0; j < 3; j++) {
                WaveHYPModel2DData wmd = dof_data[ele.getDOF(j).number];
                wmd.anz_activ_el++;
                
                double result_kx_i = -tau_wave * (koeffmat[j][1] * waveSigma_mean + koeffmat[j][1] * (-cgy_mean * waveKy_mean) + koeffmat[j][2] * cgy_mean * waveKx_mean) * ele.area;
                
                double result_ky_i = -tau_wave * (koeffmat[j][2] * waveSigma_mean + koeffmat[j][2] * (-cgx_mean * waveKx_mean) + koeffmat[j][1] * cgx_mean * waveKy_mean) * ele.area;

                double result_sigma_i = -tau_wave * (koeffmat[j][1] * cgx_mean + koeffmat[j][2] * cgy_mean)  * waveSigma_mean * ele.area;
                result_sigma_i -= (cg_mean / PhysicalParameters.G + Function.norm(dwadx, dwady)) * (koeffmat[j][1] * dsigmadx + koeffmat[j][2] * dsigmady) * ele.area;
                
                double result_wa_i = -tau_wave * (koeffmat[j][1] * cgx_mean + koeffmat[j][2] * cgy_mean) * waveAmplitude_mean * ele.area;
                result_wa_i -= cg_mean / PhysicalParameters.G * (koeffmat[j][1] * dwadx + koeffmat[j][2] * dwady) * ele.area;

                double extwa=0., extkx=0., extky=0., extsigma=0.;
                int waAnz=0, sigmaAnz=0;

                for (int l = 0; l < 3; l++) {
                    
                    final int lGlobal = ele.getDOF(l).number;
                    final double vorfak =  ele.area * ((l == j) ? 1. / 6. : 1. / 12.);
                    double gl = (l == j) ? 1. : dof_data[lGlobal].wlambda;

                    /* Wellenzahlvektor */
                    result_kx_i -= vorfak * terms_kx[l] * gl;
                    result_ky_i -= vorfak * terms_ky[l] * gl;

                    /* Kreisfrequenz */
                    result_sigma_i -= vorfak * terms_sigma[l] * gl;

                    /* Wellenamplithude */
                    result_wa_i -= vorfak * terms_wa[l] * gl;
                    
                    if (l != j) {
                        if (dof_data[lGlobal].wa > WATT) {
                            extwa += dof_data[lGlobal].wa;
                            waAnz++;
                            if (dof_data[lGlobal].bsintheta != null) { // doppelt Wichtung gesetzter RB
                                extwa += dof_data[lGlobal].wa;
                                waAnz++;
                            }
                        }
                        extkx += dof_data[lGlobal].kx;
                        extky += dof_data[lGlobal].ky;
                        extsigma += dof_data[lGlobal].sigma;
                        sigmaAnz++;
                        if (dof_data[lGlobal].bsintheta != null) { // doppelt Wichtung gesetzter RB
                            extkx += dof_data[lGlobal].kx;
                            extky += dof_data[lGlobal].ky;
                            extsigma += dof_data[lGlobal].sigma;
                            sigmaAnz++;
                        }
                    }
                } // end for l
                
                synchronized (wmd) {
                    wmd.rkx += result_kx_i;
                    wmd.rky += result_ky_i;
                    wmd.rsigma += result_sigma_i;
                    wmd.rwa += result_wa_i;

                    wmd.extwa += extwa;
                    wmd.waAnz += waAnz;
                    
                    wmd.extkx += extkx;
                    wmd.extky += extky;
                    wmd.extsigma += extsigma;
                    wmd.sigmaAnz += sigmaAnz;
   
                }
            } // end for j
        return timeStep;
    } // end ElementApproximation

    // ----------------------------------------------------------------------
    // setBoundaryCondition
    // ----------------------------------------------------------------------
    @Override
    public void setBoundaryCondition(DOF dof, double t) {

        WaveHYPModel2DData wavehyp = dof_data[dof.number];

        wavehyp.rkx = 0.;
        wavehyp.rky = 0.;
        wavehyp.rsigma = 0.;
        wavehyp.rwa = 0.;
        
        if (wavehyp.waAnz != 0) {
            wavehyp.extwa /= wavehyp.waAnz;
        } else {
            wavehyp.extwa = wavehyp.wa;
        }

        if (wavehyp.sigmaAnz != 0) {
            wavehyp.extsigma /= wavehyp.sigmaAnz;
            wavehyp.extkx /= wavehyp.sigmaAnz;
            wavehyp.extky /= wavehyp.sigmaAnz;
        } else {
            wavehyp.extsigma = wavehyp.sigma;
            wavehyp.extkx = wavehyp.kx;
            wavehyp.extky = wavehyp.ky;
        }

        double u = 0, v = 0.;
        double totaldepth = dof.z + meanWaterLevelOffSet;
        double ks = 0.015; //ebene Flusssohle mit feinem Sand
        CurrentModel2DData current = CurrentModel2DData.extract(dof);
        if (current != null) {
            totaldepth = current.totaldepth;
            u = current.u;
            v = current.v;
            ks=current.ks;
        }
        CurrentModel3DData current3D = CurrentModel3DData.extract(dof);
        if (current3D != null) {
            totaldepth = current3D.totaldepth;
            u = current3D.getU();
            v = current3D.getV();
            ks=current3D.ks;
        }

        totaldepth = Function.max(totaldepth, 0.);

        // Knoten die im Patch mit einer gesetzten Randbedingung verbunden sind
        if (wavehyp.ibound > 0) {
            wavehyp.kx = wavehyp.extkx;
            wavehyp.ky = wavehyp.extky;
            wavehyp.sigma = wavehyp.extsigma;
        }

        if (wavehyp.bwa != null) {
            wavehyp.wa = wavehyp.bwa.getValue(t);
//            wmd.dwadt = wmd.bwa.getDifferential(t);
        } else if(wavehyp.extrapolate_wa) wavehyp.wa = (9. * wavehyp.wa + wavehyp.extwa)/10.;
        
        if (wavehyp.wa < 0.) wavehyp.wa=0.;
        if (wavehyp.wa >= totaldepth * MICHEKOEFF) wavehyp.wa = totaldepth * MICHEKOEFF; // Peter 14.01.21

        wavehyp.wlambda = Function.min(1., wavehyp.wa / WATT);
        wavehyp.w1_lambda = 1. - wavehyp.wlambda;

        if (wavehyp.bsigma != null) {
            wavehyp.sigma = wavehyp.bsigma.getValue(t);
//            wmd.dsigmadt = wmd.bsigma.getDifferential(t);
        } else if (wavehyp.extrapolate_sigma) {
            wavehyp.sigma = (9. * wavehyp.sigma + wavehyp.extsigma) / 10.;
        } 
        else 
            wavehyp.sigma = wavehyp.wlambda * (999. * wavehyp.sigma + wavehyp.extsigma) / 1000. + wavehyp.w1_lambda * (9. * wavehyp.sigma + wavehyp.extsigma) / 10.; // smooth and extrapoating to nodes without waveamplitude
        
        wavehyp.sigmaA = wavehyp.sigma;
        if(current != null)
            wavehyp.sigmaA += wavehyp.kx*current.u+wavehyp.ky*current.v; // absolut angular frequency

// ToDo   Wellenlaenge aendern auf Grund der Stroemung
        wavehyp.kres = WaveFunction.WaveNumber(Math.max(WATT, totaldepth), wavehyp.sigma);
        // Wavenumber with Diffraction ! funktioniert so nicht !
//        final double deltaStar = 1./ Math.max(wavehyp.wa, WATT) * Function.norm(wavehyp.dwa2dx2, wavehyp.dwa2dy2) * wavehyp.wlambda;
//        wavehyp.kres = Math.sqrt(wavehyp.kres*wavehyp.kres+deltaStar);

        if (wavehyp.bsintheta != null) {
            wavehyp.kx = wavehyp.bcostheta.getValue(t) * wavehyp.kres;
            wavehyp.ky = wavehyp.bsintheta.getValue(t) * wavehyp.kres;
        } else if ((wavehyp.extrapolate_kx || wavehyp.extrapolate_ky)) {
            wavehyp.kx = (9. * wavehyp.kx + wavehyp.extkx) / 10.;
            wavehyp.ky = (9. * wavehyp.ky + wavehyp.extky) / 10.;
            double ktmp = Function.norm(wavehyp.kx, wavehyp.ky);
            wavehyp.kx = wavehyp.kx / ktmp * wavehyp.kres;
            wavehyp.ky = wavehyp.ky / ktmp * wavehyp.kres;
        } 
        else {                    
            wavehyp.kx = wavehyp.wlambda * (99. * wavehyp.kx + wavehyp.extkx) / 100. + wavehyp.w1_lambda * (9. * wavehyp.kx + wavehyp.extkx) / 10.; // smooth and extrapoating to nodes without waveamplitude
            wavehyp.ky = wavehyp.wlambda * (99. * wavehyp.ky + wavehyp.extky) / 100. + wavehyp.w1_lambda * (9. * wavehyp.ky + wavehyp.extky) / 10.;
            double ktmp = Function.norm(wavehyp.kx, wavehyp.ky);
            wavehyp.kx = wavehyp.kx / ktmp * wavehyp.kres;
            wavehyp.ky = wavehyp.ky / ktmp * wavehyp.kres;
        }

        // update waveparameter
        //-----------------------------
        //Wavevelocity
        wavehyp.c = wavehyp.sigma / wavehyp.kres;
        // relative wavegroupevelocity
        wavehyp.cgR = 0.5 * (1. + 2. * wavehyp.kres * totaldepth
                / Math.sinh(2. * wavehyp.kres * Math.max(WATT, totaldepth))) * wavehyp.c;
        wavehyp.cgxR = wavehyp.cgR * wavehyp.kx / wavehyp.kres;
        wavehyp.cgyR = wavehyp.cgR * wavehyp.ky / wavehyp.kres;
        // absolute wavegroupevelocity
        wavehyp.cgx = wavehyp.cgxR + u;
        wavehyp.cgy = wavehyp.cgyR + v;
        wavehyp.cg = Function.norm(wavehyp.cgx, wavehyp.cgy);


        // waveenergy dissipation rate (wave breaking)
        double stabelamplitude = Math.PI / (7. * wavehyp.kres) * Math.tanh(wavehyp.kres * totaldepth); // Grenzsteilheit
//        stabelamplitude = Function.min(stabelamplitude, Math.PI / (7. * wavehyp.kres) * Math.tanh(7.* wavehyp.wa / Math.PI * wavehyp.kres)); 
        wavehyp.epsilon_b = Math.max(0., 0.5 * PhysicalParameters.RHO_WATER * PhysicalParameters.G * BRK
                * wavehyp.cg / Math.max(WATT, totaldepth)
                * (Function.sqr(wavehyp.wa) - Function.sqr(stabelamplitude)));

        // Wave induced maximal bottomvelocity
        wavehyp.bottomvelocity = wavehyp.kres * PhysicalParameters.G * wavehyp.wa / (wavehyp.sigma * Math.cosh(wavehyp.kres * totaldepth));
        wavehyp.ubwave = wavehyp.kx / wavehyp.kres * wavehyp.bottomvelocity;
        wavehyp.vbwave = wavehyp.ky / wavehyp.kres * wavehyp.bottomvelocity;
        
        /* Wave energy dissipation by bottom friction */
        final double A = wavehyp.bottomvelocity / wavehyp.sigma;// http://www.coastalwiki.org/wiki/Shallow-water_wave_theory
        /** Ansatz von Bagnold 1949, siehe Malcherek: Gezeiten und Wellen S. 260 ff */ 
        final double r = A/ks;
        double f_w;
        if(r>1.){
            f_w = 0.00251 * Math.exp(5.21*Math.pow(r, -0.19));
        }else{
            f_w=0.46;
        }
        /** Coastalwiki */
//        final double r = Math.max(1.,A/ks);// http://www.coastalwiki.org/wiki/Shallow-water_wave_theory
//        final double f_w = 0.237 * Math.pow(r,-0.52);// http://www.coastalwiki.org/wiki/Shallow-water_wave_theory
        wavehyp.bfcoeff = 0.5 * PhysicalParameters.RHO_WATER * f_w * wavehyp.bottomvelocity/* * wavehyp.bottomvelocity*/; // coastalwiki & Bagnold
        wavehyp.taubX = wavehyp.bfcoeff * wavehyp.ubwave; // maximal bottom shear stress // Peter 27,01.25
        wavehyp.taubY = wavehyp.bfcoeff * wavehyp.vbwave;
        wavehyp.bfcoeff *= wavehyp.bottomvelocity;
//        wavehyp.bfcoeff = 16. * f_w * Function.pow3(wavehyp.bottomvelocity) / (3. * Math.PI * PhysicalParameters.G); // Yoo
//        wavehyp.bfcoeff = 2. * PhysicalParameters.RHO_WATER * f_w * Function.pow3(wavehyp.bottomvelocity) / (3. * Math.PI); // Robert A. Dalrymple, Wave Propagation in Shallow Water, in Proceedings of the Short Course on Design and Reliability of Coastal Structures, Venice attached to 23. ICCE
//        wavehyp.bfcoeff = 0.6 * PhysicalParameters.RHO_WATER * f_w * Function.pow3(wavehyp.bottomvelocity); // wavehyp.root-mean-square near-bottom wave velocity [Dean and Dalrymple, 1991] // Monismith, S. G., J. S. Rogers, D. Koweek, and R. B. Dunbar (2015), Frictional wave dissipation on a remarkably rough reef, Geophys. Res. Lett., 42, doi:10.1002/2015GL063804
        
        /* wind energy input */
        wavehyp.windchangeamplitude = 0.;
        wavehyp.windchangeperiode = PhysicalParameters.KINVISCOSITY_WATER * wavehyp.epsilon_b / Function.max(totaldepth, 1.);  // Beruecksichtigung der Periodenveraenderung durch Wellenbrechen (sigma wird groeszer)
        wavehyp.windchangeperiode += 0.0001 * wavehyp.bottomvelocity; // Beruecksichtigung der Periodenveraenderung durch Bodenreibung (sigma wird groeszer)
        wavehyp.windchangekx = 0.;
        wavehyp.windchangeky = 0.;

        MeteorologyData2D meteorologyData2D = MeteorologyData2D.extract(dof);
        if (meteorologyData2D != null) {
            final double windx = meteorologyData2D.windx;
            final double windy = meteorologyData2D.windy;
            final double wc = meteorologyData2D.windspeed;
            if (wc > 0.1) {
                // Die maximale Wellenperiode bei der Erzeugung durch Wind ist durch die Wassertiefe beschraenkt
                // Resio, D.T., Bratos, S.M., and Thompson, E.F. (2003). Meteorology and Wave Climate, Chapter II-2. Coastal Engineering Manual. US Army Corps of Engineers, Washington DC, 72pp.
                final double T_max = 9.78 * Math.sqrt(Math.max(WATT, totaldepth) / G);
                final double sigma_min = 2. * PI / T_max;
                
                final double waWind = (0.008 * wc * wc + 0.11 * wc) / 2.;
                final double wkx = wavehyp.kx / wavehyp.kres, wky = wavehyp.ky / wavehyp.kres;
                final double wdx = windx / wc, wdy = windy / wc;
                
                final double uwind = Math.max(0., (wkx * windx + wky * windy));
                /* Peters einfache Variante nach Pabst 1998*/
                /* wh=0.0035*uwind*uwind+0.05*uwind*/
                final double waUWind = Math.min(stabelamplitude, (0.008 * uwind * uwind + 0.11 * uwind) / 2.);

                double alphaWind = angleRad(wdx, wdy);
                double alphaWave = angleRad(wkx, wky);
                double sinus = Math.sin(alphaWind - alphaWave);
                
                double lambda = CWAVE * (waWind-waUWind) / Function.max(WATT, wavehyp.wa)/* * wavehyp.cg*/; // Peter 20.08.2021 cg auskommentiert
                wavehyp.windchangekx += -wavehyp.ky * sinus * lambda;
                wavehyp.windchangeky +=  wavehyp.kx * sinus * lambda;
                wavehyp.windchangeperiode += Math.min(0., Math.max(sigma_min, 2. * Math.PI / (1.8 * Math.sqrt(wc))) - wavehyp.sigma) * lambda; // wc > 0. 

                wavehyp.windchangeamplitude = CWAVE * Math.max(0., waUWind - wavehyp.wa) * wavehyp.cg;
                if (wavehyp.windchangeamplitude > 0.) {
                    /* Peters einfache Variante nach Pabst 1998*/
                    /* Tp=1.55*Math.sqrt(uwind) */
                    wavehyp.windchangeperiode += (Math.max(sigma_min, 2. * Math.PI / (1.8 * Math.sqrt(uwind))) - wavehyp.sigma) * wavehyp.windchangeamplitude; // wenn wavehyp.windchangeamplitude > 0., dann ist auch uwind > 0.

                    wavehyp.windchangekx += -wavehyp.ky * sinus * wavehyp.windchangeamplitude;
                    wavehyp.windchangeky +=  wavehyp.kx * sinus * wavehyp.windchangeamplitude;
                }
            }
           
            if (wavehyp.bwa != null) {
                wavehyp.windchangeamplitude = 0.;
            }
        }

// ToDo
// Achtung extrapolation an geschlossenen Raendern muss noch geprueft werden       
        if (wavehyp.sigma < .1) wavehyp.sigma = .1;
        if (wavehyp.sigma > 15.) wavehyp.sigma = 15.;

        wavehyp.anz_activ_el = 0;
        
        wavehyp.extkx =0.;        
        wavehyp.extky =0.;        
        wavehyp.extsigma =0.;        
        wavehyp.extwa =0.;        
        wavehyp.waAnz =0;        
        wavehyp.sigmaAnz=0; 
    } // end setBoundaryCondition
    
    // normierte Vektoren
    public static double angleRad(double wdx, double wdy) {
        return (wdy < 0.) ? (2.*Math.PI - Math.acos(wdx)) : Math.acos(wdx);
    }

    @Override
    public ModelData genData(FElement felement) {
        element_data[felement.number] = new WaveHYPElementData();
        return element_data[felement.number];
    }

    // ----------------------------------------------------------------------
    // genData
    // ----------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof) {

        dof_data[dof.number] = new WaveHYPModel2DData();
        //...Rueckgabe.......................................................
        return dof_data[dof.number];

    } // end genData

    // ----------------------------------------------------------------------
    // readBoundCond
    // ----------------------------------------------------------------------
    public final void readBoundCond(String name) {
        boolean noEOF = true;
        RandN_Reader randnreader = new RandN_Reader(name);
        while (noEOF) {
            DiscretScalarFunction1d[] fkt = randnreader.readnextTimefunction();
            if (fkt == null) {
                return;
            }
            int[] nummern = randnreader.readNodes();
            for (int i = 0; i < nummern.length; i++) {
                DOF dof = fenet.getDOF(nummern[i]);
                bcs.add(dof);
                WaveHYPModel2DData wavehyp = WaveHYPModel2DData.extract(dof);
                wavehyp.bwa = fkt[0];
                wavehyp.btheta = fkt[1];
                wavehyp.bsigma = fkt[2];

                wavehyp.bsintheta = fkt[3];
                wavehyp.bcostheta = fkt[4];

                wavehyp.bwa.setPeriodic(true);
                wavehyp.btheta.setPeriodic(true);
                wavehyp.bsigma.setPeriodic(true);

                wavehyp.bsintheta.setPeriodic(true);
                wavehyp.bcostheta.setPeriodic(true);
                
                // Kennung fuer RB an benachbarten Knoten hochzaehlen
                FElement[] felem = dof.getFElements();
                for (FElement elem : felem) {
                    for (int l = 0; l < 3; l++) {
                        dof_data[elem.getDOF((l)).number].ibound++;
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // generateClosedBoundCond
    // ----------------------------------------------------------------------
    final void generateClosedBoundCond() {
        //System.out.println(" generateClosedBoundCond ");
        ZeroFunction1d zerofct = new ZeroFunction1d();
        WaveHYPModel2DData wavehyp;
        FElement[] felem = fenet.getFElements();
        for (FElement felem1 : felem) {
            FTriangle tele = (FTriangle) felem1;
            if (tele.getKennung() != 0) {

                // Kennung fuer RB hochzaehlen
                WaveHYPModel2DData.extract(tele.getDOF(0)).ibound++;
                WaveHYPModel2DData.extract(tele.getDOF(1)).ibound++;
                WaveHYPModel2DData.extract(tele.getDOF(2)).ibound++;
                
                
                if (tele.getKennung() == FTriangle.bit_kante_jk) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_jk+" bit_kante_jk");
                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(1));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;

                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(2));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;
                    
                } else if(tele.getKennung() == FTriangle.bit_kante_ki) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ki+" bit_kante_ki");
                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(0));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;

                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(2));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;
                    
                } else if(tele.getKennung() == FTriangle.bit_kante_ij) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ij+" bit_kante_ij");
                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(0));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;

                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(1));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;
                    
                } else if((tele.getKennung() == FTriangle.bit_kante_ijk) ||
                        (tele.getKennung() == FTriangle.bit_kante_jki) ||
                        (tele.getKennung() == FTriangle.bit_kante_kij) ||
                        (tele.getKennung() == FTriangle.bit_kante_ijki) ) {
                    //	System.out.println("alle");
                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(0));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;

                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(1));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;

                    wavehyp = WaveHYPModel2DData.extract(tele.getDOF(2));
                    if(wavehyp.bwa == null) wavehyp.bwa = zerofct;
                    if(wavehyp.bsigma == null) wavehyp.extrapolate_sigma = true;
                    if(wavehyp.bkx == null) wavehyp.extrapolate_kx = true;
                    if(wavehyp.bky == null) wavehyp.extrapolate_ky = true;
                }
            }
        }
        DOF[] dofs = fenet.getDOFs();
        for (DOF dof : dofs) {
            wavehyp = WaveHYPModel2DData.extract(dof);
            wavehyp.extrapolate_wa |= ((dof.number < ((FTriangleMesh) fenet).anzr) && (wavehyp.bwa == null));
            wavehyp.extrapolate_sigma |= ((dof.number < ((FTriangleMesh) fenet).anzr) && (wavehyp.bsigma == null));
            wavehyp.extrapolate_kx |= ((dof.number < ((FTriangleMesh) fenet).anzr) && (wavehyp.bkx == null));
            wavehyp.extrapolate_ky |= ((dof.number < ((FTriangleMesh) fenet).anzr) && (wavehyp.bky == null));
        }
    } // end generateClosedBoundCond

    // ----------------------------------------------------------------------
    // write_erg_xf
    // ----------------------------------------------------------------------
    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void write_erg_xf(double[] erg, double t) {
    } // end write_erg_xf

    // ----------------------------------------------------------------------
    // write_erg_xf
    // ----------------------------------------------------------------------
    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            
             for (DOF dof : fenet.getDOFs()) {
                int i = dof.number; 
                WaveHYPModel2DData wavehyp = dof_data[i];
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                double kres = Function.norm(wavehyp.kx, wavehyp.ky);
                double wl = 2. * Math.PI / kres;
                double Erg_wlx = (float) (wl * wavehyp.kx / kres);
                double Erg_wly = (float) (wl * wavehyp.ky / kres);
                double Erg_wh = (float) Function.max(0., 2. * wavehyp.wa);
                double Erg_wp = 2. * Math.PI / ((float) wavehyp.sigma);
                //                if(Erg_wh < WATT/10.){
                //                    Erg_wh=0.;
                //                    Erg_wlx=0.;
                //                    Erg_wly= 0.;
                //                    Erg_wp=0.;
                //                }

                double ergZ = dof.z;
                SedimentModel2DData sed = SedimentModel2DData.extract(dof);
                if (sed != null) {
                    ergZ = sed.z;
                }

                xf_os.writeFloat((float) ergZ);
                xf_os.writeFloat((float) Erg_wlx);        // x-component of the wavelength-vector
                xf_os.writeFloat((float) Erg_wly);        // y-component of the wavelength-vector
                xf_os.writeFloat((float) Erg_wh);        // wavehight
                
                xf_os.writeFloat((float) wavehyp.epsilon_b);        // wavebreaking-Energy

                xf_os.writeFloat((float) Erg_wp);        // waveperiod
                
                xf_os.writeFloat((float) wavehyp.ubwave);        // x-component of the waveorbital-vector // Peter 27.01.2025
                xf_os.writeFloat((float) wavehyp.vbwave);        // y-component of the waveorbital-vector
                
//                xf_os.writeFloat((float) wavehyp.taubX);        // x-component of the max. bottom shearstress Vector // Peter 27.01.2025
//                xf_os.writeFloat((float) wavehyp.taubY);        // y-component of the max. bottom shearstress Vector

            }
            xf_os.flush();
        } catch (IOException e) {
            System.out.println(this.getClass()+"\n\ttime="+time+"\n");
            e.printStackTrace();
            System.exit(0);
        }
    } // end write_erg_xf

    @Override
    public void timeStep(double dt) {

        resultIsNaN = false;

        java.util.Arrays.fill(ddwadx, 0.0);
        java.util.Arrays.fill(ddwady, 0.0);
        java.util.Arrays.fill(ddwa2dx2, 0.0);
        java.util.Arrays.fill(ddwa2dy2, 0.0);
        
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
            double omega = dt / previousTimeStep / 2.0;
            beta0 = 1.0 + omega;
            beta1 = -omega;
        }
        
        Arrays.stream(fenet.getDOFs()).parallel().forEach(dof -> {
                final int i = dof.number;
                WaveHYPModel2DData wavehyp = dof_data[i];

                if (wavehyp.anz_activ_el != 0) {
                    wavehyp.rkx /= dof.lumpedMass;
                    wavehyp.rky /= dof.lumpedMass;
                    wavehyp.rwa /= dof.lumpedMass;
                    wavehyp.rsigma /= dof.lumpedMass;
                } else {
                    wavehyp.rkx = 0.;
                    wavehyp.rky = 0.;
                    wavehyp.rwa = 0.;
                    wavehyp.rsigma = 0.;
                }


                double rkx = beta0 * wavehyp.rkx + beta1 * wavehyp.dkxdt;  // zusaetzlichen Stabilisierung in Anlehnung am expliziten Adams-Bashford 2. Ordnung
                double rky = beta0 * wavehyp.rky + beta1 * wavehyp.dkydt;
                double rwa = beta0 * wavehyp.rwa + beta1 * wavehyp.dwadt;
                double rsigma = beta0 * wavehyp.rsigma + beta1 * wavehyp.dsigmadt;

                wavehyp.dkxdt = wavehyp.rkx;
                wavehyp.dkydt = wavehyp.rky;
                wavehyp.dwadt = wavehyp.rwa;
                wavehyp.dsigmadt = wavehyp.rsigma;

                wavehyp.kx += dt * rkx;
                wavehyp.ky += dt * rky;
                wavehyp.wa += dt * rwa;
                wavehyp.sigma += dt * rsigma;

                boolean rIsNaN = Double.isNaN(rkx) || Double.isNaN(rky) || Double.isNaN(rwa) || Double.isNaN(rsigma);
                if (rIsNaN) {
                    System.out.println("WaveHYPModel2D is NaN bei " + i);
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
            } catch (IOException e) {}
            System.exit(0);
        }
    }
    
    public static void main(String... args){
        double uwind=0.1;
        System.out.println((0.008 * uwind * uwind + 0.11 * uwind) / 2.);
        System.out.println("Periode = " + (1.8 * Math.sqrt(uwind)));
        System.out.println("sigma = " + (2.*Math.PI)/(1.8 * Math.sqrt(uwind)));
    }
}
