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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class represents a set of colors which can be used by a color extractor
 * to translate data values into colors.
 * 
 * NOTE: This class has been superceded by the ConfigColorSet in the
 * edu.stanford.genetics.treeview package, although I am not likely to actually
 * rewrite any of this code spontaneously.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */
public class ColorSet2 implements ConfigNodePersistent {

	private final String default_missingColor = "#909090";
	private final String default_emptyColor = "#FFFFFF";
	private final float[] default_fractions = {0.0f, 0.5f, 1.0f};
	private final String default_name = "NoName";

	private String name;
	private Color missing;
	private Color empty;
	
	private ArrayList<Color> colorList;
	private ArrayList<Double> fractionList;
	
	private Preferences configNode = null;

	/**
	 * Constructor for the ColorSet object, uses default values
	 */
	public ColorSet2() {

		super();
		
		colorList = new ArrayList<Color>();
		fractionList = new ArrayList<Double>();
		
		setDefaults();
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
	public ColorSet2(final String name, final String color1, 
			final String color2, final String color3, final String missing, 
			final String empty) {

		super();
		
		colorList = new ArrayList<Color>();
		fractionList = new ArrayList<Double>();
		
		ArrayList<Color> colors = new ArrayList<Color>();
		colors.add(decodeColor(color1));
		colors.add(decodeColor(color2));
		colors.add(decodeColor(color3));
		
		float[] floatVals = default_fractions;
		ArrayList<Double> fracs = new ArrayList<Double>(floatVals.length);
		
		for(int i = 0; i < floatVals.length; i++) {
			
			fracs.add((double)floatVals[i]);
		}
		
		setColorList(colors);
		setFractionList(fracs);
		setName(name);
		setMissing(missing);
		setEmpty(empty);
	}

	/**
	 * Sets defaults for missing and empty colors.
	 */
	private void setDefaults() {

		missing = decodeColor(default_missingColor);
		empty = decodeColor(default_emptyColor);
	}

	/**
	 * copies colors and name from other color set.
	 */
	public void copyStateFrom(final ColorSet2 other) {

		String[] colorHexVals = other.getColors();
		ArrayList<Color> colors = new ArrayList<Color>(colorHexVals.length);
		
		for(int i = 0; i < colorHexVals.length; i++) {
			
			colors.add(decodeColor(colorHexVals[i]));
		}
		
		float[] floatVals = other.getFractions();
		ArrayList<Double> fracs = new ArrayList<Double>(floatVals.length);
		
		for(int i = 0; i < floatVals.length; i++) {
			
			fracs.add((double)floatVals[i]);
		}
		
		setColorList(colors);
		setFractionList(fracs);
		setMissing(other.getMissing());
		setEmpty(other.getEmpty());
		setName(other.getName());
	}
	
	/**
	 * sets colors and name to reflect <code>ConfigNode</code>
	 */
	@Override
	public void setConfigNode(Preferences parentNode) {

		if(parentNode != null) {
			this.configNode = parentNode;
			
		} else {
			LogBuffer.println("Could not find or create ColorSet " +
					"node because parentNode was null.");
		}
		
		// Check if colors/ fractions have been defined and if nodes exist.
		// If no nodes exist, create them (mainly used for initial default
		// nodes because they are statically initialized in ColorPresets2 
		// and can't be assigned to a configNode at that point.
		try {
			if(!configNode.nodeExists("Color1") && colorList.size() > 0) {
				for(int i = 0; i < colorList.size(); i++) {
					
					configNode.put("Color" + (i + 1), 
							encodeColor(colorList.get(i)));
				}
				
				configNode.putInt("colorNum", colorList.size());
				
			} else {
				missing = decodeColor(configNode
						.get("missing", default_missingColor));
				empty = decodeColor(configNode.get(
						"empty", default_emptyColor));
				name = configNode.get("name", default_name);
			}
			
			if(!configNode.nodeExists("Fraction1") && fractionList.size() > 0) {
				for(int i = 0; i < fractionList.size(); i++) {
					
					configNode.put("Fraction" + (i + 1), 
							Double.toString(fractionList.get(i)));
				}
				
				configNode.putInt("fracNum", fractionList.size());
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		
	}

	/* inherit description */
	@Override
	public String toString() {
		
		String[] colors = getColors();
		
		String colorString = "";
		if(colors.length > 0) {
			for(String color : colors) {
				
				colorString += color + "; ";
			}
		} else {
			colorString = "No colors in node.";
		}
		
		return "ColorSet " + getName() + "\n" + "Colors: " + colorString 
				+ " missing: " + getMissing().toString() + "\t" + "empty: "
				+ getEmpty().toString() + "\t";
	}

	/**
	 * Sets the colorList for this Preset instance. 
	 * @param colors
	 */
	public void setColorList(ArrayList<Color> colors) {
		
		if(colorList != null) {
			this.colorList = colors;
			
		} else {
			LogBuffer.println("ColorList in ColorSet2.setColorList() is null!");
		}
		
		if(configNode != null) {
			for(int i = 0; i < colorList.size(); i++) {
				
				configNode.put("Color" + (i + 1), 
						encodeColor(colorList.get(i)));
			}
			
			configNode.putInt("colorNum", colorList.size());
		}
	}
	
	/**
	 * Retrieves hex representations of colors currently stored in the
	 * configNode of ColorSet2.
	 * @return
	 */
	public String[] getColors() {
		
		int colorNum = configNode.getInt("colorNum", 0);
		String[] colors = new String[colorNum];
		
		for(int i = 0; i < colorNum; i++) {
			
			colors[i] = configNode.get("Color" + (i + 1), "NoColor");
		}
		
		return colors;
	}
	
	/**
	 * Sets the fractionList for this preset instance. Combined with the 
	 * colorList, a LinearGradient can be recreated in ColorGradientChooser.
	 * @param fractions
	 */
	public void setFractionList(ArrayList<Double> fractions) {
		
		if(fractionList != null) {
			this.fractionList = fractions;
			
		} else {
			LogBuffer.println("FractionList in ColorSet2.setFractionList() " +
					"is null!");
		}
		
		if(configNode != null) {
			for(int i = 0; i < fractionList.size(); i++) {
				
				configNode.put("Fraction" + (i + 1), 
						Double.toString(fractionList.get(i)));
			}
			
			configNode.putInt("fracNum", fractionList.size());
		}
	}
	
	/**
	 * Retrieves the fraction values stored in the current configNode.
	 * @return
	 */
	public float[] getFractions() {
		
		int fractNum = configNode.getInt("fracNum", 0);
		float[] fractions = new float[fractNum];
		
		for(int i = 0; i < fractNum; i++) {
			
			fractions[i] = (float)Double.parseDouble(
					configNode.get("Fraction" + (i + 1), "0.0"));
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
	 * Color for missing values.
	 */
	public void setMissing(final String newString) {
		
		missing = decodeColor(newString);
		if (configNode != null) {
			configNode.put("missing", newString);
		}
	}

	/**
	 * Color for empty values.
	 */
	public void setEmpty(final String newString) {
		
		empty = decodeColor(newString);
		if (configNode != null) {
			configNode.put("empty", newString);
		}
	}

	/**
	 * Color for missing values.
	 */
	public void setMissing(final Color newColor) {
		
		missing = newColor;
		if (configNode != null) {
			configNode.put("missing", encodeColor(missing));
		}
	}

	/**
	 * Color for empty values.
	 */
	public void setEmpty(final Color newColor) {
		
		empty = newColor;
		if (configNode != null) {
			configNode.put("empty", encodeColor(empty));
		}
	}

	/**
	 * The name of this color set
	 */
	public void setName(final String name) {
		
		this.name = name;
		if (configNode != null) {
			configNode.put("name", name);
		}
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
