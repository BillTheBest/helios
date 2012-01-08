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
package test.org.helios.tracing.opentrace.interval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.helios.tracing.trace.MetricId;
import org.helios.tracing.trace.MetricIdReference;
import org.helios.tracing.trace.MetricType;
import org.junit.Assert;
import org.junit.Test;


/**
 * <p>Title: MetricIdTestCase</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.tracing.opentrace.interval.MetricIdTestCase</code></p>
 */

public class MetricIdTestCase {
	
	static {
		MetricId.modSize.set(2);
	}
	
	@Test
	public void testMinimalName() {
		final String name = "host1/agent1/foo";
		MetricId id = MetricId.getInstance(MetricType.INTERVAL_INCIDENT, name);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 0, id.getNamespace().length);
	}
	
	@Test
	public void testWithNameSpaceName() {
		final String name = "host1/agent1/aaa/bbb/foo";
		MetricId id = MetricId.getInstance(MetricType.INTERVAL_INCIDENT, name);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 2, id.getNamespace().length);
		Assert.assertEquals("NameSpace", new String[]{"aaa", "bbb"}, id.getNamespace());
	}
	
	@Test
	public void testSerializeMinimalName() throws Exception {
		final String name = "host1/agent1/foo";
		MetricId preid = MetricId.getInstance(MetricType.INTERVAL_INCIDENT, name);
		MetricId id = (MetricId) writeOutAndReadBack(false, preid);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 0, id.getNamespace().length);
	}
	
	@Test
	public void testSerializeWithNameSpaceName()  throws Exception {
		final String name = "host1/agent1/aaa/bbb/foo";
		MetricId preid = MetricId.getInstance(MetricType.INTERVAL_INCIDENT, name);
		MetricId id = (MetricId) writeOutAndReadBack(false, preid);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 2, id.getNamespace().length);		
		Assert.assertEquals("NameSpace", new String[]{"aaa", "bbb"}, id.getNamespace());
	}

	@Test
	public void testSerializeWithGlobalId()  throws Exception {
		final String name = "host1/agent1/aaa/bbb/foo";
		MetricId preid = MetricId.getInstance(MetricType.INTERVAL_INCIDENT, name);
		
		
		MetricId id = (MetricId) writeOutAndReadBack(false, preid);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 2, id.getNamespace().length);		
		Assert.assertEquals("NameSpace", new String[]{"aaa", "bbb"}, id.getNamespace());
		
		id = (MetricId) writeOutAndReadBack(true, preid);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 2, id.getNamespace().length);		
		Assert.assertEquals("NameSpace", new String[]{"aaa", "bbb"}, id.getNamespace());
		MetricId.assignGlobalId(name, 999L);
		id = (MetricId) writeOutAndReadBack(true, preid);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 2, id.getNamespace().length);		
		Assert.assertEquals("NameSpace", new String[]{"aaa", "bbb"}, id.getNamespace());		
		CompactObjectOutputStream.registerClass(MetricId.class);
		CompactObjectOutputStream.registerClass(MetricIdReference.class);
		id = (MetricId) writeOutAndReadBack(true, preid);
		Assert.assertEquals("HostName", "host1", id.getHostName());
		Assert.assertEquals("AgentName", "agent1", id.getAgentName());
		Assert.assertEquals("MetricName", "foo", id.getMetricName());
		Assert.assertEquals("NameSpace Length", 2, id.getNamespace().length);		
		Assert.assertEquals("NameSpace", new String[]{"aaa", "bbb"}, id.getNamespace());		
		
	}
	
	public static Object writeOutAndReadBack(boolean optimized, Object obj) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = optimized ? new CompactObjectOutputStream(baos) : new ObjectOutputStream(baos);
		oos.writeObject(obj); oos.flush(); baos.flush();
		byte[] arr = baos.toByteArray();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(arr); 
		ObjectInputStream ois = optimized ? new CompactObjectInputStream(bais) :  new ObjectInputStream(bais); 
		Object robj = ois.readObject();
		System.out.println("Object type [" + robj.getClass().getName() + "] was [" + arr.length + "] bytes");
		return robj;
	}
	
	
}
