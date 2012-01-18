
package ch.ethz.ssh2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.channel.LocalAcceptThread;
import ch.ethz.ssh2.channel.LocalAcceptThreadStateListener;

/**
 * A <code>LocalPortForwarder</code> forwards TCP/IP connections to a local
 * port via the secure tunnel to another host (which may or may not be identical
 * to the remote SSH-2 server). Checkout {@link Connection#createLocalPortForwarder(int, String, int)}
 * on how to create one.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class LocalPortForwarder
{
	ChannelManager cm;
	
	protected volatile Set<LocalPortForwarder> forwarderRegistry = null;
	protected volatile LocalAcceptThreadStateListener streamListener = null;
	String host_to_connect;

	int port_to_connect;

	LocalAcceptThread lat;
	
	public void setForwarderRegistry(Set<LocalPortForwarder> forwarderRegistry) {
		if(this.forwarderRegistry!=null) throw new RuntimeException("ForwarderRegistry Already Set");
		if(forwarderRegistry==null) throw new RuntimeException("ForwarderRegistry Cannot Be Null");
		this.forwarderRegistry = forwarderRegistry;
	}

	LocalPortForwarder(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect, LocalAcceptThreadStateListener streamListener )
			throws IOException
	{
		this.streamListener = streamListener;
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(cm, local_port, host_to_connect, port_to_connect, streamListener);
		lat.setDaemon(true);
		lat.setName(new StringBuilder("localhost:").append(local_port).append("-->").append(host_to_connect).append(":").append(port_to_connect).toString());
		lat.start();
	}

	LocalPortForwarder(ChannelManager cm, InetSocketAddress addr, String host_to_connect, int port_to_connect, LocalAcceptThreadStateListener streamListener)
			throws IOException
	{
		this.streamListener = streamListener;
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(cm, addr, host_to_connect, port_to_connect, streamListener);
		lat.setDaemon(true);
		lat.start();
	}
	
	public long getRemoteToLocalBytesTransferred() {
		if(lat!=null) {
			return lat.getRemoteToLocalBytesTransferred();
		} else {
			return 0;
		}
	}
	
	public long getLocalToRemoteBytesTransferred() {
		if(lat!=null) {
			return lat.getLocalToRemoteBytesTransferred();
		} else {
			return 0;
		}
	}
	
	public void resetBytesTransferred() {
		if(lat!=null) {
			lat.resetBytesTransferred();
		}
	}
	

	/**
	 * Stop TCP/IP forwarding of newly arriving connections.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		lat.stopWorking();
		forwarderRegistry.remove(this);
	}
	
	
	/**
	 * @return
	 * @see java.net.ServerSocket#getChannel()
	 */
	public ServerSocketChannel getChannel() {		
		return lat.getChannel();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#getInetAddress()
	 */
	public InetAddress getInetAddress() {
		return lat.getInetAddress();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#getLocalPort()
	 */
	public int getLocalPort() {
		return lat.getLocalPort();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#getLocalSocketAddress()
	 */
	public SocketAddress getLocalSocketAddress() {
		return lat.getLocalSocketAddress();
	}

	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.ServerSocket#getReceiveBufferSize()
	 */
	public int getReceiveBufferSize() throws SocketException {
		return lat.getReceiveBufferSize();
	}

	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.ServerSocket#getReuseAddress()
	 */
	public boolean getReuseAddress() throws SocketException {
		return lat.getReuseAddress();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.ServerSocket#getSoTimeout()
	 */
	public int getSoTimeout() throws IOException {
		return lat.getSoTimeout();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#isBound()
	 */
	public boolean isBound() {
		return lat.isBound();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#isClosed()
	 */
	public boolean isClosed() {
		return lat.isClosed();
	}

	/**
	 * Returns the remote port
	 * @return the port_to_connect
	 */
	public int getRemotePort() {
		return port_to_connect;
	}

	/**
	 * @return the host_to_connect
	 */
	public String getRemoteHost() {
		return host_to_connect;
	}

	/**
	 * @param streamListener the streamListener to set
	 */
	public void setStreamListener(LocalAcceptThreadStateListener streamListener) {
		this.streamListener = streamListener;
	}	
}
