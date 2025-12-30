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

import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/**
 *
 * @author milbradt
 */
public class GroundWater2DData implements ModelData {

    private static int id = NO_MODEL_DATA;
    double h, dhdt;       // gound water level or GW-Drucklinie bei gespannten  (nach oben positiv von NN)
    double nu, nv;        // new velocity
    double u, v;        // velocity
    double zG = 10.;     // impermeable Layer     (positiv nach unten von NN)
    double upperImpermeableLayer = Double.NEGATIVE_INFINITY;  // upper impermeable Layer     (positiv nach unten von NN)
    double source;      // quellterm des Grundwassermodells zur kopplung mit dem Oberflaechenmodell
    ScalarFunction1d bh = null;
    double kf = 0.0001;    //hydr. Durchlaessigkeit, permeability // mittlerer Sand
    double S0 = 0.25;     //effektive Porositaet

    /** Creates a new instance of GroundWater2DData */
    public GroundWater2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static GroundWater2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof GroundWater2DData) {
                    id = dof.getIndexOf(md);
                    return (GroundWater2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (GroundWater2DData) dof.getModelData(id);
        }
        return null;
    }
}
