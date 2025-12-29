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

import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** Element ModelDatas for shallow water equations
 * @version 3.18.0
 * @author Peter Milbradt
 */
public class Current2DElementData implements ModelData{
    
    private static int id = NO_MODEL_DATA;
    private static final long serialVersionUID = 1L;
    
    public double astx, asty;
    
    public double u_mean, v_mean, depth_mean;
    public double deepestTotalDepth;
    
    public double meanStricklerCoefficient=48.;
    
    public double dudx; public double dudy;
    public double dvdx; public double dvdy;
    public double dhdx; public double dhdy;
    
    public double ddepthdx; public double ddepthdy;

    public boolean isDry = false; // indicator for dry elements
    public int iwatt; // number of dry falling nodes
    public double wlambda; // elementprodukt wlambda - beschreibt den Grad des trockenfallens eines Elements
    
    public double elementsize;  // elemtsize depending to the shallow water flow
    public boolean withWeir; // indikator, element hat einen Knoten, der zu einem Wehr gehoert
    
    public Current2DElementData(){
        id = SEARCH_MODEL_DATA;
    }
    
    /**
     * extrahiert die Current2DElementData des FElements
     *
     * @param ele
     * @return
     */
    public static Current2DElementData extract(FElement ele) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = ele.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof Current2DElementData current2DElementData) {
                    id = ele.modelData.indexOf(md);
                    return current2DElementData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (Current2DElementData) ele.modelData.get(id);
        }
        return null;
    }
}
