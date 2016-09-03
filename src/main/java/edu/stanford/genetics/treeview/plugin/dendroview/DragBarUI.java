package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import Utilities.GUIFactory;

public class DragBarUI extends BasicSplitPaneUI {

	private String dragbarID;
	private String dragbarID_light;

	public DragBarUI(final String dragbarID, final String dragbarID_light) {

		super();
		this.dragbarID = dragbarID;
		this.dragbarID_light = dragbarID_light;
	}

	@Override
	public BasicSplitPaneDivider createDefaultDivider() {

		return new CustomDivider(this);
	}

	private class CustomDivider extends BasicSplitPaneDivider {

		/**
		 * Default serial version ID to keep Eclipse happy...
		 */
		private static final long serialVersionUID = 1L;

		private BufferedImage img;
		private BufferedImage img_dark;
		private BufferedImage img_light;

		private Color lightColor;
		private Color darkColor;
		private Color foreColor;

		public CustomDivider(BasicSplitPaneUI ui) {

			super(ui);

			this.img_dark = GUIFactory.getIconImage(dragbarID);
			this.img_light = GUIFactory.getIconImage(dragbarID_light);

			this.img = img_light;

			this.lightColor = new Color(230, 230, 230);
			this.darkColor = new Color(225, 225, 225);
			this.foreColor = lightColor;

			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseEntered(MouseEvent e) {

					img = img_dark;
					foreColor = darkColor;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {

					img = img_light;
					foreColor = lightColor;
					repaint();
				}
			});
		}

		@Override
		public void paint(final Graphics g) {

			super.paint(g);

			Point leftCorner = getLeftCornerImgPoint();
			int x = leftCorner.x;
			int y = leftCorner.y;

			g.setColor(foreColor);
			g.fillRect(0, 0, getSize().width, getSize().height);
			g.drawImage(img, x, y, img.getWidth(), img.getHeight(), this);
		}

		private Point getLeftCornerImgPoint() {

			Point leftCorner;

			int center_w = getWidth() / 2;
			int center_h = getHeight() / 2;

			int w = center_w - img.getWidth() / 2;
			int h = center_h - img.getHeight() / 2;

			leftCorner = new Point(w, h);

			return leftCorner;
		}
	}
}
