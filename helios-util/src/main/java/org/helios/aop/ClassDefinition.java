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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: ClassDefinition</p>
 * <p>Description: Annotation to supply class creation properties to the DynaClassFactory.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.aop.ClassDefinition</code></p>
 */
@Target(value={ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ClassDefinition {

	/**
	 * If this value is defined, it represents a system property name, the value of which contains a URL to an XML document that contains the injected source.
	 * The <code>src</code> attribute is then interpreted as an XPath expression used to locate the source that should be injected.
	 * @return A string representing the system property name where the URL of the source XML file can be located. Or null/blank.
	 */
	String urlprop() default "";
	
	
	/**
	 * Specifies a class name that the generated class will extend
	 */
	String superClass() default "";
	
	/**
	 * Specifies interface class names that the generated class will implement
	 */
	String[] interfaces() default {};
	
	/**
	 * Specifies fields to be generated in the created class.
	 */
	FieldDefinition[] fields() default {};
	
	/**
	 * Specifies imports that are needed during the class compilation 
	 */
	String[] imports() default {};
	
	

}
