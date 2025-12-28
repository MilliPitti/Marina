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
package de.smile.marina.fdm.model.ecological;

import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.fem.ModelData;
import static de.smile.marina.fem.ModelData.NO_MODEL_DATA;

/**
 *
 * @author milbradt
 */
@SuppressWarnings("unused")
public class MusselData implements ModelData {
    private static int id = NO_MODEL_DATA;

    enum MusselSpecies {
        Austern, Miesmuscheln;

        double wachstumsrateBiomasse;
        double maxBiomasseProMuschel;

        double groeszenWachstumsrate;
        double maxDiameter;

        double sterbeRate;

        double ausbreiteRadiusDerLarven;
    }

    MusselSpecies species;

    double biomass = 0; // kg/m**2
    double covering = 0.; // 0 - 1
    int abundanz = 0; // Individuen/m**2 - was ist die maximale Abundanz fuer Miesmuschel und Austern?
    // !! meanSizeDistributionCumulativeCurve of mussels in the representativ area;
    public ScalarFunction1d meanSizeDistributionCumulativeCurveLiving = null; // Lebend MuschelgroeszenKurve in mm
                                                                              // verwendet
    public ScalarFunction1d meanSizeDistributionCumulativeCurveDeath = null; // Tod MuschelgroeszenKurve in mm verwendet
    // !! parametrisiert beschrieben
    double minSize;
    double d50Size;
    double maxSize;
    double sorting;
    double skewness;

}
