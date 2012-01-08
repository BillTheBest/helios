package org.helios.jmx.util;

import javax.management.remote.JMXConnectorServer;
/**
 * <p>Title: JMXServerConnectionPrinter</p>
 * <p>Description: Prints the URL of a JMXConnectionServer</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class JMXServerConnectionPrinter {
	public JMXServerConnectionPrinter(JMXConnectorServer server) {
		StringBuffer b = new StringBuffer("\n\n\t=============================\n\tJMXConnectorServer\n\t=============================");
		if(server==null) {
			b.append("null");
		} else {
			b.append("\n\t\tAddress:[").append(server.getAddress().toString()).append("]");
			b.append("\n\t\tActive:[").append(server.isActive()).append("]");
		}
		b.append("\n\t=============================\n");
		
		System.out.println(b);
	}

}
