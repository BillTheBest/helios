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

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: HeliosOTClientProviderSet</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.HeliosOTClientProviderSet</code></p>
 */
public class HeliosOTClientProviderSet implements Iterable<HeliosOTClientProvider> {
	/** The provider loader */
	private static ServiceLoader<HeliosOTClientProvider> providerSetLoader = ServiceLoader.load(HeliosOTClientProvider.class);
	/** The parent package name used by the service loader to create protocol implementations */
	public static final String PROTOCOL_PACKAGE = "org.helios.ot.agent.protocol";
	/** A cache of provider constructors keyed by the protocol name */
	private static Map<String, Constructor<HeliosOTClientProvider>> cachedCtors = new ConcurrentHashMap<String, Constructor<HeliosOTClientProvider>>();
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<HeliosOTClientProvider> iterator() {
		return providerSetLoader.iterator();
	}
	
	/**
	 * Reloads the service provider
	 */
	public void reload() {
		providerSetLoader.reload();
	}
	
	/**
	 * Locates and returns an instance of the HeliosOTClientProvider supporting the passed protocol
	 * @param protocolName The name of the protocol to get a HeliosOTClientProvider for
	 * @return a HeliosOTClientProvider
	 */
	public HeliosOTClientProvider getProvider(String protocolName) {
		if(protocolName==null) throw new IllegalArgumentException("The passed protocol name was null", new Throwable());
		protocolName = protocolName.trim().toLowerCase();
		for(HeliosOTClientProvider provider:this) {
			if(provider.getProtocolName().equals(protocolName)) {
				return provider;
			}
		}
		try {
			Constructor<HeliosOTClientProvider> ctor = cachedCtors.get(protocolName);
			if(ctor==null) {
				synchronized(cachedCtors) {
					ctor = cachedCtors.get(protocolName);
					if(ctor==null) {
						Class<HeliosOTClientProvider> clazz = (Class<HeliosOTClientProvider>) Class.forName(PROTOCOL_PACKAGE + "." + protocolName + ".HeliosOTClientProvider");
						ctor = clazz.getDeclaredConstructor();
						cachedCtors.put(protocolName, ctor);
					}
				}
			}
			return ctor.newInstance();
		} catch (Exception e) {}
		throw new IllegalArgumentException("Failed to find provider for passed protocol name [" + protocolName + "]", new Throwable());
	}
	
	public String toString() {
		return providerSetLoader.toString();
	}
	
}
