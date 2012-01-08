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
package org.helios.jmx.dynamic.annotations.introspection;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helios.helpers.ClassHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.reflection.PrivateAccessor;

/**
 * <p>Title: IntrospectionExpression</p>
 * <p>Description: Annotation attributes may be configured to be an introspection expression which will result in the attribute value being reflected out of the specific object instance. </p>
 * <p>Introspection expressions supported are as follows:<ul>
 * <li><b>Field</b>:Retrieves the value from an object's field. Format: <code>$f{&lt;field name&gt;}</code> e.g. <code>$f{&lt;clientCode&gt;}</code></li>
 * <li><b>Method</b>: Retrieves the value from an object's <b>parameterless</b> method. Format: <code>$m{&lt;method name&gt;}</code> e.g. <code>$f{&lt;getClientCode&gt;}</code></li>
 * <li><b>Property</b>: Loads the value from a classpath resource.  Format: <code>${&lt;property name&gt;}</code> e.g. <code>$p{&lt;client.code&gt;}</code><br>
 *                                 The property resource that will be searched for is: <code>&lt;package-name&gt;.&lt;class-name&gt;.properties<code> </li>
 * </ul>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * org.helios.jmx.dynamic.annotations.IntrospectionExpression
 */
public class IntrospectionExpression {
	
	public static final Pattern EXPR_PATTERN = Pattern.compile("\\$[f|m|p]\\{(.*)\\}");
	
	
	/**
	 * Determines if the passed string is an Introspection Expression
	 * @param expression The string to test
	 * @return true if the passed string has the pattern of a Introspection Expression. false if it does not.
	 */
	public static boolean isExpression(String expression) {
		if(expression==null || expression.length() < 4) return false;
		return EXPR_PATTERN.matcher(expression).matches();
	}
	
	/**
	 * Returns the expression key
	 * @param expression The expression to return the key for
	 * @return returns the found key or null if the expression is invalid.
	 */
	public static String getExpressionKey(String expression) {
		if(!isExpression(expression)) return null;
		Matcher matcher = EXPR_PATTERN.matcher(expression);
		matcher.matches();
		matcher.group();
		return matcher.group(1);		
	}
	
	/**
	 * Returns the expression's operator.
	 * @param expression The expression
	 * @return An ExpressionOperator or null if the expression is invalid.
	 */
	public static ExpressionOperator getExpressionOperator(String expression) {
		if(!isExpression(expression)) return null;
		return ExpressionOperator.valueOf(expression.substring(1, 2));
	}
	
	/**
	 * Returns the resolved value of an expression 
	 * @param target The target object that the expression is associated with.
	 * @param expression The introspection expression.
	 * @return An object
	 * @throws IntrospectionExpressionException
	 */
	public static Object getExprValue(Object target, String expression) throws  IntrospectionExpressionException {
		if(target==null) throw new IntrospectionExpressionException("Expression [" + expression + "] requires a non-null target object");
		String key = getExpressionKey(expression);
		if(key==null) throw new IntrospectionExpressionException("Invalid expression [" + expression + "]");
		ExpressionOperator operator = getExpressionOperator(expression);
		if(operator==null) throw new IntrospectionExpressionException("Invalid expression [" + expression + "]");
		try {
			if(ExpressionOperator.f.equals(operator)) {
				return getFieldValue(target, key);
			} else if(ExpressionOperator.m.equals(operator)) {
				return getMethodValue(target, key);
			} else if(ExpressionOperator.p.equals(operator)) {
				return getPropertyValue(target, key);
			} else {
				throw new Exception("Unrecognized or unsupported Expression operator [" + operator + "]");
			}
		} catch (Exception e) {
			throw new IntrospectionExpressionException("Expression [" + expression + "] evaluation encountered exception", e);
		}
	}
	
	/**
	 * Returns the attribute name for the passed method JMXAttribute annotation
	 * @param targetObject the target object
	 * @param jmxAttr the JMXAttribute on the method being introspected
	 * @return the determined attribute name
	 * @throws IntrospectionExpressionException
	 */
	public static String getMethodAttributeName(Object targetObject, JMXAttribute jmxAttr) throws  IntrospectionExpressionException {
		String nameKey = jmxAttr.name();
		String attrName = getStringExprValue(targetObject, nameKey);
		return attrName==null ? nameKey : attrName;
	}
	
	/**
	 * Extracts the attribute name from a method.
	 * @param targetObject The instance of the object the method will be executed against.
	 * @param method The method to extract an attribute name from
	 * @param inherrited If true, climbs the inherritance tree to find the annotation.
	 * @return The determined attribute name
	 * @throws IntrospectionExpressionException
	 */
	public static String getMethodAttributeName(Object targetObject, Method method, boolean inherrited) throws  IntrospectionExpressionException {
		String attrName = null;
		JMXAttribute jmxAttr = null;
		String nameKey = null;
		try { jmxAttr = (JMXAttribute) ClassHelper.getAnnotationFromMethod(method, JMXAttribute.class, inherrited); } catch (Exception e) { jmxAttr=null; };
		if(jmxAttr!=null) {
			nameKey = jmxAttr.name();
			attrName = getStringExprValue(targetObject, nameKey);
			if(attrName==null) attrName = nameKey;
			return attrName;
		}  else {
			throw new IntrospectionExpressionException("The method [" + targetObject.getClass().getName() + "." + method.getName() + "] is not annotated with @JMXAttribute");
		}
	}
	
	/**
	 * Returns the resolved value of an expression as a string 
	 * @param target The target object that the expression is associated with.
	 * @param expression The introspection expression.
	 * @return A string
	 * @throws IntrospectionExpressionException
	 */
	public static String getStringExprValue(Object target, String expression) throws  IntrospectionExpressionException {
		Object o = getExprValue(target, expression);
		if(o==null) throw new IntrospectionExpressionException("Expression returned null");
		else return o.toString();
	}
	
	/**
	 * Returns the resolved value of an expression as a boolean 
	 * @param target The target object that the expression is associated with.
	 * @param expression The introspection expression.
	 * @return A boolean
	 * @throws IntrospectionExpressionException
	 */
	public static boolean getBooleanExprValue(Object target, String expression) throws  IntrospectionExpressionException {
		Object o = getExprValue(target, expression);
		if(o==null) throw new IntrospectionExpressionException("Expression returned null");
		else {
			if(Boolean.class.isAssignableFrom(o.getClass())) {
				return ((Boolean)o).booleanValue();
			} else {
				return o.toString().trim().equalsIgnoreCase("true") || o.toString().trim().equalsIgnoreCase("Y"); 
			}
		}
	}
	
	/**
	 * Retrieves the value of an object's field.
	 * @param target The target object
	 * @param fieldName The object's field name
	 * @return The value of the field.
	 * @throws Exception
	 */
	protected static Object getFieldValue(Object target, String fieldName) throws Exception {
			return PrivateAccessor.getFieldValue(target, fieldName);
	}
	
	/**
	 * Retrieves the return value of an object's method
	 * @param target The target object
	 * @param methodName The object's method name
	 * @return The return value of the method
	 * @throws Exception
	 */
	protected static Object getMethodValue(Object target, String methodName) throws Exception {
			return PrivateAccessor.invoke(target, methodName);
	}
	
	/**
	 * Retrieves the defined property from the classpath resource.
	 * @param target The target object
	 * @param propertyName The property key
	 * @return The value of the property
	 * @throws Exception
	 */
	protected static Object getPropertyValue(Object target, String propertyKey) throws Exception {		
		Class<?> clazz = target.getClass();
		String resource = new StringBuilder(clazz.getPackage().getName()).append(".").append(clazz.getName()).append(".properties").toString();
		Properties p = loadProperties(resource);		
		return p.get(propertyKey);
	}
	
	/**
	 * Loads a propery file from the classpath identified by the passed resource name.
	 * @param resourceName The resource name of the properties file.
	 * @return A possibly populated properties 
	 */
	protected static Properties loadProperties(String resourceName) {
		Properties p = new Properties();
		InputStream is = null;
		try {
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
			if(is != null) {
				p.load(is);
			}
		} catch (Exception e) {
		} finally {
			try { if(is != null) is.close(); } catch (Exception e) {}
		}
		return p;
	} 
	
}
