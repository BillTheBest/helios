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
package org.helios.collectors.wpm;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Title: WPMAttributes</p>
 * <p>Description: A POJO to hold WPM attributes for which the statistics needs to be collected.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class WPMAttributes {
	protected List<String> counter = new ArrayList<String>();
	protected List<String> service = new ArrayList<String>();
	protected List<String> fileSystem = new ArrayList<String>();
	protected List<String> process = new ArrayList<String>();
	
	/**
	 * @return the counter
	 */
	public List<String> getCounter() {
		return counter;
	}
	/**
	 * @param counter the counter to set
	 */
	public void setCounter(List<String> counter) {
		this.counter = counter;
	}
	/**
	 * @return the service
	 */
	public List<String> getService() {
		return service;
	}
	/**
	 * @param service the service to set
	 */
	public void setService(List<String> service) {
		this.service = service;
	}
	/**
	 * @return the fileSystem
	 */
	public List<String> getFileSystem() {
		return fileSystem;
	}
	/**
	 * @param fileSystem the fileSystem to set
	 */
	public void setFileSystem(List<String> fileSystem) {
		this.fileSystem = fileSystem;
	}
	/**
	 * @return the process
	 */
	public List<String> getProcess() {
		return process;
	}
	/**
	 * @param process the process to set
	 */
	public void setProcess(List<String> process) {
		this.process = process;
	}
	
}
