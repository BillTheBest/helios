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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: FieldDefinition</p>
 * <p>Description: Annotation to supply field definitions for a class to the DynaClassFactory.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.aop.FieldDefinition</code></p>
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldDefinition {
	
	public static final class VoidClass {}
	
	/**
	 * The name of the field
	 */
	String name();
	
	/**
	 * The type of the field
	 */
	Class<?> type() default VoidClass.class;
	
	/**
	 * The type name of the field (in case it is not available at compile time)
	 */
	String typeName() default "";
	
	
	/**
	 * The protection of the field
	 */
	HModifier[] modifiers() default {HModifier.PUBLIC, HModifier.NOT_ABSTRACT};
	
	
	/**
	 * Defines a field initializer
	 */
	String initializer() default "";
	
	
	
	
}
