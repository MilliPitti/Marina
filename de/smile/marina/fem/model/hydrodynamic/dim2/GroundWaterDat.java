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
