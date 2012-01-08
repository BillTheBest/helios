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
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: CPUInfo </p>
 * <p>Description: POJO to hold CPU statistics for target host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class CPUInfo implements Serializable{

	private static final long serialVersionUID = 5478610817625114850L;
	protected String vendor="";
	protected String model="";
	protected int mhz;
	protected int cpuSockets;
	protected int coresPerCpu;
	protected int totalProcessors;
	protected double totalUtilization;
	protected List<ProcessorUsageInfo> processorList = new ArrayList<ProcessorUsageInfo>();
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
	 * @return the mhz
	 */
	public int getMhz() {
		return mhz;
	}
	/**
	 * @param mhz the mhz to set
	 */
	public void setMhz(int mhz) {
		this.mhz = mhz;
	}
	/**
	 * @return the coresPerCpu
	 */
	public int getCoresPerCpu() {
		return coresPerCpu;
	}
	/**
	 * @param coresPerCpu the coresPerCpu to set
	 */
	public void setCoresPerCpu(int coresPerCpu) {
		this.coresPerCpu = coresPerCpu;
	}
	/**
	 * @return the model
	 */
	public String getModel() {
		return model;
	}
	/**
	 * @param model the model to set
	 */
	public void setModel(String model) {
		this.model = model;
	}
	/**
	 * @return the processorList
	 */
	public List<ProcessorUsageInfo> getProcessorUsageInfo() {
		return processorList;
	}
	/**
	 * @param processorList the processorList to set
	 */
	public void setProcessorUsageInfo(List<ProcessorUsageInfo> processorList) {
		this.processorList = processorList;
	}
	/**
	 * @return the cpuSockets
	 */
	public int getCpuSockets() {
		return cpuSockets;
	}
	/**
	 * @param cpuSockets the cpuSockets to set
	 */
	public void setCpuSockets(int cpuSockets) {
		this.cpuSockets = cpuSockets;
	}
	/**
	 * @return the totalProcessors
	 */
	public int getTotalProcessors() {
		return totalProcessors;
	}
	/**
	 * @param totalProcessors the totalProcessors to set
	 */
	public void setTotalProcessors(int totalProcessors) {
		this.totalProcessors = totalProcessors;
	}
	/**
	 * @return the totalUtilization
	 */
	public double getTotalUtilization() {
		return totalUtilization;
	}
	/**
	 * @param totalUtilization the totalUtilization to set
	 */
	public void setTotalUtilization(double totalUtilization) {
		this.totalUtilization = totalUtilization;
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
	    retValue.append("CPUInfo ( " + super.toString() + TAB);
	    retValue.append("vendor = " + this.vendor + TAB);
	    retValue.append("model = " + this.model + TAB);
	    retValue.append("mhz = " + this.mhz + TAB);
	    retValue.append("cpuSockets = " + this.cpuSockets + TAB);
	    retValue.append("coresPerCpu = " + this.coresPerCpu + TAB);
	    retValue.append("totalProcessors = " + this.totalProcessors + TAB);
	    retValue.append("totalUtilization = " + this.totalUtilization + TAB);
	    retValue.append("processorList = " + this.processorList + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}

}
