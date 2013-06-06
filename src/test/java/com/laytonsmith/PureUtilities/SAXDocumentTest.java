package com.laytonsmith.PureUtilities;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lsmith
 */
public class SAXDocumentTest {
	
	static String testDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
			+ "<root>"
				+ "<node1 attribute=\"value\">Text</node1>"
					+ "<nodes>"
						+ "<inode attribute=\"1\">value</inode>"
						+ "<!-- This is 2 ^ 33 -->"
						+ "<inode attribute=\"1.5\">8589934592</inode>"
						+ "<inode>true</inode>"
					+ "</nodes>"
			+ "</root>";
	SAXDocument doc;

	public SAXDocumentTest() throws Exception {
		doc = new SAXDocument(testDoc, "UTF-8");
	}
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}

	@Test
	public void testBasic() throws Exception {
		final AtomicInteger i = new AtomicInteger(0);
		doc.addListener("/root/nodes/inode", new SAXDocument.ElementCallback() {

			public void handleElement(String xpath, String tag, Map<String, String> attr, String contents) {
				i.incrementAndGet();
			}
		});
		doc.parse();
		assertEquals(3, i.get());
	}
	
	@Test
	public void testIndexWorks() throws Exception {
		final AtomicInteger i = new AtomicInteger(0);
		doc.addListener("/root/nodes/inode[1]", new SAXDocument.ElementCallback() {

			public void handleElement(String xpath, String tag, Map<String, String> attr, String contents) {
				i.incrementAndGet();
			}
		});
		doc.parse();
		assertEquals(1, i.get());
	}
}