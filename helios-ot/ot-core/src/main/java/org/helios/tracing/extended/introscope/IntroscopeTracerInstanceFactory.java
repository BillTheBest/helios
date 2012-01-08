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
package org.helios.tracing.extended.introscope;

import java.lang.management.ManagementFactory;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.ITracer;
import org.helios.tracing.ITracerInstanceFactory;
import org.helios.tracing.TracerInstanceFactoryException;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;
/**
 * <p>Title: IntroscopeTracerInstanceFactory</p>
 * <p>Description: A ITracerInstanceFactory that creates an <code>IntroscopeTracerAdapter</code> concrete implementation.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1647 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/extended/introscope/IntroscopeTracerInstanceFactory.java $
 * $Id: IntroscopeTracerInstanceFactory.java 1647 2009-10-24 21:52:31Z nwhitehead $
 */
public class IntroscopeTracerInstanceFactory extends AbstractTracerInstanceFactory implements ITracerInstanceFactory {
	/** Class instance logger */
	protected static final Logger log = Logger.getLogger(IntroscopeTracerInstanceFactory.class);
	/** The location of the Introscope Agent jar */
	protected String agentLib = null;
	/** Indicates if the trace's originating host name should be included in the Introscope name space */
	protected boolean includeTraceHost = true;
	
	
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		log.info("BasicTest");
		
		System.setProperty("introscope.agent.enterprisemanager.transport.tcp.host.DEFAULT", "njw810");
		System.setProperty("introscope.agent.enterprisemanager.transport.tcp.port.DEFAULT", "5001");
		System.setProperty("introscope.agent.agentName", "HeliosAgent");
		System.setProperty(IntroscopeAdapterConfiguration.AGENT_JAR, "/home/nwhitehead/introscope/9.0/agent/wily/Agent.jar");
//		org.helios.application.name:Undefined
		System.setProperty("introscope.agent.defaultProcessName", ManagementFactory.getRuntimeMXBean().getName());
//		org.helios.process.name:Undefined
		IntroscopeTracerInstanceFactory factory = new IntroscopeTracerInstanceFactory();
		ITracer tracer = factory.getTracer();
		Random random = new Random(System.nanoTime());
		while(true) {
			tracer.trace(random.nextInt(100), "Earth", "Solar System", "Milky Way");
			tracer.trace(random.nextInt(100), "Jupiter", "Solar System", "Milky Way");
			tracer.trace(random.nextInt(100), "Venus", "Solar System", "Milky Way");
			try { Thread.sleep(1000); } catch (Exception e) {}
		}

		
	}
	
	
	/**
	 * Config:
	 * 1. Agent.jar location
	 * 2. Profile 
	 * 3. Agent Name
	 * 4. Process Name
	 * 
	 */
	
	/**
	 * Returns a configured tracer factory.
	 * @param agentLib The location of the Introscope Agent Library.
	 * @param includeTraceHost Indicates if the trace's originating host name should be included in the Introscope name space
	 * @param returnTraces Defines if the tracer will return traces.
	 */
	public IntroscopeTracerInstanceFactory(IntroscopeAdapterConfiguration config) {
		super(new IntroscopeTracingBridge(config));		
	}


	
	/**
	 * Creates a new IntroscopeTracerInstanceFactory with no agent library.
	 */
	public IntroscopeTracerInstanceFactory() {
		this(new IntroscopeAdapterConfiguration());
	}	
	

	
	
	
	
	
	
	
	
	
	
	


	/**
	 * @return the agentLib
	 */
	public String getAgentLib() {
		return agentLib;
	}


	/**
	 * @param agentLib the agentLib to set
	 */
	public void setAgentLib(String agentLib) {
		this.agentLib = agentLib;
	}


	/**
	 * Indicates if the trace's originating host name should be included in the Introscope name space
	 * @return the includeTraceHost
	 */
	public boolean isIncludeTraceHost() {
		return includeTraceHost;
	}


	/**
	 * Indicates if the trace's originating host name should be included in the Introscope name space
	 * @param includeTraceHost the includeTraceHost to set
	 */
	public void setIncludeTraceHost(boolean includeTraceHost) {
		this.includeTraceHost = includeTraceHost;
	}


	/**
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... intervalTraces) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * @param traces
	 */
	@Override
	public void submitTraces(Trace... traces) {
		if(traces!=null) {
			this.bridge.submitTraces(traces);
		}		
	}








}
