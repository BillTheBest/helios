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
import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.Color.RED;

import java.io.PrintStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi.Color;
import org.helios.helpers.StringHelper;

/**
 * <p>Title: ConsoleOps</p>
 * <p>Description: Generic console utilities with ANSI support.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.espercl.ConsoleOps</code></p>
 */

public class ConsoleOps {
	/** Indicates if ANSI output escaping is enabled */
	static boolean ANSIENABLED = false;
	/** Indicates if print jobs should be submitted asynchronously */
	private static boolean ASYNCH = false;
	/** The asynchronous print job submission executor */
	private static Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory(){
		public Thread newThread(Runnable runnable) {
			Thread t = new Thread(runnable, "ConsoleOpsAsynchProcessor");
			t.setDaemon(true);
			return t;
		}		
	});
	
	static {
		AnsiConsole.systemInstall();
		ANSIENABLED = Ansi.isDetected();
	}
	
	
	
	/**
	 * Concatenates and err prints a message
	 * @param messages the fragments of an error message
	 */
	public static void err(Object...messages) {
		print(RED, true, System.err, messages);
	}


	
	/**
	 * Executes a print request.
	 * @param colour The colour of the output which will be ignored if <code>ANSIENABLED</code> is <code>false</code>.
	 * @param trailingEol if true, an EOL character will be appended on the end of the print
	 * @param ps The print stream the job should be sent to
	 * @param messages The message fragments that make up the print content.
	 */
	public static void print(Ansi.Color colour, boolean trailingEol, PrintStream ps, Object...messages) {
		PrintJob pj = PrintJob.newPrintJob(colour, trailingEol, ps, messages);
		if(ASYNCH) {
			executor.execute(pj);
		} else {
			pj.run();
		}
	}
	
	/**
	 * Concatenates and info prints a message
	 * @param messages the fragments of a message
	 */
	public static void info(Object...messages) {
		print(GREEN, true, System.out, messages);
	}
	
	/**
	 * Concatenates and data prints a message
	 * @param messages the fragments of a message
	 */
	public static void data(Object...messages) {
		print(BLUE, true, System.out, messages);
	}
	
	/**
	 * Concatenates and prints a notification pop message
	 * @param messages the fragments of a message
	 */
	public static void notif(Object...messages) {
		print(CYAN, true, System.out, messages);
	}
	
	/**
	 * Prints the CL prompt
	 * @param the specified prompt
	 */
	public static void prompt(String prompt) {
		print(MAGENTA, false, System.out, prompt);
	}
	
	/**
	 * Prints the CL prompt
	 */
	public static void prompt() {
		prompt(EsperCLRuntimeExecutor.PROMPT);
	}
	
	/**
	 * Concatenates a message
	 * @param messages the fragments of an error message
	 * @return the formated string
	 */
	public static String format(Object...messages) {
		if(messages==null || messages.length<1) return "";
		StringBuilder b = new StringBuilder();
		for(Object o: messages) {
			if(o!=null) {
				if(o instanceof Throwable) {
					b.append(StringHelper.formatStackTrace((Throwable)o));
				} else {
					b.append(o.toString());
				}
			}
		}
		return b.toString();
	}
}

class PrintJob implements Runnable {
	private final Ansi.Color colour;
	private final boolean trailingEol; 
	private final PrintStream ps;
	private final Object[] messages;

	/**
	 * Creates a new PrintJob
	 * @param colour the colour of the output
	 * @param trailingEol idicates if a trailing End Of Line character should be appended.
	 * @param ps The print stream to send to
	 * @param messages The message fragments to build the print job content from
	 */
	public static PrintJob newPrintJob(Color colour, boolean trailingEol, PrintStream ps, Object...messages) {
		return new PrintJob(colour, trailingEol, ps, messages);
	}
	
	/**
	 * Creates a new PrintJob
	 * @param colour the colour of the output
	 * @param trailingEol idicates if a trailing End Of Line character should be appended.
	 * @param ps The print stream to send to
	 * @param messages The message fragments to build the print job content from
	 */
	private PrintJob(Color colour, boolean trailingEol, PrintStream ps, Object...messages) {
		this.colour = colour;
		this.trailingEol = trailingEol;
		this.ps = ps;
		this.messages = messages;
	}



	@Override
	public void run() {
		String content = ConsoleOps.format(messages);
		if(trailingEol) {
			ps.println(ConsoleOps.ANSIENABLED ? Ansi.ansi(content.length()).fg(colour).a(content).reset() : content);
		} else {
			ps.print(ConsoleOps.ANSIENABLED ? Ansi.ansi(content.length()).fg(colour).a(content).reset() : content);
		}
		ps.flush();
	}
	
}


/*
import org.fusesource.jansi.*;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

this.class.classLoader.rootLoader.addURL( new URL("file://home/nwhitehead/libs/java/jansi/jansi-1.4.jar") );
AnsiConsole.systemInstall();
println Ansi.ansi().eraseScreen().fg(RED).a("Hello").reset();
println Ansi.ansi().eraseLine().fg(GREEN).a(" World").reset();
*/