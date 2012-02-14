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
package org.helios.server.ot.session.requester;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.helios.helpers.Banner;
import org.helios.helpers.JMXHelper;
import org.helios.ot.trace.ClosedTrace;
import org.helios.ot.trace.MetricId;
import org.helios.server.ot.cache.MetricNameLookup;
import org.helios.server.ot.cache.MetricTreeEntry;
import org.helios.server.ot.cache.MetricTreeEntry.DynaTreeMetricNode;
import org.helios.server.ot.jms.pubsub.RegExMatch;
import org.helios.server.ot.session.OutputFormat;
import org.helios.server.ot.session.OutputFormat.OutputSizeHandler;
import org.helios.server.ot.session.SubscriberSession;
import org.helios.server.ot.session.camel.routing.ISubscriberRoute;
import org.helios.server.ot.session.http.SubscriberSessionHttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * <p>Title: SubscriptionRequester</p>
 * <p>Description: Entry point for requesting a new subscription</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.requester.SubscriptionRequester</code></p>
 */
@Path("/sub")
@Component
@Scope("singleton")
public class SubscriptionRequester implements ApplicationContextAware, CamelContextAware, ContextResolver<JAXBContext> {
	/** The attribute key for the session listener */
	public static final String HTTP_SESSION_LISTENER = "org.helios.ot.web.subscriber.listener";
	/** The attribute key for the subscriber session */
	public static final String HTTP_SESSION_SUBSCRIBER = "org.helios.ot.web.subscriber";
	
	/** The default output format */
	public static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.JSON;
	
	
	
	/** The Spring app context */
	protected ApplicationContext applicationContext = null;
	/** The Camel context */
	protected CamelContext camelContext = null;
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/** The MetricName Cache Lookup Service */
	@Autowired(required=true)
	@Qualifier("MetricLookup")
	protected MetricNameLookup metricNameLookup = null;
	
	/** The metric tree cache */
	@Autowired(required=true)
	@Qualifier("metricTreeCache")	
	protected Cache metricTreeCache;
	
	/** The last metric cache */
	@Autowired(required=true)
	@Qualifier("lastMetricCache")	
	protected Cache lastMetricCache;
	
	
	
	private Class<?>[] types = {MetricId.class, MetricId[].class};
	private final JAXBContext context;
	
	
	/**
	 * Creates a new SubscriptionRequester
	 */
	public SubscriptionRequester() {
		try {
			 context = new JSONJAXBContext(JSONConfiguration.natural().build(), types);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create JSONJAXBContext", e);
		}
		log.info("Created SubscriptionRequester");
	}
	
	/**
	 * Returns the server date
	 * @return The server date
	 */
	@GET
	@Produces({"text/plain"})
	@Path("/serverdate")
	public String serverDate() {
		return new Date().toString();
	}
	
	/**
	 * Returns the session ID
	 * @param request The Http Request
	 * @return the http session Id.
	 */
	@GET
	@Produces({"text/plain"})
	@Path("/sessionid")
	public String sessionId(@Context HttpServletRequest request) {
		if(request==null) {
			return "<null>";
		}
		return request.getSession().getId();		
	}
	
	/**
	 * Initiates a SubscriberSession for an HTTP client
	 * @param request The http request
	 * @param outputFormat The name of the OutputFormat
	 * @param supportsWebSocket If true, this session will a WebSocket communication channel to the client, otherwise, will use a poller. 
	 * @return Returns the JMX ObjectName of the subscriber session if the start succeeded.
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/startsubscriber/{format}/{supportsWebSocket}")
	public String startSubscriber(@Context HttpServletRequest request, @PathParam("format") String outputFormat, @PathParam("supportsWebSocket") boolean supportsWebSocket) {
		if(request==null) {
			throw new RuntimeException("startSubscriber call had no request");
		}
		checkContext();
		String sessionId = sessionId(request);
		boolean exists = SubscriberSession.isSessionCreated(sessionId);
		if(!exists) {
			log.info(Banner.banner("+", 3, 10, new String[]{
					"Starting Subscriber Session",
					"Session:" + sessionId,
					"Output Format:" + outputFormat,
					"WebSocket:" + supportsWebSocket
			}));			
			SubscriberSession subSess = (SubscriberSession)applicationContext.getBean("SubscriberSession", sessionId, outputFormat, supportsWebSocket ? "WebSocketHttpSubscriptionProcessor" : "PollingHttpSubscriptionProcessor");
			SubscriberSessionHttpSessionListener sessionListener = (SubscriberSessionHttpSessionListener)applicationContext.getBean("SubscriberSessionHttpSessionListener", subSess );
			request.getSession().setAttribute(HTTP_SESSION_LISTENER, sessionListener);
			request.getSession().setAttribute(HTTP_SESSION_SUBSCRIBER, subSess);
			log.info(Banner.banner("+", 3, 10, new String[]{
					"Started Subscriber Session",
					"Session:" + sessionId,
					"Output Format:" + outputFormat,
					"WebSocket:" + supportsWebSocket
			}));			
			
		}
		return JMXHelper.objectName("org.helios.server.ot.session:type=SubscriberSession,id=" + sessionId).toString();		
	}
	
	/**
	 * Terminates a SubscriberSession for an HTTP client
	 * @param request The http request
	 * @return the session ID.
	 */
	@GET
	@Produces({"text/plain"})
	@Path("/stopsubscriber")
	public String stopSubscriber(@Context HttpServletRequest request) {
		if(request==null) {
			return "<null>";
		}
		checkContext();			
		String sessionId = sessionId(request);
		boolean exists = SubscriberSession.isSessionCreated(sessionId);
		if(!exists) return "NOSUBSCRIBER";
		SubscriberSession.getInstance(sessionId).terminate();		
		request.getSession().removeAttribute(HTTP_SESSION_LISTENER);
		request.getSession().removeAttribute(HTTP_SESSION_SUBSCRIBER);
		log.info(Banner.banner("-", 3, 10, new String[]{
				"Terminated Subscriber Session",
				"Session:" + sessionId
		}));			
		
		return "OK";		
	}

	@Path("/formap")	
	@POST
	@Produces("text/plain")	
	@Consumes("application/x-www-form-urlencoded")
	public String formap(MultivaluedMap<String, String> formParams) {
		Map<String, String> map = mvpToMap(formParams);
		log.info("Map:" + map);
		return map.toString();
	}
	
	public static Map<String, String> mvpToMap(MultivaluedMap<String, String> form) {
		Map<String, String> map = new HashMap<String, String>(form.size());
		for(Map.Entry<String, List<String>> entry: form.entrySet()) {
			map.put(entry.getKey(), entry.getValue().iterator().next());
		}
		return map;
	}
	
	public static MultivaluedMap<String, String> mapToMvp(Map<String, String> map) {
		MultivaluedMap<String, String> mvp = new MultivaluedMapImpl();
		for(Map.Entry<String, String> entry: map.entrySet()) {
			mvp.put(entry.getKey(), new ArrayList(Arrays.asList(entry.getValue())));
		}
		return mvp;
	}
	

	
	@Path("/env")	
	@GET	
	@Produces(MediaType.APPLICATION_JSON)	
	public Map<String, String>  env() {
		return System.getenv();
	}
	
	@Path("/sys")	
	@GET	
	@Produces(MediaType.APPLICATION_JSON)	
	public Properties  sys() {
		return System.getProperties();
	}	
	
	
	
	/**
	 * Initiates a feed subscription for an HTTP client
	 * @param request The Http request
	 * @param typeKey The type key of the feed
	 * @param subscriberParams A map of feed parameters
	 * @return a map of feed properties
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/startFeed/{typeKey}")
	@Consumes("application/x-www-form-urlencoded")
	public Map<String, Object> startFeed(@Context HttpServletRequest request, @PathParam("typeKey") String typeKey, MultivaluedMap<String, String> subscriberParams) {
		if(request==null) {
			throw new RuntimeException("startSubscriber call had no request");
		}		
		checkContext();
		String sessionId = sessionId(request);
		if(!SubscriberSession.isSessionCreated(sessionId)) {
			throw new IllegalStateException("A SubscriberSession has not been created for [" + sessionId + "]. Need to call /startsubscriber first", new Throwable());			
		}
		SubscriberSession subSess = SubscriberSession.getInstance(sessionId);
		Map<String, String> map = mvpToMap(subscriberParams);
		log.info("Requested Subscription for type key [" + typeKey + "] with params:\n\t" + map);
		return subSess.startSubscriptionRoute(typeKey, map);				
	}
	
	/**
	 * Stops a feed subscription for an HTTP client
	 * @param request The Http request
	 * @param typeKey The type key of the feed
	 * @param subKey The subKey of the feed
	 * @return "OK" or "ERROR"
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/stopFeed/{typeKey}/{subKey}")
	public String stopFeed(
			@Context HttpServletRequest request, 
			@PathParam("typeKey") String typeKey,
			@PathParam("subKey") String subKey) {
		if(request==null) {
			throw new RuntimeException("startSubscriber call had no request");
		}		
		checkContext();
		String sessionId = sessionId(request);
		if(!SubscriberSession.isSessionCreated(sessionId)) {
			throw new IllegalStateException("A SubscriberSession has not been created for [" + sessionId + "]. Need to call /startsubscriber first", new Throwable());			
		}
		SubscriberSession subSess = SubscriberSession.getInstance(sessionId);
		try {
			Map<String, String> params = new HashMap<String, String>(1);
			params.put(ISubscriberRoute.HEADER_SUB_FEED_KEY, subKey);
			subSess.stopSubscriptionRoute(typeKey, params);
			return "OK";
		} catch (Exception e) {
			return "ERROR:" + e;
		}
		
	}
	
	
//	/**
//	 * Polling endpoint for Ajax clients.
//	 * @param request the http request
//	 * @param response the http response
//	 * @param timeout the period of time in ms. that the request will wait for data to send if the delivery buffer is empty
//	 * @throws IOException thrown on any IO exception
//	 */
//	@GET
//	@Path("/poll/{timeout}")
//	public void poll(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("timeout") long timeout) throws IOException {
//		if(request==null) {
//			//return Response.noContent().build();
//		}		
//		checkContext();
//		String sessionId = sessionId(request);
//		if(!SubscriberSession.isSessionCreated(sessionId)) {
//			startSubscriber(request, DEFAULT_OUTPUT_FORMAT.name());
//		}
//		SubscriberSession subSess = SubscriberSession.getInstance(sessionId, DEFAULT_OUTPUT_FORMAT);
//		OutputFormat of = OutputFormat.forName(subSess.getOutputFormat());
//		SubscriptionOutputProcessor<byte[]> sop = subSess.getOutputProcessor();
//		if(sop==null) {
//			throw new IllegalStateException("The subscriber session has no output processor for OutputFormat [" + of + "]", new Throwable());
//		}
//		sop.poll(1, timeout, request, response);
//		
//	}
//	
//	
	/**
	 * @param request
	 * @param response
	 * @param timeout
	 * @param atATime
	 * @throws IOException 
	 */
	@GET
	@Path("/poll/{timeout}/{atatime}")
	public void poll(@Context HttpServletRequest request, final @Context HttpServletResponse response, @PathParam("timeout") long timeout, @PathParam("atatime") int atATime) throws IOException  {
		if(request==null) {
			throw new RuntimeException("Poll request with no request", new Throwable());
		}		
		String sessionId = sessionId(request);
		if(!SubscriberSession.isSessionCreated(sessionId)) {
			log.warn("Session ID [" + sessionId + "] issued a poll but does not have a subscriber session");
			response.setStatus(204);
			try {
				response.flushBuffer();
				return;
			} catch (IOException e) {
				log.error("Failed to flush buffer in poll op", e);
			}
		}		

		final Continuation continuation = ContinuationSupport.getContinuation(request);
		if(continuation.isExpired()) {
			response.setStatus(204);
			try {
				response.flushBuffer();
				return;
			} catch (IOException e) {
				log.error("Failed to flush buffer in poll op", e);
			}
			return;
		}
		if(continuation.isInitial() || continuation.isResumed()) {
			//log.info("Continuation was initial or resumed [" + continuation.toString() + "]");
//			if(continuation.isResumed()) {
//				log.info(String.format("[Resumed]  Response Type [%s]  Is Wrapped [%s]", response.getClass().getName(), continuation.isResponseWrapped()));
//			} else {
//				log.info(String.format("[Initial]  Response Type [%s]  Is Wrapped [%s]", response.getClass().getName(), continuation.isResponseWrapped()));
//			}
			SubscriberSession subSess = SubscriberSession.getInstance(sessionId);
			Set<byte[]> items = (Set<byte[]>)subSess.poll(atATime, timeout);
			OutputFormat outputFormat = subSess.getOutputFormat();
			if(!items.isEmpty()) {
				response.setContentType(outputFormat.getMimeType());
				response.setStatus(200);
				response.setHeader("batchcount", "" + items.size());
				OutputStream os = response.getOutputStream();
				outputFormat.writeOut(os, new OutputSizeHandler(){
					public void writeSize(int size) {
						response.setContentLength(size);
					}				
				}, items);				
			} else {
				continuation.setTimeout(timeout);
				continuation.suspend();
				subSess.registerContinuation(continuation);
				return;
			}			
		} else {
			log.warn("Continuation was not initial [" + continuation.toString() + "]");
			response.sendError(204);
		}
	}
	
//	/** The byte representation for an empty JSON response */
//	public static final byte[] EMPTY_JSON_RESPONSE = "{}".getBytes();
//
//	public static final byte[] JSON_STARTER = "[".getBytes();
//	public static final byte[] JSON_ENDER = "]".getBytes();
//	public static final byte[] JSON_DELIM = ",".getBytes();
//	public static final int JSON_SIZE = JSON_STARTER.length + JSON_ENDER.length;
//	
//	/**
//	 * Writes out a response to the session's client
//	 * @param sessionId The session id
//	 * @param outputFormat The output format to type the output stream
//	 * @param response The servlet response
//	 * @param items The marshalled items to write
//	 */
//	protected void writeResponse(String sessionId, OutputFormat outputFormat, HttpServletResponse response, byte[]...items) {
//		try {
//			boolean multi = items.length>1;
//			response.setStatus(200);
//			OutputStream os = response.getOutputStream();			
//			int size = 0;
//			for(byte[] bytes: items) {
//				size += bytes.length;
//			}
//			if(multi) {
//				size += JSON_SIZE;
//				size += (items.length-1*JSON_DELIM.length);
//			}
//			ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
//			response.setBufferSize(size);			
//			response.setHeader("Thread", Thread.currentThread().toString());
//			response.setContentType(outputFormat.getMimeType());
//			if(multi) {
//				baos.write(JSON_STARTER);
//			}
//			for(int i = 0; i < items.length; i++) {				
//				if(i > 0 && multi) {
//					baos.write(JSON_DELIM);
//				}
//				baos.write(items[i]);
//			}
//			if(multi) {
//				baos.write(JSON_ENDER);
//			}
//			baos.flush();
//			byte[] output = baos.toByteArray();
//			response.setContentLength(output.length);
//			os.write(output);
//			os.flush();
//		} catch (Exception e) {
//			log.error("Failed to write out response to session [" + sessionId + "]", e);
//			throw new RuntimeException("Failed to write out response to session [" + sessionId + "]", e);
//		}
//	}
//	
//	
	
	/**
	 * Returns an array of metricIds from the MetricId cache that match the passed expression
	 * @param name A wildcard expression to match against MetricId FQNs.
	 * @return A [possibly empty] array of matching MetricIds.
	 */
	@GET
	//@Produces({"text/xml"})
	@Produces(value=MediaType.APPLICATION_JSON)
	@Path("/metricid/{name}")
	public MetricId[] getMetricIds(@PathParam("name") String name) {
		if(name==null) {
			return MetricNameLookup.EMPTY_RESULTS;
		}
		checkContext();			
		MetricId[] ids = metricNameLookup.search(name);
		return ids;		
	}

	/**
	 * Returns the MetricTreeEntry for the passed metric tree path
	 * @param path The MetricTree path
	 * @return a MetricTreeEntry or null if one was not found for the passed path
	 */
	@GET
	@Produces(value=MediaType.APPLICATION_JSON)
	@Path("/metrictree/{path}")
	public DynaTreeMetricNode[] getMetricTree(@PathParam("path") String path) {
		if(path==null) throw new IllegalArgumentException("Passed path was null", new Throwable());
		Element element = metricTreeCache.get(path);
		if(element==null) return null;
		MetricTreeEntry me = (MetricTreeEntry)element.getValue();
		if(me==null) return null;
		//log.info("Returning DynaTreeNodes for \n" + me);
		DynaTreeMetricNode[] nodes = me.getDynaTreeNodes();
		log.info("Returning DynaTreeNodes:\n" + Arrays.toString(nodes));
		return me.getDynaTreeNodes();
	}
	
	
	/**
	 * Returns the last metric closed trace for traces matching the passed expression
	 * @param expression An expression that matches closed trace FQNs.
	 * @return A (possibly empty) collection of closed traces.
	 */
	@GET
	@Produces(value=MediaType.APPLICATION_JSON)
	@Path("/lastMetric/{expression}")	
	public Collection<ClosedTrace> searchLastTrace(@PathParam("expression") String expression) {
		if(expression==null || expression.trim().equals("")) return Collections.emptyList();
		Set<ClosedTrace> traces;		
//		String searchKey = expression.replace('.', '/').replaceAll(">$", "*").replace("%2F", "/");		
//		Results results = lastMetricCache.createQuery().addCriteria(new ILike("key", searchKey)).includeValues().execute();
		URLDecoder decoder = new URLDecoder();
		Results results = lastMetricCache.createQuery().addCriteria(RegExMatch.getInstance("key", decoder.decode(expression))).includeValues().execute();
		traces = new HashSet<ClosedTrace>(results.size());
		for(Result r: results.all()) {
			traces.add((ClosedTrace)r.getValue());
		}		
		return traces;
	}

	/**
	 * Terminates the current session
	 * @param request The Http Request
	 * @return <code>OK</code> if the session was terminated, <code>NOSESSION</code> if no session existed.
	 */
	@GET
	@Produces(value=MediaType.TEXT_PLAIN)
	@Path("/signout")
	public String signout(@Context HttpServletRequest request) {
		if(request==null) {
			throw new RuntimeException("Poll request with no request", new Throwable());
		}
		HttpSession session = request.getSession(false);
		if(session==null) {
			return "NOSESSION";
		}
		session.invalidate();
		return "OK";
	}
	
	
	/**
	 * Sets the Spring app contex
	 * @param applicationContext the applicationContext to set
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;		
	}

	/**
	 * Sets the CamelContext
	 * @param camelContext the camelContext to set
	 */
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
	}

	/**
	 * Returns the CamelContext
	 * @return the camelContext
	 */
	public CamelContext getCamelContext() {
		return camelContext;
	}
	
	/**
	 * Checks that the contexts have been injected
	 * @throws IllegalStateException
	 */
	private void checkContext() throws IllegalStateException {
		if(applicationContext==null) {
			log.error("The application context is null");
			throw new IllegalStateException("The application context is null", new Throwable());
		}
		if(camelContext==null) {
			log.error("The camel context is null");
			throw new IllegalStateException("The camel context is null", new Throwable());
		}
	}


	/**
	 * The metric name lookup service
	 * @param metricNameLookup the metricNameLookup to set
	 */
	public void setMetricNameLookup(MetricNameLookup metricNameLookup) {
		this.metricNameLookup = metricNameLookup;
	}

	@Override
	public JAXBContext getContext(Class<?> objectType) {
		for (Class<?> type : types) {
			 if (type == objectType) {
				 return context;
			 }
		}
		return null;

	}
	
	
	 

	
}
