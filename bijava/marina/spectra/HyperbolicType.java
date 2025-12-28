package bijava.marina.spectra;

//..The hyperbolic type model..
public class HyperbolicType implements DirectionalSpreadingFkt {

    /** Standard constructor */
    public HyperbolicType() {
    }
    
    /** Returns the value of the spreading funktion.
      * @param	theta:		direction
      * @param	parameter[0]:	theta_mean
      * @param	parameter[1]:	beta */
    public double D( double theta, double[] parameter ) {
	return D(theta, parameter[0], parameter[1]);
    }    
    
    /** Returns the spreaded value of D(theta, theta_mean, beta). */
    double D( double theta, double theta_mean, double beta ) {
	theta = -((int)((theta/180.)%2))*180.+theta%180;
	theta_mean = ((int)((theta_mean/180.)%2))*180.+theta_mean%180;
	
	return (.5*beta*Math.pow(cosh(Math.PI/180.*beta*(theta-theta_mean)), -2.));
    }
    
    private double cosh(double x){
	return (Math.exp(x)+Math.exp(-x))/2.;
    }
}