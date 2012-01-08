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
 * <p>Title: ProcessorUsageInfo</p>
 * <p>Description: POJO to hold processor usage information for target host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ProcessorUsageInfo implements Serializable{

	private static final long serialVersionUID = -2848250350094687994L;
    protected double user;
    protected double sys;
    protected double nice;
    protected double wait;
    protected double idle;
    protected double combined;
    
	/**
	 * @return the user
	 */
	public double getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(double user) {
		this.user = user;
	}
	/**
	 * @return the sys
	 */
	public double getSys() {
		return sys;
	}
	/**
	 * @param sys the sys to set
	 */
	public void setSys(double sys) {
		this.sys = sys;
	}
	/**
	 * @return the nice
	 */
	public double getNice() {
		return nice;
	}
	/**
	 * @param nice the nice to set
	 */
	public void setNice(double nice) {
		this.nice = nice;
	}
	/**
	 * @return the wait
	 */
	public double getWait() {
		return wait;
	}
	/**
	 * @param wait the wait to set
	 */
	public void setWait(double wait) {
		this.wait = wait;
	}
	/**
	 * @return the combined
	 */
	public double getCombined() {
		return user+sys+nice+wait;
	}
	/**
	 * @return the idle
	 */
	public double getIdle() {
		return idle;
	}
	/**
	 * @param idle the idle to set
	 */
	public void setIdle(double idle) {
		this.idle = idle;
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
	    retValue.append("ProcessorUsageInfo ( " + super.toString() + TAB);
	    retValue.append("user = " + this.user + TAB);
	    retValue.append("sys = " + this.sys + TAB);
	    retValue.append("nice = " + this.nice + TAB);
	    retValue.append("wait = " + this.wait + TAB);
	    retValue.append("idle = " + this.idle + TAB);
	    retValue.append("combined = " + this.combined + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}


}
