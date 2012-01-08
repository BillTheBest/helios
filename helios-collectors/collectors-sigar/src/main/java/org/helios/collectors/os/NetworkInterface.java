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
 * <p>Title: NetworkInterface</p>
 * <p>Description: POJO to hold network interface related information for target host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class NetworkInterface implements Serializable{

	private static final long serialVersionUID = 976237930448355146L;
	protected String name;
	protected String description;
	protected String type;
	protected String address;
	protected String hardwareAddress;
	protected String broadcast;
	protected String subnet;
	protected long mtu;
	protected long metric;
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
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return the hardwareAddress
	 */
	public String getHardwareAddress() {
		return hardwareAddress;
	}
	/**
	 * @param hardwareAddress the hardwareAddress to set
	 */
	public void setHardwareAddress(String hardwareAddress) {
		this.hardwareAddress = hardwareAddress;
	}
	/**
	 * @return the broadcast
	 */
	public String getBroadcast() {
		return broadcast;
	}
	/**
	 * @param broadcast the broadcast to set
	 */
	public void setBroadcast(String broadcast) {
		this.broadcast = broadcast;
	}
	/**
	 * @return the subnet
	 */
	public String getSubnet() {
		return subnet;
	}
	/**
	 * @param subnet the subnet to set
	 */
	public void setSubnet(String subnet) {
		this.subnet = subnet;
	}
	/**
	 * @return the mtu
	 */
	public long getMtu() {
		return mtu;
	}
	/**
	 * @param mtu the mtu to set
	 */
	public void setMtu(long mtu) {
		this.mtu = mtu;
	}
	/**
	 * @return the metric
	 */
	public long getMetric() {
		return metric;
	}
	/**
	 * @param metric the metric to set
	 */
	public void setMetric(long metric) {
		this.metric = metric;
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
	    retValue.append("NetworkInterface ( " + super.toString() + TAB);
	    retValue.append("name = " + this.name + TAB);
	    retValue.append("description = " + this.description + TAB);
	    retValue.append("type = " + this.type + TAB);
	    retValue.append("address = " + this.address + TAB);
	    retValue.append("hardwareAddress = " + this.hardwareAddress + TAB);
	    retValue.append("broadcast = " + this.broadcast + TAB);
	    retValue.append("subnet = " + this.subnet + TAB);
	    retValue.append("mtu = " + this.mtu + TAB);
	    retValue.append("metric = " + this.metric + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}
	

}
