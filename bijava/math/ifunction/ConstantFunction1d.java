package bijava.math.ifunction;

public class ConstantFunction1d implements ScalarFunction1d {
    private double value=0;
    
    public ConstantFunction1d(double value){
    	this.value=value;
    }

    @Override
    public double getValue(double t) {
	return value;
    }
	
    @Override
    public double getDifferential(double t) {
	return 0.;
    }

    @Override
    public void setPeriodic(boolean b){}
    
    @Override
    public boolean isPeriodic(){
	return true;
    }
}
