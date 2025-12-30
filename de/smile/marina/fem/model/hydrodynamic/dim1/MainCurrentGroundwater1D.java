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

public class MainCurrentGroundwater1D extends Object {

  FEDecomposition fed = new FEDecomposition();

  JFrame frame;
  JCanvas jcanvas;

    
  
  /** Creates new Main */
  public MainCurrentGroundwater1D () {
    frame = new JFrame("Groundwater");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setSize(800,400);
    frame.getContentPane().setLayout( new BorderLayout());
    jcanvas = new JCanvas();
    jcanvas.setSize(800,400);
    frame.getContentPane().add(jcanvas);
    frame.setVisible(true);

    
  
       
          
    /*double[] z=new double[500];
     for( int m = 0;m < 250; m++) {
        z[m]=-0.1+(double)m/250.*1.1;
     }
    for( int m = 0;m < 250; m++) {
        z[m+250]=1-(double)m/250.*1.1;
     }*/
    
    // Create FEDecomposition
    DOF b;
    DOF e = new DOF(0,0.,0.,interpolateZ(0.));
    for( int m = 1;m < 600; m++) {
      b = e;
      double x=(double) m/6.;
      
      e = new DOF(m,x,0., interpolateZ(x));
      fed.addFElement(new FEdge(b,e));
    }

    CurrentModel1D  current1d  = new CurrentModel1D(fed);   // Stroemungsmodell
    GroundwaterModel1D groundwater1d = new GroundwaterModel1D(fed);  // Transportmodell

    double[] currenterg  = current1d.initialSolution(0.);   // Anfangswerte (Initialisierung)
    double[] groundwatererg = groundwater1d.initialSolution(0.);  // Anfangswerte (Initialisierung)

    //    sediment1d.setNumberOfThreads(2);

//    RKETStep methode = new RK_2_3_TStep();
//    SimpleTStep methode = new EulerTStep();
    SimpleTStep methode = new HeunTStep();
    //ABMTStep   methode = new ABMTStep();

    Graphics g = jcanvas.getGraphics();
    
    current1d.setMaxTimeStep(0.01);                             // Zeitschritt
    groundwater1d.setMaxTimeStep(0.01);                            // Zeitschritt

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
	    ts1 = groundwater1d.getMaxTimeStep();
	    ts2 = current1d.getMaxTimeStep();
		   
	    if ((currenterg != null) && (groundwatererg != null)) 
		ts  = Math.min(ts1,ts2); 
	    else if (groundwatererg != null) 
		ts  = ts1; 
	    else if (currenterg != null) 
		ts  = ts2; 
	    
	    if((ta+ts)>te) ts = te - ta;

	    if (currenterg  != null) currenterg  = methode.TimeStep(current1d,  ta, ts, currenterg);
	    if (groundwatererg != null) groundwatererg = methode.TimeStep(groundwater1d, ta, ts, groundwatererg);

	    ta+=ts;

	} while (ta<te);
	
        
	if (groundwatererg  != null) groundwater1d.draw_it(jcanvas.getGraphics(), groundwatererg,  t+dt);
        if (currenterg  != null)  current1d.draw_it(jcanvas.getGraphics(), currenterg,   t+dt);
	
	jcanvas.repaint();
        System.out.println(t);

    } // end for 
    
  }  // end public MainCurrentSediment1D ()


  private double interpolateZ(double x){
       double[][] feld_z =  {{0.,2.5,5.,7.5,10.,12.5,15.,17.5,20.,22.5,25.,27.5,30.,32.5,35.,37.5,40.,42.5,45.,47.5,50.,52.5,55.,57.5,60.,62.5,65.,67.5,70.,72.5,75.,77.5,80.,82.5,85.,87.5,90.,92.5,95.,97.5,100.},
                           {-0.1,0.0,0.3,0.6,0.2,0.0,-0.15,-0.18,-0.4,-0.8,-1.,-1.2,-0.95,-0.5,-0.2,0.0,0.85,1.2,1.,0.75,0.65,0.67,0.15,-0.55,-1.,-1.25,-1.22,-1.2,-1.45,-1.66,-1.,-0.4,0.1,-0.3,-0.7,-1.,-1.45,-1.7,-1.9,-2.,-2.2}};
       double sumZ=0.;
       double sum_dist=0.;
      
       for(int j=0;j<feld_z[0].length;j++){
          
          double dist=(x-feld_z[0][j])*(x-feld_z[0][j]);
           
           if (dist==0.) return feld_z[1][j];
           sumZ+=1./dist*feld_z[1][j];
           sum_dist+=1./dist;
       }
       return sumZ/sum_dist;
       
       
  }
  /**
   * @param args the command line arguments
   */
  public static void main (String args[]) {
    MainCurrentGroundwater1D e = new MainCurrentGroundwater1D();
  }

}
