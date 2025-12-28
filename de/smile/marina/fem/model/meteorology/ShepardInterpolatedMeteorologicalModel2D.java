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

import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author milbradt
 */
public class ShepardInterpolatedMeteorologicalModel2D extends TimeDependentFEApproximation
        implements FEModel, TimeDependentModel {
    private DataOutputStream xf_os = null;
    OKWind[] windtimeseries = null;
    ShepardMeteorologyData2D[] dof_data = null;
    // fuer Temperaturansatz
    double T1 = 365. * 24. * 3600.; // 1 Jahr
    double w1 = (2. * 3.1415) / T1; // Kreisfrequenz bestimmen
    double C1 = 7.5; // Amplitude
    double x1 = 12.5; // Mittelwert
    double T2 = 24. * 3600.; // 1 Tag
    double w2 = (2. * 3.1415) / T2; // Kreisfrequenz bestimmen
    double C2 = 5.; // Amplitude
    double x2 = 0.; // Mittelwert
    // fuer Lichtintensitaetansatz bezogen auf ca. 55 Grad noerdliche Breite
    double T3 = 365. * 24. * 3600.; // 1 Jahr
    double w3 = (2. * 3.1415) / T3; // Kreisfrequenz bestimmen
    double C3 = 10.; // Amplitude
    double x3 = 12.5; // Mittelwert MJm-2day-1

    /** Creates a new instance of OKWindModel */
    public ShepardInterpolatedMeteorologicalModel2D(FEDecomposition fe, OKWind[] windtimeseries,
            MeteorologicalDat dat) {
        fenet = fe;
        femodel = this;

        this.windtimeseries = windtimeseries;

        maxTimeStep = 10000.;

        dof_data = new ShepardMeteorologyData2D[fenet.getNumberofDOFs()];

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
        ShepardMeteorologyData2D data = new ShepardMeteorologyData2D(dof);
        int dofnumber = dof.number;
        dof_data[dofnumber] = data;
        return data;
    }

    /**
     * Compute the time derivations on each node by the FE-domainapproximation of
     * the FE-Model
     * 
     * @param time
     * @param x
     * @return
     */
    @Override
    public double[] getRateofChange(double time, double x[]) {

        // System.out.println(time+" "+data.insolation+ " "+I_n);

        return new double[1];
    }

    public double[] initialSolution(double StartTime) {
        this.time = StartTime;

        System.out.println("\tinterpolating initial wind feld");

        for (ShepardMeteorologyData2D dof_data1 : dof_data) {
            dof_data1.update();
        }
        return null;
    }

    @Override
    public ModelData genData(FElement felement) {
        return null;
    }

    @Override
    public void setBoundaryCondition(DOF dof, double t) {
    }

    @Override
    public double ElementApproximation(FElement ele) {
        return Double.MAX_VALUE;
    }

    @Override
    public void timeStep(double dt) {
        this.time += dt;

        for (ShepardMeteorologyData2D dof_data1 : dof_data) {
            dof_data1.update();
        }
    }

    @Override
    public void write_erg_xf() {
        if (xf_os != null)
            try {
                xf_os.writeFloat((float) time);

                DOF[] dofs = fenet.getDOFs();
                for (DOF dof : dofs) {
                    MeteorologyData2D meteorologyData2D = MeteorologyData2D.extract(dof);
                    xf_os.writeFloat((float) meteorologyData2D.windx);
                    xf_os.writeFloat((float) meteorologyData2D.windy);
                    xf_os.writeFloat((float) meteorologyData2D.pressure);
                    xf_os.writeFloat((float) meteorologyData2D.temperature);
                    xf_os.writeFloat((float) meteorologyData2D.insolation);
                }
                xf_os.flush();
            } catch (IOException e) {
            }
    }

    class ShepardMeteorologyData2D extends MeteorologyData2D {

        double w[]; // [0..n]-Vektor mit den Gewichten fuer die Stuetzwerte; haengt ab von r und
                    // mue.

        public ShepardMeteorologyData2D(DOF dof) {
            super();
            double[] r = new double[windtimeseries.length]; // [0..n]-Vektor mit den quadratischen Euklidischen
                                                            // Abstaenden der Stuetzstellen von der Interpolationsstelle
            int j;

            w = new double[windtimeseries.length];
            for (j = 0; j < windtimeseries.length; j++) { // Abstaende r[j] berechnen
                r[j] = dof.sqrdistance2d(windtimeseries[j].getLocation());

                if (r[j] == 0.) { // Abstand Null, d. h. (x0,y0)
                    w[j] = 1.;
                    for (int i = 0; i < w.length; i++) {
                        if (i != j) {
                            w[i] = 0.;
                        }
                    }
                    j = r.length + 10;
                }
            }
            if (j != r.length + 10) {
                double norm;
                for (j = 0, norm = 0.; j < windtimeseries.length; j++) {
                    w[j] = 1. / r[j];
                    norm += w[j];
                }
                for (j = 0; j < windtimeseries.length; j++) // die Gewichte
                {
                    w[j] /= norm;
                }
            }
        }

        public void update() {
            windx = 0.;
            windy = 0.;
            for (int j = 0; j < windtimeseries.length; j++) {
                double[] wi = windtimeseries[j].getValue(time);
                windx += w[j] * wi[0];
                windy += w[j] * wi[1];
            }
            windspeed = Function.norm(windx, windy);
            temperature = C1 * Math.cos(w1 * time - Math.PI) + x1;
            // data.temperature+=C2*Math.cos(w2*time-Math.PI)+x2;

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
                insolation = 0.;
            } else {
                insolation = I_n / p * (1. + Math.cos((t - 0.5) * Math.PI * 2. / p));
            }
        }
    }
}
