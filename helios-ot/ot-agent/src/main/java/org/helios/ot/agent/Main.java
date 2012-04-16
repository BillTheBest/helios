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
package org.helios.ot.agent;


import org.helios.version.VersionHelper;

/**
 * <p>Title: Main</p>
 * <p>Description: The main entry point for the Helios OpenTrace Agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.Main</code></p>
 */

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Helios OpenTrace Agent [" + VersionHelper.getHeliosVersion(Main.class) + "]");
//		System.out.println(banner());
//		if(args.length>0) {
//			if("server".equalsIgnoreCase(args[0])) {
//				String server = OTServerDiscovery.info();
//				if(server==null || server.trim().isEmpty()) {
//					System.out.println("No Helios OT Server Found");
//				} else {
//					System.out.println(server);
//				}
//			} else if("connect".equalsIgnoreCase(args[0])) {
//				@SuppressWarnings("rawtypes")
//				HeliosEndpoint ep = new HeliosEndpoint();
//				ep.connect();
//				
//			} else if("beacon".equalsIgnoreCase(args[0])) {
//				Beacon.main(new String[]{});
//			} else if("ping".equalsIgnoreCase(args[0])) {
//				System.out.println("Ping....");
//				@SuppressWarnings("rawtypes")
//				HeliosEndpoint ep = new HeliosEndpoint();
//				ep.connect();
//				while(true) {
//					try {
//						long start = System.currentTimeMillis();
//						boolean b = ep.ping();
//						long et = System.currentTimeMillis()-start;
//						
//						if(b) {
//							System.out.println("Ping OK:" + et);
//						} else {
//							System.out.println("Ping Failed");
//						}
//						Thread.sleep(1000);
//					} catch (Exception e) {
//						
//					}
//							
//										
//				}
//			}
//		}
//	}
		

	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
