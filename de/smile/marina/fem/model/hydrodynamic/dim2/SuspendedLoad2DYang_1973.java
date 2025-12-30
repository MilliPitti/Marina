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
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**
 * Bei dieser Transportformel wird die Stroemungsleistung fuer die Bestimmung
 * der Sedimentkonzentration herangezogen, ein Kriterium fuer den Beginn der
 * Sedimentbewegung wird hier nicht verwendet. Die mittlere kritische
 * Flieszgeschwindigkeit bei Beginn der Sedimentbewegung errechnet sich aus
 * Yang, C.T. (1973). "Incipient Motion and Sediment Transport." Journal of the Hydraulics Division, 99(10), 1679-1704.
 * Die Yang-Formel (1973) wurde urspruenglich nicht exklusiv fuer Suspended Load 
 * formuliert, sondern zur Abschaetzung des Gesamttransports - mit Fokus auf 
 * feineres Material und flache, sandige Fluesse.
 * Formel wurde so angepasst, dass nur der suspensionanteil berechnet wird mit dem Grenzdurchmesser 2 mm
 * Die Transportformel ist geeignet fuer 
 * Sohlneigung: 0.043 bis 29 poMill 
 * Korngroeszen: 0.062 bis 2 (7) [mm] 
 * Mittlere Flieszgeschwindigkeiten: 0.24 bis 1.9 m/s
 *
 * @author Peter Milbradt
 * @version 4.5.1
 */
public class SuspendedLoad2DYang_1973 implements SuspendedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.062mm < d50 < 2mm */
    private final static double dTransition = 2.0E-3; // Grenzdurchmesser 2 mm
    private final static double dRange = 0.5E-3; // Uebergangsbereich 0.5 mm

    @Override
    public String toString() {
        return "Yang (1973) (not using CSF)";
    }

    @Override
    public double getConcentration(DOF dof) {

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        if (smd.d50 > dTransition + dRange) return 0.;

        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        if (cmd.totaldepth < CurrentModel2D.WATT) return 0.;

        final double reStar = SedimentModel2DData.getParticleReynoldsNumber(smd.d50, cmd);

        double vcr = smd.wc;
        if (reStar < 70) {
            vcr *= (2.5 / (Math.log10(reStar) - 0.06) + 0.66);
        } else {
            vcr *= 2.05;
        }

        if (cmd.cv <= vcr) {
            return 0.;
        }

        final double depth = Math.max(cmd.totaldepth, 0.05);
        final double tauB = Function.norm(cmd.tauBx, cmd.tauBy);
        final double S = tauB / (cmd.rho * PhysicalParameters.G * depth);
        double logCt;
        final double uStar = cmd.cv;
        final double arg = (uStar * S / smd.wc) - (vcr * S / smd.wc);

        if (arg <= 0.) {
            return 0.;
        }

        final double wSedStar = smd.wc * cmd.totaldepth / PhysicalParameters.KINVISCOSITY_WATER;

        if (smd.d50 < 2.E-3) {
            logCt = 5.435 - 0.286 * Math.log10(wSedStar)
                    - 0.457 * Math.log10(uStar / smd.wc)
                    + (1.799 - 0.409 * Math.log10(wSedStar) - 0.314 * Math.log10(uStar / smd.wc)) * Math.log10(arg);

            if (logCt > 2.0) {
                logCt = 5.165 - 0.153 * Math.log10(wSedStar)
                        - 0.297 * Math.log10(uStar / smd.wc)
                        + (1.780 - 0.360 * Math.log10(wSedStar) - 0.480 * Math.log10(uStar / smd.wc)) * Math.log10(uStar * S / smd.wc);
            }
        } else {
            logCt = 6.681 - 0.633 * Math.log10(wSedStar)
                    - 4.816 * Math.log10(uStar / smd.wc)
                    + (2.784 - 0.305 * Math.log10(wSedStar) - 0.282 * Math.log10(uStar / smd.wc)) * Math.log10(arg);
        }
        
        if (logCt < -10) return 0.; // numerische Stabilitaet
        final double Ct_ppm = Math.pow(10.0, logCt); // [ppm by weight]
        final double c_mass = Ct_ppm / 1e6 * PhysicalParameters.RHO_WATER; // [kg/m**3]
        double c_vol = c_mass / PhysicalParameters.RHO_SEDIM; // [m**3/m**3]


        // Uebergangsgewichtung
        double weight = 1.0;
        if (smd.d50 > dTransition - dRange) {
            weight = (dTransition + dRange - smd.d50) / (2 * dRange);
        }
        c_vol *= weight;

        return Math.min(cmax, c_vol);
        
    }

    @Override
    public double getLoad(DOF dof) {
        return getConcentration(dof) * CurrentModel2DData.extract(dof).totaldepth;
    }
}
