///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
// *
// * END_HEADER 
// */

//package edu.stanford.genetics.treeview.plugin.dendroview;
//
//import javax.swing.BoxLayout;
//import javax.swing.ButtonGroup;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JRadioButton;
//import javax.swing.JScrollPane;
//import javax.swing.JTextArea;
//import javax.swing.event.DocumentEvent;
//import javax.swing.event.DocumentListener;
//import javax.swing.text.BadLocationException;
//import javax.swing.text.Document;
//import javax.swing.text.Segment;
//
///**
// * this class exposes a GUI for configuring a summary view.
// */
//
//public class SummaryViewWizard extends JPanel {
//	private final DendroView dendroView;
//	private final GeneListPanel geneListPanel;
//	private final JRadioButton selectionButton, listButton;
//
//	public SummaryViewWizard(final DendroView dendroView) {
//		this.dendroView = dendroView;
//		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//		geneListPanel = new GeneListPanel();
//
//		selectionButton = new JRadioButton();
//		selectionButton.setSelected(true);
//		listButton = new JRadioButton();
//		final ButtonGroup group = new ButtonGroup();
//		group.add(selectionButton);
//		group.add(listButton);
//
//		final JPanel selectionPanel = new JPanel();
//		selectionPanel.add(selectionButton);
//		selectionPanel.add(new JLabel("Selected Genes"));
//
//		final JPanel listPanel = new JPanel();
//		listPanel.add(listButton);
//		listPanel.add(geneListPanel);
//
//		add(selectionPanel);
//		add(listPanel);
//	}
//
//	public int[] getIndexes() {
//		if (listButton.isSelected()) {
//			return geneListPanel.getIndexes();
//		}
//		return dendroView.getGeneSelection().getSelectedIndexes();
//	}
//
//	class GeneListPanel extends JPanel {
//		JTextArea textArea;
//
//		// JTextField textArea;
//		public GeneListPanel() {
//			textArea = new JTextArea("Paste one ID per row", 10, 50);
//			textArea.append("\nNote: use Ctrl-V on mac (Java is cross-platform!?)");
//			// textArea = new JTextField("Paste one ID per row");
//			textArea.setEditable(true);
//			textArea.getDocument().addDocumentListener(new DocumentListener() {
//				@Override
//				public void changedUpdate(final DocumentEvent e) {
//					listButton.setSelected(true);
//				}
//
//				@Override
//				public void insertUpdate(final DocumentEvent e) {
//					listButton.setSelected(true);
//				}
//
//				@Override
//				public void removeUpdate(final DocumentEvent e) {
//					listButton.setSelected(true);
//				}
//			});
//
//			add(new JScrollPane(textArea));
//		}
//
//		public int[] getIndexes() {
//			LineReader lineReader = new LineReader();
//			String next = lineReader.readLine();
//			int nLines = 0;
//			while (next != null) {
//				if (next.length() > 0) {
//					nLines++;
//				}
//				next = lineReader.readLine();
//			}
//			final String[] subStrings = new String[nLines];
//
//			lineReader = new LineReader();
//			next = lineReader.readLine();
//			nLines = 0;
//			while (next != null) {
//				if (next.length() > 0) {
//					subStrings[nLines++] = next;
//				}
//				next = lineReader.readLine();
//			}
//
//			dendroView.getViewFrame().getGeneFinder().findGenesById(subStrings);
//			dendroView.getViewFrame().getGeneFinder().seekAll();
//			return dendroView.getGeneSelection().getSelectedIndexes();
//		}
//
//		class LineReader {
//			char[] lineTerminator = System.getProperty("line.separator")
//					.toCharArray();
//			int documentPosition = 0;
//			Segment seg = new Segment();
//
//			public String readLine() {
//				final StringBuffer buf = new StringBuffer();
//				final char[] save = new char[lineTerminator.length];
//				int pos = 0;
//				final Document doc = textArea.getDocument();
//				try {
//					doc.getText(documentPosition++, 1, seg);
//				} catch (final BadLocationException e) {
//					return null;
//				}
//				int ch = seg.first();
//				boolean done = false;
//				do {
//					if (ch == lineTerminator[pos]) {
//						save[pos] = (char) ch;
//						pos++;
//					} else {
//						// if a char in the line terminator is returned
//						// but one was skipped, then skip it by moving pos
//						// up by two
//						if (pos + 1 < lineTerminator.length
//								&& ch == lineTerminator[pos + 1]) {
//							pos += 2;
//						} else {
//							if (pos > 0) {
//								buf.append(save, 0, pos);
//								pos = 0;
//							}
//							buf.append((char) ch);
//						}
//					}
//					done = pos >= lineTerminator.length;
//					try {
//						doc.getText(documentPosition++, 1, seg);
//					} catch (final BadLocationException e) {
//						done = true;
//					}
//					if (!done)
//						ch = seg.first();
//				} while (!done);
//				final String tempString = new String(buf);
//				return (tempString.trim());
//			}
//		}
//	}
//}
