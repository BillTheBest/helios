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

import org.helios.tracing.MetricType;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * <p>Title: AbstractSnmpRequest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Castellucci (fcast@heliosdev.org)
 * @version $LastChangedRevision$
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/snmp/request/AbstractSnmpRequest.java $
 * $Id: AbstractSnmpRequest.java 1724 2009-11-16 16:26:38Z frankc01 $
 */
public abstract class AbstractSnmpRequest {
	
	private	String					oid=null;
	private	AbstractSnmpRenderer	renderer=null;
	private	SnmpVarBind				result=null;
	
	@SuppressWarnings("unused")
	private	AbstractSnmpRequest() {}
	
	public	AbstractSnmpRequest(String oid,AbstractSnmpRenderer renderer) {
		this.oid = oid;
		this.renderer=renderer;
	}
	
	public	String	getOid() {
		return oid;
	}
	
	public	void	setOid(String oid) {
		this.oid=oid;
	}
	
	public	String		getResultName() {
		return result.getName().toString();
	}
	
	public	MetricType		getResultType() {
		return renderer.getMetricType(result.getValue().typeId());
	}
	
	public	String		getResultValue() {
		return renderer.renderValue(result);
	}
	
	public	AbstractSnmpRenderer getRenderer() {
		return this.renderer;
	}
	
	public	void	setResult(SnmpVarBind result) {
		this.result=result;
	}
	public	String	extendOid(String prefix,String value) {
		StringBuffer	sb=new StringBuffer(oid);
		if(prefix != null) sb.append(prefix);
		return sb.append(value).toString();
	}

}
