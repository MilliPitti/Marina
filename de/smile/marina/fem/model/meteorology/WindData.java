package de.smile.marina.fem.model.meteorology;
import javax.vecmath.*;
public interface WindData {
	double[] getValue (Point3d p,double t);
}
