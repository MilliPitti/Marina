/*
 * VectorFunction1d.java
 *
 * Created on 6. Dezember 2004, 15:25
 */

package bijava.math.ifunction;

/**
 *
 * @author milbradt
 */
public interface VectorFunction1d {
    
    //--------------------------------------------------------------------------//
    //  FUNKTIONSWERT                                                           //
    //--------------------------------------------------------------------------//
    /** Liefert den Wert zu einem Argument x.                                   */
    //--------------------------------------------------------------------------//
    public double[] getValue(double x);
    
    //--------------------------------------------------------------------------//
    //  ABLEITUNG                                                               //
    //--------------------------------------------------------------------------//
    /** Liefert die Ableitung zu einem Argument x.                              */
    //--------------------------------------------------------------------------//
    public double[] getDifferential(double x);
    
    //--------------------------------------------------------------------------//
    //  SETZEN EINER PERIODIZITAET                                              //
    //--------------------------------------------------------------------------//
    /** Erzeugt oder Zerst&ouml;rt die Periodizit&auml;t der skalaren Funktion.
     *
     *  @param periodic <code>true</code>, wenn die Funktion einer Kreisfunktion
     *                  entspricht und <code>false</code>, wenn sie keiner
     *                  Kreisfunktion entspricht.                               */
    //--------------------------------------------------------------------------//
    public void setPeriodic(boolean b);
    
    //--------------------------------------------------------------------------//
    //  IST DIE FUNKTION PERIODISCH ?                                           //
    //--------------------------------------------------------------------------//
    /** Pr&uuml;ft, ob die skalare Funktion periodisch ist.
     *
     *  @return <code>true</code>, wenn die Funktion einer Kreisfunktion
     *          entspricht und <code>false</code>, wenn sie keiner Kreisfunktion
     *          entspricht.                                                     */
    //--------------------------------------------------------------------------//
    public boolean isPeriodic();
    
}
