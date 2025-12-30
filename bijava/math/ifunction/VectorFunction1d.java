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
