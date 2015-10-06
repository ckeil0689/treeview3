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
		mpContainer.setToMinScale();
		assertTrue("All tiles visible.", mpContainer.showsAllTiles());
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
		fail("Not yet implemented");
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
		
		mpContainer.zoomToward(5, 10);
		
		mpContainer.scrollBy(-4);
		assertEquals("Scrolled back 4", 1, scrollbar.getValue());
		assertEquals("First Visible #2", 1, mpContainer.getFirstVisible());
	}

	@Test
	public void testGetMaxIndex() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMinIndex() {
		fail("Not yet implemented");
	}

}
