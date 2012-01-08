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
package org.helios.spring.container;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.helios.io.file.RecursiveDirectorySearch;
import org.helios.io.file.filters.ConfigurableFileExtensionFilter;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.context.*;

/**
 * <p>Title: HeliosSpringContainer</p>
 * <p>Description: Bootstrap Container for Spring</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class HeliosSpringContainer implements ApplicationListener {
	/** The version for this component */
	public static final String VERSION = "HeliosSpringContainer v0.1";
	/** static class logger */
	protected static Logger LOG = Logger.getLogger(HeliosSpringContainer.class);
	/** the main Spring app context */
	protected FileSystemXmlApplicationContext applicationContext = null;
	/** A map of the subcontexts */
	protected Map<String, SubContext> subContexts = new ConcurrentHashMap<String, SubContext>();
	

	/**
	 * Main entry point to bootstrap Spring and configuration.
	 * @param args All args are names of directories that will be recursively searched for XML files to load into Spring.
	 */
	public static void main(String[] args) {
		LOG.info("Starting " + VERSION);
		HeliosSpringContainer hsc = new HeliosSpringContainer();
		hsc.bootStrap(args);
	}
	
	/**
	 * Boostraps this container.
	 * @param configDirectories names of directories that will be recursively searched for XML files to load into Spring.
	 */
	public void bootStrap(String[] configDirectories) {
		String[] configFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(".helios.xml"), configDirectories);
		LOG.info("Located [" + configFiles.length + "] configuration files.");
		if(LOG.isDebugEnabled()) {
			StringBuilder b = new StringBuilder("\nLocated Configuration Files:\n============================");
			for(String s: configFiles) {
				b.append("\n\t").append(s);
			}
			b.append("\n");
			LOG.debug(b.toString());
		}
		applicationContext = new FileSystemXmlApplicationContext(configFiles);
		applicationContext.addApplicationListener(this);
		StringBuilder b = new StringBuilder("\nDeployed Bean List:\n============================");
		String[] beans = applicationContext.getBeanDefinitionNames();
		for(String s: beans) {
			b.append("\n\t").append(s);
		}
		b.append("\n");
		LOG.info(b.toString());		
		
	}
	
	/**
	 * Refreshes the main AppContext once the subContexts have been configured.
	 */
	protected void refresh() {
		
		for(SubContext subContext: subContexts.values()) {
			subContext.getContext().refresh();
		}
		applicationContext.refresh();
	}
	
	/**
	 * Creates a new sub-AppContext and configures it with the passed configurationFile and the main AppContext as the parent.
	 * @param configurationFile
	 * @param parent The parent app context.
	 */
	protected void deploySubContext(String configurationFile, ApplicationContext parent) {
		String[] config = new String[]{configurationFile};
		try {
			if(LOG.isDebugEnabled()) LOG.debug("Deploying SubContext [" + configurationFile + "]"); 
			XmlBeanFactory bf = new XmlBeanFactory(new FileSystemResource(configurationFile));
//			SubContext subContext = new SubContext(new FileSystemXmlApplicationContext(config, false, parent), configurationFile);
//			subContexts.put(configurationFile, subContext);	
			//subContext.getContext().refresh();
			bf.setParentBeanFactory(parent);
			if(LOG.isDebugEnabled()) LOG.debug("Deployed SubContext [" + configurationFile + "]");
		} catch (Exception e) {
			LOG.error("Failed to deploy subcontext from configuration [" + configurationFile + "]", e);
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		System.out.println("\n\tAPP EVENT:" + event);
		
	}

}
