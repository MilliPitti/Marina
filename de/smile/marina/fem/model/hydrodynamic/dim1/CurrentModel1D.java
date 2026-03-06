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

import de.smile.marina.TimeDependentModel;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FEdge;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TimeDependentFEApproximation;
import java.awt.*;

/**
 * This class describe the stabilized finite element approximation
 * of the instationary 1-dimensional shallow water equation
 * 
 * @author Prof. Dr.-Ing. habil. Peter Milbradt
 * @version 3.15.5
 */
public class CurrentModel1D extends TimeDependentFEApproximation implements FEModel, TimeDependentModel {
    static final double G = 9.81;
    static final double AST = 0.0012; // 0.0012 Austauschkoeffizient fuer Stroemung
    static final double INITIAL_MAX_TIMESTEP = 0.001;

    static public double WATT = 0.05;

    private final int numberofdofs;
    private double previousTimeStep = 0.0;

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
        // sicherer Startwert fuer den allerersten Substep
        setMaxTimeStep(INITIAL_MAX_TIMESTEP);
    }

    /**
     * compute the initial solutions
     * 
     * @param time starttime
     * @return the result vector
     */
    public double[] initialSolution(double time) {
        System.out.println("CurrentModel - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            if (dof.x<50) cmd.h=0.5;
            else cmd.h=0.;
            cmd.u = 0.;
        }
        // Courant-Schritt schon aus Initialzustand abschaetzen
        setMaxTimeStep(Math.min(INITIAL_MAX_TIMESTEP, estimateCourantTimeStepFromState()));
        return null;
    }

    private double estimateCourantTimeStepFromState() {
        double tsMin = Double.MAX_VALUE;
        for (FElement element : fenet.getFElements()) {
            FEdge edge = (FEdge) element;
            DOF d0 = edge.getDOF(0);
            DOF d1 = edge.getDOF(1);
            CurrentModel1DData c0 = CurrentModel1DData.extract(d0);
            CurrentModel1DData c1 = CurrentModel1DData.extract(d1);

            double uMean = 0.5 * (c0.u + c1.u);
            double absDepthMean = 0.5 * ((d0.z + c0.h) + (d1.z + c1.h));
            double operatorNorm = Math.abs(uMean) + Math.sqrt(G * Math.max(WATT, absDepthMean));
            if (operatorNorm <= 1.e-12) continue;

            double ts = 0.5 * edge.elm_size() / operatorNorm;
            if (Double.isFinite(ts) && ts > 0.) tsMin = Math.min(tsMin, ts);
        }
        return tsMin < Double.MAX_VALUE ? tsMin : INITIAL_MAX_TIMESTEP;
    }

    /**
     * @param dof
     * @return
     */
    @Override
    public ModelData genData(DOF dof) {
        CurrentModel1DData md = new CurrentModel1DData();
//        double x0 = 50;
//        double omega = 10.;// 0.1*(xmax-xmin);
//        md.h = 0.2 * Math.exp(-((dof.x - x0) * (dof.x - x0)) / (2. * omega));
//        if (dof.x < 25.)
//            md.h = -1.;
//        if (dof.x > 60.)
//            md.h = -1.;
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

        //double a_opt = 1.;
        //if (ast > 0.00001) {
        //    double peclet = operatornorm * elementsize / ast;
        //    a_opt = Function.coth(peclet) - 1.0 / peclet;
        //}
        //tau_cur *= a_opt;

        double cureq1_mean = 0.;
        double cureq2_mean = 0.;

        for (int j = 0; j < 2; j++) {
            CurrentModel1DData cmd = CurrentModel1DData.extract(ele.getDOF(j));
            cureq1_mean += 1. / 2. * (cmd.dhdt + absdepth[j] * dudx + cmd.u * depthdx - q[j]);
        }

        for (int j = 0; j < 2; j++) {
            DOF dof = ele.getDOF(j);
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            cureq2_mean += 1. / 2. * (cmd.dudt + G * dhdx + cmd.u * dudx
                    + G / Math.pow(Math.max(absdepth[j], WATT), 1. / 3.) * Math.abs(u[j]) / 42. / 42.
                            / Math.max(absdepth[j], WATT) * u[j]
                                // CouplingTerm from the derivation of the Formulation q -> v
                                - cmd.u / Math.max(absdepth[j], WATT) * cureq1_mean // Improvement in the dam break simulation cmd.wlamda scaled nonZeroTotalDepth against Null, if the node dries out
                                );
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

        @Override
    public void timeStep(double dt) {
        for (DOF dof : fenet.getDOFs()) {
            CurrentModel1DData current = CurrentModel1DData.extract(dof);
            if ((dof.z + current.h) <= 0.) {
                current.h = -dof.z;
                current.u = 0.;
            }
            current.dudx = 0.;
            current.ru = 0.;
            current.rh = 0.;
        }

        setBoundaryConditions();
        maxTimeStep = Double.MAX_VALUE;
        performElementLoop();

        final double beta0, beta1;
        if (previousTimeStep == 0.0) {
            beta0 = 1.0;
            beta1 = 0.0;
        } else {
            final double omega = dt / previousTimeStep / 2.;
            beta0 = 1.0 + omega;
            beta1 = -omega;
        }

        for (DOF dof : fenet.getDOFs()) {
            CurrentModel1DData current = CurrentModel1DData.extract(dof);

            final double ru = beta0 * current.ru + beta1 * current.dudt;
            final double rh = beta0 * current.rh + beta1 * current.dhdt;

            current.dudt = current.ru;
            current.dhdt = current.rh;

            current.u += dt * ru;
            current.h += dt * rh;

            if ((dof.z + current.h) <= 0.) {
                current.h = -dof.z;
                current.u = 0.;
            }
        }

        previousTimeStep = dt;
        time += dt;
    }

    public void draw_it(Graphics g, double time) {
        final DOF[] dofs = fenet.getDOFs();
        if (g == null || dofs == null || dofs.length < 2) return;

        int width = 800;
        int height = 400;
        if (g.getClipBounds() != null) {
            width = g.getClipBounds().width;
            height = g.getClipBounds().height;
        }

        g.setColor(Color.white);
        g.fillRect(0, 0, width, height);

        final int marginLeft = 55;
        final int marginRight = 55;
        final int marginTop = 25;
        final int marginBottom = 30;

        final int plotWidth = Math.max(10, width - marginLeft - marginRight);
        final int plotHeight = Math.max(10, height - marginTop - marginBottom);

        final int x0 = marginLeft;
        final int x1 = marginLeft + plotWidth;
        final int yTop = marginTop;
        final int yBottom = marginTop + plotHeight;

        double xmin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double zhMin = Double.POSITIVE_INFINITY;
        double zhMax = Double.NEGATIVE_INFINITY;
        double umax = 0.0;

        for (DOF dof : dofs) {
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            final double zDraw = -dof.z; // z-Achse zeigt nach unten
            xmin = Math.min(xmin, dof.x);
            xmax = Math.max(xmax, dof.x);
            zhMin = Math.min(zhMin, Math.min(zDraw, cmd.h));
            zhMax = Math.max(zhMax, Math.max(zDraw, cmd.h));
            umax = Math.max(umax, Math.abs(cmd.u));
        }

        if (xmax <= xmin) xmax = xmin + 1.0;
        if (zhMax <= zhMin) zhMax = zhMin + 1.0;
        if (umax < 1.0e-9) umax = 1.0;

        g.setColor(Color.black);
        g.drawRect(x0, yTop, plotWidth, plotHeight);
        g.drawString("z/h [m]", x0 + 5, yTop + 14);
        g.drawString("u [m/s]", x1 - 50, yTop + 14);
        final String timeLabel = String.format("t = %.2f s", time);
        g.drawString(timeLabel, x0 + 5, height - 8);
        g.drawString(timeLabel, Math.max(x0 + 5, x1 - 120), yTop + 14);
        g.setColor(new Color(139, 69, 19));
        g.drawString("z", x0 + 60, yTop + 14);
        g.setColor(Color.blue);
        g.drawString("h", x0 + 75, yTop + 14);
        g.setColor(Color.red);
        g.drawString("u", x0 + 90, yTop + 14);

        int zhZeroY = yBottom - (int) ((0.0 - zhMin) / (zhMax - zhMin) * plotHeight);
        if (zhZeroY >= yTop && zhZeroY <= yBottom) {
            g.setColor(Color.lightGray);
            g.drawLine(x0, zhZeroY, x1, zhZeroY);
        }
        int uZeroY = yTop + plotHeight / 2;
        g.setColor(Color.lightGray);
        g.drawLine(x0, uZeroY, x1, uZeroY);

        g.setColor(Color.black);
        g.drawString(String.format("%.2f", zhMax), 5, yTop + 5);
        g.drawString(String.format("%.2f", zhMin), 5, yBottom);
        g.drawString(String.format("+%.2f", umax), x1 + 5, yTop + 5);
        g.drawString(String.format("-%.2f", umax), x1 + 5, yBottom);

        for (int i = 0; i < dofs.length - 1; i++) {
            DOF d0 = dofs[i];
            DOF d1 = dofs[i + 1];
            CurrentModel1DData c0 = CurrentModel1DData.extract(d0);
            CurrentModel1DData c1 = CurrentModel1DData.extract(d1);
            final double z0Draw = -d0.z; // z-Achse zeigt nach unten
            final double z1Draw = -d1.z; // z-Achse zeigt nach unten

            int px0 = x0 + (int) ((d0.x - xmin) / (xmax - xmin) * plotWidth);
            int px1 = x0 + (int) ((d1.x - xmin) / (xmax - xmin) * plotWidth);

            int pz0 = yBottom - (int) ((z0Draw - zhMin) / (zhMax - zhMin) * plotHeight);
            int pz1 = yBottom - (int) ((z1Draw - zhMin) / (zhMax - zhMin) * plotHeight);
            g.setColor(new Color(139, 69, 19));
            g.drawLine(px0, pz0, px1, pz1);

            int ph0 = yBottom - (int) ((c0.h - zhMin) / (zhMax - zhMin) * plotHeight);
            int ph1 = yBottom - (int) ((c1.h - zhMin) / (zhMax - zhMin) * plotHeight);
            g.setColor(Color.blue);
            g.drawLine(px0, ph0, px1, ph1);

            int pu0 = yBottom - (int) ((c0.u + umax) / (2.0 * umax) * plotHeight);
            int pu1 = yBottom - (int) ((c1.u + umax) / (2.0 * umax) * plotHeight);
            g.setColor(Color.red);
            g.drawLine(px0, pu0, px1, pu1);
        }
    }

    @Override
    public void write_erg_xf() {
    }

    @Override
    public ModelData genData(FElement felement) {
        return null;
    }
}
