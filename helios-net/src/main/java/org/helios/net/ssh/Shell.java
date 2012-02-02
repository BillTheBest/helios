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
package org.helios.net.ssh;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.ethz.ssh2.Session;

/**
 * <p>Title: Shell</p>
 * <p>Description: Represents a SSH Session with a shell/terminal based interface</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.Shell</code></p>
 */

public class Shell implements Closeable {
	/** The SSH2 session */
	protected final Session session;
	
	/**
	 * Creates a new Shell
	 * @param session The SSH2 Session
	 */
	public Shell(Session session) {
		this.session = session;
	}
	
	
	/**
	 * Closes this session and deallocates any resources allocated on its behalf.
	 */
	@Override
	public void close() {
		try {
			session.close();
		} catch (Exception e) {}
	}


	/**
	 * Requests a standard PTY virtual terminal
	 * This method may only be called before a program or shell is started in this session.
	 * @throws IOException thrown on any exception setting the terminal rquest
	 * @see ch.ethz.ssh2.Session#requestDumbPTY()
	 */
	public void requestDumbPTY() throws IOException {
		session.requestDumbPTY();
	}


	/**
	 * Requests a named terminal environment
	 * This method may only be called before a program or shell is started in this session. 
	 * @param term The TERM environment variable value (e.g., vt100)
	 * @throws IOException thrown on any exception setting the terminal rquest
	 * @see ch.ethz.ssh2.Session#requestPTY(java.lang.String)
	 */
	public void requestPTY(String term) throws IOException {
		session.requestPTY(term);
	}


	/**
	 * Allocate a pseudo-terminal for this session. 
	 * This method may only be called before a program or shell is started in this session.
	 * @param term The TERM environment variable value (e.g., vt100)
	 * @param term_width_characters terminal width, characters (e.g., 80)
	 * @param term_height_characters terminal height, rows (e.g., 24)
	 * @param term_width_pixels terminal width, pixels (e.g., 640)
	 * @param term_height_pixels terminal height, pixels (e.g., 480)
	 * @param terminal_modes encoded terminal modes (may be <code>null</code>)
	 * @throws IOException thrown on any exception setting the terminal rquest
	 * @see ch.ethz.ssh2.Session#requestPTY(java.lang.String, int, int, int, int, byte[])
	 */
	public void requestPTY(String term, int term_width_characters,
			int term_height_characters, int term_width_pixels,
			int term_height_pixels, byte[] terminal_modes) throws IOException {
		session.requestPTY(term, term_width_characters, term_height_characters,
				term_width_pixels, term_height_pixels, terminal_modes);
	}


	/**
	 * Request X11 forwarding for the current session. 
	 * This method may only be called before a program or shell is started in this session.
	 * @param hostname the hostname of the real (target) X11 server (e.g., 127.0.0.1)
	 * @param port the port of the real (target) X11 server (e.g., 6010)
	 * @param cookie if non-null, then present this cookie to the real X11 server
	 * @param singleConnection if true, then the server is instructed to only forward one single
	 *        connection, no more connections shall be forwarded after first, or after the session
	 *        channel has been closed
	 * @throws IOException Thrown on any IO exception establishing X11 forwarding.
	 * @see ch.ethz.ssh2.Session#requestX11Forwarding(java.lang.String, int, byte[], boolean)
	 */
	public void requestX11Forwarding(String hostname, int port, byte[] cookie,
			boolean singleConnection) throws IOException {
		session.requestX11Forwarding(hostname, port, cookie, singleConnection);
	}


	/**
	 * Executes a remote command on the connected host
	 * @param cmd The command to execute
	 * @throws IOException thrown on any IO exception on the session
	 * @see ch.ethz.ssh2.Session#execCommand(java.lang.String)
	 */
	public void execCommand(String cmd) throws IOException {
		session.execCommand(cmd);
	}


	/**
	 * Starts a shell on the connected host
	 * @throws IOException thrown on any IO exception on the session
	 * @see ch.ethz.ssh2.Session#startShell()
	 */
	public void startShell() throws IOException {
		session.startShell();
	}


	/**
	 * Returns the standard out stream for this session
	 * @return the standard out stream for this session
	 * @see ch.ethz.ssh2.Session#getStdout()
	 */
	public InputStream getStdout() {
		return session.getStdout();
	}


	/**
	 * Returns the standard err stream for this session
	 * @return the standard err stream for this session
	 * @see ch.ethz.ssh2.Session#getStderr()
	 */
	public InputStream getStderr() {
		return session.getStderr();
	}


	/**
	 * Returns the standard in stream for this session
	 * @return the standard in stream for this session
	 * @see ch.ethz.ssh2.Session#getStdin()
	 */
	public OutputStream getStdin() {
		return session.getStdin();
	}


	/**
	 * Get the exit code/status from the remote command - if available. Be
	 * careful - not all server implementations return this value. It is
	 * generally a good idea to call this method only when all data from the
	 * remote side has been consumed (see also the <code<WaitForCondition</code> method).
	 * 
	 * @return An <code>Integer</code> holding the exit code, or
	 *         <code>null</code> if no exit code is (yet) available.
	 * @see ch.ethz.ssh2.Session#getExitStatus()
	 */
	public Integer getExitStatus() {
		return session.getExitStatus();
	}


	/**
	 * Get the name of the signal by which the process on the remote side was
	 * stopped - if available and applicable. Be careful - not all server
	 * implementations return this value.
	 * 
	 * @return An <code>String</code> holding the name of the signal, or
	 *         <code>null</code> if the process exited normally or is still
	 *         running (or if the server forgot to send this information).
	 * @see ch.ethz.ssh2.Session#getExitSignal()
	 */
	public String getExitSignal() {
		return session.getExitSignal();
	}
	
}
