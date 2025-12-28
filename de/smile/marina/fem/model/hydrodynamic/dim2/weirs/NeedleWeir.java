package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.hydrodynamic.dim2.*;
import de.smile.math.Function;

/**
 * beschreibt ein Nadel-Wehr
 * @author Peter Milbradt
 * @version 1.7.8
 */
public class NeedleWeir extends Weir implements TimeDependentWeir{
    
    protected double opening=1.; //oeffeneungsgrad [0.,1.]
    
    /** Creates a new instance of Weir */
    public NeedleWeir(double opening, int[] knotennummern, FEDecomposition sysdat) {
        
        super(knotennummern, sysdat);

        this.opening = opening;
        
        for ( int i = 0; i < knotennummern.length; i++ ){
            CurrentModel2DData.extract(sysdat.getDOF(knotennummern[i])).bWeir = this;
        }
    }
    
    public final void setOpening(double opening){
        this.opening=Function.max(0.,Function.min(1.,opening));
    }
    
    public final double getOpening(){
        return this.opening;
    }
    
    private boolean isWeierNode(DOF dof){
        boolean rvalue = false;
        for(int i=0;i<knotennummern.length;i++)
            if(dof.number==knotennummern[i])
                rvalue=true;
        return rvalue;
    }
    
    @Override
    public double[] getV(DOF dof, double h){
        return getV3(dof,h);
    }
    
    // einfache Variante wobei die freie sr�mungsgeschwindig durch die Wellengeschwindigkeit abgeschaetzt wird
    private double[] getV0(DOF dof, double h){
        double mue=0.68;
        double c=0.89;
        
        int i=0;
        while(dof.number!=knotennummern[i]) i++;
        
        double d = Function.max(0.,sysdat.getDOF(dof.number).z+h);
        double v = opening*( 2./30. * c * mue *  Math.sqrt(PhysicalParameters.G * d));
        
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double[] su = new double[2];
        su[0] = v * normale[0][i];
        su[1] = v * normale[1][i];
        
        return su;
    }
    
    // variation durch die ableitungen der Geschwindigkeiten und wasserspiegellagen
    private double[] getV1(DOF dof, double h){
        double mue=0.68;
        double c=0.89;
        
        int i=0;
        while(dof.number!=knotennummern[i]) i++;
        
        double dudx = 0., dudy = 0.;
        double dvdx = 0., dvdy = 0.;
        double dhdx = 0., dhdy = 0.;
        
        
        FElement[] t = dof.getFElements();
//        System.out.println(t.length);
        for (int j=0; j<t.length;j++){
            Current2DElementData edata = Current2DElementData.extract(t[j]);
            dudx+=edata.dudx;
            dudy+=edata.dudy;
            dvdx+=edata.dvdx;
            dvdy+=edata.dvdy;
            dhdx+=edata.dhdx;
            dhdy+=edata.dhdy;
        }
        dudx/=t.length;
        dudy/=t.length;
        dvdx/=t.length;
        dvdy/=t.length;
        dhdx/=t.length;
        dhdy/=t.length;
        
        double gh = Function.norm(dhdx,dhdy); // sationaere loesung
        double gu = Function.norm(dudx,dudy);
        double gv = Function.norm(dvdx,dvdy);
        double grad = Function.norm(gu,gv);
        
        double d = Function.max(0.,sysdat.getDOF(dof.number).z+h);
        double v = opening * (Math.sqrt(PhysicalParameters.G * d) * gh / Function.max(0.0001, grad));
//        System.out.println(v);
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double[] su = new double[2];
        su[0] = v * normale[0][i];
        su[1] = v * normale[1][i];
        
        return su;
    }
    
    // Geschwindigkeiten vor und nach dem Wehr zur Mittelwertbildung heranziehen
    private double[] getV2(DOF dof, double h){
        double mue=0.68;
        double c=0.89;
        
        int i=0;
        while(dof.number!=knotennummern[i]) i++;
        
        double umean = 0., vmean = 0.;
        int anz=0;
        
        FElement[] felem = dof.getFElements();
        for(int j=0; j<felem.length;j++) {
            FElement elem=felem[j];
            for(int ll=0;ll<3;ll++){
                if(elem.getDOF(ll)==dof){
                    for(int ii=1;ii<3;ii++){
                        DOF tmpdof = elem.getDOF((ll+ii)%3);
                        CurrentModel2DData tmpcdata = CurrentModel2DData.extract(tmpdof);
                        if (!isWeierNode(tmpdof)){ // und kein trockener und kein geschlossener Randknoten
                            umean+=tmpcdata.u;
                            vmean+=tmpcdata.v;
                            anz++;
                        }
                    }
                    break;
                }
            }
        }
        if (anz>0){
            umean/=anz;
            vmean/=anz;
        }
        
        double v = opening * opening * Function.norm(umean,vmean);
        double[] su = new double[2];
        su[0] = v * normale[0][i];
        su[1] = v * normale[1][i];
        
        return su;
    }
    
    // Geschwindigkeiten vor und nach dem Wehr zur Mittelwertbildung heranziehen
    // zusaetzlich wird zwischen  der aktuellen stroemungssituation und der Stroemung nach beeintraechtigung gemorpht
    private double[] getV3(DOF dof, double h){
        double mue=0.68;
        double c=0.89;
        
        int i=0;
        while(dof.number!=knotennummern[i]) i++;
        
        double umean = 0., vmean = 0.;
        int anz=0;
        
        FElement[] felem = dof.getFElements();
        for(int j=0; j<felem.length;j++) {
            FElement elem=felem[j];
            for(int ll=0;ll<3;ll++){
                if(elem.getDOF(ll)==dof){
                    for(int ii=1;ii<3;ii++){
                        DOF tmpdof = elem.getDOF((ll+ii)%3);
                        CurrentModel2DData tmpcdata = CurrentModel2DData.extract(tmpdof);
                        if (!isWeierNode(tmpdof)){ // und kein trockener und kein geschlossener Randknoten
                            umean+=tmpcdata.u;
                            vmean+=tmpcdata.v;
                            anz++;
                        }
                    }
                    break;
                }
            }
        }
        if (anz>0){
            umean/=anz;
            vmean/=anz;
        }
        
        double v = opening * opening * Function.norm(umean,vmean);
//        System.out.println(v);
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double unow = cmd.u;
        double vnow = cmd.v;
        double[] su = new double[2];
        su[0] = (1.-opening)*v * normale[0][i] + opening*unow;
        su[1] = (1.-opening)*v * normale[1][i] + opening*vnow;
        
        return su;
    }

    @Override
    public double[] getV(DOF p, double h, double t) {
        return getV(p,h);
    }
}
