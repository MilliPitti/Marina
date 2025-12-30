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