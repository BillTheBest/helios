/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package test.org.helios.webapp.js.data;




import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import test.org.helios.webapp.js.EnvJSLoader;

/**
 * <p>Title: HeliosDataTestCase</p>
 * <p>Description: Test cases for the Helios Data Plugin</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.webapp.js.data.HeliosDataTestCase</code></p>
 */

public class HeliosDataTestCase {
	protected static EnvJSLoader loader = null;
	/**
	 * Starts the basic http file server and initializes the JS Engine browser emulator.
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		loader = EnvJSLoader.getInstance("./src/main/webapp", 5555, null);
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	

	/**
	 * Stops the http server
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		loader.stop();
	}

	/**
	 * Creates a complex tree structure to interogate and modify.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		loader.loadDocument("test.html");
		
	}

	/**
	 * Clears the tree structure
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		//loader.eval("var bands = null;");
	}
	
	@Test
	public void testTopLevelSimpleNumberReference() {
		log("Executing test [testTopLevelSimpleNumberReference]");
		Number ret = (Number)loader.eval("hdata('count', bands);");
		Assert.assertEquals("Count of bands" , 2d, ret);
	}
	
	@Test
	public void testTopLevelSimpleStringReference() {
		log("Executing test [testTopLevelSimpleStringReference]");
		String ret = (String)loader.eval("hdata('version', bands);");
		Assert.assertEquals("Version of document" , "1.0", ret);
	}
	
	@Test
	public void testDeepStringReference() {
		log("Executing test [testDeepStringReference]");
		Object bands = loader.getJsObj("bands");
		String ret = (String)loader.eval("hdata('The Beatles/Tags/Style/Music/Genres[0]', bands);");
		Assert.assertEquals("First Genre for Beatles is Rock", "Rock", ret);
		ret = (String)loader.eval("hdata('The Beatles/Tags/Style/Music/Genres[0]', bands);");
		Assert.assertEquals("First Genre for Beatles is Rock", "Rock", ret);
	}
	
	@Test
	public void testArrayReference() {
		log("Executing test [testArrayReference]");
		Object[] genres = loader.getJsArray("bands", "The Beatles", "Tags", "Style", "Music", "Genres");
		Assert.assertArrayEquals("Array of genres", new Object[]{"Rock", "Folk", "Popular"}, genres);		
	}
	
	@Test
	public void testNavMapReadWrite() {
		log("Executing test [testNavMapReadWrite]");
		loader.eval("bands = resolveData(bands);");
		Map<String, Object> map = loader.getNavMap("bands");
		for(Map.Entry<String, Object> entry: map.entrySet()) {
			
			String key = entry.getKey();
			Object value = entry.getValue();
			Object expected = value;
			try {				
				Object readValue = loader.eval("hdata('" + key + "', bands);");
				// Test that the hdata read of the key is the same as the value in the nav map
				Assert.assertEquals("Initial read value of [" + key + "]", expected, readValue);
				// Now modify the data
				Object expectedModified = value + "-Modified";
				Object setRet = loader.eval("hdata('" + key + "', bands, '" + expectedModified + "');");
				// Validate the return value of the setter is the value of the set data
				Assert.assertEquals("Return value on set of [" + key + "]", expectedModified, setRet);
				// Re-read the value after modifying
				Object ret2 = loader.eval("hdata('" + key + "', bands);");
				// Test the re-read value
				Assert.assertEquals("Reread of value [" + key + "]", expectedModified, ret2);
			} catch (Exception e) {
				log("testNavMapReadWrite failed on [" + key + "]:[" + value + "]");
				throw new RuntimeException("testNavMapReadWrite failed on [" + key + "]:[" + value + "]", e);
			}
		}
	}
	
	@Test
	public void testModifyCloneOriginalUnchanged() {
		log("Executing test [testModifyCloneOriginalUnchanged]");
		// Capture the nav map of the original "bands"
		loader.eval("bands = resolveData(bands);");
		Map<String, Object> map = loader.getNavMap("bands");
		// Create a clone of "bands" called "bandsClone"
		loader.eval("var bandsClone = deepClone(bands);");
		// Validate that "bandsClone" has the same values as "bands"
		for(Map.Entry<String, Object> entry: map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Object readValue = loader.eval("hdata('" + key + "', bandsClone);");
			// Validate that readValue is the same as value
			Assert.assertEquals("Read value from clone is same as original value for key [" + key + "]", value, readValue);
			// Modify the value
			Object newValue = readValue + "-Modified";
			Object modifiedValue = loader.eval("hdata('" + key + "', bandsClone, '" + newValue + "');");
			// Validate the return value from the set is the value that was set
			Assert.assertEquals("Return value from clone set is same as set value for key [" + key + "]", newValue, modifiedValue);
			// Validate that "bands" still has the same values
			Object originalValue = loader.eval("hdata('" + key + "', bands);");
			Assert.assertEquals("Read value from original is same as original captured value for key [" + key + "]", value, originalValue);
		}
	}
	
	
	@Test
	public void testHevalDataReference() {
		String[] expectedInstruments = new String[]{"Vocals", "Guitar", "Piano", "Harmonica", "Harmonium", "Electronic Organ", "Six-string Bass"};
		Object[] readValue = EnvJSLoader.convertArray((NativeArray)loader.eval("hdata('The Beatles/Core Members[0]/instruments', bands);"));
		Assert.assertArrayEquals(expectedInstruments, readValue);
		log("Refs:[" + Arrays.toString(readValue) + "]");
		//loader.eval("var refs = resolveData(bands);");
		//Object refs = loader.invoke(loader.getJsFunction("hdata"),  "The Rolling References/Core Members[0]", loader.getJsObject("refs"), null, null, null);
		readValue = EnvJSLoader.convertArray((NativeArray)loader.eval("var refInsts = resolveData(bands); console.log('Resolved Data'); recurseData(refInsts); hdata('The Rolling References/Core Members[0]/instruments', refInsts);"));
		log("Refs:[" + Arrays.toString(readValue) + "]");
		Assert.assertArrayEquals(expectedInstruments, readValue);
	}

}
