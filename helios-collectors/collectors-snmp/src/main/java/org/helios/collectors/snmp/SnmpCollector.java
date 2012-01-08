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

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.helios.collectors.AbstractCollector;
import org.helios.collectors.CollectionResult;
import org.helios.collectors.exceptions.CollectorStartException;
import org.helios.collectors.snmp.request.AbstractSnmpRequest;
import org.helios.collectors.snmp.request.SnmpDefaultRenderer;
import org.helios.collectors.snmp.request.SnmpSingleValueRequest;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.ot.subtracer.VirtualTracer;
import org.helios.ot.type.MetricType;
import org.opennms.protocols.snmp.SnmpPeer;
import org.opennms.protocols.snmp.SnmpSMI;
import org.opennms.protocols.snmp.SnmpSession;

/**
 * <p>Title: SnmpCollector</p>
 * <p>Description: Implementation of Snmp Collector</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/SnmpCollector.java $
 * $Id: SnmpCollector.java 1724 2009-11-16 16:26:38Z frankc01 $
 */
@JMXManagedObject (declared=false, annotated=true)
public class SnmpCollector extends AbstractCollector {

	/**
	 * Used for versioning
	 */
	private static final long serialVersionUID = 1L;

	/** SNMP collector version */
	private static final String SNMP_COLLECTOR_VERSION="0.1";
	
	/** OID configuration (get, walk, etc.) from xml */
	private	List<AbstractSnmpRequest>		oidConfiguration;
	
	/** Boolean determines if we've done a first pass	*/
	private	boolean				firstPass=false;
	protected Logger log = Logger.getLogger(getClass());
	
	/** Collection of OID's configured to scan */
	private	SnmpCollectDefinition	walker = null;
	/** JoeSNMP session entities	*/
	private	SnmpSession				session;
	/**	Host name to collect from	*/
	private		String			hostName="localhost";
	/** Host InetAddress resolved from name	*/
	private  	InetAddress 	ipAddress = null;
	/**	SNMP Port agent is listening on	*/
	private		int				port=161;
	/** SNMP (1,2,2c) community name	*/
	private		String			communityName="public";		 	// -c
	/** SNMP version					*/
	private		int				snmpVersion= SnmpSMI.SNMPV1; 	// -v
	/** SNMP Agent ping oid				*/
	private		String			pingOid=null;
	private		boolean			connected=false;
	
	/**	Helios Virtual Tracer for this particular host	*/
	private	VirtualTracer vtracer = null;
	/* (non-Javadoc)
	 * @see org.helios.collectors.AbstractCollector#startCollector()
	 */
	@Override
	public void startCollector() throws CollectorStartException {
		log.info("Starting SnmpCollector");
		
		//	If version isn't correct
		if( snmpVersion == 1 )
			snmpVersion = SnmpSMI.SNMPV1;	// default
		else if( snmpVersion == 2 )
			snmpVersion = SnmpSMI.SNMPV2;
		else
			throw new CollectorStartException("Invalid SNMP version specified");
		
		
		// Convert host name to IP if needed
		try {
			this.ipAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new CollectorStartException(e.getLocalizedMessage());
		}
		
		if(oidConfiguration != null)
			log.info("Have Valid oidConfiguration with ["+oidConfiguration.size()+"] elements.");
		else {
			log.error("Non-existent oidConfiguration");
			throw new CollectorStartException("Missing SNMP-Collector OID Configuration settings.");
		}
		// Construct the definition
		try {
			walker = new SnmpCollectDefinition(snmpVersion,oidConfiguration);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CollectorStartException(e.getLocalizedMessage());
		}
		
		//	Establish session and peer
		try {
			session = new SnmpSession(getPeer());
			String	vHost = getHostName();
			String	vAgent=":"+Integer.toString(getPort())+"-SNMP";
			log.info("Starting vTracer Using vHost ["+vHost+"] with vAgent ["+vAgent+"]");
			vtracer =   (VirtualTracer)tracer.getVirtualTracer(
					vHost,
					vAgent);
			
			log.info(vtracer.getTracerName()+" start successful");
		} catch (SocketException e) {
			e.printStackTrace();
			throw new CollectorStartException(e.getLocalizedMessage());
		}
		
		// Test the connectivity of an agent with a single shot get
		
		if( performOneGet() == null) {
			session.close();
			log.error("Closing session due to startup failure");
			throw new CollectorStartException("Unable to contact agent at ["
					+getHostName()+"]");
		}
		
		log.info("Agent ["+getHostName()+"] responded successfully");
		connected=true;
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.helios.collectors.AbstractCollector#collectCallback()
	 */
	@Override
	public CollectionResult collectCallback() {
		CollectionResult result = new CollectionResult();
		long	t1 = System.currentTimeMillis();
		if(connected == true) {
			try {
				if(log.isDebugEnabled())log.debug("Executing SNMP collect ... for "+getHostName());
				for(IHeliosSnmpRequester req:walker.getRequesters())
						req.executeRequest(session);
				log.info("["+getHostName()+"] completed collect scan in ["+(System.currentTimeMillis()-t1)+"] ms");
				
				if(log.isDebugEnabled())log.debug("Executing SNMP trace ...");
				for(IHeliosSnmpRequester req:walker.getRequesters()) {
					if(req instanceof SnmpGetRequester) {
						if(log.isTraceEnabled())log.debug("Tracing get requests ");
						traceGets((SnmpGetRequester)req);
					}
					else if(req instanceof SnmpWalkRequester) {
						if(log.isTraceEnabled())log.trace("Tracing walk requests ");
						traceWalks((SnmpWalkRequester)req);
					}
				}
				result.setResultForLastCollection(CollectionResult.Result.SUCCESSFUL);
				log.info("["+getHostName()+"] completed work in ["+(System.currentTimeMillis()-t1)+"] ms");
				if( firstPass==false) {
					try {
						if(log.isDebugEnabled())log.debug("Executing SNMP first pass ... for "+getHostName());
						walker.processFirstTime(session);
					}
					catch (Exception e) {
						e.printStackTrace();
						
					}
					finally {
						firstPass=true;
					}
				}

			}
			catch(Exception ex) {
				ex.printStackTrace();
				result.setResultForLastCollection(CollectionResult.Result.FAILURE);
				result.setAnyException(ex);
			}
		}
		else
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.helios.collectors.AbstractCollector#getCollectorVersion()
	 */
	@Override
	public String getCollectorVersion() {
		return SNMP_COLLECTOR_VERSION;
	}

	/* (non-Javadoc)
	 * @see org.helios.collectors.AbstractCollector#destroyCollector()
	 */
	@Override
	public void destroyCollector() {
		if(session != null && session.isClosed()==false)
			session.close();
	}

	/**
	 * traceGets knows about the snmpget requester
	 * @param req
	 */
	private	void	traceGets(SnmpGetRequester req) {
		for(SnmpSingleValueRequest reqs:req.getRequestResults()) {
			MetricType	mt = reqs.getResultType();
			String		val = reqs.getResultValue();
			if(log.isDebugEnabled())log.debug(
					"Host ["+getHostName()+"] trace type ["+mt.toString()
					+"] for ["+reqs.getOid()
					+"] has value ["+val+"]");
			vtracer.smartTrace(mt,
					val,
					reqs.getOid(),
					this.getTracingNameSpace());
		}
	}
	
	private	void	traceWalks(SnmpWalkRequester req) {
		
	}
	/**
	 * Executes a singular get to the agent, returning the retrieved result
	 * @param Oid
	 * @return
	 */
	private	Object	performOneGet() {
		Object	result=null;
		String	oid = (pingOid == null ? SnmpMetaDictionary.SYSTEM_MIB_SYSDESC : pingOid);
		log.info("Testing host ["+getHostName()+"] for agent with oid ["+oid+"]");
		List<SnmpSingleValueRequest> oids = new ArrayList<SnmpSingleValueRequest>(1);
		oids.add(new SnmpSingleValueRequest(oid,new SnmpDefaultRenderer()));
		IHeliosSnmpRequester req = new SnmpGetRequester(oids);
		try {
			result = req.executeRequest(session);
		} catch (Exception e) {
			e.printStackTrace();
			result=null;
		}
		return result;
	}

	/**
	 * Gets the peer instance for the host
	 * @return
	 */
	public SnmpPeer	getPeer() {
		SnmpPeer	peer = new SnmpPeer(this.getIpAddress());
		peer.setPort(this.getPort());
		peer.setRetries(1);
		peer.setTimeout(1000);
        peer.getParameters().setVersion(this.getSnmpVersion()); 
        peer.getParameters().setReadCommunity(this.getCommunityName()); 
		return peer;
	}
	
	
	/**
	 * @param oidConfiguration the oidConfiguration to set
	 */
	public void setOidConfiguration(List<AbstractSnmpRequest> oidConfiguration) {
		this.oidConfiguration = oidConfiguration;
	}
	/**
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * @param hostName the hostName to set
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * @return the ipAddress
	 */
	public InetAddress getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the communityName
	 */
	public String getCommunityName() {
		return communityName;
	}

	/**
	 * @param communityName the communityName to set
	 */
	public void setCommunityName(String communityName) {
		this.communityName = communityName;
	}

	/**
	 * @return the snmpVersion
	 */
	public int getSnmpVersion() {
		return snmpVersion;
	}

	/**
	 * @param snmpVersion the snmpVersion to set
	 */
	public void setSnmpVersion(int snmpVersion) {
		this.snmpVersion = snmpVersion;
	}

	/**
	 * @param pingOid the pingOid to set
	 */
	public void setPingOid(String pingOid) {
		this.pingOid = pingOid;
	}


}
