package bijava.marina.spectra;

public class PMDirectional extends DirectionalSpectrum {
    
    public PMDirectional( PiersonMoskowitz pm1D, double theta_mean, double s_max ) {
	this.spectrum	= pm1D;
	this.spread_fkt	= new CosinePowerLonguetHiggins();
	this.para_fkt	= new SmaxParameterFkt(s_max);
	
	this.theta_mean = theta_mean;
    }
    
    public double getValue( double frequency, double direction ) {
	double S    = spectrum.getValue(frequency);
	double fp   = spectrum.getPeakFrequency();
	double D    = ((CosinePowerLonguetHiggins)spread_fkt).D(direction, theta_mean, para_fkt.getValue(frequency, fp));
	//System.out.println("S: "+S+"  D: "+D+"  fp: "+fp);
	return S*D;
    }
}