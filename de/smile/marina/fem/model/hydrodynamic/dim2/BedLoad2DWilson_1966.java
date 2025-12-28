package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**
 * Wilson (1966)
 * Anpassung von Meyer-Peter Mueller fuer hohe Transporte
 * @author Peter Milbradt
 * @version 2.4.6
 */
public class BedLoad2DWilson_1966 implements BedLoad2DFormulation {

    @Override
    public String toString() {
        return "Wilson (1966)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

        double dzdx=0.; double dzdy=0.;
        if (!cmd.boundary) {
//            final double maxSlope = SedimentModel2D.maxSlope;
            final double maxSlope = smd.innerFrictionAngle; // Peter 07.03.2012
            final double slope = smd.bottomslope-1.;
            if (slope > maxSlope) {
                final double factor = Function.min(1.,slope-maxSlope);
                dzdx = smd.bottomslope / slope * factor * smd.lambda; // decreasing depending on not erodible bottom
                dzdy = smd.bottomslope / slope * factor * smd.lambda; // decreasing depending on not erodible bottom
            }
        }
//          double C = 18.*Math.log(12.*d/cmd.ks);
//          double Cs = 18.*Math.log(12.*d/smd.d50);
//          double nue = C/Cs;

//        double CSF = smd.CSF; // variable
          double CSF = 0.047; // fest nach Meyer-Peter und Mueller (1948)

        double sfx = ((smd.grainShearStress * cmd.u) * cmd.wlambda * PhysicalParameters.RHO_WATER + dzdx * PhysicalParameters.G * Math.PI*Math.pow(smd.d50,3.)/6.*(PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) + cmd.tau_bx_extra) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = ((smd.grainShearStress * cmd.v) * cmd.wlambda * PhysicalParameters.RHO_WATER + dzdy * PhysicalParameters.G * Math.PI*Math.pow(smd.d50,3.)/6.*(PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) + cmd.tau_by_extra) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);

        sfx *= smd.lambda; // decreasing depending on not erodible bottom
        sfy *= smd.lambda; // decreasing depending on not erodible bottom

        double sf = Function.norm(sfx, sfy);

        if (sf > CSF) {
            smd.bedload = 12. * Math.sqrt((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER * PhysicalParameters.G * Math.pow(smd.d50, 3.) * Math.pow(sf - CSF, 3.)) * smd.lambda;
            smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv * Function.max(0.,smd.zh - smd.z)*(1. - smd.porosity), smd.bedload); // Peter 17.04.2012

            smd.bedloadVector[0] = smd.bedload * sfx / sf;
            smd.bedloadVector[1] = smd.bedload * sfy / sf;
        }

        return smd.bedloadVector;
    }
}
