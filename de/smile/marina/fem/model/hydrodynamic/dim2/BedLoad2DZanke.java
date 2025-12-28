package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**
 * nach Zanke in Holz Lehrunterlagen
 *
 * @author Peter Milbradt
 * @version 2.4.10
 */
public class BedLoad2DZanke implements BedLoad2DFormulation {

    @Override
    public String toString() {
        return "Zanke (based on CSF)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.0;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

        if (cmd.cv > CurrentModel2D.WATT / 10.) {

            double d = Function.max(CurrentModel2D.WATT, cmd.totaldepth);

            double CSF = smd.CSF; // Critical Shieldsfactor

            final double Factor = PhysicalParameters.G * smd.d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER;

            // van Rijn
            // Chezy coefficient
            double Cs = 2.5 * PhysicalParameters.sqrtG * Math.log(4. * d / smd.d50 + 1.);
            if(Cs<0x1.0p-300)
                return smd.bedloadVector; // Peter 28.04.2019

            // Friction velocity
            double ufx = cmd.u * PhysicalParameters.sqrtG / Cs;
            double ufy = cmd.v * PhysicalParameters.sqrtG / Cs;
            // Shields function of load particles
            double sfx = (ufx * ufx) / Factor * cmd.u / cmd.cv + cmd.tau_bx_extra / Cs; // Peter 04.12.08
            double sfy = (ufy * ufy) / Factor * cmd.v / cmd.cv + cmd.tau_by_extra / Cs; // Peter 04.12.08

            sfx *= Function.min(1., Function.max(0., smd.zh - smd.z) / (smd.d50 * 5.)); // decreasing depending on not erodible bottom // Peter 18.08.2010
            sfy *= Function.min(1., Function.max(0., smd.zh - smd.z) / (smd.d50 * 5.)); // decreasing depending on not erodible bottom // Peter 18.08.2010

            double sf = Function.norm(sfx, sfy);

            if (sf > CSF) {
                smd.bedload = 1. / smd.porosity * 6.34 * 1.E-4 * ((sf - CSF) / Function.sqr(smd.wc)) * Math.pow(smd.D, 4.) * cmd.cv * smd.lambda;
                smd.bedload = Math.min(1. / SedimentModel2D.morphFactor * cmd.cv * Function.max(0., smd.zh - smd.z) * (1. - smd.porosity), smd.bedload); // Peter 17.04.2012

                smd.bedloadVector[0] = smd.bedload * sfx / sf;
                smd.bedloadVector[1] = smd.bedload * sfy / sf;
            }
        }

        return smd.bedloadVector;
    }
}
