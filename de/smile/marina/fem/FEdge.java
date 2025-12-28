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
import javax.vecmath.Point3d;

public class FEdge extends FElement {
    private double distance = 0.;

    private final double koeffmat[][] = new double[2][2];

    public FEdge(Edge edge) {
        element = edge;
        distance = ((Edge) element).distance();
        dofs = new DOF[] { (DOF) edge.getPoint(0), (DOF) edge.getPoint(1) };
        koeffmat[0][1] = -1. / distance;
        koeffmat[1][1] = 1. / distance;
    }

    public FEdge(DOF b, DOF e) {
        element = new Edge(b, e);
        distance = ((Edge) element).distance();
        dofs = new DOF[] { b, e };
        koeffmat[0][1] = -1. / distance;
        koeffmat[1][1] = 1. / distance;
    }

    public double elm_size() {
        return distance;
    }

    public double[][] getkoeffmat() {
        double mat[][] = new double[2][2];
        for (int i = 0; i < 2; i++)
            System.arraycopy(koeffmat[i], 0, mat[i], 0, 2);
        return mat;
    }

    @Override
    public double getVolume() {
        return distance;
    }

    @Override
    public boolean contains(Point3d p) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
                                                                       // Tools | Templates.
    }
}
