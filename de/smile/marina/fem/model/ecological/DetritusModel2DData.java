package de.smile.marina.fem.model.ecological;


import de.smile.marina.fem.*;
import bijava.math.ifunction.ScalarFunction1d;
import java.util.Iterator;
/**
 *
 * @author Abuabed, Milbradt
 */
public class DetritusModel2DData implements ModelData {
    private static int id = NO_MODEL_DATA;
    
    //Mineralization
    double detritMineral;
    // Zustandsgrößen
    double detritconc; 
    // boudary conditions
    ScalarFunction1d bsc=null;
    
    double dDetritConcdt;
    
    boolean extrapolate = false;
    
    /** Creates a new instance of DetritusModel2DData */
    public DetritusModel2DData() {
        id = SEARCH_MODEL_DATA;
    }
    
    public static DetritusModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData>  modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof DetritusModel2DData) {
                    id = dof.getIndexOf(md);
                    return (DetritusModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (DetritusModel2DData) dof.getModelData(id);
        }
        return null;
    }
    
}
