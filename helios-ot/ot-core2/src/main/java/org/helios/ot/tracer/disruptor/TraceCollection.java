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
package org.helios.ot.tracer.disruptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.helios.ot.endpoint.IEndPoint;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.interval.IntervalAccumulator;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.TracerManager3.SubmissionContext;


/**
 * <p>Title: TraceCollection</p>
 * <p>Description: A batch manager for submitting batches of ITraces to be transmitted to registered endpoints.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.tracer.disruptor.TraceCollection</code></p>
 */
@SuppressWarnings("unchecked")
public class TraceCollection<T extends Trace<? extends ITraceValue>>  implements Iterable<T>, Callable<Void> { //implements EventTranslator<TraceCollection>  {
	/** A Trace Collection Serial Number Factory */
	private static final AtomicLong serialFactory = new AtomicLong(0);
	/** Static instance logger */
	private static final Logger LOG = Logger.getLogger(TraceCollection.class);
	/** This trace collection's serial number */
	public final long serial = serialFactory.incrementAndGet();
	/** A set of traces to be processed  */
	protected final Set<T> traces;
	/** A counter for context tag array references */
	private final AtomicInteger contextTagSerial = new AtomicInteger(-1);
	/** context tag array references */
	private final Object[] contextTags = new Object[MAX_TAG_INDEX+1];
	/** The maximum number of context tag entries */
	public static final int MAX_TAG_INDEX = 100;
	/** the current sequence */
	private long sequence = -1;
	/** This traceCollection's SubmissionContext */
	protected transient SubmissionContext submissionContext = null;
	/** The countdown completion counter, counted down by each multi-threaded invoked endpoint. Whichever one decrements to zero calls the closer */
	protected final transient AtomicInteger completionCountdown = new AtomicInteger(0);
	
	
	/**
	 * Sets the submissionContext to use for submissions
	 * @param submissionContext the submissionContext to use for submissions
	 */
	public void setSubmissionContext(SubmissionContext submissionContext) {
		this.submissionContext = submissionContext;
	}
	
	/**
	 * Submits this trace collection
	 */
	public void submit() {
		if(submissionContext!=null) {
		submissionContext
			.getExecutor()
			.submit(this);
		}
	}
	
	/**
	 * Executes the TraceCollection submission against all the endpoints defined in the submission context
	 * @return Void (for now)
	 * @throws Exception thrown on any errors processing the TraceCollection
	 */
	@Override
	public Void call() throws Exception {
		if(!submissionContext.isMultiThreaded()) {
			for(IEndPoint<T> endpoint: submissionContext.getEndPoints()) {
				endpoint.processTraces(this);    // TODO: We don't need a new copy of the traces for each endpoint, just an unmodifiable set.
			}
			TraceCollectionCloser closer = submissionContext.getCloser();
			if(closer!=null) closer.close(this);
			reset();
		} else {
			final TraceCollection traceCollection = this;
			completionCountdown.set(submissionContext.getEndPoints().size());
			for(IEndPoint endpoint: submissionContext.getEndPoints()) {
				final IEndPoint fendpoint = endpoint;
				submissionContext.getExecutor().submit(new Callable<Void>(){					
					public Void call() throws Exception {						
						fendpoint.processTraces(traceCollection);
						if(completionCountdown.decrementAndGet()==0) {
							TraceCollectionCloser closer = submissionContext.getCloser();
							if(closer!=null) closer.close(traceCollection);							
						}
						return null;
					}
				});
			}
		}
		return null;
	}
	
	
	
	
	/**
	 * <p>Title: FACTORY</p>
	 * <p>Description: The event factory for TraceCollections</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.disruptor.TraceCollection.FACTORY</code></p>
	 */
	public static class FACTORY {
		/**
		 * Returns a new TraceCollection
		 * @return a new TraceCollection
		 */
		public TraceCollection newInstance() {
			return new TraceCollection();
		}
	}
	
	/**
	 * Copy Constructor
	 *
	 * @param traceCollection a <code>TraceCollection</code> object
	 */
	public void load(TraceCollection traceCollection) {	    
	    this.traces.addAll(traceCollection.traces);
	    
	}

	/**
	 * Returns the next addressable context tag index
	 * @return the next context tag index
	 */
	private int getContextTagIndex() {
		int index = contextTagSerial.incrementAndGet();
		if(index>MAX_TAG_INDEX) {
			throw new RuntimeException("Maximum number of tags exceeded for this Trace Collection",new Throwable());
		}
		return index;
	}
	
	/**
	 * Sets a context tag object and returns the desginated index
	 * @param context The context object to set
	 * @return the designated index where the context object can be addressed
	 */
	public int setContextTag(Object context) {
		int index = getContextTagIndex();
		contextTags[index] = context;
		return index;
	}
	
	/**
	 * Retrieves the indexed context tag
	 * @param <K> The expected type of the context tag
	 * @param index the index of the context tag
	 * @return the context tag value
	 */
	@SuppressWarnings("unchecked")
	public <K> K getContextTag(int index) {
		if(index<0 || index > MAX_TAG_INDEX) throw new IllegalArgumentException("Invalid tag index [" + index + "]. Must be inclusively netween 0 and " + MAX_TAG_INDEX, new Throwable());
		return (K)contextTags[index];
	}
	
	/**
	 * Returns a copy of the context tags
	 * @return an array of objects
	 */
	public Object[] getContextTags() {
		int index = contextTagSerial.get()+1;
		if(index<1) return new Object[0];
		Object[] copy = new Object[index];
		System.arraycopy(contextTags, 0, copy, 0, index);
		return copy;
	}
	
	
	/**
	 * Creates a new TraceCollection using the default sizing.
	 */
	public TraceCollection() {
		this(IntervalAccumulator.DEFAULT_ACC_MAP_SIZE);		
	}
	
	/**
	 * Creates a new TraceCollection
	 * @param the predefined size of the set to hold this batche's traces 
	 */
	public TraceCollection(int size) {
		traces = new HashSet<T>(size);
		sequence = serialFactory.incrementAndGet();
	}
	
	/**
	 * Loads the tracers to be processed.
	 * To be called by the PROVIDER.
	 * @param flushTraces A collection of traces to be processed.
	 */
	public TraceCollection<T> load(Collection<T> flushTraces) {
		if(!traces.isEmpty()) {
			throw new RuntimeException("TraceCollection is being loaded by still has items", new Throwable());
		}
		traces.addAll(flushTraces);
		return this;
	}

	/**
	 * Loads the tracers to be processed.
	 * To be called by the PROVIDER.
	 * @param flushTraces An array of traces to be processed.
	 */	
	public TraceCollection<T> load(T...flushTraces) {
		if(!traces.isEmpty()) {
			throw new RuntimeException("TraceCollection is being loaded by still has items", new Throwable());
		}
		if(flushTraces!=null) {
			Collections.addAll(traces, flushTraces);			
		}
		return this;
	}
	
	/**
	 * Returns the traces to be processed.
	 * To be called by the CONSUMER.onAvailable(TraceCollection entry)
	 * @return a set of traces
	 */
	public Set<T> getTraces() {
		return new HashSet<T>(this.traces);
	}
	
	
	/**
	 * Resets the trace collection.
	 * To be called by the CONSUMER.onEndOfBatch()
	 */
	public void reset() {		
		
		traces.clear();		
		if(contextTagSerial.getAndSet(-1)!=-1) {		
			Arrays.fill(contextTags, null);
		}
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("TraceCollection [")
	        .append(TAB).append("serial = ").append(this.serial)
	        .append(TAB).append("traces = ").append(this.traces.size())
	        .append(TAB).append("context = [");
	    	Object[] copy = getContextTags();
	    	for(int i = 0; i < copy.length; i++) {
	    		retValue.append(TAB).append("\t#").append(i).append(":").append(copy[i]);
	    	}
	    	retValue.append(TAB).append("]")
	    	
	        .append("\n]");    
	    return retValue.toString();
	}

	/**
	 * <p>Title: OfflineTraceCollection</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.disruptor.TraceCollection.OfflineTraceCollection</code></p>
	 */
	public static class OfflineTraceCollection extends TraceCollection<Trace<? extends ITraceValue>> {
		/**
		 * Returns an empty set of traces
		 * @return an empty set of traces
		 * @see org.helios.ot.tracer.disruptor.TraceCollection#getTraces()
		 */
		@Override
		public Set<Trace<? extends ITraceValue>> getTraces() {
			return Collections.emptySet();
		}


		/**
		 * Loads the tracers to be processed.
		 * To be called by the PROVIDER.
		 * @param flushTraces A collection of traces to be processed.
		 * @return this
		 */
		@Override
		public TraceCollection<Trace<? extends ITraceValue>> load(Collection<Trace<? extends ITraceValue>> flushTraces) {
			return this;
		}

		/**
		 * Loads the tracers to be processed.
		 * To be called by the PROVIDER.
		 * @param flushTraces An array of traces to be processed.
		 * @return this
		 */
		@Override
		public TraceCollection<Trace<? extends ITraceValue>> load(Trace<? extends ITraceValue>... flushTraces) {
			return this;
		}

		/**
		 * Resets this tc
		 * @see org.helios.ot.tracer.disruptor.TraceCollection#reset()
		 */
		@Override
		public void reset() {
			
		}

		/**
		 * Identifies this TC as an offline TC
		 * @return offline
		 * @see org.helios.ot.tracer.disruptor.TraceCollection#toString()
		 */
		@Override
		public String toString() {
			return "Offline TraceCollection";
		}
	}

//	/**
//	 * @param event
//	 * @param sequence
//	 * @return
//	 */
//	@Override
//	public TraceCollection translateTo(TraceCollection event, long sequence) {
//		LOG.info("Translate SEQ:" + sequence + " TSEQ:" + this.sequence);
//		event.load(this);
//		return event;
//	}

	/**
	 * @return the sequence
	 */
	public long getSequence() {
		return sequence;
	}

	/**
	 * @param sequence the sequence to set
	 */
	public void setSequence(long sequence) {
		if(LOG.isDebugEnabled()) LOG.debug("TraceCollection[#" + serial + "] Set sequence:" + sequence);
		this.sequence = sequence;
	}
	
	/**
	 * @param sequence
	 * @return
	 */
	public TraceCollection sequence(long sequence) {
		setSequence(sequence);
		return this;
	}
	
	protected final Set<T> EMPTY_SET = Collections.emptySet();

	@Override
	public Iterator<T> iterator() {
		if(traces!=null && !traces.isEmpty()) {
			return Collections.unmodifiableSet(this.traces).iterator();
		}
		return EMPTY_SET.iterator();
	}
	

	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (serial ^ (serial >>> 32));
		return result;
	}

	/**
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TraceCollection other = (TraceCollection) obj;
		if (serial != other.serial)
			return false;
		return true;
	}
	




}
