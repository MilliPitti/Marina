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
