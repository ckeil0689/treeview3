package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import edu.stanford.genetics.treeview.LogBuffer;

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
			
			/* TempImg to avoid issues when loading fails */
			BufferedImage tempImg;
			BufferedImage tempLight;
			
			try {
				final ClassLoader classLoader = Thread.currentThread()
						.getContextClassLoader();
				
				InputStream input = classLoader.getResourceAsStream(dragbarID);
				tempImg = ImageIO.read(input);
				input.close();
				
				input = classLoader.getResourceAsStream(dragbarID_light);
				tempLight = ImageIO.read(input);
				input.close();
				
			} catch (IOException e) {
				tempImg = new BufferedImage(10, 10, 
						BufferedImage.TYPE_3BYTE_BGR);
				tempLight = new BufferedImage(10, 10, 
						BufferedImage.TYPE_3BYTE_BGR);
				LogBuffer.println("Could not load dragbar icon.");
				LogBuffer.logException(e);
			}
			
			this.img_dark = tempImg;
			this.img_light = tempLight;
			
			this.img = img_light;
			
			this.lightColor = this.getBackground();
			this.darkColor = new Color(237, 237, 237);
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
