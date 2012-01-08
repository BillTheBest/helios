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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.helios.collectors.snmp.request.SnmpSingleValueRequest;
import org.opennms.protocols.snmp.SnmpPduBulk;
import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: SnmpBulkGetRequester</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SnmpBulkGetRequester extends SnmpGetRequester {

	private 	Logger 								log = Logger.getLogger(getClass());
	protected	int									nonRepeaters=0;
	protected	int									maxRepitition=1;
	protected 	Map<String,SnmpSingleValueRequest>	oidMap=null;

	@SuppressWarnings("unused")
	private	SnmpBulkGetRequester() {super();}
	/**
	 * @param requests
	 */
	public SnmpBulkGetRequester(List<SnmpSingleValueRequest> requests) {
		super(requests);
		this.setHandler(new SnmpBulkGetHandler(this));
	}
	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.SnmpGetRequester#executeRequest(org.opennms.protocols.snmp.SnmpSession)
	 */
	@Override
	public Object executeRequest(SnmpSession session) throws Exception {
		boolean	success=true;
		long	t1 = System.currentTimeMillis();		
        SnmpVarBind[] vblist = getPsduBinding();
        int nonRepeaters = getNonRepeaters();
        int maxRepitition = getMaxRepitition(); 
        SnmpPduPacket pdu = new SnmpPduBulk(nonRepeaters, maxRepitition, vblist);
        pdu.setRequestId(-1);

		try {
			session.setDefaultHandler(this.getHandler());
			synchronized (this) {
				session.send(pdu);
				this.wait();
				if(log.isTraceEnabled())
					log.trace("SnmpBulkGetRequester handler has signalled complete");
			}
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		if(log.isDebugEnabled())
			log.debug("Completed BulkGET in ["+(System.currentTimeMillis()-t1)+"] ms");
		if(this.isError() == true || this.isTimeout() == true) {
			log.error("Status of get error = "+this.isError()+" timeout = "+this.isTimeout());
			success=false;
		}

		return (success == true ? getRequestList(): null);
	}
	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.SnmpGetRequester#setResult(org.opennms.protocols.snmp.SnmpVarBind)
	 */
	@Override
	public void setResult(SnmpVarBind result) {

		SnmpSingleValueRequest	req = getOidMap().get(result.getName().toString());
		if(req != null) {
			req.setResult(result);
		}
		else if(result.getName().toString().endsWith(".0")) {
			String	name =result.getName().toString();
			name = name.substring(0, name.length()-2);
			if(log.isDebugEnabled())
				log.debug("Attempting second pass for ["+name+"]");
			req = getOidMap().get(name);
			if(req != null)
				req.setResult(result);
			else
				log.fatal("Second pass failed, can't resolve oid");
		}
		else
			log.fatal("Can't find oid mapping");
	}
	/**
	 * @return the nonRepeaters
	 */
	public int getNonRepeaters() {
		return nonRepeaters;
	}
	/**
	 * @param nonRepeaters the nonRepeaters to set
	 */
	public void setNonRepeaters(int nonRepeaters) {
		this.nonRepeaters = nonRepeaters;
	}
	/**
	 * @return the maxRepitition
	 */
	public int getMaxRepitition() {
		return maxRepitition;
	}
	/**
	 * @param maxRepitition the maxRepitition to set
	 */
	public void setMaxRepitition(int maxRepitition) {
		this.maxRepitition = maxRepitition;
	}
	
	protected	SnmpVarBind[]	getPsduBinding() {
		List<SnmpSingleValueRequest> requestList = getRequestList();
		int	oidCount = requestList.size();
		
        SnmpVarBind[] vblist = new SnmpVarBind[oidCount];
        oidMap = getNewOidMap();
        for (int i = 0; i < oidCount ; i++) {
        	SnmpSingleValueRequest	req = requestList.get(i);
	        vblist[i] = new SnmpVarBind(req.getOid());
        }
        
        Map<String, SnmpSingleValueRequest>	resultMap = getNewOidMap();
        
        for(SnmpSingleValueRequest req:getResultList()) {
	    	String	soid = req.getOid();
	    	if(soid.endsWith(".0")) 
				soid = soid.substring(0, soid.length()-2);
	        if( resultMap.put(req.getOid(),req) != null)
	        	log.fatal("Shit");
        }
		return vblist;
	}

	/**
	 * @return the oidMap
	 */
	protected Map<String, SnmpSingleValueRequest> getOidMap() {
		return oidMap;
	}
	
	/**
	 * 
	 * @return
	 */
	protected	Map<String,SnmpSingleValueRequest>	getNewOidMap() {
		this.oidMap=null;
		this.oidMap=new HashMap<String,SnmpSingleValueRequest>(getRequestList().size());
		return oidMap;
	}
	
	/**
	 * 
	 * @return
	 */
	protected	List<SnmpSingleValueRequest>	getRequestList() {
		return super.getRequestResults();
	}
	
	/**
	 * 
	 * @return
	 */
	protected	List<SnmpSingleValueRequest>	getResultList() {
		return super.getRequestResults();
	}
	

}
