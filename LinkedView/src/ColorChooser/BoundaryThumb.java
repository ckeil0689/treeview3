package ColorChooser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

public class BoundaryThumb extends Thumb {

	private boolean isMin;

	public BoundaryThumb(boolean isMin) {

		super(0, 0);

		this.isMin = isMin;
		this.width = 20;
		this.height = 20;

		createThumbPath();
	}

	/**
	 * Uses the GeneralPath class and x/y-coordinates to generate a small
	 * triangular object which will represent an interactive 'thumb'.
	 */
	@Override
	public void createThumbPath() {

		/* Rotation factor */
		int fact = (isMin) ? -1 : 1;

		/* magic numbers in here are pixel offsets */
		innerthumbPath = new GeneralPath();
		innerthumbPath.moveTo(x, y);
		innerthumbPath.lineTo(x - fact * (height + 3), y + fact * (width / 2));
		innerthumbPath.lineTo(x - fact * (height + 3), y - fact * (width / 2));
		innerthumbPath.closePath();

		outerthumbPath = new GeneralPath();
		outerthumbPath.moveTo(x, y);
		outerthumbPath.lineTo(x - fact * (height + 4), y + fact
				* ((width + 4) / 2));
		outerthumbPath.lineTo(x - fact * (height + 4), y - fact
				* ((width + 4) / 2));
		outerthumbPath.closePath();
	}

	/**
	 * Paints the GeneralPath object with the set color and makes the boundary
	 * thumb visible to the user.
	 *
	 * @param g2d
	 */
	@Override
	public void paint(final Graphics2D g2d) {

		g2d.setColor(Color.GRAY);
		g2d.fill(outerthumbPath);

		g2d.setColor(thumbColor);

		/* draw circle above thumb if selected */
		if (isSelected()) {
			drawCenteredCircle(g2d, x, y, 6);
		}

		g2d.fill(innerthumbPath);
	}

	protected boolean isMin() {

		return isMin;
	}

}
