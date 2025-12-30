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
 * EHWS kombiniert die Transportformel von Wiberg&Smith 1989  mit der von Engelund&Hansen 1967
 * Wiberg&Smith hat sich fuer Rhein und Donau als geeignet Formel erwiesen, erzeugt aber fuer kleine Korngroeszen zu hohe Transporte
 * Engelund&Hansen bestimmt gute Transportmenegen fuer kleine Sedimente wie an der Havel
 * @author Peter Milbradt
 * @version 2.9.0
 */
public class BedLoad2DEHWS implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 
    0.15mm < d50 < 2mm mehr Egelund&Hansen
    0.4mm < d50 < 29mm mehr Wiberg&Smith */
    private final static double dmin = 0.15E-3;
    @Override
    public String toString() {
        return "EHWS (based on CSF)";
    }
    
    /** 
     * @param dof
     * @return  */
    @Override
    public double[] getLoadVector(DOF dof) {

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        if ((smd.d50 - dmin/2.)<=0.) return smd.bedloadVector;
        
        final double lambdaEH = Function.max(0., (4.E-3 - smd.d50)/4.E-3);
        final double lambdaWS = 1.-lambdaEH;
        
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        final double S = Function.norm(cmd.dhdx, cmd.dhdy, 1.);
        
        final double CSF = smd.CSF; // variable // Bodenneigungsanteil wird schon bei der Berechnung des kritischen Shieldsspannung in SedimentModel2DData beruecksichtigt

        double sfx = cmd.tauBx / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = cmd.tauBy / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        
        double sf = Function.norm(sfx, sfy);
        
        WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
        double sfwave = 0.;
        if (wmd != null) {
            sfwave = (cmd.rho * PhysicalParameters.G / Function.max(Function.sqr(5.), Math.pow((cmd.totaldepth < CurrentModel2D.halfWATT) ? CurrentModel2D.halfWATT : cmd.totaldepth, 1. / 3.) * Function.sqr(Function.min(cmd.kst, CurrentModel2DData.Nikuradse2Strickler(2.5 * smd.d50)))) * wmd.bottomvelocity) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        }

        if ((sf + sfwave) > CSF) {
            smd.bedload = lambdaWS * (1.6 * Math.max(0, Math.log((sf + sfwave)) + 9.8)) * Math.sqrt((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER * PhysicalParameters.G * Math.pow(smd.d50, 3.) * Math.pow((sf + sfwave) - CSF, 3.))
                    / Math.max(CSF, sf);
        }
        smd.bedload += lambdaEH * 0.1 * (sf + sfwave) * Math.sqrt((sf + sfwave)) * PhysicalParameters.RHO_SEDIM * cmd.cv * cmd.wlambda * cmd.cv * cmd.wlambda * Math.sqrt((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER * PhysicalParameters.G * Math.pow(smd.d50, 3.)) / (2. * PhysicalParameters.G /* Math.max(0.1, cmd.totaldepth)*/ * S); // Peter 18.08.2016

        smd.bedload *= smd.lambda; // decreasing depending on not erodible bottom
        smd.bedload *= Function.min(1., Function.max(Function.max(0,smd.d50) / (4.*dmin),Function.max(0,smd.d50 - dmin/2.) / (2. * dmin))); // Abminderung auf Grund zu kleiner Koerner
        
        smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv * Function.max(0.,smd.zh - smd.z)*(1. - smd.porosity)/Math.max(sf, CSF), smd.bedload);

        // dirctional bed load
        smd.bedloadVector[0] = smd.bedload * sfx ;
        smd.bedloadVector[1] = smd.bedload * sfy;
        smd.bedload = Function.norm(smd.bedloadVector[0], smd.bedloadVector[1]);

        return smd.bedloadVector;
    }
}
