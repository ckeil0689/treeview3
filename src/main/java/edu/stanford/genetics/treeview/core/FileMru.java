/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.core;

import java.util.Observable;
import java.util.Stack;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;

/*
 *  Decompiled by Mocha from FileMru.class
 */
/*
 *  Originally compiled from FileMru.java
 */

/**
 * This class encapsulates an xml-based most recently used list of files
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.1 $ $Date: 2006-09-25 22:02:02 $
 */
public class FileMru extends Observable implements ConfigNodePersistent {

	/*
	 * Java Preference node of "/TreeViewApp/TreeViewFrame/FileMRU"
	 */
	private Preferences configNode;

	/*
	 * Maximum number of files to be stored in the Preferences.
	 * Essentially changing this value should effect maximum number
	 * of recently opened files stored
	 */
	private static final int MAX_STORED_FILES = 10;
	
	/*
	 * Contains the children of configNode. Note that Both configNode and
	 * this stack gets populate at the application start
	 */
	private SizedStack<Preferences> fileNodes = new SizedStack<Preferences>(MAX_STORED_FILES);
	
	/*
	 * skipIndex is used to skip a node in the fileNodes while reading it
	 * This is used when the user loads a file that is already present in
	 * the recently loaded list.
	 */
	private int skipIndex = -1;

	/**
	 * Binds FileMru to a ConfigNode
	 *
	 * @param configNode
	 *            Node to be bound to
	 */
	@Override
	public synchronized void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(StringRes.pnode_FileMRU);
			
			pushChildrenIntoFileStack();

		} else {
			LogBuffer.println("Could not find or create FileMRU Preferences"
					+ "node because parentNode was null.");
			return;
		}
		
		this.configNode = parentNode.node(StringRes.pnode_FileMRU);
		setChanged();
	}
	
	@Override
	public Preferences getConfigNode() {
		
		return configNode;
	}

	@Override
	public void requestStoredState() {
		return;
	}

	@Override
	public void storeState() {
		return;
	}
	
	@Override
	public void importStateFrom(Preferences oldNode) {
		return;
	}

	/**
	 * This push of Files into the fileNodes stack must happen only 
	 * once, during the application start. This method essentially
	 * builds the file stack from the java preferences
	 */
	private void pushChildrenIntoFileStack() {
		try {
			for(String fileName : configNode.childrenNames()){
				fileNodes.push(configNode.node(fileName));
			}
		}
		catch(BackingStoreException e) {
			e.printStackTrace();
			LogBuffer.println("BackingStoreException when trying to remove"
					+ " a file in FileMRU: " + e.getMessage());
		}
	}

	/**
	 * Create subnode of current confignode
	 * File+currentTimeMillis is the relative Node name while creating
	 * a node
	 * @return Newly created subnode
	 */
	public synchronized Preferences createSubNode() {

		setChanged();

		final long subNodeIndex = System.currentTimeMillis();
		LogBuffer.println("Creating FileMRU subNode File" + subNodeIndex);
		Preferences node = configNode.node("File" + subNodeIndex);
		fileNodes.push(node);
		return node;
	}

	/**
	 * Gets the ConfigNode of the ith file
	 *
	 * @param i
	 *            Index of file to get node for
	 * @return The corresponding ConfigNdoe
	 */
	public Preferences getConfig(final int i) {
		
		return fileNodes.get(i);
	}

	/**
	 * Gets the configs of all files. skipIndex is used only when the user 
	 * loads a recently loaded file. Makes sure that the File is not shown
	 * in the recently loaded list again
	 * 
	 * @return Array of all ConfigNodes
	 */
	public Preferences[] getConfigs() {

		int numOfFiles = fileNodes.size();
		Preferences[] children = null;
		if(skipIndex == -1)
			children = new Preferences[numOfFiles];
		else
			children = new Preferences[numOfFiles-1];
		int k = 0;
		for (int i = 0; i < numOfFiles; i++) {
			if(i == skipIndex)
				continue;
			children[k] = fileNodes.get(i); 
			k++;
		}
		return children;
	}

	/**
	 * Gets names of all recently used files
	 *
	 * @return String [] of file names for display
	 */
	public String[] getFileNames() {
		
		int numOfFiles = fileNodes.size();
		final String astring[] = new String[numOfFiles];

		for (int i = 0; i < numOfFiles; i++) {
			if(i == skipIndex)
				continue;
			astring[i] = fileNodes.get(i).get("root", "")
					+ fileNodes.get(i).get("cdt", "");
		}
		return astring;
	}

	/**
	 * returns dir of most recently used file or null
	 *
	 * @return The Most Recent Dir or null
	 */
	public String getMostRecentDir() {
		if(fileNodes.isEmpty())
			return null;
		return fileNodes.peek().get("dir", null);
	}

	/**
	 * Delete the ith file in the list
	 *
	 * @param i
	 *            The the index of the file to delete.
	 */
	public synchronized void removeFile(final int i) {

		fileNodes.remove(i);
		setChanged();
	}

	/**
	 * Sets configNode to be last in list
	 *
	 * @param configNode
	 *            Node to move to end
	 */
	public synchronized void setLast() {
		/* 
		 * We probably do not need to set last because we can easily pop or peek
		 * from the fileNodes stack
		 */
		setChanged();
	}

	/**
	 * Returns the last FileSet that was opened by the user.
	 *
	 * @return The last open FileSet
	 */
	public synchronized FileSet getLast() {

		if(fileNodes.isEmpty())
			return null;
		return new FileSet(fileNodes.peek());
	}

	/**
	 * Must notify explicitly when a managed fileset is modified (perhaps should
	 * pass modifications through Mru?
	 */
	public void notifyFileSetModified() {

		setChanged();
	}

	/**
	 * Move FileSet to end of list
	 *
	 * @param fileSet
	 *            FileSet to move
	 */
	public synchronized void setLast(final FileSet fileSet) {
		/* 
		 * We probably do not need to set last because we can easily pop or peek
		 * from the fileNodes stack
		 */
	}

	/**
	 * add a fileset if it's not already in the list. Or, if it is in the list,
	 * delete it and create a new node, then copy the fileset
	 *
	 * @return the fileset corresponding to the correct config node
	 */
	public synchronized FileSet addUnique(final FileSet inSet) {

		// check existing file nodes...
		final Preferences[] aconfigNode = getConfigs();
		Preferences nodeToDelete = null;
		for (final Preferences element : aconfigNode) {
			final FileSet fileSet2 = new FileSet(element);
			if (fileSet2.equalsFileSet(inSet)) {
				LogBuffer.println("Found Existing node in MRU list for "
						+ inSet);
				
				nodeToDelete = element;
				break;
			}
		}
		
		final Preferences configSubNode = createSubNode();
		final FileSet fileSet3 = new FileSet(configSubNode);
		fileSet3.copyState(inSet);
		LogBuffer.println("Creating new fileset " + fileSet3);
		// now delete the old node
		if(nodeToDelete != null){
			fileNodes.remove(fileNodes.indexOf(nodeToDelete));
			inSet.setNode(configSubNode);
			setChanged();
			/*
			 * this is when the user loads an already loaded file, here we set
			 * skipIndex and notify listeners i.e. FileMenu to update with the
			 * new set of files (skipping the current file)
			 */
			skipIndex = fileNodes.indexOf(configSubNode);
			notifyObservers();
			skipIndex = -1;
		}
		return fileSet3;
	}

	public synchronized void removeMoved() {

		final Preferences[] nodes = getConfigs();

		for (int i = nodes.length; i > 0; i--) {

			final FileSet fileSet = new FileSet(nodes[i - 1]);
			if (fileSet.hasMoved()) {
				LogBuffer.println("Could not find " + fileSet.getCdt() + ", "
						+ "removing from mru...");
				removeFile(i - 1);
				setChanged();
			}
		}
	}

	/**
	 * removes any duplicates of this fileset in the mru list. it will keep the
	 * _last_ in the list.
	 *
	 * @param fileSet
	 */
	public void removeDuplicates(final FileSet inSet) {

		final Preferences[] nodes = getConfigs();
		for (int i = nodes.length; i > 0; i--) {
			final FileSet fileSet = new FileSet(nodes[i - 1]);
			if (fileSet.equalsFileSet(inSet)) {
				// delete node, keep the keeper
				LogBuffer.println("Found duplicate of " + fileSet.getCdt()
						+ ", removing from mru...");
				removeFile(i - 1);
				setChanged();
			}
		}
	}

	/**
	 * Returns the names of the current children of this class' root node.
	 *
	 * @return
	 */
	public String[] getRootChildrenNodes() {

		String[] childrenNodes = new String[fileNodes.size()];
		for(int i=0 ; i<fileNodes.size() ; i++){
			childrenNodes[i] = fileNodes.get(i).name();
		}

		return childrenNodes;

	}
	
	/*
	 * This is an implementation of a sized Stack, meaning max
	 * size of Stack will not go over given input size 
	 * T needs to be of Type Preferences
	 */
	private class SizedStack<T> extends Stack<T> {
		private static final long serialVersionUID = 1L;
		private int maxSize;

	    public SizedStack(int size) {
	        super();
	        this.maxSize = size;
	    }

	    @Override
	    public T push(T object) {
	        //If the stack is too big, remove elements until it's the right size.
	        while (this.size() >= maxSize) {
	            this.remove(0);
	        }
	        return super.push(object);
	    }
	    
	    @Override
	    public T remove(int i) {
	    	try {
				configNode.node(((Preferences)this.get(i)).name()).removeNode();
				configNode.flush();
			}
			catch(BackingStoreException e) {
				e.printStackTrace();
				LogBuffer.println("BackingStoreException when trying to remove"
						+ " a file in FileMRU: " + e.getMessage());
			}
            return super.remove(i);
	    }
	    
	    @Override
	    public T get(int i) {
			try{
				return super.get(i);
			}catch(ArrayIndexOutOfBoundsException e){
				e.printStackTrace();
				LogBuffer.println("Index ["+i+"] is out of reach, The range of this"
			        + "file stack in FileMRU.java is [0-"+this.size()+"]");
			}
			return null;
	    }
	}
}
