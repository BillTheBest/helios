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
package org.helios.jmx.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;



/**
 * <p>Title: HeliosJMXClientFactory</p>
 * <p>Description: A factory for creating factories to create new instances of HeliosJMXClients.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */
public class HeliosJMXClientFactory {

	public static final Class[] CTOR_PARAMS = new Class[]{Properties.class};
	
	@SuppressWarnings("unchecked")
	public static HeliosJMXClient getInstance(Properties env) throws ConstructionException, ConnectionException { 
		HeliosJMXClient client = null;
		String className = env.getProperty(HeliosJMXClient.CLIENT_FACTORY, HeliosJMXClient.CLIENT_FACTORY_DEFAULT);
		try {
			Class<HeliosJMXClient> clazz = (Class<HeliosJMXClient>) Class.forName(className);
			Constructor<HeliosJMXClient> ctor = clazz.getConstructor(CTOR_PARAMS);
			client = ctor.newInstance(env);
			if(client instanceof StartableClient) {
				((StartableClient)client).start();
			}
			return client;
		} catch (ClassNotFoundException e) {
			throw new ConstructionException("Failed to locate class [" + className + "]", e);
		} catch (SecurityException e) {
			throw new ConstructionException("Security Exception on CTOR of class [" + className + "]", e);
		} catch (NoSuchMethodException e) {
			throw new ConstructionException("CTOR not found for class [" + className + "]", e);
		} catch (IllegalArgumentException e) {
			throw new ConstructionException("Illegal Argument to CTOR for class [" + className + "]", e);
		} catch (InstantiationException e) {
			throw new ConstructionException("Instantiation Exception on CTOR for class [" + className + "]", e);
		} catch (IllegalAccessException e) {
			throw new ConstructionException("Illegal Access to CTOR for class [" + className + "]", e);
		} catch (InvocationTargetException e) {
			throw new ConstructionException("CTOR Invocation Exception for class [" + className + "]", e);
		} catch (Exception e) {
			throw new ConstructionException("Unexpected Exception Creating Client for class [" + className + "]", e);
		}
		
	}
	

	
	
	
}
