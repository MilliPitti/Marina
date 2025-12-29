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
package de.smile.marina.fem.model.hydrodynamic.wave;
/** 
 * Abstract class for waverelevant functions
 */
public abstract class WaveFunction{
    final static double G = 9.81;    // Erdbeschleunigung
    final static double EPSILON = 1.E-6;
    /** WaveNumber
     * @param depth
     * @param sigma
     * @return  */    
    public static double WaveNumber(double depth, double sigma){
        double wavenumber = 0.;
        double Dxkh = 0.;
        double Xkh, Xkh0, Tmp1, Tmp2;
        int i=0;
        if( sigma > EPSILON){
            if(depth > EPSILON){
                Xkh0 = depth/G * Math.pow(sigma,2.);
                Tmp1 = 1./Math.tanh(Math.pow(Xkh0,(0.75)));
                Tmp2 = Math.pow(Tmp1, (2./3.));
                Xkh = Xkh0*Tmp2;
                do{
                    i++;
                    Xkh += Dxkh;
                    Dxkh = (Xkh0 - Xkh * Math.tanh(Xkh))/(Xkh/Math.pow(Math.cosh(Xkh),2.) + Math.tanh(Xkh));
                }while( (i < 100) && (Math.abs(Dxkh/Xkh) > EPSILON) );
                if(i>99) System.out.println(" Finde keine Loesung in der Wellelaenge!");
                wavenumber = (Xkh + Dxkh)/depth;
            }else
                wavenumber= Double.MAX_VALUE/10.;
        }
        return wavenumber;
    }
    
    public static void main (String... args){
        double depth = 7.
                , sigma=0.2;
        System.out.println(WaveNumber( depth,  sigma));
    }
}
