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

import bijava.math.ifunction.DiscretScalarFunction1d;
import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FElement;
import de.smile.math.Function;

/**
 * beschraenkt den Durchfluss auf Qmax bei gegebenen Wasserstand, unabhaengig von der Durchflussrichtung
 *
 * @author milbradt
 * @version 3.15.5
 */
public class H_Qmax_Steuerung extends QSteuerung {

    private ScalarFunction1d h_qmax_Relation;
    private double Q_aktuell;

    protected H_Qmax_Steuerung() {
    }

    /**
     * @param h_qmax_Relation
     * @param knotennummern
     * @param sysdat
     */
    public H_Qmax_Steuerung(DiscretScalarFunction1d h_qmax_Relation, int[] knotennummern, FEDecomposition sysdat) {
        maxQ = h_qmax_Relation.getmax()[1];
        epsilon = Math.max(1.E-10, maxQ * repsilon); // Abbruchgrenze, relatives zur Groesze der Durchfluesse

        this.sysdat = sysdat;
        this.h_qmax_Relation = h_qmax_Relation;
        this.knotennummern = knotennummern;
        double[][] profilefeld = new double[2][knotennummern.length];
        profilefeld[0][0] = s;
        profilefeld[1][0] = sysdat.getDOF(knotennummern[0]).z;

        for (int i = 1; i < knotennummern.length; i++) {
            s += QSteuerung.distance2d(sysdat.getDOF(knotennummern[i]), sysdat.getDOF(knotennummern[i - 1]));
            profilefeld[0][i] = s;
            profilefeld[1][i] = sysdat.getDOF(knotennummern[i]).z;
        }
        this.z = new DiscretScalarFunction1d(profilefeld);			// Tiefenpolynom
        this.d = new DiscretScalarFunction1d(profilefeld);
        Q_aktuell = berechneQvorh(v);
    }

    /**
     * @param x
     * @param t
     */
    @Override
    public synchronized void update(SurfaceWaterModelData[] x, double t) {
        if (time != t) {
            Q_aktuell = Math.abs(berechneQvorh(v));
            /* START: bestimmen der mittleren Wasserspiegellage ueber den Querschnitt */
            double h_mean = 0.;
            int n = 0;
            double zmin = -z.getValueAt(0)[1];
            for (int i = 0; i < knotennummern.length; i++) {
                zmin = (zmin < -z.getValueAt(i)[1]) ? zmin : -z.getValueAt(i)[1]; // min(zmin, -z.getValueAt(i)[1]);                
                DOF dof = sysdat.getDOF(knotennummern[i]);
                for (FElement elem : dof.getFElements()) {
                    for (int ll = 0; ll < 3; ll++) {
                        if (elem.getDOF(ll) == dof) {
                            for (int ii = 0; ii < 3; ii++) {
                                int jtmp = elem.getDOF((ll + ii) % 3).number;
                                SurfaceWaterModelData tmpcdata = SurfaceWaterModelData.extract(sysdat.getDOF(jtmp));
                                if (tmpcdata.totaldepth > WATT) {
                                    h_mean += tmpcdata.eta;
                                    n++;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (n > 0) {
                /** todo hier gibt es noch Optimierungspotential */
                h_mean /= n;
                h_mean = Function.max(h_mean, zmin + Function.max(WATT, Math.abs(h_qmax_Relation.getValue(h_mean)) / (Math.sqrt(PhysicalParameters.G * 20 * WATT) * s)));
                /* END: bestimmen der mittleren Wasserspiegellage ueber den Querschnitt */
                final double Qmax = Math.abs(h_qmax_Relation.getValue(h_mean));
                if (Qmax > epsilon) {
                    final double Q_aktuellOVERQmax = Q_aktuell / Qmax;
                    if (Q_aktuellOVERQmax > 1.) {
                        /** Gechwindigkeiten durch Division verringern */
                        for (int i = 0; i < v.getSizeOfValues(); i++) {
                            v.setValueAt(i, v.getValueAt(i)[1] / Q_aktuellOVERQmax);
                        }
                    }
                }
            }
            time = t;
        }
    }

    @Override
    public double getValue(double x) {
        return v.getValue(x);
    }

    @Override
    public double getDifferential(double x) {
        throw new UnsupportedOperationException("Not supported  for H_Qmax_Steuerung.");
    }

    @Override
    public void setPeriodic(boolean b) {
        throw new UnsupportedOperationException("Not supported for H_Qmax_Steuerung.");
    }

    @Override
    public boolean isPeriodic() {
        throw new UnsupportedOperationException("Not supported  for H_Qmax_Steuerung.");
    }

}
