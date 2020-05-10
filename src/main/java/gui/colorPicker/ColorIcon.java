package gui.colorPicker;

import javax.swing.*;
import java.awt.*;

/**
 * This class is used to generate an icon which can be added to a button. The
 * 50x50 icon is used to display the current color of the parameter which the
 * button affects.
 * 
 * @author chris0689
 *
 */
public class ColorIcon implements Icon {

	private static final Color DEFAULT_COLOR = Color.GRAY;
	private static final int WIDTH = 15;
	private static final int HEIGHT = 15;

	private Color iconColor;

	/**
	 * Default constructor. Generates an icon with the default color.
	 */
	public ColorIcon() {

		this(DEFAULT_COLOR);
	}

	/**
	 * Generate a color icon with a specified color.
	 * 
	 * @param iconColor
	 *            The color of the icon.
	 */
	public ColorIcon(Color iconColor) {

		this.iconColor = iconColor;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {

		/* filled area */
		g.setColor(iconColor);
		g.fillRect(x, y, getIconWidth(), getIconHeight());
	}

	@Override
	public int getIconWidth() {

		return ColorIcon.WIDTH;
	}

	@Override
	public int getIconHeight() {

		return ColorIcon.HEIGHT;
	}

	public void setColor(Color newColor) {

		iconColor = newColor;
	}

}
