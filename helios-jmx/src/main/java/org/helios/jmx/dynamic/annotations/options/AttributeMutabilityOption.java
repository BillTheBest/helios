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
package org.helios.jmx.dynamic.annotations.options;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: AttributeMutability</p>
 * <p>Description: Defines the read/write mutability of an MBean attribute.
 * These are the current states<ul>
 * <li>READ_ONLY: The attribute can only be read and not writen.
 * <li>WRITE_ONLY: The attribute can only be writen and not read.
 * <li>READ_WRITE: The attribute can be read or writen.
 * <li>WRITE_ONCE: The attribute can be read, but only writen once.
 * </ul></p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */

public enum AttributeMutabilityOption {
	
	
	
	/**The attribute can only be read and not writen. */
	READ_ONLY(0), 
	/**The attribute can only be writen and not read. */
	WRITE_ONLY(1), 
	/**The attribute can be read, but only writen once. */
	WRITE_ONCE(2), 
	/**The attribute can be read or writen. <code>DEFAULT</code>*/
	READ_WRITE(3);
	
	public static final Map<String, AttributeMutabilityOption> comp = new HashMap<String, AttributeMutabilityOption>(15);
	
	static {
		// ==========================================
		// Map Pattern: getter + setter --> option
		// ==========================================				
		comp.put(AttributeMutabilityOption.READ_ONLY.toString() + AttributeMutabilityOption.READ_ONLY.toString(), AttributeMutabilityOption.READ_ONLY);
		comp.put(AttributeMutabilityOption.READ_ONLY.toString() + AttributeMutabilityOption.WRITE_ONLY.toString(), AttributeMutabilityOption.READ_WRITE);
		comp.put(AttributeMutabilityOption.READ_ONLY.toString() + AttributeMutabilityOption.READ_WRITE.toString(), AttributeMutabilityOption.READ_WRITE);
		comp.put(AttributeMutabilityOption.READ_ONLY.toString() + AttributeMutabilityOption.WRITE_ONCE.toString(), AttributeMutabilityOption.WRITE_ONCE);
		comp.put(AttributeMutabilityOption.READ_ONLY.toString() + "null", AttributeMutabilityOption.READ_ONLY);
		//=====================
		comp.put(AttributeMutabilityOption.WRITE_ONLY.toString() + AttributeMutabilityOption.WRITE_ONLY.toString(), AttributeMutabilityOption.WRITE_ONLY);
		comp.put(AttributeMutabilityOption.WRITE_ONLY.toString() + AttributeMutabilityOption.WRITE_ONCE.toString(), AttributeMutabilityOption.WRITE_ONCE);
		//=====================
		comp.put(AttributeMutabilityOption.READ_WRITE.toString() + AttributeMutabilityOption.WRITE_ONLY.toString(), AttributeMutabilityOption.WRITE_ONLY);
		comp.put(AttributeMutabilityOption.READ_WRITE.toString() + AttributeMutabilityOption.READ_WRITE.toString(), AttributeMutabilityOption.READ_WRITE);
		comp.put(AttributeMutabilityOption.READ_WRITE.toString() + AttributeMutabilityOption.WRITE_ONCE.toString(), AttributeMutabilityOption.WRITE_ONCE);
		//=====================
		comp.put(AttributeMutabilityOption.WRITE_ONCE.toString() + AttributeMutabilityOption.READ_ONLY.toString(), AttributeMutabilityOption.READ_ONLY);
		comp.put(AttributeMutabilityOption.WRITE_ONCE.toString() + AttributeMutabilityOption.WRITE_ONLY.toString(), AttributeMutabilityOption.WRITE_ONLY);
		comp.put(AttributeMutabilityOption.WRITE_ONCE.toString() + AttributeMutabilityOption.READ_WRITE.toString(), AttributeMutabilityOption.READ_WRITE);
		comp.put(AttributeMutabilityOption.WRITE_ONCE.toString() + AttributeMutabilityOption.WRITE_ONCE.toString(), AttributeMutabilityOption.WRITE_ONCE);
		//=====================
		comp.put("null" + AttributeMutabilityOption.WRITE_ONLY.toString(), AttributeMutabilityOption.WRITE_ONLY);
	}
	
    /** the index of this unit */
    private final int index;

    /** Internal constructor */
    AttributeMutabilityOption(int index) { 
        this.index = index;
    }
    
    /**
     * The default attribute mutability.  
     * @return
     */
    public AttributeMutabilityOption DEFAULT() {
    	return READ_WRITE;
    }     
    
    
    /**
     * A convenience method to determine if the mutability allows reads.
     * @return 
     */
    public boolean isReadable() {
    	return (index==READ_ONLY.index || index==WRITE_ONCE.index || index==READ_WRITE.index);  
    }

    /**
     * A convenience method to determine if the mutability allows writes.
     * @return
     */
    public boolean isWritable() {
    	return (index==WRITE_ONLY.index || index==WRITE_ONCE.index || index==READ_WRITE.index);  
    }
    
    
    /**
     * Determines the overriding mutability of an attribute based on the annotated attribute mutability option of both the getter and setter methods.
	 * <table border=1>
	 *  <tr ><td align="center" colspan="7">Setter Mutability</td></tr>
	 *  <tr ><td rowspan="6">Getter Mutability</td> <td style="BACKGROUND-COLOR: #000000;">&nbsp;</td> <td style="BACKGROUND-COLOR: #c0c0c0;">READ</td> <td style="BACKGROUND-COLOR: #c0c0c0;">WRITE</td> <td style="BACKGROUND-COLOR: #c0c0c0;">READ_WRITE</td> <td style="BACKGROUND-COLOR: #c0c0c0;">WRITE_ONCE</td><td style="BACKGROUND-COLOR: #c0c0c0;">Null</td></tr>
	 *  <tr ><td style="BACKGROUND-COLOR: #c0c0c0;">READ</td> <td>READ</td><td >READ_WRITE</td> <td >READ_WRITE</td> <td>WRITE_ONCE</td><td >READ</td> </tr>
	 *  <tr ><td style="BACKGROUND-COLOR: #c0c0c0;">WRITE</td><td >Error</td><td >WRITE</td><td >Error</td><td >WRITE_ONCE</td> <td >Error</td></tr>
	 *  <tr ><td style="BACKGROUND-COLOR: #c0c0c0;">READ_WRITE</td><td >Error</td><td >READ_WRITE</td><td >READ_WRITE</td><td >WRITE_ONCE</td><td >Error</td></tr>
	 *  <tr ><td style="BACKGROUND-COLOR: #c0c0c0;">WRITE_ONCE</td><td >READ</td><td >WRITE</td><td >READ_WRITE</td><td >WRITE_ONCE</td><td >Error</td></tr>
	 *  <tr ><td style="BACKGROUND-COLOR: #c0c0c0;">Null</td><td >Error</td><td >WRITE</td><td >Error</td> <td >Error</td><td >Error</td></tr>
	 * </table>   
     * @param getter
     * @param setter
     * @return the compromised mutability option.
     */
    public static AttributeMutabilityOption selectOption(AttributeMutabilityOption getter, AttributeMutabilityOption setter) {
		AttributeMutabilityOption ret = comp.get(
			(getter==null ? "null" : getter.toString()) + 
			(setter==null ? "null" : setter.toString())
		);
		//if(ret==null) throw new RuntimeException("Conflicting Mutability Options:[" + getter + "]/[" + setter + "]");
		return ret;		
    }
    
	
}
