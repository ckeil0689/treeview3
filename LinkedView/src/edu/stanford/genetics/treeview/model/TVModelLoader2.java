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
package edu.stanford.genetics.treeview.model;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.*;
import java.net.URL;

import javax.swing.*;

import edu.stanford.genetics.treeview.*;
import edu.stanford.genetics.treeview.SwingWorker;

public class TVModelLoader2 implements ProgressTrackable {
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
	protected Frame parent;
	/** model to load into */
	protected TVModel targetModel;

	private javax.swing.Timer loadTimer;

	protected LoadProgress2I loadProgress = new DummyLoadProgress();
	
	// the following is for the communication between the timer thread and the worker thread.
	
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
	public void setLength(final int length) {
		
		SwingUtilities.invokeLater(new Runnable() {
		
			@Override
			public void run() {
				
				loadProgress.setLength(length);
			}
		});
	}
	
	/** Getter for length */
	@Override
	public int getLength() {
		return loadProgress.getLength();
	}
	
	/** Setter for value */
	@Override
	public void setValue(final int value) {
		
		SwingUtilities.invokeLater(new Runnable() {
						
			@Override
			public void run() {
				
				loadProgress.setValue(value);
			}
		});
	}
	
	/** Getter for value */
	@Override
	public int getValue() {
		return loadProgress.getValue();
	}
	
	@Override
	public void incrValue(final int i) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				
				loadProgress.incrValue(i);
			}
		});
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
	public void setPhaseValue(final int phaseValue) {
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				
				loadProgress.setPhaseValue(phaseValue);
			}
		});
	}
	
	/** Getter for phaseValue */
	public int getPhaseValue() {
		return loadProgress.getPhaseValue();
	}
	
	/** Setter for phaseLength */
	public void setPhaseLength(final int phaseLength) {
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
			
				if (loadProgress != null)
					loadProgress.setPhaseLength(phaseLength);
				}
		});
	}
	
	/** Getter for phaseLength */
	public int getPhaseLength() {
		return loadProgress.getPhaseLength();
	}
	
	public void setPhaseText(final String phaseText) {
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				
				loadProgress.setPhaseText(phaseText);
			}
		});
	}
	
	/** Getter for phaseText */
	public String getPhaseText() {
		return loadProgress.getPhaseText();
	}
	
	protected FlatFileParser2 parser = new FlatFileParser2();
	
	
	public TVModelLoader2(TVModel targetModel) {
		
		this(targetModel, targetModel.getFrame());
	}
	/**
	 * @param strings
	 */

	private String [] phases = new String [] {"Loading Cdt",
			"Parsing Cdt", "Loading ATR", "Parsing ATR", "Loading GTR", 
			"Parsing GTR", "Loading Document Config", "Finished"};
	
	public TVModelLoader2(TVModel targetModel, Frame parent) {
		
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
		loadProgress = new LoadProgress2(targetModel.getFileSet().getRoot(), 
				parent);
		loadProgress.setPhases(phases);
		final SwingWorker worker = new SwingWorker() {
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
			FileSet fileSet = targetModel.getFileSet();
			setPhaseLength(phases.length);
			setPhase(0);
			println("loading " + fileSet.getCdt() + " ... ");
			try {
				parser.setParseQuotedStrings(fileSet.getParseQuotedStrings());
				parser.setResource(fileSet.getCdt());
				parser.setProgressTrackable(this);
				RectData tempTable = parser.loadIntoTable();
				
				if (loadProgress.getCanceled()) {
					return;
				}
				setPhase(1);
				parseCDT(tempTable);
				
			} catch (LoadException e) {
				throw e;
				
			} catch (Exception e) {
				// this should never happen!
				LogBuffer.println("TVModel.ResourceLoader.run() : " +
						"while parsing cdt got error " + e.getMessage());
				e.printStackTrace();
				throw new LoadException("Error Parsing CDT: " + e, 
						LoadException.CDTPARSE);
			}
			
			if (loadProgress.getCanceled()) {
				return;
			}
			
			setPhase(2);
			if (targetModel.getArrayHeaderInfo().getIndex("AID") != -1) {
				println("parsing atr");
				try {
					parser.setResource(fileSet.getAtr());
					parser.setProgressTrackable(this);
					RectData tempTable = parser.loadIntoTable();
					setPhase(3);
					parseATR(tempTable);
					targetModel.hashAIDs();
					targetModel.hashATRs();
					targetModel.aidFound(true);
				} catch (Exception e) {
					println("error parsing ATR: " + e.getMessage());
					e.printStackTrace();
					println("ignoring array tree.");
					setHadProblem(true);
					targetModel.aidFound(false);
				}
			} else {
				targetModel.aidFound(false);
			}
			
			if (loadProgress.getCanceled()) return;
			setPhase(4);
			if (targetModel.getGeneHeaderInfo().getIndex("GID") != -1) {
				println("parsing gtr");
				try {
					parser.setResource(fileSet.getGtr());
					parser.setProgressTrackable(this);
					RectData tempTable = parser.loadIntoTable();
					if (loadProgress.getCanceled()) return;
					setPhase(5);
					parseGTR(tempTable);
					targetModel.hashGIDs();
					targetModel.hashGTRs();
					targetModel.gidFound(true);
				} catch (Exception e) {
					e.printStackTrace();
					println("error parsing GTR: " + e.getMessage());
					println("ignoring gene tree.");
					setHadProblem(true);
					targetModel.gidFound(false);
				}
			} else {
				targetModel.gidFound(false);
			}
			if (loadProgress.getCanceled()) return;
			setPhase(6);

			try {
				println("parsing jtv config file");
				String xmlFile = targetModel.getFileSet().getJtv();
				
				XmlConfig documentConfig;
				if (xmlFile.startsWith("http:")) {
					documentConfig = new XmlConfig(new URL(xmlFile), "DocumentConfig");
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
			setPhase(7);
			if (getException() == null) {
				/*	
				if (!fileLoader.getCompleted()) {
					throw new LoadException("Parse not Completed", LoadException.INTPARSE);
				}
				//System.out.println("f had no exceptoin set");
				*/
			} else {
				throw getException();
			}
			targetModel.setLoaded(true);
			//	ActionEvent(this, 0, "none",0);
		} catch (java.lang.OutOfMemoryError ex) {
							JPanel temp = new JPanel();
							temp. add(new JLabel("Out of memory, allocate more RAM"));
							temp. add(new JLabel("see Chapter 3 of Help->Documentation... for Out of Memory"));
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
	protected void  parseCDT(RectData tempVector) throws LoadException {
		// find eweightLine, ngene, nexpr
		findCdtDimensions(tempVector);
		loadArrayAnnotation(tempVector);
		loadGeneAnnotation(tempVector);
		loadCdtData(tempVector);
	}
	
	/**
	 * finds ngene, nexpr, nArrayPrefix, nGenePrefix
	 * 
	 * Uses new style code that takes advantage of RectData structure and is fast.
	 */
	protected void findCdtDimensions(RectData tempVector)  {
		println("Finding Cdt Dimensions");		
		
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
		nExpr = rectCol - nGenePrefix;
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
		
		nGene = tempVector.size() - nExprPrefix;
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
	protected void loadArrayAnnotation(RectData tempVector) {
		println("loading Array Annotations");
		String [] arrayPrefix = new String[nExprPrefix];
		String [][] aHeaders = new String [nExpr][nExprPrefix];
		
		for (int i = 0; i < nExprPrefix; i++) {
			String [] tokens = (String []) tempVector.elementAt(i);
			arrayPrefix[i] = tokens[0];
			for (int j = 0; j < nExpr; j++)
				aHeaders[j][i] = tokens[j + nGenePrefix];
		}
		targetModel.setArrayPrefix(arrayPrefix);
		targetModel.setArrayHeaders(aHeaders);
	}
	
	/**
	 * Loads gene annotation from RectData into targetModel.
	 * 
	 * @param tempVector RectData contain annotation info
	 */
	protected void loadGeneAnnotation(RectData tempVector) {
		
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
//			String [] tokens = (String []) tempVector.elementAt(
			//i + nArrayPrefix);
			for (int j = 0; j < nGenePrefix; j++) {

//				gHeaders[i][j] = tokens[j];
				gHeaders[i][j] = tempVector.getString(i + nExprPrefix - 1, j);
			}
		}
		targetModel.setGenePrefix(genePrefix);
		targetModel.setGeneHeaders(gHeaders);
	}
	
	protected void loadCdtData(RectData tempVector) {
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
	
	private void parseATR(RectData tempVector) throws LoadException {
		String [] firstRow = (String []) tempVector.firstElement();
		if ( // decide if this is not an extended file..
			(firstRow.length == 4)// is the length classic?
		&& (firstRow[0].equalsIgnoreCase("NODEID") == false) // does it begin with a non-canonical upper left?
		) { // okay, need to assign headers...
			targetModel.setAtrPrefix(new String [] {"NODEID", "LEFT", "RIGHT", "CORRELATION"});
			String [][] atrHeaders = new String[tempVector.size()][];
			for (int i =0; i < atrHeaders.length; i++) {
				atrHeaders[i] = (String []) tempVector.elementAt(i);
			}
			targetModel.setAtrHeaders(atrHeaders);
		} else {// first row of tempVector is actual header names...
			targetModel.setAtrPrefix(firstRow);

			String [][] atrHeaders = new String[tempVector.size()-1][];
			for (int i =0; i < atrHeaders.length; i++) {
				atrHeaders[i] = (String []) tempVector.elementAt(i+1);
			}
			targetModel.setAtrHeaders(atrHeaders);
		}
	}
	
	private void parseGTR(RectData tempVector) throws LoadException {
		String [] firstRow = (String []) tempVector.firstElement();
		if ( // decide if this is not an extended file..
			(firstRow.length == 4)// is the length classic?
		&& (firstRow[0].equalsIgnoreCase("NODEID") == false) // does it begin with a non-canonical upper left?
		) { // okay, need to assign headers...
			targetModel.setGtrPrefix(new String [] {"NODEID", "LEFT", "RIGHT", "CORRELATION"});
			String [][] gtrHeaders = new String[tempVector.size()][];
			for (int i =0; i < gtrHeaders.length; i++) {
				gtrHeaders[i] = (String []) tempVector.elementAt(i);
			}
			targetModel.setGtrHeaders(gtrHeaders);
		} else {// first row of tempVector is actual header names...
			targetModel.setGtrPrefix(firstRow);

			String [][] gtrHeaders = new String[tempVector.size()-1][];
			for (int i =0; i < gtrHeaders.length; i++) {
				gtrHeaders[i] = (String []) tempVector.elementAt(i+1);
			}
			targetModel.setGtrHeaders(gtrHeaders);
		}
		/*/ test out the GtrHeaders...
		HeaderInfo gtrHeaders = targetModel.getGtrHeaderInfo();
		for (int i = 0; i < gtrHeaders.getNumHeaders(); i++) {
			String nodeId = gtrHeaders.getHeader(i, "NODEID");
			String leftId = gtrHeaders.getHeader(i, "LEFT");
			String rightId = gtrHeaders.getHeader(i, "RIGHT");
			String corr = gtrHeaders.getHeader(i, "CORRELATION");
			System.out.println("node " + nodeId + " has left ID " + leftId + ", right id " + rightId + "corre " + corr);
		}
		*/
	}
	
		 /*
	private int parseNodes(Vector nvec, Vector source, int ptype) {
		setLength(source.size());
		int found = 0;
		for (int row = 0; row < source.size(); row++) {
			if (getFinished()) break;
			setValue(row);
			String [] tempString = new String[3];
			String [] tokens = (String []) source.elementAt(row);
			for (int i = 0; i < 3; i++) {
				tempString[i] = tokens[i];
			}
			nvec.addElement(tempString);
			nvec.addElement(new Double(tokens[3]));
			found++;
		}
		return found;
	}
	
		 private void setValue(int j) {
			 final int i = j;
			 Runnable update = new Runnable() {
				 public void run() { loadProgress.setValue(i); }
			 };
			 SwingUtilities.invokeLater(update);
		 }
		 */
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
		final TVModel model = new TVModel();
		final JFrame frame = new JFrame("LBL Test TVModelLoader");
		final FileSet fileSet = new FileSet(argv[0], "");
		JButton button = new JButton("load " + argv[0]);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.setSource(fileSet);
				TVModelLoader2 loader = new TVModelLoader2(model, frame);
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

