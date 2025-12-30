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
