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

public class GaussianShapedDirectional extends DirectionalSpectrum {
    
    public GaussianShapedDirectional( GaussianShaped gs1D, double theta_mean, double s_max ) {
	this.spectrum	= gs1D;
	this.spread_fkt	= new CosinePowerLonguetHiggins();
	this.para_fkt	= new SmaxParameterFkt(s_max);
	
	this.theta_mean = theta_mean;
    }
    
    public double getValue( double frequency, double direction ) {
	double S    = spectrum.getValue(frequency);
	double fp   = spectrum.getPeakFrequency();
	double D    = ((CosinePowerLonguetHiggins)spread_fkt).D(direction, theta_mean, para_fkt.getValue(frequency, fp));
	
	return S*D;
    }
}