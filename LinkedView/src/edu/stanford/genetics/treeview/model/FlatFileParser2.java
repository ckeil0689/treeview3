///* BEGIN_HEADER                                              Java TreeView
// *
// * $Author: alokito $
// * $RCSfile: FlatFileParser2.java,v $
// * $Revision: 1.11 $
// * $Date: 2010-05-02 13:39:00 $
// * $Name:  $
// *
// * This file is part of Java TreeView
// * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved.
// *
// * This software is provided under the GNU GPL Version 2. In particular, 
// *
// * 1) If you modify a source file, make a comment in it containing your name and the date.
// * 2) If you distribute a modified version, you must do it under the GPL 2.
// * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
// *
// * A full copy of the license can be found in gpl.txt or online at
// * http://www.gnu.org/licenses/gpl.txt
// *
// * END_HEADER 
// */
//package edu.stanford.genetics.treeview.model;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileReader;
//import java.io.FilterInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.Reader;
//import java.util.Vector;
//
//import edu.stanford.genetics.treeview.LoadException;
//import edu.stanford.genetics.treeview.LogBuffer;
//import edu.stanford.genetics.treeview.ProgressTrackable;
//
///**
// * parses a tab-delimited file into a vector of String []. Each String []
// * represents a row of the file.
// * 
// * This object should be created and configured. None of the real action gets
// * started until the loadIntoTable() routine is called. After loading, the
// * object can be reconfigured and reused to load other files.
// */
//
//public class FlatFileParser2 {
//	
//	/* resource types */
//	public static final int FILE = 0;
//	public static final int URL = 1;
//	
//	private int resourceType = 0;
//	
//	private ProgressTrackable progressTrackable;
//	private String resource;
//
//	public ProgressTrackable getProgressTrackable() {
//		
//		return progressTrackable;
//	}
//
//	public void setProgressTrackable(final ProgressTrackable progressTrackable) {
//		
//		this.progressTrackable = progressTrackable;
//	}
//
//	public String getResource() {
//		
//		return resource;
//	}
//
//	public void setResource(final String resource) {
//		
//		this.resource = resource;
//	}
//
//	public int getResourceType() {
//		
//		return resourceType;
//	}
//
//	public void setResourceType(final int resourceType) {
//		
//		this.resourceType = resourceType;
//	}
//
//	public RectData loadIntoTable() throws LoadException, IOException {
//		
//		InputStream stream;
//		if (getResource().startsWith("http://")) {
//			try {
//				setResourceType(URL);
//				stream = openStream();
//			} catch (final Exception e) {
//				setResourceType(FILE);
//				stream = openStream();
//			}
//		} else {
//			try {
//				setResourceType(FILE);
//				stream = openStream();
//			} catch (final Exception e) {
//				setResourceType(URL);
//				stream = openStream();
//			}
//		}
//		return loadIntoTable(stream);
//	}
//
//	private void setLength(final int i) {
//		
//		if (progressTrackable != null) {
//			progressTrackable.setLength(i);
//		}
//	}
//
//	private void setValue(final int i) {
//		
//		if (progressTrackable != null) {
//			progressTrackable.setValue(i);
//		}
//	}
//
//	private int getValue() {
//		
//		if (progressTrackable != null) {
//			return progressTrackable.getValue();
//			
//		} else {
//			return 0;
//		}
//
//	}
//
//	private void incrValue(final int i) {
//		
//		if (progressTrackable != null) {
//			progressTrackable.incrValue(i);
//		}
//	}
//
//	/** returns a list of vectors of String [], representing data from file. */
//	private RectData loadIntoTable(final InputStream inputStream)
//			throws IOException, LoadException {
//		
//		final MeteredStream ms = new MeteredStream(inputStream);
//		final BufferedReader reader = new BufferedReader(new InputStreamReader(
//				ms));
//		return FlatFileReader.load(reader, getParseQuotedStrings());
//	}
//
//	/** opens a stream from the resource */
//	private InputStream openStream() throws LoadException {
//		
//		InputStream is;
//		final String file = getResource();
//		
//		if (getResourceType() == FILE) {
//			try {
//				final File fd = new File(file);
//				is = new MeteredStream(new FileInputStream(fd));
//				setLength((int) fd.length());
//				
//			} catch (final Exception ioe) {
//				throw new LoadException("File " + file
//						+ " could not be opened: " + ioe.getMessage(),
//						LoadException.CDTPARSE);
//			}
//		} else {
//			try {
//				final java.net.URL url = new java.net.URL(file);
//				final java.net.URLConnection conn = url.openConnection();
//				
//				is = new MeteredStream(conn.getInputStream());
//				setLength(conn.getContentLength());
//				
//			} catch (final IOException ioe2) {
//				throw new LoadException("Url " + file
//						+ " could not be opened: " + ioe2.getMessage(),
//						LoadException.CDTPARSE);
//			}
//		}
//		return is;
//	}
//
//	class MeteredStream extends FilterInputStream {
//		MeteredStream(final InputStream is) {
//			
//			super(is);
//		}
//
//		@Override
//		public int read() throws IOException {
//			
//			incrValue(1);
//			return super.read();
//		}
//
//		// the following should be covered by the more general read...
//		// public int read(byte [] b);
//
//		@Override
//		public int read(final byte[] b, final int off, final int len)
//				throws IOException {
//			
//			final int ret = super.read(b, off, len);
//			if (ret != -1) {
//				// for some reason, got factor of two error in sizes...
//				incrValue(ret / 2);
//			}
//			return ret;
//		}
//
//		@Override
//		public long skip(final long n) throws IOException {
//			
//			final long ret = super.skip(n);
//			if (ret != -1) {
//				// for some reason, got factor of two error in sizes...
//				incrValue((int) ret / 2);
//			}
//			return ret;
//		}
//
//		int markedValue = 0;
//
//		@Override
//		public void mark(final int readLimit) {
//			
//			super.mark(readLimit);
//			markedValue = getValue();
//		}
//
//		@Override
//		public void reset() throws IOException {
//			
//			super.reset();
//			setValue(markedValue);
//		}
//	}
//
//	/**
//	 * parse quoted strings?
//	 */
//	private boolean pqs = true;
//
//	/**
//	 * @param parseQuotedStrings
//	 */
//	public void setParseQuotedStrings(final boolean parseQuotedStrings) {
//		pqs = parseQuotedStrings;
//	}
//
//	public boolean getParseQuotedStrings() {
//		return pqs;
//	}
//}
//
///**
// * @author gcong
// * 
// *         This class loads flat files into RectData objects. It should not
// *         waste as much ram as parsing into strings. Although it's not clear
// *         why the ram doesn't get returned to the heap when you parse into
// *         strings.
// * 
// *         Note: null entries in number columns will be assigned the value NaN.
// * 
// */
//class FlatFileReader {
//	
//	static final char DEFAULT_SEP = '\t';
//	static final int DEFAULT_TESTSIZE = 10;
//	static final int DEFAULT_GAPSIZE = 1000;
//	static final String[][] filters = { { "NA", null } };
//
//	private FlatFileReader() {
//	}
//
//	public static void main(final String[] args) {
//		try {
//			final BufferedReader br1 = new BufferedReader(new FileReader(
//					args[0]));
//			final RectData data1 = FlatFileReader.load(br1, true);
//			System.out.println("Data1 sie = " + data1.size());
//			br1.close();
//			// Vector data = new FlatFile2RectData().load2(br);
//			final BufferedReader br2 = new BufferedReader(new FileReader(
//					args[0]));
//			final Vector<String[]> data2 = TestLoad.load(br2);
//			System.out.println("Data2 sie = " + data2.size());
//			br2.close();
//			for (int i = 0; i < data1.size() && i < 1000; i++) {
//				// System.out.println("--------" + i + "-------------");
//				final String[] str1 = (String[]) data1.elementAt(i);
//				final String[] str2 = data2.elementAt(i);
//				for (int j = 0; j < str1.length; j++) {
//
//					if (str1[j] == null && str2[j] == null) {
//						continue;
//					}
//					if (str1[j] == null) {
//						System.out.println(i + " " + j + " 1-");
//					}
//					if (str2[j] == null) {
//						System.out.println(i + " " + j + " 2-");
//					}
//					if (FlatFileReader.isDoubleString(str1[j])
//							&& FlatFileReader.isDoubleString(str2[j])) {
//						final double d1 = Double.parseDouble(str1[j]);
//						final double d2 = Double.parseDouble(str2[j]);
//						if (d1 != d2) {
//							System.out.println("\t*" + str1[j] + "* *"
//									+ str2[j] + "*");
//						}
//					} else {
//						if (!str2[j].equalsIgnoreCase(str1[j])) {
//							System.out.println("\t*" + str1[j] + "* *"
//									+ str2[j] + "*");
//						}
//					}
//				}
//			}
//		} catch (final Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	static public RectData load(final Reader reader,
//			final boolean parseQuotedStrings) throws IOException {
//		return load(reader, DEFAULT_SEP, 1000, parseQuotedStrings);
//	}
//
//	static public RectData load(final Reader reader, final char sep,
//			final boolean parseQuotedStrings) throws IOException {
//		return load(reader, sep, DEFAULT_GAPSIZE, DEFAULT_TESTSIZE,
//				parseQuotedStrings);
//	}
//
//	static public RectData load(final Reader reader, final char sep,
//			final int gapSize, final boolean parseQuotedStrings)
//			throws IOException {
//		return load(reader, sep, gapSize, DEFAULT_TESTSIZE, parseQuotedStrings);
//	}
//
//	/**
//	 * Note: null entries in number columns will be assigned the value NaN.
//	 * 
//	 * @param br
//	 *            a reader from which to get data
//	 * @param sep
//	 *            a separator character
//	 * @param gapSize
//	 *            completely unused. What's it for?
//	 * @param testSize
//	 *            numer of lines to fetch to determine column types.
//	 * @return RectData representing the values loaded from the file.
//	 * @throws IOException
//	 */
//	static public RectData load(final Reader br, final char sep,
//			final int gapSize, final int testSize,
//			final boolean parseQuotedStrings) throws IOException {
//
//		final FlatFileStreamLiner st = new FlatFileStreamLiner(br, sep,
//				parseQuotedStrings);
//
//		String[] names = null;// no name is allowed
//		if (st.nextLine()) {
//			names = st.getLineTokens();
//		}
//
//		String[][] lines = new String[testSize][];
//		int count = 0;
//		while (count < testSize && st.nextLine()) {
//			lines[count++] = st.getLineTokens();
//		}
//		if (count < testSize) {
//			// file has fewer that testSize lines, must do something.
//			final String[][] newLines = new String[count][];
//			for (int i = 0; i < count; i++) {
//				newLines[i] = lines[i];
//			}
//			lines = newLines;
//		}
//
//		ColumnFormat[] formats = checkColumnFormat(lines);
//
//		// special case: force all gene annotation columns to String
//		formats = makeGeneAnnoString(names, formats);
//
//		final RectData data = new RectData(names, formats, 5000);
//
//		for (int i = 0; i < count; i++) {
//			data.addData(filterString(lines[i])); // data.addData(lines[i]);
//		}
//
//		while (st.nextLine()) {
//			data.addData(filterString(st.getLineTokens()));
//		}
//
//		return data;
//	}
//
//	/**
//	 * If GWEIGHT occurs in names, it will force GWEIGHT and all columns to the
//	 * left to be String.
//	 * 
//	 * @param names
//	 * @param formats
//	 * @return
//	 */
//	private static ColumnFormat[] makeGeneAnnoString(final String[] names,
//			final ColumnFormat[] formats) {
//		// if there's a "GWEIGHT", assume everything is a string.
//		final int len = names.length;
//		int i;
//		for (i = 0; i < len; i++)
//			if ("GWEIGHT".equals(names[i]))
//				break;
//		if (i < len)
//			for (; i >= 0; i--)
//				formats[i] = ColumnFormat.StringFormat;
//		return formats;
//	}
//
//	static public ColumnFormat[] checkColumnFormat(final String[][] lines) {
//		int len = lines[0].length;
//		for (int i = 1; i < lines.length; i++) {
//			if (lines[i] == null) {
//				// this should never happen.
//				LogBuffer
//						.println("FlatFileParser.checkColumnFormat got null line");
//				continue;
//			}
//			if (len < lines[i].length) {
//				len = lines[i].length;
//			}
//		}
//		final ColumnFormat[] formats = new ColumnFormat[len];
//		for (int i = 0; i < len; i++) {
//			boolean sawNum = false; // true if ith column contains numbers
//			boolean sawString = false; // true if ith column contains strings
//			for (int j = 0; j < lines.length; j++) {
//				if (lines[j] == null) {
//					// this should never happen.
//					continue;
//				}
//				if (lines[j].length > i) {
//					if (lines[j][i] == null) {
//						continue;
//					} else if (isDoubleString(lines[j][i])) {
//						sawNum = true;
//					} else {
//						sawString = true;
//					}
//				}
//			}
//			if (sawString) {
//				formats[i] = ColumnFormat.StringFormat;
//			} else if (sawNum) {
//				formats[i] = ColumnFormat.DoubleFormat;
//			} else {
//				formats[i] = ColumnFormat.StringFormat;
//			}
//		}
//
//		return formats;
//	}
//
//	static public boolean isDoubleString(final String string) {
//		if (string == null) {
//			return true;
//		}
//		try {
//			Double.parseDouble(string);
//			return true;
//		} catch (final NumberFormatException e) {
//		}
//		return false;
//	}
//
//	static public String[] filterString(final String[] strings) {
//		final int len = strings.length;
//		final String[] str = new String[len];
//		for (int i = 0; i < len; i++) {
//			str[i] = filterString(strings[i]);
//		}
//		return str;
//	}
//
//	static public String filterString(final String string) {
//		if (string != null) {
//			final int len = filters.length;
//			for (int i = 0; i < len; i++) {
//				if (string.equalsIgnoreCase(filters[i][0])) {
//					return filters[i][1];
//				}
//			}
//		}
//		return string;
//	}
//}