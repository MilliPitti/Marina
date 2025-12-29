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

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.model.hydrodynamic.dim2.*;
import de.smile.math.Function;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.FElement;

/**
 * beschreibt ein unterstroemtes Wehr mit Wasserspiegellagekorrektur
 * @author milbradt
 */
public class UnderFlowTopoWeir extends UnderFlowWeir {

    /** Creates a new instance of Weir */
    public UnderFlowTopoWeir(double sluiceLevel, int[] knotennummern, FEDecomposition sysdat) {
        super(sluiceLevel, knotennummern, sysdat);
    }

    @Override
    public double[] getV(DOF dof, double h) {
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);

        FElement[] aElements = dof.getFElements();
        cmd.dhdx = 0.;
        cmd.dhdy = 0.;
//        double sArea = 0.;
        double h_mean = 0.;
        int hi = 0;
        for (int i = 0; i < aElements.length; i++) {
            FElement elem = aElements[i];
//            double area = elem.getVolume();
//            sArea += area;
//            cmd.dhdx += Current2DElementData.extract(elem).dhdx * area;
//            cmd.dhdy += Current2DElementData.extract(elem).dhdy * area;
            
            for (int ll = 0; ll < 3; ll++) {
                if (elem.getDOF(ll) == dof) {
                    for (int ii = 1; ii < 3; ii++) {
                        CurrentModel2DData tmpcdata = CurrentModel2DData.extract(elem.getDOF((ll + ii) % 3));
                        if (tmpcdata.totaldepth > CurrentModel2D.WATT) {
                            h_mean += tmpcdata.eta;
                            hi++;
                        }
                    }
                    break;
                }
            }
        }
        h_mean /= hi;
//        cmd.dhdx /= sArea; // Peter 08.05.2012 wird in CurrentModel2D schon berechnet
//        cmd.dhdy /= sArea;

        if (h_mean + sluiceLevel > 0.) {
            cmd.eta = -sluiceLevel;   
        }
        cmd.totaldepth = cmd.z + cmd.eta;
        
//        /* Wattstrategie fuer Stroemung   */
        cmd.wlambda = Function.min(1., cmd.totaldepth / CurrentModel2D.WATT);
        cmd.w1_lambda = 1. - cmd.wlambda;
        if (cmd.totaldepth < CurrentModel2D.WATT / 2.) {
            cmd.u *= cmd.totaldepth / CurrentModel2D.WATT / 2.;
            cmd.v *= cmd.totaldepth / CurrentModel2D.WATT / 2.;
        } else {
            double mue=0.58;  // Zielke Stroe-Skript
            double c=0.89;
            double hw = Function.max(0.,h_mean + sluiceLevel);
            double rd = Function.max(0.,dof.z+h)/Function.max(CurrentModel2D.WATT,dof.z+h_mean);
            double v = (2./3.*mue * c * Math.sqrt(2.* PhysicalParameters.G +hw))  * rd;
            if(Function.norm(cmd.u,cmd.v)>v){
                double fac = v/Function.norm(cmd.u,cmd.v);
                cmd.u *= fac;
                cmd.v *= fac;
            }
        }
        return new double[]{cmd.u, cmd.v};
    }
}
