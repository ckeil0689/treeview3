/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: MapContainer.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:45 $
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

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JScrollBar;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeDrawerNode;

/**
 * MapContainers tell the views which pixel offset to draw each array or gene
 * index at the scrollbars "scroll" by communicating with the maps.
 * 
 * This is distinct from which genes are selected (see the TreeSelection object)
 */
public class MapContainer extends Observable implements Observer,
		AdjustmentListener, ConfigNodePersistent {

	private final String default_map = "Fixed";
	private double default_scale = 1.0;
	private double minScale;
	private IntegerMap current = null;
	private String mapName;

	private FixedMap fixedMap = null;
	private FillMap fillMap = null;
	private NullMap nullMap = null;

	private JScrollBar scrollbar = null;
	private TreeDrawerNode selected = null;
	private Preferences configNode = null;

	public MapContainer(String mapName) {

		this.fixedMap = new FixedMap();
		this.fillMap = new FillMap();
		this.nullMap = new NullMap();
		this.current = nullMap;
		this.mapName = mapName;
		
	}

	public MapContainer(final String type, String mapName) {

		this(mapName);
		setMap(type);
	}

	private Preferences fetchOrCreateNode(final String name) {

		// Get first child?
		Preferences ret = configNode.node(name);
//		if (ret == null) {
//			ret = root.create(name);
//		}

		return ret;
	}

//	// confignode persistent
//	@Override
//	public void bindConfig(final Preferences configNode) {
//
//		root = configNode;
//
//		// first bind subordinate maps...
//		fixedMap.bindConfig(fetchOrCreateNode("FixedMap"));
//		fillMap.bindConfig(fetchOrCreateNode("FillMap"));
//		nullMap.bindConfig(fetchOrCreateNode("NullMap"));
//
//		// then, fix self up...
//		setMap(root.get("current", default_map));
//	}
	
	// confignode persistent
	@Override
	public void setConfigNode(Preferences parentNode) {

		if(parentNode != null) {
			this.configNode = parentNode.node(mapName);
			
		} else {
			LogBuffer.println("Could not find or create MapContainer " +
					"node because parentNode was null.");
		}

		// first bind subordinate maps...
		fixedMap.setTypeString("FixedMap");
		fixedMap.setConfigNode(configNode);
		
		fillMap.setTypeString("FillMap");
		fillMap.setConfigNode(configNode);
		
		nullMap.setTypeString("NullMap");
		nullMap.setConfigNode(configNode);

		// then, fix self up...
		setMap(configNode.get("current", default_map));
	}

	/**
	 * Sets the MapContainer's scale back to the default value.
	 * @param d
	 */
	public void setDefaultScale(final double d) {

		default_scale = d;
		fixedMap.setDefaultScale(d);
	}

	/**
	 * Resets the MapContainer so that it completely fills out the available 
	 * screen space.
	 */
	public void setHome() {
		
		setMinScale();
		setScale(minScale);
	}

	/**
	 * Sets the minimum scale for the available screen space so that no
	 * white space occurs in GlobalView.
	 */
	public void setMinScale() {

		final double pixels = getAvailablePixels();
		final double divider = getMaxIndex() - getMinIndex();
		minScale = pixels/ divider;
	}

	/**
	 * Method to zoom out of the MapContainer and set the scale for drawing
	 * lower than before. It's important to take the range of genes displayed on
	 * screen (range of scrollbar -> visibleAmount()) into account, as well as
	 * the amount of available pixels on screen. Otherwise there will be drawing
	 * and selection issues.
	 * 
	 * @param double zoomVal
	 */
	public void zoomOut() {

		double newScale = 0.0;
		double zoomVal = 1;
		double rest = 0.0;

		if (scrollbar.getVisibleAmount() <= 20) {
			zoomVal = 1;
			
		} else if (scrollbar.getVisibleAmount() <= 100) {
			zoomVal = 5;
			
		} else if (scrollbar.getVisibleAmount() <= 500) {
			zoomVal = 20;
			
		} else if (scrollbar.getVisibleAmount() <= 1000) {
			zoomVal = 50;
			
		} else if (scrollbar.getVisibleAmount() <= 6000) {
			zoomVal = 100;
		}

		newScale = getAvailablePixels()
				/ (scrollbar.getVisibleAmount() + zoomVal);
		rest = getAvailablePixels()
				% (scrollbar.getVisibleAmount() + zoomVal);

		if (newScale < minScale) {
			newScale = minScale;
		}

		setScale(newScale);
	}

	/**
	 * Method to zoom in to the MapContainer and set the scale for drawing
	 * higher than before. It's important to take the range of genes displayed
	 * on screen (range of scrollbar -> visibleAmount()) into account, as well
	 * as the amount of available pixels on screen. Otherwise there will be
	 * drawing and selection issues.
	 * 
	 * @param double zoomVal
	 */
	public void zoomIn() {

		final double minAllowedTiles = 1;
		final double maxScale = getAvailablePixels() / minAllowedTiles;
		double newScale = 0.0;
		double zoomVal = 1;
		double rest = 0.0;

		if (scrollbar.getVisibleAmount() <= 20) {
			zoomVal = 1;
			
		} else if (scrollbar.getVisibleAmount() <= 100) {
			zoomVal = 5;
			
		} else if (scrollbar.getVisibleAmount() <= 500) {
			zoomVal = 20;
			
		} else if (scrollbar.getVisibleAmount() <= 1000) {
			zoomVal = 50;
			
		} else if (scrollbar.getVisibleAmount() <= 6000) {
			zoomVal = 100;
		}

		// Recalculating scale
		newScale = getAvailablePixels()
				/ (scrollbar.getVisibleAmount() - zoomVal);
		rest = getAvailablePixels()
				% (scrollbar.getVisibleAmount() - zoomVal);

		if (newScale > maxScale) {
			newScale = maxScale;
		}

		setScale(newScale);
	}

	/**
	 * 
	 * @param scale
	 * @return
	 */
	public double roundScale(final double scale) {

		double fittedScale = 1.0;

		fittedScale = Math.round(scale * 2)/ 2;

		return fittedScale;
	}

	public void recalculateScale() {

//		if (root.fetchFirst("FixedMap").hasAttribute("scale")) {
		if (nodeHasAttribute("FixedMap","scale")) {
			if (getScale() < getAvailablePixels()) {
				return;
			}
		}

		final int range = getMaxIndex() - getMinIndex() + 1;
		final double requiredScale = getAvailablePixels() / range;
		if (requiredScale > default_scale) {
			setScale(requiredScale);

		} else {
			setScale(default_scale);
		}
	}

	public void setScrollbar(final JScrollBar scrollbar) {

		if (this.scrollbar != null) {
			this.scrollbar.removeAdjustmentListener(this);
		}

		this.scrollbar = scrollbar;
		if (this.scrollbar != null) {
			this.scrollbar.addAdjustmentListener(this);
			setupScrollbar();
		}
	}

	public IntegerMap setMap(final String string) {

		if (current.type().equals(string)) {
			return current;
		}

		IntegerMap newMap = null;
		if (nullMap.type().equals(string)) {
			// System.out.println("type " + string + " is nullMap");
			newMap = nullMap;

		} else if (fillMap.type().equals(string)) {
			// System.out.println("type " + string + " is fillMap");
			newMap = fillMap;

		} else if (fixedMap.type().equals(string)) {
			// System.out.println("type " + string + " is fixedMap");
			newMap = fixedMap;
		}

		if (newMap == null) {

			LogBuffer.println("Couldn't find map matching " + string
					+ " in MapContainer.java");
			LogBuffer.println("Choices include");
			LogBuffer.println(nullMap.type());
			LogBuffer.println(fixedMap.type());
			LogBuffer.println(fillMap.type());
			newMap = fixedMap;
		}

		switchMap(newMap);
		return current;
	}

	/*
	 * Scrollbar Functions
	 */
	public void scrollToIndex(final int i) {

		final int j = scrollbar.getValue();
		scrollbar.setValue(i - scrollbar.getVisibleAmount() / 2);

		if (j != scrollbar.getValue()) {
			setChanged();
		}
	}

	public void scrollBy(final int i) {

		final int j = scrollbar.getValue();
		scrollbar.setValue(j + i);

		if (j != scrollbar.getValue()) {
			setChanged();
		}
	}

	public JScrollBar getScroll() {

		return scrollbar;
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent adjustmentEvent) {

		setChanged();
		notifyObservers(scrollbar);
	}

	private void setupScrollbar() {

		if (scrollbar != null) {
			int value = scrollbar.getValue();
			final int extent = current.getViewableIndexes();
			final int max = current.getMaxIndex() - current.getMinIndex() + 1;
			if (value + extent > max) {
				value = max - extent;
			}

			if (value < 0) {
				value = 0;
			}

			scrollbar.setValues(value, extent, 0, max);
			scrollbar.setBlockIncrement(current.getViewableIndexes());
		}
	}

	/**
	 * expect to get updates from selection only
	 */
	@Override
	public void update(final Observable observable, final Object object) {

		System.out.println(new StringBuffer("MapContainer Got an "
				+ "update from unknown ").append(observable).toString());
		notifyObservers(object);
	}

	public void underlyingChanged() {

		setupScrollbar();
		setChanged();
	}

	public boolean contains(final int i) {

		return current.contains(i);
	}

	/*
	 * Mapping Functions
	 */

	// forward all map operations...
	public double getScale() {

		return current.getScale();
	}

	public int getPixel(final double d) {

		int offset = 0;
		if (scrollbar != null) {
			offset = scrollbar.getValue();
		}

		return current.getPixel(d - offset);
	}

	public int getPixel(final int i) {

		int offset = 0;
		if (scrollbar != null) {
			offset = scrollbar.getValue();
		}

		return current.getPixel(i - offset);
	}

	public int getIndex(final int pix) {

		int index = 0;
		if (current != null) {
			index = current.getIndex(pix);
		}

		if (scrollbar != null) {
			index += scrollbar.getValue();
		}

		return index;
	}

	public boolean isVisible(final int i) {

		final int min = getIndex(0);
		final int max = getIndex(getAvailablePixels());
		if (i < min) {
			return false;
		}

		if (i > max) {
			return false;
		}

		return true;
	}

	// {return current.getPixel(intval);}

	public int getRequiredPixels() {

		return current.getRequiredPixels();
	}

	public int getUsedPixels() {

		return current.getUsedPixels();
	}

	public void setAvailablePixels(final int i) {

		final int j = current.getUsedPixels();
		current.setAvailablePixels(i);
		setupScrollbar();

		if (j != current.getUsedPixels()) {
			setChanged();
		}
	}

	public void setIndexRange(int i, int j) {
		if (i > j) {
			final int k = i;
			i = j;
			j = k;
		}

		if (current.getMinIndex() != i || current.getMaxIndex() != j) {
			current.setIndexRange(i, j);
			setupScrollbar();
			setChanged();
		}
	}

	public void setScale(final double d) {

		if (fixedMap.getScale() != d) {
			fixedMap.setScale(d);
			setupScrollbar();
			setChanged();
		}
	}

	public int getMiddlePixel(final int i) {

		return (getPixel(i) + getPixel(i + 1)) / 2;
	}

	/**
	 * Get maximum amount of data points on entire MapContainer
	 * 
	 * @return
	 */
	public int getMaxIndex() {

		return current.getMaxIndex();
	}

	/**
	 * Get minimum amount of data points on entire MapContainer
	 * 
	 * @return
	 */
	public int getMinIndex() {

		return current.getMinIndex();
	}

	/**
	 * Get smallest scale
	 * 
	 * @return
	 */
	public double getMinScale() {

		return minScale;
	}

	public TreeDrawerNode getSelectedNode() {

		return selected;
	}

	public void setSelectedNode(final TreeDrawerNode treeDrawerNode) {

		if (selected != treeDrawerNode) {
			/*
			 * System.out.println("setindexrange called, start = " + selected);
			 * Throwable t = new Throwable(); t.printStackTrace();
			 */
			selected = treeDrawerNode;
			setChanged();
		}
	}

	public IntegerMap getCurrent() {

		return current;
	}

	public int getAvailablePixels() {

		return current.getAvailablePixels();
	}

	private void switchMap(final IntegerMap integerMap) {

		if (current != integerMap) {
			if (configNode != null) {
				configNode.put("current", integerMap.type());
			}
			integerMap.setAvailablePixels(current.getAvailablePixels());
			integerMap.setIndexRange(current.getMinIndex(),
					current.getMaxIndex());
			current = integerMap;
			setupScrollbar();
			setChanged();
		}
	}
	
	/**
	 * Checks if a certain node has a certain attribute (key).
	 * @param nodeName
	 * @param key
	 * @return
	 */
	public boolean nodeHasAttribute(String nodeName, String key) {
		
		boolean hasAttribute = false;
		
		try {
			String[] keys = configNode.node(nodeName).keys();
			for(int i = 0; i < keys.length; i++) {
				
				if(keys[i].equalsIgnoreCase(key)) {
					hasAttribute = true;
				}
			}
			
		} catch (BackingStoreException e) {
			LogBuffer.println("Error in MapContainer/nodeHasAttribute: " 
					+ e.getMessage());
			e.printStackTrace();
		}
		
		return hasAttribute;
	}
}
