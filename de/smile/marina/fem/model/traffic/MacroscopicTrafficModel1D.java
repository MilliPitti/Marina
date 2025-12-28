package de.smile.marina.fem.model.traffic;
import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEApproximation;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEModel;
import de.smile.marina.fem.FEdge;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.ModelData;
import de.smile.marina.fem.TimeDependentFEApproximation;
import de.smile.math.Function;
import java.util.*;
import java.awt.*;
import de.smile.math.ode.ivp.ODESystem;

/** This class describe the stabilized finite element approximation
 * of the instationary 1-dimensional Macroscopic Traffic Model (Kerner und Kohnheuser)
 * @author Dr.-Ing. habil. Peter Milbradt
 * @version 1.1
 */
public class MacroscopicTrafficModel1D extends TimeDependentFEApproximation implements FEModel,ODESystem {

  private int n,V,RHO,numberofdofs;
  private double[] result;
  
  double vf       = 130.0 /    3.6;    // Wunschgeschwindigkeit (130 km/h )
  double rhoMax   = 175.0 / 1000.0;    // maximale Dichte       (175 Fz/km)
  double rhoH     =  30.0 / 1000.0;    // homogene Dichte       (    Fz/km)
  double tau      =  35.0;             // Relaxationszeit       (  6  s   )
  double c0       =  13.0;             //                       ( 13  m/s )
  double c02      =  c0 * c0;          //                       (169 m2/s2)

  double mu       =  60.0;             //                       ( 60  m/s )

  /** Creates new MacroscopicTrafficModel1D
 * @param fe a finite edge domain decomposition
 */
  public MacroscopicTrafficModel1D(FEDecomposition fe) {
    fenet = fe;
    femodel=this;
    
    // DOFs initialisieren
    initialDOFs();

    numberofdofs = fenet.getNumberofDOFs();
    V = 0;
    RHO = numberofdofs;
    n = 2 * numberofdofs;
    result = new double[n];
  }

/** compute the initial solutions
 * @param time starttime
 * @return the result vector
 */
  public double[] initialSolution(double time){
    double x[] = new double[n];

    System.out.println("MacroscopicTrafficModel - Werte Initialisieren");
    DOF[] dof = fenet.getDOFs();
      for (DOF dof1 : dof) {
          int i = dof1.number;
          MacroscopicTrafficModel1DData mtmd = getMacroscopicTrafficModel1DData(dof1);
          x[RHO + i] = mtmd.rho;
          x[V + i] = mtmd.v;
      }
    return x;
  }

// ----------------------------------------------------------------------
// ToDO
// ----------------------------------------------------------------------
  @Override
	public ModelData genData(FElement felement)
	{return null;}

/**
 * @param dof
 * @return
 */
  @Override
  public ModelData genData(DOF dof){
    MacroscopicTrafficModel1DData mtd = new MacroscopicTrafficModel1DData();
    
    double laenge=30000.;
    double deltaRho =   0.01;
    //..Beispiel 1: rhoH = 0.023, Beispiel 2: rhoH = 0.063.....................//
    double cosh1 = 0.5 * ( Math.exp( 0.004*(dof.x+100.0-laenge/2.0))
                           + Math.exp(-0.004*(dof.x+100.0-laenge/2.0)));
    double cosh2 = 0.5 * ( Math.exp( 0.002*(dof.x-100.0-laenge/2.0))
                           + Math.exp(-0.002*(dof.x-100.0-laenge/2.0)));
    mtd.rho = rhoH + deltaRho * (1.0/(cosh1*cosh1) - 0.5/(cosh2*cosh2));  
//      e.rho = rhoH + deltaRho * Math.cos(2.0*Math.PI* e.x/laenge - 1.42);
    mtd.v   = V(mtd.rho);

    return mtd;
  }
/** set the solving conditions at the DOF
 * @param dof degree of freedom to set
 * @param t actual time
 */
  @Override
  public void setBoundaryCondition(DOF dof,double t) {
	  //..Randwerte setzen.......................................................//
        if(dof.number==0) {
		MacroscopicTrafficModel1DData cmd = getMacroscopicTrafficModel1DData(dof);
		MacroscopicTrafficModel1DData cmdlast = getMacroscopicTrafficModel1DData(fenet.getDOF(numberofdofs-1));
		cmd.rho = cmdlast.rho;	
        	cmd.v   = cmdlast.v;
		cmd.drhodt = cmdlast.drhodt;	
        	cmd.dvdt   = cmdlast.dvdt;
	}
  }


  @Override
  public double ElementApproximation(FElement element) {

    double timeStep=Double.POSITIVE_INFINITY;
        
    FEdge ele = (FEdge)element;
    final double[][] koeffmat = ele.getkoeffmat();
    final double[] v = new double[2];
    final double[] rho = new double[2];
    // compute element derivations
    double dvdx=0.;
    double dv2dx2 = 0.;
    double drhodx=0.;
    double v_mean = 0.;
    double rho_mean = 0.;

    for ( int j = 0; j < 2; j++) {
      DOF dof = ele.getDOF(j);
      MacroscopicTrafficModel1DData cmd = getMacroscopicTrafficModel1DData(dof);

      v[j]=cmd.v;
      v_mean+=v[j]/2.;
      dvdx += cmd.v * koeffmat[j][1];
      rho[j]=cmd.rho;
      drhodx += cmd.rho * koeffmat[j][1];
      rho_mean += rho[j] /2.;
    }

    
    double A1= Math.abs(v_mean) + c0;
    double      tauupwind = 0.5 * ele.elm_size() / A1;
    maxTimeStep = Math.min(maxTimeStep,tauupwind );
    timeStep = tauupwind;
    
    double a_opt = 1.;
    if ((mu > 0.0) && (rho_mean>0.01)){ 
	    final double A2 = mu / rho_mean;
	    double     peclet = A1 * ele.elm_size() / A2;
	    a_opt = Function.coth(Math.abs(peclet)) - 1.0 / Math.abs(peclet);
    }

    tauupwind *= a_opt; 
    
    double residum1_mean = 0.;
    double residum2_mean = 0.;

    for ( int j = 0; j < 2; j++) {
      DOF dof = ele.getDOF(j);
      MacroscopicTrafficModel1DData cmd = getMacroscopicTrafficModel1DData(dof);

        residum1_mean += 0.5 * (cmd.drhodt + cmd.v * drhodx + cmd.rho * dvdx
//                              - auffahrt(cmd.x-2000.0,t)	
                             );	
        residum2_mean += 0.5 * (cmd.dvdt + cmd.v * dvdx 
                                      - (V(cmd.rho)-cmd.v)/tau
                                      + c02/cmd.rho * drhodx
                                      - mu/cmd.rho * dv2dx2
//                                    - zufall.nextGaussian(0.0, 0.063*0.063)
                             );
    }
    //    System.out.println("Fehler = "+cureq1_mean+" "+cureq2_mean);

    for (int j=0;j<2;j++){
      DOF dof = ele.getDOF(j);
      MacroscopicTrafficModel1DData cmd = getMacroscopicTrafficModel1DData(dof);

      cmd.rrho -= tauupwind * koeffmat[j][1] *
                  (       v_mean * residum1_mean + rho_mean * residum2_mean);
      cmd.rv   -= tauupwind * koeffmat[j][1] *
                   (c02/rho_mean * residum1_mean +   v_mean * residum2_mean);
      
      double vorfaktor;
      for (int l=0;l<2;l++){
	if(j==l) vorfaktor=1./3.;
	else vorfaktor=1./6.;
      cmd.rrho -= vorfaktor* (v[l] * drhodx + rho[l] * dvdx
//                        - auffahrt(dof.x-2000.0,t)
                         );
      cmd.rv   -= vorfaktor* (v[l] * dvdx
                          - (V(rho[l])-v[l])/tau
                          +  c02/rho[l] * drhodx
                          + 2.0 * mu/rho[l] * dvdx * koeffmat[j][1]
//                        + zufall.nextGaussian(0.0, 0.063*0.063)
                         );	
//      cmd.dvdx += 0.5 * dvdx;
	}
    }
    return timeStep;
  }

  private MacroscopicTrafficModel1DData getMacroscopicTrafficModel1DData(DOF dof){
    MacroscopicTrafficModel1DData cmd=null;
    Iterator modeldatas = dof.allModelDatas();
        while (modeldatas.hasNext()) {
            ModelData md = (ModelData) modeldatas.next();
      if(md instanceof MacroscopicTrafficModel1DData)  cmd = ( MacroscopicTrafficModel1DData )md;
    }
    return cmd;
  }

  @Override
  public double[] getRateofChange(double p1,double[] x) {
    DOF[] dof = fenet.getDOFs();
      for (DOF dof1 : dof) {
          int i = dof1.number;
          MacroscopicTrafficModel1DData mtmd = getMacroscopicTrafficModel1DData(dof1);
          mtmd.rho = Math.min(rhoMax,x[RHO + i]);
          mtmd.v = Math.max(0.,x[V + i]);
          setBoundaryCondition(dof1, p1);
          x[RHO + i]=mtmd.rho;
          x[V + i]=mtmd.v;
          mtmd.drhodt=mtmd.rrho;
          mtmd.dvdt=mtmd.rv;
          mtmd.dvdx = 0.;
          // set Results to zero
          mtmd.rv=0.;
          mtmd.rrho=0.;
      }

    maxTimeStep = 10000.;

    // Elementloop
    performElementLoop();

      for (DOF dof1 : dof) {
          int i = dof1.number;
          MacroscopicTrafficModel1DData mtmd = getMacroscopicTrafficModel1DData(dof1);
          result[V+i] = mtmd.rv;
          result[RHO+i] = (3.*mtmd.rrho-mtmd.drhodt)/2.;
      }
    first=false;
    return result;
  }

  public void draw_it (Graphics g, double[] x, double t)
  { g.clearRect(0,0,800,400);
    g.setColor( Color.green );
    g.drawLine(100,300,700,300);
    int knoten=fenet.getNumberofDOFs();

    g.setColor( Color.yellow );
    g.drawLine(100,300 - (int)(1200.00*rhoMax), 
               700,300 - (int)(1200.00*rhoMax));
     
    g.setColor( Color.black );
    for (int k=0;k<knoten-1;k++)
      g.drawLine(100 + (int)(   0.02*fenet.getDOF(k).x    ), 
                 300 - (int)(1200.00*x[RHO+k]  ), 
                 100 + (int)(   0.02*fenet.getDOF(k+1).x  ),
                 300 - (int)(1200.00*x[RHO+k+1]));	
				
    g.setColor( Color.red );
    for (int k=0;k<knoten-1;k++) 
      g.drawLine(100 + (int)(0.02*fenet.getDOF(k).x  ),
                 300 - (int)(3.60*x[V+k]  ), 
                 100 + (int)(0.02*fenet.getDOF(k+1).x),
                 300 - (int)(3.60*x[V+k+1]));	

    g.setColor( Color.blue );
    g.drawString("Zeit: "        + Integer.toString((int)t)     + " s", 100, 350);
    g.drawString("Zeitschritt: " + Double.toString(maxTimeStep) + " s", 500, 350);		
  }

/*
//..Fundamentaldiagramm von Cremer..........................................//
  public double V (double rho) 
  { return vf * Math.pow(1.0 - Math.pow(rho/rhoMax, 1.4), 4.0); }
  
//..Fundamentaldiagramm (linear)............................................//
  public double V (double rho) 
  { return vf * (1.0 - (rho/rhoMax)); }

//..Fundamentaldiagramm (quadratisch).......................................//
  public double V (double rho) 
  { return vf * (1.0 - 2.0 * (rho/rhoMax) + (rho/rhoMax)*(rho/rhoMax)); }

//..Fundamentaldiagramm (Kuehne)............................................//
  public double V (double rho) 
  { return vf * Math.pow(1.0 - Math.pow(rho/rhoMax,2.05),21.11); }

//..Fundamentaldiagramm (Kerner und Konhaeuser).............................//
  public double V (double rho) 
  { return vf * ( 1.0 / ( 1.0 + Math.exp((rho/rhoMax - 0.20)/0.05))); }
*/

//..Fundamentaldiagramm (Helbing)...........................................//
  private double V (double rho) 
  { double V_ = vf / (tau * rho * A(rho) * P(rho));
    return (V_ / (2.0 * vf) * (-1.0 + Math.sqrt(1.0 + 4.0 * vf * vf / V_)));
  }

  private double A (double rho)
  { double rhoKrit  = 0.270 * rhoMax;
    double A0       = 0.008;
    double deltaA   = 2.500 * A0;
    double deltaRho =   0.01;
    return (A0 + deltaA * (Math.tanh ((rho - rhoKrit) / deltaRho) + 1.0)); }
    
  private double P (double rho)
  { double T        = 1.8;
    double help = (1.0 - rho/rhoMax); 
    return (vf * rho * T * T / (tau * A(rhoMax) * help * help));
  }  
}
