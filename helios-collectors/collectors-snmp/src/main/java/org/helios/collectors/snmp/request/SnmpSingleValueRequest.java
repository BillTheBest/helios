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

/**
 * <p>Title: SnmpSingleValueRequest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/request/SnmpSingleValueRequest.java $
 * $Id: SnmpSingleValueRequest.java 1666 2009-10-27 10:18:20Z frankc01 $
 */
public class SnmpSingleValueRequest extends AbstractSnmpRequest {

	/**
	 * @param oid
	 * @param renderer
	 */
	public SnmpSingleValueRequest(String oid, AbstractSnmpRenderer renderer) {
		super(oid, renderer);
	}

}
