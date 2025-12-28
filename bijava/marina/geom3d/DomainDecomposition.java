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
