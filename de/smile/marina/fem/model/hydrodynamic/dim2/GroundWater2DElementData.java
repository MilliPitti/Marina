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

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** 
 * @version 0.2
 * @author Peter Milbradt
 */
public class GroundWater2DElementData implements ModelData{
    
    private static int id = NO_MODEL_DATA;
    private static final long serialVersionUID = 1L;
    
    public double mean_zG=10.;
    public double kf = 0.001;  // Permeability
    public double S0 = 0.25; // effective porosity
    public double u,v;
    
    public GroundWater2DElementData(){
        id = SEARCH_MODEL_DATA;
    }

    /**
     * Schatzt die effektive Porositaet nur aus der hydraulischen Leitfaehigkeit kf.
     * Der Zusammenhang ist nicht eindeutig; daher wird eine robuste Heuristik verwendet.
     *
     * @param kf hydraulische Leitfaehigkeit [m/s]
     * @return effektive Porositaet S0 [-]
     */
    public static double getEffectivProrsity(double kf) {
        if (!Double.isFinite(kf) || kf <= 0.) {
            return 0.25;
        }

        final double logKf = Math.log10(Math.max(1e-12, kf));
        final double s0;
        if (logKf <= -8.) {
            s0 = 0.08;
        } else if (logKf >= -3.) {
            s0 = 0.35;
        } else {
            s0 = 0.08 + (logKf + 8.) / 5. * (0.35 - 0.08);
        }
        return Math.max(0.05, Math.min(0.45, s0));
    }

    /**
     * Schatzt die effektive Porositaet aus kf und Korndurchmesser d50 mit einer
     * Kozeny-Carman-basierten Naeherung.
     *
     * @param kf hydraulische Leitfaehigkeit [m/s]
     * @param d50 mittlerer Korndurchmesser [m]
     * @return effektive Porositaet S0 [-]
     */
    public static double getEffectivProrsity(double kf, double d50) {
        if (!Double.isFinite(d50) || d50 <= 0.) {
            return getEffectivProrsity(kf);
        }
        if (!Double.isFinite(kf) || kf <= 0.) {
            return 0.25;
        }

        // Umrechnung von hydraulischer Leitfaehigkeit kf auf intrinsische Permeabilitaet k [m^2]
        final double k = kf * PhysicalParameters.KINVISCOSITY_WATER / PhysicalParameters.G;
        if (!Double.isFinite(k) || k <= 0.) {
            return getEffectivProrsity(kf);
        }

        // Kozeny-Carman: k = d50^2 * n^3 / (180 * (1-n)^2)
        final double target = 180. * k / (d50 * d50);
        if (!Double.isFinite(target) || target <= 0.) {
            return getEffectivProrsity(kf);
        }

        // Loese n^3/(1-n)^2 = target per Bisektion im physikalisch sinnvollen Bereich
        double lo = 0.01;
        double hi = 0.80;
        for (int iter = 0; iter < 80; iter++) {
            final double mid = 0.5 * (lo + hi);
            final double lhs = (mid * mid * mid) / ((1. - mid) * (1. - mid));
            if (lhs < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        final double n = 0.5 * (lo + hi);
        return Math.max(0.05, Math.min(0.60, n));
    }
    
    /**
     * extrahiert die Current2DElementData des FElements
     *
     * @param ele
     * @return
     */
    public static GroundWater2DElementData extract(FElement ele) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = ele.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof GroundWater2DElementData) {
                    id = ele.modelData.indexOf(md);
                    return (GroundWater2DElementData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (GroundWater2DElementData) ele.modelData.get(id);
        }
        return null;
    }
}
