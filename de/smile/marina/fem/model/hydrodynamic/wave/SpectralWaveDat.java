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
package de.smile.marina.fem.model.hydrodynamic.wave;

public class SpectralWaveDat {
    
    public String xferg_name = "wave.bin";
    public String specoutname = "specs.bin";
    public String boundNodes = "wavernd.dat";
    public String BSHdata = "BSH.dat";
    public String randn_name = "randn.dat";
    
    public boolean spectral = true;
    public String bcname = null;
    
    public int	frequencylength = 12;
    public int	directionlength = 36;
    public double 	frequenzminimum = 0.08;
    public double 	frequenzmaximum = 0.5;
    public double 	directionminimum = -180.;
    public double 	directionmaximum = 180.;
    
    public int NumberOfThreads = 1;
    
    public double watt = 0.1;
    
}
