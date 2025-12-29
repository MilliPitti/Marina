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
import de.smile.marina.io.SmileIO.MeshFileType;

/**
 *
 * @author milbradt
 */
public class GroundWaterDat {
    
    public BoundaryConditionsReader rndwerteReader=null;
    public int NumberOfThreads=1;

    public String rndwerte_name=null;

    public String xferg_name = "groundwatererg.bin";

//    public String impermeable_Layer_name = "impLayer.dat";
//    public String startGroundWaterLevel_name = "startGroundWaterLevel.dat";
    
    public String waterlevel_name = null; // initial goundwaterlevel
    public SmileIO.MeshFileType waterLevelFileType = SmileIO.MeshFileType.SystemDat;
    
    public double standardPermeability = 0.001;// Standardwert fuer Permeabilitaet
    public String permeability_name = null; //  permeability
    public SmileIO.MeshFileType permeability_FileType = SmileIO.MeshFileType.SystemDat;
    
    public double standardImpermeability = 10.0;// Standardwert fuer Impermeabilitaetslayer
    public String impermeability_name = null; // impermeability Layer
    public SmileIO.MeshFileType impermeability_FileType = SmileIO.MeshFileType.SystemDat;
    
    public double standartUpperImpermeabilityThickness = Double.NEGATIVE_INFINITY;
    public String upperImpermeabilityThickness_name = null; // impermeability Layer
    public SmileIO.MeshFileType upperImpermeabilityThickness_FileType = SmileIO.MeshFileType.SystemDat;

    public MeshFileType surfaceWater_FileType;
    public String surfaceWater_name = null;
    
    public String startWerteDatei=null;  // currenterg-file with start results
    public int startSatz=0;
        
    /** Creates a new instance of GroundWaterDat */
    public GroundWaterDat() {
    }
    
}
