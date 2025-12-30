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
package de.smile.marina.fem.model.meteorology;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/**
 * @author milbradt
 */
public class MeteorologyData2D implements ModelData {

    private static int id = NO_MODEL_DATA;
    public double windx,  windy,  windspeed;
    public double temperature = 15.;  // standard Temperatur der Luft in Grad
    public double pressure;
    public double insolation;

    /** Creates a new instance of MeteorologyData2D */
    public MeteorologyData2D() {
        id = SEARCH_MODEL_DATA;
    }

    public static MeteorologyData2D extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof MeteorologyData2D) {
                    id = dof.getIndexOf(md);
                    return (MeteorologyData2D) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (MeteorologyData2D) dof.getModelData(id);
        }
        return null;
    }
}
