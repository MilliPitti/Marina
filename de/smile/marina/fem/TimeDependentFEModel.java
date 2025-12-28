package de.smile.marina.fem;
import de.smile.math.ode.ivp.ODESystem;


public class TimeDependentFEModel {
   
    private double[]   result;
    public Object    model;

    public TimeDependentFEModel (FEModel m, double[] r) {
        if (!(m instanceof ODESystem)) System.out.println("Modell implementiert kein ODESystem");
	model = m;
	result = r;
    }

    public FEModel getFEModel() {
	return (FEModel)model;
    }

    public ODESystem getODESystem() {
	return (ODESystem)model;
    }

    public double[] getResult() {
	return result;
    }

    public void     setResult(double[] r) {
	result = r;
    }

}
