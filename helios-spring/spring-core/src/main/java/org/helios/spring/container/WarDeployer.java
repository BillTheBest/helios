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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.io.file.jurl.URLWebArchiveLink;
import org.helios.io.file.jurl.URLWebArchiveLink.WarDefinition;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.jmx.export.MBeanExporter;


/**
 * <p>Title: WarDeployer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.spring.container.WarDeployer</code></p>
 */

public class WarDeployer {
	/** The wars requesting deployment */
	protected final Set<URLWebArchiveLink> wars = new HashSet<URLWebArchiveLink>();
	/** Static logger */
	protected static final Logger LOG = Logger.getLogger(WarDeployer.class); 
	/** The Helios ApplicationContext */
	protected HeliosApplicationContext applicationContext;
	/** The JettyContext MBean */
	protected final ObjectName jettyContexts = JMXHelper.objectName("org.helios.jetty:service=JettyContexts");

	
	/**
	 * Creates a new WarDeployer
	 */
	public WarDeployer() {
	}
	
	/**
	 * Adds a new URLWebArchiveLink
	 * @param wurl the URLWebArchiveLink
	 */
	public void addURLWebArchiveLink(URLWebArchiveLink wurl) {
		if(wurl!=null) {
			wars.add(wurl);
		}
	}
	
	/**
	 * Returns the set of wars
	 * @return the set of wars
	 */
	public Set<URLWebArchiveLink> getURLWebArchiveLinks() {
		return Collections.unmodifiableSet(wars);
	}
	
	/**
	 * Creates a new WarDeployer
	 * @param wars The wars requesting deployment
	 * @param applicationContext e Helios ApplicationContext
	 */
	public WarDeployer(Set<URLWebArchiveLink> wars, HeliosApplicationContext applicationContext) {
		if(wars!=null) {
			this.wars.addAll(wars);
		}
		this.applicationContext = applicationContext;
	}
	
	public void run() {
		LOG.info("\n\t==============================\n\tStarting deployment of [" + wars.size() + "] Wars \n\t==============================\n");
		for(URLWebArchiveLink wurl: wars) {
			for(WarDefinition war: wurl.getWars()) {
				if(war.getBeanName()==null) continue;
				LOG.info("Deploying war [" + war.getBeanName() + "/" + war.getContextPath() + "]");
				String webServer = war.getWebServerBeanName();
				if(applicationContext.containsBean(webServer)) {
					Map<String, Object> props = new HashMap<String, Object>();
					props.put("contextPath", war.getContextPath());
					props.put("war", war.getResourcePath());
					props.put("server", new RuntimeBeanReference("HttpServer"));
					props.put("logUrlOnStart", "true");
					
					GenericBeanDefinition beanDef = new GenericBeanDefinition();
					beanDef.setBeanClassName("org.eclipse.jetty.webapp.WebAppContext");
					beanDef.setPropertyValues(new MutablePropertyValues(props));		
					beanDef.setInitMethodName("start");
					beanDef.setDestroyMethodName("stop");
					beanDef.setNonPublicAccessAllowed(true);
					
					HeliosApplicationSubContext appContext = new HeliosApplicationSubContext(false, war.getBeanName(), war.getBeanName(), applicationContext, beanDef, war.getBeanName());
					Object context = null;
					try {
						context = appContext.getBean(war.getBeanName());
						JMXHelper.getHeliosMBeanServer().invoke(jettyContexts, "addHandler", new Object[]{context}, new String[]{"org.eclipse.jetty.server.Handler"});
						((DefaultListableBeanFactory)applicationContext.getBeanFactory()).registerBeanDefinition(war.getBeanName(), beanDef);
					} catch (Exception e) {
						LOG.error("Failed to register WAR handler for [" + war.getBeanName() + "]", e);
					}
					appContext.refresh();
					if(context!=null) {
						MBeanExporter exporter = new MBeanExporter();
						exporter.setServer(JMXHelper.getHeliosMBeanServer());
						exporter.registerManagedResource(context, JMXHelper.objectName("org.helios.jetty:service=WebApp,name=" + war.getBeanName()));
					}
	
					
					
					LOG.info("Added deployment info for war [" + war.getBeanName() + "]");
				} else {
					LOG.warn("Skipping war deployment for [" + war.getBeanName() + "] as the defined web server bean is not present [" + webServer + "]");
				}
			}
		}
		
	}

	/**
	 * @return the applicationContext
	 */
	public HeliosApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * @param applicationContext the applicationContext to set
	 */
	public void setApplicationContext(HeliosApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	
}
