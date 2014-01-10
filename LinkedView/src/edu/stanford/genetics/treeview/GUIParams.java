package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Font;

public class GUIParams {

	// Default
	public static Font FONTS = new Font("Sans Serif", Font.PLAIN, 18);
	public static Font FONTL = new Font("Sans Serif", Font.PLAIN, 24);
	public static Font HEADER = new Font("Sans Serif", Font.BOLD, 18);

	public static Color PANEL_BG = new Color(60, 60, 60, 255);
	public static Color LIGHTGRAY = new Color(180, 180, 180, 255);
	public static Color DARKGRAY = new Color(200, 200, 200, 255);
	public static Color TEXT = new Color(200, 200, 200, 255);
	public static Color BORDERS = new Color(200, 200, 200, 255);
	public static Color TITLE_BG = new Color(255, 205, 65, 255);
	public static Color TITLE_TEXT = new Color(20, 20, 20, 255);
	public static Color ELEMENT = new Color(255, 205, 65, 255);
	public static Color ELEMENT_HOV = new Color(255, 174, 77, 255);
	public static Color BG_COLOR = new Color(39, 40, 34, 255);
	public static Color RED1 = new Color(240, 80, 50, 255);
	public static Color TABLEHEADERS = new Color(255, 205, 120, 255);
	public static Color PROGRESS1 = new Color(60, 60, 60, 255);
	public static Color PROGRESS2 = new Color(60, 60, 60, 255);

	public static void setDayLight() {

		PANEL_BG = new Color(240, 240, 240, 255);
		LIGHTGRAY = new Color(140, 140, 140, 255);
		DARKGRAY = new Color(180, 180, 180, 255);
		TEXT = new Color(20, 20, 20, 255);
		BORDERS = new Color(100, 100, 100, 255);
		TITLE_BG = new Color(44, 185, 247, 255);
		TITLE_TEXT = new Color(254, 254, 254, 255);
		ELEMENT = new Color(44, 185, 247, 255);
		ELEMENT_HOV = new Color(122, 214, 255, 255);
		BG_COLOR = new Color(254, 254, 254, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(191, 235, 255, 255);
		PROGRESS1 = new Color(0, 0, 0, 255);
		PROGRESS2 = new Color(254, 254, 254, 255);
	}

	public static void setNight() {

		PANEL_BG = new Color(60, 60, 60, 255);
		LIGHTGRAY = new Color(180, 180, 180, 255);
		DARKGRAY = new Color(200, 200, 200, 255);
		TEXT = new Color(200, 200, 200, 255);
		BORDERS = new Color(200, 200, 200, 255);
		TITLE_BG = new Color(255, 205, 65, 255);
		TITLE_TEXT = new Color(20, 20, 20, 255);
		ELEMENT = new Color(255, 205, 65, 255);
		ELEMENT_HOV = new Color(255, 174, 77, 255);
		BG_COLOR = new Color(39, 40, 34, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(255, 205, 120, 255);
		PROGRESS1 = new Color(60, 60, 60, 255);
		PROGRESS2 = new Color(60, 60, 60, 255);
	}
}
