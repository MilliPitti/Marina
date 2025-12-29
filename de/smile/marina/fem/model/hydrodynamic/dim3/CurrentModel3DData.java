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
package de.smile.marina.fem.model.hydrodynamic.dim3;

import de.smile.marina.fem.model.hydrodynamic.dim2.SurfaceWaterModelData;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;

import bijava.math.ifunction.DiscretVectorFunction1d;
import de.smile.marina.PhysicalParameters;
import de.smile.marina.fem.FTriangle;
import de.smile.marina.fem.model.hydrodynamic.dim2.Current2DElementData;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2D;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import static de.smile.marina.fem.model.hydrodynamic.dim2.SurfaceWaterModel.WATT;
import de.smile.math.Function;
import static de.smile.math.Function.norm;
import static java.lang.Math.max;

/**
 * Represents three-dimensional current model data within a surface water simulation.
 * This class extends {@code SurfaceWaterModelData} and manages vertical profiles
 * of various hydrodynamic quantities such as velocity components (u, v, w),
 * momentum fluxes (qx, qy), and their derivatives along the vertical (z) axis.
 * It provides mechanisms for extracting and setting model data relevant to 3D currents,
 * as well as interacting with 2D current data representations.
 *
 * @author Peter
 * @version 4.7.2
 **/
public class CurrentModel3DData extends SurfaceWaterModelData {
    private static int id = NO_MODEL_DATA;
    private static final long serialVersionUID = 1L;

    transient DiscretVectorFunction1d f; // zustandsgroessen
    public static final int _u=0;
    public static final int _v=1;
    public static final int _w=2; // im Tiefenkoordinatensystem
    public static final int _qx=3;
    public static final int _qy=4;
    public static final int _dudt=5;
    public static final int _dvdt=6;
    public static final int _ru=7;
    public static final int _rv=8;
    public static final int _rw=9; // in Tiefen zu sein
    
    public static final int _layerThickness=10;
    public static final int _dudz=11;
    public static final int _dvdz=12;
    public static final int _dwdz=13;
    
    public static final int _du2dz2=14;
    public static final int _dv2dz2=15;
    
    private static final int lastIndex = _dv2dz2;

    double[] bfcoeff; // bottom friction coefficent for each layer
    double[] tau_windx; // wind shear coefficent for each layer
    double[] tau_windy; // wind shear coefficent for each layer
    
    double qx;
    double qy; // depth integrated
    
    boolean wattsickern = true;
    
    /**
     * @param dof
     * @param tiefenverteilung  */
    public CurrentModel3DData(DOF dof, double[] tiefenverteilung) {
        super(dof);
        id = SEARCH_MODEL_DATA;
        f = new DiscretVectorFunction1d(lastIndex+1, tiefenverteilung);
        bfcoeff = new double[tiefenverteilung.length];
        tau_windx = new double[tiefenverteilung.length];
        tau_windy = new double[tiefenverteilung.length];
    }

    public static CurrentModel3DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof CurrentModel3DData currentModel3DData) {
                    id = dof.getIndexOf(md);
                    return currentModel3DData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            try{
                return (CurrentModel3DData) dof.getModelData(id);
            }catch(Exception e){}
        }
        return null;
    }
    
    /** Bestimmt die tiefenintegrierte Geschwindigkeit
     * @return  */
    public double getU() {
        return qx / (((totaldepth < CurrentModel3D.WATT) ? CurrentModel3D.WATT : totaldepth));
    }
    /** Bestimmt die tiefenintegrierte Geschwindigkeit
     * @return  */
    public double getV() {
        return qy / (((totaldepth < CurrentModel3D.WATT) ? CurrentModel3D.WATT : totaldepth));
    }
    
    /** Bestimmung der bodennahen Geschwindigkeit durch Integration bis 1m ueber Boden 
     * @return  */
    public double[] getV_B() {
        double u = 0.;
        double v = 0.;
        double w = 0.;
        if (this.totaldepth >= CurrentModel3D.WATT) {
            double sschichten = 0.;

            for (int s = this.f.getSizeOfValues() - 1; s >= 0 && sschichten < 1.; s--) {
                //Schichtdicke
                double schichtdicke;
                double dh = this.f.getxAt(s);
                if (-dh >= this.eta) {
                    // Schicht an der Wasseroberflaeche
                    schichtdicke = 0.;
                } else {
                    if (s < this.f.getSizeOfValues() - 1) {
                        schichtdicke = Function.min(this.f.getxAt(s + 1) - dh, Function.max(0, this.z - dh));
                    } else {
                        // Unterste Schicht
                        schichtdicke = Function.max(0, this.z - dh);
                    }
                }
                if(sschichten + schichtdicke >= 1.) schichtdicke = 1.-sschichten;
                sschichten += schichtdicke;
                
                u += this.f.getValueAt(_u, s) * schichtdicke;
                v += this.f.getValueAt(_v, s) * schichtdicke;
                w += this.f.getValueAt(_w, s) * schichtdicke;
            }
            if (sschichten > CurrentModel3D.WATT * 0.001) { // schichten sollten eigentlich immer 1 sein
                u /= sschichten;
                v /= sschichten;
                w /= sschichten;
            }
        }
        return new double[]{u, v, w};
    }

    public final CurrentModel2DData getCurrentModel2DData(CurrentModel2DData cm2dd) {

        if(cm2dd==null) cm2dd = new CurrentModel2DData();

        cm2dd.u = this.getU();
        cm2dd.v = this.getV();
        cm2dd.cv = Function.norm(cm2dd.u, cm2dd.v);
        cm2dd.eta = this.eta;
        cm2dd.totaldepth = this.totaldepth;
        cm2dd.tauBx = this.tauBx;
        cm2dd.tauBy = this.tauBy;
        cm2dd.z = this.z;
        cm2dd.kst = this.kst;
        cm2dd.ks = this.ks;
        cm2dd.tau_bx_extra =0.;
        cm2dd.tau_by_extra =0.;

        cm2dd.wlambda = Function.min(1., cm2dd.totaldepth / CurrentModel2D.WATT);
        cm2dd.w1_lambda = 1. - cm2dd.wlambda;
        
        cm2dd.boundary = this.boundary;

        return cm2dd;
    }

    public static Current2DElementData getCurrent2DElementData(FTriangle ele, Current2DElementData element_currentdata) {
        DOF[] dofs = ele.getDOFs();
        if (element_currentdata == null) {
            element_currentdata = new Current2DElementData();
        }

        double[][] koeffmat = ele.getkoeffmat();
        element_currentdata.u_mean = 0;
        element_currentdata.v_mean = 0;
        element_currentdata.depth_mean = 0.;

        element_currentdata.ddepthdx = 0.;
        element_currentdata.ddepthdy = 0.;

        element_currentdata.astx = PhysicalParameters.DYNVISCOSITY_WATER;   // ToDo
        element_currentdata.asty = PhysicalParameters.DYNVISCOSITY_WATER;   // ToDo
        element_currentdata.iwatt = 0;

        int dry = 0;
        for (int jj = 0; jj < 3; jj++) {
            DOF dofj = dofs[jj];
            CurrentModel2DData cmd = CurrentModel2DData.extract(dofj);

            element_currentdata.u_mean += cmd.u / 3.;
            element_currentdata.v_mean += cmd.v / 3.;
            element_currentdata.depth_mean += cmd.totaldepth / 3.;

            element_currentdata.ddepthdx += cmd.totaldepth * koeffmat[jj][1];
            element_currentdata.ddepthdy += cmd.totaldepth * koeffmat[jj][2];

            if (cmd.totaldepth < CurrentModel2D.WATT) {
                element_currentdata.iwatt++;
                if (cmd.totaldepth < CurrentModel2D.halfWATT) {
                    dry++;
                }
            }
        }
        final double elementsize;
        if (norm(element_currentdata.u_mean, element_currentdata.v_mean) > WATT / 10.) {
            elementsize = ele.getVectorSize(element_currentdata.u_mean, element_currentdata.v_mean);
        } else {
            elementsize = ele.minHight;
        }
        element_currentdata.elementsize = elementsize;
        element_currentdata.isDry = (dry == 3); // indicator for dry elements
        return element_currentdata;
    }
    
    synchronized public final void setWaterLevel_synchronized(double h) {  // synchronized notwendig in setBoundaryCondition, da beim glaetten der Wasserspiegellagen an Raendern auch benachbarte Knotenwerte veraendert werden
        setWaterLevel(h);
    }
// ToDo wenn eta abnimmt, was dann mit den Daten der drueber liegenden Schichten?    
    public final void setWaterLevel(double h) {  // wenn moeglich nicht synchronized verwenden
        if ((this.z + h) <= 0.) {
            this.eta = -this.z;
            this.totaldepth = 0.;
            this.wlambda=0;
            this.w1_lambda=1.;
            for(int s=0;s<f.getSizeOfValues();s++) {
                f.setValueAt(_u,s,0.);
                f.setValueAt(_v,s,0.);
                f.setValueAt(_w,s,1.E-6);
                f.setValueAt(_qx, s, 0.);
                f.setValueAt(_qy, s, 0.);
                f.setValueAt(_dudz, s, 0);
                f.setValueAt(_dvdz, s, 0);
                f.setValueAt(_dwdz, s, 0);
                f.setValueAt(_du2dz2, s, 0);
                f.setValueAt(_dv2dz2, s, 0);
                f.setValueAt(_layerThickness, s, 0.);
                tau_windx[s] = 0.;
                tau_windy[s] = 0.;
                bfcoeff[s] = PhysicalParameters.DYNVISCOSITY_WATER;
            } 
        } else {
            this.eta = h;
            this.totaldepth = this.z + this.eta;
            this.wlambda = Function.min(1., this.totaldepth / CurrentModel3D.WATT);
            this.w1_lambda=1.-this.wlambda;
            for (int s = this.f.getSizeOfValues() - 1; s >= 0; s--) {
                if (this.z <= this.f.getxAt(s)) { // echt unterhalb vom Boden
                    f.setValueAt(_u, s, 0.);
                    f.setValueAt(_v, s, 0.);
                    f.setValueAt(_w, s, 0.);
                    f.setValueAt(_qx, s, 0.);
                    f.setValueAt(_qy, s, 0.);
                    f.setValueAt(_dudz, s, 0);
                    f.setValueAt(_dvdz, s, 0);
                    f.setValueAt(_dwdz, s, 0);
                    f.setValueAt(_du2dz2, s, 0);
                    f.setValueAt(_dv2dz2, s, 0);
                    f.setValueAt(_layerThickness, s, 0.);
                    tau_windx[s] = 0.;
                    tau_windy[s] = 0.;
                    bfcoeff[s] = PhysicalParameters.DYNVISCOSITY_WATER;
                } else {
                    double dh = this.f.getxAt(s);
                    if (-dh > this.eta) { // Schicht an der Wasseroberflaeche
                        dh = -this.eta;
                        tau_windx[s] = 0.;
                        tau_windy[s] = 0.;
                        bfcoeff[s] = PhysicalParameters.DYNVISCOSITY_WATER;
                    }
                    final double aktuelletiefe = max(0, this.z - dh);
                    double schichtdicke;
                    if (s < this.f.getSizeOfValues() - 1) {
                        schichtdicke = Math.min(Math.max(0, this.f.getxAt(s + 1) - dh), aktuelletiefe);
                    } else {
                        // Unterste Schicht
                        schichtdicke = aktuelletiefe;
                    }
                    f.setValueAt(_layerThickness, s, schichtdicke);
                }
            }
        }
    }

    synchronized public final void setBottomLevel(double z) {
        this.z = z;
        if ((this.z + eta) <= 0.) {
            this.eta = -this.z;
            this.totaldepth = 0.;
            this.wlambda = 0;
            this.w1_lambda = 1.;
            for (int s = 0; s < f.getSizeOfValues(); s++) {
                f.setValueAt(_u, s, 0.);
                f.setValueAt(_v, s, 0.);
                f.setValueAt(_w, s, 1.E-6);
                f.setValueAt(_qx, s, 0.);
                f.setValueAt(_qy, s, 0.);
                f.setValueAt(_dudz, s, 0);
                f.setValueAt(_dvdz, s, 0);
                f.setValueAt(_dwdz, s, 0);
                f.setValueAt(_du2dz2, s, 0);
                f.setValueAt(_dv2dz2, s, 0);
                f.setValueAt(_layerThickness, s, 0.);
                tau_windx[s] = 0.;
                tau_windy[s] = 0.;
                bfcoeff[s] = PhysicalParameters.DYNVISCOSITY_WATER;
            }  
        } else {
            this.totaldepth = this.z + this.eta;
            this.wlambda = Function.min(1., this.totaldepth / CurrentModel3D.WATT);
            this.w1_lambda = 1. - this.wlambda;
            for (int s = this.f.getSizeOfValues() - 1; s >= 0; s--) {
                if (this.z <= this.f.getxAt(s)) { // echt unterhalb vom Boden
                    f.setValueAt(_u, s, 0.);
                    f.setValueAt(_v, s, 0.);
                    f.setValueAt(_w, s, 0.);
                    f.setValueAt(_qx, s, 0.);
                    f.setValueAt(_qy, s, 0.);
                    f.setValueAt(_dudz, s, 0);
                    f.setValueAt(_dvdz, s, 0);
                    f.setValueAt(_dwdz, s, 0);
                    f.setValueAt(_du2dz2, s, 0);
                    f.setValueAt(_dv2dz2, s, 0);
                    f.setValueAt(_layerThickness, s, 0.);
                    tau_windx[s] = 0.;
                    tau_windy[s] = 0.;
                    bfcoeff[s] = PhysicalParameters.DYNVISCOSITY_WATER;
                } else {
                    double dh = this.f.getxAt(s);
                    if (-dh > this.eta) { // Schicht an der Wasseroberflaeche
                        dh = -this.eta;
                        tau_windx[s] = 0.;
                        tau_windy[s] = 0.;
                        bfcoeff[s] = PhysicalParameters.DYNVISCOSITY_WATER;
                    }
                    final double aktuelletiefe = max(0, this.z - dh);
                    double schichtdicke;
                    if (s < this.f.getSizeOfValues() - 1) {
                        schichtdicke = Math.min(Math.max(0, this.f.getxAt(s + 1) - dh), aktuelletiefe);
                    } else {
                        // Unterste Schicht
                        schichtdicke = aktuelletiefe;
                    }
                    f.setValueAt(_layerThickness, s, schichtdicke);
                }
            }
        }
    }
    
}
