package de.smile.marina.fem.model.ground;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/**
 * @author milbradt
 */
public class BathymetryData2D implements ModelData {

    private static int id = NO_MODEL_DATA;
    public double z;

    /** Creates a new instance of GroundData2D */
    public BathymetryData2D() {
        id = SEARCH_MODEL_DATA;
    }

    public static BathymetryData2D extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof BathymetryData2D bathymetryData2D) {
                    id = dof.getIndexOf(md);
                    return bathymetryData2D;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (BathymetryData2D) dof.getModelData(id);
        }
        return null;
    }
}
