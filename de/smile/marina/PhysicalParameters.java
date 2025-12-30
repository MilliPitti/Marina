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
package de.smile.marina;     

/**
 *
 * @author Peter
 * @version 2.4.14
 */
public class PhysicalParameters {
    
    private PhysicalParameters(){}
    
    public static final double G = 9.81;         // [m/s**2] Erdbeschleunigung
    public static final double sqrtG = 3.132;         // 2. Wurzel aus G
    
    public static final double RHO_WATER    = 998.234;  // kg/m**3 bei 20 Grad
    public static final double RHO_WATER_10 = 999.70;   // kg/m**3 bei 10 Grad
    public static final double RHO_WATER_12 = 999.50;   // kg/m**3 bei 12 Grad
    public static final double RHO_WATER_0  = 999.972;  // water by temperature 4 C 
    public static final double RHO_SEDIM    = 2650.0;      // kg/m**3
    public static final double RHO_AIR      = 1.293;	// Air density  [kg/m^3]
    public static final double KINVISCOSITY_WATER    = 1.E-6; // m**2/s   kinematische Viskositaet nu des Wassers bei 20 Grad
    public static final double DYNVISCOSITY_WATER    = 1.E-3; // [kg/(m*s)] oder [Pa s] dynamische Viskositaet eta des Wassers bei 20 Grad = KINVISCOSITY_WATER*RHO_WATER

    public static final double KARMANCONSTANT = 0.412 ;  //K`arm`an-Konstante
    
    /** dynamische Viscositaet von Wasser in Abhaengigkeit von der Temperatur
     * 
     * @param t Temperatur in grad Celsius
     * @return dynamische Viscositaet in kg/(s*m)
     */
    public static double dynViscosityWater(double t){
        return (t<0.) ? 1.E24 : 0.00178/(1.+0.0337*t+0.00022*t*t);
    }
    
    /** betimmt die Dichte des Wassers auf der Basis UNESCO 1981
     * @param t temperature in grad C
     * @return dichte in kg/m**3
     */
    public static double rhoWater(double t){
        return rhoWater(t, Double.NaN);
    }
    
    /** betimmt die Dichte des Wassers auf der Basis UNESCO-Formel 1981
     * @param s salinity in ppt
     * @param t temperature in grad C
     * @return dichte in kg/m**3
     */
    public static double rhoWater(double t, double s){
        final double a0 = 8.24493E-1; 
        final double a1 = -4.0899E-3;
        final double a2 = 7.6438E-5;
        final double a3 = -8.2467E-7;
        final double a4 = 5.3875E-9;
        
        final double b0 = -5.72466E-3;
        final double b1 = 1.0227E-4;
        final double b2 = -1.6546E-6;
        
        final double c0 = 4.8314E-4;
        
        final double d0 = 999.842594;
        final double d1 = 6.793952E-2;
        final double d2 = -9.095290E-3;
        final double d3 = 1.001685E-4;
        final double d4 = -1.120083E-6;
        final double d5 = 6.536332E-9;
        
        final double D = d0 + d1*t + d2*t*t + d3*t*t*t + d4*t*t*t*t + d5*t*t*t*t*t;
        
        if(Double.isNaN(s) || s<Double.MIN_NORMAL*128)
            return D;
        
        final double A = a0 + a1*t + a2*t*t + a3*t*t*t + a4*t*t*t*t;
        final double B = b0 + b1*t + b2*t*t;
        
        
        return A*s+B*Math.pow(s, 3./2.)+c0*s*s+D;
    }
    
}
