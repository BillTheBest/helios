
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

import ch.ethz.ssh2.log.Logger;

/**
 * RemoteAcceptThread.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class RemoteAcceptThread extends Thread
{
	private static final Logger log = Logger.getLogger(RemoteAcceptThread.class);
	
	final AtomicLong bytesTransferredR2L = new AtomicLong(0L);
	final AtomicLong bytesTransferredL2R = new AtomicLong(0L);

	Channel c;

	String remoteConnectedAddress;
	int remoteConnectedPort;
	String remoteOriginatorAddress;
	int remoteOriginatorPort;
	String targetAddress;
	int targetPort;
	StreamForwarder r2l = null;
	StreamForwarder l2r = null;
	
	Socket s;

	public RemoteAcceptThread(Channel c, String remoteConnectedAddress, int remoteConnectedPort,
			String remoteOriginatorAddress, int remoteOriginatorPort, String targetAddress, int targetPort)
	{
		this.c = c;
		this.remoteConnectedAddress = remoteConnectedAddress;
		this.remoteConnectedPort = remoteConnectedPort;
		this.remoteOriginatorAddress = remoteOriginatorAddress;
		this.remoteOriginatorPort = remoteOriginatorPort;
		this.targetAddress = targetAddress;
		this.targetPort = targetPort;

		if (log.isEnabled())
			log.log(20, "RemoteAcceptThread: " + remoteConnectedAddress + "/" + remoteConnectedPort + ", R: "
					+ remoteOriginatorAddress + "/" + remoteOriginatorPort);
	}
	public void run()
	{
		try
		{
			c.cm.sendOpenConfirmation(c);

			s = new Socket(targetAddress, targetPort);

			r2l = new StreamForwarder(c, null, null, c.getStdoutStream(), s.getOutputStream(),
					"RemoteToLocal", bytesTransferredR2L);
			l2r = new StreamForwarder(c, null, null, s.getInputStream(), c.getStdinStream(),
					"LocalToRemote", bytesTransferredL2R);

			/* No need to start two threads, one can be executed in the current thread */
			
			r2l.setDaemon(true);
			r2l.start();
			l2r.run();

			while (r2l.isAlive())
			{
				try
				{
					r2l.join();
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}

			/* If the channel is already closed, then this is a no-op */

			c.cm.closeChannel(c, "EOF on both streams reached.", true);
			s.close();
		}
		catch (IOException e)
		{
			log.log(50, "IOException in proxy code: " + e.getMessage());

			try
			{
				c.cm.closeChannel(c, "IOException in proxy code (" + e.getMessage() + ")", true);
			}
			catch (IOException e1)
			{
			}
			try
			{
				if (s != null)
					s.close();
			}
			catch (IOException e1)
			{
			}
		}
	}
	
	public long getRemoteToLocalBytesTransferred() {
		return bytesTransferredR2L.get();
	}
	
	public long getLocalToRemoteBytesTransferred() {
		return bytesTransferredL2R.get();
	}
	
	public void resetBytesTransferred() {
		bytesTransferredR2L.set(0L);
		bytesTransferredL2R.set(0L);
	}
	
}
