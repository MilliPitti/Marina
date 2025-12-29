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

/** 
 *
 * @author  Peter Milbradt
 * @version 
 */
 public class SedimentModel1DData implements ModelData {
  
  // Zustandsgroessen
  double C; double dCdt; double dCdx;
  // Ergebnisvector
  double rC;
    
  // boudary conditions
  ScalarFunction1d bqx=null;
  ScalarFunction1d bu=null;
  ScalarFunction1d bh=null;
  
  /** extrahiert die CurrentModel1DData eines DOF
     * @param dof
     * @return  */
    public static SedimentModel1DData extract(DOF dof){
        Iterator<ModelData> modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = modeldatas.next();
            if(md instanceof SedimentModel1DData)
                return ( SedimentModel1DData )md;
        }
        return null;
    }
  
}
