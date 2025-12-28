package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import bijava.math.ifunction.ScalarFunction1d;

/**
 * beschreibt ein NadelWehr dessen Oeffungsgrad durch eine Funtion ueber die Zeit beschrieben wird
 * @author milbradt
 */
public class TimeDependentNeedleWeir extends NeedleWeir{
    
    private ScalarFunction1d openingFct;
    private double t;
    private boolean initial=true;
    
    /** Creates a new instance of TimeDependentNeedleWeir */
    public TimeDependentNeedleWeir(ScalarFunction1d openingFct, int[] knotennummern, FEDecomposition sysdat) {
        
        super(0.,knotennummern,sysdat);
        
        this.openingFct=openingFct;

    }
    
    public double getOpening(double time){
        super.opening=openingFct.getValue(time);
        return super.getOpening();
    }
    
    @Override
    public double[] getV(DOF dof, double h, double t){
        
        if(initial){ this.t=t; initial=false;}
        
        if(this.t!=t){
            setOpening(openingFct.getValue(t));
            this.t=t;
        }
        return super.getV(dof,h);
    }

}
