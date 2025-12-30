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
 * Bedload-Transportformulation by Engelund und Hansen (1967) 
 * Die Transportformel nach Engelund-Hansen basiert auf Energiebetrachtungen und
 * berechnet hinreichend genaue Ergebnisse fuer sandige Fluesse mit
 * betraechtlichem Schwebstofftransport. 
 * Die Transportformel ist geeignet fuer
 * Sohlneigung: 0.005 bis 19 poMill 
 * Korngroeszen: 0.19 bis 0.98 [mm] 
 * Mittlere Flieszgeschwindigkeiten: 0.2 bis 1.9 m/s
 *
 * @author Peter Milbradt
 * @version 2.12
 */
public class BedLoad2DEngelundHansen implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.15mm < d50 < 1mm */
    private final static double dmin = 0.15E-3;
    private final static double dminTenth = dmin / 10.;
    @Override
    public String toString() {
        return "EngelundHansen (not using CSF)";
    }
    
    @Override
    public double[] getLoadVector/*2.8.6*/(DOF dof) {

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        if(smd.lambda < Double.MIN_NORMAL) return smd.bedloadVector;
        
        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        
        final double wlambda = Function.min(1., cmd.totaldepth / .1);

        double sfx = (smd.lambda * cmd.tauBx * wlambda) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = (smd.lambda * cmd.tauBy * wlambda) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);

        double sf = Function.norm(sfx, sfy);
        
        WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
        double sfwave = 0.;
        if (wmd != null) {
            sfwave = wlambda * (cmd.rho * PhysicalParameters.G / Function.max(Function.sqr(5.), Math.pow((cmd.totaldepth < CurrentModel2D.halfWATT) ? CurrentModel2D.halfWATT : cmd.totaldepth, 1. / 3.) * Function.sqr(Function.min(cmd.kst, CurrentModel2DData.Nikuradse2Strickler(2.5 * smd.d50)))) * wmd.bottomvelocity) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        }
        
        if((sf + sfwave)<Double.MIN_NORMAL*100.) return smd.bedloadVector;

        smd.bedload = 0.05 * (sf + sfwave) * Math.sqrt(sf + sfwave) * PhysicalParameters.RHO_SEDIM * cmd.cv * wlambda * cmd.cv * wlambda * Math.sqrt((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER * PhysicalParameters.G * Math.pow(smd.d50, 3.))
                * (1. / (1. - smd.porosity)) // Beruecksichtigung der Prositaet
                / (2.* PhysicalParameters.G /* Function.max(1., cmd.totaldepth)*/) // ++ warum hier durch 2 * G * depth geteilt wird ist mir nicht mehr klar! ++ // Peter 18.08.16 Wassertiefe auskommentiert
                ;

        // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
        smd.bedload = Math.min(Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);
            
        smd.bedloadVector[0] = smd.bedload * sfx;
        smd.bedloadVector[1] = smd.bedload * sfy;
        smd.bedload = Function.norm(smd.bedloadVector[0], smd.bedloadVector[1]);

        return smd.bedloadVector;
    }
    
    /** Die Transportformel nach Engelund-Hansen basiert auf Energiebetrachtungen
     * und berechnet hinreichend genaue Ergebnisse fuer sandige Fluesse mit betraechtlichem Schwebstofftransport. 
     * (Engelund & Hansen, 1967)
     * Die Transportformel wurde dahingehend angepasst, 
     * dass der Transport nach der Bodenschubspannung ausgerichtet wird
     * und nicht nur nach der tiefenintegrierten Geschwindigkeit
     * 
     * @param dof
     * @return Vektor des Sedimenttransportes [m3/(m*s)]
     */
    public double[] getLoadVector2_12(DOF dof) {

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        if(smd.lambda < Double.MIN_NORMAL) return smd.bedloadVector;
        
        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        
        final double wlambda = Function.min(1., cmd.totaldepth / .1);

        double sfx = smd.lambda * cmd.tauBx * wlambda / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = smd.lambda * cmd.tauBy * wlambda / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);

        double sf = Function.norm(sfx, sfy);
        
        if(sf<Double.MIN_NORMAL*100.) return smd.bedloadVector;
        
        WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
        double sfwave = 0.;
        if (wmd != null) {
            sfwave = wlambda * (cmd.rho * PhysicalParameters.G / Function.max(Function.sqr(5.), Math.pow((cmd.totaldepth < CurrentModel2D.halfWATT) ? CurrentModel2D.halfWATT : cmd.totaldepth, 1. / 3.) * Function.sqr(Function.min(cmd.kst, CurrentModel2DData.Nikuradse2Strickler(2.5 * smd.d50)))) * wmd.bottomvelocity) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        }

        smd.bedload = 0.05 * cmd.rho * cmd.cv * cmd.cv / Function.norm(cmd.tauBx, cmd.tauBy)
                * Math.pow(sf + sfwave,1.5)
                * Math.sqrt((PhysicalParameters.RHO_SEDIM - cmd.rho) / cmd.rho * PhysicalParameters.G * Math.pow(smd.d50, 3.))
                * wlambda   // Verringerung des Transports an tockenen Knoten
                ;
        
        if (smd.d50 < dmin) {// Abminderung auf Grund zu kleiner Koerner
            smd.bedload *= Math.max(0., (smd.d50 - dminTenth) / (dmin - dminTenth));
        }
        // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
        smd.bedload = Math.min(cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity)/sf, smd.bedload);

//         dirctional bed load
        smd.bedloadVector[0] = smd.bedload * sfx;
        smd.bedloadVector[1] = smd.bedload * sfy;
        smd.bedload *= sf;

        return smd.bedloadVector;
    }
    
    /** Die Transportformel nach Engelund-Hansen basiert auf Energiebetrachtungen
     * und berechnet hinreichend genaue Ergebnisse fuer sandige Fluesse mit betraechtlichem Schwebstofftransport. 
     * (Engelund & Hansen, 1967)
     * @param dof
     * @return Vektor des Sedimenttransportes
     * Die Transportformel ist geeignet fuer
     *  Sohlneigung: 0.005 bis 19 poMill
     *  Korngroeszen: 0.19 bis 0.98 [mm]
     *  Mittlere Flieszgeschwindigkeiten: 0.2 bis 1.9 m/s
     */
//    @Override
    public double[] getLoadVectorOriginal(DOF dof) {
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        if(smd.lambda < Double.MIN_NORMAL) return smd.bedloadVector;
        
        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double tauB = Function.norm(cmd.tauBx, cmd.tauBy);
        
        smd.bedload = 0.05 * cmd.rho * cmd.cv / tauB 
                * Math.pow(tauB / ((PhysicalParameters.RHO_SEDIM - cmd.rho) * PhysicalParameters.G * smd.d50),2.5)
                * PhysicalParameters.RHO_SEDIM * PhysicalParameters.G 
                * Math.sqrt((PhysicalParameters.RHO_SEDIM/cmd.rho-1.) * PhysicalParameters.G * Math.pow(smd.d50, 3.));
        smd.bedload *= smd.lambda;
        smd.bedload /= PhysicalParameters.RHO_SEDIM * PhysicalParameters.G;
        
        if (smd.d50 < dmin) {// Abminderung auf Grund zu kleiner Koerner
            smd.bedload *= Math.max(0., (smd.d50 - dminTenth) / (dmin - dminTenth));
        }
        // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
        smd.bedload = Math.min(cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);
        
        smd.bedloadVector[0] = smd.bedload * cmd.u;
        smd.bedloadVector[1] = smd.bedload * cmd.v;
        smd.bedload *= cmd.cv;
        
        return smd.bedloadVector;
    }
}
