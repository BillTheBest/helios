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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.helios.net.ssh.ApacheSSHDServer;
import org.helios.net.ssh.SSHService;
import org.helios.net.ssh.ServerHostKey;
import org.helios.net.ssh.portforward.LocalPortForward;
import org.helios.net.ssh.portforward.LocalPortForwardStateListener;
import org.helios.reflection.PrivateAccessor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * <p>Title: BaseSSHTestCase</p>
 * <p>Description: Base class for SSH test cases</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.auth.BaseSSHTestCase</code></p>
 */
@Ignore
public class BaseSSHTestCase {
	/** The port the test server is listening on */
	protected static int SSHD_PORT = -1;
	/** Tracks the test name */
	@Rule
    public TestName testName= new TestName();
	/** Instance logger */
	protected static final Logger LOG = Logger.getLogger(SSHAuthenticationTestCase.class);
	
	/** Known good credentials  */
	final static Map<Object, Object> goodPasswordAuths = loadProperties(new File("./src/test/resources/auth/password/credentials.properties"));
	/** Private Key Passphrases  */
	final static Map<Object, Object> pkPassphrases = loadProperties(new File("./src/test/resources/auth/keys/passphrases.properties"));
	/** SSA Private Key Files keyed by user name */
	final static Map<String, File> dsaPks = new HashMap<String, File>();	
	/** RSA Private Key Files keyed by user name */
	final static Map<String, File> rsaPks = new HashMap<String, File>();
	
	/** Random factory */
	static final Random random = new Random(System.nanoTime());
	
	/**
	 * Loads the contents of a properties file 
	 * @param f The file
	 * @return the loaded properties
	 */
	private static Properties loadProperties(File f) {
		Properties p = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			p.load(fis);
			return p;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load properties from file [" + f + "]", e);
		} finally {
			try { fis.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Starts the test SSHD server
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		ApacheSSHDServer.main();
		SSHD_PORT = ApacheSSHDServer.getPort(); 
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				System.out.println("\n\tStopping ApacheSSHDServer");
				try { ApacheSSHDServer.stop(true); } catch (Exception e) {} 
			}
		});
		// Load the RSA private keys
		for(File pk: new File("./src/test/resources/auth/keys").listFiles(new FilenameFilter(){
				public boolean accept(File dir, String name) {
					return name.endsWith("_rsa");
				}
			})) {
			rsaPks.put(pk.getName().replace("_rsa", ""), pk);
		}
		LOG.info("Loaded [" + rsaPks.size() + "] RSA Private Keys");
		// Load the DSA private keys
		for(File pk: new File("./src/test/resources/auth/keys").listFiles(new FilenameFilter(){
				public boolean accept(File dir, String name) {
					return name.endsWith("_dsa");
				}
			})) {
			dsaPks.put(pk.getName().replace("_dsa", ""), pk);
		}
		LOG.info("Loaded [" + dsaPks.size() + "] DSA Private Keys");
		
	}

	/**
	 * Stops the test SSHD server
	 */
	@AfterClass
	public static void tearDownAfterClass() {
		try { ApacheSSHDServer.stop(true); } catch (Exception e) {} 
	}
	
	/**
	 * Resets the SSH server authentication methods and logs the next test name
	 */
	@Before
	public void setUp() {
		if(!ApacheSSHDServer.isStarted()) {
			ApacheSSHDServer.main("" + SSHD_PORT);						
		}		
		ApacheSSHDServer.resetAuthenticators();
		String methodName = testName.getMethodName();
		LOG.info("\n\t******\n\t Test [" + getClass().getSimpleName() + "." + methodName + "]\n\t******");
	}	
	
	
	/**
	 * Forced clean of the SSHService static cache
	 */
	public void cleanSSHService() {
		try {
			Field f = SSHService.class.getDeclaredField("keyedServices");
			f.setAccessible(true);			
			Map<ServerHostKey, SSHService> keyedServices = (Map<ServerHostKey, SSHService>)f.get(null);
			for(SSHService ssh: keyedServices.values()) {
				try { PrivateAccessor.invoke(ssh, "_close"); } catch (Exception e) {}
			}
			keyedServices.clear();
		} catch (Exception e) {
			LOG.error("Failed to clean SSHService", e);
		}		
	}
	
	/**
	 * Tests various bits and pieces when closing an sshservice
	 * @param ssh the sshservice to close.
	 * @param expectPortForwards Indicates that we know some port forwards were opened
	 */
	public void closeSSHService(SSHService ssh,  boolean expectPortForwards) {
		try {
			Assert.assertNotNull("The SSHService not null", ssh);
			Assert.assertTrue("The SSHService still connected", ssh.isConnected());
			boolean shared = false;
			int sharedCount = ssh.getSharedCount();
			List<LocalPortForward> portForwards = ssh.getLocalPortForwards();
			final AtomicInteger portForwardCloseCounter = new AtomicInteger(ssh.getLocalPortForwardCount());
			Assert.assertEquals("The SSHService has port local forwards", expectPortForwards, portForwardCloseCounter.get()>0);
			if(portForwards.size()>0) {
				Assert.assertTrue("The SSHService had port forwards", expectPortForwards);
				LocalPortForwardStateListener listener = new LocalPortForwardStateListener() {
					public void onClose(LocalPortForward lpf) {
						portForwardCloseCounter.decrementAndGet();
					}
				};
				for(LocalPortForward lpf: ssh.getLocalPortForwards()) {
					lpf.addListener(listener);
				}
			} else {
				Assert.assertFalse("The SSHService had no port forwards", expectPortForwards);
			}
			if(ssh.isSharedConnection()) {
				Assert.assertTrue("The SSHService may have > 1 users", sharedCount>=1);
			} else {
				Assert.assertTrue("The SSHService should have 1 users", sharedCount==1);
			}
			// ==========================================================
			ssh.close();
			// ==========================================================
			portForwards = ssh.getLocalPortForwards();
			Assert.assertEquals("The SSHService share count decremented", (sharedCount-1), ssh.getSharedCount());
			if((sharedCount-1)==0) {
				Assert.assertFalse("SSHService share count decremented to zero, the ssh disconnected", ssh.isConnected());
			}		
			Assert.assertEquals("The SSHService's Local Port Forwards were closed", 0, portForwardCloseCounter.get());
			Assert.assertEquals("The SSHService's Local Port Forwards were cleared from cache", 0, portForwards.size());
			//Assert.assertEquals("The SSHService cached instance count", sharedCount-1, SSHService.getSSHServiceInstanceCount());
		} finally {
			cleanSSHService();
		}
	}

}
