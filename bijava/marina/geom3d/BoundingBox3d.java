package bijava.marina.geom3d;

import java.util.Objects;
import javax.vecmath.Point3d;

/**
 * BoundingBox3d.java provides methods for a threedimensional bounding box.
 * @author Peter Milbradt
 */
public class BoundingBox3d implements Cloneable {
    
   
    protected Point3d pmin; // the point left down of the bounding box
    protected double width, height, length; // width and height of the bounding box
 
      
    /**
     * Creates a threedimensional bounding box.
    * If the width, length or the height is less than zero, the method throws an IllegalArgumentException.
     * @param pmin point with minimal coordinates.
     * @param width width.
     * @param length length
     *@param height height.
     */
    public BoundingBox3d(Point3d pmin, double width, double length, double height) {
        if (width < 0.0) throw new IllegalArgumentException("BoundingBox2d: width < 0");
        if (height < 0.0) throw new IllegalArgumentException("BoundingBox2d: height < 0");
        if (length < 0.0) throw new IllegalArgumentException("BoundingBox2d: height < 0");
    	    	
        this.pmin = pmin;
        this.width = width;
        this.height = height;
        this.length=length;
    }
     
      /**
     * Creates a twodimensional bounding box.
     * If the width, length or the height is less than zero, the method throws an IllegalArgumentException.
     * @param pmin point with minimal coordinates.
     * @param pmax point with maximal coordinates.
      */
    public BoundingBox3d(Point3d pmin, Point3d pmax) {
        if (pmin.x > pmax.x) throw new IllegalArgumentException("BoundingBox2d: width < 0");
        if (pmin.y > pmax.y) throw new IllegalArgumentException("BoundingBox2d: length < 0");
    	if (pmin.z > pmax.z) throw new IllegalArgumentException("BoundingBox2d: height < 0");    	
        
        this.pmin = pmin;
        this.width = pmax.x-pmin.x;
        this.height =pmax.z-pmin.z;
        this.length =pmax.y-pmin.y;
        
    }
      
    
      /**
     * Creates a threedimensional bounding box.
     * @param p array of points.
     */
    public BoundingBox3d(Point3d[] p) {
        
      double xmin, xmax,ymin,ymax, zmin,zmax;
      xmin=p[0].x;
      xmax=p[0].x;
      ymin=p[0].y;
      ymax=p[0].y;
      zmin=p[0].z;
      zmax=p[0].z;
      
      for(int i=1; i<p.length;i++){
          if (p[i].x < xmin) xmin=p[i].x;
          if (p[i].x > xmax) xmax=p[i].x;
          if (p[i].y < ymin) ymin=p[i].y;
          if (p[i].y > ymax) ymax=p[i].y;
          if (p[i].z < zmin) zmin=p[i].z;
          if (p[i].z > zmax) zmax=p[i].z;
      }
      
      Point3d p1 = new Point3d(xmin,ymin,zmin);
      pmin=p1;
      width = xmax-xmin;
      height =zmax-zmin;  
      length =ymax-ymin;  
    }
        
    /**
     * Gets the point pmin  
     * @return the point pmin
     */
    public Point3d getP0() { return pmin; }
    
    /**
     * Gets the point p1 
     * @return the point p1
     */
    public Point3d getP1() { return new Point3d(getXmax(), getYmin(),getZmin());}
    
    /**
     * Gets the point p2 
     * @return the point p2
     */
    public Point3d getP2() { return new Point3d(getXmax(), getYmax(),getZmin());}
    
    /**
     * Gets the point p3 
     * @return the point p3
     */
    public Point3d getP3() { return new Point3d(getXmin(), getYmax(),getZmin());}
    
    /**
     * Gets the point p4  
     * @return the point p4
     */
    public Point3d getP4() { return new Point3d(getXmin(), getYmin(),getZmax());}
    
    /**
     * Gets the point p5  
     * @return the point p5
     */
    public Point3d getP5() { return new Point3d(getXmax(), getYmin(),getZmax());}
    
    /**
     * Gets the point p6  
     * @return the point p6
     */
    public Point3d getP6() { return new Point3d(getXmax(), getYmax(),getZmax());}
    
    /**
     * Gets the point p7 
     * @return the point p7
     */
    public Point3d getP7() { return new Point3d(getXmin(), getYmax(),getZmax());}
 
       
    
    /**
     * Get the width of the box.
     * @return 
     */   
    public double getWidth() { return width;}
 
    /**
     * Get the height of the box.
     * @return 
     */
    public double getHeight() { return height;}
    
    /**
     * Get the length of the box.
     * @return 
     */
    public double getLength() { return length;}
    
    /**
     * Gets the minimal x-coordinate.
     *
     * @return the minimal x-coordinate of this bounding box.
     */
    public double getXmin() {
        return pmin.x;
    }    
    /**
     * Gets the maximal x-coordinate.
     *
     * @return the maximal x-coordinate of this bounding box.
     */
    public double getXmax() {
        return pmin.x + width;
    }
    
    /**
     * Gets the minimal y-coordinate.
     *
     * @return the minimal y-coordinate of this bounding box.
     */
    public double getYmin() {
        return pmin.y;
    }
    
    /**
     * Gets the maximal y-coordinate.
     *
     * @return the maximal y-coordinate of this bounding box.
     */
    public double getYmax() {
        return pmin.y + length;
    }        
    /**
     * Gets the minimal z-coordinate.
     *
     * @return the minimal z-coordinate of this bounding box.
     */
    public double getZmin() {
        return pmin.z;
    }   
    /**
     * Gets the maximal z-coordinate.
     *
     * @return the maximal z-coordinate of this bounding box.
     */
    public double getZmax() {
        return pmin.z + height;
    }   
    
     /**
     * Sets the width of the box.
     * @param width
     */  
    public void setWidth(double width) { this.width = width;}
    
    /**
    * Sets the height of the box.
     * @param height
    */  
    public void setHeight(double height) {
    	this.height = height;
    }
   
    /**
     * Sets the length of the box.
     * @param length
     */  
    public void setLength(double length) {
    	this.length = length;
    }  
    
    /**
     * Sets the minimal x-coordinate.
     * The width of the box is modified correspondingly.
     * 
     * @param xmin
     */
    public void setXmin(double xmin) {    	
    	double xmax = pmin.x + width;    
    	if (xmax - xmin < 0) throw new IllegalArgumentException("invalid width: xmin=" + xmin + ", "+ this);
    	pmin.x=xmin;
    	width = xmax - pmin.x; 
    }      
    /**
     * Sets the maximal x-coordinate.
     * The width of the box is modified correspondingly.
     * 
     * @param xmax
     */
    public void setXmax(double xmax) {
    	if (xmax - pmin.x < 0) throw new IllegalArgumentException("invalid width");
    	width = xmax - pmin.x;     	
    }   
    /**
     * Sets the minimal y-coordinate.
     * The length of the box is modified correspondingly.
     * 
     * @param ymin
     */
    public void setYmin(double ymin) {
    	double ymax = pmin.y + length;
    	if (ymax - ymin < 0) throw new IllegalArgumentException("invalid length");
    	pmin.y=ymin;
    	length = ymax - pmin.y;
    }   
    /**
     * Sets the maximal y-coordinate.
     * The length of the box is modified correspondingly.
     * 
     * @param ymax
     */
    public void setYmax(double ymax) {    	
    	if (ymax - pmin.y < 0) throw new IllegalArgumentException("invalid length");
    	length = ymax - pmin.y;     	
    }
     /**
     * Sets the minimal z-coordinate.
     * The height of the box is modified correspondingly.
     * 
     * @param zmin
     */
    public void setZmin(double zmin) {
    	double zmax = pmin.z + height;
    	if (zmax - zmin < 0) throw new IllegalArgumentException("invalid height");
    	pmin.z=zmin;
    	height = zmax - pmin.z;
    }   
    
     /**
     * Sets the maximal z-coordinate.
     * The height of the box is modified correspondingly.
     * 
     * @param zmax
     */
    public void setZmax(double zmax) {    	
    	if (zmax - pmin.z < 0) throw new IllegalArgumentException("invalid height");
    	height = zmax - pmin.z;     	
    }
     
   /**
     * Sets and validates the dimensions of this bounding box.
     * @param pmin point with minimal coordinates.
     * @param width width.
    *  @param length length.
     * @param height height.
     */
    public void set(Point3d pmin, double width, double length, double height) {    
    	this.pmin = pmin;
        this.width = width;
        this.height = height;
        this.length=length;
    }
    
     /**
     * Gets the volume of this bounding box.
     * @return volume of this bounding box.
     */
    public double getVolume(){
     return width*height*length; 
    }
    
     /**
     * Gets the centroid of this bounding box.
     * @return centroid of this bounding box.
     */
    public Point3d getCentroid() {
        return new Point3d(pmin.x + width / 2., pmin.y + length / 2., pmin.z + height / 2.);
    }
    
     
    
    /**
     * Tests if this bounding box contains a point.
     * @param x x-coordinate of the point.
     * @param y y-coordinate of the point.
     * @param z z-coordinate of the point.
     * @return <code>true</code> if this bounding box contains the point.
     */
    public boolean contains(double x, double y, double z) {        
        return (x >= getXmin() && x <= getXmax() &&
                y >= getYmin() && y <= getYmax()&&
                z >= getZmin() && z <= getZmax());
    }
    

    /**
     * Tests if this bounding box contains a point.
     * @param p threedimensional point.
     * @return <code>true</code> if this bounding box contains the point.
     */
    public boolean contains(Point3d p) {
        if (p == null)
            return false;        
        return this.contains(p.x, p.y, p.z);
    }
    
    
     /**
     * Tests whether the given bounding box interferes with this bounding box.
     * @param b threedimensional bounding box.
     * @return <code>true</code> if the given bounding box interferes with this bounding box.
     */
    public boolean interferes(BoundingBox3d b) {
        if (b == this) return true;
        if (b == null) return false;
        // tests the distance of the center points
        Point3d this_c = this.getCentroid(), b_c = b.getCentroid();
        double dx = Math.abs(b_c.x - this_c.x), dy = Math.abs(b_c.y - this_c.y), dz = Math.abs(b_c.z - this_c.z);
        double width_mid = (width + b.width) / 2., length_mid = (length + b.length) / 2., height_mid = (height + b.height) / 2.;
        return dx < width_mid && dy < length_mid && dz < height_mid;
    }
  
   
      /**
     * Gets the union of this bounding box with an other bounding box.
     * @param b threedimensional bounding box.
     * qreturn union of this bounding box with <code>other</code>.
     * @return 
     */
    public BoundingBox3d union(BoundingBox3d b) {
           
        if (b == this || b == null) return this.clone();
        double xmin = pmin.x < b.pmin.x ? pmin.x : b.pmin.x;
        double ymin = pmin.y < b.pmin.y ? pmin.y : b.pmin.y;
        double zmin = pmin.z < b.pmin.z ? pmin.z : b.pmin.z;
        double xmax = this.getXmax() < b.getXmax() ? b.getXmax() : this.getXmax();
        double ymax = this.getYmax() < b.getYmax() ? b.getYmax() : this.getYmax();
        double zmax = this.getZmax() < b.getZmax() ? b.getZmax() : this.getZmax();
         
        return new BoundingBox3d(new Point3d(xmin, ymin, zmin), (xmax - xmin), (ymax - ymin), (zmax - zmin));
    }
    
    
     /**
     * Gets the intersection of this bounding box with an other bounding box.
     * @param b threedimensional bounding box.
     * qreturn intersection of this bounding box with <code>other</code>.
     * @return 
     */
    
    public BoundingBox3d intersection(BoundingBox3d b) {
        if (b == this) return this.clone();
        if (b == null) return null;
        
        BoundingBox3d result = null;
        
        if (this.interferes(b)) {
            double xmin = pmin.x > b.pmin.x ? pmin.x : b.pmin.x;
            double ymin = pmin.y > b.pmin.y ? pmin.y : b.pmin.y;
            double zmin = pmin.z > b.pmin.z ? pmin.z : b.pmin.z;
            double xmax = this.getXmax() > b.getXmax() ? b.getXmax() : this.getXmax();
            double ymax = this.getYmax() > b.getYmax() ? b.getYmax() : this.getYmax();
            double zmax = this.getZmax() > b.getZmax() ? b.getZmax() : this.getZmax();
            
            result = new BoundingBox3d(new Point3d(xmin, ymin, zmin), (xmax - xmin), (ymax - ymin), (zmax - zmin));
        }
        
        return result;
    }
    
      /**
     * Tests this bounding box on equality with other object.
     * @param o object.
     * @return <code>true</code> if this bounding box is equal with other object.
     */
    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        return (this.equals((BoundingBox3d) o));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.pmin);
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.width) ^ (Double.doubleToLongBits(this.width) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.height) ^ (Double.doubleToLongBits(this.height) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.length) ^ (Double.doubleToLongBits(this.length) >>> 32));
        return hash;
    }
     /**
     * Tests this bounding box on equality with other bounding box.
     * @param b threedimensional bounding box.
     * @return <code>true</code> if this bounding box is equal with other bounding box.
     */
    public boolean equals(BoundingBox3d b) {
        if (b == this) return true;
        if (b == null) return false;
        return this.pmin.equals(b.pmin) && this.width == b.width  && this.length == b.length && this.height == b.height;
    }
      
     /**
     * Gets the clone of this bounding box
     * @return copy of this bounding box 
     */       
    @Override
    public BoundingBox3d clone(){
       Point3d p = new Point3d(pmin); 
       return new BoundingBox3d(p, width, length, height);
    }
    
    
    @Override
    public String toString() {
    	return "xmin=" + getXmin() + ", ymin=" + getYmin()+ ", zmin=" + getZmin() 
         + ",\nxmax=" + getXmax() + ", ymax=" + getYmax() + ", zmax=" + getZmax()  
         + ",\nwidth=" + width + ", length=" + length + ", height=" + height;
    }
    
  
}
