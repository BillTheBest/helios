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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.URLHelper;
import org.helios.helpers.XMLHelper;
import org.helios.ot.agent.discovery.OTServerDiscovery;
import org.helios.time.SystemClock;
import org.w3c.dom.Node;

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
	 * Scans the passed string array looking for command line provided XML configuration directives
	 * and if successfully located, returns the URL of the XML configuration resource
	 * @param commandLineArgs The string array to scan (most likely command line args)
	 * @return The XML resource URL or null if the arguments were not found, or were demonstrably incorrect
	 */
	protected static URL getXMLConfigURL(String...commandLineArgs) {
		// We're looking for the command line arguments that specify 
		//		the argument key {X}
		//      the config URL  {Y}
		// try to be friendly and allow 
		//	"{X} {Y}"     2
		//	"{X}={Y}"     minimum: 1
		//	"{X} = {Y}	  maximum: 3
		//	"{X}= {Y}	  2
		//	"{X} ={Y}	  2
		// There are a minimum of 1 and maximum of 3 possible arguments involved
		// So let's get the possible suspects first
//		String[] args = getPossFrags(ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]));
		String[] args = getPossFrags(commandLineArgs);
		//System.out.print(Arrays.toString(args) + "  :" + args.length + "\t:");
		String fileName = null;
		switch (args.length) {
		case 1:
			String[] frags = args[0].split("=");
			if(frags.length==2) fileName = frags[1];
			break;
		case 2:
			fileName = args[1].replace("=", "");
			break;
		case 3:
			fileName = args[2].replace("=", "");
			break;
		default:
			break;
		}
		//System.out.println(fileName==null ? "NULL" : fileName.trim());
		if(fileName!=null) {
			fileName = fileName.trim();
			File f = new File(fileName);
			if(f.canRead()) {
				return URLHelper.toURL(f);
			} else {
				if(URLHelper.isValidURL(fileName)) {
					return URLHelper.toURL(fileName);
				}				
			}
		}
		return null;
	}
	
	private static String[] getPossFrags(String...args) {
		int argsLength = args.length;
		String[] possFrags = {};
		for(int i = 0; i < argsLength; i++) {
			if(args[i].toLowerCase().startsWith(Configuration.XML_CONFIG)) {
				int mal = argsLength-i;
				int arrSize = mal>=3 ? 3 : mal==0 ? 1 : mal;
				possFrags = new String[arrSize];
				System.arraycopy(args, i, possFrags, 0, arrSize);
			}
		}
		return possFrags;		
	}
	
	public static void main(String[] args) {
		URL configUrl = getXMLConfigURL(Configuration.XML_CONFIG, "=", "./src/test/resources/agent/std/ot-agent.xml");
		log("Config URL:" + configUrl);
		Node doc = XMLHelper.parseXML(configUrl).getDocumentElement();
		Node propNode = XMLHelper.getChildNodeByName(doc, "properties", false);
		if(propNode!=null) {
			Properties props = new Properties();
			Properties modprops = new Properties();
			try {
				props.load(new StringReader(XMLHelper.getNodeTextValue(propNode).trim()));
				for(String key: props.stringPropertyNames()) {
					log(key + ":" + props.getProperty(key));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
//		String fn = "/home/nwhitehead/bookmarks.html";
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + "="));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + " ="));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + "=" + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + " =" + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + "= " + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + " = " + fn));
//		fn = "file://home/nwhitehead/bookmarks.html";
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + "=" + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + " =" + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + "= " + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + " = " + fn));
//
//		fn = "/home/nwhitehead/bookmarks.h";
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + "=" + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + " =" + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + "= " + fn));
//		log("URL:" + getXMLConfigURL(Configuration.XML_CONFIG + " = " + fn));

	}
	
	
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
	
	public static void mainX(String[] args) {
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
