package de.smile.marina.fem.model.ecological;

import de.smile.marina.io.SmileIO;

    public class NitrogenDat {
  
    public String rndwerte_name = null;
    public String xferg_name = "nitraterg.bin";
    
    public String startWerteDatei=null;  // erg-file with start results
    public int startSatz=0;
    
    public double standardConc;
    
    public String concentration_name = null; // ascii-file from type system.dat with initial waterlevel
    public SmileIO.MeshFileType concentrationFileType = SmileIO.MeshFileType.SystemDat;
    
    public double watt = 0.01;
    
    public int NumberOfThreads =1;


    }
