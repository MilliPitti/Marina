package de.smile.marina.fem;

public class FTriangleMesh extends FEDecomposition{

    public int anzr = 0;
    public int anzk = 0;
    public int anze = 0;
    
    // set approximation order
    
    public FTriangleMesh()
{
	super();
}
    
 public FTriangleMesh(FTriangle[] elements, DOF[] dof)
{
	super(elements, dof);
	anze=elements.length;
	anzk=dof.length;
}	
}
