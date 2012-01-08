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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.helpers.StringHelper;
import org.kohsuke.args4j.Option;

/**
 * <p>Title: RuntimeCommandReader</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.RuntimeCommandReader</code></p>
 */

public class RuntimeCommandReader extends Thread {
	/** A scanner for reading commands from the input stream  */
	protected final Scanner inputScanner = new Scanner(System.in).useDelimiter("\\s+");
	/** the current command buffer */
	protected final List<String> argumentBuffer = new ArrayList<String>();
	/** the executor to which the reader passes back commands to */
	protected final EsperCLRuntimeExecutor executor;
	/** the stop indicator */
	protected final AtomicBoolean stop = new AtomicBoolean(false);
	/**
	 * Creates a new RuntimeCommandReader
	 * @param executor the executor to which the reader passes back commands to
	 */
	public RuntimeCommandReader(EsperCLRuntimeExecutor executor) {
		this.executor = executor;
		setName("RuntimeCommandReaderThread");
		setDaemon(false);
		setPriority(Thread.MAX_PRIORITY);	
	}
	
	public void stopReader() {
		stop.set(true);
		this.interrupt();
	}
	
	/**
	 * Starts the thread and reads from system input, passing the command set back to the executor whenever one is read.
	 * @see java.lang.Thread#run()
	 */
	public void run() {		
		try {
			while(inputScanner.hasNextLine()) {
				String[] rawArgs = StringHelper.parseWhiteSpaceQuoted(inputScanner.nextLine()).toArray(new String[0]);
				if(rawArgs==null || rawArgs.length<1) {
					ConsoleOps.prompt();
					continue;
				}
				Collections.addAll(argumentBuffer, rawArgs);
				if(rawArgs.length > 0) {
					if(rawArgs[rawArgs.length-1].equals("\\")) {						
						continue;
					} 
				}
				if(argumentBuffer.size()>0) {
					String command = argumentBuffer.remove(0).trim();
					String[] commandArgs = argumentBuffer.toArray(new String[argumentBuffer.size()]);
					executor.executeCommands(command, commandArgs);
				}
				argumentBuffer.clear();
			}
		} catch (Exception ie) {
			if(!stop.get()) {
				Thread.interrupted(); Thread.interrupted();
			} else {
				out("\nStopping Reader\n");
			}
		}
	}
	
	
	/**
	 * Concatenates and out prints a message
	 * @param messages the fragments of a message
	 */
	public static void out(Object...messages) {
		if(messages==null || messages.length<1) return;
		StringBuilder b = new StringBuilder();
		for(Object o: messages) {
			if(o!=null) {
				b.append(o.toString());
			}
		}
		System.out.print(b);
	}	
}
