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

/**
 * coordinate system: - polar coordinates
 * - regular grid
 * - direction mathematically positiv
 */

public class DiscreteSpectrum2D implements Spectrum2D {
	private int nf; // number of frequency increments
	private int nd; // number of direction increments
	private double direction_min, direction_max;
	private double frequency_min, frequency_max;
	private double value[][];

	private double incd, incf; // ..incrementwidth of direction and frequency
	private double delta_d; // ..distance between direction_max and direction_min

	static final double EPSILON = 1.0E-7;

	// ..constructor: sets default values for the frequency minimum and maximum..
	public DiscreteSpectrum2D(int frequency_length, int direction_length) {
		nf = frequency_length - 1;
		nd = direction_length - 1;
		frequency_min = 0.04; // ..default value..
		frequency_max = 0.4; // ..default value..
		direction_min = -180.0; // ..default value..
		direction_max = 180.0; // ..default value..

		delta_d = 360.;
		incd = delta_d / nd;
		incf = (frequency_max - frequency_min) / nf;

		value = new double[nf + 1][nd + 1];
	}

	// ..constructor: transferred values for the frequency minimum and maximum..
	public DiscreteSpectrum2D(int frequency_length,
			int direction_length,
			double frequency_minimum,
			double frequency_maximum) {

		nf = frequency_length - 1;
		nd = direction_length - 1;
		frequency_min = frequency_minimum;
		frequency_max = frequency_maximum;
		direction_min = -180.0;
		direction_max = 180.0;

		delta_d = 360.;
		incd = delta_d / nd;
		incf = (frequency_max - frequency_min) / nf;

		value = new double[nf + 1][nd + 1];
	}

	// * constructor: calculates a discrete spectrum from the transferred */
	public DiscreteSpectrum2D(int frequency_length,
			int direction_length,
			double frequency_minimum,
			double frequency_maximum,
			double direction_minimum,
			double direction_maximum) {

		nf = frequency_length - 1;
		nd = direction_length - 1;
		frequency_min = frequency_minimum;
		frequency_max = frequency_maximum;
		direction_min = direction_minimum;
		direction_max = direction_maximum;

		direction_min = transDirection(direction_min);
		direction_max = transDirection(direction_max);

		if (direction_min == direction_max || Math.abs(direction_max) == 180 && Math.abs(direction_min) == 180.) {
			direction_min = -180.;
			direction_max = 180.;
		}

		delta_d = Math.abs(direction_max - direction_min);
		if (direction_max < direction_min)
			delta_d = 360. - delta_d;

		incd = delta_d / nd;
		incf = (frequency_max - frequency_min) / nf;

		value = new double[nf + 1][nd + 1];
	}

	// * constructor: calculates a discrete spectrum from the transferred */
	public DiscreteSpectrum2D(int frequency_length,
			int direction_length,
			double frequency_minimum,
			double frequency_maximum,
			Spectrum2D spectrum) {

		nf = frequency_length - 1;
		nd = direction_length - 1;
		frequency_min = frequency_minimum;
		frequency_max = frequency_maximum;
		direction_min = -180.0;
		direction_max = 180.0;

		delta_d = Math.abs(direction_max - direction_min);
		if (direction_max < direction_min)
			delta_d = 360. - delta_d;
		incd = delta_d / nd;
		incf = (frequency_max - frequency_min) / nf;

		value = new double[nf + 1][nd + 1];

		for (int d = 0; d <= nd; d++)
			for (int f = 0; f <= nf; f++)
				value[f][d] = spectrum.getValue(frequency_min + f * incf, transDirection(direction_min + d * incd));
	}

	// * constructor: calculates a discrete spectrum from the transferred */
	public DiscreteSpectrum2D(int frequency_length,
			int direction_length,
			double frequency_minimum,
			double frequency_maximum,
			double direction_minimum,
			double direction_maximum,
			Spectrum2D spectrum) {

		nf = frequency_length - 1;
		nd = direction_length - 1;
		frequency_min = frequency_minimum;
		frequency_max = frequency_maximum;
		direction_min = direction_minimum;
		direction_max = direction_maximum;

		direction_min = transDirection(direction_min);
		direction_max = transDirection(direction_max);

		if (direction_min == direction_max || Math.abs(direction_max) == 180 && Math.abs(direction_min) == 180.) {
			direction_min = -180.;
			direction_max = 180.;
		}

		delta_d = Math.abs(direction_max - direction_min);
		if (direction_max < direction_min)
			delta_d = 360. - delta_d;

		incd = delta_d / nd;
		incf = (frequency_max - frequency_min) / nf;

		value = new double[nf + 1][nd + 1];

		for (int d = 0; d <= nd; d++)
			for (int f = 0; f <= nf; f++)
				value[f][d] = spectrum.getValue(frequency_min + f * incf, transDirection(direction_min + d * incd));
	}

	/**
	 * Returns the interpolation over a square item with billinear charge for the
	 * variable of state
	 */
	public double getValue(double frequency, double direction) {
		if (frequency < frequency_min) {
			System.out.println("Error in DiscretSpectrum2D.getValue: frequency is to low:" + frequency);
			return 0.;
		}
		if (frequency > frequency_max + EPSILON) {
			System.out.println("Error in DiscretSpectrum2D.getValue: frequency is to high." + frequency);
			return 0.;
		}

		// ..Calculation to [-180, 180].
		direction = transDirection(direction);

		if (!isInDirection(direction)) {
			System.out.println(
					"Error in DiscretSpectrum2D.getValue: direction is not in [min,max]. direction: " + direction);
			return 0.;
		}

		int d;
		double r;
		if (direction_max < direction_min) {
			double v = 180. - direction_min;
			double refmin = transDirection(direction_min + v);
			double refmax = transDirection(direction_max + v);
			double refd = transDirection(direction + v);
			d = (int) ((refd - refmin) / (refmax - refmin) * .99 * nd);
			r = 2. * (refd - refmin - incd * d) / incd - 1.;
		} else {
			d = (int) ((direction - direction_min) / (delta_d) * 0.99 * nd);
			r = 2. * (direction - direction_min - incd * d) / incd - 1.;
		}

		int f = (int) ((frequency - frequency_min) / (frequency_max - frequency_min) * 0.99 * nf);
		double s = 2. * (frequency - frequency_min - incf * f) / incf - 1.;

		double ferg = ((1. + r) * (1. + s) * value[f + 1][d + 1]
				+ (1. - r) * (1. + s) * value[f + 1][d]
				+ (1. - r) * (1. - s) * value[f][d]
				+ (1. + r) * (1. - s) * value[f][d + 1]) * 0.25;
		return ferg;
	}

	/**
	 * Returns the frequency, the direction and the value at the transferred
	 * discrete position.
	 * 
	 * @param frequency_index index of the discret value field for the frequency
	 * @param direction_index index of the discret value field for the direction
	 * @return a double array with [0] = frequency, [1] = direction, [2] = value
	 */
	public double[] getValueAt(int frequency_index, int direction_index) {
		double[] res = new double[3];

		res[2] = value[frequency_index][direction_index];
		res[1] = transDirection(direction_min + direction_index * incd);
		res[0] = frequency_min + frequency_index * incf;

		return res;
	}

	/**
	 * Calculates the max power density from the values at the discrete points,
	 * returns the max and additionally position of the max in polar coordinates.
	 * 
	 * @return a double array with
	 *         [0] = frequency of the index with max power density;
	 *         [1] = direction of the index with max power density;
	 *         [2] = max power density calculated from the power density values at
	 *         the discrete points
	 */
	public double[] getDiscreteMax() {
		double[] res = new double[3];

		for (int f = 0; f <= nf; f++)
			for (int d = 0; d <= nd; d++)
				if (value[f][d] - res[2] > EPSILON) {
					res[2] = value[f][d];
					res[1] = transDirection(direction_min + d * incd);
					res[0] = frequency_min + f * incf;
				}

		return res;
	}

	/**
	 * Calculates the max power density from the values at the discrete points,
	 * returns the max and additionally the orientation of the max in cartesian
	 * coordinates.
	 * 
	 * @return a double array with
	 *         [0] = x position (cartesian coordinates) from the max power density
	 *         in the unit circle;
	 *         [1] = y position (cartesian coordinates) from the max power density
	 *         in the unit circle;
	 *         [2] = max power density calculated from the power density values at
	 *         the discrete points
	 */
	public double[] getDiscreteMaxVector() {
		double[] res = new double[3];

		double[] tmp = getDiscreteMax();

		res[0] = Math.cos(Math.PI * tmp[1] / 180.);
		res[1] = Math.sin(Math.PI * tmp[1] / 180.);
		res[2] = tmp[2];

		return res;
	}

	/** Returns the length of the direction array. */
	public int getDirectionLength() {
		if (fullRange())
			return nd;
		else
			return nd + 1;
	}

	/** Returns the length of the frequency array. */
	public int getFrequencyLength() {
		return nf + 1;
	}

	/** Returns the direction max value of the discrete spectrum. */
	public double getDirectionMax() {
		// System.out.println("max:
		// "+(((int)((direction_max/180.)%2))*180.+direction_max%180.));
		return direction_max;
	}

	/** Returns the direction min value of the discrete spectrum. */
	public double getDirectionMin() {
		// System.out.println("min.
		// "+(((int)((direction_min/180.)%2))*180.+direction_min%180.));
		return direction_min;
	}

	/** Returns the frequency max value of the discrete spectrum. */
	public double getFrequencyMax() {
		return frequency_max;
	}

	/** Returns the frequency min value of the discrete spectrum. */
	public double getFrequencyMin() {
		return frequency_min;
	}

	/** Returns the increment width of frequency */
	public double getFrequencyIncrement() {
		return incf;
	}

	/** Returns the increment width of direction [deg] */
	public double getDirectionIncrement() {
		return incd;
	}

	/** Returns the total energy of the spectrum. */
	public double getEtot() {
		double sum = 0.;
		double area = incd * incf;

		for (int d = 0; d < nd; d++)
			for (int f = 0; f < nf; f++)
				sum += area * getValue(frequency_min + f * incf + incf * .5,
						transDirection(direction_min + d * incd) + incd * .5);

		return sum;
	}

	/** Return the mean energy of the spectrum in [-180,180] deg. */
	public double getEtotMean() {
		return getEtot() / (360. * (frequency_max - frequency_min));
	}

	/**
	 * Calculates the expected (math.) direction of the energy density and
	 * returns the expected direction and additionally the expected direction in
	 * cartesian coordinates.
	 * 
	 * @return a double array with
	 *         [0] = x position (cartesian coordinates) from the expected direction
	 *         in the unit circle;
	 *         [1] = y position (cartesian coordinates) from the expected direction
	 *         in the unit circle;
	 *         [2] = expected direction [deg]
	 */
	public double[] getExpectedDirection() {
		double[] res = new double[3];
		double exd = 0., dm;
		int counter = 0;

		for (int f = 0; f <= nf; f++) {
			dm = nMomentDirection(1, frequency_min + incf * f);// nMomentDirection(0, frequency_min+incf*f);
			if (!Double.isNaN(dm)) {
				exd += dm;
				counter += nMomentDirection(0, frequency_min + incf * f);
			}
		}

		if (counter == 0)
			res[2] = 0.;
		else
			res[2] = exd / counter;

		res[0] = Math.cos(Math.PI * res[2] / 180.);
		res[1] = Math.sin(Math.PI * res[2] / 180.);

		return res;
	}

	/** Returns true if direction ist in [min,max] else false. */
	public boolean isInDirection(double direction) {
		direction = transDirection(direction);

		if (direction_max > direction_min) {
			if (direction <= direction_max && direction >= direction_min)
				return true;
			return false;
		} else {
			if (direction <= direction_max || direction >= direction_min)
				return true;
			return false;
		}
	}

	/**
	 * Sets the transferred value at the position described through the index from
	 * frequency and direction.
	 */
	public void setValueAt(int frequency_index, int direction_index, double value) {
		this.value[frequency_index][direction_index] = value;

		if ((direction_index == 0 || direction_index == nd) && Math.abs(delta_d) - 360. > -1 * EPSILON) {
			this.value[frequency_index][nd] = value;
			this.value[frequency_index][0] = value;
		}
	}

	/** Sets all values of the discrete spectrum to zero. */
	public void setZero() {
		for (int d = 0; d <= nd; d++)
			for (int f = 0; f <= nf; f++)
				value[f][d] = 0.;
	}

	/**
	 * Returns true if the direction range of the discrete spectrum is 360 [deg].
	 */
	public boolean fullRange() {
		if (delta_d == 360.)
			return true;
		return false;
	}

	/**
	 * Returns the partial derivation of the direction component at the transferred
	 * discret position.
	 */
	public double partialDerivationDirection(int f, int d) {
		if ((d == 0 || d == nd) && delta_d == 360.)
			return 180. * (value[f][1] - value[f][nd - 1]) / (2. * incd * Math.PI);

		if (d == nd)
			return rearDifferenceDirection(f, d);
		if (d == 0)
			return frontDifferenceDirection(f, d);

		return centralDifferenceDirection(f, d);
	}

	/**
	 * Returns the partial derivation of the direction component at the transferred
	 * discret position.
	 * This scheme returns the rear difference if c_direction is greater zero and
	 * the front difference otherwise.
	 */
	public double partialDerivationDirection(int f, int d, double c_direction) {
		if (c_direction < 0.) {
			if (d == nd) {
				if (delta_d == 360.)
					return frontDifferenceDirection(f, 0);
				else
					return rearDifferenceDirection(f, d);
			}
			return frontDifferenceDirection(f, d);
		}
		if (c_direction > 0.) {
			if (d == 0) {
				if (delta_d == 360.)
					return rearDifferenceDirection(f, nd);
				else
					return frontDifferenceDirection(f, d);
			}
			return rearDifferenceDirection(f, d);
		}

		return partialDerivationDirection(f, d);
	}

	/**
	 * Returns the partial derivation of the direction component at the transferred
	 * discret position.
	 * /* This scheme uses a combination of an up-wind scheme and a central scheme.
	 */
	public double partialDerivationDirection(int f, int d, double c_direction, double delta_t) {

		if (d == nd)
			return rearDifferenceDirection(f, d);
		if (d == 0)
			return frontDifferenceDirection(f, d);

		double nue = -1. * sgn(c_direction) * Math.min(Math.abs(180. * c_direction * delta_t / (incd * Math.PI)), 1.);
		return 180. * ((1. + nue) * value[f][d + 1] - 2. * nue * value[f][d] - (1. - nue) * value[f][d - 1])
				/ (2. * incd * Math.PI);
	}

	/**
	 * Returns the second partial derivation of the direction component at the
	 * transferred discret position.
	 */
	public double secondPartialDerivationDirection(int f, int d) {

		if (d == 0)
			return 180. * 180. * (2. * value[f][d + 1] - 2. * value[f][d]) / (incd * incd * Math.PI * Math.PI);
		if (d == nd)
			return 180. * 180. * (2. * value[f][d - 1] - 2. * value[f][d]) / (incd * incd * Math.PI * Math.PI);

		return 180. * 180. * (value[f][d - 1] - 2. * value[f][d] + value[f][d + 1]) / (incd * incd * Math.PI * Math.PI);
	}

	/**
	 * Returns the partial derivation of the frequency component at the transferred
	 * discret position.
	 */
	public double partialDerivationFrequency(int f, int d) {
		if (f == nf)
			return rearDifferenceFrequency(f, d);
		if (f == 0)
			return frontDifferenceFrequency(f, d);

		return centralDifferenceFrequency(f, d);
	}

	/**
	 * Returns the gradient at the discret position of frequency and direction
	 * computed with the difference quotients given in these class
	 */
	public double[] grad(int f, int d) {
		double[] res = new double[2];

		res[0] = partialDerivationFrequency(f, d);
		res[1] = partialDerivationDirection(f, d);

		return res;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append("Diskretes Spektrum:\n");
		buf.append("  f_min, f_max   : ").append(frequency_min).append(" ").append(frequency_max);
		buf.append("\n  d_min, d_max   : ").append(direction_min).append(" ").append(direction_max);
		buf.append("\n  delta_direction: ").append(delta_d);
		buf.append("\n  inc_direction  : ").append(incd);
		buf.append("\n  inc_frequency  : ").append(incf);
		buf.append("\n  num_inc_freq.  : ").append(nf);
		buf.append("\n  num_inc_dir.   : ").append(nd);
		buf.append("\n");

		return buf.toString();
	}

	// ..partial derivatives with finite differences.....................
	/**
	 * Returns the central difference approximation at the transferrred discret
	 * position in frequency direction
	 */
	private double centralDifferenceFrequency(int f, int d) {
		return (value[f + 1][d] - value[f - 1][d]) / (2. * incf);
	}

	/**
	 * Returns the front difference approximation at the transferrred discret
	 * position in frequency direction
	 */
	private double frontDifferenceFrequency(int f, int d) {
		return (value[f + 1][d] - value[f][d]) / incf;
	}

	/**
	 * Returns the rear difference approximation at the transferrred discret
	 * position in frequency direction
	 */
	private double rearDifferenceFrequency(int f, int d) {
		return (value[f][d] - value[f - 1][d]) / incf;
	}

	/**
	 * Returns the central difference approximation at the transferrred discret
	 * position in direction direction
	 */
	private double centralDifferenceDirection(int f, int d) {
		return 180. * (value[f][d + 1] - value[f][d - 1]) / (2. * incd * Math.PI);
	}

	/**
	 * Returns the front difference approximation at the transferrred discret
	 * position in direction direction
	 */
	private double frontDifferenceDirection(int f, int d) {
		return 180. * (value[f][d + 1] - value[f][d]) / (incd * Math.PI);
	}

	/**
	 * Returns the rear difference approximation at the transferrred discret
	 * position in direction direction
	 */
	private double rearDifferenceDirection(int f, int d) {
		return 180. * (value[f][d] - value[f][d - 1]) / (incd * Math.PI);
	}

	/** Returns the direction in [-180,180]. */
	private double transDirection(double d) {
		return -((int) ((d / 180.) % 2)) * 180. + d % 180.;
	}

	@SuppressWarnings("unused")
	private double nMomentFrequency(int n, double direction) {
		double sum = 0.;

		direction = transDirection(direction);

		for (int f = 1; f < nf; f++)
			sum += getValue(frequency_min + f * incf, direction) * Math.pow(frequency_min + f * incf, n);

		double fa = getValue(frequency_min, direction) * Math.pow(frequency_min, n);
		double fb = getValue(frequency_max, direction) * Math.pow(frequency_max, n);

		return (nf * incf / (2. * nf) * (fa + fb + 2. * sum));
	}

	private double nMomentDirection(int n, double frequency) {
		double sum = 0.;

		for (double d = direction_min + 1.; d < (direction_min + delta_d); d += 1.)
			sum += getValue(frequency, d) * Math.pow(d, n); // ..kann probleme Geben!!!..

		double fa = getValue(frequency, direction_min) * Math.pow(direction_min, n);
		double fb = getValue(frequency, direction_max) * Math.pow(direction_max, n);

		return (nd * incd / (2. * nd) * (fa + fb + 2. * sum));
	}

	private double sgn(double value) {
		if (value < 0.)
			return -1.;
		else
			return 1.;
	}
	/*
	 * public double getMeanFrequency() {
	 * 
	 * return 0.;
	 * }
	 */

	/*
	 * public double getEtot() {
	 * //..numeric integration with the trapezodial rule..
	 * double[] oneD = new double[nf+1];
	 * for (int f=0; f<=nf; f++) {
	 * double sum = 0.;
	 * for (int d=1; d<nd; d++)
	 * sum+=getValue(frequency_min+f*incf, d*incd);
	 * oneD[f]=360./(2.*nd)*(getValue(frequency_min+f*incf,
	 * 0.)+getValue(frequency_min+f*incf, incd*nd)+2.*sum);
	 * }
	 * 
	 * double e_tot;
	 * double sum=0.;
	 * for (int f=1; f<nf; f++)
	 * sum+=oneD[f];
	 * e_tot=nf*incf/(2.*incf)*(oneD[0]+oneD[nf]+2.*sum);
	 * 
	 * return e_tot;
	 * }
	 */
}
