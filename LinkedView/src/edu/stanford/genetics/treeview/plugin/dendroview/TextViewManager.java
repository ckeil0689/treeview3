/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TextViewManager.java,v $
 * $Revision: 1.3 $
 * $Date: 2008-06-11 01:58:57 $
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

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import edu.stanford.genetics.treeview.*;

/**
 * @author avsegal
 *
 */
public class TextViewManager extends ModelView 
implements FontSelectable, PropertyChangeListener {

	private static final long serialVersionUID = 1L;
	
	private boolean ignoreDividerChange = false;
	private UrlExtractor uExtractor;
	private Component root;
	private int numViews;
	private int numShown;
	private HeaderInfo hI;
	private Vector<ModelView> textViews;
	private ConfigNode configRoot;
	private HeaderSummary headerSummary = new HeaderSummary();
	private int dividerLocations [];
	
	/**
	 * Constructor
	 * @param hI
	 * @param uExtractor
	 */
	public TextViewManager(HeaderInfo hI, UrlExtractor uExtractor, 
			DataModel model) {
		
		super();
		this.hI = hI;
		this.uExtractor = uExtractor;
		
		//Find out what kind of file was loaded
		String srcFileType = model.getSource().toLowerCase();
		
		root = null;
		textViews = new Vector<ModelView>();
		
		panel = new JPanel();
		panel.setLayout(new GridLayout());
		
		dividerLocations = new int[hI.getNumNames() - 1];
//		firstNotShown = null;
		numShown = 0;
				
		//could set up headerSummary...
		int GIDIndex = hI.getIndex("GID");
		if (GIDIndex == -1 && !srcFileType.endsWith(".cdt")) {
			headerSummary.setIncluded(new int [] {0}); //changed from {1}???
			
		} else if (GIDIndex == -1 && srcFileType.endsWith(".cdt")) {
			headerSummary.setIncluded(new int [] {1});
			
		}else {
			headerSummary.setIncluded(new int [] {2});
		}
		headerSummary.addObserver(this);
				
		makeTextViews(hI.getNumNames());
		
		for(int i = 0; i < numViews - 1; i++) {
			
			dividerLocations[i] = 50;
		}
	
		addTextViews(1);
		loadDividerLocations();
		setVisible(true);
	}
	
	/**
	 * called when confignode or headerSummary is changed.
	 */
	private void loadSelection() {		
		
		if(configRoot == null) {
			return;
		}
		
		ConfigNode [] nodes = configRoot.fetch("Selection");
		int [] included;
		if(nodes.length > 0) {
			included = new int[nodes.length];
			for(int i = 0; i < nodes.length; i++) {
				included[i] = nodes[i].getAttribute("index", -1);
			}
			headerSummary.setIncluded(included);
		}
	}

	/**
	 * called when headers to be displayed are changed.
	 */
	private void saveSelection() {
		
		if(configRoot == null) {
			return;
		}
		
		configRoot.removeAll("Selection");
		
		for(int i = 0; i < headerSummary.getIncluded().length; i++) {
			
			configRoot.create("Selection").setAttribute("index", 
					headerSummary.getIncluded()[i], -1);
		}
	}
	
	@Override
	public void update(Observable ob, Object obj) {
		if(ob == headerSummary) {
			saveSelection();
			saveDividerLocations();
			//saveDividerLocationsToConfig();
			addTextViews(headerSummary.getIncluded().length);
			loadDividerLocations();
		}
		
		for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).update(ob, obj);			
		}
	}
	
	@Override
	public void updateBuffer(Graphics g) {
		
		paintAll(g);
	}
	
	public void updateBuffer(Image buf) {
		
		for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).updateBuffer(buf);			
		}
	}
	
	@Override
	public String viewName() {
		
		return "TextViewManager";
	}
	
	/**
	 * Need to override ModelView.setViewFrame to account for the 
	 * textviews that are contained.
	 *
	 */
	@Override
	public void setViewFrame(ViewFrame m) {
		
		super.setViewFrame(m);
		for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).setViewFrame(m);
		}
	}
	
	@Override
	public void setHintPanel(MessagePanel h) {
		
		super.setHintPanel(h);
		for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).setHintPanel(h);
		}
	}
	
	@Override
	public void setStatusPanel(MessagePanel s) {
		
		super.setStatusPanel(s);
		for(int i = 0; i < textViews.size(); i++)
			((TextView)textViews.get(i)).setStatusPanel(s);
	}
	
	private void makeTextViews(int n) {
		
		numViews = n;
		for(int i = 0; i < n; i++) {
			
			textViews.add(new TextView(hI, uExtractor, i));
			((TextView)textViews.lastElement()).setHeaderSummary(headerSummary);
			headerSummary.addObserver((TextView)textViews.lastElement());
		}
	}
	
	private void addTextViews(int n) {
		
		JSplitPane temp;
		numShown = n;
		
		if(n <= 0) {
			return;
			
		} else if(n == 1) {
			root = ((TextView)textViews.get(0)).getComponent();
			
		} else {
			root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			((JSplitPane)root).setDividerSize(2);
			((JSplitPane)root).setBorder(null);
			((JSplitPane)root).setRightComponent(
					((TextView)textViews.get(n-1)).getComponent());
			((JSplitPane)root).setLeftComponent(
					((TextView)textViews.get(n-2)).getComponent());
			root.addPropertyChangeListener(
					JSplitPane.DIVIDER_LOCATION_PROPERTY, this);
			
			for(int i = n - 3; i >= 0; i--) {
				
				temp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				temp.setLeftComponent(
						((TextView)textViews.get(i)).getComponent());
				temp.setRightComponent(root);
				temp.setDividerSize(2);
				temp.setBorder(null);
				temp.addPropertyChangeListener(
						JSplitPane.DIVIDER_LOCATION_PROPERTY, this);
				root = temp;
			}
		}
		
		panel.removeAll();
		panel.add(root);
		panel.updateUI();
	}
	
	/**
	 * Need to override TextView.setGeneSelection() to account 
	 * for the textviews that are contained.
	 *
	 */
	public void setGeneSelection(TreeSelectionI selection) {
		
		for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).setGeneSelection(selection);
		}
	}
	
	public void setMap(MapContainer zoomYMap) {
		
		for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).setMap(zoomYMap);
		}
	}
	
	public void bindConfig(ConfigNode configNode) {
		
		configRoot = configNode;
        loadSelection(); // doesn't quite work yet, 
        //something more global then this headerSummary needs to be updated.
        ConfigNode [] viewNodes = configRoot.fetch("TextView");
        
        for (int i = viewNodes.length; i < textViews.size(); i++) {
        		configRoot.create("TextView");
        }
        
        viewNodes = configRoot.fetch("TextView");
		for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).bindConfig(viewNodes[i]);
		}
		
		// binding config can change fonts.
		if (textViews.size() > 0) {
			setFont(((TextView)textViews.firstElement()).getFont());
		}
		
		loadDividerLocationsFromConfig();
		loadDividerLocations();
	}
	
	@Override
	public String getFace() {
		
		return getFont().getName();
    }
	
    @Override
	public int getPoints() {
		
    	return getFont().getSize();
    }
    
    @Override
	public int getStyle() {
    		
    	return getFont().getStyle();
    }
    
    @Override
	public void setFace(String string) {
    		
    	for (int i = 0; i < textViews.size(); i++) {
    		((TextView)textViews.get(i)).setFace(string);
    	}	
    	
		if (textViews.size() > 0) {
    		setFont(((TextView)textViews.firstElement()).getFont());
    	}
		
    	repaint();
    }
    
    @Override
	public void setPoints(int size) {
    	
    	for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).setPoints(size);
		}
    	
		if (textViews.size() > 0) {
			setFont(((TextView)textViews.firstElement()).getFont());
		}
		
        repaint();
    }
    
    @Override
	public void setStyle(int style) {
    		
    	for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).setStyle(style);
		}
    	
		if (textViews.size() > 0) {
			setFont(((TextView)textViews.firstElement()).getFont());
		}
		
        repaint();
    }
    
    public void setHeaderSummary(HeaderSummary headerSummary) {
    	
    	this.headerSummary = headerSummary;
    		
    	for(int i = 0; i < textViews.size(); i++) {
			((TextView)textViews.get(i)).setHeaderSummary(headerSummary);
		}
	}
    
	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {
		
		return headerSummary;
	}
	
	public void saveDividerLocationsToConfig() {
		
		ConfigNode node = null;
		if(configRoot != null) {
			node = configRoot.fetchFirst("Dividers");
			
			if(node == null) {
				node = configRoot.create("Dividers");
			}
		} else {
			return;
		}
		
		for(int i = 0; i < numViews - 1; i++) {
			
			if(node != null) {
				node.setAttribute("Position" + i, dividerLocations[i], -1);
			}
		}
	}
	
	public void saveDividerLocations() {
		
		Component temp = panel.getComponent(0);
				
		for(int i = 0; i < numShown - 1; i++) {
			
			dividerLocations[i] = ((JSplitPane)temp).getDividerLocation();
			temp = ((JSplitPane)temp).getRightComponent();
		}
	}
	
	public void loadDividerLocationsFromConfig() {	
		
		ConfigNode node = null;
		if(configRoot != null) {
			node = configRoot.fetchFirst("Dividers");
			
		} else {
			return;
		}
		
		for(int i = 0; i < numViews - 1; i++) {
			
			if(node != null) {
				dividerLocations[i] = node.getAttribute("Position" + i, 50);
			}
		}
	}
	
	public void loadDividerLocations() {
		
		ignoreDividerChange = true;
		Component temp = panel.getComponent(0);

		for(int i = 0; i < numShown - 1; i++) {
			
			((JSplitPane)temp).setDividerLocation(dividerLocations[i]);
			temp = ((JSplitPane)temp).getRightComponent();
		}
		
		ignoreDividerChange = false;
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		
		if(!ignoreDividerChange && pce.getPropertyName() 
				== JSplitPane.DIVIDER_LOCATION_PROPERTY) {
			saveDividerLocations();
			saveDividerLocationsToConfig();
		}
	} 
}
