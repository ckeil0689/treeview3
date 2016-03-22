/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import ColorChooser.ColorSchemeType;
import Utilities.Helper;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.ContrastSelectable;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * The purpose of this class is to convert a data value into a color.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.2 $ $Date: 2007-07-13 02:33:47 $
 */

public class ColorExtractor extends Observable implements ConfigNodePersistent,
		ContrastSelectable {

	private ColorSet defaultColorSet;
	private final double default_contrast = 3.0;
	private ColorSet colorSet = null;
	
	private boolean m_logTranform = false;
	private double m_logCenter = 1.0;
	private double m_logBaseDivisor;
	private double m_logBase;

	private double dataMin;
	private double dataMax;

	private List<Color> colorList;
	private float[] fractions;

	private final List<Color> colorList_default;
	private final float[] fractions_default;

	private Preferences configNode;
	private double contrast = default_contrast;

	// stores magic values with special meaning
	private double nodata, empty;

	// store as r,g,b ints for cross-platform goodness...
	private final float[] missingColor = new float[3];
	private final float[] emptyColor = new float[3];

	/** Constructor for the ColorExtractor object */
	public ColorExtractor(double min, double max) {

		setMin(min);
		setMax(max);

		// set a default defaultColorSet... should be superceded by a user
		// setting...
		// defaultColorSet = new ColorSet();
		// defaultColorSet.setMissing(ColorSet.decodeColor("#909090"));
		// defaultColorSet.setEmpty(ColorSet.decodeColor("#FFFFFF"));
		this.colorList_default = new ArrayList<Color>();
		this.colorList_default.add(Color.red);
		this.colorList_default.add(Color.black);
		this.colorList_default.add(Color.green);

		this.fractions_default = new float[3];
		this.fractions_default[0] = 0.0f;
		this.fractions_default[1] = 0.5f;
		this.fractions_default[2] = 1.0f;

		setDefaultColorSet(defaultColorSet);
		setLogBase(2.0);
	}
	
	/**
	 * binds this ColorExtractor to a particular ConfigNode. This makes colors
	 * persistent
	 *
	 * @param configNode
	 *            confignode to bind to
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("Could not find or create ColorExtractor "
					+ "node because parentNode was null.");
			return;
		} 

		this.configNode = parentNode.node("ColorExtractor");
		
		ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		setDefaultColorSet(colorPresets.getDefaultColorSet());
		requestStoredState();
	}
	
	@Override
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {

		importStateFrom(configNode);
	}

	@Override
	public void storeState() {
		
		if(configNode == null) {
			LogBuffer.println("Could not store the state of "  
					+ this.getClass() + " because configNode was null.");
			return;
		}
		
		configNode.putDouble("contrast", contrast);
		configNode.putBoolean("logtransform", m_logTranform);
		configNode.putDouble("logcenter", m_logCenter);
		configNode.putDouble("logbase", m_logBase);
	}
	
	@Override
	public void importStateFrom(Preferences oldNode) {

		this.colorSet = findColorSetFromNode(oldNode);

		final String[] colors = colorSet.getColors();
		final List<Color> cList = new ArrayList<Color>(colors.length);
		for (final String color : colors) {
			cList.add(Color.decode(color));
		}

		setNewParams(colorSet.getFractions(), cList);

		if ((ColorSchemeType.CUSTOM.toString()).equalsIgnoreCase(
				colorSet.getName())) {
			setMin(colorSet.getMin());
			setMax(colorSet.getMax());
		}

		synchFloats(); /* sets initial missing/ empty data colors */
		this.contrast = oldNode.getDouble("contrast", getContrast());
		setLogCenter(oldNode.getDouble("logcenter", 1.0));
		setLogBase(oldNode.getDouble("logbase", 2.0));
		this.m_logTranform = (oldNode.getBoolean("logtransform", false));
		setChanged();
	}

	/**
	 * Find the last active colorset from a supplied Preferences node.
	 * 
	 * @param node
	 * @return The last active colorset or a default in case it cannot be found.
	 */
	private ColorSet findColorSetFromNode(final Preferences node) {

		ColorSet nodeColorSet = defaultColorSet;

		String lastActive = ColorSchemeType.REDGREEN.toString();
		try {
			/* Check old Preferences for data */
			if (node.nodeExists("GradientChooser")) {
				lastActive = node.node("GradientChooser").get("activeColors",
						lastActive);
			}

			boolean foundColorSet = false;

			for (final ColorSet defaultColorSet2 : ColorPresets.defaultColorSets) {
				if (defaultColorSet2.getName().equalsIgnoreCase(lastActive)) {
					nodeColorSet = new ColorSet(defaultColorSet2);
					foundColorSet = true;
					break;
				}
			}

			if (!foundColorSet) {
				// Set colorList and fractionList here, based on what the last
				// active
				// node was in GradientColorChooser. Otherwise keep defaults?
				final Preferences colorPresetNode = node.node("ColorPresets");
				final String[] childrenNodes = colorPresetNode.childrenNames();

				for (final String childNode : childrenNodes) {
					/*
					 * TODO second argument here needs to be a default, not the
					 * local variable
					 */
					if (colorPresetNode.node(childNode).get("name", lastActive)
							.equalsIgnoreCase(lastActive)) {
						nodeColorSet = new ColorSet(
								colorPresetNode.node(childNode));
						foundColorSet = true;
						break;
					}
				}
			}
			// TODO This should be a more robust way of choosing a default
			// colorset
			if (!foundColorSet) {
				nodeColorSet = new ColorSet(ColorPresets.defaultColorSets[0]);
				LogBuffer.println("Unable to find last used colorset: "
						+ lastActive + "; using " + nodeColorSet.getName());
			}
		} catch (final BackingStoreException e) {
			LogBuffer.logException(e);
			nodeColorSet = defaultColorSet;
		}

		return nodeColorSet;
	}

	public void setMin(final double min) {

		this.dataMin = min;
	}

	public void setMax(final double max) {

		this.dataMax = max;
	}

	public void setNewParams(final float[] frac, final List<Color> cl) {

		this.fractions = frac;
		this.colorList = cl;
		setChanged();
	}

	/**
	 * Sets the default colors to be used if a config node if a config node is
	 * not bound to us. Also used in setDefaults() to figure out what the
	 * default colors are.
	 */
	public void setDefaultColorSet(final ColorSet set) {

		this.defaultColorSet = set;
	}

	/**
	 * Set contrast value for future draws
	 *
	 * @param contrastValue
	 *            The desired contrast value
	 */
	@Override
	public void setContrast(final double contrastValue) {

		if (Helper.nearlyEqual(contrast, contrastValue)) {

			contrast = contrastValue;

			if (configNode != null) {
				configNode.putDouble("contrast", contrast);
			}
			setChanged();
		}
	}

	public void setLogTransform(final boolean transform) {

		if (transform != m_logTranform) {
			m_logTranform = transform;

			if (configNode != null) {
				configNode.putBoolean("logtransform", transform);
			}
			setChanged();
		}
	}

	public void setLogCenter(final double center) {

		if (Helper.nearlyEqual(m_logCenter, center)) {
			m_logCenter = center;

			if (configNode != null) {
				configNode.putDouble("logcenter", center);
			}
			setChanged();
		}
	}

	public double getLogCenter() {

		return m_logCenter;
	}

	public void setLogBase(final double base) {

		if (Helper.nearlyEqual(m_logBase, base)) {
			m_logBase = base;

			m_logBaseDivisor = Math.log(base);
			if (configNode != null) {
				configNode.putDouble("logbase", base);
			}
			setChanged();
		}
	}

	public double getLogBase() {

		return m_logBase;
	}

	public boolean getLogTransform() {

		return m_logTranform;
	}

	public ColorSet getActiveColorSet() {

		return colorSet;
	}

	/**
	 * Get contrast value
	 *
	 * @return contrastValue The current contrast value
	 */
	@Override
	public double getContrast() {

		return contrast;
	}

	/**
	 * This call sets the values which stand for missing or empty data. By
	 * default, missing data is drawn gray and empty data is drawn white. Empty
	 * data only occurs in some KNN views right now, and means that the square
	 * does not represent data, and is only there as a spacer.
	 *
	 * @param missing
	 *            The new missing value
	 * @param empty
	 *            The new empty value
	 */
	public void setMissing(final double missing, final double empty) {

		this.nodata = missing;
		this.empty = empty;

		setChanged();
	}

	/**
	 * The color for missing data.
	 */
	public Color getMissing() {

		return colorSet.getMissing();
	}

	/**
	 * The empty is a color to be used for cells which do not correspond to
	 * data, like in the KnnView. These cells are just used for spacing.
	 */
	public Color getEmpty() {

		return colorSet.getEmpty();
	}

	/* Imports colors from the current colorSet object to local variables */
	private void synchFloats() {

		synchFloats(colorSet.getMissing(), missingColor);
		synchFloats(colorSet.getEmpty(), emptyColor);
	}

	/* Sets local variables' values from the colorSet object's colors */
	private static void synchFloats(final Color newColor, final float[] comp) {

		comp[0] = (float) newColor.getRed() / 256;
		comp[1] = (float) newColor.getGreen() / 256;
		comp[2] = (float) newColor.getBlue() / 256;
	}

	/**
	 * The color for missing data.
	 */
	public void setMissingColor(final String newString) {

		if (ColorSet.encodeColor(colorSet.getMissing()).equals(newString))
			return;
		colorSet.setMissing(ColorSet.decodeColor(newString));
		synchFloats(colorSet.getMissing(), missingColor);
		setChanged();
	}

	/**
	 * The empty is a color to be used for cells which do not correspond to data
	 */
	public void setEmptyColor(final String newString) {

		if (newString == null)
			return;
		if (ColorSet.encodeColor(colorSet.getEmpty()).equals(newString))
			return;
		colorSet.setEmpty(ColorSet.decodeColor(newString));
		synchFloats(colorSet.getEmpty(), emptyColor);
		setChanged();
	}

	/**
	 * The color for missing data.
	 */
	public void setMissingColor(final Color newColor) {

		if (colorSet.getMissing().equals(newColor))
			return;
		colorSet.setMissing(newColor);
		synchFloats(colorSet.getMissing(), missingColor);
		setChanged();
	}

	/**
	 * Set emptyColor value for future draws The empty is a color to be used for
	 * cells which do not correspond to data
	 */
	public void setEmptyColor(final Color newColor) {
		if (newColor == null)
			return;
		if (colorSet.getEmpty().equals(newColor))
			return;
		colorSet.setEmpty(newColor);
		synchFloats(colorSet.getEmpty(), emptyColor);
		setChanged();
	}

	/**
	 * Gets the color corresponding to a particular data value.
	 *
	 * @param dval
	 *            double representing value we want color for
	 * @return The color value
	 */
	public Color getColor(final double dval) {
		/*
		 * if (dval == nodata) { return new Color(missingColor[0],
		 * missingColor[1], missingColor[2]); } else if (dval == empty) { return
		 * new Color(emptyColor[0], emptyColor[1], emptyColor[2]); } else { //
		 * calculate factor... double factor; if (dval < 0) { factor = -dval
		 * /contrast; } else { factor = dval / contrast; } if (factor >1.0)
		 * factor = 1.0; if (factor < 0.0) factor = 0.0; float ffactor = (float)
		 * factor; float ff1 = (float) (1.0 - factor); //calculate colors...
		 * float [] comp = new float[3]; if (dval < 0) { for (int i =0; i < 3;
		 * i++) { comp[i] = downColor[i] * ffactor + zeroColor[i] * ff1; } }
		 * else { for (int i =0; i < 3; i++) { comp[i] = upColor[i] * ffactor +
		 * zeroColor[i] * ff1; } }
		 */
		final float[] comp;
		if (fractions.length == 0 || colorList.isEmpty()) {
			comp = getFloatColor(dval, fractions_default, colorList_default);

		} else {
			comp = getFloatColor(dval, fractions, colorList);
		}
		final Color color = new Color(comp[0], comp[1], comp[2]);
		return color;
		// }
	}

	/**
	 * Gets the floatColor attribute of the ColorExtractor object
	 *
	 * @param dval
	 *            Description of the Parameter
	 * @return The floatColor value
	 */
	public float[] getFloatColor(double dval, final float[] fractionVals,
			final List<Color> colorVals) {

		if (TVModel.isMissing(dval)) {
			return missingColor;
			
		} else if (TVModel.isEmpty(dval)) {
			return emptyColor;
			
		} else {

			if (m_logTranform) {
				dval = Math.log(dval / m_logCenter) / m_logBaseDivisor;
			}
			// calculate factor...
			// double factor;
			// if (dval < 0) {
			// factor = -dval / contrast;
			// } else {
			// factor = dval / contrast;
			// }
			// if (factor > 1.0) {
			// factor = 1.0;
			// }
			// if (factor < 0.0) {
			// factor = 0.0;
			// }
			// final float ffactor = (float) factor;
			// final float ff1 = (float) (1.0 - factor);

			// calculate colors...
			final float[] comp = new float[3];

			// if(dval > 0.15) {
			// System.out.println("b");
			// }
			// How many percent away from dataMin dval is on the range.
			// This corresponds with the fractions list and the 2 colors
			// needed to calculate the gradient RGB-value can be deduced.
			// Example: dval is 0.23 (=23%) of the range between dataMin and
			// dataMax. fractions has 3 colors at [0.0, 0.5, 1.0] which is
			// default. That means the colors at 0.0 and 0.5 ought to be used
			// to calculate the gradient.
			final double valueFraction = (dval - dataMin) / (dataMax - dataMin);

			int colorIndex = 0;
			for (int i = 0; i < fractionVals.length; i++) {

				if (valueFraction > fractionVals[i]) {
					colorIndex = i;
				}
			}

			final double minFraction = fractionVals[colorIndex];
			double maxFraction = fractionVals[colorIndex];
			double x = 0.0;

			int nextIndex = colorIndex;
			if (colorIndex < fractionVals.length - 1) {
				nextIndex = colorIndex + 1;
				maxFraction = fractionVals[nextIndex];

				// Fraction position between the two colors
				x = (valueFraction - minFraction) / (maxFraction - minFraction);

				if (x < 0) {
					x = 0.0;

				} else if (x > 1.0) {
					x = 1.0;
				}
			} else {
				x = 1.0;
			}

			// The 2 colors
			final Color color1 = colorVals.get(colorIndex);
			final Color color2 = colorVals.get(nextIndex);

			for (int i = 0; i < comp.length; i++) {

				if (i == 0) {
					comp[i] = (float) ((color1.getRed() * (1 - x) + color2
							.getRed() * x) / 256);

				} else if (i == 1) {
					comp[i] = (float) ((color1.getGreen() * (1 - x) + color2
							.getGreen() * x) / 256);

				} else if (i == 2) {
					comp[i] = (float) ((color1.getBlue() * (1 - x) + color2
							.getBlue() * x) / 256);
				}
			}
			// if(fractionVals.length > colorIndex) {
			// maxFraction = fractionVals[colorIndex + 1];
			//
			// // Fraction position between the two colors
			// double x = (valueFraction - minFraction)/ (maxFraction
			// - minFraction);
			//
			// if(x < 0) {
			// x = 0.0;
			//
			// } else if(x > 1.0) {
			// x = 1.0;
			// }
			//
			// // The 2 colors
			// Color color1 = colorVals.get(colorIndex);
			// Color color2 = colorVals.get(colorIndex + 1);
			//
			// for (int i = 0; i < comp.length; i++) {
			//
			// if(i == 0) {
			// comp[i] = (float)((color1.getRed() * (1 - x)
			// + color2.getRed() * x)/ 256);
			//
			// } else if(i == 1) {
			// comp[i] = (float)((color1.getGreen() * (1 - x)
			// + color2.getGreen() * x) /256);
			//
			// } else if(i == 2) {
			// comp[i] = (float)((color1.getBlue() * (1 - x)
			// + color2.getBlue() * x) /256);
			// }
			// }
			//
			// } else {
			// Color color = colorVals.get(colorIndex);
			// for (int i = 0; i < 3; i++) {
			//
			// if(i == 0) {
			// comp[i] = (float)color.getRed();
			//
			// } else if(i == 1) {
			// comp[i] = (float)color.getGreen();
			//
			// } else if(i == 2) {
			// comp[i] = (float)color.getBlue();
			// }
			// }
			// }
			return comp;
		}
	}

	// /** prints out a description of the state to standard out */
	// public void printSelf() {
	// System.out.println("upColor " + upColor[0] + ", " + upColor[1] + ", "
	// + upColor[2]);
	// System.out.println("downColor " + downColor[0] + ", " + downColor[1]
	// + ", " + downColor[2]);
	// System.out.println("zeroColor " + zeroColor[0] + ", " + zeroColor[1]
	// + ", " + zeroColor[2]);
	// System.out.println("missingColor " + missingColor[0] + ", "
	// + missingColor[1] + ", " + missingColor[2]);
	// System.out.println("emptyColor " + emptyColor[0] + ", " + emptyColor[1]
	// + ", " + emptyColor[2]);
	// }

	/**
	 * Gets the aRGBColor attribute of the ColorExtractor object
	 *
	 * @param dval
	 *            Description of the Parameter
	 * @param isBackground
	 *            If a row/ column in DoubleArrayDrawer is not selected while
	 *            there exists another selection, it is considered to be in the
	 *            background.
	 * @return The aRGBColor value
	 */
	public int getARGBColor(final double dval) {
		/* Selection Dimming */
		// , boolean isBackground) {

		final float[] comp;
		if (fractions.length == 0 || colorList.isEmpty()) {
			comp = getFloatColor(dval, fractions_default, colorList_default);

		} else {
			comp = getFloatColor(dval, fractions, colorList);
		}

		/*
		 * Selection dimming If a pixel in DoubleArrayDrawer is in the
		 * background, darken the RGB color by multiplying R, G, and B values
		 * with a fraction.
		 */
		// if(isBackground) {
		// for(int i = 0; i < comp.length; i++) {
		// comp[i] *= 0.4;
		// }
		// }

		return ((255 << 24) | ((int) (255 * comp[0]) << 16)
				| ((int) (255 * comp[1]) << 8) | (int) (255 * comp[2]));
	}

	/** resets the ColorExtractor to a default state. */
	public void setDefaults() {

		setMissingColor(ColorSet.encodeColor(defaultColorSet.getMissing()));
		setEmptyColor(ColorSet.encodeColor(defaultColorSet.getEmpty()));
		setContrast(default_contrast);
		synchFloats();
		setChanged();
	}

	/**
	 * Returns the names of the current children of this class' root node.
	 *
	 * @return
	 */
	public String[] getRootChildrenNodes() {

		if (configNode != null) {
			String[] childrenNodes;
			try {
				childrenNodes = configNode.childrenNames();
				return childrenNodes;

			} catch (final BackingStoreException e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;
	}
}
