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
package org.helios.logging.appenders;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;


/**
 * <p>Title: LinkedListAppender</p>
 * <p>Description: A log4j appender that writes log messages to a constrained size FIFO linked list.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class LinkedListAppender extends AppenderSkeleton {
	/** the default maximum size */
	public static final int DEFAULT_SIZE = 100;
	/** the default event message rendering pattern */
	public static final String DEFAULT_PATTERN = "%d{dd MMM yyyy HH:mm:ss,SSS} %-5p - %m";
	/** the default timeout on acquiring the list lock in ms. */
	public static final long DEFAULT_TIMEOUT = 10;
	/** the platform line separator string */
	public static final String CR = System.getProperty("line.separator", "\n");
	/** the size of the platform line separator string */
	public static final int CR_SIZE = CR.length();
	/** failed display report */
	public static final String FAIL_DISPLAY_MSG = "Event Report Could Not Be Displayed. Please Try Again.";
	
	/** the target for the logger's non-soft appender messages */
	protected LinkedList<String> events = new LinkedList<String>();
	/** the target for the logger's soft appender messages */
	protected LinkedList<SoftReference<String>> softEvents = new LinkedList<SoftReference<String>>();
	
	/** the maximum number of log entries to store before dropping old events */ 
	protected int size = DEFAULT_SIZE;
	/** indicates if soft references are held on events to allow garbage collection and be more memory friendly, but potentially lose events */
	protected boolean soft = true;
	/** the logging appender pattern */
	protected String eventPattern = null;
	/** The appender pattern */
	protected PatternLayout layout = new PatternLayout(DEFAULT_PATTERN);
	/** The accumulating total size of the messages */
	protected int bufferSize = 0;
	/** A re-entrant lock granting exclusive access on the link list to the acquring thread */
	protected ReentrantLock listLock = new ReentrantLock(true);
	/** The timeout on waiting for the list lock */
	protected long timeOut = DEFAULT_TIMEOUT;
	

	/**
	 * Creates a new LinkedListAppender
	 */
	public LinkedListAppender() {
		setThreshold(Level.DEBUG);
	}

	/**
	 * Not used. Implemented for forward compatibility with log4j 1.3
	 * @param isActive
	 */
	public LinkedListAppender(boolean isActive) {
		super(isActive);		
	}
	
	/** 
	 * Activates any set options. 
	 * @see org.apache.log4j.AppenderSkeleton#activateOptions()
	 */
	@Override
	public void activateOptions() {
		if(eventPattern != null) {
			layout.setConversionPattern(eventPattern);
		}
		this.setLayout(layout);
	}

	/**
	 * Pass off a logging event from a logger.
	 * If the logging thread cannot acquire the listLock in <code>timeOut</code>, it will silently return. 
	 * @param loggingEvent The log event.
	 * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
	 */
	@Override
	protected void append(LoggingEvent loggingEvent) {
		boolean acquiredLock = false;
		try {
			acquiredLock = listLock.tryLock(timeOut, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				String renderedMessage = layout.format(loggingEvent);
				bufferSize += renderedMessage.length() + CR_SIZE;
				if(soft) {
					softEvents.addLast(new SoftReference<String>(renderedMessage));
					if(softEvents.size()>size) {
						bufferSize -= removeFirstSoftEvent() - CR_SIZE;
					}
				} else {
					events.addLast(renderedMessage);
					if(events.size()>size) {
						bufferSize -= events.removeFirst().length() - CR_SIZE;
					}			
				}
			}
		} catch (Exception e) {
		} finally {
			if(acquiredLock) {
				try { listLock.unlock(); } catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Purges the contents of the appender.
	 * Will silently return 0 if the lock could not be acquired.
	 * @return the number of entries removed.
	 */
	public int purge() {
		boolean acquiredLock = false;
		int eSize = 0;
		try {
			acquiredLock = listLock.tryLock(timeOut, TimeUnit.MILLISECONDS);
			 
			if(acquiredLock) {
				
				if(soft) {
					eSize = softEvents.size(); 
					softEvents.clear();
				} else {
					eSize = events.size();
					events.clear();
				}
				bufferSize = 0;
			}
			return eSize;
		} catch (Exception e) {
			return eSize;
		} finally {
			if(acquiredLock) {
				try { listLock.unlock(); } catch (Exception e) {}
			}
			
		}		
	}
	
	/**
	 * Returns a formatted string reporting all the events.
	 * @return A string of events.
	 */
	public String displayEvents() {
		StringBuilder buff = new StringBuilder(bufferSize);
		for(String s: getEvents()) {
			buff.append(s).append(CR);
		}
		return buff.toString();
	}
	
	/**
	 * Returns an array of all the events.
	 * @return An array of events.
	 */
	protected String[] getEvents() {
		boolean acquiredLock = false;
		List<String> evts = null;
		try {
			acquiredLock = listLock.tryLock(timeOut, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
				evts = new ArrayList<String>(soft ? softEvents.size() : events.size());
			}
		} catch (Exception e) {			
		} finally {
			if(acquiredLock) {
				try { listLock.unlock(); } catch (Exception e) {}
			}			
		}
		if(!acquiredLock) return new String[]{FAIL_DISPLAY_MSG};
		String msg = null;
		if(soft) {
			for(SoftReference<String> ref: softEvents) {
				msg = ref.get();
				if(msg != null) {
					evts.add(msg);
				}
			}
		} else {
			evts.addAll(events);
		}
		return evts.toArray(new String[evts.size()]);
	}
	
	
/*
 * 	Prototype lock Checking method.
 * 
	public void doSomething() {
		boolean acquiredLock = false;
		try {
			acquiredLock = listLock.tryLock(timeOut, TimeUnit.MILLISECONDS);
			if(acquiredLock) {
			}
		} catch (Exception e) {
		} finally {
			if(acquiredLock) {
				try { listLock.unlock(); } catch (Exception e) {}
			}
		}		
	}
 */	
	
	/**
	 * Removes the first event from the softEvent list.
	 * @return The size of the removed string or zero if the list was empty, or the event has been collected.
	 */
	protected int removeFirstSoftEvent() {
		if(softEvents.size()<1) return 0;
		SoftReference<String> sr = softEvents.removeFirst();
		String event = sr.get();
		if(event!=null) {
			return event.length();
		} else {
			return 0;
		}
	}

	/** 
	 * Deallocates resources associated with the appender.
	 * In this case, it will clear the linked list.
	 * @see org.apache.log4j.Appender#close()
	 */
	public void close() {
		events.clear();
		softEvents.clear();
	}

	/** Indicates if a layout is required.
	 * @return true if layout is required.
	 * @see org.apache.log4j.Appender#requiresLayout()
	 */
	public boolean requiresLayout() {
		return false;
	}
	
	/**
	 * Basic command line test and example.
	 * @param args
	 */
	public static void main(String args[]) {
		int loops = 100;
		SysOutLog("LinkedListAppender " + loops + " Event Test.");
		LinkedListAppender lla = new LinkedListAppender();
		Logger log = Logger.getLogger("LinkedListAppender.Test");
		log.setLevel(Level.DEBUG);
		log.setAdditivity(false);
		log.addAppender(lla);
		
		for(int x = 0; x < 2; x++) {
			lla.purge();
			long elapsedNanos = 0;
			
			for(int i = 0; i < loops; i++) {
				String message = "Hello World, " + new java.util.Date();
				long start = System.nanoTime();
				log.info(message);
				elapsedNanos += (System.nanoTime()-start);
			}
			
			SysOutLog("Completed " + loops + " " + (lla.soft ? "Soft" : "Hard") + " logs in " + elapsedNanos + " ns.");
			float avg = (float)elapsedNanos/(float)loops;
			long avgNanos = (long)avg;
			SysOutLog("Average Time:" + avgNanos + " ns.");
			lla.soft = !lla.soft;
		}
		SysOutLog(lla.displayEvents());
	}
	
	/**
	 * System Out print testing helper.
	 * @param message
	 */
	public static void SysOutLog(Object message) {
		System.out.println(message);
	}

}
