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
package de.smile.marina.fem;

import java.util.*;
import javax.vecmath.*;

/** Degree of Freedom
 * @version 4.7.0
 */
public class DOF extends Point3d {

    private static final long serialVersionUID = 1L;
    public int number =-1;
    private ModelData[] modelData = new ModelData[0];
    private FElement[] felements = new FElement[0];
    
    public double meanEdgeLength = 0.; // mittle Laenge der Kanten zu denen der Punkt gehoert
    
    public double lumpedMass;
    
    // Diese Methode könnte auch außerhalb stehen und das DOF-Objekt als Argument nehmen
    public void calculateLumpedMass() {
        this.lumpedMass = 0.0;
        for (FElement element : felements) {
            // Annahme: getVolume() gibt die Fläche für 2D-Elemente zurück
            this.lumpedMass += element.getVolume() / 3.0; 
        }
    }
    
    void computeMeanEdgeLength(){
        meanEdgeLength = 0.;
        int iEdges = 0;
        for (FElement felement : felements) {
            this.lumpedMass += felement.getVolume() / 3.0;
            for(int i=0;i<3;i++){
                if(felement.dofs[i].equals(this)){
                    meanEdgeLength += this.distance2d(felement.dofs[(i-1+3)%3]);
                    iEdges++;
                    meanEdgeLength += this.distance2d(felement.dofs[(i+1)%3]);
                    iEdges++;
                }
            }
        }
        meanEdgeLength/=iEdges;
    }

    /** construct a DOF
     * @param nr a global number
     * @param x the x-koordinate of the location
     * @param y the y-koordinate of the location
     * @param z the z-koordinate of the location
     */
    public DOF(int nr,double x,double y,double z) {
        super(x,y,z);
        number  = nr;
    }

    public DOF(Point3d p) {
        super(p);
    }
    
    /** return the Number of the DOF
     * @return the global number
     * @deprecated direkter zugriff auf das Attribut number
     */
    @Deprecated
    public final int getNumber() {
        return number;
    }
    
    public final void setNumber(int i){
        number=i;
    }
    
    /** Returns a String representation of the DOF object
     * @return a String representation of the DOF object
     */
    @Override
    public String toString() {
        return "( DOF " +number+" "+super.toString()+")";
    } 
    
    public final void addFElement(FElement element) {
        for (FElement felement : felements) {
            if (felement == element) return;
        }
        FElement[] tmp=new FElement[felements.length+1];
        System.arraycopy(felements,0,tmp,0,felements.length);
        tmp[felements.length]=element;
        felements=tmp;
    }
    
    public final int getNumberofFElements() {
        return felements.length;
    }
    
    /** Returns a Enumeration of FElements appending to this DOF
     * @param i
     * @return Enumeration of FElements appending to this DOF
     */
    public final FElement getFElementAt(int i) {
        return felements[i];
    }
    
    public final FElement[] getFElements() {
        return felements;
    }
    
    /** Append and inilize the Data For the spezified FEModel
     * @param model a object witch implements the FEModel interface
     */
    public void addModelData(FEModel model) {
        ModelData[] tmp=new ModelData[modelData.length+1];
        System.arraycopy(modelData,0,tmp,0,modelData.length);
        tmp[modelData.length]=model.genData(this);
        modelData=tmp;
    }
    /** Append the ModelData
     * @param modeldata a object witch extends the ModelData class
     */
    public void addModelData(ModelData modeldata) {
        for (ModelData modelData1 : modelData) {
            if (modelData1 == modeldata) return;
        }
        ModelData[] tmp=new ModelData[modelData.length+1];
        System.arraycopy(modelData,0,tmp,0,modelData.length);
        tmp[modelData.length]=modeldata;
        modelData=tmp;
    }
    
    
    /** Return the Iterator of ModelData at the DOF
     * @return Iterator of ModelData
     */
    public Iterator<ModelData> allModelDatas(){
        return new ModelDataIterator();
    }

    public double distance2d(Point2d p) {
        return Math.sqrt((x-p.x)*(x-p.x)+(y-p.y)*(y-p.y));
    }

    public double distance2d(Point3d p) {
        return Math.sqrt((x-p.x)*(x-p.x)+(y-p.y)*(y-p.y));
    }

    public double sqrdistance2d(Point2d p) {
        return ((x-p.x)*(x-p.x)+(y-p.y)*(y-p.y));
    }
    
    private class ModelDataIterator implements Iterator<ModelData>{
        private int i = 0;
        
        @Override
        public boolean hasNext() {
            return (i < modelData.length);
        }
        
        @Override
        public ModelData next() {
            if (hasNext())
                return modelData[i++];
            else
                return null;
        }
        
        @Override
        public void remove() {
        }
    }
    
    public ModelData getModelData(int i){
        return modelData[i];
    }
    
    public int getIndexOf(ModelData md){
        for (int i=0; i<modelData.length; i++) {
            if (modelData[i]==md)
                return i;
        }
        return ModelData.NO_MODEL_DATA;
    }

}
