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

import org.helios.collectors.snmp.request.SnmpSingleValueRequest;

/**
 * <p>Title: SnmpExtendedBulkRequester</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SnmpExtendedBulkRequester extends SnmpBulkGetRequester {

	private	int									repeatCount=0;
	private	List<SnmpSingleValueRequest>		baseOids=null;
	private	Map<String, SnmpSingleValueRequest> mapOfResults=null;
	
	/**
	 * @param requests
	 */
	public SnmpExtendedBulkRequester(int repeatCount,List<SnmpSingleValueRequest> prototype,
			List<SnmpSingleValueRequest> requests) {
		super(requests);
		this.repeatCount=repeatCount;
		this.baseOids=prototype;
	}

	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.SnmpBulkGetRequester#getMaxRepitition()
	 */
	@Override
	public int getMaxRepitition() {
		return repeatCount;
	}

	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.SnmpBulkGetRequester#getNewOidMap()
	 */
	@Override
	protected Map<String, SnmpSingleValueRequest> getNewOidMap() {
		this.mapOfResults=null;
		this.mapOfResults=new HashMap<String,SnmpSingleValueRequest>(getResultList().size());
		return mapOfResults;
	}

	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.SnmpBulkGetRequester#getOidMap()
	 */
	@Override
	protected Map<String, SnmpSingleValueRequest> getOidMap() {
		return mapOfResults;
	}

	/* (non-Javadoc)
	 * @see org.helios.collectors.snmp.SnmpBulkGetRequester#getRequestList()
	 */
	@Override
	protected List<SnmpSingleValueRequest> getRequestList() {
		return this.baseOids;
	}

}
