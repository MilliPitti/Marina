package de.smile.marina.tools;

import de.smile.math.Function;
import java.util.*;

public abstract class Utils {    
    
    /**
     *  Die Eingangsdaten der GKSS-Boje enthalten die Richtung der Wellen und des Windes in
     *  meteorologischer Richtung, d.h. Norden gleich 0 Grad und dann im Uhrzeigersinn weiter.
     *  Fuer die weitere Rechnung mit diesen Daten im Programm benoetigen wir jedoch die
     *  Richtung im mathematischen Sinne, d.h. in mathematisch positivem Umlaufsinn und sozusagen mit 0 Grad
     *  "im Osten".
     *  Ausserdem ist es erforderlich, den Richtungsvektor um 180 Grad zu drehen, da dieser in den
     *  Eingansdaten in die Richtung zeigt, aus der die Stroemung <b>kommt</b>, wir aber fuer das Programm
     *  die Richtung brauchen, in die die Stroemung <b>laeuft</b>.
     *
     *  <img src="Richtungen.gif" width=50% alt="Erklaerende Grafik">
     *
     *  Methode, die die meteorologische Richtung der Eingangsdaten in die mathematische umrechnet.
     *  Dies wird erreicht, indem der Richtungsvekor auf dem Einheitskreis auf die x- und y-Achse projiziert
     *  wird (cos/sin). Die Vorzeichen der beiden sich ergebenden Achsenabschnitte werden umgekehrt,
     *  so dass der Vektor in die entgegengesetzte Richtung zeigt. Dan wir noch ueber den arccos der Winkel
     *  berechnet.
     *  @param dir Die meteorologische Richtung in Grad
     *  @return die mathematische Richtung in Grad
     *  @see Utils#math2met
     */
    public static double met2math(double dir) {
        double wwx = (Math.sin(dir * Math.PI / 180.));
        double wwy = (Math.cos(dir * Math.PI / 180.));
        
        double wx = -wwx;
        double wy = -wwy;
        
        double wave_dir_m = (Function.sign(wy) * Math.acos(wx) * 180. / Math.PI);
        if (Function.sign(wave_dir_m) < 0.)
            wave_dir_m = 360. + wave_dir_m;
        return wave_dir_m;
    }
    
    /**
     *  Methode, die die meteorologischen Geschwindigkeiten in x- und y-Richtung in die mathematischen umrechnet.
     *  Dies wird erreicht, indem ihre Vorzeichen umgekehrt und die Werte fuer die x- und y-Richtung vertauscht
     *  werden.
     *  @param wx_metrologische Geschwindigkeit in x-Richtung in m/s
     *  @param wy_metrologische Geschwindigkeit in y-Richtung in m/s
     *  @return 1dim Feld mit den entsprechenden mathematischen Geschwindigkeiten in m/s
     *  @see Utils#math2met
     */
    public static double[] met2math(double wx_metrologische, double wy_metrologische) {
        double[] w = new double[2];
        w[0] = -wy_metrologische;
        w[1] = -wx_metrologische;
        
        return w;
    }
    
    /**
     *  Rechnet die mathematische Richtung um in die meteorologische.
     *  @param dir_math mathematische Richtung in Grad
     *  @return die meteorologische Richtung in Grad
     *  @see Utils#met2math
     */
    public static double math2met(double dir_math) {
        double wwx = (Math.cos(dir_math * Math.PI / 180.));
        double wwy = (Math.sin(dir_math * Math.PI / 180.));
        
        double wx = -wwx;
        double wy = -wwy;
        
        double wave_dir_met = (Function.sign(wx) * Math.acos(wy) * 180. / Math.PI);
        
        if (Function.sign(wave_dir_met) < 0.)
            wave_dir_met = 360. + wave_dir_met;
        return wave_dir_met;
    }
    
    /**
     *  Rechnet die mathematischen Geschwindigkeiten um in die meteorologischen.
     *  @param wx_mathematische Geschwindigkeit in x-Richtung in m/s
     *  @param wy_mathematische Geschwindigkeit in y-Richtung in m/s
     *  @return 1dim Feld mit den entsprechenden meteorologischen Geschwindigkeiten in m/s
     *  @see Utils#met2math
     */
    public static double[] math2met(double wx_mathematische, double wy_mathematische) {
        double ww[] = new double[2];
        ww[0] = -wy_mathematische;
        ww[1] = -wx_mathematische;
        
        return ww;
    }
    
    /** Umwandlung von Jahressekunden in das entsprechende Datum des Jahres.
     * @param jahr Jahr
     * @param xftime Jahressekunden
     *
     * @return Datum im Format: "TT.MM.JAHR HH:MM" */
    public static String XfToDate(int jahr, long xftime) {
        Calendar cal = Calendar.getInstance();
        cal.set(jahr, Calendar.JANUARY, 1, 0, 0, 0);
        Date d = cal.getTime();
        long baset = d.getTime();
        
        Date dd = new Date((baset + xftime * 1000));
        cal.setTime(dd);
        
        String res = cal.get(Calendar.DAY_OF_MONTH) + "." + (cal.get(Calendar.MONTH) + 1) + "." + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
        
        return res;
    }
    
    /** Umwandlung eines Datums in Jahressekunden des entsprechenden Jahres.
     * @param tag	Tag des Monats
     * @param monat	Monat (1 bis 12)
     * @param jahr	vierstellige Jahreszahl
     * @param stunde	Stunde des Tages (0 bis 23 Uhr)
     * @param min	Minute der Stunde (0 bis 59)
     *
     * @return Jahressekunden des Jahres */
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
    
    /*--------------------------------------------------------------------------------------------
     * Berechnet die x- und y-Komponente aus gegebenem Betrag und Winkel.
     * Der Winkel muss in Grad vorliegen. 
     * Die X-Komponente steht im Ergebnis-Array an Stelle 0, die Y-Komponente an Stelle 1
     *--------------------------------------------------------------------------------------------*/
    public static double[] getComponents(double absolute_value, double angle_in_degree) {
        double[] components = new double[2];
        components[0] = Math.cos(angle_in_degree*Math.PI/180.)*absolute_value;
        components[1] = Math.sin(angle_in_degree*Math.PI/180.)*absolute_value;
        return components;
    }
}
