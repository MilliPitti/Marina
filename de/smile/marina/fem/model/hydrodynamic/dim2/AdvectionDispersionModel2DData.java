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

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;
import bijava.math.ifunction.*;

/** @author Peter Milbradt
 * @version 2.1
 */
class AdvectionDispersionModel2DData implements ModelData {

    private static int id = NO_MODEL_DATA;    // Zustandsgroeszen
    double C;    // Ergebnisvector
    double radconc;

    public double sourceSink=0; // Quellen und Senken des Stoffgehaltes in mg/l/s
    
    // boudary conditions
    ScalarFunction1d bsc = null;
    ScalarFunction1d sourceQc = null;
    double dadconcdt;
    boolean extrapolate = false;

    public AdvectionDispersionModel2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static AdvectionDispersionModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof AdvectionDispersionModel2DData) {
                    id = dof.getIndexOf(md);
                    return (AdvectionDispersionModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (AdvectionDispersionModel2DData) dof.getModelData(id);
        }
        return null;
    }
}
