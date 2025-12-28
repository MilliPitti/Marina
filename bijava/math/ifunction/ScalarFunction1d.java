package bijava.math.ifunction;

//==========================================================================//

import java.io.Serializable;

//  SCHNITTSTELLE ScalarFunction1d                                          //
//==========================================================================//
//  Eindimensionale skalare Funktionen                                      //  
//                                                                          //
//  VERSION: 1.1, Juli 2000                                                 //
//                                                                          //
//  AUTHOR:  Institut fuer Bauinformatik, Universitaet Hannover             //
//           Dr.-Ing. Peter Milbradt, Dipl.-Ing. Martin Rose                //
//==========================================================================//
/** "ScalarFunction1d" stellt eine Schnittstelle f&uuml;r eine eindiemnsionale
 *  Funktion mit skalaren Werten zur Verf&uuml;gung.
 *
 *  <p><strong>Version:</strong>
 *  <br><dd>1.1, Juli 2000
 *  <p><strong>Author:</strong>
 *  <br><dd>Institut f&uuml;r Bauinformatik
 *  <br><dd>Universit&auml;t Hannover
 *  <br><dd>Dr.-Ing. Peter Milbradt, Dipl.-Ing. Martin Rose                 */ 
//==========================================================================// 
public interface ScalarFunction1d extends Serializable
{ 

//--------------------------------------------------------------------------//
//  FUNKTIONSWERT                                                           //
//--------------------------------------------------------------------------//
/** Liefert den Wert zu einem Argument x.
     * @param x
     * @return  */
//--------------------------------------------------------------------------//	
  public double getValue (double x);
  
//--------------------------------------------------------------------------//
//  ABLEITUNG                                                               //
//--------------------------------------------------------------------------//
/** Liefert die Ableitung zu einem Argument x.
     * @param x
     * @return  */
//--------------------------------------------------------------------------//
  public double getDifferential (double x);
  
//--------------------------------------------------------------------------//
//  SETZEN EINER PERIODIZITAET                                              //
//--------------------------------------------------------------------------//
/** Erzeugt oder Zerst&ouml;rt die Periodizit&auml;t der skalaren Funktion.
 *  
 *  @param b <code>true</code>, wenn die Funktion einer Kreisfunktion 
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
