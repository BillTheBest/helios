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
package org.helios.tracing.trace;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.Map;

/**
 * <p>Title: MetricIdReference</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.MetricIdReference</code></p>
 */

public class MetricIdReference implements Externalizable {
	/** The global Id Reference */
	private long globalId;
	/** Global Id Keyed Metric Id Cache */
	static Map<Long, MetricId> globalAssignedMetrics;
	
	
	/**
	 * Creates a new MetricIdReference
	 * @param globalId
	 */
	public MetricIdReference(long globalId) {
		this.globalId = globalId;		
	}
	
	/**
	 * For externalization only
	 */
	public MetricIdReference() {}

	Object readResolve() throws ObjectStreamException {
		return globalAssignedMetrics.get(globalId);
	}

	/**
	 * Reads the MetricIdRef in
	 * @param in The ObjectInput stream
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		globalId = in.readLong();
	}

	/**
	 * Writes the MetricIdRef out
	 * @param out The ObjectOutput stream
	 * @throws IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(globalId);
	}
	
	
	
}

