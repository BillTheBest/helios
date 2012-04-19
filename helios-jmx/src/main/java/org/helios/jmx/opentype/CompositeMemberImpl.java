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
package org.helios.jmx.opentype;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;

import org.helios.jmx.opentypes.OpenTypeManager;

/**
 * <p>Title: CompositeMemberImpl</p>
 * <p>Description:  Concrete CompositeMember bean</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentype.CompositeMemberImpl</code></p>
 */
public class CompositeMemberImpl {
	/** The name of the attribute */
	private final String name;
	/** The description of the attribute */
	private final String description;
	/** The OpenType of this attribute */
	private final OpenType<?> openType;
	/** Indicates if this attribute is a CompositeType index */
	private final boolean index;

	/**
	 * Creates a new CompositeMemberImpl
	 * @param method The annotated method
	 */
	public CompositeMemberImpl(Method method) {
		if(method==null) throw new IllegalArgumentException("The passed method was null", new Throwable());
		CompositeMember member = method.getAnnotation(CompositeMember.class);
		if(member==null) throw new IllegalArgumentException("The passed method [" + method.toGenericString() + "] is not annotated with @CompositeMember", new Throwable());		
		name = member.name().isEmpty() ? cleanName(method.getName()) : member.name();
		description = member.description().isEmpty() ? (method.getName() + " Composite Attribute") : member.description();
		index = member.index();
		Class<?> typePointer = member.openType();
		if(typePointer==CompositeMember.DefaultedType.class) {
			typePointer = method.getReturnType();
		}
		OpenType<?> ot = OpenTypeManager.getInstance().getOpenTypeOrNull(typePointer);
		if(ot==null) {
			CompositeDefinition cDef = typePointer.getAnnotation(CompositeDefinition.class);
			if(cDef!=null) {
				if(!cDef.name().isEmpty()) {
					ot = OpenTypeManager.getInstance().getOpenTypeOrNull(cDef.name());
				}
			}
		}
		if(ot==null) {
			throw new RuntimeException("Failed to get OpenType for class [" + typePointer.getName() + "]. Nested OpenType Discovery not supported yet.", new Throwable());
		}
		openType = ot;
	}
	
	protected static final Pattern METHOD_PREFIX_PATTERN = Pattern.compile("is|get");
	
	protected static String cleanName(String methodName) {
		return METHOD_PREFIX_PATTERN.matcher(methodName).replaceFirst("");
	}
	
	
	public static void main(String[] args) {
		try {
			CompositeType ct = CompositeDefinitionImpl.getCompositeType(Foo.class);
			log(ct);
			
			//log(cdi.toTabularType("FooTable", "A Foo Table Type"));
			//log(cdi.toTabularType());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	@CompositeDefinition
	public static class Foo {
		@CompositeMember(index=true)
		public int getInt() { return 4; }
		@CompositeMember(name="MyString")
		public String getString() { return ""; }		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * Returns the name of the attribute
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the description of the attribute
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * Returns the OpenType
	 * @return the openType
	 */
	public OpenType<?> getOpenType() {
		return openType;
	}


	/**
	 * Indicates if this attribute is an index for the type
	 * @return the index
	 */
	public boolean isIndex() {
		return index;
	}


	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append("CompositeMemberImpl [")
		    .append(TAB).append("name:").append(this.name)
		    .append(TAB).append("description:").append(this.description)
		    .append(TAB).append("openType:").append(this.openType.getTypeName())
		    .append(TAB).append("index:").append(this.index)
	    	.append("\n]");    
	    return retValue.toString();
	}
}
