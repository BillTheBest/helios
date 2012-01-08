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
package org.helios.server.ot.session;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import javax.ws.rs.core.MediaType;

/**
 * <p>Title: OutputFormat</p>
 * <p>Description: Enumerates the options for output format on a subscription</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.session.OutputFormat</code></p>
 */

public enum OutputFormat implements OutputFormatNames {
	/** Plain text output */
	TEXT("text/plain", MediaType.TEXT_PLAIN_TYPE, TEXT_NAME, new TextWriter()),
	/** JSON text output */
	JSON("application/json", MediaType.APPLICATION_JSON_TYPE, JSON_NAME, new JSONWriter()),
	/** XML text output  */
	XML("text/xml", MediaType.TEXT_XML_TYPE, XML_NAME, new XMLWriter()),
	/** Serialized Java Object output */
	JAVA("application/octet-stream", MediaType.APPLICATION_OCTET_STREAM_TYPE, JAVA_NAME, new JavaWriter());
	
	
	/**
	 * Format safe valueOf
	 * @param name The name
	 * @return The output format or null for no match
	 */
	public static OutputFormat forName(CharSequence name) {
		if(name==null) return null;
		try {
			return OutputFormat.valueOf(name.toString().toUpperCase().trim());
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Creates a new OutputFormat
	 * @param mimeType The mime type of the output
	 * @param mediaType The JAX-RS Media Type
	 * @param bean The registry name for the bean that marshalls this output format
	 * @param writer The outputformat writer
	 */
	private OutputFormat(String mimeType, MediaType mediaType, String bean, FormatWriter writer) {
		this.mimeType = mimeType;
		this.mediaType = mediaType;
		this.bean = bean;
		this.writer = writer;
	}
	
	/** The mime type of the output */
	private final String mimeType;
	/** The JAX-RS Media Type */
	private final MediaType mediaType;
	/** The registry name for the bean that marshalls this output format */
	private final String bean;
	/** The outputformat writer */
	private final FormatWriter writer;
	
	/**
	 * Writes a set of byte array represented items to the passed output stream
	 * @param out The output stream
	 * @param sizeHandler The optional size handler
	 * @param items A set of items to write
	 * @throws IOException thrown on any io exception
	 */
	public void writeOut(OutputStream out, OutputSizeHandler sizeHandler, Set<byte[]> items) throws IOException {
		writer.write(out, sizeHandler, items);
	}

	/**
	 * The mime type of the output
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * The JAX-RS Media Type
	 * @return the mediaType
	 */
	public MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * The registry name for the bean that marshalls this output format
	 * @return the bean name
	 */
	public String getBeanName() {
		return bean;
	}
	
	/**
	 * <p>Title: OutputSizeHandler</p>
	 * <p>Description: Defines a handler that does something specific with the calculated byte stream size</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.OutputFormat.OutputSizeHandler</code></p>
	 */
	public static interface OutputSizeHandler {
		/**
		 * Handles the calculated size of the byte stream
		 * @param size The calculated size of the byte stream
		 */
		public void writeSize(int size);
	}
	
	/**
	 * <p>Title: FormatWriter</p>
	 * <p>Description: Aggregates and writes out a set of byte arrays using the OutputFormat specific delimeters.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.OutputFormat.FormatWriter</code></p>
	 */
	public static abstract class FormatWriter {
		/**
		 * Aggregates and writes out a set of byte arrays using the OutputFormat specific delimeters.
		 * @param out The output stream to write to
		 * @param sizeHandler The output size handler  (ignored if null)
		 * @param items A set of byte array item representations to write
		 * @throws IOException Thrown if an IOException occurs
		 */
		public abstract void write(OutputStream out, OutputSizeHandler sizeHandler, Set<byte[]> items) throws IOException;
		
		/**
		 * The output stream write implementation
		 * @param out The output stream
		 * @param sizeHandler The optional size handler
		 * @param items A set of byte arrays to write
		 * @param starter The format starter
		 * @param ender The format ender
		 * @param startDelimiter The format item start delimiter
		 * @param endDelimiter The format item end delimiter
		 * @param starterEnderSize The size of the starter and ender
		 * @param delimiterSize The size of a delimiter pair
		 * @throws IOException Thrown on any IO exceptions
		 */
		protected void writeImpl(OutputStream out, OutputSizeHandler sizeHandler, Set<byte[]> items, byte[] starter, byte[] ender, byte[] startDelimiter, byte[] endDelimiter,  int starterEnderSize, int delimiterSize) throws IOException {
			if(!(out instanceof BufferedOutputStream)) {
				out = new BufferedOutputStream(out);
			}
			int size = starterEnderSize;
			int cnt = 0;
			
			for(byte[] bytes: items) {
				size += bytes.length;
				cnt++;
			}
			int lastItem = cnt-1;
			if(cnt>1) {
				size += (lastItem*delimiterSize);
			}
			if(sizeHandler!=null) {
				sizeHandler.writeSize(size);
			}
			out.write(starter);
			int itemIndex = 0;
			
			for(byte[] item: items) {
				out.write(startDelimiter);
				out.write(item);
				if(itemIndex!=lastItem) {
					out.write(endDelimiter);
				}
				itemIndex++;
			}
			out.write(ender);			
			out.flush();

		}
	}
	
	/**
	 * <p>Title: JSONWriter</p>
	 * <p>Description: FormatWriter for JSON feeds.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.OutputFormat.JSONWriter</code></p>
	 */
	public static class JSONWriter extends FormatWriter {

		/** The JSON aggregate starter */
		public static final byte[] STARTER = "{\"batch\":[".getBytes();
		/** The JSON aggregate ender */
		public static final byte[] ENDER = "]}".getBytes();
		/** The JSON aggregate item starter delimeter */
		public static final byte[] SDELIM = {};		
		/** The JSON aggregate item ender delimeter */
		public static final byte[] EDELIM = ",".getBytes();
		/** The size of the JSON starter end ender */
		public static final int SIZE = STARTER.length + ENDER.length;
		/** The size of JSON delimiter pair */
		public static final int DSIZE = SDELIM.length + EDELIM.length;

		/**
		 * {@inheritDoc}
		 * @see org.helios.server.ot.session.OutputFormat.FormatWriter#write(java.io.OutputStream, java.util.Set)
		 */
		@Override
		public void write(OutputStream out, OutputSizeHandler sizeHandler, Set<byte[]> items) throws IOException {
			writeImpl(out, sizeHandler, items, STARTER, ENDER, SDELIM, EDELIM, SIZE, DSIZE);
		}
		
	}
	
	/**
	 * <p>Title: XMLWriter</p>
	 * <p>Description: FormatWriter for XML feeds.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.OutputFormat.JSONWriter</code></p>
	 */
	public static class XMLWriter extends FormatWriter {

		/** The XML aggregate starter */
		public static final byte[] STARTER = "<batch>".getBytes();
		/** The XML aggregate ender */
		public static final byte[] ENDER = "</batch>".getBytes();
		/** The XML aggregate item starter delimeter */
		public static final byte[] SDELIM = "<subbatch>".getBytes();		
		/** The XML aggregate item ender delimeter */
		public static final byte[] EDELIM = "</subbatch>".getBytes();
		/** The size of the XML starter end ender */
		public static final int SIZE = STARTER.length + ENDER.length;
		/** The size of XML delimiter pair */
		public static final int DSIZE = SDELIM.length + EDELIM.length;

		/**
		 * {@inheritDoc}
		 * @see org.helios.server.ot.session.OutputFormat.FormatWriter#write(java.io.OutputStream, java.util.Set)
		 */
		@Override
		public void write(OutputStream out, OutputSizeHandler sizeHandler, Set<byte[]> items) throws IOException {
			writeImpl(out, sizeHandler, items, STARTER, ENDER, SDELIM, EDELIM, SIZE, DSIZE);
		}
		
	}
	
	/**
	 * <p>Title: TextWriter</p>
	 * <p>Description: FormatWriter for Text feeds.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.OutputFormat.JSONWriter</code></p>
	 */
	public static class TextWriter extends FormatWriter {

		/** The XML aggregate starter */
		public static final byte[] STARTER = {};
		/** The XML aggregate ender */
		public static final byte[] ENDER = "\n".getBytes();
		/** The XML aggregate item starter delimeter */
		public static final byte[] SDELIM = {};		
		/** The XML aggregate item ender delimeter */
		public static final byte[] EDELIM = "\n".getBytes();
		/** The size of the XML starter end ender */
		public static final int SIZE = STARTER.length + ENDER.length;
		/** The size of XML delimiter pair */
		public static final int DSIZE = SDELIM.length + EDELIM.length;

		/**
		 * {@inheritDoc}
		 * @see org.helios.server.ot.session.OutputFormat.FormatWriter#write(java.io.OutputStream, java.util.Set)
		 */
		@Override
		public void write(OutputStream out, OutputSizeHandler sizeHandler, Set<byte[]> items) throws IOException {
			writeImpl(out, sizeHandler, items, STARTER, ENDER, SDELIM, EDELIM, SIZE, DSIZE);
		}
		
	}
	
	/**
	 * <p>Title: JavaWriter</p>
	 * <p>Description: FormatWriter for Text feeds.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.session.OutputFormat.JSONWriter</code></p>
	 */
	public static class JavaWriter extends FormatWriter {

		/** The XML aggregate starter */
		public static final byte[] STARTER = {};
		/** The XML aggregate ender */
		public static final byte[] ENDER = {};
		/** The XML aggregate item starter delimeter */
		public static final byte[] SDELIM = {};		
		/** The XML aggregate item ender delimeter */
		public static final byte[] EDELIM = {};
		/** The size of the XML starter end ender */
		public static final int SIZE = 0;
		/** The size of XML delimiter pair */
		public static final int DSIZE = 0;

		/**
		 * {@inheritDoc}
		 * @see org.helios.server.ot.session.OutputFormat.FormatWriter#write(java.io.OutputStream, java.util.Set)
		 */
		@Override
		public void write(OutputStream out, OutputSizeHandler sizeHandler, Set<byte[]> items) throws IOException {
			writeImpl(out, sizeHandler, items, STARTER, ENDER, SDELIM, EDELIM, SIZE, DSIZE);
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	
}
