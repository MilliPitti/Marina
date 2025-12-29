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

/** Gaussian shaped spectrum */
public class GaussianShaped implements Spectrum1D {
    private double    fp;	//..peak frequency
    private double    hs;	//..significant wave height;
    private double    sigmaS;	//..derivation
      
    /** Constructor: expects H1/3, sigmaS(deriavtion) and the peak frequency. */
    public GaussianShaped( double hs, double sigmaS, double fp ) {
	this.hs	    = hs;
	this.sigmaS = sigmaS;
	this.fp	    = fp;
    }
    
    /** Returns the peak frequency of the spectrum. */
    public double getPeakFrequency() {
	return fp;
    }
    
    /** Returns the spectrum value at the freuqency f. */
    public double getValue( double f ) {
	double factor, exponent;
        
	factor  = hs*hs/16./(Math.sqrt(2.*Math.PI)*sigmaS);
	exponent= -1.*(f-fp)*(f-fp)/(2.*sigmaS*sigmaS);
    
	return factor*Math.exp(exponent);
   }
}