package de.smile.marina.fem.model.traffic;
import bijava.math.ifunction.*;
import de.smile.marina.fem.ModelData;

/** 
 *
 * @author  Peter Milbradt
 * @version 
 */
public class MacroscopicTrafficModel1DData implements ModelData {
  
  // Zustandsgroessen
  double v; double dvdt; double dvdx;
  double rho; double drhodt;
  
  // Ergebnisvector
  double rv;
  double rrho;
    
  // boudary conditions
  ScalarFunction1d bv=null;
  ScalarFunction1d brho=null;
  
}
