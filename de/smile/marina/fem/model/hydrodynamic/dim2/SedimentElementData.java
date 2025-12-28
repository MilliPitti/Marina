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

import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** Element ModelDatas for morphodynamik equations
 * @version 4.3.1
 * @author Peter Milbradt
 */
public class SedimentElementData implements ModelData {

    private static int id = NO_MODEL_DATA;
    private static final long serialVersionUID = 1L;
    public double dzdx, dzdy;
    public double bottomslope;
    
    public SedimentElementData() {
        id = SEARCH_MODEL_DATA;
    }

    /** extrahiert die ElementCurrent2DData des FElements
     * @param ele
     * @return  */
    public static SedimentElementData extract(FElement ele) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = ele.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof SedimentElementData sedimentElementData) {
                    id = ele.modelData.indexOf(md);
                    return sedimentElementData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (SedimentElementData) ele.modelData.get(id);
        }
        return null;
    }
}
