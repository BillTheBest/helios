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
package org.helios.jmx.threadservices.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: ThreadResourceSnapshot</p>
 * <p>Description: A value holder class for capturing a snapshot of a thread's currrent ThreadInfo stats.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ThreadResourceSnapshot {
	protected long cpuTime = 0L;
	protected long waitTime = 0L;
	protected long waitCount = 0L;
	protected long blockTime = 0L;
	protected long blockCount = 0L;
	protected long elapsedTimeNs = 0L;
	protected long elapsedTimeMs = 0L;
	protected int percentageCpu = 0;
	protected int percentageStopped = 0;
	protected boolean open = true;
	protected String name = Thread.currentThread().getName();
	protected long id = Thread.currentThread().getId();
	protected static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	protected static int cpuCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	protected String serial = ("" + Thread.currentThread().getId()) + System.nanoTime();
	/**
	 * @return the serial
	 */
	public String getSerial() {
		return serial;
	}
	

	/**
	 * Creates a new ThreadResourceSnapshot and populates the snapshot values for the current thread.
	 */
	public ThreadResourceSnapshot() {
		init();
	}
	
	
	/**
	 * Creates a new ThreadResourceSnapshot and populates the snapshot values for the current thread. 
	 * @param empty If true, the snapshot will be -1 initialized.
	 */
	public ThreadResourceSnapshot(boolean empty) {
		if(empty) {
			elapsedTimeNs = -1;
			elapsedTimeMs = -1;
			cpuTime = -1;
			waitTime = -1;
			waitCount = -1;
			blockTime = -1;
			blockCount = -1;
			percentageCpu = -1;
			percentageStopped = -1;
			open = false;			
		} else {
			init();
		}
	}
	
	/**
	 * Initializes the snapshot. 
	 */
	protected void init() {
		elapsedTimeNs = System.nanoTime();
		elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTimeNs, TimeUnit.NANOSECONDS);
		
		if(ThreadResourceMonitor.isThreadCpuTimeSupported()) {
			cpuTime = threadMXBean.getThreadCpuTime(id);
		}		
		if(ThreadResourceMonitor.isThreadContentionSupported()) {
			ThreadInfo ti = threadMXBean.getThreadInfo(Thread.currentThread().getId());
			waitTime = ti.getWaitedTime();
			waitCount = ti.getWaitedCount();
			blockTime = ti.getBlockedTime();
			blockCount = ti.getBlockedCount();
		}		
	}
	
	
	
	/**
	 * Caputres a new snapshot of thread stats and computes deltas.
	 */
	public void close() {
		if(!open) return;
		open = false;	
		elapsedTimeNs = System.nanoTime()-elapsedTimeNs;
		//elapsedTimeMs = System.currentTimeMillis()-elapsedTimeMs;
		elapsedTimeMs = TimeUnit.MILLISECONDS.convert(elapsedTimeNs, TimeUnit.NANOSECONDS);
		if(ThreadResourceMonitor.isThreadCpuTimeSupported()) {
			long newTime = threadMXBean.getThreadCpuTime(id)-cpuTime;
			cpuTime = newTime;			
		} else {
			cpuTime = -1L;					
		}
		if(ThreadResourceMonitor.isThreadContentionSupported()) {			
			ThreadInfo ti = threadMXBean.getThreadInfo(Thread.currentThread().getId());
			waitTime = ti.getWaitedTime()-waitTime;
			waitCount = ti.getWaitedCount()-waitCount;
			blockTime = ti.getBlockedTime()-blockTime;
			blockCount = ti.getBlockedCount()-blockCount;			
		} else {
			waitTime = -1L;
			waitCount = -1L;
			blockTime = -1L;
			blockCount = -1L;					
		}
		if(Thread.currentThread().getId() != id) {
			throw new RuntimeException("ThreadResourceSnapshot was opened by Thread[" + id + "] but closed by Thread [" + Thread.currentThread().getId() + "]");
		}
		if(ThreadResourceMonitor.isThreadCpuTimeSupported()) {
			percentageCpu = calcPercentage(cpuTime, (elapsedTimeNs));
		} else {
			percentageCpu = -1;
		}
		if(ThreadResourceMonitor.isThreadContentionSupported()) {
			percentageStopped = calcPercentage((waitTime + blockTime), elapsedTimeMs);
		} else {
			percentageStopped = -1;
		}
	}
	
	/**
	 * Percentage calculator
	 * @param fraction
	 * @param total
	 * @return
	 */
	protected static int calcPercentage(float fraction, float total) {
		if(fraction==0 || total==0) return 0;
		float percent = fraction/total*100;
		return (int)percent;
	}
	
	
	
	

	/**
	 * @return the cpuTime
	 */
	public long getCpuTime() {
		return cpuTime;
	}

	/**
	 * @return the waitTime
	 */
	public long getWaitTime() {
		return waitTime;
	}

	/**
	 * @return the waitCount
	 */
	public long getWaitCount() {
		return waitCount;
	}

	/**
	 * @return the blockTime
	 */
	public long getBlockTime() {
		return blockTime;
	}

	/**
	 * @return the blockCount
	 */
	public long getBlockCount() {
		return blockCount;
	}

	/**
	 * @return the open
	 */
	public boolean isOpen() {
		return open;
	}


	/**
	 * @return the percentageCpu
	 */
	public int getPercentageCpu() {
		return percentageCpu;
	}

	/**
	 * @return the percentageStopped
	 */
	public int getPercentageStopped() {
		return percentageStopped;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    StringBuilder retValue = new StringBuilder("ThreadResourceSnapshot:");
	    retValue.append(name).append("[").append(id).append("] State:").append(open ? "Open" : "Closed (");
	    retValue.append("\n\tcpuTime:").append(this.cpuTime);
	    retValue.append("\n\twaitTime:").append(this.waitTime);
	    retValue.append("\n\twaitCount:").append(this.waitCount);
	    retValue.append("\n\tblockTime:").append(this.blockTime);
	    retValue.append("\n\tblockCount:").append(this.blockCount);
	    retValue.append("\n\telapsedTimeNs:").append(this.elapsedTimeNs);
	    retValue.append("\n\telapsedTimeMs:").append(this.elapsedTimeMs);
	    retValue.append("\n\tpercentageCpu:").append(this.percentageCpu);
	    retValue.append("\n\tpercentageStopped:").append(this.percentageStopped);
	    retValue.append("\n)");	    
	    return retValue.toString();
	}
	
	public static void main(String[] args) {
		System.out.println("Starting ThreadResourceSnapshot Test");
		Synchronizer s = new Synchronizer();
		for(int x = 0; x < 10; x++) {
			ThreadResourceSnapshot trs = new ThreadResourceSnapshot();
			try {
				Thread.sleep(200);
				StringBuilder b = new StringBuilder();
				for(int i = 0; i < 1000000; i++) {
					b.append(System.currentTimeMillis());
					if(i%10000==0) {
						b.setLength(0);
						BlockerThread bt = new BlockerThread(s);
						bt.start();
						Thread.sleep(200);
						s.block(20);
						System.gc();
					}
				}				
				trs.close();
				System.out.println(trs);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * @return the elapsedTimeNs
	 */
	public long getElapsedTimeNs() {
		return elapsedTimeNs;
	}


	/**
	 * @return the elapsedTimeMs
	 */
	public long getElapsedTimeMs() {
		return elapsedTimeMs;
	}
	
}

class Synchronizer {
	public synchronized void block(long blockTime) {
		try {
			Thread.currentThread().join(blockTime);
		} catch (Exception e) {}
	}
}

class BlockerThread extends Thread {
	Synchronizer s = null;
	public BlockerThread(Synchronizer s) {
		this.setDaemon(true);
		this.s = s;
	}
	public void run() {
		s.block(300);
		try {
			sleep(50);
		} catch (Exception e) {}		
	}
}
