package de.smile.math;

import java.util.stream.IntStream;

// ist nicht wirklich schneller als Math.cbrt!
public final class CbrtApproximation {

    // Fall 1: [0,1] - sehr präzise
    private static final double[] TABLE_0_1 = 
        IntStream.range(0, 1000)
            .mapToDouble(i -> Math.cbrt(i / 999d))
            .toArray();
    
    // Fall 2: [0,100] - Standardbereich
    private static final double INCREMENT = 0.1;
    private static final double[] TABLE_0_100 = new double[1001];
    
    static {
        for (int i = 0; i < 1001; i++) {
            TABLE_0_100[i] = Math.cbrt(i * INCREMENT);
        }
    }
    
    // Statische Utility-Methoden (KEINE Objekterzeugung!)
    public static double cbrt0To1(double f) { 
        int idx = (int)( f * 999.0 );
        return TABLE_0_1[Math.min(idx, 999)]; 
    }
    
    public static double cbrt0To100(double a) { 
        double result;
        if ((a < 0.) || (a > (TABLE_0_100.length - 2) * INCREMENT)) {
            result = Math.cbrt(a);
        } else {
            final int idx = (int)(a / INCREMENT);
            final double f0 = TABLE_0_100[idx];
            final double f1 = TABLE_0_100[idx + 1];
            final double frac = (a / INCREMENT) - idx;
            result = f0 + (f1 - f0) * frac;
        }
        return result;
    }
    
    // Intelligente Auswahl:
    public static double cbrt(double x) {
        if(x>=0){
            if (x <= 1.0) return cbrt0To1(x);
            if (x <= 100.0) return cbrt0To100(x);
            System.out.println("Warning: cbrt called with large argument: "+x);
            return Math.cbrt(x);
        } else
            return -cbrt(Math.abs(x));
    }
}
