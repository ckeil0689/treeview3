/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ColorExtractor.java,v $
 * $Revision: 1.2 $
 * $Date: 2007-07-13 02:33:47 $
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import Utilities.Helper;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.ContrastSelectable;
import edu.stanford.genetics.treeview.LogBuffer;

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
	private ColorSet colorSet = null;// new ColorSet();// Will be backed by
	// confignode when we
	// get one...
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
	public ColorExtractor() {

		// set a default defaultColorSet... should be superceded by a user
		// setting...
		// defaultColorSet = new ColorSet();
		// defaultColorSet.setMissing(ColorSet.decodeColor("#909090"));
		// defaultColorSet.setEmpty(ColorSet.decodeColor("#FFFFFF"));
		colorList_default = new ArrayList<Color>();
		colorList_default.add(Color.red);
		colorList_default.add(Color.black);
		colorList_default.add(Color.green);

		fractions_default = new float[3];
		fractions_default[0] = 0.0f;
		fractions_default[1] = 0.5f;
		fractions_default[2] = 1.0f;

		setDefaultColorSet(defaultColorSet);
		setLogBase(2.0);
	}

	public void setMin(final double min) {

		this.dataMin = min;
	}
	
	public void setMax(final double max) {

		this.dataMax = max;
	}

	public void setNewParams(final float[] frac, final List<Color> cl) {

		fractions = frac;
		colorList = cl;
		setChanged();
	}

	/**
	 * Sets the default colors to be used if a config node if a config node is
	 * not bound to us. Also used in setDefaults() to figure out what the
	 * default colors are.
	 */
	public void setDefaultColorSet(final ColorSet set) {

		defaultColorSet = set;
	}

	// /**
	// * binds this ColorExtractor to a particular ConfigNode. This makes colors
	// * persistent
	// *
	// * @param configNode
	// * confignode to bind to
	// */
	// @Override
	// public void bindConfig(final Preferences configNode) {
	//
	// if (root != configNode) {
	//
	// root = configNode;
	// try {
	// String[] childrenNodes = root.childrenNames();
	// boolean nodePresent = false;
	//
	// for(int i = 0; i < childrenNodes.length; i++) {
	//
	// // This should later be about the children of ColorSet
	// if(childrenNodes[i].equalsIgnoreCase("ColorSet")) {
	// nodePresent = true;
	// }
	// }
	//
	// // Preferences cand = root.fetchFirst("ColorSet");
	// // if (cand == null) {
	// // cand = root.create("ColorSet");
	// // }
	//
	// Preferences cand = root.node("ColorSet");
	//
	// colorSet.bindConfig(cand);
	// synchFloats();
	// contrast = root.getDouble("contrast", getContrast());
	// setLogCenter(root.getDouble("logcenter", 1.0));
	// setLogBase(root.getDouble("logbase", 2.0));
	// m_logTranform = (root.getInt("logtransform", 0) == 1);
	// setChanged();
	//
	// } catch (BackingStoreException e) {
	// e.printStackTrace();
	// }
	// }
	// }

	/**
	 * binds this ColorExtractor to a particular ConfigNode. This makes colors
	 * persistent
	 *
	 * @param configNode
	 *            confignode to bind to
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("ColorExtractor");

		} else {
			LogBuffer.println("Could not find or create ColorExtractor "
					+ "node because parentNode was null.");
		}

		String lastActive = "RedGreen";
		try {
			if (parentNode.nodeExists("GradientChooser")) {
				lastActive = parentNode.node("GradientChooser").get(
						"activeColors", lastActive);
			}
			Boolean foundColorSet = false;

			for (final ColorSet defaultColorSet2 : ColorPresets.defaultColorSets) {
				if (defaultColorSet2.getName().equalsIgnoreCase(lastActive)) {
					colorSet = new ColorSet(defaultColorSet2);
					foundColorSet = true;
				}
			}

			if (!foundColorSet) {
				// Set colorList and fractionList here, based on what the last
				// active
				// node was in GradientColorChooser. Otherwise keep defaults?
				final Preferences colorPresetNode = parentNode
						.node("ColorPresets");
				final String[] childrenNodes = colorPresetNode.childrenNames();

				for (final String childrenNode : childrenNodes) {
					if (colorPresetNode.node(childrenNode)
							.get("name", lastActive)
							.equalsIgnoreCase(lastActive)) {
						colorSet = new ColorSet(
								colorPresetNode.node(childrenNode));
						foundColorSet = true;
						break;
					}
				}
			}
			// TODO This should be a more robust way of choosing a default
			// colorset
			if (!foundColorSet) {
				colorSet = new ColorSet(ColorPresets.defaultColorSets[0]);
				LogBuffer.println("Unable to find last used colorset: "
						+ lastActive + "; using " + colorSet.getName());
			}
		} catch (final BackingStoreException e) {
			LogBuffer.logException(e);
		}

		final String[] colors = colorSet.getColors();
		final List<Color> cList = new ArrayList<Color>(colors.length);
		for (final String color : colors) {
			cList.add(Color.decode(color));
		}

		setNewParams(colorSet.getFractions(), cList);
		synchFloats(); /* sets initial missing/ empty data colors */
		contrast = configNode.getDouble("contrast", getContrast());
		setLogCenter(configNode.getDouble("logcenter", 1.0));
		setLogBase(configNode.getDouble("logbase", 2.0));
		m_logTranform = (configNode.getInt("logtransform", 0) == 1);
		setChanged();
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
				configNode.putInt("logtransform", transform ? 1 : 0);
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
	private void synchFloats(final Color newColor, final float[] comp) {

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

		if (Helper.nearlyEqual(dval, nodata))
			return missingColor;
		// return new Color(missingColor[0], missingColor[1],
		// missingColor[2]);
		else if (Helper.nearlyEqual(dval, empty))
			// System.out.println("value " + dval + " was empty");
			return emptyColor;
		// return new Color(emptyColor[0], emptyColor[1], emptyColor[2]);
		else {

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
		} else
			return null;
	}
}
