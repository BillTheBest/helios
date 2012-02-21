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
package org.helios.ot.trace;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.ot.deltas.DeltaManager;
import org.helios.ot.subtracer.VirtualTracer;
import org.helios.ot.subtracer.pipeline.IPhaseTrigger;
import org.helios.ot.subtracer.pipeline.Phase;
import org.helios.ot.subtracer.pipeline.Phase.KeyedPhaseTrigger;
import org.helios.ot.trace.types.ByteArrayTraceValue;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.trace.types.IntTraceValue;
import org.helios.ot.trace.types.LongTraceValue;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.type.MetricType;
import org.helios.time.SystemClock;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;



/**
 * <p>Title: Trace</p>
 * <p>Description: Encapsulates the contents of a single trace instance.</p> 
 * <p>The constructor is private and creation of new Traces is forcibly through the factory's <code>getInstance</code> methods. 
 * This is for the future potential of caching commonly used templates of traces. The public constructor is only intended for Externalizable support.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.trace.Trace</code></p>
 */
@XmlRootElement(name="trace")
@XStreamAlias("trace")
public class Trace<T extends ITraceValue> implements Externalizable, Serializable {
	/** A regex expression that will parse the {@code toString()} of a trace instance */
	public static final Pattern TRACE_STR_PATTERN = Pattern.compile("\\[(.*)?](.*)?:(.*)?\\((.*)\\)");
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
	@XmlElement(name="metricId")
	//@XStreamAsAttribute
	protected MetricId metricId;
	/** The trace value */
	//@XmlElement(name="value")
	//@XmlTransient
	@XStreamOmitField
	//@XStreamAlias("value")
	protected T traceValue;
	/** Temporal flag */
	@XmlElement(name="temporal")
	@XStreamAlias("temporal")
	//@XStreamAsAttribute
	protected boolean temporal = false;
	/** urgent flag */
	@XmlElement(name="urgent")
	@XStreamAlias("urgent")
	//@XStreamAsAttribute
	protected boolean urgent = false;
	/** Phase trigger map */
	@XmlTransient
	@XStreamOmitField	
	protected final Map<Phase, Set<IPhaseTrigger>> phaseTriggers = new EnumMap<Phase, Set<IPhaseTrigger>>(Phase.class);
	/** Indicates if there any phase triggers at all */
	@XmlTransient
	@XStreamOmitField
	protected boolean anyPhaseTriggers = false;
	/** The hash code of the phase trigger signature */
	@XmlTransient
	@XStreamOmitField
	protected volatile int phaseTriggerSignature = 0;
	
	/** 
	 * Indicates if there are any phase triggers for any phase
	 * @return true if the trace has any phase triggers 
	 */
	public boolean hasAnyPhaseTriggers() {
		return anyPhaseTriggers;
	}
	
	/**
	 * Returns the phase trigger signature.
	 * 0 means there are no triggers.
	 * @return the phase trigger signature.
	 */
	public int getPhaseTriggerSignature() {
		return phaseTriggerSignature;
	}
	
	/**
	 * Returns an unmodifiable map of phase triggers.
	 * @return an unmodifiable map of phase triggers.
	 */
	Map<Phase, Set<IPhaseTrigger>> getPhaseTriggers() {
		return Collections.unmodifiableMap(phaseTriggers);
	}
	
	/** 
	 * Indicates if there are any phase triggers for the passed phase
	 * @param phase the phase to determine if there are triggers for
	 * @return true if the trace has any phase triggers for the passed phase
	 */ 		
	public boolean hasTriggersFor(Phase phase) {
		if(!anyPhaseTriggers) return false;
		Set<? extends IPhaseTrigger> triggers = phaseTriggers.get(phase);
		return triggers!=null && !triggers.isEmpty();
	}
	
	/**
	 * Adds a trigger to the builders phaseTrigegers.
	 * @param triggers A array of triggers that will be executed when this trace is processed by the annotated phase.
	 * @return this Trace
	 */
	private Trace addPhaseTriggers(IPhaseTrigger...triggers) {
		
		if(triggers!=null) {
			StringBuilder b = new StringBuilder(triggers.length*4);
			for(KeyedPhaseTrigger trigger: Phase.createPhaseTriggersFor(triggers)) {
				Set<IPhaseTrigger> set = phaseTriggers.get(trigger.getPhase());
				if(set==null) {
					set = new HashSet<IPhaseTrigger>();
					phaseTriggers.put(trigger.getPhase(), set);
					b.append(trigger.hashCode());
				}
				set.add(trigger.getTrigger());
				anyPhaseTriggers = true;
			}						
			phaseTriggerSignature = b.toString().hashCode();
		}
		return this;
	}
	
	/**
	 * Returns a set of the phase triggers for the passed phase
	 * @param phase The phase to get triggers for
	 * @return A possibly empty set of triggers
	 */
	@SuppressWarnings("unchecked")
	public Set<IPhaseTrigger> getTriggersForPhase(Phase phase) {
		if(phase==null) {
			throw new IllegalArgumentException("The passed phase was null", new Throwable());
		}
		Set<IPhaseTrigger> set = phaseTriggers.get(phase);
		return (Set<IPhaseTrigger>) (set==null 
			? Collections.emptySet() 
			: Collections.unmodifiableSet(set));
	}
	
	/**
	 * Executes the phase trigegers for the passed phase
	 * @param phase The phase to run triggers for
	 */
	public void runPhaseTriggers(Phase phase) {
		if(phase==null) {
			throw new IllegalArgumentException("The passed phase was null", new Throwable());
		}		
		for(IPhaseTrigger trigger: getTriggersForPhase(phase)) {
			trigger.phaseTrigger(phase.name(), this);
		}
	}
	
	/** The effective timestamp of the metric */
	@XmlElement(name="timeStamp")
	@XStreamAlias("timeStamp")
	//@XStreamAsAttribute
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
	public Trace() {
	}

	/**
	 * Renders the trace as a name/value map
	 * @return A map of trace attributes keyed by header constant name
	 */
	public Map<String, Object> getTraceMap() {
		Map<String, Object> map = new HashMap<String, Object>(16);
		map.putAll(metricId.getTraceMap());
		map.put(TRACE_TS, timeStamp);
		map.put(TRACE_DATE, new Date(timeStamp).toString());
		map.put(TRACE_SVALUE, traceValue.toString());
		map.put(TRACE_VALUE, traceValue.getValue());
		map.put(TRACE_TEMPORAL, temporal);
		map.put(TRACE_URGENT, urgent);
		map.put(TRACE_MODEL, false);
		return map;
	}
	



	/**
	 * Determines if this is an interval trace
	 * @return true if this is an interval trace, false otherwise
	 */
	public boolean isInterval() {
		return this instanceof IntervalTrace;
	}
	
	/**
	 * Returns the local name. That is the fully qualified name, minus the host and agent.
	 * @return the local name
	 */
	public String getLocalName() {
		return metricId.getLocalName();
	}
	
	
	
	/**
	 * Retrieves the primary value
	 * @return the primary value 
	 */
	public Object getValue() {
		return traceValue.getValue();
	}
	
	/**
	 * Retrieves the TraceValue
	 * @return the TraceValue
	 */
	public T getTraceValue() {
		return traceValue;
	}
	
	
	
	/**
	 * Retrieves the value as a string
	 * @return the value as a string
	 */
	public String getStringValue() {
		return traceValue.getValue().toString();
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
	 * Builds a trace from a map of Objects keyed by the TraceMap constants
	 * @param map a map of Objects keyed by the TraceMap constants
	 * @return a Trace
	 */
	public static Trace build(Map<String, Object> map) {
		if(map==null) throw new IllegalArgumentException("Passed map was null", new Throwable());
		return new Trace.Builder(
				getMapValue(TRACE_SVALUE, map), 
				MetricType.typeForCode((Integer)getMapValue(MetricId.TRACE_TYPE_CODE, map)), 
				getMapValue(MetricId.TRACE_FULLNAME, map).toString().split(Trace.DELIM))
			.urgent((Boolean)getMapValue(Trace.TRACE_URGENT, map))
			.temporal((Boolean)getMapValue(Trace.TRACE_TEMPORAL, map))
			.timeStamp((Long)getMapValue(Trace.TRACE_TS, map))
		.build();
	}
	
//	map.putAll(metricId.getTraceMap());
//	map.put(TRACE_TS, timeStamp);
//	map.put(TRACE_DATE, new Date(timeStamp).toString());
//	map.put(TRACE_SVALUE, value.toString());
//	map.put(TRACE_VALUE, value.getValue());
//	map.put(TRACE_TEMPORAL, temporal);
//	map.put(TRACE_URGENT, urgent);
//	map.put(TRACE_MODEL, false);
	
//	map.put(TRACE_FQN, getFQN());
//	map.put(TRACE_POINT, metricName);
//	String localNameSpace = StringHelper.fastConcatAndDelim(Trace.DELIM, namespace);
//	String fullNameSpace = StringHelper.fastConcatAndDelim(Trace.DELIM, hostName, agentName, localNameSpace);
//	map.put(TRACE_NAMESPACE, fullNameSpace);
//	map.put(TRACE_LNAMESPACE, localNameSpace);
//	map.put(TRACE_FULLNAME, StringHelper.fastConcatAndDelim(Trace.DELIM, localNameSpace, metricName));				
//	map.put(TRACE_APP_ID, agentName);
//	map.put(TRACE_HOST, hostName);
//	map.put(TRACE_TYPE_NAME, type.name());
//	map.put(TRACE_TYPE_CODE, type.getCode());
	
	
	
	/**
	 * Safe extract from the map
	 * @param key The key
	 * @param map The map to extract the value from
	 * @return The value
	 * @param <T> The expected return type
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getMapValue(String key, Map<String, Object> map) {
		T t = (T)map.get(key);
		if(t==null) throw new RuntimeException("Trace Map Builder Had Null Value for key [" + key + "]", new Throwable());
		return t;
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
		private volatile Trace t = null;
		
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
		private final ITraceValue value;
		private final MetricType metricType;
		private boolean temporal = false;
		private boolean urgent = false;
		private ITracer itracer;
		
//		 Indicates the the trace has been built and sent,
//		 so no additional builder handling is necessary
		
		// State
		private boolean baseOverriden = false;
		// Virtual Tracer
		private String agent = null;
		private String host = null;
		
		// Phase triggers
		/** Phase trigger map */
		protected final Map<Phase, Set<IPhaseTrigger>> phaseTriggers = new EnumMap<Phase, Set<IPhaseTrigger>>(Phase.class);
		/** Indicates if there any phase triggers at all */
		protected boolean anyPhaseTriggers = false;
		
		/**
		 * Adds an array of phase triggers the builders phaseTriggers.
		 * @param triggers A array of KeyedPhaseTriggers that will be executed when this trace is processed by the annotated phase.
		 * @return this Builder
		 */
		public Builder addPhaseTriggers(KeyedPhaseTrigger...triggers) {
			if(triggers!=null) {
				for(KeyedPhaseTrigger trigger: triggers) {
					if(trigger!=null) {
						Set<IPhaseTrigger> set = phaseTriggers.get(trigger.getPhase());
						if(set==null) {
							set = new HashSet<IPhaseTrigger>();
							phaseTriggers.put(trigger.getPhase(), set);
						}
						set.add(trigger.getTrigger());
						anyPhaseTriggers = true;						
					}
				}
			}
			return this;
		}

		
		/**
		 * Creates a new Trace.Builder
		 * @param value The ITraceValue for this trace
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		private Builder(ITraceValue value, MetricType metricType, String...metricName) {
			assert value!=null;
			assert metricType!=null;
			assert (metricName!=null && metricName.length>0); 
			//assert metricType.getValueType().getTraceValueClass().isAssignableFrom(value.getClass());
			this.value = value;			
			timeStamp = SystemClock.time();
			this.metricType = metricType;
			point = metricName[metricName.length-1];
			for(int i = 0; i < metricName.length-1; i++) {
				nameSpace.add(metricName[i]);
			}			
		}
		
		/**
		 * Overrides the assigned timestamp
		 * @param ts A timestamp
		 * @return this builder
		 */
		protected Builder timeStamp(long ts) {
			timeStamp = ts;
			return this;
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(int value, MetricType metricType, String...metricName) {
			this(new IntTraceValue(value), metricType, metricName);
		}

		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(long value, MetricType metricType, String...metricName) {
			this(new LongTraceValue(value), metricType, metricName);
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(String value, MetricType metricType, String...metricName) {
			this(metricType.traceValue(value), metricType, metricName);			
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(byte[] value, MetricType metricType, String...metricName) {
			this(new ByteArrayTraceValue(value), metricType, metricName);
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type
		 * @param metricName The metric point, optionally prefixed with members of the namespace.
		 */
		public Builder(InputStream value, MetricType metricType, String...metricName) {
			this(MetricType.traceValue(value, metricType), metricType, metricName);
		}
		
		/**
		 * Construct a new Trace.Builder
		 * @param value The value of the metric.
		 * @param metricType The metric type.
		 * @param metricName The name of the metric or metric name fragments
		 */
		public Builder(Object value, MetricType metricType, String...metricName) {
			this(metricType.traceValue(value), metricType, metricName);			
		}
		
		
		/**
		 * Overrides the trace host and agent based on the passed virtual tracer
		 * @param vtracer The virtual tracer
		 * @return this builder
		 */
		public Builder virtualize(VirtualTracer vtracer) {
			this.host = vtracer.getVirtualHost();
			this.agent = vtracer.getVirtualAgent();
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
		public Builder format(ITracer tracer) {
			if(tracer==null) return null;			
			return tracer.format(this);
		}
		

		/**
		 * The tracer impl. passes itself in here for builder traces.
		 * @param itracer
		 * @return this builder
		 */
		public Builder setITracer(ITracer itracer) {
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
			//if(itracer==null) throw new RuntimeException("The ITracer is null so the trace cannot be executed. [Programmer Error ?]");
			Trace trace = build();
			//itracer.traceTrace(trace);
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
			b.append(clean(point));
			return b.toString().intern();
		}
		
		public static final Pattern DELIM_PATTERN = Pattern.compile(DELIM);
		
		public static final String clean(String s) {
			if(s==null) return null;
			return DELIM_PATTERN.matcher(s).replaceAll("\\\\");
		}
		
		/**
		 * Builds a new trace from the builder.
		 * @return A new trace or null if the itracer vetoed the metric.
		 */
		public Trace build() {			
			if(t!=null) {
				return t;
			}
//			if(itracer!=null) {
//				if(itracer.format(this)==null) {
//					return null;
//				}
//			}
			t = new Trace();
			t.metricId = MetricId.getInstance(metricType, buildMetricName());
			if(accumulatorBitMask!=-1) {
				t.metricId.setTracerMask(accumulatorBitMask);
			}
			t.temporal = temporal;
			t.timeStamp = timeStamp;
			t.traceValue = value; //metricType.traceValue(value);
			t.urgent = urgent;
			if(anyPhaseTriggers) {
				t.phaseTriggers.putAll(phaseTriggers);
				t.anyPhaseTriggers = true;
			}			
			return t;
		}
		
		public boolean isBuilt() {
			return t!=null;
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
		buff.append("(").append(timeStamp).append(")");
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
			traceValue = (T)in.readObject();
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
		out.writeObject(traceValue);
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
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getAgentName()
	 */
	public String getAgentName() {
		return metricId.getAgentName();
	}

	/**
	 * @return
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getFQN()
	 */
	public String getFQN() {
		return metricId.getFQN();
	}

	/**
	 * @return
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getHostName()
	 */
	public String getHostName() {
		return metricId.getHostName();
	}

	/**
	 * @return
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getMetricName()
	 */
	public String getMetricName() {
		return metricId.getMetricName();
	}

	/**
	 * @return
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getMod()
	 */
	public int getMod() {
		return metricId.getMetricMod();
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
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getNamespace()
	 */
	public String[] getNamespace() {
		return metricId.getNamespace();
	}

	/**
	 * @return
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getSerial()
	 */
	public int getSerial() {
		return metricId.getSerial();
	}

	/**
	 * @return
	 * @see org.helios.ot.trace.tracing.trace.MetricId#getType()
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
	
	
	
	/**
	 * Copy Constructor
	 *
	 * @param trace a <code>Trace</code> object
	 */
	protected Trace(Trace trace) {
	    this.metricId = trace.metricId;
	    if(trace instanceof IntervalTrace) {
	    	this.traceValue = (T) ((IntervalTrace)trace).intervalTraceValue;
	    } else {
	    	this.traceValue = (T) trace.traceValue;
	    }
	    
	    this.temporal = trace.temporal;
	    this.urgent = trace.urgent;
	    this.timeStamp = trace.timeStamp;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((metricId == null) ? 0 : metricId.hashCode());
		result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
		result = prime * result
				+ ((traceValue == null) ? 0 : traceValue.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trace other = (Trace) obj;
		if (metricId == null) {
			if (other.metricId != null)
				return false;
		} else if (!metricId.equals(other.metricId))
			return false;
		if (timeStamp != other.timeStamp)
			return false;
		if (traceValue == null) {
			if (other.traceValue != null)
				return false;
		} else if (!traceValue.equals(other.traceValue))
			return false;
		return true;
	}

	

}
