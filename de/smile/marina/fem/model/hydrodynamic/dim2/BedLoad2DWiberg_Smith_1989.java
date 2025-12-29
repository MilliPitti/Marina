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
 * Wiberg and Smith (1989) Generalisierung der Transportformel von Meyer-Peter
 * und Mueller fuer ein breites Spektrum von Transportregimen
 * Die Transportformel ist geeignet fuer
 * Sohlneigung: 0.4 bis 20 poMill 
 * Korngroeszen: 0.4 bis 30 [mm] 
 * Mittlere Flieszgeschwindigkeiten: 0.37 bis 2.87 m/s
 *
 * @author Peter Milbradt
 * @version 2.10.1
 */
public class BedLoad2DWiberg_Smith_1989 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.4mm < d50 < 29mm */

    private final static double dmin = 0.4E-3;

    @Override
    public String toString() {
        return "Wiberg and Smith (1989) (based on CSF)";
    }

    /**
     *
     * @param dof
     * @return
     */
    @Override
    public double[] getLoadVector(DOF dof) {

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        if(smd.lambda < Double.MIN_NORMAL) return smd.bedloadVector;

        final double CSF = smd.CSF; // variable // Bodenneigungsanteil wird schon bei der Berechnung des kritischen Shieldsspannung in SedimentModel2DData beruecksichtigt

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        
        final double wlambda = Function.min(1., cmd.totaldepth / .1);
       

        double sfx = smd.lambda * cmd.tauBx * wlambda / ((PhysicalParameters.RHO_SEDIM - cmd.rho) * PhysicalParameters.G * smd.d50);
        double sfy = smd.lambda * cmd.tauBy * wlambda / ((PhysicalParameters.RHO_SEDIM - cmd.rho) * PhysicalParameters.G * smd.d50);

        double sf = Function.norm(sfx, sfy);

//        WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
        double sfwave = 0.;
//        if (wmd != null) {
////            sfwave = (cmd.rho * PhysicalParameters.G / Function.max(Function.sqr(5.), Math.pow((cmd.totaldepth < CurrentModel2D.halfWATT) ? CurrentModel2D.halfWATT : cmd.totaldepth, 1. / 3.) * Function.sqr(Function.min(cmd.kst, CurrentModel2DData.Nikuradse2Strickler(2.5 * smd.d50)))) * wmd.bottomvelocity) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
//            sfwave = smd.lambda * PhysicalParameters.RHO_WATER * wmd.bfcoeff * wmd.bottomvelocity * wlambda / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50); // Peter 26.09.2019
//        }

        if ((sf + sfwave) > CSF) {
            smd.bedload = (1.6 * Math.max(0, Math.log((sf + sfwave)) + 9.8)) * Math.sqrt((PhysicalParameters.RHO_SEDIM - cmd.rho) / cmd.rho * PhysicalParameters.G * Math.pow(smd.d50, 3.) * Math.pow((sf + sfwave) - CSF, 3.));
            
            smd.bedload *= Function.min(1., smd.d50/dmin/2); // Abminderung auf Grund zu kleiner Koerner, beginnend bei 2*dmin
            // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
            smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);

            // dirctional bed load
            smd.bedloadVector[0] = smd.bedload / sf * sfx;
            smd.bedloadVector[1] = smd.bedload / sf * sfy;
            smd.bedload = Function.norm(smd.bedloadVector[0], smd.bedloadVector[1]);
        }

        return smd.bedloadVector;
    }
}
