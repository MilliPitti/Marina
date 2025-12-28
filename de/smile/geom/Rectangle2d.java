package de.smile.geom;

import javax.vecmath.Point2d;

/**
Klasse fuer ein Rechteck mit double-Werten.

@author Dipl.-Ing. C. Lippert
@version 1.0	21.8.00
*/

public class Rectangle2d
{
   /**
     * The bitmask that indicates that a point lies to the left of
     * this <code>Rectangle2D</code>.
     * @since 1.2
     */
    public static final int OUT_LEFT = 1;

    /**
     * The bitmask that indicates that a point lies above
     * this <code>Rectangle2D</code>.
     * @since 1.2
     */
    public static final int OUT_TOP = 2;

    /**
     * The bitmask that indicates that a point lies to the right of
     * this <code>Rectangle2D</code>.
     * @since 1.2
     */
    public static final int OUT_RIGHT = 4;

    /**
     * The bitmask that indicates that a point lies below
     * this <code>Rectangle2D</code>.
     * @since 1.2
     */
    public static final int OUT_BOTTOM = 8;


	public double x;
	public double y;
	public double width;
	public double height;

	public Rectangle2d (double x, double y, double width, double height)
	{
		this.x=x;
		this.y=y;
		this.width=width;
		this.height=height;
	}

	public final double getX()
	{
		return x;
	}

	public final double getY()
	{
		return y;
	}

	public final double getMinX()
	{
		return x;
	}

	public final double getMinY()
	{
		return y;
	}

	public final double getMaxX()
	{
		return (x+width);
	}

	public final double getMaxY()
	{
		return (y+height);
	}

    public boolean equalsEps(de.smile.geom.Rectangle2d r, double eps)
	{
		if (Math.abs(getX()-r.getX())>eps)
			return false;
		if (Math.abs(getY()-r.getY())>eps)
			return false;
		if (Math.abs(getMaxX()-r.getMaxX())>eps)
			return false;
		if (Math.abs(getMaxY()-r.getMaxY())>eps)
			return false;
		return true;
	}

	public final Point2d getCenterOfGravity()
	{
		return new Point2d(x+0.5*width,y+0.5*height);
	}
        
	public final boolean contains( double x, double y )
	{
		return inside( x,y );
	}

	public de.smile.geom.Rectangle2d getBounds()
	{
		return new de.smile.geom.Rectangle2d (x,y,width,height);
	}

	// aus Rectangle2D
	public int outcode(double x, double y) {
		int out = 0;
		if (this.width <= 0) {
			out |= OUT_LEFT | OUT_RIGHT;
		} else if (x < this.x) {
			out |= OUT_LEFT;
		} else if (x > this.x + this.width) {
			out |= OUT_RIGHT;
		}
		if (this.height <= 0) {
			out |= OUT_TOP | OUT_BOTTOM;
		} else if (y < this.y) {
			out |= OUT_TOP;
		} else if (y > this.y + this.height) {
			out |= OUT_BOTTOM;
		}
		return out;
	}

	public final double getArea()
	{
		return (width*height);
	}

	public final boolean inside(Point2d p)
	{
		double px=p.getX();
		double py=p.getY();
		return ( px>=x && px<=x+width && py>=y && py<=y+height);
	}

	public final boolean insideDoubleBounds(Point2d p)
	{
		double max=Math.max(width,height);
		double px=p.getX();
		double py=p.getY();
		return ( px>=(x-max) && px<=(x+width+max) && py>=(y-max) && py<=(y+height+max));
	}

	public final boolean inside(double px, double py)
	{
		return ( px>=x && px<=x+width && py>=y && py<=y+height);
	}
    
	public final boolean insideExtendedBounds(double px, double py, double add_dx, double add_dy)
	{
		return ( px>=(x-add_dx) && px<=(x+width+add_dx) && py>=(y-add_dy) && py<=(y+height+add_dy));
	}		

	public final boolean inside(double px)
	{
		return ( px>=x && px<=x+width);
	}

	public boolean inside(Rectangle2d rect)
	{
		if (rect==null)
			return false;
		return (rect.x>=x && rect.x+rect.width<=x+width && rect.y>=y && rect.y+rect.height<=y+height);
	}

	public boolean outside(Rectangle2d rect)
	{
		if (rect==null)
			return false;

		// neu 11.1.03
		double xmin=getMinX();
		double rect_xmax=rect.getMaxX();
		// 1.Test liegt rect vollstaendig links von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmax<xmin)
			return true;

		double xmax=getMaxX();
		double rect_xmin=rect.getMinX();		
		// 2.Test liegt rect vollstaendig rechts von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmin>xmax)
			return true;

		double ymin=getMinY();
		double rect_ymax=rect.getMaxY();
		// 3.Test liegt rect vollstaendig oben von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymax<ymin)
			return true;

		double ymax=getMaxY();
		double rect_ymin=rect.getMinY();		
		// 4.Test liegt rect vollst�ndig unten von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymin>ymax)
			return true;

		return false;
	}

	public boolean intersects( Rectangle2d rect )
	{
		if (rect==null)
			return false;

		// neu 11.1.03
		double xmin=getMinX();
		double rect_xmax=rect.getMaxX();
		// 1.Test liegt rect vollstaendig links von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmax<xmin)
			return false;

		double xmax=getMaxX();
		double rect_xmin=rect.getMinX();		
		// 2.Test liegt rect vollstaendig rechts von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmin>xmax)
			return false;

		double ymin=getMinY();
		double rect_ymax=rect.getMaxY();
		// 3.Test liegt rect vollstaendig oben von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymax<ymin)
			return false;

		double ymax=getMaxY();
		double rect_ymin=rect.getMinY();		
		// 4.Test liegt rect vollstaendig unten von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymin>ymax)
			return false;

		return true;
	}

	public final boolean intersects( double x0, double y0, double w, double h )
	{
		// neu 11.1.03
		double xmin=getMinX();
		double rect_xmax=x0+w;
		// 1.Test liegt rect vollstaendig links von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmax<xmin)
			return false;

		double xmax=getMaxX();
		double rect_xmin=x0;	
		// 2.Test liegt rect vollstaendig rechts von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmin>xmax)
			return false;

		double ymin=getMinY();
		double rect_ymax=y0+h;
		// 3.Test liegt rect vollstaendig oben von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymax<ymin)
			return false;

		double ymax=getMaxY();
		double rect_ymin=y0;	
		// 4.Test liegt rect vollstaendig unten von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymin>ymax)
			return false;

		return true;
	}

	public boolean intersects( Rectangle2d rect, double min_rel_intersection_area )
	{
		if (rect==null)
			return false;

		// neu 11.1.03
		double xmin=getMinX();
		double rect_xmax=rect.getMaxX();
		// 1.Test liegt rect vollstaendig links von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmax<xmin)
			return false;

		double xmax=getMaxX();
		double rect_xmin=rect.getMinX();		
		// 2.Test liegt rect vollstaendig rechts von diesem Rectangle2d, dann kein Schnitt
		if (rect_xmin>xmax)
			return false;

		double ymin=getMinY();
		double rect_ymax=rect.getMaxY();
		// 3.Test liegt rect vollstaendig oben von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymax<ymin)
			return false;

		double ymax=getMaxY();
		double rect_ymin=rect.getMinY();		
		// 4.Test liegt rect vollstaendig unten von diesem Rectangle2d, dann kein Schnitt
		if (rect_ymin>ymax)
			return false;

		double tx1 = x;
		double ty1 = y;
		double rx1 = rect.x;
		double ry1 = rect.y;
		double tx2 = tx1; tx2 += width;
		double ty2 = ty1; ty2 += height;
		double rx2 = rx1; rx2 += rect.width;
		double ry2 = ry1; ry2 += rect.height;
		if (tx1 < rx1) tx1 = rx1;
		if (ty1 < ry1) ty1 = ry1;
		if (tx2 > rx2) tx2 = rx2;
		if (ty2 > ry2) ty2 = ry2;
		tx2 -= tx1;
		ty2 -= ty1;

		double area=(width*height);
		double area_intersection=(tx2*ty2);

		/*System.out.println("1 "+this);
		System.out.println("2 "+rect);
		System.out.println("3 "+area+" "+area_intersection);
		System.out.println("4 "+((area_intersection/area)>=min_rel_intersection_area));
		*/

		return ((area_intersection/area)>=min_rel_intersection_area);
	}

	public static double center_distance( Rectangle2d rect1, Rectangle2d rect2 )
	{
		if (rect1==null || rect2==null)
			return Double.NaN;

		Point2d c1=rect1.getCenterOfGravity();	
		Point2d c2=rect2.getCenterOfGravity();

		return c1.distance(c2);
	}

	public static Rectangle2d intersection( Rectangle2d rect1, Rectangle2d rect2 )
	{
		if (rect1==null || rect2==null)
			return null;

		if (rect1.inside(rect2))
			return rect2;
		if (rect2.inside(rect1))
			return rect1;
		if (!rect1.intersects(rect2))
			return null;

		double tx1 = rect1.x;
		double ty1 = rect1.y;
		double rx1 = rect2.x;
		double ry1 = rect2.y;
		double tx2 = tx1; tx2 += rect1.width;
		double ty2 = ty1; ty2 += rect1.height;
		double rx2 = rx1; rx2 += rect2.width;
		double ry2 = ry1; ry2 += rect2.height;
		if (tx1 < rx1) tx1 = rx1;
		if (ty1 < ry1) ty1 = ry1;
		if (tx2 > rx2) tx2 = rx2;
		if (ty2 > ry2) ty2 = ry2;
		tx2 -= tx1;
		ty2 -= ty1;
		return new Rectangle2d(tx1, ty1,tx2,ty2);
	}

	public static de.smile.geom.Rectangle2d getBounds(Point2d p0, Point2d p1)
	{
		double minx=p0.getX();
		double miny=p0.getY();
		double maxx=p0.getX();
		double maxy=p0.getY();
		if (p1.getX()<minx)
			minx=p1.getX();
		if (p1.getX()>maxx)
			maxx=p1.getX();
		if (p1.getY()<miny)
			miny=p1.getY();
		if (p1.getY()>maxy)
			maxy=p1.getY();

		return new de.smile.geom.Rectangle2d(minx,miny,(maxx-minx),(maxy-miny));
	}

	public de.smile.geom.Rectangle2d getBounds(double faktor)
	{
		double xmed = x+0.5*width;
		double ymed = y+0.5*height;
		double width_fak=width*faktor;
		double height_fak=height*faktor;
		return new de.smile.geom.Rectangle2d(xmed-0.5*width_fak,ymed-0.5*height_fak,width_fak,height_fak); 
	}

	public de.smile.geom.Rectangle2d getBounds(double add_width, double add_height)
	{
		double xmin=x-add_width;
		double xmax=x+width+add_width;
		double ymin=y-add_height;
		double ymax=y+height+add_height;
		de.smile.geom.Rectangle2d bounds=new de.smile.geom.Rectangle2d(xmin,ymin,(xmax-xmin),(ymax-ymin));
		return bounds;
	}

	public double getMaxOrthoDistance(Point2d p)
	{
		return Math.max(Math.max(Math.abs(p.getX()-x),Math.abs(p.getX()-(x+width))),Math.max(Math.abs(p.getY()-y),Math.abs(p.getY()-(y+height))));
	}

	public static de.smile.geom.Rectangle2d getBounds(de.smile.geom.Rectangle2d bounds1,de.smile.geom.Rectangle2d bounds2)
	{
		if (bounds1==null)
			return bounds2;
		if (bounds2==null)
			return bounds1;
		double xmin=Math.min(bounds1.x,bounds2.x);
		double xmax=Math.max(bounds1.x+bounds1.width,bounds2.x+bounds2.width);
		double ymin=Math.min(bounds1.y,bounds2.y);
		double ymax=Math.max(bounds1.y+bounds1.height,bounds2.y+bounds2.height);

		return new de.smile.geom.Rectangle2d(xmin,ymin,(xmax-xmin),(ymax-ymin));
	}

	public static Rectangle2d getBounds(de.smile.geom.Rectangle2d bounds1,Point2d p)
	{
		if (bounds1==null)
			return new de.smile.geom.Rectangle2d(p.getX(),p.getY(),0.0,0.0);
		if (p==null)
			return bounds1;
		double xmin=Math.min(bounds1.x,p.getX());
		double xmax=Math.max(bounds1.x+bounds1.width,p.getX());
		double ymin=Math.min(bounds1.y,p.getY());
		double ymax=Math.max(bounds1.y+bounds1.height,p.getY());

		return new de.smile.geom.Rectangle2d(xmin,ymin,(xmax-xmin),(ymax-ymin));
	}

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        
        final Rectangle2d other = (Rectangle2d) obj;
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        if (Double.doubleToLongBits(this.width) != Double.doubleToLongBits(other.width)) {
            return false;
        }
        if (Double.doubleToLongBits(this.height) != Double.doubleToLongBits(other.height)) {
            return false;
        }
        return true;
    }
        
        
}

