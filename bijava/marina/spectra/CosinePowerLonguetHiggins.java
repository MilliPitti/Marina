package bijava.marina.spectra;

/** The cosine-power model from Longuet-Higgins et al [1961]. */
public class CosinePowerLonguetHiggins implements DirectionalSpreadingFkt {

    /** Standard constructor */
    public CosinePowerLonguetHiggins() {
    }
    
    /** Returns the value of the spreading funktion.
      * @param	theta:		direction
      * @param	parameter: feld [0] enthaelt theta_mean
      *               und  feld [1] enthaelt	s */
    public double D( double theta, double[] parameter ) {
	return D(theta, parameter[0], parameter[1]);
    }    
    
    /** Returns the spreaded value of D(theta, theta_mean, s). */
    double D( double theta, double theta_mean, double s ) {
	theta = -((int)((theta/180.)%2))*180.+theta%180;
	theta_mean = -((int)((theta_mean/180.)%2))*180.+theta_mean%180;
	
	double tcalc = -((int)(((theta-theta_mean)/180.)%2))*180.+(theta-theta_mean)%180;
	
	double factor = Math.pow(2., 2.*s-1.)/Math.PI * Math.pow(Math.exp(gammaLn(s+1.)), 2.)/Math.exp(gammaLn(2.*s+1.));
	return factor * Math.pow(Math.cos(.5*(tcalc)*Math.PI/180.), 2.*s);
    }

    /** Returns the valu ln[gammaFkt(xx)] for xx > 0.
      * From: Numerical recipes in C, W.H.Press et al., 1992, p.214 */
    private double gammaLn( double xx ) {
	double x, y, tmp, ser;
    
	double cof[] = {	76.18009172947146, -86.50532032941677,
				24.01409824083091, -1.231739572450155,
				0.1208650973866179E-2, -0.5395239384953E-5 };
    
	y = x = xx;
	tmp = x+5.5;
	tmp -= (x+.5)*Math.log(tmp);
	ser = 1.000000000190015;
    
	for (int j=0; j<=5; j++)
	    ser+= cof[j]/++y;
    
	return -tmp+Math.log(2.5066282746310005*ser/x);
    }
}