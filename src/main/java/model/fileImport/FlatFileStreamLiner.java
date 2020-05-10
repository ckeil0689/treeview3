/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package model.fileImport;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Vector;

/**
 * implements the "lexical structure" of flat files basically, calling nextToken
 * returns a series of words, nulls and newlines, and finally an EOF. Note that
 * numbers are not parsed by the tokenizer. Also, there is no enforcement of the
 * correct number of tokens per line.
 * 
 * it will, however, filter out blank lines.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.7 $ $Date: 2005-03-12 22:32:08 $
 */
public class FlatFileStreamLiner {

	private final char sep;
	/**
	 * parse quoted strings?
	 */
	private boolean pqs = true;
	private final StreamTokenizer st;
	private final Vector<String> line;

	/**
	 * Constructor for the FlatFileStreamTokenizer object
	 * 
	 * @param reader
	 *            Reader of file to tokenize
	 * @param ch
	 *            Separator character to split cols
	 */
	public FlatFileStreamLiner(final Reader reader, final char ch,
			final boolean parseQuotedStrings) {
		sep = ch;
		pqs = parseQuotedStrings;
		st = new StreamTokenizer(reader);
		resetSyntax();
		line = new Vector<String>();
	}

	public void resetSyntax() {
		st.resetSyntax();
		st.wordChars(0, 3000);
		st.whitespaceChars('\r', '\r');
		st.whitespaceChars('\n', '\n');
		st.ordinaryChar(sep);
		if (pqs) {
			// make sure to add these chars to the nextLine() switch statement
			st.quoteChar('"');
		}
		st.eolIsSignificant(true);
		// st.parseNumbers(); do not uncomment.
	}

	public static void main(final String astring[]) {
		System.out.println("analysizing " + astring[0]);
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(astring[0]));
			final FlatFileStreamLiner st = new FlatFileStreamLiner(br, '\t',
					true);
			while (st.nextLine()) {
				final String[] tok = st.getLineTokens();
				for (int i = 0; i < tok.length; i++) {
					System.out.print(tok[i]);
					System.out.print(":");
				}
				System.out.print("\n");
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public String[] getLineTokens() {
		final int len = line.size();
		final String[] string = new String[len];
		for (int i = 0; i < len; i++) {
			string[i] = line.get(i);
		}
		return string;
	}

	public boolean nextLine() throws IOException {
		line.removeAllElements();
		boolean lastsep = true; // in case first token is sep

		while (st.nextToken() != StreamTokenizer.TT_EOF) {
			switch (st.ttype) {
			case StreamTokenizer.TT_EOL:
				// line ends with tab char (indicating last value null)
				if (lastsep)
					line.add(null);
				return true;
			case StreamTokenizer.TT_NUMBER:
				System.out.println("parsed number");
				line.add("" + st.nval);
				lastsep = false;
				break;
			case '"':
				if (lastsep == false) {
					// account for stupid excel embedded quotes
					line.setElementAt(line.lastElement() + st.sval,
							line.size() - 1);
					break;
				}
				// otherwise, fall through to new word.
			case StreamTokenizer.TT_WORD:
				line.add(st.sval);
				lastsep = false;
				break;
			default:
				// case statements must be constants, so can't use sep.
				if (st.ttype == sep) {
					if (lastsep) { // already one sep
						line.add(null);
					} else { // normal sep, after real token
						lastsep = true;
					}
				}
				break;
			}
		}

		// indicates that last line lacks EOL token
		if (line.size() == 0)
			return false;
		else
			return true;
	}
}
