package bijava.marina.spectra;

public class SmaxParameterFkt implements ParameterFkt {

    private double  s_max;
    
    public SmaxParameterFkt( double s_max ) {
	this.s_max = s_max;
    }
    
    public double getValue( double f, double fp ) {
	if (f<fp)
	    return s_max*Math.pow(f/fp, 5.);
	if (f>=fp)
	    return s_max*Math.pow(f/fp, -2.5);
	
	return 0.;
    }
}