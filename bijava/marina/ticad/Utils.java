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
package bijava.marina.ticad;

import java.util.*;

public abstract class Utils {

    /**
     * Die Eingangsdaten der GKSS-Boje enthalten die Richtung der Wellen und des
     * Windes in meteorologischer Richtung, d.h. Norden gleich 0 Grad und dann
     * im Uhrzeigersinn weiter. Fuer die weitere Rechnung mit diesen Daten im
     * Programm benoetigen wir jedoch die Richtung im mathematischen Sinne, d.h.
     * in mathematisch positivem Umlaufsinn und sozusagen mit 0 Grad "im Osten".
     * Ausserdem ist es erforderlich, den Richtungsvektor um 180 Grad zu drehen,
     * da dieser in den Eingansdaten in die Richtung zeigt, aus der die
     * Stroemung <b>kommt</b>, wir aber fuer das Programm die Richtung brauchen,
     * in die die Stroemung <b>laeuft</b>.
     *
     * <img src="Richtungen.gif" width=50% alt="Erklaerende Grafik">
     *
     * Methode, die die meteorologische Richtung der Eingangsdaten in die
     * mathematische umrechnet. Dies wird erreicht, indem der Richtungsvekor auf
     * dem Einheitskreis auf die x- und y-Achse projiziert wird (cos/sin). Die
     * Vorzeichen der beiden sich ergebenden Achsenabschnitte werden umgekehrt,
     * so dass der Vektor in die entgegengesetzte Richtung zeigt. Dan wir noch
     * ueber den arccos der Winkel berechnet.
     *
     * @param dir Die meteorologische Richtung in Grad
     * @return die mathematische Richtung in Grad
     * @see Utils#math2met
     */
    public static double met2math(double dir) {
        double wwx = (Math.sin(dir * Math.PI / 180.));
        double wwy = Math.cos(dir * Math.PI / 180.);

        double wx = -wwx;
        double wy = -wwy;

        double wave_dir_m = sign(wy) * Math.acos(wx) * 180. / Math.PI;
        if (sign(wave_dir_m) < 0.) {
            wave_dir_m = 360. + wave_dir_m;
        }
        return wave_dir_m;
    }

    /**
     * Methode, die die meteorologischen Geschwindigkeiten in x- und y-Richtung
     * in die mathematischen umrechnet. Dies wird erreicht, indem ihre
     * Vorzeichen umgekehrt und die Werte fuer die x- und y-Richtung vertauscht
     * werden.
     *
     * @param wwx metrologische Geschwindigkeit in x-Richtung in m/s
     * @param wwy metrologische Geschwindigkeit in y-Richtung in m/s
     * @return 1dim Feld mit den entsprechenden mathematischen Geschwindigkeiten
     * in m/s
     * @see Utils#math2met
     */
    public static double[] met2math(double wwx, double wwy) {
        double[] w = new double[2];
        w[0] = -wwy;
        w[1] = -wwx;

        return w;
    }

    /**
     * Rechnet die mathematische Richtung um in die meteorologische.
     *
     * @param dir_math mathematische Richtung in Grad
     * @return die meteorologische Richtung in Grad
     * @see Utils#met2math
     */
    public static double math2met(double dir_math) {
        double wwx = Math.cos(dir_math * Math.PI / 180.);
        double wwy = Math.sin(dir_math * Math.PI / 180.);

        double wx = -wwx;
        double wy = -wwy;

        double wave_dir_met = (sign(wx) * Math.acos(wy) * 180. / Math.PI);

        if (sign(wave_dir_met) < 0.) {
            wave_dir_met = 360. + wave_dir_met;
        }
        return wave_dir_met;
    }

    /**
     * Rechnet die mathematischen Geschwindigkeiten um in die meteorologischen.
     *
     * @param wx mathematische Geschwindigkeit in x-Richtung in m/s
     * @param wy mathematische Geschwindigkeit in y-Richtung in m/s
     * @return 1dim Feld mit den entsprechenden meteorologischen
     * Geschwindigkeiten in m/s
     * @see Utils#met2math
     */
    public static double[] math2met(double wx, double wy) {
        double ww[] = new double[2];
        ww[0] = -wy;
        ww[1] = -wx;

        return ww;
    }

    /**
     * berechnet den mathematischen Winkel in Grad aus dem Vektor (wx,wy) in einem
     * mathematischen kartesischen Koordinaten-System
     *
     * @param wx
     * @param wy
     * @return
     */
    public static double mathDirection(double wx, double wy) { // Mathematische Richtung
        final double cv = Math.max(Double.MIN_NORMAL, Math.sqrt(wx * wx + wy * wy));
        double wave_dir_m = sign(wy) * Math.acos(wx / cv) * 180. / Math.PI;
        return (wave_dir_m + 360.) % 360.;
    }

    /**
     * liefert das Vorzeichen eines double -Wertes.
     *
     * @return bei positivem Vorzeichen des uebergebenen Wertes wird 1., bei
     * negativem -1. zurueckgegeben
     */
    private static double sign(double value) {
        if (value > 0.) {
            return 1.0;
        } else {
            return -1.0;
        }
    }

    /**
     * Umwandlung von Jahressekunden in das entsprechende Datum des Jahres.
     *
     * @param jahr Jahr
     * @param xftime Jahressekunden
     *
     * @return Datum im Format: "TT.MM.JAHR HH:MM"
     */
    public static String XfToDate(int jahr, long xftime) {
        Calendar cal = Calendar.getInstance();
        cal.set(jahr, Calendar.JANUARY, 1, 0, 0, 0);
        Date d = cal.getTime();
        long baset = d.getTime();

        Date dd = new Date(baset + xftime * 1000);
        cal.setTime(dd);

        String res = cal.get(Calendar.DAY_OF_MONTH) + "." + (cal.get(Calendar.MONTH) + 1) + "." + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);

        return res;
    }

    /**
     * Umwandlung eines Datums in Jahressekunden des entsprechenden Jahres.
     *
     * @param tag	Tag des Monats
     * @param monat	Monat (1 bis 12)
     * @param jahr	vierstellige Jahreszahl
     * @param stunde	Stunde des Tages (0 bis 23 Uhr)
     * @param min	Minute der Stunde (0 bis 59)
     *
     * @return Jahressekunden des Jahres
     */
    public static long DateToXf(int tag, int monat, int jahr, int stunde, int min) {
        Calendar cal = Calendar.getInstance();
        cal.set(jahr, Calendar.JANUARY, 1, 0, 0, 0);
        Date d = cal.getTime();
        long baset = d.getTime();

        cal.set(jahr, monat - 1, tag, stunde, min, 0);
        Date dd = cal.getTime();
        long time = dd.getTime();

        long res = (time - baset) / 1000;

        return res;
    }
}
