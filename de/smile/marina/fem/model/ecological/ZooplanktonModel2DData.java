package de.smile.marina.fem.model.ecological;

import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/**
 *
 * @author abuabed, milbradt
 */
public class ZooplanktonModel2DData implements ModelData {

    private static int id = NO_MODEL_DATA;
    //Respiration and mortality
    double zooRespiration;
    double zooMortality;
    //Growth
    double zooGrowth;
    // Zustandsgrößen
    double zooconc;
    // Ergebnisvector
    double rzooconc;
    // boudary conditions
    ScalarFunction1d bsc = null;
    double dZooConcdt;
    boolean extrapolate = false;

    /** Creates a new instance of ZooplanktonModel2DData */
    public ZooplanktonModel2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static ZooplanktonModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof ZooplanktonModel2DData) {
                    id = dof.getIndexOf(md);
                    return (ZooplanktonModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (ZooplanktonModel2DData) dof.getModelData(id);
        }
        return null;
    }
}
