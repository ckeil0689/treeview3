/* BEGIN_HEADER                                              Java TreeView
*
* $Author: alokito $
* $RCSfile: TVModelLoader2.java,v $f
* $Revision: 1.27 $
* $Date: 2010-05-02 13:10:18 $
* $Name:  $
*
* This file is part of Java TreeView
* Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved.
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LoadProgress2;
import edu.stanford.genetics.treeview.LoadProgress2I;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ProgressTrackable;
import edu.stanford.genetics.treeview.SwingWorker;
import edu.stanford.genetics.treeview.XmlConfig;

public class ClusterLoader implements ProgressTrackable {
	public class DummyLoadProgress implements LoadProgress2I {
		private boolean finished = false;
		private boolean hadProblem = false;
		private int length = 0;
		private int value = 0;
		private int phaseLength = 0;
		private int phaseValue = 0;
		private String phaseText = "";
		@Override
		public boolean getCanceled() {return false;}
		@Override
		public LoadException getException() {return null;}
		@Override
		public boolean getFinished() {return finished;}
		@Override
		public boolean getHadProblem() {return hadProblem;}
		@Override
		public int getLength() {return length;}
		@Override
		public int getPhaseLength() {return phaseLength;}
		@Override
		public String getPhaseText() {return phaseText;}
		@Override
		public int getPhaseValue() {return phaseValue;}
		@Override
		public int getValue() {return value;}
		@Override
		public void incrValue(int i) {value++;}
		@Override
		public void println(String s) {System.out.println(s);}
		@Override
		public void setButtonText(String text) {}
		@Override
		public void setCanceled(boolean canceled) {}
		@Override
		public void setException(LoadException exception) {}
		@Override
		public void setFinished(boolean finished) {this.finished = finished;}
		@Override
		public void setHadProblem(boolean hadProblem) {this.hadProblem = hadProblem;}
		@Override
		public void setIndeterminate(boolean flag) {}
		@Override
		public void setLength(int i) {this.length = i;}
		@Override
		public void setPhase(int i) {this.phaseValue = i;}
		@Override
		public void setPhaseLength(int i) {this.phaseLength = i;}
		@Override
		public void setPhaseText(String i) {this.phaseText = i;}
		@Override
		public void setPhaseValue(int i) {this.phaseValue = i;}
		@Override
		public void setPhases(String[] strings) {}
		@Override
		public void setValue(int i) {this.value = i;}
		@Override
		public void setVisible(boolean b) {}
	}

	// these internal variables are needed by this class only
	/** frame to block */
	Frame parent;
	/** model to load into */
	ClusterModel targetModel;

	private javax.swing.Timer loadTimer;

	LoadProgress2I loadProgress = new DummyLoadProgress();
	
	// the following is for the communication between the timer 
	//thread and the worker thread.
	
	/** Setter for exception */
	public void setException(LoadException exception) {
		loadProgress.setException(exception);
	}
	/** Getter for exception */
	public LoadException getException() {
		return loadProgress.getException();
	}
	
	/** Setter for hadProblem */
	public void setHadProblem(boolean hadProblem) {
		loadProgress.setHadProblem(hadProblem);
	}
	/** Getter for hadProblem */
	public boolean getHadProblem() {
		return loadProgress.getHadProblem();
	}
	
	/**
	* Length in bytes of the input stream, or -1 if not known.
	*/
	@Override
	public void setLength(int length) {
		loadProgress.setLength(length);
	}
	/** Getter for length */
	@Override
	public int getLength() {
		return loadProgress.getLength();
	}
	/** Setter for value */
	@Override
	public void setValue(int value) {
		loadProgress.setValue(value);
	}
	/** Getter for value */
	@Override
	public int getValue() {
		return loadProgress.getValue();
	}
	@Override
	public void incrValue(int i) {
		loadProgress.incrValue(i);
	}
	
	/** Setter for finished */
	public void setFinished(boolean finished) {
		loadProgress.setFinished(finished);
	}
	/** Getter for finished */
	public boolean getFinished() {
		return loadProgress.getFinished();
	}
	
	/** Setter for phaseValue */
	public void setPhaseValue(int phaseValue) {
		loadProgress.setPhaseValue(phaseValue);
	}
	/** Getter for phaseValue */
	public int getPhaseValue() {
		return loadProgress.getPhaseValue();
	}
	
	/** Setter for phaseLength */
	public void setPhaseLength(int phaseLength) {
		if (loadProgress != null)
			loadProgress.setPhaseLength(phaseLength);
	}
	/** Getter for phaseLength */
	public int getPhaseLength() {
		return loadProgress.getPhaseLength();
	}
	
	public void setPhaseText(String phaseText) {
		loadProgress.setPhaseText(phaseText);
	}
	/** Getter for phaseText */
	public String getPhaseText() {
		return loadProgress.getPhaseText();
	}
	
	protected FlatFileParserCluster parser = new FlatFileParserCluster();
	
	
	public ClusterLoader(ClusterModel targetModel) {
		this(targetModel, targetModel.getFrame());
	}
	/**
	 * @param strings
	 */

	private String [] phases = new String [] {"Loading Txt",
			"Parsing Txt", "Loading Document Config", "Finished"};
	
	public ClusterLoader(ClusterModel targetModel, Frame parent) {
		this.parent = parent;
		this.targetModel = targetModel;
	}
	class TimerListener implements ActionListener { // manages the FileLoader
		// this method is invoked every few hundred ms
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (getCanceled() || getFinished()) {
				setFinished(true);
				loadTimer.stop();
				if (getHadProblem() == false) { 
					loadProgress.setVisible(false);
				} else {
					loadProgress.setButtonText("Dismiss");
					Toolkit.getDefaultToolkit().beep();
					try {
					((LoadProgress2) loadProgress).getToolkit().beep();
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	public void loadInto() throws LoadException {
		loadProgress = new LoadProgress2(targetModel.getClusterFileSet().getRoot(), 
				parent);
		loadProgress.setPhases(phases);
		final SwingWorker worker = new SwingWorker() {								//Might be an issue
			@Override
			public Object construct() {
				run();
				return null;
			}
		};
		// start up the worker thread
		worker.start();
		loadTimer = new javax.swing.Timer(200, new TimerListener());
		loadTimer.start();
		// show a modal dialog, should block until loading done...
		loadProgress.setIndeterminate(true);
		((LoadProgress2) loadProgress).pack();
		loadProgress.setVisible(true);
		
		// but just in case it doesn't, we'll join on the worker
		try {
			worker.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("loadNew 6, ex: " + fileLoader.getException());
		if (getException() != null) {
			throw getException();
		}
	}
	/**
	* Don't open a window.
	*/
	public void loadIntoNW() throws LoadException {
//		loadProgress = new LoadProgress2(targetModel.getFileSet().getRoot(), null);
//		loadProgress.setPhases(phases);
		run();
		if (getException() != null) {
			throw getException();
		}
	}
	
	protected void setPhase(int i) {
		loadProgress.setPhase(i);
	}
	
	// this routine manages phase bar stuff. 
	// progress bar stuff is set within the 
	// table loading and various parsing routines.
	protected void run() {
		try {
			ClusterFileSet fileSet = targetModel.getClusterFileSet(); //targetModel the model that was originally passed?
			setPhaseLength(phases.length);
			setPhase(0);
			println("loading " + fileSet.getTxt() + " ... "); 	      //definitely loads the correct fileSet					
			try {
				//parser.setParseQuotedStrings(fileSet.getParseQuotedStrings());       //parser is a FileClusterParser object
				parser.setResource(fileSet.getTxt()); 								
				parser.setProgressTrackable(this);
				RectDataCluster tempTable = parser.loadIntoTable();						//tempTable contains all the data! oadIntoTable call load() function
																						// which parses all the data from the fileSet, creates a RecDataCluster
																						//object and eventually adds the data to that object. It then returns it.
				
				if (loadProgress.getCanceled()) return;
				setPhase(1);
				parseTXT(tempTable);                								//parses the loaded data
			} catch (LoadException e) {
				throw e;
			} catch (Exception e) {
				// this should never happen!
				LogBuffer.println("ClusterModel.ResourceLoader.run() : while " +
						"parsing txt got error " + e.getMessage());
				e.printStackTrace();
				throw new LoadException("Error Parsing TXT: " + e, 
						LoadException.TXTPARSE);
			}
			if (loadProgress.getCanceled()) return;
			
			setPhase(2);
			try {
				println("parsing jtv config file");
				String xmlFile = targetModel.getClusterFileSet().getJtv();
				
				XmlConfig documentConfig;
				if (xmlFile.startsWith("http:")) {
					documentConfig = new XmlConfig(new URL(xmlFile), 
							"DocumentConfig");
				} else {
					documentConfig = new XmlConfig(xmlFile, "DocumentConfig");
				}
				targetModel.setDocumentConfig(documentConfig);
			} catch (Exception e) {
				targetModel.setDocumentConfig(null);
				println("Got exception " + e);
				setHadProblem(true);
			}
			if (loadProgress.getCanceled()) return;

			targetModel.setLoaded(true);
			//	ActionEvent(this, 0, "none",0);
		} catch (java.lang.OutOfMemoryError ex) {
							JPanel temp = new JPanel();
							temp. add(new JLabel("Out of memory, allocate " +
									"more RAM"));
							temp. add(new JLabel("see Chapter 3 of " +
									"Help->Documentation... for Out " +
									"of Memory"));
							JOptionPane.showMessageDialog(parent,  temp);
		} catch (LoadException e) {
			setException(e);
			println("error parsing File: " + e.getMessage());
			println("parse cannot succeed. please fix.");
			setHadProblem(true);
		}
		setFinished(true);
	}
	
	
	/**
	* This routine expects a vector of strings
	* It calls various routines that parse the expression data and annotations.
	* 
	* representing the tab-delimitted text
	*/
	protected void  parseTXT(RectDataCluster tempVector) throws LoadException {
		// find eweightLine, ngene, nexpr
		findTxtDimensions(tempVector);
		loadArrayAnnotation(tempVector);
		loadGeneAnnotation(tempVector);
		loadTxtData(tempVector);
	}
	/**
	 * finds ngene, nexpr, nArrayPrefix, nGenePrefix
	 * 
	 * Uses new style code that takes advantage of RectData structure and is fast.
	 */
	protected void findTxtDimensions(RectDataCluster tempVector)  {
		println("Finding Txt Dimensions");		
		
////	String [] firstLine =(String []) tempVector.elementAt(0);
		int gweightCol = -1;
		int rectCol = tempVector.getCol();

////	for (int i =0; i < firstLine.length; i++) {
		for (int i = 0; i < rectCol; i++){
////		String s = firstLine[i];
			String s = tempVector.getColumnName(i);
			if (s == null) {
				setHadProblem(true);
				println("Got null header, setting to empty string");
				s = "";
			}
			if (s.equalsIgnoreCase("GWEIGHT")) {
				gweightCol = i;
				break;
			}
		}
		
		if (gweightCol == -1) {
////		if (firstLine[0].equalsIgnoreCase("GID")) {
			if (tempVector.getColumnName(0).equalsIgnoreCase("GID")){
			nGenePrefix = 3;
			} else {
				nGenePrefix = 2;
			}
		} else {
			nGenePrefix = gweightCol + 1;
		}
		
////	nexpr = firstLine.length - nGenePrefix;
		nExpr = rectCol - nGenePrefix; 										//number of columns in .txt
		int eweightRow = -1;
		if (tempVector.getColumnName(0).equalsIgnoreCase("EWEIGHT")){
			eweightRow = 0;
		}else{
			int rectRow = tempVector.getRow(); 
			for (int i = 0; i < rectRow; i++) {
////		String s = ((String []) tempVector.elementAt(i))[0];
				String s = tempVector.getString(i, 0);
				if (s.equalsIgnoreCase("EWEIGHT")) {
					eweightRow = i+1;
					break;
				}
			}
		}
		if (eweightRow == -1) {
////		String [] secondLine =(String []) tempVector.elementAt(1);
////		if (secondLine[0].equalsIgnoreCase("AID")) {
			if(tempVector.getString(0, 0).equalsIgnoreCase("AID")){
				nExprPrefix = 2;
			} else {
				nExprPrefix = 1;
			}
		} else {
			nExprPrefix = eweightRow + 1;
		}
		
		nGene = tempVector.size() - nExprPrefix; //Number of gene lines in .txt
		
		setLength(100);
		setValue(100);
		targetModel.setEweightFound(eweightRow != -1);
		targetModel.setGweightFound(gweightCol != -1);
	}
	/**
	 * Loads array annotation from RectData into targetModel.
	 * 
	 * @param tempVector RectData contain annotation info
	 */
	protected void loadArrayAnnotation(RectDataCluster tempVector) {
		println("loading Array Annotations");
		String [] arrayPrefix = new String[nExprPrefix];    //not sure what arrayPrefix is but it's 2 units long
		String [][] aHeaders = new String [nExpr][nExprPrefix];
		
		for (int i = 0; i < nExprPrefix; i++) {
			String [] tokens = (String []) tempVector.elementAt(i);
			arrayPrefix[i] = tokens[0];
			for (int j = 0; j < nExpr; j++)
				aHeaders[j][i] = tokens[j + nGenePrefix];
		}
		targetModel.setArrayPrefix(arrayPrefix);           //sets the prefixArray of IntHeaderInfoCluster class
		targetModel.setArrayHeaders(aHeaders);
	}
	
	/**
	 * Loads gene annotation from RectData into targetModel.
	 * 
	 * @param tempVector RectData contain annotation info
	 */
	protected void loadGeneAnnotation(RectDataCluster tempVector) {
		println("loading Gene Annotations");
		String [] genePrefix = new String[nGenePrefix];
		String [][] gHeaders = new String [nGene][nGenePrefix];
		
		String [] firstLine = (String []) tempVector.elementAt(0);
		for (int i = 0; i < nGenePrefix; i++) {
			genePrefix[i] = firstLine[i];
		}
		setLength(nGene);
		for (int i = 0; i < nGene; i++) {
			setValue(i);
//			String [] tokens = (String []) tempVector.elementAt(i + nArrayPrefix);
			for (int j = 0; j < nGenePrefix; j++) {
//				gHeaders[i][j] = tokens[j];
				gHeaders[i][j] = tempVector.getString(i+nExprPrefix-1,j);
			}
		}
		targetModel.setGenePrefix(genePrefix);
		targetModel.setGeneHeaders(gHeaders);
	}
	
	protected void loadTxtData(RectDataCluster tempVector) {
		println("Parsing strings into doubles...");
		setLength(nGene);
		double [] exprData = new double[nGene * nExpr];
		
		for (int gene = 0 ; gene < nGene; gene++) {
			if (getFinished() == true) break; // we're cancelled
			setValue(gene);
			String [] tokens = (String []) tempVector.elementAt(gene+nExprPrefix);
			int found = tokens.length - nGenePrefix;
			if (found != nExpr) {
				setHadProblem(true);
				String err = "Wrong number of fields for gene " + tokens[0] + 
				" row " + (gene + nExprPrefix)+
				" Expected " + nExpr + ", found " + found;
				println(err);
				err = "Line contains:";
				err += " " + tokens[0];
				for (int i = 1; i < tokens.length; i++) {
					err += ", " + tokens[i];
				}
				println(err);
				if (found > nExpr) {
					println("ignoring extra values");
					found = nExpr;
				} else if (found < nExpr) {
					println("treating missing values as No Data..");
					for (int i = found; i < nExpr; i++) {
						exprData[gene*nExpr + i] = DataModel.NODATA;
					}
				}
			}
			for (int expr = 0; expr < found; expr++) {
				try {
					exprData[gene*nExpr + expr] = makeDouble(tokens[expr+nGenePrefix]);
				} catch (Exception e) {
					setHadProblem(true);
					println(e.getMessage());
					println("Treating value as not found for gene " + gene + " experiment " + expr);
					exprData[gene * nExpr + expr] = DataModel.NODATA;
				}
			}
		}
		targetModel.setExprData(exprData);

	}
	protected double makeDouble(String s) throws NumberFormatException {
		if (s == null) {
			return DataModel.NODATA;
		} else {
			try {
				Double tmp = new Double(s);
				double retval = tmp.doubleValue();
				// need to check, since RectData does this.
				if (Double.isNaN(retval)) {
					return DataModel.NODATA;
				}
				return retval; 
			} catch (Exception e) {
				setHadProblem(true);
				println("assigning nodata to badly formatted num'" + s +"'");
				return DataModel.NODATA;
			}
		}
	}
	protected int makeInteger(String s) {
		if (s == null) {
			setHadProblem(true);
			println("returning -1 for badly formatted int '" + s +"'");
			return -1;
		} else {
			try {
				Integer tmp = new Integer(s);
				int retval = tmp.intValue();
				return retval; 
			} catch (Exception e) {
				setHadProblem(true);
				println("returning -1 for badly formatted int '" + s +"'");
				return -1;
			}
		}
	}	
	
		 protected void println(String k) {
			 final String s = k;
//			  LogPanel.println(s);
//			  if (progressMonitor != null) progressMonitor.setNote(k);
			  Runnable update = new Runnable() {
				 @Override
				public void run() { loadProgress.println(s); }
			 };
			 SwingUtilities.invokeLater(update);
		 }

	public static void main(String [] argv) {
		final ClusterModel model = new ClusterModel();
		final JFrame frame = new JFrame("LBL Test ClusterModelLoader");
		final ClusterFileSet fileSet = new ClusterFileSet(argv[0], "");
		JButton button = new JButton("load " + argv[0]);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.setSource(fileSet);
				ClusterLoader loader = new ClusterLoader(model, frame);
				try {
					loader.loadInto();
				} catch (LoadException ex) {
					System.out.println(ex);
					ex.printStackTrace();
				}
			}
		});
		frame.getContentPane().add(new JLabel("LBL Test TVModelLoader"));
		frame.getContentPane().add(button);

		frame.pack();
		frame.setVisible(true);
		frame.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				System.exit(0);
			}
		});
	}
	protected int nGene;
	/**
	 * these internal variables are used to keep track of the 
	 * state of the tvmodel as it is being loaded.
	 */ 
	protected int nExpr;
	// cols to skip over before arrays begin...
	protected int nGenePrefix;
	// how many rows of annotation?
	protected int nExprPrefix;

	@Override
	public boolean getCanceled() {
		return loadProgress.getCanceled();
	}
	
}

