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
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;
import bijava.math.ifunction.*;
import de.smile.marina.PhysicalParameters;
import static de.smile.marina.PhysicalParameters.G;
import de.smile.marina.fem.model.ground.Pair;
import de.smile.math.Function;

/**
 * @author Peter Milbradt
 * @version 4.4.7
 */
public class WaveHYPModel2DData implements ModelData{
    
    private static int id = NO_MODEL_DATA;  
    private static final long serialVersionUID = 1L;
    
    // Zustandsgroessen
    // ----------------
    double kres; // wavenumber: 2*pi / wavelength in [1/m]
    double kx,    rkx,     dkxdt;
    double ky,    rky,     dkydt;
    public double wa, rwa, dwadt;    // Wave Amplitude in [m]
    double sigma, rsigma,  dsigmadt; // relative Winkelgeschwindigkeit: 2*pi / waveperiod in [1/s]
    double sigmaA; // absolute Winkelgeschwindigkeit = sigma + k*u in [1/s]
    double c; // wave velocity in [m/s]
    double cgx, cgy, cg; // absolute Gruppengeschwindigkeit und -skomponenten cgR*kx/k + u in [m/s]
    double cgxR, cgyR, cgR; // relative Gruppengeschwindigkeit und -skomponenten cgR*kx/k in [m/s]
    double wlambda, w1_lambda;
    
    double extkx, extky, extsigma, extwa; // extrapolated values
    int waAnz=0, sigmaAnz=0; // zaehlen der aufsummierten Werte: extkx, extky, extsigma, extwa
    boolean extrapolate_wa, extrapolate_kx, extrapolate_ky, extrapolate_sigma;
    
    double ubwave, vbwave, bottomvelocity;  // Wave induced maximal bottomvelocity [m/s]
    // Wave induced mean bottomvelocity  = maximal bottomvelocity / sqrt(2)
    
    double bfcoeff; // bottomfriction coefficient
    double taubX, taubY; // Wave induced maximal bottomshearstress [N/m**2]  // Peter 27,01.25
    // Wave induced mean bottomshearstress = maximal bottomshearstress / 2.
    
    // Radiationstresses
    public double sxx = 0.;
    public double sxy = 0.;
    public double syy = 0.;
    
    //energydissipation by Wavebreaking
    public double epsilon_b=0.;
    
    // energy input by wind
    double windchangeperiode, windchangeamplitude, windchangekx, windchangeky;
    
    // boudary conditions
    // ------------------
    ScalarFunction1d bkx      = null;
    ScalarFunction1d bky      = null;
    ScalarFunction1d btheta   = null;
    ScalarFunction1d bsintheta      = null;
    ScalarFunction1d bcostheta      = null;
    ScalarFunction1d bwa      = null;
    ScalarFunction1d bsigma   = null;

    int ibound=0;
    
    int anz_activ_el=0;
    
    public WaveHYPModel2DData(){
        id = SEARCH_MODEL_DATA;
    }
    
    public static WaveHYPModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof WaveHYPModel2DData waveHYPModel2DData) {
                    id = dof.getIndexOf(md);
                    return waveHYPModel2DData;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (WaveHYPModel2DData) dof.getModelData(id);
        }
        return null;
    }
    /**  Resio et al. (2003)
     * 
     * @param windSpeed in 10m Hight [m/s]
     * @param fetchLength [m]
     * @return Pair of signifcant wave and peak period
     */
    public static Pair<Double,Double> getFetchBased(double windSpeed, double fetchLength){
        final double ufPOW2 = 0.001*(1.1 + 0.035*windSpeed)*windSpeed*windSpeed;
        var wh = Math.min(211.5, 0.0413*Math.sqrt(G*fetchLength/ufPOW2))*ufPOW2/G;
        var period = Math.min(239.8, 0.651*Math.cbrt(G*fetchLength/ufPOW2))*Math.sqrt(ufPOW2)/G;
        return new Pair<>(wh, period);
}
    
    /**  Resio et al. (2003)
     * 
     * @param windSpeed in 10m Hight [m/s]
     * @param fetchLength [m]
     * @return WaveEnergy [m**2] bzw. [1.01*10**4 J/m**2]
     */
    public static double getFetchBasedWaveEnergy(double windSpeed, double fetchLength){
        final double ufPOW2 = 0.001*(1.1 + 0.035*windSpeed)*windSpeed*windSpeed;
        var wh = Math.min(211.5, 0.0413*Math.sqrt(G*fetchLength/ufPOW2))*ufPOW2/G;
//        var period = Math.min(239.8, 0.651*Math.pow(G*fetchLength/ufPOW2,1./3.))*Math.sqrt(ufPOW2)/G;
//        return 0.5 * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wh*wh * period;
        return 1./8. * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wh*wh;
    }
    
    /** *   Resio et al.(2003)
     * 
     * @param windSpeed in 10m Hight [m/s]
     * @param fetchLength [m]
     * @param depth [m]
     * @return Pair of signifcant wave and peak period
     */
    public static Pair<Double,Double> getFetchBasedDepthRestricted(double windSpeed, double fetchLength, double depth){
        final double ufPOW2 = 0.001*(1.1 + 0.035*windSpeed)*windSpeed*windSpeed;
        var wh = Math.min(211.5, 0.0413*Math.sqrt(G*fetchLength/ufPOW2))*ufPOW2/G;
        var period = Math.min(9.78*Math.sqrt(depth/G), Math.min(239.8, 0.651*Math.cbrt(G*fetchLength/ufPOW2))*Math.sqrt(ufPOW2)/G);
        return new Pair<>(wh, period);
    }
    /** Significant Wave Height from Bretschneider Empirical Relationships Solution
     * 
     * @param windSpeed in 10m Hight [m/s]
     * @param fetchLength [m]
     * @return WaveHight [m]
     */
    public static double getFetchBasedSignificantWaveHight(double windSpeed, double fetchLength){ 
        return windSpeed*windSpeed*0.283*Math.pow(Math.tanh(0.0125*((G*fetchLength)/windSpeed/windSpeed)),0.42)/G;
    }
    /** wave energy flux, which is the mean transport rate of the wave energy through a vertical plane of unit width parallel to a wave crest in watts per meter (W/m)
     * 
     * @return wave energy flux [W/m]
     */
    public double getWaveEnergyFlux(){
        return getWaveEnergy() * cg;
    }
    /** wave energy flux, which is the mean transport rate of the wave energy through a vertical plane of unit width parallel to a wave crest in watts per meter (W/m)
     * 
     * @param wh significant wave height in meters [m]
     * @param T period of the wave in seconds [s]
     * @return wave energy flux [W/m]
     */
    public static double getWaveEnergyFlux(double wh, double T){
        return  PhysicalParameters.RHO_WATER * PhysicalParameters.G * PhysicalParameters.G * T * wh*wh / (64 * Math.PI);
    }
    
    /** total wave energy also called WaveEnergyDensity
     * 
     * @return [m**2] bzw. [1.01*10**4 J/m**2]
     */
    public double getWaveEnergy(){
        return 1./2. * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wa*wa;
    }

    /** energy per surface unit also called WaveEnergyDensity
     * 
     * @param wa wave amplithude [m]
     * @return [m**2] bzw. [1.01*10**4 J/m**2]
     */
    public static double getWaveEnergy(double wa){
        return 1./2. * PhysicalParameters.RHO_WATER * PhysicalParameters.G * wa*wa;
    }
    /**
     * 
     * @return [J*s] or [m**2 / s]
     */
    public double getWaveActionDensity(){
        return getWaveEnergy()* sigma;
    }
}
