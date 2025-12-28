/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2021

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
package de.smile.math;

/** Utility-Klasse f&uuml;r elementare mathematische Funktionen */
public class Function {
    private Function() {
    }
    public static double abs(double a){
        return (a > 0) ? a : -a;
    }
    
    //--------------------------------------------------------------------------//
    //  FAKULTAET                                                               //
    //--------------------------------------------------------------------------//
    /** Liefert die Fakult&auml;t einer positiven ganzen Zahl.
     *  Der Definitionsbereich liegt zwischen 0 und plus unendlich.
     *  Der Wertebereich liegt ebenfalls zwischen 0 und plus unendlich.
     *  Sie hat eine Nullstelle bei x = 0.
     *  Die Funktion ist streng monoton wachsend.
     *  @param x	Wert, zu dem die Fakult&auml;t bestimmt werden soll.<p>
     *
     *  @return x!, falls <code>x</code> negativ ist.
     * @deprecated ersetzt durch BIjava.Math.Function.factorial*/
    //--------------------------------------------------------------------------//
    @Deprecated
    public static int fak(int x) {
        if (x < 0)
            return -1;
        else {
            if (x > 1)
                return fak(x - 1) * x;
            else
                return 1;
        }
    }
    
    //--------------------------------------------------------------------------//
    //  FAKULTAET                                                               //
    //--------------------------------------------------------------------------//
    /** Liefert die Fakult&auml;t einer positiven ganzen Zahl.
     *  Der Definitionsbereich liegt zwischen 0 und plus unendlich.
     *  Der Wertebereich liegt ebenfalls zwischen 0 und plus unendlich.
     *  Sie hat eine Nullstelle bei x = 0.
     *  Die Funktion ist streng monoton wachsend.
     *  @param x	Wert, zu dem die Fakult&auml;t bestimmt werden soll.<p>
     *
     *  @return x!, falls <code>x</code> negativ ist.
     */
    public static int factorial(int x) {
        if (x < 0)
            return -1;
        else {
            if (x > 1)
                return factorial(x - 1) * x;
            else
                return 1;
        }
    }
    
    public static double sqr(double a) {
        return a * a;
    }
    public static float sqr(float a) {
        return a * a;
    }
    public static int sqr(int a) {
        return a * a;
    }
    
    public static double pow3(double a){
        return a*a*a;
    }
    public static double pow4(double a){
        return a*a*a*a;
    }
    
    public static double powN(double a, int n){
        final int absN = (n > 0) ? n : -n;
        double rValue =1.;
        for(int i=0; i<absN;i++){rValue *=a;}
        if(n<0)
            return 1./rValue;
        else
            return rValue;
    }
    
    //--------------------------------------------------------------------------//
    //  COTANGENSHYPERBOLIKUS                                                   //
    //--------------------------------------------------------------------------//
    /** Liefert den Cotangenshyperbolikus einer Gleitpunktzahl.
     *  Der Definitionsbereich schliesst lediglich die 0 aus.
     *  Der Wertebereich schliesst lediglich das Intervall [-1,1] aus.
     *  Die Funktion ist unsymmetrisch.
     *  Sie hat einen Pol bei x = 0.
     *  Sie hat drei Asymptoten bei y = -1, y = 0 und y = 1.
     *
     *  <p><IMG SRC="./imagesFunction/coth.gif" vspace=20 hspace=20>
     *
     *  @param x	Wert, zu dem der Cotangenshyperbolikus bestimmt werden soll.
     * @return */
    //--------------------------------------------------------------------------//
    public static double coth(double x) {
        if (Math.abs(x) < 1.E-16) {
            if (x < 0.)
                return Double.NEGATIVE_INFINITY;
            if (x > 0.)
                return Double.POSITIVE_INFINITY;
            if (x == 0.)
                return Double.NaN;
        }
        if (Math.abs(x) < 600.) {
            return 1.+ 2./(Math.exp(2*x)-1.);
        } else {
            return sign(x) * 1.;
        }
    }
    
    //--------------------------------------------------------------------------//
    //  SEKANSHYPERBOLIKUS                                                      //
    //--------------------------------------------------------------------------//
    /** Liefert den Sekanshyperbolikus einer Gleitpunktzahl.
     *  Der Definitionsbereich liegt zwischen -unendlich und plus unendlich.
     *  @param x    Wert, zu dem der Sekanshyperbolikus bestimmt werden soll.
     * @return */
    //--------------------------------------------------------------------------//
    public static double sech(double x) {
        return 2. / (Math.exp(x) + Math.exp(-x));
    }
    
    public static double sign(double value) {
        if (value >= 0.)
            return 1.0;
        else
            return -1.0;
    }
    
    public static float sign(float value) {
        if (value >= 0.)
            return 1.0f;
        else
            return -1.0f;
    }
    
    public static int sign(int value) {
        if (value >= 0)
            return 1;
        else
            return -1;
    }
    
    /**Bestimmung des Binomialkoeffizienten aus natuerlichen Zahlen n und k (lies "n ueber k").
     * @param n natuerliche Zahl
     * @param k natuerliche Zahl
     * @return  */
    public static int binomialcoefficient(int n, int k) {
        if (k == 0)
            return 1; // 0!=1
        
        /* (n k) = n! / ((n-1)!k!) */
        return factorial(n) / (factorial(n - 1) * factorial(k));
    }
    
    public static double max(double a, double b) {
        return (a > b) ? a : b;
    }
    public static double min(double a, double b) {
        return (a < b) ? a : b;
    }
    /** compute the euclidian vectornorm
     * @param x first component of the vector
     * @param y the second component of the vector
     * @return norm of the vector (x,y)
     */
    public static double norm(double x, double y){
        return Math.sqrt(x*x+y*y);
    }
    /** compute the euclidian vectornorm
     * @param x first component of the vector
     * @param y the second component of the vector
     * @param z the third component of the vector
     * @return norm of the vector (x,y,z)
     */
    public static double norm(double x, double y, double z){
        return Math.sqrt(x*x+y*y+z*z);
    }

    /** Berechnet den Winkel den der Vektor (vx,vy) zur vx-Achse hat in Grad
     * hat die gleiche Wirkung wie Math.atan2(vy,vx) * 180. / Math.PI (mit vertauschten komponenten)
     * @param vx
     * @param vy
     * @return
     */
    public static double getAngle(double vx, double vy) {
        final double w = Function.norm(vx, vy);
        if (w > Double.MIN_NORMAL) {
            final double wx = vx / w;
            final double wy = vy / w;
            return (sign(wy) * Math.acos(wx) * 180. / Math.PI);
        }
        return 0.;
    }
}
