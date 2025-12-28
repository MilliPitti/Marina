package bijava.marina.spectra;

/** Directional spectrum that is anywhere zero. */
public class ZeroDirectional extends DirectionalSpectrum {
    
    /** Constructor. */
    public ZeroDirectional() {
	this.spectrum	= null;
	this.spread_fkt	= null;
	this.para_fkt	= null;
	
	this.theta_mean = 0.;
    }
    
    /** Return zero. */
    public double getValue( double frequency, double direction ) {
	return 0.;
    }
}