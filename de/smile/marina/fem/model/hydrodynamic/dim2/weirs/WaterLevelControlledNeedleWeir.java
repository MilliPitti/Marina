package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.model.hydrodynamic.dim2.*;
import bijava.math.ifunction.ScalarFunction1d;

/**
 * beschreibt ein Nadel-Wehr dessen Oeffungsweite duch ein zu steuernden Wasserstand bestimmt wird 
 * @author milbradt
 * @version 2.2.1
 */
public class WaterLevelControlledNeedleWeir extends NeedleWeir{
    
    private ScalarFunction1d waterLevel;
    private int[] mesureNodeID;
    private double weirVelocity=0.001; // [%/s]
    private double t;
    private boolean initial=true;
    public double propFactor = 1.;
    public double diffFactor = 0.;

    private double dcOld=0.;
    
    /** Creates a new instance of Weir */
    public WaterLevelControlledNeedleWeir(int[] mesureNodeID, ScalarFunction1d waterLevel, int[] knotennummern, FEDecomposition sysdat) {
        
        super(0.,knotennummern,sysdat);
        
        this.waterLevel=waterLevel;
        this.mesureNodeID = mesureNodeID;

    }
    
    private void updateOpening(double t){
        double dt = t-this.t; //System.out.println("dt="+dt); System.out.println("t="+t);System.out.println("this.t="+this.t);
        double meanH=0.;
        int anz=0;
        for (int i=0; i<mesureNodeID.length; i++){
            CurrentModel2DData cmd = CurrentModel2DData.extract(sysdat.getDOF(mesureNodeID[i]));
            if (cmd.totaldepth>CurrentModel2D.WATT){
              meanH+=cmd.eta;
              anz++;
            }
        }
        if (anz>0){
            meanH /=anz;
//            System.out.println("meanH="+meanH);
//            System.out.println("H="+waterLevel.getValue(t));
            double dc = waterLevel.getValue(t)-meanH;

            double difdc = (dc-dcOld);
            dcOld=dc;

            dc = propFactor * dc  + diffFactor * difdc; // Peter 16.05.2012
            if(dc > weirVelocity) dc = weirVelocity;
            if(dc < (-weirVelocity)) dc = -weirVelocity;
            setOpening(opening - dt * dc);
        }
//        System.out.println(opening);
    }
    
    @Override
    public final double[] getV(DOF dof, double h, double t){
        
        if(initial){ this.t=t; initial=false;}
        
        if(this.t!=t){
            updateOpening(t);
            this.t=t;
        }
        return super.getV(dof,h);
    }
}
