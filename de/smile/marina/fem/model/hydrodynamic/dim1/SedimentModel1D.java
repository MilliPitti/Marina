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

public class SedimentModel1D extends TimeDependentFEApproximation implements FEModel, TimeDependentModel {
    static final double G = 9.81;
    static final double AST=0.0012;	     	//0.0012 Austauschkoeffizient fuer Stroemung
    static final double INITIAL_MAX_TIMESTEP = 0.001;
    static final double MORPH_FACTOR = 1.0;
    static final double MORPH_POROSITY = 0.40;
    static final double MAX_CONCENTRATION = 0.5;
    static final double MAX_DZ_PER_STEP = 0.002;
    static final double MAX_EROSION_DEPTH = 5.0;

    private double previousTimeStep = 0.0;
    private double drawXMin = Double.NaN;
    private double drawXMax = Double.NaN;
    private double drawZHMin = Double.NaN;
    private double drawZHMax = Double.NaN;
    private double drawCMin = Double.NaN;
    private double drawCMax = Double.NaN;

    /** Creates new SedimentModel1D */
    public SedimentModel1D(FEDecomposition fe) {
	fenet = fe;
	femodel=this;
	// DOFs initialisieren
	initialDOFs();

        setMaxTimeStep(INITIAL_MAX_TIMESTEP);
    }

    //------------------------------------------------------------------------
    // initialSolution
    //------------------------------------------------------------------------
    //...Anfangswertberechnung...............................................  
    public void initialSolution(double time){
	System.out.println("SedimentModel - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            SedimentModel1DData smd = getSedimentModel1DData(dof);
            smd.dCdt = 0.;
            if (!Double.isFinite(smd.z)) smd.z = dof.z;
            if (!Double.isFinite(smd.z0)) smd.z0 = smd.z;
            smd.dzdt = 0.;
            smd.rZ = 0.;
        }
        setMaxTimeStep(estimateCourantTimeStepFromState());
    }

    private double estimateCourantTimeStepFromState() {
        double tsMin = Double.MAX_VALUE;
        for (FElement element : fenet.getFElements()) {
            final FEdge edge = (FEdge) element;
            final CurrentModel1DData c0 = CurrentModel1DData.extract(edge.getDOF(0));
            final CurrentModel1DData c1 = CurrentModel1DData.extract(edge.getDOF(1));

            final double u0 = (c0 != null) ? c0.u : 0.1;
            final double u1 = (c1 != null) ? c1.u : 0.1;
            final double uMean = 0.5 * (u0 + u1);
            final double dx = edge.elm_size();

            final double ast0 = AST + Math.pow(AST * dx, 2.) * Math.abs(u0);
            final double ast1 = AST + Math.pow(AST * dx, 2.) * Math.abs(u1);
            final double astMean = 0.5 * (ast0 + ast1);

            double tsAdv = Double.POSITIVE_INFINITY;
            if (Math.abs(uMean) > 1.e-8) {
                tsAdv = 0.5 * dx / Math.abs(uMean);
            }
            double tsDiff = Double.POSITIVE_INFINITY;
            if (astMean > 1.e-12) {
                tsDiff = 0.5 * dx * dx / astMean;
            }

            double ts = Math.min(tsAdv, tsDiff);
            if (Double.isFinite(ts) && ts > 0.) {
                tsMin = Math.min(tsMin, ts);
            }
        }
        return tsMin < Double.MAX_VALUE ? tsMin : INITIAL_MAX_TIMESTEP;
    }

    // ----------------------------------------------------------------------
    // ToDO
    // ----------------------------------------------------------------------
	public ModelData genData(FElement felement)
	{return null;}

    //------------------------------------------------------------------------
    // genData
    //------------------------------------------------------------------------
    public ModelData genData(DOF dof){
	SedimentModel1DData md = new SedimentModel1DData();
	if (dof.x<40 || dof.x>60) md.C=0.1;        // Konzentration  0.1
	else md.C=0.15;                            // Sprung auf     0.2
	md.z = dof.z;
	md.z0 = dof.z;
	return md;
    }

    //------------------------------------------------------------------------
    // setBoundaryCondition
    //------------------------------------------------------------------------
    public void setBoundaryCondition(DOF dof,double t) {
    }


    //------------------------------------------------------------------------
    // ElementApproximation
    //------------------------------------------------------------------------
    public double ElementApproximation(FElement element) {

    double timeStep=Double.POSITIVE_INFINITY;
        
	final FEdge ele = (FEdge)element;

	final double[][] koeffmat = ele.getkoeffmat();
	final double[] u = new double[2];
	final double[] qs = new double[2];

	// compute element derivations
	double dCdx=0.;
	double dQsdx=0.;
	double u_mean = 0.; // mittlere Geschwindigkeit
    double u_morph_mean = 0.; // mittlere Geschwindigkeit fuer morphologische Aenderung
    
	//-----------------------------------------------------------------------
	// Modelldaten holen
	//-----------------------------------------------------------------------

	//...Schleife ueber Freiheitsgerade der Elemente.........................
	for ( int j = 0; j < 2; j++) {
	    DOF dof = ele.getDOF(j);
	    SedimentModel1DData smd = getSedimentModel1DData(dof);
        CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
		if(cmd!=null) {        // Wenn Geschwindigkeit vorhanden, dann zuweisen
			u[j]=cmd.u;
		} else {
			u[j]=0; 
		}
  
        qs[j] = u[j] * smd.C;
        u_mean+=u[j]/2.;
        u_morph_mean += qs[j]/ (cmd.h+smd.z) / 2.;
		dCdx += smd.C * koeffmat[j][1];
        dQsdx += qs[j] * koeffmat[j][1];
	}

	double residuum = 0.;
	double residuumZ = 0.;
        
	double tau_sed = 0.;
    final double dx = ele.elm_size();
	if(Math.abs(u_mean)>0.0001){
		tau_sed = 0.5 * dx / Math.abs(u_mean);
		timeStep = tau_sed;
	}
    double tau_z = 0.;
    if(Math.abs(u_morph_mean)>0.0001){
	    tau_z = 0.5 * dx / Math.abs(u_morph_mean);
		timeStep = Math.min(timeStep,tau_z);
	}
    double ast = AST  + Math.pow(AST*ele.elm_size(),2.)*Math.abs(u_mean);    

	//-----------------------------------------------------------------------
	// Berechnung des lokalen Fehlers im Element
	//-----------------------------------------------------------------------

	//...Schleife ueber Freiheitsgerade der Elemente.........................
	for ( int j = 0; j < 2; j++) {
	    DOF dof = ele.getDOF(j);
	    SedimentModel1DData smd = getSedimentModel1DData(dof);
	    // Fehlerberechnung
	    residuum += 1./2. * ( smd.dCdt + u[j] * dCdx );
        residuumZ += 1./2. * (smd.dzdt + dQsdx / (1. - MORPH_POROSITY));
	}

	for (int j=0;j<2;j++){             // Schleife ueber Freiheitsgerade der Elemente
	    DOF dof = ele.getDOF(j);
	    SedimentModel1DData smd = getSedimentModel1DData(dof);

	    //...Fehlerkorrektur / Stabilisierung..................................
	    smd.rC -= tau_sed * koeffmat[j][1] * u_mean * residuum;
        smd.rZ += tau_z * koeffmat[j][1] * u_morph_mean * residuumZ;
	      
	    //...Galerkin-Approximation............................................
	    double vorfaktor;
	      for (int l=0;l<2;l++){
		if(j==l) vorfaktor=1./3.;
		else vorfaktor=1./6.;
		    smd.rC -= vorfaktor * (u[l]*dCdx + 2.*ast*dCdx*koeffmat[j][1]);
                    smd.rZ -= vorfaktor * (dQsdx / Math.max(1.e-6, (1. - MORPH_POROSITY)));
	      }
		    smd.dCdx += 0.5 * dCdx;
		}
        return timeStep;
    }

  
    //------------------------------------------------------------------------
    // getSedimentModel1DData
    //------------------------------------------------------------------------
    private SedimentModel1DData getSedimentModel1DData(DOF dof){
	SedimentModel1DData smd=null;
	Iterator<ModelData> modeldatas = dof.allModelDatas();
	while (modeldatas.hasNext()) {
	    ModelData md = modeldatas.next();
	    if(md instanceof SedimentModel1DData)  smd = ( SedimentModel1DData )md;
	}
	return smd;
    }

    private double getBedLevel(DOF dof, SedimentModel1DData smd) {
        if (smd != null && Double.isFinite(smd.z)) {
            return smd.z;
        }
        return dof.z;
    }

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    @Override
    public void timeStep(double dt) {
	    DOF[] dof = fenet.getDOFs();
        for (DOF value : dof) {
	        SedimentModel1DData current = getSedimentModel1DData(value);
	        current.dCdx = 0.;
	        current.rC = 0.;
            current.rZ = 0.;
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

        for (DOF value : dof) {
            SedimentModel1DData current = getSedimentModel1DData(value);
            final double rC = beta0 * current.rC + beta1 * current.dCdt;
            double rZ = beta0 * current.rZ + beta1 * current.dzdt;
            current.dCdt = current.rC;
            current.dzdt = current.rZ;
            current.C += dt * rC;
            if (current.C < 0.) {
                current.C = 0.;
            }
            if (current.C > MAX_CONCENTRATION) {
                current.C = MAX_CONCENTRATION;
            }

            // Exner-Update in Anlehnung an SedimentModel2D:
            // z(t+dt) = z(t) + dt * rZ * morphFactor
            if (!Double.isFinite(rZ)) rZ = 0.;
            double newZ = current.z + dt * rZ * MORPH_FACTOR;
            CurrentModel1DData cmd = CurrentModel1DData.extract(value);
            if (cmd != null) {
                // Bett darf nicht ueber die Wasseroberflaeche wachsen (Mindestwassertiefe WATT)
                double minBedLevel = Math.max(1.e-4, CurrentModel1D.WATT - cmd.h);
                if (newZ < minBedLevel) {
                    newZ = minBedLevel;
                    rZ = (newZ - current.z) / (dt * MORPH_FACTOR);
                }
            }
            // nicht-erodierbarer Horizont analog 2D (hier relativ zum Startboden)
            double maxBedLevel = current.z0 + MAX_EROSION_DEPTH;
            if (newZ > maxBedLevel) {
                newZ = maxBedLevel;
                rZ = (newZ - current.z) / (dt * MORPH_FACTOR);
            }
            // Morphologische Aenderung pro internem Rechenschritt begrenzen
            double dzStep = newZ - current.z;
            if (dzStep > MAX_DZ_PER_STEP) {
                dzStep = MAX_DZ_PER_STEP;
                newZ = current.z + dzStep;
                rZ = dzStep / (dt * MORPH_FACTOR);
            } else if (dzStep < -MAX_DZ_PER_STEP) {
                dzStep = -MAX_DZ_PER_STEP;
                newZ = current.z + dzStep;
                rZ = dzStep / (dt * MORPH_FACTOR);
            }
            current.z = newZ;
            current.dzdt = rZ;

            current.rC = 0.;
            current.rZ = 0.;
        }

        previousTimeStep = dt;
        time += dt;
    }

    //------------------------------------------------------------------------
    // draw_it
    //------------------------------------------------------------------------
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

        double xMinNow = Double.POSITIVE_INFINITY;
        double xMaxNow = Double.NEGATIVE_INFINITY;
        double zhMinNow = Double.POSITIVE_INFINITY;
        double zhMaxNow = Double.NEGATIVE_INFINITY;
        double cMinNow = Double.POSITIVE_INFINITY;
        double cMaxNow = Double.NEGATIVE_INFINITY;

        for (DOF dof : dofs) {
            SedimentModel1DData smd = getSedimentModel1DData(dof);
            CurrentModel1DData cmd = CurrentModel1DData.extract(dof);
            final double zDraw = -getBedLevel(dof, smd); // z-Achse zeigt nach unten
            final double h = (cmd != null) ? cmd.h : 0.;
            xMinNow = Math.min(xMinNow, dof.x);
            xMaxNow = Math.max(xMaxNow, dof.x);
            zhMinNow = Math.min(zhMinNow, Math.min(zDraw, h));
            zhMaxNow = Math.max(zhMaxNow, Math.max(zDraw, h));
            cMinNow = Math.min(cMinNow, smd.C);
            cMaxNow = Math.max(cMaxNow, smd.C);
        }

        if (xMaxNow <= xMinNow) xMaxNow = xMinNow + 1.0;
        if (zhMaxNow <= zhMinNow) zhMaxNow = zhMinNow + 1.0;
        if (cMaxNow <= cMinNow) cMaxNow = cMinNow + 1.0e-3;

        if (!Double.isFinite(drawXMin) || xMinNow < drawXMin) drawXMin = xMinNow;
        if (!Double.isFinite(drawXMax) || xMaxNow > drawXMax) drawXMax = xMaxNow;
        if (!Double.isFinite(drawZHMin) || zhMinNow < drawZHMin) drawZHMin = zhMinNow;
        if (!Double.isFinite(drawZHMax) || zhMaxNow > drawZHMax) drawZHMax = zhMaxNow;
        if (!Double.isFinite(drawCMin) || cMinNow < drawCMin) drawCMin = cMinNow;
        if (!Double.isFinite(drawCMax) || cMaxNow > drawCMax) drawCMax = cMaxNow;

        if (drawXMax <= drawXMin) drawXMax = drawXMin + 1.0;
        if (drawZHMax <= drawZHMin) drawZHMax = drawZHMin + 1.0;
        if (drawCMax <= drawCMin) drawCMax = drawCMin + 1.0e-3;

        g.setColor(Color.black);
        g.drawRect(x0, yTop, plotWidth, plotHeight);
        g.drawString("z/h [m]", x0 + 5, yTop + 14);
        g.drawString("C [-]", x1 - 45, yTop + 14);
        g.drawString(String.format("t = %.2f s", time), Math.max(x0 + 5, x1 - 120), yTop + 14);
        g.drawString(String.format("%.2f", drawZHMax), 5, yTop + 5);
        g.drawString(String.format("%.2f", drawZHMin), 5, yBottom);
        g.drawString(String.format("%.3f", drawCMax), x1 + 5, yTop + 5);
        g.drawString(String.format("%.3f", drawCMin), x1 + 5, yBottom);

        final int yZeroZH = yBottom - (int) ((0.0 - drawZHMin) / (drawZHMax - drawZHMin) * plotHeight);
        if (yZeroZH >= yTop && yZeroZH <= yBottom) {
            g.setColor(Color.lightGray);
            g.drawLine(x0, yZeroZH, x1, yZeroZH);
        }
        if (drawCMin <= 0.0 && drawCMax >= 0.0) {
            final int yZeroC = yBottom - (int) ((0.0 - drawCMin) / (drawCMax - drawCMin) * plotHeight);
            g.setColor(Color.lightGray);
            g.drawLine(x0, yZeroC, x1, yZeroC);
        }

        g.setColor(new Color(139, 69, 19));
        g.drawString("z", x0 + 60, yTop + 14);
        g.setColor(Color.blue);
        g.drawString("h", x0 + 75, yTop + 14);
        g.setColor(Color.red);
        g.drawString("C", x0 + 90, yTop + 14);

        for (int i = 0; i < dofs.length - 1; i++) {
            final DOF d0 = dofs[i];
            final DOF d1 = dofs[i + 1];
            final SedimentModel1DData s0 = getSedimentModel1DData(d0);
            final SedimentModel1DData s1 = getSedimentModel1DData(d1);
            final CurrentModel1DData c0 = CurrentModel1DData.extract(d0);
            final CurrentModel1DData c1 = CurrentModel1DData.extract(d1);

            final double h0 = (c0 != null) ? c0.h : 0.;
            final double h1 = (c1 != null) ? c1.h : 0.;
            final double z0Draw = -getBedLevel(d0, s0);
            final double z1Draw = -getBedLevel(d1, s1);

            int px0 = x0 + (int) ((d0.x - drawXMin) / (drawXMax - drawXMin) * plotWidth);
            int px1 = x0 + (int) ((d1.x - drawXMin) / (drawXMax - drawXMin) * plotWidth);

            int pz0 = yBottom - (int) ((z0Draw - drawZHMin) / (drawZHMax - drawZHMin) * plotHeight);
            int pz1 = yBottom - (int) ((z1Draw - drawZHMin) / (drawZHMax - drawZHMin) * plotHeight);
            g.setColor(new Color(139, 69, 19));
            g.drawLine(px0, pz0, px1, pz1);

            int ph0 = yBottom - (int) ((h0 - drawZHMin) / (drawZHMax - drawZHMin) * plotHeight);
            int ph1 = yBottom - (int) ((h1 - drawZHMin) / (drawZHMax - drawZHMin) * plotHeight);
            g.setColor(Color.blue);
            g.drawLine(px0, ph0, px1, ph1);

            int pC0 = yBottom - (int) ((s0.C - drawCMin) / (drawCMax - drawCMin) * plotHeight);
            int pC1 = yBottom - (int) ((s1.C - drawCMin) / (drawCMax - drawCMin) * plotHeight);
            g.setColor(Color.red);
            g.drawLine(px0, pC0, px1, pC1);
        }
    }

    // Kompatibilitaet fuer vorhandene Aufrufe
    public void draw_it(Graphics g, double[] x, double time) {
        draw_it(g, time);
    }

    @Override
    public void write_erg_xf() {
    }
}
