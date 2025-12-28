package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;
import bijava.math.ifunction.*;

/** @author Peter Milbradt
 * @version 2.5.0
 */
class HeatTransportModel2DData implements ModelData {

    private static int id = NO_MODEL_DATA;    

    double temperature;  // Zustandsgroeszen
    double rtemperature; // Ergebnisvector
    ScalarFunction1d bc = null; // boudary conditions
    double temperaturedt;
    boolean extrapolate = false;
    double sourceSink = 0.;

    public HeatTransportModel2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static HeatTransportModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof HeatTransportModel2DData heatTransportModel2DData) {
                    id = dof.getIndexOf(md);
                    return heatTransportModel2DData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (HeatTransportModel2DData) dof.getModelData(id);
        }
        return null;
    }
}
