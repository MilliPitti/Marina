package bijava.math.ifunction;

public class ZeroFunction1d implements ScalarFunction1d {

    @Override
    public double getValue(double t) {
	return 0.;
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
