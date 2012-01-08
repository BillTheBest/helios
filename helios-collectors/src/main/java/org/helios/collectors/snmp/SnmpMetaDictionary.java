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
import java.util.Map;

import org.apache.log4j.Logger;
import org.helios.tracing.MetricType;
import org.opennms.protocols.snmp.SnmpVarBind;
import org.springframework.util.Assert;

/**
 * <p>Title: SnmpMetaDictionary</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/SnmpMetaDictionary.java $
 * $Id: SnmpMetaDictionary.java 1691 2009-10-31 19:11:04Z frankc01 $
 */
public class SnmpMetaDictionary {
	
	public class OidEntry {
		private	String		s_name=null;
		private	MetricType	type=null;
		private	boolean		hasSymbolic=false;
		
		private	OidEntry(String name,MetricType type) {
			this.type=type;
			this.s_name=name;
		}
		
		public	String getName() {return s_name;}
		public	MetricType	getType(){return type;}
		public	boolean hasSymb() {return hasSymbolic;}
	}
	
	/**	Type map between SMI (ASN) and Helios Types	*/
	private	Map<Byte,Integer>	snmpToHeliosTypeMap=null;
	
	/** Type map between Oid and Symbolic MIB name	*/
	private	Map<String,String>		snmpOidToMibNameMap=null;
	
	/** Dictionary Entry Map	*/
	private	boolean					alive=false;
	
	private	static	SnmpMetaDictionary	instance=null;
	protected static Logger log = Logger.getLogger(SnmpMetaDictionary.class);
	
	/** Some well known OIDs			*/
	public	static final	String	MIB_2_MIB= ".1.3.6.1.2.1";		// iso.org.dod.internet.mgmt.mib-2
	public	static final	String	SYSTEM_MIB=MIB_2_MIB+".1";		// iso.org.dod.internet.mgmt.mib-2.system	
	public	static final	String	INTERFACES_MIB=MIB_2_MIB+".2";	// iso.org.dod.internet.mgmt.mib-2.interfaces	
	public	static final	String	AT_MIB=MIB_2_MIB+".3";			// iso.org.dod.internet.mgmt.mib-2.at	
	public	static final	String	IP_MIB=MIB_2_MIB+".4";			// iso.org.dod.internet.mgmt.mib-2.ip
	public	static final	String	ICMP_MIB=MIB_2_MIB+".5";		// iso.org.dod.internet.mgmt.mib-2.icmp
	public	static final	String	TCP_MIB=MIB_2_MIB+".6";			// iso.org.dod.internet.mgmt.mib-2.tcp
	public	static final	String	UDP_MIB=MIB_2_MIB+".7";			// iso.org.dod.internet.mgmt.mib-2.udp
	public	static final	String	SNMP_MIB=MIB_2_MIB+".11";		// iso.org.dod.internet.mgmt.mib-2.snmp

	/** Some lesser used or vendor dependent	*/
	public	static final	String	EGP_MIB=MIB_2_MIB+".8";			// iso.org.dod.internet.mgmt.mib-2.egp
	
	public	static final	String	HOST_MIB=MIB_2_MIB+".25";		// iso.org.dod.internet.mgmt.mib-2.host
	public	static final	String	HR_SYSTEM_MIB=HOST_MIB+".1";	// iso.org.dod.internet.mgmt.mib-2.host.hrSystem
	public	static final	String	HR_STORAGE_MIB=HOST_MIB+".2";	// iso.org.dod.internet.mgmt.mib-2.host.hrStorage
	public	static final	String	HR_DEVICE_MIB=HOST_MIB+".3";	// iso.org.dod.internet.mgmt.mib-2.host.hrDevice
	public	static final	String	HR_SWRUN_MIB=HOST_MIB+".4";		// iso.org.dod.internet.mgmt.mib-2.host.hrSWRun
	public	static final	String	HR_SWRUNPERF_MIB=HOST_MIB+".5";	// iso.org.dod.internet.mgmt.mib-2.host.hrSWRunPerf
	public	static final	String	HR_SWINSTALLED_MIB=HOST_MIB+".6";// iso.org.dod.internet.mgmt.mib-2.host.hrSWInstalled
	public	static final	String	HR_MIBADMININFO_MIB=HOST_MIB+".7";// iso.org.dod.internet.mgmt.mib-2.host.hrMIBAdminInfo

	/** Verification OID */
	public	static final	String	SYSTEM_MIB_SYSDESC= SYSTEM_MIB+".1.0"; // system.sysDescr
	
	
	public	SnmpMetaDictionary(Map<Byte,Integer> typeMap) {
		Assert.notEmpty(typeMap);
		this.snmpToHeliosTypeMap = typeMap;
		this.alive=true;
		if(instance == null) instance=this;
		for(Byte key:this.snmpToHeliosTypeMap.keySet()) 
			log.debug("SNMP Key ["+key+"] maps to Helios Type ["+snmpToHeliosTypeMap.get(key)+"]");
	}
	
	public boolean	isAlive() {
		return this.alive;
	}
	
	public static OidEntry getOidEntryFor(SnmpVarBind var) {
		return instance.getEntryFor(var);
	}
	public static Integer getHeliosIntegerType(byte in) {
		return instance.getHeliosType(in);
	}
	
	public	static MetricType	getMetricTypeFor(SnmpVarBind var) {
		return MetricType.typeForCode(instance.getHeliosType(var.getValue().typeId()));
	}
	private	OidEntry	getEntryFor(SnmpVarBind var) {
		return new OidEntry(var.getName().toString(),
				MetricType.typeForCode(getHeliosType(var.getValue().typeId())));
	}
	
	private	Integer	getHeliosType(byte in) {
		return snmpToHeliosTypeMap.get(in);
	}

}
