package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.fem.*;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2D;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.math.Function;

/**
 * beschreibt ein ueberstroemtes Wehr wobei die Knoten in der Topographie hochgezogen werden
 * @author milbradt
 * @version 1.8.13
 */
public class BroadCrestedTopoWeir extends BroadCrestedWeir implements TimeDependentWeir {

    /** Creates a new instance of Weir */
    public BroadCrestedTopoWeir(double crestLevel, int[] knotennummern, FEDecomposition sysdat) {
//        super(crestLevel, knotennummern, sysdat);
        super(knotennummern, sysdat);  // Konstruktor von Weir aufrufen
        
        this.crestLevel=crestLevel;

        for ( int i = 0; i < knotennummern.length; i++ ){
            CurrentModel2DData cmd = CurrentModel2DData.extract(sysdat.getDOF(knotennummern[i]));
            cmd.bWeir = this;
            min=Function.max(min,sysdat.getDOF(knotennummern[i]).z);
        }
        setCrestLevel(crestLevel);
    }

    @Override
    public final void setCrestLevel(double crestLevel) {
        if (!Double.isNaN(crestLevel)) {
            this.crestLevel = crestLevel;
            for (int i = 0; i < knotennummern.length; i++) {
                CurrentModel2DData.extract(sysdat.getDOF(knotennummern[i])).z = sysdat.getDOF(knotennummern[i]).z = Function.min(min,crestLevel);
            }
        }
    }

    @Override
    public double[] getV(DOF dof, double h) {
        
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        if ((cmd.z + cmd.eta) < 0.) {
            cmd.eta = -cmd.z;
        }
        cmd.totaldepth = cmd.z + cmd.eta;
        
        /* Wattstrategie fuer Stroemung   */
        cmd.wlambda = Function.min(1., cmd.totaldepth / CurrentModel2D.WATT);
        cmd.w1_lambda = 1. - cmd.wlambda;
        if (cmd.totaldepth < CurrentModel2D.WATT/2.) {
            cmd.u *= cmd.totaldepth/CurrentModel2D.WATT/2.;
            cmd.v *= cmd.totaldepth/CurrentModel2D.WATT/2.;
        }
        return new double[]{cmd.u, cmd.v};
    }

    @Override
    public double[] getV(DOF p, double h, double t) {
        return getV(p, h);
    }
}
