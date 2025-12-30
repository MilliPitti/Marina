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
package bijava.marina.geom3d;

import java.util.*;
import javax.vecmath.*;

public class DomainDecomposition {
  ArrayList<Point3d> points = new ArrayList<>();
  ArrayList<ConvexCell3d> elements = new ArrayList<>();
    
  public void addElement(ConvexCell3d element){
    Point3d[] node = element.getPoints();
      for (Point3d node1 : node) {
          if (!points.contains(node1)) {
              points.add(node1);
          }
      }
    if (!elements.contains(element)) elements.add(element);
  }
  
  public ConvexCell3d getElement(int i){
    return elements.get(i);
  }
    
    public int getNumberofElements(){
	return elements.size();
    }
    
  public Point3d getPoint(int i) {
	return points.get(i);
    }
  
  public int getNumberofPoints(){
	return points.size();
  }
}
