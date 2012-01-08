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
package org.helios.server.ot.jdbc.script;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

/**
 * <p>Title: ScriptExecution</p>
 * <p>Description: Executes SQL scripts </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.jdbc.script.ScriptExecution</code></p>
 */

public class ScriptExecution {
	/** The datasource to get the connection from  */
	protected final DataSource dataSource;
	/** The script source to run */
	protected final String scriptSource;
	
	/**
	 * Creates a new ScriptExecution
	 * @param dataSource The datasource to get the connection from 
	 * @param scriptSource The script source to run
	 */
	public ScriptExecution(DataSource dataSource, String scriptSource) {
		super();
		this.dataSource = dataSource;
		this.scriptSource = scriptSource;
	}
	
	/**
	 * Executes the configured script
	 */
	public void start() {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = dataSource.getConnection();
			ps = conn.prepareStatement(scriptSource);
			ps.execute();
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute script [" + scriptSource + "]", e);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception e) {}
			if(conn!=null) try { conn.close(); } catch (Exception e) {}
		}
	}
	
	
	
	
}
