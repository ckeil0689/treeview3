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

import Utilities.Helper;
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
	
	/* TODO Should remove MIN_TILE_NUM as per the discussion on bitbucket issue
	 * #65. Besides, should be an int and it should prevent zooming past it
	 * instead of bouncing back as it is currently doing... related to
	 * calculation in zoomToSelected() */
	private static final double MIN_TILE_NUM = 1.0;
	private static final double ZOOM_INCREMENT = 0.05;

	private final String default_map = "Fixed";
	private double default_scale = 1.0;
	private double minScale;
	private IntegerMap current = null;
	private final String mapName;

	private FixedMap fixedMap = null;
	private FillMap fillMap = null;
	private NullMap nullMap = null;

	private JScrollBar scrollbar = null;
	private TreeDrawerNode selected = null;
	private Preferences configNode = null;

	private double tileNumVisible;

	// Track explicitly manipulated visible area (instead of the visible area)
	// as
	// is manipulated via indirect actions (such as resizing the window)
	private int numVisible;
	private int firstVisible;

	public MapContainer(final String mapName) {

		this.fixedMap = new FixedMap();
		this.fillMap = new FillMap();
		this.nullMap = new NullMap();
		this.current = nullMap;
		this.mapName = mapName;
	
	}

	public MapContainer(final String type, final String mapName) {

		this(mapName);
		setMap(type);
		
		/* 
		 * TODO Initial numVisible currently set in setIndexRange()
		 * Default should NOT be zero, but max value! 
		 */
//		this.numVisible = getAvailablePixels();
		this.firstVisible = 0;
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(mapName);

		} else {
			LogBuffer.println("Could not find or create MapContainer "
					+ "node because parentNode was null.");
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
	 *
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
	public void setToMinScale() {
		// LogBuffer.println("setHome() called.");
		calculateNewMinScale();

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		firstVisible = 0;
		numVisible = getMaxIndex() + 1;
		// LogBuffer.println("setHome: numVisible has been set to [" +
		// numVisible + "].");

		setScale(minScale);
	}

	public void calculateNewMinScale() {

		// LogBuffer.println("calculateNewMinScale: tileNumVisible has been set to ["
		// + tileNumVisible + "].");
		// Added 1 because tileNumVisible is treated as number of indexes
		// visible, not max index
		this.tileNumVisible = getMaxIndex() + 1;
		minScale = getCalculatedMinScale();
		
	}

	/**
	 * Sets the scale of this MapContainer to the last saved value. The 
	 * value needs to be the tile number that is visible on screen. The actual
	 * scale value does not apply if the screen size changed since that would
	 * mean that too few or too many tiles might be visible.
	 */
	public void setLastScale() {

		if (configNode != null) {
			int lastNumVisible = configNode.getInt("scale", getMaxIndex());
			double lastScale = getAvailablePixels() / lastNumVisible;
			
			setScale(lastScale);
		}
	}

	/**
	 * Sets the minimum scale for the available screen space so that no white
	 * space occurs in GlobalView.
	 */
	public double getCalculatedMinScale() {

		final double pixels = getAvailablePixels();
		final double divider = getMaxIndex() - getMinIndex() + 1;
		return pixels / divider;
	}
	
	/**
	 * This method allows for a more intuitive call from other classes so
	 * that the meaning is conveyed and parameters don't have to be understood
	 * and set first.
	 * TODO expand on this to implement adding/ removing tiles on either side
	 * of the axis.
	 */
	public void zoomOut() {
		
		incrementalZoom(false);
	}
	
	/**
	 * This method allows for a more intuitive call from other classes so
	 * that the meaning is conveyed and parameters don't have to be understood
	 * and set first.
	 * TODO expand on this to implement adding/ removing tiles on either side
	 * of the axis.
	 */
	public void zoomIn() {
		
		incrementalZoom(true);
	}
	
	//This is for gradually zooming away from the center of the currently displayed dots
	public void zoomOutCenter() {

		//LogBuffer.println("zoomOutCenter...");
		//LogBuffer.println("zoomOut: Value of firstVisible at start: [" + firstVisible + "].");
		int zoomVal;
		int initialFirstVisible = firstVisible;
		double newScale = getScale();

		tileNumVisible = Math.round(getAvailablePixels() / getScale());
		// LogBuffer.println("zoomOut: tileNumVisible has been set to [" +
		// tileNumVisible + "].");

		zoomVal = (int) Math.round(ZOOM_INCREMENT * tileNumVisible);

		// Ensure that at least one tile will be zoomed out.
		if (zoomVal < 2) {
			zoomVal = 2;
		}

		tileNumVisible = tileNumVisible + zoomVal;
		if (tileNumVisible > (getMaxIndex() + 1)) {
			tileNumVisible = getMaxIndex() + 1;
		}
		// LogBuffer.println("zoomOut: tileNumVisible has been set to [" +
		// tileNumVisible + "].");

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		int prevNumVisible = numVisible;
		numVisible = (int) tileNumVisible;
		
		//Ensure that the difference between the previous number visible and the new number visible is even
		if((numVisible - prevNumVisible) % 2 == 1 && numVisible < (getMaxIndex() + 1)) {
			numVisible++;
			tileNumVisible += 1;
		}
		//LogBuffer.println("zoomOut: numVisible has been changed from [" + prevNumVisible + "] to [" + numVisible + "] or more precisely [" + tileNumVisible + "].  Max index: [" + (getMaxIndex() + 1) + "].");

		newScale = getAvailablePixels() / tileNumVisible;

		final double myMinScale = getCalculatedMinScale();
		if (newScale < myMinScale) {
			newScale = myMinScale;
		}
		setScale(newScale);

		int newFirstVisible = initialFirstVisible - (numVisible - prevNumVisible) / 2;
		if((newFirstVisible + numVisible) > (getMaxIndex() + 1)) {
			newFirstVisible = (getMaxIndex() + 1) - numVisible;
		}
		if(newFirstVisible < 0) {
			newFirstVisible = 0;
		}
		//LogBuffer.println("zoomOut: firstVisible has been changed from [" + initialFirstVisible + "] to [" + newFirstVisible + "].");
		if(newFirstVisible != firstVisible) {
			scrollToFirstIndex(newFirstVisible);
		}
		//LogBuffer.println("zoomOut: Official new firstVisible: [" + initialFirstVisible + "].");
		
		notifyObservers();
	}

	/**
	 * Method to zoom in the MapContainer and set the scale for drawing. 
	 * It's important to take the range of elements displayed on
	 * screen (range of scrollbar -> visibleAmount()) into account, as well as
	 * the amount of available pixels on screen. Otherwise there will be drawing
	 * and selection issues.
	 *
	 * @param boolean zoomsIn Tells the method if it should zoom in or out.
	 */
	private void incrementalZoom(boolean zoomsIn) {

		int zoomVal;
		double newScale = getScale();

		tileNumVisible = Math.round(getAvailablePixels() / getScale());

		//Changed the zoom percentage to be based on a percentage instead of on
		//the minimum number of tiles to display, as this causes a problem with
		//the zoom buttons when you want zoomToSelection to be able to zoom to a
		//single cell (i.e. MIN_TILE_NUM = 1)
		zoomVal = (int) Math.round((double) tileNumVisible * ZOOM_INCREMENT);

		// Ensure that at least one tile will be zoomed out.
		if (zoomVal < 1) {
			zoomVal = 1;
		}

		if(zoomsIn) {
			tileNumVisible -= zoomVal;
			if (tileNumVisible < 1) {
				tileNumVisible = 1;
			}
		} else {
			tileNumVisible += zoomVal;
			if (tileNumVisible > (getMaxIndex() + 1)) {
				tileNumVisible = getMaxIndex() + 1;
			}
		}

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		numVisible = (int) tileNumVisible;

		newScale = getAvailablePixels() / tileNumVisible;

		/* Take care of boundary cases */
		final double limitScale;
		if(zoomsIn) {
			limitScale = getAvailablePixels();
			if (newScale > limitScale) {
				newScale = limitScale;
			}
		} else {
			limitScale = getCalculatedMinScale();
			if (newScale < limitScale) {
				newScale = limitScale;
			}
		}
		
		setScale(newScale);

	}

	//This is for gradually zooming toward the center of the currently displayed dots
	public void zoomInCenter() {

		// final double maxScale = getAvailablePixels();
		double newScale = getScale();
		int zoomVal;

		tileNumVisible = Math.round(getAvailablePixels() / getScale());
		// LogBuffer.println("zoomIn: tileNumVisible has been set to [" +
		// tileNumVisible + "].");

		zoomVal = (int) Math.round(ZOOM_INCREMENT * tileNumVisible);

		// Ensure that at least 2 tiles will be zoomed in (1 on each side of the current zoom dimension).
		if (zoomVal < 2) {
			zoomVal = 2;
		}

		tileNumVisible = tileNumVisible - zoomVal;
		if (tileNumVisible < 1) {
			tileNumVisible = 1;
		}
		// LogBuffer.println("zoomIn: tileNumVisible has been set to [" +
		// tileNumVisible + "].");

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		int prevNumVisible = numVisible;
		numVisible = (int) tileNumVisible;
		
		//Ensure that the difference between the previous number visible and the new number visible is even
		if((prevNumVisible - numVisible) % 2 == 1 && numVisible > 1) {
			numVisible--;
			tileNumVisible -= 1;
		}
		//LogBuffer.println("zoomIn: numVisible has been changed from [" + prevNumVisible + "] to [" + numVisible + "].");

		// Recalculating scale
		newScale = getAvailablePixels() / tileNumVisible;

		final double myMaxScale = getAvailablePixels();
		if (newScale > myMaxScale) {
			newScale = myMaxScale;
		}
		setScale(newScale);
		
		int newFirstVisible = firstVisible + (prevNumVisible - numVisible) / 2;
		//LogBuffer.println("zoomIn: firstVisible has been changed from [" + firstVisible + "] to [" + newFirstVisible + "].");
		scrollToFirstIndex(newFirstVisible);

		notifyObservers();
	}

	/* TODO: Delete this function - it's outdated (though it is more consistent than its replacements). - Rob */
	//This is for gradually zooming toward a selected block of dots (there's currently no corresponding zoom away function)
	//This function is deprecated. It works, but is a little jerky when it runs into an edge.
	//It has essentially been replaced by smoothZoomTowardSelection in GlobalView and the functions it calls in here called getZoomTowardPixelOfSelection and zoomTowardPixel
	//smoothZoomTowardSelection resides in GlobalView instead of here because it keeps track of the aspect ratio, needing info on both dimensions
	public int zoomToward(int firstIndex,int numIndexes) {
		int updateAspectRatio = 0;
		//Catch errors - If num indexes is less than 1 or greater than the number of pixels available
		if(numIndexes < 1 || numIndexes > getAvailablePixels() || firstIndex < 0 || (firstIndex + numIndexes - 1) > getMaxIndex()) {
			//LogBuffer.println("zoomToward: Invalid parameters. firstIndex: [" + firstIndex + "] numIndexes: [" + numIndexes + "] available pixels: [" +
			//		getAvailablePixels() + "] maxIndex: [" + getMaxIndex() + "].");
			return(updateAspectRatio);
		}

		int initialFirstVisible = firstVisible;
		double newScale = getScale();
		int zoomVal;
		tileNumVisible = Math.round(getAvailablePixels() / getScale());
		zoomVal = (int) Math.round(ZOOM_INCREMENT * tileNumVisible);
		// Ensure that at least 2 tiles will be zoomed in (1 on each side of the current zoom dimension).
		if (zoomVal < 2) {
			zoomVal = 2;
		}
		
		tileNumVisible = tileNumVisible - zoomVal;
		if (tileNumVisible < 1) {
			tileNumVisible = 1;
		}

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		int prevNumVisible = numVisible;
		numVisible = (int) tileNumVisible;
		
		//Ensure that the difference between the previous number visible and the new number visible is even
		if((prevNumVisible - numVisible) % 2 == 1 && numVisible > 1) {
			numVisible--;
			tileNumVisible -= 1;
		}

		if (tileNumVisible < numIndexes) {
			tileNumVisible = numIndexes;
			numVisible = numIndexes;
		}
		//LogBuffer.println("zoomToward: numVisible has been changed from [" + prevNumVisible + "] to [" + numVisible + "].");

		// Recalculating scale
		newScale = getAvailablePixels() / tileNumVisible;

		final double myMaxScale = getAvailablePixels();
		if (newScale > myMaxScale) {
			newScale = myMaxScale;
		}
		setScale(newScale);
		
		int newFirstVisible = 0;
		if(numIndexes == numVisible) {
			newFirstVisible = firstIndex;
		} else {
			int diff = numVisible - numIndexes;
			
			//If the difference is odd and greater than 2, the selection isn't going to be quite centered by the time we get to it,
			//so we'll shift things early on by 1 to correct it so it's centered as soon as possible
			if(diff % 2 == 1 && diff > 2) {
				numVisible--;
				tileNumVisible -= 1;
				numIndexes--;
				newScale = getAvailablePixels() / tileNumVisible;
				setScale(newScale);
				diff++;
			}

			//If the first visible index is inside the target area
			if(initialFirstVisible > firstIndex) {
				newFirstVisible = firstIndex;
				//LogBuffer.println("zoomToward: Shifting selection start into view.");
			}
			//Else if the last visible index is inside the target area
			else if((initialFirstVisible + prevNumVisible - 1) < (firstIndex + numIndexes - 1)) {
				newFirstVisible = (firstIndex + numIndexes) - numVisible;
				//LogBuffer.println("zoomToward: Shifting selection end into view because ((" +
				//		initialFirstVisible + " + " + prevNumVisible + " - 1) < (" + firstIndex + " + " + numIndexes + " - 1)).");
			}
			//If the left/top side is closer than or equal to half the difference in area sizes
			else if((firstIndex - initialFirstVisible) <= (diff / 2) &&
					((initialFirstVisible + prevNumVisible - 1) - (firstIndex + numIndexes - 1)) > (diff / 2)) {
				newFirstVisible = initialFirstVisible;
				//LogBuffer.println("zoomToward: Left/top is remaining fixed.");
				
				updateAspectRatio = 1;
			}
			//If the right/bottom side is closer than or equal to half the difference in area sizes
			else if(((initialFirstVisible + prevNumVisible - 1) - (firstIndex + numIndexes - 1)) <= (diff / 2) &&
					(firstIndex - initialFirstVisible) > (diff / 2)) {
				newFirstVisible = (initialFirstVisible + prevNumVisible - 1) - numVisible + 1;
				//LogBuffer.println("zoomToward: Right/Bottom is remaining fixed.");

				updateAspectRatio = 1;
			}
			else {
				newFirstVisible = firstIndex - diff / 2;
				//LogBuffer.println("zoomToward: Zooming in equally. " + newFirstVisible + " = " + firstIndex + " - (int) (" + diff + " / 2)");
			}
		}
		//LogBuffer.println("zoomToward: firstVisible has been changed from [" + initialFirstVisible + "] to [" + newFirstVisible + "].");
		scrollToFirstIndex(newFirstVisible);

		notifyObservers();

		return(updateAspectRatio);
	}

	//This function smoothly zooms while trying to keep the dot the cursor is currently over under the cursor.
	//Some of the code is generalized in case it's ever called with a value out of range
	//Set customZoomVal to a negative number (e.g. -1) to dynamically determine the zoomVal based on a 5% zoom increment
	public int zoomTowardPixel(int pixelPos,int customZoomVal) {
		int updateAspectRatio = 0;
		int firstIndex = getIndex(pixelPos);
		//Catch errors - If num indexes is less than 1 or greater than the number of pixels available
		if(firstIndex < 0 || firstIndex > getMaxIndex()) {
			return(updateAspectRatio);
		}
		
		int dotOver = getIndex(pixelPos);
		int initialFirstVisible = firstVisible;
		double newScale = getScale();
		int zoomVal;
		int pxAvail = getAvailablePixels();
		tileNumVisible = Math.round(pxAvail / newScale);
		double targetZoomFrac = ZOOM_INCREMENT;

		//This is a test of selecting the best zoom value using the getBestZoomVal method (called from outside of this class)
		if(customZoomVal >= 0) {
			zoomVal = customZoomVal;
			//LogBuffer.println("zoomTowardPixel: Custom zoom value: [" + zoomVal + "].");
		}
		//If a custom zoom value has not been supplied (in order to correct dot aspect ratios (see GlobalView)),
		//select the best zoom value to make the zooming as smooth as possible
		else if(customZoomVal < 0 && tileNumVisible > 15) {
			zoomVal = getBestZoomInVal(pixelPos,numVisible,targetZoomFrac);
			if (zoomVal < 1) {
				zoomVal = 1;
			}
			//LogBuffer.println("zoomTowardPixel: Best zoom value: [" + zoomVal + "].");
		}
		//Zooming with no smoothing
		else {
			zoomVal = (int) Math.round(tileNumVisible * targetZoomFrac);

			if (zoomVal < 1) {
				zoomVal = 1;
			}
			//LogBuffer.println("zoomTowardPixel: No smoothing zoom value: [" + zoomVal + "].");
		}

		//Make sure we're left with at least 1 dot on the screen
		tileNumVisible = tileNumVisible - zoomVal;
		if (tileNumVisible < 1) {
			tileNumVisible = 1;
		}

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		int prevNumVisible = numVisible;
		numVisible = (int) tileNumVisible;

		// Recalculating scale
		newScale = pxAvail / tileNumVisible;

		final double myMaxScale = getAvailablePixels();
		if (newScale > myMaxScale) {
			newScale = myMaxScale;
		}
		setScale(newScale);

		//Now determine where to scroll to to keep the cursor over the same dot
		int newFirstVisible = 0;
		if(1 == numVisible) {
			newFirstVisible = firstIndex;
		} else {
			//If the first visible index is inside the target area
			if(initialFirstVisible > firstIndex) {
				newFirstVisible = firstIndex;
			}
			//Else if the last visible index is inside the target area (assuming the size of the area we're zooming toward is 1 data cell)
			else if((initialFirstVisible + prevNumVisible - 1) < firstIndex) {
				newFirstVisible = (firstIndex + 1) - numVisible;
			} else {
				newFirstVisible = (int) ((double) dotOver + 1 - (double) pixelPos / newScale);

				//If the result of pixelPos / newScale is a whole number, newFirstVisible must be decremented (discovered via trial & error)
				if(newFirstVisible > 0 && (double) (pixelPos / newScale) == (int) (pixelPos / newScale)) {
					//LogBuffer.println("zoomTowardPixel: Adjusting calculation for whole number results.");
					newFirstVisible--;
				}

				//If the target has shifted off the left/top side of the screen
				if(newFirstVisible > firstIndex) {
					newFirstVisible = firstIndex;
					//LogBuffer.println("zoomTowardPixel: Left/top is remaining fixed.");
					updateAspectRatio = 1;
				}
				//If the target cell has shifted off the right/bottom side of the screen
				else if((newFirstVisible + numVisible - 1) < firstIndex) {
					newFirstVisible = firstIndex - (numVisible - 1);
					//LogBuffer.println("zoomTowardPixel: Right/Bottom is remaining fixed.");
					updateAspectRatio = 1;
				} else {
					//LogBuffer.println("zoomSpreadToward: (newFirstVisible = (int) ((double) dotOver + 1 - (double) pixelPos / newScale)) = " +
					//		"[" + newFirstVisible + " = (int) ((double) " + dotOver + " + 1 - (double) " + pixelPos + " / " + newScale + ")].");
				}
			}
		}

		scrollToFirstIndex(newFirstVisible);
		
		//Catch and correct bad calculations - this is useless now that the whole-number result test was added above
		if(dotOver != getIndex(pixelPos)) {
			int correction = dotOver - getIndex(pixelPos);
			newFirstVisible += correction;
			//LogBuffer.println("Warning: The data cell hovered over has shifted. It was over [" + dotOver + "].  Now it is over: [" + getIndex(pixelPos) +
			//		"].  Previous dotOver calculation: [firstVisible + (int) ((double) pixelPos / newScale))] = [" + firstVisible + " + (int) ((double) " + pixelPos + " / " + newScale + "))].  Correcting this retroactively...");
			scrollToFirstIndex(newFirstVisible);
		}

		notifyObservers();

		return(updateAspectRatio);
	}

	/* TODO: Remove this function and replace it with the one below that has "Universal" appended to the name. - Rob */
	//Use this function to pick a pixel to zoom toward when zooming toward a selection
	//Assumes that the full selection is visible on the screen
	public int getZoomTowardPixelOfSelection(int pixelIndexOfSelec,int numPixelsOfSelec) {
		int numPixelsOffsetOfSelec = pixelIndexOfSelec;
		int numTotalPixels = getAvailablePixels() + 1;   //getAvailablePixels actually is returning the max pixel index (starting from 0)
		
		//numTotalPixels SHOULD be a count of pixels (i.e. starting from 1)
		//if(pixelIndexOfSelec < 0 && (pixelIndexOfSelec + numPixelsOfSelec) > numTotalPixels) {
		//	//I should be able to use the code below for this calculation, but that currently doesn't work and
		//	//for right now, I'm debugging other code and need a quick approximate value returned here.
		//	return((int) Math.round((double) numTotalPixels / 2.0));
		//} else
		//If any of the selected area is above/before the visible area
		//if((pixelIndexOfSelec + numPixelsOfSelec) < 0) {
		if(pixelIndexOfSelec < 0 && (pixelIndexOfSelec + numPixelsOfSelec) < numTotalPixels) {
			//LogBuffer.println("Start Pixel: [" + pixelIndexOfSelec + "] and end pixel: [" + pixelIndexOfSelec + " + " + numTotalPixels + "] of selection is out of range/less than 0.");
			//If zooming out, return the furthest pixel, otherwise return the closest pixel
			if(numPixelsOfSelec > numTotalPixels) {
				//LogBuffer.println("Returning Target pixel: [" + (numTotalPixels - 1) + "].");
				return(numTotalPixels - 1);
			} else {
				//LogBuffer.println("Returning Target pixel: [0].");
				return(0);
			}
		}
		//If any of the selected area is below/after the visible area, return the first pixel so that
		else if((pixelIndexOfSelec + numPixelsOfSelec) > numTotalPixels && pixelIndexOfSelec > numPixelsOfSelec) {
			//LogBuffer.println("Start Pixel: [" + pixelIndexOfSelec + "] and end pixel: [" + pixelIndexOfSelec + " + " + numTotalPixels + "] of selection is out of range/greater than the number of pixels that are in the display.");
			//If zooming out, return the furthest pixel, otherwise return the closest pixel
			if(numPixelsOfSelec > numTotalPixels) {
				//LogBuffer.println("Returning Target pixel: [0].");
				return(0);
			} else {
				//LogBuffer.println("Returning Target pixel: [" + (numTotalPixels - 1) + "].");
				return(numTotalPixels - 1);
			}
		}

		//The following equation is based on the merging of 2 equations and then solving for targetPixel
		//Let's say that numPixelsOffsetOfSelec is the number of pixels above/left of the start of the selection
		//Let's say that numPixelsOfSelec is the number of pixels in the selected box (in the current dimension)
		//We want the pixel where these fractions are equal: targetPixelRelativeToSelection / numPixelsOfSelec = targetPixel / numTotalPixels
		//We know that targetPixel = numPixelsOffsetOfSelec + targetPixelRelativeToSelection
		//Solve for targetPixelRelativeToSelection = targetPixel - numPixelsOffsetOfSelec
		//Substitute targetPixelRelativeToSelection with targetPixel - numPixelsOffsetOfSelec in the first selection and solve for targetPixel and you get the equation below
		int targetPixel = (int) Math.round((double) numTotalPixels * (double) numPixelsOffsetOfSelec / ((double) numPixelsOfSelec - (double) numTotalPixels));
		//LogBuffer.println("Target Pixel calculation: targetPixel = numTotalPixels * numPixelsOffsetOfSelec / (numPixelsOfSelec - numTotalPixels)");
		//LogBuffer.println("Target Pixel calculation: " + targetPixel + " = " + numTotalPixels + " * " + numPixelsOffsetOfSelec + " / (" + numPixelsOfSelec + " - " + numTotalPixels + ")");
		
		//The equation above actually has 2 solutions.  If the solution is negative, we just need to negate it, otherwise,
		//the solution turns out to need a slight adjustment because it is on the outside of the selected area.
		//I'm not sure why, but the difference with the offset just needs to be added to the offset.
		if(targetPixel < 0) {
			//LogBuffer.println("Negating Target pixel: [" + targetPixel + "].");
			targetPixel = Math.abs(targetPixel);
		} else {
			//LogBuffer.println("Adjusting Target pixel by offset: [" + targetPixel + "].");
			targetPixel = numPixelsOffsetOfSelec + Math.abs(numPixelsOffsetOfSelec-targetPixel);
		}
		
		//Decrement because pixels are indexed from 0 and the equation above assumes indexing from 1
		targetPixel--;
		
		if(targetPixel < 0 || targetPixel >= numTotalPixels) {
			//LogBuffer.println("Error: target Pixel is out of range: [" + targetPixel + "]");
			targetPixel = 0;
		}
		
		//LogBuffer.println("Returning Target pixel: [" + targetPixel + "].");
		return(targetPixel);
	}

	//This function does the reverse of zoomTowardPixel
	//Some of the code is generalized in case it's ever called with a value out of range
	public int zoomAwayPixel(int pixelPos,int customZoomVal) {
		int updateAspectRatio = 0;
		int firstIndex = getIndex(pixelPos);
		//Catch errors - If num indexes is less than 1 or greater than the number of pixels available
		if(firstIndex < 0 || firstIndex > getMaxIndex()) {
			return(updateAspectRatio);
		}
		
		int dotOver = getIndex(pixelPos);
		int initialFirstVisible = firstVisible;
		double newScale = getScale();
		int zoomVal;
		int pxAvail = getAvailablePixels();
		tileNumVisible = Math.round(pxAvail / newScale);
		double targetZoomFrac = ZOOM_INCREMENT;

		//This is a test of selecting the best zoom value using the getBestZoomVal method (called from outside of this class)
		if(customZoomVal >= 0) {
			zoomVal = customZoomVal;
		}
		//If a custom zoom value has not been supplied (in order to correct dot aspect ratios (see GlobalView)),
		//select the best zoom value to make the zooming as smooth as possible
		else if(customZoomVal < 0 && tileNumVisible >= 15) {
			zoomVal = getBestZoomOutVal(pixelPos,numVisible,targetZoomFrac);
			if (zoomVal < 1) {
				zoomVal = 1;
			}
		}
		//Zooming with no smoothing
		else {
			zoomVal = (int) Math.round(tileNumVisible * targetZoomFrac);

			if (zoomVal < 2) {
				zoomVal = 2;
			}
		}

		tileNumVisible = tileNumVisible + zoomVal;
		if (tileNumVisible > (getMaxIndex() + 1)) {
			tileNumVisible = getMaxIndex() + 1;
			updateAspectRatio = 1;
		}

		//Keep track of explicit changes, by the user, to the amount of visible data
		int prevNumVisible = numVisible;
		numVisible = (int) tileNumVisible;
		//LogBuffer.println("zoomOut: numVisible has been set to [" + numVisible + "].");

		newScale = pxAvail / tileNumVisible;

		final double myMinScale = getCalculatedMinScale();
		if (newScale < myMinScale) {
			newScale = myMinScale;
			updateAspectRatio = 1;
		}
		setScale(newScale);

		//LogBuffer.println("new scale: [" + newScale + "].");
		int newFirstVisible = 0;
		if(1 == numVisible) {
			newFirstVisible = firstIndex;
		} else {
			//If the first visible index is inside the target area
			if(initialFirstVisible > firstIndex) {
				newFirstVisible = firstIndex;
				//LogBuffer.println("zoomAwayPixel: Shifting selection start into view.");
			}
			//Else if the last visible index is inside the target area
			else if((initialFirstVisible + prevNumVisible - 1) < firstIndex) {
				newFirstVisible = (firstIndex + 1) - numVisible;
			} else {
				newFirstVisible = (int) ((double) dotOver + 1 - (double) pixelPos / newScale);
				//newFirstVisible = (int) ((double) dotOverFrac + 1.0 - (double) pixelPos / newScale);

				//If the result of pixelPos / newScale is a whole number, newFirstVisible must be decremented (discovered via trial & error)
				if(newFirstVisible > 0 && (double) (pixelPos / newScale) == (int) (pixelPos / newScale)) {
					//LogBuffer.println("zoomAwayPixel: Adjusting calculation for whole number results.");
					newFirstVisible--;
				}

				//If the left/top side is closer than or equal to half the difference in area sizes
				if(newFirstVisible > firstIndex) {
					newFirstVisible = firstIndex;
					//LogBuffer.println("zoomAwayPixel: Left/top is remaining fixed.");
					updateAspectRatio = 1;
				}
				//If the right/bottom side is closer than or equal to half the difference in area sizes
				else if((newFirstVisible + numVisible - 1) < firstIndex) {
					newFirstVisible = firstIndex - (numVisible - 1);
					//LogBuffer.println("zoomAwayPixel: Right/Bottom is remaining fixed.");
					updateAspectRatio = 1;
				} else {
					//LogBuffer.println("zoomAwayPixel: (newFirstVisible = (int) ((double) dotOver - (double) pixelPos / newScale)) = " +
					//		"[" + newFirstVisible + " = (int) ((double) " + dotOver + " - (double) " + pixelPos + " / " + newScale + ")].");
				}
			}
		}

		scrollToFirstIndex(newFirstVisible);

		//Catch and correct bad calculations - this is useless now that the whole-number result test was added above
		if(dotOver != getIndex(pixelPos)) {
			int correction = dotOver - getIndex(pixelPos);
			newFirstVisible += correction;
			//LogBuffer.println("Warning: The data cell hovered over has shifted. It was over [" + dotOver + "].  Now it is over: [" + getIndex(pixelPos) +
			//		"].  Previous dotOver calculation: [firstVisible + (int) ((double) pixelPos / newScale))] = [" + firstVisible + " + (int) ((double) " + pixelPos + " / " + newScale + "))].  Correcting this retroactively...");
			scrollToFirstIndex(newFirstVisible);
			updateAspectRatio = 1;
		}

		notifyObservers();

		return(updateAspectRatio);
	}

	/* TODO: Merge this function with getBestZoomOutVal - there are only subtle differences that could be handled in an if statement. - Rob */
	//This calculates an optimal number of data indexes to remove that will result in as smooth a zoom as possible.
	//It uses relative data index hovered over to calculate the target position of the zoom (as opposed to the relative pixel position of the cursor)
	public int getBestZoomInVal(int pixel,int cells,double targetZoomFrac) {
		int zoomVal = (int) Math.round((double) cells * targetZoomFrac);
		int numPixels = getAvailablePixels();

		//LogBuffer.println("getBestZoomInVal: Called with cells [" + cells + "] targetZoomFrac [" + targetZoomFrac + "] and calculated zoomVal as [" + zoomVal + "].");
		//If we're at the minimum zoom level, do not zoom in any more
		if((cells == 1 && targetZoomFrac <= 1) || (targetZoomFrac % 1 == 0) && ((int) Math.round(targetZoomFrac)) == 0) {
			return(0);
		}
		if (zoomVal < 2) {
			zoomVal = 2;
		}
		//If the amount of zoom is larger than the number of cells currently visible
		if (zoomVal > cells) {
			zoomVal = cells - 1;
		}
		int smallThresh = 15;
		int adjustWindow = 10;
		if(cells <= smallThresh) {
			adjustWindow = 3;
		}
		int bestZoomVal = zoomVal;
		//Select the best zoom value within a range of 5 of the target zoom val to make the zooming as smooth as possible
		int zoomMin = zoomVal - adjustWindow;
		if(zoomMin < 0) {
			zoomMin = 0;
		}
		//if(zoomMin > cells) {
		//	zoomMin = cells - 1;
		//}
		int zoomMax = zoomVal + adjustWindow;
		if((cells - zoomMax) > (getMaxIndex() + 1)) {
			zoomMax = getMaxIndex() + 1 - cells;
		}

		//Sort the range of zoom values such that the target zoomVal is first, then +1, -1, +2, -2,...
		//We're arbitrarily using the target zoom value to start out - the sort is just a heuristic anyway - doesn't really matter that much other than to prefer the target values
		double minDiff = 1.0;
		//LogBuffer.println("Creating array 	of size zoomMax - zoomMin + 2: [" + zoomMax + " - " + zoomMin + " + 2].");
		int[] zoomRange = new int[zoomMax - zoomMin + 2]; //2 was just to be on the safe side.
		int i = 0;
		for(int l = 0;l <= (zoomMax - zoomMin);l++) {
			if(l >= 0 && (zoomVal + l) <= zoomMax) {
				zoomRange[i] = zoomVal + l;
				i++;
				if(l == 0) continue;
			}
			if(i > (zoomMax - zoomMin)) break;
			if(l > 0 && (zoomVal - l) >= zoomMin) {
				zoomRange[i] = zoomVal - l;
				i++;
			}
			if(i > (zoomMax - zoomMin)) break;
		}
		
		double minDiffThresh = 0.005; //0=best possible ratio. "Good enough value" for when the ratio of the relative cell position of the cell under
		//the cursor is really close to the ratio of the relative pixel position of the cursor

		//The corresponding zoom-out function uses relative pixel position, but on zoom-in, it's more accurate to use the relative cell position the mouse is over
		//double targetFrac = ((double) getIndex(pixel) + 1.0) / ((double) getMaxIndex() + 1.0);
		double targetFrac = ((double) getIndex(pixel) + 1.0 - (double) firstVisible) / ((double) numVisible);
		double diff = 0.0;
		//LogBuffer.println("getBestZoomInVal: minZoom: [" + zoomMin + "] maxZoom: [" + zoomMax + "].");
		//Loop through the zoomVals and find which one will result in a ratio with the relative index that is closest to the pixelPos to pxAvail ratio
		for(i = 0;i <= (zoomMax - zoomMin);i++) {
			int z = zoomRange[i];
			int relCell = (int) (targetFrac * (double) z);
			//LogBuffer.println("getBestZoomInVal: [relCell = (int) (pixel / numPixels * z)] = [" + relCell + " = (int) (" + pixel + " / " + numPixels + " * " + z + ")].");
			if(z == 0) continue;
			diff = Math.abs(((double) relCell / (double) z) - targetFrac);
			//LogBuffer.println("getBestZoomInVal: [diff = Math.abs((double) (relCell / z) - (double) (pixel / numPixels))] = [" +
			//		diff + " = Math.abs((double) (" + relCell + " / " + z + ") - (double) (" + pixel + " / " + numPixels + ")].");
			if(diff < minDiff) {
				bestZoomVal = z;
				minDiff = diff;
				//Stop if the difference from the target is "good enough", i.e. close to zero
				if(minDiff < minDiffThresh) {
					break;
				}
			}
		}
		//LogBuffer.println("getBestZoomInVal: Selected zoomVal [" + bestZoomVal + "] instead of defaults [" + zoomVal + "] Diff: [" + diff + "].");
		
		//Do not force a zoom amount here because a minimum zoom should be enforced after this has been called
		if(bestZoomVal < 0) bestZoomVal = 0;
		return(bestZoomVal);
	}

	/* TODO: Merge this function with getBestZoomInVal - there are only subtle differences that could be handled in an if statement. - Rob */
	//This function performs the reverse of getBestZoomInVal, except it uses the relative pixel position of the cursor to calculate the zoom position that is targeted for smoothing
	public int getBestZoomOutVal(int pixel,int cells,double targetZoomFrac) {
		int zoomVal = (int) Math.round((double) cells * targetZoomFrac);
		int numPixels = getAvailablePixels();

		//If the closest zoom amount is 0, return 0 because it's the smoothest possible scroll value (and the math below will result in NaN)
		//The calling function is expected to handle cases resulting in no zoom
		if (zoomVal < 1) {
			return(0);
		}
		//If the resulting zoom level is bigger than the max zoom-out
		if ((zoomVal + cells) > (getMaxIndex() + 1)) {
			zoomVal = getMaxIndex() - cells;
		}
		int smallThresh = 15;
		int adjustWindow = 7;
		//If we're getting close to full zoom-out, search a smaller window so that the last bit of zoom-out isn't likely to be all on 1 axis
		if(((getMaxIndex() + 1) - cells) <= smallThresh) {
			adjustWindow = 3;
		}
		int bestZoomVal = zoomVal;
		//Select the best zoom value within a range of 5 of the target zoom val to make the zooming as smooth as possible
		int zoomMin = zoomVal - adjustWindow;
		if(zoomMin < 1) {
			zoomMin = 1;
		}
		//IUf the minimum number of cells to zoom is larger than the current number of cells displayed on this axis - this mitigates occurrences of "zoom jumping"
		//if(zoomMin > cells) {
		//	zoomMin = cells - 1;
		//}
		//If the resulting zoom max is bigger than the max zoom-out
		int zoomMax = zoomVal + adjustWindow;
		if((zoomMax + cells) > (getMaxIndex() + 1)) {
			zoomMax = getMaxIndex() + 1 - cells;
		}

		//Sort the range of zoom values such that the target zoomVal is first, then +1, -1, +2, -2,...
		//We're arbitrarily using the target zoom value to start out - the sort is just a heuristic anyway - doesn't really matter that much other than to prefer the target values
		double minDiff = 1.0;
		//LogBuffer.println("Creating array of size zoomMax - zoomMin + 2: [" + zoomMax + " - " + zoomMin + " + 2].");
		int[] zoomRange = new int[zoomMax - zoomMin + 2]; //2 was just to be on the safe side.
		int i = 0;
		for(int l = 0;l <= (zoomMax - zoomMin);l++) {
			if(l >= 0 && (zoomVal + l) <= zoomMax) {
				zoomRange[i] = zoomVal + l;
				i++;
				if(l == 0) continue;
			}
			if(i > (zoomMax - zoomMin)) break;
			if(l > 0 && (zoomVal - l) >= zoomMin) {
				zoomRange[i] = zoomVal - l;
				i++;
			}
			if(i > (zoomMax - zoomMin)) break;
		}
		
		double minDiffThresh = 0.005; //0=best possible ratio. "Good enough value" for when the ratio of the relative cell position of the cell under
		//the cursor is really close to the ratio of the relative pixel position of the cursor

		//LogBuffer.println("getBestZoomOutVal: minZoom: [" + zoomMin + "] maxZoom: [" + zoomMax + "].");
		double diff = 0.0;
		//Loop through the zoomVals and find which one will result in a ratio with the relative index that is closest to the pixelPos to pxAvail ratio
		for(i = 0;i <= (zoomMax - zoomMin);i++) {
			int z = zoomRange[i];
			int relCell = (int) ((double) (pixel + 1) / (double) numPixels * (double) z + 1);
			//LogBuffer.println("getBestZoomOutVal: [relCell = (int) (pixel / numPixels * z)] = [" + relCell + " = (int) (" + pixel + " / " + numPixels + " * " + z + ")].");
			if(z == 0) continue;
			diff = Math.abs(((double) relCell / (double) z) - ((double) (pixel + 1) / (double) numPixels));
			//LogBuffer.println("getBestZoomOutVal: [diff = Math.abs((double) (relCell / z) - (double) (pixel / numPixels))] = [" +
			//		diff + " = Math.abs((double) (" + relCell + " / " + z + ") - (double) (" + pixel + " / " + numPixels + ")].");
			if(diff < minDiff) {
				bestZoomVal = z;
				minDiff = diff;
				//Stop if the difference from the target is "good enough", i.e. close to zero
				if(minDiff < minDiffThresh) {
					break;
				}
			}
		}
		
		//Do not force a zoom amount here because a minimum zoom should be enforced after this has been called
		if(bestZoomVal < 0) bestZoomVal = 0;
		//LogBuffer.println("getBestZoomOutVal: Selected zoomVal [" + bestZoomVal + "] instead of default [" + zoomVal + "] Difference in smoothness accuracy: [" + diff + "].");
		return(bestZoomVal);
	}

	public void fullZoomOut() {
		zoomToSelected(0,getMaxIndex());
	}

	/**
	 * Zooms to the selected index range for this MapContainer. 
	 * Uses the array- and geneSelection and currently available pixels on
	 * screen retrieved from the MapContainer objects to calculate a new scale
	 * and zoom in on it by working in conjunction with centerSelection().
	 * 
	 * @param selectedMin Minimum index of selected area.
	 * @param selectedMax Maximum index of selected area.
	 */
	public void zoomToSelected(final int selectedMin, final int selectedMax) {
		
		/* 
		 * We'll allow the user to surpass the min zoom index when 
		 * they are near the edge, so that their selection is centered 
		 * on the screen, so let's get the edges of the selection.
		 */
	
		/* Set an appropriate minimum tile number. */
		double minTileNum = MIN_TILE_NUM;
		
		if(getMaxIndex() < MIN_TILE_NUM) {
			minTileNum = getMaxIndex();
		}
		
		/* 
		 * TODO Why double...Makes no logical sense. But if int there are 
		 * calculation (precision/ rounding) errors. Should find out where 
		 * that happens some time later...
		 * */
		double numSelected = selectedMax - selectedMin + 1;
		
		// If the gene selection is smaller than the minimum zoom level
		if(numSelected < minTileNum) {
			
			// If the center of the selection is less than half the distance to
			// the near edge
			if ((selectedMin + numSelected / 2) < (minTileNum / 2)) {
				numSelected = (selectedMin + numSelected / 2) * 2;
			}
			// Else if the center of the selection is less than half the
			// distance to the far edge
			else if ((selectedMin + numSelected / 2) > (getMaxIndex() 
					- (minTileNum / 2))) {
				numSelected = (getMaxIndex() - (selectedMin 
						+ numSelected / 2 - 1)) * 2;
			}
			// Otherwise, set the standard minimum zoom
			else {
				numSelected = minTileNum;
			}
		}
		
		if(numSelected > 0) {
			double newScale = getAvailablePixels() / numSelected;
			
			// Track explicitly manipulated visible area (instead of the visible
			// area) as
			// is manipulated via indirect actions (such as resizing the window)
			final int numArrayIndexes = (int) Math.round(numSelected);
			setNumVisible(numArrayIndexes);

			setScale(newScale);

			/* 
			 * TODO move this outside to avoid multiple calls? center and zoom 
			 * are usually called together... 
			 */
			notifyObservers();
		}
	}
	
	public void centerScrollOnSelection(final int startIndex, 
			final int endIndex) {
		
		int scrollVal = (int) Math.round((endIndex + startIndex) / 2.0);;
		
		scrollToIndex(scrollVal);
		
		int firstX = 0;
		while (firstX < getMaxIndex() && !isVisible(firstX)) {
			firstX++;
		}
		
		setFirstVisible(firstX);
		
		/* 
		 * TODO move this outside to avoid multiple calls? center and zoom 
		 * are usually called together... 
		 */
		notifyObservers();
	}
	
	/**
	 * Checks how many tiles are visible and uses the current available pixels
	 * to adjust the tile scale to the screen size.
	 */
	public void adjustScaleToScreen() {
		
		double newScale = getAvailablePixels() / numVisible;
		setScale(newScale);
		
		notifyObservers();
	}

	public void recalculateScale() {

		if (nodeHasAttribute("FixedMap", "scale")) {
			if (getScale() < getAvailablePixels())
				return;
		}

		// The divisor here was previously calculated by:
		// getMaxIndex() - getMinIndex() + 1, which inadvertently resulted
		// in zooming out the currently viewed area, however this function
		// was being called in contexts where the user was not intending
		// to do that, so instead, I changed the divisor to be the number
		// of data indexes currently being viewed. -Rob
		LogBuffer.println("recalculateScale: numVisible: [" + tileNumVisible
				+ "] was used to calculate requiredScale.");
		final double requiredScale = getAvailablePixels() / numVisible;
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

		if (current.type().equals(string))
			return current;

		IntegerMap newMap = null;
		if (nullMap.type().equals(string)) {
			newMap = nullMap;

		} else if (fillMap.type().equals(string)) {
			newMap = fillMap;

		} else if (fixedMap.type().equals(string)) {
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
		// The getVisibleAmount return value can change by resizing the window,
		// so use getNumVisible instead
		// This assumes that getNumVisible is updated before this function is
		// called.
		// scrollbar.setValue(i - scrollbar.getVisibleAmount() / 2);

		// LogBuffer.println("scrollToIndex: Scrolling from [" + j +
		// "] to (i - getNumVisible() / 2): [" + i + " - " + getNumVisible() +
		// " / 2] or: [" + (i - getNumVisible() / 2) + "].");
		scrollbar.setValue(i - getNumVisible() / 2);

		// Keep track of the first visible index
		// This used to be set using scrollbar.getVisibleAmount, but that can
		// change implicitly when the window is resized.
		setFirstVisible(i - getNumVisible() / 2);

		if (j != scrollbar.getValue()) {
			setChanged();
		}
	}

	public void scrollToFirstIndex(final int i) {

		final int j = scrollbar.getValue();
		scrollbar.setValue(i);

		// Keep track of the first visible index
		setFirstVisible(i);

		// LogBuffer.println("Current scrollbar value: [" + j +
		// "].  Scrolling to: [" + i + "].");

		if (j != i) {
			setChanged();
		}

		notifyObservers();
	}

	public void scrollBy(final int i) {

		final int j = scrollbar.getValue();
		scrollbar.setValue(j + i);

		// Keep track of the first visible index
		setFirstVisible(j + i);

		if (j != scrollbar.getValue()) {
			setChanged();
		}

		notifyObservers();
	}

	public JScrollBar getScroll() {

		return scrollbar;
	}

	// This is a listener for scroll events. When a scroll happens, this
	// executes.
	@Override
	public void adjustmentValueChanged(final AdjustmentEvent adjustmentEvent) {

		// LogBuffer.println("Adjusting scrollbar position?: [" +
		// adjustmentEvent.getValue() + "].");
		// Keep track of explicit view changes made by the user
		setFirstVisible(adjustmentEvent.getValue());
		setChanged();
		notifyObservers(scrollbar);
	}

	private void setupScrollbar() {

		if (scrollbar != null) {
			// This value can change when the window is resized larger, so use
			// stored value instead
			// int value = scrollbar.getValue();
			int value = getFirstVisible();

			// This value can change when the window is resized larger, so use
			// stored value instead
			// final int extent = current.getViewableIndexes();
			int extent = getNumVisible();
			if (extent < 1) {
				extent = current.getViewableIndexes();
			}

			final int max = current.getMaxIndex() - current.getMinIndex() + 1;
			if (value + extent > max) {
				value = max - extent;
				setFirstVisible(value);
			}

			if (value < 0) {
				value = 0;
				setFirstVisible(0);
			}

			// LogBuffer.println("Setting scrollbar values: [" + value + "," +
			// extent + "," + 0 + "," + max + "]");
			scrollbar.setValues(value, extent, 0, max);
			scrollbar.setBlockIncrement(current.getViewableIndexes());
		}
	}

	/**
	 * expect to get updates from selection only
	 */
	@Override
	public void update(final Observable observable, final Object object) {

		LogBuffer.println(new StringBuffer("MapContainer Got an "
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

	public double getMinScale() {

		return minScale;
		// return current.getMinScale();
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
		if (i < min)
			return false;

		if (i > max)
			return false;

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
			/* 
			 * TODO improve numVisible implementation
			 * Setting default numVisible here for now '
			 */
			setNumVisible(j + 1);
			
			// Added this, but took it out because it was to fix something that
			// previously wasn't broken, so instead of try to patch it, I'm
			// going to
			// check out the old code to see what went wrong
			// if(current.getMinIndex() != i) {
			// //Keep track of explicitly changed position of visible data
			// indexes
			// setFirstVisible(i);
			// }
			setChanged();
		}
	}

	public void setScale(final double d) {
		
		// LogBuffer.println("Scale sent in (d) = [" + d +
		// "], d*int(AvailablePix/d) = ["
		// + ((d * (int) (getAvailablePixels() / d)))
		// + "] UsedPix = [" + getUsedPixels() + "] =? AvailPix = ["
		// + getAvailablePixels() + "] Scale = [" + getScale()
		// + "] ScrollBar#: " + scrollbar.getVisibleAmount());

		if (!Helper.nearlyEqual(fixedMap.getScale(), d)) {
			fixedMap.setScale(d);
			setupScrollbar();
			setChanged();

			// LogBuffer.println("Scale set.");

			configNode.putDouble("scale", d);
			configNode.putDouble("minScale", minScale);
		}
		// else {
		// LogBuffer.println("Scale not set.  Scale sent in (d) = [" + d +
		// "].  Apparently the scale is nearly equal to: [" +
		// fixedMap.getScale() + "].");
		// }
	}

	/**
	 * The purpose of these two functions (setNumVisible and setFirst visible),
	 * other than to simply set values is to support the tracking of explicit
	 * image manipulation by the user. The main reasons these were implemented
	 * was because the image was being changed by implicit actions, such as
	 * resizing the window. The pre-existing functionality, as far as I could
	 * tell, only had a means to obtain information on what data indexes were
	 * currently being displayed by way of converting a pixel index into a data
	 * index. These variables are manipulated only by actions that the user
	 * explicitly takes to intentionally alter the image, meaning the range of
	 * spots they are looking at.
	 */
	public void setNumVisible(final int i) {
		// LogBuffer.println("setNumVisible: numVisible has been set to [" + i +
		// "].");
		numVisible = i;
	}

	public void setFirstVisible(final int i) {
		if (i >= 0) {
			firstVisible = i;
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

	/**
	 * Returns amount of tiles currently visible on this map.
	 *
	 * @return
	 */
	public int getTileNumVisible() {

		return (int) tileNumVisible;
	}

	/**
	 * The purpose of these two functions (getNumVisible and getFirst visible),
	 * other than to simply obtain values is to support the tracking of explicit
	 * image manipulation by the user. The main reasons these were implemented
	 * was because the image was being changed by implicit actions, such as
	 * resizing the window. The pre-existing functionality, as far as I could
	 * tell, only had a means to obtain information on what data indexes were
	 * currently being displayed by way of converting a pixel index into a data
	 * index. These variables are manipulated only by actions that the user
	 * explicitly takes to intentionally alter the image, meaning the range of
	 * spots they are looking at.
	 */
	public int getNumVisible() {

		return numVisible;
	}

	public int getFirstVisible() {

		return firstVisible;
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
			// Added this, but took it out because it was to fix something that
			// previously wasn't broken, so instead of try to patch it, I'm
			// going to
			// check out the old code to see what went wrong
			// //Keep track of explicitly selected first visible data index -
			// not sure if this one is an explicit change of the viewed data by
			// the user...
			// setFirstVisible(current.getMinIndex());
			setChanged();
		}
	}

	/**
	 * Checks if a certain node has a certain attribute (key).
	 *
	 * @param nodeName
	 * @param key
	 * @return
	 */
	public boolean nodeHasAttribute(final String nodeName, final String key) {

		boolean hasAttribute = false;

		try {
			final String[] keys = configNode.node(nodeName).keys();
			for (final String key2 : keys) {

				if (key2.equalsIgnoreCase(key)) {
					hasAttribute = true;
				}
			}

		} catch (final BackingStoreException e) {
			LogBuffer.println("Error in MapContainer/nodeHasAttribute: "
					+ e.getMessage());
			e.printStackTrace();
		}

		return hasAttribute;
	}
}
