/*
 * ZooplanktonDat.java
 *
 * Created on 6. November 2006, 12:04
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.smile.marina.fem.model.ecological;

import de.smile.marina.io.SmileIO;

/**
 *
 * @author abuabed
 */
public class ZooplanktonDat {
    public String zoo_rndwerteName = null;
    public String xferg_name = "zooplanktonerg.bin";
    
    public int numberOfThreads = 1;
    
       public String startWerteDatei=null;  // erg-file with start results
    public int startSatz=0;
    
    public String concentration_name = null; // ascii-file from type system.dat with initial waterlevel
    public SmileIO.MeshFileType concentrationFileType = SmileIO.MeshFileType.SystemDat;
    
    //Ecological parameter for zooplankton
    /** 
     * -Reference temperature under optimal, non-limiting, light
     *  and nutrients availability.
     */
    public static double REFERENCE_TEMPERATURE = 20.0;
    /**
     *  -Respiration, essudation and natural mortality rate at a reference temperature
     */
    private static double RES_ESSUD_REFERENCE_RATE = 0.015;//(24*60*60);//[1/d]/(24*60*60)--->[1/sec]
    public static double MORTAL_REFERENCE_RATE = 0.03;//(24*60*60);//[1/d]/(24*60*60)--->[1/sec]
//    private static double RES_ESSUD_MORTAL_REFERENCE_RATE = (0.001 + 0.2)/(24*60*60);//[1/d]/(24*60*60)--->[1/sec]
    /**
     *  -Background concentration
     */
    private static double BACKGROUND_CONCENTRATION = 0.01;
    
    /**
     *  -Phytoplankton predation rate
     *  Mass of phytoplankton per mass of predator (zooplankton) over time
     */
    private static double MAX_PHYTOPLANKTON_PREDATION_RATE = 0.35;//(24*60*60); //[1/d]/(24*60*60)--->[1/sec]
//    private static double MAX_PHYTOPLANKTON_PREDATION_RATE = 0.005/(24*60*60); //[1/d]/(24*60*60)--->[1/sec]
    /**
     *  -Ivlev Parameter
     */
    private static double IVLEV_PARAMETER = 1.2;
    /**
     *  -Filtration Parameter
     *  Accounts for filtration process (volume of water filtered per mass of zooplankton in time)
     */
    private static double FILTRATION_PARAMETER = 0.9; //[0.1-1.0]
    /**
     *  -Assimilation Parameter = 0.6
     *  accounts for assimilation of food
     */
    public static final double ASSIMILATION_PARAMETER = 0.6; //[Dimensionless]
    /**
     * -G: Loss velocity for predation (Zooplankton is the top level of the modelled food web,
     * therefor the loss velocity for predation will be constant) or included in the natural mortality
     * term
     */
    private static double GRAZING_LOSS = 0.0001;//(24*60*60); //[1/d]/(24*60*60)--->[1/sec]

    public double watt = 0.01;
    
    /** Creates a new instance of ZooplanktonDat */
    public ZooplanktonDat() {
    }

    public static double getMAX_PHYTOPLANKTON_PREDATION_RATE() {
        return MAX_PHYTOPLANKTON_PREDATION_RATE;
    }

    public static void setMAX_PHYTOPLANKTON_PREDATION_RATE(double aPHYTOPLANKTON_PREDATION_RATE) {
        MAX_PHYTOPLANKTON_PREDATION_RATE = aPHYTOPLANKTON_PREDATION_RATE;
    }

    public static double getFILTRATION_PARAMETER() {
        return FILTRATION_PARAMETER;
    }

    public static void setFILTRATION_PARAMETER(double aFILTRATION_PARAMETER) {
        FILTRATION_PARAMETER = aFILTRATION_PARAMETER;
    }

    public static double getRES_ESSUD_MORTAL_REFERENCE_RATE() {
        return RES_ESSUD_REFERENCE_RATE;
    }

    public static void setRES_ESSUD_MORTAL_REFERENCE_RATE(double aRES_ESSUD_MORTAL_REFERENCE_RATE) {
        RES_ESSUD_REFERENCE_RATE = aRES_ESSUD_MORTAL_REFERENCE_RATE;
    }

    public static double getGRAZING_LOSS() {
        return GRAZING_LOSS;
    }

    public static void setGRAZING_LOSS(double aGRAZINGLOSS) {
        GRAZING_LOSS = aGRAZINGLOSS;
    }

    public static double getIVLEV_PARAMETER() {
        return IVLEV_PARAMETER;
    }

    public static void setIVLEV_PARAMETER(double aIVLEV_PARAMETER) {
        IVLEV_PARAMETER = aIVLEV_PARAMETER;
    }

    public static double getBACKGROUND_CONCENTRATION() {
        return BACKGROUND_CONCENTRATION;
    }

    public static void setBACKGROUND_CONCENTRATION(double aBACKGROUND_CONCENTRATION) {
        BACKGROUND_CONCENTRATION = aBACKGROUND_CONCENTRATION;
    }
    
}
