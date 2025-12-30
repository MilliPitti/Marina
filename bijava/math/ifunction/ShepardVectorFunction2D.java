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
import java.awt.geom.*;

public class ShepardVectorFunction2D implements VectorFunction2d {
    
    Point2D.Double[] s;
    double f[][];
    double 	mue = 2,
    R = 1.0;
    int	methode = 0;
    
    public ShepardVectorFunction2D(
    Point2D.Double[] s,	// (x[i],y[i]) = Stuetzstellen ...............
    double f[][]        	// f[i] = Stuetzwerte ........................
    ) {
        this.s = s;
        this.f = f;
    }

    public double[] getValue(  		// Shepard-Interpolation (global,lokal,FL-Gewichte)
    Point2D.Double p      	// (x0,y0) = Interpolationsstelle ............
    )
    /***********************************************************************
     * Dieses Unterprogramm berechnet einen Funktionswert zu vorgegebenen   *
     * Stuetzstellen nach dem Interpolationsverfahren von Shepard. Dabei    *
     * besteht die Auswahl zwischen der globalen Shepard-Methode, der       *
     * lokalen Shepard-Methode und der lokalen-Shepard-Methode mit          *
     * Franke-Little-Gewichten.                                             *
     *                                                                      *
     * Eingabeparameter:                                                    *
     * =================                                                    *
     * p        p ist der Punkt, an dem der Wert der                        *
     *          Shepard-Interpolationsfunktion gesucht wird.                *
     * s        Vektor mit den Stuetzstellen                                *
     * f        [0..n]-Vektor mit den Stuetzwerten                          *
     * mue      frei waehlbarer Parameter der Shepard-Methode, der als      *
     *          Exponent bei der Berechnung der Gewichte dient (mue > 0);   *
     *          gute Ergebnisse erzielt man mit 2 < mue < 6.                *
     *          Falls mue <= 0, wird mue automatisch auf den Wert 2         *
     *          gesetzt.                                                    *
     * methode  Nummer derjenigen Variante der Shepard-Methode, die zur     *
     *          Interpolation benutzt werden soll:                          *
     *          = 0: globale Methode                                        *
     *          = 1: lokale  Methode                                        *
     *          = 2: lokale  Methode mit Franke-Little-Gewichten            *
     * R        Radius fuer die lokale Methode; er bestimmt denjenigen      *
     *          Kreis um die Interpolationsstelle (x0,y0), in dem die bei   *
     *          der Interpolation zu beruecksichtigenden Stuetzstellen      *
     *          liegen; alle Stuetzstellen ausserhalb dieses Kreises werden *
     *          ignoriert.                                                  *
     *          Der Radius sollte so gewaehlt werden, dass noch genuegend   *
     *          viele Stuetzstellen in diesem Kreis liegen.                 *
     *                                                                      *
     * Ausgabeparameter:                                                    *
     * =================                                                    *
     * PHI      Interpolationswert bei (x0,y0)                              *
     *                                                                      *
     * Funktionswert:                                                       *
     * ==============                                                       *
     * Fehlercode. Folgende Werte koennen auftreten:                        *
     * = 0: alles in Ordnung                                                *
     * = 1: nicht erlaubte Eingabeparameter:                                *
     *      n < 0  oder  methode != 0,1,2  oder  R <= 0                     *
     * = 2: Alle Gewichte w[i] sind Null.                                   *
     * = 3: Speichermangel                                                  *
     *                                                                      *
     ***********************************************************************/
    
    {
        double r[];         // [0..n]-Vektor mit den Euklidischen Abstaenden
        // der Stuetzstellen von der Interpolationsstelle
        double w[];         // [0..n]-Vektor mit den Gewichten fuer die
        // Stuetzwerte; haengt ab von r und mue.
        double psi[] = null;// [0..n]-Hilfsvektor zur Berechnung der Gewichte
        // im Falle der lokalen Shepard-Methode
        double xi[] = null; // [0..n]-Hilfsvektor zur Berechnung der Gewichte
        // im Falle der lokalen Shepard-Methode mit
        // Franke-Little-Gewichten
        double norm = 0;    // 1-Norm des Gewichtsvektors vor der Normierung
        int  j;             // Laufvariable
        
        int n = s.length-1;
        
        double PHI[] = new double[f[0].length];  	// Ausgabeparameter fuer den
        // Fehlerfall vorsichtshalber
        // mit einem ziemlich abartigen
        // Wert vorbesetzen
        for (int z_vec = 0;z_vec<f[0].length;z_vec++) {
            
            if (n < 0 ||                      	// unerlaubter Wert fuer n?
            methode < 0 || methode > 2) {  	// undefinierte Shepard-Methode?
                PHI[z_vec] = Double.NaN;
                break; 		                // Fehler melden
            }
            if (methode != 0)                 	// nicht globale Methode?
                if (R <= 0)                     // unerlaubter Wert fuer R?
                    if (methode == 2)       // lokal mit Franke-Little-Gew.?
                        R = (double)0.1;// R korrigieren
                    else {                  // andere Methode?
                        PHI[z_vec] = Double.NaN;
                    }
            
            r = new double[n+1];            	// Speicher fuer drei Vektoren
            // anfordern
            w   = new double[n+1];
            if (methode != 0)
                psi = new double[n+1];
            
            xi  = psi;                      	// nur ein anderer Name fuer
            // schon vorhandenen Speicher
            
            if (mue <= 0)                     	// unerlaubter Wert fuer mue?
                mue = 2;                        // den Standardwert 2 verwenden
            
            for (j = 0; j <= n; j++) {	         // Abstaende r[j] berechnen
                r[j] = p.distance(s[j]);
                
                if (r[j] == 0) {                // Abstand Null, d. h. (x0,y0)
                    // ist eine Stuetzstelle?
                    PHI[z_vec] = f[j][z_vec];// passenden Stuetzwert
                    // zurueckgeben
                    break;
                }
            }
            
            
            switch (methode) {           		// Gewichtsvektor
                // vorbesetzen, seine
                // 1-Norm berechnen
                case 0:                  	// globale Methode?
                    for (j = 0, norm = 0; j <= n; j++) {
                        w[j] = 1 / Math.pow(r[j], mue);
                        norm += w[j];
                    }
                    break;
                case 1:                   	// lokale Methode?
                    for (j = 0; j <= n; j++)// psi[j] berechnen
                        if (r[j] >= R)
                            psi[j] = 0;
                        else
                            psi[j] = (R / r[j]) - 1;
                    
                    for (j = 0, norm = 0; j <= n; j++)  			// aus psi die
                        if (psi[j] != 0) {          			// Gewichte (noch
                            w[j] = 1 / Math.pow(psi[j], mue);   	// nicht normiert) und
                            norm += w[j];                       	// ihre Summe
                        } else {                              	    	// berechnen
                            w[j] = 0;
                        }
                    break;
                    
                case 2:                                    	 // lokale Methode mit
                    // Franke-Little-Gew.?
                    for (j = 0; j <= n; j++)                 // xi[j] berechnen
                        if (r[j] >= R)
                            xi[j] = 0;
                        else
                            xi[j] = 1 - r[j] / R;
                    
                    for (j = 0, norm = 0; j <= n; j++) {       // aus xi die Gewichte
                        w[j] =  Math.pow(xi[j], mue);      // (noch nicht
                        norm  += w[j];                     // normiert) und ihre
                    }			                   // Summe berechnen
                    break;
                    
                default:                                   	   // eigentlich
                    PHI[z_vec] = Double.NaN;
                    break;
            }
            
            if (norm == 0) {                                   // Alle Gewichte w[j]
                // sind Null?
                PHI[z_vec] = Double.NaN;                   // Fehler melden
                break;
            }
            for (j = 0; j <= n; j++)                     	   // die Gewichte
                if (w[j] != 0)                             // normieren
                    w[j] /= norm;
            
            PHI[z_vec] = 0;                         	   // Wert der Interpolationsfunktion
            for (j = 0; j <= n; j++)         		   // an der Stelle (x0,y0) berechnen
                PHI[z_vec] += w[j] * f[j][z_vec];
            
        }
        return PHI;
    }
}