/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Enumeration;
import java.util.Vector;

import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.ProgressTrackable;

/**
 * parses a tab-delimitted file into a vector of String []. Each String []
 * represents a row of the file.
 * 
 * This object should be created and configured. None of the real action gets
 * started until the loadIntoTable() routine is called. After loading, the
 * object can be reconfigured and reused to load other files.
 */

public class FlatFileParser {

	private ProgressTrackable progressTrackable;
	private String resource;

	/* resource types */
	public static final int FILE = 0;
	public static final int URL = 1;
	private int resourceType = 0;

	private boolean cancelled = false;

	public ProgressTrackable getProgressTrackable() {

		return progressTrackable;
	}

	public void setProgressTrackable(final ProgressTrackable progressTrackable) {

		this.progressTrackable = progressTrackable;
	}

	public String getResource() {

		return resource;
	}

	public void setResource(final String resource) {

		this.resource = resource;
	}

	public int getResourceType() {

		return resourceType;
	}

	public void setResourceType(final int resourceType) {

		this.resourceType = resourceType;
	}

	public boolean getCancelled() {
		return cancelled;
	}

	public void setCancelled(final boolean cancelled) {
		this.cancelled = cancelled;
	}

	public Vector<String[]> loadIntoTable() throws LoadException, IOException {
		InputStream stream;
		if (getResource().startsWith("http://")) {
			try {
				setResourceType(URL);
				stream = openStream();
			} catch (final Exception e) {
				setResourceType(FILE);
				stream = openStream();
			}
		} else {
			try {
				setResourceType(FILE);
				stream = openStream();
			} catch (final Exception e) {
				setResourceType(URL);
				stream = openStream();
			}
		}
		return loadIntoTable(stream);
	}

	private void setLength(final int i) {
		if (progressTrackable != null) {
			progressTrackable.setLength(i);
		}
	}

	private void setValue(final int i) {
		if (progressTrackable != null) {
			progressTrackable.setValue(i);
		}
	}

	private int getValue() {
		if (progressTrackable != null) {
			return progressTrackable.getValue();
		} else {
			return 0;
		}

	}

	private void incrValue(final int i) {
		if (progressTrackable != null) {
			progressTrackable.incrValue(i);
		}
	}

	/** returns a list of vectors of String [], representing data from file. */
	private Vector<String[]> loadIntoTable(final InputStream inputStream)
			throws IOException, LoadException {
		final Vector<String[]> data = new Vector<String[]>(100, 100);
		final MeteredStream ms = new MeteredStream(inputStream);
		final Reader reader = new BufferedReader(new InputStreamReader(ms));

		FlatFileStreamTokenizer st;
		st = new FlatFileStreamTokenizer(reader);
		// ignore leading blank lines...
		while (st.nextToken() == StreamTokenizer.TT_EOL) {
		}
		st.pushBack();
		final Vector<String> line = new Vector<String>(10, 10);
		while (st.nextToken() != StreamTokenizer.TT_EOF) {
			if (getCancelled() == true)
				break; // we're cancelled
			st.pushBack();
			loadLine(line, st);
			final String tokens[] = new String[line.size()];
			final Enumeration<String> e = line.elements();
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = e.nextElement();
			}
			data.addElement(tokens);
			line.removeAllElements();
		}
		return data;
	}

	private void loadLine(final Vector<String> line,
			final FlatFileStreamTokenizer st) throws LoadException, IOException {
		int tt = st.nextToken();
		while ((tt != StreamTokenizer.TT_EOL) && (tt != StreamTokenizer.TT_EOF)) {
			if (tt == StreamTokenizer.TT_WORD) {
				line.addElement(st.sval);
			} else if (tt == FlatFileStreamTokenizer.TT_NULL) {
				line.addElement(null);
			} else {
				final String err = "In loadLine, Got token type " + tt
						+ " token " + st.toString() +

						" expected TT_WORD (" + StreamTokenizer.TT_WORD
						+ ") at line " + st.lineno();
				throw new LoadException(err, LoadException.CDTPARSE);
			}
			tt = st.nextToken();
		}
	}

	/** opens a stream from the resource */
	private InputStream openStream() throws LoadException {
		InputStream is;
		final String file = getResource();
		if (getResourceType() == FILE) {
			try {
				final File fd = new File(file);
				is = new MeteredStream(new FileInputStream(fd));
				setLength((int) fd.length());
			} catch (final Exception ioe) {
				throw new LoadException("File " + file
						+ " could not be opened: " + ioe.getMessage(),
						LoadException.CDTPARSE);
			}
		} else {
			try {
				final java.net.URL url = new java.net.URL(file);
				final java.net.URLConnection conn = url.openConnection();
				is = new MeteredStream(conn.getInputStream());
				setLength(conn.getContentLength());
			} catch (final IOException ioe2) {
				throw new LoadException("Url " + file
						+ " could not be opened: " + ioe2.getMessage(),
						LoadException.CDTPARSE);
			}
		}
		return is;
	}

	class MeteredStream extends FilterInputStream {
		MeteredStream(final InputStream is) {
			super(is);
		}

		@Override
		public int read() throws IOException {
			incrValue(1);
			return super.read();
		}

		// the following should be covered by the more general read...
		// public int read(byte [] b);

		@Override
		public int read(final byte[] b, final int off, final int len)
				throws IOException {
			final int ret = super.read(b, off, len);
			if (ret != -1) {
				// for some reason, got factor of two error in sizes...
				incrValue(ret / 2);
			}
			return ret;
		}

		@Override
		public long skip(final long n) throws IOException {
			final long ret = super.skip(n);
			if (ret != -1) {
				// for some reason, got factor of two error in sizes...
				incrValue((int) ret / 2);
			}
			return ret;
		}

		int markedValue = 0;

		@Override
		public void mark(final int readLimit) {
			super.mark(readLimit);
			markedValue = getValue();
		}

		@Override
		public void reset() throws IOException {
			super.reset();
			setValue(markedValue);
		}
	}
}
