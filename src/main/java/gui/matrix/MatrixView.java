package gui.matrix;

import gui.GUIFactory;
import gui.colorPicker.ColorChooserController;
import gui.general.HintDialog;
import model.data.trees.TreeSelectionI;
import model.export.ExportHandler.ExportWorker;
import model.export.RegionType;
import net.miginfocom.swing.MigLayout;
import util.LogBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.util.Observable;

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

	protected TreeSelectionI rowSelection;
	protected TreeSelectionI colSelection;

	protected boolean hasDrawn = false;

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
			updatePixelsWithHint();
			offscreenValid = false;

		} else {
			LogBuffer.println(viewName() + " got weird update : " + o);
			return;
		}

		repaint();
	}

	@Override
	protected void updateMatrix() {

		if(xmap.hoverChanged() || ymap.hoverChanged()) {
			xmap.unsetHoverChanged();
			ymap.unsetHoverChanged();
			return;
		}

		if (!offscreenValid) {
			adjustPixelsToMaps();
			revalidateScreen();
			setSubImage();

			//Update the saved pixels available in the mapcontainers so that
			//dependent components will adjust to the new real estate.
			xmap.adjustToScreenChange();
			ymap.adjustToScreenChange();

			if(dataChanged) {
				updatePixels();
				dataChanged = false;
			}
		}
			
		xmap.notifyObservers();
		ymap.notifyObservers();
	}

	/**
	 * Export all data to a file
	 * @author rleach
	 * @param g - Graphics object
	 * @param xIndent - The number of points to indent the image on the x axis
	 * @param yIndent - The number of points to indent the image on the y axis
	 * @param size - The number of points in each dimension of a square tile
	 */
	public void export(ExportWorker e, final Graphics g,final int xIndent,final int yIndent,
		final int tileXsize,final int tileYsize,final RegionType region,
		final boolean showSelections) {

		if(region == RegionType.ALL) {
			exportAll(e,g,xIndent,yIndent,tileXsize,tileYsize,showSelections);
		} else if(region == RegionType.VISIBLE) {
			exportVisible(e,g,xIndent,yIndent,tileXsize,tileYsize,showSelections);
		} else if(region == RegionType.SELECTION) {
			exportSelection(e,g,xIndent,yIndent,tileXsize,tileYsize,
				showSelections);
		} else {
			LogBuffer.println("ERROR: Invalid model.export region: [" + region +
				"].");
		}
	}

	/**
	 * Export the entire matrix to a file
	 * @author rleach
	 * @param g
	 * @param xIndent
	 * @param yIndent
	 * @param tileXsize
	 * @param tileYsize
	 */
	private void exportAll(ExportWorker e, final Graphics g,final int xIndent,final int yIndent,
		final int tileXsize,final int tileYsize,final boolean showSelections) {

		if(drawer != null) {
			drawer.paint(e,g,0,0,xmap.getMaxIndex(),ymap.getMaxIndex(),
				xIndent,yIndent,tileXsize,tileYsize,
				showSelections,colSelection,rowSelection);
		}
	}

	/**
	 * Export the currently visible data to a file
	 * @author rleach
	 * @param g - Graphics object
	 * @param xIndent - The number of points to indent the image on the x axis
	 * @param yIndent - The number of points to indent the image on the y axis
	 * @param size - The number of points in each dimension of a square tile
	 */
	private void exportVisible(ExportWorker e, final Graphics g,final int xIndent,
		final int yIndent,final int tileXsize,final int tileYsize,
		final boolean showSelections) {

		if(drawer != null) {
			drawer.paint(e,g,xmap.getFirstVisible(),ymap.getFirstVisible(),
				xmap.getLastVisible(),ymap.getLastVisible(),
				xIndent,yIndent,tileXsize,tileYsize,
				showSelections,colSelection,rowSelection);
		}
	}

	/**
	 * Export the currently selected (and intervening) data to a file
	 * @author rleach
	 * @param g - Graphics object
	 * @param xIndent - The number of points to indent the image on the x axis
	 * @param yIndent - The number of points to indent the image on the y axis
	 * @param size - The number of points in each dimension of a square tile
	 */
	private void exportSelection(ExportWorker e, final Graphics g,final int xIndent,
		final int yIndent,final int tileXsize,final int tileYsize,
		final boolean showSelections) {

		if(drawer != null) {
			if(colSelection != null && colSelection.getNSelectedIndexes() > 0) {
				drawer.paint(e,g,colSelection.getMinIndex(),
					rowSelection.getMinIndex(),colSelection.getMaxIndex(),
					rowSelection.getMaxIndex(),
					xIndent,yIndent,tileXsize,tileYsize,
					showSelections,colSelection,rowSelection);
			} else {
				LogBuffer.println("ERROR: No selection exists.");
			}
		}
	}

	@Override
	protected void updatePixels() {

		/* TODO remove rectangle dependency of drawer. not needed. */
		final Rectangle destRect = new Rectangle(0, 0,
				xmap.getUsedPixels(), ymap.getUsedPixels());
		
		final Rectangle sourceRect = new Rectangle(0, 0, 
				xmap.getTotalTileNum(), ymap.getTotalTileNum());

		if ((sourceRect.x >= 0) && (sourceRect.y >= 0) && drawer != null) {
			/* Set new offscreenPixels (pixel colors) */
			drawer.paint(offscreenPixels, sourceRect, destRect,
					offscreenScanSize);
		}
	}

	@Override
	protected void updatePixelsWithHint() {
		
		final HintDialog hint = new HintDialog("Updating Pixels...");
		
		final SwingWorker<Void, Void> pixelUpdater = 
				new SwingWorker<Void, Void>() {
			
			@Override
			protected Void doInBackground() throws Exception {
				
				final Rectangle destRect = new Rectangle(0, 0,
						xmap.getUsedPixels(), ymap.getUsedPixels());
				
				final Rectangle sourceRect = new Rectangle(0, 0, 
						xmap.getTotalTileNum(), ymap.getTotalTileNum());

				if ((sourceRect.x >= 0) && (sourceRect.y >= 0) && drawer != null) {
					/* Set new offscreenPixels (pixel colors) */
					drawer.paint(offscreenPixels, sourceRect, destRect,
							offscreenScanSize);
				}
				return null;
			}
			
			@Override
			protected void done() {
				
				hint.dispose();
			}
		};
		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				pixelUpdater.execute();
			}
		});

		hint.setVisible(true);
		
		try {
			pixelUpdater.get();
			
		} catch(Exception e) {
			LogBuffer.logException(e);
			LogBuffer.println("Issue when trying to update pixels.");
			return;
		}
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

		xmap.setMinScale();
		ymap.setMinScale();

		revalidateScreen();
		repaint();
	}

	/**
	 * Set geneSelection
	 *
	 * @param rowSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setRowSelection(final TreeSelectionI rowSelection) {

		if (this.rowSelection != null) {
			this.rowSelection.deleteObserver(this);
		}

		this.rowSelection = rowSelection;
		this.rowSelection.addObserver(this);
	}

	/**
	 * Set arraySelection
	 *
	 * @param colSelection
	 *            The TreeSelection which is set by selecting arrays in the
	 *            GlobalView
	 */
	public void setColSelection(final TreeSelectionI colSelection) {

		if (this.colSelection != null) {
			this.colSelection.deleteObserver(this);
		}

		this.colSelection = colSelection;
		this.colSelection.addObserver(this);
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
	 * get the xmapping for this view
	 */
	public MapContainer getXMap() {
		return xmap;
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
	 * get the ymapping for this view
	 */
	public MapContainer getYMap() {
		return ymap;
	}
	
	/**
	 * Creates a new Image with a new set of 
	 */
	protected void adjustPixelsToMaps() {
		
		// correct for zero indexing
		int x_tiles = xmap.getTotalTileNum();
		int y_tiles = ymap.getTotalTileNum();
		
		int tileCount = x_tiles * y_tiles;
		
		if(offscreenPixels.length != tileCount) {
			createNewBuffer(x_tiles, y_tiles);
		}
	}

	/**
	 * If an image does not exist yet, it will be created here. Its dimensions
	 * in pixels directly correspond to the axis dimensions of the loaded model.
	 * This is relayed by the MapContainers.
	 */
	@Override
	protected void ensureCapacity() {
		
		/* Create new image only if one doesn't exist yet. 
		 * This avoids unnecessary updates to the BufferedImage pixel raster. 
		 */
		if (offscreenImage == null) {
			int x_tiles = xmap.getTotalTileNum();
			int y_tiles = ymap.getTotalTileNum();
			createNewBuffer(x_tiles, y_tiles);
		}
	}
	
	/**
	 * Sets a reference for the sub image which is bound by the MapContainer.
	 */
	@Override
	protected void setSubImage() {
		
		int x = xmap.getFirstVisible();
		int y = ymap.getFirstVisible();
		int w = xmap.getNumVisible();
		int h = ymap.getNumVisible();
		
		try {
			paintImage = ((BufferedImage)offscreenImage).getSubimage(x, y, w, h);
			
		} catch(RasterFormatException e) {
			LogBuffer.logException(e);
			LogBuffer.println("x: " + x + " y: " + y + " w: " + w + " h: " + h);
			paintImage = null;
		}
	}
	
	protected void setBoundedSubImage(int x, int y, int w, int h) {
		
		try {
			paintImage = ((BufferedImage)offscreenImage).getSubimage(x, y, w, h);
			
		} catch(RasterFormatException e) {
			LogBuffer.logException(e);
			LogBuffer.println("x: " + x + " y: " + y + " w: " + w + " h: " + h);
			paintImage = null;
		}
	}
	
	public void setHasMouse(boolean hasMouse) {
		
		this.hasMouse = hasMouse;
	}
	
	public boolean hasMouse() {
		
		return hasMouse;
	}
	
	/**
	 * Tells the MatrixView that underlying data has changed and it needs to
	 * recalculate its pixel color values. 
	 */
	public void setDataChanged() {

		this.dataChanged = true;
	}
	
	public BufferedImage getVisibleImage() {
		
		return (BufferedImage) paintImage;
	}
}
