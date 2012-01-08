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
 * <p>Title: ProcessInfo</p>
 * <p>Description: POJO to hold Process related information for target host</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ProcessInfo implements Serializable{
	
	private static final long serialVersionUID = 1513088680840475579L;
	protected long pid;
	protected String name="";
	protected String state="";
	protected String user="";
	protected String group="";
	protected String description="";
	protected long startTime;
	protected long memory;
	protected int priority;
	protected long threads;
	protected String[] arguments={""};
	protected String exeName="";
	protected String workingDir="";
	protected boolean isTraced;
	protected StringBuilder regexBase = new StringBuilder("");
	
	public ProcessInfo() {}
	/**
	 * Copy Constructor
	 *
	 * @param processInfo a <code>ProcessInfo</code> object
	 */
	public ProcessInfo(ProcessInfo processInfo) 
	{
	    this.pid = processInfo.pid;
	    this.state = processInfo.state;
	    this.user = processInfo.user;
	    this.group = processInfo.group;
	    this.description = processInfo.description;
	    this.startTime = processInfo.startTime;
	    this.memory = processInfo.memory;
	    this.priority = processInfo.priority;
	    this.threads = processInfo.threads;
	    this.arguments = processInfo.arguments;
	    this.exeName = processInfo.exeName;
	    this.workingDir = processInfo.workingDir;
	    this.regexBase = processInfo.regexBase;
	}
	/**
	 * @return the regexBase
	 */
	public String getRegexBase() {
		return regexBase.toString();
	}
	/**
	 * @param regexBase the regexBase to set
	 */
	public void createRegexBase() {
		if(exeName!=null)
			regexBase.append(exeName);
		if(workingDir!=null)
			regexBase.append(workingDir);
		if(arguments!=null){
			for (int j = 0; j < arguments.length; j++) {
				regexBase.append(arguments[j]);
			}
		}
		//System.out.println(regexBase);
	}
	/**
	 * @return the pid
	 */
	public long getPid() {
		return pid;
	}
	/**
	 * @param pid the pid to set
	 */
	public void setPid(long pid) {
		this.pid = pid;
	}
	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}
	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}
	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}
	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}
	/**
	 * @param group the group to set
	 */
	public void setGroup(String group) {
		this.group = group;
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
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}
	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}
	/**
	 * @param priority the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}
	/**
	 * @return the threads
	 */
	public long getThreads() {
		return threads;
	}
	/**
	 * @param threads the threads to set
	 */
	public void setThreads(long threads) {
		this.threads = threads;
	}
	
	/**
	 * @return the arguments
	 */
	public String[] getArguments() {
		return arguments.clone();
	}
	/**
	 * @param arguments the arguments to set
	 */
	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}
	/**
	 * @return the exeName
	 */
	public String getExeName() {
		return exeName;
	}
	/**
	 * @param exeName the exeName to set
	 */
	public void setExeName(String exeName) {
		this.exeName = exeName;
	}
	/**
	 * @return the workingDir
	 */
	public String getWorkingDir() {
		return workingDir;
	}
	/**
	 * @param workingDir the workingDir to set
	 */
	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
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
	 * @return the isTraced
	 */
	public boolean isTraced() {
		return isTraced;
	}
	/**
	 * @param isTraced the isTraced to set
	 */
	public void setTraced(boolean isTraced) {
		this.isTraced = isTraced;
	}
	/**
	 * @return the memory
	 */
	public long getMemory() {
		return memory;
	}
	/**
	 * @param memory the memory to set
	 */
	public void setMemory(long memory) {
		this.memory = memory;
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
	    retValue.append("ProcessInfo ( " + super.toString() + TAB);
	    retValue.append("pid = " + this.pid + TAB);
	    retValue.append("name = " + this.name + TAB);
	    retValue.append("state = " + this.state + TAB);
	    retValue.append("user = " + this.user + TAB);
	    retValue.append("group = " + this.group + TAB);
	    retValue.append("description = " + this.description + TAB);
	    retValue.append("startTime = " + this.startTime + TAB);
	    retValue.append("memory = " + this.memory + TAB);
	    retValue.append("priority = " + this.priority + TAB);
	    retValue.append("threads = " + this.threads + TAB);
	    retValue.append("exeName = " + this.exeName + TAB);
	    retValue.append("workingDir = " + this.workingDir + TAB);
	    retValue.append("isTraced = " + this.isTraced + TAB);
	    retValue.append("regexBase = " + this.regexBase + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}
	
	
}
