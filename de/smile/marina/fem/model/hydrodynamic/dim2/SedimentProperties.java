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
package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.geom.LinearPoint;
import de.smile.geom.MetricPoint;
import de.smile.marina.PhysicalParameters;
import static de.smile.math.Function.sqr;
import java.io.Serializable;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Beschreibung der Sedimenteigenschaften
 *
 * @author Peter Milbradt
 * @version 4.0.0
 */
public class SedimentProperties
        implements Cloneable, MetricPoint<SedimentProperties>, LinearPoint<SedimentProperties>, Serializable {

    private static final long serialVersionUID = 1L;

    public static enum CriticalShieldsFunction {
        /**
         * Classic
         * SHIELDS (1936) gilt fuer Korngroeszen zwischen 0.1 bis 5 mm
         */
        Shields((Double D1) -> {
            if (D1 <= 1.66) {
                return 0.131325301;
            }
            if (D1 > 1.66 && D1 <= 4.) {
                return 0.218 / D1;
            }
            if (D1 > 4. && D1 <= 6.) {
                return 0.109 * Math.pow(D1, -0.5);
            }
            if (D1 > 6. && D1 <= 10.) {
                return 0.14 * Math.pow(D1, -0.64);
            }
            if (D1 > 10. && D1 <= 18.) {
                return 0.04 * Math.pow(D1, -0.1);
            }
            if (D1 > 18. && D1 <= 145.) {
                return 0.013 * Math.pow(D1, 0.29);
            }
            return 0.055;
        }),
        /**
         * Kritischer Shields-Parameter nach Soulsby, R.L., Whitehouse, R.J.S.,
         * 1997. Threshold of sediment motion in coastal environ-ments. Pacific
         * Coasts and Ports?97: Proceedings of the 13th Australasian Coastaland
         * Ocean Engineering Conference and the 6th Australasian Port and
         * Harbour Conference. Centre for Advanced Engineering, University of
         * Canterbury, Christchurch,pp. 145?150. diese Formulierung ist sehr
         * nahe an der urspruenglichen Formulierung von Shields initiation of
         * motion (movement of particles along the bed)
         */
        Soulsby((Double D) -> 0.3 / (1. + 1.2 * D) + 0.055 * (1. - Math.exp(-0.02 * D))),
        /**
         * kritische Schubspannung in der Formulierung nach Soulsby 1997
         * angepasst, so dass fuer grosze Korngroeszen dem von Knoroz entspricht
         */
        SoulsbyKnoroz((Double D) -> 0.3 / (1. + 1.2 * D) + 0.033 * (1. - Math.exp(-0.02 * D))), // auch fuer grobe
                                                                                                // Sedimente, z.B. Rhein
                                                                                                // und Donau
        Sisyphe((Double D) -> 0.45 / (1. + 1.2 * D) + 0.0267 * (1. - Math.exp(-0.03 * D))),
        /**
         * van Rijn, L.,C. (1984a): Sediment transport, part I: Bed Load
         * Transport. In: Journal of Hydraulic Engineering, Vol. 110, No. 10,
         * pp. 1431?1456
         */
        VanRijn((Double D) -> 0.3 / (1. + D) + 0.1 * (1. - Math.exp(-0.005 * D))),
        /**
         * Angepasste kritische Shieldsspannung nach Knoroz Gladkow/Soehngen:
         * Modellierung des Geschiebetransports mit unterschiedlicher
         * Korngroesse in Fluessen (BAW-Karlsruhe)
         */
        Knoroz((Double D1) -> {
            if (D1 <= 1.66) {
                return 0.131325301;
            }
            if (D1 > 1.66 && D1 <= 4.) {
                return 0.218 / D1;
            }
            if (D1 > 4. && D1 <= 6.) {
                return 0.109 * Math.pow(D1, -0.5);
            }
            if (D1 > 6. && D1 <= 10.) {
                return 0.14 * Math.pow(D1, -0.64);
            }
            if (D1 > 10. && D1 <= 18.) {
                return 0.04 * Math.pow(D1, -0.1);
            }
            if (D1 > 18. && D1 <= 25.) {
                return 0.013 * Math.pow(D1, 0.29);
            }
            return 0.033063282;
        }),
        /**
         * kritische Schubspannung nach Julien 2001
         */
        Julien((Double D) -> {
            final double d50 = D / Math.pow(PhysicalParameters.G
                    * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                    / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
            return 0.3 * Math.exp(-D / 3.) + 0.06 * getInnerFrictionAngle(d50) * (1. - Math.exp(-D / 20.));// original
        }),
        /**
         * kritische Schubspannung nach Julien 2001 mit Anpassung um fuer grosze
         * Koerner diese Formulierung liefert fuer grosze Korndurchmesser
         * wesentlich kleinere Kritische Schubspannungen als die urspruenglichen
         * Formulierung von Shields, ist nicht monoton wachsend - ungeeignet als
         * inverse Funktion
         */
        JulienKnoroz((Double D) -> {
            final double d50 = D / Math.pow(PhysicalParameters.G
                    * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                    / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
            return 0.3 * Math.exp(-D / 3.) + 0.039 * getInnerFrictionAngle(d50) * (1. - Math.exp(-D / 10.));// Peter's Anpassung um fuer grosze Koerner einen Transport zu erhalten
        });

        private final Function<Double, Double> function;

        CriticalShieldsFunction(Function<Double, Double> f) {
            this.function = f;
        }

        public double getCFS(double D) {
            return function.apply(D);
        }
    }

    CriticalShieldsFunction cfs = CriticalShieldsFunction.JulienKnoroz;

    public static final double n_trapez = .26; // Porosity bei Einkorn und Trapetzlagerung

    public double dmin = 0.042E-3; // [m] kleinstes verfuegbares Korn (entspricht in etwa d_05/2.)
    public double d50 = 0.42E-3; // [m] mittlerer Kordurchmesser, wenn eine Verteilung da ist soll dieser Wert
                                 // berechnet werden
    public double dmax = 4.2E-3; // [m] groesztes verfuegbares Korn (entspricht in etwa d_95*2.)

    public double k = .6 / ((1. - ((dmax + dmin) / 2.) / dmax) * (1. - dmin / ((dmax + dmin) / 2.)));
    public double initialSorting = 1;
    public double sorting = initialSorting * (1. - d50 / dmax) * (1. - dmin / d50) * k;

    public double wc = getWC_WuAndWang(d50); // [m/s] Sinkgeschwindigkeit

    public double porosity = getPorosityLogistic(d50, sorting);
    public double consolidation = (n_trapez / (1. + sorting * Math.sqrt(wc))) / porosity; // 1 wenn minmaler
                                                                                          // Zwischenraum - Boden
                                                                                          // maximal konsolidiert -
                                                                                          // fest!

    public double innerFrictionAngle = getInnerFrictionAngle(d50);
    double D = d50 * Math.pow(PhysicalParameters.G * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
            / PhysicalParameters.RHO_WATER
            / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1.0 / 3.0); // dimensionsloser
                                                                                                           // Teilchendurchmesser

    public SedimentProperties() {
        this(0.42E-3);
    }

    public SedimentProperties(double lowerGrainSize, double d50, double upperGrainSize, double initialSorting) {
        super();
        this.dmin = lowerGrainSize;
        this.d50 = d50;
        this.dmax = upperGrainSize;
        this.initialSorting = initialSorting;
        this.initalUpdate();
    }

    /**
     * Konstruktor
     *
     * @param d50 [m]
     */
    public SedimentProperties(double d50) {
        this(d50 / 10., d50, d50 * 10., 1.);
    }

    /**
     * Konstruktor
     *
     * @param d50            [m]
     * @param initialSorting 0 Einkrinmaterial, ..
     */
    public SedimentProperties(double d50, double initialSorting) {
        this(d50 / 10., d50, d50 * 10., initialSorting);
    }

    public void setProperties(String line) throws Exception {
        // new String[] {"nodenumber ; x ; y ; z [m] in depth ; dmax [mm] ; d50 [mm] ;
        // dmin [mm] ; meanInitialSorting ; porosity ; consolidation ; time [s] since
        // 01.01.1970 ; Datum"};
        String[] seperated = line.split(";");
        dmax = Double.parseDouble(seperated[4].trim()) / 1000.;
        d50 = Double.parseDouble(seperated[5].trim()) / 1000.;
        dmin = Double.parseDouble(seperated[6].trim()) / 1000.;
        initialSorting = Double.parseDouble(seperated[7].trim());
        this.initalUpdate();
        porosity = Double.parseDouble(seperated[8].trim());
        consolidation = Double.parseDouble(seperated[9].trim());
    }

    /**
     * update Sedimentparameter
     *
     * @param lowerGrainSize [m]
     * @param d50            [m]
     * @param upperGrainSize [m]
     * @param initialSorting 0 Einkornmaterial, ..
     */
    public final void update(double lowerGrainSize, double d50, double upperGrainSize, double initialSorting) {
        this.dmin = lowerGrainSize;
        this.d50 = d50;
        this.dmax = upperGrainSize;
        this.initialSorting = initialSorting;
        this.initalUpdate();
    }

    /**
     * update Sedimentparameter
     *
     * @param lowerGrainSize [m]
     * @param d50            [m]
     * @param upperGrainSize [m]
     */
    public final void update(double lowerGrainSize, double d50, double upperGrainSize) {
        this.dmin = lowerGrainSize;
        this.d50 = d50;
        this.dmax = upperGrainSize;
        this.initalUpdate();
    }

    /**
     * Update the dependent attributes
     */
    final void update() {
        this.D = d50 * Math.pow(PhysicalParameters.G * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
        this.sorting = initialSorting * (1. - d50 / dmax) * (1. - dmin / d50) * k;
        this.wc = getWC_WuAndWang(d50);
        this.porosity = getPorosityLogistic(d50, sorting, wc);
        consolidation = (n_trapez / (1. + sorting * Math.sqrt(wc))) / porosity; // 1 wenn minmaler Zwischenraum - Boden
                                                                                // maximal konsolidiert - fest!
        this.innerFrictionAngle = getInnerFrictionAngle(d50);
    }

    /**
     * initalization of the dependent attributes
     */
    final void initalUpdate() {
        if (dmin > dmax) {
            System.out.println("dmin>dmax an einem Knoten");
            System.exit(0);
        }
        d50 = Math.min(0.99 * dmax, d50);
        d50 = Math.max(1.01 * dmin, d50);
        this.k = .6 / ((1. - ((dmax + dmin) / 2.) / dmax) * (1. - dmin / ((dmax + dmin) / 2.)));
        this.update();
    }

    /**
     * Umrechnung der phi-Skala [phi] auf metrischer Skala [mm]
     *
     * @param phi
     * @return Korndurchmesser in [mm]
     */
    public static double phi2d(double phi) {
        return Math.pow(2., -phi);
    }

    /**
     * Umrechnung der metrischer Skala [mm] in die phi-Skala
     *
     * @param d Durchmesser in [mm]
     * @return phi
     */
    public static double d2phi(double d) {
        if (d <= 0.0) {
            throw new ArithmeticException("range exception");
        }
        return -Math.log(d) / Math.log(2.);
        // return -SpecialFunction.log2(d);
    }

    /**
     * gibt den inneren Reibungswinkel als tan(theta) zurueck
     *
     * @param d50 [m]
     * @return
     */
    public static double getInnerFrictionAngle(double d50) {
        if (d50 <= 0.063E-3) {
            return 0.45; // Winkel ca. 25 Grad
        }
        if (d50 >= 5E-3) {
            return 0.84; // Winkel ca. 40 Grad
        }
        double lambda = (d50 - 0.063E-3) / (5E-3 - 0.063E-3);
        return lambda * 0.84 + (1 - lambda) * 0.45;
    }

    /**
     * Porosity
     *
     * @param d50 in [m]
     * @return porosity between 0 (pur Sediment) and 1 (pur Water)
     */
    public static double getPorosity(double d50) {
        return getPorosityLogistic(d50);
    }

    /**
     * Porosity
     *
     * @param d50     in [m]
     * @param sorting (0 Einkron, 1 gut gemischt, grosz - breites Kornspektrum)
     * @return porosity between 0 (pur Sediment) and 1 (pur Water)
     */
    public static double getPorosity(double d50, double sorting) {
        return getPorosity(d50) / (1.0 + sorting * Math.sqrt(getWC(d50)));
    }

    /**
     * porosity based on a best fitted logistic relationship Robert J. Wilson,
     * Douglas C. Speirs, Alessandro Sabatino, and Michael R. Heath A synthetic
     * map of the north-west European Shelf sedimentary environment for
     * applications in marine science Earth Syst. Sci. Data, 10, 109?130, 2018
     * https://doi.org/10.5194/essd-10-109-2018
     *
     * @param d50 in [m]
     * @return porosity - the value is always greater than 0.26, the prostity of
     *         a ball packing of the same size
     */
    public static double getPorosityLogistic(double d50) {
        final double p1 = -0.436;
        final double p2 = 0.366;
        final double p3 = -1.227;
        final double p4 = -0.27;
        return Math.pow(10, p1 + p2 / (1 + Math.exp(-(Math.log10(d50 * 1000.0) - p3) / p4)));
    }

    /**
     * porosity based on a best fitted logistic relationship Robert J. Wilson,
     * Douglas C. Speirs, Alessandro Sabatino, and Michael R. Heath A synthetic
     * map of the north-west European Shelf sedimentary environment for
     * applications in marine science Earth Syst. Sci. Data, 10, 109?130, 2018
     * https://doi.org/10.5194/essd-10-109-2018
     *
     * @param d50     in [m]
     * @param sorting (0 Einkron, 1 gut gemischt, grosz - breites Kornspektrum)
     * @return
     */
    public static double getPorosityLogistic(double d50, double sorting) {
        return getPorosityLogistic(d50) / (1.0 + sorting * getWC_WuAndWang(d50));
    }

    /**
     * porosity based on a best fitted logistic relationship Robert J. Wilson,
     * Douglas C. Speirs, Alessandro Sabatino, and Michael R. Heath A synthetic
     * map of the north-west European Shelf sedimentary environment for
     * applications in marine science Earth Syst. Sci. Data, 10, 109?130, 2018
     * https://doi.org/10.5194/essd-10-109-2018
     *
     * @param d50     in [m]
     * @param sorting (0 Einkron, 1 gut gemischt, grosz - breites Kornspektrum)
     * @param wc      Settling velocity [m/s]
     * @return
     */
    public static double getPorosityLogistic(double d50, double sorting, double wc) {
        return getPorosityLogistic(d50) / (1.0 + sorting * wc);
    }

    /**
     * Settling velocity actual based on Wu and Wang
     *
     * @param d50 [m]
     * @return settling velocity [m/s]
     */
    static double getWC(double d50) {
        return getWC_WuAndWang(d50);
    }

    /**
     * Settling velocity, Wu and Wang 2006, JoHE, 2006, grain shape factor = 0.7
     *
     * @param d50 [m]
     * @return settling velocity [m/s]
     */
    final static double Sf = 0.7; // Formparameter fuer das Korn 1 ist kugel, 0.7 natuerlich, 0.3 unfoermig
    final static double M = 53.5 * Math.exp(-0.65 * Sf);
    final static double N = 5.65 * Math.exp(-2.5 * Sf);
    final static double n = 0.7 + 0.9 * Sf;

    public static double getWC_WuAndWang(double d50) {

        double D = d50 * Math.pow(PhysicalParameters.G * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);

        return M / N * PhysicalParameters.KINVISCOSITY_WATER / d50
                * Math.pow(Math.sqrt(1. / 4. + Math.pow(4. / 3. * N / M / M * D * D * D, 1. / n)) - 1. / 2., n);
    }

    public static void mainTest(String... args) {
        double d50 = 1e-4;
        System.out.println("Initial d50: " + d50);
        System.out.println("wc: " + getWC_WuAndWang(d50));

        d50 = 1e-3;
        System.out.println("Initial d50: " + d50);
        System.out.println("wc: " + getWC_WuAndWang(d50));

        d50 = 1e-2;
        System.out.println("Initial d50: " + d50);
        System.out.println("wc: " + getWC_WuAndWang(d50));

    }

    /**
     * Durchlaessigkeit based on Bear, J.: Dyinamics of Fluid in Porous Media,
     * Elsevier, New York, USA, 1972.
     *
     * @param d50      in mm
     * @param porosity between 0 an 1
     * @return Permeability in m/s
     */
    public static double getPermeabilityBear1972(double d50, double porosity) {
        return porosity * porosity * porosity / 180. / (1. - porosity) / (1. - porosity) * d50 * d50;
    }

    @Override
    public SedimentProperties clone() throws CloneNotSupportedException {
        SedimentProperties rvalue = (SedimentProperties) super.clone();
        rvalue.dmax = dmax; // [m] groesztes verfuegbares Korn (entspricht in etwa d_95*2)
        rvalue.d50 = d50; // [m] mittlerer Kordurchmesser, wenn eine Verteilung da ist soll dieser Wert
                          // berechnet werden
        rvalue.dmin = dmin; // [m] kleinstes verfuegbares Korn (entspricht in etwa d_05/2)
        rvalue.initialSorting = initialSorting;
        rvalue.sorting = sorting;
        rvalue.wc = wc; // [m/s] Sinkgeschwindigkeit
        rvalue.porosity = porosity;
        rvalue.consolidation = consolidation; // 1 wenn minmaler Zwischenraum - fest!
        return rvalue;
    }

    @Override
    public double distance(SedimentProperties y) {
        return Math.sqrt(sqr(this.dmax - y.dmax)
                + sqr(this.d50 - y.d50)
                + sqr(this.dmin - y.dmin)
                + sqr(this.initialSorting - y.initialSorting)
                + sqr(this.consolidation - y.consolidation));
    }

    @Override
    public SedimentProperties add(SedimentProperties point) {
        try {
            SedimentProperties rvalue = this.clone();
            rvalue.dmax += point.dmax;
            rvalue.d50 += point.d50;
            rvalue.dmin += point.dmin;
            rvalue.initialSorting += point.initialSorting;
            rvalue.sorting += point.sorting;
            rvalue.wc += point.wc; // [m/s] Sinkgeschwindigkeit
            rvalue.porosity += point.porosity;
            rvalue.consolidation += point.consolidation; // 1 wenn minmaler Zwischenraum - fest!
            return rvalue;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(SedimentProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public SedimentProperties sub(SedimentProperties point) {
        try {
            SedimentProperties rvalue = this.clone();
            rvalue.dmax -= point.dmax;
            rvalue.d50 -= point.d50;
            rvalue.dmin -= point.dmin;
            rvalue.initialSorting -= point.initialSorting;
            rvalue.sorting -= point.sorting;
            rvalue.wc -= point.wc; // [m/s] Sinkgeschwindigkeit
            rvalue.porosity -= point.porosity;
            rvalue.consolidation -= point.consolidation; // 1 wenn minmaler Zwischenraum - fest!
            return rvalue;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(SedimentProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public SedimentProperties mult(double scalar) {
        try {
            SedimentProperties rvalue = this.clone();
            rvalue.dmax *= scalar;
            rvalue.d50 *= scalar;
            rvalue.dmin *= scalar;
            rvalue.initialSorting *= scalar;
            rvalue.sorting *= scalar;
            rvalue.wc *= scalar; // [m/s] Sinkgeschwindigkeit
            rvalue.porosity *= scalar;
            rvalue.consolidation *= scalar; // 1 wenn minmaler Zwischenraum - fest!
            return rvalue;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(SedimentProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
