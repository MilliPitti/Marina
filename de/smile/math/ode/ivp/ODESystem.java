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
package de.smile.math.ode.ivp;

/**
 * This interface characterizes an ODESystem, which is a system of first order
 * ordinary differential equations. An ODESystem is described by the rate of
 * change of its state quantities for a given point in time and a given state.
 *
 * @author Peter Milbradt
 */
public interface ODESystem {

    /**
     * Calling this method determines the change rates of this system for a
     * given point in time and the current values for the state quantities.
     *
     * @param time the point in time
     * @param x the state quantities at the given point in time.
     * @return the change rates for x (the array has the same order as x)
     */
    double[] getRateofChange(double time, double x[]);

    /**
     * set the System depend maximal time step
     * @param maxtimestep
     */
    void setMaxTimeStep(double maxtimestep);

    /**
     * read the System depend maximal time step
     *
     * @return maxtimestep
     */
    double getMaxTimeStep();
}
