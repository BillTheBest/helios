package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.OutputStream;

import org.helios.net.ssh.instrumentedio.BytesOutMetric;
import org.helios.net.ssh.instrumentedio.BytesOutProvider;
import org.helios.net.ssh.instrumentedio.StreamInstrumentationSupport;

/**
 * ChannelOutputStream.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public final class ChannelOutputStream extends OutputStream implements BytesOutProvider {
	Channel c;
	protected final StreamInstrumentationSupport instr;
	boolean isClosed = false;
	
	ChannelOutputStream(Channel c)
	{
		this.c = c;
		instr  = new StreamInstrumentationSupport();
	}
	
	ChannelOutputStream(Channel c, StreamInstrumentationSupport instr)
	{
		this.c = c;
		this.instr = instr==null ? new StreamInstrumentationSupport() : instr;
	}
	
	
	public BytesOutMetric getBytesOutMetric() {
		return instr;
	}
	

	public void write(int b) throws IOException
	{	
		write(new byte[]{(byte)b}, 0, 1);		
	}

	public void close() throws IOException
	{
		if (isClosed == false)
		{
			isClosed = true;
			c.cm.sendEOF(c);
		}
	}

	public void flush() throws IOException
	{
		if (isClosed)
			throw new IOException("This OutputStream is closed.");

		/* This is a no-op, since this stream is unbuffered */
	}

	public void write(byte[] b, int off, int len) throws IOException
	{
		if (isClosed)
			throw new IOException("This OutputStream is closed.");
		
		if (b == null)
			throw new NullPointerException();

		if ((off < 0) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0) || (off > b.length))
			throw new IndexOutOfBoundsException();

		if (len == 0)
			return;
		
		c.cm.sendData(c, b, off, len);
		if(ChannelManager.instrumentedChannels)instr.write(len);
	}

	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);		
	}
}
