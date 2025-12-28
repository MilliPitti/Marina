package bijava.math.ifunction;

import java.awt.geom.*;
public class ConstantFunction2d implements ScalarFunction2d {
    private double value=0;
    
    public ConstantFunction2d(double value){
    	this.value=value;
    }

    @Override
    public double getValue(Point2D.Double p) {
	return value;
    }
	
    @Override
    public double[] getDifferential(Point2D.Double p) {
    	double[] v=new double[2];
    	v[0]=0.; v[1]=0.;
	return v;
    }

    public void setPeriodic(boolean b){}
    
    public boolean isPeriodic(){
	return true;
    }
}
