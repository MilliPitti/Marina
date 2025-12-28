package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/**
 * @author Peter Milbradt
 * @version 2.4.10
 */
public class BedLoad2DvanRijn89 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.2mm < d50 < 2mm */
    @Override
    public String toString() {
        return "van Rijn (1989) (based on CSF)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.0;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;

        double tbcr = smd.CSF * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50); // critical bottom shear stress auf der Basis des Critischen Shieldsparameter
        
        double tbce = Function.norm(cmd.tauBx, cmd.tauBy);

        if (tbce > tbcr) {
            final double Factor = PhysicalParameters.G * smd.d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER;
            
            double t = (tbce - tbcr)/tbcr;
            if (t<.000001) t = .000001;
            if (t<3.)
                smd.bedload = smd.d50 * 0.053 * Math.sqrt(Factor) / Math.pow(smd.D, 0.3) * Math.pow(t, 2.1);
            else
                smd.bedload = smd.d50 * 0.1 * Math.sqrt(Factor) / Math.pow(smd.D, 0.3) * Math.pow(t, 1.5);

            smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv*Function.max(0.,smd.zh - smd.z)*(1. - smd.porosity), smd.bedload);
            
            // dirctional bed load
            smd.bedloadVector[0] = smd.bedload * cmd.tauBx / tbce;
            smd.bedloadVector[1] = smd.bedload * cmd.tauBy / tbce;
        }

        return smd.bedloadVector;
    }
}
