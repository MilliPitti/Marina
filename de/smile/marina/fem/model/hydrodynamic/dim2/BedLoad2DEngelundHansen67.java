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
public class BedLoad2DEngelundHansen67 implements BedLoad2DFormulation {
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
     * Originalformulierung mit Welleneinfluss und 
     * drehen der Transportvektoren in Richtung der Bodenschubspannungen
     * @param dof
     * @return Vektor des Sedimenttransportes
     * Die Transportformel ist geeignet fuer
     *  Sohlneigung: 0.005 bis 19 poMill
     *  Korngroeszen: 0.19 bis 0.98 [mm]
     *  Mittlere Flieszgeschwindigkeiten: 0.2 bis 1.9 m/s
     */
    @Override
    public double[] getLoadVector/*OriginalMitDrehenInRichtung_tauB und_Seegang*/(DOF dof) {
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        final CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double tauB = Function.norm(cmd.tauBx, cmd.tauBy);        
        if(tauB<1e-10) return smd.bedloadVector;
        
        WaveHYPModel2DData wmd = WaveHYPModel2DData.extract(dof);
        if (wmd != null) {
            tauB += cmd.rho * PhysicalParameters.G / Function.max(Function.sqr(5.), Math.pow((cmd.totaldepth < CurrentModel2D.WATT) ? CurrentModel2D.WATT : cmd.totaldepth, 1. / 3.) * Function.sqr(Function.min(cmd.kst, CurrentModel2DData.Nikuradse2Strickler(2.5 * smd.d50)))) * wmd.bottomvelocity * wmd.bottomvelocity;
        }
        final double T = (tauB - smd.tau_cr)/smd.tau_cr;
        
        // Chollet-Cunge-ähnlicher Korrekturfaktor
        final double f_CC = computeFCC(T);

        // Engelund-Hansen-Vorfaktor mit CC-Korrektur
        double prefactor = 0.05 * f_CC;

        smd.bedload = prefactor * cmd.rho * cmd.cv * cmd.cv / tauB
                * Math.pow(tauB / ((PhysicalParameters.RHO_SEDIM - cmd.rho) * PhysicalParameters.G * smd.d50),2.5)
                * PhysicalParameters.RHO_SEDIM * PhysicalParameters.G 
                * Math.sqrt((PhysicalParameters.RHO_SEDIM/cmd.rho-1.) * PhysicalParameters.G * Math.pow(smd.d50, 3.));
        smd.bedload *= smd.lambda;
        smd.bedload /= PhysicalParameters.RHO_SEDIM * PhysicalParameters.G;
          
        // Extrahieren des Bedload-Anteils durch Engelund-Fredsøe-Partitionierung (1976)
        if(T>0){
            //Suspension multiplier
            double R = 0.3/Math.pow(T,1.5);

            smd.bedload *= 1 / (1+R); // bedload = q_total * 1 / (1+R);
            // c = q_total * R / (1+R) / totaldepth
        }else{
            smd.bedload = 0.;
            return smd.bedloadVector;
        }
        // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
        smd.bedload = Math.min(cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);
        
        double norm = Function.norm(cmd.tauBx, cmd.tauBy);
        smd.bedloadVector[0] = smd.bedload * cmd.tauBx / norm;
        smd.bedloadVector[1] = smd.bedload * cmd.tauBy / norm;
        
        return smd.bedloadVector;
    }

    /** Chollet-Cunge-ähnlicher Korrekturfaktor für Engelund-Hansen-Formel
     * Abhängigkeit vom Shields-Parameter T = (tauB - tau_cr) / tau_cr
     * @param T Shields-Parameter
     * @return Korrekturfaktor f_CC
     */
    private double computeFCC(final double T) {
        if (T <= 0.0) {
            return 0.0;
        }

        // Referenzbereich für "optimales" Regime (hier: T_ref ~ 3)
        final double logT = Math.log10(T);
        final double logT_ref = Math.log10(3.0);   // Zentrum der Glocke
        final double sigma = 0.5;                  // Breite der Glocke (in log10-Raum)

        // Gauss-artige Funktion: max ~1 bei T ~ T_ref, kleiner bei sehr kleinen / großen T
        double fCC = Math.exp(-Math.pow((logT - logT_ref) / sigma, 2.0));

        // Optional: Minimalwert begrenzen, damit EH nicht komplett "weg" ist
        final double fCC_min = 0.2;
        if (fCC < fCC_min) {
            fCC = fCC_min;
        }

        return fCC;
    }
}
