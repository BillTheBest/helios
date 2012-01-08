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
package org.helios.nativex.jmx.cl;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.helpers.ClassHelper;
import org.kohsuke.args4j.CmdLineParser;

/**
 * <p>Title: CmdLineParserHelper</p>
 * <p>Description: Utility Methods for Cmd Line</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.cl.CmdLineParserHelper</code></p>
 */

public class CmdLineParserHelper {
	/** Usage string cache keyed by parsed class */
	private static final Map<String, String> usageCache = new ConcurrentHashMap<String, String>();
	
	
	/**
	 * Returns the usage string for the passed bean
	 * @param bean The bean to get the usage for
	 * @return The usage string for the passed bean
	 */
	public static String getUsage(Object bean) {
		ClassHelper.nvl(bean, "Passed Bean Was Null");
		String className = bean.getClass().getName();
		String usage = usageCache.get(className);
		if(usage==null) {
			synchronized(usageCache) {
				usage = usageCache.get(className);
				if(usage==null) {
					CmdLineParser clp = new CmdLineParser(bean);
					ByteArrayOutputStream baos = null;
					try {
						baos = new ByteArrayOutputStream();
						clp.printUsage(baos);			
						usage = baos.toString();
					} finally {
						try { baos.close(); } catch (Exception e) {}
					}				
				}
			}
		}
		return usage;
	}
}
