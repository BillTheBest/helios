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

import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpPduRequest;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: IHeliosSnmpRequester</p>
 * <p>Description: Interface of shared behavior 
 * on all implementation</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/IHeliosSnmpRequester.java $
 * $Id: IHeliosSnmpRequester.java 1632 2009-10-18 03:48:40Z frankc01 $
 */
public interface IHeliosSnmpRequester {
	
	/**
	 * A requester must expose the ability to set a 
	 * result from a handler
	 * @param result from request
	 */
	public	void	setResult(SnmpVarBind result);

	/**
	 * A requester must expose the ability to set
	 * a result for a particular key
	 * @param key
	 * @param result from request
	 */
	public	void	setResultForKey(long key,SnmpVarBind result);
	
	/**
	 * A requester must expose the ability to set a
	 * general error id
	 * @param err
	 */
	public	void	setError(int err);
	
	/**
	 * A requester must expose the ability to set a 
	 * timeout error indicator
	 */
	public	void	setTimeoutError();
	
	/**
	 * Validation test for key
	 * @param key
	 * @return true if valid key for this requester
	 */
	public	boolean	isKeyValid(long key);

	/**
	 * Returns the next request (for GET, not GETNEXT)
	 * @return
	 */
	public	SnmpPduRequest	getRequest();
	
	/**
	 * Get the results of the request, which
	 * are defined by the Requester implementation
	 * @return
	 */
	public	Object	getResult();
	
	/**
	 * Get the number of results
	 * in the requester
	 * @return
	 */
	public	int		getResultCount();
	
	/**
	 * If the request had an error
	 * @return
	 */
	public	boolean	isError();
	
	/**
	 * Error code for request
	 * @return
	 */
	public	int		getErrorCode();

	/**
	 * If request timedout
	 * @return
	 */
	public	boolean isTimeout();
	
	/**
	 * Perform the request
	 * @param session
	 * @return
	 */
	public	Object	executeRequest(SnmpSession session) throws Exception;
}
