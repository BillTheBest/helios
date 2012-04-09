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
package org.helios.ot.helios;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.helpers.Banner;
import org.helios.ot.trace.MetricId;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.tracer.TracerManager3.Configuration;
import org.helios.version.VersionHelper;

/**
 * <p>Title: Beacon</p>
 * <p>Description: A simple bootstrap class for a HeliosEndpoint open trace agent that just phones home and sends standard metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.Beacon</code></p>
 */

public class Beacon {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger LOG = Logger.getLogger(Beacon.class);
		LOG.info("Helios OT Agent Beacon v." + VersionHelper.getHeliosVersion(Beacon.class));
		HeliosEndpoint he = new HeliosEndpoint();
		he.connect();
		TracerManager3.getInstance(Configuration.getDefaultConfiguration().appendEndPoint(he));
		
		LOG.info(Banner.banner("*", 2, 10, "Helios OT Agent Beacon Started.", "Agent Name:" + MetricId.getApplicationId()));
		
		try { Thread.currentThread().join(); } catch (Exception e) {}
	}

}
