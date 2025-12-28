package de.smile.marina.fem.model.hydrodynamic.dim2;

import de.smile.marina.fem.DOF;
import de.smile.marina.fem.FEDecomposition;
import de.smile.marina.fem.FEdge;
import de.smile.marina.fem.FElement;
import de.smile.marina.fem.FTriangle;
import java.util.ArrayList;

/**
 *
 * @author milbradt
 */
public class SlipBoundaryCondition {

    private final DOF dof;
    private ArrayList<FEdge> closedEdges;

    public SlipBoundaryCondition(FEDecomposition fed, DOF dof) {
        this.dof = dof;
        for (FElement felem : fed.getDOF(dof.number).getFElements()) {
            FTriangle tele = (FTriangle) felem;
            if (tele.getKennung() != 0) {
                if (tele.getKennung() == FTriangle.bit_kante_jk) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_jk+" bit_kante_jk");
                    if (tele.getDOF(1).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(2)));
                    }
                    if (tele.getDOF(2).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(1)));
                    }
                } else if (tele.getKennung() == FTriangle.bit_kante_ki) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ki+" bit_kante_ki");
                    if (tele.getDOF(0).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(2)));
                    }
                    if (tele.getDOF(2).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(0)));
                    }
                } else if (tele.getKennung() == FTriangle.bit_kante_ij) {
                    //	System.out.println("  "+FTriangle.bit_nr_kante_ij+" bit_kante_ij");
                    if (tele.getDOF(0).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(1)));
                    }
                    if (tele.getDOF(1).number == dof.number) {
                        if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                        closedEdges.add(new FEdge(dof, tele.getDOF(0)));
                    }
                } else if ((tele.getKennung() == FTriangle.bit_kante_ijk)
                        || (tele.getKennung() == FTriangle.bit_kante_jki)
                        || (tele.getKennung() == FTriangle.bit_kante_kij)
                        || (tele.getKennung() == FTriangle.bit_kante_ijki)) {
                    //	System.out.println("alle");
                    for (int ll = 0; ll < 3; ll++) {
                        if (tele.getDOF(ll) == dof) {
                            if (closedEdges == null) closedEdges = new ArrayList<>(2); //****
                            closedEdges.add(new FEdge(dof, tele.getDOF((ll + 1) % 3)));
                            closedEdges.add(new FEdge(dof, tele.getDOF((ll - 1 + 3) % 3)));
                        }
                    }
                }
            }
        }
    }
    public void setSlipCondition(){
        CurrentModel2DData current = CurrentModel2DData.extract(dof);
        double u = current.u;
        double v = current.v;
    }

}
