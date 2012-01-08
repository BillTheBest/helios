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
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.XMLHelper;
import org.w3c.dom.Node;

/**
 * <p>Title: URLArchiveLink</p>
 * <p>Description: A class to process a <b><code>.jurl</code></b> file that represents a link to a java archive or direcotry containing classes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.io.file.jurl.URLArchiveLink</code></p>
 */

public class URLArchiveLink {
	/** static class logger */
	protected static final Logger LOG = Logger.getLogger(URLArchiveLink.class);
	/** The JURL file */
	protected File jurlFile;
	/** the extracted URLs */
	protected final TreeMap<Integer, URL> urls = new TreeMap<Integer, URL>();
	/** url counter */
	protected int counter = 1;
	/** the default jurl access protocol */
	protected static final Protocol DEFAULT_PROTOCOL  = Protocol.fs; 
	

	
	/**
	 * Loads the jurl file, parses it and generates a set of URLs to be added to the classpath. 
	 * For now, only loads basic jar references.
	 */
	protected void load() {
		Node node = XMLHelper.parseXML(jurlFile).getDocumentElement();
		LOG.info("Node name:[" + node.getNodeName() + "]");
		for(Node linkNode: XMLHelper.getChildNodesByName(node, "link", false)) {
			String root = null;
			Protocol protocol = DEFAULT_PROTOCOL;
			String pcol = XMLHelper.getAttributeValueByName(linkNode, "protocol");
			if(Protocol.isProtocol(pcol)) {
				protocol = Protocol.valueOf(pcol.toLowerCase());
			}
			String pattern = XMLHelper.getAttributeValueByName(linkNode, "pattern");
			boolean recursive = false;
			if(pattern!=null) {
				recursive = XMLHelper.getAttributeBooleanByName(linkNode, "recursive", false);
			}
			
			root = ConfigurationHelper.tokenFillIn(XMLHelper.getNodeTextValue(linkNode), true, new String[]{"\\", "/"}).replace("\\", "/");
			if(LOG.isDebugEnabled()) LOG.debug("Link Node: Root [" + root + "] Pattern [" + pattern + "]  Recursive [" + recursive + "]");
			if(Protocol.fs.equals(protocol)) {
				try {					
					URL url = protocol.getURL(root);
					urls.put(counter, url);
					LOG.info("Added JURL [" + url + "]");
					counter++;
				} catch (Exception e) {
					LOG.warn("Failed to convert root resource [" + root + "] to URL", e);
				}
			}
		}		
	}
	
	/**
	 * Returns the loaded URL references.
	 * @return the loaded URL references.
	 */
	public URL[] getURLArray() {
		return urls.values().toArray(new URL[urls.size()]);
	}
	
	/**
	 * Returns the loaded URL references.
	 * @return the loaded URL references.
	 */
	public Collection<URL> getURLs() {
		return Collections.unmodifiableCollection(urls.values());
	}
	
	
	

	/**
	 * Creates a new URLArchiveLink based on the passed file. 
	 * @param jurlFile the file to read
	 */
	public URLArchiveLink(File jurlFile)  {
		this.jurlFile = jurlFile;
		if(!this.jurlFile.canRead()) {
			throw new RuntimeException(new FileNotFoundException("The file [" + jurlFile + "] could not be read."));
		}
		load();
	}
	
	/*
<jurl>
	<link pattern=".*\.jar" recursive="true" >${M2_REPO}</link>
</jurl>

	 */
	
	/**
	 * Creates a new URLArchiveLink based on the passed file. 
	 * @param jurlFile the file to read
	 */
	public URLArchiveLink(CharSequence jurlFileName) {
		this(new File(nvl(jurlFileName, "The passed jurl file name was null").toString()));		
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		LOG.info("URLArchiveLink Test");
		URLArchiveLink ual = new URLArchiveLink("./src/test/resources/test-ref.jurl");
	}
	
}
