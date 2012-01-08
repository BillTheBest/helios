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
package org.helios.ot.trace.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.helios.ot.trace.types.interval.IIntervalTraceValue;
import org.helios.ot.type.MetricType;

/**
 * <p>Title: TraceCL</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.types.TraceCL</code></p>
 */

public class TraceCL {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		log("Starting OT2 Quickie Test...");
		IIntervalTraceValue t = MetricType.INTERVAL_INCIDENT.intervalTraceValue();
		IIntervalTraceValue tvr = MetricType.LONG_AVG.intervalTraceValue();
		IIntervalTraceValue tvi = MetricType.LONG_AVG.intervalTraceValue();
		IIntervalTraceValue tt = MetricType.TIMESTAMP.intervalTraceValue();
				
		List<BigDecimal> bigDs = new ArrayList<BigDecimal>(10);
		List<Long> longs = new ArrayList<Long>(10);
		Random random = new Random(System.nanoTime());
		for(int i = 0; i < 10; i++) {
			tt.apply(MetricType.TIMESTAMP.traceValue(System.currentTimeMillis()));
			t.apply(MetricType.INTERVAL_INCIDENT.traceValue(i));
			tvi.apply(MetricType.LONG_AVG.traceValue(i));
			long rval = Math.abs(random.nextLong() + 1);
			longs.add(rval);
			if(random.nextBoolean()) {
				//rval *= -1;
			}
			
			bigDs.add(new BigDecimal(rval));
			tvr.apply(MetricType.LONG_AVG.traceValue(rval));
			try { Thread.sleep(1000); } catch (Exception e) {}
			
		}
		log("Increment:" + tvi);
		log("Random:" + tvr);
		log("Incident:" + t);
		log("Timestamp:" + tt);
		BigDecimal b = new BigDecimal(0);
		int cnt = 0;
		for(BigDecimal bd : bigDs) {
			b = b.add(bd);
			cnt++;
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
