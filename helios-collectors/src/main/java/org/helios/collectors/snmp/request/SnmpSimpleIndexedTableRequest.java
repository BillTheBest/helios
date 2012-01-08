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

import org.apache.log4j.Logger;

/**
 * <p>Title: SnmpSimpleIndexedTableRequest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/request/SnmpSimpleIndexedTableRequest.java $
 * $Id: SnmpSimpleIndexedTableRequest.java 1724 2009-11-16 16:26:38Z frankc01 $
 */
public class SnmpSimpleIndexedTableRequest extends AbstractSnmpRequest {

	private	List<SnmpSingleValueRequest>	filterList;
	private	boolean							filtered=false;
	private	int								count=0;
	protected 	Logger 						log = Logger.getLogger(getClass());
	/**
	 * @param oid
	 * @param renderer
	 */
	public SnmpSimpleIndexedTableRequest(String indexOid,
			AbstractSnmpRenderer renderer,
			List<SnmpSingleValueRequest> filterList) {
		super(indexOid, renderer);
		this.filterList=filterList;
	}
	
	/**
	 * Processes simple filtered list of elements in range 1-indexCount
	 * @param indexCount
	 * @return
	 * @throws Exception
	 */
	public	List<SnmpSingleValueRequest> processFilters(String indexCount) throws Exception{
		List<SnmpSingleValueRequest> result=null;
		if(filtered == false) {
			result = new ArrayList<SnmpSingleValueRequest>();
			count = Integer.decode(indexCount).intValue();
			for(int y=1;y<=count;++y) 
				for(SnmpSingleValueRequest svr:filterList) {
					String	newOid = svr.extendOid(".", Integer.toString(y));
					AbstractSnmpRenderer ar = svr.getRenderer();
					log.debug("Creating new request with oid = ["+newOid+"] and renderer = ["+ar+"]");
					result.add(new SnmpSingleValueRequest(newOid,ar));
				}
			filtered=true;
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
