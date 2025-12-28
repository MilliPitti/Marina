package bijava.marina.spectra;

public class BMDirectional extends DirectionalSpectrum {
    
    public BMDirectional( BretschneiderMitsuyasu bm1D, double theta_mean, double s_max ) {
	this.spectrum	= bm1D;
	this.spread_fkt	= new CosinePowerLonguetHiggins();
	this.para_fkt	= new SmaxParameterFkt(s_max);
	
	this.theta_mean = theta_mean;
    }
    
    public double getValue( double frequency, double direction ) {
	double S    = spectrum.getValue(frequency);
	double fp   = spectrum.getPeakFrequency();
	double D    = ((CosinePowerLonguetHiggins)spread_fkt).D(direction, theta_mean, para_fkt.getValue(frequency, fp));
	
	return S*D;
    }
}