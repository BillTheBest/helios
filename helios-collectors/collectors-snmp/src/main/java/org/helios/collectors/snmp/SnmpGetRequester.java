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

import java.util.List;
import org.apache.log4j.Logger;
import org.helios.collectors.snmp.request.SnmpSingleValueRequest;
import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpPduRequest;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: SnmpGetRequester</p>
 * <p>Description: Implementation of SnmpGet Requester</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/SnmpGetRequester.java $
 * $Id: SnmpGetRequester.java 1724 2009-11-16 16:26:38Z frankc01 $
 */
public class SnmpGetRequester extends AbstractSnmpRequester {

	private		List<SnmpSingleValueRequest>	requests=null;
	private		int						index=0;
	private 	Logger 					log = Logger.getLogger(getClass());
		
	protected	SnmpGetRequester() {}
	
	public	SnmpGetRequester(List<SnmpSingleValueRequest> requests,AbstractSnmpHandler handler) {
		super();
		this.requests=requests;
		this.setHandler(handler);
	}
	public	SnmpGetRequester(List<SnmpSingleValueRequest> requests) {
		super();
		this.requests=requests;
		this.setHandler(new SnmpGetHandler(this));
	}
	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#isKeyValid(long)
	 */
	//@Override
	public boolean isKeyValid(long key) {
		return (key == this.index);
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#setResult(java.lang.Object)
	 */
	//@Override
	public void setResult(SnmpVarBind result) {
		if(log.isTraceEnabled())
			log.trace("Insterting results for ["+result.getName().toString()+"]");
		requests.get(index).setResult(result);
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#setResultForKey(long, java.lang.Object)
	 */
	//@Override
	public void setResultForKey(long key, SnmpVarBind result) {
		if(isKeyValid(key)) {
			if(log.isTraceEnabled())
				log.trace("Insterting results for ["+result.getName().toString()+"]");
			requests.get((int)key).setResult(result);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#executeRequest(org.opennms.protocols.snmp.SnmpSession)
	 */
	//@Override
	public Object executeRequest(SnmpSession session) throws Exception{
		this.index = 0;
		boolean	success=true;
		long	t1 = System.currentTimeMillis();
		SnmpVarBind[] vblist = {new SnmpVarBind(this.requests.get(index).getOid())};
		SnmpPduRequest	pdu = new SnmpPduRequest(SnmpPduPacket.GET,vblist);
		pdu.setRequestId(this.index);
		try {
			session.setDefaultHandler(this.getHandler());
			synchronized (this) {
				session.send(pdu);
				this.wait();
				if(log.isTraceEnabled())
					log.trace("SnmpGetRequester handler has signalled complete");
			}
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		log.info("Completed GET in ["+(System.currentTimeMillis()-t1)+"] ms");
		if(this.isError() == true || this.isTimeout() == true) {
			log.error("Status of get error = "+this.isError()+" timeout = "+this.isTimeout());
			success=false;
		}
		return (success == true ? this.requests: null);
	}
	
	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.IHeliosSnmpRequester#getResultCount()
	 */
	public int getResultCount() {
		return this.requests.size();
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#getResult()
	 */
	//@Override
	public Object getResult() {
		return this.requests;
	}

	public List<SnmpSingleValueRequest> getRequestResults() {
		return this.requests;
	}

	public	void	addOid(SnmpSingleValueRequest oid) {
		this.requests.add(oid);
	}
	
	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#getRequest()
	 */
	//@Override
	public SnmpPduRequest getRequest() {
		SnmpPduRequest	pdu =null;
		this.index++;
		if(index < this.requests.size()) {
			SnmpVarBind[] vblist = {new SnmpVarBind(this.requests.get(index).getOid())};
			pdu = new SnmpPduRequest(SnmpPduPacket.GET,vblist);
			pdu.setRequestId(this.index);
		}
		return pdu;
	}

}
