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
import org.opennms.protocols.snmp.SnmpEndOfMibView;
import org.opennms.protocols.snmp.SnmpObjectId;
import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpPduRequest;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: SnmpWalkHandler</p>
 * <p>Description: Implementation of SnmpWalk Requester</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/SnmpWalkHandler.java $
 * $Id: SnmpWalkHandler.java 1692 2009-11-01 13:01:28Z frankc01 $
 */
public class SnmpWalkHandler extends AbstractSnmpHandler {

	private	SnmpObjectId	stopOid=null;
	protected static Logger log = Logger.getLogger(SnmpWalkHandler.class);

	@SuppressWarnings("unused")
	private	SnmpWalkHandler() {
		super();
	}
	public	SnmpWalkHandler(SnmpObjectId stopOid) {
		super();
		this.stopOid=stopOid;
	}
	
	public	SnmpWalkHandler(IHeliosSnmpRequester requester,SnmpObjectId stopOid) {
		super(requester);
		this.stopOid=stopOid;
	}
	/* (non-Javadoc)
	 * @see org.opennms.protocols.snmp.SnmpHandler#snmpReceivedPdu(org.opennms.protocols.snmp.SnmpSession, int, org.opennms.protocols.snmp.SnmpPduPacket)
	 */
	//@Override
	public void snmpReceivedPdu(SnmpSession session, int command,
			SnmpPduPacket pdu) {
		SnmpPduRequest req = null;

		if (pdu instanceof SnmpPduRequest) {
			req = (SnmpPduRequest) pdu;
		}

		if (pdu.getCommand() != SnmpPduPacket.RESPONSE) {
			log.error("Error: Received non-response command " + pdu.getCommand());
			
			synchronized (requester) {
				requester.notify();
			}
			return;
		}

		if (req.getErrorStatus() != 0) {
			log.debug("End of mib reached");
			synchronized (requester) {
				requester.notify();
			}
			return;
		}

		//
		// Passed the checks so lets get the first varbind and
		// check to see if it is the last one, otherwise
		// print out it's value
		//
		SnmpVarBind vb = pdu.getVarBindAt(0);
		if (vb.getValue().typeId() == SnmpEndOfMibView.ASNTYPE
		          || (this.stopOid != null && this.stopOid.compare(vb.getName()) < 0)) {
			log.debug("End of mib reached for ["+pdu.getRequestId()+"]");
			synchronized (requester) {
				requester.notify();
			}
			return;
		}

		log.debug(vb.getName().toString() + ": " + vb.getValue().toString());
		requester.setResultForKey(pdu.getRequestId(), vb);
		
		//
		// make the next pdu
		//
		SnmpVarBind[] vblist = {new SnmpVarBind(vb.getName())};
		SnmpPduRequest newReq = new SnmpPduRequest(SnmpPduPacket.GETNEXT, vblist);
		newReq.setRequestId(pdu.getRequestId());
		session.send(newReq);
	}

}
