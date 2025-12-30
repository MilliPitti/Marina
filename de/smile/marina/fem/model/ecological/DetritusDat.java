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
 * @author abuabed/milbradt
 */
public class DetritusDat { 
    public String detrit_rndwerteName = null;
    public String xferg_name = "detrituserg.bin";
    
    public int numberOfThreads = 1;
    public double watt = 0.01;
    
       public String startWerteDatei=null;  // erg-file with start results
    public int startSatz=0;
    
    public String concentration_name = null; // ascii-file from type system.dat with initial waterlevel
    public SmileIO.MeshFileType concentrationFileType = SmileIO.MeshFileType.SystemDat;
    
//    public static double PHYTOPLANKTON_MORTAL_RATE = 0.02/(24*60*60);//[1/d]/(24*60*60)--->[1/sec]
//    public static double ZOOPLANKTON_MORTAL_RATE = 0.02/(24*60*60);//[1/d]/(24*60*60)--->[1/sec]
    public static double DETRITUS_MINERALIZATION_RATE = 0.0058;//(24*60*60);//[1/d]/(24*60*60)--->[1/sec]
    
    /** Creates a new instance of DetritusDat */
    public DetritusDat() {
    }
    
}
