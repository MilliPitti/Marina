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
 * Bagnold 1966
 * Based on: Bagnold, R. A. (1966). An
 * approach to the sediment transport problem from general physics. US
 * Geological Survey Professional Paper, 422-I, 37.
 *
 * The concentration is calculated after Bagnold-Bailard-Ansatz
 *
 * @author Peter Milbradt
 * @version 4.5.2
 */
public class SuspendedLoad2DBagnold1966 implements SuspendedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.15mm < d50 < 1mm */
    private final static double e_s = 0.015, e_b = 0.1; // constants, e_s = 0.01-0.02,  e_b = 0.05-0.15 als obere Grenze fuer den Anteil des Geschiebes am Total Load

    /**
     * Computes the concentration of sediment at a given location based on the provided degree of freedom (DOF).
     * nach CERC TR 1995, S. 4
     * optimiert 
     * mit Seegang
     * 
     * @param dof
     * @return  in [m**3/m**3]
     */
    @Override
    public double getConcentration/*OriginalOptimiert*/(DOF dof) {

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        final double nonTotalepth = (cmd.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT : cmd.totaldepth;

        double tauB = Function.norm(cmd.tauBx, cmd.tauBy);
        final WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
        double c_wbreaking = 0;
        if (wmd != null) {
            tauB += cmd.rho * PhysicalParameters.G / Function.max(Function.sqr(5.), Math.cbrt(nonTotalepth) * Function.sqr(Function.min(cmd.kst, CurrentModel2DData.Nikuradse2Strickler(2.5 * smd.d50)))) * wmd.bottomvelocity * wmd.bottomvelocity;
            /* Einfluss des Wellenbrechens auf die Zunahme der Sedimentkonzentration */
            /* von mir in Anlehnung an die Turbulenzmodellierung nach Battjes */
            if (wmd.epsilon_b > 0.01)
                c_wbreaking = 8.9E-5 * WaveHYPModel2D.BATTJESKOEFF * Math.cbrt(wmd.epsilon_b / cmd.rho) / (PhysicalParameters.G * smd.wc);
        }
        if(tauB<1e-10) return 0.;

        final double excess = Math.max(0., (tauB - smd.tau_cr) / smd.tau_cr);

        //Variable Bedload-Effizienz
        final double eb_max = 0.15;   // Bagnold-Obergrenze
        final double T = (tauB - smd.tau_cr) / smd.tau_cr;
        double fb_EF = 0.0;
        if (T > 0.0) {
            double R = 0.3 / Math.pow(T, 1.5);
            fb_EF = 1.0 / (1.0 + R);
        }
        final double e_b = eb_max * fb_EF; // variable Geschiebeanteil am Total Load

        //Korngrößen-Gewichtung
        final double d_ref = 0.0002; // 0.2 mm
        final double f_d = Math.exp(-smd.d50 / d_ref);

        // Mobility Term
        final double f_tau = excess / (1. + excess);

        // Setzgeschwindigkeitsterm
        final double u_turb = Math.cbrt(tauB / cmd.rho);
        final double f_w = 1. / (1. + smd.wc / u_turb);

        final double es_max = 0.02;   // Bagnold/CERC typisch
//        double e_s = es_max * (f_tau * f_d * f_w); // Konservativ / ingenieurmäßig
        double e_s = es_max * Math.cbrt(f_tau * f_d * f_w); // Physikalisch / energie-basiert
//        double e_s = es_max * (f_tau + f_d + f_w)/3.; // Heuristisch / grob
        
        double c_vol = e_s * (1. - e_b) * cmd.cv / smd.wc * tauB / ((PhysicalParameters.RHO_SEDIM - cmd.rho)* PhysicalParameters.G * nonTotalepth); // [m**3/m**3]
        c_vol += c_wbreaking;
        return Math.min(cmax, c_vol * cmd.wlambda);
    }
    
    /** nach CERC TR 1995, S. 4
     * 
     * @param dof
     * @return  in [m**3/m**3]
     */
//    @Override
    public double getConcentrationOriginal(DOF dof) {
        final double dmax = 1.E-3;
        final double doubledmax = 2 * dmax;

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        if (smd.d50 > doubledmax) {
            return 0.;
        }

        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        if (cmd.totaldepth < CurrentModel2D.WATT || cmd.cv <= 1e-10) return 0.;

        final double tauB = Function.norm(cmd.tauBx, cmd.tauBy);

        final double lambda = Function.min(1., Function.max(0., (doubledmax - smd.d50)) / (dmax)); // decreasing auf Grund zu grosser Koerner
        
        final double S = tauB / (cmd.rho * PhysicalParameters.G * cmd.totaldepth); // energy slope
        final double omega = cmd.rho * PhysicalParameters.G * cmd.totaldepth * S * cmd.cv; // total stream power
        final double c_I = e_s * (1. - e_b) * cmd.cv / smd.wc * omega;// immersed-weight transport rate [N/(m*s)]
        
        double c_vol = c_I / ((PhysicalParameters.RHO_SEDIM - cmd.rho) * PhysicalParameters.G * cmd.cv * cmd.totaldepth); // [m**3/m**3]
        
        // Abminderung bei trockenem Knoten
        c_vol *= Math.min(1, (cmd.totaldepth - CurrentModel2D.WATT) / SedimentModel2D.WATT);
        return Math.min(cmax, c_vol * lambda);
    }

    /**
         * {@inheritDoc}
         *
         * <p>Calculates the load for a given degree of freedom (DOF) by multiplying the concentration
         * at that DOF with the total depth extracted from the current model data.</p>
         *
         * @param dof The degree of freedom for which to retrieve the load.
         * @return The load value for the specified DOF.
         */
    @Override
    public double getLoad(DOF dof) {
        return getConcentration(dof) * CurrentModel2DData.extract(dof).totaldepth;
    }

    @Override
    public String toString() {
        return "Bagnold 1966 (not using CSF)";
    }
}
