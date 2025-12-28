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

import bijava.math.ifunction.DiscretScalarFunction1d;
import bijava.math.ifunction.ScalarFunction1d;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEApproximation;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import de.smile.marina.io.TicadIO;
import de.smile.math.Function;
import de.smile.math.ode.ivp.ODESystem;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;

/**
 * Beschreibt ein Windmodell mit zeitvariantem aber ortskonstantem Wind
 *
 * @author milbradt
 */
public class OKWindModel extends FEApproximation implements ODESystem, FEModel {

    ScalarFunction1d windx = null;
    ScalarFunction1d windy = null;

    MeteorologyData2D data = new MeteorologyData2D();

    /** Creates a new instance of OKWindModel */
    public OKWindModel(FEDecomposition fe, String name) {
        System.out.println("OKWindModel initialization");
        fenet = fe;
        femodel = this;

        if (!name.isEmpty()) {
            try {
                Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
                StreamTokenizer st = new StreamTokenizer(r);
                st.eolIsSignificant(true);
                st.commentChar('C');

                System.out.println("\tRead OKWind from " + name);

                int anz = TicadIO.NextInt(st);
                double[][] wertewx = new double[2][anz];
                double[][] wertewy = new double[2][anz];

                // einlesen
                for (int K = 0; K < anz; K++) {
                    wertewx[0][K] = TicadIO.NextDouble(st);
                    wertewy[0][K] = wertewx[0][K];
                    wertewx[1][K] = TicadIO.NextDouble(st);
                    wertewy[1][K] = TicadIO.NextDouble(st);

                }
                windx = new DiscretScalarFunction1d(wertewx);
                windy = new DiscretScalarFunction1d(wertewy);
                windx.setPeriodic(true);
                windy.setPeriodic(true);
            } catch (FileNotFoundException e) {
                System.out.println("\tcan't open " + name + " for wind time series");
                System.exit(-1);
            }
        } else {
            System.out.println("keine wind.dat gelesen");
        }

        // DOFs initialisieren
        initialDOFs();

    }

    public long getTicadErgMask() {
        // Setzen der Ergebnismaske Konzentration
        return TicadIO.HRES_H;
    }

    @Override
    public ModelData genData(DOF dof) {
        return data;
    }

    /**
     * Compute the time derivations on each node by the FE-domainapproximation of
     * the FE-Model
     * 
     * @param time
     * @param x
     * @return
     */
    // public double[] getRateofChange(double time, double x[]){
    // if(windtimeseries != null){
    // double[] w = windtimeseries.getRateofChange(time);
    // data.windx = w[0];
    // data.windy = w[1];
    // data.windspeed = Function.norm(w[0],w[1]);
    // }
    // return new double[1];
    // }

    @Override
    public double[] getRateofChange(double time, double x[]) {

        if (windx != null)
            data.windx = windx.getValue(time);
        if (windy != null)
            data.windy = windy.getValue(time);

        data.windspeed = Function.norm(data.windx, data.windy);

        return new double[] { data.windspeed };
    }

    // @Override
    // public int getResultSize() {
    // return 1;
    // }

    @Override
    public void setMaxTimeStep(double maxtimestep) {
    }

    @Override
    public double getMaxTimeStep() {
        return Double.MAX_VALUE;
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

}
