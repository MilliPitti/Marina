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
package de.smile.marina.fem.model.hydrodynamic.wave;

import bijava.marina.spectra.*;

public class  TimeSpectrum2D {
    TSP[] values;
    
    public TimeSpectrum2D(double[] time, Spectrum2D[] wsp){
	if (time.length == wsp.length){
	    values = new TSP[time.length];
	    for(int i=0;i<time.length;i++){
		values[i]= new TSP();
		values[i].t = time[i];
		values[i].w = wsp[i];
	    }
	} else 
	    System.out.println("fehler bei der Randwerte");
	
    }
    
    
    public Spectrum2D getSpectrumAt(double t) {
    	for(int i=0; i<values.length; i++) {
    		if (values[i].isTime(t)) {
			return values[i].getSpectrum();
    		}
    	}
    	return null;
    }
    
    public double getValue(double f, double a, double t){
	double y = 10000.;
	double t1=0.,t2=0.,y1=0.,y2=0.;
	double w[][] = new double[2][2];
	int i,j,pos,anz;
	anz=values.length;
	w[0][0] = values[0].t;
	w[1][0] = values[0].w.getValue(f,a);
	if (w[0][0]==t){
	    y=w[1][0];
	} else {
	    pos = anz;
	    for (i=0;i<anz;i++) {
		if (values[i].t>t) {
		    pos = i-1;
		    i = anz;
		}
	    }
	    if (pos<anz) {
		t1 = values[pos].t;
		y1 = values[pos].w.getValue(f,a);
		t2 = values[pos].t;
		y2 = values[pos].w.getValue(f,a);
		for (j=pos;j<anz;j++) {
		    if (values[j].t>t) {
			t2 = values[j].t;
			y2 = values[j].w.getValue(f,a);
			j = anz;
		    }
		}
		if (t1 != t2)
		    y = y1 + ((t-t1)/(t2-t1))*(y2-y1);	
		else
		    y = y1;
	    }
	}
	return y;
    }
    
    public double getDifferential(double f, double a, double t) {
        double y = 10000.;
        double t1, t2, y1, y2;
        int i, j, pos, anz;
        anz = values.length;
        pos = anz;
        for (i = 0; i < anz; i++) {
            if (values[i].t > t) {
                pos = i - 1;
                i = anz;
            }

        }
        if (pos < anz) {
            t1 = values[pos].t;
            y1 = values[pos].w.getValue(f, a);
            t2 = values[pos].t;
            y2 = values[pos].w.getValue(f, a);
            for (j = pos; j < anz; j++) {
                if (values[j].t > t) {
                    t2 = values[j].t;
                    y2 = values[j].w.getValue(f, a);
                    j = anz;
                }

            }
            if (t1 != t2) {
                y = (y2 - y1) / (t2 - t1);
            } else {
                y = 1.;
            }
        }
        return y;
    }
}

class TSP {
    double t;
    Spectrum2D w;
    
    public boolean isTime(double time) {
    	return(time==t);
    }
    
    public Spectrum2D getSpectrum() {
    	return w;
    }
}
