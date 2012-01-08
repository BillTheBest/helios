package test.org.helios.collectors.spring;
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

import org.helios.collectors.url.URLCollector;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * <p>Title: SpringTest</p>
 * <p>Description: Test harness for Tracer Factory Spring Factory</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SpringTest implements FilenameFilter {
    protected FileSystemXmlApplicationContext appContext = null;
    /**
     * @param args
     */
    public static void main(String[] args) {
        if(args.length < 1) {
            log("Usage: java test.org.helios.tracing.SpringTest <directory with spring xml>");
            return;
        }
        String configFile = args[0];
        File f = new File(configFile);
        if(!f.exists()) {
            log("Could not read config file:" + configFile);
            return;
        }
        log("Starting with config file:" + configFile);
        try {
            SpringTest collector = new SpringTest(f);
        } catch (Exception e) {
            log("Failed to boot strap:" + e);
            return;
        }
    }

	public SpringTest(File configDir) {
        try {
            log("Creating SpringCollector Container using config directory:[" + configDir + "]");
            if(!configDir.exists() || !configDir.isDirectory()) {
                throw new RuntimeException("The directory [" + configDir + "] does not exist or is not a directory");
            }
            String[] locatedFiles =  configDir.list(this);
            for(int i = 0; i < locatedFiles.length; i++) {
                locatedFiles[i] = configDir.getPath() + File.separator + locatedFiles[i];
                log("Located Spring Config File:[" + locatedFiles[i] + "]");
            }
            appContext = new FileSystemXmlApplicationContext();
            //appContext.refresh();
            // ===================================================
            // replace with a properties file.       
            // ===================================================           
            java.beans.PropertyEditorManager.registerEditor(org.w3c.dom.Node.class,
                    org.helios.editors.XMLNodeEditor.class);
            java.beans.PropertyEditorManager.registerEditor(org.apache.log4j.Level.class,
                    org.helios.editors.Log4JLevelEditor.class);
            // ===================================================
           
            appContext.setConfigLocations(locatedFiles);
            appContext.refresh();
           
            int beanCount = appContext.getBeanDefinitionCount();       
            String[] beans = appContext.getBeanDefinitionNames();
            for(String bean: beans){
            	log("************** "+bean);
            }
            log("Spring Container Initialized with [" + beanCount + "] Beans.");
        } catch (Exception e) {
            log("Failed to initialize ApplicationContext:" + e);
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize ApplicationContext", e);
        }
    
    }
   
    public static void log(Object message) {
        System.out.println(message);
    }
   
    public boolean accept(File dir, String name) {       
        return name.toUpperCase().endsWith(".XML");
    }
}