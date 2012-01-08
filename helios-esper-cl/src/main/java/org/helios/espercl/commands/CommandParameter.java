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
package org.helios.espercl.commands;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: CommandParameter</p>
 * <p>Description: Provides meta-data about a Command parameter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.commands.CommandParameter</code></p>
 */
@Target(value={ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CommandParameter {
	/**
	 * Indicates if the parameter is optional or not.
	 * Optional parameters that are not mapped to a value will be passed a null.
	 */
	public boolean optional() default false;
	/**
	 * The usage description for this parameter. 
	 * If a value is not provided to override the default, the processor will implement one
	 * that will reference the data type and index of the parameter.
	 */
	public String description() default "";
	/**
	 * A non blank value indicates the class name of a property editor to use to convert the string to the desired type.
	 */
	public String propertyEditor() default "";
	
	/**
	 * Indicates a property editor will not be needed
	 */
	public boolean ignorePe() default false;
	
	/**
	 * The string value of an optional default.
	 */
	public String defaultValue() default ""; 
}
