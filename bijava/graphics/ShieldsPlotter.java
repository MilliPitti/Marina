/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2024

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
import de.smile.marina.fem.model.hydrodynamic.dim2.SedimentModel2DData;

/**
 *
 * @author milbradt
 * @version 4.0
 */
public class ShieldsPlotter {

    public static void main/*Shields*/(String... args) {
        
        double dx = 0.00001E-3;
        int n = 100000;
        
        double[] xloc = new double[n];
        double[] yShields = new double[n];
        double[] ySisyphe = new double[n];
        double[] ySoulsby = new double[n];
        double[] yJulien = new double[n];
        double[] yKnoroz = new double[n];
        double[] ySoulsbyKnoroz = new double[n];
        double[] yJulienKnoroz = new double[n];
        double[] yVanRijn = new double[n];
        for (int i = 0; i < n; i++) {
            final double d50 = dx + i * dx;
            xloc[i] = d50*1000;
            double D = d50 * Math.pow(PhysicalParameters.G * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
            yShields[i] = SedimentModel2DData.CriticalShieldsFunction.Shields.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
            ySisyphe[i] = SedimentModel2DData.CriticalShieldsFunction.Sisyphe.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
            ySoulsby[i] = SedimentModel2DData.CriticalShieldsFunction.Soulsby.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
            yJulien[i] = SedimentModel2DData.CriticalShieldsFunction.Julien.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
            yKnoroz[i] = SedimentModel2DData.CriticalShieldsFunction.Knoroz.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
            ySoulsbyKnoroz[i] = SedimentModel2DData.CriticalShieldsFunction.SoulsbyKnoroz.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
            yJulienKnoroz[i] = SedimentModel2DData.CriticalShieldsFunction.JulienKnoroz.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
            yVanRijn[i] = SedimentModel2DData.CriticalShieldsFunction.VanRijn.getCFS(D)* PhysicalParameters.G * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER);
        }
        // Kurve erzeugen
        

        FunctionPlotter fp = new FunctionPlotter("d50", "mm", "kritische Schubspannung", "N/m**2");
        fp.setTitles("Shields-Diagramm");
        fp.addFunction(xloc, yShields, "Shields");
        fp.addFunction(xloc, ySisyphe, "Sisyphe");
        fp.addFunction(xloc, ySoulsby, "Soulsby");
        fp.addFunction(xloc, yJulien, "Julien");
        fp.addFunction(xloc, yKnoroz, "Knoroz");
        fp.addFunction(xloc, ySoulsbyKnoroz, "SoulsbyKnoroz");
        fp.addFunction(xloc, yJulienKnoroz, "JulienKnoroz");
        fp.addFunction(xloc, yVanRijn, "VanRijn");
        fp.plot("Shields", 640, 480);
    }
    
    public static void mainWC(String... args) {
        
        double dx = 0.00001E-3;
        int n = 10000;
        
        double[] xloc = new double[n];
        double[] yShields = new double[n];
        
        for (int i = 0; i < n; i++) {
            xloc[i] = dx + i * dx;
            xloc[i] *= 1000.;
            yShields[i] = SedimentModel2DData.getWC_Oseen(xloc[i]/1000.);
            
        }
        // Kurve erzeugen
        

        FunctionPlotter fp = new FunctionPlotter("d50", "mm", "Sinkgeschwindigkeit", "m/s");
        fp.addFunction(xloc, yShields, "Sinkgeschwindigkeit nach Oseen");
        fp.plot("Sinkgeschwindigkeit", 640, 480);
    }
    
    public static void mainPorosity(String... args) {
        
        double dx = 0.00001E-3;
        int n = 200000;
        
        double[] xloc = new double[n];
        double[] yWuWang = new double[n];
        double[] yWuWang_025 = new double[n];
        double[] yWuWang_05 = new double[n];
        double[] yWuWang_5 = new double[n];
        double[] yKnoroz = new double[n];
        double[] ySoulsbyKnoroz = new double[n];
        double[] ySisypheKnoroz = new double[n];
        for (int i = 1; i < n; i++) {
            xloc[i] = dx + i * dx;
            yWuWang[i] = SedimentModel2DData.getPorosity(xloc[i],0.);
            yWuWang_025[i] = SedimentModel2DData.getPorosity(xloc[i],0.25);
            yWuWang_05[i] = SedimentModel2DData.getPorosity(xloc[i],0.5);
            yWuWang_5[i] = SedimentModel2DData.getPorosityLogistic(xloc[i],1);
            yKnoroz[i] = SedimentModel2DData.getPorosity(xloc[i],2);
            ySoulsbyKnoroz[i] = SedimentModel2DData.getPorosity(xloc[i],5);
            ySisypheKnoroz[i] = SedimentModel2DData.getPorosityLogistic(xloc[i]);
        
        }
        // Kurve erzeugen
        

        FunctionPlotter fp = new FunctionPlotter("d50", "m", "f(x) = Porosity", " ");
        fp.addFunction(xloc, yWuWang, "Logistic");
        fp.addFunction(xloc, yWuWang_025, "Logistic_025");
        fp.addFunction(xloc, yWuWang_05, "Logistic_05");
        fp.addFunction(xloc, yWuWang_5, "Logistic_1");
        fp.addFunction(xloc, yKnoroz, "Logistic, Sortierung=2");
        fp.addFunction(xloc, ySoulsbyKnoroz, "Logistic, Sortierung=5");
        fp.addFunction(xloc, ySisypheKnoroz, "Logistic");
        fp.plot("Porosity", 640, 480);
    }
    
    
}
