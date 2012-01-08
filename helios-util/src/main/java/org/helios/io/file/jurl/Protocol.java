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
package org.helios.io.file.jurl;

import static org.helios.helpers.ClassHelper.nvl;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * <p>Title: Protocol</p>
 * <p>Description: Enumerates the access protocols understood by the jurl processor.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.io.file.jurl.Protocol</code></p>
 */

public  enum Protocol {
	/** Straight file system reference */
	fs(new FileSystemURLFactory()),
	/** File based URL */
	file(new PrefixedURLFactory("file:")),
	/** HTTP based URL */
	http(new PrefixedURLFactory("http:"));		
	
	private Protocol(ProtocolURLFactory factory) {
		this.factory = factory;
	}
	
	private static final Map<String, Protocol> BY_NAME = new HashMap<String, Protocol>(Protocol.values().length);
	private final ProtocolURLFactory factory;
	static {
		for(Protocol p: Protocol.values()) {
			BY_NAME.put(p.name(), p);
		}
	}
	
	/**
	 * Converts the passed root resource to a URL
	 * @param root the root resource
	 * @return a URL
	 * @throws MalformedURLException
	 */
	public URL getURL(CharSequence root) throws MalformedURLException {
		return factory.getUrl(root);
	}
	
	
	/**
	 * Determines if the passed name is a valid protocol.
	 * @param name the name to test
	 * @return true if it is a valid protocol
	 */
	public static boolean isProtocol(CharSequence name) {
		if(name==null) return false;
		return BY_NAME.containsKey(name.toString().toLowerCase());
		
	}
	
	/**
	 * <p>Title: ProtocolURLFactory</p>
	 * <p>Description: A factory that each protocol instance uses to comfort the passed string into the correct URL.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	public static interface ProtocolURLFactory {
		/**
		 * Generates a URL from the passed string
		 * @param root the root identifier which is a URL resource
		 * @return a URL
		 * @throws MalformedURLException
		 */
		public URL getUrl(CharSequence root) throws MalformedURLException;
	}
	
	/**
	 * <p>Title: FileSystemURLFactory</p>
	 * <p>Description: ProtocolURLFactory implementation for simple file system references.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	public static class FileSystemURLFactory implements ProtocolURLFactory {
		/**
		 * Generates a URL from the passed string
		 * @param root the root identifier which is a URL resource
		 * @return a URL
		 * @throws MalformedURLException
		 */
		public URL getUrl(CharSequence root) throws MalformedURLException {
			File file = new File(nvl(root, "The passed root was null").toString());
			if(!file.canRead()) throw new MalformedURLException("The file [" + root + "] cannot be read");
			return file.toURI().toURL();
		}
	}
	
	/**
	 * <p>Title: PrefixedURLFactory</p>
	 * <p>Description: ProtocolURLFactory implementation for validation of File and HTTP URLs</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	public static class PrefixedURLFactory implements ProtocolURLFactory {
		protected final String prefix;
		
		public PrefixedURLFactory(String prefix) {
			super();
			this.prefix = prefix;
		}
		
		/**
		 * Generates a URL from the passed string
		 * @param root the root identifier which is a URL resource
		 * @return a URL
		 * @throws MalformedURLException
		 */
		public URL getUrl(CharSequence root) throws MalformedURLException {
			String sroot = nvl(root, "The passed root was null").toString();
			if(!sroot.startsWith(prefix)) {
				sroot = prefix + sroot;
			}
			return new URL(sroot);
		}
	}
	
}