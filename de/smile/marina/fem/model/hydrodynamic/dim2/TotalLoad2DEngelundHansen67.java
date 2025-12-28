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
 * Totalload-Transportformulation by Engelund und Hansen (1967) 
 * Die Transportformel nach Engelund-Hansen basiert auf Energiebetrachtungen und
 * berechnet hinreichend genau den Totalen Transport fuer sandige Fluesse mit
 * betraechtlichem Schwebstofftransport. 
 * Die Transportformel ist geeignet fuer
 * Sohlneigung: 0.005 bis 19 poMill 
 * Korngroeszen: 0.19 bis 0.98 [mm] 
 * Mittlere Flieszgeschwindigkeiten: 0.2 bis 1.9 m/s
 *
 * @author Peter Milbradt
 * @version 4.5.2
 */
public class TotalLoad2DEngelundHansen67 implements TotalLoad2DFormulation {
    /* Gueltigkeitsbereich 0.15mm < d50 < 1mm */
    private final static double dmin = 0.15E-3;
    private final static double dminTenth = dmin / 10.;
    @Override
    public String toString() {
        return "Engelund & Hansen 1967 (not using CSF)";
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
        
        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double tauB = Function.norm(cmd.tauBx, cmd.tauBy);
        if(tauB<1e-10) return smd.bedloadVector;
        
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
        smd.bedload = Math.min(Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);
        
        smd.bedloadVector[0] = smd.bedload * cmd.u;
        smd.bedloadVector[1] = smd.bedload * cmd.v;
        smd.bedload *= cmd.cv;
        
        return smd.bedloadVector;
    }
    
    /** Die Transportformel nach Engelund-Hansen basiert auf Energiebetrachtungen
     * und berechnet hinreichend genau den totalen Transport fuer sandige Fluesse mit betraechtlichem Schwebstofftransport. 
     * (Engelund & Hansen, 1967)
     * Originalformulierung mit 
     * Welleneinfluss und 
     * drehen der Transportvektoren in Richtung der Bodenschubspannungen
     * @param dof
     * @return Vektor des Sedimenttransportes
     * Die Transportformel ist geeignet fuer
     *  Sohlneigung: 0.005 bis 19 poMill
     *  Korngroeszen: 0.19 bis 0.98 [mm]
     *  Mittlere Flieszgeschwindigkeiten: 0.2 bis 1.9 m/s
     */
    @Override
    public double[] getTotalLoadVector/*OriginalMitDrehenInRichtung_tauB und_Seegang*/(DOF dof) {
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        double totalLoad;
        double[] totalLoadVector = new double[2];
        
        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double tauB = Function.norm(cmd.tauBx, cmd.tauBy);        
        if(tauB<1e-10) return smd.bedloadVector;
        
        WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
        if (wmd != null) {
            tauB += cmd.rho * PhysicalParameters.G / Function.max(Function.sqr(5.), Math.pow((cmd.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT : cmd.totaldepth, 1. / 3.) * Function.sqr(Function.min(cmd.kst, CurrentModel2DData.Nikuradse2Strickler(2.5 * smd.d50)))) * wmd.bottomvelocity * wmd.bottomvelocity;
        }
        
        totalLoad = 0.05 * cmd.rho * cmd.cv * cmd.cv / tauB 
                * Math.pow(tauB / ((PhysicalParameters.RHO_SEDIM - cmd.rho) * PhysicalParameters.G * smd.d50),2.5)
                * PhysicalParameters.RHO_SEDIM * PhysicalParameters.G 
                * Math.sqrt((PhysicalParameters.RHO_SEDIM/cmd.rho-1.) * PhysicalParameters.G * Math.pow(smd.d50, 3.));
        totalLoad *= smd.lambda;
        totalLoad /= PhysicalParameters.RHO_SEDIM * PhysicalParameters.G;
        
        if (smd.d50 < dmin) {// Abminderung auf Grund zu kleiner Koerner
            totalLoad *= Math.max(0., (smd.d50 - dminTenth) / (dmin - dminTenth));
        }
        // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
        totalLoad = Math.min(cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), totalLoad);
        
        double norm = Function.norm(cmd.tauBx, cmd.tauBy);
        totalLoadVector[0] = totalLoad * cmd.tauBx / norm;
        totalLoadVector[1] = totalLoad * cmd.tauBy / norm;
        
        return totalLoadVector;
    }
}
