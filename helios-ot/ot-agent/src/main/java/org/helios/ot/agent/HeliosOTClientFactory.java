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

import java.net.URI;
import java.net.URISyntaxException;

import org.helios.helpers.ConfigurationHelper;

/**
 * <p>Title: HeliosOTClientFactory</p>
 * <p>Description: A factory for Helios OT Clients</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.HeliosOTClientFactory</code></p>
 */
public class HeliosOTClientFactory {
	

	/** The service loader stub */
	private static volatile HeliosOTClientProviderSet providerSet = null;
	
	/**
	 * Returns a HeliosOTClient instance using discovery to acquire the server supplied connection URI
	 * @return a HeliosOTClient 
	 */
	public static HeliosOTClient newInstance() {
		URI uri = null;
		// perform discovery here
		// if fails, build URI from defaults in Configuration
		return newInstance(uri);
	}
	
	
	/**
	 * Returns a HeliosOTClient instance that will connect to the Helios OT Server using the passed URI 
	 * @param connectionUri The Helios OT Server connection URI
	 * @return a HeliosOTClient 
	 */
	public static HeliosOTClient newInstance(URI connectionUri) {
		if(connectionUri==null) {
			return newInstance();
		}
		if(providerSet==null) {
			providerSet = new HeliosOTClientProviderSet();
		}
		HeliosOTClientProvider clientProvider = providerSet.getProvider(connectionUri.getScheme());
		HeliosOTClient client = clientProvider.newInstance(connectionUri);
		return client;
	}

	/**
	 * Returns a HeliosOTClient instance that will connect to the Helios OT Server using the passed URI 
	 * @param connectionUri The Helios OT Server connection URI
	 * @return a HeliosOTClient 
	 */
	public static HeliosOTClient newInstance(String connectionUri) {
		if(connectionUri==null) {
			return newInstance();
		}
		try {
			return newInstance(new URI(connectionUri));
		} catch (URISyntaxException uex) {
			throw new RuntimeException("Invalid URI [" + connectionUri + "]", uex);
		}
	}
	
	/**
	 * Reloads the service provider if it has been created, otherwise creates it.
	 */
	public static HeliosOTClientProviderSet reloadServiceProvider() {
		if(providerSet==null) {
			providerSet = new HeliosOTClientProviderSet();
		} else {
			providerSet.reload();
		}		
		return providerSet;
	}
	
	public static void main(String[] args) {
		log("Loader Test");
		try {
			URI connectionUri = new URI("");
			String protocol = connectionUri.getScheme();
			if(protocol==null) {
				protocol = ConfigurationHelper.getSystemThenEnvProperty(Configuration.PROTOCOL, "tcp").trim().toLowerCase();
			}
			HeliosOTClientProvider provider = reloadServiceProvider().getProvider(protocol);
			log("Provider:" + provider.getClass().getName());
			
			HeliosOTClient client = provider.newInstance(connectionUri);
			log("Client:" + client);
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
//		provider = reloadServiceProvider().getProvider("foo");
//		log("Provider:" + provider.getClass().getName());
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
