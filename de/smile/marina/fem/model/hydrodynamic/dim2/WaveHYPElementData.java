package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/** Element ModelDatas for hyperbolic wave equations
 * @version 0.1
 * @author Peter Milbradt
 */
public class WaveHYPElementData implements ModelData{
    
    private static int id = SEARCH_MODEL_DATA;
    
    public WaveHYPElementData(){
        id = SEARCH_MODEL_DATA;
    }
    
    /**
     * extrahiert die WaveHYPElementData des FElements
     * 
     * @param ele
     * @return 
     */
    public static WaveHYPElementData extract(FElement ele) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = ele.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof WaveHYPElementData) {
                    id = ele.modelData.indexOf(md);
                    return (WaveHYPElementData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (WaveHYPElementData) ele.modelData.get(id);
        }
        return null;
    }
}
