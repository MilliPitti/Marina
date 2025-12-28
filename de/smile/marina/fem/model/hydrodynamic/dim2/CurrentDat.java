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
import de.smile.marina.io.SmileIO;


/**
 * @author Peter Milbradt
 * @version 3.15.7
 */
public class CurrentDat{
    
    public String referenceDate = "1970-01-01 00:00:00 UTC+1"; // Reference date [yyyy-MM-dd HH:mm:ss z]
    public int epsgCode = -1;
    
    public BoundaryConditionsReader rndwerteReader=null;
    public String rndwerte_name = null;
    public String xferg_name = "currenterg2d.bin";
    public String current3d_erg_name = "current3d.bin";
    
   // public enum SysDatFileType {JanetBin, SystemDat};
    public enum BottomFriction {ManningStrickler, Nikuradse}
    public BottomFriction bottomFriction = BottomFriction.ManningStrickler;
    public String strickler_name = null;
    public SmileIO.MeshFileType stricklerFileType = SmileIO.MeshFileType.SystemDat;
    public double constantStrickler = 48.;

    public String nikuradse_name = null;
    public SmileIO.MeshFileType nikuradseFileType = SmileIO.MeshFileType.SystemDat;
    public double constantNikuradse = 13.;
    
    public enum WeirFileType {weirXML};
    public String weirsFileName = null;
    public WeirFileType weirsFileType;
     
    public String waterlevel_name = null; // ascii-file from type system.dat with initial waterlevel
    public SmileIO.MeshFileType waterLevelFileType = SmileIO.MeshFileType.SystemDat;
    
    public String startWerteDatei=null;  // currenterg-file with start results
    public int startSatz=0;
    
    public String startWerte3DDatei=null;  // current3Derg-file with start results
    public int start3DSatz=0;
    
    public double watt = 0.01;
    public double infiltrationRate = 0.; // default value 1.e-5 for fine sand
    
    public int NumberOfThreads =2;
}
