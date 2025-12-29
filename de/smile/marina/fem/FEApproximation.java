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

/**  FE-Approximation of a System of partial differential equations
 * @version 3.18.0
 * @author Peter Milbradt
 */
public abstract class FEApproximation {
    
    public boolean resultIsNaN = false;
    public int isNaN=0;
    
    public FEDecomposition fenet;
    public FEModel femodel;
    protected int numberOfThreads = 1;
    
    public int epsgCode = -1; // coordinatereferencesystem
    
    public final void setNumberOfThreads(int i){
        numberOfThreads = Math.max(1, Math.min(i,Runtime.getRuntime().availableProcessors()));
    }
    
    public int getNumberOfThreads(){
        return numberOfThreads;
    }
       
    
    /** DOFs initialisieren*/
    public final void initialDOFs(){
        for (DOF dof : fenet.getDOFs()) {
            dof.addModelData(femodel);
        }
    }
    
    /** DOFs initialisieren*/
    public final void initialElementModelData(){
        for (FElement felement : fenet.getFElements()) {
            felement.addModelData(femodel);
        }
    }  
}
