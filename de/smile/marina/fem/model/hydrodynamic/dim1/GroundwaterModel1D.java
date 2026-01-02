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
import java.awt.*;
import java.util.*;

public class GroundwaterModel1D extends TimeDependentFEApproximation implements FEModel {

    private int n,  H,  numberofdofs;
    private double[] result;

    /** Creates new SedimentModel1D */
    public GroundwaterModel1D(FEDecomposition fe) {
        fenet = fe;
        femodel = this;
        // DOFs initialisieren
        initialDOFs();

        numberofdofs = fenet.getNumberofDOFs();
        H = 0;
        n = numberofdofs;
        result = new double[n];
    }

    //------------------------------------------------------------------------
    // initialSolution
    //------------------------------------------------------------------------
    //...Anfangswertberechnung...............................................  
    public double[] initialSolution(double time) {
        double x[] = new double[getResultSize()];

        System.out.println("GroundwaterModel - Werte Initialisieren");
        DOF[] dof = fenet.getDOFs();
        for (int j = 0; j < dof.length; j++) {
            int i = dof[j].number;
            GroundwaterModel1DData smd = getGroundwaterModel1DData(dof[j]);
            x[H + i] = smd.h;
        }
        return x;
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

        //...Schleife ueber Freiheitsgerade der Elemente.........................
        for (int j = 0; j < 2; j++) {
            DOF dof = ele.getDOF(j);
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);

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
                gwm.rh -= (ast * dhdx * koeffmat[j][1] - q[j]);
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
    // getResultSize
    //------------------------------------------------------------------------
    public int getResultSize() {
        return n;
    }

    //------------------------------------------------------------------------
    // getRateofChange
    //------------------------------------------------------------------------
    public double[] getRateofChange(double p1, double[] x) {

        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            if ((gwm.zG + x[H + i]) <= 0.) {
                x[H + i] = -gwm.zG;
            }
            gwm.h = x[H + i];
            setBoundaryCondition(dof, p1);
            gwm.dhdt = gwm.rh;
            gwm.dhdx = 0.;
            // set Results to zero
            gwm.rh = 0.;
        }

        // Elementloop
        performElementLoop();

        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            GroundwaterModel1DData gwm = getGroundwaterModel1DData(dof);
            result[H + i] = gwm.rh;
        }

        return result;
    }

    //------------------------------------------------------------------------
    // draw_it
    //------------------------------------------------------------------------
    public void draw_it(Graphics g, double[] x, double time) {

        int anz = fenet.getNumberofDOFs();
        g.clearRect(0, 0, 800, 400);
        g.setColor(Color.white);
        g.fillRect(0, 0, 800, 400);


        g.setColor(Color.black);
        for (int k = 0; k < anz - 1; k++) {

            g.drawLine((int) (5 * fenet.getDOF(k).x) + 100, 400 - ((int) (-50. * fenet.getDOF(k).z) + 200),
                    (int) (5 * fenet.getDOF(k + 1).x + 100), 400 - ((int) (-50. * fenet.getDOF(k + 1).z) + 200));
        }


        for (int k = 0; k < anz - 1; k++) {
            DOF dof0 = fenet.getDOF(k);
            DOF dof1 = fenet.getDOF(k + 1);
            GroundwaterModel1DData gwm0 = getGroundwaterModel1DData(dof0);
            GroundwaterModel1DData gwm1 = getGroundwaterModel1DData(dof1);

            double h0 = gwm0.h;
            double h1 = gwm1.h;
            g.setColor(Color.magenta);
            g.drawLine((int) (5 * fenet.getDOF(k).x) + 100, (int) (-50. * h0) + 200,
                    (int) (5 * fenet.getDOF(k + 1).x + 100), (int) (-50. * h1) + 200);

            double z0 = -gwm0.zG;
            double z1 = -gwm1.zG;
            g.setColor(Color.red);
            g.drawLine((int) (5 * fenet.getDOF(k).x) + 100, (int) (-50. * z0) + 200,
                    (int) (5 * fenet.getDOF(k + 1).x + 100), (int) (-50. * z1) + 200);

        }

    }
}
