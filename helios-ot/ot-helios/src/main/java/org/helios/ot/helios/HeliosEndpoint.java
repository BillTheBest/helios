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
package org.helios.ot.helios;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.endpoint.EndpointConnectException;
import org.helios.ot.endpoint.EndpointTraceException;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.helios.time.SystemClock;
import org.helios.version.VersionHelper;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * <p>Title: HeliosEndpoint</p>
 * <p>Description: OpenTrace endpoint optimized for the Helios OT Server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.helios.HeliosEndpoint</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class HeliosEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> implements UncaughtExceptionHandler {
	/** The helios OT server host name or ip address */
	protected String host;
	/** The helios OT server listening port */
	protected int port;
	/** The helios OT server comm protocol */
	protected Protocol protocol;
	/** The helios OT connector of the configured protocol */
	protected AbstractEndpointConnector connector;
	
	/** The count of exceptions */
	protected final AtomicLong exceptionCount = new AtomicLong(0);
	
	/** A set of connect listeners that will be added when an asynch connect is initiated */
	protected final Set<ChannelFutureListener> connectListeners = new CopyOnWriteArraySet<ChannelFutureListener>();
	
	
	/**  */
	private static final long serialVersionUID = -433677190518825263L;
	/** The last elapsed message */
	protected String lastElapsed = null;
	
	
	/**
	 * Creates a new HeliosEndpoint from system properties and an optional external XML file
	 */
	public HeliosEndpoint() {
		// Read the basic config
		host = HeliosEndpointConfiguration.getHost();
		port = HeliosEndpointConfiguration.getPort();
		protocol = HeliosEndpointConfiguration.getProtocol();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#connectImpl()
	 */
	@Override
	protected void connectImpl() throws EndpointConnectException {
		try {
			connector = AbstractEndpointConnector.connect(this);
			reflectObject(connector);
		} catch (Exception e) {
			throw new EndpointConnectException("HeliosEndpoint failed to connect:" + e);			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#disconnectImpl()
	 */
	@Override
	protected void disconnectImpl() {
		connector.disconnect();
	}
	
	
//	@SuppressWarnings("rawtypes")
//	public static void main(String[] args) {
//		BasicConfigurator.configure();
//		Logger LOG = Logger.getLogger(HeliosEndpoint.class);
//		Logger.getRootLogger().setLevel(Level.INFO);
//		LOG.info("Test");
//		HeliosEndpoint he = new HeliosEndpoint();
//		he.connect();
//		he.reflectObject(he.connector.getInstrumentation());
//		TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration().appendEndPoint(he));
//		boolean b = he.connect();
//		LOG.info("Connected:"+b);
//		try { Thread.currentThread().join(); } catch (Exception e) {}
//		LOG.info("Exiting.......");
//		System.exit(-1);
//	}
	
	public static void main(String[] args) {		
		System.out.println(banner());
		if(args.length>0) {
			if("server".equalsIgnoreCase(args[0])) {
				String server = OTServerDiscovery.info();
				if(server==null || server.trim().isEmpty()) {
					System.out.println("No Helios OT Server Found");
				} else {
					System.out.println(server);
				}
			}
		}
	}
	
	protected static String banner() {
		return "Helios OpenTrace Agent [" + VersionHelper.getHeliosVersion(HeliosEndpoint.class) + "]";
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("Uncaught exception on thread [" + t + "]", e);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#newBuilder()
	 */
	@Override
	public org.helios.ot.endpoint.AbstractEndpoint.Builder newBuilder() {
		return null;
	}

	/**
	 * Returns the system clock measurement of the last submission elapsed time
	 * @return the system clock measurement of the last submission elapsed time
	 */
	@JMXAttribute(name="LastElapsed", description="The system clock measurement of the last submission elapsed time", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getLastElapsed() {
		return lastElapsed;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.endpoint.AbstractEndpoint#processTracesImpl(org.helios.ot.tracer.disruptor.TraceCollection)
	 */
	@Override
	protected boolean processTracesImpl(TraceCollection<T> traceCollection) throws EndpointConnectException, EndpointTraceException {
		SystemClock.startTimer();
		if(isConnected()) {
			connector.write(traceCollection);
			lastElapsed = SystemClock.endTimer().toString();
			return true;
		} 
		return false;
	}
	

	
	

	/**
	 * Returns the helios OT server host name or ip address
	 * @return the host
	 */
	@JMXAttribute(name="Host", description="The helios OT server host name or ip address", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getHost() {
		return host;
	}

	/**
	 * Returns the helios OT server listening port
	 * @return the port
	 */
	@JMXAttribute(name="Port", description="The helios OT server listening port", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getPort() {
		return port;
	}

	/**
	 * Returns the helios OT server comm protocol
	 * @return the protocol
	 */
	@JMXAttribute(name="Protocol", description="The helios OT server comm protocol", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProtocol() {
		return protocol.name();
	}

	/**
	 * Returns the cumulative exception count
	 * @return the exceptionCount
	 */
	@JMXAttribute(name="ExceptionCount", description="The cumulative exception count", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getExceptionCount() {
		return exceptionCount.get();
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    StringBuilder retValue = new StringBuilder("HeliosEndpoint [")
	        .append("host:").append(this.host)
	        .append(" port:").append(this.port)
	        .append(" protocol:").append(this.protocol)
	        .append(" connected:").append(isConnected.get())
	        .append("]");    
	    return retValue.toString();
	}






}
