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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: MethodDefinition</p>
 * <p>Description: Annotation to supply method definitions for a class to the DynaClassFactory.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.aop.MethodDefinition</code></p>
 */
@Target(value={ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodDefinition {
	/**
	 * Straight java raw source code to be inserted as the method's body (before the call to super, if applicable)
	 */
	String bsrc() default "";
	
	/**
	 * Straight java raw source code to be inserted as the method's body (after the call to super, if applicable)
	 */
	String asrc() default "";
	
	
	/**
	 * A reference to an XML node in the ClassDefinition's urlprop. (before the call to super, if applicable)
	 */
	String bref() default "";
	
	/**
	 * A reference to an XML node in the ClassDefinition's urlprop. (before the call to super, if applicable)
	 */
	String aref() default "";
	
	/**
	 * Indicates if the method should call super.x if the dynaclass has a defined parent class
	 */
	boolean callSuper() default false;
	
	/**
	 * Dynamically defined parameter definitions
	 */
	ParameterDefinition[] params() default {};
	
	/**
	 * Specifies an override of the templated method's return type.
	 */
	ReturnTypeDefinition retType() default @ReturnTypeDefinition();
	
	/**
	 * Dynamically defined local field definitions
	 */
	FieldDefinition[] fields() default {};
	
	/**
	 * The modifier of the method
	 */
	HModifier[] modifiers() default HModifier.PUBLIC;
	
	String fieldGetter() default "";
	
	String fieldSetter() default "";
	
	boolean replace() default false;
	
}
