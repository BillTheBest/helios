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
package org.helios.espercl;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.management.remote.JMXServiceURL;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * <p>Title: EsperCLStartOptions</p>
 * <p>Description: The esper command line startup options.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.EsperCLStartOptions</code></p>
 */

public class EsperCLStartOptions {
	protected JMXServiceURL jmxServiceUrl = null;

	/**
	 * Returns the JMX Service URL of the Helios Esper MBeanServer to connect to.
	 * @return the JMX Service URL of the Helios Esper MBeanServer to connect to.
	 */
	public JMXServiceURL getJmxServiceUrl() {
		return jmxServiceUrl;
	}

	/**
	 * Sets the JMX Service URL of the Helios Esper MBeanServer to connect to.
	 * @param jmxServiceUrl a JMX Service URL 
	 * @throws MalformedURLException 
	 */
	@Option(name="-jmxurl", required=false, usage="A JMX Service URL to connect to a Helios Esper MBeanServer")
	public void setJmxServiceUrl(String jmxServiceUrl)  {
		try {
			this.jmxServiceUrl = new JMXServiceURL(jmxServiceUrl);
		} catch (MalformedURLException me) {
			err("Failed to create JMXServiceURL from [" + jmxServiceUrl + "] " + me);
		}
	}
	
	/**
	 * All the non option arguments. 
	 */
	@Argument
	protected List<String> arguments = new ArrayList<String>();

	
	
	/**
	 * Called by the application main to process the command line options.
	 * @param args the command line arguments.
	 */
	public void doMain(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        
        // if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);

        try {
            // parse the arguments.
            parser.parseArgument(args);
            if(jmxServiceUrl!=null) {
            	out("Connecting to JMXServiceURL [" + jmxServiceUrl + "]......");
            	//out("Additional arguments ", arguments);
            	
            } else {
            	//throw new Exception("No JMX URL provided");
            	parser.printUsage(System.out);
            	System.exit(0);
            }
            
        } catch( Exception e ) {
        	err("Command line exception:", e);
        }
	}
	
	/**
	 * Concatenates and err prints a message
	 * @param messages the fragments of an error message
	 */
	public static void err(Object...messages) {
		if(messages==null || messages.length<1) return;
		StringBuilder b = new StringBuilder();
		for(Object o: messages) {
			if(o!=null) {
				b.append(o.toString());
			}
		}
		System.err.println(b);
	}
	
	/**
	 * Concatenates and out prints a message
	 * @param messages the fragments of a message
	 */
	public static void out(Object...messages) {
		if(messages==null || messages.length<1) return;
		StringBuilder b = new StringBuilder();
		for(Object o: messages) {
			if(o!=null) {
				b.append(o.toString());
			}
		}
		System.out.println(b);
	}
	
}
