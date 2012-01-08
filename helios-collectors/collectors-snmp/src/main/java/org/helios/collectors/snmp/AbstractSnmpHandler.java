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

package org.helios.collectors.snmp;

import org.opennms.protocols.snmp.SnmpHandler;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpSyntax;

/**
 * <p>Title: AbstractSnmpHandler</p>
 * <p>Description: Abstraction of common handler methods</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/AbstractSnmpHandler.java $
 * $Id: AbstractSnmpHandler.java 1692 2009-11-01 13:01:28Z frankc01 $
 */

public abstract class AbstractSnmpHandler implements IHeliosSnmpHandler, SnmpHandler{

	protected IHeliosSnmpRequester requester = null;

	protected	AbstractSnmpHandler() {
		
	}
	
	/**
	 * Default constructor 
	 * @param requester 
	 * @param requesterKey
	 */
	public AbstractSnmpHandler(IHeliosSnmpRequester requester) {
		this.requester=requester;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpHandler#setSnmpRequester(org.helios.snmp.IHeliosSnmpRequester, int)
	 */
	//@Override
	public void setSnmpRequester(IHeliosSnmpRequester requester) {
		this.requester=requester;		
	}

	/* (non-Javadoc)
	 * @see org.opennms.protocols.snmp.SnmpHandler#snmpInternalError(org.opennms.protocols.snmp.SnmpSession, int, org.opennms.protocols.snmp.SnmpSyntax)
	 */
	//@Override
	public void snmpInternalError(SnmpSession session, int err, SnmpSyntax pdu) {
		requester.setError(err);
		synchronized(requester) {
			requester.notify();
		}
	}

	/* (non-Javadoc)
	 * @see org.opennms.protocols.snmp.SnmpHandler#snmpTimeoutError(org.opennms.protocols.snmp.SnmpSession, org.opennms.protocols.snmp.SnmpSyntax)
	 */
	//@Override
	public void snmpTimeoutError(SnmpSession session, SnmpSyntax pdu) {
		requester.setTimeoutError();
		synchronized(requester) {
			requester.notify();
		}
		
	}



}