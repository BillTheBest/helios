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
package org.helios.nativex.jmx.process;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.helios.helpers.CollectionHelper;

/**
 * <p>Title: ProcessState</p>
 * <p>Description: Enumerates the OS states of processes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.process.ProcessState</code></p>
 */

public enum ProcessState {
	IDLE((char)68),
	RUN((char)82),
	SLEEP((char)83),
	STOP((char)84),
	UNKNOWN((char)85),
	ZOMBIE((char)90);
	
	public static final Map<Character, ProcessState> CODE2ENUM = Collections.unmodifiableMap(new HashMap<Character, ProcessState>(CollectionHelper.createKeyedValueMap(Character.class, ProcessState.class,  
			new Character(IDLE.code), IDLE,
			new Character(RUN.code), RUN,
			new Character(SLEEP.code), SLEEP,
			new Character(STOP.code), STOP,
			new Character(UNKNOWN.code), UNKNOWN,
			new Character(ZOMBIE.code), ZOMBIE
	)));
	
	private ProcessState(char code) {
		this.code = code;
	}
	
	
	private final char code;
	
	/**
	 * Returns the ProcessState for the passed character code
	 * @param c The character code
	 * @return a ProcessState which will be UNKNOWN if the character code is not recognized.
	 */
	public static ProcessState forCode(char c) {
		ProcessState ps = CODE2ENUM.get(c);
		if(ps==null) ps = UNKNOWN;
		return ps;
	}
}
