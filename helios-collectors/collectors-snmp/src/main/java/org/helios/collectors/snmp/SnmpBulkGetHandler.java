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
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: SnmpBulkGetHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SnmpBulkGetHandler extends AbstractSnmpHandler {

	private 	Logger log = Logger.getLogger(getClass());
	@SuppressWarnings("unused")
	private SnmpBulkGetHandler() {}
	/**
	 * @param requester
	 */
	public SnmpBulkGetHandler(IHeliosSnmpRequester requester) {
		super(requester);
	}

	/* (non-Javadoc)
	 * @see org.opennms.protocols.snmp.SnmpHandler#snmpReceivedPdu(org.opennms.protocols.snmp.SnmpSession, int, org.opennms.protocols.snmp.SnmpPduPacket)
	 */
	public void snmpReceivedPdu(SnmpSession session, int command,
			SnmpPduPacket pdu) {
		
        int responseLength = pdu.getLength();
        for (int i = 0; i < responseLength; i++)
        {
            SnmpVarBind varBind = pdu.getVarBindAt(i);
    		if(log.isTraceEnabled())
    			log.trace("Received value: " +varBind.toString()+" ");
            requester.setResult(varBind);
        }

		synchronized (requester) {
			requester.notify();
		}


	}

}
