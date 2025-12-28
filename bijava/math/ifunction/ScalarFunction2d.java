package bijava.math.ifunction;

import java.awt.geom.*;

public interface ScalarFunction2d {
	public double getValue (Point2D.Double p);
	public double[] getDifferential (Point2D.Double p);
}
