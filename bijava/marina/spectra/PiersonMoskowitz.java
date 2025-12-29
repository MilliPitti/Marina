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

/** PiersonMoskowitz spectrum:
  * This one-dimensional characterizing the fully developed sea is given by Pierson and Moskowitz [1964].*/
public class PiersonMoskowitz implements Spectrum1D {
    private double    fp;	    //..peak frequency
    private double    g=9.81;	    //..
    private double    alpha=0.0081; //..
      
    /** Constructor: expects peak frequency. */
    public PiersonMoskowitz( double fp ) {
	this.fp = fp;
    }
    
    /** Returns the peak frequency of the spectrum. */
    public double getPeakFrequency() {
	return fp;
    }
    
    /** Returns the spectrum value at the freuqency f. */
    public double getValue( double f ) {
	double factor;
	 
	factor  = alpha*g*g*Math.pow(2.*Math.PI,-4.)*Math.pow(f, -5.);
    
	return (factor*Math.exp(-5./4.*Math.pow(f/fp, -4.)));
    }
}