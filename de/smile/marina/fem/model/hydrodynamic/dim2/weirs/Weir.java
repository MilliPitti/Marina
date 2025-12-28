package de.smile.marina.fem.model.hydrodynamic.dim2.weirs;

import de.smile.marina.fem.*;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.model.hydrodynamic.dim2.CurrentModel2DData;
import de.smile.math.Function;

/**
 * abstrakte Klasse zu Beschreibung von Wehren im CurrentModel2D
 *
 * @author milbradt
 * @version 3.20.0
 */
public abstract class Weir {

    protected FEDecomposition sysdat;
    protected int[] knotennummern;
    protected double[][] normale; // Feld mit den Normalenvektoren

    static double stricklerValue = 20.;

    public Weir(int[] knotennummern, FEDecomposition sysdat) {

        this.sysdat = sysdat;
        this.knotennummern = knotennummern;

        normale = new double[2][knotennummern.length];

        normale[0][0] = sysdat.getDOF(knotennummern[1]).y - sysdat.getDOF(knotennummern[0]).y;  // in x-Richtung
        normale[1][0] = -1. * (sysdat.getDOF(knotennummern[1]).x - sysdat.getDOF(knotennummern[0]).x);  // in y-Richtung
        double norm = Function.norm(normale[0][0], normale[1][0]);
        normale[0][0] /= norm;
        normale[1][0] /= norm;

        for (int i = 1; i < knotennummern.length; i++) {
            normale[0][i] = sysdat.getDOF(knotennummern[i]).y - sysdat.getDOF(knotennummern[i - 1]).y;  // in x-Richtung
            normale[1][i] = -1. * (sysdat.getDOF(knotennummern[i]).x - sysdat.getDOF(knotennummern[i - 1]).x);  // in y-Richtung
            norm = Function.norm(normale[0][i], normale[1][i]);
            normale[0][i] /= norm;
            normale[1][i] /= norm;
            final DOF dof = sysdat.getDOF(knotennummern[i]);
            for (FElement element : dof.getFElements()) {
                DOF[] dofs = element.getDOFs();
                for (DOF elementDof : dofs) {
                    CurrentModel2DData currentdata = CurrentModel2DData.extract(elementDof);
                    currentdata.kst = stricklerValue;
                    currentdata.ks = CurrentModel2DData.Strickler2Nikuradse(stricklerValue);
                }
            }
        }
    }

    public abstract double[] getV(DOF p, double h);
}
