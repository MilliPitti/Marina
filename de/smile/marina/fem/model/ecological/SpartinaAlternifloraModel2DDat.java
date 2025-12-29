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
package de.smile.marina.fem.model.ecological;

import de.smile.marina.io.SmileIO;


/**
 *
 * @author milbradt
 */
public class SpartinaAlternifloraModel2DDat {
    
    public String xferg_name = "spartinaAlternifloraerg.bin";
    
    public int numberOfThreads = 1;
    
    public String startWerteDatei=null;  // erg-file with start results
    public int startSatz=0;
    
    public String desity_name = null; // ascii-file from type system.dat with initial density
    public SmileIO.MeshFileType densityFileType = SmileIO.MeshFileType.SystemDat;

    public double startDensity = Double.NaN;
}
