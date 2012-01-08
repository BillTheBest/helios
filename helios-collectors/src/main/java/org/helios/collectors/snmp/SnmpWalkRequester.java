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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.opennms.protocols.snmp.SnmpObjectId;
import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpPduRequest;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: SnmpWalkRequester</p>
 * <p>Description: Implementation of SnmpWalk Requester</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/SnmpWalkRequester.java $
 * $Id: SnmpWalkRequester.java 1692 2009-11-01 13:01:28Z frankc01 $
 */
public class SnmpWalkRequester extends AbstractSnmpRequester {

	private	String			startOid=null;
	private	SnmpObjectId	stopOid=null;
	private	int				key=1;
	private	List<SnmpVarBind>	results=new ArrayList<SnmpVarBind>();
	private Logger 			log = Logger.getLogger(getClass());
	
	public	SnmpWalkRequester(String oid) {
		super();
		this.startOid=oid;
		//
		// set the stop point
		//
		SnmpObjectId id = new SnmpObjectId(this.startOid);
		int[] ids = id.getIdentifiers();
		++ids[ids.length - 1];
		id.setIdentifiers(ids);
		this.stopOid = id;
		this.setHandler(new SnmpWalkHandler(this,this.stopOid));
	}
	
	public	String	getStartOid() {
		return this.startOid;
	}
	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#executeRequest(org.opennms.protocols.snmp.SnmpSession)
	 */
	//@Override
	public Object executeRequest(SnmpSession session) throws Exception {
		long	t1 = System.currentTimeMillis();
		
		//	Clear the contents
		
		results.clear();
		
		//
		// send the first request
		//
		SnmpVarBind[] vblist = {new SnmpVarBind(this.startOid)};
		SnmpPduRequest pdu = new SnmpPduRequest(SnmpPduPacket.GETNEXT, vblist);
		pdu.setRequestId(this.key);
		try {
			session.setDefaultHandler(this.getHandler());
			synchronized (this) {
				session.send(pdu);
				this.wait();
			}
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		log.info("Completed WALK in ["+(System.currentTimeMillis()-t1)+"] ms");
		return this.results;
	}

	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.IHeliosSnmpRequester#getResultCount()
	 */
	public int getResultCount() {
		return this.results.size();
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#getResult()
	 */
	//@Override
	public Object getResult() {
		return results;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#isKeyValid(long)
	 */
	//@Override
	public boolean isKeyValid(long key) {
		return (this.key == key);
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#setResult(java.lang.Object)
	 */
	//@Override
	public void setResult(SnmpVarBind result) {
		results.add(result);
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#setResultForKey(long, java.lang.Object)
	 */
	//@Override
	public void setResultForKey(long key, SnmpVarBind result) {
		this.setResult(result);

	}

}
