package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import bijava.math.ifunction.ScalarFunction1d;

/**
 * beschreibt ein ueberstroemtes Wehr dessen Lage durch eine Funtion ueber die Zeit beschrieben wird
 * @author milbradt
 * @version 1.7
 */
public class TimeDependentBroadCrestedTopoWeir extends BroadCrestedTopoWeir{
    
    private ScalarFunction1d crestLevelFct;
    private double t;
    private boolean initial=true;
    
    /** Creates a new instance of Weir */
    public TimeDependentBroadCrestedTopoWeir(ScalarFunction1d crestLevelFct, int[] knotennummern, FEDecomposition sysdat) {
        super(Double.NaN,knotennummern,sysdat);
        this.crestLevelFct=crestLevelFct;
    }
    
    public double getCrestLevel(double time){
        setCrestLevel(crestLevelFct.getValue(time));
        return super.getCrestLevel();
    }
    
    @Override
    public double[] getV(DOF dof, double h, double t){
        
        if(initial || this.t!=t){ 
            setCrestLevel(crestLevelFct.getValue(t));
            initial=false;
            this.t=t;
        }

        return super.getV(dof,h);
    }

}
