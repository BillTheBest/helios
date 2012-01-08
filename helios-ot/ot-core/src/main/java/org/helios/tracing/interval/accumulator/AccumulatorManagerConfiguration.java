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
package org.helios.tracing.interval.accumulator;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import org.helios.helpers.ConfigurationHelper;

/**
 * <p>Title: AccumulatorManagerConfiguration</p>
 * <p>Description:  Values class for AccumulatorManager configuration values </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.interval.accumulator.AccumulatorManagerConfiguration</code></p>
 */

public class AccumulatorManagerConfiguration implements Serializable {
	
	/**  */
	private static final long serialVersionUID = 2820257895781092491L;
	/** The default modulo for partitioning concurrent streams of traces */
	public static final int DEFAULT_HOT_MOD = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The default queue size for the queue that closed intervals are flushed into */
	public static final int DEFAULT_SUB_QUEUE_SIZE = AccumulatorConfiguration.DEFAULT_ACCUMULATOR_MAP_SIZE * DEFAULT_HOT_MOD;
	/** The default queue fairness for the queue that closed intervals are flushed into */
	public static final boolean DEFAULT_SUB_QUEUE_FAIRNESS = false;
	/** The default insert timeout in ms. for the queue that closed intervals are flushed into */
	public static final long DEFAULT_SUB_QUEUE_TIMEOUT = 50;
	/** The accumulator manager's default flush frequency in ms. */
	public static final long DEFAULT_HOT_PERIOD = 15000;
	/** The accumulator manager's default flush batch size */
	public static final int DEFAULT_FLUSH_BATCH_SIZE = 50;
	
	/** The configuration prefix */
	public final String CONFIG_PREFIX = getClass().getPackage().getName() + ".accumulator.mgr.";

	
	/** The accumulator modulo, also the number of accumulators in this accumulator group. */
	protected int accumulatorMod = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_HOT_MOD);
	/** The accumulator manager submission queue size. */
	protected int submissionQueueSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_SUB_QUEUE_SIZE);
	/** The accumulator manager submission queue fairness. */
	protected boolean submissionQueueFairness = ConfigurationHelper.getBooleanSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_SUB_QUEUE_FAIRNESS);
	/** The accumulator submission queue insert timeout. */
	protected long submissionQueueInsertTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_SUB_QUEUE_TIMEOUT);
	/** The Accumulator flush interval in ms. */
	protected long flushPeriod = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_HOT_PERIOD);
	/** The flush batch size. */
	protected int flushBatchSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "flushsize", DEFAULT_FLUSH_BATCH_SIZE);
	
	/**
	 * Sets the value of this AccMgr from another AccMgr
	 * @param accumulatorManagerConfiguration a <code>AccumulatorManagerConfiguration</code> object
	 */
	public void copyFrom(AccumulatorManagerConfiguration accumulatorManagerConfiguration) {
	    this.accumulatorMod = accumulatorManagerConfiguration.accumulatorMod;
	    this.submissionQueueSize = accumulatorManagerConfiguration.submissionQueueSize;
	    this.submissionQueueFairness = accumulatorManagerConfiguration.submissionQueueFairness;
	    this.submissionQueueInsertTimeout = accumulatorManagerConfiguration.submissionQueueInsertTimeout;
	    this.flushPeriod = accumulatorManagerConfiguration.flushPeriod;
	    this.flushBatchSize = accumulatorManagerConfiguration.flushBatchSize;
	}

	/**
	 * Creates a new AccumulatorManagerConfiguration
	 * @param accumulatorMod the modulo for partitioning concurrent streams of traces
	 * @param submissionQueueSize the queue size for the queue that closed intervals are flushed into
	 * @param submissionQueueFairness the queue fairness for the queue that closed intervals are flushed into
	 * @param submissionQueueInsertTimeout the insert timeout in ms. for the queue that closed intervals are flushed into
	 * @param flushPeriod the accumulator manager's flush frequency in ms
	 * @param flushBatchSize the flush batch size 
	 */
	public AccumulatorManagerConfiguration(int accumulatorMod, int submissionQueueSize, boolean submissionQueueFairness, long submissionQueueInsertTimeout, long flushPeriod, int flushBatchSize) {
		this.accumulatorMod = accumulatorMod;
		this.submissionQueueSize = submissionQueueSize;
		this.submissionQueueFairness = submissionQueueFairness;
		this.submissionQueueInsertTimeout = submissionQueueInsertTimeout;
		this.flushPeriod = flushPeriod;
		this.flushBatchSize = flushBatchSize;
	}
	
	/**
	 * Creates a new AccumulatorManagerConfiguration
	 */
	public AccumulatorManagerConfiguration() {
		
	}
	
	/**
	 * Creates a new AccumulatorManagerConfiguration
	 * @param props Configuration properties
	 */
	public AccumulatorManagerConfiguration(Properties props) {
		accumulatorMod = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_HOT_MOD, props);
		submissionQueueSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_SUB_QUEUE_SIZE, props);
		submissionQueueFairness = ConfigurationHelper.getBooleanSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_SUB_QUEUE_FAIRNESS, props);
		submissionQueueInsertTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_SUB_QUEUE_TIMEOUT, props);
		flushPeriod = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_HOT_PERIOD, props);
		flushBatchSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "flushsize", DEFAULT_FLUSH_BATCH_SIZE, props);
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("AccumulatorManagerConfiguration [")
	        .append(TAB).append("accumulatorMod = ").append(this.accumulatorMod)
	        .append(TAB).append("submissionQueueSize = ").append(this.submissionQueueSize)
	        .append(TAB).append("submissionQueueFairness = ").append(this.submissionQueueFairness)
	        .append(TAB).append("submissionQueueInsertTimeout = ").append(this.submissionQueueInsertTimeout)
	        .append(TAB).append("flushPeriod = ").append(this.flushPeriod)
	        .append(TAB).append("flushBatchSize = ").append(this.flushBatchSize)	        
	        .append("\n]");    
	    return retValue.toString();
	}

	/**
	 * Returns the modulo for partitioning concurrent streams of traces
	 * @return the accumulator Modulo
	 */
	public int getAccumulatorMod() {
		return accumulatorMod;
	}

	/**
	 * Sets the modulo for partitioning concurrent streams of traces
	 * @param accumulatorMod the accumulator Mod to set
	 */
	public void setAccumulatorMod(int accumulatorMod) {
		this.accumulatorMod = accumulatorMod;
	}

	/**
	 * Returns the queue size for the queue that closed intervals are flushed into
	 * @return the submission Queue Size
	 */
	public int getSubmissionQueueSize() {
		return submissionQueueSize;
	}

	/**
	 * Sets the queue size for the queue that closed intervals are flushed into
	 * @param submissionQueueSize the submission Queue Size to set
	 */
	public void setSubmissionQueueSize(int submissionQueueSize) {
		this.submissionQueueSize = submissionQueueSize;
	}

	/**
	 * Returns the queue fairness for the queue that closed intervals are flushed into
	 * @return the submission Queue Fairness
	 */
	public boolean isSubmissionQueueFair() {
		return submissionQueueFairness;
	}

	/**
	 * Sets the queue fairness for the queue that closed intervals are flushed into
	 * @param submissionQueueFairness the submissionQueueFairness to set
	 */
	public void setSubmissionQueueFairness(boolean submissionQueueFair) {
		this.submissionQueueFairness = submissionQueueFair;
	}

	/**
	 * Returns the the insert timeout in ms. for the queue that closed intervals are flushed into
	 * @return the submissionQueue Insert Timeout
	 */
	public long getSubmissionQueueInsertTimeout() {
		return submissionQueueInsertTimeout;
	}

	/**
	 * Sets the insert timeout in ms. for the queue that closed intervals are flushed into
	 * @param submissionQueueInsertTimeout the submission Queue Insert Timeout to set
	 */
	public void setSubmissionQueueInsertTimeout(long submissionQueueInsertTimeout) {
		this.submissionQueueInsertTimeout = submissionQueueInsertTimeout;
	}

	/**
	 * Returns the accumulator flush interval in ms. 
	 * @return the flush Period
	 */
	public long getFlushPeriod() {
		return flushPeriod;
	}

	/**
	 * Sets the accumulator flush interval in ms. 
	 * @param flushPeriod the flush period to set
	 */
	public void setFlushPeriod(long flushPeriod) {
		this.flushPeriod = flushPeriod;
	}

	/**
	 * Returns the flush batch size
	 * @return the flush Batch Size
	 */
	public int getFlushBatchSize() {
		return flushBatchSize;
	}

	/**
	 * Sets the flush batch size
	 * @param flushBatchSize the flush Batch Size to set
	 */
	public void setFlushBatchSize(int flushBatchSize) {
		this.flushBatchSize = flushBatchSize;
	}
	

}
