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
import de.smile.marina.fem.FEApproximation;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FEdge;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TimeDependentFEApproximation;
import java.awt.*;
import java.util.*;
import de.smile.math.ode.ivp.ODESystem;

public class SedimentModel1D extends TimeDependentFEApproximation implements FEModel,ODESystem {
    static final double G = 9.81;
    static final double AST=0.0012;	     	//0.0012 Austauschkoeffizient fuer Stroemung

    private int n,C,numberofdofs;
    private double MaxTimeStep;
    private double[] result;

    /** Creates new SedimentModel1D */
    public SedimentModel1D(FEDecomposition fe) {
	fenet = fe;
	femodel=this;
	// DOFs initialisieren
	initialDOFs();

	numberofdofs = fenet.getNumberofDOFs();
	C = 0;
	n = numberofdofs;
	result = new double[n];
    }

    //------------------------------------------------------------------------
    // initialSolution
    //------------------------------------------------------------------------
    //...Anfangswertberechnung...............................................  
    public double[] initialSolution(double time){
	double x[] = new double[getResultSize()];

	System.out.println("SedimentModel - Werte Initialisieren");
        for (DOF dof : fenet.getDOFs()) {
            int i = dof.number;
            SedimentModel1DData smd = getSedimentModel1DData(dof);
            x[C + i] = smd.C;
        }
	return x;
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

	// compute element derivations
	double dCdx=0.;
	double dC2dx2 = 0.; // fuer Diffusion
	double C_mean = 0.; // mittlere Konzentration
	double u_mean = 0.; // mittlere Geschwindigkeit
    
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
		u[j]=0.1;          // ansonsten mit 0.1 belegen (Transportgeschwindigkeit)
	    }
	    u_mean+=u[j]/2.;
	    dCdx += smd.C * koeffmat[j][1];
	}

	double residuum = 0.;
        
        double tau_sed = 0.;
	if(Math.abs(u_mean)>0.0001){
		tau_sed = 0.5 * ele.elm_size() / Math.abs(u_mean);
		timeStep = tau_sed;
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
	}
	//    System.out.println("Fehler = "+cureq1_mean+" "+cureq2_mean);

	for (int j=0;j<2;j++){             // Schleife ueber Freiheitsgerade der Elemente
	    DOF dof = ele.getDOF(j);
	    SedimentModel1DData smd = getSedimentModel1DData(dof);

	    //...Fehlerkorrektur / Stabilisierung..................................
	    smd.rC -= tau_sed * koeffmat[j][1] * u_mean * residuum;
      
	    //...Galerkin-Approximation............................................
	    double vorfaktor;
      for (int l=0;l<2;l++){
	if(j==l) vorfaktor=1./3.;
	else vorfaktor=1./6.;
	    smd.rC -= vorfaktor * (u[l]*dCdx + 2.*ast*dCdx*koeffmat[j][1]);
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



    //------------------------------------------------------------------------
    // getResultSize
    //------------------------------------------------------------------------
    public int getResultSize() {
	return n;
    }

    //------------------------------------------------------------------------
    // getRateofChange
    //------------------------------------------------------------------------
    public double[] getRateofChange(double p1,double[] x) {


	DOF[] dof = fenet.getDOFs();
        for (int j=0; j<dof.length;j++){
            int i = dof[j].number;
	    SedimentModel1DData current = getSedimentModel1DData(dof[j]);

	    current.C = x[C + i];

	    current.dCdt=current.rC;

	    current.dCdx = 0.;
	    // set Results to zero
	    current.rC=0.;
	}

	MaxTimeStep = 10000.;

	// Elementloop
	performElementLoop();

	dof = fenet.getDOFs();
        for (int j=0; j<dof.length;j++){
            int i = dof[j].number;
	    SedimentModel1DData current = getSedimentModel1DData(dof[j]);

	    result[C+i] = current.rC;
	}

	return result;
    }

    //------------------------------------------------------------------------
    // draw_it
    //------------------------------------------------------------------------
    public void draw_it( Graphics g, double[] x, double time) {

	int anz=fenet.getNumberofDOFs();

	g.setColor( Color.blue );
	for( int k=0;k<anz-1;k++) {
	    g.drawLine(	(int)(5*fenet.getDOF(k).x)+100, (int)(500.*x[C+k])+200,
			(int)(5*fenet.getDOF(k+1).x+100), (int)(500.*x[C+k+1])+200);
	}
	/*
	  g.setColor( Color.red );
	  for( int k=0;k<anz-1;k++) {
	  g.drawLine(	(int)(5*fenet.getDOF(k).x+100), (int)(500.*x[U+k])+200,
	  (int)(5*fenet.getDOF(k+1).x+100), (int)(500.*x[U+k+1])+200);
	  }*/
	g.setColor( Color.blue );

    }
}
