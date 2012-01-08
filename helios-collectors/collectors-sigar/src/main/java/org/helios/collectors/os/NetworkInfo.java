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
package org.helios.collectors.os;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: NetworkInfo</p>
 * <p>Description: POJO to hold Network related information for target host.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class NetworkInfo implements Serializable{

	private static final long serialVersionUID = 4323867637223733299L;
	protected String host = "";
	protected String domain = "";
	protected String primaryDNS = "";
	protected String secondaryDNS = "";
	protected String defaultGateway = "";
	protected List<NetworkInterface> interfaces = new ArrayList<NetworkInterface>();
	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}
	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}
	/**
	 * @param domain the domain to set
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}
	/**
	 * @return the primaryDNS
	 */
	public String getPrimaryDNS() {
		return primaryDNS;
	}
	/**
	 * @param primaryDNS the primaryDNS to set
	 */
	public void setPrimaryDNS(String primaryDNS) {
		this.primaryDNS = primaryDNS;
	}
	/**
	 * @return the secondaryDNS
	 */
	public String getSecondaryDNS() {
		return secondaryDNS;
	}
	/**
	 * @param secondaryDNS the secondaryDNS to set
	 */
	public void setSecondaryDNS(String secondaryDNS) {
		this.secondaryDNS = secondaryDNS;
	}
	/**
	 * @return the defaultGateway
	 */
	public String getDefaultGateway() {
		return defaultGateway;
	}
	/**
	 * @param defaultGateway the defaultGateway to set
	 */
	public void setDefaultGateway(String defaultGateway) {
		this.defaultGateway = defaultGateway;
	}
	/**
	 * @return the interfaces
	 */
	public List<NetworkInterface> getInterfaces() {
		return interfaces;
	}
	/**
	 * @param interfaces the interfaces to set
	 */
	public void setInterfaces(List<NetworkInterface> interfaces) {
		this.interfaces = interfaces;
	}
	/**
	 * Constructs a <code>StringBuilder</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    StringBuilder retValue = new StringBuilder("");
	    retValue.append("NetworkInfo ( " + super.toString() + TAB);
	    retValue.append("host = " + this.host + TAB);
	    retValue.append("domain = " + this.domain + TAB);
	    retValue.append("primaryDNS = " + this.primaryDNS + TAB);
	    retValue.append("secondaryDNS = " + this.secondaryDNS + TAB);
	    retValue.append("defaultGateway = " + this.defaultGateway + TAB);
	    retValue.append("interfaces = " + this.interfaces + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}

	
}
