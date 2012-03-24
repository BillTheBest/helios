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
package org.helios.ot.generic;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;


/**
 * <p>Title: GenericMetricDef</p>
 * <p>Description: A metric definition that serves as a flyweight cached instance for {@link IGenericMetric}s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.generic.GenericMetricDef</code></p>
 */
public class GenericMetricDef implements Externalizable {
	/** The APM Domain */
	private String domain;
	/** The agent host */
	private String host;
	/** The agent process */
	private String process;
	/** The agent name */
	private String agent;
	/** The metric resource */
	private String[] resource;
	/** The metric name */
	private String metricName;
	/** The local full metric name */
	private String localName;	
	/** The fully qualified metric name */
	private String fullName = null;
	/** The metric type code */
	private int typeCode = -1;	
	/** The full name hash code */
	private int fullNameHash = -1;
	/** Indicates if this metric def's hash code is a recognized subst token */
	private boolean recognized = false;	
	/** Regex to split resource */
	public static final Pattern resourcePattern = Pattern.compile("\\|");
	/** Regex to split resource and metric */
	public static final Pattern metricPattern = Pattern.compile(":");
	/** Regex to parse out local name */
	public static final Pattern localPattern = Pattern.compile("\\||:");
	/** Zero segment resource value */	
	public static final String[] EMPTY_RESOURCE =  new String[]{}; 		
	
	/** The flyweight cache */
	private static final Map<Integer, GenericMetricDef> GDEFS = new ConcurrentHashMap<Integer, GenericMetricDef>();
	/** A cache of metric def hash codes that are confirmed by a remote as being recognized and mapable back to a metric def */
	private static final Set<Integer> substTokens = new CopyOnWriteArraySet<Integer>();
	
	
	/**
	 * Creates a new GenericMetricDef
	 * @param fullName The metric full name
	 * @param typeCode The metric data type code
	 * @return the defined GenericMetricDef
	 */
	public static GenericMetricDef getInstance(String fullName, int typeCode) {
		if(fullName==null) throw new IllegalArgumentException("The passed fullName was null", new Throwable());
		fullName = fullName.trim();
		int key = fullName.hashCode();
		GenericMetricDef metricDef = GDEFS.get(key);
		if(metricDef==null) {
			synchronized(GDEFS) {
				metricDef = GDEFS.get(key);
				if(metricDef==null) {
					metricDef = new GenericMetricDef(fullName, typeCode, key);
					GDEFS.put(key, metricDef);
				}
			}
		}
		return metricDef;
	}
	
	/**
	 * Retrieves a metric def from the reference cache
	 * @param hashCode The hash code of the metric def
	 * @return a metric def
	 */
	public static GenericMetricDef getInstance(int hashCode) {
		GenericMetricDef md = GDEFS.get(hashCode);
		if(md==null) {
			throw new RuntimeException("No cached GenericMetricDef for token [" + hashCode + "]", new Throwable());
		}
		return md;
	}
	
	public GenericMetricDef() {}
	
	
	/**
	 * Creates a new GenericMetricDef
	 * @param fullName The metric full name
	 * @param typeCode The metric data type code
	 * @param hashCode The full name hash code
	 */
	public GenericMetricDef(String fullName, int typeCode, int hashCode) {
		this.fullName = fullName;
		this.typeCode = typeCode;
		this.fullNameHash = hashCode;
		init();
	}
	
	
	/**
	 * Marks an array of subst tokens as recognized
	 * @param tokens An array of subst tokens
	 */
	public static void recognizeSubstTokens(int...tokens) {
		if(tokens==null || tokens.length<1) return;
		for(int token: tokens) {
			if(token==0 || substTokens.contains(token)) continue;
			GenericMetricDef metricDef = GDEFS.get(token);
			if(metricDef==null) {
				// Corrupt ?
				System.err.println("GenericMetricDef Corrupted SubstToken Table. Token [" + token + "] was marked as recognized but had no metric def");
				continue;
			}
			metricDef.setRecognized();
		}
	}
	
	/**
	 * Marks an array of subst tokens as recognized
	 * @param tokens An array of subst tokens
	 */
	public static void recognizeSubstTokens(Integer...tokens) {
		if(tokens==null || tokens.length<1) return;
		int[] itokens = new int[tokens.length];
		for(int i = 0; i < tokens.length; i++) {
			itokens[i] = tokens[i];
		}
		recognizeSubstTokens(itokens);
	}
	
	/**
	 * Creates a new GenericMetricDef
	 * @param in An object input stream
	 */
	public GenericMetricDef(ObjectInput in) {
		try {
			this.readExternal(in);
		} catch (Exception e) {
			throw new RuntimeException("Failed to read GenericMetricDef from ObjectInput", e);
		}
	}
	
	/**
	 * Initializes the internal naming fields
	 */
	private void init() {
		String[] frags = localPattern.split(this.fullName);
		domain = frags[0];
		host = frags[1];
		process = frags[2];
		agent = frags[3];
		metricName = frags[frags.length-1];				
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getDomain()
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getHost()
	 */
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getProcess()
	 */
	public String getProcess() {		
		return process;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getAgent()
	 */
	public String getAgent() {
		return agent;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getResource()
	 */
	public String[] getResource() {
		if(resource==null) {
			try {
				resource = resourcePattern.split(metricPattern.split(resourcePattern.split(fullName, 5)[4])[0]);
			} catch (Exception e) {
				resource = EMPTY_RESOURCE;
			}
		}
		return resource;
	}
	
	/**
	 * Returns the resource array with the first <code>trim</code> segments removed.
	 * @param trim The number of leading segments to trim
	 * @return The trimmed resource
	 */
	public String[] getResource(int trim) {
		String[] res = getResource();
		int newLength = res.length-trim;
		String[] trimmed = new String[newLength];
		System.arraycopy(res, trim, trimmed, 0, newLength);
		return trimmed;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getMetricName()
	 */
	public String getMetricName() {
		return metricName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getSegment(int)
	 */
	public String getSegment(int pos) {
		if(pos<0) throw new IllegalArgumentException("Segment index < 0", new Throwable());
		if(pos>(resource.length-1)) throw new IllegalArgumentException("Segment index [" + pos + "] exceeds resource length [" + resource.length + "]", new Throwable());
		return resource[pos];
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getTypeCode()
	 */
	public int getTypeCode() {
		return typeCode;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getFullName()
	 */
	public String getFullName() {
		return fullName;
	}
	
	/**
	 * Returns a hash of the full name
	 * @return the fullNameHash
	 */
	public int getFullNameHash() {
		return fullNameHash;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.wily.IGenericMetric#getLocalName()
	 */
	public String getLocalName() {
		if(localName==null) {
			localName = localPattern.split(fullName, 5)[4];
		}
		return localName;
	}	
	
	/**
	 * Indicates if this metric def's hash code is a recognized subst token
	 * @return true if recognized, false otherwise
	 */
	public boolean isRecognized() {
		return recognized;
	}

	/**
	 * Sets this metric def as recognized
	 */
	public void setRecognized() {
		this.recognized = true;
	}

	
	public static final byte[] NOT_RECOGNIZED = new byte[]{0};
	public static final byte[] RECOGNIZED = new byte[]{1};
	
	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		if(recognized) {
			out.write(RECOGNIZED);
			out.writeInt(fullNameHash);
		} else {
			out.write(NOT_RECOGNIZED);
			out.writeUTF(fullName);
			out.writeInt(typeCode);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte[] recognized = new byte[1];
		in.read(recognized);
		if(recognized[0]==1) {
			int token = in.readInt();
			
		}
		fullName = in.readUTF();
		typeCode = in.readInt();
		fullNameHash = fullName.hashCode();
		init();
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
				+ ((fullName == null) ? 0 : fullName.hashCode());
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
		GenericMetricDef other = (GenericMetricDef) obj;
		if (fullName == null) {
			if (other.fullName != null)
				return false;
		} else if (!fullName.equals(other.fullName))
			return false;
		return true;
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
	    StringBuilder retValue = new StringBuilder("GenericMetricDef [")
	    	.append(TAB).append("fullName:").append(this.fullName)
	    	.append(TAB).append("localName:").append(getLocalName())
	        .append(TAB).append("domain:").append(this.domain)
	        .append(TAB).append("host:").append(this.host)
	        .append(TAB).append("process:").append(this.process)
	        .append(TAB).append("agent:").append(this.agent)
	        .append(TAB).append("resource:").append(Arrays.toString(getResource()))
	        .append(TAB).append("metricName:").append(this.metricName)
	        .append(TAB).append("typeCode:").append(this.typeCode)
	        .append(TAB).append("recognized:").append(this.recognized)
	        .append("\n]");    
	    return retValue.toString();
	}


	

}
