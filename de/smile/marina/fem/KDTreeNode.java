package de.smile.marina.fem;

import javax.vecmath.Point3d;
/** 

<HR>
@author Dipl.-Ing. C. Lippert / Peter Milbradt
*/
class KDTreeNode
{

	public static int AUTO_DETECT_SPLIT_DIRECTION=0;

	public static int objects_count=0;
	public static int nodes=0;

	de.smile.geom.Rectangle2d bounds=null;
	int[] objects=null;

	public double	split=Double.NaN;
	public boolean	dir_x=false;

	public KDTreeNode child1=null;
	public KDTreeNode child2=null;

	public int rekDepth=0;
	public static int maxRekDepth=100;

	public KDTreeNode()
	{}

	public KDTreeNode(FEDecomposition mesh, int[] objects, de.smile.geom.Rectangle2d bounds, int maxObjectsInLeaf, boolean opt_split_mode, int direction_mode, int TREE_TYPE, int rekDepth)
	{
		this.rekDepth=rekDepth;
		nodes++;

		if ((objects.length<maxObjectsInLeaf) || (rekDepth>=maxRekDepth) || bounds.width<0.01 || bounds.height<0.01)
			newLeaf(objects,bounds);
		else
			newChildren(mesh, objects, bounds, maxObjectsInLeaf, opt_split_mode, direction_mode, TREE_TYPE);
	}

	public int getMaxObjectsInLeaf()
	{
		if (isLeaf())
		{
			if (objects==null)
				return 0;
			return objects.length;
		}
		else
		{
			int len1=child1.getMaxObjectsInLeaf();
			int len2=child2.getMaxObjectsInLeaf();
			return Math.max(len1,len2);
		}
	}

	public int getObjectsInLeafs()
	{
		if (isLeaf())
		{
			if (objects==null)
				return 0;
			return objects.length;
		}
		else
		{
			int len1=child1.getObjectsInLeafs();
			int len2=child2.getObjectsInLeafs();
			return len1+len2;
		}
	}

	public int getMaxRekDepth()
	{
		if (isLeaf())
		{
			return rekDepth;
		}
		else
		{
			int rd1=child1.getMaxRekDepth();
			int rd2=child2.getMaxRekDepth();
			return Math.max(rd1,rd2);
		}	
	}

	public int getLeafSize()
	{
		if (isLeaf())
		{
			return 1;
		}
		else
		{
			int ls1=child1.getLeafSize();
			int ls2=child2.getLeafSize();
			return ls1+ls2;
		}	
	}

	public long getPathNumber(Point3d p, long path)
	{
		if (rekDepth>=64)
			return path;

		//System.out.println(""+rekDepth+" 1: "+path);
		boolean left=true;
		if (dir_x)
		{
			if (p.x>split)
				left=false;
		}
		else
		{
			if (p.y>split)
				left=false;
		}
		long maske=(1L<<rekDepth);
		if (left)
		{
			//maske = java.lang.Long.reverse(maske);
			//System.out.println(""+rekDepth+" 2a: "+path+" "+maske);
			if (child1!=null)
			{
				path=child1.getPathNumber(p,path);
				return path;
			}
			else
			{
				return path;
			}
		}
		else
		{
			//System.out.println(""+rekDepth+" 2b: "+path+" "+maske);
			if (child2!=null)
			{
				path=path | maske;
				path=child2.getPathNumber(p,path);
				return path;
			}
			else
			{
				return path;
			}
		}
	}

	public String getPath(Point3d p, String path)
	{
		//System.out.println(""+rekDepth+" 1: "+path);
		boolean left=true;
		if (dir_x)
		{
			if (p.x>split)
				left=false;
		}
		else
		{
			if (p.y>split)
				left=false;
		}
		if (left)
		{
			//maske = java.lang.Long.reverse(maske);
			//System.out.println(""+rekDepth+" 2a: "+path+" "+maske);
			if (child1!=null)
			{
				path=child1.getPath(p,new String(path+"0"));
				return path;
			}
			else
			{
				return path;
			}
		}
		else
		{
			//System.out.println(""+rekDepth+" 2b: "+path+" "+maske);
			if (child2!=null)
			{
				path=child2.getPath(p,new String(path+"1"));
				return path;
			}
			else
			{
				return path;
			}
		}
	}

	protected long convertPathToPathNumber(String path)
	{
		long number=0L;
		for (int i=0; i<path.length(); i++)
		{
			char c=path.charAt(i);
			if (c=='1')
			{
				long maske=(1 << i);
				number=number | maske;
			}
		}
		return number;
	}

	protected String convertPathNumberToPath(long id)
	{
		StringBuilder path=new StringBuilder("");
		for (int i=0; i<64; i++)
		{
			long maske=(1L << i);
			long set=id & maske;
			if (set==0)
				path.append("0");
			else
				path.append("1");
		}
		return path.toString();
	}

	public final void newLeaf(int[] objects, de.smile.geom.Rectangle2d bounds)
	{
		//System.out.println(""+rekDepth+" "+bounds+" "+objects.length);
            objects_count += objects.length;
            this.objects=objects;
            this.bounds=bounds;
	}

	public final void newChildren(FEDecomposition mesh, int[] objects, de.smile.geom.Rectangle2d bounds, int maxObjectsInLeaf, boolean opt_split_mode, int direction_mode, int TREE_TYPE)
	{
			newPointInElementSearchChildren(mesh, objects, bounds, maxObjectsInLeaf, opt_split_mode, direction_mode, TREE_TYPE);
		
	}

	public void newPointInElementSearchChildren(FEDecomposition mesh, int[] objects, de.smile.geom.Rectangle2d bounds, int maxObjectsInLeaf, boolean opt_split_mode, int direction_mode, int TREE_TYPE)
	{
		FElement topo;
		int child1Count=0;
		int child2Count=0;
		de.smile.geom.Rectangle2d bounds1;
		de.smile.geom.Rectangle2d bounds2;
		int ps;
		java.util.BitSet left=new java.util.BitSet(objects.length);
		java.util.BitSet right=new java.util.BitSet(objects.length);
		// in x- oder y-Richtung teilen????
		if ((direction_mode==AUTO_DETECT_SPLIT_DIRECTION && bounds.width>bounds.height)){
			if (opt_split_mode)
			{
				double[] xs=new double[objects.length];
				for (int i=0; i<objects.length; i++)
				{
					topo=mesh.getFElement(objects[i]);
					xs[i]=getRepresentativeElementCoordinate(topo,true);
				}
				if (objects.length<10000)
					java.util.Arrays.sort(xs);
				else
					java.util.Arrays.parallelSort(xs);

				split=xs[(xs.length/2)];
			}
			else
				split=bounds.x+bounds.width/2.0;
			
			dir_x=true;

			//System.out.println("X "+bounds+", "+split_x);
			for (int i=0; i<objects.length; i++)
			{
				topo=mesh.getFElement(objects[i]);
                                Point3d[] points = topo.getDOFs();
				left.set(i,false);
				right.set(i,false);
				ps=points.length;
				for (int a=0; a<ps; a++)
				{
					//p=topo.getPointAt(a);
					double xa=points[a].x;
					if (xa<=split)
					{
						left.set(i,true);
					}
					if (xa>=split)
					{
						right.set(i,true);
					}
				}
			}
			// Bounding-Boxes der Kinder erzeugen
			bounds1=new de.smile.geom.Rectangle2d(bounds.x,bounds.y,split-bounds.x,bounds.height);
			bounds2=new de.smile.geom.Rectangle2d(split,bounds.y,bounds.x+bounds.width-split,bounds.height);
		}
		else
		{
			if (opt_split_mode)
			{
				double[] ys=new double[objects.length];
				for (int i=0; i<objects.length; i++)
				{
					topo=mesh.getFElement(objects[i]);
					ys[i]=getRepresentativeElementCoordinate(topo,false);
				}
				if (objects.length<10000)
					java.util.Arrays.sort(ys);
				else
					java.util.Arrays.parallelSort(ys);

				split=ys[(ys.length/2)];
			}
			else
				split=bounds.getY()+bounds.height/2.0;
			
			dir_x=false;

			//System.out.println("Y "+bounds+", "+split_y);
			
			for (int i=0; i<objects.length; i++)
			{
				topo=mesh.getFElement(objects[i]);
                                Point3d[] points = topo.getDOFs();
				left.set(i,false);
				right.set(i,false);
				ps=points.length;
				for (int a=0; a<ps; a++)
				{
					//p=topo.getPointAt(a);
					double ya=points[a].y;
					if (ya<=split)
					{
						left.set(i,true);
					}
					if (ya>=split)
					{
						right.set(i,true);
					}
				}
			}
			// Bounding-Boxes der Kinder erzeugen
			bounds1=new de.smile.geom.Rectangle2d(bounds.x,bounds.y,bounds.width,split-bounds.y);
			bounds2=new de.smile.geom.Rectangle2d(bounds.x,split,bounds.width,bounds.y+bounds.height-split);	
		}
		// nun Objekte umkopieren, Kindern erzeugen
		for (int i=0; i<objects.length; i++)
		{
			if (left.get(i))
				child1Count++;
			if (right.get(i))
				child2Count++;
		}

		// unguenstige Aufteilung mit sukzessiver Rekursion bis zur max. Rektiefe verhindern
		int test_max=5;
		if (maxObjectsInLeaf<10)
			test_max=2;
		if ((child1Count>objects.length-test_max || child2Count>objects.length-test_max) && rekDepth>5)
		{
			/*if (direction_mode==AUTO_DETECT_SPLIT_DIRECTION)
			{
				int mode=X_SPLIT_DIRECTION;
				if (dir_x)
					mode=Y_SPLIT_DIRECTION;
				newPointInElementSearchChildren(mesh,objects,bounds,maxObjectsInLeaf,opt_split_mode,mode,TREE_TYPE);
			}
			else
			*/
				newLeaf(objects,bounds);
			return;
		}

		int[] objects1=new int[child1Count];
		int[] objects2=new int[child2Count];

		//System.out.println("Child1 "+child1Count+", Child2 "+child2Count);

		child1Count=0;
		child2Count=0;
		for (int i=0; i<objects.length; i++)
		{
			if (left.get(i))
			{
				objects1[child1Count]=objects[i];
				child1Count++;
			}
			if (right.get(i))
			{
				objects2[child2Count]=objects[i];
				child2Count++;
			}
		}

		boolean multi_threading=true;
		if (multi_threading && rekDepth==0)
		{
			KDTreeNodeThread thread1=new KDTreeNodeThread(mesh,objects1,bounds1,maxObjectsInLeaf,opt_split_mode,KDTreeNode.AUTO_DETECT_SPLIT_DIRECTION,TREE_TYPE,rekDepth+1);
			KDTreeNodeThread thread2=new KDTreeNodeThread(mesh,objects2,bounds2,maxObjectsInLeaf,opt_split_mode,KDTreeNode.AUTO_DETECT_SPLIT_DIRECTION,TREE_TYPE,rekDepth+1);
		
			thread1.start();	thread2.start();
			try
			{
				thread1.join();	thread2.join();			
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			child1=thread1.getKDTreeNode();
			child2=thread2.getKDTreeNode();
		}
		else
		{
			child1=new KDTreeNode(mesh,objects1,bounds1,maxObjectsInLeaf,opt_split_mode,KDTreeNode.AUTO_DETECT_SPLIT_DIRECTION,TREE_TYPE,rekDepth+1);
			child2=new KDTreeNode(mesh,objects2,bounds2,maxObjectsInLeaf,opt_split_mode,KDTreeNode.AUTO_DETECT_SPLIT_DIRECTION,TREE_TYPE,rekDepth+1);
		}
	}

	public boolean isLeaf()
	{
		return (child1==null);
	}

	public int[] searchLeafNumbers(Point3d p)
	{
		if (isLeaf())
		{
			return objects;
		}
		else
		{
			//System.out.println(p.getNumber()+" p "+p.getX()+" "+split_x+" p "+p.getY()+" "+split_y);
			if (dir_x)
			{
				if (p.x<=split)
					return child1.searchLeafNumbers(p);
				else
					return child2.searchLeafNumbers(p);
			}
			else
			{
				if (p.y<=split)
					return child1.searchLeafNumbers(p);
				else
					return child2.searchLeafNumbers(p);
			}
		}
	}

	public int[] searchLeafNumbers(Point3d p, double epsilon)
	{
		if (isLeaf())
		{
			return objects;
		}
		else
		{
			if (dir_x)
			{
				if (p.x<=(split-epsilon))
					return child1.searchLeafNumbers(p,epsilon);
				else
					return child2.searchLeafNumbers(p,epsilon);
			}
			else
			{
				if (p.y<=(split-epsilon))
					return child1.searchLeafNumbers(p,epsilon);
				else
					return child2.searchLeafNumbers(p,epsilon);
			}
		}
	}

	public int[][] getLeafNumbers()
	{
		if (isLeaf())
		{
			int[][] os=new int[1][];
			os[0]=objects;
			return os;
		}
		else
		{
			int[][] o1=child1.getLeafNumbers();
			int[][] o2=child2.getLeafNumbers();
			int[][] os=new int[o1.length+o2.length][];
                    System.arraycopy(o1, 0, os, 0, o1.length);
                    System.arraycopy(o2, 0, os, o1.length, o2.length);
			return os;
		}
	}

	public int[][] searchLeafNumbers(de.smile.geom.Rectangle2d bounds)
	{
		if (isLeaf())
		{
			int[][] os=new int[1][];
			os[0]=objects;
			return os;
		}
		else
		{
			int[][] o1;
			int[][] o2;
			if (dir_x)
			{
				double min_x=bounds.x;
				if (min_x<=split)
					o1=child1.searchLeafNumbers(bounds);
				else
					o1=new int[0][0];
				double max_x=bounds.x+bounds.width;
				if (max_x>=split)
					o2=child2.searchLeafNumbers(bounds);
				else
					o2=new int[0][0];		
			}
			else
			{
				double min_y=bounds.y;
				if (min_y<=split)
					o1=child1.searchLeafNumbers(bounds);
				else
					o1=new int[0][0];
				double max_y=bounds.y+bounds.height;
				if (max_y>=split)
					o2=child2.searchLeafNumbers(bounds);
				else
					o2=new int[0][0];			
			}
			int[][] os=new int[o1.length+o2.length][];
                    System.arraycopy(o1, 0, os, 0, o1.length);
                    System.arraycopy(o2, 0, os, o1.length, o2.length);
			return os;
		}
	}

	public void dispose()
	{
		objects=null;
		bounds=null;
		try
		{
			child1.dispose();				
		}
		catch (Throwable ex){}	
		try
		{
			child2.dispose();				
		}
		catch (Throwable ex){}	
	}

	protected final double getRepresentativeElementCoordinate(FElement topo, boolean x_dir)
	{
            if (x_dir)
			return topo.getBounds().x;//  getMinX();
		else
			return topo.getBounds().y;// getMinY();
	}

    protected class KDTreeNodeThread extends Thread {

        KDTreeNode node = null;

        FEDecomposition mesh;
        int[] objects;
        de.smile.geom.Rectangle2d bounds;
        int maxObjectsInLeaf;
        boolean opt_split_mode;
        int direction_mode;
        int TREE_TYPE;
        int rekDepth;

        protected KDTreeNodeThread(FEDecomposition mesh, int[] objects, de.smile.geom.Rectangle2d bounds, int maxObjectsInLeaf, boolean opt_split_mode, int direction_mode, int TREE_TYPE, int rekDepth) {
            this.mesh = mesh;
            this.objects = objects;
            this.bounds = bounds;
            this.maxObjectsInLeaf = maxObjectsInLeaf;
            this.TREE_TYPE = TREE_TYPE;
            this.opt_split_mode = opt_split_mode;
            this.direction_mode = direction_mode;
            this.rekDepth = rekDepth;
        }

        @Override
        public void run() {
            node = new KDTreeNode(mesh, objects, bounds, maxObjectsInLeaf, opt_split_mode, direction_mode, TREE_TYPE, rekDepth);
        }

        public KDTreeNode getKDTreeNode() {
            return node;
        }
    }
}
