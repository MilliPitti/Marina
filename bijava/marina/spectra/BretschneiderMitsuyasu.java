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

/** BretschneiderMitsuyasu directional spectrum */
public class BretschneiderMitsuyasu implements Spectrum1D {
    private double    fp;	//..peak frequency
    private double    h1_3;	//..
    private double    t1_3;	//..
      
    /** Constructor: expects H1/3 and T1/3. */
    public BretschneiderMitsuyasu( double h1_3, double t1_3 ) {
	this.h1_3	= h1_3;
	this.t1_3	= t1_3;
    
	fp  = 1./(1.05*t1_3);
    }
    
    /** Returns the peak frequency of the spectrum: fp=1/(1.05*T1/3). */
    public double getPeakFrequency() {
	return fp;
    }
    
    /** Returns the spectrum value at the freuqency f. */
    public double getValue( double f ) {
	double factor;
	 
	factor  = .25*h1_3*h1_3*t1_3*Math.pow(t1_3*f, -5.);
    
	return (factor*Math.exp(-1.03*Math.pow(t1_3*f, -4.)));
    }
}