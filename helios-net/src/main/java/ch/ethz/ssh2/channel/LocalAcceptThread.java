
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

import org.helios.net.ssh.instrumentedio.BytesInMetric;
import org.helios.net.ssh.instrumentedio.BytesInProvider;
import org.helios.net.ssh.instrumentedio.BytesOutMetric;
import org.helios.net.ssh.instrumentedio.BytesOutProvider;

/**
 * LocalAcceptThread.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class LocalAcceptThread extends Thread implements IChannelWorkerThread, BytesInProvider, BytesOutProvider
{
	ChannelManager cm;
	String host_to_connect;
	int port_to_connect;
	StreamForwarder r2l = null;
	StreamForwarder l2r = null;
	

	final ServerSocket ss;
	protected volatile LocalAcceptThreadStateListener streamListener = null;

	public LocalAcceptThread(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect, LocalAcceptThreadStateListener streamListener)
			throws IOException
	{
		this.streamListener = streamListener;
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		ss = new ServerSocket(local_port);
	}

	public LocalAcceptThread(ChannelManager cm, InetSocketAddress localAddress, String host_to_connect, int port_to_connect, LocalAcceptThreadStateListener streamListener ) throws IOException
	{
		this.streamListener = streamListener;
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		ss = new ServerSocket();
		ss.bind(localAddress);
	}
	
	public void run() {
		streamListener.onStart();
		try {
			runAccept();
		} catch (Exception e) {			
		}
		streamListener.onStop();
	}

	public void runAccept()
	{
		try
		{
			cm.registerThread(this);
		}
		catch (IOException e)
		{
			stopWorking();
			return;
		}

		while (true)
		{
			Socket s = null;

			try
			{
				s = ss.accept();
			}
			catch (IOException e)
			{
				stopWorking();
				return;
			}

			Channel cn = null;

			try
			{
				/* This may fail, e.g., if the remote port is closed (in optimistic terms: not open yet) */

				cn = cm.openDirectTCPIPChannel(host_to_connect, port_to_connect, s.getInetAddress().getHostAddress(), s
						.getPort());

			}
			catch (IOException e)
			{
				/* Simply close the local socket and wait for the next incoming connection */

				try
				{
					s.close();
				}
				catch (IOException ignore)
				{
				}

				continue;
			}

			try
			{
				r2l = new StreamForwarder(cn, null, null, cn.stdoutStream, s.getOutputStream(), "RemoteToLocal");
				l2r = new StreamForwarder(cn, r2l, s, s.getInputStream(), cn.stdinStream, "LocalToRemote");
			}
			catch (IOException e)
			{
				try
				{
					/* This message is only visible during debugging, since we discard the channel immediatelly */
					cn.cm.closeChannel(cn, "Weird error during creation of StreamForwarder (" + e.getMessage() + ")",
							true);
				}
				catch (IOException ignore)
				{
				}

				continue;
			}

			r2l.setDaemon(true);
			l2r.setDaemon(true);
			r2l.start();
			l2r.start();
		}
	}
	
	

	public void stopWorking()
	{
		try
		{
			/* This will lead to an IOException in the ss.accept() call */
			ss.close();
		}
		catch (IOException e)
		{
		}
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#getChannel()
	 */
	public ServerSocketChannel getChannel() {
		if(ss==null) throw new RuntimeException("ServerSocket is closed");
		return ss.getChannel();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#getInetAddress()
	 */
	public InetAddress getInetAddress() {
		if(ss==null) throw new RuntimeException("ServerSocket is closed");
		return ss.getInetAddress();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#getLocalPort()
	 */
	public int getLocalPort() {
		if(ss==null) throw new RuntimeException("ServerSocket is closed");
		return ss.getLocalPort();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#getLocalSocketAddress()
	 */
	public SocketAddress getLocalSocketAddress() {
		if(ss==null) throw new RuntimeException("ServerSocket is closed");
		return ss.getLocalSocketAddress();
	}

	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.ServerSocket#getReceiveBufferSize()
	 */
	public int getReceiveBufferSize() throws SocketException {
		if(ss==null) throw new RuntimeException("ServerSocket is closed");
		return ss.getReceiveBufferSize();
	}

	/**
	 * @return
	 * @throws SocketException
	 * @see java.net.ServerSocket#getReuseAddress()
	 */
	public boolean getReuseAddress() throws SocketException {
		if(ss==null) throw new RuntimeException("ServerSocket is closed");
		return ss.getReuseAddress();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.ServerSocket#getSoTimeout()
	 */
	public int getSoTimeout() throws IOException {
		if(ss==null) throw new RuntimeException("ServerSocket is closed");
		return ss.getSoTimeout();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#isBound()
	 */
	public boolean isBound() {
		if(ss==null) return false;
		return ss.isBound();
	}

	/**
	 * @return
	 * @see java.net.ServerSocket#isClosed()
	 */
	public boolean isClosed() {
		if(ss==null) return false;
		return ss.isClosed();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.BytesOutProvider#getBytesOutMetric()
	 */
	@Override
	public BytesOutMetric getBytesOutMetric() {
		return l2r.c.getBytesOutMetric();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.net.ssh.instrumentedio.BytesInProvider#getBytesInMetric()
	 */
	@Override
	public BytesInMetric getBytesInMetric() {
		return r2l.c.getBytesInMetric();
	}
}
