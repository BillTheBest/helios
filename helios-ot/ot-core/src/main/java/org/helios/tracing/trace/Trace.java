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
package org.helios.tracing.trace;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.tracing.TracerImpl;
import org.helios.tracing.TraceValue;
import org.helios.tracing.deltas.DeltaManager;
import org.helios.tracing.subtracer.IVirtualTracer;



/**
 * <p>Title: Trace</p>
 * <p>Description: Encapsulates the contents of a single trace instance.</p> 
 * <p>The constructor is private and creation of new Traces is forcibly through the factory's <code>getInstance</code> methods. 
 * This is for the future potential of caching commonly used templates of traces. The public constructor is only intended for Externalizable support.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead DOT nicholas AT gmail DOT com)
 */
public class Trace implements Externalizable, Serializable {
	
	/**
	 * @TODO
	 * =====
	 * Tags:
	 * 		byte[] mimetype
	 * 		byte[] subtype
	 * 		Actual Tracer for subtracers (host/agent)
	 * 
	 * Do we need to transfer tags in every trace instance ?
	 */
	
	
	/** Static class logger */
	protected static transient final Logger log = Logger.getLogger(Trace.class);
	/** The trace metric Id */
	protected MetricId metricId;
	/** The trace value */
	protected TraceValue traceValue;
	
	/** Temporal flag */
	protected boolean temporal = false;
	/** urgent flag */
	protected boolean urgent = false;
	
	/** The effective timestamp of the metric */
	protected long timeStamp = -1L;
	/** The delimeter between namespace entries */
	public static final String DELIM = "/";
	/** The delimeter before the metric value */
	public static final String VALUE_DELIM = ":";
	
	/** The header constant name for the Trace timestamp */
	public static final String TRACE_TS = "timestamp";
	/** The header constant name for the Trace date */
	public static final String TRACE_DATE = "date";
	/** The header constant name for the Trace value as a string */
	public static final String TRACE_SVALUE = "svalue";
	/** The header constant name for the Trace value in native type */
	public static final String TRACE_VALUE = "value";	
	/** The header constant name for the Trace temporal flag */
	public static final String TRACE_TEMPORAL = "temporal";
	/** The header constant name for the Trace urgent flag */
	public static final String TRACE_URGENT = "urgent";
	/** The header constant name for the Trace model flag */
	public static final String TRACE_MODEL = "model";
	
	static final String[] EMPTY_STR_ARR = new String[]{};

	/**
	 * Public parameterless constructor.
	 * Only used for externalization support and builder.
	 */
	public Trace() {}
	
	/**
	 * Renders the trace as a name/value map
	 * @return A map of trace attributes keyed by header constant name
	 */
	public Map<String, Object> getTraceMap() {
		Map<String, Object> map = new HashMap<String, Object>(16);
		map.putAll(metricId.getTraceMap());
		map.put(TRACE_TS, timeStamp);
		map.put(TRACE_DATE, new Date(timeStamp));
		map.put(TRACE_SVALUE, traceValue.toString());
		map.put(TRACE_VALUE, traceValue.getValue());
		map.put(TRACE_TEMPORAL, temporal);
		map.put(TRACE_URGENT, urgent);
		map.put(TRACE_MODEL, false);
		return map;
	}
	
	/**
	 * The generic representation of the trace measurement.
	 * @return the value
	 */
	public Object getValue() {
		return traceValue.getValue();
	}
	
	/**
	 * Returns the local name. That is the fully qualified name, minus the host and agent.
	 * @return the local name
	 */
	public String getLocalName() {
		return metricId.getLocalName();
	}
	
	
	
	/**
	 * Retrieves the value as a long
	 * @return the value as a long
	 */
	public long getLongValue() {
		return traceValue.getLongValue();
	}
	
	/**
	 * Retrieves the value as an int
	 * @return the value as an int
	 */
	public int getIntValue() {
		return traceValue.getIntValue();
	}
	
	/**
	 * Returns either an int or a long type as a long
	 * @return the numeric value expressed as a long
	 */
	public long getNumericValue() {
		return traceValue.getNumericValue();
	}
	
	
	/**
	 * Retrieves the value as a string
	 * @return the value as a string
	 */
	public String getStringValue() {
		return traceValue.getStringValue();
	}

	/**
	 * Retrieves the value as a byte array
	 * @return the value as a byte array
	 */
	public byte[] getByteArrayValue() {
		return traceValue.getByteArrValue();
	}
	
	
	
	/**
	 * Construct a new Trace.Builder
	 * @param value The value of the metric.
	 * @param metricType The metricType
	 * @param metricName The name of the metric or the metric name fragments
	 * @return A Trace Builder instance.
	 */
	public static Builder build(int value, MetricType metricType, String...metricName) {
		return new Trace.Builder(value, metricType, metricName);
	}
	
	/**
	 * Construct a new Trace.Builder
	 * @param value The value of the metric.
	 * @param metricType The metricType
	 * @param metricName The name of the metric or the metric name fragments
	 * @return A Trace Builder instance.
	 */
	public static Builder build(long value, MetricType metricType, String...metricName) {
		return new Trace.Builder(value, metricType, metricName);
	}
	
	/**
	 * Construct a new Trace.Builder
	 * @param value The value of the metric.
	 * @param metricType The metricType
	 * @param metricName The name of the metric or the metric name fragments
	 * @return A Trace Builder instance.
	 */
	public static Builder build(String value, MetricType metricType, String...metricName) {
		return new Trace.Builder(value, metricType, metricName);
	}
	
	/**
	 * Construct a new Trace.Builder
	 * @param value The value of the metric.
	 * @param metricType The metricType
	 * @param metricName The name of the metric or the metric name fragments
	 * @return A Trace Builder instance.
	 */
	public static Builder build(byte[] value, MetricType metricType, String...metricName) {
		return new Trace.Builder(value, metricType, metricName);
	}
	
	/**
	 * Construct a new Trace.Builder
	 * @param value The value of the metric.
	 * @param metricType The metricType
	 * @param metricName The name of the metric or the metric name fragments
	 * @return A Trace Builder instance.
	 */
	public static Builder build(InputStream value, MetricType metricType, String...metricName) {
		return new Trace.Builder(value, metricType, metricName);
	}
	
	/**
	 * Construct a new Trace.Builder
	 * @param value The value of the metric.
	 * @param metricType The metricType
	 * @param metricName The name of the metric or the metric name fragments
	 * @return A Trace Builder instance.
	 */
	public static Builder buildFromObject(Object value, MetricType metricType, String...metricName) {
		return new Trace.Builder(value, metricType, metricName);
	}
	
	
	
	
	
	//=======================================================================================
	//   Builder Implementation
	//=======================================================================================
	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description: Implements a builder pattern for the Trace class.</p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Whitehead (whitehead.nicholas@gmail.com)
	 * @version $LastChangedRevision: 1718 $
	 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-opentrace-pre-maven/branches/DEV-0.1/src/org/helios/tracing/Trace.java $
	 * $Id: Trace.java 1718 2009-11-11 17:50:21Z nwhitehead $
	 */
	public static class Builder {
		
		private long timeStamp = 0L;
		
		// ==============================
		// MetricId Name components
		// Host and Agent default to MetricId _host and _agent
		// unless overriden by a virtual agent setting
		// ==============================
		private final String point;
		private final LinkedList<String> nameSpace = new LinkedList<String>();
		// ==============================
		
		
		/** The accumulator filtering bit mask */
		private long accumulatorBitMask = -1;
		private final TraceValue value;
		private final MetricType metricType;
		private boolean temporal = false;
		private boolean urgent = false;
		// State
		private boolean baseOverriden = false;
		// ITracer
		private TracerImpl itracer = null;
		// Virtual Tracer
		private String agent = null;
		private String host = null;
		
		/**
		 * Creates a new Trace.Builder
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		private Builder(TraceValue value, MetricType metricType, String...metricName) {
			this.value = value;
			timeStamp = System.currentTimeMillis();
			this.metricType = metricType;
			if(metricName==null || metricName.length<1) {
				throw new RuntimeException("Trace.Builder created with null or zero length metric name array");
			}
			point = metricName[metricName.length-1];
			for(int i = 0; i < metricName.length-1; i++) {
				nameSpace.add(metricName[i]);
			}			
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(int value, MetricType metricType, String...metricName) {
			this(new TraceValue(value), metricType, metricName);
		}

		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(long value, MetricType metricType, String...metricName) {
			this(new TraceValue(value), metricType, metricName);
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(String value, MetricType metricType, String...metricName) {
			this(new TraceValue(value), metricType, metricName);
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(byte[] value, MetricType metricType, String...metricName) {
			this(new TraceValue(value), metricType, metricName);
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(InputStream value, MetricType metricType, String...metricName) {
			this(new TraceValue(value), metricType, metricName);
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type.
		 * @param metricName The name of the metric or metric name fragments
		 */
		public Builder(Object value, MetricType metricType, String...metricName) {
			this(TraceValue.create(metricType, value), metricType, metricName);			
		}
		
		
		//=====================================================================
		//  Commenting these since they should only be set by the virtual tracer.
		//=====================================================================
		
//		/**
//		 * Overrides the tracer's host.
//		 * @param host The new host name.
//		 * @return the builder.
//		 */
//		public Builder host(String host) {
//			this.host = host;
//			baseOverriden = true;
//			return this;
//		}
//
//		/**
//		 * Overrides the tracer's agent.
//		 * @param host The new agent name.
//		 * @return the builder.
//		 */
//		public Builder agent(String agent) {
//			this.agent = agent;
//			baseOverriden = true;
//			return this;
//		}
		
		public Builder virtual(IVirtualTracer ivt) {
			agent = ivt.getVirtualAgent();
			host = ivt.getVirtualHost();
			baseOverriden = true;
			return this;
		}
		
		
		
		/**
		 * Sets the temporal to true for the builder.
		 * @return the builder.
		 */
		public Builder temporal() {
			this.temporal = true;
			return this;
		}
		
		/**
		 * Sets the temporal for the builder.
		 * @param temporal
		 * @return the builder.
		 */
		public Builder temporal(boolean temporal) {
			this.temporal = temporal;
			return this;
		}
		
		/**
		 * Sets the accumulator bit mask on the metric Id
		 * @param accumulatorBitMask The accumulator filtering bit mask 
		 * @return this builder
		 */
		public Builder bitMask(long accumulatorBitMask) {
			this.accumulatorBitMask = accumulatorBitMask;
			return this;
		}
		
		/**
		 * Sets the urgent  to true for the builder.
		 * @return the builder.
		 */
		public Builder urgent() {
			this.urgent = true;
			return this;
		}
		
		/**
		 * Sets the urgent for the builder.
		 * @param urgent
		 * @return the builder.
		 */
		public Builder urgent(boolean urgent) {
			this.urgent = urgent;
			return this;
		}		
		
		/**
		 * Allows a tracer to adjust a trace before it is fully built.
		 * @param tracer The tracer invoking the builder.
		 * @return the modified builder.
		 */
		public Builder format(TracerImpl tracer) {
			return tracer.format(this);
		}
		

		/**
		 * The tracer impl. passes itself in here for builder traces.
		 * @param itracer
		 * @return this builder
		 */
		public Builder setITracer(TracerImpl itracer) {
			this.itracer = itracer;
			return this;
		}
		
		/**
		 * Adds the passed segments to the end of the namespace.
		 * @param segments The name space segments to append on the end of the namespace to be built.
		 * @return this builder
		 */
		public Builder segment(String...segments) {
			if(segments!=null) {
				for(String s: segments) {
					if(s!=null && s.length()>0) {
						nameSpace.addLast(s.trim());
					}
				}
			}
			return this;
		}
		
		/**
		 * Prepends the passed segments to the begining of the namespace.
		 * @param segments The name space segments to preifx on the begining of the namespace to be built.
		 * @return this builder
		 */
		public Builder prefix(String...segments) {
			if(segments!=null) {
				for(int i = segments.length-1; i > -1; i-- ) {				
					if(segments[i]!=null && segments[i].length()>0) {
						nameSpace.addFirst(segments[i].trim());
					}
				}
			}
			return this;
		}
		
		/**
		 * Build and trace the build trace.
		 * @return the built trace.
		 */
		public Trace trace() {
			if(itracer==null) throw new RuntimeException("The ITracer is null so the trace cannot be executed. [Programmer Error ?]");
			Trace trace = build();
			itracer.traceTrace(trace);
			return trace;
		}
		
		/**
		 * Builds the metric name from the Builder collected fragments
		 * @return The fully qualified metric name
		 */
		private String buildMetricName() {
			StringBuilder b = new StringBuilder();
			if(!baseOverriden) {
				b.append(MetricId._hostName).append(DELIM).append(MetricId._applicationId).append(DELIM);
			} else {
				b.append(host).append(DELIM).append(agent).append(DELIM);
			}
			for(String s: nameSpace) {
				b.append(s).append(DELIM);
			}
			b.append(point);
			return b.toString();
		}
		
		/**
		 * Builds a new trace from the builder.
		 * @return A new trace or null if the itracer vetoed the metric.
		 */
		public Trace build() {					
			if(itracer!=null) {
				if(itracer.format(this)==null) {
					return null;
				}
			}
			Trace t = new Trace();
			t.metricId = MetricId.getInstance(metricType, buildMetricName());
			if(accumulatorBitMask!=-1) {
				t.metricId.setTracerMask(accumulatorBitMask);
			}
			t.temporal = temporal;
			t.timeStamp = timeStamp;
			t.traceValue = value;
			t.urgent = urgent;
			return t;
		}
		
		/**
		 * Determines the delta of the passed value for the passed keys against the value in state and stores the passed value in state.
		 * If no value is held in state, or the in state value is greater than the new value, the new value is placed in state and a null is returned.
		 * @param namespace The fully qualified metric namespace.
		 * @param value The new int value
		 * @return The delta of the passed value against the value in state, or a null.
		 */
		@JMXOperation (name="deltaInt", description="Processes an int delta.")
		public synchronized Integer deltaInt(
				@JMXParameter(name="namespace", description="The fully qualified metric namespace.") String namespace, 
				@JMXParameter(name="value", description="The new int value") int value) { 		
			Number n = DeltaManager.getInstance().delta(namespace, value, MetricType.DELTA_INT_AVG);
			if(n==null) return null;
			else return n.intValue();
		}
		
		/**
		 * Determines the delta of the passed value for the passed keys against the value in state and stores the passed value in state.
		 * If no value is held in state, or the in state value is greater than the new value, the new value is placed in state and a null is returned.
		 * @param namespace The fully qualified metric namespace.
		 * @param value The new long value
		 * @return The delta of the passed value against the value in state, or a null.
		 */
		@JMXOperation (name="deltaLong", description="Processes a long delta.")
		public synchronized Long deltaLong(
				@JMXParameter(name="namespace", description="The fully qualified metric namespace.") String namespace, 
				@JMXParameter(name="value", description="The new long value") long value) { 		
			Number n = DeltaManager.getInstance().delta(namespace, value, MetricType.DELTA_LONG_AVG);
			if(n==null) return null;
			else return n.longValue();
		}	
				
		
	}
	
	
	//=======================================================================================
	


	
	public static void log(Object message) {
		System.out.println(message);
	}
	
	
	
	
	/**
	 * Generates a String representation of the Trace.
	 * @return A string.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder(metricId.toString());
		buff.append(VALUE_DELIM).append(traceValue.toString());
		buff.append(")").append(timeStamp).append(")");
		return buff.toString();		
	}
	
	
	/**
	 * The object implements the readExternal method to restore its contents by calling the methods of 
	 * DataInput for primitive types and readObject for objects, strings and arrays. 
	 * @param in the stream to write the object to 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		try {
			metricId = new MetricId();
			metricId.readExternal(in);
			traceValue = new TraceValue();
			traceValue.readExternal(in);
			timeStamp = in.readLong();
			temporal = in.readBoolean();
			urgent = in.readBoolean();			
		} catch (Exception e) {
			log.fatal("Failed to ReadExternal on instance of " + getClass().getName(), e);
			throw new RuntimeException("Failed to ReadExternal on instance of " + getClass().getName(), e);			
		}
		
	}

	/**
	 * The object implements the readExternal method to restore its contents by calling the methods of 
	 * DataInput for primitive types and readObject for objects, strings and arrays. 
	 * The readExternal method must read the values in the same sequence and with the same types as were written by writeExternal.  
	 * @param out the stream to read data from in order to restore the object 
	 * @throws IOException
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		metricId.writeExternal(out);
		traceValue.writeExternal(out);
		out.writeLong(timeStamp);
		out.writeBoolean(temporal);
		out.writeBoolean(urgent);
	}


	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return timeStamp;
	}
	
	/**
	 * @return
	 */
	public boolean isTemporal() {
		return temporal;
	}
	
	/**
	 * @return
	 */
	public boolean isUrgent() {
		return urgent;
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getAgentName()
	 */
	public String getAgentName() {
		return metricId.getAgentName();
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getFQN()
	 */
	public String getFQN() {
		return metricId.getFQN();
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getHostName()
	 */
	public String getHostName() {
		return metricId.getHostName();
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getMetricName()
	 */
	public String getMetricName() {
		return metricId.getMetricName();
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getMod()
	 */
	public int getMod() {
		return metricId.getMod();
	}
	
	/**
	 * Returns the trace metric type
	 * @return the trace metric type
	 */
	public MetricType getMetricType() {
		return metricId.getType();
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getNamespace()
	 */
	public String[] getNamespace() {
		return metricId.getNamespace();
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getSerial()
	 */
	public int getSerial() {
		return metricId.getSerial();
	}

	/**
	 * @return
	 * @see org.helios.tracing.trace.MetricId#getType()
	 */
	public MetricType getType() {
		return metricId.getType();
	}

	/**
	 * Returns the trace MetricId
	 * @return the metricId
	 */
	public MetricId getMetricId() {
		return metricId;
	}	
	

}
