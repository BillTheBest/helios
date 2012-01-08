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
 * <p>Title: MemoryInfo</p>
 * <p>Description: POJO to hold memory statistics for target host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class MemoryInfo implements Serializable{

	private static final long serialVersionUID = -3176520843323979443L;
	protected long total;
	protected long used;
	protected long free;
	protected double freePercent;
	protected double usedPercent;
	
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
	 * @return the freePercent
	 */
	public double getFreePercent() {
		return freePercent;
	}
	/**
	 * @param freePercent the freePercent to set
	 */
	public void setFreePercent(double freePercent) {
		this.freePercent = freePercent;
	}
	/**
	 * @return the usedPercentage
	 */
	public double getUsedPercent() {
		return usedPercent;
	}
	/**
	 * @param usedPercent the usedPercent to set
	 */
	public void setUsedPercent(double usedPercent) {
		this.usedPercent = usedPercent;
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
	    retValue.append("MemoryInfo ( " + super.toString() + TAB);
	    retValue.append("total = " + this.total + TAB);
	    retValue.append("used = " + this.used + TAB);
	    retValue.append("free = " + this.free + TAB);
	    retValue.append("freePercent = " + this.freePercent + TAB);
	    retValue.append("usedPercent = " + this.usedPercent + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}	
}
