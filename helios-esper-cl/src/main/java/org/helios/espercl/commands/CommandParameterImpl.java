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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import org.helios.helpers.ClassHelper;

/**
 * <p>Title: CommandParameterImpl</p>
 * <p>Description: Impl for the <code>@CommandParameter</code> annotation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.commands.CommandParameterImpl</code></p>
 */

public class CommandParameterImpl {
	/** The index of the method parameter */
	protected final int parameterIndex;
	/** The @CommandParameter instance attached to the target parameter */
	public final CommandParameter commandParameter;
	/** The parameter type */
	protected final Class<?> parameterType;
	/** Indicates if the parameter is optional */
	protected final boolean optional;
	/** The defined default value */
	protected final String defaultValue;
	/** The parameter description */
	protected final String description;
	/** The determined property editor */
	protected PropertyEditor propertyEditor;
	/** Indicates a property editor will not be needed */
	protected final boolean ignorePe;
	
	/**
	 * Creates a new CommandParameterImpl
	 * @param commandMethod The method to extract the CommandParameterImpl from
	 * @param parameterIndex The index of the method parameter
	 */
	public CommandParameterImpl(Method commandMethod, int parameterIndex) {
		commandMethod.setAccessible(true);
		Command cmd = commandMethod.getAnnotation(Command.class);
		if(cmd==null) {
			throw new RuntimeException("The commandMethod [" + commandMethod.toGenericString() + "] is not annotated with @CommandParameter", new Throwable());
		}
		CommandParameter[] cps = cmd.params();
		if(parameterIndex>cps.length-1) {
			throw new RuntimeException("The parameter [" + parameterIndex + "] in the method [" + commandMethod.toGenericString() + "] does not have a matching @CommandParameter", new Throwable());
		}
		commandParameter = cps[parameterIndex];
		if(commandParameter==null) throw new RuntimeException("The parameter [" + parameterIndex + "] in the method [" + commandMethod.toGenericString() + "] is not annotated with @CommandParameter", new Throwable());
		this.parameterIndex = parameterIndex;
		parameterType = commandMethod.getParameterTypes()[this.parameterIndex];
		optional = commandParameter.optional();
		ignorePe = commandParameter.ignorePe();
		description = (!"".equals(commandParameter.description()) ? commandParameter.description() : "Parameter [" + parameterIndex + "] of type [" + parameterType.getName() + "]");   
		String peName = commandParameter.propertyEditor();
		try {
			propertyEditor = ("".equals(peName)) ? PropertyEditorManager.findEditor(parameterType) : (PropertyEditor)Class.forName(peName).newInstance();
		} catch (Exception e) {
			//throw new RuntimeException("Failed to find property editor for the parameter [" + parameterIndex + "] in the method [" + commandMethod.toGenericString() + "] is not annotated with @CommandParameter", e);
		}
		if(propertyEditor==null && ignorePe==false) {
			throw new RuntimeException("Failed to find property editor for the parameter [" + parameterIndex + "] in the method [" + commandMethod.toGenericString() + "] is not annotated with @CommandParameter", new Throwable());
		}
		defaultValue = commandParameter.defaultValue()==null ? null : commandParameter.defaultValue();
		 
	}
	
	/**
	 * Returns the passed string edited into the correct type
	 * @param strValue the input string value
	 * @return the converted value
	 */
	public Object getValue(String strValue) {
		try {
			if(strValue==null) {
				if(defaultValue!=null) {
					strValue = defaultValue;
				} else {
					if(!optional) {
						throw new RuntimeException("The parameter [" + parameterIndex + "] is mandatory and does not have a default value");
					} else {
						return null;
					}
				}
			}
			if(strValue.length()<1 && CharSequence.class.isAssignableFrom(parameterType)) {
				return "";
			}
			synchronized(propertyEditor) {
				propertyEditor.setAsText(strValue);
				return propertyEditor.getValue();
			}
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	/**
	 * @return the parameterIndex
	 */
	public int getParameterIndex() {
		return parameterIndex;
	}

	/**
	 * @return the commandParameter
	 */
	public CommandParameter getCommandParameter() {
		return commandParameter;
	}

	/**
	 * @return the parameterType
	 */
	public Class<?> getParameterType() {
		return parameterType;
	}

	/**
	 * @return the optional
	 */
	public boolean isOptional() {
		return optional;
	}

	/**
	 * @return the defaultValue
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return new StringBuilder(optional ? "[Optional]" : "").append(description).toString();
	}

	/**
	 * @return the propertyEditor
	 */
	public PropertyEditor getPropertyEditor() {
		return propertyEditor;
	}


}
