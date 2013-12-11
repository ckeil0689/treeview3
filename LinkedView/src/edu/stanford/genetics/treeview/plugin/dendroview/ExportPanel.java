/* BEGIN_HEADER                                              Java TreeView
*
* $Author: alokito $
* $RCSfile: ExportPanel.java,v $
* $Revision: 1.2 $
* $Date: 2010-05-02 13:39:00 $
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
package edu.stanford.genetics.treeview.plugin.dendroview;

import edu.stanford.genetics.treeview.*;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.event.*;
/**
* This class is a superclass which implements a GUI for selection of options relating to output. 
* It makes most of the relevant variables accessible to subclasses through protected methods.
*/
public abstract class ExportPanel extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;

	private ConfigNode root;
	
	// external links
	private HeaderInfo arrayHeaderInfo; // allows access to array headers.
	private HeaderInfo geneHeaderInfo;  // allows access to gene headers.
	private FileSet sourceSet;          // FileSet from which current data was constructed.
	private TreeSelectionI geneSelection;
	private TreeSelectionI arraySelection;
	private InvertedTreeDrawer arrayTreeDrawer;
	private LeftTreeDrawer geneTreeDrawer;
	private ArrayDrawer arrayDrawer;
	private MapContainer geneMap;
	private MapContainer arrayMap;
	private Double explicitGtrWidth = null;
	private Double explicitAtrHeight = null;
	
	// accessors
	protected HeaderInfo getArrayHeaderInfo() {
		return arrayHeaderInfo;
	}
	protected HeaderInfo getGeneHeaderInfo() {
		return geneHeaderInfo;
	}
	protected TreeSelectionI getGeneSelection() {
		return geneSelection;
	}
	protected TreeSelectionI getArraySelection() {
		return arraySelection;
	}
	protected ArrayDrawer getArrayDrawer() {
		return arrayDrawer;
	}
	
	public FileSet getSourceSet() {
		return sourceSet;
	}
	public void setSourceSet( FileSet fs) {
		sourceSet = fs;
		if (filePanel != null) {
			filePanel.setFilePath(getInitialFilePath());
		}
	}
	// NOTE: border pixels appear on all sides.
	int borderPixels = 0;
	/** Setter for borderPixels */
	public void setBorderPixels(int border) {
		this.borderPixels = border;
	}
	/** Getter for borderPixels */
	public int getBorderPixels() {
		return borderPixels;
	}
	private static int textSpacing = 2; //pixels between boxes and text
	
	/**
	* for communication with subclass... (in this case PostscriptExport)
	*/
	protected boolean hasBbox() {
		return true;
	}
	
	/**
	* for communication with subclass... (in this case CharExport)
	* NOTE: better to have local, to avoid obligatory subclassing.
	*/
	boolean hasChar;
	protected boolean hasChar() {
		return hasChar;
	}
	
	// components
	private FilePanel filePanel;
	private InclusionPanel inclusionPanel;
	private HeaderSelectionPanel headerSelectionPanel;
	private PreviewPanel previewPanel;
	
	// accessors for configuration information
	/**
	* returns the font for gene annotation information
	*/
	private Font geneFont = new Font("Courier", 0, 12);
	protected Font getGeneFont() {
		return geneFont;
	}
	public void setGeneFont(Font f) {
		if (f != null) {
			geneFont =f;
		}
	}
	private Font arrayFont = new Font("Courier", 0, 12);
	protected Font getArrayFont() {
		return arrayFont;
	}
	public void setArrayFont(Font f) {
		if (f != null) {
			arrayFont =f;
		}
	}
	
	/**
	* True if an explict bounding box should be included in the output.
	* Subclasses are to use this when creating output. The returned value reflects
	* what the user has selected in the GUI.
	* This is only meaningful for postscript.
	*/
	protected boolean includeBbox() {
		return inclusionPanel.useBbox();
	}
	
	/** 
	* This method returns the minimum correlation for the gene nodes which will be drawn.
	*/
	protected double getMinGeneCorr() {
		if (drawSelected()) {
			if (geneTreeDrawer == null) LogBuffer.println("ExportPanel.getMinGeneCorr: geneTreeDrawer null");
			TreeSelectionI selection = getGeneSelection();
			if (selection == null) LogBuffer.println("ExportPanel.getMinGeneCorr: selection null");
			String selectedId = selection.getSelectedNode();
			if (selectedId == null) LogBuffer.println("ExportPanel.getMinGeneCorr: selectedId null");
			TreeDrawerNode selectedNode = geneTreeDrawer.getNodeById(selectedId);
			if (selectedNode == null) LogBuffer.println("ExportPanel.getMinGeneCorr: selectedNode null , id " + selectedId);
			return  selectedNode.getCorr();
		} else {
			return geneTreeDrawer.getCorrMin();
		}
	}
	/** 
	* This method returns the minimum correlation for the gene nodes which will be drawn.
	*/
	protected double getMinArrayCorr() {
		if (drawSelected()) {
			return  arrayTreeDrawer.getNodeById(getArraySelection().getSelectedNode()).getCorr();
		} else {
			return arrayTreeDrawer.getCorrMin();
		}
	}
	
	/**
	* This method is for drawing the actual data. 
	*
	* It returns the offset of the first pixel of the block corresponding to the geneIndex
	* where the first block (index 0) always has an offset of zero.
	*/
	protected int getYmapPixel(double geneIndex) {
		double dp = geneMap.getPixel(geneIndex) - geneMap.getPixel(0);
		double ret = (int) (dp * getYscale() / geneMap.getScale());
		return (int) ret;
	}
	
	/**
	* This method is for drawing the actual data. 
	*
	* It returns the offset of the first pixel of the block corresponding to the arrayIndex
	* where the first block (index 0) always has an offset of zero.
	*/
	protected int getXmapPixel(double geneIndex) {
		double dp = arrayMap.getPixel(geneIndex) - arrayMap.getPixel(0);
		int ret = (int) (dp * getXscale() / arrayMap.getScale());
		return ret;
	}
	protected boolean geneAnnoInside() {
		return headerSelectionPanel.geneAnnoInside();
	}
	protected boolean getArrayAnnoInside() {
		return headerSelectionPanel.getArrayAnnoInside();
	}
	protected void setArrayAnnoInside(boolean newval) {
		headerSelectionPanel.setArrayAnnoInside(newval);
	}
	
	protected String getGeneAnno(int i) {
		return headerSelectionPanel.getGeneAnno(i);
	}
	protected String getArrayAnno(int i) {
		return headerSelectionPanel.getArrayAnno(i);
	}
	private Color getFgColor(HeaderInfo headerInfo, int index) {
		int colorIndex       = headerInfo.getIndex("FGCOLOR");
		if (colorIndex > 0) {
			String[] headers  = headerInfo.getHeader(index);
			return TreeColorer.getColor(headers[colorIndex]);
		}
		return null;
	}
	private Color getBgColor(HeaderInfo headerInfo, int index) {
		int colorIndex       = headerInfo.getIndex("BGCOLOR");
		if (colorIndex > 0) {
			String[] headers  = headerInfo.getHeader(index);
			return TreeColorer.getColor(headers[colorIndex]);
		}
		return null;
	}
	
	protected Color getGeneFgColor(int i) {
		return getFgColor(geneHeaderInfo, i);
	}
	protected Color getArrayFgColor(int i) {
		return getFgColor(arrayHeaderInfo, i);
	}
	protected Color getGeneBgColor(int i) {
		return getBgColor(geneHeaderInfo, i);
	}
	protected Color getArrayBgColor(int i) {
		return getBgColor(arrayHeaderInfo, i);
	}
	// gene node to actually draw
	protected TreeDrawerNode getGeneNode() {
		if (inclusionPanel.drawSelected()) {
			return geneTreeDrawer.getNodeById(geneSelection.getSelectedNode());
		} else {
			return getGeneRootNode();
		}
	}
	
	// array node to actually draw
	protected TreeDrawerNode getArrayNode() {
		if (inclusionPanel.drawSelected()) {
			return arrayTreeDrawer.getNodeById(arraySelection.getSelectedNode()) ;
		} else {
			return getArrayRootNode();
		}
	}
	
	protected File getFile() {
		return filePanel.getFile();
	}
	public String getFilePath() {
		return filePanel.getFilePath();
	}
	public void setFilePath(String newFile) {
		filePanel.setFilePath(newFile);
	}
	protected TreeDrawerNode getGeneRootNode() {
		if (geneTreeDrawer == null) return null;
		return geneTreeDrawer.getRootNode();
	}
	protected TreeDrawerNode getArrayRootNode() {
		if (arrayTreeDrawer == null) return null;
		return arrayTreeDrawer.getRootNode();
	}
	protected String getInitialExtension() {
		return ".ps";
	}
	protected String getInitialFilePath() {
		String defaultPath = null;
		if (sourceSet == null) {
			defaultPath = System.getProperty("user.home");
		} else {
			defaultPath = sourceSet.getDir() + sourceSet.getRoot() + getInitialExtension();
		}
		if (root == null) {
			return defaultPath;
		} else {
			return root.getAttribute("file", defaultPath);
		}
	}
	
	
	/**
	* the scale of the passed in gene map and array map define the initial size.
	* The export panel will not actually modify the map settings for now. 
	*
	* To Developers- if you want to simpify the code by changing the scale settings in the maps, make copies of them first. This might involve implementing copyStateFrom functions in the MapContainer class.
	*
	* hasChar - indicates whether or not there are characters in the data area. Used when we have a CharArrayDrawer.
	*/
	public ExportPanel(HeaderInfo arrayHeaderInfo, HeaderInfo geneHeaderInfo, 
	TreeSelectionI geneSelection, TreeSelectionI arraySelection, InvertedTreeDrawer arrayTreeDrawer, LeftTreeDrawer geneTreeDrawer, ArrayDrawer arrayDrawer, MapContainer arrayMap,MapContainer geneMap, boolean hasChar) {
		this.arrayHeaderInfo = arrayHeaderInfo;
		this.geneHeaderInfo = geneHeaderInfo;
		this.geneSelection = geneSelection;
		this.arraySelection = arraySelection;
		this.arrayTreeDrawer = arrayTreeDrawer;
		this.geneTreeDrawer = geneTreeDrawer;
		this.arrayDrawer = arrayDrawer;
		this.arrayMap = arrayMap;
		this.geneMap = geneMap;
		this.hasChar = hasChar;
		setupWidgets();
		inclusionPanel.synchSelected();
		inclusionPanel.synchEnabled();
	}
	
	public void setIncludedGeneHeaders(int []newSelected) {
		headerSelectionPanel.geneList.setSelectedIndices(newSelected);
		headerSelectionPanel.setupSelected();
	}
	public void setIncludedArrayHeaders(int []newSelected) {
		headerSelectionPanel.arrayList.setSelectedIndices(newSelected);
		headerSelectionPanel.setupSelected();
	}
	
	public static final void main(String [] argv) {
			HeaderInfo aH = new DummyHeaderInfo();
			HeaderInfo gH = new DummyHeaderInfo();
			MapContainer aMap = new MapContainer();
			aMap.setMap("Fixed");
			aMap.setScale(10);
			aMap.setIndexRange(0,aH.getNumHeaders());
			MapContainer gMap = new MapContainer();
			gMap.setMap("Fixed");
			gMap.setScale(12);
			gMap.setIndexRange(0,gH.getNumHeaders());
			TreeSelectionI gsel = new TreeSelection(gH.getNumHeaders());
			TreeSelectionI asel = new TreeSelection(aH.getNumHeaders());
			ExportPanel testExportPanel = new TestExportPanel(aH, gH, gsel, asel, aMap, gMap);
			JFrame test = new JFrame("Test Export Panel");
			test.getContentPane().add(testExportPanel);
			test.pack();
			test.setVisible(true);
	}
	public void bindConfig(ConfigNode configNode)
	{
		root = configNode;
	}
	public ConfigNode createSubNode()
	{
		return root.create("File");
	}
	
	private void setupWidgets() {
		Box upperPanel; // holds major widget panels
		upperPanel = new Box(BoxLayout.X_AXIS);
		headerSelectionPanel = new HeaderSelectionPanel();
		upperPanel.add(headerSelectionPanel);
		inclusionPanel = new InclusionPanel();
		upperPanel.add(inclusionPanel);
		previewPanel = new PreviewPanel();
		upperPanel.add(previewPanel);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(upperPanel);
		filePanel = new FilePanel(getInitialFilePath());
		add(filePanel);
		// can add more panels below in subclass.
	}	
	
	//drawing specific convenience methods...
	protected boolean includeAtr() {
		return inclusionPanel.includeAtr();
	}
	protected void includeAtr(boolean flag) {
		inclusionPanel.includeAtr(flag);
	}
	protected boolean includeGtr() {
		return inclusionPanel.includeGtr();
	}
	protected void includeGtr(boolean flag) {
		inclusionPanel.includeGtr(flag);
	}
	protected boolean includeData() {
		return inclusionPanel.includeData();
	}
	private boolean includeChar() {
		return inclusionPanel.includeChar();
	}
	protected void includeData(boolean flag) {
		inclusionPanel.includeData(flag);
	}
	protected boolean drawSelected() {
		return inclusionPanel.drawSelected();
	}
	protected boolean includeGeneMap() {
		return (includeGtr() || includeData() || (numGeneHeaders() > 0));
	}
	protected boolean includeArrayMap() {
		return (includeAtr() || includeData() || (numArrayHeaders() > 0));
	}
	
	public double getXmapWidth() {
		// HACK, doesn't account for discontinuous selection
		return (int) ((arrayMap.getPixel(maxArray() + 1) - arrayMap.getPixel(minArray())) * getXscale() / arrayMap.getScale());
	}
	public double getGtrWidth() {
		if (explicitGtrWidth == null)
			return 150 * getXscale() / arrayMap.getScale();
		else 
			return explicitGtrWidth;
	}
	public double getXscale() {
		return inclusionPanel.getXscale();
	}
	public void setXscale(double newval) {
		inclusionPanel.setXscale(newval);
	}
	
	public double getYmapHeight() {
		// HACK, doesn't account for discontinuous selection
		double ret = (geneMap.getPixel(maxGene() + 1) - geneMap.getPixel(minGene())) * getYscale() / geneMap.getScale();
		return ret;
	}
	
	public double getAtrHeight() {
		if (explicitAtrHeight == null)
			return 150 * getYscale() / geneMap.getScale();
		else
			return explicitAtrHeight;
	}
	public double getYscale() {
		return inclusionPanel.getYscale();
	}
	public void setYscale(double newval) {
		inclusionPanel.setYscale(newval);
	}
	public int getBboxWidth() {
		return inclusionPanel.getBboxWidth();
	}
	public int getBboxHeight() {
		return inclusionPanel.getBboxHeight();
	}
	
	public int minGene() {
		if (inclusionPanel.drawSelected()) {
			return geneSelection.getMinIndex();
		} else {
			return 0;
		}
	}
	
	public int minArray() {
		if (inclusionPanel.drawSelected()) {
			return arraySelection.getMinIndex();
		} else {
			return 0;
		}
	}
	
	public int maxGene() {
		if (inclusionPanel.drawSelected()) {
			return geneSelection.getMaxIndex();
		} else {
			return geneHeaderInfo.getNumHeaders() - 1;
		}
	}
	
	public int maxArray() {
		if (inclusionPanel.drawSelected()) {
			return arraySelection.getMaxIndex();
		} else {
			return arrayHeaderInfo.getNumHeaders() - 1;
		}
	}
	
	public int estimateHeight() {
		int height = 2*getBorderPixels();
		// do we need to include the height of the map?
		if (includeGeneMap()) {
			height += (int) getYmapHeight();
		} else {
		}
		// additional space for gene tree...
		if (includeAtr()) {
			height += (int) getAtrHeight();
		}
		height += getArrayAnnoLength();
		return height;
	}
	public int estimateWidth() {
		int width = 2*getBorderPixels();
		// do we need to include the width of the map?
		if (includeArrayMap()) {
			width += (int) getXmapWidth();
		} else {
		}
		// additional space for gene tree...
		if (includeGtr()) {
			width += (int) getGtrWidth();
		}
		width += getGeneAnnoLength(); 
		return width;
	}
	protected int getGeneAnnoLength() {
		// deal with text length...
		if ((inclusionPanel == null) || (inclusionPanel.useBbox() == false)) { 
			// no bounding box, have to wing it...
			Integer rawMaxLength = headerSelectionPanel.geneMaxLength();
			if (rawMaxLength != null) {
				return rawMaxLength + textSpacing;
			} else {
				// no gene anno, return 0
				return 0;
			}
		} else {
			return getBboxWidth();
		}
	}
	protected int getArrayAnnoLength() {
		// deal with text length...
		if ((inclusionPanel == null) ||(inclusionPanel.useBbox() == false)) { 
			// no bounding box, have to wing it...
			return headerSelectionPanel.arrayMaxLength()+ textSpacing;
		} else {
			return getBboxHeight();
		}
	}
	
	public int numArrayHeaders() {
		return headerSelectionPanel.numArrayHeaders();
	}
	public int numGeneHeaders() {
		return headerSelectionPanel.numGeneHeaders();
	}
	
	public void deselectHeaders() {
		headerSelectionPanel.deselectHeaders();
	}
	
	class PreviewPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		JCheckBox drawPreview;
		DrawingPanel drawingPanel;
		WaitScreen waitingPanel;
		public void updatePreview() {
			if ((drawPreview == null) || drawPreview.isSelected()) {
				remove(waitingPanel);
				add(drawingPanel, BorderLayout.CENTER);
			} else {
				remove(drawingPanel);
				add(waitingPanel, BorderLayout.CENTER);
			}
			repaint();
		}
		PreviewPanel() {
			setLayout(new BorderLayout());
			add(new JLabel("Preview"), BorderLayout.NORTH);
			drawingPanel = new DrawingPanel();
			waitingPanel = new WaitScreen(new String [] {"Check Box to", "Display Preview"});
			add(waitingPanel, BorderLayout.CENTER);
			drawPreview = new JCheckBox("Draw Preview");
			drawPreview.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updatePreview();
				}
			});
			drawPreview.setSelected(false);
			add(drawPreview, BorderLayout.SOUTH);
		}
		class DrawingPanel extends JPanel {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g) {
				Dimension size = getSize();
				int width = estimateWidth();
				int height = estimateHeight();
				if ((width == 0) || (height == 0)) {
					return;
				}
				// if the width * size.height is greater than the the height *size.width
				// then if we make width = size.width, the height will be less than size.height.
				if (width *size.height > height * size.width) {
					height = (height * size.width) / width;
					width = size.width;
				} else { // otherwise, the converse is true.
					width = (width * size.height) / height;
					height = size.height;
				}
				if ((drawPreview == null) || drawPreview.isSelected()) {
					double scale = (double)width / estimateWidth();
					// 5 views to worry about... first, calculate datamatrix's origin...
					
					int dataX = (int) (scale * getDataX());
					int dataY = (int) (scale * getDataY());
					
					drawGtr(g, 0, dataY, scale);
					
					if (includeAtr()) {
						if (headerSelectionPanel.getArrayAnnoInside()) {
							drawAtr(g,dataX, 0, scale);
							drawArrayAnnoBox(g,dataX, (int) (scale * getAtrHeight()), scale);
						} else {
							drawArrayAnnoBox(g,dataX, 0, scale);
							drawAtr(g,dataX, dataY - (int) (scale * getAtrHeight()), scale);
						}
					} else {
						drawArrayAnnoBox(g,dataX, 0, scale);
					}
					drawData(g,dataX, dataY, scale);
					if (includeArrayMap()) {
						drawGeneAnnoBox(g,dataX + (int) (getXmapWidth() * scale), dataY, scale);
					} else {
						drawGeneAnnoBox(g,dataX , dataY, scale);
					}
					g.setColor(Color.blue);
					g.drawOval(dataX-2, dataY -2, 5, 5);
					g.setColor(Color.black);
				} else {
					//			g.setColor(Color.red);
					//			g.drawOval(0,0,width,height);
					int [] xPoints = new int[4];
					int [] yPoints = new int[4];
					xPoints[0] = 0; xPoints[1] = 5; xPoints[2] = width; xPoints[3] = width - 5;
					yPoints[0] = 5; yPoints[1] = 0; yPoints[2] = height -5; yPoints[3] = height;
					
					g.fillPolygon(xPoints, yPoints, 4);
					yPoints[0] = height-5; yPoints[1] = height; yPoints[2] = 5; yPoints[3] = 0;
					g.fillPolygon(xPoints, yPoints, 4);
				}
			}
		}
	}
	
	/**
	* The following method gets the x coordiate of the data matrix, according to current settings.
	*/
	protected int getDataX() {
		int dataX = getBorderPixels();
		if (includeGtr()) dataX += getGtrWidth();
		return dataX;
	}
	
	/**
	* The following method gets the y coordiate of the data matrix, according to current settings.
	*/
	protected int getDataY() {
		int dataY = getBorderPixels();
		if (includeAtr()) dataY += getAtrHeight();
		dataY += getArrayAnnoLength();
		return dataY;
	}
	
	/**
	* does the dirty work by calling methods in the superclass.
	*/
	public void drawAll(Graphics g, double scale) {
		int width = estimateWidth();
		int height = estimateHeight();
		if ((width == 0) || (height == 0)) {
			return;
		}
		// 5 views to worry about... first, calculate datamatrix's origin...
		
		int dataX = (int) (scale * getDataX());
		int dataY = (int) (scale * getDataY());
		int scaleP = (int) (scale * getBorderPixels());
		drawGtr(g, scaleP, dataY, scale);
		
		if (includeAtr()) {
			if (getArrayAnnoInside()) {
				drawAtr(g,dataX, scaleP, scale);
				drawArrayAnno(g,dataX, scaleP + (int) (scale * getAtrHeight()), scale);
			} else {
				drawArrayAnno(g,dataX, scaleP, scale);
				drawAtr(g,dataX, dataY - (int) (scale * getAtrHeight()), scale);
			}
		} else {
			drawArrayAnno(g,dataX, scaleP, scale);
		}
		drawData(g,dataX, dataY, scale);
		if (includeArrayMap()) {
			drawGeneAnno(g,dataX + (int) (getXmapWidth() * scale), dataY, scale);
		} else {
			drawGeneAnno(g,dataX , dataY, scale);
		}
	}
	/**
	* draws a scaled Gene Tree at the suggested x,y location
	*/
	protected void drawGtr(Graphics g, int x, int y, double scale) {
		if (includeGtr() == false) return;
		int width = (int) (getGtrWidth() * scale);
		int height = (int) (getYmapHeight() * scale);
		if ((height == 0) || (width == 0)) return;
		
		// clear the pallette...
		g.setColor(Color.black);
		
		//	calculate Scaling
		Rectangle destRect = new Rectangle();
		destRect.setBounds(x,y, width, height);
		
		double minCorr = getMinGeneCorr();
		LinearTransformation xScaleEq = new LinearTransformation
		(minCorr, destRect.x,
		geneTreeDrawer.getCorrMax(), destRect.x + destRect.width);
		
		LinearTransformation yScaleEq = new LinearTransformation
		(minGene(), destRect.y,
		maxGene()+ 1, 
		destRect.y + destRect.height);
		
		// draw
		geneTreeDrawer.paintSubtree(g, 
		xScaleEq, yScaleEq,
		destRect, getGeneNode(), false);
		
	}
	/**
	* draws a scaled Array Tree at the suggested x,y location
	*/
	protected void drawAtr(Graphics g, int x, int y, double scale) {
		if (includeAtr() == false) return;
		int width = (int) (getXmapWidth() * scale);
		int height = (int) (getAtrHeight() * scale);
		if ((height == 0) || (width == 0)) return;
		// clear the pallette...
		g.setColor(Color.black);
		
		//	calculate Scaling
		Rectangle destRect = new Rectangle();
		destRect.setBounds(x, y, width, height);
		LinearTransformation xScaleEq = new LinearTransformation
		(minArray(), destRect.x,
		maxArray()+1, destRect.x + destRect.width);
		double minCorr = arrayTreeDrawer.getCorrMin();
		if (drawSelected()) {
			minCorr = arrayTreeDrawer.getNodeById(getArraySelection().getSelectedNode()).getCorr();
		}
		LinearTransformation yScaleEq = new LinearTransformation
		(minCorr, destRect.y,
		arrayTreeDrawer.getCorrMax(), destRect.y + destRect.height);
		
		// draw
		arrayTreeDrawer.paintSubtree(g, 
		xScaleEq, yScaleEq,
		destRect, getArrayNode(), false);
	}
	
	/**
	* draws an appropriately sized box for each annotation string at the specific location
	*/
	protected void drawGeneAnnoBox(Graphics g, int x, int y, double 
	scale) {
		// HACK doesn't deal with discontinuous selection right.
		int width = (int) (getGeneAnnoLength() * scale);
		int height = (int) (getYmapHeight() * scale);
		g.setColor(Color.black);
		FontMetrics fontMetrics = getFontMetrics(getGeneFont());
		int geneHeight = (int) (fontMetrics.getAscent() * scale);
		int min = minGene();
		int max = maxGene();
		double spacing = (double) height/(max - min + 1);
		for (int i = min; i <= max;  i++) {
			/*
			int geneWidth = (int) (scale * headerSelectionPanel.getLength (headerSelectionPanel.getGeneAnno(i)));
			*/
			int geneWidth = width;
			g.fillRect(x,
			y + (int)((i - min) *spacing   + (spacing - geneHeight) /2),
			geneWidth,
			geneHeight);
		}
	}
	
	/**
	* draws an appropriately sized box for each annotation string at the specific location
	*/
	public void drawArrayAnnoBox(Graphics g, int x, int y, double scale) {
		// HACK doesn't deal with discontinuous selection right.
		int height = (int) (getArrayAnnoLength() * scale);
		int width = (int) (getXmapWidth() * scale);
		
		g.setColor(Color.black);
		FontMetrics fontMetrics = getFontMetrics(getArrayFont());
		int arrayWidth = (int) (fontMetrics.getAscent() * scale);
		int min = minArray();
		int max = maxArray();
		double spacing = (double) width/(max - min + 1);
		for (int i = min; i <= max;  i++) {
			//		int arrayHeight = (int) (scale * headerSelectionPanel.getLength(headerSelectionPanel.getArrayAnno(i)));
			int arrayHeight = height;
			int thisx = x+ (int)((i - min) *spacing   + (spacing - arrayWidth) /2);
			int thisy = y + height - arrayHeight;
			if (headerSelectionPanel.getArrayAnnoInside()) {
				thisy = y;
			}
			g.fillRect(thisx,
			thisy,
			arrayWidth,
			arrayHeight);
		}
	}
	/**
	* draws an annotation strings at the specific location
	*/
	protected void drawGeneAnno(Graphics g, int x, int y, double 
	scale) {
		// HACK doesn't deal with discontinuous selection right.
		int width = (int) (getGeneAnnoLength() * scale);
		int height = (int) (getYmapHeight() * scale);
		if ((height == 0) || (width == 0)) return;
		int min = minGene();
		int max = maxGene();
		double spacing = (double) height/(max - min + 1);
		
		MapContainer tempMap = new MapContainer("Fixed");
		tempMap.setScale(spacing);
		tempMap.setIndexRange(min,max);
		tempMap.setAvailablePixels(height + getBorderPixels());
		TextView anv = new TextView(geneHeaderInfo, null);
		anv.setMap(tempMap);
		anv.setHeaderSummary(headerSelectionPanel.getGeneSummary());
		Image buf = new BufferedImage(width + getBorderPixels(), height 
				+ getBorderPixels(), BufferedImage.TYPE_INT_ARGB); 
		System.out.println("setting font for genes to " + getGeneFont());
		anv.setFace(getGeneFont().getName());
		anv.setStyle(getGeneFont().getStyle());
		anv.setPoints(getGeneFont().getSize());
		anv.updateBuffer(buf);
		g.drawImage(buf,x+textSpacing, y, null);
		
		/*
		g.setColor(Color.black);
		g.setFont(getGeneFont());
		FontMetrics fontMetrics = getFontMetrics(g.getFont());
		int geneHeight = (int) (fontMetrics.getAscent() * scale);
		int inset = (int) (scale * getBorderPixels());
		for (int i = min; i <= max;  i++) {
			g.drawString(getGeneAnno(i), x + inset,
			y + (int)((i - min + 1.0) *spacing   - (spacing - geneHeight) /2));
		}
		*/
	}
	
	/**
	* draws array annotation strings at the specific location
	*/
	public void drawArrayAnno(Graphics real, int x, int y, double scale) {
		int height = (int) (getArrayAnnoLength() * scale);
		int width = (int) (getXmapWidth() * scale);
		if ((height == 0) || (width == 0)) return;
		int min = minArray();
		int max = maxArray();
		double spacing = (double) width/(max - min + 1);
		
		MapContainer tempMap = new MapContainer("Fixed");
		tempMap.setScale(spacing);
		tempMap.setIndexRange(min,max);
		tempMap.setAvailablePixels(width + getBorderPixels());
		ArrayNameView anv = new ArrayNameView(arrayHeaderInfo);
		anv.setFace(getArrayFont().getName());
		anv.setStyle(getArrayFont().getStyle());
		anv.setPoints(getArrayFont().getSize());
		anv.setHeaderSummary(headerSelectionPanel.getArraySummary());
		anv.setMap(tempMap);
		Image buf = new BufferedImage(width + getBorderPixels(), height + getBorderPixels(), BufferedImage.TYPE_INT_ARGB); 
		buf.getGraphics().setFont(getArrayFont());
		anv.updateBuffer(buf);
		real.drawImage(buf,x, y-getBorderPixels()-textSpacing, null);
	}
	
	/**
	* draws the data matrix
	*/
	public void drawData(Graphics g, int x, int y, double scale) {
		if (includeData() == false) return;
		int height = (int) (getYmapHeight() * scale);
		int width = (int) (getXmapWidth() * scale);
		
		Rectangle sourceRect = new Rectangle();
		sourceRect.setBounds(minArray(),minGene(),
		(maxArray() + 1 - minArray()), 
		(maxGene() + 1 - minGene()));
		Rectangle destRect = new Rectangle();
		
		// HACK does not deal with discontinuous selection...
		/* old version, kinda slow... 
		destRect.setBounds(x,y, width, height);
		arrayDrawer.paint(g, sourceRect ,destRect);
		*/
		destRect.setBounds(0,0, width, height);
		int [] pixels = new int [width * height];
		arrayDrawer.paint(pixels, sourceRect, destRect, width);
		MemoryImageSource source = new MemoryImageSource(width, height, pixels, 0, width);
		Image image = createImage(source);
		g.drawImage(image, x, y, null);
		if (includeChar()) {
			try {
				Image cimage = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
				//				destRect.x += x;
//				destRect.y += y;
				cimage.getGraphics().drawImage(image, 0, 0, null);
				((CharArrayDrawer) arrayDrawer).paintChars(cimage.getGraphics(), sourceRect, destRect);
				g.drawImage(cimage, x, y, null);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Problem drawing Sequence data:" + e);
				LogBuffer.println("" + e);
				e.printStackTrace();
				g.drawImage(image, x, y, null);
			}
		} else {
				g.drawImage(image, x, y, null);
		}
	}
	
	JCheckBox selectionBox;
	public boolean getDrawSelected() {
		return selectionBox.isSelected();
	}
	public void setDrawSelected(boolean bool) {
		selectionBox.setSelected(bool);
	}
	
	
	class InclusionPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		JCheckBox gtrBox, atrBox, dataBox, bboxBox, charBox;
		JTextField xScaleField, yScaleField;
		JTextField borderField;
		BboxRow bboxRow;
		SizeRow sizeRow;
		public boolean useBbox() {
			return bboxBox.isSelected();
		}
		public boolean includeAtr() {
			return atrBox.isSelected();
		}
		public void includeAtr(boolean flag) {
			atrBox.setSelected(flag);
		}
		
		public boolean includeGtr() {
			return gtrBox.isSelected();
		}
		public void includeGtr(boolean flag) {
			gtrBox.setSelected(flag);
		}
		
		public boolean includeData() {
			return dataBox.isSelected();
		}
		public boolean includeChar() {
			if (charBox == null) return false;
			boolean isSelected = charBox.isSelected();
			return isSelected;
		}
		public void includeData(boolean flag) {
			dataBox.setSelected(flag);
		}
		
		public double getXscale() {
			return extractDouble(xScaleField.getText());
		}
		public void setXscale(double newval) {
			xScaleField.setText("" + newval);
		}
		public double getYscale() {
			return extractDouble(yScaleField.getText());
		}
		public void setYscale(double newval) {
			yScaleField.setText("" + newval);
		}
		
		
		public int getBorderPixels() {
			return (int) extractDouble(borderField.getText());
		}
		private double extractDouble(String text) {
			try {
				Double tmp = new Double(text);
				return tmp.doubleValue();
			} catch(java.lang.NumberFormatException e) {
				return 0;
			}
		}
		public int getBboxWidth() {
			return bboxRow.xSize();
		}
		public int getBboxHeight() {
			return bboxRow.ySize();
		}
		public boolean drawSelected() {
			return selectionBox.isSelected();
		}
		public void synchEnabled() {
			selectionBox.setEnabled((geneSelection.getNSelectedIndexes() != 0) ||
			(arraySelection.getNSelectedIndexes() != 0));
			bboxRow.setEnabled(bboxBox.isSelected());
			
			// deal with array tree...
			if (getArrayRootNode() == null) { // no array clustering...
				atrBox.setSelected(false);
				atrBox.setEnabled(false);
			} else {
				if (selectionBox.isSelected()) { // outputting selection...
					if (arraySelection.getSelectedNode() == null) { // no array node selected...
						atrBox.setSelected(false);
						atrBox.setEnabled(false);
					} else {
						atrBox.setEnabled(true);
					}
				} else { // outputting global, array tree exists...
					atrBox.setEnabled(true);
				}
			}
			
			// deal with gene tree...
			if (getGeneRootNode() == null) { // no gene clustering...
				gtrBox.setSelected(false);
				gtrBox.setEnabled(false);
			} else {
				if (selectionBox.isSelected()) { // outputting selection...
					if (geneSelection.getSelectedNode() == null) { // no gene node selected...
						gtrBox.setSelected(false);
						gtrBox.setEnabled(false);
					} else {
						gtrBox.setEnabled(true);
					}
				} else { // outputting global, gene tree exists...
					gtrBox.setEnabled(true);
				}
			}
			
			if (arrayDrawer == null) {
				dataBox.setSelected(false);
				dataBox.setEnabled(false);
			}
			updateSize();
			if (previewPanel != null) previewPanel.updatePreview();
		}
		
		
		/**
		* This routine selects options so that they make sense with respect to the current data
		* in the dendrogram. It should be called during initialization before synchEnabled()
		*/
		public void synchSelected() {
			// do we output selected or the whole thing?
			selectionBox.setSelected((geneSelection.getNSelectedIndexes() != 0) ||
			(arraySelection.getNSelectedIndexes() != 0));
			
			if (selectionBox.isSelected()) { 
				//outputting selected...
				atrBox.setSelected(arraySelection.getSelectedNode() != null);
				gtrBox.setSelected(geneSelection.getSelectedNode() != null);
			} else {
				// outputing everything
				atrBox.setSelected(getArrayRootNode() != null);
				gtrBox.setSelected(getGeneRootNode() != null);
			}
			// always inlcude the data by default... if you have the drawer, that is.
			dataBox.setSelected(arrayDrawer != null);
			
			// recalculateBbox();
			
			updateSize();
			if (previewPanel != null) previewPanel.updatePreview();
		}
		public void recalculateBbox() {
			if (headerSelectionPanel == null) {
				bboxRow.setXsize(2);
				bboxRow.setYsize(2);
			} else {
				Integer rawMaxLength = headerSelectionPanel.geneMaxLength(); 
				bboxRow.setXsize(rawMaxLength==null?0:rawMaxLength);
				bboxRow.setYsize(headerSelectionPanel.arrayMaxLength());
			}
		}
		
		public void updateSize() {
			try {
				sizeRow.setXsize(estimateWidth());
				sizeRow.setYsize(estimateHeight());
				setBorderPixels(getBorderPixels());
			} catch (Exception e) {
				// ignore...
			}
		}
		
		InclusionPanel() {
			documentListener = new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					updateSize();
					if (previewPanel != null) previewPanel.updatePreview();
				}
				@Override
				public void insertUpdate(DocumentEvent e) {
					updateSize();
					if (previewPanel != null) previewPanel.updatePreview();
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					updateSize();
					if (previewPanel != null) previewPanel.updatePreview();
				}
			};
			setupWidgets();
			recalculateBbox();
		}
		DocumentListener documentListener = null;
		private void setupWidgets() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			ActionListener syncher = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					synchEnabled();
				}
			};
			
			add(new JLabel("Include"));
			selectionBox = new JCheckBox("Selection Only");
			selectionBox.addActionListener(syncher);
			JPanel outputPanel = new JPanel();
			outputPanel.add(selectionBox);
			add(outputPanel);
			
			gtrBox = new JCheckBox("Gene Tree");
			gtrBox.addActionListener(syncher);
			outputPanel = new JPanel();
			outputPanel.add(gtrBox);
			add(outputPanel);
			atrBox = new JCheckBox("Array Tree");
			atrBox.addActionListener(syncher);
			outputPanel = new JPanel();
			outputPanel.add(atrBox);
			add(outputPanel);

			dataBox = new JCheckBox("Data Matrix");
			dataBox.addActionListener(syncher);
			outputPanel = new JPanel();
			outputPanel.add(dataBox);
			add(outputPanel);

			if (hasChar) {
				charBox = new JCheckBox("Sequence");
				charBox.addActionListener(syncher);
				outputPanel = new JPanel();
				outputPanel.add(charBox);
				add(outputPanel);
			}
			
			JPanel scalePanel = new JPanel();
			scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.Y_AXIS));
			JPanel Xsub = new JPanel();
			xScaleField = new JTextField(Double.toString(arrayMap.getScale()));
			Xsub.add(new JLabel("x scale"));
			Xsub.add(xScaleField);
			scalePanel.add(Xsub);
			
			yScaleField = new JTextField(Double.toString(geneMap.getScale()));
			JPanel Ysub = new JPanel();
			Ysub.add(new JLabel("y scale"));
			Ysub.add(yScaleField);
			scalePanel.add(Ysub);
			
			borderField = new JTextField(Double.toString(ExportPanel.this.getBorderPixels()));
			JPanel Bsub = new JPanel();
			Bsub.add(new JLabel("Border "));
			Bsub.add(borderField);
			scalePanel.add(Bsub);
			
			scalePanel.add(new JLabel("Use apple key to select multiple headers"));
			
			add(scalePanel);
			
			xScaleField.getDocument().addDocumentListener(documentListener);
			yScaleField.getDocument().addDocumentListener(documentListener);
			borderField.getDocument().addDocumentListener(documentListener);
			
			bboxBox = new JCheckBox("Bounding Box?", hasBbox());
			
			bboxBox.addActionListener(syncher);
			
			outputPanel = new JPanel();
			outputPanel.add(bboxBox);
			bboxRow = new BboxRow();
			if (hasBbox()) {
				add(outputPanel);
				add(bboxRow);
			}
			sizeRow = new SizeRow();
			add(sizeRow);
		}
		class BboxRow extends SizeRow {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void setupWidgets() {
				DocumentListener documentListener = new DocumentListener() {
					@Override
					public void changedUpdate(DocumentEvent e) {
						updateSize();
						if (previewPanel != null) previewPanel.updatePreview();
					}
					@Override
					public void insertUpdate(DocumentEvent e) {
						updateSize();
						if (previewPanel != null) previewPanel.updatePreview();
					}
					@Override
					public void removeUpdate(DocumentEvent e) {
						updateSize();
						if (previewPanel != null) previewPanel.updatePreview();
					}
				};
				add(new JLabel("BBox size:"));
				xSize = new JTextField("2", 4);
				ySize = new JTextField("2", 4);
				add(xSize);
				add(new JLabel("x"));
				add(ySize);
				add(new JLabel("(pixels)"));
				xSize.getDocument().addDocumentListener(documentListener);
				ySize.getDocument().addDocumentListener(documentListener);
			}
		}
		class SizeRow extends JPanel {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			JTextField xSize, ySize;
			public SizeRow() {
				setupWidgets();
			}
			protected void setupWidgets() {
				add(new JLabel("Total Size:"));
				xSize = new JTextField("2", 5);
				ySize = new JTextField("2", 5);
				add(xSize);
				add(new JLabel("x"));
				add(ySize);
				add(new JLabel("(pixels)"));
			}
			double conversionFactor = 1;
			int xSize() {
				return (int) (extractDouble(xSize.getText()) * conversionFactor);
			}
			int ySize() {
				return (int) (extractDouble(ySize.getText()) * conversionFactor);
			}
			
			void setXsize(int points) {
				xSize.setText(convert(points));
			}
			void setYsize(int points) {
				ySize.setText(convert(points));
			}
			/*
			* makes an inch representation of the points, with 2 decimal places.
			*/
			private String convert(int points) {
				Double inch = new Double(Math.rint(((double) points * 100 )/ conversionFactor)/ 100.0);
				return inch.toString();
			}
			@Override
			public void setEnabled(boolean flag) {
				super.setEnabled(flag);
				xSize.setEnabled(flag);
				ySize.setEnabled(flag);
			}
			
		}
	}
	
	class HeaderSelectionPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JCheckBox geneAnnoInside, arrayAnnoInside;
		private HeaderSummary geneSummary = new HeaderSummary();
		public HeaderSummary getGeneSummary() {
			return geneSummary;
		}
		private HeaderSummary arraySummary = new HeaderSummary();
		public HeaderSummary getArraySummary() {
			return arraySummary;
		}
		
		public JList geneList, arrayList;
		public String getGeneAnno(int i) {
			return geneSummary.getSummary(geneHeaderInfo, i);
			//			return assembleAnno(i, geneHeaderInfo, geneList.getSelectedIndices());
		}
		public String getArrayAnno(int i) {
			return arraySummary.getSummary(arrayHeaderInfo, i);
			
			// return assembleAnno(i, arrayHeaderInfo, arrayList.getSelectedIndices());
		}
		public int arrayMaxLength() {
			if (inclusionPanel == null) return 100;
			FontMetrics fontMetrics = getFontMetrics(getArrayFont());
			int max = 0;
			boolean drawSelected = inclusionPanel.drawSelected();
			for (int i = minArray(); i < maxArray(); i++) {
				if (drawSelected &&(arraySelection.isIndexSelected(i) == false)) continue;
				String anno = getArrayAnno(i);
				if (anno == null) continue;
				int length = fontMetrics.stringWidth(anno);
				if (length > max) max = length;
			}
			return max;
		}
		// returns null if there is no selected gene name.
		private Integer geneMaxLength() {
			if (inclusionPanel == null) return 100;
			FontMetrics fontMetrics = getFontMetrics(getGeneFont());
			Integer max = null;
//			boolean drawSelected = inclusionPanel.drawSelected();
			for (int i = minGene(); i < maxGene(); i++) {
				//		if (drawSelected && (geneSelection.isIndexSelected(i) == false)) continue;
				int length = fontMetrics.stringWidth(getGeneAnno(i));
				if (length > 0 && (max == null || length > max)) {
					max = length;
				}
			}
			return max;
		}
		public int getLength(String txt) {
			if (txt == null) return 0;
			//	  FontMetrics fontMetrics = getFontMetrics(getGraphics().getFont());
			FontMetrics fontMetrics = getFontMetrics(getGeneFont());
			return fontMetrics.stringWidth(txt);
		}
		
		public int numArrayHeaders() {
			return arrayList.getSelectedIndices().length;
		}
		public int numGeneHeaders() {
			return geneList.getSelectedIndices().length;
		}
		public void deselectHeaders() {
			arrayList.clearSelection();
			geneList.clearSelection();
		}
		
		public boolean geneAnnoInside() {
			return geneAnnoInside.isSelected();
		}
		public boolean getArrayAnnoInside() {
			return arrayAnnoInside.isSelected();
		}
		public void setArrayAnnoInside(boolean newval) {
			arrayAnnoInside.setSelected(newval);
		}
		
		
		@Override
		public void addNotify() {
			super.addNotify();
			inclusionPanel.recalculateBbox();
		}
		
		HeaderSelectionPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			add(new JLabel("Gene Headers"));
			String [] geneHeaders = geneHeaderInfo.getNames();
			if (geneHeaders == null) {
				geneList = new JList(new String [0]);
			} else {
				geneList = new JList(geneHeaders);
			}
			geneList.setVisibleRowCount(5);
			add(new JScrollPane(geneList));
			
			geneAnnoInside = new JCheckBox("Right of Tree?");
			//	  add(geneAnnoInside);
			add(new JLabel("Array Headers"));
			
			String [] arrayHeaders = arrayHeaderInfo.getNames();
			if (arrayHeaders == null) {
				arrayList = new JList(new String [0]);
			} else {
				arrayList = new JList(arrayHeaders);
			}
			arrayList.setVisibleRowCount(5);
			add(new JScrollPane(arrayList));
			
			arrayAnnoInside = new JCheckBox("Below Tree?");
			arrayAnnoInside.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					inclusionPanel.updateSize();
					if (previewPanel != null)
						previewPanel.updatePreview();
				}
			});
			add(arrayAnnoInside);
			
			ListSelectionListener tmp = new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (inclusionPanel != null) {
						inclusionPanel.recalculateBbox();
						inclusionPanel.updateSize();
						geneSummary.setIncluded(geneList.getSelectedIndices());
						arraySummary.setIncluded(arrayList.getSelectedIndices());
					}
					if (previewPanel != null)
						previewPanel.updatePreview();
				}
			};
			geneList.addListSelectionListener(tmp);
			arrayList.addListSelectionListener(tmp);
			arrayList.setSelectedIndex(0);
			geneList.setSelectedIndex(1);
			setupSelected();
		}
		public void setupSelected() {
			geneSummary.setIncluded(geneList.getSelectedIndices());
			arraySummary.setIncluded(arrayList.getSelectedIndices());
			if (inclusionPanel != null)
				inclusionPanel.updateSize();
		}
	}
	
	class FilePanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JTextField fileField;
		String getFilePath() {
			return fileField.getText();
		}
		File getFile() {
			return new File(getFilePath());
		}
		void setFilePath(String fp) {
			fileField.setText(fp);
			fileField.invalidate();
			fileField.revalidate();
			fileField.repaint();
			
		}
		public FilePanel(String initial) {
			super();
			add(new JLabel("Export To: "));
			fileField = new JTextField(initial);
			add(fileField);
			JButton chooseButton = new JButton("Browse");
			chooseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						JFileChooser chooser = new JFileChooser();
						int returnVal = chooser.showSaveDialog(ExportPanel.this);
						if(returnVal == JFileChooser.APPROVE_OPTION) {
							fileField.setText(chooser.getSelectedFile().getCanonicalPath());
						}
					} catch (java.io.IOException ex) {
						LogBuffer.println("Got exception " + ex);
					}
				}
			});
			add(chooseButton);
		}
	}

	public abstract void save();
	public void setExplicitGtrWidth(Double explicitGtrWidth) {
		this.explicitGtrWidth = explicitGtrWidth;
	}
	public void setExplicitAtrHeight(Double explicitAtrHeight) {
		this.explicitAtrHeight = explicitAtrHeight;
	}
	
}

class TestExportPanel extends ExportPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	TestExportPanel(MapContainer aMap, MapContainer gMap) {
		this(new DummyHeaderInfo(), new DummyHeaderInfo(), new TreeSelection(4),new TreeSelection(5), aMap, gMap);
	}
	TestExportPanel(HeaderInfo arrayHeaderInfo, HeaderInfo geneHeaderInfo, 
	TreeSelectionI geneSelection,
	TreeSelectionI arraySelection,
	MapContainer aMap,MapContainer gMap) {		
		super(arrayHeaderInfo, geneHeaderInfo, geneSelection, arraySelection, null, null, null, aMap, gMap, false);
	}
	@Override
	public void save() {
		
	}
}


