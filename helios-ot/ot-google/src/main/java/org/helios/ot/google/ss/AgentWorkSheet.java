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

import java.util.concurrent.atomic.AtomicInteger;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * <p>Title: AgentWorkSheet</p>
 * <p>Description: Container to track the state of an individual worksheet that represents one Host/Agent pair.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.google.ss.AgentWorkSheet</code></p>
 */

public class AgentWorkSheet {
	/** The parent spreadsheet control */
	protected final SpreadsheetControl ssControl;
	/** The agent's host */
	protected final String hostName;
	/** The agent name */
	protected final String agentName;
	/** The designated worksheet name */
	protected final String workSheetName;
	/** The number of rows  */
	protected final AtomicInteger rowCount = new AtomicInteger(0);
	/** The local metric name (hashcode) to row mapping */
	protected final TIntIntHashMap rowMap = new TIntIntHashMap(SpreadsheetControl.DEFAULT_ROW_SIZE);
	
	/** The no entry value for the row map */
	public final int NO_ENTRY = rowMap.getNoEntryValue();
	
	/**
	 * Creates a new AgentWorkSheet
	 * @param ssControl The parent spreadsheet control
	 * @param hostName The agent's host
	 * @param agentName The agent name
	 */
	public AgentWorkSheet(SpreadsheetControl ssControl, String hostName, String agentName) {
		this.hostName = hostName;
		this.agentName = agentName;
		workSheetName = hostName + "/" + agentName;
		this.ssControl = ssControl;
	}
	
	
	/**
	 * Returns the row of the sheet that the passed local metric name will be published into
	 * @param metricName The local metric name
	 * @return the row id
	 */
	public int getRowId(CharSequence metricName) {
		if(metricName==null) throw new IllegalArgumentException("The passed metricName was null");
		int hashCode = metricName.toString().hashCode();
		int rowId = rowMap.get(hashCode);
		if(rowId==NO_ENTRY) {
			rowId = rowCount.incrementAndGet();
			rowMap.put(hashCode, rowId);
		}
		if(rowId%SpreadsheetControl.DEFAULT_ROW_BATCH==0) {
			ssControl.expandWorkSheet(this);
			rowMap.compact();
		}
		return rowId;
	}
	
	
	
}
