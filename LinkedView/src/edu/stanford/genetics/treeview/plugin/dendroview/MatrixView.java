package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;
import ColorChooser.ColorChooserController;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelViewProduced;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * Basic skeleton for a MatrixView. Can be used as a basis for the interactive
 * MatrixView as well as the overview MatrixView since their only difference is
 * interactivity.
 * 
 * @author chris0689
 *
 */
public abstract class MatrixView extends ModelViewProduced {

	protected MapContainer xmap;
	protected MapContainer ymap;

	protected TreeSelectionI geneSelection;
	protected TreeSelectionI arraySelection;

	protected boolean hasDrawn = false;
	protected boolean pixelsChanged = false;

	protected ArrayDrawer drawer;

	protected final JScrollPane scrollPane;

	/**
	 * Default so warnings do not pop up...
	 */
	private static final long serialVersionUID = 1L;

	public MatrixView() {

		super();

		setLayout(new MigLayout());

		scrollPane = new JScrollPane(this,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		panel = scrollPane;
		panel.setBorder(BorderFactory.createEtchedBorder());
		panel.setBackground(GUIFactory.ELEMENT_HOV);
	}

	@Override
	public Dimension getPreferredSize() {

		return new Dimension(xmap.getRequiredPixels(), ymap.getRequiredPixels());
	}

	@Override
	public void update(Observable o, Object arg) {

		if (o == xmap || o == ymap) {
			recalculateOverlay();
			offscreenValid = false;

		} else if (o == drawer && drawer != null) {
			/*
			 * signal from drawer means that it need to draw something
			 * different.
			 */
			offscreenValid = false;

		} else if (o == xmap || o == ymap) {
			offscreenValid = false;

		} else if (o instanceof TreeSelectionI) {
			return;
			
		} else if (o instanceof ColorChooserController) {
			LogBuffer.println("Observer notified of pixel update.");
			pixelsChanged = true;

		} else {
			LogBuffer.println(viewName() + " got weird update : " + o);
			return;
		}

		repaint();
	}

	@Override
	protected void updatePixels() {
		// TODO Auto-generated method stub

	}

	@Override
	public String viewName() {

		return "MatrixView";
	}

	@Override
	protected void updateBuffer(Graphics g) {
		// TODO Auto-generated method stub

	}

	/**
	 * Checks the selection or view of genes and arrays and calculates the
	 * appropriate selection or viewport rectangle.
	 */
	protected abstract void recalculateOverlay();

	/**
	 * Checks current availability of space on the screen (pixels) and updates
	 * the MapContainers accordingly. Setting offScreenValid to false results in
	 * a repaint by the drawer when updatePixels() is called.
	 */
	protected void revalidateScreen() {

		if (offscreenChanged) {
			offscreenValid = false;
			xmap.setAvailablePixels(offscreenSize.width);
			ymap.setAvailablePixels(offscreenSize.height);

			if (!hasDrawn) {
				// total kludge, but addnotify isn't working correctly...
				xmap.recalculateScale();
				ymap.recalculateScale();
				hasDrawn = true;
			}
		}
	}

	/**
	 * Initiates a revalidation of the available screen space and sets the
	 * MapContainers to minimum scale.
	 */
	public void resetView() {

		LogBuffer.println("Resetting view: " + viewName());

		xmap.setToMinScale();
		ymap.setToMinScale();

		revalidateScreen();
	}

	/**
	 * Set geneSelection
	 *
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setRowSelection(final TreeSelectionI geneSelection) {

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}

		this.geneSelection = geneSelection;
		this.geneSelection.addObserver(this);
	}

	/**
	 * Set arraySelection
	 *
	 * @param arraySelection
	 *            The TreeSelection which is set by selecting arrays in the
	 *            GlobalView
	 */
	public void setColSelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);
		}

		this.arraySelection = arraySelection;
		this.arraySelection.addObserver(this);
	}

	/**
	 * Set ArrayDrawer
	 *
	 * @param arrayDrawer
	 *            The ArrayDrawer to be used as a source
	 */
	public void setArrayDrawer(final ArrayDrawer arrayDrawer) {

		if (drawer != null) {
			drawer.deleteObserver(this);
		}

		drawer = arrayDrawer;
		drawer.addObserver(this);
	}

	/**
	 * DEPRECATE set the xmapping for this view
	 *
	 * @param m
	 *            the new mapping
	 */
	public void setXMap(final MapContainer m) {

		if (xmap != null) {
			xmap.deleteObserver(this);
		}

		xmap = m;
		xmap.addObserver(this);
	}

	/**
	 * DEPRECATE set the ymapping for this view
	 *
	 * @param m
	 *            the new mapping
	 */
	public void setYMap(final MapContainer m) {

		if (ymap != null) {
			ymap.deleteObserver(this);
		}

		ymap = m;
		ymap.addObserver(this);
	}
	
	/**
	 * Creates a new Image with a new set of 
	 */
	protected void adjustPixelsToMaps() {
		
		int x_tiles = xmap.getMaxIndex() + 1;
		int y_tiles = ymap.getMaxIndex() + 1;
		
		int tileCount = x_tiles * y_tiles;
		
		if(offscreenPixels.length != tileCount) {
			LogBuffer.println("Creating new Image.");
			createNewBuffer(x_tiles, y_tiles);
		}
	}
	
	protected void setSubImage() {
		
		int x = xmap.getFirstVisible();
		int y = ymap.getFirstVisible();
		int w = xmap.getNumVisible();
		int h = ymap.getNumVisible();
		
		paintImage = ((BufferedImage)offscreenImage).getSubimage(x, y, w, h);
	}

}
