package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;
import bijava.math.ifunction.*;
import de.smile.marina.PhysicalParameters;
/** ModelDatas for fluid mud flow
 * @version 2.0.8
 * @author Peter Milbradt   
 */
public class FluidMudFlowModel2DData implements ModelData {

    private static int id = NO_MODEL_DATA;
    // Zustandsgroessenen
    public double u=0., dudt=0., dudx=0., dudy=0.;       // velocity in x-direction
    public double v=0., dvdt=0., dvdx=0., dvdy=0.;       // velocity in y-direction
    public double p=0.; // Druck als treibende Kraft
    public double m=0., dmdt=0.;       // elevation of Fluid Mud

    public double cv=0.;           // norm of the velocity
    public boolean wattsickern = true; // hilfsvariabele

    public double skonc = 0.;
    public double sedimentSource = 0.;
    double dCdt = 0.;

    public double rho = 1065.;  // densety of Fluid Mud
    public double viscosity = PhysicalParameters.DYNVISCOSITY_WATER*10.; // viscosity of fluid mud
    public double temp = 4.;      // [Grad C]

    public double kst = 30.;    // Stricklerbeiwert(Bodenrauhheit)
    public double ks = Math.pow(25. / kst, 6.); // aequivalte Bodenhauheit fuer Nikuradse

    public double tau_b;        // bottomfriction koefficient

    public double tau_currentdx,  tau_currentdy;     // current stress koeffizient

    public double z;            // actual depth
    public double dzdt=0.;         // if morphodynamik model
    public double thickness;   //z+m

    public double wlambda = 1.,  w1_lambda = 0.; // lambda und (1-lambda) zur Beruecksichtigung des Trockenfallens: lambda=Function.min(1.,thickness/WATT);

    // Zwischenergebnisse der rechte Seite
    double ru=0., rv=0., rm=0.;
    double rC=0.;

    // boudary conditions
    public ScalarFunction1d bqx = null;
    public ScalarFunction1d bqy = null;
    public ScalarFunction1d bu = null;
    public ScalarFunction1d bv = null;
    public ScalarFunction1d bh = null;
    public QSteuerung bQx = null;
    public QSteuerung bQy = null;
    public ScalarFunction1d sourceQ = null;
    public ScalarFunction1d sourceh = null;
    public double source_dhdt = 0.;
    boolean boundary = false; // indicator that the node has boundary conditions
    public boolean extrapolate_h; // Peter 04.10.08
    boolean extrapolate_u;
    boolean extrapolate_v;
    ScalarFunction1d bconc = null; // Randbedingungsfunktion fuer geloestes Fluidmud

    public FluidMudFlowModel2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static FluidMudFlowModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof FluidMudFlowModel2DData) {
                    id = dof.getIndexOf(md);
                    return (FluidMudFlowModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (FluidMudFlowModel2DData) dof.getModelData(id);
        }
        return null;
    }
}
