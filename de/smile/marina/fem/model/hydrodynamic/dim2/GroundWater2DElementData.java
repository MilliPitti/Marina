package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** 
 * @version 0.1
 * @author Peter Milbradt
 */
public class GroundWater2DElementData implements ModelData{
    
    private static int id = NO_MODEL_DATA;
    
    double mean_zG=10.;
    double kf = 0.001;
    double u,v;
    
    public GroundWater2DElementData(){
        id = SEARCH_MODEL_DATA;
    }
    
    /**
     * extrahiert die Current2DElementData des FElements
     *
     * @param ele
     * @return
     */
    public static GroundWater2DElementData extract(FElement ele) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = ele.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof GroundWater2DElementData) {
                    id = ele.modelData.indexOf(md);
                    return (GroundWater2DElementData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (GroundWater2DElementData) ele.modelData.get(id);
        }
        return null;
    }
}
