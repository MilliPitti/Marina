package de.smile.marina.fem;

import javax.vecmath.Point3d;

/**
 *
 * < HR> @author Dipl.-Ing. C. Lippert / Peter Milbradt
 */
class KDTree {

    protected int maxRekDepth = -1;

    FEDecomposition mesh = null;
    KDTreeNode root = null;
    int TREE_TYPE;  // ELEMENT_TREE=2;
    protected double epsilon;

    protected boolean tree_structure_only = false;
    protected boolean opt_split_mode = true;

    protected int initial_object_size = 0;

    public KDTree(FEDecomposition mesh, int maxObjectsInLeaf) {
        //System.out.println("KD-Tree, max. Objects "+maxObjectsInLeaf);
        //System.out.println("");

        this.mesh = mesh;

        KDTreeNode.nodes = 0;
        de.smile.geom.Rectangle2d bounds = mesh.getBounds();
        this.epsilon = 0.0;

        initial_object_size = mesh.getElementSize();
        int[] objects = new int[initial_object_size];
        for (int i = 0; i < initial_object_size; i++) {
            objects[i] = i;
        }
        root = new KDTreeNode(mesh, objects, bounds, maxObjectsInLeaf, opt_split_mode, KDTreeNode.AUTO_DETECT_SPLIT_DIRECTION, /*TREE_TYPE=*/ 2, 0);
    }

    public FElement searchElement(Point3d p) {
        //System.out.println(""+p.getNumber());
        if (root == null) {
            return null;
        } else {
            int[] objects = root.searchLeafNumbers(p);
            //System.out.println(""+p.getNumber()+"  "+objects.length);

            for (int i = 0; i < objects.length; i++) {
                FElement e = mesh.getFElement(objects[i]);
                //System.out.print(" "+e.getNumber());
                if (e.contains(p)) {
                    return mesh.getFElement(objects[i]);
                }
            }

        }
        return null;
    }
}
