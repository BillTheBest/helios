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
package org.helios.spring.web;

import org.apache.log4j.Logger;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

/**
 * <p>Title: HeliosContextLoaderListener</p>
 * <p>Description:A custom extended Spring Web Helios Context Loader Listener that injects the HeliosApplicationContext into the Spring web context </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.spring.web.HeliosContextLoaderListener</code></p>
 */
public class HeliosContextLoaderListener extends ContextLoaderListener {
	/** The internal context loader to load the Helios app context */
	protected ContextLoader contextLoader = null;
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(HeliosContextLoaderListener.class);
	/**
	 * Creates a new HeliosContextLoaderListener
	 */
	public HeliosContextLoaderListener() {
		super();
		LOG.info("Created HeliosContextLoaderListener" );
	}
	
	/**
	 * Create the ContextLoader to use.
	 * @return the new ContextLoader
	 * @see org.springframework.web.context.ContextLoaderListener#createContextLoader()
	 */
	@Override
	protected ContextLoader createContextLoader() {
		contextLoader = new HeliosContextLoader();
		LOG.info("Returning HeliosContextLoader");
		return contextLoader;
	}
	
	/**
	 * Return the ContextLoader used by this listener. 
	 * @return the current ContextLoader
	 * @see org.springframework.web.context.ContextLoaderListener#getContextLoader()
	 */
	@Override
	public ContextLoader getContextLoader() {
		return contextLoader;
	}
	

}
