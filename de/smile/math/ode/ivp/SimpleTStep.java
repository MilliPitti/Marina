package de.smile.math.ode.ivp;

public interface SimpleTStep {
    double[] TimeStep(ODESystem sys, double t, double dt,  double x[]) /*throws Exception*/;
}
