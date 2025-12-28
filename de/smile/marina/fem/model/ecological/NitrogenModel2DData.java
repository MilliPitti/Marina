package de.smile.marina.fem.model.ecological;
import java.util.*;

import bijava.math.ifunction.*;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;


/**
 *
 * @author milbradt
 */
class NitrogenModel2DData implements ModelData{
    
    private static int id = NO_MODEL_DATA;
    
    // Zustandsgroeszen
    double skonc;
    double dskoncdt;
    double dskoncdx;
    double dskoncdy;
    
    // boudary conditions
    ScalarFunction1d bsc=null;
    
    // Ergebnisvector
    double rskonc;
    double rz;
    
    boolean extrapolate = false;
    
    public NitrogenModel2DData(){
        id = SEARCH_MODEL_DATA;
    }
    
    /** extrahiert die NitratModel2DData aus dem DOF */
//    public static NitratModel2DData extract(DOF dof) {
//        NitratModel2DData nitratmodeldata = null;
//        Iterator modeldatas = dof.allModelDatas();
//        while (modeldatas.hasNext()) {
//            ModelData md = (ModelData) modeldatas.next();
//            if(md instanceof NitratModel2DData)  nitratmodeldata = ( NitratModel2DData )md;
//        }
//        return nitratmodeldata;
//    }
    
    public static NitrogenModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof NitrogenModel2DData) {
                    id = dof.getIndexOf(md);
                    return (NitrogenModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (NitrogenModel2DData) dof.getModelData(id);
        }
        return null;
    }
}