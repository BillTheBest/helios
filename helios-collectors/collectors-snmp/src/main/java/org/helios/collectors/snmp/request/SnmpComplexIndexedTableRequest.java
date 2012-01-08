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

package org.helios.collectors.snmp.request;

import java.util.ArrayList;
import java.util.List;

import org.helios.collectors.snmp.SnmpWalkRequester;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: SnmpComplexIndexedTableRequest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SnmpComplexIndexedTableRequest extends AbstractSnmpRequest {

	private	List<SnmpSingleValueRequest> 	filterList=null;
	private	boolean							filtered=false;
	private	int								count=0;

	/**
	 * @param oid
	 * @param renderer
	 */
	public SnmpComplexIndexedTableRequest(String oid,
			AbstractSnmpRenderer renderer,
			List<SnmpSingleValueRequest> filterList) {
		super(oid, renderer);
		this.filterList=filterList;
	}

	/**
	 * Resolve oid templates to actual oid target requests
	 * @param walk
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public	List<SnmpSingleValueRequest> processFilters(SnmpWalkRequester walk) throws Exception{
		List<SnmpSingleValueRequest> result=null;
		if( filtered == false ) {
			result=new ArrayList<SnmpSingleValueRequest>();
			int beginIndex = this.getOid().length();
			List<SnmpVarBind> vars=(List<SnmpVarBind>)walk.getResult();
			count = vars.size();
			for (SnmpVarBind snmpVarBind : vars) {
				String	resultOid = snmpVarBind.getName().toString();
				String	newOid = resultOid.substring(beginIndex, resultOid.length());
				for(SnmpSingleValueRequest filt:filterList) {
					
					result.add(new SnmpSingleValueRequest(filt.getOid()+newOid,filt.getRenderer()));
				}
			}
			filtered = true;
		}
		else
			throw new Exception("Invalid attempt to process filters more than once");
		return result;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @return the filterList
	 */
	public List<SnmpSingleValueRequest> getFilterList() {
		return filterList;
	}
}
