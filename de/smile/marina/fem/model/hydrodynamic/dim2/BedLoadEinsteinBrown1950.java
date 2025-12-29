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
 * BedLoad-Transportformulation by Einstein-Brown (1950)
 * 
 * In dieser Transportformel wird eine die Sohlformen beruecksichtigende 
 * Reduzierung der Bodenschubspannungen vorgenommen
 *
 * @author Peter Milbradt
 * @version 3.1.0
 */
public class BedLoadEinsteinBrown1950 implements BedLoad2DFormulation {

    @Override
    public String toString() {
        return "Einstein / Brown (1950)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

        double sfx = smd.lambda * cmd.tauBx / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = smd.lambda * cmd.tauBy / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);

        double sf = Function.norm(sfx, sfy);
        sf = (sf > 0x1.0p-100) ? sf : 0x1.0p-100;
        double f1 = (sf < .2) ? 2.15 * Math.exp(-0.391 / sf) : 40. * sf * sf * sf;

        smd.bedload = Math.sqrt((PhysicalParameters.RHO_SEDIM - cmd.rho) / cmd.rho * PhysicalParameters.G * Math.pow(smd.d50, 3.))
                * f1 * Math.max(0.,Math.sqrt(Math.max(0., 2. / 3. - 36. / smd.D / smd.D / smd.D)) - Math.sqrt(36. / smd.D / smd.D / smd.D)); // koennen hier negative Werte raus kommen?

        smd.bedload *= smd.lambda; // decreasing depending on not erodible bottom

        // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
        smd.bedload = Math.min(1. / SedimentModel2D.morphFactor * cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);

        smd.bedloadVector[0] = smd.bedload * sfx / sf;
        smd.bedloadVector[1] = smd.bedload * sfy / sf;

        return smd.bedloadVector;
    }
}
