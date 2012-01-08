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
package org.helios.tracing.extended.logging;

import java.util.concurrent.Executor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.tracing.AbstractTracerInstanceFactory;
import org.helios.tracing.ITracerInstanceFactory;
import org.helios.tracing.bridge.ITracingBridge;
import org.helios.tracing.interval.IIntervalTrace;
import org.helios.tracing.trace.Trace;
import org.kohsuke.args4j.Option;

/**
 * <p>Title: Log4JTracerInstanceFactory</p>
 * <p>Description: A factory for Log4JTracers.</p>
 * <p>Tracing level mapping to logger level:<ul>
 * 		<li><b>OFF, FATAL</b>: Nothing</li>
 * 		<li><b>WARN</b>: Currently nothing, to be extended for drops etc.</li>
 * 		<li><b>INFO</b>: Logs trace and trace interval toStrings.</li>
 * 		<li><b>DEBUG</b>: Logs trace and trace interval toStrings.</li>
 * 		<li><b>TRACE</b>: Logs trace and trace interval toStrings. To be extended.</li>
 * </ul></p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1058 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/extended/logging/Log4JTracerInstanceFactory.java $
 * $Id: Log4JTracerInstanceFactory.java 1058 2009-02-18 17:33:54Z nwhitehead $
 * <p><code>org.helios.tracing.extended.logging.Log4JTracerInstanceFactory</code></p>
 */
public class Log4JTracerInstanceFactory extends AbstractTracerInstanceFactory implements  ITracingBridge, ITracerInstanceFactory {
	/** The tracer specific logger level */
	protected Level level = Level.INFO;
	/** Class instance logger */
	protected Logger log = Logger.getLogger("org.helios.tracing." + getClass().getSimpleName());
	
	public static Log4JTracerInstanceFactory getInstance() {
		return _getInstance(Log4JTracerInstanceFactory.class);
	}
	
	public static Log4JTracerInstanceFactory getInstance(String...configuration) {
		return _getInstance(Log4JTracerInstanceFactory.class, configuration);
	}
	
	public static Log4JTracerInstanceFactory getForcedInstance(String...configuration) {
		return _getForcedInstance(Log4JTracerInstanceFactory.class, configuration);
	}

	/**
	 * Creates a new default configuration tracer instance factory.
	 */
	public Log4JTracerInstanceFactory() {
		super();
	}
	
	
	

	/**
	 * @return the level
	 */
	public String getLevel() {
		return level.toString();
	}


	/**
	 * Sets the level of the tracer's logger
	 * @param level the level to set
	 */
	@Option(name="-level", usage="Sets the level of the tracer that will be created")
	public void setLevel(String level) {
		this.level = Level.toLevel(level);
		log.setLevel(this.level);
	}




	/**
	 * @param intervalTraces
	 */
	@Override
	public void submitIntervalTraces(IIntervalTrace... intervalTraces) {
		if(level.isGreaterOrEqual(Level.WARN)) return;
		if(intervalTraces!=null) {
			for(IIntervalTrace it: intervalTraces) {
				log.debug(it);
			}				
		}
	}




	/**
	 * @param traces
	 */
	@Override
	public void submitTraces(Trace... traces) {
		if(level.isGreaterOrEqual(Level.WARN)) return;
		if(traces!=null) {
			for(Trace it: traces) {
				log.log(level, it);
			}				
		}
	}




	/**
	 * @return
	 */
	@Override
	public String getEndPointName() {		
		return "Log4J:" + log.getName();
	}




	/**
	 * @return
	 */
	@Override
	public Executor getExecutor() {
		return this.executor;
	}




	/**
	 * @return
	 */
	@Override
	public boolean isIntervalCapable() {
		return true;
	}




	/**
	 * @return
	 */
	@Override
	public boolean isStateful() {
		return false;
	}









}
