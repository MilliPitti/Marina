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
package bijava.math.ifunction;

/**
@Version 3.4.0
*/
public class LinearScalarFunction1d implements ScalarFunction1d {

    private final double werte[][];
    private final int size;
    double tmin, tmax;

    private boolean isperiodic = false;
    private boolean positiveInfinite = !isperiodic;     // flag: function is positive infinite
    private boolean negativeInfinite = !isperiodic;     // flag: function is negative infinite
    
    protected int actPosition = 0;

    public LinearScalarFunction1d(double[][] w) {
        werte = w;
        size = w[0].length;
        tmin = werte[0][0];
        tmax = werte[0][size - 1];
    }

    public double[][] getmin() {
        double y[][] = new double[2][1];
        y[0][0] = werte[0][0];
        y[1][0] = werte[1][0];
        for (int i = 1; i < size; i++) {
            if (werte[1][i] < y[1][0]) {
                y[0][0] = werte[0][i];
                y[1][0] = werte[1][i];
            }
        }
        return y;
    }

    public double[][] getmax() {
        double y[][] = new double[2][1];
        y[0][0] = werte[0][0];
        y[1][0] = werte[1][0];
        for (int i = 1; i < size; i++) {
            if (werte[1][i] > y[1][0]) {
                y[0][0] = werte[0][i];
                y[1][0] = werte[1][i];
            }
        }
        return y;
    }

    //--------------------------------------------------------------------------//
    /**
     * Gets the value of an argument t.
     *
     * @param t If the argument isn't in the definition range the methode
     * returns <code>Double.NaN</code>
     * @return
     */
//--------------------------------------------------------------------------//
    @Override
    synchronized public double getValue(double t) {

        if (isperiodic) {
            while (t >= tmax) {
                t -= tmax;
            }
            while (t < tmin) {
                t += tmin;
            }
        } else {
            if (t < werte[0][0]) {
                return negativeInfinite ? werte[1][0] : Double.NaN;
            }
            if (t > werte[0][size - 1]) {
                return positiveInfinite ? werte[1][size - 1] : Double.NaN;
            }
        }

        // ? liegt t im letzten Intervall ?
        if (actPosition < size - 1) {
            if (t >= werte[0][actPosition] && t <= werte[0][actPosition + 1]) {
                double x1 = werte[0][actPosition];
                double y1 = werte[1][actPosition];
                double x2 = werte[0][actPosition + 1];
                double y2 = werte[1][actPosition + 1];
                return (x1 != x2) ? y1 + ((t - x1) / (x2 - x1)) * (y2 - y1) : y1;
            }
        }
        
        // ? liegt t im naechsten Intervall ?
        actPosition++;
        if (actPosition < size - 1) {
            if (t > werte[0][actPosition] && t <= werte[0][actPosition + 1]) {
                double x1 = werte[0][actPosition];
                double y1 = werte[1][actPosition];
                double x2 = werte[0][actPosition + 1];
                double y2 = werte[1][actPosition + 1];
                return (x1 != x2) ? y1 + ((t - x1) / (x2 - x1)) * (y2 - y1) : y1;
            }
        }

        // Sonst binaere Suche aus java.util.Arrays
        int insertion_point = java.util.Arrays.binarySearch(werte[0], t);
        if (insertion_point >= 0) {
            actPosition = insertion_point;
            return werte[1][insertion_point];
        } else {
            actPosition = (-1) * insertion_point - 2;
            double x1 = werte[0][actPosition];
            double y1 = werte[1][actPosition];
            double x2 = werte[0][actPosition + 1];
            double y2 = werte[1][actPosition + 1];
            return (y1 + ((t - x1) / (x2 - x1)) * (y2 - y1));
        }
    }

    @Override
    public double getDifferential(double t) {
        if (isperiodic) {
            while (t >= tmax) {
                t -= tmax;
            }
            while (t < tmin) {
                t += tmin;
            }
        }
        double y = Double.NaN;
        double t1, t2, y1, y2;
        int i, j, pos;
        pos = size;
        for (i = 0; i < size; i++) {
            if (werte[0][i] > t) {
                pos = i - 1;
                i = size;
            }

        }
        if (pos < size) {
            t1 = werte[0][pos];
            y1 = werte[1][pos];
            t2 = werte[0][pos];
            y2 = werte[1][pos];
            for (j = pos; j < size; j++) {
                if (werte[0][j] > t) {
                    t2 = werte[0][j];
                    y2 = werte[1][j];
                    j = size;
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

    @Override
    public void setPeriodic(boolean b) {
        isperiodic = b;
        positiveInfinite = negativeInfinite = !b;
    }

    @Override
    public boolean isPeriodic() {
        return isperiodic;
    }

    @Override
    public String toString() {
        String s = "";

        for (int i = 0; i < size; i++) {
            s += " " + werte[0][i] + " " + werte[1][i];
        }
        return s;
    }
}
