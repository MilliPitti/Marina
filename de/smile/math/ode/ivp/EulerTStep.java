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
package de.smile.math.ode.ivp;

public class EulerTStep implements SimpleTStep {

    int resultSize = 0;
    double sysValue[], x[];
    double dt;
    private int NumberOfThreads = 1;

    /**
     * Anzahl der Threads Festlegen, die zur Loesung erzeugt werden sollen,
     * Defaultwert ist 1
     * @param i
     */
    public void setNumberOfThreads(int i) {
        if (i >= 1) {
            NumberOfThreads = i;
        } else {
            NumberOfThreads = 1;
        }
    }

    public int getNumberOfThreads() {
        return NumberOfThreads;
    }

    @Override
    public double[] TimeStep(ODESystem sys, double t, double dt, double x[]) /*throws Exception*/{
        resultSize = x.length;
        this.dt = dt;
        this.x = x;

        sysValue = sys.getRateofChange(t, x);
//        if(sysValue.length!=resultSize)
//            throw new Exception("Dimension des ODESystem ist nicht gleich dem des Loesungsverktors");

        if (NumberOfThreads == 1) {
            for (int i = 0; i < resultSize; i++) {
                x[i] += dt * sysValue[i];
            }
        } else {
            ParallelLoop[] ploop = new ParallelLoop[NumberOfThreads];
            for (int ii = 0; ii < NumberOfThreads; ii++) {
                ploop[ii] = new ParallelLoop(resultSize * ii / NumberOfThreads, resultSize * (ii + 1) / NumberOfThreads);
                ploop[ii].start();
            }
            for (int ii = 0; ii < NumberOfThreads; ii++) {
                try {
                    ploop[ii].join();
                } catch (InterruptedException e) {
                }
            }
        }

        return x;

    }

    class ParallelLoop extends Thread {

        int lo, hi;

        ParallelLoop(int lo, int hi) {
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        public void run() {
            for (int i = lo; i < hi; i++) {
                x[i] += dt * sysValue[i];
            }
        }
    }
}
