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
package org.helios.jmxenabled.service;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.AttributeChangeNotification;

import org.apache.log4j.Logger;
import org.helios.helpers.ClassHelper;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.threads.HeliosThreadGroup;
import org.helios.threads.latches.CompletionKeyLatch;
import org.helios.time.SystemClock;

/**
 * <p>Title: ServiceState</p>
 * <p>Description: Enumerates the possible states of an OT service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.jmxenabled.service</code></p>
 */

public enum ServiceState {
	/** The service is stopped or in an initial state where it has never been started */
	STOPPED(true, false),
	/** The service is starting */
	STARTING(false, false),
	/** The service start failed */
	STARTFAILED(false, true),	
	/** The service is fully started */
	STARTED(false, true),
	/** The service is stopping */
	STOPPING(false, false);
	
	/**
	 * Creates a new ServiceState 
	 * @param canStart Indicates if the service can be started when in this state
	 * @param canStop Indicates if the service can be stopped when in this state
	 */
	private ServiceState(boolean canStart, boolean canStop) {
		this.canStart = canStart;
		this.canStop= canStop;
	}
	
	/** Indicates if the service can be started when in this state */
	private final boolean canStart;
	/** Indicates if the service can be stopped when in this state */
	private final boolean canStop;
	
	/** A set of registered service names */
	private static final Set<String> SERVICE_NAMES = new CopyOnWriteArraySet<String>();
	
	
	/**
	 * Indicates if the service can be started when in this state
	 * @return true if the service can be started 
	 */
	public boolean isStartable() {
		return canStart;
	}
	/**
	 * Indicates if the service can be stopped when in this state
	 * @return true if the service can be stopped 
	 */
	public boolean isStopable() {
		return canStop;
	}
	
	/**
	 * Creates a new ServiceStateController in the STOPPED state
	 * @param name The logical name for the service this controller is watching state for
	 * @return a new ServiceStateController
	 */
	public static ServiceStateController newController(String name) {
		return new ServiceStateController(name);
	}
	
	/**
	 * Creates a new ServiceStateController in the STOPPED state
	 * @param initialState The initial state of the controller
	 * @param name The logical name for the service this controller is watching state for
	 * @return a new ServiceStateController
	 */
	public static ServiceStateController newController(ServiceState initialState, String name) {
		if(name==null) throw new IllegalArgumentException("Passed service name was null", new Throwable());
		if(initialState==null) throw new IllegalArgumentException("Passed initial state was null", new Throwable());
		if(!SERVICE_NAMES.contains(name)) {
			synchronized (SERVICE_NAMES) {
				if(!SERVICE_NAMES.contains(name)) {
					SERVICE_NAMES.add(name);
					return new ServiceStateController(initialState, name);
				}
			}
		}
		throw new RuntimeException("The service controller for [" + name + "] already exists", new Throwable());		
	}
	
	
	/**
	 * <p>Title: ServiceStateControllerListener</p>
	 * <p>Description: Defines a class that is notified of ServiceStateController state changes.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.jmxenabled.service.ServiceStateControllerListener</code></p>
	 */
	public static interface ServiceStateControllerListener {
		/**
		 * Fired when the observed ServiceStateController changes state
		 * @param name The logical name of the service issuing this state change
		 * @param priorState The state the controller transitioned from
		 * @param newState The state the controller transitioned to
		 */
		public void onStateChange(String name, ServiceState priorState, ServiceState newState);
	}
	
	/**
	 * <p>Title: ServiceStateController</p>
	 * <p>Description: A state controller that triggers events on state changes.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.jmxenabled.service.ServiceStateController</code></p>
	 */
	@JMXManagedObject(annotated=true, declared=true)
	@JMXNotifications(notifications={
	        @JMXNotification(description="Notification indicating the service changed state", types={
	                @JMXNotificationType(type=AttributeChangeNotification.ATTRIBUTE_CHANGE)
	        })
	})	
	public static class ServiceStateController extends ManagedObjectDynamicMBean {
		/** The logical name of the service being watched by this controller */
		protected final String serviceName;
		/** The watched service state  */
		protected final AtomicReference<ServiceState> state = new AtomicReference<ServiceState>();
		/** A set of state change listeners */
		protected final Set<ServiceStateControllerListener> listeners = new CopyOnWriteArraySet<ServiceStateControllerListener>();
		/** A set of count down latches for state change waiters keyed by the service state they're waiting for */
		protected final Map<ServiceState, Set<CompletionKeyLatch<ServiceState>>> stateWaiters;
		/** Instance logger */
		protected final Logger log;
		
		/**
		 * Registers a new state change listener
		 * @param listener The listener to register
		 */
		public void addListener(ServiceStateControllerListener listener) {
			if(listener!=null) {
				listeners.add(listener);
			}
		}
		
		/**
		 * Unregisters a state change listener
		 * @param listener The listener to remove
		 */
		public void removeListener(ServiceStateControllerListener listener) {
			if(listener!=null) {
				listeners.remove(listener);
			}
		}
		
		/**
		 * Causes the current thread to wait for a state change transition.
		 * @param timeout The period of time to wait for the state change
		 * @param unit The unit of the time
		 * @param states The states which will satisfy the wait condition
		 * @return The service state that the service transitioned to that released the wait, or null if the request timed out or was interrupted. 
		 */
		public ServiceState waitForStates(long timeout, TimeUnit unit, ServiceState...states) {
			final CompletionKeyLatch<ServiceState> latch = new CompletionKeyLatch<ServiceState>();
			try {				
				if(addWaiterLatches(latch, states)<1) {
					throw new IllegalArgumentException("No states provided to wait on", new Throwable());
				}
				return latch.await(timeout, unit);
			} finally {
				removeWaiterLatches(latch);
			}			
		}
		
		/**
		 * Causes the current thread to wait indefinitely for a state change transition.
		 * @param states The states which will satisfy the wait condition
		 * @return The service state that the service transitioned to that released the wait, or null if the request timed out or was interrupted. 
		 */
		public ServiceState waitForStates(ServiceState...states) {
			return waitForStates(Long.MAX_VALUE, TimeUnit.SECONDS, states);
		}

		
		/**
		 * Adds a waiter count down latch to each of the passed states
		 * @param latch The latch to add
		 * @param states The states to add the latches to
		 * @return the number of actuall adds that completed
		 */
		protected int addWaiterLatches(CompletionKeyLatch<ServiceState> latch, ServiceState...states) {
			int added = 0;
			if(states!=null) {
				for(ServiceState state: states) {
					if(state==null) continue;
					stateWaiters.get(state).add(latch);
					added++;
				}
			}
			return added;
		}
		
		/**
		 * Removes the waiter completion latch from all passed states
		 * @param latch The latch to remove
		 */
		protected void removeWaiterLatches(CompletionKeyLatch<ServiceState> latch) {
			if(latch!=null) {
				for(Set<CompletionKeyLatch<ServiceState>> latches: stateWaiters.values()) {
					latches.remove(latch);
				}
			}
		}
		
		
		/**
		 * Transitions the state controller to {@link ServiceState#STARTING}.
		 * @throws IllegalStateException thrown if if this transition is disallowed for the current state.
		 */
		public synchronized void setStarting() {
			if(!state.get().isStartable()) {
				throw new IllegalStateException("Cannot transition to STARTING state from [" + state.get() + "]", new Throwable());				
			}
			fireStateChangeEvent(state.getAndSet(STARTING), STARTING);
		}
		
		/**
		 * Transitions the state controller to {@link ServiceState#STOPPING}.
		 * @throws IllegalStateException thrown if if this transition is disallowed for the current state.
		 */
		public synchronized void setStopping() {
			if(!state.get().isStopable()) {
				throw new IllegalStateException("Cannot transition to STOPPING state from [" + state.get() + "]", new Throwable());				
			}
			fireStateChangeEvent(state.getAndSet(STOPPING), STOPPING);
		}
		
		/**
		 * Transitions the state controller to {@link ServiceState#STOPPED}.
		 * @throws IllegalStateException thrown if if this transition is disallowed for the current state.
		 */
		public synchronized void setStopped() {
			if(!state.get().equals(STOPPING)) {
				throw new IllegalStateException("Cannot transition to STOPPED state from [" + state.get() + "]", new Throwable());				
			}
			fireStateChangeEvent(state.getAndSet(STOPPED), STOPPED);
		}
		
		/** A serial number generator for starter threads */
		private static final AtomicLong starterSerial = new AtomicLong(0L);
		
		/** The default service start wait time in ms. which is {@value ServiceStateController#DEFAULT_SVC_START_TIME} */
		private static final long DEFAULT_SVC_START_TIME = 10000;
		
		/**
		 * Asynchronously starts a service using the passed runnable and then waits the default time for the {@link #STARTED} state transition.
		 * If the runnable throws an exception, it will be rethrown.
		 * @param startTask The runnable to start the target service
		 * @return true if the service started, false if the start request times out.
		 * @throws RuntimeException thrown if the startTask runnable throws an exception.
		 */
		public boolean managedServiceStart(final Runnable startTask) {
			return managedServiceStart(startTask, DEFAULT_SVC_START_TIME, TimeUnit.MILLISECONDS);
		}
		
		
		/**
		 * Asynchronously starts a service using the passed runnable and then waits for the {@link #STARTED} state transition.
		 * If the runnable throws an exception, it will be rethrown.
		 * @param startTask The runnable to start the target service
		 * @param timeout The period of time to wait for the service to start
		 * @param unit The unit of the wait time
		 * @return true if the service started, false if the start request times out.
		 * @throws RuntimeException thrown if the startTask runnable throws an exception.
		 */
		public boolean managedServiceStart(final Runnable startTask, long timeout, TimeUnit unit) {
			final AtomicReference<Throwable> startTaskException = new AtomicReference<Throwable>(null);
			final ServiceStateController controller = this;
			Thread starterThread = new Thread(
					HeliosThreadGroup.getInstance(getClass().getSimpleName() + "Starter"), 
					startTask, 
					"[" + serviceName + "] StarterThread#" + starterSerial.incrementAndGet()
			);
			starterThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					startTaskException.set(e);
					controller.setStartFailed();
				}
			});
			starterThread.start();					
			ServiceState st = startAndWaitForStarted(timeout, unit);
			Throwable startTaskEx = startTaskException.get();
			if(startTaskEx!=null) {
				throw new RuntimeException("Failed to start service [" + serviceName + "]", startTaskEx);
			}			
			return STARTED.equals(st);
		}
		
		/**
		 * Transitions the state controller to {@link #STARTED}.
		 * @throws IllegalStateException thrown if if this transition is disallowed for the current state.
		 */
		public synchronized void setStarted() {
			if(!state.get().equals(STARTING)) {
				throw new IllegalStateException("Cannot transition to STARTED state from [" + state.get() + "]", new Throwable());				
			}
			fireStateChangeEvent(state.getAndSet(STARTED), STARTED);
		}
		
		/**
		 * Transitions the state controller to {@link #STARTFAILED}.
		 * @throws IllegalStateException thrown if if this transition is disallowed for the current state.
		 */
		public synchronized void setStartFailed() {
			if(!state.get().equals(STARTING)) {
				throw new IllegalStateException("Cannot transition to STARTFAILED state from [" + state.get() + "]", new Throwable());				
			}
			fireStateChangeEvent(state.getAndSet(STARTFAILED), STARTFAILED);
		}
		
		
		/**
		 * Starts the controller and waits for the {@link #STARTED} transition state
		 * @param timeout The period of time to wait for the STARTED transition
		 * @param unit The unit of the wait time
		 * @return The service state that the service transitioned to that released the wait,
		 * which would be {@link #STARTED} or {@link #STARTFAILED}, 
		 * or null if the request timed out or was interrupted.
		 */ 
		public ServiceState startAndWaitForStarted(long timeout, TimeUnit unit) {
			setStarting();
			return waitForStates(timeout, unit, STARTED, STARTFAILED);
		}
		
		/**
		 * Starts the controller and waits the default time for the {@link #STARTED} transition state
		 * @return The service state that the service transitioned to that released the wait,
		 * which would be {@link #STARTED} or {@link #STARTFAILED}, 
		 * or null if the request timed out or was interrupted.
		 */ 
		public ServiceState startAndWaitForStarted() {
			setStarting();
			return waitForStates(STARTED, STARTFAILED);
		}
		
		
		/**
		 * Stops the controller and waits for the {@link #STOPPED} transition state
		 * @param timeout The period of time to wait for the STOPPED transition
		 * @param unit The unit of the wait time
		 * @return The service state that the service transitioned to that released the wait, 
		 * which would be {@link #STOPPED}
		 * or null if the request timed out or was interrupted.		 
		 */
		public ServiceState stopAndWaitForStopped(long timeout, TimeUnit unit) {
			setStopping();
			return waitForStates(timeout, unit, STOPPED);
		}
		
		
		/**
		 * Fires a state change event against all registered listeners
		 * @param priorState The state the controller transitioned from
		 * @param newState The state the controller transitioned to
		 */
		protected void fireStateChangeEvent(ServiceState priorState, ServiceState newState) {
			sendStateChangeNotification(priorState, newState);
			for(CompletionKeyLatch<ServiceState> latch: stateWaiters.get(newState)) {
				latch.release(newState);
			}
			for(ServiceStateControllerListener listener: listeners) {
				listener.onStateChange(serviceName, priorState, newState);
			}
		}
		
		
		/**
		 * Sends a state change JMX notification
		 * @param priorState The state the controller transitioned from
		 * @param newState The state the controller transitioned to
		 */
		protected void sendStateChangeNotification(ServiceState priorState, ServiceState newState) {
			AttributeChangeNotification acn = new AttributeChangeNotification(objectName, this.nextNotificationSequence(), SystemClock.currentClock().getTime(), serviceName + " Changed State From [" + priorState + "] to [" + newState + "]", "State", ServiceState.class.getName(), priorState.name(), newState.name());
			this.sendNotification(acn);
		}
		
		
		/**
		 * Returns the current state
		 * @return a ServiceState
		 */
		public ServiceState getState() {
			return state.get();
		}
		
		/**
		 * Returns the name of the current state
		 * @return the name of the current state
		 */
		@JMXAttribute(name="State", description="The current state of the service", mutability=AttributeMutabilityOption.READ_ONLY)
		public String getServiceStateName() {
			return state.get().name();
		}
		
		/**
		 * Creates a new ServiceStateController in the STOPPED state
		 * @param name The logical name for the service this controller is watching state for 
		 */
		private ServiceStateController(String name) {
			this(STOPPED, name);
		}
		
		/**
		 * Creates a new ServiceStateController 
		 * @param initialState The initial state of this controller
		 * @param name The logical name for the service this controller is watching state for
		 */
		private ServiceStateController(ServiceState initialState, String name) {
			serviceName = ClassHelper.nvl(name, "The passed name was null");
			log = Logger.getLogger(getClass().getName() + "." + name);			
			state.set(initialState);
			// initialize the waiter map
			HashMap<ServiceState, Set<CompletionKeyLatch<ServiceState>>> tmpMap = new HashMap<ServiceState, Set<CompletionKeyLatch<ServiceState>>>();			 
			for(ServiceState state: ServiceState.values()) {
				tmpMap.put(state, new CopyOnWriteArraySet<CompletionKeyLatch<ServiceState>>());
			}
			stateWaiters = Collections.unmodifiableMap(tmpMap);
			try {
				objectName = JMXHelper.objectName(new StringBuilder("org.helios.service:service=ServiceStateController,name=").append(name));
				JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
			} catch (Exception e) {
				log.warn("Failed to register ServiceStateController [" + name + "]. Continuing without", e);
			}
		}
	}
}

















