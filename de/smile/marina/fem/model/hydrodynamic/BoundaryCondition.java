package de.smile.marina.fem.model.hydrodynamic;
import bijava.math.ifunction.*;
/** Hilfsklasse zur Speicherung der Randbedingungen
 * @version 2.2.0
 * @author Peter Milbradt
 */
public class BoundaryCondition{
    // Stringkonstanten 
    
    //Stroemungsmodell 
    public final static String velocity_u		="VELOCITY U";
    public final static String velocity_v		="VELOCITY V";      
    public final static String free_surface		="FREE SURFACE";
    public final static String specific_flowrate_x      ="SPECIFIC FLOWRATE X";
    public final static String specific_flowrate_y      ="SPECIFIC FLOWRATE Y";
    public final static String absolute_flowrate_x      ="ABSOLUTE FLOWRATE X";
    public final static String absolute_flowrate_y      ="ABSOLUTE FLOWRATE Y";
    
    public final static String pointbased_Q_source      ="POINTBASED Q SOURCE";
    public final static String pointbased_h_source      ="POINTBASED H SOURCE";
    
    
    //Sedimenttransport-Modell
    public final static String bottom			="BOTTOM";
    public final static String concentration_sediment   ="SEDIMENT CONCENTRATION";
    public final static String d50                      ="D50";
    
    //Salztransport-Modell
    public final static String concentration_salt       ="SALT CONCENTRATION";
    public final static String pointbased_salt_source	="POINTBASED SALT SOURCE";
    
    //Heattransport-Modell
    public final static String water_temperature        ="WATER TEMPERATURE";

    //Oxygentransport-Modell
    public final static String concentration_oxygen     ="WATER OXYGEN CONCENTRATION";

    //AdvectionDispersion-Modell
    public final static String ad_concentration         ="AD CONCENTRATION";
    public final static String pointbased_ad_source	="POINTBASED AD SOURCE";
    
    //WaveHypModel
    public final static String wave_period              ="WAVE PERIOD";
    public final static String wave_direction           ="WAVE DIRECTION";    
    public final static String wave_height              ="WAVE HEIGHT";
    
    //Meteorological-Modell
    public final static String wind_x                   ="WIND X";
    public final static String wind_y                   ="WIND Y";
    public final static String air_pressure		="AIR PRESSURE";
    public final static String air_temperature          ="AIR_TEMPERATURE";  
    public final static String insolation               ="INSOLATION";
    
    //Groundwater
    public final static String groundwater_free_surface="GROUNDWATER FREE SURFACE";
    
    
    //other (noch nicht genutzt)
    
    
    public final static String water_depth		="WATER DEPTH     M               ";  
    public final static String celerity			="CELERITY        M/S             ";   
    public final static String froude_number            ="FROUDE NUMBER                   ";
    public final static String scalar_flowrate          ="SCALAR FLOWRATE M2/S            ";
    public final static String tracer			="TRACER                          "; 
    public final static String turbulent_energy         ="TURBULENT ENERG.JOULE/KG        ";
    public final static String dissipation		="DISSIPATION     WATT/KG         ";
    public final static String viscosity		="VISCOSITY       M2/S            ";  
    public final static String scalar_velocity          ="SCALAR VELOCITY M/S             ";   
    public final static String bottom_friction          ="BOTTOM FRICTION                 ";
    public final static String drift_along_x            ="DRIFT ALONG X   M               ";
    public final static String drift_along_y            ="DRIFT ALONG Y   M               ";
    public final static String courant_number           ="COURANT NUMBER                  ";
    public final static String varaiable23		="VARIABLE 23     UNIT   ??       ";
    
    public final static String varaiable25		="VARIABLE 25     UNIT   ??       ";
    public final static String varaiable26		="VARIABLE 26     UNIT   ??       ";
    
    public String boundary_condition_key;
    public int pointnumber;
    public ScalarFunction1d function;

    public BoundaryCondition( String boundary_condition_key, int nr, ScalarFunction1d fkt ){
        this.boundary_condition_key=boundary_condition_key;        
        pointnumber = nr;
        function    = fkt;
    }
}