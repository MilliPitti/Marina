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
package de.smile.marina.fem.model.meteorology;

import de.smile.marina.fem.*;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import javax.vecmath.Point3d;

/**
 *
 * @author milbradt
 * @version 1.9.3
 */
public class SysErgInterpolatedMeteorologicalModel2D extends MeteorologicalModel2D {

    FEMeteorologicalModel2D fe0 = null;
    FEMeteorologicalModel2D fe1 = null;

    String fileName = null;

    /**
     * Creates a new instance of SysErgInterpolatedMeteorologicalModel2D
     * 
     * @param fe
     * @param meteoergPath
     * @param dat
     * @throws java.io.IOException
     */
    public SysErgInterpolatedMeteorologicalModel2D(FEDecomposition fe, String meteoergPath, MeteorologicalDat dat)
            throws IOException {
        super(fe);
        System.out.println("MeteorologicalModel2D initialization");
        femodel = this;
        this.fileName = meteoergPath;
        System.out.println("\tOpen meteorology data file " + meteoergPath);

        this.fe0 = new FEMeteorologicalModel2D(fileName, 0);

        // DOFs initialisieren
        initialDOFs();

        try {
            xf_os = new DataOutputStream(new FileOutputStream(dat.xferg_name));
            // Setzen der Ergebnismaske
            TicadIO.write_xf(xf_os, this);
        } catch (FileNotFoundException e) {
            System.out.println("The file " + dat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public ModelData genData(DOF dof) {
        return new FEInterpolatedMeteorologyData2D(dof);
    }

    @Override
    public void timeStep(double dt) {

        this.time += dt;

        if (fe0.time > time)
            return;

        if (fe1 == null)
            try {
                fe1 = new FEMeteorologicalModel2D(fileName, fe0.record + 1);
                // System.out.println("updaten");
                for (DOF dof : fenet.getDOFs()) {
                    FEInterpolatedMeteorologyData2D md = (FEInterpolatedMeteorologyData2D) MeteorologyData2D
                            .extract(dof);
                    md.update(fe0, fe1);
                }
            } catch (IOException ex) {
                Logger.getLogger(SysErgInterpolatedMeteorologicalModel2D.class.getName()).log(Level.SEVERE, null, ex);
            }
        if (fe1 == null)
            return;
        // System.out.println("time = "+this.time+", time1="+this.fe1.time);
        while (fe1.time < time) {
            fe0 = fe1;
            try {
                fe1 = new FEMeteorologicalModel2D(fileName, fe0.record + 1);
                // System.out.println("updaten");
                for (DOF dof : fenet.getDOFs()) {
                    FEInterpolatedMeteorologyData2D md = (FEInterpolatedMeteorologyData2D) MeteorologyData2D
                            .extract(dof);
                    md.update(fe0, fe1);
                }
            } catch (IOException ex) {
                Logger.getLogger(SysErgInterpolatedMeteorologicalModel2D.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // System.out.println("time0="+this.fe0.time+", time = "+this.time+",
        // time1="+this.fe1.time);

        double lambda = (this.time - fe0.time) / (fe1.time - fe0.time);

        for (DOF dof : fenet.getDOFs()) {
            FEInterpolatedMeteorologyData2D md = (FEInterpolatedMeteorologyData2D) MeteorologyData2D.extract(dof);
            md.insolation = (1 - lambda) * md.md0.insolation + lambda * md.md1.insolation;
            md.temperature = (1 - lambda) * md.md0.temperature + lambda * md.md1.temperature;
            md.windx = (1 - lambda) * md.md0.windx + lambda * md.md1.windx;
            md.windy = (1 - lambda) * md.md0.windy + lambda * md.md1.windy;
            md.windspeed = Function.norm(md.windx, md.windy);
            md.pressure = (1 - lambda) * md.md0.pressure + lambda * md.md1.pressure;
            md.temperature = C1 * Math.cos(w1 * time - Math.PI) + x1; // Jahresschwankung
            md.temperature += C2 * Math.cos(w2 * time - Math.PI) + x2; // Tagesschwankung
            // Bestimme Lichtintensitaet in MJm-2day-1
            double I_n = C3 * Math.cos(w3 * time - Math.PI) + x3;
            // Umrechnung in Wm-2<==>Jm-2s-1
            I_n *= 1000000. / (24. * 3600.);
            // Bestimmung der Dauer der Photoperiode bezogen auf 1 Tag
            double n = time / (3600. * 24.) + 1.;
            n -= 60; // auf 1. Maerz beziehen
            // Umrechnung Tag -> Winkel bezogen auf's ganze Jahr'
            double y = 2 * Math.PI * (n - 21) / 365;
            // Berechne Deklination der Sonne
            double deklination = 0.38092 - 0.76996 * Math.cos(y) + 23.265 * Math.sin(y)
                    + 0.36958 * Math.cos(2. * y) + 0.10868 * Math.sin(2. * y)
                    + 0.01834 * Math.cos(3. * y) - 0.00392 * Math.sin(3. * y)
                    - 0.00392 * Math.cos(4. * y) - 0.00072 * Math.sin(4. * y)
                    - 0.00051 * Math.cos(5. * y) + 0.0025 * Math.sin(5. * y);
            double varphi = 55.; // Noerdliche Breite
            varphi = varphi / 180 * Math.PI;
            // Bestimme Photoperiode bezogen auf ganzen Tag [0...1]
            double p = (2 * Math.acos(-Math.tan(varphi) * Math.tan(deklination / 180. * Math.PI))) / (Math.PI * 2.);
            double t = time % (24. * 3600.);
            t /= (24. * 3600.);
            if ((t < 0.5 - p / 2.) || (t > 0.5 + p / 2.)) {
                md.insolation = 0.;
            } else {
                md.insolation = I_n / p * (1. + Math.cos((t - 0.5) * Math.PI * 2. / p));
            }
        }
    }

    @Override
    public double[] initialSolution(double StartTime) {
        this.time = StartTime;

        timeStep(0.);

        return null;
    }

    class FEInterpolatedMeteorologyData2D extends MeteorologyData2D {

        DOF myDof;
        int elementNumber = -1;
        double time0;
        MeteorologyData2D md0;
        double time1;
        MeteorologyData2D md1;

        FEInterpolatedMeteorologyData2D(DOF dof) {
            super();
            this.myDof = dof;

            DOF ndof;

            FElement elem = fe0.fenet.getElement(dof);

            if (elem != null) {
                elementNumber = elem.number;
                ndof = fe0.interpolate(elem, dof);
            } else { // Knoten liegt auszerhalb des Windfeldes !
                ndof = new DOF(-1, myDof.x, myDof.y, myDof.z);
                MeteorologyData2D md = new MeteorologyData2D();
                ndof.addModelData(md);
            }

            md0 = MeteorologyData2D.extract(ndof);// (fe0.getValue(dof));
            time0 = fe0.time;
            md1 = MeteorologyData2D.extract(ndof);// (fe0.getValue(dof));
            time1 = fe0.time;
            this.windx = md0.windx;
            this.windy = md0.windy;
            this.windspeed = Function.norm(this.windx, this.windy);
            this.pressure = md0.pressure;
            this.insolation = md0.insolation;
            this.temperature = md0.temperature;
        }

        void update(FEMeteorologicalModel2D fe0, FEMeteorologicalModel2D fe1) {
            DOF ndof;
            if (elementNumber > -1) {
                FElement elem = fe0.fenet.getFElement(elementNumber);
                ndof = fe0.interpolate(elem, myDof);
            } else { // Knoten liegt auszerhalb des Windfeldes !
                ndof = new DOF(-1, myDof.x, myDof.y, myDof.z);
                MeteorologyData2D md = new MeteorologyData2D();
                ndof.addModelData(md);
            }
            md0 = MeteorologyData2D.extract(ndof);// (fe0.getValue(myDof));
            time0 = fe0.time;

            DOF ndof2;
            if (elementNumber > -1) {
                FElement elem = fe1.fenet.getFElement(elementNumber);
                ndof2 = fe1.interpolate(elem, myDof);
            } else { // Knoten liegt auszerhalb des Windfeldes !
                ndof2 = new DOF(-1, myDof.x, myDof.y, myDof.z);
                MeteorologyData2D md = new MeteorologyData2D();
                ndof2.addModelData(md);
            }
            md1 = MeteorologyData2D.extract(ndof2);// (fe1.getValue(myDof));
            time1 = fe1.time;
        }
    }

    class FEMeteorologicalModel2D extends FEInterpolation {

        double time = 0;
        int record = 0;

        @SuppressWarnings("unused")
        FEMeteorologicalModel2D(String meteoergPath, int record) throws IOException {

            File sysergFile = new File(meteoergPath);
            try (FileInputStream stream = new FileInputStream(sysergFile);
                    DataInputStream inStream = new DataInputStream(stream)) {

                // Kommentar lesen, bis ASCII-Zeichen 7 kommt
                StringBuilder description = new StringBuilder();
                char c;
                do {
                    c = (char) inStream.readByte();
                    description.append(c);
                } while (c != 7);
                // Ende Kommentar
                // sind Offset-Koordianten gespeichert
                String model_component = "";
                double offset_x = 0.0, offset_y = 0.0;
                try {
                    offset_x = TicadIO.getStoredIntegerValue(description.toString(), "OffSetX");
                    offset_y = TicadIO.getStoredIntegerValue(description.toString(), "OffSetY");
                    // System.out.println("OffSetX " + offset_x);
                    // System.out.println("OffSetY " + offset_y);
                    model_component = TicadIO.getStoredModelComponent(description.toString());
                    // System.out.println("ModelComponent " + model_component);
                } catch (Exception ex) {
                }

                // Anzahl Elemente, Knoten und Rand lesen
                int anzKnoten = inStream.readInt();
                int anzr = inStream.readInt();
                int anzElemente = inStream.readInt();
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

                DOF[] dof = new DOF[anzKnoten];
                for (int i = 0; i < dof.length; i++)
                    dof[i] = new DOF(i, 0., 0., 0.);

                FTriangle[] elem = new FTriangle[anzElemente];

                for (int i = 0; i < anzElemente; i++) {

                    DOF dof0 = dof[inStream.readInt()];
                    DOF dof1 = dof[inStream.readInt()];
                    DOF dof2 = dof[inStream.readInt()];

                    elem[i] = new FTriangle(dof0, dof1, dof2);
                    elem[i].number = i;

                    inStream.readInt(); // Kennung
                }

                // Schleife Nummern der Randknoten lesen
                for (int i = 0; i < anzr; i++) {
                    inStream.readInt(); // Knotennummer ueberlesen
                }

                // Schleife Knoten lesen 3 mal float
                for (int i = 0; i < anzKnoten; i++) {
                    DOF p = dof[i];
                    p.x = inStream.readFloat() + offset_x;
                    p.y = inStream.readFloat() + offset_y;
                    p.z = inStream.readFloat();
                }

                fenet = new FTriangleMesh(elem, dof);

                // bis zum record-Satz springen
                inStream.skip((4L + anzKnoten * anzWerte * 4) * record);
                this.record = record;
                time = inStream.readFloat();
                for (int i = 0; i < fenet.getNumberofDOFs(); i++) {

                    MeteorologyData2D data = new MeteorologyData2D();
                    dof[i].addModelData(data);

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
                        data.windx = inStream.readFloat();
                        data.windy = inStream.readFloat();
                        data.windspeed = Function.norm(data.windx, data.windy);
                    }
                    if (Q_gesetzt) {
                        inStream.skip(8);
                    }
                    if (H_gesetzt) {
                        data.pressure = inStream.readFloat();
                    }
                    if (SALT_gesetzt) {
                        data.insolation = inStream.readFloat();
                    }
                    if (EDDY_gesetzt) {
                        data.temperature = inStream.readFloat();
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
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SysErgInterpolatedMeteorologicalModel2D.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public ModelData genData(DOF dof) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void ElementInterpolation(FElement ele) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public DOF getValue(DOF dof) {

            DOF rdof = new DOF(-1, dof.x, dof.y, dof.z);
            MeteorologyData2D md = new MeteorologyData2D();
            rdof.addModelData(md);
            // for (FElement elem : fenet.getFElements()) {
            FElement elem = fenet.getElement(rdof);
            if (elem == null)
                return rdof;
            // if (elem.contains(rdof)) {
            double[] lambda = elem.getNaturefromCart(rdof);
            MeteorologyData2D md0 = MeteorologyData2D.extract(elem.getDOF(0));
            MeteorologyData2D md1 = MeteorologyData2D.extract(elem.getDOF(1));
            MeteorologyData2D md2 = MeteorologyData2D.extract(elem.getDOF(2));
            md.windx = lambda[0] * md0.windx + lambda[1] * md1.windx + lambda[2] * md2.windx;
            md.windy = lambda[0] * md0.windy + lambda[1] * md1.windy + lambda[2] * md2.windy;
            md.windspeed = Function.norm(md.windx, md.windy);
            md.pressure = lambda[0] * md0.pressure + lambda[1] * md1.pressure + lambda[2] * md2.pressure;
            md.insolation = lambda[0] * md0.insolation + lambda[1] * md1.insolation + lambda[2] * md2.insolation;
            md.temperature = lambda[0] * md0.temperature + lambda[1] * md1.temperature + lambda[2] * md2.temperature;
            // break; // Peter 21.09.2016
            // }
            // }
            return rdof;
        }

        protected DOF interpolate(FElement elem, Point3d point) {

            // if (elem.contains(point)) {
            DOF rdof = new DOF(-1, point.x, point.y, point.z);
            MeteorologyData2D md = new MeteorologyData2D();
            rdof.addModelData(md);
            double[] lambda = elem.getNaturefromCart(rdof);
            MeteorologyData2D md0 = MeteorologyData2D.extract(elem.getDOF(0));
            MeteorologyData2D md1 = MeteorologyData2D.extract(elem.getDOF(1));
            MeteorologyData2D md2 = MeteorologyData2D.extract(elem.getDOF(2));
            md.windx = lambda[0] * md0.windx + lambda[1] * md1.windx + lambda[2] * md2.windx;
            md.windy = lambda[0] * md0.windy + lambda[1] * md1.windy + lambda[2] * md2.windy;
            md.windspeed = Function.norm(md.windx, md.windy);
            md.pressure = lambda[0] * md0.pressure + lambda[1] * md1.pressure + lambda[2] * md2.pressure;
            md.insolation = lambda[0] * md0.insolation + lambda[1] * md1.insolation + lambda[2] * md2.insolation;
            md.temperature = lambda[0] * md0.temperature + lambda[1] * md1.temperature + lambda[2] * md2.temperature;

            return rdof;
            // }
            // return null;
        }
    }
}
