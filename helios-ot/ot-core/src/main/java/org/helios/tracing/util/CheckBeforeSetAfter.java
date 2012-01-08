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
package org.helios.tracing.util;

import org.helios.helpers.XMLHelper;
import org.w3c.dom.Node;

/**
 * <p>Title: CheckBeforeSetAfter</p>
 * <p>Description: A utility class that manages setting system properties according to a precedent.
 * The participants are:<ul>
 * <li>The property name.</li>
 * <li>The property value.</li>
 * <li>Properties to check before.</li>
 * <li>Properties to set after.</li>
 * </ul>
 * The order of processing is as follows:<ol>
 * <li>Each of the check before properties are checked in System. The first one that is found to be not null is used as the value for the targeted property.</li>
 * <li>If none of the check before properties are found to be not null, then the provided value is used and set in System.</li>
 * <li>Each of the set after properties is then set to the same value if they are null in System.</li>
 * </ol>
 * </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1647 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/util/CheckBeforeSetAfter.java $
 * $Id: CheckBeforeSetAfter.java 1647 2009-10-24 21:52:31Z nwhitehead $
 */
public class CheckBeforeSetAfter {
	protected String propertyName = null;
	protected String propertyValue = null;
	protected String[] checkBefore = null;
	protected String[] setAfter = null;
	protected boolean fired = false;
	protected String value = null;
	
	/**
	 * Constructor system property set.
	 * @param propertyName The name of the system property to set.
	 * @param propertyValue The default value to set the system property to.
	 * @param checkBefore The system properties to check before using the default.
	 * @param setAfter The system properties to set to the same value.
	 */
	public CheckBeforeSetAfter(String propertyName, String propertyValue, String[] checkBefore, String[] setAfter) {
		value = checkBeforeSetAfter(propertyName, propertyValue, checkBefore, setAfter);
		fired = true;
	}
	
	/**
	 * Constructor system property set with no checks or sets.
	 * @param propertyName The name of the system property to set.
	 * @param propertyValue The default value to set the system property to.
	 */
	public CheckBeforeSetAfter(String propertyName, String propertyValue) {
		value = checkBeforeSetAfter(propertyName, propertyValue, new String[]{}, new String[]{});
		fired = true;
	}
	
	
	/**
	 * Creates an unfired CheckBeforeSetAfter from the passed XML node.
	 * @param xml an XML config node.
	 */
	public CheckBeforeSetAfter(Node xml) {
		String checkBeforeVal = XMLHelper.getAttributeValueByName(xml, "checkbefore");
		if(checkBeforeVal != null) {
			checkBefore = checkBeforeVal.split(",");
		}
		String setAfterVal = XMLHelper.getAttributeValueByName(xml, "setafter");
		if(setAfterVal != null) {
			setAfter = setAfterVal.split(",");
		}
		propertyValue = XMLHelper.getAttributeValueByName(xml,"value");
		propertyName = XMLHelper.getAttributeValueByName(xml,"name");		
	}
	
/*
	<property checkbefore="org.helios.application.name" 
			  setafter="org.helios.application.name" 
			  value="myHostHeliosAgent" 
			  name="com.wily.introscope.agent.agentName"/>

 */	
	
	/**
	 * Constructor system property set.
	 * @param defer If true, the checkBeforeSetAfter operation is defered until fire.
	 * @param propertyName The name of the system property to set.
	 * @param propertyValue The default value to set the system property to.
	 * @param checkBefore The system properties to check before using the default.
	 * @param setAfter The system properties to set to the same value.
	 */
	public CheckBeforeSetAfter(boolean defer, String propertyName, String propertyValue, String[] checkBefore, String[] setAfter) {
		if(!defer){ 
			value = checkBeforeSetAfter(propertyName, propertyValue, checkBefore, setAfter);
			fired = true;
		}
		else {
			this.propertyName = propertyName;
			this.propertyValue = propertyValue;
			this.checkBefore = checkBefore==null ? new String[]{} : checkBefore.clone();
			this.setAfter = setAfter==null ? new String[]{} : setAfter.clone();
		}
	}	
	
	/**
	 * Executes the defered checkBeforeSetAfter operation if it has not already been fired.
	 * @return The property's set value.
	 */
	public String fire() {
		if(!fired) {
			value = checkBeforeSetAfter(propertyName, propertyValue, checkBefore, setAfter);
			fired = true;
		}
		return value;
	}
	
	/**
	 * Static system property set.
	 * @param propertyName The name of the system property to set.
	 * @param propertyValue The default value to set the system property to.
	 * @param checkBefore The system properties to check before using the default.
	 * @param setAfter The system properties to set to the same value.
	 * @return The final value of the set property.
	 */
	public static String checkBeforeSetAfter(String propertyName, String propertyValue, String[] checkBefore, String[] setAfter) {
		boolean foundCheckBefore = false;
		String foundValue = null;
		String finalValue = null;
		if(checkBefore != null) {
			for(String s: checkBefore) {
				foundValue = System.getProperty(s);
				if(foundValue != null) {
					foundCheckBefore=true;
					finalValue = foundValue;
					System.setProperty(propertyName, foundValue);
					break;
				}
			}
		}
		if(!foundCheckBefore) {
			System.setProperty(propertyName, propertyValue);
			finalValue = propertyValue;
		}
		if(setAfter != null) {
			for(String s: setAfter) {
				foundValue = System.getProperty(s);
				if(foundValue == null) {
					System.setProperty(s, finalValue);
				}
			}
		}
		return finalValue;
	}

	/**
	 * Returns true if the checkBeforeSetAfter operation has been fired. 
	 * @return the fired
	 */
	public boolean isFired() {
		return fired;
	}

	/**
	 * Returns the result of the checkBeforeSetAfter value, or null if the op has not been fired.
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
}
