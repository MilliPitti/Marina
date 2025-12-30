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
/*
 * PhytoplanktonDat.java
 *
 * Created on 25. Oktober 2006, 10:46
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
public class PhytoplanktonDat {
    public String phyto_rndwerteName = null;
    public String xferg_name = "phytoplanktonerg.bin";
    public double watt = 0.01;
    
    public int numberOfThreads = 1;
    
    public String startWerteDatei=null;  // erg-file with start results
    public int startSatz=0;
    
    public String concentration_name = null; // ascii-file from type system.dat with initial waterlevel
    public SmileIO.MeshFileType concentrationFileType = SmileIO.MeshFileType.SystemDat;
    
    //Ecological parameter for phytoplankton
    /** 
     * -Reference temperature under optimal, non-limiting, light
     *  and nutrients availability.
     */
    public static double REFERENCE_TEMPERATURE = 20.0;
    
    /**
     * -Maximum growth rate at a reference temperature [1/sec]
     */
    private static double MAX_GROWTH_RATE = 1.0;//(24*60*60); // [1/d]/(24*60*60)--->[1/sec]
//    private static double MAX_GROWTH_RATE = 0.05/(24*60*60); // [1/d]/(24*60*60)--->[1/sec]
//    public static double GROWTH_RATE = 1.;
    /**
     *  -Light limitation
     *  KL: Semisaturation constant (0.004 - 0.006) [kcal * m^-2 * sec^-1]
     */
    private static double LIGHT_SEMISATURATION_CONSTANT = 0.1;//0.005;
    /**
     *  -Nutrient limitation
     *  KC: Semisaturation constant 
     *  *********** from MS thesis ******** 1.952 micro mol/l
     *  *********** 14.0067 + 3. * 15.9994; //Molare Masse Nitrat
     */
    private static double NUTRIENT_SEMISATURATION_CONSTANT = 1.5;//1.952 * (14.0067 + 3. * 15.9994);//
    /**
     *  -Photosynthetic activity coefficient = 0.56
     */
    public static final double PHOTOSYNTHETIC_ACTIVITY_COEFFICIENT = 0.56;
    /**
     *  -Light extinction coefficient
     */
    public static final double LIGHT_EXTINCTION_COEFFICIENT = 1.0;
    /**
     *  -Respiration, essudation and natural mortality rate at a reference temperature
     */
    private static double RES_ESSUD_REFERENCE_RATE = 0.015;//(24*60*60);//0.0208;///[1/d]/(24*60*60)--->[1/sec]
    public static double MORTAL_REFERENCE_RATE = 0.03;//(24*60*60);//0.0208;///[1/d]/(24*60*60)--->[1/sec]
//    private static double RES_ESSUD_MORTAL_REFERENCE_RATE = (0.001 + 0.002)/(24*60*60);//0.0208;///[1/d]/(24*60*60)--->[1/sec]
    /**
     *  Background concentration
     */
    private static double BACKGROUND_CONCENTRATION = 0.01;
    
    /** Creates a new instance of PhytoplanktonDat */
    public PhytoplanktonDat() {
    }

    public static double getMAX_GROWTH_RATE() {
        return MAX_GROWTH_RATE;
    }

    public static void setMAX_GROWTH_RATE(double aMAX_GROWTH_RATE) {
        MAX_GROWTH_RATE = aMAX_GROWTH_RATE;
    }

    public static double getLIGHT_SEMISATURATION_CONSTANT() {
        return LIGHT_SEMISATURATION_CONSTANT;
    }

    public static void setLIGHT_SEMISATURATION_CONSTANT(double aLIGHT_SEMISATURATION_CONSTANT) {
        LIGHT_SEMISATURATION_CONSTANT = aLIGHT_SEMISATURATION_CONSTANT;
    }

    public static double getNutrient_SEMISATURATION_CONSTANT() {
        return NUTRIENT_SEMISATURATION_CONSTANT;
    }

    public static void setNutrient_SEMISATURATION_CONSTANT(double aNutrient_SEMISATURATION_CONSTANT) {
        NUTRIENT_SEMISATURATION_CONSTANT = aNutrient_SEMISATURATION_CONSTANT;
    }

    public static double getRES_ESSUD_MORTAL_REFERENCE_RATE() {
        return RES_ESSUD_REFERENCE_RATE;
    }

    public static void setRES_ESSUD_MORTAL_REFERENCE_RATE(double aRES_ESSUD_MORTAL_REFERENCE_RATE) {
        RES_ESSUD_REFERENCE_RATE = aRES_ESSUD_MORTAL_REFERENCE_RATE;
    }

    public static double getBACKGROUND_CONCENTRATION() {
        return BACKGROUND_CONCENTRATION;
    }

    public static void setBACKGROUND_CONCENTRATION(double aBACKGROUND_CONCENTRATION) {
        BACKGROUND_CONCENTRATION = aBACKGROUND_CONCENTRATION;
    }
    
}
