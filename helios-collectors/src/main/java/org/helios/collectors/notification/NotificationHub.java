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
package org.helios.collectors.notification;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

/**
 * <p>Title: NotificationHub</p>
 * <p>Description: Hub for trapping notifications that Helios is interested in and broadcasting it further to various consumers</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class NotificationHub{
	protected Logger log = Logger.getLogger(NotificationHub.class);
	protected JMXServiceURL url = null;
	protected JMXConnector connector = null;
	protected MBeanServerConnection server;
	protected ObjectName mbeanServerDelegate = null;
	protected HubNotificationListener listener = null;
	protected HeliosNotificationListener heliosListener = null;
	protected List<ObjectName> collectorRegistry = new ArrayList<ObjectName>();
	protected String[] regexMatcher;  
	protected String[] superClassMatcher;
	protected Pattern[] patterns;
	/**
	 * Constructor that sets up a link to the MBean Server and registers itself as a listener to 
	 * all MBeanServerNotifications coming out of there. 
	 * @param jmxServiceURL
	 */
	public NotificationHub(String jmxServiceURL, HeliosNotificationListener customListener) {
		try{
			url = new JMXServiceURL(jmxServiceURL);
			heliosListener = customListener;
			connector = JMXConnectorFactory.connect(url, null);
			server = connector.getMBeanServerConnection();
			mbeanServerDelegate = new ObjectName("JMImplementation:type=MBeanServerDelegate"); 
			listener = new HubNotificationListener();
			server.addNotificationListener(mbeanServerDelegate, listener, null, null);
		}catch(MalformedURLException muex){
			log.error("Invalid jmx service url is passed to HeliosNotificationHub [ "+jmxServiceURL+" ]",muex);
		}catch(Exception ex){
			log.error("An exception occured while trying to connect to the jmx service url [ "+jmxServiceURL+" ]",ex);
		}
	}
	

	/**
	 * @return the regexMatcher
	 */
	public String[] getRegexMatcher() {
		return regexMatcher.clone();
	}

	/**
	 * @param regexMatcher the regexMatcher to set
	 */
	public void setRegexMatcher(String[] regexMatcher) {
		this.regexMatcher = regexMatcher;
		patterns = new Pattern[regexMatcher.length];
		for(int i=0;i<regexMatcher.length;i++){
			patterns[i]=Pattern.compile(regexMatcher[i]);
		}
	}
       	
	
	/**
	 * @return the collectorRegistry
	 */
	public List<ObjectName> getCollectorRegistry() {
		return collectorRegistry;
	}  
	
	public void displayCollectorRegistry(){
		Iterator<ObjectName> collectorI= collectorRegistry.iterator();
		while(collectorI.hasNext())
			log.info(collectorI.next().getCanonicalName());
	}
	
    /**
     * Hub notification listener that will listen on registration and 
     * unregistration events of all MBeans 
     */
    private class HubNotificationListener implements NotificationListener {
        public void handleNotification(Notification notification, Object handback) {
            if(notification instanceof MBeanServerNotification){
            	MBeanServerNotification mbeanNotification = (MBeanServerNotification) notification;
            	ObjectName mbeanName = mbeanNotification.getMBeanName();     
            	if(notification.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)){
            			match(mbeanName, notification.getType());
            	}else if(notification.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)){
            		if(collectorRegistry.remove(mbeanName))
            			log.debug("*************** Listerner deactivated for mbean: [ "+mbeanName+ " ]");
            	}
            }
        }
        
		private void match(ObjectName mbeanName, String type) {
    		if(superClassMatcher!=null && superClassMatcher.length>0){
    			for(String name: superClassMatcher){
    				try{
						if(server.isInstanceOf(mbeanName, name)){
							if(!collectorRegistry.contains(mbeanName)){
		            			server.addNotificationListener(mbeanName, heliosListener, null, null);
		            			collectorRegistry.add(mbeanName);
		            			log.debug("*************** Listerner activated for mbean: [ "+mbeanName+ " ]");
		            			return;  
		            		}
						}
    				}catch(Exception ex){
    					//Ignore exception for now
    				}
    			}
    		}
    		
    		if(regexMatcher!=null && regexMatcher.length>0){
    			for(Pattern pattern: patterns){
    				try{
    					//matches the pattern on either mBeanName or notification type
    					if(	pattern.matcher(mbeanName.getCanonicalName()).matches() 
    						|| pattern.matcher(type).matches()){
	            			server.addNotificationListener(mbeanName, heliosListener, null, null);
	            			collectorRegistry.add(mbeanName);
	            			log.debug("*************** Listerner activated for mbean: [ "+mbeanName+ " ]");
	            			return;    						
    					}
    				}catch(Exception ex){
    					//Ignore exception for now
    				}
    			}    			
    		}
		}
        
    } // End of class HubNotificationListener

	/**
	 * @return the heliosListener
	 */
	public HeliosNotificationListener getHeliosListener() {
		return heliosListener;
	}

	/**
	 * @param heliosListener the heliosListener to set
	 */
	public void setHeliosListener(HeliosNotificationListener heliosListener) {
		this.heliosListener = heliosListener;
	}


	/**
	 * @return the connector
	 */
	public JMXConnector getConnector() {
		return connector;
	}


	/**
	 * @param connector the connector to set
	 */
	public void setConnector(JMXConnector connector) {
		this.connector = connector;
	}


	/**
	 * @return the superClassMatcher
	 */
	public String[] getSuperClassMatcher() {
		return superClassMatcher.clone();
	}


	/**
	 * @param superClassMatcher the superClassMatcher to set
	 */
	public void setSuperClassMatcher(String[] superClassMatcher) {
		this.superClassMatcher = superClassMatcher;
	}
    
}
