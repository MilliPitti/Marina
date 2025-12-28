package de.smile.marina.fem.model.ecological;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TicadModel;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.marina.io.TicadIO;
import java.io.DataOutputStream;

/** Das Gewoehnliche Seegras (Zostera marina) ist eine Pflanzenart in der Familie der Seegrasgewaechse (Zosteraceae).
 *
 * @author Peter
 */
public class ZosteraMarina  extends TimeDependentFEApproximation implements FEModel, TicadModel {
    private DataOutputStream xf_os = null;

    @Override
    public void setBoundaryCondition(DOF dof, double t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] getRateofChange(double time, double[] x) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ModelData genData(DOF dof) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ModelData genData(FElement felement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double ElementApproximation(FElement ele) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write_erg_xf(double[] erg, double t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getTicadErgMask() {
        // Setzen der Ergebnismaske Planzen pro 
        return TicadIO.HRES_H;
    }
    
}
