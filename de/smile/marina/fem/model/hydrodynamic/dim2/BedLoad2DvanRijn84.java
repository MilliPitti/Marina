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
package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.DOF;
import de.smile.math.Function;

/** wie in Delft3D implementiert
 * @author Peter Milbradt
 * @version 2.10.10
 */
public class BedLoad2DvanRijn84 implements BedLoad2DFormulation {
    /* Gueltigkeitsbereich 0.2mm < d50 < 2mm */
    static double rksc    = 0.1;        // reference level van Rijn (1984) [m]
    static double smfac   = 1.; // factor for sand-mud interaction
    @Override
    public String toString() {
        return "van Rijn (1984)";
    }

    @Override
    public double[] getLoadVector(DOF dof) {

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        smd.bedload = 0.0;
        smd.bedloadVector[0] = 0.;
        smd.bedloadVector[1] = 0.;
        
        if( cmd.totaldepth/rksc < 1.33 || cmd.cv < 1.E-3) return smd.bedloadVector;
        
        double d90 = smd.dmax; // oder 4 * d50 wie dies bei Delft3D vorgeschlagen wird
        
        final double del = (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER;
        final double dstar = smd.d50*Math.cbrt(del*PhysicalParameters.G/PhysicalParameters.KINVISCOSITY_WATER/PhysicalParameters.KINVISCOSITY_WATER);
        final double rmuc = Math.pow(Math.log10(12.*cmd.totaldepth/rksc)/Math.log10(12.*cmd.totaldepth/3./d90),2.);
        final double fc = .24*Math.pow(Math.log10(12.*cmd.totaldepth/rksc), -2);
        final double tbc = .125*PhysicalParameters.RHO_WATER*fc*Math.pow(cmd.cv,2);
        final double tbce = rmuc*tbc;
        final double thetcr = shld(dstar); // Critical Shieldsparameter
        final double tbcr = (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)*PhysicalParameters.G*smd.d50*thetcr*smfac; // critical bottom shear stress
        double t = (tbce - tbcr)/tbcr;
        if (t<.000001) t = .000001;
        if (t<3.)
            smd.bedload = 0.053*Math.sqrt(del)*Math.sqrt(PhysicalParameters.G)* Math.pow(smd.d50,1.5) / Math.pow(dstar, 0.3) * Math.pow(t, 2.1);
        else
            smd.bedload = 0.1*Math.sqrt(del)*Math.sqrt(PhysicalParameters.G)* Math.pow(smd.d50,1.5) / Math.pow(dstar, 0.3) * Math.pow(t, 1.5);
        
        smd.bedload = Math.min(1./SedimentModel2D.morphFactor * cmd.cv*Function.max(0.,smd.zh - smd.z)*(1. - smd.porosity), smd.bedload);
        
        // dirctional bed load
        smd.bedloadVector[0] = smd.bedload * cmd.u / cmd.cv;
        smd.bedloadVector[1] = smd.bedload * cmd.v / cmd.cv;

        return smd.bedloadVector;
    }
    
    static double shld(double dstar){
        double shld;
    
        if (dstar<=4.) shld = 0.240/dstar;
        else if (dstar<=10.) shld = Math.pow(0.140/dstar, 0.64);
        else if (dstar<=20.) shld = Math.pow(0.040/dstar, 0.10);
        else if (dstar<=150.) shld = Math.pow(0.013*dstar,0.29);
        else shld = 0.055;
        
        return shld;
    }
}
