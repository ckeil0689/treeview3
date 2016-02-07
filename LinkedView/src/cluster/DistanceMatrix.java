package cluster;

import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.JOptionPane;

import edu.stanford.genetics.treeview.LogBuffer;
import utilities.Helper;

public class DistanceMatrix {

	private double[][] matrix;

	private int min_row_index;
	private int min_col_index;

	private int size;

	public DistanceMatrix(final int size) {

		this.matrix = new double[size][];
		this.size = size;

		/* To set the minimum indices. */
		findCurrentMin(Double.MAX_VALUE);
	}

	public void setMatrix(final double[][] matrix) {

		this.matrix = matrix;
		this.size = matrix.length;
	}

	/**
	 * Sets a row of the matrix at the specified index.
	 *
	 * @param row
	 *            The array to be set.
	 * @param index
	 *            Index of the row to be set.
	 */
	public void setRow(final double[] row, final int index) {

		this.matrix[index] = row;
	}

	/**
	 * Returns a reference to the row at the provided index of the matrix.
	 *
	 * @param index
	 * @return
	 */
	public double[] getRow(final int index) {

		return this.matrix[index];
	}

	/**
	 * Deletes the row and column at a given index in the distance matrix
	 * object, effectively shrinking it.
	 *
	 * @param index
	 *            Index of the row and column to be deleted.
	 */
	public void deleteIndex(final int index) {

		size -= 1;
		final double[][] newMatrix = new double[size][];

		/*
		 * This should shift the elements of distMatrix up by one once it
		 * reaches the index which should be removed. A double of the last array
		 * in the element should remain at the end.
		 */
		for (int i = 0; i < newMatrix.length; i++) {

			newMatrix[i] = (i < index) ? matrix[i] : matrix[i + 1];
		}

		/* Delete the column */
		for (int i = 0; i < newMatrix.length; i++) {

			final double[] oldArray = newMatrix[i];

			final int length = (i < index) ? oldArray.length
					: oldArray.length - 1;
			final double[] array = new double[length];

			/* Fill the row, skipping the value at 'index' */
			for (int j = 0; j < array.length; j++) {

				array[j] = (j < index) ? oldArray[j] : oldArray[j + 1];
			}

			/* Replace the row */
			newMatrix[i] = array;
		}

		this.matrix = newMatrix;
	}

	/**
	 * Replaces a row at the provided index with another new row.
	 *
	 * @param newRow
	 * @param index
	 */
	public void replaceIndex(final double[] newRow, final int index) {

		/* Make sure to set a deep copy, so it won't be a reference */
		final double[] replacementRow = new double[index];
		System.arraycopy(newRow, 0, replacementRow, 0, index);

		this.matrix[index] = replacementRow;
	}

	/**
	 * Clones a supplied matrix and stores it in the matrix instance of this
	 * object.
	 *
	 * @param oldMatrix
	 *            The matrix to be cloned.
	 * @return
	 */
	public void cloneFrom(final DistanceMatrix oldMatrix) {

		this.matrix = new double[oldMatrix.getSize()][];

		for (int i = 0; i < oldMatrix.getSize(); i++) {

			final double[] oldList = oldMatrix.getRow(i);
			final double[] newList = new double[oldList.length];

			System.arraycopy(oldList, 0, newList, 0, oldList.length);

			matrix[i] = newList;
		}
	}

	/**
	 * Finds and returns the current minimum value in the cluster matrix. The
	 * minimum value determines which rows/ columns are closest together and
	 * will be clustered. They will form a new row that replaces the other two
	 * and as a result a new minimum must be found at each step to determine the
	 * new row pair to be clustered. Complexity: O(n^2)
	 *
	 * @return The minimum value in the current distance matrix.
	 */
	public double findCurrentMin(final double oldMin) {

		/* New min must be bigger than old min */
		double newMin = Double.MAX_VALUE;

		for (int i = 0; i < matrix.length; i++) {

			final double[] row = matrix[i];

			for (int j = 0; j < row.length; j++) {

				final double element = row[j];

				if ((element > oldMin || Helper.nearlyEqual(element, oldMin))
						&& element < newMin) {
					newMin = element;
					min_row_index = i;
					min_col_index = j;
				}
			}
		}

		return newMin;
	}

	/**
	 * Returns the amount of rows in the current distance matrix object.S
	 *
	 * @return
	 */
	public int getSize() {

		return matrix.length;
	}

	/**
	 * Returns the row index of the matrix' current minimum value.
	 *
	 * @return int Row index of current matrix minimum.
	 */
	public int getMinRowIndex() {

		return min_row_index;
	}

	/**
	 * Returns the col index of the matrix' current minimum value.
	 *
	 * @return int Col index of current matrix minimum.
	 */
	public int getMinColIndex() {

		return min_col_index;
	}

	/**
	 * Writes the distance matrix values to a tab-delimited TXT-file.
	 * 
	 * @param axis
	 */
	public void writeMatrix(String axis) {

		String fileName = "test_distMatrix_" + axis + ".txt";
		File file = new File(fileName);
		try {
			if (file.exists()) {
				int n = JOptionPane.showConfirmDialog(Frame.getFrames()[0],
						"File already exists. Overwrite?", "Confirmation",
						JOptionPane.YES_NO_OPTION);

				switch (n) {

				case JOptionPane.YES_OPTION:
					file.createNewFile();
					break;
				case JOptionPane.NO_OPTION:
				default:
					file = getNewFile(fileName);
					file.createNewFile();
					break;
				}
			}

			BufferedWriter bw = new BufferedWriter((new OutputStreamWriter(
					new FileOutputStream(file.getAbsoluteFile()), "UTF-8")));

			for (double[] row : matrix) {
				for (double val : row) {
					bw.write(Double.toString(val) + "\t");
				}
				bw.write("\n");
			}

			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			LogBuffer.logException(e);
			e.printStackTrace();
		}
	}

	// TODO can be static -> no instance variables used here.
	private File getNewFile(String oldName) {

		File file = new File(oldName);
		int fileCount = 0;

		while (file.exists()) {
			fileCount++;
			file = new File("test_distMatrix_" + fileCount + ".txt");
		}

		return file;
	}
}
