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
package de.smile.marina.fem;

import de.smile.math.ode.ivp.ODESystem;
import java.util.Arrays;
import java.util.OptionalDouble;

/**  FE-Approximation of a System of timedependent partial differential equations
 * @author Peter Milbradt
 * @version 3.10
 */
public abstract class TimeDependentFEApproximation extends FEApproximation implements ODESystem {
    
    public String referenceDate = "1970-01-01 00:00:00 UTC+1"; // Reference date [yyyy-MM-dd HH:mm:ss z]
    
    protected boolean first = true; // Indikator ob der erste Zeitintegration-Schritt noch zu machen ist
    
    protected double maxTimeStep = Double.MAX_VALUE;
    protected double time;
    public abstract void setBoundaryCondition(DOF dof, double t);
    
    public final void setStartTime(double starttime){
        time = starttime;
    }
    
    //------------------------------------------------------------------------
    // setMaxTimeStep
    //------------------------------------------------------------------------
    @Override
    synchronized public final void setMaxTimeStep(double maxtimestep){
        maxTimeStep = maxtimestep;
    }
    
    //------------------------------------------------------------------------
    // getMaxTimeStep
    //------------------------------------------------------------------------
    @Override
    synchronized public final double getMaxTimeStep(){
        return maxTimeStep;
    }
    
    synchronized protected final void updateMaxTimeStep(double maxtimestep){
        maxTimeStep = ((maxTimeStep < maxtimestep) ? maxTimeStep : maxtimestep);
    }
    
    /** perform a loop over all DOF and update values and set boundary conditions using the Method setBoundaryCondition  */
    public void setBoundaryConditions(){
        Arrays.stream(fenet.getDOFs()).parallel().forEach( dof -> {
            femodel.setBoundaryCondition(dof,time);
        });
    }
    
    /** perform Elementloop using the Method ElementApproximation  */
    public final void performElementLoop(){
        OptionalDouble tStep = Arrays.stream(fenet.getFElements()).parallel().mapToDouble((FElement element) -> femodel.ElementApproximation(element)).min();
        final double timeStep = tStep.getAsDouble();
        maxTimeStep = ((maxTimeStep < timeStep) ? maxTimeStep : timeStep);
    }
}
