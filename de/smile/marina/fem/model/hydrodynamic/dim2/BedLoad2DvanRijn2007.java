package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**Van Rijn (1984, 1993) proposed a simplified formula for bed-load transport in current only conditions
 * and recalibrated using measured bed load transport data (Van Rijn 2007)
 * @author Peter Milbradt
 * @version 2.8.
 */
public class BedLoad2DvanRijn2007 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.2mm < d50 < 2mm */
    private final static double alpha=0.015;
    private final static double nue=1.5;
    @Override
    public String toString() {
        return "vanRijn2007";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.0;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

        if (cmd.totaldepth > CurrentModel2D.halfWATT) {
            final double totaldepth = Math.max(0.1, cmd.totaldepth);
            final double lambda = Function.min(1., Function.max(0.,smd.zh - smd.z)/smd.bound); // increasing factor depending on not erodible bottom
            // Bodenneigung
            // Verringerung der kritischen Schubspannung fuer Spontanerosion - Peter's Ansatz - bei nichterodierbaren Knoten erfolgt keine Verringerung
            final double factor = Function.max(0.,smd.innerFrictionAngle-(smd.bottomslope -1.))/smd.innerFrictionAngle*lambda + (1.-lambda);
            final double u_diff = cmd.cv - factor*SedimentModel2DData.criticalVelocitySoulsby(smd.d50, totaldepth);
            if (u_diff > 1.E-5) {
                final double Me = u_diff / Math.sqrt((PhysicalParameters.RHO_SEDIM - cmd.rho) / cmd.rho * PhysicalParameters.G * smd.d50);
                smd.bedload = alpha * cmd.totaldepth * Math.pow(smd.d50 / totaldepth, 1.2) * Math.pow(Me, nue);
                
                // nicht mehr transportieren als ueber dem nicht erodierbarem Horizont vohanden ist
                smd.bedload = Math.min(1. / SedimentModel2D.morphFactor * Math.max(((smd.bottomslope - 1.) > smd.innerFrictionAngle) ? 1. : 0., cmd.cv) * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload);

                // dirctional bed load
                smd.bedloadVector[0] = smd.bedload * cmd.u;
                smd.bedloadVector[1] = smd.bedload * cmd.v;
                
                smd.bedload = Function.norm(smd.bedloadVector[0], smd.bedloadVector[1]);
            }
        }
        return smd.bedloadVector;
    }
}
