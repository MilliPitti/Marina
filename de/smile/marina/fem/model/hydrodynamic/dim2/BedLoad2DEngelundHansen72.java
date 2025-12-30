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
 * Total-Transportformulation by Engelund und Hansen (1972)
 *
 * @author Peter Milbradt
 * @version 2.9.0
 */
public class BedLoad2DEngelundHansen72 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.15mm < d50 < 1mm */
    private final static double dmin = 0.15E-3;
    private final static double dminTenth = dmin / 10.;
    @Override
    public String toString() {
        return "EngelundHansen72 (not using CSF)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {
       
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        if(smd.lambda < Double.MIN_NORMAL) return smd.bedloadVector;
        
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        final double wlambda = Function.min(1., cmd.totaldepth / .1);
        final double cv = cmd.cv;
        if(cv<0x1.0p-200)
            return smd.bedloadVector;

        double C = 18. * Math.log(12. * cmd.totaldepth / cmd.ks); 
        if(C<0x1.0p-300)
            return smd.bedloadVector;

        smd.bedload = 0.05 * Math.pow(cv, 4) * Math.pow(PhysicalParameters.RHO_SEDIM/PhysicalParameters.RHO_WATER-1., 2) / PhysicalParameters.sqrtG / smd.d50 / Math.pow(C, 3.);

        if (smd.d50 < dmin) {// Abminderung auf Grund zu kleiner Koerner
            smd.bedload *= Math.max(0., (smd.d50 - dminTenth) / (dmin - dminTenth));
        }
        smd.bedload *= wlambda; // decreasing depending on dry falling
        // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
        smd.bedload = Math.min(Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);

        smd.bedloadVector[0] = smd.bedload * cmd.u;
        smd.bedloadVector[1] = smd.bedload * cmd.v;
        smd.bedload *= cv;
        
        return smd.bedloadVector;
    }
}
