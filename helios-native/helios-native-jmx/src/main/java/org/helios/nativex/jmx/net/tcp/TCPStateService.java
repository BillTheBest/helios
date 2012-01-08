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
package org.helios.nativex.jmx.net.tcp;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.LongDeltaRollingCounter;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.Tcp;

/**
 * <p>Title: TCPStateService</p>
 * <p>Description: Native monitor for TCP socket states </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.net.TCPStateService</code></p>
 */

public class TCPStateService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = -4965961946123380784L;
	/** The TCP state gatherer */
	protected final Tcp tcp;
	
	/** The number of times TCP connections have made a direct transition to the SYN-SENT state from the CLOSED state. */
	protected final LongRollingCounter activeOpensCounter = new LongRollingCounter("TCPActiveOpensCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of TCP connections have made a direct transition to the SYN-SENT state from the CLOSED state. */
	protected final LongDeltaRollingCounter activeOpensRate = new LongDeltaRollingCounter("TCPActiveOpensRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The number of times TCP connections have made a direct transition to the SYN-RCVD state from the LISTEN state. */
	protected final LongRollingCounter passiveOpensCounter = new LongRollingCounter("TCPPassiveOpensCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of TCP connections have made a direct transition to the SYN-RCVD state from the LISTEN state. */
	protected final LongDeltaRollingCounter passiveOpensRate = new LongDeltaRollingCounter("TCPPassiveOpensRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The number of times TCP connections have made a direct transition to the CLOSED state from either the SYN-SENT state or the SYN-RCVD state, plus the number of times TCP connections have made a direct transition to the LISTEN state from the SYN-RCVD state. */
	protected final LongRollingCounter attemptFailsCounter = new LongRollingCounter("TCPAttemptFailsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of TCP connections have made a direct transition to the CLOSED state from either the SYN-SENT state or the SYN-RCVD state, plus the rate of TCP connections have made a direct transition to the LISTEN state from the SYN-RCVD state. */
	protected final LongDeltaRollingCounter attemptFailsRate = new LongDeltaRollingCounter("TCPAttemptFailsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The number of times TCP connections have made a direct transition to the CLOSED state from either the ESTABLISHED state or the CLOSE-WAIT state. */
	protected final LongRollingCounter estabResetsCounter = new LongRollingCounter("TCPEstabResetsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of TCP connections have made a direct transition to the CLOSED state from either the ESTABLISHED state or the CLOSE-WAIT state. */
	protected final LongDeltaRollingCounter estabResetsRate = new LongDeltaRollingCounter("TCPEstabResetsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The number of TCP connections for which the current state is either ESTABLISHED or CLOSE- WAIT. */
	protected final LongRollingCounter currEstabCounter = new LongRollingCounter("TCPCurrEstabCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The total number of segments received, including those received in error. This count includes segments received on currently established connections. */
	protected final LongRollingCounter inSegsCounter = new LongRollingCounter("TCPInSegsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of segments received, including those received in error. This count includes segments received on currently established connections. */
	protected final LongDeltaRollingCounter inSegsRate = new LongDeltaRollingCounter("TCPInSegsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The total number of segments sent, including those on current connections but excluding those containing only retransmitted octets. */
	protected final LongRollingCounter outSegsCounter = new LongRollingCounter("TCPOutSegsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of segments sent, including those on current connections but excluding those containing only retransmitted octets. */
	protected final LongDeltaRollingCounter outSegsRate = new LongDeltaRollingCounter("TCPOutSegsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The total number of segments retransmitted - that is, the number of TCP segments transmitted containing one or more previously transmitted octets. */
	protected final LongRollingCounter retransSegsCounter = new LongRollingCounter("TCPRetransSegsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of segments retransmitted - that is, the rate of TCP segments transmitted containing one or more previously transmitted octets. */
	protected final LongDeltaRollingCounter retransSegsRate = new LongDeltaRollingCounter("TCPRetransSegsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The total number of segments received in error (e.g., bad TCP checksums). */
	protected final LongRollingCounter inErrsCounter = new LongRollingCounter("TCPInErrsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of segments received in error (e.g., bad TCP checksums). */
	protected final LongDeltaRollingCounter inErrsRate = new LongDeltaRollingCounter("TCPInErrsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The number of TCP segments sent containing the RST flag. */
	protected final LongRollingCounter outRstsCounter = new LongRollingCounter("TCPOutRstsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The rate of TCP segments sent containing the RST flag. */
	protected final LongDeltaRollingCounter outRstsRate = new LongDeltaRollingCounter("TCPOutRstsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	
	
	public TCPStateService() {
		super();
		tcp = HeliosSigar.getInstance().getTcp();		
		this.scheduleSampling();
		registerCounterMBean("service", "TCPStateService");
		initPerfCounters();
	}
	
	/**
	 * Gathers and increments swap stats
	 */
	@Override
	public void run() {
		try {
			tcp.gather(HeliosSigar.getInstance().getSigar());
			long gathered = -1L;
			gathered = tcp.getActiveOpens();
			activeOpensCounter.put(gathered);
			activeOpensRate.put(gathered);
			gathered = tcp.getPassiveOpens();
			passiveOpensCounter.put(gathered);
			passiveOpensRate.put(gathered);
			gathered = tcp.getAttemptFails();
			attemptFailsCounter.put(gathered);
			attemptFailsRate.put(gathered);
			gathered = tcp.getEstabResets();
			estabResetsCounter.put(gathered);
			estabResetsRate.put(gathered);
			gathered = tcp.getCurrEstab();
			currEstabCounter.put(gathered);
			gathered = tcp.getInSegs();
			inSegsCounter.put(gathered);
			inSegsRate.put(gathered);
			gathered = tcp.getOutSegs();
			outSegsCounter.put(gathered);
			outSegsRate.put(gathered);
			gathered = tcp.getRetransSegs();
			retransSegsCounter.put(gathered);
			retransSegsRate.put(gathered);
			gathered = tcp.getInErrs();
			inErrsCounter.put(gathered);
			inErrsRate.put(gathered);
			gathered = tcp.getOutRsts();
			outRstsCounter.put(gathered);
			outRstsRate.put(gathered);			
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		}
	}
	
	
	/**
	 * Boots this service
	 */
	public static void boot() {
		new TCPStateService();
	}
	
	/**
	 * Returns The number of times TCP connections have made a direct transition to the SYN-SENT state from the CLOSED state. 
	 * @return a counter value
	 */
	@JMXAttribute(name="ActiveOpens", description="The number of times TCP connections have made a direct transition to the SYN-SENT state from the CLOSED state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getActiveOpens() {
		return activeOpensCounter.getLastValue();
	}

	/**
	 * Returns The rate of TCP connections have made a direct transition to the SYN-SENT state from the CLOSED state. 
	 * @return a rate value
	 */
	@JMXAttribute(name="ActiveOpensRate", description="The rate of TCP connections have made a direct transition to the SYN-SENT state from the CLOSED state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getActiveOpensRate() {
		return activeOpensRate.getLastValue();
	}

	/**
	 * Returns The number of times TCP connections have made a direct transition to the SYN-RCVD state from the LISTEN state. 
	 * @return a counter value
	 */
	@JMXAttribute(name="PassiveOpens", description="The number of times TCP connections have made a direct transition to the SYN-RCVD state from the LISTEN state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPassiveOpens() {
		return passiveOpensCounter.getLastValue();
	}

	/**
	 * Returns The rate of TCP connections have made a direct transition to the SYN-RCVD state from the LISTEN state. 
	 * @return a rate value
	 */
	@JMXAttribute(name="PassiveOpensRate", description="The rate of TCP connections have made a direct transition to the SYN-RCVD state from the LISTEN state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPassiveOpensRate() {
		return passiveOpensRate.getLastValue();
	}

	/**
	 * Returns The number of times TCP connections have made a direct transition to the CLOSED state from either the SYN-SENT state or the SYN-RCVD state, plus the number of times TCP connections have made a direct transition to the LISTEN state from the SYN-RCVD state. 
	 * @return a counter value
	 */
	@JMXAttribute(name="AttemptFails", description="The number of times TCP connections have made a direct transition to the CLOSED state from either the SYN-SENT state or the SYN-RCVD state, plus the number of times TCP connections have made a direct transition to the LISTEN state from the SYN-RCVD state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAttemptFails() {
		return attemptFailsCounter.getLastValue();
	}

	/**
	 * Returns The rate of TCP connections have made a direct transition to the CLOSED state from either the SYN-SENT state or the SYN-RCVD state, plus the rate of TCP connections have made a direct transition to the LISTEN state from the SYN-RCVD state. 
	 * @return a rate value
	 */
	@JMXAttribute(name="AttemptFailsRate", description="The rate of TCP connections have made a direct transition to the CLOSED state from either the SYN-SENT state or the SYN-RCVD state, plus the rate of TCP connections have made a direct transition to the LISTEN state from the SYN-RCVD state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAttemptFailsRate() {
		return attemptFailsRate.getLastValue();
	}

	/**
	 * Returns The number of times TCP connections have made a direct transition to the CLOSED state from either the ESTABLISHED state or the CLOSE-WAIT state. 
	 * @return a counter value
	 */
	@JMXAttribute(name="EstabResets", description="The number of times TCP connections have made a direct transition to the CLOSED state from either the ESTABLISHED state or the CLOSE-WAIT state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getEstabResets() {
		return estabResetsCounter.getLastValue();
	}

	/**
	 * Returns The rate of TCP connections have made a direct transition to the CLOSED state from either the ESTABLISHED state or the CLOSE-WAIT state. 
	 * @return a rate value
	 */
	@JMXAttribute(name="EstabResetsRate", description="The rate of TCP connections have made a direct transition to the CLOSED state from either the ESTABLISHED state or the CLOSE-WAIT state.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getEstabResetsRate() {
		return estabResetsRate.getLastValue();
	}

	/**
	 * Returns The number of TCP connections for which the current state is either ESTABLISHED or CLOSE- WAIT. 
	 * @return a counter value
	 */
	@JMXAttribute(name="CurrEstab", description="The number of TCP connections for which the current state is either ESTABLISHED or CLOSE- WAIT.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getCurrEstab() {
		return currEstabCounter.getLastValue();
	}

	/**
	 * Returns The total number of segments received, including those received in error. This count includes segments received on currently established connections. 
	 * @return a counter value
	 */
	@JMXAttribute(name="InSegs", description="The total number of segments received, including those received in error. This count includes segments received on currently established connections.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInSegs() {
		return inSegsCounter.getLastValue();
	}

	/**
	 * Returns The rate of segments received, including those received in error. This count includes segments received on currently established connections. 
	 * @return a rate value
	 */
	@JMXAttribute(name="InSegsRate", description="The rate of segments received, including those received in error. This count includes segments received on currently established connections.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInSegsRate() {
		return inSegsRate.getLastValue();
	}

	/**
	 * Returns The total number of segments sent, including those on current connections but excluding those containing only retransmitted octets. 
	 * @return a counter value
	 */
	@JMXAttribute(name="OutSegs", description="The total number of segments sent, including those on current connections but excluding those containing only retransmitted octets.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOutSegs() {
		return outSegsCounter.getLastValue();
	}

	/**
	 * Returns The rate of segments sent, including those on current connections but excluding those containing only retransmitted octets. 
	 * @return a rate value
	 */
	@JMXAttribute(name="OutSegsRate", description="The rate of segments sent, including those on current connections but excluding those containing only retransmitted octets.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOutSegsRate() {
		return outSegsRate.getLastValue();
	}

	/**
	 * Returns The total number of segments retransmitted - that is, the number of TCP segments transmitted containing one or more previously transmitted octets. 
	 * @return a counter value
	 */
	@JMXAttribute(name="RetransSegs", description="The total number of segments retransmitted - that is, the number of TCP segments transmitted containing one or more previously transmitted octets.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRetransSegs() {
		return retransSegsCounter.getLastValue();
	}

	/**
	 * Returns The rate of segments retransmitted - that is, the rate of TCP segments transmitted containing one or more previously transmitted octets. 
	 * @return a rate value
	 */
	@JMXAttribute(name="RetransSegsRate", description="The rate of segments retransmitted - that is, the rate of TCP segments transmitted containing one or more previously transmitted octets.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getRetransSegsRate() {
		return retransSegsRate.getLastValue();
	}

	/**
	 * Returns The total number of segments received in error (e.g., bad TCP checksums). 
	 * @return a counter value
	 */
	@JMXAttribute(name="InErrs", description="The total number of segments received in error (e.g., bad TCP checksums).", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInErrs() {
		return inErrsCounter.getLastValue();
	}

	/**
	 * Returns The rate of segments received in error (e.g., bad TCP checksums). 
	 * @return a rate value
	 */
	@JMXAttribute(name="InErrsRate", description="The rate of segments received in error (e.g., bad TCP checksums).", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getInErrsRate() {
		return inErrsRate.getLastValue();
	}

	/**
	 * Returns The number of TCP segments sent containing the RST flag. 
	 * @return a counter value
	 */
	@JMXAttribute(name="OutRsts", description="The number of TCP segments sent containing the RST flag.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOutRsts() {
		return outRstsCounter.getLastValue();
	}

	/**
	 * Returns The rate of TCP segments sent containing the RST flag. 
	 * @return a rate value
	 */
	@JMXAttribute(name="OutRstsRate", description="The rate of TCP segments sent containing the RST flag.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOutRstsRate() {
		return outRstsRate.getLastValue();
	}

	
}
