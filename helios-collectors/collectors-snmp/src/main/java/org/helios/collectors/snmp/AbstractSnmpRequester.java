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

import org.opennms.protocols.snmp.SnmpPduRequest;

/**
 * <p>Title: AbstractSnmpRequester</p>
 * <p>Description: Abstraction of Snmp Requester, 
 * extensions must implement execution methods</p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/AbstractSnmpRequester.java $
 * $Id: AbstractSnmpRequester.java 1631 2009-10-14 12:21:31Z frankc01 $
 */
public abstract class AbstractSnmpRequester implements IHeliosSnmpRequester {

	private		int					errorCode=0;
	private		boolean				error=false;
	private		boolean				timeoutError=false;
	private		AbstractSnmpHandler	handler=null;

	/**
	 * Constructor
	 */
	public	AbstractSnmpRequester() {
	}
	
	/**
	 * Sets the handler for this requester
	 * @param handler
	 */
	protected	void	setHandler(AbstractSnmpHandler handler) {
		this.handler=handler;
	}
	
	/**
	 * Gets the handler assigned to this requester
	 * @return handler
	 */
	protected	AbstractSnmpHandler	getHandler() {
		return this.handler;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#getErrorCode()
	 */
	//@Override
	public int getErrorCode() {
		return errorCode;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#isError()
	 */
	//@Override
	public boolean isError() {
		return error;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#isTimeout()
	 */
	//@Override
	public boolean isTimeout() {
		return timeoutError;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#setError(int)
	 */
	//@Override
	public void setError(int err) {
		errorCode = err;
		error = true;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#setTimeoutError()
	 */
	//@Override
	public void setTimeoutError() {
		timeoutError = true;
	}

	/* (non-Javadoc)
	 * @see org.helios.snmp.IHeliosSnmpRequester#getRequest()
	 */
	//@Override
	public SnmpPduRequest getRequest() {
		return null;
	}

}
