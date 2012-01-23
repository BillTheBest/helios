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

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * <p>Title: ApacheSSHDServer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.ApacheSSHDServer</code></p>
 */
public class ApacheSSHDServer {
	static final Logger LOG = Logger.getLogger(ApacheSSHDServer.class);
	/**
	 * @param args
	 */
	public static void main(String...args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		LOG.info("Starting SSHd Server");
		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(22);
		//SecurityUtils.setSecurityProvider(SecurityUtils.BOUNCY_CASTLE);
		UserAuth userAuth = new UserAuthPassword.Factory().create();
		LOG.info("UserAuth Class:" + userAuth.getClass().getName());
		List<NamedFactory<UserAuth>> authFactories = new ArrayList<NamedFactory<UserAuth>>();
		authFactories.add(new UserAuthNone.Factory());
		sshd.setUserAuthFactories(authFactories);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
		sshd.setShellFactory(new ProcessShellFactory(new String[] { "cmd.exe", "/K" }));
		//Provider provider = new com.sun.crypto.provider.SunJCE();
		Provider provider = new BouncyCastleProvider();
		Security.addProvider(provider);
//		LOG.info(getAlgoList(provider));
//        try {
//            KeyGenerator kg = KeyGenerator.getInstance("SERPENT", provider);
//            Key key = kg.generateKey();
//            
//            LOG.info("Key format: " + key.getFormat() + "  Algorithm:" + key.getAlgorithm() + " Length:" + key.getEncoded().length);            
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }		
//		Provider[] providers = Security.getProviders();
//        for (int i = 0; i < providers.length; i++) {
//            Provider provider = providers[i];
//            System.out.println("Provider:" + provider.getName() + "(" + provider.getVersion() + ")");
//        }		
		try {
			sshd.start();
			LOG.info("Server started");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}
	
	public static String getAlgoList(Provider p) {
		StringBuilder b = new StringBuilder();
		Set<String> types = new HashSet<String>();
		for(Provider.Service svc: p.getServices()) {
			types.add(svc.getType());
			if("KeyGenerator".equals(svc.getType())) 
			b.append("\n\t").append("[").append(svc.getType()).append("] ").append(svc.getAlgorithm());
		}
		System.out.println("Types:\n" + types);
//		for(Map.Entry<Object, Object> entry: p.entrySet()) {
//			String key = (String)entry.getKey();
//			String value = (String)entry.getValue();
//			if(key.startsWith("Cipher."))
//			b.append("\n\t[").append(key).append("]:").append(value);			
//			if(key!=null && key.startsWith("Cipher.") && !key.contains(" ")) {
//				key = key.split("\\.")[1];				
//				b.append("\n\t[").append(key).append("]:").append(value);
//			}
//		}
		return b.toString();
	}

}
