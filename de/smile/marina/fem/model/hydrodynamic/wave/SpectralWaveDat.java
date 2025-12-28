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
