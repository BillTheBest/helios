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
package org.helios.helpers;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: EpochHelper</p>
 * <p>Description: Utility class to provide Unix epoch date conversions.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.helpers.EpochHelper</p></code>
 */
public class EpochHelper {
	/**
	 * Converts the passed UTC timestamp to an integral Unix time.
	 * @param time A time to convert in the form of a UTC long timestamp
	 * @return the unix time as an int.
	 */
	public static int getUnixTime(long time) {
		return (int) TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Converts the passed java timestamp to an integral Unix time.
	 * @param time A time to convert in the form of a UTC long timestamp
	 * @return the unix time as an int.
	 */
	public static int getUnixTime(Date time) {
		if(time==null) throw new RuntimeException("The passed date was null");
		return (int) TimeUnit.SECONDS.convert(time.getTime(), TimeUnit.MILLISECONDS);
	}	

	/**
	 * Converts the current UTC timestamp to an integral Unix time.
	 * @return the unix time as an int.
	 */
	public static int getUnixTime() {
		return (int) TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Converts the current UTC timestamp to a string Unix time.
	 * @return the unix time as an int string
	 */
	public static String getUnixTimeStr() {
		return "" + TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}	
	
	/**
	 * Converts the passed unix time to a UTC long timestamp
	 * @param unixTime the unix time to convert
	 * @return a UTC long timestamp
	 */
	public static long getUTCTime(int unixTime) {
		return TimeUnit.MILLISECONDS.convert(unixTime, TimeUnit.SECONDS);
	}

	/**
	 * Converts the passed unix time to a java timestamp
	 * @param unixTime the unix time to convert
	 * @return a java timestamp
	 */
	public static Date getUTCDate(int unixTime) {
		return new Date(TimeUnit.MILLISECONDS.convert(unixTime, TimeUnit.SECONDS));
	}
	
	
	/**
	 * Converts the passed unix time to a UTC long timestamp
	 * @param unixTime the unix time to convert
	 * @return a UTC long timestamp
	 */
	public static long getUTCTime(CharSequence unixTime) {
		return TimeUnit.MILLISECONDS.convert(Integer.parseInt(unixTime.toString().trim()), TimeUnit.SECONDS);
	}
	
	/**
	 * Converts the passed unix time to a java timestamp
	 * @param unixTime the unix time to convert
	 * @return a java timestamp
	 */
	public static Date getUTCDate(CharSequence unixTime) {
		return new Date(TimeUnit.MILLISECONDS.convert(Integer.parseInt(unixTime.toString().trim()), TimeUnit.SECONDS));
	}
	
	/**
	 * Converts the passed comma separated unix times to an array of UTC long timestamps
	 * @param unixTime the unix times to convert
	 * @return an array of UTC long timestamps
	 */
	public static long[] getUTCTimeRange(CharSequence unixTimes) {
		String[] times = unixTimes.toString().split(",");
		long[] utcTimes = new long[times.length];
		for(int i = 0; i < times.length; i++) {
			try {
				utcTimes[i] = getUTCTime(times[i]);
			} catch (Exception e) {
				throw new RuntimeException("Failed to convert item [" + i + "] with value [" + times[i] + "]", e);
			}
		}
		return utcTimes;
	}
	
	/**
	 * Converts the passed comma separated unix times to an array of java timestamps
	 * @param unixTime the unix times to convert
	 * @return an array of java timestamps
	 */
	public static Date[] getUTCDateRange(CharSequence unixTimes) {
		String[] times = unixTimes.toString().split(",");
		Date[] utcTimes = new Date[times.length];
		for(int i = 0; i < times.length; i++) {
			try {
				utcTimes[i] = getUTCDate(times[i]);
			} catch (Exception e) {
				throw new RuntimeException("Failed to convert item [" + i + "] with value [" + times[i] + "]", e);
			}
		}
		return utcTimes;
	}
	
	/**
	 * Builds a string metric name from an array of CharSequences.
	 * @param name an array of CharSequences.
	 * @return the built name.
	 */
	public static String makeName(CharSequence...name) {
		StringBuilder b = new StringBuilder();
		if(name==null || name.length<1) throw new RuntimeException("Name segments was null or zero length", new Throwable());
		for(CharSequence ch: name) {
			b.append(stripTrailing(ch, ".")).append(".");
			
		}
		return stripTrailing(b, ".").toString();
	}
	
	/**
	 * Strips the tailing symbols off a string
	 * @param cs
	 * @param symbol
	 * @return
	 */
	public static CharSequence stripTrailing(CharSequence cs, CharSequence symbol ) {
		if(cs==null) throw new RuntimeException("Passed CharSequence to strip was null", new Throwable());
		if(symbol==null) throw new RuntimeException("Passed symbol to strip was null", new Throwable());
		String css = cs.toString();
		String symbols = symbol.toString();
		while(css.endsWith(symbols)) {
			css = css.substring(0, css.length()-symbols.length());
		}
		return css;
	}

}
