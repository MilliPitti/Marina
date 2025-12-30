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

import de.smile.marina.io.BoundaryConditionsReader;
import de.smile.marina.io.SmileIO;
/**
 * 
 * @author milbradt
 * @version 2.7.16
 */
public class OxygenTransportDat {
    
    public BoundaryConditionsReader rndwerteReader=null;
    public String oxygenrndwerte_name = null;
    public String xferg_name = "oxygenerg.bin";
    
    public String initalValuesASCIIFile = null;
    
    public String concentration_name = null; // file-name with initial concentration
    public SmileIO.MeshFileType concentrationFileType = SmileIO.MeshFileType.SystemDat;
    
    public String initalValuesErgFile=null;
    public int startCounter=0;
    
    public int NumberOfThreads =1;
    public double waterTemperature = 10.; // Water Temperature if no HeatTransportModel exists
}
