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
package org.helios.esper.engine.service.interactive;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EventBean;

/**
 * <p>Title: Render</p>
 * <p>Description: Enumerates built-in rendering options for interactives and encapsulates the renderers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.esper.engine.service.interactive.Render</code></p>
 */

public enum Render implements Serializable {
	/** Renders an Event bean to the underlying pojo */
	POJO(new POJORenderer()),
	/** Renders an Event bean to JSON */
	JSON(new JSONRenderer()),
	/** Returns the EventBeans unmodified */
	EVENT(new EventRenderer()),	
	/** Renders an Event bean to XML */
	XML(new XMLRenderer());
	
	
	/*
	 * TODO:
	 * EVENT Renderer: Sends EventBean: Needs serialization wrapper for remote sends.
	 * HTML Renderer: Sends formatted HTML
	 */
	
	/** A map of renders keyed by name */
	private static Map<String, Render> NAME_MAP = new HashMap<String, Render>(Render.values().length);
	
	static {
		for(Render r: Render.values()) {
			NAME_MAP.put(r.name(), r);
		}		
	}
	
	/**
	 * Returns the default render
	 * @return the default render
	 */
	public static Render defaultRender() {
		return POJO;
	}
	
	/**
	 * Creates a new Render 
	 * @param renderer the Render's encapsulated Renderer 
	 */
	private Render(Renderer renderer) {
		this.renderer = renderer;
	}
	
	/**
	 * Serializes the render as the render's name
	 * @return the render name
	 * @throws ObjectStreamException
	 */
	Object writeReplace() throws ObjectStreamException {
		return name();
	}
	
	/** the Render's encapsulated Renderer */
	private final Renderer renderer;
	
	/**
	 * Invokes the renderer for this Render
	 * @param runtime The esper runtime
	 * @param bean the event beans to render
	 * @return the rendered events
	 */
	public Object render(EPRuntime runtime, EventBean...beans) {
		return renderer.render(runtime, beans);
	}
	
	/**
	 * Indicates if the passed name is a valid render. (Case insensitive)
	 * @param name the name to test
	 * @return true if the name is a valid render.
	 */
	public static boolean isRender(String name) {
		if(name==null) return false;
		return NAME_MAP.containsKey(name.toUpperCase());
	}
	
	/**
	 * <p>Title: Renderer</p>
	 * <p>Description: Defines an EventBean rendering option</p> 
	 */
	public static interface Renderer {
		/**
		 * Renders an EventBean
		 * @param runtime An esper runtime
		 * @param beans the event beans to render
		 * @return the rendering of the event beans.
		 */
		public Object render(EPRuntime runtime, EventBean...beans);
				
	}
	
	/**
	 * <p>Title: JSONRenderer</p>
	 * <p>Description: An EventBean JSON Renderer</p> 
	 */
	public static class JSONRenderer implements Renderer {
		/**
		 * Renders an EventBean as JSON
		 * @param runtime An esper runtime
		 * @param beans the event beans to render
		 * @return the JSON rendering of the event beans.
		 */
		public String render(EPRuntime runtime, EventBean...beans) {
			StringBuilder b = new StringBuilder();
			for(EventBean bean: beans) {
				b.append(runtime.getEventRenderer().renderJSON(bean.getEventType().getName(), bean));
			}
			return b.toString();
		}
	}
	
	/**
	 * <p>Title: XMLRenderer</p>
	 * <p>Description: An EventBean XML Renderer</p> 
	 */
	public static class XMLRenderer implements Renderer {
		/**
		 * Renders an EventBean as XML
		 * @param runtime An esper runtime
		 * @param beans the event beans to render
		 * @return the XML rendering of the event beans.
		 */
		public String render(EPRuntime runtime, EventBean...beans) {
			StringBuilder b = new StringBuilder();
			for(EventBean bean: beans) {
				b.append(runtime.getEventRenderer().renderXML(bean.getEventType().getName(), bean));
			}
			return b.toString();
		}
	}
	
	/**
	 * <p>Title: POJORenderer</p>
	 * <p>Description: An EventBean POJO that renders the underlying bean of each event</p> 
	 */
	public static class POJORenderer implements Renderer {
		/**
		 * Returns the underlying Event Bean pojo
		 * @param runtime An esper runtime
		 * @param bean the event beans to extract the pojo from
		 * @return the event beans underlying pojos
		 */
		public Object render(EPRuntime runtime, EventBean...beans) {
			Set<Object> pojos = new HashSet<Object>(beans.length);
			for(EventBean bean: beans) {
				pojos.add(bean.getUnderlying());
			}
			return pojos;
		}
	}
	
	/**
	 * <p>Title: EventRenderer</p>
	 * <p>Description: An EventBean POJO Renderer Passthrough</p> 
	 */
	public static class EventRenderer implements Renderer {
		/**
		 * Returns the Event Bean
		 * @param runtime An esper runtime
		 * @param bean the event beans to extract the pojo from
		 * @return the event beans 
		 */
		public Object render(EPRuntime runtime, EventBean...beans) {
			return beans;
		}
	}	
	
	
}
