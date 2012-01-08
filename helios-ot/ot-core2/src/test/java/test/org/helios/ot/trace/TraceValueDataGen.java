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
package test.org.helios.ot.trace;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.helios.ot.type.MetricType;

/**
 * <p>Title: TraceValueDataGen</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.ot.trace.TraceValueDataGen</code></p>
 */

public class TraceValueDataGen {
	private static final Random random = new Random(System.nanoTime());
	private static final char[] ALPHA = new char[]{'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
	private static final int ALPHA_LENGTH = ALPHA.length;
	
	public static Object testValue(MetricType type) {
		if(type==null) return null;
		Class<?> dataType = type.getValueType().getBaseType();
		if(byte[].class.equals(dataType)) {
			byte[] byteArr = new byte[10];
			random.nextBytes(byteArr);
			return byteArr;
		} else if(int.class.equals(dataType)) {
			return random.nextInt();
		} else if(long.class.equals(dataType)) {
			return random.nextLong();
		} else if(String.class.equals(dataType)) {
			char[] arr = new char[10];
			for(int i = 0; i < 10; i++) {
				arr[i] = ALPHA[Math.abs(random.nextInt()%ALPHA_LENGTH)];
			}
			return new String(arr);
		} else {
			return null;
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Set<String> testTypes = new HashSet<String>();
		for(MetricType type: MetricType.values()) {
			log("TestData for [" + type.name() + "]:" + testValue(type));
		}

	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
