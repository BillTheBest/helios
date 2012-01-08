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
package org.helios.webapp.jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * <p>Title: ClientRequestServlet</p>
 * <p>Description: JMX Request handling servlet for filling in where Jolokia leaves off. To be replaced with some better REST impl.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.webapp.jetty.ClientRequestServlet</code></p>
 */

public class ClientRequestServlet extends HttpServlet {
	/**  */
	private static final long serialVersionUID = 3414181688485689268L;
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());

	public ClientRequestServlet() {
		log.info("\n\t===========================\n\tStarted ClientRequestServlet\n\t===========================\n");
	}
	
	/**
	 * Processes JMX Requests:<ul>
	 * <li><b>Add Notification Listener</b> Param Siagnature:[<code><b>addJMXListener</b>/<b>&lt;Host&gt;</b>/<b>&lt;VMId&gt;</b>/<b>&lt;DefaultDomain&gt;</b>/<b>&lt;ObjectName&gt;</b></code>]
	 * 	<ol>
	 * 		<li><b>Host</b>: The host name of the MBeanServer</li>
	 *  	<li><b>VMId</b>: The vm id of the MBeanServer</li>
	 *  	<li><b>DefaultDomain</b>: The default domain of the MBeanServer</li>
	 *  	<li><b>ObjectName</b>: The ObjectName of the MBean to register a notification listener for.</li>
	 * 	</ol>
	 * </ul>
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(log.isDebugEnabled()) {
			log.debug("Processing Request:"); // add anatomizer here.
		}
		try {
			String[] queryFrags = request.getQueryString().split("/");
			
		} catch (Exception e) {
			log.error("Failed to process Client Request", e);
			throw new ServletException("Failed to process Client Request", e);
		}
	}

}
