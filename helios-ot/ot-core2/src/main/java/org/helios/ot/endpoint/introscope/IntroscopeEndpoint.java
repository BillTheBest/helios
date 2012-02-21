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
package org.helios.ot.endpoint.introscope;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.helios.aop.DynaClassFactory;
import org.helios.cache.softref.SoftReferenceCache;
import org.helios.cache.softref.SoftReferenceCacheService;
import org.helios.helpers.URLHelper;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.ot.endpoint.AbstractEndpoint;
import org.helios.ot.endpoint.EndpointConfigException;
import org.helios.ot.endpoint.EndpointConnectException;
import org.helios.ot.endpoint.EndpointTraceException;
import org.helios.ot.trace.Trace;
import org.helios.ot.trace.types.ITraceValue;
import org.helios.ot.tracer.ITracer;
import org.helios.ot.tracer.TracerManager3;
import org.helios.ot.tracer.disruptor.TraceCollection;
import org.helios.ot.trace.MetricId;
import org.helios.ot.type.MetricType;
import org.helios.helpers.Banner;

/**
 * <p>Title: IntroscopeEndpoint</p>
 * <p>Description: A concrete endpoint implementation for Introscope. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 * <p><code>org.helios.ot.endpoint.introscope.IntroscopeEndpoint</code></p> 
 */
@JMXManagedObject (declared=false, annotated=true)
public class IntroscopeEndpoint<T extends Trace<? extends ITraceValue>> extends AbstractEndpoint<T> {

	private static final long serialVersionUID = 8843324552731685413L;
	/** The introscope agent name */
	protected String agentName = null;
	/** Default agent name in case either agent name or agent profile is not specified */
	protected static final String DEFAULT_AGENT_NAME = "HeliosAgent";
	/** The introscope process name */
	protected String processName = null;
	/** Default process name in case either process name or agent profile is not specified */
	protected static final String DEFAULT_PROCESS_NAME = "HeliosProcess";
	/** The introscope agent's jar location */
	protected String agentJarLocation = null;
	/** The introscope profile */
	protected String profileName = null;
	/** Introscope Enterprise Manager's IP address */
	protected String emHost = null;
	/** Introscope Enterprise Manager's port.  Default is 5001 */
	protected String emPort = null;
	/** The manifest entry key that identifies a jar as the introscope agent */
	protected static final String WILY_MANIFEST_KEY = "com-wily-Name";
	/** The manifest entry value that identifies a jar as the introscope agent */
	protected static final String WILY_MANIFEST_VALUE = "Introscope Agent";
	/** The Java Agent Signature Pattern */
	protected static final Pattern JAVA_AGENT_PATTERN = Pattern.compile("-javaagent\\:(.*?\\.jar).*", Pattern.CASE_INSENSITIVE);
	/** The Introscope defined system property for the agent name */
	protected static final String AGENT_NAME = "introscope.agent.agentName";
	/** The Introscope defined system property for the process name */
	protected static final String PROCESS_NAME = "introscope.agent.defaultProcessName";
	/** The Introscope defined system property for the agent's profile */
	protected static final String PROFILE_NAME = "introscope.agent.com.wily.introscope.agentProfile";
	/** The Helios defined system property for the agent jat */
	protected static final String AGENT_JAR = "introscope.agent.jar";
	/** The agent's profile file */
	protected static final String AGENT_PROFILE = "com.wily.introscope.agentProfile";
	/** The Introscope defined system property for EM Host */
	private static final String EM_HOST = "introscope.agent.enterprisemanager.transport.tcp.host.DEFAULT";
	/** The Introscope defined system property for EM Port */
	private static final String EM_PORT = "introscope.agent.enterprisemanager.transport.tcp.port.DEFAULT";	
	/** A flag to indicate whether the endpoint is based on Agent's profile file or directly 
	 * through EM Host/Port configuration.  This flag will not be used if Introscope Agent settings are specified 
	 * through VM arguments. */
	protected boolean profileBased = false;
	/** The introscope agent adapter instance */
	protected final AtomicReference<IntroscopeAdapter> adapter = new AtomicReference<IntroscopeAdapter>(null);
	/** SoftRef Cache for mapping helios metricIds to created Introscope metric names */
	protected static final SoftReferenceCache<String, String> metricCache =  SoftReferenceCacheService.getInstance().createCache();	
	/** The IntroscopeAgent classloader */
	protected ClassLoader agentLoader;	
	/** The Introscope Agent's reported host name */
	protected static final AtomicReference<String> host = new AtomicReference<String>(null);
	/** The Introscope Agent's reported agent name */
	protected static final AtomicReference<String> agent = new AtomicReference<String>(null);
	/** The configured builder */
	protected Builder builder = null;
	
	/**
	 * Constructor to validate configuration specified and load Introscope Agent
	 * 
	 * @param builder
	 * @throws EndpointConfigException
	 */
	public IntroscopeEndpoint(Builder builder) throws EndpointConfigException{
		super(builder);
		this.builder = builder;
		processParameters(builder);
		loadAgent();
	}
	
	/**
	 * This method validates parameters and determine which mode IntorscopeEndpoint will run.  Here are the three modes:
	 * 1. If it finds agent jar loaded through VM arguments already, it uses that mode.  
	 * 2. If not, it checks for agent jar and profile location combination.  
	 * 3. If that combination does not exist, it checks for agent jar and EM Host/Port combination.  
	 * 
	 * If none of those modes can be turned on, EndpointConfigException is thrown and endpoint is not activated.    
	 * 
	 * @param builder
	 * @throws EndpointConfigException
	 */
	public void processParameters(Builder builder) throws EndpointConfigException{
		String javaAgentJar = getJavaAgentJar();
		agentJarLocation = builder.getAgentJar();
		agentName = builder.getAgentName();
		processName = builder.getProcessName();
		profileName = builder.getProfile();
		emHost = builder.getEmHost();
		emPort = builder.getEmPort() == null ? "5001" : builder.getEmPort();
		
		if(javaAgentJar!=null){ //Introscope Agent is already initialized through VM arguments
			agentJarLocation = javaAgentJar;
			log.info(Banner.banner("*** Agent is already initialized through VM arguments during startup...using that confirguration instead."));
		}else{ 
			if(agentJarLocation==null){
				throw new EndpointConfigException("Introscope Endpoint will not activate as agent jar location is missing.");
			}
			System.setProperty(AGENT_JAR, agentJarLocation);
			if(profileName==null){
				if(emHost==null){
					throw new EndpointConfigException("Introscope Endpoint will not activate as neither agent profile location nor EM Host information was specified.");
				}
				else{
					profileBased = false;
					System.setProperty(EM_HOST,emHost);
					System.setProperty(EM_PORT,emPort);
				}
			}else{
				profileBased = true;
				System.setProperty(AGENT_PROFILE, profileName);
			}
		}		
		
		if(!profileBased && (javaAgentJar==null)){ 
			// We need default Agent and Process Name in this case as neither VM arguments nor profile based mode can be enabled
			System.setProperty(AGENT_NAME, (agentName!=null?agentName:DEFAULT_AGENT_NAME));
			if(System.getProperty(MetricId.APPLICATION_ID)==null)
				System.setProperty(MetricId.APPLICATION_ID, (agentName!=null?agentName:DEFAULT_AGENT_NAME));
			System.setProperty(PROCESS_NAME, (processName!=null?processName:DEFAULT_PROCESS_NAME));					
		}else{ // Override existing only if the new ones are provided through the configuration
			if(agentName!=null){
				System.setProperty(AGENT_NAME, agentName);
				System.setProperty(MetricId.APPLICATION_ID, agentName);
			}
			if(processName!=null)
				System.setProperty(PROCESS_NAME, processName);
		}
		log.info(Banner.banner(this.toString()));		
	}

	/**
	 * Attempts to locate the Introscope Agent Jar from the JVM's input arguments.
	 * @return the jar file name or null if it was not found.
	 */
	public static String getJavaAgentJar() {
		for(String s: ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			s = s.trim();
			Matcher m = JAVA_AGENT_PATTERN.matcher(s);
			if(m.matches()) {
				String jar = m.group(1);
				if(isWilyJar(jar))
					return jar;
			}
		}
		return null;
	}
	
	/**
	 * Determines if the passed string represents the Introscope Agent Jar
	 * @param jarName The file name
	 * @return true if the name is the agent jar
	 */
	public static boolean isWilyJar(String jarName) {
		JarFile jarFile = null;
		if(jarName==null) return false;
		try {
			jarFile = new JarFile(jarName);
			Manifest manifest = jarFile.getManifest();
			if(manifest==null) return false;
			Attributes attrs = manifest.getMainAttributes();
			if(attrs==null) return false;
			String value = attrs.getValue(WILY_MANIFEST_KEY);
			if(value==null) return false;
			return (value.trim().equals(WILY_MANIFEST_VALUE));
		} catch (Exception e) {
			return false;
		} finally {
			try { jarFile.close(); } catch (Exception e) {}
		}
	}	
	
	public Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Returns the builder 
	 * @return Builder
	 */
	public static Builder getBuilder() {
		return new Builder();
	}
	
	/**
	 * This method tries to connect to Introscope agent with a 30 seconds timeout window.  An EndpointConnectException
	 * if thrown if that timeout period expires before the connection is established.
	 * 
	 * @throws EndpointConnectException
	 */
	protected void connectImpl() throws EndpointConnectException {
		if(adapter.get()==null){
			loadAgent();
		}
		else{
			try{
				log.debug("Waiting to connect to Introscope EM with 30 seconds timeout period...");
				if(adapter.get().connectWithWait(30, TimeUnit.SECONDS)) {
					log.debug("Triplet:" + Arrays.toString(adapter.get().getHostProcessAgent()));
					host.set(adapter.get().getHost());
					agent.set(adapter.get().getName());
				} else {
					Banner.bannerErr("Unable to connect to Introscope EM within 30 seconds ");
					throw new EndpointConnectException("Unable to connect to Introscope EM within 30 seconds ");
				}
			}catch (Exception e) {
				throw new EndpointConnectException("An error occured while connecting to Introscope EM", e);
			}
		}
	}

	/**
	 * It loads agent's classes in the new classloader and dynamically creates an instance 
	 * of IntroscopeAdapter class. 
	 */
	private void loadAgent() {
		ClassLoader[] cls = null;		
		if(isAgentAccessible()) {
			cls = new ClassLoader[]{};
			agentLoader = Thread.currentThread().getContextClassLoader();
		} else {
			agentLoader = getAgentClassLoader(agentJarLocation);
			if(agentLoader==null) {
				log.warn("The Introscope Agent is not in the class path and could not be located from the config [" + agentJarLocation + "]. The IntroscopeTracingBridge is inactive");
				return;
			}
			cls = new ClassLoader[]{agentLoader};
		}
		ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(agentLoader);
		try {
			adapter.set((IntroscopeAdapter)DynaClassFactory.generateClassInstance(
					IntroscopeTracerAdapter.class.getPackage().getName() + ".TracerInstance", 
					IntroscopeTracerAdapter.class, cls));
		} finally {
			Thread.currentThread().setContextClassLoader(currentCl);
		}
		log.info(Banner.banner("Created and loaded IntroscopeAdapter [" + adapter.get() + "] successfully"));
	}
	
	/**
	 * Disconnects from Introscope agent
	 */
	protected void disconnectImpl() {
		if(adapter.get()!=null) {
			adapter.get().disconnect();
			log.info(Banner.banner("Disconneted from Introscope agent successfully"));
		}else{
			log.warn("Ignoring request to disconnet as it wasn't connected to Introscope agent");
		}
	}	
	
	/**
	 * Transforms the Helios Tracing API metric name arguments into an Introscope metric name.
	 * @param metricName The metric name.
	 * @param prefix The full metric name prefix.
	 * @param nameSpace The full metric name suffix.
	 * @return An introscope metric name.
	 */
	public static String getIntroscopeMetric(MetricId metricId) {
		String metricName = metricCache.get(metricId.getFQN());
		if(metricName==null) {
			synchronized(metricCache) {
				metricName = metricCache.get(metricId.getFQN());
				if(metricName==null) {
					StringBuilder buff = new StringBuilder();
					if(!isMetricLocal(metricId)) {
						buff.append(metricId.getHostName()).append("|").append(metricId.getAgentName()).append("|");
					}
					if(metricId.getNamespace()!=null) {						
						for(String s: metricId.getNamespace()) {
							if(s!=null && s.length() > 0) {
								buff.append(clean(s)).append("|");
							}
						}
					}
					if(buff.length() > 0) {
						buff.deleteCharAt(buff.length()-1).append(":");
					}
					buff.append(clean(metricId.getMetricName()));
					metricName = buff.toString();
					metricCache.put(metricId.getFQN(), metricName);
				}
			}		
		}
		return metricName;
	}	
	
	/** Introscope Pipe ('|') pattern  */
	public static final Pattern ISCOPE_PIPE = Pattern.compile("\\|");
	/** Introscope Colon (':') pattern  */
	public static final Pattern ISCOPE_COLON = Pattern.compile(":");
	
	
	public static String clean(CharSequence m) {
		if(m==null) return null;
		String s = ISCOPE_PIPE.matcher(m.toString()).replaceAll("(:)");
		s = ISCOPE_COLON.matcher(s).replaceAll(";");
		return s;
	}
	
	/**
	 * Determines if the passed metricId has the same host/agent name as the activated Introscope agent.
	 * @param metricId the metricId to test
	 * @return true if they match
	 */
	public static boolean isMetricLocal(MetricId metricId) {
		if(metricId==null) throw new IllegalArgumentException("Passed metricId was null", new Throwable());
		return metricId.getHostName().equals(host.get()) && metricId.getAgentName().equals(agent.get());
	}	
	
	/**
	 * Determines if the Introscope agent is accessible in the current classpath
	 * @return true if it is, false otherwise
	 */
	public static boolean isAgentAccessible() {
		try {
			Class.forName("com.wily.introscope.agent.IAgent");
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Creates a classloader for the agent at the passed location
	 * @param agentLocation The file name of the agent
	 * @return An Introscope agent jar classloader or null if it could not be created
	 */
	protected ClassLoader getAgentClassLoader(String agentLocation) {
		if(agentLocation==null) return null;
		URL agentUrl = null;
		File agentFile = null;
		if(URLHelper.isValidURL(agentLocation)) {
			try { agentFile =new File(new URL(agentLocation).getFile()); } catch (Exception e) { 
				log.warn("Unexpected error creating URL/File from [" + agentLocation + "]", e);
				return null;
			} 
		} else {
			agentFile = new File(agentLocation);
		}
		if(agentFile.exists() && isWilyJar(agentFile.getAbsolutePath())) {
			try { 
				agentUrl = agentFile.toURI().toURL();
				return new URLClassLoader(new URL[]{agentUrl}, Thread.currentThread().getContextClassLoader());
			} catch (Exception e) {
				log.warn("Unexpected error creating URL/File from [" + agentLocation + "]", e);
				return null;
			}
		} else {
			log.warn("Failed to resolve Introscope Agent JAR from [" + agentLocation + "]", new Throwable());
			return null;
		}
	}
		
	
	@Override
	protected boolean processTracesImpl(TraceCollection traceCollection)
			throws EndpointConnectException, EndpointTraceException {
		IntroscopeAdapter agent = adapter.get();
		if(agent==null) return false;
		Set<Trace<? extends ITraceValue>> traces = traceCollection.getTraces();
		if(traces!=null && !traces.isEmpty()) {		
			for(Trace trace: traces) {
				String iMetricName = getIntroscopeMetric(trace.getMetricId());
				MetricType type = trace.getMetricType();
				int typeCode = type.getCode();
				String value = trace.getValue().toString();
					if( typeCode == MetricType.TYPE_INT_AVG)
						agent.recordDataPoint(iMetricName, Double.valueOf(value).intValue());
					else if( typeCode == MetricType.TYPE_LONG_AVG)
						agent.recordDataPoint(iMetricName, Double.valueOf(value).longValue());
					else if( typeCode == MetricType.TYPE_STICKY_INT_AVG)
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).intValue());
					else if( typeCode == MetricType.TYPE_STICKY_LONG_AVG )
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).longValue());
					else if( typeCode == MetricType.TYPE_DELTA_INT_AVG )
						agent.recordDataPoint(iMetricName, Double.valueOf(value).intValue());
					else if( typeCode == MetricType.TYPE_DELTA_LONG_AVG)
						agent.recordDataPoint(iMetricName, Double.valueOf(value).longValue());
					else if( typeCode == MetricType.TYPE_STICKY_DELTA_INT_AVG)
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).intValue());
					else if( typeCode == MetricType.TYPE_STICKY_DELTA_LONG_AVG)
						agent.recordCurrentValue(iMetricName, Double.valueOf(value).longValue());
					else if( typeCode == MetricType.TYPE_INTERVAL_INCIDENT)
						agent.recordIntervalIncident(iMetricName, Double.valueOf(value).intValue());	
					else if( typeCode == MetricType.TYPE_STRING)
						agent.recordDataPoint(iMetricName, value);
					else if( typeCode == MetricType.TYPE_TIMESTAMP)
						agent.recordTimeStamp(iMetricName, Double.valueOf(value).longValue());
					else
						if(log.isTraceEnabled()) log.trace("Introscope Tracer Error: Metric Type Code [" + type + "] not recognized for metric:" + iMetricName);
			}
			return true;
		}else{
			return false;
		}
	}	

	/**
	 * @return the agentName
	 */
	public String getAgentName() {
		return agentName;
	}

	/**
	 * @param agentName the agentName to set
	 */
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	/**
	 * @return the processName
	 */
	public String getProcessName() {
		return processName;
	}

	/**
	 * @param processName the processName to set
	 */
	public void setProcessName(String processName) {
		this.processName = processName;
	}

	/**
	 * @return the agentJar
	 */
	public String getAgentJarLocation() {
		return agentJarLocation;
	}

	/**
	 * @param agentJar the agentJar to set
	 */
	public void setAgentJarLocation(String agentJarLocation) {
		this.agentJarLocation = agentJarLocation;
	}

	/**
	 * @return the emHost
	 */
	public String getEmHost() {
		return emHost;
	}

	/**
	 * @param emHost the emHost to set
	 */
	public void setEmHost(String emHost) {
		this.emHost = emHost;
	}

	/**
	 * @return the emPort
	 */
	public String getEmPort() {
		return emPort;
	}

	/**
	 * @param emPort the emPort to set
	 */
	public void setEmPort(String emPort) {
		this.emPort = emPort;
	}	
	


	public String toString() {
		return "IntroscopeEndpoint [agentName=" + agentName + ", processName="
				+ processName + ", agentJarLocation=" + agentJarLocation
				+ ", profileName=" + profileName + ", emHost=" + emHost
				+ ", emPort=" + emPort + "]";
	}

	/**
	 * Only for testing purposes
	 * 
	 * @param args
	 * @throws EndpointConnectException
	 * @throws EndpointConfigException
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		try{
			IntroscopeEndpoint endpoint = (IntroscopeEndpoint)IntroscopeEndpoint.getBuilder()
										  //.agentJar("C:/temp/wily8/Agent.jar")
										  //.profile("C:/temp/wily8/IntroscopeAgent.profile")
										  .emHost("10.19.47.119")
										  .emPort("5001")
										  .build();
	
			TracerManager3 traceManager = TracerManager3.getInstance(TracerManager3.Configuration.getDefaultConfiguration()
											  .appendEndPoint(endpoint)
			);
		
			endpoint.connect();
			
			/** Submit sample metrices**/
			Random random = new Random(System.nanoTime());
			ITracer tracer = traceManager.getTracer();
			int i=0;
			while(i<=20) {
				i++;
	
				ArrayList<Trace> tracesToProcess = new ArrayList<Trace>();
				tracesToProcess.add(tracer.smartTrace(MetricType.INT_AVG.ordinal(), "" + random.nextInt(), "INT_AVG", "Tracers"));
				tracesToProcess.add(tracer.smartTrace(MetricType.LONG_AVG.ordinal(), "" + random.nextLong(), "LONG_AVG", "Tracers"));
				tracesToProcess.add(tracer.smartTrace(MetricType.STRING.ordinal(), ""+System.getProperty("os.name"), "String", "Tracers"));
				if(i==1) // Trace Sticky int only once so we can verify the behavior in Intoscope Investigator
					tracesToProcess.add(tracer.smartTrace(MetricType.STICKY_INT_AVG.ordinal(), "10" , "STICKY_INT", "Tracers"));
				
				tracesToProcess.add(tracer.smartTrace(MetricType.TIMESTAMP.ordinal(), ""+System.currentTimeMillis(), "Timestamp", "Tracers"));
				TraceCollection traceCollection = traceManager.getNextTraceCollectionSlot();
				traceCollection.load(tracesToProcess);
				traceCollection.submit();
				try { Thread.sleep(3000); } catch (Exception e) {}
			}
			
			endpoint.disconnect();
		}catch(EndpointConfigException e){
			Banner.bannerErr(e.getMessage());
			System.exit(-1);
		}
		System.exit(0);
	}
	
	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description: Builder implementation for IntroscopeEndpoint. </p> 
	 * <p>Company: Helios Development Group</p>
	 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
	 */
	public static class Builder extends AbstractEndpoint.Builder {
		private String agentName = null;
		private String processName = null;
		private String profile = null;
		private String agentJar = null;
		private String emHost = null;
		private String emPort = null;
		
		public IntroscopeEndpoint build() throws EndpointConfigException {
			IntroscopeEndpoint iEndpoint = new IntroscopeEndpoint(this);
			return iEndpoint;
		}	
		
		/**
		 * @return the agentName
		 */
		public String getAgentName() {
			return agentName;
		}
		/**
		 * @param agentName the agentName to set
		 */
		public void setAgentName(String agentName) {
			this.agentName = agentName;
		}
		
		/**
		 * Sets Agent Name
		 * @param agentName
		 * @return this builder
		 */
		public Builder agentName(String agentName) {
			this.agentName = agentName;
			return this;
		}
		
		/**
		 * @return the processName
		 */
		public String getProcessName() {
			return processName;
		}
		/**
		 * @param processName the processName to set
		 */
		public void setProcessName(String processName) {
			this.processName = processName;
		}
		
		/**
		 * Sets process name
		 * @param processName
		 * @return this builder
		 */
		public Builder processName(String processName) {
			this.processName = processName;
			return this;
		}
		
		/**
		 * @return the agentJar
		 */
		public String getAgentJar() {
			return agentJar;
		}
		/**
		 * @param agentJar the agentJar to set
		 */
		public void setAgentJar(String agentJar) {
			this.agentJar = agentJar;
		}
		
		/**
		 * Sets location for Introscope Agent jar
		 * @param agentJar
		 * @return this builder
		 */
		public Builder agentJar(String agentJar) {
			this.agentJar = agentJar;
			return this;
		}

		/**
		 * @return the profile
		 */
		public String getProfile() {
			return profile;
		}

		/**
		 * @param profile the profile to set
		 */
		public void setProfile(String profile) {
			this.profile = profile;
		}
		
		public Builder profile(String profile) {
			this.profile = profile;
			return this;
		}

		/**
		 * @return the emHost
		 */
		public String getEmHost() {
			return emHost;
		}

		/**
		 * @param emHost the emHost to set
		 */
		public void setEmHost(String emHost) {
			this.emHost = emHost;
		}
		
		public Builder emHost(String emHost) {
			this.emHost = emHost;
			return this;
		}		

		/**
		 * @return the emPort
		 */
		public String getEmPort() {
			return emPort;
		}

		/**
		 * @param emPort the emPort to set
		 */
		public void setEmPort(String emPort) {
			this.emPort = emPort;
		}
		
		public Builder emPort(String emPort) {
			this.emPort = emPort;
			return this;
		}		
	}
	
//	-javaagent:C:\\temp\\wily8\\Agent.jar 
//	-Dcom.wily.introscope.agentProfile=C:\\temp\\wily8\\IntroscopeAgent.profile 
//	-Dorg.helios.application.name=HAgent
	
}
