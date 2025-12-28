package bijava.marina.spectra;

import java.io.*;

public final class SpectraIO {

    /** Writes the discrete spectrum spec to the DataOutputStream os. */
    public static void writeSpectrum( DiscreteSpectrum2D spec, DataOutputStream os ) {
	try {
	    os.writeInt(spec.getFrequencyLength());
	    if (spec.fullRange())
		os.writeInt(spec.getDirectionLength()+1);
	    else
		os.writeInt(spec.getDirectionLength());
	    os.writeFloat((float)spec.getFrequencyMin());
	    os.writeFloat((float)spec.getFrequencyMax());
	    os.writeFloat((float)spec.getDirectionMin());
	    os.writeFloat((float)spec.getDirectionMax());
	    
	    for (int f=0; f<spec.getFrequencyLength(); f++)
		for (int d=0; d<spec.getDirectionLength(); d++)
		    os.writeFloat((float)spec.getValueAt(f,d)[2]);
	} catch (Exception e) {}
    }
    
    /** Returns a discrete spectrum read from DataInputStream is. */
    public static DiscreteSpectrum2D readSpectrum( DataInputStream is ) {
	DiscreteSpectrum2D spec = null;
	
	try {
	    spec = new DiscreteSpectrum2D(  is.readInt(), is.readInt(),
					    is.readFloat(), is.readFloat(),
					    is.readFloat(), is.readFloat() );
	    
	    for (int f=0; f<spec.getFrequencyLength(); f++)
		for (int d=0; d<spec.getDirectionLength(); d++)
		    spec.setValueAt(f, d, is.readFloat());
	} catch (Exception e) {;}
	
	return spec;
    }
}