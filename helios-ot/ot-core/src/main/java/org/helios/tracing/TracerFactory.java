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
package org.helios.tracing;

import java.beans.PropertyEditorManager;
import java.io.BufferedInputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.BeanHelper;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.SystemEnvironmentHelper;
import org.helios.helpers.XMLHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.tracing.instrumentation.ObjectDeepMemorySizer;
import org.helios.tracing.stack.StackTracerInstanceFactory;
import org.helios.tracing.util.CheckBeforeSetAfter;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>Title: TracerFactory</p>
 * <p>Description: Bootstrap class for tracing engine and factory/access point for configured tracers.</p>
 * <p>The class looks for the URL to the XML configuration, first in the system property <code>CONF_PROPERTY</code> and then in the
 * environment variable <code>CONF_PROPERTY</code>.  
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1761 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/TracerFactory.java $
 * $Id: TracerFactory.java 1761 2010-06-06 20:40:58Z nwhitehead $
 */
@JMXManagedObject (declared=true, annotated=true)
public class TracerFactory extends ManagedObjectDynamicMBean {

	/** serial uid */
	private static final long serialVersionUID = 6724808097949971204L;
	/** The primary tracer */
	protected ITracer tracer = null; 
	/** The class logger */
	protected static final Logger log = Logger.getLogger(TracerFactory.class);
	/** A map of all created tracers keyed by tracer name */
	protected final Map<String, ITracer> tracerMap = new ConcurrentHashMap<String, ITracer>();
	/** The TracerFactory instance */
	protected static volatile TracerFactory tracerFactory = null;
	/** A lock to enforce the singleton */
	protected static final Object lock = new Object();
	/** The MBeanServer where the factory is registered */
	protected MBeanServer mbeanServer = null;
	/** The default jmx default domain is referenced */
	public static final String JMX_OBJECT_NAME_DEFAULT = "org.helios.tracing:service=TracerFactory";	
	/** The JMX ObjectName of the factory's registered mbean */
	protected static final ObjectName objectName = JMXHelper.objectName(JMX_OBJECT_NAME_DEFAULT);
	/** The JVM's instrumentation instance */
	protected Instrumentation instrumentation = null;
	/** The JVM's java agent arguments */
	protected String javaAgentArguments = null;
	
	/** The property name that resolves to a URL where the tracer factory configuration can be loaded from */
	public static final String CONF_PROPERTY = "helios.opentrace.config.url";
	/** The default name of the primary tracer */
	public static final String DEFAULT_TRACER_NAME = "DEFAULT";
	/** The property name where the jmx default domain is referenced */
	public static final String JMX_DOMAIN_PROPERTY = "helios.opentrace.config.jmx.domain";
	/** The default jmx default domain is referenced */
	public static final String JMX_DOMAIN_DEFAULT = "DefaultDomain";
	/** The property name where the jmx MBean ObjectName is referenced */
	public static final String JMX_OBJECT_NAME_PROPERTY = "helios.opentrace.config.jmx.objectname";
	
	
	/** The MBeanServer's default domnain */
	protected static final String defaultDomain = SystemEnvironmentHelper.getSystemPropertyThenEnv(JMX_DOMAIN_PROPERTY, JMX_DOMAIN_DEFAULT);
	
	
	/**
	 * 
	 */
	protected void initInstrumentation() {
		try {
			instrumentation = (Instrumentation)ManagementFactory.getPlatformMBeanServer().getAttribute(
						new ObjectName("org.helios.java.agent:service=JavaAgent"),
						"Instrumentation"
					);
		} catch (Throwable t) {}		
	}
	
	/**
	 * Creates a new TracerFactory
	 * @param server The defined MBeanServer.
	 * @param configUrlStr The tracer factory configuration file URL
	 */
	private TracerFactory(MBeanServer server, String configUrlStr) {
		mbeanServer = server;
		this.reflectObject(this);
		initInstrumentation();
		if(configUrlStr == null) {
			log.warn("\n\n\tFailed to locate Configuration in System Property or Environmental Variable [" + CONF_PROPERTY + "]\n\tTracerFactory is using default configuration.\n");
			return;
		}
		BufferedInputStream is = null;
		try {
			if(log.isDebugEnabled()) log.debug("TracerFactory reading configuration from URL:" + configUrlStr);
			URL configUrl = new URL(configUrlStr);
			is = new BufferedInputStream(configUrl.openStream());
			Node tracerConfiguration = XMLHelper.parseXML(is).getDocumentElement();			
			//buildTracer(tracerConfiguration);
			
		} catch (Exception e) {
			log.fatal("Unexpected error configuring TracerFactory", e);
			// ===================================
			// If the config fails, then
			// set the itracer to a Log4JTracer
			// ===================================
			//throw new RuntimeException("Unexpected error configuring TracerFactory", e);
		} finally {
			try { is.close(); } catch (Exception e) {}
		}		
	}
	
	/**
	 * Creates a new TracerFactory.
	 * When using this constructor, it is assumed the tracers will be configuired seperately.
	 * @param server The defined MBeanServer.
	 */
	private TracerFactory(MBeanServer server) {
		mbeanServer = server;
		this.reflectObject(this);		
		initInstrumentation();
	}	
	
	
	/**
	 * Acquires a reference to the JMX singleton tracer factory. 
	 * @return the JMX singleton tracer factory.
	 */
	@JMXAttribute (name="Instance", description="A direct reference to the tracer factory.", mutability=AttributeMutabilityOption.READ_ONLY)
	public static TracerFactory getInstance() {
		if(tracerFactory==null) {			
			synchronized(lock) {
				if(tracerFactory==null) {
					MBeanServer server = JMXHelper.getLocalMBeanServer(defaultDomain);
					if(server.isRegistered(objectName)) {
						tracerFactory = (TracerFactory) JMXHelper.getAttribute(objectName, server, "Reference");
					} else {
						tracerFactory = new TracerFactory(server, SystemEnvironmentHelper.getSystemPropertyThenEnv(CONF_PROPERTY, null));
						try {
							server.registerMBean(tracerFactory, objectName);
						} catch (Exception e) {
							log.fatal("Failed to register TracerFactory in JMX Agent", e);
							//throw new RuntimeException("Failed to register TracerFactory in JMX Agent", e);
						}					
					}
				}
			}
		}
		return tracerFactory;
	}
	
	public void registerTracer(String name, TracerImpl tracer) {
		if(name!=null && !tracerMap.containsValue(name) && tracer!=null) {
			tracerMap.put(name, tracer);
		}
	}
	
	/**
	 * Acquires a reference to the JMX singleton tracer factory.
	 * @param configUrl A url string referencing the configuration XML. 
	 * @return the JMX singleton tracer factory.
	 */
	//@JMXAttribute (name="Instance", description="A direct reference to the tracer factory.", mutability=AttributeMutabilityOption.READ_ONLY)
	public static TracerFactory getInstance(String configUrl) {
		if(tracerFactory==null) {			
			synchronized(lock) {
				if(tracerFactory==null) {
					MBeanServer server = JMXHelper.getLocalMBeanServer(defaultDomain);
					if(server.isRegistered(objectName)) {
						tracerFactory = (TracerFactory) JMXHelper.getAttribute(objectName, server, "Reference");
					} else {
						tracerFactory = new TracerFactory(server, configUrl);
						if(!server.isRegistered(objectName)) {
							try {
								server.registerMBean(tracerFactory, objectName);
							} catch (Exception e) {
								log.fatal("Failed to register TracerFactory in JMX Agent", e);
								//throw new RuntimeException("Failed to register TracerFactory in JMX Agent", e);
							}
						}
					}
				}
			}
		}
		return tracerFactory;
	}
	
	/**
	 * Acquires a reference to the JMX singleton tracer factory.
	 * This accessor will override any <code>ITracerInstanceFactory</code> stack configuration previously set up by other paramatered accessors.
	 * @param configUrl A url string referencing the configuration XML. 
	 * @return the JMX singleton tracer factory.
	 */
	@JMXAttribute (name="Instance", description="A direct reference to the tracer factory.", mutability=AttributeMutabilityOption.READ_ONLY)
	public static TracerFactory getInstance(Collection<ITracerInstanceFactory> iTracerInstanceFactories, Collection<CheckBeforeSetAfter> globalProperties) {
		if(tracerFactory==null) {			
			synchronized(lock) {
				if(tracerFactory==null) {
					MBeanServer server = JMXHelper.getLocalMBeanServer(defaultDomain);
					if(server.isRegistered(objectName)) {
						tracerFactory = (TracerFactory) JMXHelper.getAttribute(objectName, server, "Reference");
					} else {
						tracerFactory = new TracerFactory(server);
						// process globalProperties											
						tracerFactory.setGlobalProperties(globalProperties);
						// process tracerInstanceFactories
						tracerFactory.setTracerInstanceFactories(iTracerInstanceFactories);						
						try {
							server.registerMBean(tracerFactory, objectName);
						} catch (Exception e) {
							log.fatal("Failed to register TracerFactory in JMX Agent", e);
							//throw new RuntimeException("Failed to register TracerFactory in JMX Agent", e);
						}					
					}
				}
			}
		}
		synchronized(lock) {
			tracerFactory.setTracerInstanceFactories(iTracerInstanceFactories);
		}
		return tracerFactory;
	}	
	
	/**
	 * Initializes the tracer (stack or single).
	 * This
	 * @param iTracerInstanceFactories
	 */
	private void setTracerInstanceFactories(Collection<ITracerInstanceFactory> iTracerInstanceFactories) {
		if(iTracerInstanceFactories==null || iTracerInstanceFactories.size() < 1) return;
		try {
			if(iTracerInstanceFactories.size()==1) {
				tracer = iTracerInstanceFactories.iterator().next().getTracer();
				tracerMap.put(tracer.getTracerName(), tracer);
			} else {
				// iterate through factories. see if one of them is stack.
				// if yes, use it and add the other tracers to it.
				// if not, create a new one and add the other tracers to it.
				// if there are multiple stack tracer factories, the first one will be used and the rest will be ignored.
				for(ITracerInstanceFactory tif: iTracerInstanceFactories) {
					if(tif instanceof StackTracerInstanceFactory) {
						continue;
					} else {
						try {
							ITracer itracer = tif.getTracer();
							
							tracerMap.put(itracer.getTracerName(), itracer);
						} catch (TracerInstanceFactoryException e) {
							log.warn("Failed to add tracer to stack tracer from ITracerInstanceFactory[" + tif.getClass().getName() + "]", e);
						}
					}
				}
				
			}
		} catch (Exception e) {
			log.fatal("Failed to TracerInstanceFactories. Tracer will default.", e);
			tracerMap.clear();
			tracer = null;
		}
			
	}
	
	/**
	 * Iterates through constructed <code>CheckBeforeSetAfter</code>s and fires them if they have not been fired.
	 * @param globalProperties
	 */
	private void setGlobalProperties(Collection<CheckBeforeSetAfter> globalProperties) {
		for(CheckBeforeSetAfter cbsa: globalProperties) {
			if(!cbsa.isFired()) cbsa.fire();
		}
	}
	
	
	
	
	
	/**
	 * Returns a direct reference to this tracer factory.
	 * @return this tracer factory.
	 */
	@JMXAttribute(description="The direct reference to the TracerFactory", name="Reference", mutability=AttributeMutabilityOption.READ_ONLY)
	public TracerFactory getReference() {
		return tracerFactory;
	}
	
	
	
//	/**
//	 * Creates a new TracerImpl.
//	 * @param tracerFactoryNode The tracer XML configuration node.
//	 * @throws TracerInstanceFactoryException
//	 */
//	public void buildTracer(Node tracerFactoryNode) throws TracerInstanceFactoryException {
//		try {
//			Collection<CheckBeforeSetAfter> globalProperties = processProperty(tracerFactoryNode);
//			setGlobalProperties(globalProperties);
//			log.info("TracerFactory:Added [" + globalProperties.size() + "] Global Properties");
//			for(Node node: XMLHelper.getChildNodesByName(tracerFactoryNode, "editor", false)) {
//				processEditor(node);
//			}
//			Collection<ITracerInstanceFactory> iTracerInstanceFactories = new HashSet<ITracerInstanceFactory>();
//			for(Node node: XMLHelper.getChildNodesByName(tracerFactoryNode, "tracer", false)) {
//				iTracerInstanceFactories.add(AbstractTracerInstanceFactory.getInstance(node));
//			}
//			setTracerInstanceFactories(iTracerInstanceFactories);
//			log.info("TracerFactory:Added [" + iTracerInstanceFactories.size() + "] TracerInstanceFactories");
//		} catch (Exception e) {
//			log.error("Failed to create TracerImpl", e);
//			throw new TracerInstanceFactoryException("Failed to create TracerImpl", e);
//		}
//	}
//	
	/**
	 * Returns the names of all the registered tracers.
	 * @return A string array of all registered tracers.
	 */
	@JMXAttribute (name="TracerNames", description="An array of all the registered tracer names.", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getTracerNames() {
		return tracerMap.keySet().toArray(new String[0]);
	}
	
	
	
	/**
	 * Class loads and registers the named property editor.
	 * @param editorNode The editor node.
	 */
	@SuppressWarnings("unchecked")
	protected static void processEditor(Node editorNode) {
		String name = XMLHelper.getAttributeValueByName(editorNode, "name");
		String type = XMLHelper.getAttributeValueByName(editorNode, "type");
		try {
			Class ctype = Class.forName(type);
			Class ntype = Class.forName(name);
			PropertyEditorManager.registerEditor(ctype,ntype);
			if(log.isDebugEnabled()) log.debug("Registered Property Editor [" + ntype.getName() + "] for type [" + ctype.getName() + "]");
		} catch (Exception e) {
			log.warn("Failed to process editor [" + name + "].");
		}
	}
	
	/**
	 * Processes an attribute node from the xml config. The extracted attribute is applied to the tracer instance factory.
	 * @param attributeNode The attribute node.
	 * @param factory The factory to apply the attribute to.
	 */
	protected static void processAttribute(Node attributeNode, ITracerInstanceFactory factory) {
		String name = XMLHelper.getAttributeValueByName(attributeNode, "name");
		String value = XMLHelper.getAttributeValueByName(attributeNode, "value");
		if(name==null) {
			throw new RuntimeException("Configuration Attribute had null name"); 
		}
		if(value==null) {
			StringBuilder buff = new StringBuilder();
			NodeList subNodes = attributeNode.getChildNodes();
			for(int i = 0; i < subNodes.getLength(); i++) {
				Node node = subNodes.item(i);
				if(node.getNodeType()==Node.ELEMENT_NODE) {
					buff.append(XMLHelper.getStringFromNode(node));
				}
			}
			
			value = buff.toString();
			if(value==null) {
				throw new RuntimeException("Configuration Attribute [" + name + "] had null value");
			}			
		}
		if(value != null && value.startsWith("${") && value.endsWith("}")) {
			value = value.substring(1,value.length()-1);
			value = System.getProperty(value, "");
		}
		BeanHelper.setAttribute(name, value, factory);
		log.info("ITracerInstanceFactory Attribute Set:[" + name + "]");		
	}
	
	/**
	 * Processes a property set with checkBefore and setAfter processing.
	 * @param propertyNode The configuration node.
	 */
	protected static Collection<CheckBeforeSetAfter> processProperty(Node tracerFactoryNode) {
		Collection<CheckBeforeSetAfter> globalProperties = new HashSet<CheckBeforeSetAfter>();
		for(Node node: XMLHelper.getChildNodesByName(tracerFactoryNode, "property", false)) {
			globalProperties.add(new CheckBeforeSetAfter(node));
		}		
		return globalProperties;
	}
	
	/**
	 * Determines the deep size of the passed object.
	 * @param obj The object to size.
	 * @return the deep size of the object or zero if the object or the instrumentation is null.
	 */
	@JMXOperation (name="deepSize", description="Calulates the deep size of the passed object.")
	public long deepSize(
			@JMXParameter(name="Object", description="The object to calculate the deep size of.") Object obj) {
		if(obj==null) return 0;
		if(instrumentation==null) {
			log.warn("Attempt to call deepSize failed because javaAgent was not initialized");
			return 0;
		}
		try {
			return ObjectDeepMemorySizer.getInstance(instrumentation).deepSizeOf(obj);
		} catch (Throwable t) {
			log.error("Attempt to call deepSize unexpectedly failed", t);
			return 0;			
		}
	}

	/**
	 * Returns the default tracer.
	 * @return The default tracer.
	 */
	@JMXAttribute (name="Tracer", description="Acquires the default tracer.", mutability=AttributeMutabilityOption.READ_ONLY)
	public ITracer getTracer() {
		return tracer;
	}
	
	/**
	 * Returns the named tracer.
	 * @param tracerName The name of the tracer.
	 * @return The requsted tracer, or null if the name was not found.
	 */
	@JMXOperation (name="getTracer", description="Acquires the named tracer.")
	public ITracer getTracer(
			@JMXParameter(name="TracerName", description="The name of the tracer to acquire.") String tracerName) {
		return tracerMap.get(tracerName);
	}

	/**
	 * Returns the JVM's instrumentation instance.
	 * @return the instrumentation. Will be null if the java agent was not activated.
	 */
	public Instrumentation getInstrumentation() {
		return instrumentation;
	}

	/**
	 * Initializes the TracerFactory with a reference to the JVM's instrumentation instance. 
	 * @param instrumentation the instrumentation to set
	 */
	public void setInstrumentation(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
	}

	/**
	 * The javaAgent's arguments.
	 * @return the javaAgentArguments
	 */
	@JMXAttribute (name="JavaAgentArguments", description="The arguments to the JavaAgent", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getJavaAgentArguments() {
		return javaAgentArguments;
	}

	/**
	 * Initializes the TracerFactory with the javaAgent's arguments.
	 * @param javaAgentArguments the javaAgentArguments to set
	 */
	public void setJavaAgentArguments(String javaAgentArguments) {
		this.javaAgentArguments = javaAgentArguments;
	}

	/**
	 * The default JMX Agent where management interfaces should be registered.
	 * @return an MBeanServer
	 */
	@JMXAttribute (name="DefaultMBeanServer", description="The default JMX Agent where management interfaces should be registered.", mutability=AttributeMutabilityOption.READ_ONLY)
	public MBeanServer getDefaultMBeanServer() {
		return mbeanServer;
	}
	

	
}

