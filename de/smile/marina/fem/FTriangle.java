/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2026

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
package de.smile.marina.fem;

import de.smile.geom.Rectangle2d;
import javax.vecmath.Point3d;

/**
 * @version 4.10.7
 * @author Peter Milbradt
 */

public class FTriangle extends FElement {
    public static final double minV = 1.E-6; // minimal velocity to be considered for vectorLength computation
    /*----------------------------------------------------------------------*/
    /* Elementkennung (Bits von Element[].kennung) */
    /*----------------------------------------------------------------------*/
    public static final int bit_nr_kante_ij = 4; /* Bit Nr. 2 steht fuer i-j */
    public static final int bit_nr_kante_jk = 1; /* Bit Nr. 0 steht fuer j-k */
    public static final int bit_nr_kante_ki = 2; /* Bit Nr. 1 steht fuer k-i */
    public static final int bit_kante_ij = 4; /* (2^2) Kante i-j geschlossen */
    public static final int bit_kante_ji = 4; /* (2^2) */
    public static final int bit_kante_jk = 1; /* (2^0) Kante j-k geschlossen */
    public static final int bit_kante_kj = 1; /* (2^0) */
    public static final int bit_kante_ki = 2; /* (2^1) Kante k-i geschlossen */
    public static final int bit_kante_ik = 2; /* (2^1) */
    public static final int bit_kante_ijk = 5; /* (2^0 + 2^2) Kante ij und jk */
    public static final int bit_kante_jki = 3; /* (2^0 + 2^1) Kante jk und ki */
    public static final int bit_kante_kij = 6; /* (2^1 + 2^2) Kante ki und ij */
    public static final int bit_kante_ijki = 7; /* (2^0 + 2^1 + 2^2) alle Kanten */
    private final double koeffmat[][] = new double[3][3];
    private int ken;
    public final double minHight, maxEdgeLength;
    public final double dx, dy; // dx=xmax-xmin, ..
    public final double area;

    public double dzdx, dzdy, bottomslope;
    public double elemFormParameter=1;

    public double[][] distance = new double[3][3]; // Feld mit den Kantenlaengen

    // Vorberechnete lokale Kantenvektoren fuer getVectorSize (relativ zu jedem Knoten)
    // edgeLocal[i][0] = dofs[(i+1)%3].x - dofs[i].x,  edgeLocal[i][1] = dofs[(i+1)%3].y - dofs[i].y
    // edgeLocal[i][2] = dofs[(i+2)%3].x - dofs[i].x,  edgeLocal[i][3] = dofs[(i+2)%3].y - dofs[i].y
    // edgeLocal[i][4] = edgeLocal[i][3] - edgeLocal[i][1] = (e2y - e1y)
    // edgeLocal[i][5] = edgeLocal[i][2] - edgeLocal[i][0] = (e2x - e1x)
    // edgeLocal[i][6] = e1x*e2y - e2x*e1y (= 2*area, Vorzeichen abhaengig von Orientierung)
    private final double[][] edgeLocal = new double[3][7];

    public FTriangle(DOF d1, DOF d2, DOF d3) {

        order = 1;

        double area2 = ((d2.x - d1.x) * (d3.y - d1.y) - (d3.x - d1.x) * (d2.y - d1.y));
        area = 0.5 * area2;

        dofs = new DOF[3];
        dofs[0] = d1;
        dofs[1] = d2;
        dofs[2] = d3;

        // von allen Knoten einfach die Korrdinaten des ersten Knoten abziehen
        koeffmat[0][0] = ((dofs[1].x - d1.x) * (dofs[2].y - d1.y) - (dofs[2].x - d1.x) * (dofs[1].y - d1.y))
                / area2;
        koeffmat[1][0] = ((dofs[2].x - d1.x) * (dofs[0].y - d1.y) - (dofs[0].x - d1.x) * (dofs[2].y - d1.y))
                / area2;
        koeffmat[2][0] = ((dofs[0].x - d1.x) * (dofs[1].y - d1.y) - (dofs[1].x - d1.x) * (dofs[0].y - d1.y))
                / area2;

        koeffmat[0][1] = (dofs[1].y - dofs[2].y) / area2;
        koeffmat[1][1] = (dofs[2].y - dofs[0].y) / area2;
        koeffmat[2][1] = (dofs[0].y - dofs[1].y) / area2;
        koeffmat[0][2] = (dofs[2].x - dofs[1].x) / area2;
        koeffmat[1][2] = (dofs[0].x - dofs[2].x) / area2;
        koeffmat[2][2] = (dofs[1].x - dofs[0].x) / area2;

        // Kantenlaengen erzeugen
        distance[0][1] = distance[1][0] = dofs[0].distance(dofs[1]);
        distance[1][2] = distance[2][1] = dofs[1].distance(dofs[2]);
        distance[2][0] = distance[0][2] = dofs[2].distance(dofs[0]);

        double maxEdgeLength = distance[0][1];
        maxEdgeLength = Math.max(maxEdgeLength, distance[1][2]);
        maxEdgeLength = Math.max(maxEdgeLength, distance[2][0]);
        this.maxEdgeLength = maxEdgeLength;

        double minEdgeLength = distance[0][1];
        minEdgeLength = Math.min(minEdgeLength, distance[1][2]);
        minEdgeLength = Math.min(minEdgeLength, distance[2][0]);

        minHight = 2 * area / maxEdgeLength;

        dzdx = dofs[0].z * koeffmat[0][1] + dofs[1].z * koeffmat[1][1] + dofs[2].z * koeffmat[2][1];
        dzdy = dofs[0].z * koeffmat[0][2] + dofs[1].z * koeffmat[1][2] + dofs[2].z * koeffmat[2][2];
        bottomslope = Math.sqrt(dzdx * dzdx + dzdy * dzdy + 1.);

        final double xmin = Math.min(dofs[0].x, Math.min(dofs[1].x, dofs[2].x));
        final double xmax = Math.max(dofs[0].x, Math.max(dofs[1].x, dofs[2].x));
        final double ymin = Math.min(dofs[0].y, Math.min(dofs[1].y, dofs[2].y));
        final double ymax = Math.max(dofs[0].y, Math.max(dofs[1].y, dofs[2].y));
        dx = xmax - xmin;
        dy = ymax - ymin;

        // Shape-Faktor (Standard in FEM) ab Marina 4.10.4
        final double sumEdgeSq = distance[0][1] * distance[0][1] + distance[1][2] * distance[1][2]
                + distance[2][0] * distance[2][0];
        elemFormParameter = area / (sumEdgeSq * 0.144338);

        // Vorberechnung der lokalen Kantenvektoren fuer getVectorSize
        for (int ii = 0; ii < 3; ii++) {
            final int ii1 = (ii + 1) % 3;
            final int ii2 = (ii + 2) % 3;
            edgeLocal[ii][0] = dofs[ii1].x - dofs[ii].x; // e1x
            edgeLocal[ii][1] = dofs[ii1].y - dofs[ii].y; // e1y
            edgeLocal[ii][2] = dofs[ii2].x - dofs[ii].x; // e2x
            edgeLocal[ii][3] = dofs[ii2].y - dofs[ii].y; // e2y
            edgeLocal[ii][4] = edgeLocal[ii][3] - edgeLocal[ii][1]; // e2y - e1y
            edgeLocal[ii][5] = edgeLocal[ii][2] - edgeLocal[ii][0]; // e2x - e1x
            edgeLocal[ii][6] = edgeLocal[ii][0] * edgeLocal[ii][3] - edgeLocal[ii][2] * edgeLocal[ii][1]; // e1x*e2y - e2x*e1y
        }
    }

    /**
     * @param vx
     * @param vy
     * @return elementsize connecting to the vector (vx,vy)
     */
    public final double getVectorSize(double vx, double vy) {
        double dl = 0.;
        int i = 0;

        final double normV = Math.hypot(vx, vy);

        if (normV >= minV) {
            do {
                final double[] el = edgeLocal[i];

                // denominator of s and t are negatives of each other (denom_t = -denom_s)
                final double denomS = vx * el[4] - vy * el[5]; // vx*(e2y-e1y) - vy*(e2x-e1x)

                if (Math.abs(denomS) < 1e-14 * Math.abs(area)) { // ray parallel to edge
                    i++;
                    continue;
                }

                double s = el[6] / denomS; // (e1x*e2y - e2x*e1y) / denomS
                double t = (vx * el[1] - vy * el[0]) / (-denomS); // (vx*e1y - vy*e1x) / (-denomS)

                if (t < 1.00001 && t > -0.00001) // geschnitten
                    dl = Math.abs(s) * normV;

                i++;
            } while ((i < 3) && (dl == 0.));

            if (dl == 0.) {
                System.out.println("kann keine Elementausdehnung berechnen");
                return minHight;
            }
            
            if (normV < minV*10.) {
                final double lambda = (normV - minV)/(minV*10. - minV);
                return lambda*dl +(1-lambda)*minHight;
            }
        } else {
            return minHight;
        }
        return (dl);
    }

    public final int getKennung() {
        return ken;
    }

    public final void setKennung(int i) {
        ken = i;
    }

    public final double[][] getkoeffmat() {
        // double mat[][] = new double[3][3];
        // for (int i=0;i<3;i++)
        // for (int j=0;j<3;j++)
        // mat[i][j]=koeffmat[i][j];
        // return mat;
        return koeffmat;
    }

    @Override
    public final double getVolume() {
        return area;
    }

    public final double ShapeMass(int i, int j) {
        if (i == j)
            return 1. / 6.;
        else
            return 1. / 12.;
    }

    @Override
    public final boolean contains(Point3d p) {
        if (p == null)
            return false;

        double repsilon = 1e-7;

        double a2 = (dofs[1].x - dofs[0].x) * (dofs[2].y - dofs[0].y)
                - (dofs[2].x - dofs[0].x) * (dofs[1].y - dofs[0].y); // vorzeichenbehafteter Flaecheninhalt
        // erste Teilflaeche
        if ((dofs[1].x * (dofs[2].y - p.y) + dofs[2].x * (p.y - dofs[1].y) +
                p.x * (dofs[1].y - dofs[2].y)) / a2 <= -repsilon)
            return false;
        // zweite Teilflaeche
        if ((dofs[2].x * (dofs[0].y - p.y) + dofs[0].x * (p.y - dofs[2].y) +
                p.x * (dofs[2].y - dofs[0].y)) / a2 <= -repsilon)
            return false;
        // dritte Teilflaeche
        return (dofs[0].x * (dofs[1].y - p.y) + dofs[1].x * (p.y - dofs[0].y) +
                p.x * (dofs[0].y - dofs[1].y)) / a2 > -repsilon;
    }

    /**
     * Gets the natural coordinates of a point.
     *
     * @param p - a <code>Point2d</code>.
     * @return <code>double[]</code> representing the natural
     *         coordinates of <code>other</code>.
     */
    @Override
    public final double[] getNaturefromCart(Point3d p) {
        if (p == null)
            return null;

        double repsilon = 1e-7;

        double a2 = (dofs[1].x - dofs[0].x) * (dofs[2].y - dofs[0].y)
                - (dofs[2].x - dofs[0].x) * (dofs[1].y - dofs[0].y); // vorzeichenbehafteter Flaecheninhalt
        double[] erg = new double[3];
        // erste Teilflaeche
        erg[0] = dofs[1].x * (dofs[2].y - p.y) + dofs[2].x * (p.y - dofs[1].y) +
                p.x * (dofs[1].y - dofs[2].y);
        erg[0] /= a2;
        if (erg[0] <= -repsilon)
            return null;
        // zweite Teilflaeche
        erg[1] = dofs[2].x * (dofs[0].y - p.y) + dofs[0].x * (p.y - dofs[2].y) +
                p.x * (dofs[2].y - dofs[0].y);
        erg[1] /= a2;
        if (erg[1] <= -repsilon)
            return null;
        // dritte Teilflaeche
        erg[2] = dofs[0].x * (dofs[1].y - p.y) + dofs[1].x * (p.y - dofs[0].y) +
                p.x * (dofs[0].y - dofs[1].y);
        erg[2] /= a2;
        if (erg[2] <= -repsilon)
            return null;

        if (erg[0] <= repsilon)
            erg[0] = 0;
        else if (erg[0] >= 1 - repsilon)
            erg[0] = 1;

        if (erg[1] <= repsilon)
            erg[1] = 0;
        else if (erg[1] >= 1 - repsilon)
            erg[1] = 1;

        if (erg[2] <= repsilon)
            erg[2] = 0;
        else if (erg[2] >= 1 - repsilon)
            erg[2] = 1;

        return erg;
    }

    @Override
    // --------------------------------------------------------------------------//
    // Bounding Box der konvexen Zelle //
    // --------------------------------------------------------------------------//
    public Rectangle2d getBounds() {
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double xmax = -Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;

        // minimales, maximales x und y ermitteln
        for (Point3d point : dofs) {
            if (point.x < xmin)
                xmin = point.x;
            if (point.x > xmax)
                xmax = point.x;
            if (point.y < ymin)
                ymin = point.y;
            if (point.y > ymax)
                ymax = point.y;
        }
        return new Rectangle2d(xmin, ymin, xmax - xmin, ymax - ymin);
    }
}
