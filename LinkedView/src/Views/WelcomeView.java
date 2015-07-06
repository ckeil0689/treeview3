package Views;

import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import Utilities.GUIFactory;
import Utilities.StringRes;

public class WelcomeView {

	// Initial
	private final JPanel loadPanel;
	private final JPanel homePanel;
	private final JPanel title_bg;

	private final JLabel jl;
	private final JLabel jl2;

	private JButton loadButton;
	private JButton loadLastButton;

	private boolean isLoading = false;

	// Loading stuff
	private static JProgressBar loadBar = GUIFactory.createPBar();
	private static JLabel loadLabel = GUIFactory.createLabel("",
			GUIFactory.FONTL);

	public WelcomeView() {

		homePanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);

		title_bg = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING, null);

		final JPanel titleContainer = GUIFactory.createJPanel(false,
				GUIFactory.NO_PADDING, null);

		loadPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);

		final JPanel logo = new ImagePanel();

		jl = GUIFactory.createLabel(StringRes.title_Hello, GUIFactory.FONTL);

		jl2 = GUIFactory.createLabel(StringRes.title_Welcome
				+ StringRes.appName + StringRes.dot, GUIFactory.FONTXXL);

		titleContainer.add(logo, "span 1 2, push, grow, w 100::, h 100::");
		titleContainer.add(jl, "push, alignx 0%, aligny 100%, span, wrap");
		titleContainer.add(jl2, "push, alignx 0%, aligny 0%, span");
		titleContainer.add(new JSeparator(SwingConstants.HORIZONTAL), "w 80%, "
				+ "pushx, alignx 50%, span");

		title_bg.add(titleContainer, "push, growx, align 50%");

		homePanel.add(title_bg, "push,, alignx 50%, span, wrap");
		homePanel.add(loadPanel, "push, grow, alignx 50%");
	}

	public class ImagePanel extends JPanel {

		/**
		 * Default serial version ID to keep Eclipse happy...
		 */
		private static final long serialVersionUID = 1L;

		private BufferedImage image;

		public ImagePanel() {
			try {
				final ClassLoader classLoader = Thread.currentThread()
						.getContextClassLoader();
				final InputStream input = classLoader
						.getResourceAsStream("logo_small.png");

				if (input != null) {
					image = ImageIO.read(input);
					input.close();
				}

			} catch (final IOException ex) {
				LogBuffer.logException(ex);
				image = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
			}
		}

		@Override
		protected void paintComponent(final Graphics g) {

			super.paintComponent(g);

			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}
		}
	}

	/**
	 * Access TreeViewFrame's load_Icon panel.
	 *
	 * @return
	 */
	public JPanel getLoadIcon() {

		return loadPanel;
	}

	/**
	 * Access TreeViewFrame's load_Icon panel.
	 *
	 * @return
	 */
	public JButton getLoadButton() {

		return loadButton;
	}

	public boolean isLoading() {

		return isLoading;
	}

	/**
	 * Equipping the loadButton with an ActionListener
	 *
	 * @param loadData
	 */
	public void addLoadListener(final ActionListener loadData) {

		loadButton.addActionListener(loadData);
	}

	/**
	 * Equipping the loadLastButton with an ActionListener
	 *
	 * @param loadData
	 */
	public void addLoadLastListener(final ActionListener loadData) {

		loadLastButton.addActionListener(loadData);
	}

	public JPanel makeWelcome(FileSet fs) {

		isLoading = false;

		loadPanel.removeAll();

		loadButton = GUIFactory.createLargeBtn("Open...");
		loadButton.requestFocusInWindow();
		loadLastButton = GUIFactory.createBtn("Load last file");

		String filename;
		if (fs != null) {
			filename = fs.getCdt();
			loadLastButton.setEnabled(true);
			((JFrame) JFrame.getFrames()[0]).getRootPane().setDefaultButton(
					loadLastButton);
		} else {
			filename = "none";
			loadLastButton.setEnabled(false);
			((JFrame) JFrame.getFrames()[0]).getRootPane().setDefaultButton(
					loadButton);
		}

		JLabel last = new JLabel("Last file: ");
		last.setFont(GUIFactory.FONTS_B);
		JLabel filePath = new JLabel(filename);

		loadPanel.add(loadButton, "pushx, alignx 50%, aligny 0%, span, wrap");
		loadPanel.add(loadLastButton,
				"pushx, alignx 50%, aligny 0%, span, wrap");
		loadPanel.add(last, "pushx, alignx 100%, aligny 0%");
		loadPanel.add(filePath, "pushx, alignx 0%, aligny 0%");

		loadPanel.revalidate();
		loadPanel.repaint();

		return homePanel;
	}

	public JPanel makeLoading() {

		isLoading = true;

		loadPanel.removeAll();

		final JPanel loadContainer = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT, null);

		jl.setText(StringRes.load_OneSec);
		jl2.setText(StringRes.load_active);

		resetLoadBar();
		setLoadText("Setting up...");

		loadContainer.add(loadLabel, "pushx, alignx 50%, wrap");
		loadContainer.add(loadBar, "pushx, growx");

		loadPanel.add(loadContainer, "pushx, growx, align 50%, w 60%!");

		loadPanel.revalidate();
		loadPanel.repaint();

		return homePanel;
	}

	// LoadBar functions
	/**
	 * Updates the loading bar by setting it to i.
	 *
	 * @param i
	 */
	public static void updateLoadBar(final int i) {

		if (i <= loadBar.getMaximum()) {
			loadBar.setValue(i);
		}
	}

	/**
	 * Resets the loading bar to 0.
	 *
	 * @param i
	 */
	public static void resetLoadBar() {

		loadBar.setValue(0);
	}

	/**
	 * Sets the maximum of the loading bar.
	 *
	 * @param max
	 */
	public static void setLoadBarMax(final int max) {

		loadBar.setMaximum(max);
	}

	/**
	 * Changes the text of the loading label.
	 *
	 * @param text
	 */
	public static void setLoadText(final String text) {

		loadLabel.setText(text);
	}
}
