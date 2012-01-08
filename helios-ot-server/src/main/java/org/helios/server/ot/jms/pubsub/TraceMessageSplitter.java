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
package org.helios.server.ot.jms.pubsub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.camel.Body;
import org.apache.log4j.Logger;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.jmxenabled.counters.RollingCounter;
import org.helios.ot.trace.ClosedTrace;
import org.helios.ot.trace.Trace;
import org.helios.time.SystemClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: TraceMessageSplitter</p>
 * <p>Description: A message splitter </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.jms.pubsub.TraceMessageSplitter</code></p>
 */
@ManagedResource(objectName="org.helios.server.ot.jms.pubsub:service=TraceMessageSplitter")
public class TraceMessageSplitter  {
	/** The host cache */
	@Autowired(required=true)
	@Qualifier("hostCache")
	protected Cache hostCache;
	/** The agent cache */
	@Autowired(required=true)
	@Qualifier("agentCache")	
	protected Cache agentCache;
	
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass());

	/** Rolling counter register group */
	protected final Map<String, RollingCounter> metricGroup = new HashMap<String, RollingCounter>();
	
	/** Elapsed Time Rolling Counter */
	protected final LongRollingCounter elapsedTimeCounter = new LongRollingCounter("ElapsedTime", 10, metricGroup);
	/** Total exchanges */
	protected final AtomicLong exchangeCount = new AtomicLong(0);
	
	/**
	 * Returns the last elapsed time in ms.
	 * @return the last elapsed time in ms.
	 */
	@ManagedAttribute(description="The last elapsed time in ms.")
	public long getLastElapsedTime() {
		return elapsedTimeCounter.getLastValue();
	}
	
	/**
	 * Returns the average elapsed time in ms.
	 * @return the average elapsed time in ms.
	 */
	@ManagedAttribute(description="The average elapsed time in ms.")
	public long getAverageElapsedTime() {
		return elapsedTimeCounter.getAverage();
	}
	
	
	/**
	 * Splits a inbound agent trace message into a list of individual traces for republication
	 * @param payload The Object payload from an inbound exchange
	 * @return a (possibly empty) list of traces.
	 */
	public List<ClosedTrace> split(@Body Object payload) {		
		try {
			if(payload==null || !(payload instanceof Trace[])) return Collections.emptyList();
			SystemClock.startTimer();
			exchangeCount.incrementAndGet();
			Trace[] traces = (Trace[])payload;
			List<ClosedTrace> closedTraces = new ArrayList<ClosedTrace>(traces.length);
			Set<String> hosts = new HashSet<String>();
			Set<String> agents = new HashSet<String>();
			for(Trace trace: traces) {
				hosts.add(trace.getHostName());
				agents.add(trace.getAgentName());
				closedTraces.add(ClosedTrace.newClosedTrace(trace));
			}
			for(String host: hosts) {
				Element e = hostCache.get(host);
				if(e==null) {
					e = new Element(host, host);
					hostCache.put(e);
				}
			}
			for(String agent: agents) {
				Element e = agentCache.get(agent);
				if(e==null) {
					e = new Element(agent, agent);
					agentCache.put(e);
				}			
			}
			elapsedTimeCounter.put(SystemClock.endTimer().elapsedMs);
			return closedTraces;
		} catch (Throwable e) {
			log.error("TraceMessageSplitter Error:", e);
			throw new RuntimeException("TraceMessageSplitter Error:", e);
		} finally {
			try { SystemClock.endTimer(); } catch (Exception e) {}
		}
	}

	/**
	 * The total number of exchanges processed
	 * @return the exchangeCount
	 */
	@ManagedAttribute(description="The total number of exchanges processed")
	public long getExchangeCount() {
		return exchangeCount.get();
	}


	
}
