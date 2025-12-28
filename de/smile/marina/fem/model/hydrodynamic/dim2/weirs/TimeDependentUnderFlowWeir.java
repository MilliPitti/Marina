package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import bijava.math.ifunction.ScalarFunction1d;

/**
 * beschreibt ein unterstroemtes Wehr nach Wehrformel dessen Lage durch eine Function ueber die Zeit beschrieben wird
 * @author milbradt
 * @version 1.7.7
 */
public class TimeDependentUnderFlowWeir extends UnderFlowWeir{
    
    private ScalarFunction1d crestLevelFct;
    private double t;
    private boolean initial=true;
    
    /** Creates a new instance of Weir */
    public TimeDependentUnderFlowWeir(ScalarFunction1d crestLevelFct, int[] knotennummern, FEDecomposition sysdat) {
        super(Double.NaN,knotennummern,sysdat);
        this.crestLevelFct=crestLevelFct;
    }
    
    @Override
    public double[] getV(DOF dof, double h, double t){
        
        if(initial || this.t!=t){ 
            setSluiceLevel(crestLevelFct.getValue(t));
            initial=false;
            this.t=t;
        }

        return super.getV(dof,h);
    }

}
