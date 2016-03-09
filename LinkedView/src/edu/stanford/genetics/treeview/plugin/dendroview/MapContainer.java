/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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

	/*
	 * TODO Should remove MIN_TILE_NUM as per the discussion on bitbucket issue
	 * #65. Besides, should be an int and it should prevent zooming past it
	 * instead of bouncing back as it is currently doing... related to
	 * calculation in zoomToSelected()
	 */
	private static final double MIN_TILE_NUM        = 1.0;
	private static final double ZOOM_INCREMENT      = 0.05;
	private static final double ZOOM_INCREMENT_FAST = 0.15;
	
	public static final int ZOOM_DEFAULT = 0;
	public static final int ZOOM_FAST = 1;
	public static final int ZOOM_SLOW = 2;
	public static final int ZOOM_SLAM = 3;

	private final int default_map = IntegerMap.FIXED;
	private double default_scale = 1.0;
	private double  minScale;
	private IntegerMap current = null;
	private final String     mapName;

	private FixedMap fixedMap = null;
	private FillMap  fillMap  = null;
	private NullMap  nullMap  = null;

	private JScrollBar     scrollbar  = null;
	private TreeDrawerNode selected   = null;
	private Preferences    configNode = null;

	private double tileNumVisible;

	//Track explicitly manipulated visible area (instead of the visible area) as
	//is manipulated via indirect actions (such as resizing the window)
	private int numVisible;
	private int firstVisible;

	//track the first and last visible labels (controlled by LabelView) so that
	//it can be used in other classes (e.g. for matching the trees to the
	//labels)
	private int firstVisibleLabel       = -1;
	private int lastVisibleLabel        = -1;
	private int firstVisibleLabelOffset = 0;
	private int lastVisibleLabelOffset  = 0;

	//Track the explicitly manipulated visible labels. These can change as a
	//result of a scroll in the label pane
	private boolean overLabels            = false;
	private boolean overLabelsScrollbar   = false;
	private boolean overInteractiveMatrix = false;
	private boolean overTree              = false;
	private boolean overDivider           = false;
	private boolean draggingDivider       = false;
	private boolean labelsBeingScrolled   = false;
	private boolean lastTreeModeGlobal    = true;
	private boolean keepTreeGlobal        = true;
	private int     hoverPixel            = -1;
	private int     hoverIndex            = -1;
	private int     hoverTreeMinIndex     = -1;
	private int     hoverTreeMaxIndex     = -1;
	private boolean hoverChanged          = false;
	private boolean selecting             = false;
	private boolean toggling              = false;
	private boolean deselecting           = false;
	private int     selectingStart        = -1;
	private boolean whizMode              = false;

	int debug = 0;
	//1 = debug the state of the variables in overALabelPortLinkedView
	//18 = debug tree hover highlighting of labels

	public MapContainer(final String mapName) {

		this.fixedMap = new FixedMap();
		this.fillMap = new FillMap();
		this.nullMap = new NullMap();
		this.current = nullMap;
		this.mapName = mapName;
	}

	public MapContainer(final int type, final String mapName) {

		this(mapName);
		setMap(type);

		/*
		 * TODO Initial numVisible currently set in setIndexRange() Default
		 * should NOT be zero, but max value!
		 */
		setFirstVisible(0);
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(mapName);

		} else {
			LogBuffer.println("Could not find or create MapContainer "
					+ "node because parentNode was null.");
			return;
		}

		// first bind subordinate maps...
		fixedMap.setType(IntegerMap.FIXED);
		fixedMap.setConfigNode(configNode);

		fillMap.setType(IntegerMap.FILL);
		fillMap.setConfigNode(configNode);

		nullMap.setType(IntegerMap.NULL);
		nullMap.setConfigNode(configNode);

		// then, fix self up...
		setMap(configNode.getInt("current", default_map));
	}

	/**
	 * Sets the MapContainer's scale back to the default value.
	 *
	 * @param d
	 */
	public void setDefaultScale(final double d) {

		this.default_scale = d;
		fixedMap.setDefaultScale(d);
	}

	/**
	 * Resets the MapContainer so that it completely fills out the available
	 * screen space.
	 */
	public void setToMinScale() {
		
		setMinScale();
		setScale(minScale);
		notifyObservers();
	}

	public void setMinScale() {

		this.tileNumVisible = getTotalTileNum();
		this.minScale = getCalculatedMinScale();
		setFirstVisible(0);
		setNumVisible(getTotalTileNum());
		
		setScale(minScale);
		notifyObservers();
	}

	/**
	 * Sets the scale of this MapContainer to the last saved value. The 
	 * value needs to be the tile number that is visible on screen. The actual
	 * scale value does not apply if the screen size changed since that would
	 * mean that too few or too many tiles might be visible.
	 */
	public void setLastScale() {

		if(configNode != null) {
			int lastNumVisible = configNode.getInt("scale", getMaxIndex());
			double lastScale = getAvailablePixels() / lastNumVisible;

			setScale(lastScale);
		}
	}

	/**
	 * Calculates how many pixels this side of a tile is long so that all tiles
	 * can be fir onto the screen (minimum scale). It is based on the
	 * available screen space. 
	 * @return The length of a tile's side such that all tiles fit 
	 * on the screen.
	 */
	public double getCalculatedMinScale() {

		final double pixels  = getAvailablePixels();
		final double divider = getMaxIndex() - getMinIndex() + 1;
		
		return (pixels/ divider);
	}
	
	/**
	 * Tests whether all data tiles for this MapContainer are visible.
	 * @return True if all tiles are visible, false otherwise.
	 */
	public boolean showsAllTiles() {
		
		return getTotalTileNum() == getNumVisible();
	}

	/**
	 * This method allows for a more intuitive call from other classes so
	 * that the meaning is conveyed and parameters don't have to be understood
	 * and set first.
	 */
	public void zoomOutBegin() {
		
		incrementalZoom(false, true, true);
	}

	/**
	 * This method allows for a more intuitive call from other classes so
	 * that the meaning is conveyed and parameters don't have to be understood
	 * and set first.
	 */
	public void zoomInBegin() {
		
		incrementalZoom(true, true, true);
	}

	/**
	 * This method allows for a more intuitive call from other classes so
	 * that the meaning is conveyed and parameters don't have to be understood
	 * and set first.
	 */
	public void zoomOutEnd() {
		
		incrementalZoom(false, false, true);
	}

	/**
	 * This method allows for a more intuitive call from other classes so
	 * that the meaning is conveyed and parameters don't have to be understood
	 * and set first.
	 */
	public void zoomInEnd() {
		
		incrementalZoom(true, false, true);
	}

	//This is for gradually zooming away from the center of the currently
	//displayed dots
	public void zoomOutCenter(final int speed) {

		int zoomVal;
		int initialFirstVisible = firstVisible;
		double newScale = getScale();

		this.tileNumVisible = Math.round(getAvailablePixels() / getScale());

		switch(speed) {
		
		case ZOOM_SLOW: 
			zoomVal = 2;
			break;
		case ZOOM_FAST:
			zoomVal = (int) Math.round(ZOOM_INCREMENT_FAST * tileNumVisible);
			break;
		case ZOOM_SLAM:
			zoomVal = getTotalTileNum() - (int) Math.round(tileNumVisible);
			break;
		default:
			zoomVal = (int) Math.round(ZOOM_INCREMENT * tileNumVisible);
		
		}

		// Ensure that at least one tile will be zoomed out.
		if (zoomVal < 2) {
			zoomVal = 2;
		}

		tileNumVisible = tileNumVisible + zoomVal;
		if (tileNumVisible > (getTotalTileNum())) {
			tileNumVisible = getTotalTileNum();
		}

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		int prevNumVisible = numVisible;
		setNumVisible((int) tileNumVisible);

		//Ensure that the difference between the previous number visible and the
		//new number visible is even
		if((numVisible - prevNumVisible) % 2 == 1 &&
		   numVisible < (getTotalTileNum())) {
			setNumVisible(numVisible + 1);
			tileNumVisible += 1;
		}

		newScale = getAvailablePixels() / tileNumVisible;

		final double myMinScale = getCalculatedMinScale();
		if (newScale < myMinScale) {
			newScale = myMinScale;
		}
		setScale(newScale);

		int newFirstVisible = initialFirstVisible -
		                      (numVisible - prevNumVisible) / 2;
		if((newFirstVisible + numVisible) > (getTotalTileNum())) {
			newFirstVisible = (getTotalTileNum()) - numVisible;
		}
		if(newFirstVisible < 0) {
			newFirstVisible = 0;
		}
		if(newFirstVisible != firstVisible) {
			scrollToFirstIndex(newFirstVisible);
		}

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
	 * @param boolean zoomsIn Tells the method if it should fix the beginning
	 *        (e.g. left/top) or end (e.g. right/bottom).
	 */
	private void incrementalZoom(boolean zoomsIn, boolean beginFixed,
			boolean hardFixed) {

		int zoomVal;
		double newScale = getScale();
		int newFirstVis = getFirstVisible();

		tileNumVisible = Math.round(getAvailablePixels() / getScale());
		if(tileNumVisible > (getTotalTileNum())) {
			tileNumVisible = getTotalTileNum();
		}

		// Changed the zoom percentage to be based on a percentage instead of on
		// the minimum number of tiles to display, as this causes a problem with
		// the zoom buttons when you want zoomToSelection to be able to zoom to
		// a
		// single cell (i.e. MIN_TILE_NUM = 1)
		zoomVal = (int) Math.round(tileNumVisible * ZOOM_INCREMENT);

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
			//Don't add columns on the side that is fixed
			if(hardFixed) {
				if(beginFixed) {
					if(zoomVal > ((getTotalTileNum()) -
						(firstVisible + numVisible))) {

						zoomVal = (getTotalTileNum()) -
							(firstVisible + numVisible);
						if(zoomVal <= 0) {
							return;
						}
					}
				} else {
					if(zoomVal > firstVisible) {
						zoomVal = firstVisible;
						if(zoomVal <= 0) {
							return;
						}
					}
				}
			}
			tileNumVisible += zoomVal;
			if (tileNumVisible > (getTotalTileNum())) {
				tileNumVisible = getTotalTileNum();
			}
		}

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		setNumVisible((int) tileNumVisible);

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

		if(!beginFixed && zoomVal != 0) {
			newFirstVis += (zoomsIn ? zoomVal : -zoomVal);
			scrollToFirstIndex(newFirstVis);
		}
	}

	//This is for gradually zooming toward the center of the currently displayed
	//dots
	public void zoomInCenter(final int speed) {

		// final double maxScale = getAvailablePixels();
		double newScale = getScale();
		int zoomVal;

		tileNumVisible = Math.round(getAvailablePixels() / getScale());
		// LogBuffer.println("zoomIn: tileNumVisible has been set to [" +
		// tileNumVisible + "].");
		if(tileNumVisible > (getTotalTileNum())) {
			tileNumVisible = getTotalTileNum();
		}
		
		switch(speed) {
		
		case ZOOM_SLOW: 
			zoomVal = 2;
			break;
		case ZOOM_FAST:
			zoomVal = (int) Math.round(ZOOM_INCREMENT_FAST * tileNumVisible);
			break;
		case ZOOM_SLAM:
			zoomVal = (int) Math.round(tileNumVisible) - 1;
			break;
		default:
			zoomVal = (int) Math.round(ZOOM_INCREMENT * tileNumVisible);
	
		}

		//Ensure that at least 2 tiles will be zoomed in (1 on each side of the
		//current zoom dimension).
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
		setNumVisible((int) tileNumVisible);

		//Ensure that the difference between the previous number visible and the
		//new number visible is even
		if((prevNumVisible - numVisible) % 2 == 1 && numVisible > 1) {
			setNumVisible(numVisible - 1);
			tileNumVisible -= 1;
		}

		// Recalculating scale
		newScale = getAvailablePixels() / tileNumVisible;

		final double myMaxScale = getAvailablePixels();
		if (newScale > myMaxScale) {
			newScale = myMaxScale;
		}
		setScale(newScale);

		int newFirstVisible = firstVisible + (prevNumVisible - numVisible) / 2;
		scrollToFirstIndex(newFirstVisible);

		notifyObservers();
	}

	/*
	 * TODO: Delete this function - it's outdated (though it is more consistent
	 * than its replacements). - Rob
	 */
	//This is for gradually zooming toward a selected block of dots (there's
	//currently no corresponding zoom away function)
	//This function is deprecated. It works, but is a little jerky when it runs
	//into an edge.
	//It has essentially been replaced by smoothZoomTowardSelection in
	//GlobalView and the functions it calls in here called
	//getZoomTowardPixelOfSelection and zoomTowardPixel
	//smoothZoomTowardSelection resides in GlobalView instead of here because it
	//keeps track of the aspect ratio, needing info on both dimensions
	public int zoomToward(int firstIndex,int numIndexes) {
		int updateAspectRatio = 0;
		//Catch errors - If num indexes is less than 1 or greater than the
		//number of pixels available
		if(numIndexes < 1 || numIndexes > getAvailablePixels() ||
		   firstIndex < 0 || (firstIndex + numIndexes - 1) > getMaxIndex()) {
			return(updateAspectRatio);
		}

		int initialFirstVisible = firstVisible;
		double newScale = getScale();
		int zoomVal;
		tileNumVisible = Math.round(getAvailablePixels() / getScale());
		if(tileNumVisible > (getTotalTileNum())) {
			tileNumVisible = getTotalTileNum();
		}
		zoomVal = (int) Math.round(ZOOM_INCREMENT * tileNumVisible);
		// Ensure that at least 2 tiles will be zoomed in (1 on each side of the
		//current zoom dimension).
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
		setNumVisible((int) tileNumVisible);

		//Ensure that the difference between the previous number visible and the
		//new number visible is even
		if((prevNumVisible - numVisible) % 2 == 1 && numVisible > 1) {
			setNumVisible(numVisible - 1);
			tileNumVisible -= 1;
		}

		if (tileNumVisible < numIndexes) {
			tileNumVisible = numIndexes;
			setNumVisible(numIndexes);
		}

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

			//If the difference is odd and greater than 2, the selection isn't
			//going to be quite centered by the time we get to it, so we'll
			//shift things early on by 1 to correct it so it's centered as soon
			//as possible
			if(diff % 2 == 1 && diff > 2) {
				setNumVisible(numVisible - 1);
				tileNumVisible -= 1;
				numIndexes--;
				newScale = getAvailablePixels() / tileNumVisible;
				setScale(newScale);
				diff++;
			}

			//If the first visible index is inside the target area
			if(initialFirstVisible > firstIndex) {
				newFirstVisible = firstIndex;
			}
			//Else if the last visible index is inside the target area
			else if((initialFirstVisible + prevNumVisible - 1) <
			        (firstIndex + numIndexes - 1)) {
				newFirstVisible = (firstIndex + numIndexes) - numVisible;
			}
			//If the left/top side is closer than or equal to half the
			//difference in area sizes
			else if((firstIndex - initialFirstVisible) <= (diff / 2) &&
					((initialFirstVisible + prevNumVisible - 1) -
					 (firstIndex + numIndexes - 1)) > (diff / 2)) {
				newFirstVisible = initialFirstVisible;
				//LogBuffer.println("zoomToward: Left/top is remaining fixed.");

				updateAspectRatio = 1;
			}
			//If the right/bottom side is closer than or equal to half the
			//difference in area sizes
			else if(((initialFirstVisible + prevNumVisible - 1) -
			         (firstIndex + numIndexes - 1)) <= (diff / 2) &&
					(firstIndex - initialFirstVisible) > (diff / 2)) {
				newFirstVisible = (initialFirstVisible + prevNumVisible - 1) -
					numVisible + 1;

				updateAspectRatio = 1;
			} else {
				newFirstVisible = firstIndex - diff / 2;
			}
		}
		scrollToFirstIndex(newFirstVisible);

		notifyObservers();

		return(updateAspectRatio);
	}

	/**
	 * TODO: Make customZoomVal required (no ability to supply -1) and strip out
	 * the code that picks the zoom value - this has been replaced with
	 * getBestZoomIn/OutVal. Merge with zoomAwayPixel.
	 */
	//This function smoothly zooms while trying to keep the dot the cursor is
	//currently over under the cursor.  Some of the code is generalized in case
	//it's ever called with a value out of range
	//Set customZoomVal to a negative number (e.g. -1) to dynamically determine
	//the zoomVal based on a 5% zoom increment
	public int zoomTowardPixel(int pixelPos,int customZoomVal) {
		
		int updateAspectRatio = 0;
		int firstIndex = getIndex(pixelPos);
		//Catch errors - If num indexes is less than 1 or greater than the
		//number of pixels available
		if(firstIndex < 0 || firstIndex > getMaxIndex()) {
			return(updateAspectRatio);
		}

		int dotOver = getIndex(pixelPos);
		int initialFirstVisible = firstVisible;
		double newScale = getScale();
		int zoomVal;
		int pxAvail = getAvailablePixels();
		tileNumVisible = Math.round(pxAvail / newScale);
		if(tileNumVisible > (getTotalTileNum())) {
			tileNumVisible = getTotalTileNum();
		}
		double targetZoomFrac = ZOOM_INCREMENT;

		//This is a test of selecting the best zoom value using the
		//getBestZoomVal method (called from outside of this class)
		if(customZoomVal >= 0) {
			zoomVal = customZoomVal;
		}
		//If a custom zoom value has not been supplied (in order to correct dot
		//aspect ratios (see GlobalView)),
		//select the best zoom value to make the zooming as smooth as possible
		else if(customZoomVal < 0 && tileNumVisible > 15) {
			zoomVal = getBestZoomInVal(pixelPos,targetZoomFrac);
			if (zoomVal < 1) {
				zoomVal = 1;
			}
		}
		//Zooming with no smoothing
		else {
			zoomVal = (int) Math.round(tileNumVisible * targetZoomFrac);

			if (zoomVal < 1) {
				zoomVal = 1;
			}
		}

		//Make sure we're left with at least 1 dot on the screen
		tileNumVisible = tileNumVisible - zoomVal;
		if (tileNumVisible < 1) {
			tileNumVisible = 1;
		}

		// Keep track of explicit changes, by the user, to the amount of
		// visible data
		int prevNumVisible = numVisible;
		setNumVisible((int) tileNumVisible);

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
			//Else if the last visible index is inside the target area (assuming
			//the size of the area we're zooming toward is 1 data cell)
			else if((initialFirstVisible + prevNumVisible - 1) < firstIndex) {
				newFirstVisible = (firstIndex + 1) - numVisible;
			} else {
				newFirstVisible =
					(int) ((double) dotOver + 1 - pixelPos / newScale);

				//If the result of pixelPos / newScale is a whole number,
				//newFirstVisible must be decremented (discovered via trial &
				//error)
				if(newFirstVisible > 0 &&
				   pixelPos / newScale ==
				   (int) (pixelPos / newScale)) {
					newFirstVisible--;
				}

				//If the target has shifted off the left/top side of the screen
				if(newFirstVisible > firstIndex) {
					newFirstVisible = firstIndex;
					updateAspectRatio = 1;
				}
				//If the target cell has shifted off the right/bottom side of
				//the screen
				else if((newFirstVisible + numVisible - 1) < firstIndex) {
					newFirstVisible = firstIndex - (numVisible - 1);
					updateAspectRatio = 1;
				}
			}
		}

		scrollToFirstIndex(newFirstVisible);

		//Catch and correct bad calculations - this is useless now that the
		//whole-number result test was added above
		if(dotOver != getIndex(pixelPos)) {
			int correction = dotOver - getIndex(pixelPos);
			newFirstVisible += correction;
			scrollToFirstIndex(newFirstVisible);
		}

		notifyObservers();

		return(updateAspectRatio);
	}

	/*
	 * TODO: This function SHOULD be able to select a pixel offscreen, though I
	 * have checks early-on to prevent that and return an edge pixel.  Make it
	 * so it can return a negative pixel index or a pixel index larger than max.
	 * - Rob
	 */
	//Use this function to pick a pixel to zoom toward when zooming toward a
	//selection
	//Assumes that the full selection is visible on the screen
	public int getZoomTowardPixelOfSelection(int pixelIndexOfSelec,
		int numPixelsOfSelec) {

		int numPixelsOffsetOfSelec = pixelIndexOfSelec;
		int numTotalPixels = getAvailablePixels();

		//If any of the selected area is above/before the visible area
		if(pixelIndexOfSelec < 0 &&
			(pixelIndexOfSelec + numPixelsOfSelec) < numTotalPixels) {
			//If zooming out, return the furthest pixel, otherwise return the
			//closest pixel
			if(numPixelsOfSelec > numTotalPixels) {
				return(numTotalPixels - 1);
			}
			//LogBuffer.println("Returning Target pixel: [0].");
			return(0);
		}
		//If any of the selected area is below/after the visible area, return
		//the first pixel so that
		else if((pixelIndexOfSelec + numPixelsOfSelec) > numTotalPixels &&
			pixelIndexOfSelec > 0) {
			//If zooming out, return the furthest pixel, otherwise return the
			//closest pixel
			if(numPixelsOfSelec > numTotalPixels) {
				//LogBuffer.println("Returning Target pixel: [0].");
				return(0);
			}
			return(numTotalPixels - 1);
		}

		//The following equation is based on the merging of 2 equations and then
		//solving for targetPixel
		//Let's say that numPixelsOffsetOfSelec is the number of pixels above/
		//left of the start of the selection
		//Let's say that numPixelsOfSelec is the number of pixels in the
		//selected box (in the current dimension)
		//We want the pixel where these fractions are equal:
		//targetPixelRelativeToSelection / numPixelsOfSelec = targetPixel /
		//numTotalPixels
		//We know that targetPixel = numPixelsOffsetOfSelec +
		//targetPixelRelativeToSelection
		//Solve for targetPixelRelativeToSelection = targetPixel -
		//numPixelsOffsetOfSelec
		//Substitute targetPixelRelativeToSelection with targetPixel -
		//numPixelsOffsetOfSelec in the first selection and solve for
		//targetPixel and you get the equation below
		int targetPixel = (int) Math.round((double) numTotalPixels *
		                                   (double) numPixelsOffsetOfSelec /
		                                   ((double) numPixelsOfSelec -
		                                    (double) numTotalPixels));
		//LogBuffer.println("Target Pixel calculation: targetPixel = " +
		//	"numTotalPixels * numPixelsOffsetOfSelec / (numPixelsOfSelec - " +
		//	"numTotalPixels)");
		//LogBuffer.println("Target Pixel calculation: " + targetPixel + " = " +
		//	numTotalPixels + " * " + numPixelsOffsetOfSelec + " / (" +
		//	numPixelsOfSelec + " - " + numTotalPixels + ")");
		
		//The equation above actually has 2 solutions.  If the solution is
		//negative, we just need to negate it, otherwise, the solution turns out
		//to need a slight adjustment because it is on the outside of the
		//selected area.
		//I'm not sure why, but the difference with the offset just needs to be
		//added to the offset.
		if(targetPixel < 0) {
			//LogBuffer.println("Negating Target pixel: [" + targetPixel +"].");
			targetPixel = Math.abs(targetPixel);
		} else {
			targetPixel = numPixelsOffsetOfSelec +
				Math.abs(numPixelsOffsetOfSelec-targetPixel);
		}

		//Decrement because pixels are indexed from 0 and the equation above
		//assumes indexing from 1
		targetPixel--;

		if(targetPixel < 0 || targetPixel >= numTotalPixels) {
			targetPixel = 0;
		}

		//LogBuffer.println("Returning Target pixel: [" + targetPixel + "].");
		return(targetPixel);
	}

	/**
	 * TODO: Make customZoomVal required (no ability to supply -1) and strip out
	 * the code that picks the zoom value - this has been replaced with
	 * getBestZoomIn/OutVal. Merge with zoomTowardPixel.
	 */
	//This function does the reverse of zoomTowardPixel
	//Some of the code is generalized in case it's ever called with a value out
	//of range
	public int zoomAwayPixel(int pixelPos,int customZoomVal) {
		int updateAspectRatio = 0;
		int firstIndex = getIndex(pixelPos);
		//Catch errors - If num indexes is less than 1 or greater than the
		//number of pixels available
		if(firstIndex < 0 || firstIndex > getMaxIndex()) {
			LogBuffer.println("ERROR: Either firstIndex [" + firstIndex +
			                  "] derived from pixelIndex [" + pixelPos +
			                  " out of " + getAvailablePixels() +
			                  " available] is less than 0 or greater than " +
			                  "maxIndex [" + getMaxIndex() + "]");
			return(updateAspectRatio);
		}

		int dotOver = getIndex(pixelPos);
		int initialFirstVisible = firstVisible;
		double newScale = getScale();
		int zoomVal;
		int pxAvail = getAvailablePixels();
		tileNumVisible = Math.round(pxAvail / newScale);
		if(tileNumVisible > (getTotalTileNum())) {
			tileNumVisible = getTotalTileNum();
		}
		double targetZoomFrac = ZOOM_INCREMENT;

		//This is a test of selecting the best zoom value using the
		//getBestZoomVal method (called from outside of this class)
		if(customZoomVal >= 0) {
			zoomVal = customZoomVal;
		}
		//If a custom zoom value has not been supplied (in order to correct dot
		//aspect ratios (see GlobalView)), select the best zoom value to make
		//the zooming as smooth as possible
		else if(customZoomVal < 0 && tileNumVisible >= 15) {
			zoomVal = getBestZoomOutVal(pixelPos,targetZoomFrac);
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
		if (tileNumVisible > (getTotalTileNum())) {
			tileNumVisible = getTotalTileNum();
			updateAspectRatio = 1;
		}

		//Keep track of explicit changes, by the user, to the amount of visible
		//data
		int prevNumVisible = numVisible;
		setNumVisible((int) tileNumVisible);

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
			}
			//Else if the last visible index is inside the target area
			else if((initialFirstVisible + prevNumVisible - 1) < firstIndex) {
				newFirstVisible = (firstIndex + 1) - numVisible;
			} else {
				newFirstVisible =
					(int) ((double) dotOver + 1 - pixelPos / newScale);

				//If the result of pixelPos / newScale is a whole number,
				//newFirstVisible must be decremented (discovered via trial &
				//error)
				if(newFirstVisible > 0 &&
				   pixelPos / newScale ==
				   (int) (pixelPos / newScale)) {
					newFirstVisible--;
				}

				//Make sure we're not past what is possible to scroll to
				if((newFirstVisible + numVisible - 1) > getMaxIndex()) {
					newFirstVisible = getMaxIndex() - numVisible + 1;
				}

				//If the left/top side is closer than or equal to half the
				//difference in area sizes
				if(newFirstVisible > firstIndex) {
					newFirstVisible = firstIndex;
					updateAspectRatio = 1;
				}
				//If the right/bottom side is closer than or equal to half the
				//difference in area sizes
				else if((newFirstVisible + numVisible - 1) < firstIndex) {
					newFirstVisible = firstIndex - (numVisible - 1);
					updateAspectRatio = 1;
				}
			}
		}

		if(newFirstVisible != getFirstVisible())
			scrollToFirstIndex(newFirstVisible);

		int correction = dotOver - getIndex(pixelPos);
		//Catch and correct bad calculations - this is useless now that the
		//whole-number result test was added above
		if(correction != 0 && !(newFirstVisible == 0 && correction < 0) &&
		   !((newFirstVisible + numVisible - 1) > getMaxIndex()) &&
		   correction > 0) {
			newFirstVisible += correction;

			//Make sure we're not past what is possible to scroll to
			if((newFirstVisible + numVisible - 1) > getMaxIndex()) {
				newFirstVisible = getMaxIndex() - numVisible + 1;
			}

			LogBuffer.println("WARNING: The data cell hovered over has " +
				"shifted. It was over [" + dotOver + "].  Now it is over: [" +
				getIndex(pixelPos) + "].  Previous dotOver calculation: " +
				"[firstVisible + (int) ((double) pixelPos / newScale))] = [" +
				firstVisible + " + (int) ((double) " + pixelPos + " / " +
				newScale + "))].  Correcting this retroactively...");
			scrollToFirstIndex(newFirstVisible/*,true*/);
			updateAspectRatio = 1;
		}

		notifyObservers();

		return(updateAspectRatio);
	}

	/*
	 * TODO: Merge this function with getBestZoomOutVal - there are only subtle
	 * differences that could be handled in an if statement. - Rob
	 */
	//This calculates an optimal number of data indexes to remove that will
	//result in as smooth a zoom as possible.
	//It uses relative data index hovered over to calculate the target position
	//of the zoom (as opposed to the relative pixel position of the cursor)
	public int getBestZoomInVal(int pixel,double targetZoomFrac) {
		
		int cells = getNumVisible();
		//If the targetZoomFrac is 1.0, return the remainder of this dimension
		if((targetZoomFrac % 1) == 0 &&
			((int) Math.round(targetZoomFrac)) == 1) {
			return(getTotalTileNum() - cells);
		}
		int zoomVal = (int) Math.round(cells * targetZoomFrac);
		//int numPixels = getAvailablePixels();

		//If we're at the minimum zoom level, do not zoom in any more
		if((cells == 1 && targetZoomFrac <= 1) || (targetZoomFrac % 1 == 0) &&
			((int) Math.round(targetZoomFrac)) == 0) {
			return(0);
		}
		if (zoomVal < 2) {
			zoomVal = 2;
		}
		//If the amount of zoom is larger than the number of cells currently
		//visible
		if (zoomVal > cells) {
			zoomVal = cells - 1;
		}
		int smallThresh = 15;
		int adjustWindow = 10;
		if(cells <= smallThresh) {
			adjustWindow = 3;
		}
		int bestZoomVal = zoomVal;
		//Select the best zoom value within a range of 5 of the target zoom val
		//to make the zooming as smooth as possible
		int zoomMin = zoomVal - adjustWindow;
		if(zoomMin < 0) {
			zoomMin = 0;
		}
		//if(zoomMin > cells) {
		//	zoomMin = cells - 1;
		//}
		int zoomMax = zoomVal + adjustWindow;
		if((cells - zoomMax) > (getTotalTileNum())) {
			zoomMax = getTotalTileNum() - cells;
		}

		//Sort the range of zoom values such that the target zoomVal is first,
		//then +1, -1, +2, -2,...
		//We're arbitrarily using the target zoom value to start out - the sort
		//is just a heuristic anyway - doesn't really matter that much other
		//than to prefer the target values
		double minDiff = 1.0;
		int[] zoomRange = new int[zoomMax - zoomMin + 2]; // 2 was just to be on
															// the safe side.
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

		
		double minDiffThresh = 0.005;
		//0=best possible ratio. "Good enough value" for when the ratio of the
		//relative cell position of the cell under the cursor is really close to
		//the ratio of the relative pixel position of the cursor

		//The corresponding zoom-out function uses relative pixel position, but
		//on zoom-in, it's more accurate to use the relative cell position the
		//mouse is over
		//double targetFrac = ((double) getIndex(pixel) + 1.0) /
		//		((double) getTotalTileNum());
		double targetFrac = (getIndex(pixel) + 1.0 -
				firstVisible) / (numVisible);
		double diff = 0.0;
		//LogBuffer.println("getBestZoomInVal: minZoom: [" + zoomMin +
		//		"] maxZoom: [" + zoomMax + "].");
		//Loop through the zoomVals and find which one will result in a ratio
		//with the relative index that is closest to the pixelPos to pxAvail
		//ratio
		for(i = 0;i <= (zoomMax - zoomMin);i++) {
			int z = zoomRange[i];
			int relCell = (int) (targetFrac * z);
			//LogBuffer.println("getBestZoomInVal: [relCell = (int) (pixel / " +
			//		"numPixels * z)] = [" + relCell + " = (int) (" + pixel +
			//		" / " + numPixels + " * " + z + ")].");
			if(z == 0) continue;
			diff = Math.abs(((double) relCell / (double) z) - targetFrac);
			//LogBuffer.println("getBestZoomInVal: [diff = Math.abs((double) " +
			//		"(relCell / z) - (double) (pixel / numPixels))] = [" +
			//		diff + " = Math.abs((double) (" + relCell + " / " + z +
			//		") - (double) (" + pixel + " / " + numPixels + ")].");
			if(diff < minDiff) {
				bestZoomVal = z;
				minDiff = diff;
				//Stop if the difference from the target is "good enough",
				//i.e. close to zero
				if(minDiff < minDiffThresh) {
					break;
				}
			}
		}
		//LogBuffer.println("getBestZoomInVal: Selected zoomVal [" +
		//		bestZoomVal + "] instead of defaults [" + zoomVal +
		//		"] Diff: [" + diff + "].");
		
		//Do not force a zoom amount here because a minimum zoom should be
		//enforced after this has been called
		if(bestZoomVal < 0) bestZoomVal = 0;
		return(bestZoomVal);
	}

	/*
	 * TODO: Merge this function with getBestZoomInVal - there are only subtle
	 * differences that could be handled in an if statement. - Rob
	 */
	//This function performs the reverse of getBestZoomInVal, except it uses the
	//relative pixel position of the cursor to calculate the zoom position that
	//is targeted for smoothing
	public int getBestZoomOutVal(int pixel,double targetZoomFrac) {
		
		int cells = getNumVisible();
		//If the targetZoomFrac is 1.0, return the remainder of this dimension
		if((targetZoomFrac % 1) == 0 &&
		   ((int) Math.round(targetZoomFrac)) == 1) {
			return(getTotalTileNum() - cells);
		}
		//int zoomVal = (int) Math.round((double) cells * targetZoomFrac);
		int zoomVal = (int) Math.round(cells /
				(1 - targetZoomFrac) - cells);
		int numPixels = getAvailablePixels();
		//LogBuffer.println("getBestZoomOutVal: Called with pixel [" + pixel +
		//		"] and targetZoomFrac [" + targetZoomFrac +
		//		"] resulting in target zoomVal [" + zoomVal + "].");

		//If the closest zoom amount is 0, return 0 because it's the smoothest
		//possible scroll value (and the math below will result in NaN)
		//The calling function is expected to handle cases resulting in no zoom
		if(zoomVal < 1) {
			return(0);
		}
		//If the resulting zoom level is bigger than or equal to full zoom-out
		if((zoomVal + cells) >= (getTotalTileNum())) {
			zoomVal = getTotalTileNum() - cells;
			return(zoomVal);
		}
		int smallThresh = 15;
		int adjustWindow = 7;
		//If we're getting close to full zoom-out, search a smaller window so
		//that the last bit of zoom-out isn't likely to be all on 1 axis
		if(((getTotalTileNum()) - cells) <= smallThresh) {
			adjustWindow = 3;
		}
		int bestZoomVal = zoomVal;
		//Select the best zoom value within a range of 5 of the target zoom val
		//to make the zooming as smooth as possible
		int zoomMin = zoomVal - adjustWindow;
		if(zoomMin < 1) {
			zoomMin = 1;
		}
		//If the minimum number of cells to zoom is larger than the current
		//number of cells displayed on this axis - this mitigates occurrences of
		//"zoom jumping"
		//if(zoomMin > cells) {
		//	zoomMin = cells - 1;
		//}
		//If the resulting zoom max is bigger than the max zoom-out
		int zoomMax = zoomVal + adjustWindow;
		if((zoomMax + cells) > (getTotalTileNum())) {
			zoomMax = getTotalTileNum() - cells;
		}

		//Sort the range of zoom values such that the target zoomVal is first,
		//then +1, -1, +2, -2,...
		//We're arbitrarily using the target zoom value to start out - the sort
		//is just a heuristic anyway - doesn't really matter that much other
		//than to prefer the target values
		double minDiff = 1.0;
		//LogBuffer.println("Creating array of size zoomMax - zoomMin + 2: [" +
		//		zoomMax + " - " + zoomMin + " + 2].");
		int[] zoomRange = new int[zoomMax - zoomMin + 2];	//2 was just to be
		int i = 0;											//on the safe side.
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

		
		double minDiffThresh = 0.005;	//0=best possible ratio. "Good enough
										//value" for when the ratio of the
										//relative cell position of the cell
										//under the cursor is really close to
										//the ratio of the relative pixel
										//position of the cursor

		//LogBuffer.println("getBestZoomOutVal: minZoom: [" + zoomMin +
		//		"] maxZoom: [" + zoomMax + "].");
		double diff = 0.0;
		//Loop through the zoomVals and find which one will result in a ratio
		//with the relative index that is closest to the pixelPos to pxAvail
		//ratio
		for(i = 0;i <= (zoomMax - zoomMin);i++) {
			int z = zoomRange[i];
			int relCell = (int) ((double) (pixel + 1) /
					(double) numPixels * z);
			//LogBuffer.println("getBestZoomOutVal: [relCell = (int) (pixel " +
			//		"/ numPixels * z)] = [" + relCell + " = (int) (" + pixel +
			//		" / " + numPixels + " * " + z + ")].");
			if(z == 0) continue;
			diff = Math.abs(((double) relCell / (double) z) -
					((double) (pixel + 1) / (double) numPixels));
			//LogBuffer.println("getBestZoomOutVal: [diff = relCell / z - " +
			//		"pixel / numPixels] = [" +
			//		diff + " = " + relCell + " / " + z + " - " + pixel + " / " +
			//		numPixels + "]. target zoomVal: [" + zoomVal +
			//		"] targetZoomFrac: [" + targetZoomFrac + "].");
			if(diff < minDiff) {
				bestZoomVal = z;
				minDiff = diff;
				//Stop if the difference from the target is "good enough",
				//i.e. close to zero
				if(minDiff < minDiffThresh) {
					break;
				}
			}
		}

		//Do not force a zoom amount here because a minimum zoom should be
		//enforced after this has been called
		if(bestZoomVal < 0) bestZoomVal = 0;
		//LogBuffer.println("getBestZoomOutVal: Selected zoomVal [" +
		//bestZoomVal + "] instead of default [" + zoomVal +
		//"] Difference in smoothness accuracy: [" + diff + "].");
		return(bestZoomVal);
	}

	/**
	 * This function calculates the fraction by which the zoom increment should
	 * be if you want to draw increments in an animation (e.g. an animated
	 * zoom).  Drawing large matrices takes a lot of time, thus animations
	 * should zoom more when there are large amounts of squares to zoom
	 * through
	 */
	public double getOptimalZoomIncrement(int targetNumIndexes,
										  boolean zoomingOut) {
		//double maxZoomFrac = 0.5;
		double maxZoomFrac = 0.6;
		if(ZOOM_INCREMENT > maxZoomFrac) {
			return(maxZoomFrac);
		}
		int maxZF = (int) Math.round(maxZoomFrac / ZOOM_INCREMENT);
		double targetZoomFrac = maxZoomFrac;
		//The following loop cycles through zoom increments 0.05,0.1,0.15,...
		//to 0.5
		//and stops when the zoom increment is large enough to overcome the
		//long time it takes to draw huge matrices
		//It uses an equation that was solved using these 2 equations (forcing
		//a logarithmic curve to pass through 2 points)
		//6000 = a * b^0.5 and 100 = a * b^0.05
		//where
		//double a = 63.45; //6000 = a * b^0.5 and 100 = a * b^0.05
		//double b = 8942; //6000 = a * b^0.5 and 100 = a * b^0.05
		double a = 68.39; // 6000 = a * b^0.6 and 100 = a * b^0.05
		double b = 1732; // 6000 = a * b^0.6 and 100 = a * b^0.05
		//Basically, when the number of visible spots is 6000, the zoom
		//increment will be 50%.
		//When the number of visible spots is 100, the zoom increment will be
		//5%. There's a log scale in between.
		int largerXSize = getNumVisible();
		//if(targetNumIndexes > largerXSize) {
		//	largerXSize = targetNumIndexes;
		//}
		for(int zf = 1;zf<=maxZF;zf++) {
		//for(int zf = maxZF;zf > 0;zf--) {
			double zmfc = zf * ZOOM_INCREMENT;
			if(a * Math.pow(b,zmfc) >= largerXSize &&
					//Only select this zoom fraction if the next zoom fraction
					//will be larger - this is to reduce the number of steps in
					//large zoom changes
					a*Math.pow(b,zmfc) < (largerXSize +
							(zf + 1) * ZOOM_INCREMENT * largerXSize)) {
			//if(a*Math.pow(b,zmfc) <= largerXSize) {
				targetZoomFrac = zmfc;
				if(zoomingOut) targetZoomFrac += ZOOM_INCREMENT;
				break;
			}
			//targetZoomFrac = zmfc;
		}
		if(targetZoomFrac > maxZoomFrac) {
			targetZoomFrac = maxZoomFrac;
		}
		//If this step size is larger(zoomout) or smaller(zoomin) than what's
		//remaining for the next step, just go all the way
		//This is an approximation because I should actually be using the next
		//step, not this one
		if((zoomingOut &&
				targetZoomFrac * largerXSize >
				Math.abs(largerXSize + 1 * 
							(int) Math.round(targetZoomFrac * largerXSize) -
				targetNumIndexes))) {
			//The above seems only necessary for zoom out (for speed)
			// ||
			//(!zoomingOut && targetZoomFrac * largerXSize <
			//Math.abs(largerXSize + (zoomingOut ? 1 : -1) *
			//(int) Math.round(targetZoomFrac * largerXSize) -
			//targetNumIndexes))) {
			//LogBuffer.println("Slamming to target");
			targetZoomFrac = 1;
		}
		return(targetZoomFrac);
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
	 * @param selectedMin
	 *            Minimum index of selected area.
	 * @param selectedMax
	 *            Maximum index of selected area.
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
		 */
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
			else if((selectedMin + numSelected / 2) > (getMaxIndex() 
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
			int numArrayIndexes = (int) Math.round(numSelected);
			if(numArrayIndexes > (getTotalTileNum())) {
				numArrayIndexes = getTotalTileNum();
			}
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
		
		int scrollVal = (int) Math.round((endIndex + startIndex) / 2.0);
		
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
	 * to adjust the tile scale to the screen size and then re-scrolls to the
	 * position currently in focus.
	 */
	public void adjustScaleToScreen() {
		
		if(numVisible == 0) {
			setNumVisible(getTotalTileNum());
		}

		double newScale = (double) getAvailablePixels() / (double) numVisible;
		setScale(newScale);

		notifyObservers();
	}

	public void adjustScrollToScreen() {

		if (getNumVisible() > 0) {
			scrollToFirstIndex(getFirstVisible()/*,true*/);
		} else {
			setFirstVisible(0);
			scrollToFirstIndex(0);
		}

		notifyObservers();
	}

	public void adjustToScreenChange() {
		
		adjustScaleToScreen();
		adjustScrollToScreen();
	}

	public void recalculateScale() {

		if (nodeHasAttribute("FixedMap", "scale")) {
			if (getScale() < getAvailablePixels()) {
				return;
			}
		}

		// The divisor here was previously calculated by:
		// getMaxIndex() - getMinIndex() + 1, which inadvertently resulted
		// in zooming out the currently viewed area, however this function
		// was being called in contexts where the user was not intending
		// to do that, so instead, I changed the divisor to be the number
		// of data indexes currently being viewed. -Rob
		//LogBuffer.println("recalculateScale: numVisible: [" + tileNumVisible
		//		+ "] was used to calculate requiredScale.");
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

	public IntegerMap setMap(final int type) {

		if (current.equalsType(type)) {
			return current;
		}

		IntegerMap newMap = null;
		if (nullMap.equalsType(type)) {
			newMap = nullMap;

		} else if (fillMap.equalsType(type)) {
			newMap = fillMap;

		} else if (fixedMap.equalsType(type)) {
			newMap = fixedMap;
		}

		if (newMap == null) {
			LogBuffer.println("Couldn't find map matching type " + type
					+ " in MapContainer.java");
			LogBuffer.println("Choices include");
			LogBuffer.println(nullMap.typeName());
			LogBuffer.println(fixedMap.typeName());
			LogBuffer.println(fillMap.typeName());
			newMap = fixedMap;
		}

		switchMap(newMap);
		return current;
	}

	/*
	 * Scrollbar Functions
	 * TODO This method is broken and breaks scrolling when out of range values
	 * are created. 
	 */
	public void scrollToIndex(final int i) {

		final int j = scrollbar.getValue();

		// The getVisibleAmount return value can change by resizing the window,
		// so use getNumVisible instead
		// This assumes that getNumVisible is updated before this function is
		// called.
		// scrollbar.setValue(i - scrollbar.getVisibleAmount() / 2);

		scrollbar.setValue(i - getNumVisible() / 2);

		// Keep track of the first visible index
		// This used to be set using scrollbar.getVisibleAmount, but that can
		// change implicitly when the window is resized.
		setFirstVisible(i - getNumVisible() / 2);

		//Image needs to be updated if either scroll position changes (because a
		//scroll of the labels changes the blue box)
		if(j != scrollbar.getValue() /* || k != getFirstVisibleLabel() */) {
			setChanged();
		}
	}

	public void scrollToFirstIndex(final int i) {

		if(i < getMinIndex() || i > getMaxIndex()) {
			LogBuffer.println("Cannot set first index to " + i);
			return;
		}
		
		final int j = scrollbar.getValue();

		scrollbar.setValue(i);

		// Keep track of the first visible index
		setFirstVisible(i);

		if (j != scrollbar.getValue()) {
			setChanged();
		}

		notifyObservers();
	}

	/**
	 * Set a new value for the MapContainer's associated scrollbar. The supplied
	 * parameter will be added to the current scrollbar value. If it 
	 * is positive, the value will increase. If negative the scrollbar value
	 * will decrease. No matter what value is supplied, the scrollbar can 
	 * never be set to scroll such that the range of visible tiles 
	 * in MapContainer ends up below its minimum index or above 
	 * its maximum index. 
	 * @param i The value by which the current scrollbar value is changed.
	 */
	public void scrollBy(final int i) {

		if(showsAllTiles()) {
			return;
		}
		
		final int j = scrollbar.getValue();
		int newVal = j + i; // is firstVisible not last... -> error

		// out of range
		if(newVal < scrollbar.getMinimum() ) {
			newVal = scrollbar.getMinimum();
			
		} else if(newVal + getNumVisible() > scrollbar.getMaximum()) {
			newVal = scrollbar.getMaximum() - getNumVisible();
		}
		
		scrollbar.setValue(newVal);

		// Keep track of the first visible index
		setFirstVisible(newVal);

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
	/** TODO: See if this needs to update firstVisibleLabel */
	@Override
	public void adjustmentValueChanged(final AdjustmentEvent adjustmentEvent) {

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

	/**
	 * Get the first pixels belonging to the tile which represents the given
	 * index.
	 * @param i The index for which to retrieve the pixel.
	 * @return A pixel index.
	 */
	public int getPixel(final int i) {

		int offset = 0;
		if (scrollbar != null) {
			offset = scrollbar.getValue();
		}

		return current.getPixel(i - offset);
	}

	/**
	 * Uses the MapContainers assigned IntegerMap object to figure out what
	 * index a certain pixel belongs to.
	 * @param pixel The pixel to be checked.
	 * @return The index of the tile to which the given pixel belongs.
	 */
	public int getIndex(final int pixel) {

		int index = -1;
		if (current != null) {
			index = current.getIndex(pixel);
		}

		if (scrollbar != null) {
			index += scrollbar.getValue();
		}

		return index;
	}

	/**
	 * This checks if an index is currently in the range of visible pixels.
	 * @param i The index to be checked.
	 * @return Whether an index is within range of visible screen pixels.
	 */
	public boolean isVisible(final int i) {

		final int min = getIndex(0);
		final int max = getIndex(getAvailablePixels());
		
		if (i < min || i > max) {
			return false;
		}

		return true;
	}

	/**
	 * @return The number of required pixels to fill out the index range of the
	 * MapContainer's IntegerMap at the currently set tile scale.
	 */
	public int getRequiredPixels() {

		return current.getRequiredPixels();
	}

	/**
	 * @return How many pixels of the MapContainer's IntegerMap are actually 
	 * used/ filled out by tiles.
	 */
	public int getUsedPixels() {

		return current.getUsedPixels();
	}

	/**
	 * Tells MapContainer's IntegerMap how many pixels are available on the
	 * screen. 
	 * @param i The number of pixels available for 
	 * the MapContainer's IntegerMap (axis).
	 */
	public void setAvailablePixels(final int i) {

		final int j = current.getUsedPixels();
		current.setAvailablePixels(i);
		setupScrollbar();

		if (j != current.getUsedPixels()) {
			setChanged();
		}
	}

	/**
	 * Set a new index range for the MapContainer's IntegerMap.
	 * @param i The new minimum index.
	 * @param j The new maximum index.
	 */
	public void setIndexRange(int i, int j) {
		
		if (i > j) {
			final int k = i;
			i = j;
			j = k;
		}

		if (current.getMinIndex() != i || current.getMaxIndex() != j) {
			current.setIndexRange(i, j);
			setupScrollbar();
			setNumVisible(j + 1);
			setChanged();
		}
	}

	/**
	 * Setting the axis size per tile in pixels for this MapContainer.
	 * @param d The new axis size per tile in pixels.
	 */
	public void setScale(final double d) {

		if (!Helper.nearlyEqual(fixedMap.getScale(), d)) {
			fixedMap.setScale(d);
			setupScrollbar();
			setChanged();

			configNode.putDouble("scale", d);
			configNode.putDouble("minScale", minScale);
		}
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

		if(i > getTotalTileNum()) {
			numVisible = getTotalTileNum();
			LogBuffer.println("Setting numVisible for [" + mapName + "] to max [" + numVisible + "] because i was too big: [" + i + "]");
		} else if(i < 1) {
			numVisible = 1;
			LogBuffer.println("Setting numVisible for [" + mapName + "] to [" + numVisible + "]");
		} else {
			numVisible = i;
			LogBuffer.println("Setting numVisible for [" + mapName + "] to [" + numVisible + "]");
		}
//		//If the number of visible squares has dipped below the number of
//		//visible labels
//		if(i < getNumVisibleLabels()) {
//			setNumVisibleLabels(i);
//		}
	}

	public void setFirstVisible(final int i) {
		if (i >= 0 && i < getTotalTileNum()) {
			firstVisible = i;
			LogBuffer.println("Setting firstVisible for [" + mapName + "] to [" + firstVisible + "]");
			return;
		} else {
			LogBuffer.println("Did not set firstVisible for [" + mapName + "] because the value was out of range: [" + i + "]");
		}
	}

	public int getMiddlePixel(final int i) {

		return (getPixel(i) + getPixel(i + 1)) / 2;
	}
	
	/**
	 * Gives information about the total number of currently assigned to its
	 * MapContainer object.
	 * @return
	 */
	public int getTotalTileNum() {
		
		return getMaxIndex() + 1;
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
	 * The purpose of these two functions (getNumVisible and getFirstVisible),
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
		return(numVisible);
	}

	public int getFirstVisible() {
		return(firstVisible);
	}

	public int getLastVisible() {
		int lastVisible = getFirstVisible() + getNumVisible() - 1;
		/** TODO: For some undetermined reason, the above sometimes yields an
		 * out of bounds number. I suspect that it has something to do with
		 * multiple cells under a single pixel, but I'm not sure. For now, this
		 * work-around will prevent exceptions. Figure this out & fix it
		 * eventually. */
		if(lastVisible > getMaxIndex()) {
			lastVisible = getMaxIndex();
		}
		return(lastVisible);
	}

	private void switchMap(final IntegerMap integerMap) {

		if (current != integerMap) {
			if (configNode != null) {
				configNode.putInt("current", integerMap.type());
			}
			integerMap.setAvailablePixels(current.getAvailablePixels());
			integerMap.setIndexRange(current.getMinIndex(),
					current.getMaxIndex());
			current = integerMap;
			setupScrollbar();
			// Added this, but took it out because it was to fix something that
			// previously wasn't broken, so instead of try to patch it, I'm
			// going to check out the old code to see what went wrong
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

	/**
	 * @param overLabels the overLabels to set
	 */
	public void setOverLabels(boolean overLabels) {
		
		this.overLabels = overLabels;
		setChanged();
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @param overLabels the overLabels to set
	 */
	public void setOverLabelsScrollbar(boolean overLabels) {
		
		this.overLabelsScrollbar = overLabels;
		setChanged(); // TODO why does the mapcontainer need to update here?
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @param overLabels the overLabels to set
	 */
	public void setLabelsBeingScrolled(boolean overLabels) {
		
		this.labelsBeingScrolled = overLabels;
		setChanged();
		setHoverChanged();
		notifyObservers(); //calls recalculateOverlay for IMV & GMV... should'nt
	}

	/**
	 * @return boolean
	 */
	public boolean areLabelsBeingScrolled() {
		
		return(labelsBeingScrolled);
	}

	/**
	 * @param overTree the overTree to set
	 */
	public void setOverTree(boolean overTree) {
		
		this.overTree = overTree;
		unsetHoverTreeMinIndex();
		unsetHoverTreeMaxIndex();
		setChanged();
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @param overDivider the overDivider to set
	 */
	public void setOverDivider(boolean overDivider) {
		
		this.overDivider = overDivider;
		setChanged();
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @author rleach
	 * @return the draggingDivider
	 */
	public boolean isDraggingDivider() {
		return(draggingDivider);
	}

	/**
	 * @author rleach
	 * @param draggingDivider the draggingDivider to set
	 */
	public void setDraggingDivider(boolean draggingDivider) {
		this.draggingDivider = draggingDivider;
	}

	/**
	 * @param overInteractiveMatrix the overInteractiveMatrix to set
	 */
	public void setOverInteractiveMatrix(boolean overInteractiveMatrix) {
		
		this.overInteractiveMatrix = overInteractiveMatrix;
		setChanged(); // see above
		setHoverChanged();
		notifyObservers(); // see above...
	}

	public boolean isOverIMV() {
		
		return(overInteractiveMatrix);
	}

	//This function is used to determine whether the label port should be drawn
	//or not.  When the mouse is hovered over a pane that activates the label
	//port or if a scrollbar for such a pane is being scrolled, or a selection
	//is in the process of being made, the label port is active.  These things
	//must be tracked here in the data model because it's the common point of
	//reference for all the classes in the view and is their only way to
	//communicate with one another.
	public boolean overALabelPortLinkedView() {
		debug("overALabelPortLinkedView: overLabels [" +
			(overLabels ? "true" : "false") + "] overInteractiveMatrix [" +
			(overInteractiveMatrix ? "true" : "false") +
			"] overLabelsScrollbar [" +
			(overLabelsScrollbar ? "true" : "false") +
			"] labelsBeingScrolled [" +
			(labelsBeingScrolled ? "true" : "false") +
			"] selecting [" + (selecting ? "true" : "false") +
			"] overTree [" + (overTree ? "true" : "false") + "] overDivider [" +
			(overDivider ? "true" : "false") + "] draggingDivider [" +
			(draggingDivider ? "true" : "false") + "]",1);
		return(overLabels || overInteractiveMatrix || overLabelsScrollbar ||
			labelsBeingScrolled || selecting || overTree || overDivider ||
			draggingDivider);
	}

	/**
	 * @return the overRowLabels
	 */
	public boolean isOverLabels() {
		
		return overLabels;
	}

	/* This variable is used and managed from LabelView because running the
	 * repaints on a timer is much smoother and snappier than via
	 * notifyObservers */
	//TODO only declare member variable before constructor.
	private boolean labelAnimeRunning = false;
	public void setLabelAnimeRunning(boolean state) {
		
		labelAnimeRunning = state;
	}
	
	public boolean isLabelAnimeRunning() {
		
		return(labelAnimeRunning);
	}

	/**
	 * @return the hoverIndex
	 */
	public int getHoverIndex() {
		
		return hoverIndex;
	}

	/**
	 * @param hoverIndex the hoverIndex to set
	 */
	public void setHoverIndex(int hoverIndex) {
		
		this.hoverIndex = hoverIndex;
		//setHoverChanged prevents the IMV (and other views) from repainting
		//when no visual change has occurred (i.e. it's just a change in hover
		//position of the mouse.  setChanged is called above though so that
		//LabelView's update will be called

		//Commented this out because this method is not called from the mouse
		//wheel listener
//		setHoverChanged();

		//If LabelView is not controlling its own repaints anymore, start it
		//back up with a call to notifyObservers
		if(!labelAnimeRunning && overALabelPortLinkedView()) {
			setChanged();
			notifyObservers();
		}
	}

	/**
	 * This sets the hoverIndex to a value indicating that the cursor is not
	 * hovered over a position corresponding to a visible data index
	 * @author rleach
	 * @param none
	 * @return nothing
	 */
	public void unsetHoverIndex() {
		
		this.hoverIndex = -1;
	}

	public int getHoverPixel() {
		
		return(hoverPixel);
	}

	public void setHoverPixel(int pixelIndex) {
		
		hoverPixel = pixelIndex;
	}

	public void unsetHoverPixel() {
		
		hoverPixel = -1;
	}

	/**
	 * This is used to decide to not actually repaint because all that changed
	 * is the hover position of the mouse - however, that DOES trigger a change
	 * in the LabelView classes
	 * TODO maybe setChanged & notify shouldn't be triggered on simple hovering
	 * @return hoverChanged
	 */
	public boolean hoverChanged() {
		
		return(hoverChanged);
	}

	/**
	 * This is used to distinguish between hover updates and actual visual
	 * change updates - The LabelView classes views do change on hover changed
	 */
	public void setHoverChanged() {
		
		hoverChanged = true;
	}

	/**
	 * This is used to distinguish between hover updates and actual visual
	 * change updates - The LabelView classes views do change on hover changed
	 */
	public void unsetHoverChanged() {
		
		hoverChanged = false;
	}

	/**
	 * @author rleach
	 * @return the hoverTreeMinIndex
	 */
	public int getHoverTreeMinIndex() {
		return(hoverTreeMinIndex);
	}

	/**
	 * @author rleach
	 * @param hoverTreeMinIndex the hoverTreeMinIndex to set
	 */
	public void setHoverTreeMinIndex(int hoverTreeMinIndex) {
		this.hoverTreeMinIndex = hoverTreeMinIndex;
		debug("Setting new tree min index hover to [" + hoverTreeMinIndex + "].",18);
		setChanged();
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @author rleach
	 */
	public void unsetHoverTreeMinIndex() {
		this.hoverTreeMinIndex = -1;
		setChanged();
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @author rleach
	 * @return the hoverTreeMaxIndex
	 */
	public int getHoverTreeMaxIndex() {
		return(hoverTreeMaxIndex);
	}

	/**
	 * @author rleach
	 * @param hoverTreeMaxIndex the hoverTreeMaxIndex to set
	 */
	public void setHoverTreeMaxIndex(int hoverTreeMaxIndex) {
		this.hoverTreeMaxIndex = hoverTreeMaxIndex;
		debug("Setting new tree max index hover to [" + hoverTreeMaxIndex + "].",18);
		setChanged();
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @author rleach
	 */
	public void unsetHoverTreeMaxIndex() {
		this.hoverTreeMaxIndex = -1;
		setChanged();
		setHoverChanged();
		notifyObservers();
	}

	/**
	 * @return the selectingStart
	 */
	public int getSelectingStart() {
		
		return selectingStart;
	}

	/**
	 * @param selectingStart the selectingStart to set
	 */
	public void setSelectingStart(int selectingStart) {
		
		this.selectingStart = selectingStart;
	}

	/**
	 * @return the selecting
	 */
	public boolean isSelecting() {
		
		return selecting;
	}

	/**
	 * @param selecting the selecting to set
	 */
	public void setSelecting(boolean selecting) {
		
		this.selecting = selecting;
	}

	public boolean isToggling() {
		
		return toggling;
	}

	public void setToggling(boolean toggling) {
		
		this.toggling = toggling;
	}

	public boolean isDeSelecting() {
		
		return deselecting;
	}

	public void setDeSelecting(boolean deselecting) {
		
		this.deselecting = deselecting;
	}

	/**
	 * TODO Why is there a method to access a static member variable?
	 * This is needed for IMV UNTIL we have implemented a way to handle smooth
	 * zooming that is aware of aspect ratio
	 * @author rleach
	 * @return double ZOOM_INCREMENT
	 */
	public double getZoomIncrement() {
		
		return(ZOOM_INCREMENT);
	}

	/**
	 * TODO Why is there a method to access a static member variable?
	 * This is needed for IMV UNTIL we have implemented a way to handle smooth
	 * zooming that is aware of aspect ratio
	 * @author rleach
	 * @return double ZOOM_INCREMENT_FAST
	 */
	public double getZoomIncrementFast() {
		
		return(ZOOM_INCREMENT_FAST);
	}

	/**
	 * Set the first visible label data index.  For use by LabelView.
	 * -1 = unset
	 * @author rleach
	 * @param p
	 */
	public void setFirstVisibleLabel(int p) {
		if(p < getMinIndex() || p > getMaxIndex())
			firstVisibleLabel = -1;
		else
			firstVisibleLabel = p;
	}

	/**
	 * Retrieves the first visible label data index
	 * @author rleach
	 * @return firstVisiblelabel data index
	 */
	public int getFirstVisibleLabel() {
		return(firstVisibleLabel);
	}

	/**
	 * Set the last visible label data index.  For use by LabelView.
	 * -1 = unset
	 * @author rleach
	 * @param p
	 */
	public void setLastVisibleLabel(int p) {
		if(p < getMinIndex() || p > getMaxIndex())
			lastVisibleLabel = -1;
		else
			lastVisibleLabel = p;
	}

	/**
	 * Retrieves the last visible label data index
	 * @author rleach
	 * @return lastVisiblelabel data index
	 */
	public int getLastVisibleLabel() {
		return(lastVisibleLabel);
	}

	/**
	 * Retrieves the last visible label data index
	 * @author rleach
	 * @return lastVisiblelabel data index
	 */
	public int getNumVisibleLabels() {
		if(lastVisibleLabel < 0) {
			return(-1);
		}
		return(lastVisibleLabel - firstVisibleLabel + 1);
	}

	/**
	 * This provides the number of pixels the first label is offset from the
	 * nearest edge.  This is required by the trees in order to align the leaves
	 * with the labels
	 * @author rleach
	 * @return the firstVisibleLabelOffset
	 */
	public int getFirstVisibleLabelOffset() {
		return(firstVisibleLabelOffset);
	}

	/**
	 * This sets the number of pixels the first label is offset from the
	 * nearest edge.  This is required by the trees in order to align the leaves
	 * with the labels
	 * @author rleach
	 * @param firstVisibleLabelOffset the firstVisibleLabelOffset to set
	 */
	public void setFirstVisibleLabelOffset(int firstVisibleLabelOffset) {
		this.firstVisibleLabelOffset = firstVisibleLabelOffset;
	}

	/**
	 * This provides the number of pixels the last label is offset from the
	 * nearest edge.  This is required by the trees in order to align the leaves
	 * with the labels
	 * @author rleach
	 * @return the lastVisibleLabelOffset
	 */
	public int getLastVisibleLabelOffset() {
		return(lastVisibleLabelOffset);
	}

	/**
	 * This sets the number of pixels the last label is offset from the
	 * nearest edge.  This is required by the trees in order to align the leaves
	 * with the labels
	 * @author rleach
	 * @param lastVisibleLabelOffset the lastVisibleLabelOffset to set
	 */
	public void setLastVisibleLabelOffset(int lastVisibleLabelOffset) {
		this.lastVisibleLabelOffset = lastVisibleLabelOffset;
	}


	/**
	 * @author rleach
	 * @return the lastTreeModeGlobal
	 */
	public boolean wasLastTreeModeGlobal() {
		return(lastTreeModeGlobal);
	}

	/**
	 * @author rleach
	 * @param lastTreeModeGlobal the lastTreeModeGlobal to set
	 */
	public void setLastTreeModeGlobal(boolean lastTreeModeGlobal) {
		this.lastTreeModeGlobal = lastTreeModeGlobal;
	}

	/**
	 * @author rleach
	 * @return the keepTreeGlobal
	 */
	public boolean shouldKeepTreeGlobal() {
		return(keepTreeGlobal);
	}

	/**
	 * @author rleach
	 * @param keepTreeGlobal the keepTreeGlobal to set
	 */
	public void setKeepTreeGlobal(boolean keepTreeGlobal) {
		this.keepTreeGlobal = keepTreeGlobal;
	}

	public boolean somethingIsDragging() {
		return(selecting || labelsBeingScrolled || draggingDivider);
	}

	/**
	 * @author rleach
	 * @return the whizMode
	 */
	public boolean isWhizMode() {
		return(whizMode);
	}

	/**
	 * @author rleach
	 * @param whizMode the whizMode to set
	 */
	public void setWhizMode(boolean whizMode) {
		this.whizMode = whizMode;
	}

	public void debug(String msg, int level) {
		
		if(level == debug) {
			LogBuffer.println(msg);
		}
	}
}
