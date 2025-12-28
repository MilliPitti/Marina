package bijava.marina.geom3d;

import javax.vecmath.*;

public class Edge extends ConvexCell3d {
	double distance = 0.;
	
	public Edge(Point3d b, Point3d e){
		points = new Point3d[2];
		points[0]=b;
		points[1]=e;
		distance=b.distance(e);
	}

	public double distance(){
		return distance;
	}
	
        @Override
    public boolean contains(Point3d p)
    {
	if(p==null) return false;
		   
	double dx=points[1].x-points[0].x;
	double dy=points[1].y-points[0].y;
	double dz=points[1].z-points[0].z;	   
	
	double lambdaX=0, lambdaY=0, lambdaZ=0;
	if(Math.abs(dx)>=epsilon)
		lambdaX=(p.x-points[0].x)/dx;
	if(Math.abs(dy)>=epsilon)
		lambdaY=(p.y-points[0].y)/dy;
	if(Math.abs(dz)>=epsilon)
		lambdaZ=(p.z-points[0].z)/dz;
	
	//Fallunterscheidung wenn dX gleich null ist
	if(lambdaX<=-epsilon || lambdaX>=1+epsilon)
		return false;
	if(lambdaY<=-epsilon || lambdaY>=1+epsilon)
		return false;
	if(lambdaZ<=-epsilon || lambdaZ>=1+epsilon)
		return false;
		
	if(Math.abs(lambdaX-lambdaY)>=epsilon)
		return false;
        
	return Math.abs(lambdaX-lambdaZ) < epsilon;
    }
        @Override
    public double[] getNaturefromCart(Point3d p)
    {
	     if(p==null) return null;
		   
	double dx=points[1].x-points[0].x;
	double dy=points[1].y-points[0].y;
	double dz=points[1].z-points[0].z;	   
	
        double lambdaX=0, lambdaY=0, lambdaZ=0;
	if(Math.abs(dx)>=epsilon)
		lambdaX=(p.x-points[0].x)/dx;
	if(Math.abs(dy)>=epsilon)
		lambdaY=(p.y-points[0].y)/dy;
	if(Math.abs(dz)>=epsilon)
		lambdaZ=(p.z-points[0].z)/dz;
	
	//Fallunterscheidung wenn dX gleich null ist
	if(lambdaX<=-epsilon || lambdaX>=1+epsilon)
		return null;
	if(lambdaY<=-epsilon || lambdaY>=1+epsilon)
		return null;
	if(lambdaZ<=-epsilon || lambdaZ>=1+epsilon)
		return null;
		
	if(Math.abs(lambdaX-lambdaY)>=epsilon)
		return null;
	if(Math.abs(lambdaX-lambdaZ)>=epsilon)
		return null;
		
	double[] erg=new double[2];
	erg[0]=lambdaX;
	erg[1]=1-lambdaX;
        
	return erg;
    }
}
