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

/**
 * <p>Title: AsynchCompletionNotifyOption</p>
 * <p>Description: Defines the options available for specifying how to handle the completion of an asynch operation.
 * The options are:<ul>
 * <li>NOTIFY: Emit a notification only.
 * <li>RETURN: Return the applicable completion object only.
 * <li>BOTH: Return and notify.
 * <li>EXCEPTION_ONLY_NOTIFY: Only notify if the operation throws an exception.
 * <li>EXCEPTION_ONLY_RETURN: Only return if the operation throws an exception.
 * <li>EXCEPTION_ONLY_BOTH: Return and notify if the operation throws an exception.
 * </ul>
 * </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $Revision$
 */


public enum AsynchCompletionNotifyOption {
	/**Emit a notification only.*/
	NOTIFY(0), 
	/**Return the applicable completion object only. */
	RETURN(1), 
	/**Return and notify. <code>DEFAULT</code>*/
	BOTH(2), 
	/**Only notify if the operation throws an exception. */
	EXCEPTION_ONLY_NOTIFY(3), 
	/**Only return if the operation throws an exception. */
	EXCEPTION_ONLY_RETURN(4), 
	/**Return and notify if the operation throws an exception.  */
	EXCEPTION_ONLY_BOTH(5);
	
    /** the index of this unit */
    private final int index;

    /** Internal constructor */
    AsynchCompletionNotifyOption(int index) { 
        this.index = index; 
    }
    
    /**
     * The default notification type.  
     * @return
     */
    public AsynchCompletionNotifyOption DEFAULT() {
    	return RETURN;
    }    
    
    /**
     * Determines if the option involves some form of notification.
     * @return true if a notification will be emitted.
     */
    public boolean isNotify() {
    	return (index==NOTIFY.index || index==BOTH.index || index==EXCEPTION_ONLY_NOTIFY.index || index==EXCEPTION_ONLY_BOTH.index);
    }
    
    /**
     * Determines if the option involves only a notification.
     * @return true if only notification will be emitted.
     */
    public boolean isNotifyOnly() {
    	return (index==NOTIFY.index || index==EXCEPTION_ONLY_NOTIFY.index);
    }
    
    /**
     * Determines if the option involves a return.
     * @return true if a return will be made.
     */
    public boolean isReturn() {
    	return (index==BOTH.index || index==RETURN.index || index==EXCEPTION_ONLY_BOTH.index || index==EXCEPTION_ONLY_RETURN.index);
    }
    
    /**
     * Determines if the option involves only a return.
     * @return true if only a return.
     */
    public boolean isReturnOnly() {
    	return (index==RETURN.index || index==EXCEPTION_ONLY_RETURN.index);
    }    
    
    
    /**
     * Determines if only an exception will be returned or notified.
     * @return true if only exceptions are handled.
     */
    public boolean isExceptionOnly() {
    	return (index==EXCEPTION_ONLY_RETURN.index || index==EXCEPTION_ONLY_NOTIFY.index || index==EXCEPTION_ONLY_BOTH.index);
    }
    

}
