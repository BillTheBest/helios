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

import org.apache.log4j.BasicConfigurator;
import org.helios.helpers.ConfigurationHelper;
import org.helios.time.SystemClock;

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
		String uriString = OTServerDiscovery.discover();
		if(uriString!=null) {
			try {
				uri = new URI(uriString);
			} catch (Exception e) {
				try { uri = new URI(""); } catch (Exception ex) {
					throw new RuntimeException("Blank URI Failed. Should not happen !!", ex);
				}
			}
		}
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
		BasicConfigurator.configure();
		log("Loader Test");
		try {
			URI connectionUri = new URI("tcp://helioshq:9428");
			String protocol = connectionUri.getScheme();
			if(protocol==null) {
				protocol = ConfigurationHelper.getSystemThenEnvProperty(Configuration.PROTOCOL, "tcp").trim().toLowerCase();
			}
			HeliosOTClientProvider provider = reloadServiceProvider().getProvider(protocol);
			log("Provider:" + provider.getClass().getName());
			
			HeliosOTClient client = provider.newInstance(connectionUri);
			
			HeliosOTClientEventListener listener = new EmptyHeliosOTClientEventListener(){
				public void onConnect(HeliosOTClient client) {
					log("Client Connected: " + client);
				}
				public void onConnectFailure(HeliosOTClient client, Throwable cause) {
					log("Client Connection Failure: " + cause);
					if(cause!=null) {
						cause.printStackTrace(System.err);
					}
				}
				public void onDisconnect(HeliosOTClient client, Throwable cause) {
					log("Client Disconnected. Expected ?: " + (cause==null));
					if(cause!=null) {
						cause.printStackTrace(System.err);
					}
					
				}				
			};
			client.addListener(listener);
			client.setConnectTimeout(120000);
			client.setOperationTimeout(120000);
			client.connect(false);
			log("Client:" + client);
			log("Pinging......");
			int pingCount = 10000;
			long totalElapsed = 0L;
			for(int i = 0; i < pingCount; i++) {
				if(!client.ping()) {
					throw new RuntimeException("Ping timed out");
				}
			}
			for(int i = 0; i < pingCount; i++) {
				SystemClock.startTimer();
				if(!client.ping()) {
					throw new RuntimeException("Ping timed out");
				}
				totalElapsed += SystemClock.endTimer().elapsedMs;
				//log("\tPing Elapsed:" + SystemClock.endTimer());
			}
			log("Average Ping Time:" + avg(pingCount, totalElapsed) + " ms.");
			//Thread.currentThread().join(5000);			
			client.disconnect();
			Thread.currentThread().join(3000);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static long avg(double total, double count) {
		if(total<1 || count<1) return 0;
		double d = total/count;
		return (long)d;
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
