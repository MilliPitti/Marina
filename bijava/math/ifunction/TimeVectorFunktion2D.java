package bijava.math.ifunction;
import java.awt.geom.*;

public class TimeVectorFunktion2D {
	double[] time;
	VectorFunction2d[] vectorFunction2d;
	
	public TimeVectorFunktion2D (double[] time, VectorFunction2d[] vectorFunction2d) {
		this.time = time;
		this.vectorFunction2d = vectorFunction2d;
	}
	
	public double[] getValue(double t, Point2D.Double p) {
		
		int t1,t2;
		double ta,te;
		double wert_a[], wert_e[];
		
		for (t1 = 0; t1 < time.length;t1++) {
			if (t < time[t1]) {
				t1 -= 1;
				break;
			}
		}
		
		for (t2 = t1; t2 < time.length;t2++) {
			if (t < time[t2]) {
				t2 -= 1;
				break;
			}
		}
		
		ta = time[t1];
		te = time[t2];
		
		wert_a = vectorFunction2d[t1].getValue(p);
		wert_e = vectorFunction2d[t2].getValue(p);
		
		double[] y = new double[wert_a.length];

		for (int i = 0;i<wert_a.length;i++) {
			if (ta != te)
				y[i] = wert_a[i] + ((t-ta)/(te-ta))*(wert_e[i]-wert_a[i]);	
			else
				y[i] = wert_a[i];
		}
		
		return y;
	}
}
	
				