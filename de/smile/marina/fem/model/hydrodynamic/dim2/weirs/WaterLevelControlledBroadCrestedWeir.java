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
import bijava.math.ifunction.ScalarFunction1d;

/**
 * beschreibt ein ueberstroemtes Wehr dessen Wehrhoehe/-stellung duch ein zu steuernden Wasserstand bestimmt wird 
 * @author milbradt
 * @version 1.7.8
 */
public class WaterLevelControlledBroadCrestedWeir extends BroadCrestedWeir{
    
    private ScalarFunction1d waterLevel;
    private int[] mesureNodeID;
    private double weirVelocity=0.05; // [m/s]
    private double t;
    private boolean initial=true;
    public double propFactor = 1., diffFactor = 1.;
    
    private double dcOld=0.;
    
    /** Creates a new instance of Weir */
    public WaterLevelControlledBroadCrestedWeir(int[] mesureNodeID, ScalarFunction1d waterLevel, int[] knotennummern, FEDecomposition sysdat) {
        
        super(0.,knotennummern,sysdat);
        
        this.waterLevel=waterLevel;
        this.mesureNodeID = mesureNodeID;
        
        super.crestLevel=min;
    }
    
    public WaterLevelControlledBroadCrestedWeir(int[] mesureNodeID, ScalarFunction1d waterLevel, int[] knotennummern, FEDecomposition sysdat, double propFactor, double diffFactor) {
        
        super(0.,knotennummern,sysdat);
        
        this.waterLevel=waterLevel;
        this.mesureNodeID = mesureNodeID;
        
        this.propFactor = propFactor;
        this.diffFactor = diffFactor;
        
        super.crestLevel=min;
    }
    
    private void updateCrestLevel(double t){
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

            dc = dt*propFactor * dc  + diffFactor * difdc;
            if(dc > weirVelocity) dc = weirVelocity;
            if(dc < (-weirVelocity)) dc = -weirVelocity;
            setCrestLevel(Math.max(crestLevel - dc ,-waterLevel.getValue(t)-4.*CurrentModel2D.WATT));
        }
    }
    
    @Override
    public double[] getV(DOF dof, double h, double t){
        
        if(initial){ this.t=t; initial=false;}
        
        if(this.t!=t){
            updateCrestLevel(t);
            this.t=t;
        }
        return super.getV(dof,h);
    }
}
