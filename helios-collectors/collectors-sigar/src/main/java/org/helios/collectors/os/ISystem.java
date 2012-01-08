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

import java.util.List;

/**
 * <p>Title: ISystem</p>
 * <p>Description: An interface for pluggable OS collector implementation.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public interface ISystem {
	public OSInfo getOSInfo() throws MethodNotSupportedException, OSException;
	public long getUptime() throws MethodNotSupportedException, OSException;
	public List<ProcessInfo> getProcessList() throws MethodNotSupportedException, OSException;
	public ServiceInfo getServiceByName(String name) throws MethodNotSupportedException, OSException;
	public List<ServiceInfo> getServiceList() throws MethodNotSupportedException, OSException;
	public MemoryInfo getMemoryInfo() throws MethodNotSupportedException, OSException;
	public NetworkInfo getNetworkInfo() throws MethodNotSupportedException, OSException;
	public List<FileSystemInfo> getFileSystemInfo() throws MethodNotSupportedException, OSException;
	public CPUInfo getCPUInfo() throws MethodNotSupportedException, OSException;
	public List<ProcessInfo> getMatchedProcessList(String[] processMatcher) throws MethodNotSupportedException, OSException;
	//public boolean isReady();
}
