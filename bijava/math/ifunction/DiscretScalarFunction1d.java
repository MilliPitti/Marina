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

//==========================================================================//
//  KLASSE DiscretScalarFunction1d                                          //
//==========================================================================//
//  Eindimensionale diskrete skalare Functionen                             //  
//                                                                          //
//  VERSION: 1.1, Juli 2000                                                 //
//                                                                          //
//  AUTHOR:  Institut fuer Bauinformatik, Universitaet Hannover             //
//           Dr.-Ing. Peter Milbradt, Dipl.-Ing. Martin Rose                //
//==========================================================================//
/**
 * "DiscretScalarFunction1d" stellt eine Klasse f&uuml;r eine diskrete Funktion
 * von eindimensionalen Skalaren zur Verf&uuml;gung.
 *
 * <p><strong>Version:</strong>
 * <br><dd>1.1, Juli 2000
 * <p><strong>Author:</strong>
 * <br><dd>Institut f&uuml;r Bauinformatik
 * <br><dd>Universit&auml;t Hannover
 * <br><dd>Dr.-Ing. Peter Milbradt, Dipl.-Ing. Martin Rose
 */
//==========================================================================// 
public class DiscretScalarFunction1d implements ScalarFunction1d {

    protected double werte[][];              // Feld der skalaren double-Werte
    protected int anz;                    // Anzahl der Stuetzstellen
    protected double xmin, xmax;             // Grenzen des Definitionsbereichs
    protected boolean periodic = false;       // Kennung: periodische Funktion
    protected double epsilon = 0.00001;      // Epsilon-Umgebung eines Wertes  
    private int actPosition = 0;

    protected DiscretScalarFunction1d() {
    }

//--------------------------------------------------------------------------//
//  UMWANDLUNGSDKONSTRUKTOR                                                 //
//--------------------------------------------------------------------------//
    /**
     * Erzeugt eine diskrete Funktion von eindimensionalen skalaren Werten
     * f&uuml;r &auml;quidistante Argumente in einem festgelegten
     * Definitionsbereich.
     *
     * @param n Anzahl der Argumente der diskreten Funktion.
     * @param xmin minimales Argument des Definitionsbereichs
     * @param xmax maximales Argument des Definitionsbereichs
     */
//--------------------------------------------------------------------------//	
    public DiscretScalarFunction1d(int n, double xmin, double xmax) {
        anz = n;
        werte = new double[2][anz];

        for (int i = 0; i < anz; i++) {
            werte[0][i] = xmin + i * (xmax - xmin) / (anz - 1);
            werte[1][i] = 0.0;
        }

        this.xmin = xmin;
        this.xmax = xmax;
    }

//--------------------------------------------------------------------------//
//  UMWANDLUNGSDKONSTRUKTOR                                                 //
//--------------------------------------------------------------------------//
    /**
     * Erzeugt eine diskrete Nullfunktion von eindimensionalen skalaren Werten.
     *
     * @param arguments Eindimensionales Feld <code>[x]</code> mit den diskreten
     * Argumenten der Funktion. Das Feld <code>values</code> wird kopiert.
     */
//--------------------------------------------------------------------------//	
    public DiscretScalarFunction1d(double[] arguments) {
        anz = arguments.length;
        werte = new double[2][anz];

        for (int i = 0; i < anz; i++) {
            werte[0][i] = arguments[i];
            werte[1][i] = 0.0;
        }

        xmin = arguments[0];
        xmax = arguments[anz - 1];
    }

//--------------------------------------------------------------------------//
//  UMWANDLUNGSDKONSTRUKTOR                                                 //
//--------------------------------------------------------------------------//
    /**
     * Erzeugt eine diskrete Funktion von eindimensionalen skalaren Werten.
     *
     * @param values Zweidimensionales Feld <code>[x][value]</code> mit den
     * Werten <code>value</code> zu den diskreten Argumenten <code>x</code>. Das
     * Feld <code>values</code> wird kopiert.
     */
//--------------------------------------------------------------------------//	
    public DiscretScalarFunction1d(double[][] values) {
        anz = values[0].length;
        werte = new double[2][anz];

        for (int i = 0; i < anz; i++) {
            werte[0][i] = values[0][i];
            werte[1][i] = values[1][i];
        }

        xmin = werte[0][0];
        xmax = werte[0][anz - 1];
    }

//--------------------------------------------------------------------------//
// GLEICHHEIT                                                               //
//--------------------------------------------------------------------------//
    /**
     * Pr&uuml;ft die diskrete Skalarfunktion auf Gleichheit.
     *
     * @param object Objekt, das auf Gleichheit gepr&uuml;ft wird.
     *
     * @return	<code>true</code>, wenn <code>object</code> eine Instanz der
     * Klasse <code>DiscretScalarFunction1d</code> ist, die Funktionen den
     * gleichen Definitionsbereich haben und die Funktionsverl&auml;fe mit einer
     * maximalen Abweichung von <code>epsilon</code> &uuml;bereinstimmen.
     */
//--------------------------------------------------------------------------//
    @Override
    public boolean equals(Object object) {

        if(this == object) return true;
        if(object == null || !(object instanceof DiscretScalarFunction1d)) return false;

        DiscretScalarFunction1d fkt = (DiscretScalarFunction1d) object;

//..Gleichheit der Definitionsbereiche pruefen..............................//
        if (Math.abs(xmin - fkt.xmin) > epsilon) {
            return false;
        }
        if (Math.abs(xmax - fkt.xmax) > epsilon) {
            return false;
        }

//..Liegen die diskreten Funktionswerte auf dem Verlauf von fkt?............//
        for (int i = 0; i < anz; i++) {
            if (Math.abs(werte[1][i] - fkt.getValue(werte[0][i])) > epsilon) {
                return false;
            }
        }

//..Liegen die diskreten Werte von fkt auf dem Verlauf der Funktion?........//
        for (int i = 0; i < fkt.anz; i++) {
            if (Math.abs(fkt.werte[1][i] - getValue(fkt.werte[0][i])) > epsilon) {
                return false;
            }
        }

        return true;
    }

//--------------------------------------------------------------------------//
//  HOLEN EINES FUNKTIONSWERTES                                             //
//--------------------------------------------------------------------------//
    /**
     * Liefert den Wert zu einem diskreten Funktionsargument.
     *
     * @param i Position des diskreten Arguments in der Funktion
     *
     * @return Feld mit dem Argument und dem Wert des diskreten Gr&ouml;sse
     */
//--------------------------------------------------------------------------//
    public double[] getValueAt(int i) {
        double[] d = new double[2];
        d[0] = werte[0][i];
        d[1] = werte[1][i];
        return d;
    }

//--------------------------------------------------------------------------//
//  HOLEN EINES FUNKTIONSWERTES                                             //
//--------------------------------------------------------------------------//
    /**
     * Liefert den Wert zu einem Argument x.<p>
     *
     * @param x
     * @return Der Definitionsbereich der diskreten Funktion geht von der ersten
     * St&uuml,tzstelle (einschliesslich) bis zur letzten St&uuml,tzstelle
     * (einschliesslich). Ist die Funktion nicht periodisch, so liefert getValue
     * ausserhalb des Definitionsbereichs NaN.
     */
//--------------------------------------------------------------------------//	
    @Override
    public double getValue(double x) {
        double x1, x2, y1, y2;

//      System.out.println(periodic);
        if (periodic) {
            while (x >= xmax) {
                x -= xmax - xmin;
            }
            while (x < xmin) {
                x += xmax - xmin;
            }
        } // dass muss schneller gehen
        //      else if ((x < xmin) || (x > xmax)){
        //          System.out.println("out of the definition area");
        //          return y;
        //      }
        else {
            if (x < xmin) {
                return werte[1][0];
            }
            if (x > xmax) {
                return werte[1][anz - 1];
            }
        }
        try {
// peters Optimierung
            x1 = werte[0][actPosition];
            x2 = werte[0][actPosition + 1];
            if (!((x >= x1) && (x <= x2))) {
                actPosition = min(actPosition + 1, anz - 2);
                x1 = werte[0][actPosition];
                x2 = werte[0][actPosition + 1];
                if ((x >= x1) && (x <= x2)) {
                    y1 = werte[1][actPosition];
                    y2 = werte[1][actPosition + 1];
                    return (y1 + ((x - x1) / (x2 - x1)) * (y2 - y1));
                }
            } else {
                y1 = werte[1][actPosition];
                y2 = werte[1][actPosition + 1];
                return (y1 + ((x - x1) / (x2 - x1)) * (y2 - y1));
            }
        } catch (Exception ex) {
        }

        // geaendert Christoph: java.util.Arrays-Methoden ausnutzen
        int insertion_point = java.util.Arrays.binarySearch(werte[0], x);

        if (insertion_point >= 0) {
            actPosition = insertion_point;
            return werte[1][insertion_point];
        } else {
            actPosition = (-1) * insertion_point - 2;
            x1 = werte[0][actPosition];
            y1 = werte[1][actPosition];
            x2 = werte[0][actPosition + 1];
            y2 = werte[1][actPosition + 1];
            return (y1 + ((x - x1) / (x2 - x1)) * (y2 - y1));
        }
    }

//--------------------------------------------------------------------------//
//  SETZEN EINES FUNKTIONSWERTES                                            //
//--------------------------------------------------------------------------//
    /**
     * Setzt den Wert zu einem diskreten Argument.
     *
     * @param i Position des diskreten Arguments in der Funktion
     * @param value zu setzender Funktionswert
     */
//--------------------------------------------------------------------------//	
    public void setValueAt(int i, double value) {
        werte[1][i] = value;
    }

//--------------------------------------------------------------------------//
//  SETZEN ALLER FUNKTIONSWERTE MIT DEN WERTEN EINER SKALARFUNKTION         //
//--------------------------------------------------------------------------//
    /**
     * Setzt die Werte an aller diskreter Argumente der Skalarfunktion mit den
     * Werten einer beliebigen Skalarfunktion.
     *
     * @param function beliebige eindiamnesionale Funktion mit skalaren Werten.
     */
//--------------------------------------------------------------------------//	
    public void setValues(ScalarFunction1d function) {
        for (int i = 0; i < anz; i++) {
            werte[1][i] = function.getValue(werte[0][i]);
        }

        xmin = werte[0][0];
        xmax = werte[0][anz - 1];
    }

//--------------------------------------------------------------------------//
//  ANZAHL DER DISKRETEN WERTE                                              //
//--------------------------------------------------------------------------//
    /**
     * Liefert die Anzahl der diskreten Werte der Skalarfunktion.
     * @return 
     */
//--------------------------------------------------------------------------//  
    public int getSizeOfValues() {
        return anz;
    }

//--------------------------------------------------------------------------//
//  MINIMUM DER FUNKTION                                                    //
//--------------------------------------------------------------------------//
    /**
     * Liefert den minimalen Wert des Wertebreichs der diskreten Skalarfunktion.
     *
     * @return Feld mit dem Argument und dem minimalen Funktionswert.
     */
//--------------------------------------------------------------------------//	
    public double[] getmin() {
        double y[] = new double[2];
        y[0] = werte[0][0];
        y[1] = werte[1][0];

        for (int i = 1; i < anz; i++) {
            if (werte[1][i] < y[1]) {
                y[0] = werte[0][i];
                y[1] = werte[1][i];
            }
        }

        return y;
    }

//--------------------------------------------------------------------------//
//  MAXIMUM DER FUNKTION                                                    //
//--------------------------------------------------------------------------//
    /**
     * Liefert den maximalen Wert des Wertebreichs der diskreten Skalarfunktion.
     *
     * @return Feld mit dem Argument und dem maximalen Funktionswert.
     */
//--------------------------------------------------------------------------//  
    public double[] getmax() {
        double y[] = new double[2];
        y[0] = werte[0][0];
        y[1] = werte[1][0];

        for (int i = 1; i < anz; i++) {
            if (werte[1][i] > y[1]) {
                y[0] = werte[0][i];
                y[1] = werte[1][i];
            }
        }

        return y;
    }

//--------------------------------------------------------------------------//
//  MINIMUM DES DEFINITIONSBEREICHS                                         //
//--------------------------------------------------------------------------//
    /**
     * Liefert den minimalen Wert des Definitionsbreichs der diskreten
     * Skalarfunktion.
     *
     * @return minimales diskretes Argument des Definitionsbreichs.
     */
//--------------------------------------------------------------------------// 
    public double getxmin() {
        return werte[0][0];
    }

//--------------------------------------------------------------------------//
//  MAXIMUM DES DEFINITIONSBEREICHS                                         //
//--------------------------------------------------------------------------//
    /**
     * Liefert den maximalen Wert des Definitionsbreichs der diskreten
     * Skalarfunktion.
     *
     * @return maximales diskretes Argument des Definitionsbreichs.
     */
//--------------------------------------------------------------------------//  
    public double getxmax() {
        return werte[0][anz - 1];
    }

//--------------------------------------------------------------------------//
//  GEGLAETTETER WERT                                                       //
//--------------------------------------------------------------------------//
    /**
     * Erzeugt einen gemittelten Wert für ein Argument und die umliegenden
     * Argumente einer diskrteten Skalarfunktion.
     *
     * @param i Position des diskreten Arguments in der Funktion
     * @param n Anzahl der diskreten Argumente, die links bzw. rechts neben dem
     * Argument i liegen. Mit den Werten dieser Argumente wird der Mittelwert
     * gebildet. Ist die Funktion nicht periodisch und &uuml;berschreitet n den
     * Definitionsbreich der Funktion, so werden nur die Werte bis zur minimalen
     * bzw. maximalen Bereichsgrenze ber&uuml;cksichtigt.
     * @return 
     */
//--------------------------------------------------------------------------//
    public double getSmoothValue(int i, int n) {
        double sum = 0.0;
        int zaehler = 0;

        for (int j = (i - n); j <= (i + n); j++) {
            if (j < 0 && periodic) {
                sum += werte[1][anz + j];
                zaehler++;
            } else if (j >= anz && periodic) {
                sum += werte[1][anz - j];
                zaehler++;
            } else if (j >= 0 && j < anz) {
                sum += werte[1][j];
                zaehler++;
            }
        }
        return (sum / zaehler);
    }

//--------------------------------------------------------------------------//
//  GEGLAETTETE FUNKTION                                                    //
//--------------------------------------------------------------------------//
    /**
     * Erzeugt eine geglätte Funktion der diskrteten Skalarfunktion.
     *
     * @param n Anzahl der diskreten Argumente mit denen mittels der Methode
     * <code>getSmoothValue (i, n)</code> der Mittelwert gebildet wird.
     *
     * @return Glatte diskrteten Skalarfunktion
     */
//--------------------------------------------------------------------------//
    public DiscretScalarFunction1d getSmoothFunction(int n) {
        double[][] w = new double[2][anz];
        w[0] = werte[0];

        for (int i = 0; i < anz; i++) {
            w[1][i] = this.getSmoothValue(i, n);
        }

        return new DiscretScalarFunction1d(w);
    }

//--------------------------------------------------------------------------//
//  SETZEN EINER PERIODIZITAET                                              //
//--------------------------------------------------------------------------//
    /**
     * Erzeugt oder zerst&ouml;rt die Periodizit&auml;t der diskreten
     * Skalarfunktion.
     *
     * <p>Vorsicht! F&uuml;r eine periodische Funktion muss der erste
     * Funktionswert gleich dem letzten Funktionswert sein. Um dies zu
     * gew&auml;hrleisten, werden die beiden Grenzwerte durch ihren Mittelwert
     * ersetzt.
     *
     * @param periodic <code>true</code>, wenn die Funktion einer Kreisfunktion
     * entspricht und <code>false</code>, wenn sie keiner Kreisfunktion
     * entspricht.
     */
//--------------------------------------------------------------------------//	
    @Override
    public void setPeriodic(boolean periodic) {
        this.periodic = periodic;

//    if (periodic)
//      werte[1][0] = werte[1][anz-1] = 0.5 * (werte[1][0] + werte[1][anz-1]);
    }

//--------------------------------------------------------------------------//
//  IST DIE FUNKTION PERIODISCH ?                                           //
//--------------------------------------------------------------------------//
    /**
     * Pr&uuml;ft, ob die diskrete Skalarfunktion periodisch ist.
     *
     * @return <code>true</code>, wenn die Funktion einer Kreisfunktion
     * entspricht und <code>false</code>, wenn sie keiner Kreisfunktion
     * entspricht.
     */
//--------------------------------------------------------------------------//	
    @Override
    public boolean isPeriodic() {
        return periodic;
    }

//--------------------------------------------------------------------------//
//  HOLEN DER EPSILON-UMGEBUNG                                              //
//--------------------------------------------------------------------------//
    /**
     * Liefert den Epsilon-Wert f&uuml;r den Umgenbungsbereich eines
     * Funktionswerte.
     * @return 
     */
//--------------------------------------------------------------------------//	
    public double getEpsilon() {
        return epsilon;
    }

//--------------------------------------------------------------------------//
//  SETZEN DER EPSILON-UMGEBUNG                                             //
//--------------------------------------------------------------------------//
    /**
     * Setzt den Epsilon-Wert f&uumnl;r den Umgebungsbereich eines
     * Funktionswertes.
     *
     * @param epsilon Dieser Wert sollte deutlich kleiner als eins sein umd muss
     * in jedem Fall positiv sein. Sollte <code>epsilon</code> negativ oder
     * gr&ouml;sser als eins sein, so wird die Umgebungsvariable nicht neu
     * gesetzt.
     */
//--------------------------------------------------------------------------//	
    public void setEpsilon(double epsilon) {
        if (epsilon < 1.0 && epsilon >= 0.0) {
            this.epsilon = epsilon;
        }
    }

//--------------------------------------------------------------------------//
//  VORWAERTSDIFFERENZ                                                      //
//--------------------------------------------------------------------------//
    /**
     * Bildet die Vorw&auml;rtsdifferenz f&uuml;r einen Funktionswert innerhalb
     * der diskreten Skalarfunktion.
     *
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @return 
     */
//--------------------------------------------------------------------------//
    public double frontDifference(int i) {
        if (i >= (anz - 1)) {
            if (periodic) {
                return frontDifference(0);
            } else {
                return rearDifference(anz - 1);
            }
        }

        return (werte[1][i + 1] - werte[1][i]) / (werte[0][i + 1] - werte[0][i]);
    }

//--------------------------------------------------------------------------//
//  RUECKWAERTSDIFFERENZ                                                    //
//--------------------------------------------------------------------------//
    /**
     * Bildet die R&uuml;ckw&auml;rtsdifferenz f&uuml;r einen Funktionswert
     * innerhalb der diskreten Skalarfunktion.
     *
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @return 
     */
//--------------------------------------------------------------------------//	
    public double rearDifference(int i) {
        if (i <= 0) {
            if (periodic) {
                return rearDifference(anz - 1);
            } else {
                return frontDifference(0);
            }
        }

        return (werte[1][i] - werte[1][i - 1]) / (werte[0][i] - werte[0][i - 1]);
    }

//--------------------------------------------------------------------------//
//  ZENTRALE DIFFERENZ                                                      //
//--------------------------------------------------------------------------//
    /**
     * Bildet die zentrale Differenz f&uuml;r einen Funktionswert innerhalb der
     * diskreten Skalarfunktion.
     *
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @return 
     */
//--------------------------------------------------------------------------//	
    public double centralDifference(int i) {
        return upwindDifference(i, 0.5);
    }

//--------------------------------------------------------------------------//
//  UPWINDING-DIFFERENZ                                                     //
//--------------------------------------------------------------------------//
    /**
     * Bildet eine gewichtete Differenz f&uuml;r einen Funktionswert innerhalb
     * der diskreten Skalarfunktion.
     *
     * <p>Mit dem Upwinding-Koeffizient
     * <code>alpha</code> wird der Anteil der Vorw&auml;rtsdifferenz zur
     * R&uuml;ckw&auml;rtsdifferenz gewichtet.
     *
     * <p>Bei
     * <code>alpha = 0.0</code> ergibt sich eine Vorw&auml;rtsdifferenz, bei
     * <code>alpha = 0.5</code> ergibt sich eine zentrale Differenz und bei
     * <code>alpha = 1.0</code> ergibt sich eine R&uuml;ckw&auml;rtsdifferenz.
     *
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @param alpha Upwinding-Koeffizient (0.0 bis 1.0)
     * @return 
     */
//--------------------------------------------------------------------------//
    public double upwindDifference(int i, double alpha) {
        return alpha * rearDifference(i) + (1.0 - alpha) * frontDifference(i);
    }

//--------------------------------------------------------------------------//
//  DIFFERENZ ZUR ZWEITEN ABLEITUNG                                         //
//--------------------------------------------------------------------------//
    /**
     * Bildet die Differenz zur zweiten Ableitung eines Funktionswerts innerhalb
     * der diskreten Skalarfunktion.
     *
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @return 
     */
//--------------------------------------------------------------------------//	
    public double secondDifference(int i) {
        double deltaX;

        if (i <= 0 || i >= (anz - 1)) {
            if (periodic) {
                deltaX = (werte[0][1] - werte[0][0] + werte[0][anz - 1] - werte[0][anz - 2]);
            } else {
                return 0.0;
            }
        } else {
            deltaX = (werte[0][i + 1] - werte[0][i - 1]);
        }

        return 2.0 * (frontDifference(i) - rearDifference(i)) / deltaX;
    }

//--------------------------------------------------------------------------//
//  ABLEITUNG                                                               //
//--------------------------------------------------------------------------//
    /**
     * Liefert die Ableitung zu einem Argument x.
     * @param x
     * @return 
     */
//--------------------------------------------------------------------------//
    @Override
    public double getDifferential(double x) {
        if (periodic) {
            while (x >= xmax) {
                x -= xmax;
            }
            while (x < xmin) {
                x += xmin;
            }
        }

        double y = Double.NaN;
        double x1, x2, y1, y2;
        int i, j, pos;

        pos = anz;

        for (i = 0; i < anz; i++) {
            if (werte[0][i] > x) {
                pos = i - 1;
                i = anz;
            }
        }

        if (pos < anz) {
            x1 = werte[0][pos];
            y1 = werte[1][pos];
            x2 = werte[0][pos];
            y2 = werte[1][pos];

            for (j = pos; j < anz; j++) {
                if (werte[0][j] > x) {
                    x2 = werte[0][j];
                    y2 = werte[1][j];
                    j = anz;
                }
            }

            if (x1 != x2) {
                y = (y2 - y1) / (x2 - x1);
            } else {
                y = 1.0;
            }
        }
        return y;
    }

//--------------------------------------------------------------------------//
//  ZEICHENKETTE                                                            //
//--------------------------------------------------------------------------//
    /**
     * Konvertiert die eindimensionale diskrete Skalarfunktion in eine
     * Zeichenkette.
     */
//--------------------------------------------------------------------------//
    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < anz; i++) {
            s += " " + werte[0][i] + " " + werte[1][i];
        }
        return s;
    }

    private int min(int a, int b) {
        return (a <= b) ? a : b;
    }
}
