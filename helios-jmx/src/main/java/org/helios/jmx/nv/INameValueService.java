/**
 * 
 */
package org.helios.jmx.nv;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

/**
 * <p>Title: INameValueService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface INameValueService {

	public static final String NOTIF_NAME_ADDED = "org.helios.jmx.namevalue.valueadded";
	public static final String NOTIF_NAME_REMOVED = "org.helios.jmx.namevalue.valueremoved";
	public static final String NOTIF_NAME_UPDATED = "org.helios.jmx.namevalue.valueupdated";

	/**
	 * @param name
	 * @return
	 */
	@JMXOperation(name = "get", description = "Retrieves the named value")
	public Object get(
			@JMXParameter(name = "name", description = "The name of the value to retrieve") String name);

	/**
	 * @param name
	 * @param value
	 * @return
	 */
	@JMXOperation(name = "set", description = "Sets the named value. Returns the prior bound value.")
	public Object set(
			@JMXParameter(name = "name", description = "The name of the value to set") String name,
			@JMXParameter(name = "value", description = "The value to set") Object value);

	/**
	 * @param name
	 * @return
	 */
	@JMXOperation(name = "remove", description = "Removes the named value")
	public Object remove(
			@JMXParameter(name = "name", description = "The name of the value to remove") String name);

	/**
	 * @param name
	 * @return
	 */
	@JMXOperation(name = "exists", description = "Checks for the existence of a value bound under the passed name")
	public boolean exists(
			@JMXParameter(name = "name", description = "The name of the value to check existence for") String name);

	/**
	 * @return
	 */
	@JMXAttribute(name = "names", description = "An array of all the names bound", mutability = AttributeMutabilityOption.READ_ONLY)
	public String[] names();

}