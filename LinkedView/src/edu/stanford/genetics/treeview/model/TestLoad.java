/*
 * Created on Nov 28, 2004
 *
 */
package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import edu.stanford.genetics.treeview.LogBuffer;
/**
 * @author gcong
 *
 *
 *
 *
 *
 */
public class TestLoad {

	static public  Vector<String[]> load(BufferedReader br) throws IOException{
		Vector<String[]> data = new Vector<String[]>(100,100);
		
		FlatFileStreamTokenizer st;
		st = new FlatFileStreamTokenizer(br);
		// ignore leading blank lines...
		while (st.nextToken() == FlatFileStreamTokenizer.TT_EOL) {}
		st.pushBack();
		
		
		while (st.nextToken() != FlatFileStreamTokenizer.TT_EOF) {
			Vector<String> line = new Vector<String>(10, 10);
			st.pushBack();
			loadLine(line,st);
			String tokens[] = new String[line.size()];
			Enumeration<String> e = line.elements();
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = (String) e.nextElement();
			}
			data.addElement(tokens);
			//data.addElement(line);
			line.removeAllElements();
		}
		return data;
	}
	
		static public void loadLine(Vector<String> line, FlatFileStreamTokenizer st) 
	throws IOException {
		int tt = st.nextToken();
		while ((tt != FlatFileStreamTokenizer.TT_EOL) && (tt != FlatFileStreamTokenizer.TT_EOF)) {
			if (tt == FlatFileStreamTokenizer.TT_WORD) {
				line.addElement(st.sval);
				//line.addElement("012345678");
				//line.addElement("" + Math.random());
			} else if (tt == FlatFileStreamTokenizer.TT_NULL) {
				line.addElement(null);		
			} else {
				LogBuffer.println("In loadLine, Got token type " + tt + " token " + st.toString() +
				" expected TT_WORD (" + FlatFileStreamTokenizer.TT_WORD + ") at line " + st.lineno());
				
			}
			tt = st.nextToken();
		}
	}
}

