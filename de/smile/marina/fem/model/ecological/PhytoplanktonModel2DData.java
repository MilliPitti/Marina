package de.smile.marina.fem.model.ecological;

import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/**
 *
 * @author abuabed, Milbradt
 */
public class PhytoplanktonModel2DData implements ModelData {
    
    private static int id = NO_MODEL_DATA;
    
    //Respiration and mortality
    double phytoRespiration;
    double phytoMortality;
    //Growth
    double phytoGrowth;
    // Zustandsgrößen
    double phytoconc; 
    // Ergebnisvector
    double rphytoconc;
    // boudary conditions
    ScalarFunction1d bsc=null;
    
    double dPhytoConcdt;
    
    boolean extrapolate = false;
    
    
    /** Creates a new instance of PhytoplanktonModel2DData */
    public PhytoplanktonModel2DData() {
        id = SEARCH_MODEL_DATA;
    }
    
    public static PhytoplanktonModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof PhytoplanktonModel2DData) {
                    id = dof.getIndexOf(md);
                    return (PhytoplanktonModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (PhytoplanktonModel2DData) dof.getModelData(id);
        }
        return null;
    }

}
