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
package org.helios.ot.google.ss;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.helpers.ConfigurationHelper;

/**
 * <p>Title: SpreadhseetControl</p>
 * <p>Description: Container to track spreadhseet sizes, metric name to row mappings and worksheets in the target document.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.google.ss.SpreadsheetControl</code></p>
 */

public class SpreadsheetControl {
	/*
	 * Doc Name and Key
	 * Header Names
	 * Host/Agent to Worksheet Mapping
	 * Default Worksheet Sizes and Size increase batch size
	 * Worksheet Running Size
	 * Local metric name to row number map per Host/Agent/Worksheet
	 */
	
	/** The plain text name of the document */
	protected String documentName = null;
	/** The google API key for the document */
	protected String documentKey = null;
	/** The defined headers and names of the trace fields to render */
	protected final Set<String> fieldNames = new HashSet<String>();
	/** The hostname/agent to worksheet name mapping */
	protected final Map<String, String> worksheets = new ConcurrentHashMap<String, String>();
	
	
	/** The system property of environmental var name that overrides the default initial row size of created spreadhseets */
	public static final String ROW_SIZE_PROP = "org.helios.gdata.ss.row.size";
	/** The system property of environmental var name that overrides the default number of rows added when capacity is reached */
	public static final String ROW_BATCH_PROP = "org.helios.gdata.ss.row.batch";
	/** The default initial row size of created spreadhseets */
	public static final int DEFAULT_ROW_SIZE = ConfigurationHelper.getIntSystemThenEnvProperty(ROW_SIZE_PROP, 100);
	/** The default number of rows added when capacity is reached */
	public static final int DEFAULT_ROW_BATCH = ConfigurationHelper.getIntSystemThenEnvProperty(ROW_BATCH_PROP, 100);
	
	
	
	
	void expandWorkSheet(AgentWorkSheet sheet) {
		
	}
}
