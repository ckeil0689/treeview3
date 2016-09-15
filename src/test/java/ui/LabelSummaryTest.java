package ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.prefs.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.stanford.genetics.treeview.LabelSummary;

public class LabelSummaryTest {

	private final String NAME = "TestSummary";
	private final String ROOT_NODE = "testNode";
	private LabelSummary labelSummary;
	
	@Before
	public void setUp() throws Exception {
		
		this.labelSummary = new LabelSummary(NAME);
	}

	@After
	public void tearDown() throws Exception {
		
		Preferences.userRoot().node(ROOT_NODE).removeNode();
	}

	@Test
	public void testLabelSummary() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetConfigNode() {
		
		/* Testing this setter because it contains some logic */
		Preferences parentNode = Preferences.userRoot().node(ROOT_NODE);
		labelSummary.setConfigNode(parentNode);
		
		Preferences hsNode = labelSummary.getConfigNode();
		assertNotNull("ConfigNode was set", hsNode);
		assertTrue("ConfigNode has name " + NAME, NAME.equals(hsNode.name()));
	}

	@Test
	public void testRequestStoredState() {
		fail("Not yet implemented");
	}

	@Test
	public void testStoreState() {
		fail("Not yet implemented");
	}

	@Test
	public void testImportStateFrom() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetIncluded() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetHeaders() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetIncluded() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSummary() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSummaryArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testNodeHasAttribute() {
		fail("Not yet implemented");
	}

}
