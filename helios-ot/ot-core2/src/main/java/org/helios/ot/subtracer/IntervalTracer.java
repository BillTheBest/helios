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
package org.helios.ot.subtracer;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.helpers.ClassHelper;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.Trace.Builder;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerImpl;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: IntervalTracer</p>
 * <p>Description: SubTracer that supresses trace propagation and instead submits all traces to the AccumulatorManager.</p>
 * <p><b><i></i><font color='red'>NOTE:</font></i></b> For the time being, this needs to be the <i>last</i> subtracer since it returns null. 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.IntervalTracer</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class IntervalTracer extends DelegatingTracer {
	/** A reference to the interval accumulator */
	protected static volatile IntervalAccumulator ia = null;
	/** indicates if the vtracer is the root */
	protected final boolean isParentRoot;
	
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(IntervalTracer.class);
	
	/**
	 * Calls a manual flush on the Interval Accumulator
	 */
	public void flush() {
		if(ia==null) {
			throw new IllegalStateException("The IntervalAccumulator is not started", new Throwable());
		}
		ia.flush();
	}
	
	/**
	 * Clears all registered interval traces from the accumulator map channel
	 */
	@JMXOperation(name="resetMapChannel", description="Clears all registered interval traces from the accumulator map channel")
	public void resetMapChannel() {
		if(ia!=null) ia.resetMapChannel();
	}
	/**
	 * Returns the IntervalTracer appropriate to be a subtracer for the passed ITracer.
	 * @param innerTracer The inner tracer
	 * @return an IntervalTracer
	 * @ To Do: This should genericizable 
	 */
	public static IntervalTracer getInstance(ITracer innerTracer) {		
		ClassHelper.nvl(innerTracer, "Passed vtracer was null");
		ObjectName on = createObjectName(IntervalTracer.class, innerTracer);
		IntervalTracer tracer = (IntervalTracer) SUB_TRACERS.get(on);
		if(tracer==null) {
			tracer = (IntervalTracer) SUB_TRACERS.get(on);
			if(tracer==null) {
				tracer = new IntervalTracer(innerTracer, IntervalTracer.class.getSimpleName(), on);
				SUB_TRACERS.put(on, tracer);
			}
		}
		return tracer;
	}
	
	/**
	 * Creates a new IntervalTracer 
 	 * @param vtracer The wrapped inner tracer
	 * @param tracerName The tracer name
	 * @param tracerObjectName The tracer ObjectName

	 */
	private IntervalTracer(ITracer vtracer, String tracerName, ObjectName tracerObjectName) {
		super(vtracer, tracerName, tracerObjectName);
		isParentRoot = (TracerImpl.class.equals(vtracer.getClass()));
		//ia = IntervalAccumulator.getInstance();		
	}

	/**
	 * Builds the trace, sends it to the AccumulatorManager and returns null.
	 * @param builder The trace builder
	 * @return null.
	 */
	@Override
	public Builder subformat(Builder builder) {		
		if(ia!=null) {
			ia.submit(builder.build());
			sendCounter.incrementAndGet();
		}		
		return isParentRoot ? NULL_BUILDER : builder;
	}

	/**
	 * Sets the IntervalAccumulator when it created and nulls when it shuts down
	 * @param ia A new IntervalAccumulator on start and null on shutdown
	 */
	public static void setIntervalAccumulator(IntervalAccumulator newia) {
		if(ia!=null && newia!=null) {
			ia = newia;
			return;
		}

		if(ia==null && newia!=null) {
			ia = newia;
			LOG.info("\n\tIntervalTracer's IA Started");
		} else if(ia!=null && newia==null ) {
			ia=null;
			LOG.info("\n\tIntervalTracer's IA Shutdown");
	 	} else {
	 		Throwable t = new Throwable();
	 		String msg = "\n\tIntervalTracer invalid IA state change: this IA[" + ia==null ? "null" : "set" + "]  new IA[" + newia==null ? "null" : "set" + "]";
	 		LOG.error(msg, t);
	 		throw new IllegalStateException(msg, t);
	 	}
	}
	
	protected static final IntervalBuilder NULL_BUILDER = new IntervalBuilder();
	
	public static class IntervalBuilder extends Builder {

		public IntervalBuilder() {
			super(new Integer(1), MetricType.INT_AVG, new String[]{"NullBuilder"});
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.ot.trace.Trace.Builder#build()
		 */
		@SuppressWarnings("unchecked")
		public Trace build() {
			return null;
		}

		
	}

}
