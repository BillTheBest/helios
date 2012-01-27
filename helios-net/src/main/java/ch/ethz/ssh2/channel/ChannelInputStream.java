
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.InputStream;

import org.helios.net.ssh.instrumentedio.BytesInMetric;
import org.helios.net.ssh.instrumentedio.BytesInProvider;
import org.helios.net.ssh.instrumentedio.StreamInstrumentationSupport;

/**
 * ChannelInputStream.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public final class ChannelInputStream extends InputStream implements BytesInProvider {
	Channel c;
	protected final StreamInstrumentationSupport instr;
	
	boolean isClosed = false;
	boolean isEOF = false;
	boolean extendedFlag = false;
	
	public BytesInMetric getBytesInMetric() {
		return instr;
	}

	ChannelInputStream(Channel c, boolean isExtended)
	{
		this.c = c;
		this.extendedFlag = isExtended;
		instr  = new StreamInstrumentationSupport();
	}
	
	ChannelInputStream(Channel c, boolean isExtended, StreamInstrumentationSupport instr)
	{
		this.c = c;
		this.extendedFlag = isExtended;
		this.instr  = instr==null ? new StreamInstrumentationSupport() : instr;
	}
	

	public int available() throws IOException
	{
		if (isEOF)
			return 0;

		int avail = c.cm.getAvailable(c, extendedFlag);

		/* We must not return -1 on EOF */

		return (avail > 0) ? avail : 0;
	}

	public void close() throws IOException
	{
		isClosed = true;
	}

	public int read(byte[] b, int off, int len) throws IOException
	{
		if (b == null)
			throw new NullPointerException();

		if ((off < 0) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0) || (off > b.length))
			throw new IndexOutOfBoundsException();

		if (len == 0)
			return 0;

		if (isEOF)
			return -1;

		int ret = c.cm.getChannelData(c, extendedFlag, b, off, len);

		if (ret == -1)
		{
			isEOF = true;
		}
		
		if(ChannelManager.instrumentedChannels)instr.read(ret);
		return ret;
	}

	public int read(byte[] b) throws IOException
	{
		
		int read = read(b, 0, b.length);
		return read;
	}

	public int read() throws IOException
	{
		/* Yes, this stream is pure and unbuffered, a single byte read() is slow */

		final byte b[] = new byte[1];

		int ret = read(b, 0, 1);

		if (ret != 1)
			return -1;		
		return b[0] & 0xff;
	}
}
