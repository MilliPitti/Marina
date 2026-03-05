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
package de.smile.marina.fem.model.traffic;

import bijava.graphics.JCanvas;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEdge;
import javax.swing.*;
import java.awt.*;

public class MainMacroscopicTrafficModel1D extends Object {

  FEDecomposition fed = new FEDecomposition();

  JFrame frame;
  JCanvas jcanvas;

  /** Creates new Main */
  public MainMacroscopicTrafficModel1D() {
    frame = new JFrame("MacroscopicTrafficModel");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setSize(800, 400);
    frame.getContentPane().setLayout(new BorderLayout());
    jcanvas = new JCanvas();
    jcanvas.setSize(800, 400);
    frame.getContentPane().add(jcanvas);
    frame.setVisible(true);

    int knoten = 301;
    double laenge = 30000.0;
    double dx = laenge / (knoten - 1);
    // Create FEDecomposition
    DOF b;
    DOF e = new DOF(0, 0., 0., 0.);
    for (int m = 1; m < knoten; m++) {
      b = e;
      e = new DOF(m, m * dx, 0, 0);
      fed.addFElement(new FEdge(b, e));
    }

    MacroscopicTrafficModel1D mtraffic1d = new MacroscopicTrafficModel1D(fed); 

    mtraffic1d.initialSolution(0.); // Anfangswerte (Initialisierung)
    double[] mtrafficerg = getState(mtraffic1d);

    double startTime = 0.0; // [sec]
    double endTime = 44700; // [sec]
    double dt = 10.; // Intervall fuer Neuzeichnung
    long sleepMillis = 500; // Pause pro Bild [ms]

    // Startzustand zeichnen
    mtraffic1d.draw_it(jcanvas.getGraphics(), mtrafficerg, startTime);
    frame.setTitle(String.format("MacroscopicTrafficModel - t = %.2f s", startTime));
    jcanvas.repaint();

    // ... Schleife ueber die Zeit ...........................................
    for (double t = startTime; t < endTime; t += dt) {

      double ta = t;      // Anfangszeit des Ausgabeintervalls
      double te = t + dt; // Endzeit des Ausgabeintervalls
      do {
        double ts = mtraffic1d.getMaxTimeStep();
        if ((ta + ts) > te) {
          ts = te - ta;
        }
        mtraffic1d.timeStep(ts);
        ta += ts;
      } while (ta < te);
      mtrafficerg = getState(mtraffic1d);

      mtraffic1d.draw_it(jcanvas.getGraphics(), mtrafficerg, t + dt);
      frame.setTitle(String.format("MacroscopicTrafficModel - t = %.2f s", t + dt));
      jcanvas.repaint();
      try {
        Thread.sleep(sleepMillis);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        break;
      }

    } // end for

  } // end

  public static void main(String args[]) {
    new MainMacroscopicTrafficModel1D();
  }

  private double[] getState(MacroscopicTrafficModel1D mtraffic1d) {
    DOF[] dofs = fed.getDOFs();
    int n = dofs.length;
    double[] state = new double[2 * n];
    for (DOF dof : dofs) {
      int i = dof.number;
      MacroscopicTrafficModel1DData data = MacroscopicTrafficModel1DData.extract(dof);
      state[i] = data.v;
      state[n + i] = data.rho;
    }
    return state;
  }
}
