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
package org.helios.spring.container;

import java.io.File;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * <p>Title: SubContext</p>
 * <p>Description: A container for a created subcontext.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SubContext {
	protected FileSystemXmlApplicationContext context = null;
	protected String configLocation = null;
	protected long timeStamp = 0L;
	
	/**
	 * Creates a new SubContext
	 * @param context The AppContext to manage.
	 * @param configLocation The location of the configuration file.
	 */
	public SubContext(FileSystemXmlApplicationContext context, String configLocation) {
		super();
		this.context = context;
		this.configLocation = configLocation;
		File f = new File(configLocation);
		timeStamp = f.lastModified();		
	}

	/**
	 * @return the context
	 */
	public FileSystemXmlApplicationContext getContext() {
		return context;
	}

	/**
	 * @param context the context to set
	 */
	public void setContext(FileSystemXmlApplicationContext context) {
		this.context = context;
	}

	/**
	 * @return the configLocation
	 */
	public String getConfigLocation() {
		return configLocation;
	}

	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return timeStamp;
	}
}
