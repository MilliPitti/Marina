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

/**
 * Meyer-Peter und Mueller (1948)
 * Die Transportformel ist geeignet fuer
 * Sohlneigung: 0.4 bis 20 poMill 
 * Korngroeszen: 0.4 bis 30 [mm] 
 * Mittlere Flieszgeschwindigkeiten: 0.37 bis 2.87 m/s
 * 
 * In dieser Transportformel wird eine die Sohlformen beruecksichtigende 
 * Reduzierung der Bodenschubspannungen vorgenommen
 * 
 * @author Peter Milbradt
 * @version 2.6.1
 */
public class BedLoad2DMPM48 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.4mm < d50 < 29mm */
    private final static double dmin = 0.4E-3;

    @Override
    public String toString() {
        return "MPM48 (based on CSF)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

//          double C = 18.*Math.log(12.*d/cmd.ks);
//          double Cs = 18.*Math.log(12.*d/smd.d50);
//          double nue = C/Cs;

//        double CSF = smd.CSF; // variable
          double CSF = 0.047; // fest nach Meyer-Peter und Mueller (1948)

        double sfx = smd.lambda * cmd.tauBx / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = smd.lambda * cmd.tauBy / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        
        double sf = Function.norm(sfx, sfy);

        if (sf > CSF) {
            smd.bedload = 8. * Math.sqrt((PhysicalParameters.RHO_SEDIM - cmd.rho) / cmd.rho * PhysicalParameters.G * Math.pow(smd.d50, 3.) * Math.pow(sf - CSF, 3.));
           
            smd.bedload *= Function.min(1., smd.d50/dmin); // Abminderung auf Grund zu kleiner Koerner
            
            // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
            smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);

            smd.bedloadVector[0] = smd.bedload * sfx / sf;
            smd.bedloadVector[1] = smd.bedload * sfy / sf;
        }

        return smd.bedloadVector;
    }
}
