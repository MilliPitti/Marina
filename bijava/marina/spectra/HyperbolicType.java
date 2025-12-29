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