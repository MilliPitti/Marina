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

import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.Iterator;

/**
 *
 * @author Peter Milbradt
 * @version 4.1.0
 */
public abstract class SurfaceWaterModelData implements ModelData {

    private static final long serialVersionUID = 1L;
    
    public double eta; // free surface waterlevel
    public double detadt;
    
    public double z; // location of the bottom in depth
    
    public double totaldepth; //z+eta actual total depth
    
    // boudary conditions
    public ScalarFunction1d bqx = null;
    public ScalarFunction1d bqy = null;
    public ScalarFunction1d bu = null;
    public ScalarFunction1d bv = null;
    public ScalarFunction1d bh = null;
    public boolean extrapolate_h;
    public boolean extrapolate_u;
    public boolean extrapolate_v;
    public QSteuerung bQx = null;
    public QSteuerung bQy = null;
    
    public double kst = 48.; // Stricklerbeiwert(Bodenrauhheit) in [m**(1/3) / s]
    public double ks = Math.pow(25. / kst, 6.); // aequivalente Bodenhauheit fuer Nikuradse [mm]
   
    public double tauBx=0., tauBy=0., tauBz=0.; // resultierende Bodenschubspannungen
    
    public double wlambda = 1.;
    public double w1_lambda = 0.; // lambda und (1-lambda) zur Beruecksichtigung des Trockenfallens: lambda=Function.min(1.,totaldepth/WATT);
    
    public double puddleLambda = 0.; // ist 0 wenn alle Knoten im Patch trocken sind (initialisieren mit 0, dann max mit cmd.wlambda)

    public boolean closedBoundary = false; // indicator that the node is at the cloused boundary
    
    public boolean boundary = false; // indicator that the node has boundary conditions - is nessesery for z-boundarycondition in SedimentModel2D
    
    protected SurfaceWaterModelData() {
    }
    
    protected SurfaceWaterModelData(DOF dof) {
        z=dof.z;
    }
    
    public static SurfaceWaterModelData extract(DOF dof) {
        Iterator<ModelData> modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = modeldatas.next();
            if(md instanceof SurfaceWaterModelData surfaceWaterModelData)  return surfaceWaterModelData;
        }
        return null;
    }
}
