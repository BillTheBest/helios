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
package org.helios.aop;

import javassist.Modifier;

/**
 * <p>Title: HModifier</p>
 * <p>Description: Enumerates the field or method protection level</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.aop.HModifier</code></p>
 */

public enum HModifier {
	PUBLIC(Modifier.PUBLIC),
	NOT_PUBLIC(~Modifier.PUBLIC),
	PRIVATE(Modifier.PRIVATE),
	NOT_PRIVATE(~Modifier.PRIVATE),
	PROTECTED(Modifier.PROTECTED),
	NOT_PROTECTED(~Modifier.PROTECTED),
	STATIC(Modifier.STATIC),
	NOT_STATIC(~Modifier.STATIC),
	FINAL(Modifier.FINAL),
	NOT_FINAL(~Modifier.FINAL),
	SYNCHRONIZED(Modifier.SYNCHRONIZED),
	NOT_SYNCHRONIZED(~Modifier.SYNCHRONIZED),
	VOLATILE(Modifier.VOLATILE),
	NOT_VOLATILE(~Modifier.VOLATILE),
	TRANSIENT(Modifier.TRANSIENT),
	NOT_TRANSIENT(~Modifier.TRANSIENT),
	NATIVE(Modifier.NATIVE),
	NOT_NATIVE(~Modifier.NATIVE),
	INTERFACE(Modifier.INTERFACE),
	NOT_INTERFACE(~Modifier.INTERFACE),
	ABSTRACT(Modifier.ABSTRACT),
	NOT_ABSTRACT(~Modifier.ABSTRACT),
	STRICT(Modifier.STRICT),
	NOT_STRICT(~Modifier.STRICT),
	ANNOTATION(Modifier.ANNOTATION),
	NOT_ANNOTATION(~Modifier.ANNOTATION),
	ENUM(Modifier.ENUM),
	NOT_ENUM(~Modifier.ENUM);
	
	private HModifier(int modifier) {
		this.modifier = modifier;
	}
	
	private final int modifier;

	/**
	 * @return the modifier
	 */
	public int getModifier() {
		return modifier;
	}
	
	public static int getModifier(HModifier...ps) {
		if(ps==null || ps.length<1) return 0;
		int flag = 0;
		for(HModifier p: ps) {
			if(p.name().startsWith("NOT_")) {
				flag = (flag & p.getModifier());
			} else {
				flag = (flag | p.getModifier());
			}
		}
		return flag;		
	}
	
	/**
	 * Determines if the passed bit-mask is enabled for this HModifier
	 * @param mod The bit mask to test
	 * @return true if the bit-mask is enabled for this HModifier
	 */
	public boolean isEnabledFor(int mod) {
		return (mod & modifier)==modifier;
	}
	
	/**
	 * Determines if the passed bit-mask is enabled for the passed HModifier
	 * @param modifier The HModifier to test for
	 * @param mod The bit-mask to test
	 * @return true if the bit-mask is enabled for the passed HModifier
	 */
	public static boolean isEnabledFor(HModifier modifier, int mod) {
		return (mod & modifier.getModifier())==modifier.getModifier();
	}
	
	public static String getModifierNames(int mod) {
		StringBuilder names = new StringBuilder("[");
		for(HModifier hmod: HModifier.values()) {
			if(hmod.name().startsWith("XNOT_")) continue;
			if(isEnabledFor(hmod, mod)) {
				if(names.length()>1) {
					names.append(",");
				}
				names.append(hmod.name());
			}
		}
		names.append("]");
		return names.toString();
	}
	
	public static void main(String[] args) {
		log("Testing HModifiers");
		int i = HModifier.getModifier(PUBLIC, FINAL, STATIC);
		log("Mod:" + i + "-->" + HModifier.getModifierNames(i));
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
