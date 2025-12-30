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


/** "DiscretVectorFunction1d" stellt eine Klasse f&uuml;r eine diskrete
 * Funktion von eindimensionalen Skalaren zur Verf&uuml;gung.
 *
 * <p><strong>Version:</strong>
 * <br><dd>1.1, Juli 2000
 * <p><strong>Author:</strong>
 * <br><dd>Institut f&uuml;r Bauinformatik
 * <br><dd>Universit&auml;t Hannover
 * @author Peter Milbradt
 * @version 1.1
 */
//==========================================================================//
public class DiscretVectorFunction1d implements VectorFunction1d {
    private double   x[];
    private double   werte[][];              // Feld der skalaren double-Werte
    private int      anz;                   // Anzahl der Stuetzstellen
    private int      vlaenge;               // Laenge der vectoriellen Groesse
    private double   xmin, xmax;             // Grenzen des Definitionsbereichs
    private boolean  periodic = false;       // Kennung: periodische Funktion
    private double   epsilon = 0.00001;      // Epsilon-Umgebung eines Wertes
    
    //--------------------------------------------------------------------------//
    //  UMWANDLUNGSDKONSTRUKTOR                                                 //
    //--------------------------------------------------------------------------//
    /** Erzeugt eine diskrete Funktion von eindimensionalen skalaren Werten
     *  f&uuml;r &auml;quidistante Argumente in einem festgelegten
     *  Definitionsbereich.
     *
     * @param n    Vektorlaenge
     *  @param n    Anzahl der Argumente der diskreten Funktion.
     *  @param xmin minimales Argument des Definitionsbereichs
     *  @param xmax maximales Argument des Definitionsbereichs                  */
    //--------------------------------------------------------------------------//
    public DiscretVectorFunction1d(int v,int n, double xmin_, double xmax_) {
        anz = n;
        vlaenge=v;
        werte = new double[vlaenge][anz];
        x = new double [anz];
        
        for (int i=0; i<anz; i++) {
            x[i] = xmin + i*(xmax-xmin)/(anz-1);
            for (int j=0;j<vlaenge ;j++ ) {
                
                werte[j][i] = 0.0;
            }
        }
        xmin = xmin_;
        xmax = xmax_;
    }
    
    //--------------------------------------------------------------------------//
    //  UMWANDLUNGSDKONSTRUKTOR                                                 //
    //--------------------------------------------------------------------------//
    /** Erzeugt eine diskrete Nullfunktion von v-dimensionalen vektoriellen Werten.
     *
     * @param v dimension der Vektorwerte
     * @param arguments Eindimensionales Feld <code>[value]</code> mit den
     *                   diskreten Argumenten der Funktion.
     *                   Das Feld <code>values</code> wird kopiert.             */
    //--------------------------------------------------------------------------//
    public DiscretVectorFunction1d(int v,double[] arguments) {
        anz   = arguments.length;
        vlaenge=v;
        x=new double [anz];
        werte = new double[vlaenge][anz];
        
        for (int i=0; i<anz; i++) {
            x[i] = arguments[i];
            for (int j=0;j<vlaenge ;j++ ) {
                
                werte[j][i] = 0.0;
            }
        }
        
        xmin  = arguments[0];
        xmax  = arguments[anz-1];
    }
    
    //--------------------------------------------------------------------------//
    //  UMWANDLUNGSDKONSTRUKTOR                                                 //
    //--------------------------------------------------------------------------//
    /** Erzeugt eine diskrete Funktion von eindimensionalen skalaren Werten.
     *
     *  @param values Zweidimensionales Feld <code>[2][value]</code> mit den
     *                Werten <code>value</code> zu den diskreten Argumenten
     *                <code>x</code>. Das Feld <code>values</code> wird kopiert.*/
    //--------------------------------------------------------------------------//
    public DiscretVectorFunction1d(double arguments[],double[][] values) {
        anz   = arguments.length;
        werte = new double[values[0].length][anz];
        x=new double [anz];
        
        for (int i=0; i<anz; i++) {
            x[i] = arguments[i];
            for (int j=0;j<vlaenge ;j++ ) {
                
                werte[j][i] = 0.0;
            }
        }
        
        xmin =x[0];
        xmax = x[anz-1];
    }
    
    //--------------------------------------------------------------------------//
    // GLEICHHEIT                                                               //
    //--------------------------------------------------------------------------//
    /** Pr&uuml;ft die diskrete Skalrfunktion auf Gleichheit.
     *
     *  @param object Objekt, das auf Gleichheit gepr&uuml;ft wird.
     *
     *  @return	<code>true</code>, wenn <code>object</code> eine Instanz der
     *		Klasse <code>DiscretScalarFunction1d</code> ist,
     *              die Funktionen den gleichen Definitionsbereich haben und die
     *              Funktionsverl&auml;fe mit einer maximalen Abweichung von
     *              <code>epsilon</code> &uuml;bereinstimmen.                   */
    //--------------------------------------------------------------------------//
    public synchronized boolean epsilonEquals(Object object) {
        if (!(object instanceof DiscretVectorFunction1d)) return false;
        
        DiscretVectorFunction1d fkt = (DiscretVectorFunction1d) object;
        
        //..Gleichheit der Definitionsbereiche pruefen..............................//
        if (Math.abs(xmin-fkt.xmin)>epsilon) return false;
        if (Math.abs(xmax-fkt.xmax)>epsilon) return false;
        
        //..Liegen die diskreten Funktionswerte auf dem Verlauf von fkt?............//
        for (int i=0; i<anz; i++)
            for (int j=0;j<vlaenge ;j++ )
                if (Math.abs(werte[j][i] - fkt.getValue(x[i])[j])>epsilon)
                    return false;
        
        //..Liegen die diskreten Werte von fkt auf dem Verlauf der Funktion?........//
        for (int i=0; i<fkt.anz; i++)
            for (int j=0;j<vlaenge ;j++ )
                if (Math.abs(fkt.werte[j][i] - getValue(fkt.x[i])[j])>epsilon)
                    return false;
        
        return true;
    }
    
    //--------------------------------------------------------------------------//
    //  HOLEN EINES FUNKTIONSWERTES                                             //
    //--------------------------------------------------------------------------//
    /** Liefert den Wert zu einem diskreten Funktionsargument.
     *
     *  @param i Position des diskreten Arguments in der Funktion
     *
     *  @return Feld mit dem Argument und dem Wert des diskreten Gr&ouml;sse    */
    //--------------------------------------------------------------------------//
    public double[] getValueAt(int i) {
        double[] d = new double[vlaenge];
        for (int j=0;j<vlaenge ;j++ ) {
            d[j]=	werte[j][i];
        }
        return d;
    }
    
    /**
     * 
     * @param v 
     * @param i 
     * @return 
     */
    public double getValueAt(int v,int i) {
        
        return werte[v][i];
    }
    
    
    
    
    //--------------------------------------------------------------------------//
    //  HOLEN EINES FUNKTIONSWERTES                                             //
    //--------------------------------------------------------------------------//
    /** Liefert den Wert zu einem Argument x.<p>
     *
     *  @return Der Definitionsbereich der diskreten Funktion geht von der
     *          ersten St&uuml,tzstelle (einschliesslich) bis zur letzten
     *          St&uuml,tzstelle (einschliesslich). Ist die Funktion nicht
     *          periodisch, so liefert getValue ausserhalb des Definitionsbereichs
     *          NaN.                                                            */
    //--------------------------------------------------------------------------//
    public double[] getValue(double xx) {
        double[] d = new double[vlaenge];
        for (int j=0;j<vlaenge ;j++ ) {
            d[j]=	Double.NaN;
        }
        double x1    = 0.0, x2 = 0.0;
        int    i, pos;
        
        pos     = anz;
        
        if (periodic) {while (xx >= xmax) xx -= xmax; while (xx<xmin) xx += xmin; }
        
        for (i=0; i<anz; i++) {
            if ( Math.abs(x[i]-xx) < 1.E-7 ) {
                for (int j=0;j<vlaenge ;j++ ) {
                    d[j]=	werte[j][i];
                }
                return d;
            }
            if (x[i]>xx) { pos = i-1; i = anz; }
        }
        
        if (pos<anz){
            x1 = x[pos];
            x2 = x[pos+1];
            
            if (x1 != x2)
                for (int j=0;j<vlaenge ;j++ )
                    d[j] = werte[j][pos] + ((xx-x1)/(x2-x1))*(werte[j][pos+1]-werte[j][pos]);
            else
                for (int j=0;j<vlaenge ;j++ )
                    d[j] = werte[j][pos];
        }
        return d;
    }
    
    /**
     * 
     * @param xx 
     * @return 
     */
    public double[] getDifferential(double xx){
        double[] d = new double[vlaenge];
        for (int j=0;j<vlaenge ;j++ ) {
            d[j]=	Double.NaN;
        }
        double x1    = 0.0, x2 = 0.0;
        int    i, pos;
        
        pos     = anz;
        
        if (periodic) {while (xx >= xmax) xx -= xmax; while (xx<xmin) xx += xmin; }
        
        for (i=0; i<anz; i++) {
            if ( Math.abs(x[i]-xx) < 1.E-7 ) {
                for (int j=0;j<vlaenge ;j++ ) {
                    d =	centralDifference(i);
                }
                return d;
            }
            if (x[i]>xx) { pos = i-1; i = anz; }
        }
        
        if (pos<anz){
            x1 = x[pos];
            x2 = x[pos+1];
            
            if (x1 != x2)
                for (int j=0;j<vlaenge ;j++ )
                    d[j] = (werte[j][pos+1]-werte[j][pos])/(x2-x1);
            else
                for (int j=0;j<vlaenge ;j++ )
                    d[j] = 0.;
        }
        return d;
    }
    
    //--------------------------------------------------------------------------//
    //  HOLEN EINES FUNKTIONSWERTES                                             //
    //--------------------------------------------------------------------------//
    /** Liefert den Wert zu einem Argument x mit Hilfe eines Shepard-Verfahrens.
     *
     *  @param x Argument des Funktionswertes                                   */
    //--------------------------------------------------------------------------//
/*  public double getValuebyShepard (double x)
  { double ret = 0.0;
 
    computeWeight(x);
 
    if (treffer) return werte[1][treffi];
    for (int i=0; i<anz; i++) ret += weight[i]*werte[1][i];
    return ret;
  }
 */
    //--------------------------------------------------------------------------//
    //  SETZEN EINES FUNKTIONSWERTES                                            //
    //--------------------------------------------------------------------------//
    /** Setzt den Wert zu einem diskreten Argument.
     *
     *  @param n     Position im Vektor
     *  @param i     Position des diskreten Arguments in der Funktion
     *  @param value zu setzender Funktionswert                                 */
    //--------------------------------------------------------------------------//
    public void setValueAt(int v,int i, double value){
        werte[v][i] = value;
    }
    
    /** Setzt den Wert zu einem diskreten Argument.
     *
     *  @param n     Position im Vektor
     *  @param value zu setzender vectorwertiger Funktionswert                */
    public void setValueAt(int i, double[] value){
        for (int j=0;j<vlaenge ;j++ )
            werte[j][i] = value[j];
    }
    
    //--------------------------------------------------------------------------//
    //  SETZEN ALLER FUNKTIONSWERTE MIT DEN WERTEN EINER SKALARFUNKTION         //
    //--------------------------------------------------------------------------//
    /** Setzt die Werte an aller diskreter Argumente der Skalarfunktion mit den
     *  Werten einer  beliebigen Skalarfunktion.
     *
     *  @param function beliebige eindiamnesionale Funktion mit skalaren Werten.*/
    //--------------------------------------------------------------------------//
/*  public void setValues (ScalarFunction1d function)
  { for (int i=0; i<anz; i++) werte[1][i] = function.getValue(werte[0][i]);
 
    xmin = werte[0][0];
    xmax = werte[0][anz-1];
  }
 */
    //--------------------------------------------------------------------------//
    //  ANZAHL DER DISKRETEN WERTE                                              //
    //--------------------------------------------------------------------------//
    /** Liefert die Anzahl der diskreten Werte der Skalarfunktion.              */
    //--------------------------------------------------------------------------//
    public int getSizeOfValues() { return anz;	}
    
    //--------------------------------------------------------------------------//
    //  MINIMUM DER FUNKTION                                                    //
    //--------------------------------------------------------------------------//
    /** Liefert den minimalen Wert des Wertebreichs der diskreten Skalarfunktion.
     *
     *  @return Feld mit dem Argument und dem minimalen Funktionswert.          */
    //--------------------------------------------------------------------------//
    public double getmin(int v) {
        double y;
        y = werte[v][0];
        
        for (int i=1; i<anz; i++) {
            if (werte[v][i] < y) { y = werte[v][i];}
        }
        
        return y;
    }
    
    //--------------------------------------------------------------------------//
    //  MAXIMUM DER FUNKTION                                                    //
    //--------------------------------------------------------------------------//
    /** Liefert den maximalen Wert des Wertebreichs der diskreten Skalarfunktion.
     *
     *  @return Feld mit dem Argument und dem maximalen Funktionswert.          */
    //--------------------------------------------------------------------------//
    public double getmax(int v) {
        double y;
        y = werte[v][0];
        
        for (int i=1; i<anz; i++) {
            if (werte[v][i] > y) { y = werte[v][i];}
        }
        return y;
    }
    
    //--------------------------------------------------------------------------//
    //  MINIMUM DES DEFINITIONSBEREICHS                                         //
    //--------------------------------------------------------------------------//
    /** Liefert den minimalen Wert des Definitionsbreichs der diskreten
     *  Skalarfunktion.
     *
     *  @return minimales diskretes Argument des Definitionsbreichs.            */
    //--------------------------------------------------------------------------//
    public double getxmin() { return x[0]; }
    
    //--------------------------------------------------------------------------//
    //  MAXIMUM DES DEFINITIONSBEREICHS                                         //
    //--------------------------------------------------------------------------//
    /** Liefert den maximalen Wert des Definitionsbreichs der diskreten
     *  Skalarfunktion.
     *
     *  @return maximales diskretes Argument des Definitionsbreichs.            */
    //--------------------------------------------------------------------------//
    public double getxmax() { return x[anz-1]; }
    
    /**
     * 
     * @param i 
     * @return 
     */
    public double getxAt(int i) { return x[i]; }
    //--------------------------------------------------------------------------//
    //  GEGLAETTETER WERT                                                       //
    //--------------------------------------------------------------------------//
    /** Erzeugt einen gemittelten Wert fuer ein Argument und die umliegenden
     *  Argumente einer diskrteten Skalarfunktion.
     *
     *  @param i Position des diskreten Arguments in der Funktion
     *  @param n Anzahl der diskreten Argumente, die links bzw. rechts neben dem
     *           Argument i liegen. Mit den Werten dieser Argumente wird der
     *           Mittelwert gebildet. Ist die Funktion nicht periodisch und
     *           &uuml;berschreitet n den Definitionsbreich der Funktion, so
     *           werden nur die Werte bis zur minimalen bzw. maximalen
     *           Bereichsgrenze ber&uuml;cksichtigt.                            */
    //--------------------------------------------------------------------------//
    
  /*
  public double getSmoothValue (int i, int n)
  { double sum     = 0.0;
    int    zaehler = 0;
   
    for (int j=(i-n); j<=(i+n); j++)
    {      if (j<0    && periodic) { sum += werte[1][anz+j]; zaehler++; }
      else if (j>=anz && periodic) { sum += werte[1][anz-j]; zaehler++; }
      else if (j>=0   && j<anz)    { sum += werte[1][j];     zaehler++; }
    }
    return (sum/zaehler);
  }
   */
    //--------------------------------------------------------------------------//
    //  GEGLAETTETE FUNKTION                                                    //
    //--------------------------------------------------------------------------//
    /** Erzeugt eine geglaette Funktion der diskrteten Skalarfunktion.
     *
     *  @param n Anzahl der diskreten Argumente mit denen mittels der Methode
     *           <code>getSmoothValue (i, n)</code> der Mittelwert gebildet wird.
     *
     *  @return Glatte diskrteten Skalarfunktion                                */
    //--------------------------------------------------------------------------//
 /*
  public DiscretScalarFunction1d getSmoothFunction (int n)
  { double[][] w = new double[2][anz];
            w[0] = werte[0];
  
    for (int i=0; i<anz; i++) { w[1][i] = this.getSmoothValue(i,n); }
  
    return new DiscretScalarFunction1d(w);
  }
  */
    //--------------------------------------------------------------------------//
    //  SETZEN EINER PERIODIZITAET                                              //
    //--------------------------------------------------------------------------//
    /** Erzeugt oder zerst&ouml;rt die Periodizit&auml;t der diskreten
     *  Skalarfunktion.
     *
     *  <p>Vorsicht! F&uuml;r eine periodische Funktion muss der erste
     *  Funktionswert gleich dem letzten Funktionswert sein. Um dies zu
     *  gew&auml;hrleisten, werden die beiden Grenzwerte durch ihren Mittelwert
     *  ersetzt.
     *
     *  @param periodic <code>true</code>, wenn die Funktion einer Kreisfunktion
     *                  entspricht und <code>false</code>, wenn sie keiner
     *                  Kreisfunktion entspricht.                               */
    //--------------------------------------------------------------------------//
    
    public void setPeriodic(boolean periodic) {
        this.periodic = periodic;
        
        if (periodic)
            for (int j=0;j<vlaenge ;j++ )
                werte[j][0] = werte[j][anz-1] = 0.5 * (werte[j][0] + werte[j][anz-1]);
    }
    
    //--------------------------------------------------------------------------//
    //  IST DIE FUNKTION PERIODISCH ?                                           //
    //--------------------------------------------------------------------------//
    /** Pr&uuml;ft, ob die diskrete Skalarfunktion periodisch ist.
     *
     *  @return <code>true</code>, wenn die Funktion einer Kreisfunktion
     *          entspricht und <code>false</code>, wenn sie keiner Kreisfunktion
     *          entspricht.                                                     */
    //--------------------------------------------------------------------------//
    public boolean isPeriodic() { return periodic; }
    
    //--------------------------------------------------------------------------//
    //  HOLEN DER EPSILON-UMGEBUNG                                              //
    //--------------------------------------------------------------------------//
    /** Liefert den Epsilon-Wert f&uuml;r den Umgenbungsbereich eines
     *  Funktionswerte.                                                         */
    //--------------------------------------------------------------------------//
    public double getEpsilon() { return epsilon; }
    
    //--------------------------------------------------------------------------//
    //  SETZEN DER EPSILON-UMGEBUNG                                             //
    //--------------------------------------------------------------------------//
    /** Setzt den Epsilon-Wert f&uumnl;r den Umgebungsbereich eines
     *  Funktionswertes.
     *
     *  @param epsilon Dieser Wert sollte deutlich kleiner als eins sein umd muss
     *                 in jedem Fall positiv sein. Sollte <code>epsilon</code>
     *                 negativ oder gr&ouml;sser als eins sein, so wird die
     *                 Umgebungsvariable nicht neu gesetzt.                     */
    //--------------------------------------------------------------------------//
    public void setEpsilon(double epsilon){
        if (epsilon<1.0 && epsilon>=0.0) this.epsilon = epsilon;
    }
    
    //--------------------------------------------------------------------------//
    //  VORWAERTSDIFFERENZ                                                      //
    //--------------------------------------------------------------------------//
    /** Bildet die Vorw&auml;rtsdifferenz f&uuml;r einen Funktionswert
     *  innerhalb der diskreten Skalarfunktion.
     *
     *  @param i Position des Funktionswertes in der diskreten Funktion         */
    //--------------------------------------------------------------------------//
    public double[] frontDifference(int i){
        if (i>=(anz-1))   return  rearDifference(anz-1);
        double[] d = new double[vlaenge];
        if (Math.abs(getxAt(i+1) - getxAt(i))<epsilon) return d;
        double deltax = getxAt(i+1) - getxAt(i);
        
        for (int j=0;j<vlaenge ;j++ )
            d[j] =(werte[j][i+1] - werte[j][i]) / deltax ;
        return d;
    }
    
    //--------------------------------------------------------------------------//
    //  RUECKWAERTSDIFFERENZ                                                    //
    //--------------------------------------------------------------------------//
    /** Bildet die R&uuml;ckw&auml;rtsdifferenz f&uuml;r einen Funktionswert
     *  innerhalb der diskreten Skalarfunktion.
     *
     *  @param i Position des Funktionswertes in der diskreten Funktion         */
    //--------------------------------------------------------------------------//
    public double[] rearDifference(int i){
        if (i<=0) {    return frontDifference(0);       }
        double[] d = new double[vlaenge];
        if (Math.abs(getxAt(i) - getxAt(i-1))<epsilon) return d;
        double deltax = getxAt(i) - getxAt(i-1);
        for (int j=0;j<vlaenge ;j++ )
            d[j] = (werte[j][i] - werte[j][i-1]) / deltax;
        return d;
    }
    
    //--------------------------------------------------------------------------//
    //  ZENTRALE DIFFERENZ                                                      //
    //--------------------------------------------------------------------------//
    /** Bildet die zentrale Differenz f&uuml;r einen Funktionswert
     *  innerhalb der diskreten Skalarfunktion.
     *
     *  @param i Position des Funktionswertes in der diskreten Funktion         */
    //--------------------------------------------------------------------------//
    public double[] centralDifference(int i) { return upwindDifference(i,0.5); }
    
    //--------------------------------------------------------------------------//
    //  UPWINDING-DIFFERENZ                                                     //
    //--------------------------------------------------------------------------//
    /**
     * Bildet eine gewichtete Differenz f&uuml;r einen Funktionswert innerhalb
     *  der diskreten Skalarfunktion.
     * 
     *  <p>Mit dem Upwinding-Koeffizient <code>alpha</code> wird der Anteil der
     *  Vorw&auml;rtsdifferenz zur R&uuml;ckw&auml;rtsdifferenz gewichtet.
     * 
     *  <p>Bei <code>alpha = 0.0</code> ergibt sich eine Vorw&auml;rtsdifferenz,
     *  bei <code>alpha = 0.5</code> ergibt sich eine zentrale Differenz und bei
     *  <code>alpha = 1.0</code> ergibt sich eine R&uuml;ckw&auml;rtsdifferenz.
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @param alpha Upwinding-Koeffizient (0.0 bis 1.0)
     * @return 
     */
    //--------------------------------------------------------------------------//
    public double[] upwindDifference(int i, double alpha) {
        double[] d = new double[vlaenge];
        double[] fd =frontDifference(i);
        double[] rd =rearDifference(i);
        for (int j=0;j<vlaenge ;j++ )
            d[j]=alpha * rd[j] + (1.0-alpha) * fd[j];
        return d;
    }
 
    
    
//  Spezielle Implementierungen fuer das 3d-Stroemungsmodell  //
//---------------------------------------------------------- //
    //--------------------------------------------------------------------------//
    //  DIFFERENZ ZUR ZWEITEN ABLEITUNG                                         //
    //--------------------------------------------------------------------------//
    /**
     * Bildet die Differenz zur zweiten Ableitung eines Funktionswerts
     *  innerhalb der diskreten Skalarfunktion.
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @deprecated wird vom 3d-stroemungsmodell genutzt
     */
    //--------------------------------------------------------------------------//
    @Deprecated
    public double secondDifference(int v,int i) {
        double deltaX;
        
        if (i<=0 || i>=(anz-1)) {
            return 0.0;
        } else deltaX = (getxAt(i+1) - getxAt(i-1));
        if (Math.abs(deltaX)<epsilon) return 0.;
        return 2.0 * (frontDifference(v,i) - rearDifference(v,i)) / deltaX;
    }
    
    //--------------------------------------------------------------------------//
    //  VORWAERTSDIFFERENZ                                                      //
    //--------------------------------------------------------------------------//
    /**
     * Bildet die Vorw&auml;rtsdifferenz f&uuml;r einen Funktionswert
     *  innerhalb der diskreten Skalarfunktion.
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @deprecated wird vom 3d-stroemungsmodell genutzt
     */
    //--------------------------------------------------------------------------//
    @Deprecated
    public double frontDifference(int v,int i) {
        if (anz<2) return 0.;
        if (i>=(anz-1))   return  rearDifference(v,anz-1);
        if (Math.abs(getxAt(i+1) - getxAt(i))<epsilon) return 0.;
        return (werte[v][i+1] - werte[v][i]) / (getxAt(i+1) - getxAt(i)) ;
    }
    
    //--------------------------------------------------------------------------//
    //  RUECKWAERTSDIFFERENZ                                                    //
    //--------------------------------------------------------------------------//
    /**
     * Bildet die R&uuml;ckw&auml;rtsdifferenz f&uuml;r einen Funktionswert
     *  innerhalb der diskreten Skalarfunktion.
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @deprecated wird vom 3d-stroemungsmodell genutzt
     */
    //--------------------------------------------------------------------------//
    @Deprecated
    public double rearDifference(int v, int i) {
        if (i<=0) {    return frontDifference(v,0);       }
        if (Math.abs(getxAt(i) - getxAt(i-1))<epsilon) return 0.;
        return (werte[v][i] - werte[v][i-1]) / (getxAt(i) - getxAt(i-1));
    }
    
    //--------------------------------------------------------------------------//
    //  ZENTRALE DIFFERENZ                                                      //
    //--------------------------------------------------------------------------//
    /**
     * Bildet die zentrale Differenz f&uuml;r einen Funktionswert
     *  innerhalb der diskreten Skalarfunktion.
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @deprecated wird vom 3d-stroemungsmodell genutzt
     */
    //--------------------------------------------------------------------------//
    @Deprecated
    public double centralDifference(int v,int i) { return upwindDifference(v,i,0.5); }
    
    //--------------------------------------------------------------------------//
    //  UPWINDING-DIFFERENZ                                                     //
    //--------------------------------------------------------------------------//
    /**
     * Bildet eine gewichtete Differenz f&uuml;r einen Funktionswert innerhalb
     *  der diskreten Skalarfunktion.
     * 
     *  <p>Mit dem Upwinding-Koeffizient <code>alpha</code> wird der Anteil der
     *  Vorw&auml;rtsdifferenz zur R&uuml;ckw&auml;rtsdifferenz gewichtet.
     * 
     *  <p>Bei <code>alpha = 0.0</code> ergibt sich eine Vorw&auml;rtsdifferenz,
     *  bei <code>alpha = 0.5</code> ergibt sich eine zentrale Differenz und bei
     *  <code>alpha = 1.0</code> ergibt sich eine R&uuml;ckw&auml;rtsdifferenz.
     * @param i Position des Funktionswertes in der diskreten Funktion
     * @param alpha Upwinding-Koeffizient (0.0 bis 1.0)
     * @deprecated wird vom 3d-stroemungsmodell genutzt
     */
    //--------------------------------------------------------------------------//
    @Deprecated
    public double upwindDifference(int v,int i, double alpha) {
        return alpha * rearDifference(v,i) + (1.0-alpha) * frontDifference(v,i); }
    
}
