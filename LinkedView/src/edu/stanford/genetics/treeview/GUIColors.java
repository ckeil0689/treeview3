package edu.stanford.genetics.treeview;

import java.awt.Color;

public class GUIColors {

	//Default
	public static Color GRAY1 = new Color(140, 140, 140, 255);
	public static Color GRAY2 = new Color(180, 180, 180, 255);
	public static Color TEXT = new Color(20, 20, 20, 255);
	public static Color BLUE1 = new Color(44, 185, 247, 255);
	public static Color BLUE2 = new Color(122, 214, 255, 255);
	public static Color BG_COLOR = new Color(254, 254, 254, 255);
	public static Color RED1 = new Color(240, 80, 50, 255);
	public static Color TABLEHEADERS = new Color(191, 235, 255, 255);
	
	public static void setDayLight() {
		
		GRAY1 = new Color(140, 140, 140, 255);
		GRAY2 = new Color(180, 180, 180, 255);
		TEXT = new Color(20, 20, 20, 255);
		BLUE1 = new Color(44, 185, 247, 255);
		BLUE2 = new Color(122, 214, 255, 255);
		BG_COLOR = new Color(254, 254, 254, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(191, 235, 255, 255);
	}
	
	public static void setNight() {
		
		GRAY1 = new Color(180, 180, 180, 255);
		GRAY2 = new Color(200, 200, 200, 255);
		TEXT = new Color(200, 200, 200, 255);
		BLUE1 = new Color(44, 185, 247, 255);
		BLUE2 = new Color(122, 214, 255, 255);
		BG_COLOR = new Color(20, 20, 20, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(191, 235, 255, 255);
	}
}
