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
package org.helios.net.ssh.agent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: SSHAgentAdapter</p>
 * <p>Description: Utility class to interface with the native ssh-agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.agent.SSHAgentAdapter</code></p>
 */
public class SSHAgentAdapter {
	/** Indicates if the VM is running in Windows */
	public static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("windows");
	/** The ssh-agent executable */
	public static final String SSH_AGENT_NAME = IS_WIN ? "ssh-agent.exe" : "ssh-agent";
	/** The ssh-add executable */
	public static final String SSH_ADD_NAME = IS_WIN ? "ssh-add.exe" : "ssh-add";
	/** The JVM's line separator */
	public static final String EOL = System.getProperty("line.separator", "\n");
	
	
	/** The environment captured from the Process */
	protected final Map<String,String> environment; 
	/** The process builder to create the native OS process */
	protected final ProcessBuilder processBuilder;
	/** The ssh-agent process */
	protected volatile Process agentProcess;
	
	/**
	 * Creates a new SSHAgentAdapter
	 */
	public SSHAgentAdapter() {
		processBuilder = new ProcessBuilder();
		environment = processBuilder.environment();
	}
	
	/**
	 * Initializes the environment from the ssh-agent output
	 * @return The SSH Agent's environment directives
	 */
	public String[] initialize()  {
		final StringBuilder err = new StringBuilder();
		try {
			final ProcessBuilder pb = new ProcessBuilder(SSH_AGENT_NAME);
			agentProcess = pb.start();
			final Process process = agentProcess;
			final StringBuilder output = new StringBuilder();			
			final CountDownLatch latch = new CountDownLatch(1); 
			new Thread("ssh-agent-err-reader") {
				public void run() {
					try {
						BufferedReader bis = new BufferedReader(new InputStreamReader(process.getErrorStream()));
						String line = null;
						while((line = bis.readLine())!=null) {
							err.append(line).append(EOL);													
						}
					} catch (Exception e) {
						log("Failed to process ssh-agent output. Stack trace follows.");
						e.printStackTrace(System.err);
					}
				}
			}.start();
			new Thread("ssh-agent-output-reader") {
				public void run() {
					try {
						BufferedReader bis = new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line = null;
						while((line = bis.readLine())!=null) {
							if(!line.toLowerCase().startsWith("echo")) {
								output.append(line).append(EOL);
							}							
						}
						latch.countDown();
					} catch (Exception e) {
						log("Failed to process ssh-agent output. Stack trace follows.");
						e.printStackTrace(System.err);
					}
				}
			}.start();
			if(!latch.await(5000, TimeUnit.MILLISECONDS)) {
				process.destroy();
				throw new Exception("No response from agent. Request timed out.", new Throwable());				
			}
			
			String[] envs = output.toString().split(EOL);
			for(String s: envs) {
				String[] pair = s.split(";")[0].split("=");
				environment.put(pair[0], pair[1]);
				log("Added Env Pair[" + pair[0] + ":" + pair[1]);				
			}
			return envs;
		} catch (Exception e) {
			log("Error:" + err);
			throw new RuntimeException("Failed to invoke [" + SSH_AGENT_NAME + "]", e);
		}
	}
	
	/** Thread name serial number */
	protected static final AtomicLong serial = new AtomicLong(0L);
	
	/**
	 * Creates an input stream logging thread and starts it.
	 * @param is The input stream to read
	 * @param name The name of the process running
	 */
	public void newStreamReader(final InputStream is, final String name) {
		newStreamReader(is, name, null, null);
	}
	
	
	/**
	 * Creates an input stream reader thread and starts it.
	 * @param is The input stream to read
	 * @param name The name of the process running
	 * @param buffer An optional buffer to write captured output to
	 * @param latch An optiona latch to drop when the stream ends
	 */
	public void newStreamReader(final InputStream is, final String name, final StringBuilder buffer, final CountDownLatch latch) {
		Thread t = new Thread("Stream reader for [" + name + "] #" + serial.incrementAndGet()) {
			public void run() {
				try {
					BufferedReader bis = new BufferedReader(new InputStreamReader(is));
					String line = null;
					while((line = bis.readLine())!=null) {
						if(buffer!=null) {
							buffer.append(line).append(EOL);
						} else {
							log(line);
						}													
					}
					if(latch!=null) {
						latch.countDown();
					}
				} catch (Exception e) {
					log("Failed to process [" + name + "]. Stack trace follows.");
					e.printStackTrace(System.err);
				}
			}			
		};
		t.setDaemon(true);
		t.start();
	}
	
	public void close() {
		if(agentProcess!=null) {
			try {
				processBuilder.command("bash.exe", "-c" , "\"ssh-agent -k\"");
				final Process process = processBuilder.start();
				new Thread("ssh-agent-kill-err-reader") {
					public void run() {
						try {
							BufferedReader bis = new BufferedReader(new InputStreamReader(process.getErrorStream()));
							String line = null;
							while((line = bis.readLine())!=null) {
								log("ERROR:" + line);													
							}
						} catch (Exception e) {
							log("Failed to process ssh-agent output. Stack trace follows.");
							e.printStackTrace(System.err);
						}
					}
				}.start();
				new Thread("ssh-agent-kill-output-reader") {
					public void run() {
						try {
							BufferedReader bis = new BufferedReader(new InputStreamReader(process.getInputStream()));
							String line = null;
							while((line = bis.readLine())!=null) {
								log("OUT:" + line);														
							}							
						} catch (Exception e) {
							log("Failed to process ssh-agent output. Stack trace follows.");
							e.printStackTrace(System.err);
						}
					}
				}.start();
				
				int result = process.waitFor();
				log("Exit Code:" + result);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("SSH Agent Test");
		SSHAgentAdapter agent = new SSHAgentAdapter(); 
		log(Arrays.toString(agent.initialize()));
		agent.close();
	}

}
