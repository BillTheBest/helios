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
package org.helios.server.ot.net.dicsovery;

import java.util.Map;

import org.helios.helpers.Banner;

/**
 * <p>Title: InfoFormat</p>
 * <p>Description: Enumerates the available formats for delivering multicast info and a formater for each</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.server.ot.net.dicsovery.InfoFormat</code></p>
 */

public enum InfoFormat {
	TEXT(new TextFormater()),
	XML(new XMLFormater());
	
	/** The data map header prefix indicating the end of a header */
	public static final String HEADER_PREFIX = "ENDOF|";
	/** The length of the header prefix  */
	public static final int HEADER_PREFIX_LENGTH = HEADER_PREFIX.length();
	
	
	/**
	 * Returns the InfoFormat that matches the passed name, or TEXT if no match can be made/
	 * @param name The name of the InfoFormat
	 * @return an InfoFormat
	 */
	public static InfoFormat getInstance(CharSequence name) {
		if(name==null) return TEXT;
		String IF = name.toString().toUpperCase().trim();
		try {
			return InfoFormat.valueOf(IF);
		} catch (Exception e) {
			return TEXT;
		}
	}
	
	/**
	 * Formats the passed map into a string
	 * @param data the map of data to format
	 * @return a formated string
	 */
	public String format(Map<String, String> data) {
		return formater.format(data);
	}
	
	private final InfoFormater formater;
	
	private InfoFormat(InfoFormater formater) {
		this.formater = formater;
	}
	
	public static interface InfoFormater {
		/**
		 * Formats the passed map into the specified format
		 * @param data The data to format
		 * @return a formated string
		 */
		public String format(Map<String, String> data);
	}
	
	public static class TextFormater implements InfoFormater {
		public String format(Map<String, String> data) {
			StringBuilder b = new StringBuilder(Banner.banner("*", 0, 24, "Helios Open Trace Server"));
			boolean inSub = false;
			for(Map.Entry<String, String> entry: data.entrySet()) {
				String k = entry.getKey();
				String v = entry.getValue();
				if(!v.isEmpty()) {
					if(inSub) {
						b.append("\n\t").append(v);
					} else {
						b.append("\n\t").append(k).append(":").append(v);
					}
					
				} else {
					if(!k.startsWith(HEADER_PREFIX)) {
						b.append("\n").append(k);
						inSub = true;
					} else {
						inSub = false;
					}
				}
			}
			b.append("\n************************\n");
			return b.toString();
		}
	}
	
	public static class XMLFormater implements InfoFormater {
		public String format(Map<String, String> data) {
			StringBuilder b = new StringBuilder("<HeliosOpenTraceServer>");			
			for(Map.Entry<String, String> entry: data.entrySet()) {
				String k = entry.getKey().replace(" ", "");
				String v = entry.getValue();
				if(!v.isEmpty()) {
					b.append("<").append(k).append(">").append(v).append("</").append(k).append(">");
				} else {
					if(!k.startsWith(HEADER_PREFIX)) {
						b.append("<").append(k).append(">");						
					} else {
						b.append("</").append(k.substring(HEADER_PREFIX_LENGTH)).append(">");
					}
				}
			}
			b.append("</HeliosOpenTraceServer>");
			return b.toString();
		}
	}
	
}
