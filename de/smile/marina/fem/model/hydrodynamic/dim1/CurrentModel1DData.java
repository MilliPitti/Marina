package de.smile.marina.fem.model.hydrodynamic.dim1;
import bijava.math.ifunction.*;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/**
 *
 * @author  Peter Milbradt
 * @version 2.7.3
 */
public class CurrentModel1DData implements ModelData {
    
    // Zustandsgroessen
    double u; double dudt; double dudx;
    double h; double dhdt; double dhdx;
    // Ergebnisvector
    double ru;
    double rh;
    
    // boudary conditions
    ScalarFunction1d bqx=null;
    ScalarFunction1d bu=null;
    ScalarFunction1d bh=null;
    
    /** extrahiert die CurrentModel1DData eines DOF
     * @param dof
     * @return  */
    public static CurrentModel1DData extract(DOF dof){
        Iterator<ModelData> modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = modeldatas.next();
            if(md instanceof CurrentModel1DData)
                return ( CurrentModel1DData )md;
        }
        return null;
    }
    

}
