package edu.stanford.genetics.treeview.model;

import java.util.Hashtable;
import java.util.Observable;

import edu.stanford.genetics.treeview.HeaderInfo;

/**
 * A generic headerinfo, backed by private arrays.
 * 
 * @author aloksaldanha
 *
 */
public class IntHeaderInfo extends Observable implements HeaderInfo {
	
	private String [] prefixArray = new String[0];
	private String [][] headerArray = new String[0][];
	private Hashtable<String, Integer> id2row = new Hashtable<String, Integer>();
	
	public void hashIDs(String header) {
		int index = getIndex(header);
		id2row = TVModel.populateHash(this, index , id2row);
	}
	public void clear() {
		prefixArray = new String[0];
		setHeaderArray(new String[0][]);
		id2row.clear();
	}
	public void setPrefixArray(String[] newVal) {
		prefixArray = newVal;
	}
	public void setHeaderArray(String[][] newVal) {
		headerArray = newVal;
	}
	public String [] getNames() { 
		return prefixArray;
	}
	public int getNumNames() {
		return prefixArray.length;
	}
	  
	public int getNumHeaders() {
		return getHeaderArray().length;
	}
	  
	  /**
	  * Returns the header for a given gene and column heading.
	  */
	  public String [] getHeader(int gene) {
		  try{
			  if (getHeaderArray()[gene] == null) {
				  return new String[0];
			  } else {
				  return getHeaderArray()[gene];
			  }
		  } catch (java.lang.ArrayIndexOutOfBoundsException e) {
			  System.out.println("error: tried to retrieve header for  index " +
					  				gene + " but max is "+ getHeaderArray().length);
			  e.printStackTrace();
			  return new String[0];
		  }
	  }

	  /**
	  * Returns the header for a given gene and column heading,
	  * or null if not present.
	  */
	  public String getHeader(int gene, String col) {
		int index = getIndex(col);
		if (index == -1) {
			return null;
		}
		return getHeader(gene, index);
	  }
		public String getHeader(int rowIndex, int columnIndex) {
			  return (getHeader(rowIndex))[columnIndex];
		}
		  public int getIndex(String header) {
				for (int i = 0 ; i < prefixArray.length; i++) {
				  if (header.equalsIgnoreCase(prefixArray[i]))
					return i;
				}
				return -1;
			  }
	  
		public int getHeaderIndex(String id) {
			Object ind = id2row.get(id);
			if (ind == null) {
				return -1;
			} else {
				return ((Integer) ind).intValue();
			}
		}

		/**
		 * adds new header column of specified name at specified index.
		 * @param name
		 * @param index
		 * @return
		 */
		public boolean addName(String name, int index) {
			int existing = getIndex(name);
			//already have this header
			if (existing != -1) return false;
			int newNumNames = getNumNames()+1;
			for (int row = 0; row < getNumHeaders(); row++) {
				String [] from = getHeaderArray()[row];
				String [] to = new String[newNumNames];
				for (int col = 0; col < index; col++)
					to[col] = from[col];
				for (int col = index+1; col < newNumNames; col++)
					to[col] = from[col-1];
				getHeaderArray()[row] = to;
			}
			String [] newPrefix = new String[newNumNames];
			for (int col = 0; col < index; col++)
				newPrefix [col] = prefixArray[col];
			newPrefix[index] = name;
			for (int col = index+1; col < newNumNames; col++)
				newPrefix [col] = prefixArray[col-1];
			prefixArray = newPrefix;
			setModified(true);
			return true;
		}
		public boolean reorderHeaders(int [] ordering) {
			if (ordering.length == getHeaderArray().length) {					
				String [][] temp2 = new String[getHeaderArray().length][];
				
				for(int i = 0; i < getHeaderArray().length; i++)
				{
					if(i < ordering.length)
					{
						temp2[i] = getHeaderArray()[ordering[i]];
					}
					else
					{
						temp2[i] = getHeaderArray()[i];
					}
				}
				setHeaderArray(temp2);
				return true;
			} else {
				return false;
			}
		}
		
		public boolean setHeader(int i, String name, String value) {
			if (getHeaderArray().length < i) return false;
			int nameIndex = getIndex(name);
			if (nameIndex == -1) return false;
			if (getHeaderArray()[i][nameIndex] == value) return false;
			getHeaderArray()[i][nameIndex] = value;
			setModified(true);
			return true;
		}
		public boolean getModified() {return modified;}
		public void setModified(boolean mod) {
			setChanged();
			notifyObservers();
			modified = mod;
		}
		public String [][] getHeaderArray()
		{
			return headerArray;
		}
		private boolean modified = false;
		/*
		public void printHashKeys() {
			Enumeration e = id2row.keys();
			while (e.hasMoreElements()) {
				System.err.println(e.nextElement());
			}
		}
		*/
	}