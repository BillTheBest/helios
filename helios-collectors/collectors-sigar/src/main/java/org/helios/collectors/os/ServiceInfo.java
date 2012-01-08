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
package org.helios.collectors.os;

/**
 * <p>Title: ServiceInfo</p>
 * <p>Description: POJO to hold service information for target Windows host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ServiceInfo extends ProcessInfo {

	private static final long serialVersionUID = 7016076008297884144L;
	protected String serviceName="";
	protected String displayName="";
	protected String startupType="";
	protected String[] dependencies={""};
	protected String status="";
	
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	public ServiceInfo(ProcessInfo pInfo) {
		super(pInfo);
	}
	public ServiceInfo() {}
	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}
	/**
	 * @param serviceName the serviceName to set
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * @return the startupType
	 */
	public String getStartupType() {
		return startupType;
	}
	/**
	 * @param startupType the startupType to set
	 */
	public void setStartupType(String startupType) {
		this.startupType = startupType;
	}
	/**
	 * @return the dependencies
	 */
	public String[] getDependencies() {
		return dependencies.clone();
	}
	/**
	 * @param dependencies the dependencies to set
	 */
	public void setDependencies(String[] dependencies) {
		this.dependencies = dependencies;
	}
	/**
	 * Constructs a <code>StringBuilder</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    StringBuilder retValue = new StringBuilder("");
	    retValue.append("ServiceInfo ( " + super.toString() + TAB);
	    retValue.append("serviceName = " + this.serviceName + TAB);
	    retValue.append("displayName = " + this.displayName + TAB);
	    retValue.append("startupType = " + this.startupType + TAB);
	    retValue.append("status = " + this.status + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}
}
