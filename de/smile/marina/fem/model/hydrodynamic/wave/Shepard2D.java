package de.smile.marina.fem.model.hydrodynamic.wave;

import java.awt.geom.*;
import java.util.*;
import bijava.marina.spectra.*;

public class Shepard2D {
    
    private Vector area;
    private SpectralWaveDat	wavedat;
    
    public Shepard2D(SpectralWaveDat wd) {
        area = new Vector();
        wavedat = wd;
    }
    
    public void addPoint(Point2D.Double p, DiscreteSpectrum2D v) {
        dat d = new dat(p, v);
        area.add(d);
    }
    
    public DiscreteSpectrum2D getInterpolationAt(Point2D.Double P) {
        double R=0.;
        DiscreteSpectrum2D res = new DiscreteSpectrum2D(
        wavedat.frequencylength, wavedat.directionlength,
        wavedat.frequenzminimum, wavedat.frequenzmaximum,
        wavedat.directionminimum, wavedat.directionmaximum);
        
        for (Enumeration e = area.elements(); e.hasMoreElements();) {
            dat Dat=(dat)e.nextElement();
            Point2D.Double isP = Dat.getPoint();
            DiscreteSpectrum2D is = Dat.getValue();
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

class dat{
    private Point2D.Double 		p;
    private DiscreteSpectrum2D	value;
    private int			num;
    
    public dat(Point2D.Double P, DiscreteSpectrum2D v) {
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