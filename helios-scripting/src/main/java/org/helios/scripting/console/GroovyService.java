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
package org.helios.scripting.console;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;

import java.io.InputStream;
import java.lang.reflect.Constructor;

import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.StreamHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.scripting.manager.ConcurrentBindings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
/**
 * <p>Title: GroovyService</p>
 * <p>Description: Bootstrap to launch a groovy console inside the Helios JVM.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.scripting.console.GroovyService</code></p>
 */
@JMXManagedObject(annotated=true, declared=true, objectName=GroovyService.OBJECT_NAME_STR )
public class GroovyService extends ManagedObjectDynamicMBean implements ApplicationContextAware, InitializingBean, ApplicationListener<ContextRefreshedEvent> { 
	/**  */
	private static final long serialVersionUID = -2758890352866603234L;
	/** The default JMX ObjectName for this MBean */
	public static final String OBJECT_NAME_STR = "org.helios.scripting:service=GroovyService";
	/** The JMX ObjectName for the GroovyService service */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName(OBJECT_NAME_STR);
	/** Static logger */
	protected Logger LOG = Logger.getLogger(GroovyService.class);
	/** The injected application context */
	protected ApplicationContext applicationContext = null;
	/** The useful bindings init script */
	protected String initScript = null;
	/** The container for the useful bindings */
	protected final ConcurrentBindings bindings = new ConcurrentBindings();
	/** The console class */
	protected Class<?> consoleClass = null;
	/** The console class ctor */
	protected Constructor<?> consoleCtor = null;
	/** The groovy classloader */
	protected GroovyClassLoader gcl = null;
	

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Creates a new GroovyService and registers the MBean interface 
	 */
	public GroovyService() {
		
	}
	
	/**
	 * Launches an interactive GroovyService UI
	 */
	@JMXOperation(name="launchConsole", description="Launches an interactive GroovyService UI")
	public void launchConsole() {
		try {
			Object console = consoleCtor.newInstance(gcl, bindings.getGroovyBinding());
			consoleClass.getDeclaredMethod("run").invoke(console);
		} catch (Exception e) {
			LOG.error("Failed to launch console", e);
			throw new RuntimeException("Failed to launch console", e);
		}
		
	}
	
	/**
	 * Returns the initialization script
	 * @return the initialization script
	 */
	@JMXAttribute(name="InitScript", description="The useful bindings initialization script", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getInitScript() {
		return initScript;
	}
	
	/**
	 * Returns a view of the bindings
	 * @return a view of the bindings
	 */
	@JMXAttribute(name="Bindings", description="The useful bindings", mutability=AttributeMutabilityOption.READ_ONLY)
	public TabularData getBindings() {
		return bindings.getTabularData();
	}

	/**
	 * Sets the initialization script
	 * @param initScript the initialization script
	 */
	public void setInitScript(String initScript) {
		this.initScript = initScript;
	}

	/**
	 * {@inheritDoc}
	 * <p>Initializes the useful bindings</p>
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		LOG.info("\n\t==============================\n\tStarting GroovyService\n\t==============================\n");
		InputStream is = null;
		try {
			// ==================================
			// Prepare Console Class
			// ==================================
			is = getClass().getClassLoader().getResourceAsStream("scripts/groovy/Console.groovy");
			byte[] consoleBytes = StreamHelper.readByteArrayFromStream(is);
			gcl = new GroovyClassLoader(getClass().getClassLoader());
			gcl.parseClass(new String(consoleBytes));
			consoleClass =  Class.forName("scripts.groovy.Console", true, gcl);
			consoleCtor = consoleClass.getDeclaredConstructor(ClassLoader.class, Binding.class);
			// ==================================
			// Load useful bindings
			// ==================================
			bindings.put("AppCtx", applicationContext);
			bindings.put("jmxHelper", JMXHelper.class);
			bindings.put("mserver", JMXHelper.getHeliosMBeanServer());			
			bindings.put("gbindings", bindings);
			bindings.put("log", LOG);
			LOG.info("\n\t==============================\n\tStarted GroovyService\n\t==============================\n");
		} catch (Exception e) {
			LOG.error("Failed to start GroovyService", e);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception e) {}
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(event!=null) {
			LOG.info("\n\t==============================\n\tProcessing GroovyService Init Bindings\n\t==============================\n");
			try {
				// ==================================
				// Process useful bindings init script
				// ==================================
				if(initScript!=null && !initScript.trim().isEmpty()) {
					Binding binding = bindings.getGroovyBinding();
					binding.setProperty("AppCtx", event.getApplicationContext());
					binding.setProperty("gbindings", bindings);
					GroovyShell shell = new GroovyShell(binding);
					shell.evaluate(initScript);
				}
			} catch (Exception e) {
				LOG.error("Failed to process bindings init script.", e);
			}
		}
	}
	
}
