///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
// *
// * END_HEADER 
// */

//package edu.stanford.genetics.treeview.model;
//
//import java.util.ArrayList;
//
//import edu.stanford.genetics.treeview.LogBuffer;
//
///**
// * 
// * @author aloksaldanha
// * 
// *         Represents a rectangle of data, where some columns are strings and
// *         some columns are doubles.
// */
//public class RectData {
//
//	private final Column[] dataArray;
//
//	/**
//	 * 
//	 */
//	public RectData(final String[] names, final ColumnFormat[] formats,
//			final int gap) {
//
//		final int col = names.length;
//		dataArray = new Column[col];
//
//		for (int i = 0; i < names.length; i++) {
//			dataArray[i] = ColumnFormat.initColumn(formats[i], names[i], gap);
//		}
//	}
//
//	public int addData(final String[] data) {
//
//		int index = 0;
//		final int col = getCol();
//		final int len = data.length;
//
//		for (int i = 0; i < col; i++) {
//			if (i < len) {
//				index = dataArray[i].addData(data[i]);
//			} else {
//				index = dataArray[i].addData(null);
//			}
//		}
//		return index;
//	}
//
//	public String getString(final int row, final int col) {
//
//		return dataArray[col].getString(row);
//	}
//
//	public double getDouble(final int row, final int col) {
//
//		return dataArray[col].getDouble(row);
//	}
//
//	public int getRow() {
//		if (dataArray.length < 1) {
//			return 0;
//		} else {
//			return dataArray[0].getNum();
//		}
//	}
//
//	public int getCol() {
//
//		return dataArray.length;
//	}
//
//	public String getColumnName(final int index) {
//
//		return dataArray[index].getName();
//	}
//
//	// make it works like Vector
//	public Object elementAt(final int index) {
//
//		final int col = getCol();
//		final String[] string = new String[col];
//		if (index == 0) {
//			for (int i = 0; i < col; i++) {
//				string[i] = dataArray[i].getName();
//			}
//		} else {
//			for (int i = 0; i < col; i++) {
//				string[i] = getString(index - 1, i);
//			}
//		}
//		return string;
//	}
//
//	public int size() {
//
//		return getRow() + 1;
//	}
//
//	public Object firstElement() {
//
//		return elementAt(0);
//	}
//}
//
///**
// * @author gcong
// * 
// *         This represents the column of a RectData object.
// */
//abstract class Column {
//
//	protected String name;
//	protected int gap, num;
//	protected ArrayList<Object> dataList;
//	protected boolean isDouble;
//
//	protected abstract ColumnFormat getFormat();
//
//	protected abstract double getDouble(int index, int offset);
//
//	protected abstract String getString(int index, int offset);
//
//	protected abstract void addData(int index, int offset, String string);
//
//	protected abstract Object initData();
//
//	/**
//	 * 
//	 */
//	public Column(final String name, final int gap) {
//
//		this.name = name;
//		this.gap = gap;
//		dataList = new ArrayList<Object>();
//		num = 0;
//	}
//
//	protected int incIndex() {
//
//		if (num % gap == 0) {
//
//			dataList.add(initData());
//		}
//
//		return (num++) % gap;
//	}
//
//	public String getName() {
//
//		return name;
//	}
//
//	public String getString(final int index) {
//
//		final int ind = index / gap;
//		final int off = index % gap;
//
//		return getString(ind, off);
//	}
//
//	public double getDouble(final int index) {
//
//		final int ind = index / gap;
//		final int off = index % gap;
//
//		return getDouble(ind, off);
//	}
//
//	public int addData(final String string) {
//
//		final int off = incIndex();
//		final int ind = dataList.size() - 1;
//		addData(ind, off, string);
//
//		return num;
//	}
//
//	public int getNum() {
//
//		return num;
//	}
//}
//
//class DoubleColumn extends Column {
//
//	public DoubleColumn(final String name, final int gap) {
//
//		super(name, gap);
//	}
//
//	@Override
//	public String getString(final int index, final int offset) {
//
//		final double data = ((double[]) dataList.get(index))[offset];
//		return (data == Double.NaN) ? null : "" + data;
//	}
//
//	@Override
//	public double getDouble(final int index, final int offset) {
//
//		return ((double[]) dataList.get(index))[offset];
//	}
//
//	@Override
//	protected void addData(final int index, final int offset,
//			final String string) {
//
//		double data;
//		if (string == null) {
//			data = Double.NaN;
//		} else
//			try {
//				data = Double.parseDouble(string);
//			} catch (final Exception e) {
//				LogBuffer.println("error converting double:" + e);
//				e.printStackTrace();
//				data = Double.NaN;
//			}
//		((double[]) dataList.get(index))[offset] = data;
//	}
//
//	@Override
//	protected Object initData() {
//
//		return new double[gap];
//	}
//
//	@Override
//	public ColumnFormat getFormat() {
//
//		return ColumnFormat.DoubleFormat;
//	}
//}
//
//class ColumnFormat {
//
//	public static final ColumnFormat StringFormat = new ColumnFormat(
//			"String Format");
//	public static final ColumnFormat DoubleFormat = new ColumnFormat(
//			"Double Format");
//	public static final ColumnFormat IntFormat = new ColumnFormat("Int Format");
//
//	private final String name;
//
//	private ColumnFormat(final String name) {
//
//		this.name = name;
//	}
//
//	/**
//	 * 
//	 */
//	@Override
//	public String toString() {
//
//		return name;
//	}
//
//	public static Column initColumn(final ColumnFormat format,
//			final String name, final int gap) {
//
//		if (format == StringFormat) {
//			return new StringColumn(name, gap);
//		} else if (format == DoubleFormat) {
//			return new DoubleColumn(name, gap);
//		} else if (format == IntFormat) {
//			return new IntColumn(name, gap);
//		}
//		return null;
//	}
//}
//
//class IntColumn extends Column {
//
//	/**
//	 * @param name
//	 * @param gap
//	 */
//	public IntColumn(final String name, final int gap) {
//
//		super(name, gap);
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.model.lbl.Column#getFormat()
//	 */
//	@Override
//	public ColumnFormat getFormat() {
//
//		return ColumnFormat.IntFormat;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.model.lbl.Column#getString(int, int)
//	 */
//	@Override
//	protected String getString(final int index, final int offset) {
//
//		final double data = ((int[]) dataList.get(index))[offset];
//		return (data == 0) ? null : "" + data;
//	}
//
//	@Override
//	protected double getDouble(final int index, final int offset) {
//
//		return ((int[]) dataList.get(index))[offset];
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.model.lbl.Column#addData(int, int,
//	 * java.lang.String)
//	 */
//	@Override
//	protected void addData(final int index, final int offset,
//			final String string) {
//		final int data = (string == null) ? 0 : Integer.parseInt(string);
//		((int[]) dataList.get(index))[offset] = data;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.model.lbl.Column#initData()
//	 */
//	@Override
//	protected Object initData() {
//
//		return new int[gap];
//	}
//}
//
//class StringColumn extends Column {
//
//	public StringColumn(final String name, final int gap) {
//
//		super(name, gap);
//	}
//
//	@Override
//	protected Object initData() {
//
//		return new byte[gap][];
//	}
//
//	@Override
//	protected String getString(final int index, final int offset) {
//
//		final byte[] tmp = ((byte[][]) dataList.get(index))[offset];
//		return (tmp == null) ? null : new String(tmp);
//	}
//
//	@Override
//	protected double getDouble(final int index, final int offset) {
//
//		final String string = getString(index, offset);
//		return (string == null) ? Double.NaN : Double.parseDouble(string);
//	}
//
//	@Override
//	protected void addData(final int index, final int offset,
//			final String string) {
//
//		if (string != null) {
//
//			((byte[][]) dataList.get(index))[offset] = string.getBytes();
//		}
//	}
//
//	@Override
//	public ColumnFormat getFormat() {
//
//		return ColumnFormat.StringFormat;
//	}
//}