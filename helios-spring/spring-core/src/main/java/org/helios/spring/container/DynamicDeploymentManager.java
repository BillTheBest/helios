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

import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.helpers.JMXHelperExtended;
import org.helios.helpers.XMLHelper;
import org.helios.io.file.RecursiveDirectorySearch;
import org.helios.io.file.filters.ConfigurableFileExtensionFilter;
import org.helios.spring.container.templates.SpringAccessorDirectiveModel;
import org.helios.spring.container.templates.provider.ITemplateProvider;
import org.helios.spring.container.templates.provider.ITemplateProvision;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * <p>Title: DynamicDeploymentManager</p>
 * <p>Description: A manager for deploying and tracking dynamic XML configurations</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class DynamicDeploymentManager implements Runnable, ThreadFactory, UncaughtExceptionHandler {
	public static final int DEFAULT_SCAN_FREQUENCY = 15000;
	protected int scanFrequency = 0;
	protected String[] searchDirectories = new String[]{};
	protected Map<String, Long> errorFiles = new ConcurrentHashMap<String, Long>();
	protected Set<File> searchDirs = new CopyOnWriteArraySet<File>();
	protected Map<String, DynamicConfiguration> managedFiles = new ConcurrentHashMap<String, DynamicConfiguration>();
	protected HeliosApplicationContext parentAppContext = null;
	protected ScheduledExecutorService scheduler = null;
	protected static AtomicLong serial = new AtomicLong(0);
	protected static Logger LOG = Logger.getLogger(DynamicDeploymentManager.class); 
	protected SpringBeanTemplateAccessor springBeanTemplateAccessor = null;
	protected MBeanAttributeTemplateAccessor mBeanAttributeTemplateAccessor = null;
	/**
	 * @return
	 */
	Set<Map.Entry<String, DynamicConfiguration>> getConfigurations() {
		return Collections.unmodifiableMap(managedFiles).entrySet();
	}
	
	
	/**
	 * @param scanFrequency
	 * @param searchDirectories
	 * @param parentAppContext
	 */
	public DynamicDeploymentManager(int scanFrequency,
			String[] searchDirectories,
			HeliosApplicationContext parentAppContext) {
		this.scanFrequency = scanFrequency;
		this.searchDirectories = searchDirectories;
		this.parentAppContext = parentAppContext;
		springBeanTemplateAccessor = new SpringBeanTemplateAccessor(this.parentAppContext);
		mBeanAttributeTemplateAccessor = new MBeanAttributeTemplateAccessor(JMXHelperExtended.getHeliosMBeanServer());
		StringBuilder b = new StringBuilder("Constructed DynamicDeploymentManager.");
		b.append("\n\tSearch Directories:");
		
		if(searchDirectories!=null && searchDirectories.length > 0) {
			for(String s: searchDirectories) {
				File f = new File(s);
				if(f.canRead() && f.isDirectory()) {
					searchDirs.add(f);
					b.append("\n\t\t[").append(s).append("]");
				} else {
					LOG.warn("The provided search directory cannot be read or is not a directory [" + s + "]");
				}
				
			}
		}
		b.append("\n\tScan Frequency:[").append(this.scanFrequency).append("] ms.\nLoading Dynamics Now....");		
		LOG.info(b.toString());
		run();
		if(this.scanFrequency > 0) {
			scheduler = Executors.newScheduledThreadPool(1, this);
			scheduler.scheduleWithFixedDelay(this, 200, scanFrequency, TimeUnit.MILLISECONDS);
		}
	}
	
	/**
	 * @param scanFrequency
	 * @param searchDirectories
	 * @param parentAppContext
	 */
	public DynamicDeploymentManager(String[] searchDirectories, HeliosApplicationContext parentAppContext) {
		this(DEFAULT_SCAN_FREQUENCY, searchDirectories, parentAppContext);
	}

	/**
	 * Starts the deployment scanner thread.
	 * @see java.lang.Runnable#run()
	 * HeliosContainerMain.HELIOS_XML_TEMPLATE
	 */
	public void run() {
		if(LOG.isDebugEnabled()) LOG.debug("Starting Dynamic Deployment Scan");
		Set<String> seenFiles = new HashSet<String>();
		for(File dir: searchDirs) {
			if(LOG.isDebugEnabled()) LOG.debug("Scanning [" + dir + "]....");
			String[] dConfigFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(HeliosContainerMain.HELIOS_XML_DYNAMIC), dir.toString());			
			Set<DynamicConfiguration> newConfigurations = new TreeSet<DynamicConfiguration>();
			if(dConfigFiles!=null && dConfigFiles.length > 0) {
				for(String fName: dConfigFiles) {
					File configFile = new File(fName);
					if(!configFile.canRead()) {
						LOG.warn("Located config file but could not read [" + configFile + "]");
						continue;
					}
					seenFiles.add(fName);
					if(errorFiles.containsKey(fName)) {
						File eFile = new File(fName);
						long lastMod = eFile.lastModified();
						if(lastMod <= errorFiles.get(fName)) {
							if(LOG.isDebugEnabled()) LOG.debug("Skipping file [" + fName + "] since it is an in errored state and has not changed.");
							continue;
						} else {
							errorFiles.remove(fName);
						}
					}					
					DynamicConfiguration dc = null;
					if(managedFiles.containsKey(fName)) {
						dc = managedFiles.get(fName);
						if(dc.hasChanged()) {
							try { 
								dc.refresh();
							} catch (Exception e) {
								LOG.error("Failed to refresh updated configuration:[" + dc.getDisplayName() + "/" + dc.getId() + "]\n\tWill retry when file is changed.", e);
								errorFiles.put(fName, configFile.lastModified());
							}
						}
					} else {
						try {
							dc = new DynamicConfiguration(configFile, parentAppContext);
							managedFiles.put(fName, dc);							
							//dc.start();
							newConfigurations.add(dc);
						} catch (Exception e) {
							LOG.error("Failed to load configuration:[" + configFile + "]\nAdding to errored files.\n", e);
							errorFiles.put(fName, configFile.lastModified());
						}
					}
				} // end of for loop on located files
				// start all new DCs
				for(DynamicConfiguration dc: newConfigurations) {
					try {
						dc.start();
					} catch (Exception e) {
						LOG.error("Failed to start configuration:[" + dc.getDisplayName() + "/" + dc.getId() + "]\n\tWill retry when file is changed.", e);
					}
				}
				
			}
			if(LOG.isDebugEnabled()) LOG.debug("Completed Scan of [" + dir + "]");
		}
		// look for managed files that were not located
		for(String name: managedFiles.keySet()) {
			if(!seenFiles.contains(name)) {
				LOG.info("Detected deleted configuration file: [" + name + "]");
				DynamicConfiguration dc = managedFiles.remove(name);
				if(dc==null) {
					LOG.warn("Could not locate DynamicConfiguration for [" + name + "]");
				} else {
					try { 
						dc.stop();
					} catch (Exception e) {
						LOG.error("Failed to stop configuration:[" + dc.getDisplayName() + "/" + dc.getId() + "]", e);
					}							
				}
			}
		}
		
		if(LOG.isDebugEnabled()) LOG.debug("Starting Template Scan");
		for(File dir: searchDirs) {
			if(LOG.isDebugEnabled()) LOG.debug("Scanning [" + dir + "]....");
			String[] templateFiles = RecursiveDirectorySearch.searchDirectories(new ConfigurableFileExtensionFilter(HeliosContainerMain.HELIOS_XML_TEMPLATE), dir.toString());
			for(String fileName: templateFiles) {
				processTemplate(fileName);
			}
		}		
	}
	
	/**
	 * @param fileName
	 */
	protected void processTemplate(String fileName)  {
		
		LOG.info("Processing Template [" + fileName + "]\nProcessing XML  parse...");
		// Do once per provision
		Configuration cfg = new Configuration();
		cfg.setSharedVariable("h", new SpringAccessorDirectiveModel(parentAppContext));
		String providerName = null;
		String providerService = null;
		String relativeDir = null;
		ITemplateProvider tProvider = null;
		String cleanedTemplate = null;
		File xmlFile = null;
		try {
			xmlFile = new File(fileName);
			Element docElement = XMLHelper.parseXML(xmlFile).getDocumentElement();
			/**
			 * <template-directive providerName="pn" relativeDir="." providerService="svc" fileName=""/>
			 */
			Node directiveNode = XMLHelper.getChildNodeByName(docElement, "template-directive",false);
			if(directiveNode != null) {
				providerName = XMLHelper.getAttributeValueByName(directiveNode, "providerName");
				providerService = XMLHelper.getAttributeValueByName(directiveNode, "providerService");
				relativeDir = XMLHelper.getAttributeValueByName(directiveNode, "relativeDir");
				docElement.removeChild(directiveNode);				
			}
			int i = 0;
			if(providerName==null) i++;
			if(providerService==null) i++;
			if(i==1) throw new Exception("Both provider service and provider name must be defined, or neither.");
//			Class<?> clazz = Class.forName("org.apache.xerces.dom.CoreDOMImplementationImpl");
//			StringBuilder b = new StringBuilder("\n\tInterfaces");
//			for(Class<?> iface: clazz.getInterfaces()) {
//				b.append("\n\t").append(iface.getName()).append(":").append(iface.getClassLoader());
//			}
//			LOG.info(b);
//			LOG.info("DOMImplementationLS:" + DOMImplementationLS.class.getClassLoader());
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();			
			DOMImplementationLS impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
			LSSerializer writer = impl.createLSSerializer();
			cleanedTemplate = writer.writeToString(docElement);
			if(relativeDir==null) relativeDir=".";
			if(providerService!=null && providerName!=null) {
				tProvider = (ITemplateProvider)parentAppContext.getBean(providerService, ITemplateProvider.class);				
			}
		} catch (Exception e) {
			LOG.error("Failed to initialize template generator", e);
		}
		
		try {
			cleanedTemplate = cleanedTemplate.replace("&quot;", "\"");
			Reader stringReader = new StringReader(cleanedTemplate);
			Template template = new Template("Helios Dynamic Deployment Generator", stringReader, cfg);
			Map<String, Object> rootMap = new HashMap<String, Object>();
			LOG.info("Processing Template....");
			if(tProvider!=null & providerName!=null) {				
				for(ITemplateProvision provision: tProvider.getProvisions(providerName)) {
					rootMap.clear();
					StringWriter sw = new StringWriter();
					rootMap.put("hp", provision); 
					rootMap.put("hpb", springBeanTemplateAccessor);
					rootMap.put("hpj", mBeanAttributeTemplateAccessor);					
					template.process(rootMap, sw);
					
					File outFile = new File(xmlFile.getParent() + "/" + relativeDir + "/" + xmlFile.getName().split("\\.")[0] + "-" + provision.getId().replaceAll("\\s+|\\?|\\*", "_") + "." + provision.getProvisionId() + HeliosContainerMain.HELIOS_XML_DYNAMIC_TEMPLATIZED);
					if(LOG.isDebugEnabled()) LOG.debug("Writing out dynamic temporary file [" + outFile +"]");
					FileOutputStream fos = null;
					
					try {						
						outFile.delete();
						fos = new FileOutputStream(outFile, false);
						fos.write(sw.toString().getBytes());
						fos.flush();
						fos.close();
						LOG.info("Generated dynamic temporary deployment [" + outFile +"]");
					} catch (Exception e) {
						LOG.error("Failed to write out dynamic temporary file [" + outFile + "]", e);
					} finally {						
						try { fos.close(); } catch (Exception e) {}
					}
					if(LOG.isDebugEnabled()) LOG.debug("Template Processed. Rendered Content Follows:\n\t%%%%%%%%%%%%%%%%%%%%%%%\n" + sw.toString() + "\n\t%%%%%%%%%%%%%%%%%%%%%%%\n" );
				}
			} else {
				StringWriter sw = new StringWriter();
				template.process(rootMap, sw);
				if(LOG.isDebugEnabled()) LOG.debug("Template Processed. Rendered Content Follows:\n\t%%%%%%%%%%%%%%%%%%%%%%%\n" + sw.toString() + "\n\t%%%%%%%%%%%%%%%%%%%%%%%\n" );
			}
			
			
		} catch (Exception e) {
			LOG.error("Failed to process template [" + fileName + "]", e);
		}
		
	}

	/**
	 * @param r
	 * @return
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "DynamicDeploymentManager Scan Thread#" + serial.incrementAndGet());
		t.setDaemon(true);
		t.setUncaughtExceptionHandler(this);
		return t;
	}

	/**
	 * @param t
	 * @param e
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	public void uncaughtException(Thread t, Throwable e) {
		LOG.warn("Deployment Scanner Uncaught Exception from thread [" + t.getName() + "/" + t.getId(), e);
	}
	
	
	
	
}

