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
package org.helios.jmx.reference;

import java.lang.management.ManagementFactory;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: InstanceCountingSoftReferenceQueue</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class InstanceCountingSoftReferenceQueue<T> extends SoftReference<T> {
	protected static AtomicLong gcCount = new AtomicLong(0);
	protected SoftReference<FinalizeCounter<T>> sr = null;
	/**
	 * 
	 */
	public InstanceCountingSoftReferenceQueue(T referent) {
		super(null);
		sr = new SoftReference<FinalizeCounter<T>>(new FinalizeCounter<T>(referent, gcCount));		
	}
	
	/**
	 * 
	 */
	public InstanceCountingSoftReferenceQueue(T referent,  ReferenceQueue<? super T> q) {
		super(null);
		sr = new SoftReference<FinalizeCounter<T>>(new FinalizeCounter<T>(referent, gcCount));
	}
	
	public T get() {
		FinalizeCounter<T> fc = sr.get();
		if(fc!=null) return fc.getRef();
		else return null;
	}

	/**	
	 * @return the gcCount
	 */
	public long getGcCount() {
		return gcCount.get();
	}
	
	public static void main(String[] args) {
		List<InstanceCountingSoftReferenceQueue<Payload>> stress = new ArrayList<InstanceCountingSoftReferenceQueue<Payload>>();
		Properties p = System.getProperties();
		StringBuilder buff = new StringBuilder();
		for(Object key: p.keySet()) {
			for(int i = 0; i < 100; i++) {
				buff.append(p.get(key));
			}
		}
		byte[] payload = buff.toString().getBytes();
//		ReferenceQueue<Payload> rq = new ReferenceQueue<Payload>();
		for(int i = 0; i < 1000000; i++) {
//			byte[] b = new byte[payload.length];
//			System.arraycopy(payload, 0, b, 0, payload.length);
			stress.add(new InstanceCountingSoftReferenceQueue<Payload>(new Payload(payload)));
			if(i%1000==0) {
				System.out.println("Added 1000 Items [" + payload.length + "]. Total:" + i);
				System.out.println("Heap:" + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
				int notNulls = 0;
//				for(InstanceCountingSoftReferenceQueue<Payload> srq: stress) {
//					if(srq.get() != null) notNulls++;
//				}
				System.out.println("Finalizeds:" + InstanceCountingSoftReferenceQueue.gcCount.get());
				
			}
		}
	}
}

class FinalizeCounter<T> {
	protected T ref = null;
	protected AtomicLong cnt = null;

	/**
	 * @param ref
	 * @param cnt
	 */
	public FinalizeCounter(T ref, AtomicLong cnt) {
		super();
		this.ref = ref;
		this.cnt = cnt;
	}

	/**
	 * @return the ref
	 */
	public T getRef() {
		return ref;
	}
	
	public void finalize() throws Throwable {
		cnt.incrementAndGet();
		super.finalize();		
	}	
	
}

class Payload {
	protected byte[] payload = null;
	protected static AtomicLong cnt = new AtomicLong(0);
	/**
	 * @param payload
	 */
	public Payload(byte[] payload) {
		super();
		this.payload = payload;
	}
	
	public void finalize() throws Throwable {
		//cnt.incrementAndGet();
		super.finalize();
		//System.out.println("Finalize Count:" + cnt.incrementAndGet());
	}
	
}
