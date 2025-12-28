package bijava.marina.spectra;

/** PiersonMoskowitz spectrum:
  * This one-dimensional characterizing the fully developed sea is given by Pierson and Moskowitz [1964].*/
public class PiersonMoskowitz implements Spectrum1D {
    private double    fp;	    //..peak frequency
    private double    g=9.81;	    //..
    private double    alpha=0.0081; //..
      
    /** Constructor: expects peak frequency. */
    public PiersonMoskowitz( double fp ) {
	this.fp = fp;
    }
    
    /** Returns the peak frequency of the spectrum. */
    public double getPeakFrequency() {
	return fp;
    }
    
    /** Returns the spectrum value at the freuqency f. */
    public double getValue( double f ) {
	double factor;
	 
	factor  = alpha*g*g*Math.pow(2.*Math.PI,-4.)*Math.pow(f, -5.);
    
	return (factor*Math.exp(-5./4.*Math.pow(f/fp, -4.)));
    }
}