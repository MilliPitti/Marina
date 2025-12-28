package de.smile.marina.fem.model.hydrodynamic.dim1;
import bijava.math.ifunction.*;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** 
 *
 * @author  Peter Milbradt
 * @version 
 */
 public class SedimentModel1DData implements ModelData {
  
  // Zustandsgroessen
  double C; double dCdt; double dCdx;
  // Ergebnisvector
  double rC;
    
  // boudary conditions
  ScalarFunction1d bqx=null;
  ScalarFunction1d bu=null;
  ScalarFunction1d bh=null;
  
  /** extrahiert die CurrentModel1DData eines DOF
     * @param dof
     * @return  */
    public static SedimentModel1DData extract(DOF dof){
        Iterator<ModelData> modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = modeldatas.next();
            if(md instanceof SedimentModel1DData)
                return ( SedimentModel1DData )md;
        }
        return null;
    }
  
}
