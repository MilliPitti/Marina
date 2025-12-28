package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**
 * Fernandez-Luque and van Beck (1976)
 * Anpassung von Meyer-Peter Mueller fuer geringere Transporte
 * @author Peter Milbradt
 * @version 2.4.6
 */
public class BedLoad2DFernandezLuque_vanBeck_1976 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.4mm < d50 < 29mm */

    private final static double dmin = 0.4E-3;

    @Override
    public String toString() {
        return "Fernandez-Luque and van Beck (1976)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

//          double C = 18.*Math.log(12.*d/cmd.ks);
//          double Cs = 18.*Math.log(12.*d/smd.d50);
//          double nue = C/Cs;

//        double CSF = smd.CSF; // variable
          double CSF = 0.047; // fest nach Meyer-Peter und Mueller (1948)

        double sfx = ((smd.grainShearStress * cmd.u) * cmd.wlambda * PhysicalParameters.RHO_WATER + cmd.tau_bx_extra) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = ((smd.grainShearStress * cmd.v) * cmd.wlambda * PhysicalParameters.RHO_WATER + cmd.tau_by_extra) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);

        double sf = Function.norm(sfx, sfy);

        if (sf > CSF) {
            smd.bedload = 5.7 * Math.sqrt((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER * PhysicalParameters.G * Math.pow(smd.d50, 3.) * Math.pow(sf - CSF, 3.));
            
            smd.bedload *= smd.lambda; // decreasing depending on not erodible bottom
            smd.bedload *= Function.min(1., Function.max(Function.max(0,smd.d50) / (4.*dmin),Function.max(0,smd.d50 - dmin/2.) / (2. * dmin))); // Abminderung auf Grund zu kleiner Koerner
        
            smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv * Function.max(0.,smd.zh - smd.z)*(1. - smd.porosity), smd.bedload);

            smd.bedloadVector[0] = smd.bedload * sfx / sf;
            smd.bedloadVector[1] = smd.bedload * sfy / sf;
        }

        return smd.bedloadVector;
    }
}
