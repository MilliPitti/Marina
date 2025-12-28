/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2025

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

/** van Rijn 1984
 * @author Peter Milbradt
 * @version 2.10.10
 */
public class SuspendedLoad2DvanRijn1984 implements SuspendedLoad2DFormulation {

//    gilt nur fuer Korngoessen zwischen 180 mikrometer und 700 mikrometer hierfuer gibt es den Parameter lambda
    private final static double dmax = 0.001;
    private final static double doubledmax = 2*dmax;
//    final double dmin = 0.0001;
//    final double halfdmin = 0.5*dmin;
//    gilt nur fuer Wassertiefen ab 0.1 m
    
    
    @Override
    public String toString() {
        return "van Rijn (1984)";
    }
    
    @Override
    public double getConcentration(DOF dof) {
        return getConcentrationDelft3D(dof);
    }
    
    public double getConcentrationPeters(DOF dof) {
        final double alpha=0.008;
        final double nue=2.5;
        double rvalue = 0.;

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        if (cmd.totaldepth < CurrentModel2D.halfWATT) return 0.;
        final double totaldepth = Math.max(0.1, cmd.totaldepth);
        final double u_diff = cmd.cv - SedimentModel2DData.criticalVelocityVanRijn(smd.d50, totaldepth);
        if (u_diff > 1.E-5) {
            final double Me = u_diff / Math.sqrt((PhysicalParameters.RHO_SEDIM - cmd.rho) / cmd.rho * PhysicalParameters.G * smd.d50);
            rvalue = alpha / totaldepth * smd.d50 * Math.pow(Me, nue) * Math.pow(smd.D, -0.6);
            rvalue = Function.min(cmax, rvalue);
            rvalue *= Function.min(1., cmd.totaldepth/0.1);  // increasing depending on water depth
        }
        return rvalue;
    }
    
    public double getConcentrationDelft3D(DOF dof) {
        final double alf1    = 2.0;  // calibration coefficient [-]
        final double rksc    = 0.1;        // reference level van Rijn (1984) [m]
        final double smfac   = 1.; // factor for sand-mud interaction

        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        
        if( cmd.totaldepth/rksc < 1.33 || cmd.cv < 1.E-3) return 0.;
        
        double d90 = smd.dmax; // oder 4 * d50 wie dies bei Delft3D vorgeschlagen wird
        
        final double del = (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER;
        final double dstar = smd.d50*Math.pow(del*PhysicalParameters.G/PhysicalParameters.KINVISCOSITY_WATER/PhysicalParameters.KINVISCOSITY_WATER,1./3.);
        final double rmuc = Math.pow(Math.log10(12.*cmd.totaldepth/rksc)/Math.log10(12.*cmd.totaldepth/3./d90),2.);
        final double fc = .24*Math.pow(Math.log10(12.*cmd.totaldepth/rksc), -2);
        final double tbc = .125*PhysicalParameters.RHO_WATER*fc*Math.pow(cmd.cv,2);
        final double tbce = rmuc*tbc;
        final double thetcr = shld(dstar); // Critical Shieldsparameter
        final double tbcr = (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)*PhysicalParameters.G*smd.d50*thetcr*smfac;
        double t = (tbce - tbcr)/tbcr;
        if (t<.000001) return 0.;
        double rvalue = .015*alf1*smd.d50/rksc*Math.pow(t, 1.5)/Math.pow(dstar,.3);
        rvalue = Function.min(cmax, rvalue);
        rvalue *= Function.min(1., cmd.totaldepth/0.1);  // increasing depending on water depth
        
        return rvalue;
    }

    public double getConcentrationHR(DOF dof) {
        final double e_s = 0.015; // constants, default e_s = 0.015

        double rvalue = 0.;

        SedimentModel2DData smd = SedimentModel2DData.extract(dof);
        if(smd.d50>doubledmax) return 0.;
        
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        if(cmd.totaldepth<CurrentModel2D.halfWATT) return 0.;
        
        final double CSF = smd.CSF; // variable // Bodenneigungsanteil wird schon bei der Berechnung des kritischen Shieldsspannung in SedimentModel2DData beruecksichtigt // 18.05.2011

//        final double eta = Function.max(smd.bound, Function.min(cmd.totaldepth, cmd.totaldepth
//                / PhysicalParameters.G * (PhysicalParameters.RHO_WATER * cmd.grainShearStress * cmd.cv + Function.norm(cmd.tau_bx_extra,cmd.tau_by_extra)))); // dicke der suspensionsschicht ToDo formel ergaenzen _ Chezy-Koeffizient


        double sfx = cmd.tauBx / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);
        double sfy = cmd.tauBy / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * smd.d50);

        double sf = Function.norm(sfx, sfy);
//        sf *= smd.lambda; // increasing depending on not erodible bottom // Peter 01.06.2013
        sf *= Function.min(1., cmd.totaldepth/0.1);  // increasing depending on water depth

        double lambda;
        if (sf > CSF) {
            if(smd.d50<=dmax)
                lambda = 1.; 
            else
                lambda = Math.min(1., Math.max(0., (doubledmax-smd.d50))/(dmax)); // Abminderung auf Grund zu grosser Koerner

            rvalue = e_s * smd.d50/Function.max(0.1, cmd.totaldepth) * Math.pow((sf-CSF)/CSF,1.5) / Math.pow(smd.D,0.3) * lambda;
            rvalue = Function.min(cmax, rvalue);
            rvalue *= Function.min(1., cmd.totaldepth/0.1); // Abminderung auf Grund zu kleiner Wassertiefen
        }
        return rvalue;
    }

    public double getConcentrationMalcharek(DOF dof) {

        double rvalue = 0.;

        CurrentModel2DData currentmodeldata = CurrentModel2DData.extract(dof);
        SedimentModel2DData sdata = SedimentModel2DData.extract(dof);

        double CSF = sdata.CSF; // variable

        double sfx = ((sdata.grainShearStress * currentmodeldata.u) * PhysicalParameters.RHO_WATER + currentmodeldata.tau_bx_extra) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * sdata.d50);
        double sfy = ((sdata.grainShearStress * currentmodeldata.v) * PhysicalParameters.RHO_WATER + currentmodeldata.tau_by_extra) / ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * sdata.d50);

        double sf = Function.norm(sfx, sfy);
        sf *= sdata.lambda; // decreasing depending on not erodible bottom
        sf *= Function.min(1., currentmodeldata.totaldepth/0.1);  // increasing depending on water depth // Peter 23.08.2010

        if (sf > CSF) {
            double z0 = Function.min(0.3*sdata.d50  * Math.pow((sf-CSF)/CSF,0.5) * Math.pow(sdata.D,0.7),currentmodeldata.totaldepth);
            rvalue = 0.18 * 0.65 * (sf-CSF)/CSF / sdata.D *z0/Function.max(sdata.bound, currentmodeldata.totaldepth) * Function.min(1., currentmodeldata.totaldepth/0.1);
        }
        return rvalue;
    }

    @Override
    public double getLoad(DOF dof) {
        return getConcentration(dof) * CurrentModel2DData.extract(dof).totaldepth;
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
