package de.smile.geom;
/** describe the propreties of an element in a metric space 
 * @author milbradt
 * @param <E>
 */
public interface MetricPoint<E extends MetricPoint<?>>{
	public double distance(E y);
}
