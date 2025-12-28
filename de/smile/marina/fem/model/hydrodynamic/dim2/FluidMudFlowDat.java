package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.io.BoundaryConditionsReader;
import de.smile.marina.io.SmileIO;


/**
 * @author Peter Milbradt
 * @version 2.0.8
 */
public class FluidMudFlowDat 
{
    public BoundaryConditionsReader rndwerteReader=null;
    public String rndwerte_name = null;
    public String xferg_name = "fluidmuderg2d.bin";
     
    public String fluidmudlevel_name = null; // ascii-file from type system.dat with initial waterlevel
    public SmileIO.MeshFileType fluidmudLevelFileType = SmileIO.MeshFileType.SystemDat;

    public String density_name = null;
    public SmileIO.MeshFileType densityFileType = SmileIO.MeshFileType.SystemDat;
    public double constantDensity = 1065.;

    public String viscosity_name = null; 
    public SmileIO.MeshFileType viscosityFileType = SmileIO.MeshFileType.SystemDat;
    public double constantViscosity = 1.;
    
    public String startWerteDatei=null;  // fluidmuderg2d-file with start results
    public int startSatz=0;
    
    public int NumberOfThreads =2;
}
