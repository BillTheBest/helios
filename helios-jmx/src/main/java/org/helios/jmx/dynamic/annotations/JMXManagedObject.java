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
package org.helios.jmx.dynamic.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: JMXManagedObject</p>
 * <p>Description: Annotation to allow fine grained control of how an object's meta data, attributes and operations are exposed through JMX.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface JMXManagedObject {
	public static final String NULL_OBJECT_NAME = ":type=null";
	/**
	 * If true if the managed object will only expose operations and attributes directly implemented by this class. If false, it will 
	 * expose operations and attributes of the current class and any super classes.
	 * @return true if declared. 
	 */
	boolean declared() default true;
	
	/**
	 * If true if the managed object should have only annotated methods exposed as operations and attributes.
	 * @return true if the managed object should have only annotated methods exposed as operations and attributes. 
	 */
	boolean annotated() default true; 
	
	/**
	 * A JMX ObjectName string that can be used by the MODB ctor to auto-register the MODB using the default domain names in <b><code>domains</code></b>/
	 */
	String objectName() default NULL_OBJECT_NAME;
	
	/**
	 * The default JMX domain names of MBeanServers where the MODB should be registered.
	 */
	String[] domains() default {"DefaultDomain"};
}
