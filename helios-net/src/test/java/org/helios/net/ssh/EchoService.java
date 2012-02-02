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

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * <p>Title: EchoService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.EchoService</code></p>
 */
public class EchoService {
	static volatile NioSocketAcceptor acceptor = null;
	static final Logger LOG = Logger.getLogger(EchoService.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Port:" + start());
			Thread.sleep(5000);
			stop();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}
	
	
	public static void stop() {
		if(acceptor!=null) {
			System.out.println("Stopping EchoService");
			acceptor.unbind();
			acceptor.dispose();
			acceptor = null;
			LOG.info("Stopped EchoService");
			
		}
	}
	
	public static int start() throws IOException {
		if(acceptor!=null) throw new IllegalStateException("Acceptor already started");
		LOG.info("Starting EchoService");
        acceptor = new NioSocketAcceptor();
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                IoBuffer recv = (IoBuffer) message;
                IoBuffer sent = IoBuffer.allocate(recv.remaining());
                sent.put(recv);                
                sent.flip();
                session.write(sent);                
            }
        });
        acceptor.setReuseAddress(true);
        InetSocketAddress sockAdd = new InetSocketAddress(0);
        acceptor.bind(sockAdd);
        acceptor.setCloseOnDeactivation(true);
        int port = acceptor.getLocalAddress().getPort();
        LOG.info("Started EchoService on [" + acceptor.getLocalAddress().getHostName() + ":" + port +  "]");
        return port;
               
	}

}
