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
import java.util.*;

public class GroundwaterModel1D extends TimeDependentFEApproximation implements FEModel, TimeDependentModel {
    private static final double INITIAL_MAX_TIMESTEP = 0.001;

    private int n,  numberofdofs;
    private double previousTimeStep = 0.0;
    private double drawXMin = Double.NaN;
    private double drawXMax = Double.NaN;
    private double drawYMin = Double.NaN;
    private double drawYMax = Double.NaN;

    /** Creates new SedimentModel1D */
    public GroundwaterModel1D(FEDecomposition fe) {
        fenet = fe;
        femodel = this;
        // DOFs initialisieren
        initialDOFs();

        numberofdofs = fenet.getNumberofDOFs();
        n = numberofdofs;
        setMaxTimeStep(INITIAL_MAX_TIMESTEP);
    }

    //------------------------------------------------------------------------
    // initialSolution
    //------------------------------------------------------------------------
    //...Anfangswertberechnung...............................................  
    public void initialSolution(double time) {
        System.out.println("GroundwaterModel - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            gwm.dhdt = 0.;
        }
        setMaxTimeStep(estimateCourantTimeStepFromState());
    }

    private double estimateCourantTimeStepFromState() {
        double tsMin = Double.MAX_VALUE;
        for (FElement element : fenet.getFElements()) {
            final FEdge edge = (FEdge) element;
            final GroundwaterModel1DData g0 = getGroundwaterModel1DData(edge.getDOF(0));
            final GroundwaterModel1DData g1 = getGroundwaterModel1DData(edge.getDOF(1));
            final double astMean = 0.5 * (g0.kf / g0.S0 + g1.kf / g1.S0);
            if (astMean <= 1.e-12) {
                continue;
            }
            final double dx = edge.elm_size();
            final double ts = 0.5 * dx * dx / astMean;
            if (Double.isFinite(ts) && ts > 0.) {
                tsMin = Math.min(tsMin, ts);
            }
        }
        return tsMin < Double.MAX_VALUE ? tsMin : INITIAL_MAX_TIMESTEP;
    }

    // ----------------------------------------------------------------------
    // ToDO
    // ----------------------------------------------------------------------
    public ModelData genData(FElement felement) {
        return null;
    }

    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    public ModelData genData(DOF dof) {
        GroundwaterModel1DData md = new GroundwaterModel1DData();

        md.zG = interpolateZG(dof.x);
        md.h = interpolateH(dof.x);//md.zG;//+0.5;
        return md;
    }

    private double interpolateZG(double x) {
        double[][] feld_z = {{0., 25., 50., 75., 100.},
            {1., 2., 1.2, 1.7, 0.5}};
        double sumZ = 0.;
        double sum_dist = 0.;

        for (int j = 0; j < feld_z[0].length; j++) {

            double dist = (x - feld_z[0][j]) * (x - feld_z[0][j]);

            if (dist == 0.) {
                return feld_z[1][j];
            }
            sumZ += 1. / dist * feld_z[1][j];
            sum_dist += 1. / dist;
        }
        return sumZ / sum_dist;

    }

    private double interpolateH(double x) {
        double[][] feld_z = {{0., 25., 50., 75., 100.},
            {-0.5, -1.0, -0.5, -0.3, 1.5}};
        double sumZ = 0.;
        double sum_dist = 0.;

        for (int j = 0; j < feld_z[0].length; j++) {

            double dist = (x - feld_z[0][j]) * (x - feld_z[0][j]);

            if (dist == 0.) {
                return feld_z[1][j];
            }
            sumZ += 1. / dist * feld_z[1][j];
            sum_dist += 1. / dist;
        }
        return sumZ / sum_dist;

    }

    //------------------------------------------------------------------------
    // setBoundaryCondition
    //------------------------------------------------------------------------
    public void setBoundaryCondition(DOF dof, double t) {

        if (dof.number == 0) {
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            gwm.h = -0.5;
        }
        if (dof.number == numberofdofs - 1) {
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            gwm.h = 1.5;
        }
    }


    //------------------------------------------------------------------------
    // ElementApproximation
    //------------------------------------------------------------------------
    public double ElementApproximation(FElement element) {

        double timeStep=Double.POSITIVE_INFINITY;
        
        final FEdge ele = (FEdge) element;

        final double[][] koeffmat = ele.getkoeffmat();
        final double[] eta = new double[2];


        // compute element derivations
        double dhdx = 0.;
        double dh2dx2 = 0.; // fuer Diffusion
        double C_mean = 0.; // mittlere Konzentration
        double u_mean = 0.; // mittlere Geschwindigkeit
        double[] q = new double[2];

        //-----------------------------------------------------------------------
        // Modelldaten holen
        //-----------------------------------------------------------------------

        double astMean = 0.;
        //...Schleife ueber Freiheitsgerade der Elemente.........................
        for (int j = 0; j < 2; j++) {
            DOF dof = ele.getDOF(j);
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            astMean += 0.5 * gwm.kf / gwm.S0;

            CurrentModel1DData cmd = getCurrentModel1DData(dof);

            //Grundwasserpegel an Topographie anpassen
            if (gwm.h > -dof.z) {
            //  gwm.h=-dof.z;                
            }
            if (cmd != null) {
                // Wenn Oberflaechenwasser vorhanden, dann Grundwasserpegel anpassen
                if ((dof.z + cmd.h > CurrentModel1D.WATT)) {
                    q[j] = cmd.h - gwm.h;
                }
            } else {

            }

            dhdx += gwm.h * koeffmat[j][1];
        }

        if (astMean > 1.e-12) {
            final double dx = ele.elm_size();
            timeStep = 0.5 * dx * dx / astMean;
        }


        /* the 2. derivation is for linear interpolation in the element equal  0 */
        //also entfaellt der gesamte Stabilisierungs-Anteil (Least-Squares)


        for (int j = 0; j < 2; j++) {             // Schleife ueber Freiheitsgerade der Elemente
            DOF dof = ele.getDOF(j);
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);

            //bestimme Austauschkoeffizient aus hydr. Durchlaessigkeit und Porositaet des Bodens
            double ast = gwm.kf / gwm.S0;

            //...Galerkin-Approximation............................................
            double vorfaktor;
            for (int l = 0; l < 2; l++) {
                if (j == l) {
                    vorfaktor = 1. / 3.;
                } else {
                    vorfaktor = 1. / 6.;
                }
                gwm.rh -= vorfaktor * (ast * dhdx * koeffmat[j][1] - q[j]);
            }

        }

        return timeStep;
    }

    //------------------------------------------------------------------------
    // getGroundWaterModel1DData
    //------------------------------------------------------------------------
    private GroundwaterModel1DData getGroundwaterModel1DData(DOF dof) {
        GroundwaterModel1DData smd = null;
        Iterator<ModelData> modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = modeldatas.next();
            if (md instanceof GroundwaterModel1DData) {
                smd = (GroundwaterModel1DData) md;
            }
        }
        return smd;
    }


    //------------------------------------------------------------------------
    // getCurrentModel1DData
    //------------------------------------------------------------------------
    private CurrentModel1DData getCurrentModel1DData(DOF dof) {
        CurrentModel1DData cmd = null;
        Iterator<ModelData> modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = modeldatas.next();
            if (md instanceof CurrentModel1DData) {
                cmd = (CurrentModel1DData) md;
            }
        }
        return cmd;
    }

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    @Override
    public void timeStep(double dt) {
        for (DOF dof : fenet.getDOFs()) {
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            if ((gwm.zG + gwm.h) <= 0.) {
                gwm.h = -gwm.zG;
            }
            gwm.dhdx = 0.;
            gwm.rh = 0.;
        }

        setBoundaryConditions();
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
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            final double rh = beta0 * gwm.rh + beta1 * gwm.dhdt;
            gwm.dhdt = gwm.rh;
            gwm.h += dt * rh;
            if ((gwm.zG + gwm.h) <= 0.) {
                gwm.h = -gwm.zG;
            }
            gwm.rh = 0.;
        }

        previousTimeStep = dt;
        time += dt;
    }

    //------------------------------------------------------------------------
    // draw_it
    //------------------------------------------------------------------------
    public void draw_it(Graphics g, double[] x, double time) {
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
        final int marginRight = 20;
        final int marginTop = 25;
        final int marginBottom = 30;

        final int plotWidth = Math.max(10, width - marginLeft - marginRight);
        final int plotHeight = Math.max(10, height - marginTop - marginBottom);
        final int x0 = marginLeft;
        final int x1 = marginLeft + plotWidth;
        final int yTop = marginTop;
        final int yBottom = marginTop + plotHeight;

        double xMinNow = Double.POSITIVE_INFINITY;
        double xMaxNow = Double.NEGATIVE_INFINITY;
        double yMinNow = Double.POSITIVE_INFINITY;
        double yMaxNow = Double.NEGATIVE_INFINITY;

        for (DOF dof : dofs) {
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            final double zTop = -dof.z;
            final double zG = -gwm.zG;
            xMinNow = Math.min(xMinNow, dof.x);
            xMaxNow = Math.max(xMaxNow, dof.x);
            yMinNow = Math.min(yMinNow, Math.min(zTop, Math.min(gwm.h, zG)));
            yMaxNow = Math.max(yMaxNow, Math.max(zTop, Math.max(gwm.h, zG)));
        }

        if (xMaxNow <= xMinNow) xMaxNow = xMinNow + 1.0;
        if (yMaxNow <= yMinNow) yMaxNow = yMinNow + 1.0;

        // Skalierung nur erweitern, niemals verkleinern
        if (!Double.isFinite(drawXMin) || xMinNow < drawXMin) drawXMin = xMinNow;
        if (!Double.isFinite(drawXMax) || xMaxNow > drawXMax) drawXMax = xMaxNow;
        if (!Double.isFinite(drawYMin) || yMinNow < drawYMin) drawYMin = yMinNow;
        if (!Double.isFinite(drawYMax) || yMaxNow > drawYMax) drawYMax = yMaxNow;
        if (drawXMax <= drawXMin) drawXMax = drawXMin + 1.0;
        if (drawYMax <= drawYMin) drawYMax = drawYMin + 1.0;

        g.setColor(Color.black);
        g.drawRect(x0, yTop, plotWidth, plotHeight);
        g.drawString("Groundwater [m]", x0 + 5, yTop + 14);
        g.drawString(String.format("t = %.2f s", time), Math.max(x0 + 5, x1 - 120), yTop + 14);
        g.drawString(String.format("%.2f", drawYMax), 5, yTop + 5);
        g.drawString(String.format("%.2f", drawYMin), 5, yBottom);

        final int yZero = yBottom - (int) ((0.0 - drawYMin) / (drawYMax - drawYMin) * plotHeight);
        if (yZero >= yTop && yZero <= yBottom) {
            g.setColor(Color.lightGray);
            g.drawLine(x0, yZero, x1, yZero);
        }

        for (int i = 0; i < dofs.length - 1; i++) {
            DOF dof0 = dofs[i];
            DOF dof1 = dofs[i + 1];
            GroundwaterModel1DData gwm0 = getGroundwaterModel1DData(dof0);
            GroundwaterModel1DData gwm1 = getGroundwaterModel1DData(dof1);

            int px0 = x0 + (int) ((dof0.x - drawXMin) / (drawXMax - drawXMin) * plotWidth);
            int px1 = x0 + (int) ((dof1.x - drawXMin) / (drawXMax - drawXMin) * plotWidth);

            int pyTop0 = yBottom - (int) (((-dof0.z) - drawYMin) / (drawYMax - drawYMin) * plotHeight);
            int pyTop1 = yBottom - (int) (((-dof1.z) - drawYMin) / (drawYMax - drawYMin) * plotHeight);
            g.setColor(Color.black);
            g.drawLine(px0, pyTop0, px1, pyTop1);

            int pyH0 = yBottom - (int) ((gwm0.h - drawYMin) / (drawYMax - drawYMin) * plotHeight);
            int pyH1 = yBottom - (int) ((gwm1.h - drawYMin) / (drawYMax - drawYMin) * plotHeight);
            g.setColor(Color.magenta);
            g.drawLine(px0, pyH0, px1, pyH1);

            int pyZG0 = yBottom - (int) (((-gwm0.zG) - drawYMin) / (drawYMax - drawYMin) * plotHeight);
            int pyZG1 = yBottom - (int) (((-gwm1.zG) - drawYMin) / (drawYMax - drawYMin) * plotHeight);
            g.setColor(Color.red);
            g.drawLine(px0, pyZG0, px1, pyZG1);
        }

        g.setColor(Color.black);
        g.drawString("Topographie", x0 + 10, height - 8);
        g.setColor(Color.magenta);
        g.drawString("h", x0 + 90, height - 8);
        g.setColor(Color.red);
        g.drawString("zG", x0 + 110, height - 8);
    }

    @Override
    public void write_erg_xf() {
    }
}
