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
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import static de.smile.marina.fem.model.hydrodynamic.dim2.SurfaceWaterModel.halfWATT;
import de.smile.marina.fem.model.hydrodynamic.dim2.weirs.TimeDependentWeir;
import de.smile.math.Function;
import static de.smile.math.Function.sqr;
import static java.lang.Math.abs;
import static java.lang.Math.log10;
import static java.lang.Math.sqrt;
import java.util.*;

/**
 * ModelDatas for shallow water equations
 *
 * @version 3.24.X zusaetzliche Variablen fuer die getrennte Berechnung der Residuumkorrektur
 * @author Peter Milbradt
 */
public class CurrentModel2DData extends SurfaceWaterModelData {

    private static int id = NO_MODEL_DATA;  
    private static final long serialVersionUID = 1L;
    
    // Zustandsgroessenen
    public double u = 0., dudt = 0.;       // velocity in x-direction
    public double v = 0., dvdt = 0.;       // velocity in y-direction
    public double cv = 0.;           // norm of the velocity

    public double _dhdx = 0., _dhdy = 0., dhdx = 0., dhdy = 0.;    // wird nur fuer UnderFlowTopoWeir verwendet TODO elementieren?

    public double rho = PhysicalParameters.RHO_WATER_0;  // waterdensety by temperature 4 Grad C
    public double temp = 4.;      // [Grad C]
    
    double bottomFrictionCoefficient; // Reibungsbeiwert oder Reibungskoeffizient der die Stroemung bremst
    double _tau_bx_extra, _tau_by_extra;  // temporary for elementdependent extra parts of bottom shear stress (secondary flow)
    public double tau_bx_extra, tau_by_extra;  // for elementdependent extra parts of bottom shear stress (secondary flow)

    public double tau_windx, tau_windy;     // wind stress koeffizient

    // Zwischenergebnisse der rechte Seite
    double ru = 0., rv = 0., reta = 0.;
    double ruCorrection = 0., rvCorrection = 0., retaCorrection = 0.;
    double duCdt, dvCdt, detaCdt;

    // boudary conditions
    public ScalarFunction1d sourceQ = null;
    public ScalarFunction1d sourceh = null;
    public double source_dhdt = 0.;
    public TimeDependentWeir bWeir = null;

    public CurrentModel2DData(DOF dof) {
        super(dof);
        id = SEARCH_MODEL_DATA;
    }

    public static CurrentModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof CurrentModel2DData currentModel2DData) {
                    id = dof.getIndexOf(md);
                    return currentModel2DData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (CurrentModel2DData) dof.getModelData(id);
        }
        return null;
    }

    /**
     *
     * @param ks in m
     * @return in m**1/3 / s
     */
    public static double Nikuradse2Strickler(double ks) { // http://www.baw.de/vip/abteilungen/wbk/Publikationen/scn/sc1-99a/node21.htm
        return 25. / Math.pow(ks, 1. / 6.);
    }

    /**
     *
     * @param kst in m**1/3 / s
     * @return in m
     */
    public static double Strickler2Nikuradse(double kst) { // http://www.baw.de/vip/abteilungen/wbk/Publikationen/scn/sc1-99a/node21.htm
        return Math.pow(25. / kst, 6.);
    }

    /**
     * Naeherungsformeln von Nikuradse zur Schaetzung des dimensionslose
     * Reibungsfaktor ? im Darcy-Weisbach-Gesetz direkt aus der
     * absoluteRauheitshoehe Rz und dem hydraulischen Durchmesser D (depth) gilt
     * nur fuer glatte Oberflaechen
     *
     * @param Rz absoluteRauheitshoehe [m]
     * @param depth Wassertiefe [m]
     * @return dimensionslose Reibungsfaktor lambda im Darcy-Weisbach-Gesetz
     */
    public static double absoluteRauheitshoehe2DarcyWeisbachLambda_by_Nikuradse(double Rz, double depth) {
        final double nonZeroDepth = Math.max(1.E-10, depth);
        return 1. / sqr(0.85 * Math.log10(Rz / nonZeroDepth) + 1.74);
    }

    /**
     * Bestimmung des dimensionslose Reibungsfaktor lambda im
     * Darcy-Weisbach-Gesetz direkt aus der absoluteRauheitshoehe Rz und dem
     * hydraulischen Durchmesser D (depth)
     *
     * @param Rz absoluteRauheitshoehe [m]
     * @param cv [m/s]
     * @param depth Wassertiefe [m]
     * @return dimensionslose Reibungsfaktor lambda im Darcy-Weisbach-Gesetz
     */
    public static double absoluteRauheitshoehe2DarcyWeisbachLambda(double Rz, double cv, double depth) {
        final double nonZeroDepth = Math.max(1.E-10, depth);
        final double Re = getReynoldsNumber(cv, depth);
        double oneOverSqrtLambda = 1. / sqrt(absoluteRauheitshoehe2DarcyWeisbachLambda_by_Nikuradse(Rz, nonZeroDepth));
        boolean status;
        do {
            final double oldOneOverSqrtLambda = oneOverSqrtLambda;
            oneOverSqrtLambda = -2.0 * log10((Rz / nonZeroDepth) / 3.7 + 2.51 / Re * oldOneOverSqrtLambda);
            final double diff = oldOneOverSqrtLambda - oneOverSqrtLambda;
            System.out.println(diff);
            status = abs(diff) > 1.E-10;
        } while (status);

        return 1. / oneOverSqrtLambda;
    }

    /**
     * Bestimmung des dimensionslose Reibungsfaktor lambda im
     * Darcy-Weisbach-Gesetz direkt aus der absoluteRauheitshoehe Rz
     *
     * @param Rz absoluteRauheitshoehe [m]
     * @return dimensionslose Reibungsfaktor lambda im Darcy-Weisbach-Gesetz
     */
    public double getDarcyWeisbachLambdafromAbsoluteRoughnessHeight(double Rz) {
        return absoluteRauheitshoehe2DarcyWeisbachLambda(Rz, cv, totaldepth);
    }

    public static double DarcyWeisbachLambda2ManningStrickler(double lambda, double totalDepth) {
        final double nonZeroDepth = Math.max(1.E-10, totalDepth);
        final double nonZeroLambda = Math.max(1.E-10, lambda);
        return sqrt(8 * PhysicalParameters.G / nonZeroLambda / Math.cbrt(nonZeroDepth));
    }

    /**
     * calculate the dimensionless Darcy-Weisbach friction factor From the Chezy
     * coefficient according to Silberman et al.(1963)
     *
     * @param C
     * @return lambda
     */
    public static double Chezy2DarcyWeisbachLambda(double C) {
        return 8 * PhysicalParameters.G / C / C;
    }

    public static void main(String... args) {
        final double depth = 10.;
        final double d50 = 1.23E-3; // in [m] entspricht 0.5 * mittleren Rauheitshoehe
        final double Ra = 0.5 * d50;
        final double Rz = 6.4 * Ra;
//        System.out.println(DarcyWeisbachLambda2ManningStrickler(absoluteRauheitshoehe2DarcyWeisbachLambda(1.E-3, .1, depth), depth));
        System.out.println(absoluteRauheitsHoehe2Strickler(Rz, depth));
        System.out.println(Nikuradse2Strickler(3. * d50));
        System.out.println(Nikuradse2Strickler(3. * d50, depth));
    }

    /**
     * absoluter Rauheitshoehe Rz [mm] nach Strickler-Beiwert k_ST [m^(1/3/s]
     * umrechnen in Anlehnung an DIN EN 752-4
     *
     * @param Rz absolute Rauheitshoehe
     * @param depth Tiefe / im original hydraulischer Radius
     * @return Strickler-Beiwert [m^(1/3)/s] aus dem Inervall [1,Unendlich]
     */
    public static double absoluteRauheitsHoehe2Strickler(double Rz, double depth) {
        final double nonZeroDepth = Math.max(1.E-3, depth);
        final double nonZeroRz = Math.max(1.E-6, Rz);
        return Math.max(1., 17.72 / Math.pow(nonZeroDepth, .1 / 6.) * Math.log10(14.84 * nonZeroDepth / nonZeroRz));
    }

    /**
     * absoluteRauheitsbeiwert k_s [m] nach Strickler-Beiwert k_ST [m^(1/3/s]
     * umrechnen in Anlehnung an DIN EN 752-4
     *
     * @param k_s Nikuradse-Beiwert [m]
     * @param depth Tiefe / im original hydraulischer Radius
     * @return Strickler-Beiwert [m^(1/3)/s] aus dem Inervall [1,Unendlich]
     */
    public static double Nikuradse2Strickler(double k_s, double depth) {
        final double nonZerok_s = Math.max(1.E-6, k_s); // 1 mym
        if(depth<0.1){
            return Nikuradse2Strickler(k_s);
        }else if(depth<0.5){
            double lambda = (0.5-depth)/0.4;
            return lambda * Nikuradse2Strickler(k_s) + (1-lambda) * Math.max(1., 17.72 / Math.pow(depth, .1 / 6.) * Math.log10(14.84 * depth / nonZerok_s));
        }
        return Math.max(1., 17.72 / Math.pow(depth, .1 / 6.) * Math.log10(14.84 * depth / nonZerok_s));
    }

    /**
     * absoluter Rauheitshoehe Rz [m] in absoluteRauheitsbeiwert k_s umrechnen
     *
     * @param Rz absolute Rauheitshoehe [m]
     * @param depth
     * @return absoluteRauheitsbeiwert [m]
     */
    public static double absoluteRauheitsHoehe2absoluteRauheitsbeiwert(double Rz, double depth) {
        final double nonZeroDepth = Math.max(1.E-10, depth);
        return Rz / nonZeroDepth;
    }

    public static double Nikuradse2Taylor(double ks, double depth) {
        return PhysicalParameters.G / sqr(18. * Math.log(12 * depth / ks));
    }

    public CurrentModel2DData() {
        super();
        id = SEARCH_MODEL_DATA;
    }

    synchronized final void setWaterLevel_synchronized(double h) {  // synchronized notwendig in setBoundaryCondition, da beim glaetten der Wasserspiegellagen an Raendern auch benachbarte Knotenwerte veraendert werden
        if ((this.z + h) <= 0.) {
            this.eta = -this.z;
            this.cv = this.u = this.v = 0.;
            this.totaldepth = 0.;
            this.wlambda = 0;
            this.w1_lambda = 1.;
        } else {
            this.eta = h;
            this.totaldepth = this.z + this.eta;
            this.wlambda = Function.min(1., this.totaldepth / CurrentModel2D.WATT);
            this.w1_lambda = 1. - this.wlambda;
            // Froude - kostet viel Zeit, ist aber bei Dammbruchsimulationen notwendig 
            if (this.totaldepth > halfWATT / 10.) {
                final double cv2 = this.u * this.u + this.v * this.v;
                final double vg = PhysicalParameters.G * this.totaldepth;
                final double froud = Math.sqrt(cv2 / vg) / (1. + 2. * Math.min(1., this.totaldepth)); // in 1m tiefem Wasser darf die Froud-Zahl bis 2 gehen
                if (froud > 1.) {
                    this.u /= froud;
                    this.v /= froud;
                }
            }else{
                this.u *= this.wlambda;
                this.v *= this.wlambda;
            }
        }
    }

    public final void setWaterLevel(double h) {
        if ((this.z + h) <= 0.) {
            this.eta = -this.z;
            this.cv = this.u = this.v = 0.;
            this.totaldepth = 0.;
            this.wlambda = 0;
            this.w1_lambda = 1.;
        } else {
            this.eta = h;
            this.totaldepth = this.z + this.eta;
            this.wlambda = Function.min(1., this.totaldepth / CurrentModel2D.WATT);
            this.w1_lambda = 1. - this.wlambda;
            // Froude - kostet viel Zeit, ist aber bei Dammbruchsimulationen notwendig 
            if (this.totaldepth > halfWATT / 10.) {
                final double cv2 = this.u * this.u + this.v * this.v;
                final double vg = PhysicalParameters.G * this.totaldepth;
                final double froud = Math.sqrt(cv2 / vg) / (1. + 2. * Math.min(1., this.totaldepth)); // in 1m tiefem Wasser darf die Froud-Zahl bis 2 gehen
                if (froud > 1.) {
                    this.u /= froud;
                    this.v /= froud;
                }
            }else{
                this.u *= this.wlambda;
                this.v *= this.wlambda;
            }
        }
    }

    public final void setBottomLevel(double z) {
        this.z = z;
        this.setWaterLevel(this.eta);
    }

    public static double getReynoldsNumber(double cv, double totaldepth) {
        return PhysicalParameters.RHO_WATER_10 * cv * totaldepth / PhysicalParameters.DYNVISCOSITY_WATER;
    }

    public final double getReynoldsNumber() {
        return rho * cv * totaldepth / PhysicalParameters.DYNVISCOSITY_WATER;
    }

    public static double getWhiteColebrookCoeffizientfromMeanRoughnessHeight(double Ra, double cv, double totaldepth) {
        return 0.25 / sqr(Math.log10(Ra / 3.7 + 5.74 / Math.pow(getReynoldsNumber(cv, totaldepth), 0.9)));
    }

    public final double getWhiteColebrookCoeffizientfromMeanRoughnessHeight(double Ra) {
        return getWhiteColebrookCoeffizientfromMeanRoughnessHeight(Ra, cv, totaldepth);
    }

    public static double getWhiteColebrookCoeffizientfromAbsoluteRoughnessHeight(double Rz, double cv, double totaldepth) {
        return 2 * Math.log10(sqrt((Rz / 3.7) + 2.51 / sqrt(getReynoldsNumber(cv, totaldepth)))) - 2;
    }

    public double getWhiteColebrookCoeffizientfromAbsoluteRoughnessHeight(double Rz) {
        return getWhiteColebrookCoeffizientfromAbsoluteRoughnessHeight(Rz, cv, totaldepth);
    }

    public static double getWhiteColebrookCoeffizientfromAbsoluteRoughnessHeight_V2(double Rz, double cv, double totaldepth) {
        final double Re = getReynoldsNumber(cv, totaldepth);
        return 1. / sqrt(Re) * (1. / sqrt(Rz) + 2. / sqrt(Re));
    }

    public static double WhiteColebrookCoeffizient2ManningStrickler(double cf) {
        return 1. / cf / cf;
    }
}
