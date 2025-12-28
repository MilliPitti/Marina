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
