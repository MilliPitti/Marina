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

import bijava.math.ifunction.*;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;

/** @author Peter Milbradt
 * @version 1.7.18
 */
public class SaltModel2DData implements ModelData {

    private static int id = NO_MODEL_DATA;    // Zustandsgroeszen
    public double C;    // salinity in ppt
    double rsaltconc;
    // boudary conditions
    ScalarFunction1d bsc = null;
    ScalarFunction1d sourceQc = null;
    double dsaltconcdt;
    boolean extrapolate = false;
    double sourceSink;

    public SaltModel2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static SaltModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof SaltModel2DData saltModel2DData) {
                    id = dof.getIndexOf(md);
                    return saltModel2DData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (SaltModel2DData) dof.getModelData(id);
        }
        return null;
    }
}
