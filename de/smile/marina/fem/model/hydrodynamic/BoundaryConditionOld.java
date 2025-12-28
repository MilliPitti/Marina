package de.smile.marina.fem.model.hydrodynamic;
import bijava.math.ifunction.*;
/** Hilfsklasse zur Speicherung der Randbedingungen
 * @version 1.1
 * @author Peter Milbradt
 */
public class BoundaryConditionOld{
    public int pointnumber;
    public ScalarFunction1d function;

    public BoundaryConditionOld( int nr, ScalarFunction1d fkt ){
        pointnumber = nr;
        function    = fkt;
    }
}