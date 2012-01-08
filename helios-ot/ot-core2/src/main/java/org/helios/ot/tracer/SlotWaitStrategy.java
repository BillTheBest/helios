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
package org.helios.ot.tracer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.helios.ot.tracer.disruptor.TraceCollection;

/**
 * <p>Title: SlotWaitStrategy</p>
 * <p>Description: Enumerates the options for waiting for an available slot from TracerManager slot queue</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.tracer.SlotWaitStrategy</code></p>
 */

public enum SlotWaitStrategy {
	/** Waiting thread is blocked until a slot is available */
	Blocking(new BlockingWaitStrategy()),
	/** Waiting thread spins in a yield loop until a slot is available */
	Yield(new YieldWaitStrategy()),
	/** Waiting thread spins in a sleep for 1 ms loop until a slot is available */
	Sleep(new SleepWaitStrategy()),
	/** Waiting thread spins in a join for 1 ms loop until a slot is available */
	Join(new JoinWaitStrategy()),	
	/** Waiting thread spins in busy loop until a slot is available */
	Spin(new SpinWaitStrategy()),
	/** Attempts a no-wait poll and returns immediately if a slot is not available */
	NoWait(new NoWaitStrategy());
	
	/** The default strategy */
	public static final SlotWaitStrategy DEFAULT = Blocking;
	
	/** The number of ns in a ms */
	public static final long NS_IN_MS = TimeUnit.NANOSECONDS.convert(1L, TimeUnit.MILLISECONDS);
	
	/**
	 * Creates a new SlotWaitStrategy Enum
	 * @param strategy the strategy implementation for this enum
	 */
	private SlotWaitStrategy(ISlotWaitStrategy strategy) {
		this.strategy = strategy;
	}
	
	/** The enum's strategy instance */
	private final ISlotWaitStrategy strategy;

	/**
	 * Returns this enum's strategy instance
	 * @return the strategy
	 */
	public ISlotWaitStrategy getStrategy() {
		return strategy;
	}
	
	
	/**
	 * <p>Title: ISlotWaitStrategy</p>
	 * <p>Description: Defines a class that uses a specific strategy to acquire a TraceCollection from the TraceCollection slot queue</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.SlotWaitStrategy.ISlotWaitStrategy</code></p>
	 */
	public static interface ISlotWaitStrategy {
		/**
		 * Waits for a slot until a slot is returned or the passed timeout (in ms.) elapses
		 * @param queue The queue to get the TraceCollection from
		 * @param timeout the maximum elapsed time to wait for a slot in ms.
		 * @return the acquired slot or null if the reques timed out or the wait was interrupted
		 */
		public TraceCollection getSlot(BlockingQueue<TraceCollection> queue, long timeout);
		
		/**
		 * Returns the SlotWaitStrategy for this impl.
		 * @return the SlotWaitStrategy 
		 */
		public SlotWaitStrategy getStrategy();
		
	}

	/**
	 * <p>Title: BlockingWaitStrategy</p>
	 * <p>Description: Slot wait strategy where waiting thread is blocked until a slot is available</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.SlotWaitStrategyBlockingWaitStrategy</code></p>
	 */
	public static class BlockingWaitStrategy implements ISlotWaitStrategy {
		/**
		 * Waits for a slot until a slot is returned or the passed timeout (in ms.) elapses
		 * @param queue The queue to get the TraceCollection from
		 * @param timeout the maximum elapsed time to wait for a slot in ms.
		 * @return the acquired slot or null if the reques timed out or the wait was interrupted
		 */
		public TraceCollection getSlot(BlockingQueue<TraceCollection> queue, long timeout) {
			try {
				return queue.poll(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ie) {
				Thread.interrupted();
				return null;
			}
		}
		
		/**
		 * Returns the SlotWaitStrategy for this impl.
		 * @return the SlotWaitStrategy 
		 */
		public SlotWaitStrategy getStrategy() {
			return Blocking;
		}
		
		
	}
	
	/**
	 * <p>Title: YieldWaitStrategy</p>
	 * <p>Description: Slot wait strategy where waiting thread spins in a yield loop until a slot is available</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.SlotWaitStrategyBlockingWaitStrategy</code></p>
	 */
	public static class YieldWaitStrategy implements ISlotWaitStrategy {
		/**
		 * Waits for a slot until a slot is returned or the passed timeout (in ms.) elapses
		 * @param queue The queue to get the TraceCollection from
		 * @param timeout the maximum elapsed time to wait for a slot in ms.
		 * @return the acquired slot or null if the reques timed out
		 */
		public TraceCollection getSlot(BlockingQueue<TraceCollection> queue, long timeout) {
			long until = System.currentTimeMillis() + (timeout * NS_IN_MS);
			TraceCollection tc = null;
			while(tc==null) {
				tc = queue.poll();
				if(tc!=null || System.nanoTime()>=until) break;
				Thread.yield();
			}
			return tc;
		}
		
		/**
		 * Returns the SlotWaitStrategy for this impl.
		 * @return the SlotWaitStrategy 
		 */
		public SlotWaitStrategy getStrategy() {
			return Yield;
		}
		
	}
	
	/**
	 * <p>Title: SleepWaitStrategy</p>
	 * <p>Description: Slot wait strategy where waiting thread thread spins in a sleep for 1 ms loop until a slot is available</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.SlotWaitStrategyBlockingWaitStrategy</code></p>
	 */
	public static class SleepWaitStrategy implements ISlotWaitStrategy  {
		/**
		 * Waits for a slot until a slot is returned or the passed timeout (in ms.) elapses or the thread is interrupted
		 * @param queue The queue to get the TraceCollection from
		 * @param timeout the maximum elapsed time to wait for a slot in ms.
		 * @return the acquired slot or null if the reques timed out or the wait was interrupted
		 */
		public TraceCollection getSlot(BlockingQueue<TraceCollection> queue, long timeout) {
			long until = System.currentTimeMillis() + (timeout * NS_IN_MS);
			TraceCollection tc = null;
			while(tc==null) {
				tc = queue.poll();
				if(tc!=null || System.nanoTime()>=until) break;
				try {
					Thread.sleep(1);
				} catch (InterruptedException ie) {
					Thread.interrupted();
					return null;					
				}
			}
			return tc;
		}
		
		/**
		 * Returns the SlotWaitStrategy for this impl.
		 * @return the SlotWaitStrategy 
		 */
		public SlotWaitStrategy getStrategy() {
			return Sleep;
		}
		
	}
	
	/**
	 * <p>Title: JoinWaitStrategy</p>
	 * <p>Description: Slot wait strategy where waiting thread thread spins in a join for 1 ms loop until a slot is available</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.SlotWaitStrategyBlockingWaitStrategy</code></p>
	 */
	public static class JoinWaitStrategy implements ISlotWaitStrategy {
		/**
		 * Waits for a slot until a slot is returned or the passed timeout (in ms.) elapses or the thread is interrupted
		 * @param queue The queue to get the TraceCollection from
		 * @param timeout the maximum elapsed time to wait for a slot in ms.
		 * @return the acquired slot or null if the reques timed out or the wait was interrupted
		 */
		public TraceCollection getSlot(BlockingQueue<TraceCollection> queue, long timeout) {
			long until = System.currentTimeMillis() + (timeout * NS_IN_MS);
			TraceCollection tc = null;
			while(tc==null) {
				tc = queue.poll();
				if(tc!=null || System.nanoTime()>=until) break;
				try {
					Thread.currentThread().join(1);
				} catch (InterruptedException ie) {
					Thread.interrupted();
					return null;					
				}
			}
			return tc;
		}		
		
		/**
		 * Returns the SlotWaitStrategy for this impl.
		 * @return the SlotWaitStrategy 
		 */
		public SlotWaitStrategy getStrategy() {
			return Join;
		}
		
	}
	
	/**
	 * <p>Title: SpinWaitStrategy</p>
	 * <p>Description: Slot wait strategy where waiting thread thread spins busy loop until a slot is available or the timeout elapses</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.SlotWaitStrategyBlockingWaitStrategy</code></p>
	 */
	public static class SpinWaitStrategy implements ISlotWaitStrategy {
		/**
		 * Waits for a slot until a slot is returned or the passed timeout (in ms.) elapses or the thread is interrupted
		 * @param queue The queue to get the TraceCollection from
		 * @param timeout the maximum elapsed time to wait for a slot in ms.
		 * @return the acquired slot or null if the reques timed out or the wait was interrupted
		 */
		public TraceCollection getSlot(BlockingQueue<TraceCollection> queue, long timeout) {
			long until = System.currentTimeMillis() + (timeout * NS_IN_MS);
			TraceCollection tc = null;
			while(tc==null) {
				tc = queue.poll();
				if(tc!=null || System.nanoTime()>=until) break;
			}
			return tc;
		}

		/**
		 * Returns the SlotWaitStrategy for this impl.
		 * @return the SlotWaitStrategy 
		 */
		public SlotWaitStrategy getStrategy() {
			return Spin;
		}

	}
	
	/**
	 * <p>Title: NoWaitStrategy</p>
	 * <p>Description: Slot wait strategy where waiting thread does not wait if there is no slot available</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.ot.tracer.SlotWaitStrategyBlockingWaitStrategy</code></p>
	 */
	public static class NoWaitStrategy implements ISlotWaitStrategy {
		/**
		 * Attempts to acquire a slot and returns immediately if one is not available
		 * @param queue The queue to get the TraceCollection from
		 * @param timeout ignored
		 * @return the acquired slot or null if the reques timed out or the wait was interrupted
		 */
		public TraceCollection getSlot(BlockingQueue<TraceCollection> queue, long timeout) {			
			return queue.poll();
		}
		
		/**
		 * Returns the SlotWaitStrategy for this impl.
		 * @return the SlotWaitStrategy 
		 */
		public SlotWaitStrategy getStrategy() {
			return NoWait;
		}
		
	}

	
	
}
