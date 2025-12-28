/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2021

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
package de.smile.marina.fem.model.ecological;

import de.smile.marina.fem.ModelData;
import de.smile.math.function.ScalarFunction1d;

/**
 *
 * @author Peter Milbradt
 */
public abstract class Mussel2DData implements ModelData {

    public MusselSpecies species;
    
    double density=0.; // zwischen 0 und 1
    double d50 = 0.; // [m]
    
    double bioMass = 0.; // [g/m**2] biomasse pro quadratmeter
    double dbmdt = 0.; // zeitliche Veraenderung der biomasse pro quadratmeter und Zeit [g/m**2 / s]
    double rbioMass = 0.;
    
    protected static double consumptionRateOfAlgae = 1.; // c is the maximum consumption rate of algae by mussels

    double mortalityRateIncreasingFactor = 1.;
    double growthRateDecreasingFactor = 1.;

    static double maxTemperature = 30.;
    static double optimalTemperature = 10.;
    static double minTemperature = 0.;
    
    double temperature = optimalTemperature; // mean water temperature of the north sea [grad C]

    double bottomShareStress = 0.; //   [N/m**2]    max. 1.3 

    double waveBreakingEnergy = 0.; //  [W/m**2]    max. 0.25 W/m**2
    double orbitalVelocity = 0.; //     [m/s]       max. 0.5 m/s

    double dzdt = 0.; // erosion / sedimentation-Rate in depth (Erosion dzdt>0, Sedimentation dzdt<0); -0.2 bis +0.7 m/a

    double algaConcentration = 1.; // [g/l] algal concentrations between 0.68 and 1.80 g/m**3

    public double getMaxBioMass() {
        return species.maxBioMassPerSqrMeter;
    }

    public double getGrowthRate() {
        return species.growthRate;
    }

    public double getMortalityRate() {
        return species.mortalityRate;
    }

}
