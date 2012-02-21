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

import java.beans.PropertyEditorManager;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.helios.editors.Log4JLevelEditor;
import org.helios.editors.URLPropertyEditor;
import org.helios.editors.XMLNodeEditor;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.JMXHelperExtended;
import org.helios.io.file.RecursiveDirectorySearch;
import org.helios.io.file.filters.ConfigurableFileExtensionFilter;
import org.helios.io.file.jurl.URLArchiveLink;
import org.helios.io.file.jurl.URLWebArchiveLink;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.propertyeditors.ObjectNamePropertyEditor;
import org.helios.spring.container.jmx.ApplicationContextService;
import org.helios.version.VersionHelper;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;


/**
 * <p>Title: HeliosContainerMain</p>
 * <p>Description: BeanRegistry exposing bootstrap container.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.spring.container.HeliosContainerMain</code></p>
 */
public class HeliosContainerMain implements ApplicationListener, PropertyEditorRegistrar {
	/** The version for this component */
	public static final String VERSION = "HeliosSpring " + VersionHelper.getHeliosVersion(HeliosContainerMain.class);
	/** static class logger */
	protected static Logger LOG = Logger.getLogger(HeliosContainerMain.class);
	/** the main Spring app context */
	protected HeliosApplicationContext applicationContext = null;
	/** the main Spring app context */
	protected Map<String, GenericApplicationContext> childContexts = new ConcurrentHashMap<String, GenericApplicationContext>();
	/** template repository */
	protected XmlBeanFactory templateRepo = null;
	/** the XML bean definition loader */
	protected XmlBeanDefinitionReader beanDefinitionReader = null;
	/** template app context */
	protected GenericApplicationContext templateFactory = null; 
	/** template XML bean definition loader */
	protected XmlBeanDefinitionReader templateDefinitionReader = null;
	/** PropertyEditor Manager */
	protected CustomEditorConfigurer editorManager = new CustomEditorConfigurer();
	/** The container's classpath loader */
	protected static URLClassLoader containerClassLoader = null;
	/** The file extension for static bean deployment files */
	protected static final String HELIOS_XML_STATIC = ".helios.xml";
	/** The file extension for template deployment files */
	protected static final String HELIOS_XML_TEMPLATE = ".helios.t.xml";
	/** The file extension for dynamic deployment files */
	protected static final String HELIOS_XML_DYNAMIC = ".helios.hot.xml";
	/** The file extension for dynamic templatized deployment files */
	protected static final String HELIOS_XML_DYNAMIC_TEMPLATIZED = ".helios.tmp.xml";
	/** The commons logging adapter property name */
	protected static final String COMMONS_LOGGING_PROP = "org.apache.commons.logging.Log";
	/** Default log4j commons logging adapter */
	protected static final String COMMONS_LOGGING_DEFAULT_ADAPTER = "org.apache.commons.logging.impl.Log4JLogger";
	/** Default conf directory */
	protected static final String[] DEFAULT_CONF_DIRS = {"./conf"};
	/** Default lib directory */
	protected static final String[] DEFAULT_LIB_DIRS = {"./lib", "./libs"};
	/** Default cp resource directory */
	protected static final String[] DEFAULT_CP_DIRS = {DEFAULT_CONF_DIRS[0] + "/resource", "./classes"};	
	/** Default log4j config file */
	protected static final String LOG4J_DEFAULT_CONFIG = DEFAULT_CONF_DIRS[0] + "/log4j/log4j.xml";
    /** library scan extensions */
    protected static final String[] LIB_EXTENSIONS = new String[] {".jar", ".zip", ".jurl", ".wurl"};

	protected DynamicDeploymentManager ddm = null;
	
	/** A set of discovered wars to deploy */
	protected static final WarDeployer warDeployer = new WarDeployer();
	
	public static final String CONF_ARG = "-conf";
	public static final String LIB_ARG = "-lib";
	public static final String SCP_ARG = "-cpd";
	public static final String XBC_ARG = "-xbc";  
	public static final String ECLIPSE_LAUNCHER = "-el";
	
	public static final String ISO_CP_ARG = "-isolate";
	public static final String DAEMON_ARG = "-daemon";
	
	private static boolean isolated = false;
	private static boolean daemon= false;
	
	public HeliosContainerMain() {
		
	}
	
	/**
	 * Sets Log4j as the commons logging adapter if one is not set already.
	 */
	protected static void defaultLogging() {
		String adapter = System.getProperty(COMMONS_LOGGING_PROP);
		if(adapter==null) {
			System.setProperty(COMMONS_LOGGING_PROP, COMMONS_LOGGING_DEFAULT_ADAPTER);
		}
		BasicConfigurator.configure();
	}
	
	/**
	 * Attempts to configure Log4j using the default config file.
	 */
	protected static void defaultLog4jConfig() {
		File f = new File(LOG4J_DEFAULT_CONFIG);
		if(f.canRead()) {
			DOMConfigurator.configureAndWatch(LOG4J_DEFAULT_CONFIG);
		} else {
			BasicConfigurator.configure();
		}
	}
	
	/**
	 * Attempts to configure Log4j using the specified config file.
	 */
	protected static void log4jConfig(File f) {
		DOMConfigurator.configureAndWatch(f.toString());
		LOG.info("Configured logging from specified file [" + f.toString() + "]");
	}
	
	
	/**
	 * Configures the default conf, lib and cp directories.
	 * @param confDirs
	 * @param libDirs
	 * @param cpDirs
	 */
	protected static void defaultConfigs(Set<String> confDirs, Set<String> libDirs, Set<String> cpDirs) {
		for(String s: DEFAULT_CONF_DIRS) {
			File f = new File(s);
			if(f.exists() && f.isDirectory()) {
				confDirs.add(s);
			}
		}
		for(String s: DEFAULT_CP_DIRS) {
			File f = new File(s);
			if(f.exists() && f.isDirectory()) {
				cpDirs.add(s);
			}
		}
		for(String s: DEFAULT_LIB_DIRS) {
			File f = new File(s);
			if(f.exists() && f.isDirectory()) {
				libDirs.add(s);
			}
		}
	}
	
	/**
	 * @param args
	 * @param confDirs
	 * @param libDirs
	 * @param cpDirs
	 * @param eclipseDirs
	 */
	protected static void processCommandLineArgs(String[] args, Set<String> confDirs, Set<String> libDirs, Set<String> cpDirs, Set<URL> eclipseDirs) {
		for(int i = 0; i < args.length; i++) {
			if(CONF_ARG.equalsIgnoreCase(args[i]) && args.length >= (i+2)) {
				confDirs.add(args[i+1]);
				i++;
			} else if(LIB_ARG.equalsIgnoreCase(args[i]) && args.length >= (i+2)) {
				libDirs.add(args[i+1]);
				i++;						
			} else if(SCP_ARG .equalsIgnoreCase(args[i]) && args.length >= (i+2)) {
				cpDirs.add(args[i+1]);
				i++;						
			} else if(ISO_CP_ARG.equalsIgnoreCase(args[i])) {
				isolated=true;
			} else if(DAEMON_ARG.equalsIgnoreCase(args[i])) {
				daemon=true;
			} else if(ECLIPSE_LAUNCHER.equalsIgnoreCase(args[i])) {
				i++;
				EclipseLauncher el = new EclipseLauncher(new File(args[i]));
				eclipseDirs.addAll(el.getClasspathEntries());
			} 
		}		
	}
	

	
	/**
	 * @param libDirs
	 * @param cpDirs
	 * @param eclipseDirs
	 * @return
	 */
	protected static Map<Integer, URL> processClassPathEntries(Set<String> libDirs, Set<String> cpDirs, Set<URL> eclipseDirs) {
		Map<Integer, URL> classPath = new TreeMap<Integer, URL>();		
		int urlCounter = 0;
		if(cpDirs.size()>0) {
			for(String cpDir: cpDirs) {
				File dir = new File(cpDir);
				if(dir.isDirectory()) {
					try {
						classPath.put(urlCounter, dir.getAbsoluteFile().toURI().toURL());
						urlCounter++;
					} catch (Exception e) {}
				}
			}
		}
		if(libDirs.size()>0) {
			for(String dir: libDirs) {
				File dirFile = new File(dir);
				LOG.info("Scanning lib directory [" + dirFile.getAbsolutePath() + "]");
				String[] jarFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(false, LIB_EXTENSIONS), dirFile.getAbsolutePath());
				for(String jar: jarFiles) {
					if(jar.toLowerCase().endsWith(".jurl")) {
						try {
							URLArchiveLink ual = new URLArchiveLink(jar);
							for(URL url: ual.getURLArray()) {
								classPath.put(urlCounter, url);
								urlCounter++;
							}							
						} catch (Exception e) {
							LOG.warn("Failed to process URLArchiveLink [" + jar + "]:" + e);
						}
					} else if(jar.toLowerCase().endsWith(".wurl")) {
						try {
							warDeployer.addURLWebArchiveLink(new URLWebArchiveLink(jar));
						} catch (Exception e) {
							LOG.warn("Failed to validate WURL [" + jar + "]", e);
						}
					} else {
						File f = new File(jar);
						if(f.exists()) {
							try {
								classPath.put(urlCounter, f.getAbsoluteFile().toURI().toURL());
								urlCounter++;
							} catch (Exception e) {}
						}
					}
				}
			}					
		}
		for(URL url: eclipseDirs) {
			if(url!=null) {
				classPath.put(urlCounter, url);
				urlCounter++;
			}
		}
		return classPath;
	}

	/**
	 * Main entry point to bootstrap Spring and configuration.
	 * @param args All args are names of directories that will be recursively searched for XML files to load into Spring.
	 * TODO: Handle all these iffy options with args4j
	 */
	public static void main(String[] args) {
		if(args.length > 0 && args[0].equalsIgnoreCase("-help")) {
			banner();
			System.exit(0);
		}
		File log4jFile = null;
		boolean loggingConfigSpecified = false;
		boolean supressDefaults = false;
		for(int i = 0; i < args.length; i++) {
			if("-log4j".equalsIgnoreCase(args[i])) {
				if(args.length < (i+2)) {
					System.err.println("Could not process -log4j option. Insufficient arguments.");
					banner();
					System.exit(0);
				} else {
					log4jFile = new File(args[i+1]);
					if(!log4jFile.canRead()) {
						System.err.println("Could not read -log4j specified file [" + log4jFile.toString() + "]");
						System.exit(0);
					} else {
						loggingConfigSpecified = true;
					}
					break;
				}
			}
		}
		
		
		if(args.length > 0) {
			for(int i = 0; i < args.length; i++) {
				if("-sd".equalsIgnoreCase(args[i])) {
					supressDefaults=true;
					break;
				}
			}			
		}
		defaultLogging();
		LOG.info("Starting " + VERSION);
		if(supressDefaults) {
			if(loggingConfigSpecified) {
				log4jConfig(log4jFile);
			}
			LOG.info("Supressed Default Configuration");
		} else {
			LOG.info("Processing Default Configurations");
			if(loggingConfigSpecified) {
				LOG.info("Configuring logging from [" + log4jFile.toString() + "]");
				log4jConfig(log4jFile);
			} else {
				LOG.info("Configuring default logging");
				defaultLog4jConfig();
			}
		}
		
		
		Set<String> confDirs = new HashSet<String>();
		Set<String> libDirs = new HashSet<String>();
		Set<String> cpDirs = new HashSet<String>();
		Set<URL> eclipseDirs = new HashSet<URL>();
		if(!supressDefaults) {
			defaultConfigs(confDirs, libDirs, cpDirs);
		}
		processCommandLineArgs(args, confDirs, libDirs, cpDirs, eclipseDirs);
		Map<Integer, URL> addToClassPath = processClassPathEntries(libDirs, cpDirs, eclipseDirs);
		ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader containerClassLoader = null;
		try {
			if(isolated) {
				containerClassLoader  = new URLClassLoader(addToClassPath.values().toArray(new URL[addToClassPath.size()]), ClassLoader.getSystemClassLoader());
				Thread.currentThread().setContextClassLoader(containerClassLoader);
				LOG.info("Using isolated class loader [" + containerClassLoader + "]");			
				
			} else {
				StringBuilder bClassPath = new StringBuilder(System.getProperty("java.class.path"));
				for(URL url: addToClassPath.values()) {
					try {
						ClassPathHacker.addURL(url);
						bClassPath.append(File.pathSeparatorChar).append(url.toURI().getSchemeSpecificPart().replace("//", ""));
					} catch (Exception e) {
						LOG.error("Faled to add Class Path URL [" + url + "]", e);
					}
				}
				System.setProperty("java.class.path", bClassPath.toString());
			}
			LOG.info("Added [" + addToClassPath.size() + "] Jar files and directories to the classpath");
			if(LOG.isDebugEnabled()) {
				StringBuilder b = new StringBuilder("Added following jar files to classpath:");
				for(URL url: addToClassPath.values()) {
					b.append("\n\t").append(url);
				}
				b.append("\n");
				LOG.debug(b);
			}
			String[] bootStrapArgs = confDirs.toArray(new String[confDirs.size()]);
			HeliosContainerMain hsc = new HeliosContainerMain();
			hsc.bootStrap(bootStrapArgs);
			// Set<URLWebArchiveLink> wars
		} finally {
			if(isolated) {
				Thread.currentThread().setContextClassLoader(defaultClassLoader);
			}
		}
		if(daemon) {
			Thread t = new ContainerNonDaemonThread();
			t.start();
		}
	}		
		
		
		
			
	protected static void banner() {
		StringBuilder b = new StringBuilder(VERSION);
		b.append("\n\tUsage: java org.helios.spring.container.HeliosContainerMain [-help]: Prints this banner.");
		b.append("\n\tUsage: java org.helios.spring.container.HeliosContainerMain [-conf <configuration directory>] [-lib <jar directory>] [-cpd <directory>] [-isolate]");
		//b.append("\n\tUsage: java org.helios.spring.container.HeliosContainerMain [-multi <multi container configuration file>]");
		b.append("\n\t-conf and -lib can be repeated more than once.");
		b.append("\n\t-lib will recursively search the passed directory and add any located jar files to the container's classpath.");
		b.append("\n\t-cpd will add the passed directory to the container's classpath.");
		b.append("\n\t-isolate configures the container classpath in a seperate class loader. By default, -cpd and -lib will append to the classpath.");
		b.append("\n\t-daemon keeps the container JVM alive even in the absence of any non-daemon threads. ");
		b.append("\n\t" + ECLIPSE_LAUNCHER + " extracts classpath entries from an Eclipse Run Configuration File");
		
		b.append("\n\t-sd supresses default configurations for lib, cpd and conf. ");
		b.append("\n\t-log4j <log4j xml config file> Configures logging from the specified file.");
		//b.append("\n\t-multi  will load the specified multi-container configuration file which specifies groups of -libs and -confs which will be loaded in seperate containers with classloader isolation.");
		b.append("\n\tDefault Settings (use -sd to supress) \n\t================");
		b.append("\n\tConf:\t").append(Arrays.toString(DEFAULT_CONF_DIRS));
		b.append("\n\tCP:\t").append(Arrays.toString(DEFAULT_CP_DIRS));
		b.append("\n\tLib:\t").append(Arrays.toString(DEFAULT_LIB_DIRS));
		b.append("\n\tLog4j:\t").append(LOG4J_DEFAULT_CONFIG);
		b.append("\n\n");
		System.out.println(b);
	}
	
	/**
	 * <multi>
	 * 		<container name="">
	 * 			<lib dir="dir#1"/>
	 * 			<lib dir="dir#2"/>
	 * 			<lib dir="dir#n"/>
	 * 			<conf dir="dir#1"/>
	 * 			<conf dir="dir#2"/>
	 * 			<conf dir="dir#n"/>
	 * 		</container>
	 * </multi>
	 * 
	 * Shared Lib Dirs  (shared class loader)
	 * Wild Card Provider Lookups
	 * Template Store Service (file system, DB etc.)
	 */
	
	/**
	 * Boostraps this container.
	 * @param configDirectories names of directories that will be recursively searched for XML files to load into Spring.
	 */
	public HeliosApplicationContext bootStrap(String...configDirectories) {
		LOG.info("Conf Dirs:" + configDirectories.length);
		for(String s: configDirectories) {
			LOG.info("Conf Dirs:" + s);
		}
		
		PropertyEditorRegistrySupport registry = new PropertyEditorRegistrySupport();
		this.registerCustomEditors(registry);

		
		String[] configFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(HELIOS_XML_STATIC), configDirectories);
		for(int i = 0; i < configFiles.length; i++) {
			try {
				File f = new File(configFiles[i]);
				configFiles[i] = f.toURI().toURL().toString();
			} catch (Exception e) {
				LOG.error("Failed to conver file to URL:[" + configFiles[i] + "]", e);
			}
		}
		String[] tConfigFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(HELIOS_XML_TEMPLATE), configDirectories);
		String[] dConfigFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(HELIOS_XML_DYNAMIC), configDirectories);
		String[] tmpConfigFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(HELIOS_XML_DYNAMIC_TEMPLATIZED), configDirectories);
		LOG.info("Located [" + configFiles.length + "] static configuration files.");
		LOG.info("Located [" + tConfigFiles.length + "] template configuration files.");
		LOG.info("Located [" + dConfigFiles.length + "] dynamic configuration files.");
		LOG.info("Located [" + tmpConfigFiles.length + "] dynamic templatized configuration files.");
		LOG.info("Located [" + warDeployer.getURLWebArchiveLinks().size() + "] target war deployments");
		if(LOG.isDebugEnabled()) {
			StringBuilder b = new StringBuilder("\nLocated Configuration Files:\n============================");
			for(String s: configFiles) {
				b.append("\n\t").append(s);
			}
			b.append("\n");
			b.append("\nLocated Template Configuration Files:\n============================");
			for(String s: tConfigFiles) {
				b.append("\n\t").append(s);
			}
			b.append("\n");
			b.append("\nLocated Dynamic Configuration Files:\n============================");
			for(String s: dConfigFiles) {
				b.append("\n\t").append(s);
			}
			b.append("\n");
			b.append("\nCleaning Located Dynamic Templatized Configuration Files:\n============================");
			for(String s: tmpConfigFiles) {
				File f = new File(s);
				b.append("\n\tDeleted:").append(s).append(":").append(f.delete());
			}
			b.append("\n");
			
			LOG.debug(b.toString());
		}
		for(String s: tmpConfigFiles) {
			File f = new File(s);
		}
		
		applicationContext = new HeliosApplicationContext(configFiles, false);
		ApplicationContextService acs = new ApplicationContextService(applicationContext);
		try {
			JMXHelperExtended.getHeliosMBeanServer().registerMBean(acs, ApplicationContextService.OBJECT_NAME);
		} catch (Exception e) {
			LOG.warn("Failed to register HeliosApplicationContext management interface [" + e.toString() + "]. Continuing without.");
		}
				
//		for(Object o: templateFactory.getBeansOfType(DynamicBeanFactory.class).values()) {
//			((DynamicBeanFactory)o).setApplicationContext(applicationContext);
//			((DynamicBeanFactory)o).startX();
//		}

//		beanDefinitionReader = new XmlBeanDefinitionReader(applicationContext);
//		
//		for(String s: configFiles) {
//			beanDefinitionReader.loadBeanDefinitions(new FileSystemResource(s));
//		}
		
//		applicationContext.setParent(templateFactory);
		
		try {
			applicationContext.refresh();
		} catch (Exception e) {
			LOG.error("Failed to refresh app context", e);
			System.exit(0);
		}
		applicationContext.registerShutdownHook();
		warDeployer.setApplicationContext(applicationContext);
		warDeployer.run();
		
		StringBuilder b = new StringBuilder("\nStatic Deployed Bean List:\n============================");
		String[] beans = applicationContext.getQualifiedBeanDefinitionNames();
		for(String s: beans) {
			b.append("\n\t").append(s);
			try {
				ObjectName on = null; //inspectForJMXRegister(s);
				if(on!=null) {
					b.append("\t[JMX:").append(on.toString() + "]");
				}
			} catch (BeanIsAbstractException bia) {
			} catch (Exception e) {
				LOG.warn("Failed to register MODB [" + s + "] with Agent", e);
			}
		}
		b.append("\n============================\n");
		b.append(applicationContext.toString()).append("\n");
		LOG.info(b.toString());		
		
		// Deploy dynamics
		ddm = new DynamicDeploymentManager(configDirectories, applicationContext);
		b.setLength(0);
		b.append("\nDynamic Deployed Bean List:\n============================");		
		for(Map.Entry<String, DynamicConfiguration> dc: ddm.getConfigurations()) {
			b.append("\n\t").append(dc.getKey());
			for(Map.Entry<String, Object> managedBean: dc.getValue().getManagedBeans()) {
				b.append("\n\t\t").append(managedBean.getKey()).append(" [").append(managedBean.getValue().getClass().getName()).append("]");
			}			
		}
		b.append("\n============================\n");
		LOG.info(b.toString());
		// Load Templates
//		templateFactory = new GenericApplicationContext();
//		templateDefinitionReader = new XmlBeanDefinitionReader(templateFactory);
//		for(String s: tConfigFiles) {
//			templateDefinitionReader.loadBeanDefinitions(new FileSystemResource(s));
//		}
//		templateFactory.refresh();
//		templateFactory.registerShutdownHook();
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		long startupTime = TimeUnit.SECONDS.convert((System.currentTimeMillis()- runtimeMXBean.getStartTime()), TimeUnit.MILLISECONDS);
		String[] beanDefNames = JMXHelper.getRuntimeHeliosMBeanServer().getAttribute(JMXHelper.objectName("org.helios.spring:service=HeliosApplicationContext") , "BeanDefinitionNames", String[].class);		
		LOG.info(Banner.banner("*", 3, 10, new String[]{
				"Helios Spring Container Boot Complete",
				"Version:" + this.getClass().getPackage().getImplementationVersion(),
				"Spring Version:" + ApplicationContext.class.getPackage().getImplementationVersion(),
				"Deployed Bean Count:" + beanDefNames.length,
				"JVM:" + runtimeMXBean.getVmVendor() + " " + runtimeMXBean.getVmName() + " " + runtimeMXBean.getVmVersion(),
				"Process ID:" + runtimeMXBean.getName().split("@")[0],
				"Startup Time:" + startupTime + " s."
		}));
		
		return applicationContext;
	}
	
	/**
	 * Inspects an object to see if it a MODB, and if so, if it has an assigned ObjectName and is registered.
	 * @param beanName The bean name
	 * @return The ObjectName assigned or null.
	 * @throws MalformedObjectNameException
	 * @throws NullPointerException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws NotCompliantMBeanException
	 */
	protected ObjectName inspectForJMXRegister(String beanName) throws MalformedObjectNameException, NullPointerException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
		Object bean = applicationContext.getBean(beanName);
		if(applicationContext.isPrototype(beanName)) {
			return null;
		}
		if(bean instanceof ManagedObjectDynamicMBean) {
			ManagedObjectDynamicMBean modb = (ManagedObjectDynamicMBean)bean;
			ObjectName on = modb.getObjectName();
			if(!modb.isRegistered()) {				
				if(on==null) {
					on = assignObjectName(beanName, modb);
				}
				JMXHelperExtended.getHeliosMBeanServer().registerMBean(modb, on);
			}
			return on;
		} else {
			return null;
		}
	}
	
	/**
	 * Generates a new ObjectName based on the class and bean names.
	 * @param beanName The Spring bean name
	 * @param modb The MODB to assign an ObjectName to.
	 * @return The generated ObjectName.
	 * @throws MalformedObjectNameException
	 * @throws NullPointerException
	 */
	protected ObjectName assignObjectName(String beanName, ManagedObjectDynamicMBean modb) throws MalformedObjectNameException, NullPointerException {
		StringBuilder b = new StringBuilder("org.helios.jmx:service=");
		b.append(modb.getClass().getSimpleName());
		b.append(",name=").append(beanName);
		ObjectName on = new ObjectName(b.toString());		
		modb.setObjectName(on);
		return on;
	}
	
	

	public void onApplicationEvent(ApplicationEvent event) {
		LOG.info("\n\tAPP EVENT:" + event);
		if(event instanceof ContextRefreshedEvent) {
			System.err.println("ContextRefreshedEvent");
			Object source = event.getSource();
			if(source instanceof GenericApplicationContext && !source.equals(applicationContext)) {
				System.err.println("Child");
				GenericApplicationContext child = (GenericApplicationContext)source;
				System.err.println("Adding Child App Context Instance[" + child.getDisplayName() + "] / [" + child.getId() + "]");
				childContexts.put(child.getDisplayName(), child);
			}
		}
				
	}

	/**
	 * @return the applicationContext
	 */
	public HeliosApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(javax.management.ObjectName.class, new ObjectNamePropertyEditor());
		registry.registerCustomEditor(org.w3c.dom.Node.class, new XMLNodeEditor());
		registry.registerCustomEditor(java.net.URL.class, new URLPropertyEditor());
		registry.registerCustomEditor(org.apache.log4j.Level.class, new Log4JLevelEditor());

		PropertyEditorManager.registerEditor(javax.management.ObjectName.class, ObjectNamePropertyEditor.class);
		PropertyEditorManager.registerEditor(org.w3c.dom.Node.class, XMLNodeEditor.class);
		PropertyEditorManager.registerEditor(java.net.URL.class, URLPropertyEditor.class);
		PropertyEditorManager.registerEditor(org.apache.log4j.Level.class, Log4JLevelEditor.class);

		
	}

}

/*

Deployed Bean List:
============================
	MBeanServerJMXUrl
	Printer
	RMIRegistry
	MBeanServerConnector
	SchedulingDataSource
	HeliosScheduler
	MBeanServer
	CronInsertTask
	FixedInsertTask
	SelectTask
	StopTask
	DynamicTaskFactory
	DynamicTask Application Context/DynamicTask
	
*/
