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
package org.helios.enums;

/**
 * <p>Title: MaskEnum</p> <p>Description: </p>  <p>Company: Helios Development Group</p>
 * @author   Whitehead (whitehead.nicholas@gmail.com)
 * @version   $LastChangedRevision$  <p><code>org.helios.enums.MaskEnum</code></p>
 */
public enum MaskEnum implements IBinaryCounter   {
	/**
	 * @uml.property  name="a"
	 * @uml.associationEnd  
	 */
	A,/**
	 * @uml.property  name="b"
	 * @uml.associationEnd  
	 */
	B,/**
	 * @uml.property  name="c"
	 * @uml.associationEnd  
	 */
	C,/**
	 * @uml.property  name="d"
	 * @uml.associationEnd  
	 */
	D,/**
	 * @uml.property  name="e"
	 * @uml.associationEnd  
	 */
	E,/**
	 * @uml.property  name="f"
	 * @uml.associationEnd  
	 */
	F,/**
	 * @uml.property  name="g"
	 * @uml.associationEnd  
	 */
	G,/**
	 * @uml.property  name="h"
	 * @uml.associationEnd  
	 */
	H;
	
	private MaskEnum() {
		code = counter.next();
	}
	
	/**
	 * @uml.property  name="code"
	 */
	private final int code;

	/**
	 * @return  the code
	 * @uml.property  name="code"
	 */
	public int getCode() {
		return code;
	}
	
	public static void main(String[] args) {
		log("MaskEnum Test");
		for(MaskEnum me: MaskEnum.values()) {
			log(me.name() + ":" + me.getCode() + "\t[" + Integer.toBinaryString(me.getCode()) + "]");		
		}
		int i = 0;
//		i = i | MaskEnum.B.code;
//		i = i | MaskEnum.D.code;
//		i = i | MaskEnum.H.code;
		log("i:[" + Integer.toBinaryString(i) + "]");
	}
	
	public static void log(Object message) {
		System.out.println(message);
	}
}
