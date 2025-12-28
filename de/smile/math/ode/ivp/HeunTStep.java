package de.smile.math.ode.ivp;

public class HeunTStep implements SimpleTStep {

    int resultSize = 0;
    double t1_result[], sysValue[], x[];
    double dt;
    private int NumberOfThreads = 1;

    /**
     * Anzahl der Threads Festlegen, die zur Loesung erzeugt werden sollen,
     * Defaultwert ist 1.
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

        t1_result = new double[resultSize];

        sysValue = sys.getRateofChange(t, x);
//        if(sysValue.length!=resultSize)
//            throw new Exception("Dimension des ODESystem ist nicht gleich dem des Loesungsverktors");

        if (NumberOfThreads == 1) {
            for (int i = 0; i < resultSize; i++) {
                t1_result[i] = x[i] + dt / 2. * sysValue[i];
            }
        } else {
            ParallelLoop1[] ploop1 = new ParallelLoop1[NumberOfThreads];
            for (int ii = 0; ii < NumberOfThreads; ii++) {
                ploop1[ii] = new ParallelLoop1(resultSize * ii / NumberOfThreads, resultSize * (ii + 1) / NumberOfThreads);
                ploop1[ii].start();
            }
            for (int ii = 0; ii < NumberOfThreads; ii++) {
                try {
                    ploop1[ii].join();
                } catch (InterruptedException e) {
                }
            }
        }


        sysValue = sys.getRateofChange(t + dt / 2., t1_result);
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

    class ParallelLoop1 extends Thread {

        int lo, hi;

        ParallelLoop1(int lo, int hi) {
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        public void run() {
            for (int i = lo; i < hi; i++) {
                t1_result[i] = x[i] + dt / 2. * sysValue[i];
            }
        }
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
