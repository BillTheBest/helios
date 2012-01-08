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
package org.helios.server.ot.session.camel.marshal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.camel.Exchange;
import org.apache.camel.spi.DataFormat;
import org.apache.log4j.Logger;
import org.codehaus.jackson.jaxrs.Annotations;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.ManagedObjectDynamicMBean;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.server.ot.session.camel.marshal.custom.ProviderTypeReplacer;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * <p>Title: JaxbJsonMarshalingProvider</p>
 * <p>Description: A wrapper for {@link JaxbJsonMarshalingProvider} that also serves as a Camel marshaler. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.camel.marshal.JaxbJsonMarshalingProvider</code></p>
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json"})
@Produces({MediaType.APPLICATION_JSON, "text/json"})
@JMXManagedObject(annotated=true, declared=true)
public class JaxbJsonMarshalingProvider extends JacksonJaxbJsonProvider implements DataFormat {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The number of provider reads since the last reset */
	protected final AtomicLong providerReads = new AtomicLong(0L);
	/** The number of provider writes since the last reset */
	protected final AtomicLong providerWrites = new AtomicLong(0L);
	/** The number of marshal operations since the last reset */
	protected final AtomicLong marshals = new AtomicLong(0L);
	/** The number of unmarshal operations since the last reset */
	protected final AtomicLong unmarshals = new AtomicLong(0L);
	/** A map of type replacers keyed by the outbound target type */
	protected final Map<Class<?>, ProviderTypeReplacer<?, ?>> replacers = new HashMap<Class<?>, ProviderTypeReplacer<?, ?>>(); 
	/** The managed object dynamic mbean container */
	protected final ManagedObjectDynamicMBean modb = new ManagedObjectDynamicMBean("A wrapper for a JaxbJsonMarshalingProvider that also serves as a Camel marshaler.");
	
	/** Serial number factory */
	protected static final AtomicLong serial = new AtomicLong(0L);
	
	/**
	 * Creates a new JaxbJsonMarshalingProvider
	 */
	public JaxbJsonMarshalingProvider() {
		super();
		register();
	}

	/**
	 * Creates a new JaxbJsonMarshalingProvider
	 * @param annotationsToUse The annotations to use
	 */
	public JaxbJsonMarshalingProvider(Annotations... annotationsToUse) {
		super(annotationsToUse);
		register();
	}

	/**
	 * Creates a new JaxbJsonMarshalingProvider
	 * @param mapper An object mapper to use
	 * @param annotationsToUse The annotations to use.
	 */
	public JaxbJsonMarshalingProvider(ObjectMapper mapper, Annotations[] annotationsToUse) {
		super(mapper, annotationsToUse);
		register();		
	}
	
	/**
	 * Registers a map of replacers with this provider
	 * @param replacers The map of replacers to register
	 */
	public void setReplacers(Map<Class<?>, ProviderTypeReplacer<?, ?>> replacers) {
		if(replacers!=null) {
			for(Map.Entry<Class<?>, ProviderTypeReplacer<?, ?>> entry: replacers.entrySet()) {
				if(entry.getKey()!=null && entry.getValue()!=null) {
					this.replacers.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	
	protected void register() {
		log.info(Banner.banner("*", 3, 10, "Started JaxbJsonMarshalingProvider"));
		ObjectName on = JMXHelper.objectName(getClass().getPackage().getName() + ":service=" + getClass().getSimpleName() + ",serial=" + serial.incrementAndGet() );
		modb.reflectObject(this);
		JMXHelper.getRuntimeHeliosMBeanServer().registerMBean(modb, on);
	}
	
    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,Object> httpHeaders, OutputStream entityStream)  throws IOException {
    	providerWrites.incrementAndGet();
    	//log.info("Marshalling Generic Type:" + genericType);
		ProviderTypeReplacer replacer = replacers.get(value.getClass());
		if(replacer!=null) {
			value = replace(value);
			type = value.getClass();
			genericType = type.getGenericSuperclass();
		}

    	super.writeTo(value, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,String> httpHeaders, InputStream entityStream) throws IOException {
    	providerReads.incrementAndGet();
    	return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
    
	

	@Override
	public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
		marshals.incrementAndGet();
		MultivaluedMap headers = new MultivaluedMapImpl();
		graph = replace(graph);
		writeTo(graph, graph.getClass(), null, null, MediaType.APPLICATION_JSON_TYPE, headers, stream);		
	}
	
	
	/**
	 * Executes a marshal replace for the passed object if a replacer for that type is registered.
	 * @param target The target to tentatively replace
	 * @return the new replacement object or the passed object if no replacer for the passed target was registered.
	 */
	protected Object replace(Object target) {
		if(target==null) throw new IllegalArgumentException("The passed replace target object was null", new Throwable());
		ProviderTypeReplacer replacer = replacers.get(target.getClass());
		if(replacer!=null) {
			return replacer.marshalReplace(target);
		}
		return target;

	}

	@Override
	public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
		unmarshals.incrementAndGet();
		return null;
	}
	
	/**
	 * Returns an array of the class names for which there are registered replacers.
	 * @return an array of class names
	 */
	@JMXAttribute(name="ReplacedClasses", description="An array of the class names for which there are registered replacers", mutability=AttributeMutabilityOption.READ_ONLY)
	public String[] getReplacedClasses() {
		Set<String> set = new HashSet<String>(replacers.size());
		for(Class<?> clazz: replacers.keySet()) {
			set.add(clazz.getName());
		}
		return set.toArray(new String[set.size()]);
	}

	/**
	 * @return the providerReads
	 */
	@JMXAttribute(name="ProviderReads", description="The number of provider reads since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getProviderReads() {
		return providerReads.get();
	}

	/**
	 * @return the providerWrites
	 */
	@JMXAttribute(name="ProviderWrites", description="The number of provider writes since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getProviderWrites() {
		return providerWrites.get();
	}

	/**
	 * @return the marshals
	 */
	@JMXAttribute(name="Marshals", description="The number of marshal operations since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMarshals() {
		return marshals.get();
	}

	/**
	 * @return the unmarshals
	 */
	@JMXAttribute(name="Unmarshals", description="The number of unmarshal operations since the last reset", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUnmarshals() {
		return unmarshals.get();
	}
	
	

}
