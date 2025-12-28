/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2025

 * This file is part of Marina.

 * Marina is free software: you can redistribute it and/or modify              
 * it under the terms of the GNU Affero General Public License as               
 * published by the Free Software Foundation version 3.
 * 
 * Marina is distributed in the hope that it will be useful,                  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                
 * GNU Affero General Public License for more details.                          
 *                                                                              
 * You should have received a copy of the GNU Affero General Public License     
 * along with Marina.  If not, see <http://www.gnu.org/licenses/>.             
 *                                                                               
 * contact: milbradt@smileconsult.de                                        
 * smile consult GmbH                                                           
 * Schiffgraben 11                                                                 
 * 30159 Hannover, Germany 
 * 
 */
package bijava.marina.geom3d;

import de.smile.geom.Rectangle2d;
import java.io.Serializable;
import javax.vecmath.*;

//==========================================================================//
//  KLASSE ConvexCell3d                                                     //
//==========================================================================//
/**
 * Die Klasse <code>ConvexCell3d</code> beschreibt Objekte zur Darstellung
 * von konvexen Zellen im dreidimensionalen euklidischen Raum. Sie ist eine
 * abstrakte Basisklasse. Das heisst, es kann kein Objekt von
 * <code>ConvexCell3d</code> erzeugt werden.
 *
 * <p>
 * <strong>Version:</strong>
 * <br>
 * <dd>1.0, Februar 2000
 * <p>
 * <strong>Author:</strong>
 * <br>
 * <dd>Institut f&uuml;r Bauinformatik
 * <br>
 * <dd>Universit&auml;t Hannover
 * <br>
 * <dd>Dipl.-Ing. Martin Rose
 */
// ==========================================================================//
public abstract class ConvexCell3d implements Serializable {

  private static final long serialVersionUID = 1L;
  int dimension;
  Point3d[] points;
  protected double epsilon = 1e-6;

  // --------------------------------------------------------------------------//
  // DIMENSION LESEN //
  // --------------------------------------------------------------------------//
  /**
   * Liefert die Diemnsion der konvexen Zelle.
   * 
   * @return
   */
  // --------------------------------------------------------------------------//
  public int getDimension() {
    return dimension;
  }

  // --------------------------------------------------------------------------//
  // ANZAHL DER RANDpoints LESEN //
  // --------------------------------------------------------------------------//
  /**
   * Liefert die Anzahl der points der konvexen Hülle.
   * 
   * @return
   */
  // --------------------------------------------------------------------------//
  public int getPointSize() {
    return points.length;
  }

  // --------------------------------------------------------------------------//
  // RANDpoints LESEN //
  // --------------------------------------------------------------------------//
  /**
   * Liefert die points der konvexen Hülle.
   * 
   * @return
   */
  // --------------------------------------------------------------------------//
  public Point3d[] getPoints() {
    return points;
  }

  // --------------------------------------------------------------------------//
  // RANDPUNKT LESEN //
  // --------------------------------------------------------------------------//
  /**
   * Liefert einen Punkt der konvexen Hülle.
   * <p>
   * 
   * @param index Position des pointss im Punktfeld
   *              <p>
   * @return
   * @throws ArrayIndexOutOfBoundsException wenn der Index
   *                                        nicht im Bereiche der Punktanzahl
   *                                        liegt
   */
  // --------------------------------------------------------------------------//
  public Point3d getPoint(int index) throws ArrayIndexOutOfBoundsException {
    if (index < 0 || index >= points.length) {
      throw new ArrayIndexOutOfBoundsException();
    }

    return new Point3d(points[index]);
  }

  // --------------------------------------------------------------------------//
  // EPSILONUMGEBUNG HOLEN //
  // --------------------------------------------------------------------------//
  /**
   * Liefert die festgelegte Epsilon-Umgebung aller Zellpoints.
   * 
   * @return
   */
  // --------------------------------------------------------------------------//
  public double getEpsilon() {
    return epsilon;
  }

  // --------------------------------------------------------------------------//
  // EPSIONUMGEBUNG SETZEN //
  // --------------------------------------------------------------------------//
  /**
   * Setzt die Epsilon-Umgebung aller Zellpoints.
   *
   * @param epsilon Neuer Wert der Epsilon-Umgebung
   */
  // --------------------------------------------------------------------------//
  public void setEpsilon(double epsilon) {
    this.epsilon = epsilon;
  }

  // --------------------------------------------------------------------------//
  // ABSTAND EINES pointsS ZU EINER EBENE //
  // --------------------------------------------------------------------------//
  @SuppressWarnings("unused")
  private double distance(Point3d point, Point3d line1, Point3d line2) {
    Vector3d b = new Vector3d();
    b.sub(line2, line1);
    Vector3d c = new Vector3d();
    c.sub(point, line1);
    Vector3d kreuz = new Vector3d();
    kreuz.cross(b, c);

    return (kreuz.length() / b.length());
  }

  // --------------------------------------------------------------------------//
  // Bounding Box der konvexen Zelle //
  // --------------------------------------------------------------------------//
  public Rectangle2d getBounds() {
    double xmin = Double.MAX_VALUE;
    double ymin = Double.MAX_VALUE;
    double xmax = (-1) * Double.MAX_VALUE;
    double ymax = (-1) * Double.MAX_VALUE;

    // minimales, maximales x und y ermitteln
    for (Point3d point : points) {
      if (point.getX() < xmin)
        xmin = point.getX();
      if (point.getX() > xmax)
        xmax = point.getX();
      if (point.getY() < ymin)
        ymin = point.getY();
      if (point.getY() > ymax)
        ymax = point.getY();
    }
    return new Rectangle2d(xmin, ymin, xmax - xmin, ymax - ymin);
  }

  // neu---------------------
  public abstract boolean contains(Point3d p);

  public abstract double[] getNaturefromCart(Point3d p);

  // --------------------------------------------------------------------------//
  // AUSAGABE //
  // --------------------------------------------------------------------------//
  /**
   * Liefert die konvexe Zelle als Zeichenkette.
   * 
   * @return
   */
  // --------------------------------------------------------------------------//
  @Override
  public String toString() {
    String wort = dimension + "D-Zelle: (" + points[0];
    for (int i = 1; i < points.length; i++)
      wort += ", " + points[i];
    wort += ")";
    return wort;
  }
}
