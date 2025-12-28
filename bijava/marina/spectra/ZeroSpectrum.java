package bijava.marina.spectra;

/** Spectrum is everywhere zero*/
public class ZeroSpectrum implements Spectrum1D {
    private double    fp;	//..peak frequency
    private double    h1_3;	//..
    private double    t1_3;	//..
      
    /** Constructor */
    public ZeroSpectrum() {
    }
    
    /** Returns the peak frequency of the spectrum. */
    public double getPeakFrequency() {
	return 0.;
    }
    
    /** Returns the spectrum value at the freuqency f. */
    public double getValue( double f ) {
	return 0.;
    }
}