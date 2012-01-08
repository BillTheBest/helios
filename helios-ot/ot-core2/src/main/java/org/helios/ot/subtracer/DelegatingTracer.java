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
package org.helios.ot.subtracer;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import org.helios.helpers.ClassHelper;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.ot.trace.Trace.Builder;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerImpl;

/**
 * <p>Title: DelegatingTracer</p>
 * <p>Description: An abstract tracer that delegates all tracing calls to an inner concrete tracer. Provided for implementing subtracers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.tracing.subtracer.DelegatingTracer</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public abstract class DelegatingTracer extends TracerImpl {
	/**  */
	private static final long serialVersionUID = 3085712238794346968L;
	/** The inner wrapped concrete tracer */
	protected final ITracer vtracer;
	/** The subtracer stack */
	protected final Vector<DelegatingTracer> stack = new Vector<DelegatingTracer>();
	/** The JMX ObjectName key prefix for subtracers */
	public static final String SUBTRACER_KEY = "SubTracer";
	/** A regex to extract the subtracer # from the prefix for subtracers */
	public static final Pattern SUBTRACER_KEY_PATTERN = Pattern.compile("SubTracer(\\d+)$");

	
	/**
	 * Creates a new DelegatingTracer 
	 * @param vtracer The wrapped inner tracer
	 * @param tracerName The tracer name
	 * @param tracerObjectName The tracer ObjectName
	 */
	protected DelegatingTracer(ITracer vtracer, String tracerName, ObjectName tracerObjectName) {
		super(
				ClassHelper.nvl(tracerName, "Passed tracerName was null"),
				ClassHelper.nvl(tracerObjectName, "Passed tracerObjectName was null"),
				ClassHelper.nvl(vtracer, "Passed vtracer was null").getTracerManager()
		);
		if(vtracer instanceof DelegatingTracer) {			
			Vector<DelegatingTracer> vtracerStack = ((DelegatingTracer)vtracer).stack;
			Set<Class<? extends DelegatingTracer>> unique = new HashSet<Class<? extends DelegatingTracer>>(vtracerStack.size());
			
			for(DelegatingTracer dTracer: vtracerStack) {
				if(unique.contains(dTracer.getClass())) {
					throw new RecursiveSubTracerException("SubTracer instance of type [" + getClass().getName() + "] already has an instance of [" + dTracer.getClass().getName() + "]  in the parent SubTracer stack.");
				}
				unique.add(dTracer.getClass());				
				stack.add(dTracer);
			}
			if(unique.contains(this.getClass())) {
				throw new RecursiveSubTracerException("SubTracer instance of type [" + getClass().getName() + "] already has an instance of [" + this.getClass().getName() + "]  in the parent SubTracer stack [" + this.toString() + "]");
			}
			unique.clear();
		} else {
			// If vtracer is not a sub tracer
			// it must be a root tracer, so there's nothing left to do
		}
		// add this tracer to the end of the stack
		stack.add(this);
		this.vtracer = vtracer;
	}
	
	/**
	 * Returns the tracer name.
	 * @return the tracer name.
	 */
	@Override  // overriden so we can generate a name that reflects the sub-tracer stack
	@JMXAttribute (name="DefaultName", description="The default name of this tracer", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDefaultName() {
		StringBuilder b = new StringBuilder();
		for(DelegatingTracer dt: stack) {
			b.append(dt.getTracerName()).append("<--");
		}
		for(int i = 0; i < 3; i++) {b.deleteCharAt(b.length()-1);}
		return b.toString();
	}
	
	
	/**
	 * Creates a new ObjectName for a subTracer by appending its sequenced key to the parent's ObjectName.
	 * @param dTracerClass The class of the DelegatingTracer an ObjectName is being created for 
	 * @param innerTracer The inner tracer
	 * @param props Additional properties to append to the created object name
	 * @return a new ObjectName
	 */
	protected static ObjectName createObjectName(Class<? extends DelegatingTracer> dTracerClass, ITracer innerTracer, CharSequence...props) {
		String basicName = ClassHelper.nvl(dTracerClass, "Passed dTracerClass was null").getSimpleName();
		ObjectName parentObjectName = ClassHelper.nvl(innerTracer, "Passed innerTracer was null").getTracerObjectName();
		Hashtable<String, String> keyProps = new Hashtable<String, String>();
		int i = -1;
		for(String s: parentObjectName.getKeyPropertyList().keySet()) {
			if(s.startsWith(SUBTRACER_KEY)) {
				Matcher m = SUBTRACER_KEY_PATTERN.matcher(s);
				if(m.matches()) {
					int seq = Integer.parseInt(m.group(1));
					if(seq>i) i = seq;
				}
			}
		}
		i++;
		
		if(props!=null) {
			for(CharSequence s: props) {
				try {
					String[] nameVal = s.toString().split("=");
					keyProps.put(nameVal[0], nameVal[1]);
				} catch (Exception e) {					
					throw new RuntimeException("Appended property [" + s + "] is not in the name=value format", e);
				}
			}
		}
		keyProps.put(SUBTRACER_KEY + i, basicName);
		keyProps.putAll(parentObjectName.getKeyPropertyList());
		return JMXHelper.objectName(parentObjectName.getDomain(), keyProps);
	}
	
	/**
	 * Attempts to acquire the delegating tracer instance from an already registered MBean
	 * @return the DelegatingTracer instance, or null if the ObjectName was not registered
	 */
	public static <T extends DelegatingTracer> T getFromMBeanServer(ObjectName tracerObjectName) {		
		try {
			if(!JMXHelper.getHeliosMBeanServer().isRegistered(tracerObjectName)) {
				return null;
			}
			return (T) JMXHelper.getHeliosMBeanServer().getAttribute(tracerObjectName, "Instance");
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire DelegatingTracer from [" + tracerObjectName + "]", e);
		}
	}
	
	
	//==========================================================
	
	/**
	 * Customizes the output of the builder.
	 * This class defines the method as final since it delegates to subformat.
	 * @param builder A reference to the builder just prior to generating the trace.
	 * @return The builder.
	 */
	@Override
	public final Builder format(Builder builder) {
		builder = vtracer.format(builder);
		return subformat(builder);
	}
	
	/**
	 * Implemented by concrete subtracers to do their bit on the builder before returning the builder for tracing.
	 * @param builder The builder to tweak
	 * @return the tweaked builder or null if this subtracer takes care of the tracing itself
	 */
	public abstract Builder subformat(Builder builder);
	
	

	//==================================================

}
