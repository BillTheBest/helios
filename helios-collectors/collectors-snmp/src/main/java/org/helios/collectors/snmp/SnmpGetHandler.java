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

import org.apache.log4j.Logger;
import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpPduRequest;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: SnmpGetHandler</p>
 * <p>Description: Implementation of Snmp Handler</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/SnmpGetHandler.java $
 * $Id: SnmpGetHandler.java 1692 2009-11-01 13:01:28Z frankc01 $
 */
public class SnmpGetHandler extends AbstractSnmpHandler  {

	protected static Logger log = Logger.getLogger(SnmpGetHandler.class);
	@SuppressWarnings("unused")
	private	SnmpGetHandler() {
		super();
	}
	
	public	SnmpGetHandler(IHeliosSnmpRequester requester) {
		super(requester);
	}
	
	/* (non-Javadoc)
	 * @see org.opennms.protocols.snmp.SnmpHandler#snmpReceivedPdu(org.opennms.protocols.snmp.SnmpSession, int, org.opennms.protocols.snmp.SnmpPduPacket)
	 */
	//@Override
	public void snmpReceivedPdu(SnmpSession session, int command,
			SnmpPduPacket pdu) {
		SnmpVarBind varBind = pdu.getVarBindAt(0);
		if(log.isDebugEnabled())
			log.debug("Received value: " +varBind.toString()+" ");
		if(pdu.getRequestId() == -1)
			requester.setResult(varBind);
		else
			requester.setResultForKey(pdu.getRequestId(), varBind);
		SnmpPduRequest	pduRequest = requester.getRequest();
		if( pduRequest == null ) {
			synchronized (requester) {
				requester.notify();
			}
		}
		else {
			session.send(pduRequest,this);
		}
	}

}
