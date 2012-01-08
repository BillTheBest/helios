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
package org.helios.server.ot.session.camel.transform.trace;

import java.io.Externalizable;
import java.lang.management.ManagementFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.helios.ot.trace.ClosedTrace;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.AbstractTraceValue;
import org.helios.ot.trace.types.interval.AbstractIntervalTraceValue;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.reflections.Reflections;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * <p>Title: TestJSONMarshalling</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.transform.trace.TestJSONMarshalling</code></p>
 */

public class TestJSONMarshalling {
	protected static final Logger LOG = Logger.getLogger(TestJSONMarshalling.class); 
	static Class<?>[] annotatedClasses;
	
	static {
		BasicConfigurator.configure();
		
		annotatedClasses = new Reflections(Trace.class.getPackage().getName())
			.merge(new Reflections(AbstractTraceValue.class.getPackage().getName()))		
			.merge(new Reflections(AbstractIntervalTraceValue.class.getPackage().getName()))
			.getTypesAnnotatedWith(XStreamAlias.class).toArray(new Class[0]);
		
		StringBuilder b = new StringBuilder("\n\tAnnotated Classes");
		for(Class<?> clazz: annotatedClasses) {
			b.append("\n\t\t").append(clazz.getName());
		}
		b.append("\n");
		LOG.info(b.toString());

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//BasicConfigurator.configure();
		ITracer tracer = TracerManager3.getInstance().getTracer();
		Trace trace = tracer.trace(ManagementFactory.getThreadMXBean().getDaemonThreadCount(), "Daemon Thread Count", "Threading", "Threads");
		ClosedTrace ct = ClosedTrace.newClosedTrace(trace);
		//XStream xstream = new XStream(new JettisonMappedXmlDriver());
		//XStream xstream = new XStream();
		XStream xstream = new XStream(new PureJavaReflectionProvider(), new JettisonMappedXmlDriver());
		//xstream = new XStream();
        xstream.setMode(XStream.NO_REFERENCES);
		//xstream.setMode(XStream.ID_REFERENCES);

        
        xstream.registerConverter(new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider()) {        	
        	public boolean canConvert(Class type) {
        		return Externalizable.class.isAssignableFrom(type);
        	}
        }, XStream.PRIORITY_LOW);
        xstream.processAnnotations(annotatedClasses);
        
        //xstream.aliasField("hostName", MetricId.class, "hostName"); 
		//xstream.autodetectAnnotations(true);		
        
        LOG.info("\n" + xstream.toXML(trace.getTraceValue()) + "\n");


	}

}
