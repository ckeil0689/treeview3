/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TVModel.java,v $f
 * $Revision: 1.37 $
 * $Date: 2008-04-23 23:29:19 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package Cluster;
import java.awt.Frame;
import java.util.Hashtable;
import java.util.Observable;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.FileSetListener;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.XmlConfig;
import edu.stanford.genetics.treeview.model.IntHeaderInfo;

public class ClusterModel extends Observable implements DataModel {
  /*
   * This not-so-object-oriented hack is in those rare instances
   * where it is not enough to know that we've got a DataModel.
   */
	public String getType() {
		return "ClusterModel";
	}
	/** has model been successfully loaded? */
	boolean loaded = false;
	/*
	 * For cases where we are comparing two models (this needs to be changed).
	 */
	ClusterModel compareModel = null;
	int extraCompareExpr = 0;
	
  	public void setModelForCompare(DataModel m)
  	{
  		if(m == null)
  		{
  			compareModel = null;
  			extraCompareExpr = 0;
  		}
  		else
  		{
  			compareModel = (ClusterModel)m;
  			extraCompareExpr = compareModel.nExpr() + 2;
  		}
  		hasChanged();
  	}
    // accessor methods	
	public IntHeaderInfoCluster getGeneHeaderInfo() {
	  return geneHeaderInfo;
	}
	public IntHeaderInfoCluster getArrayHeaderInfo() {
	  return arrayHeaderInfo;
	}
	public ClusterDataMatrix getDataMatrix() {
		
		return dataMatrix;
	}
	public HeaderInfo getAtrHeaderInfo() {
		return atrHeaderInfo;
	}
	public HeaderInfo getGtrHeaderInfo() {
		return gtrHeaderInfo;
	}
    public boolean gweightFound() {
	return gweightFound;
    }

    public int nGene() {return geneHeaderInfo.getNumHeaders();}
    public int nExpr() {return arrayHeaderInfo.getNumHeaders() + extraCompareExpr;}

	public void setExprData(double [] newData) {
		dataMatrix.setExprData(newData);
	}
    
	public double getValue(int x, int y) {
		int nexpr = nExpr();
		int ngene = nGene();
		if(x >= nexpr + 2)
		{
			if (compareModel != null)
				return compareModel.getValue(x - (nexpr + 2), y); // check offsets
		}
		else if(x >= nexpr && y < ngene)
		{
			return 0; // gray border
		}
		if ((x < nexpr && y < ngene) &&	(x >= 0    && y >= 0))
		{
			return dataMatrix.getValue(x, y);
		}
		return NODATA;
    }

	public boolean aidFound() {return aidFound;}
	void aidFound(boolean newVal) {aidFound = newVal;}
    public boolean gidFound() {return gidFound;};
	void gidFound(boolean newVal) {gidFound = newVal;}

	public void setSource(ClusterFileSet source) {
		this.source = source;
		setChanged();
	}
	public String getSource() {
	  if (source == null) {
		return "No Data Loaded";
	  } else {
		return source.getTxt();
	  }
	}
	public String getName() {
		return getClusterFileSet().getRoot();
	}
	public ClusterFileSet getClusterFileSet() {
	  return source;
	}
	@Override
	public void clearFileSetListeners() {
		source.clearFileSetListeners();
	}
	@Override
	public void addFileSetListener(FileSetListener listener) {
		source.addFileSetListener(listener);
	}
    public XmlConfig getDocumentConfig() {return documentConfig;}
    public ConfigNode getDocumentConfigRoot() {return documentConfig.getRoot();}
    public void setDocumentConfig(XmlConfig newVal) { documentConfig = newVal;}
    
    public ClusterModel() {
    	super();
	  /* build ClusterModel, initially empty... */	
	  	geneHeaderInfo = new IntHeaderInfoCluster();
	  	arrayHeaderInfo = new IntHeaderInfoCluster();
	  	atrHeaderInfo = new IntHeaderInfoCluster();
		gtrHeaderInfo = new IntHeaderInfoCluster();
		dataMatrix = new ClusterDataMatrix();
    }
    
    public void setFrame(Frame f) {
	frame = f;
    }
	public Frame getFrame() {
		return frame;
	}

	protected void hashAIDs() {
		arrayHeaderInfo.hashIDs("AID");
	}
	 
	protected void hashGIDs() {
		geneHeaderInfo.hashIDs("GID");
	}
	 
	protected void hashATRs() {
		atrHeaderInfo.hashIDs("NODEID");
	}
	
	protected void hashGTRs() {
		gtrHeaderInfo.hashIDs("NODEID");
	}
	
	protected static Hashtable populateHash(HeaderInfo source, String headerName, Hashtable target) {
		int indexCol = source.getIndex(headerName);
		return populateHash(source, indexCol, target);
	}
	 protected static Hashtable populateHash(HeaderInfo source, int indexCol, Hashtable target) {
		 if (target == null) {
			 target = new Hashtable((source.getNumHeaders() * 4) /3, .75f);
		 } else {
			 target.clear();
		 }

		 if (indexCol <0) indexCol = 0;
		 for( int i = 0; i < source.getNumHeaders(); i++) {
			 target.put(source.getHeader(i)[indexCol], new Integer(i)); //puts all the specific infor from source file into Hashtable object
		 }
		 
		 return target;
	 }	 
	 /**
	  * Reorders all the arrays in the new ordering.
	  * @param ordering the new ordering of arrays, must have size equal to number of arrays
	  */
	public void reorderArrays(int [] ordering)
	{
		if(ordering == null || 
				ordering.length != dataMatrix.getNumUnappendedCol()) // make sure input to function makes sense
		{
			return;
		}
	
	
		DataMatrix data = getDataMatrix();
	
		double [] temp = new double[data.getNumUnappendedCol()];
		for(int j = 0; j < data.getNumRow(); j++)
		{
			for(int i = 0; i < ordering.length; i++)
			{
				temp[i] = data.getValue(ordering[i], j);
			}
			for(int i = 0; i < ordering.length; i++)
			{
				data.setValue(temp[i], i, j);
			}
		}
		String [][]aHeaders = arrayHeaderInfo.getHeaderArray();
		String [][] temp2 = new String[aHeaders.length][];
	
		for(int i = 0; i < aHeaders.length; i++)
		{
			if(i < ordering.length)
			{
				temp2[i] = aHeaders[ordering[i]];
			}
			else
			{
				temp2[i] = aHeaders[i];
			}
		}
		setArrayHeaders(temp2);
		hashAIDs();
		
					
		setChanged();
	}

	 /**
	  * Reorders all the arrays in the new ordering.
	  * @param ordering the new ordering of arrays, must have size equal to number of arrays
	  */
	public void reorderGenes(int [] ordering)
	{
		if(ordering == null || 
				ordering.length != dataMatrix.getNumRow()) // make sure input to function makes sense
		{
			return;
		}
		
		DataMatrix data = getDataMatrix();
		double [] temp = new double[data.getNumRow()];	
		for(int j = 0; j < data.getNumUnappendedCol(); j++)
		{
			for(int i = 0; i < ordering.length; i++)
			{
				temp[i] = data.getValue(j, ordering[i] );
			}
			for(int i = 0; i < ordering.length; i++)
			{
				data.setValue(temp[i], j, i);
			}
		}
		geneHeaderInfo.reorderHeaders(ordering);
		hashGIDs();
		setChanged();
	}

	 public void resetState () {
		 // reset some state stuff.
		 //	if (documentConfig != null)
		 //          documentConfig.store();
		 documentConfig = null;
		 setLoaded(false);
		 aidFound = false;
		 gidFound = false;
		 source = null;
		 
		 eweightFound = false;
		 gweightFound = false;
		 
		 geneHeaderInfo.clear();
		 arrayHeaderInfo.clear();
		 atrHeaderInfo.clear();
		 gtrHeaderInfo.clear();
		 dataMatrix.clear();
	 }
	 
	 public String toString() {
		 String [] strings = toStrings();
		 String msg = "";
		 for (int i = 0; i < strings.length; i++) {
			 msg += strings[i] + "\n";
		 }
		 return msg;
	 }
    public String[] toStrings() {
	String[] msg = {"Selected ClusterModel Stats",
			"Source = " + getSource(),
			"Nexpr   = " + nExpr(),
			"NGeneHeader = " + getGeneHeaderInfo().getNumNames(),
			"Ngene   = " + nGene(),
			"eweight  = " + eweightFound,
			"gweight  = " + gweightFound,
			"aid  = " + aidFound,
			"gid  = " + gidFound};
	
		return msg;
    }
    
    public void removeAppended()
	{
		if(appendIndex == -1)
		{
			return;
		}
		int ngene = nGene();
		int nexpr = nExpr();
		double [] temp = new double[ngene*appendIndex];
		
		int i = 0;
		
		for(int g = 0; g < this.dataMatrix.getNumRow(); g++)
		{
			for(int e = 0; e < nexpr; e++)
			{
				if(e < appendIndex)
				{
					temp[i++] = getValue(e, g);
				}					
			}
		}
		dataMatrix.setExprData(temp);
		
		String [][] tempS = new String[appendIndex][];
		
		for(int j = 0; j < appendIndex; j++)
		{
			tempS[j] = arrayHeaderInfo.getHeader(j);
		}
		
		arrayHeaderInfo.setHeaderArray(tempS);
		nexpr = appendIndex;
		appendIndex = -1;
		setChanged();
	}
	
	/**
	 * Appends a second matrix to this one provided they have the same height. Used for comparison of two data sets where the data is displayed side by side.
	 * 
	 */
	public void append(DataModel m)
	{
		int ngene = nGene();
		int nexpr = nExpr();
		if(m == null || m.getDataMatrix().getNumRow() != ngene || appendIndex != -1)
		{
			System.out.println("Could not compare.");
			return;
		}
		
		double [] temp = new double[getDataMatrix().getNumRow()* getDataMatrix().getNumCol() + 
		                            m.getDataMatrix().getNumRow()*(m.getDataMatrix().getNumCol() + 1)];
		
		int i = 0;
				
		for(int g = 0; g < m.getDataMatrix().getNumRow(); g++)
		{
			for(int e = 0; e < nexpr + m.getDataMatrix().getNumCol() + 1; e++)
			{
				if(e < nexpr)
				{
					temp[i++] = getValue(e, g);
				}
				else if(e < nexpr + 1)
				{
					temp[i++] = DataModel.NODATA;
				}
				else
				{
					temp[i++] = m.getDataMatrix().getValue(e - nexpr - 1, g);
				}
				
			}
		}
		
		String [][] tempS = new String[getArrayHeaderInfo().getNumHeaders() + m.getArrayHeaderInfo().getNumHeaders() + 1][];
		
		i = 0;
		for(int j = 0; j < getArrayHeaderInfo().getNumHeaders(); j++)
		{
			tempS[i++] = getArrayHeaderInfo().getHeader(j);
		}
		
		tempS[i] = new String[getArrayHeaderInfo().getNumNames()];
		
		for(int j = 0; j < tempS[i].length; j++)
		{
			tempS[i][j] = "-----------------------";
		}
		i++;
		
		for(int j = 0; j < getArrayHeaderInfo().getNumHeaders(); j++)
		{
			tempS[i++] = getArrayHeaderInfo().getHeader(j);
		}
		
		
		arrayHeaderInfo.setHeaderArray(tempS);
		appendIndex = nexpr;
		nexpr += m.getDataMatrix().getNumCol() + 1;
		dataMatrix.setExprData(temp); //sets the dataset in the model to the updated version
		setChanged();
	}

	int appendIndex = -1;
	
    protected Frame frame;
    protected ClusterFileSet source = null;
    protected String  dir = null;
    protected String  root;

	
	protected ClusterDataMatrix dataMatrix;
		
		
    protected IntHeaderInfoCluster arrayHeaderInfo;
    protected IntHeaderInfoCluster geneHeaderInfo;
	protected IntHeaderInfoCluster atrHeaderInfo;
	protected IntHeaderInfoCluster gtrHeaderInfo;
	
	protected boolean aidFound = false;
	protected boolean gidFound = false;
		
    protected boolean eweightFound = false;
    protected boolean gweightFound = false;
    protected XmlConfig documentConfig; // holds document config
	/**
	 * Really just a thin wrapper around exprData array.
	 * @author aloksaldanha
	 *
	 */
	class ClusterDataMatrix implements DataMatrix {
		private boolean modified = false;
	    private double [] exprData = null;

		public void clear() {
			exprData = null;
		}

	    public double getValue(int x, int y) {
			int nexpr = nExpr();
			int ngene = nGene();
			if ((x < nexpr) && (y < ngene) && (x >= 0) && (y >= 0)) {
				return exprData[x + y * nexpr];
			} else {
				return DataModel.NODATA;
			}
		}
		
	    //used to set the parameter as an 'expression data' array for the model which the model then can access
		public void setExprData(double[] newData) {
			exprData = newData;
		}

		public void setValue(double value, int x, int y)
		{
			exprData[x + y*getNumCol()] = value;
			setModified(true);
			setChanged();
		}
		public int getNumRow() {
			return nGene();
		}
		public int getNumCol() {
			return nExpr();
		}
		
		public int getNumUnappendedCol()
		{
			return appendIndex == -1?getNumCol():appendIndex;
		}

		public void setModified(boolean modified) {
			this.modified = modified;
		}

		public boolean getModified() {
			return modified;
		}
		
		/**
		 * Used to retrieve the double array of all the data in other classes
		 * @return
		 */
		public double[] getExprData(){
			
			return exprData;
		}
	}

	/** holds actual node information for array tree */
	public void setAtrHeaders(String [][]atrHeaders) {
		atrHeaderInfo.setHeaderArray(atrHeaders);
	}
	/** holds header row from atr file */
	public void setAtrPrefix(String [] atrPrefix) {
		atrHeaderInfo.setPrefixArray(atrPrefix);
	}

	/** holds actual node information for gene tree */
	public void setGtrHeaders(String [][] gtrHeaders) {
		gtrHeaderInfo.setHeaderArray(gtrHeaders);
	}

	public void setGtrPrefix(String [] gtrPrefix) {
		gtrHeaderInfo.setPrefixArray(gtrPrefix);
	}

	public void setArrayHeaders(String [] [] newHeaders) {
		arrayHeaderInfo.setHeaderArray(newHeaders);
	}
	public void setArrayPrefix(String [] newPrefix) {
		arrayHeaderInfo.setPrefixArray(newPrefix);
	}
	
	
	
	class GeneHeaderInfo extends IntHeaderInfo {
		
	  public int getAIDIndex() {
		return 1;
	  }
	public int getGIDIndex() {
		return 0;
	  }
	  public int getYorfIndex() {
		if (getIndex("GID") == -1) {
		  return 0;
		} else {
		  return 1;
		}
	  }
	  public int getNameIndex() {
		if (getIndex("GID") == -1) {
		  return 1;
		} else {
		  return 2;
		}
	  }
	  
	  
	  /**
	  * There are two special indexes, YORF and NAME.
	  */
	  public int getIndex(String header) {
		  int retval = super.getIndex(header);
		  if (retval != -1) {
			  return retval;
		  }
		  if (header.equals("YORF")) {
			  return getYorfIndex();
		  }	

		  if(header.equals("NAME")) {
			  return getNameIndex();
		  }	
		  return -1;
	  }
	  
	}

	void setGenePrefix(String [] newVal) {
		  geneHeaderInfo.setPrefixArray(newVal);
	  }
	  void setGeneHeaders(String [][] newVal) {
		  geneHeaderInfo.setHeaderArray(newVal);
	  }
	// loading stuff follows...

    /**
     *
     *
     * @param fileSet fileset to load
     *
     */
	 public void loadNew(ClusterFileSet fileSet) throws LoadException {
		 resetState();
		 setSource(fileSet);
		 final ClusterLoader loader = new ClusterLoader(this);
		 loader.loadInto();
		 if (!isLoaded())
			 throw new LoadException("Loading Cancelled", LoadException.INTPARSE);
	 }
	 /**
	 * Don't open a loading window...
	 */
	 public void loadNewNW(ClusterFileSet fileSet) throws LoadException {
		 resetState();
		 setSource(fileSet);
		 final ClusterLoader loader = new ClusterLoader(this);
		 loader.loadIntoNW(); 
		 if (!isLoaded())
			 throw new LoadException("Loading Cancelled", LoadException.INTPARSE);
	 }

	/**
	 * @param b
	 */
	public void setEweightFound(boolean b) {
		eweightFound = b;
	}
	/**
	 * @param b
	 */
	public void setGweightFound(boolean b) {
		gweightFound = b;
		
	}
	public boolean getModified() {
		return  getGtrHeaderInfo().getModified() ||
//		getGeneHeaderInfo().getModified() ||
//		getArrayHeaderInfo().getModified() ||
		getAtrHeaderInfo().getModified();
	}
	public boolean isLoaded() {
		return loaded;
	}
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}
	@Override
	public FileSet getFileSet() {
		// TODO Auto-generated method stub
		return null;
	}

}
