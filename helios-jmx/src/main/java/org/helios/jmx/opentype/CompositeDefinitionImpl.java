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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularType;

import org.helios.helpers.ClassHelper;
import org.helios.jmx.opentypes.OpenTypeManager;

/**
 * <p>Title: CompositeDefinitionImpl</p>
 * <p>Description: Concrete CompositeDefinition bean</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentype.CompositeDefinitionImpl</code></p>
 */
public class CompositeDefinitionImpl {
	/** The name of the type */
	private final String name;
	/** The description of the type */
	private final String description;
	/** The composite member names */
	private final Set<String> memberNames = new LinkedHashSet<String>();
	/** The composite member descriptions */
	private final List<String> memberDescriptions = new ArrayList<String>();
	/** The composite member types */
	private final List<OpenType<?>> memberTypes = new ArrayList<OpenType<?>>();
	/** The composite member index names */
	private final Set<String> memberIndexes = new LinkedHashSet<String>();
	
	
	/**
	 * Returns a composite type for the passed class
	 * @param clazz The class to generate a composite type for
	 * @return the composite type
	 */
	public static CompositeType getCompositeType(Class<?> clazz) {
		CompositeType ct = (CompositeType)OpenTypeManager.getInstance().getOpenTypeOrNull(clazz);
		if(ct==null) {
			CompositeDefinitionImpl cdi = new CompositeDefinitionImpl(clazz);
			ct = cdi.toCompositeType();			
			OpenTypeManager.getInstance().putOpenType(ct);
		}
		return ct;
	}
	
	/**
	 * Returns a tabular type for the passed class
	 * @param clazz The class to generate a tabular type for
	 * @param name The name of the type
	 * @param description The description of the type
	 * @return a tabular type
	 */
	public static TabularType getTabularType(Class<?> clazz, String name, String description) {
		TabularType tt = (TabularType)OpenTypeManager.getInstance().getOpenTypeOrNull(clazz);
		if(tt!=null) {
			if(!tt.getTypeName().equals(name) || !tt.getDescription().equals(description)) {
				try {
					return new TabularType(name, description, tt.getRowType(), tt.getIndexNames().toArray(new String[0]));
				} catch (OpenDataException e) {
					throw new RuntimeException("Failed to create name/desc overriden TabularType for [" + clazz.getName() + "]", e);
				}
			}
		} else {
			CompositeDefinitionImpl cdi = new CompositeDefinitionImpl(clazz);
			tt = cdi.toTabularType(name, description);
			OpenTypeManager.getInstance().putOpenType(tt);
		}
		return tt;
	}
	
	/**
	 * Returns a tabular type for the passed class
	 * @param clazz The class to generate a tabular type for
	 * @return a tabular type
	 */
	public static TabularType getTabularType(Class<?> clazz) {
		TabularType tt = (TabularType)OpenTypeManager.getInstance().getOpenTypeOrNull(clazz);
		if(tt==null) {
			CompositeDefinitionImpl cdi = new CompositeDefinitionImpl(clazz);
			tt = cdi.toTabularType();
			OpenTypeManager.getInstance().putOpenType(tt);
		}
		return tt;		
	}
	
	public static CompositeDataSupport getCompositeDataSupport(Object...objs) {
		if(objs==null || objs.length<1 || objs[0]==null) {
			throw new IllegalArgumentException("Passed Object Array was null, empty or had a null first item", new Throwable());
		}
		CompositeType ct = getCompositeType(objs[0].getClass());
		//CompositeDataSupport cds = new CompositeDataSupport(ct)
		return null;
	}
	
	/**
	 * Creates a new CompositeDefinitionImpl
	 * @param annotatedClass The annotated class to generate a CompositeDefinition for
	 */
	private CompositeDefinitionImpl(Class<?> annotatedClass) {
		if(annotatedClass==null) throw new IllegalArgumentException("The passed class was null", new Throwable());
		CompositeDefinition compDef = annotatedClass.getAnnotation(CompositeDefinition.class);
		if(compDef==null) throw new IllegalArgumentException("The passed class [" + annotatedClass.getName() + "] is not annotated with @CompositeDefinition", new Throwable());		
		name = compDef.name().isEmpty() ? annotatedClass.getName() : compDef.name();
		description = compDef.description().isEmpty() ? (annotatedClass.getSimpleName() + " Composite Type") : compDef.description();
		for(Method m: ClassHelper.getAnnotatedMethods(annotatedClass, CompositeMember.class, true)) {
			CompositeMemberImpl cdi = new CompositeMemberImpl(m);
			memberNames.add(cdi.getName());
			memberDescriptions.add(cdi.getDescription());
			memberTypes.add(cdi.getOpenType());
			if(cdi.isIndex()) {
				memberIndexes.add(cdi.getName());
			}
		}
	}
	
	/**
	 * Builds the composite type for this definition
	 * @return the composite type for this definition
	 */
	public CompositeType toCompositeType() {
		try {
			return new CompositeType(name, description, memberNames.toArray(new String[0]), memberDescriptions.toArray(new String[0]), memberTypes.toArray(new OpenType[0]));
		} catch (Exception e) {
			throw new RuntimeException("Failed to build composite type for [" + name + "]", e);
		}
	}
	
	/**
	 * Builds a tabular type for this definition
	 * @param name The tabular type name
	 * @param description The tabular type description
	 * @return The tabular type 
	 */
	public TabularType toTabularType(String name, String description) {
		try {
			return new TabularType(name, description, toCompositeType(), memberIndexes.toArray(new String[0]));
		} catch (Exception e) {
			throw new RuntimeException("Failed to build tabular type named [" + name + "] for [" + this.name + "]", e);
		}
	}
	
	/**
	 * Builds a tabular type for this definition using a synthetic name and description
	 * @return The tabular type 
	 */
	public TabularType toTabularType() {
		return toTabularType(name + "Table", "A table of " + description + "s");		
	}
	

	/**
	 * Returns the name of the type
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the description of the type
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder();    
	    retValue.append("CompositeDefinitionImpl [")
		    .append(TAB).append("name:").append(this.name)
		    .append(TAB).append("description:").append(this.description)
		    .append(TAB).append("members:").append(memberNames)
	    	.append("\n]");    
	    return retValue.toString();
	}
	
	
	
}
