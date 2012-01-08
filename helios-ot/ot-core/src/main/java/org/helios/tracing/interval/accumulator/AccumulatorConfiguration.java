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
import java.util.Properties;

import org.helios.helpers.ConfigurationHelper;

/**
 * <p>Title: AccumulatorConfiguration</p>
 * <p>Description: Values class for Accumulator configuration values </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.interval.accumulator.AccumulatorConfiguration</code></p>
 */

public class AccumulatorConfiguration implements Serializable {
	/**  */
	private static final long serialVersionUID = 5042447770663895148L;
	
	/** The default size for the map that each accumulator maintains of accumulating intervals */
	public static final int DEFAULT_ACCUMULATOR_MAP_SIZE = 256;
	/** The default load factor for the map that each accumulator maintains of accumulating intervals */
	public static final float DEFAULT_ACCUMULATOR_MAP_LOAD = 0.75f;
	/** The default size of the processing queue that traces are dropped into in order to be processed by the accumulator */
	public static final int DEFAULT_ACCUMULATOR_SUB_QUEUE_SIZE = 200;
	/** The default fairness of the processing queue that traces are dropped into in order to be processed by the accumulator */
	public static final boolean DEFAULT_ACCUMULATOR_SUB_QUEUE_FAIRNESS = false;
	/** The default maximum amount of time (in ms) an accumulator will wait on the accumulator latch (ie. waiting for other accumulators to finish) */
	public static final long DEFAULT_ACCUMULATOR_LATCH_TIMEOUT = 500L;
	/** The default maximum amount of time (in ms) an accumulator will wait on the queue for a trace to process */
	public static final long DEFAULT_ACCUMULATOR_QUEUE_TIMEOUT = 10L;
	
	/** The configuration prefix */
	public final String CONFIG_PREFIX = getClass().getPackage().getName() + ".accumulator.";
	
	/** The accumulator interval map size. */
	protected int accumulatorMapSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_ACCUMULATOR_MAP_SIZE);
	/** The accumulator interval map load factor. */
	protected float accumulatorMapLoadFactor = ConfigurationHelper.getFloatSystemThenEnvProperty(CONFIG_PREFIX + "mapload", DEFAULT_ACCUMULATOR_MAP_LOAD);	
	/** The accumulator queue size. */
	protected int accumulatorQueueSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "queuesize", DEFAULT_ACCUMULATOR_SUB_QUEUE_SIZE);
	/** The accumulator queue fairness. */
	protected boolean accumulatorQueueFairness = ConfigurationHelper.getBooleanSystemThenEnvProperty(CONFIG_PREFIX + "queuefair", DEFAULT_ACCUMULATOR_SUB_QUEUE_FAIRNESS);
	/** The accumulator post flush latch wait timeout. */
	protected long accumulatorLatchTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "latchtimeout", DEFAULT_ACCUMULATOR_LATCH_TIMEOUT);
	/** The accumulator queue wait timeout. */
	protected long accumulatorQueueTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "queuetimeout", DEFAULT_ACCUMULATOR_QUEUE_TIMEOUT);
	
	
	/**
	 * Creates a new AccumulatorConfiguration
	 */
	public AccumulatorConfiguration() {
		
	}
	
	/**
	 * Creates a new AccumulatorConfiguration
	 * @param props Configuration properties
	 */
	public AccumulatorConfiguration(Properties props) {
		accumulatorMapSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "mapsize", DEFAULT_ACCUMULATOR_MAP_SIZE, props);
		accumulatorMapLoadFactor = ConfigurationHelper.getFloatSystemThenEnvProperty(CONFIG_PREFIX + "mapload", DEFAULT_ACCUMULATOR_MAP_LOAD, props);	
		accumulatorQueueSize = ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_PREFIX + "queuesize", DEFAULT_ACCUMULATOR_SUB_QUEUE_SIZE, props);
		accumulatorQueueFairness = ConfigurationHelper.getBooleanSystemThenEnvProperty(CONFIG_PREFIX + "queuefair", DEFAULT_ACCUMULATOR_SUB_QUEUE_FAIRNESS, props);
		accumulatorLatchTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "latchtimeout", DEFAULT_ACCUMULATOR_LATCH_TIMEOUT, props);
		accumulatorQueueTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(CONFIG_PREFIX + "queuetimeout", DEFAULT_ACCUMULATOR_LATCH_TIMEOUT, props);
	}
	
	
	/**
	 * Creates a new AccumulatorConfiguration
	 * @param accumulatorMapSize size for the map that each accumulator maintains of accumulating intervals
	 * @param accumulatorMapLoadFactor load factor for the map that each accumulator maintains of accumulating intervals
	 * @param accumulatorQueueSize size of the processing queue that traces are dropped into in order to be processed by the accumulator
	 * @param accumulatorQueueFairness fairness of the processing queue that traces are dropped into in order to be processed by the accumulator 
	 * @param accumulatorLatchTimeout maximum amount of time (in ms) an accumulator will wait on the accumulator latch (ie. waiting for other accumulators to finish)
	 * @param accumulatorQueueTimeout accumulator queue wait timeout
	 */
	public AccumulatorConfiguration(int accumulatorMapSize, float accumulatorMapLoadFactor, int accumulatorQueueSize, boolean accumulatorQueueFairness, long accumulatorLatchTimeout) {
		this.accumulatorMapSize = accumulatorMapSize;
		this.accumulatorMapLoadFactor = accumulatorMapLoadFactor;
		this.accumulatorQueueSize = accumulatorQueueSize;
		this.accumulatorQueueFairness = accumulatorQueueFairness;
		this.accumulatorLatchTimeout = accumulatorLatchTimeout;
	}


	/**
	 * Returns the size for the map that each accumulator maintains of accumulating intervals 
	 * @return the accumulator Map Size
	 */
	public int getAccumulatorMapSize() {
		return accumulatorMapSize;
	}


	/**
	 * Sets the size for the map that each accumulator maintains of accumulating intervals 
	 * @param accumulatorMapSize the accumulator Map Size to set
	 */
	public void setAccumulatorMapSize(int accumulatorMapSize) {
		this.accumulatorMapSize = accumulatorMapSize;
	}


	/**
	 * Returns the load factor for the map that each accumulator maintains of accumulating intervals
	 * @return the accumulator Map LoadFactor
	 */
	public float getAccumulatorMapLoadFactor() {
		return accumulatorMapLoadFactor;
	}


	/**
	 * Sets the load factor for the map that each accumulator maintains of accumulating intervals
	 * @param accumulatorMapLoadFactor the accumulator Map Load Factor to set
	 */
	public void setAccumulatorMapLoadFactor(float accumulatorMapLoadFactor) {
		this.accumulatorMapLoadFactor = accumulatorMapLoadFactor;
	}


	/**
	 * Returns the size of the processing queue that traces are dropped into in order to be processed by the accumulator
	 * @return the accumulator Queue Size
	 */
	public int getAccumulatorQueueSize() {
		return accumulatorQueueSize;
	}


	/**
	 * Sets the size of the processing queue that traces are dropped into in order to be processed by the accumulator
	 * @param accumulatorQueueSize the accumulator Queue Size to set
	 */
	public void setAccumulatorQueueSize(int accumulatorQueueSize) {
		this.accumulatorQueueSize = accumulatorQueueSize;
	}


	/**
	 * Returns the fairness of the processing queue that traces are dropped into in order to be processed by the accumulator
	 * @return the accumulator Queue fairness
	 */
	public boolean isAccumulatorQueueFair() {
		return accumulatorQueueFairness;
	}


	/**
	 * Sets the fairness of the processing queue that traces are dropped into in order to be processed by the accumulator
	 * @param accumulatorQueueFairness the accumulator Queue fairness to set
	 */
	public void setAccumulatorQueueFair(boolean accumulatorQueueFairness) {
		this.accumulatorQueueFairness = accumulatorQueueFairness;
	}


	/**
	 * Returns the maximum amount of time (in ms) an accumulator will wait on the accumulator latch (ie. waiting for other accumulators to finish)
	 * @return the accumulator Latch Timeout
	 */
	public long getAccumulatorLatchTimeout() {
		return accumulatorLatchTimeout;
	}


	/**
	 * Sets the maximum amount of time (in ms) an accumulator will wait on the accumulator latch (ie. waiting for other accumulators to finish)
	 * @param accumulatorLatchTimeout the accumulator Latch Timeout to set
	 */
	public void setAccumulatorLatchTimeout(long accumulatorLatchTimeout) {
		this.accumulatorLatchTimeout = accumulatorLatchTimeout;
	}
	
	/**
	 * Returns the maximum amount of time (in ms) an accumulator will wait on the queue for a trace to process
	 * @return the accumulator Queue Timeout
	 */
	public long getAccumulatorQueueTimeout() {
		return accumulatorQueueTimeout;
	}


	/**
	 * Sets the maximum amount of time (in ms) an accumulator will wait on the queue for a trace to process
	 * @param accumulatorLatchTimeout the accumulator Queue Timeout to set
	 */
	public void setAccumulatorQueueTimeout(long accumulatorQueueTimeout) {
		this.accumulatorLatchTimeout = accumulatorQueueTimeout;
	}
	


	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("AccumulatorConfiguration [")
	        .append(TAB).append("accumulatorMapSize = ").append(this.accumulatorMapSize)
	        .append(TAB).append("accumulatorMapLoadFactor = ").append(this.accumulatorMapLoadFactor)
	        .append(TAB).append("accumulatorQueueSize = ").append(this.accumulatorQueueSize)
	        .append(TAB).append("accumulatorQueueFairness = ").append(this.accumulatorQueueFairness)
	        .append(TAB).append("accumulatorLatchTimeout = ").append(this.accumulatorLatchTimeout)
	        .append(TAB).append("accumulatorQueueTimeout = ").append(this.accumulatorQueueTimeout)
	        .append("\n]");    
	    return retValue.toString();
	}
	

}
