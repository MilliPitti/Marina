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

import de.smile.marina.fem.DOF;

/**
 * @author Peter Milbradt
 * @version 1.8.13
 */
public interface SuspendedLoad2DFormulation {
    /** maximal volume concentration [m**3/m**3] */
    static final double cmax = 0.65;
    
//    /**
//     * @param u
//     * @param v
//     * @param d
//     * @param bottomslope
//     * @param epsilon_b is the enegy dissipation rate by wavebraking
//     * @return  */  
//    public double getLoad          (double u, double v, double d, double bottomslope, double epsilon_b);
    
//    /**
//     * @param dof
//     * @param u
//     * @param v
//     * @param d
//     * @param bottomslope
//     * @param epsilon_b is the enegy dissipation rate by wavebraking
//     * @return   */  
//    public double getConcentration (double u, double v, double d, double bottomslope, double epsilon_b);
    
    /**
     * Returns the load applied to the specified degree of freedom (DOF).
     *
     * @param dof The degree of freedom for which to retrieve the load.
     * @return The load value [m**3/m**2] at the specified DOF.
     */
    public double getLoad          (DOF dof);
    
    /**
     * mean volume concentration [m**3/m**3]
     *
     * @param dof the degree of freedom to sample.
     * @return mean volume concentration [m**3/m**3] at the specified degree of freedom.
     */
    public double getConcentration (DOF dof);
}
