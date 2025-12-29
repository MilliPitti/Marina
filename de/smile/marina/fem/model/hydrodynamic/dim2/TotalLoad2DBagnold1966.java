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
 * Bagnold 1966 ist ein "Total Load"-Modell, das sowohl die "Bedload" (Geschiebefracht) als auch die "Suspended Load" (Schwebfracht) beruecksichtigt
 *
 * @author Peter Milbradt
 * @version 4.4.13
 */
public class TotalLoad2DBagnold1966 implements TotalLoad2DFormulation {
    private final static double e_s = 0.015; // constants, default e_s = 0.02 // Peter 27.11.2014 in Anlehnung an den Koeffizienten von vanRijn1984

    
    /** 
     * 
     * @param dof
     * @return  in [m**3/m**3]
     */
    @Override
    public double[] getTotalLoadVector(DOF dof) {

        double[] totalLoadVector = new double[2];
        
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);


        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        if (cmd.totaldepth < CurrentModel2D.WATT || cmd.cv <= 1e-10) return totalLoadVector;

        final double tauB = Function.norm(cmd.tauBx, cmd.tauBy);
        
        final double S = tauB / (cmd.rho * PhysicalParameters.G * cmd.totaldepth); // energy slope
        final double omega = cmd.rho * PhysicalParameters.G * cmd.totaldepth * S * cmd.cv; // total stream power
        final double c_I = e_s * cmd.cv / smd.wc * omega;// immersed-weight transport rate [N/(m*s)]
        
        double totalLoad = c_I / ((PhysicalParameters.RHO_SEDIM - cmd.rho) * PhysicalParameters.G); // [m**3/m/s]
        
        totalLoadVector[0] = totalLoad * cmd.u/cmd.cv;
        totalLoadVector[1] = totalLoad * cmd.v/cmd.cv;
        return totalLoadVector;
    }

    @Override
    public String toString() {
        return "Bagnold 1966 (not using CSF)";
    }
}
