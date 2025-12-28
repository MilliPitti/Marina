package de.smile.marina.fem.model.hydrodynamic.wave;

import bijava.marina.spectra.*;
public class Spectrum2DatTime implements Spectrum2D {
	private double t;
	private TimeSpectrum2D s;

	public Spectrum2DatTime(TimeSpectrum2D s,double t){
		this.t=t;
		this.s=s;	
	}
	
	public double getValue(double f, double d){
		return s.getValue(f,d,t);
	}
}