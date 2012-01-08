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
package org.helios.tracing.bridge.config;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;

import org.helios.helpers.ConfigurationHelper;

/**
 * <p>Title: BridgePropertyImpl</p>
 * <p>Description: An instantiation of the @BridgeProperty annotation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.bridge.config.BridgePropertyImpl</code></p>
 */

public class BridgePropertyImpl {
	/** Indicates that changing this value at runtime will require restarting the bridge. */
	private final boolean requiresRestart;
	/** The default value in string format. */
	private final Object defaultValue;
	/** The value of the system property or environmental variable read default value for this property */
	private final String propertyValue;
	
	/**
	 * Creates a new BridgePropertyImpl for the passed method
	 * @param method The @BridProperty annotated method
	 */
	public BridgePropertyImpl(Method method) {
		if(method==null) throw new IllegalArgumentException("Passed method was null");
		Class<?>[] params = method.getParameterTypes();
		if(params.length <1 || params.length > 2) {
			throw new IllegalArgumentException("Passed method [" + method.toGenericString() + "] had an invalid number of arguments [" + params.length + "]");
		}
		if(!CharSequence.class.isAssignableFrom(params[0])) {
			throw new IllegalArgumentException("Passed method [" + method.toGenericString() + "] does not have a CharSequence for the first param type");
		}
		if(params.length==2) {
			if(!method.getReturnType().isAssignableFrom(params[1])) {
				throw new IllegalArgumentException("Passed method [" + method.toGenericString() + "] has default type incompatible with default param");
			}
		}
		BridgeProperty bp = method.getAnnotation(BridgeProperty.class);
		if(bp==null) {
			throw new IllegalArgumentException("Passed method [" + method.getDeclaringClass().getName() + "." + method.getName() + "] is not annotated with @BridgeProperty");
		}
		requiresRestart = bp.requiresRestart();
		String tmp = bp.propertyEditor();
		if(!"".equals(tmp)) {
			try {
				Class<? extends PropertyEditor> peClazz = (Class<? extends PropertyEditor>) Class.forName(tmp);
				PropertyEditorManager.registerEditor(method.getReturnType(), peClazz);
			} catch (Exception e) {
				throw new RuntimeException("The property editor class [" + tmp + "] could not be found");
			}
		}
		tmp = bp.defaultValue();
		if(!"".equals(tmp)) {
			if(method.getReturnType().equals(String.class)) {
				defaultValue=tmp;
			} else {
				PropertyEditor pe = PropertyEditorManager.findEditor(method.getReturnType());
				if(pe==null) throw new RuntimeException("Failed to find property editor for type [" + method.getReturnType().getName() + "] for the return type in [" + method.toGenericString() + "]");
				pe.setAsText(tmp);
				defaultValue = pe.getValue();
			}
		} else {
			defaultValue=null;
		}
		tmp = bp.propertyName();
		if(!"".equals(tmp)) {
			propertyValue = ConfigurationHelper.getSystemThenEnvProperty(tmp, null);
		} else {
			propertyValue=null;
		}
		
	}

	/**
	 * Indicates if changing the property at runtime will require restarting the bridge.
	 * @return if true, requires restarting the bridge
	 */
	public boolean isRequiresRestart() {
		return requiresRestart;
	}

	/**
	 * Returns the default value for the bridge property
	 * @return the defaultValue
	 */
	public Object getDefaultValue() {
		return propertyValue==null ? defaultValue : propertyValue;
	}


}
