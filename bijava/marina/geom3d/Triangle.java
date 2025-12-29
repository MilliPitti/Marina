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

import de.smile.math.Function;
import javax.vecmath.*;

/**
 * Represents a triangle in 2.5D space, extending the {@link ConvexCell3d} class.
 * It provides methods for calculating area, normal vector, containment checks using barycentric coordinates,
 * Delaunay circle tests, and other geometric properties specific to triangles, such as Voronoi radius and Voronoi point.
 *
 * <p>
 * The triangle is defined by three {@link Point3d} objects representing its vertices.
 * The class also provides methods for determining if a given point is inside the triangle
 * and for calculating the natural coordinates of a point with respect to the triangle.
 * </p>
 *
 * <p>
 * The area is calculated using the determinant formula. The normal vector is calculated using the cross product
 * of two edges of the triangle.  The Voronoi point and radius are computed based on the circumcircle of the triangle.
 * </p>
 */
public class Triangle extends ConvexCell3d{
    double area = 0.;
    double VoronoiRadius;
    Point3d VoronoiPoint;
    
    double minEdgeLength;
    
    /** triangele element
     * @param points0
     * @param points1
     * @param points2 */
    public Triangle(Point3d points0,Point3d points1,Point3d points2) {
        points = new Point3d[3];
        points[0] = points0;
        points[1] = points1;
        points[2] = points2;
        
        area = (
                - points0.x * points2.y
                - points1.x * points0.y
                - points2.x * points1.y
                + points0.x * points1.y
                + points1.x * points2.y
                + points2.x * points0.y
                ) / 2.0;
        
        double c=Math.sqrt(Math.pow(points[0].x-points[1].x,2)+Math.pow(points[0].y-points[1].y,2));
        double b=Math.sqrt(Math.pow(points[1].x-points[2].x,2)+Math.pow(points[1].y-points[2].y,2));
        double a=Math.sqrt(Math.pow(points[2].x-points[0].x,2)+Math.pow(points[2].y-points[0].y,2));
        double s=0.5*(a+b+c);
        double z=s*(s-a)*(s-b)*(s-c);
        
        double w= sign((a*a+b*b-c*c)/a*b) * Math.sqrt(Math.abs(z*z/c*c-0.25));
        double x=0.5*(points[1].x-points[0].x)-w*(points[1].y-points[0].y)+points[0].x;
        double y=0.5*(points[1].y-points[0].y)-w*(points[1].x-points[0].x)+points[0].y;
        
        VoronoiRadius=0.25*(a*b*c)/Math.sqrt(z);
        VoronoiPoint = new Point3d(x,y,0.);
        
        minEdgeLength=points[0].distance(points[1]);
        minEdgeLength=Math.min(minEdgeLength,points[1].distance(points[2]));
        minEdgeLength=Math.min(minEdgeLength,points[2].distance(points[0]));
        
    }
    
    public Vector3f getNormal(){
        Vector3f normal = new Vector3f();
        
        normal.x = (float)((points[0].y - points[1].y) * (points[0].z + points[1].z) +
                (points[1].y - points[2].y) * (points[1].z + points[2].z) +
                (points[2].y - points[0].y) * (points[2].z + points[0].z));
        
        normal.y = (float)((points[0].z - points[1].z) * (points[0].x + points[1].x) +
                (points[1].z - points[2].z) * (points[1].x + points[2].x) +
                (points[2].z - points[0].z) * (points[2].x + points[0].x));
        
        normal.z = (float)((points[0].x - points[1].x) * (points[0].y + points[1].y) +
                (points[1].x - points[2].x) * (points[1].y + points[2].y) +
                (points[2].x - points[0].x) * (points[2].y + points[0].y));
        
        float l = normal.length();
        
        normal.x /= l;
        normal.y /= l;
        normal.z /= l;
        
        return normal;
    }
    
    @Override
    public boolean contains(Point3d p) {
        if(p==null)
            return false;
        
        double a=this.getArea();
        double[] erg=new double[3];
        //erste Teilflaeche
        erg[0]=points[1].x*(points[2].y-p.y)+points[2].x*(p.y-points[1].y)+p.x*(points[1].y-points[2].y);
        erg[0]/=2.*a;
        if (erg[0]<=-epsilon)
            return false;
        //zweite Teilflaeche
        erg[1]=points[2].x*(points[0].y-p.y)+points[0].x*(p.y-points[2].y)+p.x*(points[2].y-points[0].y);
        erg[1]/=2.*a;
        if (erg[1]<=-epsilon)
            return false;
        //dritte Teilflaeche
        erg[2]=points[0].x*(points[1].y-p.y)+points[1].x*(p.y-points[0].y)+p.x*(points[0].y-points[1].y);
        erg[2]/=2.*a;
        
        return erg[2] > -epsilon;
    }
    @Override
    public double[] getNaturefromCart(Point3d p) {
        if(p==null)
            return null;

        double a=this.getArea();
        double[] erg=new double[3];
        //erste Teilflaeche
        erg[0]=points[1].x*(points[2].y-p.y)+points[2].x*(p.y-points[1].y)+p.x*(points[1].y-points[2].y);
        erg[0]/=2.*a;
        if (erg[0]<=-epsilon)
            return null;
        //zweite Teilflaeche
        erg[1]=points[2].x*(points[0].y-p.y)+points[0].x*(p.y-points[2].y)+p.x*(points[2].y-points[0].y);
        erg[1]/=2.*a;
        if (erg[1]<=-epsilon)
            return null;
        //dritte Teilflaeche
        erg[2]=points[0].x*(points[1].y-p.y)+points[1].x*(p.y-points[0].y)+p.x*(points[0].y-points[1].y);
        erg[2]/=2.*a;
        if (erg[2]<=-epsilon)
            return null;
        
        if(erg[0]<=epsilon)
            erg[0]=0;
        else if(erg[0]>=1-epsilon)
            erg[0]=1;
        
        if(erg[1]<=epsilon)
            erg[1]=0;
        else if(erg[1]>=1-epsilon)
            erg[1]=1;
        
        if(erg[2]<=epsilon)
            erg[2]=0;
        else if(erg[2]>=1-epsilon)
            erg[2]=1;
        
        return erg;
    }
    
    public double getArea(){
        if(area <=0.00001){
            area = (
                - points[0].x * points[2].y
                - points[1].x * points[0].y
                - points[2].x * points[1].y
                + points[0].x * points[1].y
                + points[1].x * points[2].y
                + points[2].x * points[0].y
                ) / 2.0;
        }
        return area;
    }
    public boolean inDelaunayCircle(Point3d p){
        double distance2d=Math.sqrt(Math.pow(VoronoiPoint.x-p.x,2)+Math.pow(VoronoiPoint.y-p.y,2));
        return (distance2d < VoronoiRadius);
    }
    double getVoronoiRadius(){
        return VoronoiRadius;
    }
    
    Point3d getVoronoiPoint(){
        return VoronoiPoint;
    }
    
    /** Elementsize connecting to a vector
     * @param vx
     * @param vy
     * @return  */
    public double VectorSize(double vx,double vy){
        double dl=0.;
        int i=0;
        
        double normV = Function.norm(vx,vy);
        
        if (normV >= 0.001) {
            do{
                final int i1=(0+i)%3;
                final int i2=(1+i)%3;
                final int i3=(2+i)%3;
                
                double p1x = points[i1].x+vx;
		double p1y = points[i1].y+vy;
                
                double s = (points[i1].x * (points[i2].y - points[i3].y) + points[i2].x * (points[i3].y - points[i1].y) +
                		points[i3].x * (points[i1].y - points[i2].y)) / (points[i1].x * (points[i2].y - points[i3].y) +
				p1x * (points[i3].y - points[i2].y) + points[i2].x * (p1y - points[i1].y) +
				points[i3].x * (points[i1].y - p1y));
                double t = (points[i1].x * (p1y - points[i2].y) + p1x * (points[i2].y - points[i1].y) +
                		points[i2].x * (points[i1].y - p1y)) / (points[i1].x * (points[i3].y - points[i2].y) +
				p1x * (points[i2].y - points[i3].y) + points[i2].x * (points[i1].y - p1y) +
				points[i3].x * (p1y - points[i1].y));
                
                if (t<=1 && t>=0) // geschnitten
                    dl = Math.abs(s) * normV;
                
                i++;
            } while( ( i <= 3) && (dl == 0.) );
            
            if (dl == 0.) {
		System.out.println("error in element size computation");
                return minEdgeLength;
            }
        } else {
            return minEdgeLength;
        }
        return(dl);
    }
    
    private double sign(double a){
        if(a>0.)
            return 1.;
        else if(a<0.)
            return -1.;
        else
            return 0.;
    }

    public static void main(String[] args) {
        Point3d p1 = new Point3d(150,300,5);
        Point3d p2 = new Point3d(400,280,7);
        Point3d p3 = new Point3d(280,500,9);
        
        Triangle t = new Triangle(p1, p2, p3);
        System.out.println(t.getVoronoiPoint());
        System.out.println(t.getVoronoiRadius());
//        System.out.println(t.inDelaunayCircle(new Point2d(200., 300.)));
//
//        System.out.println(t.getBarycentre());
//
//        System.out.println(t.contains(t.getBarycentre()));
//        System.out.println(t.contains(p3));
//        double[] koord = t.getNaturefromCart(p3);
//        System.out.println(koord[0]); System.out.println(koord[1]); System.out.println(koord[2]);
    }
    
}
