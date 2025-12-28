/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2025

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


/** Rossinskie Debolsky
 * mit Beruecksichtigung des Seegangs und der Spontanen Erosion auf Grund zu groszer Bodenneigung
 * @author Peter Milbradt
 * @version 3.15.4
 */
public class SuspendedLoad2DRD implements SuspendedLoad2DFormulation {

    static final double BATTJESKOEFF =     0.2; // Turbulenzkoeffizient infolge Wellenbrechen	
    
    @Override
    public double getConcentration(DOF dof) {
        
        double  konzmax     = 0.0;
        
        CurrentModel2DData  currentmodeldata = CurrentModel2DData.extract(dof);
        double cv=currentmodeldata.cv;
        final double d=currentmodeldata.totaldepth;

        if (d > CurrentModel2D.halfWATT) {
            
            WaveHYPModel2DData wave = WaveHYPModel2DData.extract(dof);
            double epsilon_b=0.;
            if(wave!=null){
                cv += wave.bottomvelocity;
                epsilon_b = wave.epsilon_b;
            }
            
            SedimentModel2DData smd = SedimentModel2DData.extract(dof);
            /* erzeugen von Konzentrationen infolge spontaner Erosion */
            /* meine eigene einfache Implemntierung */
            cv += (1.-1./smd.bottomslope) * smd.wc * currentmodeldata.wlambda * smd.lambda;
            
            /* Rossinsky u. Debolsky */
            konzmax = 8.9E-5 * cv*cv*cv / (PhysicalParameters.G*smd.wc*Function.max(.1,d)) * currentmodeldata.wlambda;

            /* Einfluss des Wellenbrechens auf die Zunahme der Sedimentkonzentration */
            /* von mir in Anlehnung an die Turbulenzmodellierung nach Battjes */
            if (epsilon_b > 0.01)
                konzmax += 8.9E-5 * BATTJESKOEFF * Math.pow(epsilon_b / PhysicalParameters.RHO_WATER, 0.33333) / (PhysicalParameters.G * smd.wc) * currentmodeldata.wlambda;

            konzmax *= smd.lambda;  // not erodible bottom // Peter 04.07.2025
            konzmax *= Function.min(currentmodeldata.wlambda, d / smd.bound);  // increasing depending on water depth

        }

        return Math.min(cmax, konzmax);
    }
    
    @Override
    public double getLoad(DOF dof) {
	return getConcentration(dof)*CurrentModel2DData.extract(dof).totaldepth;
    }
    
    @Override
    public String toString(){
        return "Rossinskie Debolsky (not using CSF)";
    }
    
}