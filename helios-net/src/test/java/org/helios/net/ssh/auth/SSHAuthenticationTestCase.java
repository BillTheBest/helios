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

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.helios.helpers.FileHelper;
import org.helios.net.ssh.ApacheSSHDServer;
import org.helios.net.ssh.SSHAuthenticationException;
import org.helios.net.ssh.SSHService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: SSHAuthenticationTestCase</p>
 * <p>Description: Test cases to validate various different combinations of SSHd authentication.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.auth.SSHAuthenticationTestCase</code></p>
 */
public class SSHAuthenticationTestCase {
	/** The port the test server is listening on */
	protected static int SSHD_PORT = -1;
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();
	/** Instance logger */
	protected final Logger LOG = Logger.getLogger(getClass());
	
	/** Known good credentials  */
	static Map<Object, Object> goodPasswordAuths = FileHelper.loadProperties(new File("./src/test/resources/auth/password/credentials.properties"));
	
	/**
	 * Starts the test SSHD server
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ApacheSSHDServer.main();
		SSHD_PORT = ApacheSSHDServer.getPort(); 
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				System.out.println("\n\tStopping ApacheSSHDServer");
				try { ApacheSSHDServer.stop(true); } catch (Exception e) {} 
			}
		});
	}

	/**
	 * Stops the test SSHD server
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() {
		try { ApacheSSHDServer.stop(true); } catch (Exception e) {} 
	}
	
	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */	
	@Before
	public void setUp() {
		ApacheSSHDServer.resetAuthenticators();
		String methodName = testName.getMethodName();
		LOG.debug("\n\t******\n\t Test [" + getClass().getSimpleName() + "." + methodName + "]\n\t******");
	}	

	/**
	 * Tests authentication with no credentials
	 * @throws Exception
	 */
	@Test
	public void testNoAuthSSHLogin() throws Exception {
		SSHService ssh = SSHService.createSSHService("localhost", SSHD_PORT, "" + System.nanoTime())
		.sshUserPassword("" + System.nanoTime())
		.connect()
		.authenticate();
		Assert.assertEquals("The SSHService shared count", 1, ssh.getSharedCount());
		Assert.assertTrue("The SSHService is connected", ssh.isConnected());
		Assert.assertTrue("The SSHService is authenticated", ssh.isAuthenticated());
		ssh.close();
		Assert.assertEquals("The SSHService shared count", 0, ssh.getSharedCount());
		Assert.assertFalse("The SSHService is connected", ssh.isConnected());
		Assert.assertFalse("The SSHService is authenticated", ssh.isAuthenticated());		
	}
	
	/**
	 * Tests authentication with no credentials with an expected failure
	 * @throws Exception
	 */
	@Test(expected=SSHAuthenticationException.class)
	public void testBadNoAuthSSHLogin() throws Exception {
		ApacheSSHDServer.activatePasswordAuthenticator(true);
		SSHService.createSSHService("localhost", SSHD_PORT, "" + System.nanoTime())
		.sshUserPassword("" + System.nanoTime())
		.connect()
		.authenticate();
	}
	
	/**
	 * Tests authentication with known good username/password
	 * @throws Exception
	 */
	@Test()
	public void testPasswordSSHLogin() throws Exception {
		ApacheSSHDServer.activatePasswordAuthenticator(true);
		for(Map.Entry<Object, Object> creds: goodPasswordAuths.entrySet()) {
			SSHService ssh = SSHService.createSSHService("localhost", SSHD_PORT, creds.getKey().toString())
				.sshUserPassword(creds.getValue().toString())
				.connect()
				.authenticate();
				Assert.assertEquals("The SSHService shared count", 1, ssh.getSharedCount());
				Assert.assertTrue("The SSHService is connected", ssh.isConnected());
				Assert.assertTrue("The SSHService is authenticated", ssh.isAuthenticated());
				ssh.close();
				Assert.assertEquals("The SSHService shared count", 0, ssh.getSharedCount());
				Assert.assertFalse("The SSHService is connected", ssh.isConnected());
				Assert.assertFalse("The SSHService is authenticated", ssh.isAuthenticated());					
		}
	}
	
	/**
	 * Tests authentication with known good username/password
	 * @throws Exception
	 */
	@Test()
	public void testSharedPasswordSSHLogin() throws Exception {
		ApacheSSHDServer.activatePasswordAuthenticator(true);
		Map.Entry<Object, Object> creds = goodPasswordAuths.entrySet().iterator().next();
		SSHService ssh = SSHService.createSSHService("localhost", SSHD_PORT, creds.getKey().toString())
				.sshUserPassword(creds.getValue().toString())
				.connect()
				.authenticate();
		Assert.assertEquals("The SSHService1 shared count", 1, ssh.getSharedCount());
		Assert.assertTrue("The SSHService1 is connected", ssh.isConnected());
		Assert.assertTrue("The SSHService1 is authenticated", ssh.isAuthenticated());
		
		SSHService ssh2 = SSHService.createSSHService("localhost", SSHD_PORT, creds.getKey().toString())
				.sshUserPassword(creds.getValue().toString())
				.connect();
		Assert.assertEquals("The SSHService2 shared count", 2, ssh2.getSharedCount());		
		Assert.assertTrue("The SSHService2 is connected", ssh2.isConnected());
		Assert.assertTrue("The SSHService2 is authenticated", ssh2.isAuthenticated());

		Assert.assertEquals("The SSHService1 shared count", 2, ssh.getSharedCount());		
		Assert.assertTrue("The SSHService1 is connected", ssh.isConnected());
		Assert.assertTrue("The SSHService1 is authenticated", ssh.isAuthenticated());
		
		ssh.close();
		
		Assert.assertEquals("The SSHService2 shared count", 1, ssh2.getSharedCount());		
		Assert.assertTrue("The SSHService2 is connected", ssh2.isConnected());
		Assert.assertTrue("The SSHService2 is authenticated", ssh2.isAuthenticated());

		Assert.assertEquals("The SSHService1 shared count", 1, ssh.getSharedCount());		
		Assert.assertTrue("The SSHService1 is connected", ssh.isConnected());
		Assert.assertTrue("The SSHService1 is authenticated", ssh.isAuthenticated());
		
		ssh2.close();
		
		Assert.assertEquals("The SSHService2 shared count", 0, ssh2.getSharedCount());		
		Assert.assertFalse("The SSHService2 is connected", ssh2.isConnected());
		Assert.assertFalse("The SSHService2 is authenticated", ssh2.isAuthenticated());

		Assert.assertEquals("The SSHService1 shared count", 0, ssh.getSharedCount());		
		Assert.assertFalse("The SSHService1 is connected", ssh.isConnected());
		Assert.assertFalse("The SSHService1 is authenticated", ssh.isAuthenticated());
		
	}
	

}
