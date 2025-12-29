/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2026

 * This file is part of Marina.

 * Marina is free software: you can redistribute it and/or modify              
 * it under the terms of the GNU Affero General Public License as               
 * published by the Free Software Foundation version 3.
 * 
 * Marina is distributed in the hope that it will be useful,                  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                
 * GNU Affero General Public License for more details.                          
 *                                                                              
 * You should have received a copy of the GNU Affero General Public License     
 * along with Marina.  If not, see <http://www.gnu.org/licenses/>.             
 *                                                                               
 * contact: milbradt@smileconsult.de                                        
 * smile consult GmbH                                                           
 * Schiffgraben 11                                                                 
 * 30159 Hannover, Germany 
 * 
 */
package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.model.hydrodynamic.dim2.*;
import de.smile.math.Function;

/**
 * beschreibt ein unterstroemtes Wehr nach Wehrformel
 * @author milbradt
 * @version 1.7.13
 */
public class UnderFlowWeir extends Weir implements TimeDependentWeir{
    
    protected double sluiceLevel;
    protected double faktor_l_eff; // Faktor der effektiven Breite 10.09.08
    
    /** Creates a new instance of Weir */
    public UnderFlowWeir(double sluiceLevel, int[] knotennummern, FEDecomposition sysdat) {
        
        super(knotennummern, sysdat);
        
        this.sluiceLevel = sluiceLevel;
        
        // System.out.println(notennummern[0]);
        for ( int i = 0; i < knotennummern.length; i++ ){
            CurrentModel2DData cmd = CurrentModel2DData.extract(sysdat.getDOF(knotennummern[i]));
            cmd.bWeir = this;
            cmd.bu=null; cmd.bv=null; cmd.extrapolate_h = false; // Peter 04.10.08
        }
// 10.09.08        
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
    
    public void setSluiceLevel(double sluiceLevel) {
        this.sluiceLevel = sluiceLevel;
    }

    public double getSluiceLevel() {
        return sluiceLevel;
    }
    
    @Override
    public double[] getV(DOF dof, double h){
        
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof); // Peter 04.09.08
        if ((cmd.totaldepth<CurrentModel2D.WATT/3.)||(dof.z-sluiceLevel <CurrentModel2D.WATT/3.)) // Peter 04.09.08 Trocken oder Wehr geschlossen
            return new double[]{0.,0.};
        
        if(dof.number==knotennummern[0] || dof.number==knotennummern[knotennummern.length-1]){  // Peter 04.10.08
            return new double[]{0.,0.};
        }
        
        // suchen des groeszten eta - ist wahrscheinlich der Oberwasserstand
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
        
        if(-sluiceLevel>h_max)  // Peter 09.09.08  Schuetz offen
            return new double[]{cmd.u,cmd.v};
        
        double mue=0.58;  // Zielke Stroe-Skript
        double c=0.89;
        
        int i=0;
        while(dof.number!=knotennummern[i]) i++;
        
        double factor = Function.min(1.,Function.max(0.,cmd.z-sluiceLevel)/Function.max(CurrentModel2D.WATT,cmd.totaldepth )); // Peter 10.09.08

        double hw = h_max-h_min;
        double rd = Function.max(0.,dof.z+Function.min(-sluiceLevel,h))/Function.max(CurrentModel2D.WATT,cmd.totaldepth);
        double v = (c * Math.sqrt(2.* PhysicalParameters.G * hw))  * rd ; // Peter 04.09.08
        double[] su = new double[2];
        su[0] = faktor_l_eff * (1.-factor)* v * normale[0][i] + factor * cmd.u;  // Peter 04.10.08
        su[1] = faktor_l_eff * (1.-factor)* v * normale[1][i] + factor * cmd.v;  // Peter 04.10.08
        
        return su;
    }

    @Override
    public double[] getV(DOF p, double h, double t) {
        return getV(p,h);
    }
}
