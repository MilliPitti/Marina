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

/**
 * "IVP" stellt Klasse zur L&ouml;sung einer gew&ouml;hnlichen
 *  Differentialgleichung erster Ordnung <code>y' = F(t,x[])</code>
 *  zur Verf&uuml;gung.
 * 
 *  Beginnend mit der Startl&ouml;sung <code>x0[]</code> zum Zeitpunkt
 *  <code>t0</code> l&ouml;st sie die gew&ouml;hnliche Differentialgleichung
 *  am Punkt <code>t1</code>.
 * 
 *  @author Peter Milbradt
 */ 
//==========================================================================// 
public class IVP {
  // verhindert, dass ein Objekt dieser Klasse erzeugt wird
    public IVP(){}
    
//--------------------------------------------------------------------------//
//  DGL MIT EINEM EINSCHRITT-ZEITSCHRITTVERFAHREN                           //
//--------------------------------------------------------------------------//
/** Berechnet die L&ouml;sung eines gew&ouml;hnlichen
 *  Differentialgleichungssystems erster Ordnung mit Hilfe eines einfachen
 *  expliziten Einschrittverfahrens.
 *
 *  @param sys      gew&ouml;hnliches Differentialgleichungssystem
 *  @param t0       Startzeitpunkt
 *  @param x[]      Startl&ouml;sung
 *  @param t1       Zeitpunkt, f&uuml;r dem die DGL gel&ouml;st wird
 *  @param method   einfaches Einschritt-Verfahren
 * @return                           */
//--------------------------------------------------------------------------//
    public static double [] Solve(ODESystem sys, double t0, double x[],
                                            double t1, SimpleTStep method) {
        double t          = t0;
        double dt;
        
        do{
            dt = sys.getMaxTimeStep();
            if((t+dt)>t1) dt = t1 - t;
            x = method.TimeStep(sys,t,dt,x);
            t += dt;
        } while (t<t1);
        
        return x;
    } 
}
