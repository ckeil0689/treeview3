/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import ColorChooser.ColorSchemeType;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class represents a set of colors which can be used by a color extractor
 * to translate data values into colors.
 *
 * NOTE: This class has been superceded by the ConfigColorSet in the
 * edu.stanford.genetics.treeview package, although I am not likely to actually
 * rewrite any of this code spontaneously.
 *
 * NOTE: Attempting to rewrite to separate ColorSet from the persistence of the
 * ColorSet
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */

public class ColorSet {

	/* Default values for a ColorSet. */
	private final static String default_name = ColorSchemeType.REDGREEN.toString();
	private final static float[] default_fractions = { 0.0f, 0.5f, 1.0f };
	private final static String[] default_colors = { "#FF0000", "#000000", "#00FF00" };
	private final static String default_missingColor = "#FFFFFF";
	private final static String default_emptyColor = "#FFFFFF";

	private final static double default_min = -1.0;
	private final static double default_max = 1.0;

	private final String name;
	private List<Color> colorList = new ArrayList<Color>();
	private List<Float> fractionList = new ArrayList<Float>();
	private double min;
	private double max;
	private Color missing;
	private Color empty;

	/**
	 * Constructor for the ColorSet object
	 *
	 * @param name
	 *            initial name
	 * @param List
	 *            <Color> colorList List of initial colors
	 * @param List
	 *            <Double> fractionList List of initial fractions
	 * @param missing
	 *            string representing initial missing color
	 * @param empty
	 *            string representing initial empty color
	 */
	public ColorSet(final String name, final List<Color> colorList,
			final List<Float> fractionList, final double min,
			final double max, final String missing, final String empty) {

		this.name = name;
		this.colorList = colorList;
		this.fractionList = fractionList;
		this.min = min;
		this.max = max;
		this.missing = decodeColor(missing);
		this.empty = decodeColor(empty);
	}

	/**
	 * Constructor for ColorSet that loads values from ConfigNode
	 *
	 * @param Preferences
	 *            colorSetNode Preferences node with color set information
	 *            stored
	 */
	public ColorSet(final Preferences colorSetNode) {

		this.name = colorSetNode.get("name", default_name);
		int colorNum = colorSetNode.getInt("colorNum", default_colors.length);

		try {
			for (int i = 0; i < colorNum; i++) {
				colorList.add(decodeColor(colorSetNode.get("Color" + (i + 1), default_colors[0])));
				fractionList.add(colorSetNode.getFloat("Fraction" + (i + 1), default_fractions[1]));
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			LogBuffer.logException(e);

			// Add defaults if there's an issue
			colorNum = default_colors.length;
			for (int i = 0; i < colorNum; i++) {
				colorList.add(decodeColor(default_colors[i]));
				fractionList.add(default_fractions[i]);
			}
		}

		/*
		 * TODO this is bad... will screw up dataset colors on first load since
		 * min/max by default have nothing to do with
		 */
		this.min = colorSetNode.getDouble("min", default_min);
		this.max = colorSetNode.getDouble("max", default_max);

		this.missing = decodeColor(colorSetNode.get("missing",
				default_missingColor));
		this.empty = decodeColor(colorSetNode.get("empty", default_emptyColor));
	}

	/**
	 * Constructor for the ColorSet object
	 *
	 * @param name
	 *            inital name
	 * @param color1
	 *            string representing inital color1
	 * @param color2
	 *            string representing inital color2
	 * @param color3
	 *            string representing inital color3
	 * @param missing
	 *            string representing inital missing color
	 * @param empty
	 *            string representing inital empty color
	 */
	public ColorSet(final String name, final String[] colors,
			final String missing, final String empty) {

		final List<Color> newColorList = new ArrayList<Color>();
		for (String color : colors) {
			newColorList.add(decodeColor(color)); /* min in ColorChooser */
		}

		final List<Float> newFractionList = new ArrayList<Float>();
		for (int i = 0; i < default_fractions.length; i++) {
			newFractionList.add(default_fractions[i]);
		}

		this.name = name;
		this.colorList = newColorList;
		this.fractionList = newFractionList;
		this.min = default_min;
		this.max = default_max;
		this.missing = decodeColor(missing);
		this.empty = decodeColor(empty);
	}

	/**
	 * Copy Constructor
	 *
	 * @param another
	 */
	public ColorSet(final ColorSet another) {

		this.name = another.name;
		this.colorList = another.colorList;
		this.fractionList = another.fractionList;
		this.min = another.min;
		this.max = another.max;
		this.missing = another.missing;
		this.empty = another.empty;
	}

	/**
	 * Save ColorSet to ConfigNode
	 *
	 * @param Preferences
	 *            colorSetNode Preferences node to store ColorSet in
	 */
	public void save(final Preferences colorSetNode) {

		LogBuffer.println("Saving the ColorSet with name: " + name);	
		colorSetNode.put("name", this.name);

		final int colorNum = colorList.size();
		colorSetNode.putInt("colorNum", colorNum);

		for (int i = 0; i < colorNum; i++) {
			colorSetNode.put("Color" + (i + 1), encodeColor(colorList.get(i)));
		}

		for (int i = 0; i < colorNum; i++) {
			colorSetNode.putFloat("Fraction" + (i + 1), fractionList.get(i).floatValue());
		}

		colorSetNode.putDouble("min", min);
		colorSetNode.putDouble("max", max);

		colorSetNode.put("missing", encodeColor(this.missing));
		colorSetNode.put("empty", encodeColor(this.empty));
	}

	/* inherit description */
	@Override
	public String toString() {

		final String[] colors = getColors();

		String colorString = "";
		if (colors.length > 0) {
			for (final String color : colors) {

				colorString += color + "; ";
			}
		} else {
			colorString = "No colors in node.";
		}

		return "ColorSet " + getName() + "\n" + "Colors: " + colorString
				+ " missing: " + getMissing().toString() + "\t" + "empty: "
				+ getEmpty().toString() + "\t";
	}

	public void setMissing(final Color missing) {
		this.missing = missing;
	}

	public void setEmpty(final Color empty) {
		this.empty = empty;
	}

	/**
	 * Retrieves hex representations of colors
	 *
	 * @return String[]
	 */
	public String[] getColors() {

		final int colorNum = colorList.size();
		final String[] colors = new String[colorNum];

		for (int i = 0; i < colorNum; i++) {
			colors[i] = encodeColor(colorList.get(i));
		}

		return colors;
	}

	public List<Color> getColorList() {

		return colorList;
	}

	public List<Float> getFractionList() {

		return fractionList;
	}

	public double getMin() {

		return min;
	}

	public double getMax() {

		return max;
	}

	/**
	 * Retrieves the fraction values stored in the current configNode.
	 *
	 * @return
	 */
	public float[] getFractions() {

		final int colorNum = colorList.size();
		final float[] fractions = new float[colorNum];

		for (int i = 0; i < colorNum; i++) {
			fractions[i] = fractionList.get(i).floatValue();
		}

		return fractions;

	}

	/**
	 * Color for missing values.
	 */
	public Color getMissing() {

		return missing;
	}

	/**
	 * Color for empty values.
	 */
	public Color getEmpty() {

		return empty;
	}

	/**
	 * The name of this color set
	 */
	public String getName() {
		return name;
	}

	/**
	 * Convert a color from a hex string to a Java <code>Color</code> object.
	 *
	 * @param colorString
	 *            hex string, such as #FF11FF
	 * @return The corresponding java color object.
	 */
	public final static Color decodeColor(final String colorString) {

		return Color.decode(colorString);// will this work?
	}

	/**
	 * Convert a java <code>Color</code> object to a hex string.
	 *
	 * @param color
	 *            A java color object
	 * @return The corresponding hex string
	 */
	public final static String encodeColor(final Color color) {

		final int red = color.getRed();
		final int green = color.getGreen();
		final int blue = color.getBlue();

		return "#" + hex(red) + hex(green) + hex(blue);
	}

	private final static String hex(final int buf) {

		final int hi = buf / 16;
		final int low = buf % 16;

		return hexChar(hi) + hexChar(low);
	}

	private final static String hexChar(final int i) {

		switch (i) {
		case 0:
			return "0";
		case 1:
			return "1";
		case 2:
			return "2";
		case 3:
			return "3";
		case 4:
			return "4";
		case 5:
			return "5";
		case 6:
			return "6";
		case 7:
			return "7";
		case 8:
			return "8";
		case 9:
			return "9";
		case 10:
			return "A";
		case 11:
			return "B";
		case 12:
			return "C";
		case 13:
			return "D";
		case 14:
			return "E";
		case 15:
			return "F";
		default:
			return "F";
		}
	}

	// Save/ load methods
	/**
	 * extract values from Eisen-formatted file specified by the string argument
	 * The Eisen format is a 16 byte file. The first four bytes are interpreted
	 * as RBGA values specifying red, green, blue and alpha values from 0-255
	 * (00 - FF in base 16) for up-regulated genes, the next four are the values
	 * for unchanged, then down regulated, then the color for missing values.
	 *
	 * @param file
	 *            file to load from
	 * @exception IOException
	 *                throw if problems with file
	 */
	public void loadEisen(final String file) throws IOException {

		loadEisen(new File(file));
	}

	/**
	 * extract values from Eisen-formatted file
	 *
	 * @param file
	 *            file to load from
	 * @exception IOException
	 *                throw if problems with file
	 */
	public void loadEisen(final File file) throws IOException {

		final FileInputStream stream = new FileInputStream(file);
		missing = unpackEisen(stream);
		stream.close();
	}

	/**
	 * save values to Eisen-formatted file specified by the String
	 *
	 * @param file
	 *            file to store to
	 * @exception IOException
	 *                throw if problems with file
	 */
	public void saveEisen(final String file) throws IOException {

		saveEisen(new File(file));
	}

	/**
	 * save values to Eisen-formatted file sp
	 *
	 * @param file
	 *            file to store to
	 * @exception IOException
	 *                throw if problems with file
	 */
	public void saveEisen(final File file) throws IOException {

		final FileOutputStream stream = new FileOutputStream(file);
		packEisen(missing, stream);
		stream.close();
	}

	private static Color unpackEisen(final InputStream stream)
			throws IOException {

		final int red = stream.read();
		final int green = stream.read();
		final int blue = stream.read();
		final int alpha = stream.read();

		return new Color(red, green, blue, alpha);
	}

	private static void packEisen(final Color out, final OutputStream stream)
			throws IOException {

		stream.write(out.getRed());
		stream.write(out.getGreen());
		stream.write(out.getBlue());
		stream.write(out.getAlpha());
	}
}
