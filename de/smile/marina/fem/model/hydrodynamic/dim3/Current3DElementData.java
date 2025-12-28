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
package de.smile.marina.fem.model.hydrodynamic.dim3;

import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import static de.smile.marina.fem.ModelData.NO_MODEL_DATA;
import java.util.Iterator;

/**
 *
 * @author milbradt
 */
public class Current3DElementData implements ModelData{
    
    private static int id = NO_MODEL_DATA;
    public double contiResiduum = 0.;
    
    public Current3DElementData(){
        id = SEARCH_MODEL_DATA;
    }
    
    /**
     * extrahiert die Current2DElementData des FElements
     *
     * @param ele
     * @return
     */
    public static Current3DElementData extract(FElement ele) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = ele.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof Current3DElementData current3DElementData) {
                    id = ele.modelData.indexOf(md);
                    return current3DElementData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (Current3DElementData) ele.modelData.get(id);
        }
        return null;
    }
    
}
