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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.helios.helpers.XMLHelper;
import org.w3c.dom.Node;

/**
 * <p>Title: URLWebArchiveLink</p>
 * <p>Description: Extension of <code>URLArchiveLink</code> to support additional attributes for a simple WAR deployment. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.io.file.jurl.URLWebArchiveLink</code></p>
 */

public class URLWebArchiveLink  {
	
	/** Parsed war definitions */
	protected final Set<WarDefinition> parsedWars = new HashSet<WarDefinition>();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/** The WURL file */
	protected File wurlFile;

	/**
	 * <p>Title: WarDefinition</p>
	 * <p>Description: Wraps a war definition from a URLWebArchiveLink</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	public static class WarDefinition {
		/** The Spring bean name of the Jetty web server to deploy to */
		protected final String webServerBeanName;
		/** The context path of the war to be deployed */
		protected final String contextPath;
		/** The bean name of the war bean to be deployed */
		protected final String beanName;
		/** The resource path of the war bean to be deployed */
		protected final String resourcePath;
		
		/**
		 * Creates a new  WarDefinition from the passed node
		 * @param node
		 */
		public WarDefinition(Node node) {
			this.webServerBeanName = XMLHelper.getAttributeValueByName(node, "webserver", "HttpServer");
			this.contextPath = XMLHelper.getAttributeValueByName(node, "path");
			this.beanName = XMLHelper.getAttributeValueByName(node, "beanName");
			this.resourcePath = XMLHelper.getNodeTextValue(node);
		}
		
		
		
		/**
		 * The bean name of the war bean to be deployed
		 * @return the beanName
		 */
		public String getBeanName() {
			return beanName;
		}
		

		/**
		 * @return the webServerBeanName
		 */
		public String getWebServerBeanName() {
			return webServerBeanName;
		}

		/**
		 * @return the contextPath
		 */
		public String getContextPath() {
			return contextPath;
		}

		/**
		 * @return the resourcePath
		 */
		public String getResourcePath() {
			return resourcePath;
		}



		/**
		 * @return
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((beanName == null) ? 0 : beanName.hashCode());
			result = prime * result
					+ ((contextPath == null) ? 0 : contextPath.hashCode());
			result = prime * result
					+ ((resourcePath == null) ? 0 : resourcePath.hashCode());
			result = prime
					* result
					+ ((webServerBeanName == null) ? 0 : webServerBeanName
							.hashCode());
			return result;
		}



		/**
		 * @param obj
		 * @return
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WarDefinition other = (WarDefinition) obj;
			if (beanName == null) {
				if (other.beanName != null)
					return false;
			} else if (!beanName.equals(other.beanName))
				return false;
			if (contextPath == null) {
				if (other.contextPath != null)
					return false;
			} else if (!contextPath.equals(other.contextPath))
				return false;
			if (resourcePath == null) {
				if (other.resourcePath != null)
					return false;
			} else if (!resourcePath.equals(other.resourcePath))
				return false;
			if (webServerBeanName == null) {
				if (other.webServerBeanName != null)
					return false;
			} else if (!webServerBeanName.equals(other.webServerBeanName))
				return false;
			return true;
		}
		
	}
	
	/**
	 * Creates a new 
	 * @param jurlFile
	 */
	public URLWebArchiveLink(File wurlFile) {
		this.wurlFile = wurlFile;
		if(!this.wurlFile.canRead()) {
			throw new RuntimeException(new FileNotFoundException("The file [" + wurlFile + "] could not be read."));
		}
		load();
	}

	/**
	 * Creates a new 
	 * @param jurlFileName
	 */
	public URLWebArchiveLink(CharSequence wurlFileName) {
		wurlFile = new File(nvl(wurlFileName, "The passed wurl file name was null").toString());
		load();
	}
	

	/**
	 * Returns a set of the parsed war definitions
	 * @return a set of the parsed war definitions
	 */
	public Set<WarDefinition> getWars() {
		return Collections.unmodifiableSet(parsedWars);
	}

	
	
	protected void load() {		
		//super.load();
		Node node = XMLHelper.parseXML(wurlFile).getDocumentElement();
		for(Node linkNode: XMLHelper.getChildNodesByName(node, "link", false)) {
			try { 
				WarDefinition war = new WarDefinition(linkNode);
				parsedWars.add(war); 
			} catch (Exception e) {
				log.warn("Failed to parse war definition node", e);
			}
		}
	}
	

}
