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
import java.io.FileFilter;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.helpers.XMLHelper;
import org.helios.version.VersionHelper;
import org.w3c.dom.Node;

/**
 * <p>Title: EclipseLauncher</p>
 * <p>Description: Parses an eclipse launcher file and executes the launch</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.spring.container.EclipseLauncher</code></p>
 */

public class EclipseLauncher {
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(EclipseLauncher.class);
	/** The end-of-line XML delimiter */
	public static final String XML_EOL = "&#10;";
	/** The launcher file */
	protected File launcher = null;
	/** The project file */
	protected File projectFile = null;
	/** The classpath file */
	protected File classpathFile = null;
	/** The project home directory */
	protected File projectHome = null;
	
	/** The main class name */
	protected String mainClass = null;
	/** The program arguments */
	protected String arguments = null;
	/** The VM arguments */
	protected String vmArguments = null;
	/** The launcher's project name */
	protected String projectName = null;
	/** Classpath Entries */
	protected final LinkedList<URL> classpathEntries = new LinkedList<URL>(); 
	
	/** A map of substitution variables */
	public static final Map<String, String> ENV_REPLACES = new HashMap<String, String>();
	
	
	/** The attribute prefix */
	public static final String ATTR_PREFIX = "org.eclipse.jdt.launching."; 
	/** The main class attribute suffix */
	public static final String MAIN_TYPE = "MAIN_TYPE";
	/** The program arguments attribute suffix */
	public static final String PROGRAM_ARGUMENTS = "PROGRAM_ARGUMENTS";
	/** The launcher project name attribute suffix */
	public static final String PROJECT_ATTR = "PROJECT_ATTR";
	/** The VM arguments attribute suffix */
	public static final String VM_ARGUMENTS = "VM_ARGUMENTS";
	
	static {
		String m2Repo = System.getenv("M2_REPO");
		if(m2Repo==null) {
			m2Repo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
			File m2RepoDir = new File(m2Repo);
			if(m2RepoDir.exists() && m2RepoDir.isDirectory()) {
				ENV_REPLACES.put("M2_REPO", m2Repo);
			}
		} else {
			ENV_REPLACES.put("M2_REPO", m2Repo);
		}		
	}
	
	
	/**
	 * Parses an eclipse launcher file and executes the launch
	 * @param args The eclipse launcher file name
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		/*
		File root = new File("/home/nwhitehead/hprojects/helios/helios-ot");		
		File subDir = findProjectDir(root, "helios-ot-core2");
		LOG.info(subDir);
		*/
		
		LOG.info("EclipseLauncher:" + VersionHelper.getHeliosVersion(EclipseLauncher.class));
		if(args==null || args.length<1) {
			LOG.error("Usage: org.helios.spring.container.EclipseLauncher <Eclipse Launch File>");
			return;
		}
		File f = new File(args[0]);
		if(!f.canRead()) {
			LOG.error("Failed to read Eclipse Launcher File [" + f + "]");
			return;			
		}
		EclipseLauncher launcher = new EclipseLauncher(f);
		LOG.info("\n" + launcher);
		
	}
	
	/**
	 * Creates a new EclipseLauncher
	 * @param f The eclipse launcher file
	 */
	public EclipseLauncher(File f) {
		launcher=f;
		LOG.info("Parsing Eclipse Launch File [" + f + "]");
		Node launcherNode = XMLHelper.parseXML(f).getDocumentElement();
		for(Node strAttr: XMLHelper.getChildNodesByName(launcherNode, "stringAttribute", false)) {
			String attrName = XMLHelper.getAttributeValueByName(strAttr, "key");
			if(attrName!=null) {
				if(attrName.equals(ATTR_PREFIX + MAIN_TYPE)) {
					mainClass = XMLHelper.getAttributeValueByName(strAttr, "value");
				} else if(attrName.equals(ATTR_PREFIX + PROJECT_ATTR)) {
					projectName = XMLHelper.getAttributeValueByName(strAttr, "value");
				} else if(attrName.equals(ATTR_PREFIX + PROGRAM_ARGUMENTS)) {
					arguments = extractArgs(XMLHelper.getAttributeValueByName(strAttr, "value"));
				} else if(attrName.equals(ATTR_PREFIX + VM_ARGUMENTS)) {
					vmArguments = extractArgs(XMLHelper.getAttributeValueByName(strAttr, "value"));
				}
			}
		}
		if(projectName==null || projectName.length()<1) {
			throw new RuntimeException("Launcher parse did not find a project name", new Throwable());
		}
		setProjectHome(f);
		setClasspath();
	}
	
	/**
	 * Iterates up the directory tree from the launcher to find the project home
	 * @param launcherFile The parsed launcher file.
	 */
	public void setProjectHome(File launcherFile) {
		File f = launcherFile.getAbsoluteFile();
		while((f=f.getParentFile())!=null) {
			LOG.info("Testing [" + f + "]");
			if(f.getName().equals(projectName)) {
				projectHome = f;
				if(new File(f.getAbsolutePath() + File.separator + ".project").exists()) {
					projectFile = new File(f.getAbsolutePath() + File.separator + ".project");
					LOG.info("Project File:" + projectFile);
				}
				if(new File(f.getAbsolutePath() + File.separator + ".classpath").exists()) {
					classpathFile = new File(f.getAbsolutePath() + File.separator + ".classpath");
					LOG.info("Classpath File:" + classpathFile);
				}	
				if(projectFile!=null && classpathFile!=null) return;
				projectHome = null;
				projectFile=null;
				classpathFile=null;
			}			
		}
	}
		
	/**
	 * Parses a configured arguments attribute value and converts it into a single string
	 * @param value The raw attribute value
	 * @return The formatted attribute value
	 */
	public static String extractArgs(String value) {
		if(value==null) return null;
		value = value.trim();
		if(value.length()<1) return "";
		if(value.contains(XML_EOL)) {
			StringBuilder b = new StringBuilder();
			for(String arg: value.split(XML_EOL)) {
				b.append(arg.trim()).append(" ");
			}
			return b.toString();
		}
		return value.replace("\n", "");		
	}
	
	/**
	 * Executes all configured replacements on the passed line
	 * @param entry The line to execute replacements on
	 * @return The replaced line
	 */
	public static String subst(String entry) {
		for(Map.Entry<String, String> replace: ENV_REPLACES.entrySet()) {
			entry = entry.replace(replace.getKey(), replace.getValue());
		}
		return entry;
	}
	
	/**
	 * Reads the classpath entries from the class path file
	 */
	public void setClasspath() {
		try{
			LOG.info("Reading classpath from [" + classpathFile + "]");						
			Node cpNode = XMLHelper.parseXML(classpathFile).getDocumentElement();
			for(Node entry: XMLHelper.getChildNodesByName(cpNode, "classpathentry", false)) {
				String kind = XMLHelper.getAttributeValueByName(entry, "kind");
				if(kind.equals("var")) {
					String path = XMLHelper.getAttributeValueByName(entry, "path");
					if(path!=null) {
						path = subst(path);
						File cp = new File(path);
						if(!cp.canRead()) {
							LOG.warn("Cannot Resolve JAR Classpath Entry [" + cp + "]");
						} else {
							classpathEntries.add(cp.toURI().toURL());
							//LOG.info("Adding JAR Path [" + cp.toURI().toURL() + "]");
						}
					}
				} else if(kind.equals("lib")) {
					File cp = new File(XMLHelper.getAttributeValueByName(entry, "path"));
					classpathEntries.add(cp.toURI().toURL());
				} else {
					//LOG.info("Unhandled kind: [" + kind + "]");
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read classpath from [" + classpathFile + "]", e);
		}
	}
	
	
	public static File findSubDir(File root, final String dirName) {
		File target = null;
		for(File f: root.listFiles()) {
			if(f.isDirectory()) {
				if(f.getName().equals(dirName)) {
					target = f;
					break;
				}
				target = findSubDir(f, dirName);
				if(target!=null) break;
			}
		}
		return target;
	}
	
	protected static final FileFilter PROJECT_FILTER = new FileFilter() {
		public boolean accept(File pathname) {			
			return pathname.isFile() && pathname.getName().equals(".project");
		}		
	};
	
	public static String getProjectName(File projectFile) {
		Node cpNode = XMLHelper.parseXML(projectFile).getDocumentElement();
		return XMLHelper.getNodeTextValue(XMLHelper.getChildNodeByName(cpNode, "name", false));
	}
	
	public static File findProjectDir(File root, final String projectName) {
		File target = null;
		
		for(File f: root.listFiles()) {
			if(f.isDirectory()) {		
				if(f.getName().equals("src") || f.getName().equals("target") || f.getName().startsWith(".")) break;
				target = findProjectDir(f, projectName);
				if(target!=null) break;
			} else {
				if(f.getName().equals(".project")) {
					if(projectName.equals(getProjectName(f))) {
						target = f.getParentFile();
						break;
					}
				}
			}
		}
		return target;
	}
	

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("EclipseLauncher [")
	        .append(TAB).append("mainClass = ").append(this.mainClass)
	        .append(TAB).append("arguments = ").append(this.arguments)
	        .append(TAB).append("vmArguments = ").append(this.vmArguments)
	        .append(TAB).append("projectName = ").append(this.projectName)
	        .append(TAB).append("projectHome = ").append(this.projectHome)
	        .append(TAB).append("projectFile = ").append(this.projectFile)
	        .append(TAB).append("classpathFile = ").append(this.classpathFile)
	        .append(TAB).append("classpath entries = ");
	    	for(URL url: classpathEntries) {
	    		retValue.append("\n\t\t").append(url);
	    	}
	        retValue.append("\n]");    
	    return retValue.toString();
	}

	/**
	 * Returns the parsed class path entries
	 * @return the classpathEntries
	 */
	public Collection<URL> getClasspathEntries() {
		return Collections.unmodifiableCollection(classpathEntries);
	}
	

}
