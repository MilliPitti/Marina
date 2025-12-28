package bijava.marina.spectra;

/** Gaussian shaped spectrum */
public class GaussianShaped implements Spectrum1D {
    private double    fp;	//..peak frequency
    private double    hs;	//..significant wave height;
    private double    sigmaS;	//..derivation
      
    /** Constructor: expects H1/3, sigmaS(deriavtion) and the peak frequency. */
    public GaussianShaped( double hs, double sigmaS, double fp ) {
	this.hs	    = hs;
	this.sigmaS = sigmaS;
	this.fp	    = fp;
    }
    
    /** Returns the peak frequency of the spectrum. */
    public double getPeakFrequency() {
	return fp;
    }
    
    /** Returns the spectrum value at the freuqency f. */
    public double getValue( double f ) {
	double factor, exponent;
        
	factor  = hs*hs/16./(Math.sqrt(2.*Math.PI)*sigmaS);
	exponent= -1.*(f-fp)*(f-fp)/(2.*sigmaS*sigmaS);
    
	return factor*Math.exp(exponent);
   }
}