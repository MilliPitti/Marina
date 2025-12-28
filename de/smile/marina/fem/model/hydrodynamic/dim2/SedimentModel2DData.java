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

import bijava.math.ifunction.*;
import de.smile.marina.PhysicalParameters;
import static de.smile.marina.PhysicalParameters.KINVISCOSITY_WATER;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import de.smile.math.Function;
import java.util.*;

/**
 * @author Peter Milbradt
 * @version 4.9.3
 */
public class SedimentModel2DData extends SedimentProperties implements ModelData {

    private static int id = NO_MODEL_DATA; // Zustandsgroessen
    private static final long serialVersionUID = 1L;

    double u, v; // tiefenintegrierte Geschwindigkeiten, werden durch sekundaerstroemung
                 // angepasst
    double cv;

    public double sC; // depth integrated sediment concentration in [m^2/m^3]
    double dsCdt; // rate of change of the depth integrated sediment concentration in [m^2/m^3/s]
    public double sedimentSource; // source and sink of suspendet sediment

    public double qsx = 0., qsy = 0.; // suspendierter Sedimenttransport qs=sC*totaldepth*cv

    double qTotal_x, qTotal_y; // Komponenten des totalen Sedimenttransportes
    double u_bank, v_bank;

    public double bedload = 0.0; // bedload sedimenttransport [m**3 / (m*s)]
    public double[] bedloadVector = new double[] { 0., 0. };

    public double lambdaQs = 0.; // Indikator fuer Sedimenttransport am Knoten: 0 kein Transport, 1 relevanter
                                 // Transport
    public boolean isDeepest = false, _isDeepest = true; // falls der Knoten der Tiefste im Patch ist
    public boolean isHighest = false, _isHighest = true; // falls der Knoten der hoechtse im Patch ist

    public double bottomslope = 1.;
    double _bottomslope = 0.; // temporary value

    public double z; // [m] Lage des Gewaesserbodens als Tiefe
    double dzdt;

    double dzTransportdt; // zur Residualberechnung

    public double zh; // [m] nichterodierbarar Tiefenhorizont als Tiefe
    public double maintainedDepth = -7000.; // mindestens zu haltende Fahrwasser-Tiefe
    double bound = 0.42E-3 * 10.; // Grenzschichtdicke von 10 Koernern ab denen Wattstrategie und Strategie fuer
                                  // nicht erodierbare Horizonte anschlaegt
    double lambda = 1.; // increasing factor depending on not erodible bottom
    double d50init = 0.42E-3;

    // double suspendedGrainSize = 0.42E-3;
    // double suspendedWc = getWC(suspendedGrainSize);
    // double suspendedD = suspendedGrainSize *
    // Math.pow(PhysicalParameters.G*(PhysicalParameters.RHO_SEDIM-PhysicalParameters.RHO_WATER)/PhysicalParameters.RHO_WATER/(PhysicalParameters.KINVISCOSITY_WATER*PhysicalParameters.KINVISCOSITY_WATER),1./3.);
    // // dimensionsloser Teichendurchmesser
    // boudary conditions
    ScalarFunction1d bconc; // boundary condition for sediment concenration
    ScalarFunction1d bz; // boundary condition for bottom
    ScalarFunction1d bd50; // boundary condition for d50
    boolean extrapolate_z = false; // indicator for extrapolating - for boundary nodes without boundary conditions
    boolean extrapolate_conc = false; // indicator for extrapolating - for boundary nodes without boundary conditions
    // boolean extrapolate_d50 = false; // indicator for extrapolating - for
    // boundary nodes without boundary conditions
    double CSF = 0.24 / D;
    double tau_cr = CSF * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
            * PhysicalParameters.G * d50;
    double rC;
    double rZTransport; // for Transport
    double rZCorrect; // for Diffusion
    double rSKoncCorrect;
    double dzCdt;

    double duneHeight = 0;
    double duneLengthX = 0;
    double duneLengthY = 0;
    double duneLength;

    double grainShearStress; // Schubspannung die auf die Koerner wirkt

    public SedimentModel2DData() {
        super();
        id = SEARCH_MODEL_DATA;
        setD50(this.d50);
    }

    public void initialD50(double d50) {
        dmax = d50 * 10.;
        dmin = d50 / 10.;
        d50init = d50;
        setD50(d50);
    }

    /**
     * Porosity nach Puls
     * 
     * @param phi50 in [PHI]
     * @return Porosity
     */
    final public static double getPorosity_phi50(double phi50) {
        if (phi50 <= .767) {
            return .3;
        } else if (phi50 >= 6.22) {
            return .823;
        } else {
            return .2603 * Math.pow(1.20325, phi50);
        }
    }

    /**
     * Wu und Wang 2006 passten Komuras formula an
     * 
     * @param d50 in [m]
     * @return
     */
    final public static double getPorosityKomuraWuWang(double d50) {
        return 0.13 + 0.21 / Math.pow(d50 * 1.E3 + 0.002, 0.21);
    }

    /**
     * Komura/Simmons 1967 oder 63 formula
     * 
     * @param d50 in [m]
     * @return
     */
    final public static double getPorosityKomuraSimmons(double d50) {
        return 0.245 + 0.0864 / Math.pow(0.1 * d50 * 1.E3, 0.21);
    }

    /**
     * porosity computed from -0.06 * S + 0.36 Delft3D R. Frings
     * 
     * @param S is the log std of the sediment mixture
     *          sigmix = 0.0_fp
     *          do l = 1, this%settings%nfrac
     *          sigmix = sigmix + mfrac(l)*((phi(l)-phim)**2 + sigphi(l)**2)
     *          enddo
     *          sigmix = sqrt(sigmix)
     * @return
     */
    final public static double getPorosityDelft3D(double S) {
        return -0.06 * S + 0.36;
    }

    /**
     * porosity G.J. Weltje based on data by Beard & Weyl (AAPG Bull., 1973) /
     * Delft3D
     * 
     * @param S is the log std of the sediment mixture (phi-basierte
     *          Standartabweichung)
     *          sigphi ! standard deviation expressed on phi scale, units : -
     *          phim = 0.0_fp
     *          do l = 1, this%settings%nfrac
     *          phim = phim + phi(l)*mfrac(l)
     *          enddo
     *          sigmix = 0.0_fp
     *          do l = 1, this%settings%nfrac
     *          sigmix = sigmix + mfrac(l)*((phi(l)-phim)**2 + sigphi(l)**2)
     *          enddo
     *          sigmix = sqrt(sigmix)
     * @return
     */
    final public static double getPorosityBeard_Weyl(double S) {
        final double x = 3.7632 * Math.pow(S, -0.7552);
        return 0.45 * x / (1 + x);
    }

    public final void setD50(double d50) {

        this.d50 = Function.max(1.1 * this.dmin, Function.min(.9 * this.dmax, d50));
        super.update();

        this.bound = d50 * 10.;

        this.CSF = this.cfs.getCFS(D);

        lambda = Function.min(1., Function.max(0., zh - z) / bound); // increasing factor depending on not erodible
                                                                     // bottom
        CSF *= 1. / (1. + 1. - lambda); // Verdopplung der kritischen Bodenschubspannung bei nichterodierbarem Boden
        tau_cr = CSF * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                * PhysicalParameters.G * d50;
    }

    /**
     * critical depth-averaged velocity for initiation of suspesion (Soulsby 1997)
     * 
     * @param d50   [mm]
     * @param depth
     * @return kritische Schubspannung [N/m/m]
     */
    final public static double criticalVelocitySoulsby(double d50, double depth) {
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
        return 5.75 * Math.log(12 * depth / 6 * d50)
                * Math.sqrt(SedimentProperties.CriticalShieldsFunction.Soulsby.getCFS(D)
                        * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER)
                        * PhysicalParameters.G * d50);
    }

    /**
     * critical depth-averaged velocity for initiation of suspesion (Van Rijn 1993)
     * 
     * @param d50   [mm]
     * @param depth
     * @return kritische Schubspannung [N/m/m]
     */
    final public static double criticalVelocityVanRijn(double d50, double depth) {
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
        return 5.75 * Math.log(12 * depth / 6 * d50)
                * Math.sqrt(SedimentProperties.CriticalShieldsFunction.VanRijn.getCFS(D)
                        * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER)
                        * PhysicalParameters.G * d50);
    }

    /**
     * mittlere kritische Geschwindigkeit nach Zanke 1982
     * 
     * @param d50 [mm]
     * @return kritische Schubspannung [N/m/m]
     */
    public static double criticalVelocityZanke(double d50) {
        final double ca = 1; // Adhaesionkoeffizient [0.2 bis 1]
        return 2.8 * Math.sqrt((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                / PhysicalParameters.RHO_WATER * PhysicalParameters.G * d50)
                + 14.7 * PhysicalParameters.KINVISCOSITY_WATER / d50 * ca;
    }

    // public void setSuspendedD50(double d50){
    // suspendedGrainSize = d50;
    // suspendedWc = getWC(d50);
    // suspendedD = suspendedGrainSize *
    // Math.pow(PhysicalParameters.G*(PhysicalParameters.RHO_SEDIM-PhysicalParameters.RHO_WATER)/PhysicalParameters.RHO_WATER/(PhysicalParameters.KINVISCOSITY_WATER*PhysicalParameters.KINVISCOSITY_WATER),1./3.);
    // // dimensionsloser Teichendurchmesser
    // }
    public static SedimentModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof SedimentModel2DData sedimentModel2DData) {
                    id = dof.getIndexOf(md);
                    return sedimentModel2DData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (SedimentModel2DData) dof.getModelData(id);
        }
        return null;
    }

    /**
     * Settling velocity Stokes
     * 
     * @param d50 [m]
     * @return settling velocity [m/s]
     */
    final public static double getWC_Stokes(double d50) {
        return PhysicalParameters.G * d50 * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                / PhysicalParameters.RHO_WATER / 18. / PhysicalParameters.KINVISCOSITY_WATER;
    }

    /**
     * Settling velocity Oseen
     * 
     * @param d50 [m]
     * @return settling velocity [m/s]
     */
    final public static double getWC_Oseen(double d50) {
        double wc = PhysicalParameters.G * d50 * d50 * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                / PhysicalParameters.RHO_WATER / 18. / PhysicalParameters.KINVISCOSITY_WATER;
        double dn, wcn;
        int i = 0;
        do {
            wcn = Math.sqrt(PhysicalParameters.G * d50 * d50
                    * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER / 18.
                    * wc / (PhysicalParameters.KINVISCOSITY_WATER + 3. / 16. * wc * d50));
            dn = Math.abs(wc - wcn) / wcn;
            wc = wcn;
            i++;
        } while (dn > 0.001 && i < 100);
        return wc;
    }

    /**
     * Settling velocity, WU and Wang, JoHE, 2006, grain shape factor = 0.7
     * 
     * @param d50 [m]
     * @return settling velocity [m/s]
     */
    final public static double getWC_Wu(double d50) {

        double dimD = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);

        return (33.9 * PhysicalParameters.KINVISCOSITY_WATER / (0.98 * d50)) * Math.pow(
                (Math.sqrt(0.25 + Math.pow((4 * 0.98 * Math.pow(dimD, 3.) / (3 * 33.9 * 33.9)), (1.0 / 1.33))) - 0.5),
                1.33);
    }

    /**
     * Settling velocity according to Ferguson & Church (2004).
     *
     * @param d50 [m] D50 grain size
     * @return settling velocity [m/s]
     */
    public static double getWC_FergusonChurch(double d50) {
        final double C2 = 1.0; // theTurbulent drag coefficient (=0.4 for natural sediments)
        // Umrechnung von m in mm f�r die Schwellenwerte
        double d50_mm = d50 * 1000.0;

        // 1. Berechnung von Rg basierend auf Korngr��e
        final double Rg;

        if (d50_mm < 0.002) {
            // Ton: Rg = 1.40
            Rg = 1.40;
        } else if (d50_mm < 0.063) {
            // Schluff: Lineare Interpolation zwischen 1.40 und 1.65
            Rg = 1.40 + (d50_mm - 0.002) / (0.063 - 0.002) * (1.65 - 1.40);
        } else {
            // Sand und gr�ber: Rg = 1.65
            Rg = 1.65;
        }

        // 2. Berechnung von C1 basierend auf Korngr��e
        final double C1;

        if (d50_mm < 0.002) {
            // Ton: Hoeherer C1-Wert wegen pl�ttchenf�rmiger Struktur
            C1 = 24.0;
        } else if (d50_mm < 0.063) {
            // Schluff: Lineare Interpolation zwischen 24.0 und 18.0
            C1 = 18.0 + (0.063 - d50_mm) / (0.063 - 0.002) * (24.0 - 18.0);
        } else {
            // Sand und gr�ber: Standardwert fuer sphaerische Partikel
            C1 = 18.0;
        }
        // Calculate the numerator: Rg * d50^2
        double numerator = Rg * Math.pow(d50, 2.0);

        // Calculate the term inside the square root: 0.75 * C2 * Rg * d50^3
        double termInsideSqrt = 0.75 * C2 * Rg * Math.pow(d50, 3.0);

        // Calculate the denominator: C1 * nu + sqrt(termInsideSqrt)
        double denominator = C1 * KINVISCOSITY_WATER + Math.sqrt(termInsideSqrt);

        // Calculate the settling velocity
        return numerator / denominator;
    }

    /**
     * Settling velocity, Zanke
     * 
     * @param d50 [m]
     * @return settling velocity [m/s]
     */
    final public static double getWC_Zanke(double d50) {
        double D = d50 * Math.pow(PhysicalParameters.G * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);

        return 11. * KINVISCOSITY_WATER / 0.7 * (Math.sqrt(1. + 0.01 * D * D * D) - 1.);
    }

    /**
     * Settling velocity, Dietrich
     * 
     * @param d50 [m]
     * @return settling velocity [m/s]
     */
    final public static double getWC_Dietrich(double d50) {
        double D = d50 * Math.pow(PhysicalParameters.G * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER)
                / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);

        double r1 = -3.76715 + 5.78832 * Math.log(D) - 0.88335 * Math.pow(Math.log(D), 2.)
                - 0.15525 * Math.pow(Math.log(D), 3.) + 0.04536 * Math.pow(Math.log(D), 4.);
        // System.out.println("r1="+r1);
        double csf = 0.7;
        double r2 = 1. - ((1. - csf) / 0.85);
        // System.out.println("r2="+r2);
        double p = 5.;
        double r3 = Math.pow(0.65 - (csf / 2.83 * Math.tanh(3. * Math.log(D) - 4.6)), 1 + (3.5 - p) / 2.5);
        // System.out.println("r3="+r3);
        double w_Star = r2 * r3 * Math.pow(10., r1);
        return Math.pow(
                w_Star * PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.G
                        * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER,
                1. / 3.);
    }

    // Bestimmung der Duenenparameter - bei Korndurchmessern kleiner 0,2 mm (0.2E-3
    // m) koennen keine Duenen (Grossformen) existieren (Zanke 1982)
    // dune hight based on the empirical formula according to van Rijn 1985 bzw.
    // 1993
    public static double getvanRijnDuneHeight(double d50, double taub, double depth, double bottomslope) {
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
        final double tauc = SedimentProperties.CriticalShieldsFunction.VanRijn.getCFS(D);
        final double T = (taub - tauc) / tauc;
        if (d50 <= 0.2E-3 || T >= 25. || T <= 0.)
            return 0.;
        if (depth > 0.1) {
            final double lambda = Function.min(1., (d50 - 0.2E-3) / 0.2E-3);
            return lambda
                    * Function.max(0.,
                            0.11 * depth * Math.pow(d50 / depth, 0.3) * (1. - Math.exp(-0.5 * T)) * (25. - T))
                    / bottomslope / bottomslope;
        }
        return 0.;
    }

    final double getvanRijnDuneHeight(double taub, double totaldepth) {
        double tbcr = CSF
                * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * d50);
        final double T = (taub - tbcr) / tbcr;
        if (d50 <= 0.2E-3 || T >= 25. || T <= 0.)
            return 0.;
        if (totaldepth > 0.1) {
            final double lambda = Function.min(1., (d50 - 0.2E-3) / 0.2E-3);
            return lambda
                    * (0.11 * totaldepth * Math.pow(d50 / totaldepth, 0.3) * (1. - Math.exp(-0.5 * T)) * (25. - T))
                    / bottomslope / bottomslope;
        }
        return 0.;
    }

    public static double getvanRijnDuneLength(double d50, double taub, double depth, double bottomslope) {
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
        final double tauc = SedimentProperties.CriticalShieldsFunction.VanRijn.getCFS(D)
                * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * d50);
        final double T = (taub - tauc) / tauc;
        if (d50 <= 0.2E-3 || T >= 25. || T <= 0.)
            return 0.;
        if (depth > 0.1) {
            final double lambda = Function.min(1., (d50 - 0.2E-3) / 0.2E-3);
            return lambda * 7.3 * depth / bottomslope / bottomslope;
        }
        return 0.;
    }

    final public double getvanRijnDuneLength(double taub, double depth) {
        double tbcr = CSF
                * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * d50);
        final double T = (taub - tbcr) / tbcr;
        if (d50 <= 0.2E-3 || T >= 25. || T <= 0.)
            return 0.;
        if (depth > 0.1) {
            final double lambda = Function.min(1., (d50 - 0.2E-3) / 0.2E-3);
            return lambda * 7.3 * depth / bottomslope / bottomslope;
        }
        return 0.;
    }

    public static double getYalinDuneHeight(double d50, double taub, double totaldepth, double bottomslope) {
        final double dmin = 0.065E-3; // Peter 22.01.2025
        final double dmaxfinesand = 0.2E-3; // Peter 22.01.2025
        if (d50 <= dmin)
            return 0.;
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
        final double tauc = SedimentProperties.CriticalShieldsFunction.Shields.getCFS(D)
                * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * d50);
        if (taub > tauc) {
            final double lambda = Function.min(1., (d50 - dmin) / (dmaxfinesand - dmin));
            return lambda * totaldepth / 6. * Function.max(0., 1. - tauc / taub) / bottomslope / bottomslope;
        } else
            return 0.;
    }

    final public double getYalinDuneHeight(double taub, double totaldepth) {
        final double dmin = 0.065E-3; // Peter 22.01.2025
        final double dmaxfinesand = 0.2E-3; // Peter 22.01.2025
        if (d50 <= dmin)
            return 0.;
        final double tbcr = CSF
                * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * d50);
        if (taub > tbcr) {
            final double lambda = Function.min(1., (d50 - dmin) / (dmaxfinesand - dmin));
            return lambda * totaldepth / 6. * Function.max(0., 1. - tbcr / taub) / bottomslope / bottomslope;
        }
        return 0.;
    }

    public static double getYalin80DuneHeight(double d50, double taub, double totaldepth, double bottomslope) {
        if (d50 < 0.2E-3)
            return 0.;
        final double lambda = Function.min(1., Function.max(0, d50 - 0.2E-3) / 0.2E-3);
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.);
        final double tauc = SedimentProperties.CriticalShieldsFunction.Shields.getCFS(D)
                * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * d50);
        if (taub > tauc)
            return lambda * totaldepth / 0.023 * Function.max(0., taub / tauc - 1)
                    * Math.exp(1. - Function.max(0., taub / tauc - 1) / 12.84) / bottomslope / bottomslope;
        else
            return 0.;
    }

    final public double getYalin80DuneHeight(double taub, double totaldepth) {
        if (d50 < 0.2E-3)
            return 0.;
        double tbcr = CSF
                * ((PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) * PhysicalParameters.G * d50);
        if (taub > tbcr) {
            final double lambda = Function.min(1., Function.max(0, d50 - 0.2E-3) / 0.2E-3);
            return lambda * totaldepth / 0.023 * Function.max(0., taub / tbcr - 1)
                    * Math.exp(1. - Function.max(0., taub / tbcr - 1) / 12.84) / bottomslope / bottomslope;
        } else
            return 0.;
    }

    final public static double getYalinDuneLength(double duneHight) {
        return 36. * duneHight;
    }

    // dune length based on Flemming, B.W. 1988
    public static double getFlemmingDuneLength(double duneHight) {
        return 27.8 * Math.pow(duneHight, 1. / 0.8098);
    }

    // based on Soulsby and Whitehouse (2005) valid between D > 1.58 and D < 14.
    /**
     *
     * @param d50
     * @param theta Shields bed shear stress
     * @return
     */
    public static double getRippleHight(double d50, double theta) {
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.); // dimensionsloser
                                                                                                             // Teilchendurchmesser
        double theta_wo = 0.916;
        if (D > 1.58)
            theta_wo = 1.66 * Math.pow(D, -1.3);
        double theta_sf = 1.25;
        if (D > 1.58)
            theta_sf = 2.26 * Math.pow(D, -1.3);
        final double theta_c = SedimentProperties.CriticalShieldsFunction.Soulsby.getCFS(D);
        // maximum ripple height
        final double mrh = d50 * 202 * Math.pow(D, -.554);
        // equilibrium ripple height
        if (theta_c < theta && theta < theta_wo)
            return mrh;
        if (theta_wo < theta && theta < theta_sf)
            return mrh * (theta_sf - theta) / (theta_sf - theta_wo);
        if (theta_sf < theta)
            return 0.;

        return mrh; // theta<theta_c - kein transport, kein wachsen oder zerfallen von Rippeln
    }

    public static double getRippleLength(double d50) {
        final double D = d50 * Math.pow(PhysicalParameters.G
                * (PhysicalParameters.RHO_SEDIM - PhysicalParameters.RHO_WATER) / PhysicalParameters.RHO_WATER
                / (PhysicalParameters.KINVISCOSITY_WATER * PhysicalParameters.KINVISCOSITY_WATER), 1. / 3.); // dimensionsloser
                                                                                                             // Teilchendurchmesser
        return d50 * (500 + 1881 * Math.pow(D, -1.5));
    }

    // public static void main(String[] args) {
    // double d50 = .001000E-3;
    //
    // System.out.println("** Porosity **");
    // System.out.println(getPorosityKomuraSimmons(d50));
    // System.out.println(getPorosityKomuraWuWang(d50));
    // System.out.println(getPorosity_phi50(d2phi(d50*1E3)));
    // System.out.println(getPorosityLogistic(d50)+"\t Logistic");
    ////        System.out.println(getPorosity(d50,1));
    //
    //// System.out.println(getRippleHight(d50,
    /// 1E-5)); System.out.println(getRippleLength(d50));
    //
    ////        System.out.println(getInnerFrictionAngle(d50));
    //
    // System.out.println("\n ** Sinkgeschwindigkeit **");
    // System.out.println(getWC_Stokes(d50));
    // System.out.println(getWC_Oseen(d50)+"\t Oseen");
    // System.out.println(getWC_Dietrich(d50));
    // System.out.println(getWC_Zanke(d50));
    // System.out.println(getWC_WuAndWang(d50)+"\t WuAndWang");
    //// ScalarFunction1d sf = new ShieldsStress(); ScalarFunction1d isf = new
    /// InverseCriticalShieldsFunction(); double csf = sf.getValue(d50);
    //// System.out.println(csf); System.out.println(isf.getValue(csf));

    //
    ////        System.out.println(getPorosity(0.42E-3));
    //
    // }

    public static double getParticleReynoldsNumber(double d50, CurrentModel2DData cmd) {

        // Friction velocity
        // final double d = Function.max(CurrentModel2D.WATT, cmd.totaldepth);
        // double Cs = 18.*Math.log(12.*d/d50);
        // final double uStar = cmd.cv * PhysicalParameters.sqrtG / Cs;
        // final double uStar = Math.sqrt(cmd.grainShearStress/cmd.rho);

        // Chezy coefficient
        final double Cs = 2.5 /* PhysicalParameters.sqrtG */ * Math.log(4. * cmd.totaldepth / d50 + 1.);
        // // Friction velocity
        final double uStar = cmd.cv /* PhysicalParameters.sqrtG */ / Cs;

        return uStar * d50 / PhysicalParameters.KINVISCOSITY_WATER;

    }

    @Override
    public SedimentModel2DData clone() throws CloneNotSupportedException {
        SedimentModel2DData rvalue = (SedimentModel2DData) super.clone(); // To change body of generated methods, choose
                                                                          // Tools | Templates.

        rvalue.sC = sC; // in m^2/m^3
        rvalue.sedimentSource = sedimentSource;
        rvalue.dsCdt = dsCdt;

        rvalue.qsx = qsx;
        rvalue.qsy = qsy; // suspendet sedimenttransport qs=sC*totaldepth*cv
        rvalue.u = u;
        rvalue.v = v; // tiefenintegrierte Geschwindigkeiten, werden durch sekundaerstroemung
                      // angepasst

        rvalue.bedload = bedload; // bedload sedimenttransport
        rvalue.bedloadVector = new double[] { bedloadVector[0], bedloadVector[1] };

        rvalue.u_bank = u_bank;
        rvalue.v_bank = v_bank;

        rvalue.lambdaQs = lambdaQs; // Indikator fuer Sedimenttransport am Knoten: 0 kein Transport, 1 relevanter
                                    // Transport

        rvalue.bottomslope = bottomslope;
        rvalue._bottomslope = _bottomslope; // temporary value

        rvalue.z = z; // [m] Lage des Gewaesserbodens als Tiefe
        rvalue.dzdt = dzdt;

        rvalue.zh = zh; // [m] nichterodierbarar Tiefenhorizont als Tiefe
        rvalue.maintainedDepth = maintainedDepth; // mindestens zu haltende Fahrwasser-Tiefe
        rvalue.bound = bound; // Grenzschichtdicke von 10 Koernern ab denen Wattstrategie und Strategie fuer
                              // nicht erodierbare Horizonte anschlaegt
        rvalue.lambda = lambda; // increasing factor depending on not erodible bottom
        rvalue.d50init = d50init;

        rvalue.innerFrictionAngle = innerFrictionAngle;

        // boudary conditions
        rvalue.bconc = bconc; // boundary condition for sediment concenration
        rvalue.bz = bz; // boundary condition for bottom
        rvalue.bd50 = bd50; // boundary condition for d50
        rvalue.extrapolate_z = extrapolate_z; // indicator for extrapolating - for boundary nodes without boundary
                                              // condistions
        rvalue.extrapolate_conc = extrapolate_conc; // indicator for extrapolating - for boundary nodes without boundary
                                                    // condistions

        rvalue.D = D; // dimensionsloser Teilchendurchmesser
        rvalue.CSF = CSF;
        rvalue.rC = rC;
        rvalue.rZTransport = rZTransport;

        rvalue.duneHeight = duneHeight;
        rvalue.duneLengthX = duneLengthX;
        rvalue.duneLengthY = duneLengthY;

        return rvalue;
    }
}