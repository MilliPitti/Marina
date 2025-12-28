package de.smile.marina.fem;

/** System of partial differential equations for FE-Approximation */
public interface FEModel {

  public abstract ModelData genData(DOF dof); // Modelldaten an den Knoten
  public abstract ModelData genData(FElement felement); // Modelldaten an den Elementen
  
  public abstract void setBoundaryCondition(DOF dof, double t); 
  
  public abstract double ElementApproximation(FElement ele);
  
}
