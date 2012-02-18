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
package test.org.helios.scripting;

import java.io.File;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.helios.helpers.FileHelper;
import org.helios.reflection.PrivateAccessor;
import org.helios.scripting.manager.script.ScriptInstance;


/**
 * <p>Title: ScriptInstanceTestCase</p>
 * <p>Description: Some simple test cases using scripts with expected return values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.scripting.ScriptInstanceTestCase</code></p>
 */

public class ScriptInstanceTestCase extends TestCase {

	/**
	 * @throws java.lang.Exception
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	/**
	 * Passes a static array of numbers to the test script for addition and tests the response for a know value.
	 * @throws Exception
	 */
	public void testSimpleGroovyScript() throws Exception {
		log("Testing [SimpleGroovyTest.groovy]");
		ScriptInstance si = new ScriptInstance(new File("./src/test/resources/scripts/groovy/SimpleGroovyTest.groovy").toURI().toURL());
		Number[] numbers = new Number[]{1,2,3,4,5};
		Number n = (Number)si.exec("numbers", numbers);
		Number expected = (1+2+3+4+5);
		Assert.assertEquals("The return value from the script was [" + n + "] not [" + expected + "]", expected, n);		
	}
	
	/**
	 * Generates a randomly sized number array and populates with a random set of numbers.
	 * Calculates the total of the samples and compares to the script's calculation.
	 * @throws Exception
	 */
	public void testRandomSimpleGroovyScript() throws Exception {
		log("Testing [Random SimpleGroovyTest.groovy]");
		ScriptInstance si = new ScriptInstance(new File("./src/test/resources/scripts/groovy/SimpleGroovyTest.groovy").toURI().toURL());
		Random random = new Random(System.nanoTime());
		int arrSize = random.nextInt(1000);
		Number[] numbers = new Number[arrSize];
		long expected = 0L;
		for(int i = 0; i < arrSize; i++) {
			long sample = random.nextLong();
			numbers[i] = sample;
			expected += sample;
		}
		Number n = (Number)si.exec("numbers", numbers);
		Assert.assertEquals("The return value from the script was [" + n + "] not [" + expected + "]", expected, n);		
	}
	
	/**
	 * Writes out a groovy script that adds up the values and calls it, testing the response.
	 * Then modifies the script to return the max of the values, calls it and tests the response.
	 * @throws Exception
	 */
	public void testRecompileOnSimpleGroovyScript() throws Exception {
		log("Testing [RecompileOnSimpleGroovyScript]");
		String sumScript = " total = 0; numbers.each() { total += it; };  return total;";
		String maxScript = " return numbers.max();";
		File tmpFile = File.createTempFile("testGroovyScript", ".groovy");
		tmpFile.deleteOnExit();
		FileHelper.replaceFileContent(tmpFile.getAbsolutePath(), sumScript);
		ScriptInstance si = new ScriptInstance(tmpFile);		
		final Number[] numbers = new Number[]{1,2,3,4,5};
		Number n = (Number)si.exec("numbers", numbers);
		Number expected = (1+2+3+4+5);
		Assert.assertEquals("The return value from script 1 was [" + n + "] not [" + expected + "]", expected, n);
		FileHelper.replaceFileContent(tmpFile.getAbsolutePath(), maxScript);
		FileHelper.tick(tmpFile);
		si.reload();
		n = (Number)si.exec("numbers", numbers);
		expected = 5;
		Assert.assertEquals("The return value from script 2 was [" + n + "] not [" + expected + "]", expected, n);
	}
	
	/**
	 * Writes out a JS script that adds up the values and calls it, testing the response.
	 * Then modifies the script to return the max of the values, calls it and tests the response.
	 * @throws Exception
	 */
	public void testRecompileOnSimpleJSScript() throws Exception {
		log("Testing [RecompileOnSimpleJavaScript]");
		String sumScript = " for(n in numbers) { total = total + parseInt(numbers[n]); }";
		String maxScript = " for(n in numbers){if(numbers[n] > maxv){maxv = numbers[n];}}";
		File tmpFile = File.createTempFile("testJSScript", ".js");
		tmpFile.deleteOnExit();
		FileHelper.replaceFileContent(tmpFile.getAbsolutePath(), sumScript);
		ScriptInstance si = new ScriptInstance(tmpFile);
		final Number[] numbers = new Number[]{1,2,3,4,5};
		si.set("numbers", numbers);
		si.set("total", 0);
		si.exec();
		Number n = null;
		Object ret = null;
		
		
		ret = si.get("total");
		if(ret!=null && ret.getClass().getSimpleName().equals("NativeJavaObject")) {
			n = (Number)PrivateAccessor.invoke(ret, "unwrap");
		} else {
			n = (Number)ret;
		}
		
		Number expected = (1+2+3+4+5);
		Assert.assertTrue("The return value from script 1 was [" + n + "] not [" + expected + "]", expected.equals(n.intValue()));		
		FileHelper.replaceFileContent(tmpFile.getAbsolutePath(), maxScript);
		FileHelper.tick(tmpFile);
		si.reload();
		si.set("numbers", numbers);
		si.set("maxv", 0);
		si.exec();		
		ret = si.get("maxv");
		if(ret!=null && ret.getClass().getSimpleName().equals("NativeJavaObject")) {
			n = (Number)PrivateAccessor.invoke(ret, "unwrap");
		} else {
			n = (Number)ret;
		}
		
		expected = 5;
		Assert.assertEquals("The return value from script 2 was [" + n + "] not [" + expected + "]", expected, n);
	}
	
	
	
	public static void log(Object message) {
		System.out.println("[ScriptInstanceTestCase]" + message);
	}

}
