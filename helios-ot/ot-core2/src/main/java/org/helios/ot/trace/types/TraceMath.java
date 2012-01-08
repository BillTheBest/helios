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
package org.helios.ot.trace.types;

/**
 * <p>Title: TraceMath</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.TraceMath</code></p>
 */

public class TraceMath {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Over Int Max:" + ((Integer.MAX_VALUE-1) + (2)));
		log("Int Max                 :" + Integer.MAX_VALUE);
		log("Avg of IntMax and IntMax:" + avgi(Integer.MAX_VALUE, Integer.MAX_VALUE));
		log("Over Long Max:" + ((Long.MAX_VALUE-1) + (2)));
		log("Long Max                  :" + Long.MAX_VALUE);
		log("Avg of LongMax and LongMax:" + avgl(Long.MAX_VALUE, Long.MAX_VALUE));
		log("Long Max                  :" + (Long.MAX_VALUE-(((long)Integer.MAX_VALUE)*2)));
		log("Avg of LongMax and LongMax:" + avgl(Long.MAX_VALUE-((Integer.MAX_VALUE)), Long.MAX_VALUE-(((long)Integer.MAX_VALUE)*3)));

	}
	
	public static int avgi(double a, double b) {
		a = a/10;
		b = b/10;
		double d = (a+b)/2*10;
		return (int)d;
	}
	public static long avgl(double a, double b) {
		a = a/10;
		b = b/10;
		double d = ((a+b)/2)*10;
		return (long)d;
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
