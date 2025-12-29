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
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.math.Function;
import javax.vecmath.*;

/**
 * @author Peter Milbradt
 * @version 4.4.9
 */
public class QSteuerung implements ScalarFunction1d {

    protected FEDecomposition sysdat;

    protected double time = Double.NEGATIVE_INFINITY;
    protected ScalarFunction1d qzeitreihe;
    protected DiscretScalarFunction1d z, v, d;
    protected int[] knotennummern;

    protected final double repsilon = 1.E-4; // relative Fehlergrenze
    protected double epsilon = 1.E-10; // absolute Fehlergrenze
    protected double A;
    protected double s = 0.; // Laenge des Querschnittes
    protected double maxQ = 0.;

    protected final double WATT = CurrentModel2D.WATT;

    protected QSteuerung() {
    }

    /**
     * @param qzeitreihe
     * @param knotennummern
     * @param sysdat
     */
    public QSteuerung(DiscretScalarFunction1d qzeitreihe, int[] knotennummern, FEDecomposition sysdat) {
        maxQ = Math.max(qzeitreihe.getmax()[1], -qzeitreihe.getmin()[1]);
        epsilon = Math.max(1.E-10, maxQ * repsilon); // Abbruchgrenze, relatives zur Groesze der Durchfluesse

        this.sysdat = sysdat;
        this.qzeitreihe = qzeitreihe;
        this.knotennummern = knotennummern;
        double[][] profilefeld = new double[2][knotennummern.length];
        profilefeld[0][0] = s;
        profilefeld[1][0] = sysdat.getDOF(knotennummern[0]).z;
        // System.out.println(notennummern[0]);
        for (int i = 1; i < knotennummern.length; i++) {
            s += distance2d(sysdat.getDOF(knotennummern[i]), sysdat.getDOF(knotennummern[i - 1]));
            profilefeld[0][i] = s;
            profilefeld[1][i] = sysdat.getDOF(knotennummern[i]).z;
        }
        this.z = new DiscretScalarFunction1d(profilefeld); // Tiefenpolynom
        // ausgabez();
        this.d = new DiscretScalarFunction1d(profilefeld);

    }

    /**
     * @param b
     */
    @Override
    public void setPeriodic(boolean b) {
        qzeitreihe.setPeriodic(b);
    }

    protected static double distance2d(Point3d p, Point3d q) {
        return Math.sqrt((q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y));
    }

    public synchronized void update(SurfaceWaterModelData[] x, double t) {
        if (time != t) {
            /* START: bestimmen der mittleren Wasserspiegellage ueber den Querschnitt */
            double h_mean = 0.;
            int n = 0;
            double zmin = -z.getValueAt(0)[1];
            for (int i = 0; i < knotennummern.length; i++) {
                zmin = (zmin < -z.getValueAt(i)[1]) ? zmin : -z.getValueAt(i)[1]; // min(zmin, -z.getValueAt(i)[1]);
                DOF dof = sysdat.getDOF(knotennummern[i]);
                SurfaceWaterModelData cdata = SurfaceWaterModelData.extract(dof); // Peter 20.03.25 // Instabil am Q
                                                                                  // Einlaufrand
                if (cdata.totaldepth > WATT) {
                    h_mean += cdata.eta;
                    n++;
                }
                // for (FElement elem : dof.getFElements()) { // Peter 20.03.25 auskommentiert
                // // Instabil am Q Einlaufrand
                // for (int ll = 0; ll < 3; ll++) {
                // if (elem.getDOF(ll) == dof) {
                // for (int ii = 0; ii < 3; ii++) {
                // int jtmp = elem.getDOF((ll + ii) % 3).number;
                // SurfaceWaterModelData cdata =
                // SurfaceWaterModelData.extract(sysdat.getDOF(jtmp));
                // if (cdata.totaldepth > WATT) {
                // h_mean += cdata.eta;
                // n++;
                // }
                // }
                // break;
                // }
                // }
                // }
            }
            if (n > 0) { /** todo hier gibt es noch Optimierungspotential */
                h_mean /= n;
                // h_mean =
                // Function.max(h_mean,zmin+Function.max(WATT,Math.abs(qzeitreihe.getValue(t))/(Math.sqrt(PhysicalParameters.G
                // * 20 *WATT)*s))); // 20.05.2025 auskommentiert
            } else { // alle Knoten der RB haben Tiefe = 0, falls Q != 0 so muss der wasserspiegel
                     // angehoben werden!!
                h_mean = zmin + Function.max(WATT,
                        Math.abs(qzeitreihe.getValue(t)) / (Math.sqrt(PhysicalParameters.G * 10 * WATT) * s));
            }
            /* END: bestimmen der mittleren Wasserspiegellage ueber den Querschnitt */
            for (int i = 0; i < knotennummern.length; i++) {
                if ((z.getValueAt(i)[1] + h_mean) > 0 && x[knotennummern[i]].bh == null) {
                    x[knotennummern[i]].eta = h_mean;
                }
                d.setValueAt(i, Function.max(0., z.getValueAt(i)[1] + x[knotennummern[i]].eta));
            }
            berechneV(qzeitreihe.getValue(t)); // Geschwindigkeitsverteilung iterativ berechnet
            time = t;
        }
    }

    /**
     * @param nr
     * @return
     */
    public double getValueAt(int nr) {
        int i = 0;
        while (nr != knotennummern[i])
            i++;
        return v.getValueAt(i)[1];
    }

    // Funktion zum Berechnen der gesamten Wasser-Flaeche des uebergebenen QS
    private void berechneA() {
        A = 0.0;

        int anz = z.getSizeOfValues();
        for (int i = 0; i < anz - 1; i++) {
            // Erfragen der Wassertiefen am Knoten i und Knoten i+1
            double[] d0 = d.getValueAt(i);
            double[] d1 = d.getValueAt(i + 1);
            A += (d1[0] - d0[0]) * (d1[1] + d0[1]) * 0.5;
        }
    }

    // Funktion zum iterativen Berechnen der Geschwindigkeitsverteilung des
    // uebergebenen QS
    protected void berechneV(double Q) {

        berechneA(); // QS-Flaeche
        // System.out.println("Flaeche = "+A);

        // Geschwindigkeitsverteilung: Verlauf entsprechend dem QS-Profil (1.Schritt)
        v = berechneVvorh(Q);

        if (Math.abs(Q) > epsilon) {
            // Iteratives Berechnen der Geschwindigkeitsverteilung, solange bis Qvorh vom
            // Eingangswert
            // Q maximal nur noch um epsilon = 1 % abweicht
            int anz = v.getSizeOfValues();
            int schritt = 0;
            double Qvorh = berechneQvorh(v);
            double dQ = Q - Qvorh;

            while ((Math.abs(dQ / Q) > epsilon) && (schritt < 20)) {

                // Berechnen der Korrektur-Geschwindigkeitsverteilung
                DiscretScalarFunction1d dv = berechneVvorh(dQ);
                for (int i = 0; i < anz; i++) {
                    v.setValueAt(i, v.getValueAt(i)[1] + dv.getValueAt(i)[1]);
                }
                schritt++;
                // Berechnen der Durchfluss-Differenz
                dQ = Q - berechneQvorh(v);
            }
            // System.out.println(Q + " " + berechneQvorh(v));
        }
    }

    // Funktion zum Berechnen einer Geschwindigkeitsverteilung (abhaengig vom
    // uebergebenen Qein)
    private DiscretScalarFunction1d berechneVvorh(double Qin) {
        double vm = Qin / A;
        // Berechnung von vm und vmax
        double vmax = vm / 0.8;

        // Berechnung der maximalen Wassertiefe, wo vmax angenommen wird
        double dmax = d.getmax()[1];

        // Berechnung der Geschwindigkeitsverteilung
        int anz = z.getSizeOfValues();
        double[][] vfeld = new double[2][anz];
        for (int i = 0; i < anz; i++) {
            vfeld[0][i] = z.getValueAt(i)[0]; // s-Koordinate
            if (i == 0 || i == anz - 1)
                vfeld[1][i] = 0.0;
            else
                vfeld[1][i] = vmax * d.getValueAt(i)[1] / dmax;
        }
        return new DiscretScalarFunction1d(vfeld);
    }

    // Funktion zum Berechnen des vorhandenen Durchflusses: Rueckrechnung
    protected double berechneQvorh(DiscretScalarFunction1d vin) {
        double Qvorh = 0.0;

        int anz = z.getSizeOfValues();
        for (int i = 0; i < anz - 1; i++) {
            // Erfragen der Wassertiefen und Geschwindigkeiten am Knoten i und Knoten i+1
            double[] d0 = d.getValueAt(i);
            double[] d1 = d.getValueAt(i + 1);
            double[] v0 = vin.getValueAt(i);
            double[] v1 = vin.getValueAt(i + 1);

            // Trapetzregel
            Qvorh += 0.5 * (d1[1] * v1[1] + d0[1] * v0[1]) * (d1[0] - d0[0]);
        }
        return Qvorh;
    }

    @SuppressWarnings("unused")
    private void ausgabe() {
        System.out.println("QS-Werte: A = " + A);
        System.out.println("");
        System.out.println("Geschwindigkeitsverteilung");
        System.out.println("knotennr  x[m]  v[m/s] d[m]");
        System.out.println("------------");
        int anz = v.getSizeOfValues();
        for (int i = 0; i < anz; i++) {
            System.out.println(knotennummern[i] + " , " + v.getValueAt(i)[0] + " , " + v.getValueAt(i)[1] + " + "
                    + d.getValueAt(i)[1]);
        }
        System.out.println("");
    }

    @SuppressWarnings("unused")
    private void ausgabez() {
        System.out.println("QS-Werte: A = " + A);
        System.out.println("");
        System.out.println("Geschwindigkeitsverteilung");
        System.out.println("x[m]  z[m/s]");
        System.out.println("------------");
        int anz = z.getSizeOfValues();
        for (int i = 0; i < anz; i++) {
            System.out.println(z.getValueAt(i)[0] + " , " + z.getValueAt(i)[1]);
        }
        System.out.println("");
    }

    @Override
    public double getDifferential(double x) {
        return v.getDifferential(x);
    }

    @Override
    public double getValue(double x) {
        return v.getValue(x);
    }

    @Override
    public boolean isPeriodic() {
        return qzeitreihe.isPeriodic();
    }

}
