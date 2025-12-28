package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**
 * Komarova, N. L. and S. J. M. H. Hulscher (2000). Linear
 * instability mechanics for sand wave formation. J. of Fluid
 * Mechanics 413, 219?246.
 * 
 * @author Peter Milbradt
 * @version 2.6.1
 */
public class BedLoad2DKomarovaAndHulscher_2000 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.4mm < d50 < 29mm */
    private final static double dmin = 0.4E-3;

    @Override
    public String toString() {
        return "KomarovaAndHulscher_2000";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        final double gamma = 1.;

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

        double tau = Function.norm(cmd.tauBx, cmd.tauBy);

        if(tau > 1.E-6){
            smd.bedload = 8. * gamma * cmd.rho / PhysicalParameters.RHO_SEDIM /  PhysicalParameters.G * Math.sqrt(tau);

            smd.bedload *= smd.lambda; // decreasing depending on not erodible bottom
            smd.bedload *= Function.min(1., smd.d50/dmin); // Abminderung auf Grund zu kleiner Koerner
//
            smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv * Function.max(0.,smd.zh - smd.z)*(1. - smd.porosity), smd.bedload);

            smd.bedloadVector[0] = smd.bedload * cmd.tauBx;
            smd.bedloadVector[1] = smd.bedload * cmd.tauBy;
            smd.bedload = Function.norm(smd.bedloadVector[0],smd.bedloadVector[1]);
        }

        return smd.bedloadVector;
    }
}
