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
package org.helios.espercl.options;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.espercl.ConsoleOps;
import org.helios.espercl.commands.Command;
import org.helios.espercl.commands.CommandParameter;
import org.helios.espercl.scripts.ScriptManager;
import org.helios.helpers.StringHelper;
import org.helios.options.BooleanOption;
import org.helios.options.IOption;
import org.helios.options.LongOption;
import org.helios.options.Option;
import org.helios.options.StringOption;

import static org.helios.helpers.ClassHelper.nvl;


/**
 * <p>Title: Options</p>
 * <p>Description: A class to define, manage and serialize the user options. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.options.Options</code></p>
 */
public class Options {
	/** Singleton instance */
	private static volatile Options instance = null;
	/** Singleton instance lock */
	private static final Object lock = new Object();
	
	/** Indicates if the console is Ansi escape enabled */
	public static final String ANSICONSOLE = "ansiconsole";

	/** Indicates if the console is asynch enabled */
	public static final String ASYNCHCONSOLE = "asynchconsole";

	/** The default client prompt */
	public static final String CLIENTPROMPT = "clientprompt";

	/** The client timeout in ms. waiting for the first response */
	public static final String INITIALTIMEOUT = "initialtimeout";

	/** The default JMX Service URL to connect to */
	public static final String JMXURL = "jmxurl";

	/** The maximum number of events that can be retrieved for a single batch */
	public static final String MAXBATCHEVENTS = "maxbatchevents";

	/** The maximum total number of events that can be retrieved for a statement */
	public static final String MAXEVENTS = "maxevents";

	/** The client timeout in ms. waiting for the next response */
	public static final String RUNNINGTIMEOUT = "timeout";

	/** The default directory where cl dynamic scripts are read from */
	public static final String SCRIPTDIR = "scriptdir";
	
	/**
	 * Creates a new Options 
	 */
	private Options() {
		
	}
	
	/**
	 * Acquires the Options singleton instance
	 * @return the Options singleton instance
	 */
	public static Options getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Options();
				}
			}
		}
		return instance;
	}
	
	
	/** A map of the loaded options keyed by option name */
	private final Map<String, IOption<?>> options = new ConcurrentHashMap<String, IOption<?>>() {
		private static final long serialVersionUID = 8351734997126859035L;
		{
			for (Class<?> clazz : Options.class.getDeclaredClasses()) {
				if (IOption.class.isAssignableFrom(clazz)) {
					try {
						IOption<?> opt = (IOption<?>) clazz.newInstance();						
						put(opt.getName(), opt);
						StringBuilder b = new StringBuilder("\n\t/** ");
						b.append(opt.getDescription()).append(" */");
						b.append("\n\tpublic static final String ").append(opt.getClass().getSimpleName().toUpperCase());
						b.append(" = \"").append(opt.getName()).append("\";");
						//System.out.println(b);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
		}
	};
	
	/**
	 * Returns the current value of the named option
	 * @param name The name of the option to retrieve
	 * @return the value of the named option
	 */
	@Command(name="viewopt", aliases={"vo"},  description="Displays the named option", params={
			@CommandParameter(optional=true, description="The option name to view or view all options if not passed")
	})
	public <T> T get(String name) {
		if(name==null) {
			for(IOption opt: options.values()) {
				ConsoleOps.info(opt.display());
			}
			return null;
		}
		IOption<T> opt = (IOption<T>) options.get(nvl(name, "Passed option name was null"));
		if(opt==null) {
			throw new RuntimeException("There is no option named [" + name + "]");
		}
		ConsoleOps.info(opt.display());
		return opt.getValue();
	}
	
	/**
	 * Sets the value of the named option
	 * @param name The name of the option to set the value for
	 * @param value The value to set the option to
	 */
	@Command(name="setopt", aliases={"so"},  description="Sets the value of the named option", params={
			@CommandParameter(optional=false, description="The option name to set"),
			@CommandParameter(optional=false, description="The value of the option", ignorePe=true)
	})	
	public <T> void set(String name, T value) {
		IOption<T> opt = (IOption<T>) options.get(nvl(name, "Passed option name was null"));
		if(opt==null) {
			throw new RuntimeException("There is no option named [" + name + "]");
		}
		opt.setValue(value);
	}
	
	/**
	 * Lists the name and current value of each registered option out to the console.
	 */
	public void list() {
		StringBuilder b = new StringBuilder(2000);
		b.append("\n\tCurrent Option Values\n\t=====================");
		for(IOption<?> opt: options.values()) {
			b.append("\n\t").append(opt.getName()).append(":").append(opt.getValue());
		}
		b.append("\n");
		ConsoleOps.info(b);
	}
	
	/**
	 * Lists the available option names, description and current value.
	 */
	public void help() {
		
	}

	public static void main(String[] args) {
		Options options = new Options();
		options.list();
	}

	/*
	 * event.listener.render
	 * event.listener.filter
	 * event.listener.in / out / inout
	 * statement.cache
	 * statement.cache.maxsize
	 * 
	 * 
	 */

	@Option(name = "scriptdir", key = "script.dir", type = String.class, defaultValue = "${user.home}/.helios/scripts", description = "The default directory where cl dynamic scripts are read from")
	public static class ScriptDir extends StringOption<String> {
		public String load() {
			String s = super.load();
			if(s==null) {
				s = defaultValue;
				value = defaultValue;
				update();
				File f = new File(s);
				if(!f.exists()) {
					f.mkdirs();
				}
			}
			return s;
		}
	}
	
	@Option(name = "jmxurl", key = "remote.default.jmxurl", type = String.class, defaultValue = "service:jmx:rmi://localhost:8003", description = "The default JMX Service URL to connect to")
	public static class JMXUrl extends StringOption<String> {}
	
	@Option(name = "clientprompt", key = "client.prompt", type = String.class, defaultValue = "helios>", description = "The default client prompt")
	public static class ClientPrompt extends StringOption<String> {}	

	@Option(name = "ansiconsole", key = "console.ansi.enabled", type = Boolean.class, defaultValue = "true", description = "Indicates if the console is Ansi escape enabled")
	public static class AnsiConsole extends BooleanOption<Boolean> {}
	
	@Option(name = "asynchconsole", key = "console.ansi.asynch", type = Boolean.class, defaultValue = "true", description = "Indicates if the console is asynch enabled")
	public static class AsynchConsole extends BooleanOption<Boolean> {}
	
	@Option(name = "initialtimeout", key = "event.listener.timeout.initial", type = Long.class, defaultValue = "10000", description = "The client timeout in ms. waiting for the first response")
	public static class InitialTimeOut extends LongOption<Long> {}
	
	@Option(name = "timeout", key = "event.listener.timeout.running", type = Long.class, defaultValue = "10000", description = "The client timeout in ms. waiting for the next response")
	public static class RunningTimeOut extends LongOption<Long> {}
	
	@Option(name = "maxevents", key = "event.listener.maxevents.all", type = Long.class, defaultValue = "9999999999", description = "The maximum total number of events that can be retrieved for a statement")
	public static class MaxEvents extends LongOption<Long> {}
	
	@Option(name = "maxbatchevents", key = "event.listener.maxevents.batch", type = Long.class, defaultValue = "1000", description = "The maximum number of events that can be retrieved for a single batch")
	public static class MaxBatchEvents extends LongOption<Long> {}
	
}
