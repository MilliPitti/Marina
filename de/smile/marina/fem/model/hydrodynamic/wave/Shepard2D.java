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

import java.awt.geom.*;
import java.util.*;
import bijava.marina.spectra.*;

public class Shepard2D {
    
    private Vector<Dat> area;
    private SpectralWaveDat	wavedat;
    
    public Shepard2D(SpectralWaveDat wd) {
        area = new Vector<Dat>();
        wavedat = wd;
    }
    
    public void addPoint(Point2D.Double p, DiscreteSpectrum2D v) {
        Dat d = new Dat(p, v);
        area.add(d);
    }
    
    public DiscreteSpectrum2D getInterpolationAt(Point2D.Double P) {
        double R=0.;
        DiscreteSpectrum2D res = new DiscreteSpectrum2D(
        wavedat.frequencylength, wavedat.directionlength,
        wavedat.frequenzminimum, wavedat.frequenzmaximum,
        wavedat.directionminimum, wavedat.directionmaximum);
        
        for(Dat dat: area) {
            Point2D.Double isP = dat.getPoint();
            DiscreteSpectrum2D is = dat.getValue();
            double d = isP.distanceSq(P);
            if(d==0.) {
                return  new DiscreteSpectrum2D(
                wavedat.frequencylength, wavedat.directionlength,
                wavedat.frequenzminimum, wavedat.frequenzmaximum,
                wavedat.directionminimum, wavedat.directionmaximum, is);
            } else {
                //d = Math.pow(d, 10.);
                for(int i=0; i<res.getFrequencyLength(); i++)
                    for(int j=0; j<res.getDirectionLength(); j++)
                        res.setValueAt(i,j,res.getValueAt(i,j)[2]+is.getValueAt(i,j)[2]/d);
                R+=1./d;
            }
        }
        if(R!=0.)
            for(int i=0; i<res.getFrequencyLength(); i++)
                for(int j=0; j<res.getDirectionLength(); j++)
                    res.setValueAt(i,j,res.getValueAt(i,j)[2]/R);
        else res.setZero();
        return res;
    }
}

class Dat{
    private Point2D.Double 		p;
    private DiscreteSpectrum2D	value;
    @SuppressWarnings("unused")
    private int			num;
    
    public Dat(Point2D.Double P, DiscreteSpectrum2D v) {
        p 	= (Point2D.Double)P.clone();
        value	= v;
        
    }
    
    public Point2D.Double getPoint() {
        return p;
    }
    
    public DiscreteSpectrum2D getValue() {
        return value;
    }
}