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
package org.helios.ot.subtracer;

import java.util.Arrays;

/**
 * <p>Title: SubTracerKey</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.SubTracerKey</code></p>
 */

public class SubTracerKey {
	/** The tracer key elements */
	protected final String[] keyElements;
	
	public SubTracerKey(Class<? extends DelegatingTracer> subTracerType, String...subKeys) {
		if(subTracerType==null) throw new IllegalArgumentException("Passed Type Was Null", new Throwable());
		keyElements = makeArr(subTracerType, subKeys);
	}
	
	public static SubTracerKey makeKey(Class<? extends DelegatingTracer> subTracerType, String...subKeys) {
		return new SubTracerKey(subTracerType, subKeys);
	}
	
	private static String[] makeArr(Class<? extends DelegatingTracer> subTracerType, String...subKeys) {
		String[] arr = new String[subKeys==null ? 1 : subKeys.length+1];
		arr[0] = subTracerType.getName();
		if(subKeys!=null) {
			for(int i = 0; i < subKeys.length; i++) {
				arr[i+1] = subKeys[i]==null ? "" : subKeys[i];
			}
		}
		return arr;		
	}

	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(keyElements);
		return result;
	}

	/**
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubTracerKey other = (SubTracerKey) obj;
		if (!Arrays.equals(keyElements, other.keyElements))
			return false;
		return true;
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("SubTracerKey ")
	        .append(Arrays.toString(this.keyElements));
	    return retValue.toString();
	}
}
