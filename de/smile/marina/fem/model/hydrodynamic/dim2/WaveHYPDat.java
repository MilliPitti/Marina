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
package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.io.BoundaryConditionsReader;
/**
 *
 * @author  Peter Milbradt
 * @version 3.15.7
 */
public class WaveHYPDat {

    public String referenceDate = "1970-01-01 00:00:00 UTC+1"; // Reference date [yyyy-MM-dd HH:mm:ss z]
    
    public BoundaryConditionsReader rndwerteReader=null;
    public String randn_name = null;
    public String xferg_name = "waveerg.bin";
    public double watt = 0.1;
    
    public String startWerteDatei = null;
    public int startSatz = 0;
    
    public double waterLevelOffSet = 0.;
    
    public int NumberOfThreads =1;
    
}
