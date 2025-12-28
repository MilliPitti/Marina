package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.io.BoundaryConditionsReader;
import de.smile.marina.io.SmileIO;

public class HeatTransportDat {
    
    public BoundaryConditionsReader rndwerteReader=null;
    public String temperaturerndwerte_name = null;
    public String xferg_name = "temperatureerg.bin";
    
    public String temperature_name = null; // file-name with initial temperature
    public SmileIO.MeshFileType temperatureFileType = SmileIO.MeshFileType.SystemDat;
    
    public int NumberOfThreads =1;
    
    public double airTemperature = Double.NaN;
}
