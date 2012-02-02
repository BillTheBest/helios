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
package org.helios.net.ssh.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.helios.net.ssh.EchoService;
import org.helios.net.ssh.LocalConfig;
import org.helios.net.ssh.SSHService;
import org.helios.net.ssh.portforward.LocalPortForward;
import org.helios.reflection.PrivateAccessor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>Title: SSHLocalPortForwardingTestCase</p>
 * <p>Description: Local port forwarding test cases</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.auth.SSHLocalPortForwardingTestCase</code></p>
 */

public class SSHLocalPortForwardingTestCase extends BaseSSHTestCase {
	/** The port the echo service is listening on */
	protected static int ECHO_PORT = -1;
	/** Indicates if the local echo service is available on port 7 */
	protected static boolean LOCAL_ECHO_SERVICE = false;
	
	/** The byte sample size to test port forwards with */
	protected static int BYTE_SAMPLE_SIZE = 1024;

	/**
	 * Starts the echo service
	 * @throws IOException thrown if echo service fails to start
	 */
	@BeforeClass
	public static void startEchoService() throws IOException {
		ECHO_PORT = EchoService.start();
		try {
			byte[] randomData = new byte[BYTE_SAMPLE_SIZE * 10];
			byte[] echoedData = echo("localhost", 7, randomData);
			Assert.assertArrayEquals("The out data equals the in data", randomData, echoedData);
			LOCAL_ECHO_SERVICE = true;
		} catch (Exception e) {
			LOCAL_ECHO_SERVICE = false;
		}
	}
	
	/**
	 * Stops the test echp service
	 */
	@AfterClass
	public static void stopEchoService() {
		EchoService.stop();
		ECHO_PORT = -1;
	}
	
	
	/**
	 * Validates that the echo service is responsive
	 * @throws Exception thrown if echo service cannot be verified.
	 */
	@Test
	public void testEchoService() throws Exception {
		byte[] randomData = new byte[BYTE_SAMPLE_SIZE];
		byte[] echoedData = echo("localhost", ECHO_PORT, randomData);
		Assert.assertArrayEquals("The out data equals the in data", randomData, echoedData);
	}
	
	
	
	/**
	 * Tests a simple port forward to the echo service through the test SSH server
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSimplePortForward() throws Exception {	
		Map.Entry<Object, Object> entry = goodPasswordAuths.entrySet().iterator().next();
		String user = entry.getKey().toString();
		String password = entry.getValue().toString();
		SSHService ssh = SSHService.createSSHService("127.0.0.1", SSHD_PORT, user, false)				
				.connect()
				.sshUserPassword(password)
				.authenticate();
		Assert.assertTrue("The SSHService is connected", ssh.isConnected());
		Assert.assertTrue("The SSHService is authenticated", ssh.isAuthenticated());
		LocalPortForward lpf = ssh.localPortForward(ECHO_PORT);
		int localPort = lpf.getLocalPort();
		byte[] randomData = new byte[BYTE_SAMPLE_SIZE];		
		Assert.assertArrayEquals("The out data equals the in data", randomData, echo("127.0.0.1", localPort, randomData));
		// ============
		// Periodically fails.
		// ============
		//Assert.assertEquals("The Local to Remote Bytes Transferred", BYTE_SAMPLE_SIZE, lpf.getLocalToRemoteBytesTransferred());
		Assert.assertEquals("The Remote to Local Bytes Transferred", BYTE_SAMPLE_SIZE, lpf.getRemoteToLocalBytesTransferred());
		closeSSHService(ssh, true);
		Assert.assertEquals("The Portforward is closed", true, lpf.isClosed());
		Assert.assertEquals("The Portforward is closed", false, lpf.isConnected());
	}
	
	
	/**
	 * Tests port forward exclusivity with ephemeral local ports
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testEphemeralPortForwardExclusivity() throws Exception {
		SSHService ssh = null;
		LocalPortForward lpf1  = null;
		LocalPortForward lpf2  = null;
		try {
			Map.Entry<Object, Object> entry = goodPasswordAuths.entrySet().iterator().next();
			String user = entry.getKey().toString();
			String password = entry.getValue().toString();
			ssh = SSHService.createSSHService("127.0.0.1", SSHD_PORT, user, true)				
					.connect()
					.sshUserPassword(password)
					.authenticate();
			Assert.assertEquals("The SSHService cached instance count", 1, SSHService.getSSHServiceInstanceCount());
			Assert.assertEquals("Connection has zero port forwards", 0, ssh.getLocalPortForwardCount());
			Assert.assertTrue("The SSHService is connected", ssh.isConnected());
			Assert.assertTrue("The SSHService is authenticated", ssh.isAuthenticated());
			lpf1 = ssh.localPortForward(ECHO_PORT);
			Assert.assertEquals("Connection has 1 port forward", 1, ssh.getLocalPortForwardCount());
			lpf2 = ssh.localPortForward(ECHO_PORT);
			//Assert.assertEquals("Connection has 2 port forwards", 2, portForwards.size());
			Assert.assertNotSame("LPF#1 key not same as LPF#2 key", lpf1.getPortForwardKey(), lpf2.getPortForwardKey());
			Assert.assertNotSame("LPF#1 key hashcode not same as LPF#2 key hashcode", lpf1.getPortForwardKey().hashCode(), lpf2.getPortForwardKey().hashCode());
			// Verify lpfs are the not same
			Assert.assertEquals("LPF#1 is not LPF#2", false, lpf1==lpf2);
			Assert.assertNotSame("LPF#1 is not LPF#2", System.identityHashCode(lpf1), System.identityHashCode(lpf2));
			Assert.assertNotSame("LPF#1 local port is not LPF#2 local port", lpf1.getLocalPort(), lpf2.getLocalPort());
			
			byte[] randomData = new byte[BYTE_SAMPLE_SIZE];
			
			
			// Verify both port forwards are connected
			Assert.assertEquals("LPF#1 is connected", true, lpf1.isConnected());
			Assert.assertEquals("LPF#2 is connected", true, lpf2.isConnected());
			// Test both port forwards		
			Assert.assertArrayEquals("LPF#1 Works Ok", randomData, echo("127.0.0.1", lpf1.getLocalPort(), randomData));
			Assert.assertArrayEquals("LPF#2 Works Ok", randomData, echo("127.0.0.1", lpf2.getLocalPort(), randomData));
		} finally {
			closeSSHService(ssh, true);
			// Verify both lpfs are closed
			Assert.assertEquals("LPF#1 is closed", true, lpf1.isClosed());
			Assert.assertEquals("LPF#2 is closed", true, lpf2.isClosed());
			Assert.assertEquals("LPF#1 is closed", false, lpf1.isConnected());
			Assert.assertEquals("LPF#2 is closed", false, lpf2.isConnected());			
		}
		
		
	}
	
	
	/**
	 * Tests a simple port forward to the echo service through the native SSH server
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testSimpleNativePortForward() throws Exception {	
		Assume.assumeTrue(LocalConfig.LOCAL_CONFIG.canRead());
		LocalConfig.load();
		String user = LocalConfig.getUsers()[0];		
		SSHService ssh = SSHService.createSSHService(LocalConfig.getSSHHost(user), LocalConfig.getSSHPort(user), user, false)				
				.connect()
				.sshUserPassword(LocalConfig.getPassword(user))
				.authenticate();
		Assert.assertTrue("The SSHService is connected", ssh.isConnected());
		Assert.assertTrue("The SSHService is authenticated", ssh.isAuthenticated());
		LocalPortForward lpf = ssh.localPortForward(ECHO_PORT);
		int localPort = lpf.getLocalPort();		
		byte[] randomData = new byte[BYTE_SAMPLE_SIZE];		
		Assert.assertArrayEquals("The out data equals the in data", randomData, echo("127.0.0.1", localPort, randomData));
		// ============
		// Periodically fails.
		// ============		
		//Assert.assertEquals("The Local to Remote Bytes Transferred", BYTE_SAMPLE_SIZE, lpf.getLocalToRemoteBytesTransferred());
		Assert.assertEquals("The Remote to Local Bytes Transferred", BYTE_SAMPLE_SIZE, lpf.getRemoteToLocalBytesTransferred());
		closeSSHService(ssh, true);
		Assert.assertEquals("The Portforward is closed", true, lpf.isClosed());
		Assert.assertEquals("The Portforward is closed", false, lpf.isConnected());		
	}
	
	

	
	/**
	 * Executes an echo test against the target socket
	 * @param host The host
	 * @param port The listening echo port
	 * @param sendArray The byte array to send
	 * @return The echoed byte array
	 * @throws Exception thrown on any exception
	 */
	public static byte[] echo(String host, int port, byte[] sendArray) throws Exception {
		Socket socket = null;
		try {						
			socket = new Socket(host, port);
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			os.write(sendArray);
			os.flush();
			byte[] buffer = new byte[sendArray.length];
//			ByteArrayOutputStream baos = new ByteArrayOutputStream(sendArray.length);
//			int bytesRead = -1;
//			while((bytesRead = is.read(buffer))!=-1) {
//				baos.write(buffer, 0, bytesRead);
//			}
//			baos.flush();
//			return baos.toByteArray();
			is.read(buffer);
			return buffer;
		} finally {
			try { socket.close(); } catch (Exception e) {}
		}		
	}
	
}
