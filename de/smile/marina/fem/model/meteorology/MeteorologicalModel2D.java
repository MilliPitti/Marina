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
package de.smile.marina.fem.model.meteorology;

import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.*;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * tiefenintegriertes Meteorologisches Modell
 * @author milbradt
 * @version 2.7.15
 */
public class MeteorologicalModel2D extends TimeDependentFEApproximation implements FEModel, TicadModel, TimeDependentModel {


    protected DataOutputStream xf_os = null;
    protected FileOutputStream xf_fs = null;

    private OKWind windtimeseries = null;


    private MeteorologyData2D data = new MeteorologyData2D();

    //fuer Temperaturansatz
    double T1=365.*24.*3600.;   //1 Jahr
    double w1=(2.*Math.PI)/T1;   //Kreisfrequenz bestimmen
    double C1=7.5;              //Amplitude
    double x1=12.5;              //Mittelwert

    double T2=24.*3600.;   //1 Tag
    double w2=(2.*Math.PI)/T2;   //Kreisfrequenz bestimmen
    double C2=5.;              //Amplitude
    double x2=0.;              //Mittelwert


    //fuer Lichtintensitaetansatz bezogen auf ca. 55 Grad noerdliche Breite
    double T3=365.*24.*3600.;   //1 Jahr
    double w3=(2.*Math.PI)/T3;   //Kreisfrequenz bestimmen
    double C3=10.;              //Amplitude
    double x3=12.5;              //Mittelwert MJm-2day-1


    protected MeteorologicalModel2D(FEDecomposition fe) {
        fenet = fe;
    }


    /** Creates a new instance of OKWindModel */
    public MeteorologicalModel2D(FEDecomposition fe, String name, MeteorologicalDat dat) {
        System.out.println("MeteorologicalModel2D initialization");
        fenet = fe;
        this.maxTimeStep = Double.MAX_VALUE;
        femodel=this;

        if (!name.isEmpty())
            windtimeseries = new OKWind(name);
        else
            windtimeseries = new OKWind();

        // DOFs initialisieren
        initialDOFs();

        try {
            xf_os = new DataOutputStream(new FileOutputStream(dat.xferg_name));
            // Setzen der Ergebnismaske 
            TicadIO.write_xf(xf_os, this );
        } catch (FileNotFoundException e) {
            System.out.println("The file "+ dat.xferg_name + " cannot be opened");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } 
    }


    @Override
    public ModelData genData(DOF dof){
        return data;
    }

    @Override
    public void timeStep(double dt) {

        this.time += dt;

        if(windtimeseries != null){
            double[] w = windtimeseries.getValue(time);
            data.windx = w[0];
            data.windy = w[1];
            data.windspeed = Function.norm(w[0],w[1]);
        }

        data.temperature=C1*Math.cos(w1*time-Math.PI)+x1; // Jahresschwankung
        data.temperature+=C2*Math.cos(w2*time-Math.PI)+x2;  // Tagesschwankung

        //Bestimme Lichtintensitaet in MJm-2day-1
        double I_n=C3*Math.cos(w3*time-Math.PI)+x3;
        //Umrechnung in Wm-2<==>Jm-2s-1
        I_n*=1000000./(24.*3600.);

        //Bestimmung der Dauer der Photoperiode bezogen auf 1 Tag
        double n=time/(3600.*24.)+1.;
        n-=60;  //auf 1. Maerz beziehen
        //Umrechnung Tag -> Winkel bezogen auf's ganze Jahr'
        double y=2*Math.PI*(n-21)/365;
        //Berechne Deklination der Sonne
        double deklination=0.38092-0.76996*Math.cos(y)+23.265*Math.sin(y)+
                                   0.36958*Math.cos(2.*y)+0.10868*Math.sin(2.*y)+
                                   0.01834*Math.cos(3.*y)-0.00392*Math.sin(3.*y)-
                                   0.00392*Math.cos(4.*y)-0.00072*Math.sin(4.*y)-
                                   0.00051*Math.cos(5.*y)+0.0025*Math.sin(5.*y);
        double varphi=55.;  //Noerdliche Breite
        varphi=varphi/180*Math.PI;
        //Bestimme Photoperiode bezogen auf ganzen Tag   [0...1]
        double p=(2*Math.acos(-Math.tan(varphi)*Math.tan(deklination/180.*Math.PI)))/(Math.PI*2.);

        double t=time%(24.*3600.);
        t/=(24.*3600.);
        if ((t<0.5-p/2.) || (t>0.5+p/2.))
            data.insolation=0.;
        else data.insolation=I_n/p*(1.+Math.cos((t-0.5)*Math.PI*2./p));

    }

    public double[] initialSolution(double StartTime) {
        return new double[1];
    }

    @Override
    public ModelData genData(FElement felement) {
        return null;
    }

    @Override
    public void setBoundaryCondition(DOF dof, double t) {
    }

    @Override
    public double ElementApproximation(FElement ele) {
        return Double.MAX_VALUE;
    }

    @Override
    public int getTicadErgMask() {
        // Windgeschwindigkeit, Luftdruck, Einstrahlung, Temperatur
        return TicadIO.HRES_V | TicadIO.HRES_H | TicadIO.HRES_SALT | TicadIO.HRES_EDDY;
    }

    @Override
    public void write_erg_xf() {
        if (xf_os != null) {
            try {
                xf_os.writeFloat((float) time);

                for (DOF dof : fenet.getDOFs()) {
                    MeteorologyData2D meteorologyData2D = MeteorologyData2D.extract(dof);
                    xf_os.writeFloat((float) meteorologyData2D.windx);
                    xf_os.writeFloat((float) meteorologyData2D.windy);
                    xf_os.writeFloat((float) meteorologyData2D.pressure);
                    xf_os.writeFloat((float) meteorologyData2D.insolation);
                    xf_os.writeFloat((float) meteorologyData2D.temperature);
                }
                xf_os.flush();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    @Override
    @Deprecated
    public void write_erg_xf(double[] erg, double t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    @Deprecated
    public double[] getRateofChange(double time, double[] x) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
