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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * <p>Title: LocalConfig</p>
 * <p>Description: Reads config from user's home directory in <code>.helios-net.properties</code></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.LocalConfig</code></p>
 */
public class LocalConfig {
	public static String[] read() {
		File f = new File(System.getProperty("user.home") + File.separator + ".helios-net.properties");
		if(!f.exists()) throw new RuntimeException("No file found at [" + f.getAbsolutePath() + "]");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			Properties p = new Properties();
			p.load(fis);
			String[] results = new String[3];
			results[0] = p.getProperty("user.name");
			results[1] = p.getProperty("user.password");
			results[2] = p.getProperty("host.name");
			return results;
		} catch (Exception e) {
			throw new RuntimeException("Failed to read file at [" + f.getAbsolutePath() + "]", e);
		} finally {
			if(fis!=null) try { fis.close(); } catch (Exception e) {}
		}
	}
}
