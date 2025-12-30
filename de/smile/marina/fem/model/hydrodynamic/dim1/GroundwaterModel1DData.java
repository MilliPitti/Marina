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
package de.smile.marina.fem.model.hydrodynamic.dim1;

import bijava.math.ifunction.*;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** Current1DModelData.java
 *
 * @author  Peter Milbradt
 * @version 
 */
public class GroundwaterModel1DData implements ModelData {

    // Zustandsgroessen
    double h;
    double dhdt;
    double dhdx;
    double zG = 10.; //Tiefe der impermeability layer
    double kf = 0.001;   //hydr. Durchlaessigkeit
    double S0 = 0.25;   //Porositaet 
    // Ergebnisvector
    double rh;
    // boudary conditions
    ScalarFunction1d bh = null;

    /** Creates new Current1DModelData */
    public GroundwaterModel1DData() {

    }

    @Override
    public GroundwaterModel1DData clone() {
        GroundwaterModel1DData r = new GroundwaterModel1DData();
        r.h = h;
        return r;
    }

    /** erzeugt neue Modelldaten und initalisiert diese mit 0 */
    public ModelData initialNew() {
        return new GroundwaterModel1DData();
    }

    /**  addieren als Voraussetung bei der Interpolation */
    public ModelData add(ModelData md) {
        GroundwaterModel1DData r = null;
        if (md instanceof GroundwaterModel1DData) {
            GroundwaterModel1DData m = (GroundwaterModel1DData) md;
            r = this.clone();
            r.h += m.h;


        }
        return r;
    }

    /** skalar multiplizieren  als Voraussetung bei der Interpolation */
    public ModelData mult(double scalar) {
        GroundwaterModel1DData r = this.clone();
        r.h *= scalar;


        return r;
    }

    /** extrahiert die CurrentModel1DData eines DOF
     * @param dof
     * @return  */
    public static GroundwaterModel1DData extract(DOF dof) {
        Iterator<ModelData> modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = modeldatas.next();
            if (md instanceof GroundwaterModel1DData) {
                return (GroundwaterModel1DData) md;
            }
        }
        return null;
    }
}
