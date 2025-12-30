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
/**
 *
 * @author  Peter Milbradt
 * @version
 */
import bijava.graphics.JCanvas;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEdge;
import de.smile.math.ode.ivp.HeunTStep;
import de.smile.math.ode.ivp.SimpleTStep;
import javax.swing.*;
import java.awt.*;

public class MainCurrentSediment1D extends Object {

  FEDecomposition fed = new FEDecomposition();

  JFrame frame;
  JCanvas jcanvas;

  /** Creates new Main */
  public MainCurrentSediment1D () {
    frame = new JFrame("Sediment");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setSize(800,400);
    frame.getContentPane().setLayout( new BorderLayout());
    jcanvas = new JCanvas();
    jcanvas.setSize(800,400);
    frame.getContentPane().add(jcanvas);
    frame.setVisible(true);

    // Create FEDecomposition
    DOF b;
    DOF e = new DOF(0,0.,0.,1.);
    for( int m = 1;m < 500; m++) {
      b = e;
      e = new DOF(m,(double) m/5.,0.,1.);
      fed.addFElement(new FEdge(b,e));
    }

    CurrentModel1D  current1d  = new CurrentModel1D(fed);   // Stroemungsmodell
    SedimentModel1D sediment1d = new SedimentModel1D(fed);  // Transportmodell

    double[] currenterg  = current1d.initialSolution(0.);   // Anfangswerte (Initialisierung)
    double[] sedimenterg = sediment1d.initialSolution(0.);  // Anfangswerte (Initialisierung)

    //    sediment1d.setNumberOfThreads(2);

//    RKETStep methode = new RK_2_3_TStep();
//    SimpleTStep methode = new EulerTStep();
    SimpleTStep methode = new HeunTStep();
    //ABMTStep   methode = new ABMTStep();

    Graphics g = jcanvas.getGraphics();
    
    current1d.setMaxTimeStep(0.01);                             // Zeitschritt
    sediment1d.setMaxTimeStep(0.01);                            // Zeitschritt

    double startTime = 0.0;     // [sec]
    double endTime   = 44700;   // [sec]
    double dt        = 1.0;    //0.1;

    //double dt=sd.dt;
    //... Schleife ueber die Zeit ...........................................
    for (double t=startTime;t<endTime;t+=dt) {
	
	double ta=t;
	double te=t+dt;
	double ts = 0.;

	//...Schleife ueber einen Zeitschritt................................
	do {
	    double ts1,ts2;
	    ts1 = sediment1d.getMaxTimeStep();
	    ts2 = current1d.getMaxTimeStep();
		   
	    if ((currenterg != null) && (sedimenterg != null)) 
		ts  = Math.min(ts1,ts2); 
	    else if (sedimenterg != null) 
		ts  = ts1; 
	    else if (currenterg != null) 
		ts  = ts2; 
	    
	    if((ta+ts)>te) ts = te - ta;

	    if (currenterg  != null) currenterg  = methode.TimeStep(current1d,  ta, ts, currenterg);
	    if (sedimenterg != null) sedimenterg = methode.TimeStep(sediment1d, ta, ts, sedimenterg);

	    ta+=ts;

	} while (ta<te);
	
	if (sedimenterg  != null) sediment1d.draw_it(jcanvas.getGraphics(), sedimenterg,  t+dt);
	if (currenterg  != null)  current1d.draw_it(jcanvas.getGraphics(), currenterg,   t+dt);
	jcanvas.repaint();

    } // end for 
    
  }  // end public MainCurrentSediment1D ()


  /**
   * @param args the command line arguments
   */
  public static void main (String args[]) {
    MainCurrentSediment1D e = new MainCurrentSediment1D();
  }

}
