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
 * <p>Title: FileSystemInfo </p>
 * <p>Description: POJO to hold filesystem statistics for target host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class FileSystemInfo implements Serializable{

	private static final long serialVersionUID = 282491726564093203L;
	protected String deviceName="";
	protected String mountedOn="";
	protected String type="";
	protected String systemType="";
	protected long total;
	protected long used;
	protected long free;
	protected double freePercentage;
	protected double usedPercentage;
	protected long totalReads;
	protected long totalWrites;
	protected long totalBytesRead;
	protected long totalBytesWritten;
	/**
	 * @return the deviceName
	 */
	public String getDeviceName() {
		return deviceName;
	}
	/**
	 * @param deviceName the deviceName to set
	 */
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	/**
	 * @return the mountedOn
	 */
	public String getMountedOn() {
		return mountedOn;
	}
	/**
	 * @param mountedOn the mountedOn to set
	 */
	public void setMountedOn(String mountedOn) {
		this.mountedOn = mountedOn;
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
	 * @return the systemType
	 */
	public String getSystemType() {
		return systemType;
	}
	/**
	 * @param systemType the systemType to set
	 */
	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}
	/**
	 * @return the total
	 */
	public long getTotal() {
		return total;
	}
	/**
	 * @param total the total to set
	 */
	public void setTotal(long total) {
		this.total = total;
	}
	/**
	 * @return the used
	 */
	public long getUsed() {
		return used;
	}
	/**
	 * @param used the used to set
	 */
	public void setUsed(long used) {
		this.used = used;
	}
	/**
	 * @return the free
	 */
	public long getFree() {
		return free;
	}
	/**
	 * @param free the free to set
	 */
	public void setFree(long free) {
		this.free = free;
	}
	/**
	 * @return the freePercentage
	 */
	public double getFreePercentage() {
		return freePercentage;
	}
	/**
	 * @param freePercentage the freePercentage to set
	 */
	public void setFreePercentage(double freePercentage) {
		this.freePercentage = freePercentage;
	}
	/**
	 * @return the usedPercentage
	 */
	public double getUsedPercentage() {
		return usedPercentage;
	}
	/**
	 * @param usedPercentage the usedPercentage to set
	 */
	public void setUsedPercentage(double usedPercentage) {
		this.usedPercentage = usedPercentage;
	}
	/**
	 * @return the totalReads
	 */
	public long getTotalReads() {
		return totalReads;
	}
	/**
	 * @param totalReads the totalReads to set
	 */
	public void setTotalReads(long totalReads) {
		this.totalReads = totalReads;
	}
	/**
	 * @return the totalWrites
	 */
	public long getTotalWrites() {
		return totalWrites;
	}
	/**
	 * @param totalWrites the totalWrites to set
	 */
	public void setTotalWrites(long totalWrites) {
		this.totalWrites = totalWrites;
	}
	/**
	 * @return the totalBytesRead
	 */
	public long getTotalBytesRead() {
		return totalBytesRead;
	}
	/**
	 * @param totalBytesRead the totalBytesRead to set
	 */
	public void setTotalBytesRead(long totalBytesRead) {
		this.totalBytesRead = totalBytesRead;
	}
	/**
	 * @return the totalBytesWritten
	 */
	public long getTotalBytesWritten() {
		return totalBytesWritten;
	}
	/**
	 * @param totalBytesWritten the totalBytesWritten to set
	 */
	public void setTotalBytesWritten(long totalBytesWritten) {
		this.totalBytesWritten = totalBytesWritten;
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
	    retValue.append("FileSystemInfo ( " + super.toString() + TAB);
	    retValue.append("deviceName = " + this.deviceName + TAB);
	    retValue.append("mountedOn = " + this.mountedOn + TAB);
	    retValue.append("type = " + this.type + TAB);
	    retValue.append("systemType = " + this.systemType + TAB);
	    retValue.append("total = " + this.total + TAB);
	    retValue.append("used = " + this.used + TAB);
	    retValue.append("free = " + this.free + TAB);
	    retValue.append("freePercentage = " + this.freePercentage + TAB);
	    retValue.append("usedPercentage = " + this.usedPercentage + TAB);
	    retValue.append("totalReads = " + this.totalReads + TAB);
	    retValue.append("totalWrites = " + this.totalWrites + TAB);
	    retValue.append("totalBytesRead = " + this.totalBytesRead + TAB);
	    retValue.append("totalBytesWritten = " + this.totalBytesWritten + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}

}
