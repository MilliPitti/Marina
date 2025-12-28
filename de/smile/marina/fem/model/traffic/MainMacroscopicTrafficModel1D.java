package de.smile.marina.fem.model.traffic;
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
import de.smile.math.ode.ivp.HeunTStep;
import de.smile.math.ode.ivp.SimpleTStep;
import de.smile.math.ode.ivp.IVP;

public class MainMacroscopicTrafficModel1D extends JCanvas {

  FEDecomposition fed = new FEDecomposition();

  JFrame frame;


  /** Creates new Main */
  public MainMacroscopicTrafficModel1D () {
    frame = new JFrame("MacroscopicTrafficModel");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setSize( 800,400);
    setSize(800,400);
    frame.getContentPane().add(this);
    frame.setVisible(true);

    int    knoten   = 301;
    double laenge   = 30000.0;	
    double dx       = laenge/(knoten-1);
    // Create FEDecomposition
    DOF b;
    DOF e = new DOF(0,0.,0.,0.);
    for (int m=1; m<knoten; m++)
    { b = e;
      e = new DOF(m, m*dx,0,0); 
      fed.addFElement(new FEdge(b,e));
    }

    MacroscopicTrafficModel1D  mtraffic1d  = new MacroscopicTrafficModel1D(fed);   // Stroemungsmodell

    double[] mtrafficerg  = mtraffic1d.initialSolution(0.);   // Anfangswerte (Initialisierung)

    //SimpleTStep methode = new EulerTStep();
    SimpleTStep methode = new HeunTStep();
    
    mtraffic1d.setMaxTimeStep(0.001);                             // Zeitschritt

    double startTime = 0.0;     // [sec]
    double endTime   = 44700;   // [sec]
    double dt        = 10.;    //0.1;
    
    //... Schleife ueber die Zeit ...........................................
    mtraffic1d.draw_it (getGraphics(), mtrafficerg,   startTime);
    for (double t=startTime;t<endTime;t+=dt) {
	
	double te=t+dt;
	double ts = 0.;

        mtrafficerg = IVP.Solve(mtraffic1d,t,mtrafficerg,t+dt,methode);

	mtraffic1d.draw_it (getGraphics(), mtrafficerg,   t+dt);
        repaint();

    } // end for 
    
  }  // end 

  /**
   * @param args the command line arguments
   */
  public static void main (String args[]) {
    MainMacroscopicTrafficModel1D e = new MainMacroscopicTrafficModel1D();
  }

}
