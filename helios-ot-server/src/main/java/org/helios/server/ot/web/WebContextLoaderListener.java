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
package org.helios.server.ot.web;

import org.helios.spring.web.HeliosContextLoader;
import org.helios.spring.web.HeliosContextLoaderListener;
import org.springframework.web.context.ContextLoader;

/**
 * <p>Title: WebContextLoaderListener</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.web.WebContextLoaderListener</code></p>
 */

public class WebContextLoaderListener extends HeliosContextLoaderListener {

	/**
	 * Creates a new WebContextLoaderListener
	 */
	public WebContextLoaderListener() {
	}
	
	/**
	 * Create the ContextLoader to use.
	 * @return the new ContextLoader
	 * @see org.springframework.web.context.ContextLoaderListener#createContextLoader()
	 */
	@Override
	protected ContextLoader createContextLoader() {
		contextLoader = new WebContextLoader();
		LOG.info("Returning WebContextLoader");
		return contextLoader;
	}
	

}
