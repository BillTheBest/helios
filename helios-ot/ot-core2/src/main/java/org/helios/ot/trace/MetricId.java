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
package org.helios.ot.trace;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.helios.containers.counters.IntegerCircularCounter;
import org.helios.helpers.ConfigurationHelper;
import org.helios.helpers.ExternalizationHelper;
import org.helios.helpers.StringHelper;
import org.helios.ot.type.MetricType;
import org.helios.patterns.queues.Filterable;
import org.helios.patterns.queues.LongBitMaskFactory.LongBitMaskSequence;

//import com.thoughtworks.xstream.annotations.XStreamAlias;
//import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
//import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * <p>Title: MetricId</p>
 * <p>Description: Represents and manages instances of unique metric identifiers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.MetricId</code></p>
 */
@XmlRootElement(name="metricId")
//@XStreamAlias("metricId")
public class MetricId implements Serializable , Externalizable {
	/** The metric host name */
	@XmlElement(name="hostName")
//	//@XStreamAlias("hostName")
//	//@XStreamAsAttribute
	private String hostName;
	/** The metric agent name */
	@XmlElement(name="agentName")
	//@XStreamAlias("agentName")	
	private String agentName;
	/** The metric point */
	@XmlElement(name="point")
	//@XStreamAlias("point")
	protected String metricName;
	/** The metric name space segments */
	@XmlElement(name="namespace")
	//@XStreamAlias("namespace")
	//@XStreamAsAttribute
	protected String[] namespace;	
	/** The metric type */
	@XmlElement(name="type")
	//@XStreamAlias("type")
	protected MetricType type;
	/** The local metric serial number */
	//@XStreamOmitField
	protected transient int serial;
	/** This metric's mod */
	//@XStreamOmitField
	protected transient int metricMod = -1;
	/** The local accumulator mod */
	//@XStreamOmitField
	protected static transient int mod = -1;
	/** The bitmask representing which tracers the metric have originated from */
	//@XStreamOmitField
	protected final transient AtomicLong tracerMask = new AtomicLong(0);
	
	/** The transient FQN */
	//@XStreamOmitField
	protected volatile transient String fullyQualifiedName = null;
	
	/** The server side designated global ID for this MetricId */
	//@XStreamOmitField
	protected final transient AtomicLong globalId = new AtomicLong(-1L);
	/** The statically set and internal host name where the tracer is running */
	protected static final String _hostName = _hostName();
	/** The statically set and internal application ID where the tracer is running */
	protected static final String _applicationId = _applicationId();
	/** The system property name for the application id */
	public static final String APPLICATION_ID = "org.helios.application.name";
	
	/** The header constant name for the Trace fully qualified name */
	public static final String TRACE_FQN = "fqn";
	/** The header constant name for the Trace point */
	public static final String TRACE_POINT = "point";
	/** The header constant name for the Trace full name (fqn minus host and agent) */
	public static final String TRACE_FULLNAME = "fullname";
	/** The header constant name for the Trace name space segments */
	public static final String TRACE_NAMESPACE = "namespace";
	/** The header constant name for the Trace local namespace (namespace minust host and agent) */
	public static final String TRACE_LNAMESPACE = "lnamespace";
	/** The header constant name for the Trace agent name */
	public static final String TRACE_APP_ID = "agent";
	/** The header constant name for the Trace host */
	public static final String TRACE_HOST = "host";
	/** The header constant name for the Trace type  */
	public static final String TRACE_TYPE = "type";	
	/** The header constant name for the Trace type name */
	public static final String TRACE_TYPE_NAME = "typename";
	/** The header constant name for the Trace type code */
	public static final String TRACE_TYPE_CODE = "typecode";

	/** The default OpenTrace mod {@value DEFAULT_MOD} */
	public static final int DEFAULT_MOD = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()/2;

	
	
	
	/** Metric Id Cache */
	private static final Map<String, MetricId> ids = new ConcurrentHashMap<String, MetricId>(1024);
	/** Global Id Keyed Metric Id Cache */
	private static final Map<Long, MetricId> globalAssignedMetrics = new ConcurrentHashMap<Long, MetricId>(1024);
	
	/** The open trace mod size */
	public static final AtomicInteger modSize = new AtomicInteger(-1);
	/** Mod counter */
	private static volatile transient IntegerCircularCounter modCounter;
	
	/** The system property or env-var that designates the mod size for this VM. */
	public static final String MOD_PROP = "org.helios.ot.mod";
	
	
	/**
	 * Renders the metricId as a name/value map
	 * @return A map of metric Id attributes keyed by header constant name
	 */
	public Map<String, Object> getTraceMap() {
		Map<String, Object> map = new HashMap<String, Object>(16);
		map.put(TRACE_FQN, getFQN());
		map.put(TRACE_POINT, metricName);
		String localNameSpace = StringHelper.fastConcatAndDelim(Trace.DELIM, namespace);
		String fullNameSpace = StringHelper.fastConcatAndDelim(Trace.DELIM, hostName, agentName, localNameSpace);
		map.put(TRACE_NAMESPACE, fullNameSpace);
		map.put(TRACE_LNAMESPACE, localNameSpace);
		map.put(TRACE_FULLNAME, StringHelper.fastConcatAndDelim(Trace.DELIM, localNameSpace, metricName));				
		map.put(TRACE_APP_ID, agentName);
		map.put(TRACE_HOST, hostName);
		map.put(TRACE_TYPE_NAME, type.name());
		map.put(TRACE_TYPE_CODE, type.getCode());
		//map.put(TRACE_TYPE, type);
		return map;
	}	
	
	static {
		MetricIdReference.globalAssignedMetrics = globalAssignedMetrics; 
	}
	
	public static void assignGlobalId(CharSequence fullName, long globalId) {
		MetricId id = ids.get(fullName.toString());
		if(id!=null) {
			id.globalId.set(globalId);
			globalAssignedMetrics.put(globalId, id);
		}
	}
	
	public static int getMod() {
		if(modSize.get()==-1) {
			synchronized(modSize) {
				if(modSize.get()==-1) {
					modSize.set(ConfigurationHelper.getIntSystemThenEnvProperty(MOD_PROP, DEFAULT_MOD));
					mod = modSize.get();
				}
			}
		}
		return mod;
	}
	
	
	
	
	/**
	 * Returns the metricId instance for the passed metric name
	 * @param type The metric type
	 * @param fullName The full metric name
	 * @return the metric Id.
	 */
	public static MetricId getInstance(MetricType type, CharSequence fullName) {
		if(modCounter==null) {
			getMod();
			modCounter = new IntegerCircularCounter(mod);
		}
		if(fullName==null) throw new IllegalArgumentException("Full name was null");		
		String name = fullName.toString().intern();
		MetricId id = ids.get(name);
		if(id==null) {
			synchronized(ids) {
				ids.get(name);
				if(id==null) {
					if(type==null) throw new IllegalArgumentException("Metric type was null");
					id = new MetricId(modCounter.nextCounter(), type, name);
					ids.put(name, id);
				}
			}
		}
		return id;
	}
	
	/**
	 * Returns the mod assigned to this metricId instance
	 * @return this metricId instance's mod
	 */
	public int getMetricMod() {
		return metricMod;
	}

	/**
	 * Creates a new MetricId
	 * @param mod The designated local mod
	 * @param type The metric type
	 * @param fullName The metric name
	 */
	private MetricId(int mod, MetricType type, String fullName) {
		this.metricMod = mod;
		String[] fragments = fullName.toString().split("/");
		if(fragments.length < 3) throw new IllegalArgumentException("Invalid Full Name [" + fullName + "]");
		this.type = type;
		this.serial = generateSerial(fullName);
		hostName = fragments[0];
		agentName = fragments[1];
		metricName = fragments[fragments.length-1];
		namespace = new String[fragments.length-3];
		if(namespace.length>0) {
			System.arraycopy(fragments, 2, namespace, 0, namespace.length);
		}			
	}
	
	/**
	 * Resets the MetricId repository and configuration.
	 */
	public static void reset() {
		modCounter  =  null;
		modSize.set(-1);
		ids.clear();
		globalAssignedMetrics.clear();		
	}
	
	
	/**
	 * Returns the local name. That is the fully qualified name, minus the host and agent.
	 * @return the local name
	 */
	public String getLocalName() {
		String local = StringHelper.fastConcatAndDelim(Trace.DELIM, namespace);
		return StringHelper.fastConcatAndDelim(Trace.DELIM, local, metricName);
	}
	
	/**
	 * Generates a unique serial number for the passed metric name.
	 * @param fullName the full metric name
	 * @return a unique serial number
	 */
	private static int generateSerial(CharSequence fullName) {
		return fullName.toString().hashCode();
	}
	
	/**
	 * Renders the MetricId as a readable string
	 * @return the MetricId as a readable string
	 */
	public String toString() {
		StringBuilder b = new StringBuilder("[");
		b.append(type.name()).append("]").append(getFQN());
		return b.toString();
	}
	
	/**
	 * Returns the fully qualified metric name
	 * @return the fully qualified metric name
	 */
	public String getFQN() {
		if(fullyQualifiedName==null) {
			synchronized(this) {
				if(fullyQualifiedName==null) {
					StringBuilder b = new StringBuilder();
					b.append(hostName).append("/").append(agentName).append("/");
					if(namespace.length>0) {
						for(String s: namespace) {
							b.append(s).append("/");
						}
					}
					b.append(metricName);
					fullyQualifiedName = b.toString();
				}
			}
		}
		return fullyQualifiedName;
	}
	
	
	/**
	 * For Externalizable
	 */
	public MetricId() {}
	
	Object writeReplace() throws ObjectStreamException {
		if(globalId.get()<1) {
			return this;			
		} else {
			return new MetricIdReference(globalId.get());
		}
	}
	
	/**
	 * Reads the MetricId in
	 * @param in The ObjectInput stream
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		boolean isGlobalAssigned = in.readBoolean();
		if(isGlobalAssigned) {
			globalId.set(in.readLong());			
		}
		hostName = ExternalizationHelper.unExternalizeString(in);
		agentName = ExternalizationHelper.unExternalizeString(in);
		metricName = ExternalizationHelper.unExternalizeString(in);
		namespace = ExternalizationHelper.unExternalizeStringArray(in);
		type = MetricType.typeForCode(in.readInt());
	}

	/**
	 * Writes the MetricId out
	 * @param out The ObjectOutput stream
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		boolean isGlobalAssigned = globalId.get()>0;
		out.writeBoolean(isGlobalAssigned);
		if(isGlobalAssigned) {
			out.writeLong(globalId.get());
		} else {
			ExternalizationHelper.externalizeString(out, hostName);
			ExternalizationHelper.externalizeString(out, agentName);
			ExternalizationHelper.externalizeString(out, metricName);
			ExternalizationHelper.externalizeStringArray(out, namespace);
			out.writeInt(type.getCode());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((getFQN() == null) ? 0 : getFQN().hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetricId other = (MetricId) obj;
		return getFQN().equals(other.getFQN());
	}

	/**
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * @return the agentName
	 */
	public String getAgentName() {
		return agentName;
	}

	/**
	 * @return the metricName
	 */
	public String getMetricName() {
		return metricName;
	}

	/**
	 * @return the namespace
	 */
	public String[] getNamespace() {
		return namespace;
	}

	/**
	 * @return the type
	 */
	public MetricType getType() {
		return type;
	}

	/**
	 * @return the serial
	 */
	public int getSerial() {
		return serial;
	}

	
	/**
	 * Determines the agentId from the environment, then sys props then defaults.
	 * @return the generated application id.
	 */
	protected static String _applicationId() {
		String appId =System.getenv(APPLICATION_ID);
		if(appId==null) {
			appId = System.getProperty(APPLICATION_ID, ManagementFactory.getRuntimeMXBean().getName());
		} 
		System.setProperty(APPLICATION_ID, appId);
		return appId;		
	}
	
	/**
	 * Determines the current host name.
	 * @return the current host name.
	 */
	protected static String _hostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {}
		String osName = System.getProperty("os.name").toLowerCase();
		String hn = null;
		if(osName.contains("windows")) {
				hn = System.getenv("COMPUTERNAME");
				if(hn!=null) return hn;
		} else if(osName.contains("linux") || osName.contains("unix")) {
				hn = System.getenv("HOSTNAME");
				if(hn!=null) return hn;
		}
		return ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
	}


	/**
	 * The bitmask representing which tracers the metric have originated from
	 * @return the tracerMask
	 */
	public long getTracerMask() {
		return tracerMask.get();
	}
	
	/**
	 * Applies a tracer's bit mask to the tracer bit mask
	 * @param bit the tracer's bit
	 */
	public void setTracerMask(long bit) {
		tracerMask.set(LongBitMaskSequence.setEnabledFor(tracerMask.get(), bit));
	}
	
	/**
	 * Determines if the tracerMask is enabled for the passed bit
	 * @param bit The tracer bit to test for 
	 * @return true if the tracerMask is enabled for the passed bit
	 */
	public boolean isEnabledFor(long bit) {
		return LongBitMaskSequence.isEnabledFor(tracerMask.get(), bit);
	}


//	/**
//	 * Determines if the passed bit is enabled in the tracer mask
//	 * @param filterKey The mask bit of a FilteredBlockingQueue
//	 * @return true if this item should be dropped from the FilteredBlockingQueue  (i.e. the bitmask is not enabled)
//	 */
//	@Override
//	public boolean drop(Long filterKey) {		
//		long mask = tracerMask.get();
//		if(mask==0) return false;
//		return !LongBitMaskSequence.isEnabledFor(mask, filterKey);
//	}

	/**
	 * Returns the default local agent host name
	 * @return the default local agent host name
	 */
	public static String getHostname() {
		return _hostName;
	}

	/**
	 * Returns the default local agent appId
	 * @return the default local agent appId
	 */
	public static String getApplicationId() {
		return _applicationId;
	}
	
	
}

