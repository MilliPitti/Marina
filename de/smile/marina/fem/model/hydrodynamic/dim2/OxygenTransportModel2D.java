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

/**
 * this ODE describe oxygen-transport model for depth integrated simulations
 * 
 * @version 4.7.0
 * @author Peter Milbradt
 */
public class OxygenTransportModel2D extends TimeDependentFEApproximation
        implements FEModel, TicadModel, TimeDependentModel {

    public final static double dispersionCoefficient = 1.1E-9; // Diffusionskoeffizient fuer Sauerstoff in m^2/s

    private DataOutputStream xf_os = null;

    private Vector<DOF> initsc = new Vector<>();

    private Vector<BoundaryCondition> bsc = new Vector<>();

    private OxygenTransportDat oxygendat;
    private double previousTimeStep;

    public OxygenTransportModel2D(FEDecomposition fe, OxygenTransportDat oxygendata) {
        fenet = fe;
        femodel = this;
        this.oxygendat = oxygendata;
        System.out.println("OxygenModel2D initalization");

        setNumberOfThreads(oxygendat.NumberOfThreads);

        readBoundCond();
        bsc.forEach((bcond) -> {
            initsc.add(fenet.getDOF(bcond.pointnumber));
        });

        // DOFs initialisieren
        initialDOFs();

        try {
            xf_os = new DataOutputStream(new FileOutputStream(oxygendat.xferg_name));
            // Setzen der Ergebnismaske
            TicadIO.write_xf(xf_os, this);
        } catch (FileNotFoundException e) {
            System.out.println("The file " + oxygendat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getTicadErgMask() {
        // Setzen der Ergebnismaske Salz-Konzentration
        return TicadIO.HRES_SALT;
    }

    private double initialOxygenConcentration(DOF dof, double time) {
        // System.out.println("initialOxygen");
        double sc = 0., R = 0., d;

        OxygenTransportModel2DData oxygendata = OxygenTransportModel2DData.extract(dof);
        if (oxygendata.bsc != null)
            sc = oxygendata.bsc.getValue(time);
        else {
            for (Enumeration<DOF> e = initsc.elements(); e.hasMoreElements();) {
                DOF ndof = e.nextElement();
                OxygenTransportModel2DData oxygen = OxygenTransportModel2DData.extract(ndof);
                if ((dof != ndof) & (oxygen.bsc != null)) {
                    d = dof.distance(ndof);
                    sc += oxygen.bsc.getValue(time) / d;
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

    /**
     * initialisiert die Salzkonzentration mit einem konstanten Wert
     * 
     * @param initalvalue
     * @return
     */
    public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value " + initalvalue + " mg/l");

        for (DOF dof : fenet.getDOFs()) {
            OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);
            oxygenmodeldata.oxygenConc = initalvalue;
        }
        return null;
    }

    /**
     * Read the start solution from file
     * 
     * @param oxygenerg file with simulation results
     * @param record    record in the file
     * @return the vector of start solution
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unused")
    public double[] initialSolutionFromTicadErgFile(String oxygenerg, int record) throws Exception {

        System.out.println("\t Read inital values from result file " + oxygenerg);
        // erstes Durchscannen
        File sysergFile = new File(oxygenerg);
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
            boolean H_gesetzt = ((ergMaske & TicadIO.HRES_H) == TicadIO.HRES_H);
            boolean SALT_gesetzt = ((ergMaske & TicadIO.HRES_SALT) == TicadIO.HRES_SALT);
            boolean EDDY_gesetzt = ((ergMaske & TicadIO.HRES_EDDY) == TicadIO.HRES_EDDY);
            boolean SHEAR_gesetzt = ((ergMaske & TicadIO.HRES_SHEAR) == TicadIO.HRES_SHEAR);
            boolean V_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_V_SCAL) == TicadIO.HRES_V_SCAL);
            boolean Q_SCAL_gesetzt = ((ergMaske & TicadIO.HRES_Q_SCAL) == TicadIO.HRES_Q_SCAL);
            boolean AH_gesetzt = ((ergMaske & TicadIO.HRES_AH) == TicadIO.HRES_AH);

            inStream.readInt();

            // Elemente, Rand und Knoten âÂºberlesen
            inStream.skip((anzElemente * 4L + anzr + 3L * anzKnoten) * 4L); // 4 Bytes je float und int

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4L) * record);

            float t = inStream.readFloat();
            for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
                DOF dof = fenet.getDOF(i);
                OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);

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
                    oxygenmodeldata.oxygenConc = inStream.readFloat();
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

    /**
     * the method initialOxygenConcentrationFromSysDat read the datas for skonc
     * from a sysdat-file named filename
     * 
     * @param filename name of the file to be open
     * @param time
     * @return
     * @throws java.lang.Exception
     */
    public double[] initialOxygenConcentrationFromSysDat(String filename, double time) throws Exception {
        this.time = time;
        int rand_knoten = 0;
        int gebiets_knoten = 0;
        int knoten_nr;

        double skonc;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading OxygenConcentration-File (in TiCAD-System.Dat-Format): " + filename);

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

            if (rand_knoten < 0 || rand_knoten > 10000000 || gebiets_knoten < 0 || gebiets_knoten > 10000000)
                throw new Exception("Fehler");

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
                    strto.nextToken();
                    strto.nextToken();
                    try {
                        skonc = Double.parseDouble(strto.nextToken());

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
                    OxygenTransportModel2DData.extract(dof).oxygenConc = skonc;

                    // if(p_count%1000==0) System.out.println(p_count);
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

    /**
     * the method initialOxygenConcentrationFromJanetBin read the datas for skonc
     * from a JanetBinary-file named filename
     * 
     * @param filename name of the file to be open
     * @param time
     * @return
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unused")
    public double[] initialOxygenConcentrationFromJanetBin(String filename, double time) throws Exception {
        int anzAttributes = 0;
        double skonc;

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

            System.out.println("\t Read Concentration-File from " + filename);

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
                    bin_in.fbinreaddouble();
                    bin_in.fbinreaddouble();
                    skonc = bin_in.fbinreaddouble();

                    // Plausibilitaetskontrolle
                    if (Double.isNaN(skonc) || skonc < 0.)
                        hasValidValues = false;

                    DOF dof = fenet.getDOF(nr);
                    OxygenTransportModel2DData.extract(dof).oxygenConc = skonc;

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
            System.out.println("Fehler beim Lesen der Salzkonzentration-Datei!");
            System.exit(0);
        }

        return null;
    }

    public double[] initialSolution(double time) {

        System.out.println("OxygenModel2D - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);
            oxygenmodeldata.oxygenConc = initialOxygenConcentration(dof, time);
        }
        initsc = null;
        return null;
    }

    /**
     * @deprecated
     * @param time
     * @param x
     * @return
     */
    @Deprecated
    @Override
    public double[] getRateofChange(double time, double x[]) {
        return null;
    } // end getRateofChange

    // ------------------------------------------------------------------------
    // ElementApproximation
    // ------------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element) {

        double timeStep = Double.POSITIVE_INFINITY;

        final Current2DElementData eleCurrentData = Current2DElementData.extract(element);
        if (eleCurrentData != null) {
            if (!eleCurrentData.isDry) {

                final FTriangle ele = (FTriangle) element;
                final double[][] koeffmat = ele.getkoeffmat();

                final double[] terms_Oxygen = new double[3];

                final double u_mean = eleCurrentData.u_mean;
                final double v_mean = eleCurrentData.v_mean;

                // compute element derivations
                // -------------------------------------------------------------------
                double doxygenconcdx = 0.;
                double doxygenconcdy = 0.;
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);

                    doxygenconcdx += oxygenmodeldata.oxygenConc * koeffmat[j][1];
                    doxygenconcdy += oxygenmodeldata.oxygenConc * koeffmat[j][2];
                } // end for

                final double current_mean = Function.norm(u_mean, v_mean);
                final double elementsize = eleCurrentData.elementsize;

                // dispersion
                double astx = eleCurrentData.astx + dispersionCoefficient;
                double asty = eleCurrentData.asty + dispersionCoefficient;

                final double nonZeroDeepestTotalDepth = Function.max(eleCurrentData.deepestTotalDepth, 1);

                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);
                    CurrentModel2DData currentmodeldata = CurrentModel2DData.extract(dof);

                    terms_Oxygen[j] = (currentmodeldata.u * doxygenconcdx + currentmodeldata.v * doxygenconcdy)
                            // turbulence term
                            - (astx * eleCurrentData.ddepthdx * doxygenconcdx
                                    + asty * eleCurrentData.ddepthdy * doxygenconcdy) / nonZeroDeepestTotalDepth
                                    * eleCurrentData.wlambda
                            + 3. * (koeffmat[j][1] * astx * doxygenconcdx + koeffmat[j][2] * asty * doxygenconcdy)
                                    * currentmodeldata.wlambda
                    // - 3. * source_dCdt // wird im Zeitschritt integriert
                    ;

                    if ((oxygenmodeldata.bsc == null) && (!oxygenmodeldata.extrapolate))
                        Koeq1_mean += 1. / 3. * (oxygenmodeldata.doxygenconcdt + terms_Oxygen[j])
                                * currentmodeldata.wlambda;
                }

                double tau_konc = 0.;
                if (current_mean > 1.E-5) {
                    tau_konc = 0.5 * elementsize / current_mean;
                    timeStep = tau_konc;
                }

                // Fehlerkorrektur durchfuehren
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);
                    final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
                    // Fehlerkorrektur durchfuehren
                    double result_SKonc_i = -tau_konc * (koeffmat[j][1] * u_mean + koeffmat[j][2] * v_mean) * Koeq1_mean
                            * ele.area;
                    if (result_SKonc_i > 0)
                        result_SKonc_i *= cmd.wlambda; // Konzentration will wachsen, Knoten aber Wattknoten

                    // Begin standart Galerkin-step
                    for (int l = 0; l < 3; l++) {
                        final double vorfak = ele.area * ((l == j) ? 1. / 6. : 1. / 12.);
                        final double gl = (l == j) ? 1.
                                : Function.min(CurrentModel2DData.extract(ele.getDOF(l)).wlambda,
                                        CurrentModel2DData.extract(ele.getDOF(l)).totaldepth
                                                / Function.max(CurrentModel2D.WATT, cmd.totaldepth));
                        result_SKonc_i -= vorfak * terms_Oxygen[l] * gl;
                    }
                    synchronized (oxygenmodeldata) {
                        oxygenmodeldata.rOxygenConc += result_SKonc_i;
                    }
                }
            }
        }
        return timeStep;
    } // end ElementApproximation

    // ------------------------------------------------------------------------
    // setBoundaryCondition
    // ------------------------------------------------------------------------
    @Override
    public void setBoundaryCondition(DOF dof, double t) {

        OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);

        /* prevention of negative concentration */
        if (oxygenmodeldata.oxygenConc <= 0.)
            oxygenmodeldata.oxygenConc = 0.;
        /* prevention of oxidentconcentration higher the oxygenSaturation */
        // Temperature
        final HeatTransportModel2DData temperatureModelData = HeatTransportModel2DData.extract(dof);
        if (temperatureModelData != null)
            oxygenmodeldata.waterTemperature = temperatureModelData.temperature;
        oxygenmodeldata.oxygenConc = Math.min(oxygenmodeldata.oxygenConc, oxygenmodeldata.oxygenSaturation());

        if (oxygenmodeldata.bsc != null) {
            oxygenmodeldata.oxygenConc = oxygenmodeldata.bsc.getValue(t);
            // oxygenmodeldata.doxygenconcdt = oxygenmodeldata.bsc.getDifferential(t);
        }

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        if (oxygenmodeldata.extrapolate && (cmd.totaldepth >= CurrentModel2D.WATT)) {
            OxygenTransportModel2DData tmpdata;
            for (FElement elem : dof.getFElements()) {
                for (int ll = 0; ll < 3; ll++) {
                    if (elem.getDOF(ll) == dof) {
                        for (int ii = 1; ii < 3; ii++) {
                            CurrentModel2DData tmpcmd = CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3));
                            if (tmpcmd.totaldepth > CurrentModel2D.WATT) {
                                tmpdata = OxygenTransportModel2DData.extract(elem.getDOF((ll + ii) % 3));
                                double dC = (oxygenmodeldata.oxygenConc - tmpdata.oxygenConc) / 10.;
                                if (tmpdata.extrapolate) {
                                    dC /= 100.;
                                }
                                double lambda = Math.min(1., tmpcmd.totaldepth / cmd.totaldepth);
                                synchronized (oxygenmodeldata) {
                                    oxygenmodeldata.oxygenConc -= dC * lambda;
                                }
                                // synchronized (tmpdata) {
                                // tmpdata.oxygenConc += dC / elem.getDOF((ll + ii) % 3).getNumberofFElements()
                                // * dof.getNumberofFElements();
                                // }
                            }
                        }
                    }
                    break;
                }
            }
        }

        // if ( cmd.totaldepth < CurrentModel2D.WATT) {
        // int anz = 0;
        // double meanC = 0.;
        // for (FElement elem : dof.getFElements()) {
        // for (int ll = 0; ll < 3; ll++) {
        // if (elem.getDOF(ll) == dof) {
        // for (int ii = 1; ii < 3; ii++) {
        // if (CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3)).totaldepth >
        // CurrentModel2D.WATT) {
        // meanC +=
        // OxygenTransportModel2DData.extract(elem.getDOF((ll+ii)%3)).oxygenConc;
        // anz++;
        // }
        // }
        // }
        // break;
        // }
        // }
        // if (anz > 0) {
        // synchronized (oxygenmodeldata){ oxygenmodeldata.oxygenConc = cmd.w1_lambda *
        // 0. + cmd.wlambda * meanC / anz;}
        // }
        // else {
        // synchronized (oxygenmodeldata){ oxygenmodeldata.oxygenConc = cmd.w1_lambda *
        // 0. + cmd.wlambda * oxygenmodeldata.oxygenConc;}
        // }
        // }

        oxygenmodeldata.sourceSink = 0.;
        if (cmd.totaldepth > CurrentModel2D.WATT) {
            if (cmd.sourceQ != null) {
                double area = 0.;
                FElement[] feles = dof.getFElements();
                for (FElement fele : feles) // muss nicht jedes mal neu berechet werden
                {
                    area += fele.getVolume();
                }
                double cquelle = 0.0;
                if (oxygenmodeldata.sourceQc != null) {
                    cquelle = oxygenmodeldata.sourceQc.getValue(time);
                }
                double c = oxygenmodeldata.oxygenConc;
                double source_dhdt = cmd.sourceQ.getValue(time) / area;
                oxygenmodeldata.sourceSink = (source_dhdt / (cmd.totaldepth)) * (cquelle - c);
            }
        } else {
            oxygenmodeldata.sourceSink = dispersionCoefficient * (0. - oxygenmodeldata.oxygenConc);
        }

        // Rechte Seite initialisieren
        oxygenmodeldata.rOxygenConc = 0.;
    }

    // ------------------------------------------------------------------------
    // genData
    // ------------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof) {
        OxygenTransportModel2DData data = new OxygenTransportModel2DData();
        int dofnumber = dof.number;
        Enumeration b = bsc.elements();
        while (b.hasMoreElements()) {
            BoundaryCondition bcond = (BoundaryCondition) b.nextElement();
            if (dofnumber == bcond.pointnumber) {
                data.bsc = bcond.function;
                bsc.removeElement(bcond);
            }
        }

        HeatTransportModel2DData htmdata = HeatTransportModel2DData.extract(dof);
        if (htmdata != null)
            data.waterTemperature = htmdata.temperature;
        else
            data.waterTemperature = this.oxygendat.waterTemperature;

        CurrentModel2DData current = CurrentModel2DData.extract(dof);

        // nicht vollstaendig spezifizierte Randbedingungen schaetzen
        data.extrapolate = ((data.bsc == null) && ((current.bu != null) && (current.bu instanceof ZeroFunction1d)
                && (current.bv != null) && (current.bv instanceof ZeroFunction1d) && (current.bh == null)));
        return data;
    }

    /**
     * @deprecated
     * @param erg
     * @param t
     */
    @Override
    @Deprecated
    public void write_erg_xf(double[] erg, double t) {
        System.out.println("deprecated method is called");
    }

    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            for (DOF dof : fenet.getDOFs()) {
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);

                xf_os.writeFloat((float) oxygenmodeldata.oxygenConc); // OxygenConcentration in mg/l
            }
            xf_os.flush();
        } catch (IOException e) {
            System.out.println(this.getClass() + "\n\ttime=" + time + "\n");
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public ModelData genData(FElement felement) {
        System.out.println("noch nicht genutzt");
        return null;
    }

    /**
     * Neue Einleseroutine readBoundCond
     * liest die spezifizierten Datensaetze (Randbedingungen) in der
     * boundary_condition_key_mask
     * aus entsprechenden Randwertedatei (oxygendat.rndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in oxygendat.rndwerteReader)
     */
    public final void readBoundCond() {

        String[] boundary_condition_key_mask = { BoundaryCondition.concentration_oxygen };

        try {
            bsc.addAll(Arrays.asList(oxygendat.rndwerteReader.readBoundaryConditions(boundary_condition_key_mask)));
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
            OxygenTransportModel2DData oxygenmodeldata = OxygenTransportModel2DData.extract(dof);

            oxygenmodeldata.rOxygenConc /= dof.lumpedMass;

            // Variable Adams-Bashforth 2. Ordnung
            double rOxygen = beta0 * oxygenmodeldata.rOxygenConc + beta1 * oxygenmodeldata.doxygenconcdt;

            oxygenmodeldata.doxygenconcdt = oxygenmodeldata.rOxygenConc;

            rOxygen += oxygenmodeldata.sourceSink; // Sauerstoffquellen und -senken bruecksichtigen

            oxygenmodeldata.oxygenConc += dt * rOxygen;
            /* prevention of negative concentration */
            if (oxygenmodeldata.oxygenConc < 0.)
                oxygenmodeldata.oxygenConc = 0.;

            boolean rIsNaN = Double.isNaN(rOxygen);
            if (rIsNaN)
                System.out.println("Oxygentransport is NaN bei " + dof.number + " doxygenconcdt=" + rOxygen);
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