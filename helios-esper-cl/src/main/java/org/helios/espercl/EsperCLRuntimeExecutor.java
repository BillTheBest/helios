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
package org.helios.espercl;

import static org.helios.espercl.ConsoleOps.data;
import static org.helios.espercl.ConsoleOps.err;
import static org.helios.espercl.ConsoleOps.info;
import static org.helios.espercl.ConsoleOps.notif;
import static org.helios.espercl.ConsoleOps.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.espercl.commands.Command;
import org.helios.espercl.commands.CommandMapper;
import org.helios.espercl.commands.CommandParameter;
import org.helios.espercl.options.Options;
import org.helios.espercl.scripts.ScriptManager;
import org.helios.helpers.JMXHelper;

import com.espertech.esper.client.EventBean;



/**
 * <p>Title: EsperCLRuntimeExecutor</p>
 * <p>Description: Parses and executes runtime options. </p>
 * <p>There are 3 MBeans on the server that this service will interact with:<ul>
 * <li><b>org.helios.esper:service=Engine</b>: The Helios Esper engine. It is called once to start a remote session by calling
 * <code>public ObjectName startRemote(String connectionId)</code>. The return value is the ObjectName of the created session.</li>
 * <li><b>org.helios.esper.remote:connectionid=&lt;connection-id&gt;</b>: This is the remote session MBean where <b><code>&lt;connection-id&gt;</code></b> is the JMX connection id that unqiuely identifies
 * the session created for this client. This session is communicated with by creating an <code>MBeanServerInvocationHandler</code> with this ObjectName (<code>sessionObjectName</code>)
 * and the interface <code>EsperSession</code></code>.</li>
 * <li><b>org.helios.engine.metrics:metrictype=Statement,name=&lt;statement-name&gt;,type=&lt;statement-type&gt;</b>: This is the object name of the Esper Statement metrics MBean. The <b><code>&lt;statement-name&gt;</code></b>
 * property is the statement name and the <b><code>&lt;statement-type&gt;</code></b> key is the statement type. These values may not be known ahead of time so the <b><code>EsperSession</code></b> can be called to
 * acquire the ObjectName of the currently executing subscription statement. This MBean is transient and will be unregistered when the subscription statement is destroyed, so a new ObjectName and invocation handler 
 * must be acquired for each new subscription statement to be watched.</li>
 *  </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.EsperCLRuntimeExecutor</code></p>
 */

public class EsperCLRuntimeExecutor implements NotificationListener, StatementListener {
	/** the command line command mapper */
	protected final CommandMapper commandMapper = new CommandMapper();	
	/** the runtime command reader */
	protected final RuntimeCommandReader commandReader = new RuntimeCommandReader(this);
	/** The connectionId of the MBeanServerConnection to the Esper Server */
	protected String connectionId;
	/** The JMXServiceURL for the MBeanServerConnection to the Esper Server */
	protected final JMXServiceURL serviceUrl;
	/** the JMXConnector to the Esper Server */
	protected JMXConnector connector = null;
	/** flag to indicate the client is quiting and suppres error logging */
	protected AtomicBoolean quitting = new AtomicBoolean(false);
	//==============================================================================
	//	ObjectName for esper engine
	//  No proxy since we only call one op
	//==============================================================================
	/** The Esper Engine MBean ObjectName */
	public static final ObjectName ESPER_ENGINE = JMXHelper.objectName("org.helios.esper:service=Engine");
	//==============================================================================
	//	ObjectName for remote session
	//==============================================================================
	/** this client's remote session ObjectName */
	protected ObjectName sessionObjectName = null;
	/** a remote session proxy */
	protected EsperSession sessionProxy = null;
	
	
	
	
	/** the MBeanServerConnection to the Esper Server */
	protected MBeanServerConnection connection = null;
	
	/** The ObjectName format for the remote session MBean */
	public static final String SESSION_OBJECT_NAME = "org.helios.esper.remote:connectionid={0}";

	/** The command line application command prompt */
	public static final String PROMPT = "\nhelios>";
	
	protected void cleanupStatement() {
		
	}
	
	
	//================================================================
	//	Runtime command handlers
	//================================================================
	
	/**
	 * Stops all activity and exits. 
	 */
	@Command(name="quit", aliases={"q","exit"}, description="Stops all client activity and exits")
	protected void quit() {
		quitting.set(true);
		try { sessionProxy.destroySession(); } catch (Exception e) {}		
		commandReader.stopReader();
		try { connector.removeConnectionNotificationListener(this); } catch (Exception e) {}
		try { connector.close(); } catch (Exception e) {}
		info("Disconnected from [", serviceUrl , "]\n\tBye !");
		connector = null;		
		connection = null;
		sessionObjectName = null;		
		sessionProxy = null;
		System.exit(-1);
	}
	
	/**
	 * Creates an EPL statement and subscribes to it.
	 * @param epl the epl query
	 * @param name the optional statement name
	 */
	@Command(name="epl", aliases={"sub", "subscribe"}, description="Executes the passed EPL as Esper query subscription", params={
			@CommandParameter(optional=false, description="The epl to execute and subscribe to"),
			@CommandParameter(optional=true, description="The name of the statement")
	})	
	protected void subscribe(String epl, String name) {
		try {
			if(name==null) {
				sessionProxy.subscribe(epl);
			} else {
				sessionProxy.subscribe(epl, name);
			}
		} catch (Exception e) {
			err("Failed to execute epl [" , epl , "] ", e);
		}
	}
	
	
	
	
	/**
	 * Issues an immediate on demmand EPL query
	 * @param epl the epl query
	 */
	@Command(name="iepl", aliases={"imm", "immediate"},  description="Executes the passed EPL as an immediate Esper query", params={
			@CommandParameter(optional=false, description="The immediate epl to execute")
	})
	protected void immediate(String epl) {
		try {
			data(sessionProxy.query(epl));
		} catch (Exception e) {
			Throwable cause = e.getCause();
			
			err("Failed to execute immediate epl [" , epl , "] " + cause);
		}
	}
	
	/**
	 * Stops the current subscription 
	 */
	@Command(name="stop", aliases={}, description="Stop the currently executing EPL subscription")
	protected void stop() {		
		if(sessionProxy.isStatementRegistered()) {
			sessionProxy.stopSubscription();
		} else {
			notif("No active subscription");
		}		
	}
	
	/**
	 * Destroys the current subscription 
	 */
	@Command(name="destroy", aliases={}, description="Stop the currently executing EPL subscription")
	protected void destroy() {		
		if(sessionProxy.isStatementRegistered()) {
			try { sessionProxy.stopSubscription(); } catch (Exception e) {}
			sessionProxy.destroySubscription();
		} else {
			notif("No active subscription");
		}		
	}
	
	
	/**
	 * [Re]Starts the current subscription 
	 */
	@Command(name="start", aliases={}, description="[Re]Starts the currently stopped EPL subscription")
	protected void start() {		
		if(sessionProxy.isStatementRegistered()) {
			sessionProxy.startSubscription();
		} else {
			notif("No active subscription");
		}		
	}
	
	
	protected MBeanServerConnection getConnection() {
		return connection;
	}
	
	/**
	 * Creates a new EsperCLRuntimeExecutor and connects to the designated JMX URL
	 * @param serviceUrl The JMX Service URL
	 */
	public EsperCLRuntimeExecutor(JMXServiceURL serviceUrl) {
		this.serviceUrl = serviceUrl;
		try {			
			connector = JMXConnectorFactory.connect(serviceUrl);
			connectionId = connector.getConnectionId();
			connection = connector.getMBeanServerConnection();			
			info("Connected to JMXServer [", serviceUrl, "]");
			info("Connection ID:[", connectionId, "]");
			sessionObjectName = (ObjectName) connection.invoke(ESPER_ENGINE, "startRemote", new Object[]{connectionId}, new String[]{String.class.getName()});
			info("Session ObjectName:[", sessionObjectName, "]");
			connector.addConnectionNotificationListener(this, null, null);
			//connection.addNotificationListener(sessionObjectName, this, null, null);
			sessionProxy = new EsperSessionImpl(connection, sessionObjectName, this);
				//MBeanServerInvocationHandler.newProxyInstance(connection, sessionObjectName, EsperSession.class, true);
			commandMapper.mapCommands(this);
			commandMapper.mapCommands(Options.getInstance());
			commandMapper.mapCommands(ScriptManager.getInstance());
			// sessionObjectName
			commandReader.start();
			
			Runtime.getRuntime().addShutdownHook(new Thread(){
				public void run() {
					if(getConnection()!=null) {
						quit();
					}
				}
			});
			prompt(PROMPT);
		} catch (Exception e) {
			err("Failed to connect to [", serviceUrl, "].", e);
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
	
	
	synchronized void executeCommands(String command, String[] args) {
		try {
			if(command!=null) {
				commandMapper.invoke(command, args);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			
		} finally {
			prompt(PROMPT);
		}
	}
	
	//[sub, select, *, from, LastMetricWindow]
	protected String[] preparse(String[] args) {
		List<String> newArgs = new ArrayList<String>();
		if(args!=null && args.length > 0) {
			newArgs.add(args[0]);
		}
		StringBuilder b = new StringBuilder();
		for(int i = 1; i < args.length; i++) {
			b.append(args[i]).append(" ");
		}
		newArgs.add(b.toString().trim());
		return newArgs.toArray(new String[newArgs.size()]);
	}
	
	


	/**
	 * Handles a notification from the JMX connection
	 * @param notification
	 * @param handback
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		//outln("handling notification [\n", notification, "\n]");
		if("jmx.remote.connection.closed".equals(notification.getType())) {
			this.connection = null;
			if(!quitting.get()) err("Connection dropped by server. Exiting .....");
			System.exit(-1);
		} else {
			notif("Received unhandled notification [", notification, "]");
		}
	}


	/**
	 * Called when a statement starts or restarts
	 * @param statementName The name of the statement
	 */
	public void onStart(String statementName) {
		
	}
	/**
	 * Called when a statement stops
	 * @param statementName The name of the statement
	 */
	public void onStop(String statementName) {
		
	}
	/**
	 * Called when a statement is destroyed
	 * @param statementName The name of the statement
	 */	
	public void onDestroy(String statementName) {
		
	}
	
	/**
	 * Called when a statement is created
	 * @param statementName The name of the statement
	 */
	public void onCreate(String statementName) {
		
	}
	
	/**
	 * Called when a statement reports a failure state
	 * @param statementName The name of the statement
	 */	
	public void onFail(String statementName) {
		
	}
	/**
	 * Called when the statement returns events entering the event stream
	 * @param statementName The name of the statement
	 * @param events An array of events entering the event stream
	 */
	public void onInEvents(String statementName, Object...events) {
		//ConsoleOps.notif("Received [", (events==null ? 0 : events.length) , "]messages from [", statementName, "]");
		if(events!=null) {
			if(ScriptManager.getInstance().isScriptLoaded()) {
				ScriptManager.getInstance().exec("events", events);
			} else {
				for(Object event: events) {
					ConsoleOps.data(event);
				}
			}
		}
		
	}
	/**
	 * Called when the statement returns events leaving the event stream
	 * @param statementName The name of the statement
	 * @param events An array of events leaving the event stream
	 */
	public void onOutEvents(String statementName, EventBean...events) {
		
	}


	/**
	 * Returns the command reader
	 * @return the commandReader
	 */
	public RuntimeCommandReader getCommandReader() {
		return commandReader;
	}
}
