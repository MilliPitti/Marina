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
package bijava.graphics;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.math.Function;

/**
 *
 * @author milbradt
 */
public class PlotterTest {
    static double ks=.1;
    static double getValueDepth(double x){
        return 8 * PhysicalParameters.G / Function.sqr(18.*Math.log(12.* x/ (ks*1.E-3)));
    }
    
    static double getValueDepthDW(double x){
        return (.24 / Function.sqr(Math.log10(12.* x/ (ks*1.E-3)))) / 8;
    }
    
    static double getValueDepthChezy(double x){
        return (1 / Function.sqr(5.75* Math.log10(12.* x/ (ks*1.E-3))));
    }
    
    static double getValueDepth_ks(double x){
        final double k = 0.41; // Karman-Constante
        return Function.sqr(k / Math.log(12.*x/ (ks*1.E-3)));
    }
    
    static double getValueDepth_kst(double x){
        double kst=CurrentModel2DData.Nikuradse2Strickler(ks);
        return PhysicalParameters.G *1. / Math.cbrt(x) / Function.sqr(kst); // wie in Martin und Marina implementiert nach BAW 
    }
    static double getValueDepth_Strickler(double x){
        return PhysicalParameters.G / ( 25 * 25 * Math.cbrt(x/(ks*1.E-3)));
    }
    
    
    public static void main(String... args) {
        
        System.out.println(CurrentModel2DData.Nikuradse2Strickler(ks));
        
        double dx = .1;
        int n = 1000;
        
        double[] xloc = new double[n];
        double[] yDW = new double[n];
        double[] yST = new double[n];
        double[] yChezy = new double[n];
        double[] yNikuradse = new double[n];
        double[] yStrickler = new double[n];
        double[] yNikuradseKarman = new double[n];
        
        for (int i = 0; i < n; i++) {
            xloc[i] = dx + i * dx;
            yDW[i] = getValueDepthDW(xloc[i]);
            yST[i] = getValueDepth_Strickler(xloc[i]);
            yChezy[i] = getValueDepthChezy(xloc[i]);
            yNikuradse[i] = getValueDepth(xloc[i]);
            yStrickler[i] = getValueDepth_kst(xloc[i]);
            yNikuradseKarman[i] = getValueDepth_ks(xloc[i]);
        }
        // Kurve erzeugen
        

        FunctionPlotter fp = new FunctionPlotter("depth", "m", "cf", "m/s");
        fp.addFunction(xloc, yDW, "Darcy-Weiszbach");
        fp.addFunction(xloc, yChezy, "Chezy");
        fp.addFunction(xloc, yNikuradseKarman, "NikuradseKarman");
        fp.addFunction(xloc, yNikuradse, "Nikuradse");
        fp.addFunction(xloc, yStrickler, "Strickler");
        fp.addFunction(xloc, yST, "Strickler mit ks");
        fp.plot("Bodenschubspannung", 640, 480);
    }
}
