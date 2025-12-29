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
package de.smile.marina.fem.model.hydrodynamic.dim1;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FEdge;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.math.Function;
import java.awt.*;

/**
 * This class describe the stabilized finite element approximation
 * of the instationary 1-dimensional shallow water equation
 * 
 * @author Prof. Dr.-Ing. habil. Peter Milbradt
 * @version 3.15.5
 */
public class CurrentModel1D extends TimeDependentFEApproximation implements FEModel {
    static final double G = 9.81;
    static final double AST = 0.0012; // 0.0012 Austauschkoeffizient fuer Stroemung

    static public double WATT = 0.05;

    private final int n, U, H, numberofdofs;
    private final double[] result;

    /**
     * Creates new CurrentModel1D
     * 
     * @param fe a finite edge domain decomposition
     */
    public CurrentModel1D(FEDecomposition fe) {
        fenet = fe;
        femodel = this;
        // DOFs initialisieren
        initialDOFs();

        numberofdofs = fenet.getNumberofDOFs();
        U = 0;
        H = numberofdofs;
        n = 2 * numberofdofs;
        result = new double[n];
    }

    /**
     * compute the initial solutions
     * 
     * @param time starttime
     * @return the result vector
     */
    public double[] initialSolution(double time) {
        double x[] = new double[n];

        System.out.println("CurrentModel - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            x[H + i] = cmd.h;
            x[U + i] = 0.;
        }
        return x;
    }

    /**
     * @param dof
     * @return
     */
    @Override
    public ModelData genData(DOF dof) {
        CurrentModel1DData md = new CurrentModel1DData();
        // if (dof.x<50) md.h=0.5;
        // else md.h=0.;

        double x0 = 50;
        double omega = 10.;// 0.1*(xmax-xmin);
        md.h = 0.2 * Math.exp(-((dof.x - x0) * (dof.x - x0)) / (2. * omega));
        if (dof.x < 25.)
            md.h = -1.;
        if (dof.x > 60.)
            md.h = -1.;
        return md;
    }

    /**
     * set the solving conditions at the DOF
     * 
     * @param dof degree of freedom to set
     * @param t   actual time
     */
    @Override
    public void setBoundaryCondition(DOF dof, double t) {
        if (dof.number == 0) {
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            cmd.u = 0.;
        }
        if (dof.number == numberofdofs - 1) {
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            cmd.u = 0.;
        }
    }

    /**
     * @param element
     * @return
     */
    @Override
    public double ElementApproximation(FElement element) {

        FEdge ele = (FEdge) element;
        double[][] koeffmat = ele.getkoeffmat();
        double[] u = new double[2];
        double[] h = new double[2];
        double[] absdepth = new double[2];
        // compute element derivations
        double dudx = 0.;
        double dhdx = 0.;
        double depthdx = 0.;
        double u_mean = 0.;
        double absdepth_mean = 0.;
        int iwatt = 0;
        double[] q = new double[2];

        for (int j = 0; j < 2; j++) {
            DOF dof = ele.getDOF(j);
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            GroundwaterModel1DData gwm = GroundwaterModel1DData.extract(dof);

            if (gwm != null) {
                // Wenn Grundwasser vorhanden, dann Oberflaechenwasser anpassen
                if ((gwm.zG + gwm.h > CurrentModel1D.WATT)) {
                    q[j] = gwm.h - cmd.h;
                }
            }

            u[j] = cmd.u;
            u_mean += u[j] / 2.;
            h[j] = cmd.h;
            absdepth[j] = dof.z + cmd.h;
            if (absdepth[j] < WATT)
                iwatt++;
            dhdx += cmd.h * koeffmat[j][1];
            dudx += cmd.u * koeffmat[j][1];
            absdepth_mean += absdepth[j] / 2.;
            depthdx += absdepth[j] * koeffmat[j][1];
        }

        if (iwatt != 0) {
            dhdx = 0.;
            if (iwatt == 1)
                for (int j = 0; j < 2; j++) {
                    if (absdepth[j] < WATT)
                        if (h[j] < h[(j + 1) % 2])
                            dhdx = h[j] * koeffmat[j][1] + h[(j + 1) % 2] * koeffmat[(j + 1) % 2][1];
                        else
                            dhdx = (absdepth[j] / WATT * h[j] + (1. - absdepth[j] / WATT) * h[(j + 1) % 2])
                                    * koeffmat[j][1] + h[(j + 1) % 2] * koeffmat[(j + 1) % 2][1];
                }
            else
                dhdx = (absdepth[0] / WATT * h[0] + (1. - absdepth[0] / WATT) * h[1]) * koeffmat[0][1]
                        + (absdepth[1] / WATT * h[1] + (1. - absdepth[1] / WATT) * h[0]) * koeffmat[1][1];
        }

        double elementsize = ele.elm_size();
        double ast = AST;
        // Smagorinsky-Ansatz
        ast += Math.pow(ast * elementsize, 2.) * Math.abs(dudx);
        ast += AST * Math.pow(G, 0.5) / 42 * Math.abs(u_mean) * absdepth_mean;

        double operatornorm = Math.abs(u_mean) + Math.sqrt(G * Math.max(WATT, absdepth_mean));

        double tau_cur = 0.5 * elementsize / operatornorm;

        double timeStep = tau_cur;

        double a_opt = 1.;
        if (ast > 0.00001) {
            double peclet = operatornorm * elementsize / ast;
            a_opt = Function.coth(peclet) - 1.0 / peclet;
        }
        tau_cur *= a_opt;

        double cureq1_mean = 0.;
        double cureq2_mean = 0.;

        for (int j = 0; j < 2; j++) {
            DOF dof = ele.getDOF(j);
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);

            cureq1_mean += 1. / 2. * (cmd.dhdt + absdepth[j] * dudx + cmd.u * depthdx - q[j]);
            cureq2_mean += 1. / 2. * (cmd.dudt + G * dhdx + cmd.u * dudx
                    + G / Math.pow(Math.max(absdepth[j], WATT), 1. / 3.) * Math.abs(u[j]) / 42. / 42.
                            / Math.max(absdepth[j], WATT) * u[j]);
        }

        for (int j = 0; j < 2; j++) {
            DOF dof = ele.getDOF(j);
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);

            cmd.rh -= tau_cur * (koeffmat[j][1] * absdepth_mean * cureq2_mean +
                    koeffmat[j][1] * u_mean * cureq1_mean);
            cmd.ru -= tau_cur * (koeffmat[j][1] * u_mean * cureq2_mean +
                    koeffmat[j][1] * G * cureq1_mean);

            double vorfaktor;
            for (int l = 0; l < 2; l++) {
                if (j == l)
                    vorfaktor = 1. / 3.;
                else
                    vorfaktor = 1. / 6.;

                cmd.rh -= vorfaktor * (absdepth[l] * dudx + u[l] * depthdx - q[j]);
                cmd.ru -= vorfaktor * (G * dhdx + u[l] * dudx + 2. * ast * dudx * koeffmat[j][1]
                        + G / Math.pow(Math.max(absdepth[l], WATT), 1. / 3.) * Math.abs(u[l]) / 42. / 42.
                                / Math.max(absdepth[l], WATT) * u[l]);
            }
            cmd.dudx += 0.5 * dudx;
        }
        return timeStep;
    }

    /**
     * getRateofChange
     * 
     * @return
     */
    @Override
    public double[] getRateofChange(double p1, double[] x) {

        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            CurrentModel1DData current = CurrentModel1DData.extract(dof);
            if ((dof.z + x[H + i]) <= 0.) {
                x[H + i] = -dof.z;
                x[U + i] = 0.;
            }
            // if ((dof[j].z + x[H + i]) < WATT) x[U + i] *= (dof[j].z + x[H + i])/WATT;
            current.h = x[H + i];
            current.u = x[U + i];
            setBoundaryCondition(dof, p1);
            x[H + i] = current.h;
            x[U + i] = current.u;
            current.dudx = 0.;
            // set Results to zero
            current.ru = 0.;
            current.rh = 0.;
        }

        // Elementloop
        performElementLoop();

        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            CurrentModel1DData current = CurrentModel1DData.extract(dof);

            result[U + i] = current.ru;
            result[H + i] = current.rh;

            current.dudt = result[U + i];
            current.dhdt = result[H + i];
        }

        return result;
    }

    public void draw_it(Graphics g, double[] x, double time) {

        g.clearRect(0, 0, 800, 400);
        g.setColor(Color.white);
        g.fillRect(0, 0, 800, 400);

        int anz = fenet.getNumberofDOFs();

        g.setColor(Color.blue);
        for (int k = 0; k < anz - 1; k++) {
            if ((fenet.getDOF(k + 1).z + x[H + k]) >= 0.)
                g.drawLine((int) (10 * fenet.getDOF(k).x), 400 - ((int) (50. * x[H + k]) + 200),
                        (int) (10 * fenet.getDOF(k + 1).x), 400 - ((int) (50. * x[H + k + 1]) + 200));
        }
    }

    @Override
    public ModelData genData(FElement felement) {
        return null;
    }
}
