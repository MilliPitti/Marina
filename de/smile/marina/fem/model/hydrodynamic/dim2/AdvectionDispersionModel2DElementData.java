package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** Element ModelDatas for AdvectionDispersionModel2D
 * @version 2.8.1
 * @author Peter Milbradt
 */
public class AdvectionDispersionModel2DElementData implements ModelData {

    private static int id = NO_MODEL_DATA;
    double absRes=0.; // summer der absoluten lokalen Fehler
    
    public AdvectionDispersionModel2DElementData() {
        id = SEARCH_MODEL_DATA;
    }

    /** extrahiert die ElementCurrent2DData des FElements
     * @param ele
     * @return  */
    public static AdvectionDispersionModel2DElementData extract(FElement ele) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = ele.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof AdvectionDispersionModel2DElementData) {
                    id = ele.modelData.indexOf(md);
                    return (AdvectionDispersionModel2DElementData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (AdvectionDispersionModel2DElementData) ele.modelData.get(id);
        }
        return null;
    }
}
