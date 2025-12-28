package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.hydrodynamic.dim2.*;
import de.smile.math.Function;

/**
 * beschreibt ein ueberstroemtes Wehr
 * @author milbradt
 * @version 1.7.14
 */
public class BroadCrestedWeir extends Weir implements TimeDependentWeir{

    protected double crestLevel;
    protected double min, max;
    protected double faktor_l_eff; // Faktor der effektiven Breite 10.09.08

    protected BroadCrestedWeir(int[] knotennummern, FEDecomposition sysdat) { // Peter 07.10.09
        super(knotennummern, sysdat);
    }

    /** Creates a new instance of Weir */
    public BroadCrestedWeir(double crestLevel, int[] knotennummern, FEDecomposition sysdat) {

        super(knotennummern, sysdat);

        this.crestLevel=crestLevel;

        for ( int i = 0; i < knotennummern.length; i++ ){
            CurrentModel2DData cmd = CurrentModel2DData.extract(sysdat.getDOF(knotennummern[i]));
            cmd.bWeir = this;
            cmd.bu=null; cmd.bv=null; cmd.extrapolate_h = false; // Peter 04.10.08
            min=Function.max(min,sysdat.getDOF(knotennummern[i]).z);
        }

        double l = 0.0;
        double l_rand = 0.0;
        for (int i = 1; i < knotennummern.length; i++) {
            if (i == 1 || i == knotennummern.length-1) {
                l_rand += 0.5 * sysdat.getDOF(knotennummern[i]).distance(sysdat.getDOF(knotennummern[i - 1]));
            }
            l += sysdat.getDOF(knotennummern[i]).distance(sysdat.getDOF(knotennummern[i - 1]));
        }
        faktor_l_eff = l / (l - l_rand);
    }

    public void setMaxCrestLevel(double l){
        max=l;
    }

    public void setCrestLevel (double crestLevel){
        this.crestLevel=Function.min(min,crestLevel);
    }

    public double getCrestLevel (){
        return crestLevel;
    }

    @Override
    public double[] getV(DOF dof, double h){

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);

        if(dof.number==knotennummern[0] || dof.number==knotennummern[knotennummern.length-1]){
            return new double[]{0.,0.};
        }

        // suchen des groeszten eta - ist wahrscheinlich der Oberwasserstand
        // suchen des minimales eta - ist wahrscheinlich der Unterwasserstand
        FElement[] aElements = dof.getFElements();
        double h_max = cmd.eta;
        double h_min = cmd.eta;
        for (int i = 0; i < aElements.length; i++) {
            FElement elem = aElements[i];
            for (int ll = 0; ll < 3; ll++) {
                if (elem.getDOF(ll) == dof) {
                    for (int ii = 1; ii < 3; ii++) {
                        CurrentModel2DData tmpcdata = CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3));
                        if (tmpcdata.totaldepth > CurrentModel2D.WATT) {
                            h_max = Math.max(h_max, tmpcdata.eta);
                            h_min = Math.min(h_min, tmpcdata.eta);
                        }
                    }
                    break;
                }
            }
        }

        cmd.eta = Function.max(cmd.eta,Function.max(crestLevel,(h_max+h_min)/2.));
        cmd.totaldepth = dof.z + cmd.eta;

        if(cmd.totaldepth<CurrentModel2D.WATT/3.) // Peter 27.08.08
            return new double[]{0.,0.};

        final double mue= 0.9; //0.58;  // Zielke Stroe-Skript
        final double c= 1.;    //0.89;

//        c = Math.sqrt(1.-Math.pow(h_min/h_max,16.)); // Nujic

        int i=0;
        while(dof.number!=knotennummern[i]) i++;

        double d = Function.max(0.,Function.min(crestLevel,sysdat.getDOF(dof.number).z)+h_max);

        double factor = d/Function.max(CurrentModel2D.WATT,cmd.totaldepth); // Peter 14.08.09

        double v = (2./3.*mue * c * Math.sqrt(2.* PhysicalParameters.G * d));// * factor;
        double[] su = new double[2];
        su[0] = faktor_l_eff * (1.-factor)* v * normale[0][i] + factor * cmd.u;
        su[1] = faktor_l_eff * (1.-factor)* v * normale[1][i] + factor * cmd.v;

        return su;
    }

    @Override
    public double[] getV(DOF p, double h, double t) {
        return getV(p,h);
    }
}