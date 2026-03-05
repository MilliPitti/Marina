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
 */
import bijava.graphics.JCanvas;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEdge;
import javax.swing.*;
import java.awt.*;

public class MainCurrent1D extends Object {
    
    FEDecomposition fed = new FEDecomposition();
    
    JFrame frame;
    JCanvas jcanvas;
    
    /** Creates new Main */
    public MainCurrent1D() {
        frame = new JFrame("Current1D");
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
            //e = new DOF(m,m/5.,0.,1.-m/400.);
            e = new DOF(m,m/5.,0.,1.);
            fed.addFElement(new FEdge(b,e));
        }
        
        CurrentModel1D  current1d  = new CurrentModel1D(fed);   // Stroemungsmodell
        current1d.initialSolution(0.);                          // Anfangswerte (Initialisierung)
        
        double startTime = 0.0;     // [sec]
        double endTime   = 44700;   // [sec]
        double dt        = 1;       // Intervall fuer Neuzeichnung
        long sleepMillis = 500;      // Pause pro Bild [ms]

        // Startzustand zeichnen
        current1d.draw_it(jcanvas.getGraphics(), startTime);
        frame.setTitle(String.format("Current1D - t = %.2f s", startTime));
        jcanvas.repaint();
        
        //... Schleife ueber die Zeit ...........................................
        for (double t = startTime; t < endTime; t += dt) {
            double ta = t;      // Anfangszeit des Ausgabeintervalls
            double te = t + dt; // Endzeit des Ausgabeintervalls

            //...Schleife ueber die Courant-Substeps.............................
            do {
                double ts = current1d.getMaxTimeStep();
                if ((ta + ts) > te) ts = te - ta;
                current1d.timeStep(ts);
                ta += ts;
            } while (ta < te);

            current1d.draw_it(jcanvas.getGraphics(), t + dt);
            frame.setTitle(String.format("Current1D - t = %.2f s", t + dt));
            jcanvas.repaint();
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

        } // end for
        
    }  // end public MainCurrent1D ()
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        MainCurrent1D e = new MainCurrent1D();
    }
    
}
