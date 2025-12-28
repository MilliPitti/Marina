package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**
 * Camenen, B., Larson, M., 2005. A general formula for non-cohesive bed load sediment transport. Estuarine, Coastal, and Shelf Science
 * @author Peter Milbradt
 * @version 2.6.1
 */
public class BedLoad2DCL2005 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.5mm < d50 < 29mm */
    private final static double dmin = 0.5E-3;
    
    @Override
    public String toString() {
        return "CamenenLarson2005 (based on CSF)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

//        if (cmd.cv > CurrentModel2D.WATT / 10.) {

            final double tau_crit = smd.CSF * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50; // critical shear stress

            double taux = cmd.tauBx;
            double tauy = cmd.tauBy;

            taux *= smd.lambda; // decreasing depending on not erodible bottom
            tauy *= smd.lambda; // decreasing depending on not erodible bottom

            final double tau = Function.norm(taux, tauy);

            if (tau > tau_crit) {
                smd.bedload = 12. / (PhysicalParameters.G * Math.sqrt(PhysicalParameters.RHO_WATER) * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)) * Math.sqrt(tau) * Math.exp(-4.5 * tau_crit / tau) * smd.lambda;
                
                smd.bedload *= Function.min(1., smd.d50/(2.*dmin)); // Abminderung auf Grund zu kleiner Koerner

                smd.bedload = Math.min(1. / SedimentModel2D.morphFactor * cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload); // Peter 17.04.2012
                
                // dirctional bed load
//                smd.bedloadVector[0] = smd.bedload * taux;
//                smd.bedloadVector[1] = smd.bedload * tauy;
//                smd.bedload *=tau;

                final double gamma=1.;
                final double lambda2=1./smd.innerFrictionAngle;
                final double lambda1=lambda2 * 3./(2.*gamma) * smd.CSF * PhysicalParameters.G * (PhysicalParameters.RHO_SEDIM/cmd.rho - 1) * smd.d50 * 1000.;

                smd.bedloadVector[0] = smd.bedload * taux;
                smd.bedloadVector[1] = smd.bedload * tauy;
                smd.bedload = Function.norm(smd.bedloadVector[0],smd.bedloadVector[1]);
            }
//        }
        return smd.bedloadVector;
    }
}
