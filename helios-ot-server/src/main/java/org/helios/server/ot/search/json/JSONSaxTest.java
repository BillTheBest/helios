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
package org.helios.server.ot.search.json;

import java.io.File;
import java.io.IOException;

import org.helios.helpers.FileHelper;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * <p>Title: JSONSaxTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.search.json.JSONSaxTest</code></p>
 */

public class JSONSaxTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("JSON SAX Test");
		File f = new File("/home/nwhitehead/hprojects/aa4h/trunk/aa4h/test/xmlqueries/SimpleEmp.json");
		try {
			String jsonText = new String(FileHelper.getBytesFromUrl(f.toURI().toURL()));
			JSONParser parser = new JSONParser();
			JSONContentHandler handler = new JSONContentHandler();
			parser.parse(jsonText, handler, true);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		

	}
	
	public static void log(Object obj) {
		System.out.println(obj);
	}
	
	public static class JSONContentHandler implements ContentHandler {
		public static void log(Object obj) {
			System.out.println(obj);
		}

		@Override
		public boolean endArray() throws ParseException, IOException {
			log("End Array");
			return false;
		}

		@Override
		public void endJSON() throws ParseException, IOException {
			log("End JSON");
			
		}

		@Override
		public boolean endObject() throws ParseException, IOException {
			log("End Object");
			return true;
		}

		@Override
		public boolean endObjectEntry() throws ParseException, IOException {
			log("End Object Entry");
			return true;
		}

		@Override
		public boolean primitive(Object value) throws ParseException, IOException {
			log("End Primitive [" + value + "]");
			return true;
		}

		@Override
		public boolean startArray() throws ParseException, IOException {
			log("Start Array");
			return false;
		}

		@Override
		public void startJSON() throws ParseException, IOException {
			log("Start JSON");
			
		}

		@Override
		public boolean startObject() throws ParseException, IOException {
			log("Start Object");
			return true;
		}

		@Override
		public boolean startObjectEntry(String key) throws ParseException, IOException {
			log("Start ObjectEntry [" + key + "]");
			return true;
		}
		
	}

}
