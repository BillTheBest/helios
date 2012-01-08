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
package org.helios.ot.endpoint;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.Notification;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.ClassHelper;
import org.helios.helpers.JMXHelper;
import org.helios.helpers.SystemEnvironmentHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmx.threadservices.scheduling.HeliosScheduler;
import org.helios.jmx.threadservices.scheduling.ScheduledTaskHandle;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.helios.time.SystemClock;
import org.helios.time.SystemClock.ElapsedTime;

/**
 * <p>Title: AbstractEndpoint</p>
 * <p>Description: Abstract Base class for each Helios tracer endpoint.</p>
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@JMXManagedObject (declared=false, annotated=true)
@JMXNotifications(notifications={
        @JMXNotification(description="Notification indicating change in endpoint state", types={
                @JMXNotificationType(type="org.helios.ot.endpoint.state")
        })
})
public abstract class AbstractEndpoint<T extends Trace<? extends ITraceValue>> extends ManagedObjectDynamicMBean implements LifecycleAwareIEndPoint<T>, Callable<Boolean> {

	private static final long serialVersionUID = -2805888058877530608L;
	/** Whether this endpoint is active or not */
	private boolean isConnected = false;
	/** Last timestamp (in millis) when connected was established to an endpoint */
	protected long lastConnected = 0L;
	/** Last timestamp (in millis) when we disconnected from this endpoint */
	protected long lastDisconnected = 0L;
	/** Number of consecutive errors produced by this endpoint so far */
	private int consecutiveSubmitFailureCount = 0;
	/** Number of reconnect attempts made so far */
	private int consecutiveReconnectAttempts = 0;
	/** Number of failures during trace submits before Helios switches marks this Endpoint disconnected (offline)  */
	protected int submitFailureThreshold = 5;	
	/** Frequency at which reconnect will happen - default 30 seconds - set to -1 or 0 to disable auto connection attempts */
	protected long reconnectAttemptFrequency = 30000L;
	/** Maximum number of times auto reconnect should happen before disabling this feature */
	protected int maxReconnectAttempts = 10;
	/**Indicates if stackTraces for an endpoint failures should be logged.  */
	protected boolean logErrors=false;
	
	/** Total number of active endpoints at this time */
	private static AtomicInteger numberOfActiveEndpoints = new AtomicInteger();

	private static AtomicLong notificationSerialNumber = new AtomicLong(0);

	protected Logger log;


	/**
	 * Name with which this endpoint's MBean will be registered in an MBeanServer.  If this property
	 * is not provided, a default value will be created.
	 *
	 * Default Value: <package_name>:type=<Class_name>,name=<Bean_Id>
	 */
	protected ObjectName objectName = null;

	/** The property name where the JMX domain name for all endpoints would be picked */
	protected static final String ENDPOINTS_DOMAIN_PROPERTY="helios.endpoints.jmx.domain";
	/** Default JMX domain name for all endpoints in case ENDPOINTS_DOMAIN_PROPERTY is not specified*/
	protected static final String ENDPOINTS_DOMAIN_DEFAULT="org.helios.endpoints";

	/** Total number of traces submitted to this Endpoint */
	private int totalTraceSubmits = 0;
	/**Number of times traces were submitted to this endpoint successfully */
	private int totalSuccessfulTraceSubmits = 0;
	/**Number of times we failed to submit traces to this endpoint  */
	private int totalFailedTraceSubmits = 0;
	/** The builder used to build this endpoint */
	protected Builder builder = null;	

	/** Reference to the primary task scheduling service for Helios */
	private HeliosScheduler hScheduler = null;

	/** Handle to a scheduled frequency task */
	private ScheduledTaskHandle<Boolean> frequencyTaskHandle = null;
	/** Maximum time taken so far to submit traces to the this endpoint */
	private long maxTimeTakenToSubmitTraces = 0l;
	/**  Minimum time taken so far to submit traces to the this endpoint */
	private long minTimeTakenToSubmitTraces = 0L;
	
	/** The total number of traces dropped since the last reset */
	protected final AtomicLong dropCount = new AtomicLong(0L);

	/**
	 * Classes extending AbstractEndpoints should call this constructor using super()
	 */
	protected AbstractEndpoint() {
		log = Logger.getLogger(this.getClass());
	}
	
	/**
	 * Returns a new Builder for this endpoint
	 * @return an endpoint builder
	 */
	public abstract Builder newBuilder();
	
	/**
	 * Creates a new AbstractEndpoint
	 * @param builder A configured builder
	 */
	protected AbstractEndpoint(Builder builder) {
		this();
		this.builder = builder;
		submitFailureThreshold=builder.getSubmitFailureThreshold();	
		reconnectAttemptFrequency=builder.getReconnectAttemptFrequency();
		maxReconnectAttempts=builder.getMaxReconnectAttempts();		
		logErrors=builder.isLogErrors();
		if(builder.processorThreadPrefix==null) {
			builder.processorThreadPrefix=getClass().getSimpleName() + "@" + System.identityHashCode(this);			
		}
	}
	

	/**
	 * Callback method for HeliosScheduler to trigger connection reattempts to this Endpopint.
	 */
	public Boolean call() throws EndpointConnectException {
		if(consecutiveReconnectAttempts < maxReconnectAttempts){
			consecutiveReconnectAttempts++;
			return Boolean.valueOf(connect());
		}
		log.error("Tried ["+consecutiveReconnectAttempts+"] times to restore connection to the Endpoint ["+this.getClass().getSimpleName()+"] but failed.  Canceling future reattempts.");
		unScheduleEndpointConnectAttempt();
		return Boolean.valueOf(false);
	}
	
	/**
	 * Callback from the tracermanager when it starts.
	 */
	public void onTracerManagerStartup() {
		connect();
	}
	/**
	 * Callback from the tracermanager when it stops
	 */
	public void onTracerManagerShutdown() {
		disconnect();
		log.info("\n\t[" + getClass().getSimpleName() + "] shutting down. " + 
				"\n\tCompleted Submissions:" + totalSuccessfulTraceSubmits + 
				"\n\tTotal Submissions:" + totalTraceSubmits +
				"\n\tDropped Submissions:" + dropCount.get());
				
	}
	

	/**
	 *  Triggers the start of this Endpoint lifecycle
	 */
	@JMXOperation (name="connect", description="Connect to this Endpoint")
	public final boolean connect() {
		log.trace("connect executed...");
		if(isConnected){
			log.debug("Endpoint Connection is already active so ignore this call...");
			return true;
		}
		long start = System.currentTimeMillis();
		try {
			connectImpl();
			registerEndpointMBean();
			// Check if a reconnect schedule is already active for this Endpoint
			// If yes, unschedule it as Endpoint came back online
			unScheduleEndpointConnectAttempt();
			numberOfActiveEndpoints.incrementAndGet();
			lastConnected = System.currentTimeMillis();
			if(!isConnected)
				this.sendNotification(new Notification("org.helios.ot.endpoint.state",this,notificationSerialNumber.incrementAndGet(),System.currentTimeMillis(),"Connected to endpoint ["+this.getClass().getSimpleName()+"]"));
			isConnected = true;
			log.debug("Connect call for Endpoint ["+ this.getClass().getSimpleName() +"] completed in " + (System.currentTimeMillis() - start) + " ms.");
		} catch (EndpointConnectException e) {
			log.warn("An error occured while connecting to Endpoint: " + this.getClass().getSimpleName(), e);
			cleanup();
			scheduleConnectAttempt();
		}
		return true;
	}

	private void registerEndpointMBean() {
		try {
			objectName = ObjectName.getInstance(SystemEnvironmentHelper.getSystemPropertyThenEnv(ENDPOINTS_DOMAIN_PROPERTY, ENDPOINTS_DOMAIN_DEFAULT)+":type="+this.getClass().getName().substring(this.getClass().getName().lastIndexOf( '.' ) + 1 )+",name="+this.getClass().getSimpleName());
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (javax.management.InstanceAlreadyExistsException iex){
			log.trace("An MBean already exists for this Endpoint so ignoring the exception...");
		}catch (Exception e) {
			log.warn("An error occured while registering MBean for the endpoint " + this.getClass().getSimpleName() + " with objectName " + objectName, e);
		}

	}

	/**
	 * Disconnects from Endpoint and recycle resources used by it
	 */
	public final void destroy(){
		log.trace("destroy executed...");
		disconnect();
		cleanup();
		unregisterEndpointMBean();
	}

	/**
	 * Concrete endpoint connect operation
	 * @throws EndpointConnectException thrown if the endpoint fails to connect
	 */
	protected abstract void connectImpl() throws EndpointConnectException;
	/**
	 * Concrete endpoint trace collection processing and submission operation
	 * @param traceCollection The trace collection to submit.
	 * @throws EndpointConnectException thrown if the submission fails specifically because the endpoint's 
	 * conection or connection related resources encountered an error. Unrecoverable without reconnecting.
	 * @throws EndpointTraceException Assumed to be a recoverable error but representing a submission problem specific to the traceCollection instance.
	 * @return true if the trace was processed, false if it was dropped 
	 */
	protected abstract boolean processTracesImpl(TraceCollection<T> traceCollection) throws EndpointConnectException, EndpointTraceException;
	/**
	 * Concrete endpoint disconnect operation
	 */
	protected abstract void disconnectImpl();


	@JMXOperation (name="disconnect", description="Disconnect from this Endpoint")
	public final void disconnect(){
		log.debug("disconnect executed...");
		disconnectImpl();
		//unregisterEndpointMBean();
		numberOfActiveEndpoints.decrementAndGet();
		lastDisconnected = System.currentTimeMillis();
		if(isConnected)
			this.sendNotification(new Notification("org.helios.ot.endpoint.state",this,notificationSerialNumber.incrementAndGet(),System.currentTimeMillis(),"Disconnected from endpoint ["+this.getClass().getSimpleName()+"]"));
		isConnected = false;
	}

	private void unregisterEndpointMBean() {
		log.trace("unregister executed...");
		try {
			JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
			log.debug("Endpoint ["+ this.getClass().getSimpleName()+"] with object name ["+objectName+"] is unregistered from MBean server.");
		} catch (MBeanRegistrationException e) {
			log.warn("An error occured while unregistering MBean for Endpoint: " + this.getClass().getSimpleName(), e);
		} catch (InstanceNotFoundException e) {
			log.warn("An error occured while unregistering MBean for Endpoint: " + this.getClass().getSimpleName(), e);
		}
	}

	public final boolean reconnect() {
		log.trace("reconnect executed...");
		disconnect();
		return connect();
	}

	/**
	 * Endpoint can override this method to perform any specific cleanup of resources
	 */
	public void cleanup() {	log.trace("cleanup executed..."); }

	/**
	 * @param traces
	 * @throws Exception
	 */
	public void processTraces(TraceCollection<T> traces) throws Exception {
		long start = System.currentTimeMillis();
//		if(!isConnected){
//			connect();
//		}
		try{
			if(log.isTraceEnabled()) {
				SystemClock.startTimer();
			}
			if(processTracesImpl(traces)) {
				long elapsedTime = (System.currentTimeMillis() - start);
				if(elapsedTime > maxTimeTakenToSubmitTraces)
					maxTimeTakenToSubmitTraces = elapsedTime;
				else if(elapsedTime < minTimeTakenToSubmitTraces)
					minTimeTakenToSubmitTraces = elapsedTime;											
				totalSuccessfulTraceSubmits++;
				if(log.isTraceEnabled()) {
					ElapsedTime et = SystemClock.endTimer();
					log.trace("process traces for Endpoint ["+ this.getClass().getSimpleName() +"] " + et);
				}				
			} else {
				dropCount.incrementAndGet();
			}
			totalTraceSubmits++;
		}catch(EndpointConnectException eex){
			totalFailedTraceSubmits++;
			if(logErrors)
				log.error("An error occured while connecting to Endpoint: " + this.getClass().getSimpleName(), eex);
			if(isConnected)
				this.sendNotification(new Notification("org.helios.ot.endpoint.state",this,notificationSerialNumber.incrementAndGet(),System.currentTimeMillis(),"Disconnected from endpoint ["+this.getClass().getSimpleName()+"]"));
			isConnected = false;
			scheduleConnectAttempt();
		} catch (EndpointTraceException e) {
			if(logErrors)
				log.error("An error occured while submitting traces to Endpoint: " + this.getClass().getSimpleName(), e);
			totalFailedTraceSubmits++;
			if(consecutiveSubmitFailureCount<submitFailureThreshold)
				consecutiveSubmitFailureCount++;
			else{
				// Failure threshold reached - going to mark this Endpoint offline now
				disconnect();
				consecutiveSubmitFailureCount = 0;
			}
		}
	}

	/**
	 * Scheduled a frequency based task with HeliosScheduler to attempt to reconnect to this Endpoint
	 */
	private void scheduleConnectAttempt(){
		log.trace("scheduleConnectAttempt executed...");
		if(reconnectAttemptFrequency > 0L){
			if(frequencyTaskHandle == null){
				if(hScheduler==null)
					hScheduler = HeliosScheduler.getInstance();

				frequencyTaskHandle = hScheduler.scheduleAtFrequency(false, this, 5000L, reconnectAttemptFrequency, TimeUnit.MILLISECONDS);
			}else
				log.trace("A reconnect frequency is already active for this endpoint: " + this.getClass().getSimpleName());
		}
		else
			log.trace("Skipping reconnect attempts as invalid frequency [" +reconnectAttemptFrequency+ "] provided for the endpoint" + this.getClass().getSimpleName());

	}

	private void unScheduleEndpointConnectAttempt(){
		log.trace("unScheduleEndpointConnectAttempt executed...");
		if(frequencyTaskHandle!=null){
			frequencyTaskHandle.cancel(true);
			frequencyTaskHandle = null;
			hScheduler = null;
		}
	}

	@JMXAttribute (name="Connected", description="Whether the connection to Endpoint is active currently", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * @return the lastConnected
	 */
	@JMXAttribute (name="LastConnected", description="Last timestamp (in millis) when connected was established to an endpoint", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastConnected() {
		return lastConnected;
	}


	/**
	 * @return the lastDisconnected
	 */
	@JMXAttribute (name="LastDisconnected", description="Last timestamp (in millis) when connected broke to this endpoint", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getLastDisconnected() {
		return lastDisconnected;
	}

	@JMXOperation (name="resetStatistics", description="Reset numbers for submitted traces")
	public void resetStatistics(){
		totalTraceSubmits = 0;
		totalFailedTraceSubmits = 0;
		totalSuccessfulTraceSubmits = 0;
	}

	/**
	 * @return the submitFailureThreshold
	 */
	@JMXAttribute (name="SubmitFailureThreshold", description="Number of failures during trace submits before Helios switches marks this Endpoint disconnected (offline)", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getSubmitFailureThreshold() {
		return submitFailureThreshold;
	}

	/**
	 * @param submitFailureThreshold the submitFailureThreshold to set
	 */
	public void setSubmitFailureThreshold(int submitFailureThreshold) {
		this.submitFailureThreshold = submitFailureThreshold;
	}

	/**
	 * @return the reconnectAttemptFrequency
	 */
	@JMXAttribute (name="ReconnectAttemptFrequency", description="Frequency (in milliseconds) at which attempts will be made to reconnect to an offline endpoint", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getReconnectAttemptFrequency() {
		return reconnectAttemptFrequency;
	}

	/**
	 * @param reconnectAttemptFrequency the reconnectAttemptFrequency to set
	 */
	public void setReconnectAttemptFrequency(long reconnectAttemptFrequency) {
		this.reconnectAttemptFrequency = reconnectAttemptFrequency;
	}

	/**
	 * @return the maxReconnectAttempts
	 */
	@JMXAttribute (name="MaxReconnectAttempts", description="Maximum number of connection attempts to be made when connection endpoint goes offline", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getMaxReconnectAttempts() {
		return maxReconnectAttempts;
	}

	/**
	 * @param maxReconnectAttempts the maxReconnectAttempts to set
	 */
	public void setMaxReconnectAttempts(int maxReconnectAttempts) {
		this.maxReconnectAttempts = maxReconnectAttempts;
	}

	/**
	 * @return the logErrors
	 */
	@JMXAttribute (name="LogErrors", description="Should detailed error messages be printed in logs", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean isLogErrors() {
		return logErrors;
	}

	/**
	 * @param logErrors the logErrors to set
	 */
	public void setLogErrors(boolean logErrors) {
		this.logErrors = logErrors;
	}

	/**
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * @param objectName the objectName to set
	 */
	public void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}

	/**
	 * @return the totalSuccessfulTraceSubmits
	 */
	@JMXAttribute (name="TotalSuccessfulTraceSubmits", description="Number of trace submits that are successful so far", mutability=AttributeMutabilityOption.READ_ONLY)
	public final int getTotalSuccessfulTraceSubmits() {
		return totalSuccessfulTraceSubmits;
	}

	/**
	 * @return the totalFailedTraceSubmits
	 */
	@JMXAttribute (name="TotalFailedTraceSubmits", description="Number of trace submits so that are failed so far", mutability=AttributeMutabilityOption.READ_ONLY)
	public final int getTotalFailedTraceSubmits() {
		return totalFailedTraceSubmits;
	}

	/**
	 * @return the totalTraceSubmits
	 */
	@JMXAttribute (name="TotalTraceSubmits", description="Number of traces submitted so far", mutability=AttributeMutabilityOption.READ_ONLY)
	public final int getTotalTraceSubmits() {
		return totalTraceSubmits;
	}

	/**
	 * @return the numberOfActiveEndpoints
	 */
	@JMXAttribute (name="NumberOfActiveEndpoints", description="Number of active endpoints at this time", mutability=AttributeMutabilityOption.READ_ONLY)
	public static AtomicInteger getNumberOfActiveEndpoints() {
		return numberOfActiveEndpoints;
	}

	/**
	 * @return the consecutiveSubmitFailureCount
	 */
	@JMXAttribute (name="ConsecutiveSubmitFailureCount", description="Number of consecutive errors produced by this endpoint so far", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getConsecutiveSubmitFailureCount() {
		return consecutiveSubmitFailureCount;
	}

	/**
	 * @return the consecutiveReconnectAttempts
	 */
	@JMXAttribute (name="ConsecutiveReconnectAttempts", description="Number of reconnect attempts made so far", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getConsecutiveReconnectAttempts() {
		return consecutiveReconnectAttempts;
	}
	
	@JMXAttribute (name="MaxTimeTakenToSubmitTraces", description="Maximum time taken so far to submit traces to the this endpoint", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMaxTimeTakenToSubmitTraces() {
		return maxTimeTakenToSubmitTraces;
	}

	@JMXAttribute (name="MinTimeTakenToSubmitTraces", description="Minimum time taken so far to submit traces to the this endpoint", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMinTimeTakenToSubmitTraces() {
		return minTimeTakenToSubmitTraces;
	}
	
	
	/**
	 * Searches the builder's endpoint args map for a match to the construct name. 
	 * If one is found, the arguments are applied to that construct.
	 * @param constructName The name of the construct (usually the simple class name of the construct)
	 * @param construct The instance of the construct
	 * @see AbstractEndpoint.Builder#setEndpointArgs(Map) for additional details on how this works
	 */
	public void applyEndpointArguments(String constructName, Object construct) {
		if(constructName==null || construct==null) return;
		Class<?> constructClass = construct.getClass();
		Map<String, Object[]> args = this.builder.arbitraryArgs.get(constructName);
		if(args!=null) {
			for(Map.Entry<String, Object[]> entry: args.entrySet()) {
				Method method = ClassHelper.getMatchingMethod(constructClass, entry.getKey(), entry.getValue());
				if(method!=null) {
					method.setAccessible(true);
					try {
						if(Modifier.isStatic(method.getModifiers())) {
							method.invoke(null, entry.getValue());
						} else {
							method.invoke(construct, entry.getValue());
						}
						if(log.isDebugEnabled()) log.debug("Applied [" + method.getName() + "] with values " + Arrays.toString(entry.getValue()) + " on Construct [" + constructName + "]");
					} catch (Exception e) {
						throw new RuntimeException("Failed to apply endpoint arguments to instance of [" + constructClass.getName() + "] using method [" + method.toGenericString() + "]", e);
					}
				}
			}
		}
	}
	
	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.endpoint.Builder</code></p>
	 */
	public abstract static class Builder {
		/** Number of failures during trace submits before Helios switches marks this Endpoint disconnected (offline)  */
		private int submitFailureThreshold = 5;	
		/** Frequency at which reconnect will happen - default 30 seconds - set to -1 or 0 to disable auto connection attempts */
		private long reconnectAttemptFrequency = 30000L;
		/** Maximum number of times auto reconnect should happen before disabling this feature */
		private int maxReconnectAttempts = 10;
		/**Indicates if stackTraces for an endpoint failures should be logged.  */
		private boolean logErrors=false;
		/** The number of threads that should spin up to process submissions to this endpoint */
		private int processorThreadCount = 1;
		/** The thread group and thread prefix name for threads that process submissions for this endpoint */
		protected String processorThreadPrefix = null;
		/** The batch size of the endpoint processor */
		private int batchSize = 1;
		/** The size of the endpoint processor's submission queue */
		private int queueSize = 1000;
		/** Arbitrary configuration calls to the primary endpoint underlying provider e.g. a JMS Connection */
		private Map<String, Map<String, Object[]>> arbitraryArgs = new HashMap<String, Map<String, Object[]>>();
		
		
		/**
		 * Compiles the builder and builds an endpoint instance
		 * @return a built endpoint
		 * @throws EndpointConfigException 
		 */
		public abstract AbstractEndpoint build() throws EndpointConfigException;
		
		/**
		 * Returns the submission cont failure threshold
		 * @return the submitFailureThreshold
		 */
		public int getSubmitFailureThreshold() {
			return submitFailureThreshold;
		}
		/**
		 * Sets the submission cont failure threshold
		 * @param submitFailureThreshold the submitFailureThreshold to set
		 */
		public void setSubmitFailureThreshold(int submitFailureThreshold) {
			this.submitFailureThreshold = submitFailureThreshold;
		}
		
		/**
		 * Sets the submission cont failure threshold
		 * @param submitFailureThreshold the submitFailureThreshold to set
		 * @return this builder
		 */
		public Builder submitFailureThreshold(int submitFailureThreshold) {
			this.submitFailureThreshold = submitFailureThreshold;
			return this;
		}
		
		/**
		 * Returns the frequency of reconnect attempts in ms.
		 * @return the reconnectAttemptFrequency
		 */
		public long getReconnectAttemptFrequency() {
			return reconnectAttemptFrequency;
		}
		/**
		 * Sets the frequency of reconnect attempts in ms.
		 * @param reconnectAttemptFrequency the reconnectAttemptFrequency to set
		 */
		public void setReconnectAttemptFrequency(long reconnectAttemptFrequency) {
			this.reconnectAttemptFrequency = reconnectAttemptFrequency;
		}
		/**
		 * Sets the frequency of reconnect attempts in ms.
		 * @param reconnectAttemptFrequency the reconnectAttemptFrequency to set
		 * @return this builder
		 */
		public Builder reconnectAttemptFrequency(long reconnectAttemptFrequency) {
			this.reconnectAttemptFrequency = reconnectAttemptFrequency;
			return this;
		}

		/**
		 * Returns the maximum number of reconnect attempts
		 * @return the maxReconnectAttempts
		 */
		public int getMaxReconnectAttempts() {
			return maxReconnectAttempts;
		}
		/**
		 * Sets the maximum number of reconnect attempts
		 * @param maxReconnectAttempts the maxReconnectAttempts to set
		 */
		public void setMaxReconnectAttempts(int maxReconnectAttempts) {
			this.maxReconnectAttempts = maxReconnectAttempts;
		}
		/**
		 * Sets the maximum number of reconnect attempts
		 * @param maxReconnectAttempts the maxReconnectAttempts to set
		 * @return this builder
		 */
		public Builder maxReconnectAttempts(int maxReconnectAttempts) {
			this.maxReconnectAttempts = maxReconnectAttempts;
			return this;
		}
		
		/**
		 * Indicates if submission errors are being logged 
		 * @return the logErrors
		 */
		public boolean isLogErrors() {
			return logErrors;
		}
		/**
		 * Configures if submission errors are being logged
		 * @param logErrors true to log errors, false to suppress
		 */
		public void setLogErrors(boolean logErrors) {
			this.logErrors = logErrors;
		}
		/**
		 * Configures if submission errors are being logged
		 * @param logErrors true to log errors, false to suppress
		 * @return this builder
		 */
		public Builder logErrors(boolean logErrors) {
			this.logErrors = logErrors;
			return this;
		}

		/**
		 * Returns the number of threads that should spin up to process submissions to this endpoint
		 * @return the processorThreadCount
		 */
		public int getProcessorThreadCount() {
			return processorThreadCount;
		}

		/**
		 * Sets the number of threads that should spin up to process submissions to this endpoint
		 * @param processorThreadCount the processorThreadCount to set
		 */
		public void setProcessorThreadCount(int processorThreadCount) {
			this.processorThreadCount = processorThreadCount;
		}
		
		/**
		 * Sets the number of threads that should spin up to process submissions to this endpoint
		 * @param processorThreadCount the processorThreadCount to set
		 * @return this builder
		 */
		public Builder processorThreadCount(int processorThreadCount) {
			this.processorThreadCount = processorThreadCount;
			return this;
		}
		

		/**
		 * Returns thread group and thread prefix name for threads that process submissions for this endpoint
		 * @return the processorThreadPrefix
		 */
		public String getProcessorThreadPrefix() {
			return processorThreadPrefix; 
		}

		/**
		 * Sets thread group and thread prefix name for threads that process submissions for this endpoint
		 * @param processorThreadPrefix the processorThreadPrefix to set
		 */
		public void setProcessorThreadPrefix(String processorThreadPrefix) {
			this.processorThreadPrefix = processorThreadPrefix;
		}
		
		/**
		 * Sets thread group and thread prefix name for threads that process submissions for this endpoint
		 * @param processorThreadPrefix the processorThreadPrefix to set
		 * @return this builder
		 */
		public Builder processorThreadPrefix(String processorThreadPrefix) {
			this.processorThreadPrefix = processorThreadPrefix;
			return this;
		}

		/**
		 * Returns the endpoint processing batch size
		 * @return the batchSize
		 */
		public int getBatchSize() {
			return batchSize;
		}

		/**
		 * Sets the endpoint processing batch size
		 * @param batchSize the batchSize to set
		 */
		public void setBatchSize(int batchSize) {
			this.batchSize = batchSize;
		}
		
		/**
		 * Sets the endpoint processing batch size
		 * @param batchSize the batchSize to set
		 * @return this builder
		 */
		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}
		/**
		 * Returns the endpoint processor's queue size
		 * @return the queueSize
		 */
		public int getQueueSize() {
			return queueSize;
		}

		/**
		 * Sets the endpoint processor's queue size
		 * @param queueSize the queueSize to set
		 */
		public void setQueueSize(int queueSize) {
			this.queueSize = queueSize;
		}
		
		/**
		 * Sets the endpoint processor's queue size
		 * @param queueSize the queueSize to set
		 * @return this builder
		 */
		public Builder queueSize(int queueSize) {
			this.queueSize = queueSize;
			return this;
		}
		
		// Map<String, Map<String, Object[]>> arbitraryArgs
		/**
		 * Sets a map of arbitrary args to be applied to an endpoint's underlying constructs.
		 * The parameter is a map of attribute names / methods and arguments to be applied, keyed by
		 * the arbitrary (i.e. endpoint specific) name of the construct to apply to.
		 * <p>Example: This endpoint arg is intended to be applied to an instance of {@link org.apache.activemq.ActiveMQConnection}<ul>
		 * 	<li><b>Key:</b>JMSConnection</li>
		 *  <li>Map Entry:<ul>
		 *  	<li><b>Key:</b>setSendAcksAsync</li>
		 *      <li><b>Value:</b>true</li>
		 *  </ul></li>
		 * </ul>
		 * The endpoint would call {@link AbstractEndpoint#applyEndpointArguments(String, Object)} and pass the arguments <code>JMSConnection</code> and the <code>JMSConnection</code> instance.
		 * The endpoint will make a best effort to match the attribute/method name and argument to the passed instance and invoke.
		 *  map entries for the key <b>JMSConnection</b></p>
		 * @param endpointArgs a map of endpoint arguments
		 */		
		public void setEndpointArgs(Map<String, Map<String, Object[]>> endpointArgs) {
			if(endpointArgs!=null && endpointArgs.size()>0) {
				this.arbitraryArgs.putAll(endpointArgs);
			}
		}
		
		/**
		 * Adds a new Endpoint arg to the builder.
		 * @param name The name of the construct these args will be applied to 
		 * @param endpointArgs A map of arguments keyed by the attribute or method name they target in the the target construct
		 * @see AbstractEndpoint.Builder#setEndpointArgs(Map)
		 */
		public void setEndpointArg(String name, Map<String, Object[]> endpointArgs) {
			if(endpointArgs!=null && endpointArgs.size()>0) {
				Map<String, Object[]> existing = arbitraryArgs.get(name);
				if(existing==null) {
					synchronized(arbitraryArgs) {
						existing = arbitraryArgs.get(name);
						if(existing==null) {
							existing  = new HashMap<String, Object[]>();
							arbitraryArgs.put(name, existing);
						}
					}
				}
				existing.putAll(existing);
			}
		}
		
		/**
		 * Adds a single endpoint arg to the builder
		 * @param name The construct name of the target construct
		 * @param attrName The attribute or method name to invoke on the target construct 
		 * @param endpointArgs An array of arguments to be passed to the method invocation
		 * @see AbstractEndpoint.Builder#setEndpointArgs(Map)
		 */
		public void setEndpointArg(String name, String attrName, Object...endpointArgs) {
			Map<String, Object[]> existing = arbitraryArgs.get(name);
			if(existing==null) {
				synchronized(arbitraryArgs) {
					existing = arbitraryArgs.get(name);
					if(existing==null) {
						existing  = new HashMap<String, Object[]>();
						arbitraryArgs.put(name, existing);
					}
				}
			}
			existing.put(attrName, (endpointArgs==null || endpointArgs.length<1) ? new Object[0] : endpointArgs);
		}
		
		/**
		 * Adds a single endpoint arg to the builder
		 * @param name The construct name of the target construct
		 * @param attrName The attribute or method name to invoke on the target construct 
		 * @param endpointArgs An array of arguments to be passed to the method invocation
		 * @return this builder
		 * @see AbstractEndpoint.Builder#setEndpointArgs(Map)
		 */
		public Builder endpointArg(String name, String attrName, Object...endpointArgs) {
			Map<String, Object[]> existing = arbitraryArgs.get(name);
			if(existing==null) {
				synchronized(arbitraryArgs) {
					existing = arbitraryArgs.get(name);
					if(existing==null) {
						existing  = new HashMap<String, Object[]>();
						arbitraryArgs.put(name, existing);
					}
				}
			}
			existing.put(attrName, (endpointArgs==null || endpointArgs.length<1) ? new Object[0] : endpointArgs);
			return this;
		}
		
		
		
	}


}
