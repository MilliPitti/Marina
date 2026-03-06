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
import javax.swing.*;
import java.awt.*;

public class MainCurrentSediment1D extends Object {

  FEDecomposition fed = new FEDecomposition();

  JFrame frame;
  JCanvas currentCanvas;
  JCanvas sedimentCanvas;

  /** Creates new Main */
  public MainCurrentSediment1D () {
    frame = new JFrame("Sediment");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setMinimumSize(new Dimension(900, 800));
    frame.getContentPane().setLayout(new BorderLayout());
    currentCanvas = new JCanvas();
    sedimentCanvas = new JCanvas();
    currentCanvas.setPreferredSize(new Dimension(900, 390));
    sedimentCanvas.setPreferredSize(new Dimension(900, 390));

    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currentCanvas, sedimentCanvas);
    split.setResizeWeight(0.5);
    split.setContinuousLayout(true);
    split.setOneTouchExpandable(true);
    split.setDividerLocation(0.5);

    frame.getContentPane().add(split, BorderLayout.CENTER);
    frame.pack();
    frame.setLocationRelativeTo(null);
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

    current1d.initialSolution(0.);   // Anfangswerte (Initialisierung)
    sediment1d.initialSolution(0.);  // Anfangswerte (Initialisierung)

    double startTime = 0.0;     // [sec]
    double endTime   = 44700;   // [sec]
    double dt        = 1.0;    //0.1;
    long sleepMillis = 100;      // Pause pro Bild [ms]

    current1d.draw_it(currentCanvas.getGraphics(), startTime);
    sediment1d.draw_it(sedimentCanvas.getGraphics(), startTime);
    frame.setTitle(String.format("Current+Sediment1D - t = %.2f s", startTime));
    currentCanvas.repaint();
    sedimentCanvas.repaint();

    //double dt=sd.dt;
    //... Schleife ueber die Zeit ...........................................
    for (double t=startTime;t<endTime;t+=dt) {
	
	double ta = t;
	double te = t + dt;
	double ts;

	//...Schleife ueber einen Zeitschritt................................
	do {
	    double ts1 = sediment1d.getMaxTimeStep();
	    double ts2 = current1d.getMaxTimeStep();
	    ts = Math.min(ts1, ts2);
	    
	    if((ta+ts)>te) ts = te - ta;

	    current1d.timeStep(ts);
	    sediment1d.timeStep(ts);

	    ta+=ts;

	} while (ta<te);
		current1d.draw_it(currentCanvas.getGraphics(), t+dt);
		sediment1d.draw_it(sedimentCanvas.getGraphics(), t+dt);
        frame.setTitle(String.format("Current+Sediment1D - t = %.2f s", t + dt));
		currentCanvas.repaint();
		sedimentCanvas.repaint();
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            break;
        }

    } // end for 
    
  }  // end public MainCurrentSediment1D ()


  /**
   * @param args the command line arguments
   */
  public static void main (String args[]) {
    MainCurrentSediment1D e = new MainCurrentSediment1D();
  }

}
