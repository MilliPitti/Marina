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

import bijava.marina.geom3d.BoundingBox3d;
import de.smile.geom.Rectangle2d;
import javax.vecmath.*;

/**
 * @version 3.17.3
 * @author Peter Milbradt
 */
public class FEDecomposition {
    
    public int epsg = -1; // coordinatereferencesystem

    protected FElement[] feelem = new FElement[0];
    protected DOF[] dofs = new DOF[0];
    
    private KDTree kdtree=null;

    public FEDecomposition() {
    }

    public FEDecomposition(FElement[] elements, DOF[] dof) {
        this.feelem = elements;
        this.dofs = dof;
    }
    
    public void initialMeanEdgeLength(){
        for (DOF dof : dofs) {
            dof.computeMeanEdgeLength();
        }
    }

    /**
     * @param element
     */
    public final void addFElement(FElement element) {
        for (FElement feelem1 : feelem) {
            if (feelem1 == element) {
                return;
            }
        }
        for (int i = 0; i < element.getNumberofDOFs(); i++) {
            DOF dof = element.getDOF(i);
            addDOF(dof);
            dof.addFElement(element);
        }
        FElement[] tmp = new FElement[feelem.length + 1];
        System.arraycopy(feelem, 0, tmp, 0, feelem.length);
        tmp[feelem.length] = element;
        feelem = tmp;
    }

    public final FElement getFElement(int i) {
        return feelem[i];
    }

    public final FElement[] getFElements() {
        return feelem;
    }

    public final int getNumberofFElements() {
        return feelem.length;
    }

    public final void addDOF(DOF dof) {
        for (DOF dof1 : dofs) {
            if (dof1 == dof) {
                return;
            }
        }
        DOF[] tmp = new DOF[dofs.length + 1];
        System.arraycopy(dofs, 0, tmp, 0, dofs.length);
        tmp[dofs.length] = dof;
        dofs = tmp;
    }

    public final void setDOF(int j, DOF dof) {
        for (DOF dof1 : dofs) {
            if (dof1 == dof) {
                return;
            }
        }
        DOF[] tmp = new DOF[Math.max(dofs.length, j + 1)];
        System.arraycopy(dofs, 0, tmp, 0, dofs.length);
        tmp[j] = dof;
        dofs = tmp;
    }

    public final DOF getDOF(int i) {
        return dofs[i];
    }

    public final DOF[] getDOFs() {
        return dofs;
    }

    public final int getNumberofDOFs() {
        return dofs.length;
    }

    public final FElement getElement(Point3d p) {
        if (p == null) {
            return null;
        }

        if (kdtree == null) {
            generateTree();
        }
        return kdtree.searchElement(p);
    }

    public Rectangle2d getBounds() {
        double xmin=Double.MAX_VALUE;
	double ymin=Double.MAX_VALUE;
	double xmax=(-1)*Double.MAX_VALUE;
	double ymax=(-1)*Double.MAX_VALUE;

	// minimales, maximales x und y ermitteln
	for (DOF dof: dofs){
            if (dof.x<xmin)	xmin=dof.x;
            if (dof.x>xmax)	xmax=dof.x;
            if (dof.y<ymin)	ymin=dof.y;
            if (dof.y>ymax)	ymax=dof.y;
        }
        return new Rectangle2d(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    public int getElementSize() {
        return feelem.length;
    }
    
    public void generateTree() {
        if (feelem.length > 0) {
            generateTree(Math.max(100, (int) Math.pow(feelem.length, 1. / 3.)));
        }
    }

    public void generateTree(int max_objects_in_treenode) {
        if (feelem.length > 0) {
            kdtree = new KDTree(this, max_objects_in_treenode);
        }
    }

    public BoundingBox3d getBoundingBox3d() {
        double minX = dofs[0].x;
        double maxX = minX;
        double minY = dofs[0].y;
        double maxY = minY;
        double minZ = dofs[0].z;
        double maxZ = minZ;

        for (int i = 1; i < dofs.length; i++) {
            minX = Math.min(minX, dofs[i].x);
            minY = Math.min(minY, dofs[i].y);
            minZ = Math.min(minZ, dofs[i].z);
            maxX = Math.max(maxX, dofs[i].x);
            maxY = Math.max(maxY, dofs[i].y);
            maxZ = Math.max(maxZ, dofs[i].z);
        }
        Point3d lower = new Point3d(minX, minY, minZ);
        Point3d upper = new Point3d(maxX, maxY, maxZ);
        return new BoundingBox3d(lower, upper);
    }
}
