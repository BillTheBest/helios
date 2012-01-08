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
package org.helios.helpers.classext;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * <p>Title: Access</p>
 * <p>Description: Defines member access levels and logic</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public enum Access {
	PUBLIC(1),
	PROTECTED(2),
	DEFAULT(3),
	PRIVATE(4);
	
	/**
	 * The code that maps the access to the <code>java.lang.reflect.Modifier</code> equivalent constant.
	 */
	private int modifierCode = -1;
	
	private Access(int modifierCode) {
		this.modifierCode = modifierCode;
	}
	
	public int modifierCode() {
		return modifierCode;
	}
	
	/**
	 * Returns true if the passed <code>Access</code> is equal to or more open than this access.
	 * @param access The access to compare to.
	 * @return true if passed access is of equal or more open than this access.
	 */
	public boolean asAccessible(Access access) {
		return this.modifierCode <= access.modifierCode;
	}
	
	/**
	 * Returns true if the passed <code>Access</code> is equal to or more restrictive than this access.
	 * @param access The access to compare to.
	 * @return true if passed access is of equal or more restricttive than this access.
	 */
	public boolean asRestrictive(Access access) {
		return this.modifierCode >= access.modifierCode;
	}
	
	
	/**
	 * Decodes a full member modifier set into an Access
	 * @param modifier
	 * @return
	 */
	public static Access fromModifiers(int modifier) {
		if(Modifier.isPrivate(modifier)) {
			return PRIVATE;
		} else if(Modifier.isProtected(modifier)) {
			return PROTECTED;
		} else if(Modifier.isPublic(modifier)) {
			return PUBLIC;
		} else {
			return DEFAULT;
		}
	}
	
	/**
	 * Decodes a single bit modifier into an Access.
	 * @param modifier
	 * @return
	 */
	public static Access fromModifier(int modifier) {
		switch (modifier) {
		case Modifier.PRIVATE:
			return PRIVATE;
		case 3:
			return DEFAULT;			
		case Modifier.PROTECTED:
			return PROTECTED;
		case Modifier.PUBLIC:
			return PUBLIC;
		default:
			throw new RuntimeException("The modifier [" + modifier + "] does not represent a method modifier");
		}
	}
	
	/**
	 * Tests the passed full modifiers and returns true if the modifer is at least as open as this Access.
	 * @param modifier the modifier to test
	 * @return returns true if the passed modifier access is at least as open as this Access.
	 */
	public boolean isAccessibleAs(int modifier) {		
		return asAccessible(fromModifiers(modifier));
	}
	
	/**
	 * Tests the passed full modifiers and returns true if the modifer is at least as restrictive as this Access.
	 * @param modifier the modifier to test
	 * @return returns true if the passed modifier access is at least as restrictive as this Access.
	 */
	public boolean isRestrictiveAs(int modifier) {		
		return asRestrictive(fromModifiers(modifier));
	}		
	
	

	/**
	 * Tests the modifiers for the passed member and returns true if the passed member access is at least as open as this Access.
	 * @param member the member to test
	 * @return returns true if the passed member access is at least as open as this Access, false if it is more restrictive or the member is null.
	 */
	public boolean isAccessibleAs(Member member) {
		if(member==null) return false;
		return isAccessibleAs(member.getModifiers());
	}
	
	
	/**
	 * Tests the modifiers for the passed class and returns true if the passed class access is at least as open as this Access.
	 * @param clazz The class to test
	 * @return returns true if the passed class access is at least as open as this Access, false if it is more restrictive or the class is null.
	 */
	public boolean isAccessibleAs(Class<?> clazz) {
		if(clazz==null) return false;
		return isAccessibleAs(clazz.getModifiers());
	}
	
	/**
	 * Tests the modifiers for the passed member and returns true if the passed member access is at least as restrictive as this Access.
	 * @param member the member to test
	 * @return returns true if the passed member access is at least as restrictive as this Access, false if it is less restrictive or the member is null.
	 */
	public boolean isRestrictiveAs(Member member) {
		if(member==null) return false;
		return isRestrictiveAs(member.getModifiers());
	}
	
	
	/**
	 * Tests the modifiers for the passed class and returns true if the passed class access is at least as restrictive as this Access.
	 * @param clazz The class to test
	 * @return returns true if the passed class access is at least as open as this Access, false if it is less restrictive or the class is null.
	 */
	public boolean isRestrictiveAs(Class<?> clazz) {
		if(clazz==null) return false;
		return isRestrictiveAs(clazz.getModifiers());
	}
	
	/**
	 * Returns the Access for the passed member
	 * @param member The member to get the Access for
	 * @return an Access
	 */
	public static Access memberAccess(Member member) {
		if(member==null) throw new RuntimeException("Member was null");
		return fromModifiers(member.getModifiers());
	}
	
	/**
	 * Returns the Access for the passed class
	 * @param clazz The class to get the Access for
	 * @return an Access
	 */
	public static Access classAccess(Class<?> clazz) {
		if(clazz==null) throw new RuntimeException("Class was null");
		return fromModifiers(clazz.getModifiers());
	}
	
	
	
}
