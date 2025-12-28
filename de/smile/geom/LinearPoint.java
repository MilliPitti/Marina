package de.smile.geom;

/** describe a point of a linear space
 *
 * @author milbradt
 * @param <E>
 */
public interface LinearPoint<E extends LinearPoint<?>> {

    public E add(E point);

    public E sub(E point);

    public E mult(double scalar); // gibt einen neuen LinearPoint zurueck
    
}
