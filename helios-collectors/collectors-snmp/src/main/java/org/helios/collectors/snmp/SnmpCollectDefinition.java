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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.helios.collectors.snmp.request.AbstractSnmpRequest;
import org.helios.collectors.snmp.request.SnmpComplexIndexedTableRequest;
import org.helios.collectors.snmp.request.SnmpDefaultRenderer;
import org.helios.collectors.snmp.request.SnmpSimpleIndexedTableRequest;
import org.helios.collectors.snmp.request.SnmpSingleValueRequest;
import org.opennms.protocols.snmp.SnmpSMI;
import org.opennms.protocols.snmp.SnmpSession;

/**
 * <p>Title: SnmpHostDefinition</p>
 * <p>Description: A serializable object that defines
 * the information necessary for SNMP external polling</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/SnmpCollectDefinition.java $
 * $Id: SnmpCollectDefinition.java 1724 2009-11-16 16:26:38Z frankc01 $
 */
public class SnmpCollectDefinition implements java.io.Serializable{
	
	/**
	 * <p>Title: ComplexSnmpPackage</p>
	 * <p>Description: Carrier for post startup processing</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Castellucci (fcast@heliosdev.org)
	 */
	private	class	SnmpPackage {
		protected	SnmpWalkRequester				walkRequester=null;
		protected	SnmpComplexIndexedTableRequest	ctableRequest=null;
		protected	SnmpGetRequester				getRequester=null;
		protected	SnmpSimpleIndexedTableRequest	stableRequest=null;
		public	SnmpPackage(SnmpWalkRequester wr,SnmpComplexIndexedTableRequest ctr) {
			this.walkRequester=wr;
			this.ctableRequest=ctr;
		}
		public	SnmpPackage(SnmpGetRequester gr,SnmpSimpleIndexedTableRequest str) {
			this.getRequester=gr;
			this.stableRequest=str;
		}
	}
	/**
	 * We use our own code for serializable if we need object
	 * migration on the fly
	 */
	private static final long serialVersionUID = 1L;
	/** Collection of OID signatures	*/
	private		Map<String,IHeliosSnmpRequester> oidList=new HashMap<String,IHeliosSnmpRequester>();
	private		List<SnmpPackage>	complexList=null;
	private		List<SnmpPackage>	simpleList=null;
	private		int			currentSnmpVersion=0;
	private 	Logger log = Logger.getLogger(getClass());
	
	/**
	 * Basic constructor
	 */
	protected SnmpCollectDefinition() {
		super();
	}

	public	SnmpCollectDefinition(int snmpVersion,List<AbstractSnmpRequest> targets) throws Exception{
		this();
		this.currentSnmpVersion = snmpVersion;
		
		List<SnmpSingleValueRequest> bulkSVR = new ArrayList<SnmpSingleValueRequest>();
		
		// Complete proper requester settings
		for(AbstractSnmpRequest target:targets) {
			if(target instanceof SnmpSingleValueRequest) {
				if(target.getOid().endsWith(".0") == false &&
						snmpVersion==SnmpSMI.SNMPV1)
					target.setOid(target.extendOid(null,".0"));
				bulkSVR.add((SnmpSingleValueRequest)target);
			}
			else if(target instanceof SnmpSimpleIndexedTableRequest) {
				SnmpSimpleIndexedTableRequest simp= (SnmpSimpleIndexedTableRequest)target;
				log.info("Have Simple Index Table Request for ["+simp.getOid()+"]");
				if(simpleList == null)
					simpleList = new ArrayList<SnmpPackage>();
				List<SnmpSingleValueRequest>	ol = new ArrayList<SnmpSingleValueRequest>(1);
				ol.add(new SnmpSingleValueRequest(simp.getOid(),new SnmpDefaultRenderer()));
				simpleList.add(new SnmpPackage(new SnmpGetRequester(ol),simp));
			}
			else if(target instanceof SnmpComplexIndexedTableRequest) {
				SnmpComplexIndexedTableRequest cimp = (SnmpComplexIndexedTableRequest)target;
				log.info("Have Complex Index Table Request for ["+cimp.getOid()+"]");
				if(complexList == null) 
					complexList = new ArrayList<SnmpPackage>();
				complexList.add(new SnmpPackage(new SnmpWalkRequester(cimp.getOid()),cimp));
			}
		}
		
		if(bulkSVR.size()>0) {
			if(snmpVersion==SnmpSMI.SNMPV1) {
				log.debug("Building V1 BULK GET requester with ["+bulkSVR.size()+"] oids");
				oidList.put("BULKGET", new SnmpGetRequester(bulkSVR));
			}
			else if(snmpVersion==SnmpSMI.SNMPV2) {
				log.debug("Building V2 BULK GET requester with ["+bulkSVR.size()+"] oids");
				oidList.put("BULKGET", new SnmpBulkGetRequester(bulkSVR));
			}
		}
		
	}
	
	/**
	 * 
	 * @param session
	 * @throws Exception
	 */
	public	void	processFirstTime(SnmpSession session) throws Exception{
		log.debug("Extending Table Processing");
		try {
			if( simpleList != null && simpleList.size()>0) {
				log.info("Processing Simple Table Indexes");
				int	count=0;
				for(SnmpPackage ssp:simpleList) {
					ssp.getRequester.executeRequest(session);
					String	val = ssp.getRequester.getRequestResults().get(0).getResultValue();
					List<SnmpSingleValueRequest> lr =ssp.stableRequest.processFilters(val);
					if(currentSnmpVersion == SnmpSMI.SNMPV1) {
						SnmpGetRequester gr = (SnmpGetRequester)oidList.get("BULKGET");
						for(SnmpSingleValueRequest ssvr:lr) {
							if(log.isDebugEnabled())log.debug("Adding ["+ssvr.getOid()+"] to get set");
							++count;
							gr.addOid(ssvr);
						}
					}
					else if(currentSnmpVersion==SnmpSMI.SNMPV2) {
						count = ssp.stableRequest.getCount();
						SnmpExtendedBulkRequester sebr = 
							new SnmpExtendedBulkRequester(count,ssp.stableRequest.getFilterList(),lr);
						oidList.put("BULKSIMPLETABLE", sebr);
					}
					else {
						throw new RuntimeException("Invalid version specification");
					}
				}
				log.info("Get Set extended by ["+count+"] oids successfully");
				if(log.isDebugEnabled())log.debug("Freeing simple index list");
				simpleList.clear();
				simpleList=null;
			}
			if( complexList != null && complexList.size() > 0 ) {
				int	count = 0;
				log.info("Processing Complex Table Indexes");
				for(SnmpPackage csp:complexList) {
					csp.walkRequester.executeRequest(session);
					List<SnmpSingleValueRequest> lr=csp.ctableRequest.processFilters(csp.walkRequester);
					if(currentSnmpVersion == SnmpSMI.SNMPV1) {
						SnmpGetRequester gr = (SnmpGetRequester)oidList.get("BULKGET");
						for(SnmpSingleValueRequest ssvr:lr) {
							if(log.isDebugEnabled())log.debug("Adding ["+ssvr.getOid()+"] to get set");
							++count;
							gr.addOid(ssvr);
						}
					}
					else if(currentSnmpVersion==SnmpSMI.SNMPV2) {
						count = csp.ctableRequest.getCount();
						SnmpExtendedBulkRequester sebr =
							new SnmpExtendedBulkRequester(count,csp.ctableRequest.getFilterList(),lr);
						oidList.put("BULKCOMPLEXTABLE", sebr);
					}
					else {
						throw new RuntimeException("Invalid version specification");
					}
				}
				log.info("Get Set extended by ["+count+"] oids successfully");
				if(log.isDebugEnabled())log.debug("Freeing complex index list");
				complexList.clear();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public	Collection<IHeliosSnmpRequester> getRequesters() {
		return this.oidList.values();
	}
	
}
