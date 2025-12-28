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

import de.smile.marina.MarinaXML;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.fem.model.meteorology.MeteorologyData2D;
import de.smile.marina.io.FileIO;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.*;
import java.util.*;

/** this TimeDependentFEApproximation describe heat-transport model for depth integrated simulations
 * @version 4.7.0
 * @author Peter Milbradt
 */
public class HeatTransportModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel {

    private HeatTransportModel2DData[] dof_data = null;
    private DataOutputStream xf_os = null;
    
    private Vector<DOF> initsc = new Vector<>();
    private Vector<BoundaryCondition> bsc = new Vector<>();
    private HeatTransportDat temperaturedat;
    static final double SpecificHeatCapacity = 4183.;   //[J kg-1 K-1]
    static final double astTemp = 1.4E-6; // Austausch zwischen Luft und wasser
    static final double kw = 0.597; // Waermeleitfaehigkeit [W/mK]
    private double airTemperature = Double.NaN;
    private double bottomTemperature = 10.;
    private double previousTimeStep;

    public HeatTransportModel2D(FEDecomposition fe, HeatTransportDat temperaturedat) {
        System.out.println("HeatTransportModel2D initalization");
        fenet = fe;
        femodel = this;
        this.temperaturedat = temperaturedat;
        airTemperature = temperaturedat.airTemperature;

        dof_data = new HeatTransportModel2DData[fenet.getNumberofDOFs()];

        setNumberOfThreads(temperaturedat.NumberOfThreads);

        if (temperaturedat.temperaturerndwerte_name != null) {
            readBoundCond();
        }
        BoundaryCondition bcond;
        Enumeration<BoundaryCondition> be = bsc.elements();
        while (be.hasMoreElements()) {
            bcond = be.nextElement();
            initsc.addElement(fenet.getDOF(bcond.pointnumber));
        }

        // DOFs initialisieren
        initialDOFs();

        try {
            xf_os = new DataOutputStream(new FileOutputStream(temperaturedat.xferg_name));
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ temperaturedat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }

    @Override
    public int getTicadErgMask() {
        // Setzen der Ergebnismaske Temperatur
        return TicadIO.HRES_H;
    }

    private double initialTemperature(DOF dof, double time) {
        double sc = 0., R = 0., d;
        HeatTransportModel2DData heattransportmodel2Ddata = HeatTransportModel2DData.extract(dof);
        if (heattransportmodel2Ddata.bc != null) {
            sc = heattransportmodel2Ddata.bc.getValue(time);
        } else {
            for (Enumeration<DOF> e = initsc.elements(); e.hasMoreElements();) {
                DOF ndof = e.nextElement();
                HeatTransportModel2DData temperature = HeatTransportModel2DData.extract(ndof);
                if ((dof != ndof) & (temperature.bc != null)) {
                    d = dof.distance(ndof);
                    sc += temperature.bc.getValue(time) / d;
                    R += 1. / d;
                }
            }
            if (R != 0.) {
                sc /= R;
            } else {
                sc = bottomTemperature;
            }
        }
        return sc;
    }

    public double[] initialSolution(double time) {
        this.time = time;

        System.out.println("\tinterpolate inital values from boundary conditions");
        for (DOF dof : fenet.getDOFs()) {
            HeatTransportModel2DData heattransportmodel2Ddata = HeatTransportModel2DData.extract(dof);
            heattransportmodel2Ddata.temperature = initialTemperature(dof, time);
        }
        initsc = null;
        return null;
    }

    /** Read the start solution from file
     * @param heaterg file with simulation results
     * @param record record in the file
     * @return the vector of start solution
     */
    public double[] initialSolutionFromTicadErgFile(String heaterg, int record) throws Exception {

        System.out.println("\tRead inital values from result file " + heaterg);
        //erstes Durchscannen
        File sysergFile = new File(heaterg);
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
                System.out.println("Netz" + fenet.getNumberofDOFs() + " Startdatei " + anzKnoten);
                System.exit(1);
            }
            int anzr = inStream.readInt();
            int anzElemente = inStream.readInt();

            //Ueberlesen folgende Zeilen
            inStream.skip(9 * 4);

            //Ergebnismaske lesen und auswerten
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

            //Elemente, Rand und Knoten überlesen
            inStream.skip((anzElemente * 4L + anzr + 3L * anzKnoten) * 4L); //4 Bytes je float und int

            // bis zum record-Satz springen
            inStream.skip((4L + anzKnoten * anzWerte * 4L) * record);

            float t = inStream.readFloat();
            for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
                DOF dof = fenet.getDOF(i);
                HeatTransportModel2DData heatmodeldata = HeatTransportModel2DData.extract(dof);

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
                    heatmodeldata.temperature = inStream.readFloat();
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
        return null;
    }

    /** initialisiert die Temperaturverteilung mit einem konstanten Wert
     * @param initalvalue initial temperature
     * @return 
     */
    public double[] constantInitialSolution(double initalvalue) {
        System.out.println("\tSet initial value " + initalvalue);

        for (int i = 0; i < fenet.getNumberofDOFs(); i++) {
            dof_data[(fenet.getDOF(i)).number].temperature = initalvalue;
        }

        return null;
    }

    /** the method  initialTemperatureFromSysDat read the datas for temperature 
     *  from a sysdat-file named filename
     *  @param filename  name of the file to be open
     * @param time
     * @return 
     * @throws java.lang.Exception */
    public double[] initialTemperatureFromSysDat(String filename, double time) throws Exception {
        this.time = time;
        int rand_knoten = 0;
        int gebiets_knoten = 0;
        int knoten_nr;


        double temperature;

        String line;

        try {
            FileIO systemfile = new FileIO();
            systemfile.fopen(filename, FileIO.input, 'C');

            System.out.println("\tReading Temperature-File (in TiCAD-System.Dat-Format): " + filename);

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
                    strto.nextToken();
                    strto.nextToken();
                    try {
                        temperature = Double.parseDouble(strto.nextToken());

                    } catch (Exception ex) {
                        temperature = Double.NaN;
                    }
                    // Plausibilitaetskontrolle
                    if (Double.isNaN(temperature) || temperature <= -273. || temperature >= 100.) {


                        System.out.println("");

                        System.out.println("********************************       ERROR         ***********************************");
                        System.out.println("Invalid temperature-value (temp=NaN or temp<-273 or temp>100) in Temperature-File: <" + filename + "> node number <" + p_count + ">");
                        System.out.println("To correct this problem ensure that node nr <" + p_count + "> has a correct floating point ");
                        System.out.println("Temperature  value");
                        System.out.println("*****************************************************************************************");
                        System.out.println("");
                        System.exit(0);
                    }
                    DOF dof = fenet.getDOF(knoten_nr);
                    HeatTransportModel2DData.extract(dof).temperature = temperature;

                    //if(p_count%1000==0) System.out.println(p_count);
                    p_count++;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\t\tcannot open file: "+filename);
            System.exit(0);
        }

        return null;

    }

    /** the method initialTemperatureFromJanetBin read the datas for temperature
     *  from  a JanetBinary-file named filename
     *  @param filename  name of the file to be open
     * @param time
     * @return 
     * @throws java.lang.Exception */
    public double[] initialTemperatureFromJanetBin(String filename, double time) throws Exception {
        int anzAttributes = 0;
        double temperature;

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
                throw new Exception("Deprecated version of Janet-Binary-Format, version found: " + version + ", current version: 1.8");
            }

            if (version < 1.79) {
                read_status_byte = true;
            }

            System.out.println("\t Read Temperature-File from " + filename);

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

                    // x,y,s lesen
                    bin_in.fbinreaddouble();
                    bin_in.fbinreaddouble();
                    temperature = bin_in.fbinreaddouble();

                    // Plausibilitaetskontrolle
                    if (Double.isNaN(temperature) || temperature <= -273. || temperature >= 100.) {
                        hasValidValues = false;
                    }

                    DOF dof = fenet.getDOF(nr);
                    HeatTransportModel2DData.extract(dof).temperature = temperature;

                    // Status-Flag lesen
                    if (writePointStatus) {
                        bin_in.fbinreadshort();
                    }

                }

                // Abbruch, wenn Netz nicht ok!
                if (!hasValidValues) {
                    System.out.println("***                     WARNUNG                       ***");
                    System.out.println("***   Ueberpruefen Sie die Temperatur-Werte des       ***");
                    System.out.println("***   Konzentrationnetzes. Das verwendetet Netz hat   ***");
                    System.out.println("***   Knoten mit  nicht definierten  Temperaturen!    ***");
                    System.out.println("***                                                   ***");
                    System.out.println("***   Die Simulation wird nicht fortgesetzt           ***");
                    System.exit(0);
                }

            } else {
                System.out.println("system und temperatur.jbf different number of nodes");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\t\tcannot open file: "+filename);
            System.exit(0);
        }

        return null;
    }


    @Deprecated
    @Override
    public double[] getRateofChange(double time, double x[]) {
        return null;
    } // end getRateofChange

    //------------------------------------------------------------------------
    // ElementApproximation
    //------------------------------------------------------------------------
    @Override
    public double ElementApproximation(FElement element) {

        double timeStep=Double.POSITIVE_INFINITY;

        FTriangle ele = (FTriangle) element;

        final double[][] koeffmat = ele.getkoeffmat();
        double dTemperaturedx = 0.;
        double dTemperaturedy = 0.;
        for (int j = 0; j < 3; j++) {
            final HeatTransportModel2DData heattransportmodel2Ddata = dof_data[ele.getDOF(j).number];
            dTemperaturedx += heattransportmodel2Ddata.temperature * koeffmat[j][1];
            dTemperaturedy += heattransportmodel2Ddata.temperature * koeffmat[j][2];
        }

        Current2DElementData eleCurrentData = Current2DElementData.extract(element);
        if (eleCurrentData != null) {
            if (!eleCurrentData.isDry) { // Peter 15.05.2013

                double[] terms_T = new double[3];

                double u_mean = eleCurrentData.u_mean;
                double v_mean = eleCurrentData.v_mean;

                final double current_mean = norm(u_mean, v_mean);
                final double elementsize = eleCurrentData.elementsize;

                // dispersion
                final double astx = eleCurrentData.astx * eleCurrentData.wlambda + kw;
                final double asty = eleCurrentData.asty * eleCurrentData.wlambda + kw;
                
                double nonZeroDeepestTotalDepth = Math.max(eleCurrentData.deepestTotalDepth,1);
                
                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    DOF dof = ele.getDOF(j);
                    HeatTransportModel2DData heattransportmodel2Ddata = dof_data[dof.number];
                    CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
                    
                    terms_T[j] = (cmd.u * dTemperaturedx + cmd.v * dTemperaturedy)
                                    - heattransportmodel2Ddata.sourceSink
                                        // turbulence term
                                    - (astx * eleCurrentData.ddepthdx * dTemperaturedx + asty * eleCurrentData.ddepthdy * dTemperaturedy)/nonZeroDeepestTotalDepth * eleCurrentData.wlambda
                                    + 3. * (koeffmat[j][1] * astx * dTemperaturedx + koeffmat[j][2] * asty * dTemperaturedy)
                            ;

                    if ((heattransportmodel2Ddata.bc == null))// && !heattransportmodel2Ddata.extrapolate)
                        Koeq1_mean += 1. / 3. * (heattransportmodel2Ddata.temperaturedt + terms_T[j]) * cmd.wlambda;
                }

                double tau_konc = 0.;
                if (current_mean > 1.E-5) {
                    tau_konc = 0.5 * elementsize / current_mean;
                    timeStep=tau_konc;
                }

                
                for (int j = 0; j < 3; j++) {
                    HeatTransportModel2DData heattransportmodel2Ddata = dof_data[ele.getDOF(j).number];
                    CurrentModel2DData  cmd  = CurrentModel2DData.extract(ele.getDOF(j));
                    synchronized (heattransportmodel2Ddata) {
                        // Fehlerkorrektur durchfuehren
                        heattransportmodel2Ddata.rtemperature -= tau_konc * (koeffmat[j][1] * u_mean * Koeq1_mean + koeffmat[j][2] * v_mean * Koeq1_mean) * cmd.wlambda * ele.area;
                    }

                    for (int l = 0; l < 3; l++) {
                    final double vorfak =  ele.area * ((l == j) ? 1. / 6. : 1. / 12.);
                        final double gl = (l == j) ? 1. :  Math.min(CurrentModel2DData.extract(ele.getDOF(l)).wlambda, CurrentModel2DData.extract(ele.getDOF(l)).totaldepth/Math.max(CurrentModel2D.WATT,cmd.totaldepth)); // Peter 21.01.2016
                        synchronized (heattransportmodel2Ddata) {
                            heattransportmodel2Ddata.rtemperature -= vorfak * terms_T[l]*gl;
                        }
                    }
                }
            } else {

                for (int j = 0; j < 3; j++) {
                    HeatTransportModel2DData heat2Ddata = dof_data[ele.getDOF(j).number];
                    synchronized (heat2Ddata) {
                        heat2Ddata.rtemperature += 1. / 3. * heat2Ddata.sourceSink
                                - (koeffmat[j][1] * kw * dTemperaturedx + koeffmat[j][2] * kw * dTemperaturedy)
                                ;
                    }
                }
            }
        }
        return timeStep;
    } // end ElementApproximation

    //------------------------------------------------------------------------
    // setBoundaryCondition
    //------------------------------------------------------------------------
    @Override
    public void setBoundaryCondition(DOF dof, double t) {

        HeatTransportModel2DData heattransportmodel2Ddata = dof_data[dof.number];

        /* prevention of too smale and too hight temperature */
        if (heattransportmodel2Ddata.temperature < 0.) {
            heattransportmodel2Ddata.temperature = 0.;
        }
        if (heattransportmodel2Ddata.temperature > 100.) {
            heattransportmodel2Ddata.temperature = 100.;
        }

        if (heattransportmodel2Ddata.bc != null) {
            heattransportmodel2Ddata.temperature = heattransportmodel2Ddata.bc.getValue(t);
//            heattransportmodel2Ddata.temperaturedt = heattransportmodel2Ddata.bc.getDifferential(t);
        }

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
//        if (heattransportmodel2Ddata.extrapolate && (cmd.totaldepth >= CurrentModel2D.WATT)) {
//            HeatTransportModel2DData tmpdata;
//            FElement[] felem = dof.getFElements();
//            for (int j = 0; j < felem.length; j++) {
//                FElement elem = felem[j];
//                for (int ll = 0; ll < 3; ll++) {
//                    if (elem.getDOF(ll) == dof) {
//                        for (int ii = 1; ii < 3; ii++) {
//                            CurrentModel2DData tmpcmd = CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3));
//                            if (tmpcmd.totaldepth > CurrentModel2D.WATT) {
//                                tmpdata = dof_data[elem.getDOF((ll + ii) % 3).number];
//                                double dt = (heattransportmodel2Ddata.temperature - tmpdata.temperature) / 10.;
//                                if (tmpdata.extrapolate) {
//                                    dt /= 100.;
//                                }
//                                double lambda = Math.min(1., tmpcmd.totaldepth/cmd.totaldepth);
//                                synchronized (heattransportmodel2Ddata) {
//                                    heattransportmodel2Ddata.temperature -= dt*lambda;
//                                }
//                                synchronized (tmpdata) {
//                                    tmpdata.temperature += dt / elem.getDOF((ll + ii) % 3).getNumberofFElements() * dof.getNumberofFElements();
//                                }
//                            }
//                        }
//                    }
//                    break;
//                }
//            }
//        }

//        if (cmd.totaldepth < CurrentModel2D.WATT) { // Peter 28.04.2010
//            int anz = 0;
//            double meanTemperature = 0.;
//            FElement[] felem = dof.getFElements();
//            for (int j = 0; j < felem.length; j++) {
//                FElement elem = felem[j];
//                for (int ll = 0; ll < 3; ll++) {
//                    if (elem.getDOF(ll) == dof) {
//                        for (int ii = 1; ii < 3; ii++) {
//                            if (CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3)).totaldepth > CurrentModel2D.WATT) {
//                                meanTemperature += dof_data[elem.getDOF((ll + ii) % 3).number].temperature;
//                                anz++;
//                            }
//                        }
//                    }
//                    break;
//                }
//            }
//            double aTemp = bottomTemperature;
//            MeteorologyData2D meteorologyData = MeteorologyData2D.extract(dof);
//            if (meteorologyData != null) {
//                aTemp = meteorologyData.temperature;
//            } else if (!Double.isNaN(airTemperature)) {
//                aTemp = airTemperature;
//            }
//            if (anz > 0) {
//                heattransportmodel2Ddata.temperature = cmd.w1_lambda * (1 * aTemp + 0. * bottomTemperature) + cmd.wlambda * meanTemperature / anz;
//            } else {
//                heattransportmodel2Ddata.temperature = cmd.w1_lambda * (1 * aTemp + 0. * bottomTemperature) + cmd.wlambda * heattransportmodel2Ddata.temperature;
//            }
//        }  // Peter 09.08.2010

        // Rechte Seite initialisieren  //
        heattransportmodel2Ddata.rtemperature = 0.;

        // bestimmen der Quellen und Senken
        MeteorologyData2D meteorologyData = MeteorologyData2D.extract(dof);
        if (meteorologyData != null) {
            heattransportmodel2Ddata.sourceSink = astTemp * (meteorologyData.temperature - heattransportmodel2Ddata.temperature) / Function.max(cmd.totaldepth, CurrentModel2D.WATT) - meteorologyData.insolation / (SpecificHeatCapacity * Function.max(cmd.totaldepth, CurrentModel2D.WATT) * PhysicalParameters.RHO_WATER);
        } else {
            if (!Double.isNaN(airTemperature)) {
                heattransportmodel2Ddata.sourceSink = astTemp * (airTemperature - heattransportmodel2Ddata.temperature) / Function.max(cmd.totaldepth, CurrentModel2D.WATT);
            } else {
                heattransportmodel2Ddata.sourceSink = astTemp * (bottomTemperature - heattransportmodel2Ddata.temperature) / Function.max(cmd.totaldepth, CurrentModel2D.WATT) * cmd.w1_lambda; // Peter 15.05.2013
            }
        }
    }

    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    @Override
    public ModelData genData(DOF dof) {
        HeatTransportModel2DData data = new HeatTransportModel2DData();
        int dofnumber = (int) dof.number;
        dof_data[dofnumber] = data;

        Enumeration b = bsc.elements();
        while (b.hasMoreElements()) {
            BoundaryCondition bcond = (BoundaryCondition) b.nextElement();
            if (dofnumber == bcond.pointnumber) {
                data.bc = bcond.function;
                bsc.removeElement(bcond);
            }
        }

        CurrentModel2DData currentmodeldata = CurrentModel2DData.extract(dof);

        // nicht vollstaendig spezifizierte Randbedingungen schaetzen
        data.extrapolate = ((data.bc == null) && ((currentmodeldata.bh != null) || (currentmodeldata.bu != null) || (currentmodeldata.bv != null) || (currentmodeldata.bqx != null) || (currentmodeldata.bqy != null) || (currentmodeldata.bQx != null) || (currentmodeldata.bQy != null)));

        return data;
    }

    @Deprecated
    @Override
    public void write_erg_xf(double[] erg, double t) {
        System.out.println("deprecated method is called");
    }

    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            DOF[] dof = fenet.getDOFs();
            for (int j = 0; j < dof.length; j++) {

                setBoundaryCondition(dof[j], time);  // Peter 12.03.10

                double erg1 = dof_data[j].temperature;

                if (MarinaXML.release) { // Peter 01.05.2010
                    CurrentModel2DData cmd = CurrentModel2DData.extract(dof[j]);
                    if (cmd.totaldepth > CurrentModel2D.WATT) {
                        int anz = 0;
                        double meanTemperature = 0.;
                        FElement[] felem = dof[j].getFElements();
                        for (FElement elem : felem) {
                            for (int ll = 0; ll < 3; ll++) {
                                if (elem.getDOF(ll) == dof[j]) {
                                    for (int ii = 1; ii < 3; ii++) {
                                        if (CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3)).totaldepth > CurrentModel2D.WATT) {
                                            meanTemperature += dof_data[elem.getDOF((ll + ii) % 3).number].temperature;
                                            anz++;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        if (anz > 0) {
                            erg1 = (2. * erg1 + meanTemperature / anz) / 3.;
                        }
                    }
                }

                xf_os.writeFloat((float) erg1);
            }
            xf_os.flush();
        } catch (Exception e) {
            System.out.println(this.getClass()+"\n\ttime="+time+"\n");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /** compute the eklidian vectornorm
     * @param x first component of the vector
     * @param y the second component of the vector
     * @return norm of the vector (x,y)
     */
    // austauschen durch BIjava.Math.Function.L2NormD2
    private double norm(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    @Override
    public ModelData genData(FElement felement) {
        System.out.println("noch nicht genutzt");
        return null;
    }

    /** Neue Einleseroutine readBoundCond 
     * liest die spezifizierten Datensaetze (Randbedingungen) in der boundary_condition_key_mask 
     * aus entsprechenden Randwertedatei (temperaturedat.temperaturerndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in temperaturedat.rndwerteReader)
     */
    public final void readBoundCond() {
        
        String[] boundary_condition_key_mask = new String[1];
        boundary_condition_key_mask[0] = BoundaryCondition.water_temperature;
        
        try {            
            for (BoundaryCondition bc : temperaturedat.rndwerteReader.readBoundaryConditions(boundary_condition_key_mask)) {
                if (bc.boundary_condition_key.equals(BoundaryCondition.water_temperature)) {
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

        maxTimeStep = 10000.;

        // Elementloop
        performElementLoop();
        
        // Berechne omega und die Koeffizienten fuer Variable Adams-Bashforth 2. Ordnung einmal vor dem parallelen Stream
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
            HeatTransportModel2DData smd = dof_data[j];

            smd.rtemperature /= dof.lumpedMass;

            // Variable Adams-Bashforth 2. Ordnung
            double rt = beta0 * smd.rtemperature + beta1 * smd.temperaturedt;

            smd.temperaturedt = smd.rtemperature;

            smd.temperature += dt * rt;

            boolean rIsNaN = Double.isNaN(rt);
            if (rIsNaN) {
                System.out.println("HeatTransportModel is NaN bei " + j + " dtempdt=" + rt);
            }
            resultIsNaN |= rIsNaN;
        });

        this.time += dt;

        if (resultIsNaN) {
            System.out.println("Time=" + this.time + " and timestep is" + dt);
            write_erg_xf();
            try {
                xf_os.close();
            } catch (Exception e) {
            }
            System.exit(0);
        }
    }
}




