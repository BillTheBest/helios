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
package org.helios.collectors.wpm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Arrays;

/**
 * <p>Title: WPMCollector </p>
 * <p>Description: A NSClient4J based collector to gather Windows Performance Monitor (WPM) statistics.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead(whitehead.nicholas@gmail.com)
 */

public class AttributeSegment {
	protected String metricSegment[] = null;
	protected String metricName = null;

	/** Compound Counter Name Matching Pattern */
	protected static Pattern compoundCounterMatcher = null;
	/** Flat Counter Name Parser */
	protected static Pattern flatCounterParser = null;
	/** Compound Counter Name Parser */
	protected static Pattern compoundCounterParser = null;

	static {
		compoundCounterMatcher = Pattern.compile("\\\\.*\\(.*\\)\\\\.*");
		flatCounterParser = Pattern.compile("\\\\");
		compoundCounterParser = Pattern.compile("\\\\|\\(|\\)");		
	}
	
	
	public AttributeSegment(String counterName) {
		List<String> segments = new ArrayList<String>();
		boolean isCompound = compoundCounterMatcher.matcher(counterName).find();
		String[] pSegments = null;
		if(isCompound) {
			pSegments = compoundCounterParser.split(counterName);
		} else {
			pSegments = flatCounterParser.split(counterName);
		}
		for(String s: pSegments) {
			if(s.length() > 0) segments.add(s);
		}
		pSegments = segments.toArray(new String[segments.size()]);
		metricName = pSegments[pSegments.length-1].replaceAll("\\|", "}").replaceAll(":", ";");
		metricSegment = new String[pSegments.length-1];
		System.arraycopy(pSegments, 0, metricSegment, 0, pSegments.length-1);
	}
		
	/**
	 * @return the metricName
	 */
	public String getMetricName() {
		return metricName;
	}
	/**
	 * @param metricName the metricName to set
	 */
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}
	/**
	 * @return the metricSegment
	 */
	public String[] getMetricSegment() {
		return metricSegment;
	}
	/**
	 * @param metricSegment the metricSegment to set
	 */
	public void setMetricSegment(String[] metricSegment) {
		this.metricSegment = metricSegment;
	}

	/**
	 * Renders a human readable string representing the counter
	 * @return A string rendered Segment
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("CounterSegment[");
		buffer.append("\n\tmetricName=").append(metricName);
		if (metricSegment == null) {
			buffer.append("\n\tmetricSegment=").append("null");
		} else {
			buffer.append("\n\tmetricSegment=").append(
					Arrays.asList(metricSegment).toString());
		}
		buffer.append("\n]");
		return buffer.toString();
	}
	
}
