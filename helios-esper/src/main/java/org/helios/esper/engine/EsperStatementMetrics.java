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
package org.helios.esper.engine;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.EPStatementStateListener;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.metric.StatementMetric;
import com.espertech.esper.core.service.EPStatementImpl;

/**
 * <p>Title: EsperStatementMetrics</p>
 * <p>Description: Metric manager for esper statement metrics.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 */
@JMXManagedObject(annotated=true, declared=true)
public class EsperStatementMetrics extends ManagedObjectDynamicMBean implements StatementAwareUpdateListener, EPStatementStateListener {
	/**  */
	private static final long serialVersionUID = 225348129161683823L;
	protected EPStatement statement = null;
	protected EPStatement subStatement = null;
	protected String statementText = null;
	protected String statementName = null;
	protected EPServiceProvider provider = null;
	protected AtomicReference<StatementMetric> metric = new AtomicReference<StatementMetric>();
	protected EventType eventType = null;
	protected EventPropertyDescriptor[] eventPropertyDescriptors = null;

	
	
	/**
	 * @param statement
	 */
	public EsperStatementMetrics(EPStatement statement, EPServiceProvider provider) {
		super("Esper Statement [" + statement.getName() + "] Metrics");
		this.statement = statement;
		this.provider = provider;
		statementText = statement.getText();
		statementName = statement.getName();
		subStatement = provider.getEPAdministrator().createEPL("select * from com.espertech.esper.client.metric.StatementMetric.win:time(5 sec) where statementName = '" + statementName + "'", "Monitor-" + statementName);
		subStatement.addListener(this);
		eventType = this.statement.getEventType();
		if(eventType!=null) eventPropertyDescriptors = eventType.getPropertyDescriptors();
		provider.addStatementStateListener(this);
	}
	

	/**
	 * @param newBeans
	 * @param oldBeans
	 * @param statement
	 * @param provider
	 * @see com.espertech.esper.client.StatementAwareUpdateListener#update(com.espertech.esper.client.EventBean[], com.espertech.esper.client.EventBean[], com.espertech.esper.client.EPStatement, com.espertech.esper.client.EPServiceProvider)
	 */
	public void update(EventBean[] newBeans, EventBean[] oldBeans, EPStatement statement, EPServiceProvider provider) {
		if(newBeans.length > 0) {
			for(EventBean bean: newBeans) {
				if(bean.getUnderlying() instanceof StatementMetric) {
					StatementMetric st = (StatementMetric)bean.getUnderlying();
					if(metric.get()==null) {
						metric.set(st);
					} else {
						metric.get().addCPUTime(st.getCpuTime());
						metric.get().addNumOutputIStream((int)st.getNumOutputIStream());
						metric.get().addNumOutputRStream((int)st.getNumOutputRStream());
						metric.get().addWallTime(st.getWallTime());
						metric.get().setTimestamp(st.getTimestamp());
					}
				}
			}
		}
	}	

	/**
	 * @return
	 * @see com.espertech.esper.client.metric.StatementMetric#getCpuTime()
	 */
	@JMXAttribute(description="The statement CPU time", name="CpuTime", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCpuTime() {
		if(metric.get()==null) return 0;
		return metric.get().getCpuTime();
	}
	
	/**
	 * Returns the statement's event type name
	 * @return the statement's event type name
	 */
	@JMXAttribute(description="The statement return type name", name="EventTypeName", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getEventTypeName() {
		return statement.getEventType().getName();
	}

	/**
	 * Returns the statement's event type
	 * @return the statement's event type
	 */
	@JMXAttribute(description="The statement return type", name="EventType", mutability=AttributeMutabilityOption.READ_ONLY)
	public EventType getEventType() {
		return statement.getEventType();
	}
	
	
	@JMXAttribute(description="The statement property names", name="PropertyNames", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getPropertyNames() {
		String[] names = new String[eventPropertyDescriptors.length];
		int cnt = 0;
		for(EventPropertyDescriptor pd: eventPropertyDescriptors) {
			names[cnt] = pd.getPropertyName();
			cnt++;
		}
		return names;
	}
	
	@JMXAttribute(description="The statement type names", name="TypeNames", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getTypeNames() {
		String[] names = new String[eventPropertyDescriptors.length];
		int cnt = 0;
		for(EventPropertyDescriptor pd: eventPropertyDescriptors) {
			names[cnt] = pd.getPropertyType().getName();
			cnt++;
		}
		return names;
	}
	
	
	@JMXAttribute(description="The timestamp of the last statement change", name="LastTimeChange", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getLastTimeChange() {
		return statement.getTimeLastStateChange();
	}
	
	@JMXAttribute(description="The date of the last statement change", name="LastDateChange", mutability=AttributeMutabilityOption.READ_ONLY)	
	public Date getLastDateChange() {
		return new Date(statement.getTimeLastStateChange());
	}
	
	
	
	/**
	 * Returns the state of the statement.
	 * @return the state of the statement.
	 */
	@JMXAttribute(description="The state of the statement (STOPPED, STARTED, DESTROYED)", name="State", mutability=AttributeMutabilityOption.READ_WRITE)
	public String getState() {
		return this.statement.getState().name();
	}
	
	/**
	 * @param state
	 */
	public void setState(String state) {
		if(EPStatementState.DESTROYED.name().equalsIgnoreCase(state)) {
			// nothing to do. statement is destroyed
		} else if(EPStatementState.STARTED.name().equalsIgnoreCase(state)) {
			if(statement.getState().equals(EPStatementState.STOPPED)) {
				statement.start();
			}
		}  else if(EPStatementState.STOPPED.name().equalsIgnoreCase(state)) {
			if(statement.getState().equals(EPStatementState.STARTED)) {
				statement.stop();
			}
		}
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.metric.MetricEvent#getEngineURI()
	 */
	@JMXAttribute(description="The URI of the engine instance.", name="EngineURI", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getEngineURI() {	
		if(metric.get()==null) return "";
		return metric.get().getEngineURI();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.metric.StatementMetric#getNumOutputIStream()
	 */
	@JMXAttribute(description="Number of insert stream rows output to listeners or the subscriber, if any.", name="NumOutputIStream", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getNumOutputIStream() {
		if(metric.get()==null) return 0;
		return metric.get().getNumOutputIStream();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.metric.StatementMetric#getNumOutputRStream()
	 */
	@JMXAttribute(description="Number of remove stream rows output to listeners or the subscriber, if any.", name="NumOutputRStream", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getNumOutputRStream() {
		if(metric.get()==null) return 0;
		return metric.get().getNumOutputRStream();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.metric.StatementMetric#getStatementName()
	 */
	@JMXAttribute(description="Statement name, if provided at time of statement creation, otherwise a generated name.", name="StatementName", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getStatementName() {
		return statementName;
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.metric.StatementMetric#getTimestamp()
	 */
	@JMXAttribute(description="The current engine time.", name="Timestamp", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTimestamp() {
		if(metric.get()==null) return 0;
		return metric.get().getTimestamp();
	}

	/**
	 * @return
	 * @see com.espertech.esper.client.metric.StatementMetric#getWallTime()
	 */
	@JMXAttribute(description="Statement processing wall time.", name="WallTime", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getWallTime() {
		if(metric.get()==null) return 0;
		return metric.get().getWallTime();
	}

	/**
	 * @return the statementText
	 */
	@JMXAttribute(description="Statement text.", name="StatementText", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getStatementText() {
		return statementText;
	}

	/**
	 * @return the number of listeners registered
	 */
	@JMXAttribute(description="The number of listeners registered.", name="ListenerCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getListenerCount() {
		return ((EPStatementImpl)statement).getListenerSet().getListeners().size();
	}
	
	/**
	 * @return the number of statement aware listeners registered
	 */
	@JMXAttribute(description="The number of statement aware listeners registered.", name="StatementAwareListenerCount", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getStatementAwareListenerCount() {
		return ((EPStatementImpl)statement).getListenerSet().getStmtAwareListeners().size();
	}
	
	/**
	 * @return
	 */
	@JMXAttribute(description="The statement type.", name="StatementType", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getStatementType() {
		return ((EPStatementImpl)statement).getStatementMetadata().getStatementType().toString();
	}
	
	/**
	 * Indicates if a subscriber is attached to this statement
	 * @return true if a subscriber is attached to this statement, false otherwise
	 */
	@JMXAttribute(description="Indicates if a subscriber is registered", name="HasSubscriber", mutability=AttributeMutabilityOption.READ_ONLY)
	public boolean getHasSubscriber() {
		return statement.getSubscriber()!=null;
	}
	
	/**
	 * Stops the statement if it is started
	 */
	@JMXOperation(name="stop", description="Stops the statement if it is started")
	public void stop() {
		if(statement.isStarted()) statement.stop();
	}
	
	/**
	 * Starts the statement if it is stopped
	 */
	@JMXOperation(name="start", description="Starts the statement if it is stopped")
	public void start() {
		if(statement.isStopped()) statement.start();
	}
	
	/**
	 * Destroys the statement
	 */
	@JMXOperation(name="destroy", description="Destroys the statement")
	public void destroy() {
		provider.removeStatementStateListener(this);
		statement.destroy();
	}


	/**
	 * {@inheritDoc}
	 * @see com.espertech.esper.client.EPStatementStateListener#onStatementCreate(com.espertech.esper.client.EPServiceProvider, com.espertech.esper.client.EPStatement)
	 */
	@Override
	public void onStatementCreate(EPServiceProvider provider, EPStatement statement) {
		
	}


	/**
	 * {@inheritDoc}
	 * @see com.espertech.esper.client.EPStatementStateListener#onStatementStateChange(com.espertech.esper.client.EPServiceProvider, com.espertech.esper.client.EPStatement)
	 */
	@Override
	public void onStatementStateChange(EPServiceProvider provider, EPStatement statement) {
		if(this.statement==statement) {
			eventType = this.statement.getEventType();
			if(eventType!=null) eventPropertyDescriptors = eventType.getPropertyDescriptors();	
			if(statement.isDestroyed()) {
				
			}
		}
		
	}
	
	


	


}
