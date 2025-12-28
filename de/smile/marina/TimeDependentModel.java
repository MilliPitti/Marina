package de.smile.marina;

public interface TimeDependentModel {
   
    public void timeStep(double dt);
    public void write_erg_xf();

}
