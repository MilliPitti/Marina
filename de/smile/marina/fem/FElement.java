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
package de.smile.marina.fem;

import bijava.marina.geom3d.*;
import de.smile.geom.Rectangle2d;
import java.io.Serializable;
import javax.vecmath.*;
import java.util.*;
 
public abstract class FElement implements Serializable{

    private static final long serialVersionUID = 1L;
  ConvexCell3d element;
  int order=0;
  DOF[] dofs = new DOF[0];
  
  public int number;
  
  // elementmodeldata, g.E. derivations of values used in other models, 
  // viscosity used in current, sediment, salt, wave ..
  public  ArrayList<ModelData> modelData = new ArrayList<>(1);

  public final int getOrder(){
    return order;
  }
  
  public final void setDOFs(DOF[] dofs){
	this.dofs=dofs;
  }

  public final DOF[] getDOFs(){
	return dofs;
  }

  public final int getNumberofDOFs(){
    return dofs.length;
  }
  public final DOF getDOF(int i){
    return dofs[i];
  }
  
  public final void addModelData(FEModel model) {
        modelData.add(model.genData(this));
  }
  
  /** Return the Iterator of ModelData at the DOF
     * @return Iterator of ModelData
     */
    public final Iterator<ModelData> allModelDatas(){
        return modelData.iterator();
    }
/** Test
     * @param i    
     * @return  */    
    public final ModelData getModelData(int i){
        return modelData.get(i);
    }
    public final int getIndexOf(ModelData md){
        return modelData.indexOf(md);
    }
  
  //neu-----------
  public double[] getNaturefromCart(Point3d p){
	  return element.getNaturefromCart(p);
  }
  public abstract boolean contains(Point3d p);
//  {
//	  return element.contains(p);
//  }
  
  public Rectangle2d getBounds() {
        return element.getBounds();
    }
  
  public abstract double getVolume();

    public Point3d[] getPointsOfConvexCell3d() {
        return element.getPoints();
    }

  /** erzeugt einen DOF mit den zugehoerigen Modelldaten und belegt diese mit
     *  den Interpolierten Werten
     */
//    public DOF Interpolate(Point3d point){
//        DOF result = null;
//        if (element.contains(point)){
//            result=new DOF(point);  //diesmal ohne Modelldaten
//            Iterator<ModelData> modeldatas = dofs[0].allModelDatas();
//
//            while (modeldatas.hasNext()) {
//                ModelData md = modeldatas.next();
//                ModelData mdres = md.initialNew();
//                for (int i=0; i<interpolationFunction.length;i++){
//                     mdres = mdres.add(interpolationFunction[i].getDOF().getModelData(md).mult(interpolationFunction[i].getValue(point)));
//                }
//                result.addModelData(mdres);
//            }
//        }
//        return result;
//    }
}
