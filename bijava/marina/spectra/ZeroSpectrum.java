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

/** Spectrum is everywhere zero */
public class ZeroSpectrum implements Spectrum1D {
    @SuppressWarnings("unused")
    private double fp; // ..peak frequency
    @SuppressWarnings("unused")
    private double h1_3; // ..
    @SuppressWarnings("unused")
    private double t1_3; // ..

    /** Constructor */
    public ZeroSpectrum() {
    }

    /** Returns the peak frequency of the spectrum. */
    public double getPeakFrequency() {
        return 0.;
    }

    /** Returns the spectrum value at the freuqency f. */
    public double getValue(double f) {
        return 0.;
    }
}