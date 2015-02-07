/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: ColorSet.java,v $
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

	private Color missing;
	private Color empty;

	private final String name;
	private List<Color> colorList = new ArrayList<Color>();
	private List<Double> fractionList = new ArrayList<Double>();

	private final String default_missingColor = "#FFFFFF";
	private final String default_emptyColor = "#FFFFFF";
	private final float[] default_fractions = { 0.0f, 0.5f, 1.0f };
	private final String[] default_colors = { "#FF0000", "#000000", "#00FF00" };
	private final String default_name = "RedGreen";

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
			final List<Double> fractionList, final String missing,
			final String empty) {

		this.colorList = colorList;
		this.fractionList = fractionList;
		this.name = name;
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
		this.missing = decodeColor(colorSetNode.get("missing",
				default_missingColor));
		this.empty = decodeColor(colorSetNode.get("empty", default_emptyColor));

		final int colorNum = colorSetNode.getInt("colorNum",
				default_colors.length);
		/*
		 * default colors/ fracs is always length 3. The original code here
		 * produced ArrayIndexOutOfBoundsExceptions if the user adds colors and
		 * makes colorNum > 3
		 */
		for (int i = 0; i < colorNum; i++) {
			colorList.add(decodeColor(colorSetNode.get("Color" + i + 1,
					default_colors[0])));// default_colors[i])));
		}
		for (int i = 0; i < colorNum; i++) {
			fractionList.add(new Double(colorSetNode.getFloat("Fraction" + i
					+ 1, default_fractions[1])));// default_fractions[i])));
		}
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
	public ColorSet(final String name, final String color1,
			final String color2, final String color3, final String missing,
			final String empty) {

		// final String name, final List<Color> colorList,
		// final List<Double> fractionList, final Color missing,
		// final Color empty
		final List<Color> newColorList = new ArrayList<Color>();
		newColorList.add(decodeColor(color1));
		newColorList.add(decodeColor(color2));
		newColorList.add(decodeColor(color3));
		final List<Double> newFractionList = new ArrayList<Double>();
		newFractionList.add(0.0);
		newFractionList.add(0.5);
		newFractionList.add(1.0);

		this.name = name;
		this.colorList = newColorList;
		this.fractionList = newFractionList;
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

		colorSetNode.put("name", this.name);

		final int colorNum = colorList.size();
		colorSetNode.putInt("colorNum", colorNum);

		for (int i = 0; i < colorNum; i++) {
			colorSetNode.put("Color" + i + 1, encodeColor(colorList.get(i)));
		}

		for (int i = 0; i < colorNum; i++) {
			colorSetNode.putFloat("Fraction" + i + 1, fractionList.get(i)
					.floatValue());
		}

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

	public List<Double> getFractionList() {

		return fractionList;
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
		}
		return "F";
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

	private Color unpackEisen(final InputStream stream) throws IOException {

		final int red = stream.read();
		final int green = stream.read();
		final int blue = stream.read();
		final int alpha = stream.read();

		return new Color(red, green, blue, alpha);
	}

	private void packEisen(final Color out, final OutputStream stream)
			throws IOException {

		stream.write(out.getRed());
		stream.write(out.getGreen());
		stream.write(out.getBlue());
		stream.write(out.getAlpha());
	}
}
