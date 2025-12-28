package de.smile.marina.fem.model.ecological;

import de.smile.marina.io.SmileIO;


/**
 *
 * @author milbradt
 */
public class SpartinaAlternifloraModel2DDat {
    
    public String xferg_name = "spartinaAlternifloraerg.bin";
    
    public int numberOfThreads = 1;
    
    public String startWerteDatei=null;  // erg-file with start results
    public int startSatz=0;
    
    public String desity_name = null; // ascii-file from type system.dat with initial density
    public SmileIO.MeshFileType densityFileType = SmileIO.MeshFileType.SystemDat;

    public double startDensity = Double.NaN;
}
