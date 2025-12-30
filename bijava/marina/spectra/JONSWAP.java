/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2026

 * This file is part of Marina.

 * Marina is free software: you can redistribute it and/or modify              
 * it under the terms of the GNU Affero General Public License as               
 * published by the Free Software Foundation version 3.
 * 
 * Marina is distributed in the hope that it will be useful,                  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                
 * GNU Affero General Public License for more details.                          
 *                                                                              
 * You should have received a copy of the GNU Affero General Public License     
 * along with Marina.  If not, see <http://www.gnu.org/licenses/>.             
 *                                                                               
 * contact: milbradt@smileconsult.de                                        
 * smile consult GmbH                                                           
 * Schiffgraben 11                                                                 
 * 30159 Hannover, Germany 
 * 
 */
package bijava.marina.spectra;

public class JONSWAP implements Spectrum1D {
    private double    g = 9.81;	//..[m/(s*s)]
    private double    U = 10.;	//..wind velocity
    private double    X = 200.0;	//..fetch length;
    private double    alpha;	//..energy scaling factor (the Phillips "constant")
    private double    fp;		//..peak frequency
    private double    gamma;	//..(>=1) peak enhancement paramter
    private double    epsilon1;	//..factor that detemines the width of the peak enhancement for omega<=omegaP
    private double    epsilon2;	//..factor that detemines the width of the peak enhancement for omega>omegaP
  
     public JONSWAP( double X, double U, double gamma, double epsilon1, double epsilon2 ) {
	this.X	    = X;
	this.U	    = U;
	this.gamma	    = gamma;
	this.epsilon1   = epsilon1;
	this.epsilon2   = epsilon2;
    
	alpha   =  alpha();
	fp  = fP();
    }
    
    public double getValue( double f ) {
	double factor, term, exponent, epsilon;
	
	if (f<=fp)
	    epsilon = epsilon1;
	else
	    epsilon = epsilon2;
	 
	factor  = alpha*g*g*Math.pow(2.*Math.PI, -4.)*Math.pow(f, -5.);
	term    = Math.exp(-5./4. * Math.pow((f/fp), -4.));
	exponent= Math.exp(-1.*(f-fp)*(f-fp)/(2.*epsilon*epsilon*fp*fp));

    	return ( factor*term*Math.pow(gamma, exponent) );
    }
    
    public double getPeakFrequency() {
	return fp;
    }
    
    public double getU() {
	return U;
    }
    
    private double alpha() {
	return .076*Math.pow(g*X/(U*U), -0.22);
    }
  
    private double fP() {
	return 7.*Math.PI*(g/U)*Math.pow(g*X/(U*U), -.33);
    }
}