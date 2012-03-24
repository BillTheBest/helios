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
package org.helios.esper.engine.beans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Date;

import org.helios.wily.GenericMetricDef;
import org.helios.wily.IGenericMetric;
import org.helios.wily.IUpdateableGenericMetric;
import org.helios.wily.WilyGenericMetricImpl;

/**
 * <p>Title: EngineGenericMetric</p>
 * <p>Description: An updateable generic metric for use inside the Esper engine</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.esper.engine.beans.EngineGenericMetric</code></p>
 */

public class EngineGenericMetric implements IUpdateableGenericMetric, Externalizable {
	/**  */
	private static final long serialVersionUID = -1903300182813749813L;
	/** The metrid def */
	private GenericMetricDef metricDef = null;
	/** The interval start time */
	private long intervalStart = -1;
	/** The interval end time */
	private long intervalEnd = -1;
	/** The interval start date */
	private transient Date intervalStartDate = null;
	/** The interval end date */
	private transient Date intervalEndDate = null;
	/** Metric value average */
	private long avg = -1;
	/** Metric value maximum */
	private long max = -1;
	/** Metric value minimum */
	private long min = -1;
	/** Metric sampling count */
	private long count = -1;
	
	
	/**
	 * Converts an array of generic metrics into an array of Engine metrics
	 * @param genericMetrics An array of generic metrics
	 * @return An array of engine metrics
	 */
	public EngineGenericMetric[] getEngineMetrics(IGenericMetric[] genericMetrics) {
		if(genericMetrics==null || genericMetrics.length<1) return new EngineGenericMetric[0];
		EngineGenericMetric[] emetrics = new EngineGenericMetric[genericMetrics.length];
		for(int i = 0; i < genericMetrics.length; i++) {
			emetrics[i] = new EngineGenericMetric(genericMetrics[i]);
		}
		return emetrics;
	}
	
	/**
	 * Copy Constructor
	 * @param genericMetric an {@link IGenericMetric} object
	 */
	public EngineGenericMetric(IGenericMetric genericMetric) {
		if(genericMetric==null) throw new IllegalArgumentException("The passed generic metric was null", new Throwable());
	    this.metricDef = genericMetric.getMetricDef();
	    this.intervalStart = genericMetric.getStartTime();
	    this.intervalEnd = genericMetric.getEndTime();
	    this.avg = genericMetric.getAvg();
	    this.max = genericMetric.getMax();
	    this.min = genericMetric.getMin();
	    this.count = genericMetric.getCount();
	}
	
	
	public static void main(String[] args) {
		log("Metric Extern Test");
		try {
			EngineGenericMetric metric = new EngineGenericMetric();
			String name = "SooperDomain|MyHost|SnazzyProcess|CoolAgent|A|B|C:foo";
			GenericMetricDef mdef = new GenericMetricDef(name, 99, name.hashCode());
			long now = System.currentTimeMillis();
			metric.metricDef = mdef;
		    metric.intervalStart = now;
		    metric.intervalEnd = now + 1000;
		    metric.avg = 5;
		    metric.max = 10;
		    metric.min = 1;
		    metric.count = 4;
		    WilyGenericMetricImpl wimpl = new WilyGenericMetricImpl(metric);
		    log("Writing Metric....");
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream(baos);
		    oos.writeObject(wimpl);
		    oos.flush();
		    baos.flush();
		    log("Reading Metric....");		    
		    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		    ObjectInputStream ois = new ObjectInputStream(bais);
		    WilyGenericMetricImpl metric2 = (WilyGenericMetricImpl)ois.readObject();
		    log(metric2);
		    
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * Creates a new EngineGenericMetric
	 */
	public EngineGenericMetric() {
		
	}
	
	/**
	 * Creates a new EngineGenericMetric
	 * @param startTime The earliest start time of the interval
	 * @param endTime The latest start time of the interval
	 * @param avg The average average
	 * @param max The maximum maximum
	 * @param min The minimum minimum
	 * @param count The total count of instances
	 * @param metricDef The metric definition
	 */
	public EngineGenericMetric(long startTime, long endTime, long avg, long max, long min, long count, GenericMetricDef metricDef) {
		this.metricDef = metricDef;
		this.intervalStart = startTime;
		this.intervalEnd = endTime;
		this.avg = avg;
		this.min = min;
		this.max = max;
		this.count = count;
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
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getDomain()
	 */
	public String getDomain() {
		return metricDef.getDomain();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getHost()
	 */
	public String getHost() {
		return metricDef.getHost();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getProcess()
	 */
	public String getProcess() {		
		return metricDef.getProcess();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getAgent()
	 */
	public String getAgent() {
		return metricDef.getAgent();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getLocalName()
	 */
	public String getLocalName() {
		return metricDef.getLocalName();
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getResource()
	 */
	public String[] getResource() {
		return metricDef.getResource();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getMetricName()
	 */
	public String getMetricName() {
		return metricDef.getMetricName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getSegment(int)
	 */
	public String getSegment(int pos) {
		return metricDef.getSegment(pos);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getAvg()
	 */
	public long getAvg() {
		return avg;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getMin()
	 */
	public long getMin() {
		return min;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getMax()
	 */
	public long getMax() {
		return max;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getCount()
	 */
	public long getCount() {
		return count;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getStartTime()
	 */
	public long getStartTime() {
		return intervalStart;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getEndTime()
	 */
	public long getEndTime() {
		return intervalEnd;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getStartDate()
	 */
	public Date getStartDate() {
		if(intervalStartDate==null) {
			intervalStartDate = new Date(intervalStart);
		}
		return intervalStartDate;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getEndDate()
	 */
	public Date getEndDate() {
		if(intervalEndDate==null) {
			intervalEndDate = new Date(intervalEnd);
		}
		return intervalEndDate;
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
	 * @see org.helios.wily.IGenericMetric#getTypeCode()
	 */
	public int getTypeCode() {
		return metricDef.getTypeCode();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getFullName()
	 */
	public String getFullName() {
		return metricDef.getFullName();
	}
	
	/**
	 * Sets the metric interval start time
	 * @param intervalStart the intervalStart to set
	 */
	public void setStartTime(long intervalStart) {
		this.intervalStart = intervalStart;
	}

	/**
	 * Sets the metric interval end time
	 * @param intervalEnd the intervalEnd to set
	 */
	public void setEndTime(long intervalEnd) {
		this.intervalEnd = intervalEnd;
	}

	/**
	 * Sets the average value
	 * @param avg the avg to set
	 */
	public void setAvg(long avg) {
		this.avg = avg;
	}

	/**
	 * Sets the maximum value
	 * @param max the max to set
	 */
	public void setMax(long max) {
		this.max = max;
	}

	/**
	 * Sets the minimum value
	 * @param min the min to set
	 */
	public void setMin(long min) {
		this.min = min;
	}

	/**
	 * Sets the metric sample count
	 * @param count the count to set
	 */
	public void setCount(long count) {
		this.count = count;
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
	    StringBuilder retValue = new StringBuilder("EngineGenericMetric [")
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
	



	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		metricDef.writeExternal(out);
		out.writeLong(intervalStart);
		out.writeLong(intervalEnd);
		out.writeLong(count);
		out.writeLong(avg);
		out.writeLong(min);
		out.writeLong(max);
		//  Domain|Host|Process|Agent|A|B|C:D
		// or
		//  Domain|Host|Process|Agent:D
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		metricDef = new GenericMetricDef(in);
		intervalStart = in.readLong();
		intervalEnd = in.readLong();
		count = in.readLong();
		avg =  in.readLong();
		min = in.readLong();
		max = in.readLong();
	}
}
