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
import de.smile.math.ode.ivp.HeunTStep;
import de.smile.math.ode.ivp.SimpleTStep;

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
            e = new DOF(m,m/5.,0.,1.-m/400.);
            fed.addFElement(new FEdge(b,e));
        }
        
        CurrentModel1D  current1d  = new CurrentModel1D(fed);   // Stroemungsmodell
        double[] currenterg  = current1d.initialSolution(0.);   // Anfangswerte (Initialisierung)
        
//        RKETStep methode = new RK_2_3_TStep();
//	SimpleTStep methode = new EulerTStep();
        SimpleTStep methode = new HeunTStep();
        //ABMTStep   methode = new ABMTStep();
        
        current1d.setMaxTimeStep(0.01);                             // Zeitschritt
        
        double startTime = 0.0;     // [sec]
        double endTime   = 44700;   // [sec]
        double dt        = 1.;    //0.1;
        
        //double dt=sd.dt;
        //... Schleife ueber die Zeit ...........................................
        for (double t=startTime;t<endTime;t+=dt) {
            
            double ta=t;
            double te=t+dt;
            
            //...Schleife ueber einen Zeitschritt................................
            do {
                double ts  = current1d.getMaxTimeStep();
                
                if((ta+ts)>te) ts = te - ta;
                
                currenterg  = methode.TimeStep(current1d,  ta, ts, currenterg);
                
                ta+=ts;
                
            } while (ta<te);
            
            current1d.draw_it(jcanvas.getGraphics(), currenterg,   t+dt);
            jcanvas.repaint();
            
        } // end for
        
    }  // end public MainCurrent1D ()
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        MainCurrent1D e = new MainCurrent1D();
    }
    
    private void drawIt( Graphics g, double[] x, double time) {
        g.clearRect(0,0,800,400);
        g.setColor( Color.white);
        g.fillRect(0,0,800,400);
        
        int width = jcanvas.getWidth();
        int height = frame.getHeight();
        int n = x.length/2;
        double cmax = 0.2;
        
        int deltax=(int)((0.9*width)/n);
        int deltay=(int)(0.45*height);
        
        int xgrid,x0,x1,ygrid,yc0,yc1;
        int xoffset=(int)((0.05*width));
        ygrid=(int)(0.5*height);
        
        g.setColor(Color.BLACK);
        g.drawLine(xoffset,ygrid,(int)((0.95*width)),ygrid); 
        g.setColor(Color.GRAY);
        g.drawLine(xoffset,ygrid-(int)((1./cmax)*deltay),(int)((0.95*width)),ygrid-(int)((1./cmax)*deltay)); 
            
        for(int i=0;i<n;i++){
            xgrid=xoffset+i*deltax;
            g.setColor(Color.BLACK);
            
            //zeichne Linie u ... u+1 
            if (i<n-1){
               
                x0=xgrid;
                x1=x0+deltax;
//                if (xAxe[i]%10.==0)
                g.drawLine(x0,ygrid,x0,ygrid-5); 
//                if (xAxe[i]%20.==0)
                g.drawLine(x0,ygrid,x0,ygrid-15); 


                yc0=ygrid-(int)((x[i]/cmax)*deltay);
                yc1=ygrid-(int)((x[i+1]/cmax)*deltay);
                
                g.setColor(Color.RED);
                g.drawLine(x0,yc0,x1,yc1);
                
                yc0=ygrid-(int)((x[n+i]/cmax)*deltay);
                yc1=ygrid-(int)((x[n+i+1]/cmax)*deltay);
                
                g.setColor(Color.BLUE);
                g.drawLine(x0,yc0,x1,yc1);
                
            }
        }
    }
    
}

