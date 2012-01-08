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
package org.helios.containers.buckets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.containers.counters.IntegerCircularCounter;
import org.helios.helpers.ClassHelper;


/**
 * <p>Title: TimeRotatingBucket</p>
 * <p>Description: A container that rotates at access time based on elapsed intervals</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class TimeRotatingBucket<K> {
	/** the start time of the time mamgement */
	protected long startTime = -1L;
	/** the number of buckets */
	protected int bucketCount = -1;
	/**  the id of the current bucket */
	protected AtomicInteger currentBucket = new AtomicInteger(0);
	/**  the id of the prior bucket */
	protected AtomicInteger priorBucket = new AtomicInteger(-1);
	/** A map of the managed buckets */
	protected Map<Integer, Bucket<K>> buckets = null;
	/** The class of the pojos that the buckets contain */
	protected Class<K> pojoClass = null;
	/** Indicates if the time bucket has started */
	protected AtomicBoolean started = new AtomicBoolean(false);
	/** indicates if elapsed time is measured in nanos. otherwise measured in ms. */
	protected boolean nanos = false;
	/** the elapsed time of one period */
	protected long period = -1L;
	/** the current period */
	protected AtomicLong currentPeriod = new AtomicLong(0);
	/** a circular counter to track bucket switches */
	protected IntegerCircularCounter counter = null;
	/** the unit of the period */
	protected TimeUnit periodUnit = null;
	/** Signature constant for an int */
	protected static final Class<?>[] INT_ARG = new Class[]{int.class};
	/** Signature constant for a long */
	protected static final Class<?>[] LONG_ARG = new Class[]{long.class};
	
	/**
	 * Creates a new TimeRotatingBucket that will manage the passed pojos
	 * @param period The elapsed time of one period.
	 * @param timeUnit The time unit of the period.
	 * @param start Indicates if the time bucketing should start now.
	 * @param pojos An array of pojos to manage.
	 */
	@SuppressWarnings("unchecked")
	public TimeRotatingBucket(long period, TimeUnit periodTimeUnit, boolean start, K...pojos) {
		if(pojos==null || pojos.length<1) throw new RuntimeException("Cannot create an empty TimeRotatingBucket and passed pojo array was null or zero length");
		bucketCount = pojos.length;
		counter = new IntegerCircularCounter(bucketCount);
		this.periodUnit = periodTimeUnit;
		this.nanos = (periodTimeUnit.ordinal() < TimeUnit.MILLISECONDS.ordinal());
		if(nanos) {
			this.period = TimeUnit.NANOSECONDS.convert(period, periodUnit);
		} else {
			this.period = TimeUnit.MILLISECONDS.convert(period, periodUnit);
		}
		if(this.period==0) this.period = 1;
		pojoClass = (Class<K>) pojos[0].getClass();
		boolean bucketIface = (IBucket.class.isAssignableFrom(pojoClass));
		
		Set<Method> openMethods = getBucketMethods(BucketOpen.class, pojoClass, LONG_ARG);
		Set<Method> closeMethods = getBucketMethods(BucketClose.class, pojoClass);
		Set<Method> idMethods = getBucketMethods(BucketId.class, pojoClass, INT_ARG);
		buckets = new ConcurrentHashMap<Integer, Bucket<K>>(bucketCount);
		for(int i = 0; i < bucketCount; i++) {
			if(bucketIface) ((IBucket)pojos[i]).setBucketId(i);
			else {
				for(Method m: idMethods) {
					try { m.invoke(pojos[i], i); } catch (Exception e) {}
				}
			}
			buckets.put(i, new Bucket<K>(pojos[i], openMethods, closeMethods));
		}
		if(start) start();
	}
	
	/**
	 * Creates a new TimeRotatingBucket that will manage the passed pojos and automatically starts the time bucketing.
	 * The elapsed time unit defaults to ms. 
	 * @param period The elapsed time of one period in ms.
	 * @param pojos An array of pojos to manage.
	 */
	public TimeRotatingBucket(long period, K...pojos) {
		this(period, TimeUnit.MILLISECONDS,  true, pojos);
	}
	
	/**
	 * Retrieves the current timestamp in ns. if <code>nanos</code> is true, else returns in ms.
	 * @return the current timestamp
	 */
	protected long getCurrentTime() {
		if(nanos) return System.nanoTime();
		else return System.currentTimeMillis();
	}
	
	/**
	 * Retrieves the elapsed time since start in ns. if <code>nanos</code> is true, else returns in ms.
	 * @return the elapsed time since start
	 */
	protected long getElapsedTime() {
		return getCurrentTime()  - startTime;
	}
	
	/**
	 * Returns the current period based on the rounded elapsed time didvided by the period length.
	 * @return the actual current period 
	 */
	protected long getActualPeriod() {
		long elapsed = getElapsedTime();
		return (long)elapsed/period;
	}
	
	/**
	 * Returns the last recorded current period.
	 * @return the last recorded current period
	 */
	protected long getCurrentPeriod() {
		return currentPeriod.get();
	}
	
	
	/**
	 * Determines if the actual current period is higher than the last recorded period.
	 * @return true if the actual period is greater than the last recorded period.
	 */
	protected boolean isNewPeriod() {
		return currentPeriod.get() < getActualPeriod();
	}
	
	/**
	 * Retrieves the pojo in the current bucket.
	 * @return the pojo from the current bucket.
	 */
	public synchronized K get() {
		Bucket<K> bucket = null;
		if(started.get() && isNewPeriod()) {
			currentPeriod.set(getActualPeriod());
			bucket = rollBuckets();
			
		} else {
			bucket = buckets.get(currentBucket.get());
		}
		return bucket.getPojo(); 
	}
	
	/**
	 * Returns the prior close bucket.
	 * @return the pojo from the prior bucket.
	 */
	public K getPrior() {
		return buckets.get(getPriorBucketId()).getPojo();
	}
	
	/**
	 * Returns the next bucket Id.
	 * @return
	 */
	protected int getNextBucketId() {
		return counter.nextCounter();
	}
	
	/**
	 * Returns the prior bucket Id.
	 * @return
	 */
	protected int getPriorBucketId() {
		return counter.priorCounter();
	}	
	
	/**
	 * Closes the current bucket and opens the new one, placing the new bucket in state. 
	 */
	protected Bucket<K> rollBuckets() {
		int currentBucketId = currentBucket.get();
		Bucket<K> currBucket = buckets.get(currentBucketId);
		currBucket.close();
		
		int nextBucketId = getNextBucketId();		
		Bucket<K> nextBucket = buckets.get(nextBucketId);
		nextBucket.open(currentPeriod.get());
		
		currentBucket.set(nextBucketId);
		priorBucket.set(currentBucketId);
		
		return nextBucket;
		
		
	}
	
	/**
	 * Starts the time bucketing if it has not already started.
	 */
	public void start() {
		if(!started.get()) {
			startTime = getCurrentTime();
			currentBucket.set(this.getNextBucketId());
			currentPeriod.set(1);
			buckets.get(currentBucket.get()).open(1);						
			started.set(true);
		}
	}
	

	
	
	/**
	 * Extracts a set of methods from the passed class that are annotated with the passed annotation.
	 * Returns an empty set if the class implements <code>org.helios.containers.buckets.IBucket</code>.
	 * @param annotation The annotation to look for.
	 * @param pojoClass The class to search.
	 * @param paramTypes The parameter signature of the target methods.
	 * @return A (possibly empty) set of methods.
	 */
	protected Set<Method> getBucketMethods(Class<? extends Annotation> annotation, Class<K> pojoClass, Class<?>...paramTypes) {		
		Set<Method> methods = new HashSet<Method>();
		if(IBucket.class.isAssignableFrom(pojoClass)) return methods;
		if(paramTypes==null) paramTypes = new Class[]{};
		Set<Method> allMethods = new HashSet<Method>();		
		Collections.addAll(allMethods, pojoClass.getMethods());
		Collections.addAll(allMethods, pojoClass.getDeclaredMethods());
		for(Method method: allMethods) {
			if(method.getAnnotation(annotation) != null) {
				if(!ClassHelper.classArraysEqual(method.getParameterTypes(), paramTypes)) continue;
				if(!Modifier.isPublic(method.getModifiers())) {
					method.setAccessible(true);
				}
				methods.add(method);
			}
		}
		return methods;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		log("TimeRotatingBucket Test");
//		for(Method m: AnnotatedBucket.class.getDeclaredMethods()) {
//			Annotation ann = ClassHelper.getAnnotationFromMethod(m, BucketOpen.class, true);
//			log(m.getName() + ":" + ann);
//		}
		AnnotatedBucket[] buckets = new AnnotatedBucket[] {
			new AnnotatedBucket("John"),
			new AnnotatedBucket("George"),
			new AnnotatedBucket("Paul"),
			new AnnotatedBucket("Ringo")
		};
		TimeRotatingBucket<? extends BaseBucket> trb = new TimeRotatingBucket<BaseBucket>(100, TimeUnit.MICROSECONDS, false, buckets);
		log(trb);		
		trb.start();
		for(int i = 0; i < 10000; i++) {
			long start = System.nanoTime();
			try {
				Thread.currentThread().join(0, 3000);
				trb.get();		
				
				if(i>1000 && i%1000==0) {
					long elapsed = System.nanoTime()-start;
					log(i + " at " + elapsed + " ns.");
					start = System.nanoTime();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for(BaseBucket b: buckets) {
			log(b);
		}
		log("Current Bucket:" + trb.get());
		log("Prior Bucket:" + trb.getPrior());
	}
	
	public static void log(Object message) {
		System.out.println(message);
	}



	/**
	 * Indicates if the time bucketing has started.
	 * @return true if started
	 */
	public boolean isStarted() {
		return started.get();
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("TimeRotatingBucket [");    
	    retValue.append(TAB).append("startTime=").append(this.startTime);    
	    retValue.append(TAB).append("bucketCount=").append(this.bucketCount);    
	    retValue.append(TAB).append("currentBucket=").append(this.currentBucket);    
	    retValue.append(TAB).append("priorBucket=").append(this.priorBucket);    
	    retValue.append(TAB).append("pojoClass=").append(this.pojoClass.getName());    
	    retValue.append(TAB).append("started=").append(this.started.get());    
	    retValue.append(TAB).append("nanos=").append(this.nanos);    
	    retValue.append(TAB).append("period=").append(this.period);    
	    retValue.append(TAB).append("currentPeriod=").append(this.currentPeriod);    
	    retValue.append(TAB).append("periodUnit=").append(this.periodUnit);    
	    retValue.append("\n]");
	    return retValue.toString();
	}

}

class BaseBucket {
	protected String message = null;
	protected AtomicInteger bucketId = new AtomicInteger(-1);
	protected AtomicLong counter = new AtomicLong(0);
	protected long period = 0L;
	
	public BaseBucket(String message) {
		this.message = message;
	}

	public void close() {
		//log("[" + message + "] closing counter [" + counter.get() + "]");
	}

	public void open(long period) {
		counter.incrementAndGet();
		this.period = period;
		//log("[" + message + "] opening counter [" + counter.get() + "]");		
	}

	public void setBucketId(int bucketId) {
		this.bucketId.set(bucketId);
	}
	
	
	
	public static void log(Object message) {
		System.out.println(message);
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("TestBucket [");    
	    retValue.append(TAB).append("message=").append(this.message);    
	    retValue.append(TAB).append("bucketId=").append(this.bucketId.get());    
	    retValue.append(TAB).append("counter=").append(this.counter.get());    
	    retValue.append(TAB).append("period=").append(this.period);
	    retValue.append("\n]");
	    return retValue.toString();
	}

	public void setPeriod(long periodId) {
		period = periodId;
	}	
	
}

/**
 * <p>Title: AnnotatedBucket</p>
 * <p>Description: A bucket implementation annotated for all rotation lifecycle methods.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.containers.buckets.AnnotatedBucket</code></p>
 */
class AnnotatedBucket  extends BaseBucket {

	public AnnotatedBucket(String message) {
		super(message);
	}

	@BucketClose
	public void close() {
		super.close();
		
	}

	@BucketOpen
	public void open(long period) {
		super.open(period);
		
	}
	
	@BucketId
	public void setBucketId(int bucketId) {
		super.setBucketId(bucketId);
	}

	
}

class TestBucket extends BaseBucket implements IBucket {

	public TestBucket(String message) {
		super(message);
	}
}

/**
 * <p>Title: Bucket</p>
 * <p>Description: The bucket container for an instance of a pojo to be managed.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
class Bucket<K> {
	/** the pojo being managed	 */
	protected K pojo = null;
	/** the class of the pojo */
	protected Class<K> pojoClass = null;
	/** the open methods to be called on the pojo when this bucket comes into scope */
	protected Set<Method> openMethods = new HashSet<Method>();
	/** the close methods to be called on the pojo when this bucket exits scope */
	protected Set<Method> closeMethods = new HashSet<Method>();	
	
	/** the open state of the bucket */
	protected AtomicBoolean state = new AtomicBoolean(false);
	/** indicates if the class of the managed pojo implemented the <code>IBucket</code> interface */
	protected boolean isIBucket = false;
	/** the cast iBucket  */
	protected IBucket iBucket = null;
	

	/**
	 * Creates a new Bucket to manage the passed pojo
	 * @param pojo The pojo to manage
	 * @param openMethods A set of open methods to be called for this pojo
	 * @param closeMethods A set of close methods to be called for this pojo
	 */
	public Bucket(K pojo, Set<Method> openMethods, Set<Method>  closeMethods) {
		this.pojo = pojo;
		isIBucket = this.pojo instanceof IBucket;
		if(isIBucket) {
			iBucket = (IBucket)pojo;
		} else {
			this.openMethods = openMethods;
			this.closeMethods = closeMethods;
		}
	}

	/**
	 * @return the pojo
	 */
	public K getPojo() {
		return pojo;
	}

	/**
	 * Indicates if the bucket is an open state
	 * @return true if the bucket is open, false if it is closed.
	 */
	public boolean isOpen() {
		return state.get();
	}
	
	/**
	 * Opens the bucket.
	 */
	protected void open(long period) {
		if(isOpen()) {
			System.err.println("Attempted to open a bucket that is already open [" + this.toString() + "]");
			return;
		}
		if(isIBucket) {
			iBucket.open(period);
		} else {
			for(Method m: openMethods) {
				try {
					if(Modifier.isStatic(m.getModifiers())) {
						m.invoke(null, period);
					} else {
						m.invoke(pojo, period);
					}
				} catch (Exception e) {
					System.err.println("Failed to invoke @BucketOpen method [" + m.getName() + "] on managed instance of [" + pojo.getClass().getName() + "]:" + e);
				}
			}
		}
		state.set(true);
	}
	
	/**
	 * Closes the bucket. 
	 */
	protected void close() {
		if(!isOpen()) {
			System.err.println("Attempted to close a bucket that is already closed [" + this.toString() + "]");
			return;
		}
		if(isIBucket) {
			iBucket.close();
		} else {
			for(Method m: closeMethods) {
				try {
					if(Modifier.isStatic(m.getModifiers())) {
						m.invoke(null);
					} else {
						m.invoke(pojo);
					}
				} catch (Exception e) {
					System.err.println("Failed to invoke @BucketClose method [" + m.getName() + "] on managed instance of [" + pojo.getClass().getName() + "]:" + e);
				}
			}
		}
		state.set(false);
		
	}
}
