package de.smile.marina.fem.model.ecological;

import de.smile.marina.fem.*;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2D;
import de.smile.math.Function;
import java.util.Iterator;

/**
 *
 * @author milbradt
 */
public class SpartinaAlternifloraModel2DData implements ModelData {

    private static int id = NO_MODEL_DATA;
    // Pflanzendichte Planzen pro m^2 maximal 10
    double density;
    // Hoehe
    double height;
    // Pflanzenstengeldicke
    double diameter;

    /** berechnet eine Scheinrauhigkeit
     * @param waterDepth
     * @return  */
    public double getStrickler(double waterDepth){

        double lambda = density/SpartinaAlternifloraModel2D.maxdensity      // Bewuchsdichte
                * diameter/SpartinaAlternifloraModel2D.maxdiameter          // grasdurchmesser
                * Function.min( height/Function.max(waterDepth, CurrentModel2D.WATT) ,1.) // Ueberflutungshoehe
                ;

        return lambda*SpartinaAlternifloraModel2D.minKst + (1.-lambda)*SpartinaAlternifloraModel2D.maxKst;
    }

    /** ist 1 bei voller Wirkung
     * @param waterDepth
     * @return  */
    public double getLambda(double waterDepth){

        return density/SpartinaAlternifloraModel2D.maxdensity      // Bewuchsdichte
                * diameter/SpartinaAlternifloraModel2D.maxdiameter          // grasdurchmesser
                * Function.min( height/Function.max(waterDepth, CurrentModel2D.WATT) ,1.) // Ueberflutungshoehe
                ;
    }


    /** Creates a new instance of SpartinaAlternifloraModel2DData */
    public SpartinaAlternifloraModel2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static SpartinaAlternifloraModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof SpartinaAlternifloraModel2DData) {
                    id = dof.getIndexOf(md);
                    return (SpartinaAlternifloraModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (SpartinaAlternifloraModel2DData) dof.getModelData(id);
        }
        return null;
    }
}
