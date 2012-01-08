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

import java.io.Serializable;

/**
 * <p>Title: OSInfo</p>
 * <p>Description: POJO to hold operating system specific information for target host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class OSInfo implements Serializable{

	private static final long serialVersionUID = -5658961956604971782L;
	protected String architecture="";
	protected String vendor=""; 
	protected String name="";
	protected String version="";
	protected String vendorVersion="";
	protected String patchLevel="";
	protected String dataModel="";
	protected String description="";
	/**
	 * @return the architecture
	 */
	public String getArchitecture() {
		return architecture;
	}
	/**
	 * @param architecture the architecture to set
	 */
	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}
	/**
	 * @return the vendor
	 */
	public String getVendor() {
		return vendor;
	}
	/**
	 * @param vendor the vendor to set
	 */
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	/**
	 * @return the vendorVersion
	 */
	public String getVendorVersion() {
		return vendorVersion;
	}
	/**
	 * @param vendorVersion the vendorVersion to set
	 */
	public void setVendorVersion(String vendorVersion) {
		this.vendorVersion = vendorVersion;
	}
	/**
	 * @return the patchLevel
	 */
	public String getPatchLevel() {
		return patchLevel;
	}
	/**
	 * @param patchLevel the patchLevel to set
	 */
	public void setPatchLevel(String patchLevel) {
		this.patchLevel = patchLevel;
	}
	/**
	 * @return the dataModel
	 */
	public String getDataModel() {
		return dataModel;
	}
	/**
	 * @param dataModel the dataModel to set
	 */
	public void setDataModel(String dataModel) {
		this.dataModel = dataModel;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
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
	    retValue.append("OSInfo ( " + super.toString() + TAB);
	    retValue.append("architecture = " + this.architecture + TAB);
	    retValue.append("vendor = " + this.vendor + TAB);
	    retValue.append("name = " + this.name + TAB);
	    retValue.append("version = " + this.version + TAB);
	    retValue.append("vendorVersion = " + this.vendorVersion + TAB);
	    retValue.append("patchLevel = " + this.patchLevel + TAB);
	    retValue.append("dataModel = " + this.dataModel + TAB);
	    retValue.append("description = " + this.description + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}
	
	
}
