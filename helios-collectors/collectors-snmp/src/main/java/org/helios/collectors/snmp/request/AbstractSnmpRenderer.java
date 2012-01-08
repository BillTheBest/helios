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

import org.helios.ot.type.MetricType;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: AbstractSnmpRenderer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/request/AbstractSnmpRenderer.java $
 * $Id: AbstractSnmpRenderer.java 1671 2009-10-28 15:30:06Z frankc01 $
 */
public abstract class AbstractSnmpRenderer {
	protected	MetricType			metricType = null;
	
	public	AbstractSnmpRenderer() {
		
	}
	
	public	abstract MetricType getMetricType();
	public	abstract MetricType	getMetricType(byte in);
	
	public	abstract String	renderValue(SnmpVarBind var);	
		
}
