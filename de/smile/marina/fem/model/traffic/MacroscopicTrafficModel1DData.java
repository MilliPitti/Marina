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
package de.smile.marina.fem.model.traffic;
import bijava.math.ifunction.*;
import de.smile.marina.fem.ModelData;

/** 
 *
 * @author  Peter Milbradt
 * @version 
 */
public class MacroscopicTrafficModel1DData implements ModelData {
  
  // Zustandsgroessen
  double v; double dvdt; double dvdx;
  double rho; double drhodt;
  
  // Ergebnisvector
  double rv;
  double rrho;
    
  // boudary conditions
  ScalarFunction1d bv=null;
  ScalarFunction1d brho=null;
  
}
