import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.swing.JScrollBar;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.stanford.genetics.treeview.plugin.dendroview.IntegerMap;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

public class MapContainerTest {

	private final static int MIN_IDX = 0;
	private final static int MAX_IDX = 99;
	
	private MapContainer mpContainer;
	private JScrollBar scrollbar;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
		mpContainer = new MapContainer(IntegerMap.FIXED, "TestMap");
		
		scrollbar = new JScrollBar();
		mpContainer.setScrollbar(scrollbar);
		
		mpContainer.setIndexRange(MIN_IDX, MAX_IDX);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCalculateNewMinScale() {
		fail("Not yet implemented");
	}

	@Test
	public void testShowsAllTiles() {
		
		// Case 1: Fully zoomed out.
		assertTrue("All tiles visible.", mpContainer.showsAllTiles());
		
		// Case 2: Zoomed in once.
		mpContainer.zoomInEnd();
		assertFalse("Not all tiles visible.", mpContainer.showsAllTiles());
		
		// Case 3: Zoomed out again
		mpContainer.setMinScale();
		assertTrue("All tiles visible.", mpContainer.showsAllTiles());
	}

	@Test
	public void testGetFirstVisible() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testGetLastVisible() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testGetNumVisible() {
		
		// Case 1: Fully zoomed out - all visible
		assertEquals("Full number of tiles visible.", 100, 
				mpContainer.getNumVisible());
	}

	@Test
	public void testGetBestZoomInVal() {
		fail("Not yet implemented");
	}

	@Test
	public void testRecalculateScale() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetScrollbar() {
		fail("Not yet implemented");
	}

	@Test
	public void testScrollToIndex() {
		fail("Not yet implemented");
	}

	@Test
	public void testScrollToFirstIndex() {
		
		assertEquals("Scroll value - Before zoom", 0, mpContainer.getScroll().getValue());
		
		mpContainer.zoomToSelected(5, 10);
		
		assertEquals("Scroll value - After zoom", 0, mpContainer.getScroll().getValue());
		
		mpContainer.scrollToFirstIndex(5);
		
		assertEquals("Scroll value - After scroll", 5, mpContainer.getScroll().getValue());
		assertEquals("First Visible - After scroll", 5, mpContainer.getFirstVisible());
	}

	@Test
	public void testScrollBy() {
		
		assertEquals("Scrollbar Object Equality", scrollbar, 
				mpContainer.getScroll());
		
		assertEquals("Init Scroll Value", 0, 
				mpContainer.getScroll().getValue());
		
		assertEquals("Scroll Min", MIN_IDX, 
				mpContainer.getScroll().getMinimum());
		assertEquals("Scroll Max", MAX_IDX + 1, 
				mpContainer.getScroll().getMaximum());
		
		mpContainer.scrollBy(10);
		assertEquals("Scroll - All visible", 0, scrollbar.getValue());
		assertEquals("First Visible #1", 0, mpContainer.getFirstVisible());
		
		mpContainer.zoomToSelected(5, 10);
		mpContainer.scrollToFirstIndex(5);
		assertEquals("Zoomed to first idx: 5", 5, scrollbar.getValue());
		
		mpContainer.scrollBy(-4);
		assertEquals("Scrolled back 4", 1, scrollbar.getValue());
		assertEquals("First Visible #2", 1, mpContainer.getFirstVisible());
		
		// Out of bounds - smaller than 0
		mpContainer.scrollBy(-4);
		assertEquals("Scrolled back 4 - out of bounds", 0, scrollbar.getValue());
		assertEquals("First Visible #2", 0, mpContainer.getFirstVisible());
		
		// Out of bounds - bigger than MAX_IDX
		int first_idx = MAX_IDX - 10;
		int last_idx = MAX_IDX - 3;
		int range = last_idx - first_idx + 1;
		int max_scroll_allowed = MAX_IDX - last_idx;
		int scroll_num = max_scroll_allowed + 3; // out of bounds by 3
		
		mpContainer.zoomToSelected(first_idx, last_idx);
		mpContainer.scrollToFirstIndex(first_idx);
		mpContainer.scrollBy(scroll_num);
		
		assertEquals("First visible - scrolled out of bounds", 
				first_idx + max_scroll_allowed, mpContainer.getFirstVisible());
		
		assertEquals("Num visible - scrolled out of bounds", 
				range, mpContainer.getNumVisible());
		
		assertEquals("Last visible - scrolled out of bounds", 
				MAX_IDX, mpContainer.getLastVisible());
	}

	@Test
	public void testGetMaxIndex() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMinIndex() {
		fail("Not yet implemented");
	}
	
	// Zoom code tests
	@Test
	public void testZoomToSelected() {
		
		mpContainer.zoomToSelected(5, 10);

		assertEquals("Num Visible after zoom", 6, 
				mpContainer.getNumVisible());
	}
}
