/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2022

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
package de.smile.marina.fem.model.ecological;

import de.smile.marina.MarinaXML;
import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.fem.model.hydrodynamic.dim2.*;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.DataOutputStream;

/** Muschelmodell unabhaengig von der Art 
 *
 * @author Peter
 */
public class MusselModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel {
    
    private static double musselHeight = 0.1; // Grenze ab der die Muschel als trocken gefallen angesehen werden
    private boolean basedOnCurrentModel3D = false;

    private Mussel2DData[] dof_data=null;

    private SurfaceWaterModelData[] dof_surfaceWaterModelData=null;
    
    private DataOutputStream xf_os = null;

    @Override
    public void setBoundaryCondition(DOF dof, double t) {
        Mussel2DData mussel2DData = dof_data[dof.number];
        
        /* prevention of negative concentration */
        if (mussel2DData.bioMass < 0.)   mussel2DData.bioMass = 0.;
        
        /* nicht mehr Biomasse als Maximal moeglich */
        if (mussel2DData.bioMass > mussel2DData.getMaxBioMass())   mussel2DData.bioMass = mussel2DData.getMaxBioMass();
        
        // Update Muschelwachstumsparameter
        final SurfaceWaterModelData surfaceWaterModelData = dof_surfaceWaterModelData[dof.number];
        if(surfaceWaterModelData.totaldepth<musselHeight){
            mussel2DData.growthRateDecreasingFactor *= surfaceWaterModelData.totaldepth/musselHeight;
            mussel2DData.mortalityRateIncreasingFactor += (1.-surfaceWaterModelData.totaldepth/musselHeight);
        }
        
        final SaltModel2DData  saltmd  = SaltModel2DData.extract(dof);
        if(saltmd!=null){
            mussel2DData.growthRateDecreasingFactor *= mussel2DData.species.salzPotential.getValue(saltmd.C);
        }
                    
        final CurrentModel2DData  cmd  = CurrentModel2DData.extract(dof);
        if(cmd!=null){
//            mussel2DData.growthRateDecreasingFactor *= mussel2DData.tauBottomMemberFunction.getValue(cmd.tau_b);
        }
        
        final SedimentModel2DData  sedmd  = SedimentModel2DData.extract(dof);
        if(sedmd!=null){
//            mussel2DData.growthRateDecreasingFactor *= mussel2DData.d50MemberFunction.getValue(sedmd.d50);
//            mussel2DData.growthRateDecreasingFactor *= mussel2DData.dzdtMemberFunction.getValue(sedmd.dzdt);
        }
                    
//                    double algalConcentration = 0.;

        
    }

    @Override
    public double[] getRateofChange(double time, double[] x) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ModelData genData(DOF dof) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ModelData genData(FElement felement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double ElementApproximation(FElement element) {
        double timeStep=Double.POSITIVE_INFINITY;

        FTriangle ele = (FTriangle) element;
        final double[][] koeffmat = ele.getkoeffmat();
        
        double[] terms = new double[3];
                
        // compute element derivations
        //-------------------------------------------------------------------
                double dbmdx = 0.;
                double dbmdy = 0.;
                for (int j = 0; j<3; j++) {
                    Mussel2DData mussel2DData = dof_data[ele.getDOF(j).number];
                    dbmdx += mussel2DData.bioMass * koeffmat[j][1];
                    dbmdy += mussel2DData.bioMass * koeffmat[j][2];
                } // end for

                double Koeq1_mean = 0.;
                // Elementfehler berechnen
                for (int j = 0; j < 3; j++) {
                    Mussel2DData mussel2DData = dof_data[ele.getDOF(j).number];
                    
                    terms[j] = (mussel2DData.growthRateDecreasingFactor*mussel2DData.getGrowthRate() - mussel2DData.mortalityRateIncreasingFactor*mussel2DData.getMortalityRate()) * mussel2DData.bioMass
                            // + (u*dbmdx+v*dbmdy) * wenn Temperatur des Wassers > 20 grad C * Larven zu Biomassenverhaeltnis
                            ; 
                    
                    Koeq1_mean += 1. / 3. * ( mussel2DData.dbmdt + terms[j] );
                }
//                
//                double tau_konc=0.;
//                if (current_mean > 1.E-5) {
//                    tau_konc = 0.5 * elementsize / current_mean;
//                    timeStep = tau_konc;
//                }
                
                // Fehlerkorrektur durchfuehren
                for (int j = 0; j < 3; j++) {
                    Mussel2DData mussel2DData = dof_data[ele.getDOF(j).number];
                    final CurrentModel2DData  cmd  = CurrentModel2DData.extract(ele.getDOF(j));
                    
                    double result_bioMass_i = 0; //-tau_konc * (koeffmat[j][1] * u_mean + koeffmat[j][2] * v_mean) * Koeq1_mean;
//                    if(result_bioMass_i>0) result_bioMass_i *= cmd.wlambda; // Konzentration will wachsen, Knoten aber Wattknoten
                    
                    // tubulenc-term // Peter 29.09.2016
//                    result_bioMass_i -= + (koeffmat[j][1] * astx * dbmdx + koeffmat[j][2] * asty * dbmdy) * eleCurrentData.wlambda;
                    
                    // Begin standart Galerkin-step
                    for (int l = 0; l < 3; l++) {
                        final double vorfak = (l == j) ? 1./6. : 1./12.;
                        final double gl = (l == j) ? 1. :  Function.min(CurrentModel2DData.extract(ele.getDOF(l)).wlambda, CurrentModel2DData.extract(ele.getDOF(l)).totaldepth/Function.max(CurrentModel2D.WATT,cmd.totaldepth));
                        result_bioMass_i -= vorfak * terms[l]*gl;
                    }
                    synchronized (mussel2DData) {
                        mussel2DData.rbioMass += result_bioMass_i;
                    }
                }

        return timeStep;
    }

    @Override
    public void write_erg_xf(double[] erg, double t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getTicadErgMask() {
        // Setzen der Ergebnismaske MuschelBiomasse g pro quadratmeter
        return TicadIO.HRES_H;
    }

    @Override
    public void timeStep(double dt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write_erg_xf() {
        try {
            xf_os.writeFloat((float) time);
            for (DOF dof : fenet.getDOFs()) {
                if (MarinaXML.release) {
                    setBoundaryCondition(dof, time);
                }
                Mussel2DData mussel2DData = dof_data[dof.number];
                xf_os.writeFloat((float)mussel2DData.bioMass);
            }
            xf_os.flush();
        } catch (Exception e) {
            System.out.println(this.getClass()+"\n\ttime="+time+"\n");
            e.printStackTrace();
            System.exit(0);
        }
    }

}
