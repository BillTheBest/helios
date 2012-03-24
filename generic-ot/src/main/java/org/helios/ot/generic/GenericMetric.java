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
package org.helios.ot.generic;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import org.bson.types.ObjectId;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;
import com.mongodb.Mongo;

/**
 * <p>Title: GenericMetric</p>
 * <p>Description: A basic  {@link IGenericMetric} implementation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.generic.GenericMetric</code></p>
 */
@Entity(value="metric", noClassnameStored=true)
public class GenericMetric implements IGenericMetric, Externalizable {

	/** The metrid def */
	@Reference("metricdef")
	private GenericMetricDef metricDef = null;
	@Id
	private ObjectId id;
	/** The interval start time */
	private long intervalStart = -1;
	/** The interval end time */
	private long intervalEnd = -1;
	/** The interval start date */
	@Transient
	private transient Date intervalStartDate = null;
	/** The interval end date */
	@Transient
	private transient Date intervalEndDate = null;
	/** Metric value average */
	private long avg = -1;
	/** Metric value maximum */
	private long max = -1;
	/** Metric value minimum */
	private long min = -1;
	/** Metric sampling count */
	private long count = -1;

	
	public static void main(String[] args) {
		log("GenericMetric Persistence Test");
		Random random = new Random(System.nanoTime());
		Mongo mongo = null;
		try {
			mongo = new Mongo("localhost");
			Morphia morphia = new Morphia();
			morphia.map(GenericMetric.class);
			morphia.map(GenericMetricDef.class);
			Datastore ds = new Morphia().createDatastore(mongo, "helios");			
			log("Connected:" + ds.getDB().getName());
			mongo.getDB("helios").getCollection("metrics").drop();
			mongo.getDB("helios").getCollection("metricdef").drop();
			log("Metric Def Count:" + ds.getCount(GenericMetricDef.class));
			log("Generic Metric Count:" + ds.getCount(GenericMetric.class));			
			if(1-1==0) return;
			for(int x = 0; x < 50; x++) {
				int metricCount = 1000;
				GenericMetric[] metrics = new GenericMetric[metricCount];
				GenericMetricDef[] metricDefs = new GenericMetricDef[metricCount];
				String agent = ManagementFactory.getRuntimeMXBean().getName();
				long now = System.currentTimeMillis();
				for(int i = 0; i < metricCount; i++) {
					long min = Math.abs(random.nextInt());
					long max = min*4;
					long avg = min*2;
					
					metrics[i] = new GenericMetric(now, now + 15000, avg, max, min, 3, GenericMetricDef.getInstance("herserval|" + agent + "|MongoTest|SimpleMetrics:Metric" + i, 1) );
					metricDefs[i] = metrics[i].getMetricDef();
				}
				log("Saving Warmup...");
				long start = System.currentTimeMillis();		
				ds.save(metricDefs);
				Iterable<Key<GenericMetric>> keys = ds.save(metrics);			
				long elapsed = System.currentTimeMillis()-start;
				log("Warmup Save Time:" + elapsed);
				for(GenericMetric m: metrics) {
					ds.delete(m);
				}
				metricCount = 1000;
				metrics = new GenericMetric[metricCount];			
				now = System.currentTimeMillis();
				for(int i = 0; i < metricCount; i++) {
					long min = Math.abs(random.nextInt());
					long max = min*4;
					long avg = min*2;
					
					metrics[i] = new GenericMetric(now, now + 15000, avg, max, min, 3, GenericMetricDef.getInstance("herserval|" + agent + "|MongoTest|SimpleMetrics:Metric" + i, 1) );
					metricDefs[i] = metrics[i].getMetricDef();
				}
				log("Saving...");
				start = System.currentTimeMillis();			
				ds.save(metricDefs);
				keys = ds.save(metrics);			
				
				elapsed = System.currentTimeMillis()-start;
				log("Save Time:" + elapsed);
//				log("Purging.....");
//				for(GenericMetric key: ds.find(GenericMetric.class).fetch()) {
//					ds.delete(key);
//				}
//				log("Purge Complete");
				mongo.fsync(false);
				for(GenericMetric m: metrics) {
					ds.delete(m);
				}
				log("Metric Def Count:" + ds.getCount(GenericMetricDef.class));
				log("Generic Metric Count:" + ds.getCount(GenericMetric.class));
				
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			try { mongo.close(); } catch (Exception e) {}
		}
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Creates a new GenericMetric
	 * For externalization only
	 */
	public GenericMetric() {
		
	}
	
	/**
	 * Returns the value idfentified by the passed property name (case insensitive)
	 * @param propName The property name
	 * @return the value
	 */
	public Object getProperty(String propName) {
		if(propName==null) throw new IllegalArgumentException("The passed property name was null", new Throwable());
		propName = propName.trim();
		if(propName.equalsIgnoreCase("agent")) return getAgent();
		 else if(propName.equalsIgnoreCase("avg")) return getAvg();
		 else if(propName.equalsIgnoreCase("count")) return getCount();
		 else if(propName.equalsIgnoreCase("domain")) return getDomain();
		 else if(propName.equalsIgnoreCase("endDate")) return getEndDate();
		 else if(propName.equalsIgnoreCase("endTime")) return getEndTime();
		 else if(propName.equalsIgnoreCase("fullName")) return getFullName();
		 else if(propName.equalsIgnoreCase("host")) return getHost();
		 else if(propName.equalsIgnoreCase("localName")) return getLocalName();
		 else if(propName.equalsIgnoreCase("max")) return getMax();
		 else if(propName.equalsIgnoreCase("metricDef")) return getMetricDef();
		 else if(propName.equalsIgnoreCase("metricName")) return getMetricName();
		 else if(propName.equalsIgnoreCase("min")) return getMin();
		 else if(propName.equalsIgnoreCase("process")) return getProcess();
		 else if(propName.equalsIgnoreCase("resource")) return getResource();
		 else if(propName.equalsIgnoreCase("startDate")) return getStartDate();
		 else if(propName.equalsIgnoreCase("startTime")) return getStartTime();
		 else if(propName.equalsIgnoreCase("typeCode")) return getTypeCode();	
		 else throw new IllegalArgumentException("The passed property name [" + propName + "] was not recognized", new Throwable());
	}
	
	/**
	 * Creates a new GenericMetric
	 * @param startTime The earliest start time of the interval
	 * @param endTime The latest start time of the interval
	 * @param avg The average average
	 * @param max The maximum maximum
	 * @param min The minimum minimum
	 * @param count The total count of instances
	 * @param metricDef The metric definition
	 */
	public GenericMetric(long startTime, long endTime, long avg, long max, long min, long count, GenericMetricDef metricDef) {
		this.metricDef = metricDef;
		this.intervalStart = startTime;
		this.intervalEnd = endTime;
		this.avg = avg;
		this.min = min;
		this.max = max;
		this.count = count;
	}

	/**
	 * Copy Constructor
	 * @param genericMetric an {@link IGenericMetric} object
	 */
	public GenericMetric(IGenericMetric genericMetric) {
		if(genericMetric==null) throw new IllegalArgumentException("The passed generic metric was null", new Throwable());
	    this.metricDef = genericMetric.getMetricDef();
	    this.intervalStart = genericMetric.getStartTime();
	    this.intervalEnd = genericMetric.getEndTime();
	    this.avg = genericMetric.getAvg();
	    this.max = genericMetric.getMax();
	    this.min = genericMetric.getMin();
	    this.count = genericMetric.getCount();
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getDomain()
	 */
	public String getDomain() {
		return metricDef.getDomain();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getHost()
	 */
	public String getHost() {
		return metricDef.getHost();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getProcess()
	 */
	public String getProcess() {		
		return metricDef.getProcess();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getAgent()
	 */
	public String getAgent() {
		return metricDef.getAgent();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getResource()
	 */
	public String[] getResource() {
		return metricDef.getResource();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getMetricName()
	 */
	public String getMetricName() {
		return metricDef.getMetricName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getSegment(int)
	 */
	public String getSegment(int pos) {
		return metricDef.getSegment(pos);
	}

	/**
	 * Returns the metric Def for this instance
	 * @return the metric Def for this instance
	 */
	public GenericMetricDef getMetricDef() {
		return metricDef;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getAvg()
	 */
	public long getAvg() {
		return avg;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getMin()
	 */
	public long getMin() {
		return min;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getMax()
	 */
	public long getMax() {
		return max;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getCount()
	 */
	public long getCount() {
		return count;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getStartTime()
	 */
	public long getStartTime() {
		return intervalStart;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getEndTime()
	 */
	public long getEndTime() {
		return intervalEnd;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getStartDate()
	 */
	public Date getStartDate() {
		if(intervalStartDate==null) {
			intervalStartDate = new Date(intervalStart);
		}
		return intervalStartDate;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getEndDate()
	 */
	public Date getEndDate() {
		if(intervalEndDate==null) {
			intervalEndDate = new Date(intervalEnd);
		}
		return intervalEndDate;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getTypeCode()
	 */
	public int getTypeCode() {
		return metricDef.getTypeCode();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getFullName()
	 */
	public String getFullName() {
		return metricDef.getFullName();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.ot.generic.IGenericMetric#getLocalName()
	 */
	public String getLocalName() {
		return metricDef.getLocalName();
	}


	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("GenericMetric [")
	    	.append(TAB).append("fullName = ").append(getFullName())
	    	.append(TAB).append("localName = ").append(getLocalName())
	        .append(TAB).append("domain = ").append(getDomain())
	        .append(TAB).append("host = ").append(getHost())
	        .append(TAB).append("process = ").append(getProcess())
	        .append(TAB).append("agent = ").append(getAgent())
	        .append(TAB).append("resource = ").append(Arrays.toString(getResource()))
	        .append(TAB).append("metricName = ").append(getMetricName())
	        .append(TAB).append("intervalStartDate = ").append(getStartDate())
	        .append(TAB).append("intervalEndDate = ").append(getEndDate())
	        .append(TAB).append("typeCode = ").append(getTypeCode())
	        .append(TAB).append("avg = ").append(this.avg)
	        .append(TAB).append("max = ").append(this.max)
	        .append(TAB).append("min = ").append(this.min)
	        .append(TAB).append("count = ").append(this.count)
	        .append("\n]");    
	    return retValue.toString();
	}
	
	public static final byte[] NOT_RECOGNIZED = new byte[]{0};
	public static final byte[] RECOGNIZED = new byte[]{1};


	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		if(metricDef.isRecognized()) {
			out.write(RECOGNIZED);
		} else {
			out.write(NOT_RECOGNIZED);
			out.writeUTF(metricDef.getFullName());
			out.writeInt(metricDef.getTypeCode());
		}
		//metricDef.writeExternal(out);
		out.writeLong(intervalStart);
		out.writeLong(intervalEnd);
		out.writeLong(count);
		out.writeLong(avg);
		out.writeLong(min);
		out.writeLong(max);
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte[] recognized = new byte[1];
		in.read(recognized);
		if(recognized[0]==1) {
			metricDef = GenericMetricDef.getInstance(in.readInt());			
		} else {
			metricDef = GenericMetricDef.getInstance(in.readUTF(), in.readInt());
		}
		intervalStart = in.readLong();
		intervalEnd = in.readLong();
		count = in.readLong();
		avg =  in.readLong();
		min = in.readLong();
		max = in.readLong();
	}
	


}
