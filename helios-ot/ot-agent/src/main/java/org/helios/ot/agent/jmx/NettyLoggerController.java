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
package org.helios.ot.agent.jmx;

import org.apache.log4j.Logger;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXNotification;
import org.helios.jmx.dynamic.annotations.JMXNotificationType;
import org.helios.jmx.dynamic.annotations.JMXNotifications;
import org.helios.jmxenabled.logging.LoggerControl;
import org.helios.ot.agent.HeliosOTClient;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * <p>Title: NettyLoggerController</p>
 * <p>Description: LoggerController extended to be able to control the logging channel handler in the pipelines.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.agent.jmx.NettyLoggerController</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
@JMXNotifications(notifications={
        @JMXNotification(description="Channel Event Logging", types={
        		// "state", "message", "exception", "write", "idle", "other"
                @JMXNotificationType(type="state"),
                @JMXNotificationType(type="message"),
                @JMXNotificationType(type="exception"),
                @JMXNotificationType(type="write"),
                @JMXNotificationType(type="idle"),
                @JMXNotificationType(type="other")
        })
})

public class NettyLoggerController extends LoggerControl {
	/** THe netty pipeline to put the logging handler in and out of */
	protected volatile ChannelPipeline pipeline = null;
	/**
	 * Creates a new NettyLoggerController
	 * @param targetLogger The target logger to control
	 */
	public NettyLoggerController(Logger targetLogger) {
		super(targetLogger);
	}
	/**
	 * Returns the pipeline
	 * @return the pipeline
	 */
	public ChannelPipeline getPipeline() {
		return pipeline;
	}
	/**
	 * Sets the pipeline
	 * @param pipeline the pipeline to set
	 */
	public void setPipeline(ChannelPipeline pipeline) {
		this.pipeline = pipeline;
	}

}
