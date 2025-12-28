package bijava.marina.spectra;

public class JONSWAPDirectional extends DirectionalSpectrum {
    
    public JONSWAPDirectional( JONSWAP j1D, double theta_mean ) {
	this.spectrum	= j1D;
	this.spread_fkt	= new CosinePowerLonguetHiggins();
	this.para_fkt	= new MitsuyasuParameterFkt(j1D.getU());
	
	this.theta_mean = theta_mean;
    }
    
    public double getValue( double frequency, double direction ) {
	double S    = spectrum.getValue(frequency);
	double fp   = spectrum.getPeakFrequency();
	double D    = ((CosinePowerLonguetHiggins)spread_fkt).D(direction, theta_mean, para_fkt.getValue(frequency, fp));
	
	return S*D;
    }
}