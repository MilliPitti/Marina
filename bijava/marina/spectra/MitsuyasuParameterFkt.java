package bijava.marina.spectra;

public class MitsuyasuParameterFkt implements ParameterFkt {
    private double  g = 9.81;
    private double  U;
    
    public MitsuyasuParameterFkt( double U ) {
	this.U = U;
    }
    
    public double getValue( double f, double fp ) {
	double fpm  = U*fp/g;
	double fm   = U*f/g;
	double sp   = 11.5*Math.pow(fpm, -2.5);
	
	if (f<fp)
	    return sp*Math.pow(fm/fpm, 5.);
	if (f>=fp)
	    return sp*Math.pow(fm/fpm, -2.5);
	
	return 0.;
    }
}