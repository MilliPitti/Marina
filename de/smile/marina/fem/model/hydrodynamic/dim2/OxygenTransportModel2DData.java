package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.ModelData;
import java.util.*;
import bijava.math.ifunction.*;

/** @author Peter Milbradt
 * @version 2.7.17
 */
class OxygenTransportModel2DData implements ModelData {
    
    /* Der Saettigungswert betraegt bei 0 gradC 14,6 mg O2/l (a. d.) und sinkt bei 20 gradC auf 9,1 mg O2/l (a. d.) bei 1013,25 hPa Luftdruck.*/

    private static int id = NO_MODEL_DATA;    // // Zustandsgroessen
    private static final DiscretScalarFunction1d oxygenTemperatureFunction = new DiscretScalarFunction1d(new double [][] {{0.,14.16}, {1.,13.77}, {2.,13.40}, {3.,13.05}, {4.,12.70}, {4.,12.70}, {5, 12.37}, {6.,12.06}, {6.,12.06}, {7.,11.76}, {8.,11.47}, {9.,11.19},
            {10.,10.92}, {11.,10.67}, {12., 10.43}, {13.,10.20}, {14.,9.98}, {15.,9.76}, {15., 9.76}, {16.,9.56}, {17.,9.37}, {18.,9.18}, {19.,9.01},  
            {20.,8.84}, {21.,8.68}, {22.,8.53}, {23.,8.38}, {24.,8.25}, {25.,8.11}, {26.,7.99}, {27.,7.86}, {28.,7.75}, {29.,7.64}, 
            {30.,7.53}, {31.,7.42}, {32.,7.32}, {33., 7.22}, {34.,7.13}, {35.,7.04}, {36.,6.94}, {37.,6.86}, {38.,6.76}, {39.,6.68}, {40., 6.59}, {100., 0} });
    double oxygenConc;    // OxygenConcentration in mg/l
    double rOxygenConc;
    
    public double sourceSink=0; // quellen und Senken des Sauerstoffgehaltes in mg/l/s
    
    // boudary conditions
    ScalarFunction1d bsc = null;
    ScalarFunction1d sourceQc = null;
    double doxygenconcdt;
    boolean extrapolate = false;
    double waterTemperature = 10.;

    public OxygenTransportModel2DData() {
        id = SEARCH_MODEL_DATA;
    }

    public static OxygenTransportModel2DData extract(DOF dof) {
        if (id == NO_MODEL_DATA) {
            return null;
        }
        if (id == SEARCH_MODEL_DATA) {
            Iterator<ModelData> modeldatas = dof.allModelDatas();
            while (modeldatas.hasNext()) {
                ModelData md = modeldatas.next();
                if (md instanceof OxygenTransportModel2DData) {
                    id = dof.getIndexOf(md);
                    return (OxygenTransportModel2DData) md;
                }
            }
            id = NO_MODEL_DATA;
        } else {
            return (OxygenTransportModel2DData) dof.getModelData(id);
        }
        return null;
    }
    /** get the Oxygen-Saturation depends on the water temperatur (gradC) 
     * [Truesdale, Downing und Lowden - J. Appl. Chem. 5 (1955). 53] 
     */
    public static double oxygenSaturation(double temperatur){
        return oxygenTemperatureFunction.getValue(temperatur);
    }
    public double oxygenSaturation(){
        return oxygenTemperatureFunction.getValue(waterTemperature);
    }
    
    private double getSaettingungsWert(DOF dof){
        CurrentModel2DData cmd = CurrentModel2DData.extract(dof);
        double kelvinT = cmd.temp+273.15; // Kopmann (1999)
        double rvalue=Math.exp(-139.3441+1.575701*Math.pow(10, 5)/kelvinT-6.642308*Math.pow(10, 7)/Math.pow(kelvinT, 2)+1.2438*Math.pow(10, 10)/Math.pow(kelvinT, 3)-8.621949*Math.pow(10, 11)/Math.pow(kelvinT, 4));

        SaltModel2DData smd = SaltModel2DData.extract(dof);
        if(smd!=null){
            rvalue *= (1-0.0048*smd.C); // Konzentration in g/l ? in Beziehung zu ppt
        }

        return rvalue;
    }
}
