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
package org.helios.spring.container.templates.merging;

/**
 * <p>Title: StringArrayMerger</p>
 * <p>Description: </p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class StringArrayMerger implements ArgumentMerger {
	protected static final String[] typePattern = new String[]{};
	protected static final String NULL_SYMBOL = "NULL";

	/**
	 * @param value
	 * @return
	 * @see org.helios.spring.container.templates.merging.ArgumentMerger#buildInstance(java.lang.String)
	 */
	public String[] buildInstance(String value) {
		String[] parsed = value.split(",");
		if(parsed==null) return new String[]{};
		else return parsed;
	}


	/**
	 * @param index
	 * @param tArr
	 * @param pArr
	 * @return
	 */
	protected String merge(int index, String[] tArr, String[] pArr) {
		if(index  > (tArr.length-1)) {
			if(index  > (pArr.length-1)) {
				return NULL_SYMBOL;
			} else {
				return clean(pArr[index]);
			}
		} else if(index  > (pArr.length-1)) {
			if(index  > (tArr.length-1)) {
				return NULL_SYMBOL;
			} else {
				return clean(tArr[index]);
			}
		} else {
			return clean(mergeArrEntry(tArr[index], pArr[index]));
		}		
	}
	
	protected String mergeArrEntry(String tVal, String pVal) {
		if(pVal.length()<1 || pVal.equals("null")) {
			return tVal;
		} else {
			return pVal;
		}
	}

	/**
	 * @param raw
	 * @return
	 */
	protected String clean(String raw) {
		if(raw==null || raw.length()<1 || raw.equals("null")) {
			return NULL_SYMBOL;
		} else {
			return raw;
		}
	}

	/**
	 * @param templateArgument
	 * @param providerArgument
	 * @return
	 * @see org.helios.spring.container.templates.merging.ArgumentMerger#mergeArguments(java.lang.Object, java.lang.Object)
	 */
	public String mergeArguments(String templateArgument, String providerArgument) {
		if(templateArgument==null) return providerArgument;
		if(providerArgument==null) return templateArgument;
		String[] tArr = buildInstance(templateArgument);
		String[] pArr = buildInstance(providerArgument);
		String[] merged = new String[Math.max(tArr.length, pArr.length)];

		for(int i = 0; i < merged.length; i++) {
			merged[i] = merge(i, tArr, pArr);
		}
		return marshall(merged);
	}
	
	/**
	 * @param arr
	 * @return
	 */
	public String marshall(Object o) {
		String[] arr = (String[])o;
		if(arr==null || arr.length < 1) return "";
		StringBuilder b = new StringBuilder();
		for(String s: arr) {
			b.append(s).append(",");
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();		
	}

	/**
	 * @return
	 * @see org.helios.spring.container.templates.merging.ArgumentMerger#supportedType()
	 */
	public Class supportedType() {
		return typePattern.getClass();
	}

}
