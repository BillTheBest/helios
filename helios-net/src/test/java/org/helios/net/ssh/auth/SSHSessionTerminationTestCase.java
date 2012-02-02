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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.helios.net.ssh.ApacheSSHDServer;
import org.helios.net.ssh.Reconnector;
import org.helios.net.ssh.SSHService;
import org.helios.net.ssh.SSHServiceConnectionListener;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Title: SSHSessionTerminationTestCase</p>
 * <p>Description: Test case to validate that an interruption of comm to the SSH server will be detected by the SSHService</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.auth.SSHSessionTerminationTestCase</code></p>
 */

public class SSHSessionTerminationTestCase extends BaseSSHTestCase {
	/**
	 * Tests SSH service interruption by shutting down the SSHD server
	 * @throws Exception thrown on any exception
	 */
	@Test()
	public void testSessionInterrupted() throws Exception {
		SSHService ssh = null;
		try {
			ApacheSSHDServer.activatePasswordAuthenticator(true);		
			Map.Entry<Object, Object> creds = goodPasswordAuths.entrySet().iterator().next();
			Assert.assertEquals("Test for existing shared SSHService", true, null==SSHService.findServiceFor("localhost", SSHD_PORT));
			ApacheSSHDServer.activatePasswordAuthenticator(true);
			ssh = SSHService.createSSHService("localhost", SSHD_PORT, creds.getKey().toString())				
					.connect()
					.sshUserPassword(creds.getValue().toString())
					.authenticate();
			Assert.assertTrue("The SSHService cache has a shared instance", SSHService.hasSharedServiceFor("localhost", SSHD_PORT));
			Assert.assertEquals("The SSHService cached instance count", 1, SSHService.getSSHServiceInstanceCount());
			Assert.assertEquals("The SSHService1 shared count", 1, ssh.getSharedCount());
			Assert.assertTrue("The SSHService1 is shared connection", ssh.isSharedConnection());
			Assert.assertTrue("The SSHService1 is connected", ssh.isConnected());
			Assert.assertTrue("The SSHService1 is authenticated", ssh.isAuthenticated());
			Assert.assertEquals("Test for existing shared SSHService", true, null!=SSHService.findServiceFor("localhost", SSHD_PORT, 0, 0));
			Assert.assertEquals("The SSHService cached instance count", 1, SSHService.getSSHServiceInstanceCount());
			// ===============================================================
			// Register a ssh service listener and stop the ssh server
			// ===============================================================
			final Map<String, Object> state = new HashMap<String, Object>();
			final CountDownLatch failLatch = new CountDownLatch(1); 
			final CountDownLatch closeLatch = new CountDownLatch(1);
			SSHServiceConnectionListener listener = new SSHServiceConnectionListenerImpl() {
				@Override
				public void onConnectionFailure(Throwable t, SSHService sshService) {
					state.put("SERVICE", sshService);
					state.put("THROWABLE", t);
					failLatch.countDown();
				}
				@Override
				public void onConnectionHardClosed(SSHService sshService) {
					state.put("CLOSEDSERVICE", sshService);
					closeLatch.countDown();
					
				}
				@Override
				public void onConnect(SSHService sshService) {
					
				}
			};
			ssh.addListener(listener);
			ApacheSSHDServer.stop();
			boolean latchFired = failLatch.await(5000, TimeUnit.MILLISECONDS);
			Assert.assertEquals("The SSHService listener triggered the fail latch", true, latchFired);			
			Assert.assertEquals("The SSHService listener was passed this ssh", ssh, state.get("SERVICE"));
			Assert.assertNotNull("The SSHService listener was passed a throwable", state.get("THROWABLE"));
			
			boolean closeLatchFired = closeLatch.await(ssh.getConnectionTimeout()*2, TimeUnit.MILLISECONDS);
			Assert.assertEquals("The SSHService listener triggered the close latch", true, closeLatchFired);
			Assert.assertEquals("The SSHService listener was passed this closed ssh", ssh, state.get("CLOSEDSERVICE"));
			Assert.assertTrue("The SSH service is closed", !ssh.isConnected());
		} finally {
			if(ssh!=null && ssh.isConnected()) {
				closeSSHService(ssh, false);
			} else {
				cleanSSHService();
			}
		}
	}
	
	/**
	 * Tests SSH service interruption and recovery by shutting down the SSHD server and restarting it (hopefully before the timeout)
	 * @throws Exception thrown on any exception
	 */
	@Test()
	public void testSessionInterruptedAndRecovered() throws Exception {
		SSHService ssh = null;
		ApacheSSHDServer.activatePasswordAuthenticator(true);
		
		try {
			ApacheSSHDServer.activatePasswordAuthenticator(true);		
			Map.Entry<Object, Object> creds = goodPasswordAuths.entrySet().iterator().next();
			Assert.assertEquals("Test for existing shared SSHService", true, null==SSHService.findServiceFor("localhost", SSHD_PORT));
			ssh = SSHService.createSSHService("localhost", SSHD_PORT, creds.getKey().toString())				
					.connect()
					.sshUserPassword(creds.getValue().toString())
					.authenticate();
			Assert.assertTrue("The SSHService cache has a shared instance", SSHService.hasSharedServiceFor("localhost", SSHD_PORT));
			Assert.assertEquals("The SSHService cached instance count", 1, SSHService.getSSHServiceInstanceCount());
			Assert.assertEquals("The SSHService1 shared count", 1, ssh.getSharedCount());
			Assert.assertTrue("The SSHService1 is shared connection", ssh.isSharedConnection());
			Assert.assertTrue("The SSHService1 is connected", ssh.isConnected());
			Assert.assertTrue("The SSHService1 is authenticated", ssh.isAuthenticated());
			Assert.assertEquals("Test for existing shared SSHService", true, null!=SSHService.findServiceFor("localhost", SSHD_PORT, 0, 0));
			Assert.assertEquals("The SSHService cached instance count", 1, SSHService.getSSHServiceInstanceCount());
			// ===============================================================
			// Register a ssh service listener and stop the ssh server
			// ===============================================================
			final Map<String, Object> state = new HashMap<String, Object>();
			final CountDownLatch failLatch = new CountDownLatch(1); 
			final CountDownLatch closeLatch = new CountDownLatch(1);
			final CountDownLatch reconnectLatch = new CountDownLatch(1);
			SSHServiceConnectionListener listener = new SSHServiceConnectionListenerImpl() {
				@Override
				public void onConnectionFailure(Throwable t, SSHService sshService) {
					state.put("SERVICE", sshService);
					state.put("THROWABLE", t);
					failLatch.countDown();
				}
				@Override
				public void onConnectionHardClosed(SSHService sshService) {
					state.put("CLOSEDSERVICE", sshService);
					closeLatch.countDown();
					
				}
				@Override
				public void onReconnect(SSHService service) {
					reconnectLatch.countDown();
				}
			};
			ssh.addListener(listener);			
			ApacheSSHDServer.stop();
			boolean latchFired = failLatch.await(5000, TimeUnit.MILLISECONDS);
			Assert.assertEquals("The SSHService listener triggered the fail latch", true, latchFired);			
			Assert.assertEquals("The SSHService listener was passed this ssh", ssh, state.get("SERVICE"));
			Assert.assertNotNull("The SSHService listener was passed a throwable", state.get("THROWABLE"));
			
			boolean closeLatchFired = closeLatch.await(ssh.getConnectionTimeout()*2, TimeUnit.MILLISECONDS);
			Assert.assertEquals("The SSHService listener triggered the close latch", true, closeLatchFired);
			Assert.assertEquals("The SSHService listener was passed this closed ssh", ssh, state.get("CLOSEDSERVICE"));
			Assert.assertTrue("The SSH service is closed", !ssh.isConnected());
			
			ApacheSSHDServer.main("" + SSHD_PORT);
			boolean reconnectLatchFired = reconnectLatch.await(Reconnector.DEFAULT_RECONNECT_PERIOD*2, TimeUnit.MILLISECONDS);
			Assert.assertEquals("The SSHService listener triggered the reconnect latch", true, reconnectLatchFired);
		} finally {
			if(ssh!=null && ssh.isConnected()) {
				closeSSHService(ssh, false);
			} else {
				cleanSSHService();
			}
		}
	}
	
	
	
//	/**
//	 * Restarts the SSH test server if it is stopped
//	 */
//	@Before
//	public void restartSSHDServer() {
//		if(!ApacheSSHDServer.isStarted()) {
//			ApacheSSHDServer.main();
//			SSHD_PORT = ApacheSSHDServer.getPort();
//		}
//	}

}
