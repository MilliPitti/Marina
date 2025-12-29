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

import bijava.math.ifunction.ZeroFunction1d;
import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.TicadModel;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;
import de.smile.marina.io.BoundaryConditionsReader;
import java.util.ArrayList;

/** abstrakte Oberklasse zu CurrentModel2D und ..3D
 *
 * @author milbradt
 * @version 3.13.5
 */
public abstract class SurfaceWaterModel extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel {

    protected boolean nikuradse = false;
    
    protected final ArrayList<BoundaryCondition> bqx = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> bqy = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> bu = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> bv = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> bh = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> bQx = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> bQy = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> sQ = new ArrayList<>();
    protected final ArrayList<BoundaryCondition> sh = new ArrayList<>();
    
    protected ArrayList<DOF> inith = new ArrayList<>();
    
    static protected double Coriolis = 0.0001;	// Coriolisbeiwert 2*Omega*sin(phi)
    // Omega=2*pi/T mit T=86400s periode der Erddrehung
    // phi geographische Breite ca.54,5 fuer Ostsee und Nordfriesland
    
    protected double infiltrationRate = 0.; // 1.e-5; // initial infilration rate of fine sand
    protected static final double SqrtFrom2 = Math.sqrt(2.);

    public static double WATT = 0.01;
    public static double halfWATT = 0.005;
    
    public SurfaceWaterModel() {
    }

    /** Neue Einleseroutine readBoundCond
     * liest die spezifizierten Datensaetze (Randbedingungen) in der boundary_condition_key_mask
     * aus entsprechenden Randwertedatei (currentdat.rndwerte_name)
     * nach der jeweiligen Einleselogik (spezifiziert in currentdat.rndwerteReader)
     * @param rndwerteReader
     */
    public final void readBoundCond(BoundaryConditionsReader rndwerteReader) {
        String[] boundary_condition_key_mask = new String[9];
        boundary_condition_key_mask[0] = BoundaryCondition.absolute_flowrate_x;
        boundary_condition_key_mask[1] = BoundaryCondition.absolute_flowrate_y;
        boundary_condition_key_mask[2] = BoundaryCondition.specific_flowrate_x;
        boundary_condition_key_mask[3] = BoundaryCondition.specific_flowrate_y;
        boundary_condition_key_mask[4] = BoundaryCondition.free_surface;
        boundary_condition_key_mask[5] = BoundaryCondition.velocity_u;
        boundary_condition_key_mask[6] = BoundaryCondition.velocity_v;
        boundary_condition_key_mask[7] = BoundaryCondition.pointbased_Q_source;
        boundary_condition_key_mask[8] = BoundaryCondition.pointbased_h_source;
        if (rndwerteReader != null) {
            try {
                for (BoundaryCondition bc : rndwerteReader.readBoundaryConditions(boundary_condition_key_mask)) {
                    if (bc.boundary_condition_key.equals(BoundaryCondition.absolute_flowrate_x)) {
                        bQx.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.absolute_flowrate_y)) {
                        bQy.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.specific_flowrate_x)) {
                        bqx.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.specific_flowrate_y)) {
                        bqy.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.free_surface)) {
                        bh.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.velocity_u)) {
                        bu.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.velocity_v)) {
                        bv.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.pointbased_h_source)) {
                        sh.add(bc);
                    }
                    if (bc.boundary_condition_key.equals(BoundaryCondition.pointbased_Q_source)) {
                        sQ.add(bc);
                    }
                }
            } catch (Exception e) {
                System.exit(1);
            }
        }
    } // end readBoundCond
    
    // ----------------------------------------------------------------------
    // generateClosedBoundCond
    // ----------------------------------------------------------------------
    public final void generateClosedBoundCond(){
        ZeroFunction1d zerofct  = new ZeroFunction1d();
        SurfaceWaterModelData current;
        for (FElement felem : fenet.getFElements()) {
            FTriangle tele = (FTriangle) felem;
            if (tele.getKennung() != 0){
                
                if (tele.getKennung() == FTriangle.bit_kante_jk) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_jk+" bit_kante_jk");
                    current = SurfaceWaterModelData.extract(tele.getDOF(1));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                    
                    current = SurfaceWaterModelData.extract(tele.getDOF(2));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                }
                else if(tele.getKennung() == FTriangle.bit_kante_ki) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ki+" bit_kante_ki");
                    current = SurfaceWaterModelData.extract(tele.getDOF(0));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                    
                    current = SurfaceWaterModelData.extract(tele.getDOF(2));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                }
                else if(tele.getKennung() == FTriangle.bit_kante_ij) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ij+" bit_kante_ij");
                    current = SurfaceWaterModelData.extract(tele.getDOF(0));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                    
                    current = SurfaceWaterModelData.extract(tele.getDOF(1));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                }
                else if((tele.getKennung() == FTriangle.bit_kante_ijk) ||
                        (tele.getKennung() == FTriangle.bit_kante_jki) ||
                        (tele.getKennung() == FTriangle.bit_kante_kij) ||
                        (tele.getKennung() == FTriangle.bit_kante_ijki) ) {
                    //	System.out.println("alle");
                    current = SurfaceWaterModelData.extract(tele.getDOF(0));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                    
                    current = SurfaceWaterModelData.extract(tele.getDOF(1));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                    
                    current = SurfaceWaterModelData.extract(tele.getDOF(2));
                    if (current.bu == null) current.bu = zerofct;
                    if (current.bv == null) current.bv = zerofct;
                    current.closedBoundary = true;
                }
            }
        }

        for (DOF dof : fenet.getDOFs()) {
            current = SurfaceWaterModelData.extract(dof);
            current.extrapolate_h = ((current.bu != null) && (current.bv != null) && (current.bh == null) && !current.closedBoundary) || ((current.bqx != null) && (current.bqy != null) && (current.bh == null)); // kein extrapolieren der Wasserstaende an geschlossenen Knoten
            current.extrapolate_u = ((current.bh != null) && (current.bu == null)) || ((current.bh != null) && (current.bqx == null)) || ((current.bh != null) && (current.bQx == null));
            current.extrapolate_v = ((current.bh != null) && (current.bv == null)) || ((current.bh != null) && (current.bqy == null)) || ((current.bh != null) && (current.bQy == null));
        }
    } // end generateClosedBoundCond

    
    public static void setLatitude(double latitude) {
        Coriolis = 4. * Math.PI / 86400 * Math.sin(Math.toRadians(latitude));
    }

    public static double getCoriolisParameter() {
        return Coriolis;
    }
    
}
