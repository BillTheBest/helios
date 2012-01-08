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
package org.helios.ot.trace.types;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.helios.helpers.ExternalizationHelper;
import org.helios.helpers.StringHelper;
import org.helios.ot.trace.types.interval.ByteArrayIntervalTraceValue;
import org.helios.ot.trace.types.interval.IIntervalTraceValue;
import org.helios.ot.trace.types.interval.IncidentIntervalTraceValue;
import org.helios.ot.trace.types.interval.IntIntervalTraceValue;
import org.helios.ot.trace.types.interval.LongIntervalTraceValue;
import org.helios.ot.trace.types.interval.StringIntervalTraceValue;
import org.helios.ot.trace.types.interval.StringsIntervalTraceValue;
import org.helios.ot.trace.types.interval.TimestampIntervalTraceValue;

/**
 * <p>Title: ITraceValueFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ot.trace.typesvalue.ITraceValueFactory</code></p>
 */

public interface ITraceValueFactory<T extends ITraceValue> {
	public ITraceValue createTraceValue(Object val);
	public ITraceValue createTraceValue(Number val);
	public IIntervalTraceValue<T> createIntervalTraceValue(T...val);
	public Class<? extends ITraceValue> getTraceValueClass();
	
	
	
	
	/**
	 * <p>Title: LongTraceValueFactory</p>
	 * <p>Description:A TraceValue factory associated to an Long TraceValueType </p> 
	 */
	public static class LongTraceValueFactory<T extends LongTraceValue> implements ITraceValueFactory<T> {
		public Class<? extends ITraceValue> getTraceValueClass() {
			return LongTraceValue.class;
		}
		public LongTraceValue createTraceValue(Object val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			if(val instanceof Number) {
				return new LongTraceValue(((Number)val).longValue());
			} else if(val instanceof CharSequence) {
				return new LongTraceValue(Long.parseLong(StringHelper.cleanNumber(val.toString())));
			} else {
				throw new IllegalArgumentException("Passed value [" + val.getClass().getName() + "] could not be converted to a long", new Throwable());
			}
		}
		public LongTraceValue createTraceValue(Number val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new LongTraceValue(val.longValue());
		}
		public LongIntervalTraceValue<T> createIntervalTraceValue(T...traces) {
			return new LongIntervalTraceValue<T>(traces);
		}		
	}
	
	
	
	
	/**
	 * <p>Title: TimestampTraceValueFactory</p>
	 * <p>Description:A TraceValue factory associated to a Timestamp TraceValueType </p> 
	 */
	public static class TimestampTraceValueFactory<T extends TimestampTraceValue> implements ITraceValueFactory<T> {
		public Class<? extends ITraceValue> getTraceValueClass() {
			return TimestampTraceValue.class;
		}
		public TimestampTraceValue createTraceValue(Object val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			if(val instanceof Number) {
				return new TimestampTraceValue(((Number)val).longValue());
			} else if(val instanceof CharSequence) {
				return new TimestampTraceValue(Long.parseLong(StringHelper.cleanNumber(val.toString())));
			} else {
				throw new IllegalArgumentException("Passed value [" + val.getClass().getName() + "] could not be converted to a long", new Throwable());
			}
		}
		public TimestampTraceValue createTraceValue(Number val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new TimestampTraceValue(val.longValue());
		}
		public TimestampIntervalTraceValue<T> createIntervalTraceValue(T...traces) {
			return new TimestampIntervalTraceValue<T>(traces);
		}		
	}
	
	/**
	 * <p>Title: IntTraceValueFactory</p>
	 * <p>Description:A TraceValue factory associated to an Int TraceValueType </p> 
	 */
	public static class IntTraceValueFactory<T extends IntTraceValue> implements ITraceValueFactory<T> {
		public Class<? extends ITraceValue> getTraceValueClass() {
			return IntTraceValue.class;
		}		
		public IntTraceValue createTraceValue(Object val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			if(val instanceof Number) {
				return new IntTraceValue(((Number)val).intValue());
			} else if(val instanceof CharSequence) {
				return new IntTraceValue(Integer.parseInt(StringHelper.cleanNumber(val.toString())));
			} else {
				throw new IllegalArgumentException("Passed value [" + val.getClass().getName() + "] could not be converted to an int", new Throwable());
			}
		}
		public IntTraceValue createTraceValue(Number val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new IntTraceValue(val.intValue());
		}
		public IntIntervalTraceValue<T> createIntervalTraceValue(T...traces) {
			return new IntIntervalTraceValue<T>(traces);
		}		
	}
	
	/**
	 * <p>Title: IncidentTraceValueFactory</p>
	 * <p>Description:A TraceValue factory associated to an Incident TraceValue</p> 
	 */
	public static class IncidentTraceValueFactory<T extends IncidentTraceValue> implements ITraceValueFactory<T> {
		public Class<? extends ITraceValue> getTraceValueClass() {
			return IncidentTraceValue.class;
		}		
		public IncidentTraceValue createTraceValue(Object val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			if(val instanceof Number) {
				return new IncidentTraceValue(((Number)val).intValue());
			} else if(val instanceof CharSequence) {
				return new IncidentTraceValue(Integer.parseInt(StringHelper.cleanNumber(val.toString())));
			} else {
				throw new IllegalArgumentException("Passed value [" + val.getClass().getName() + "] could not be converted to an int", new Throwable());
			}
		}
		public IncidentTraceValue createTraceValue(Number val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new IncidentTraceValue(val.intValue());
		}
		public IncidentIntervalTraceValue<T> createIntervalTraceValue(T...traces) {
			return new IncidentIntervalTraceValue<T>(traces);
		}		
	}
	
	

	/**
	 * <p>Title: StringTraceValueFactory</p>
	 * <p>Description: TraceValue factory for String</p> 
	 */
	public static class StringTraceValueFactory<T extends StringTraceValue> implements ITraceValueFactory<T> {
		public Class<? extends ITraceValue> getTraceValueClass() {
			return StringTraceValue.class;
		}		
		public StringTraceValue createTraceValue(Object val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new StringTraceValue(val.toString());
		}
		public StringTraceValue createTraceValue(Number val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new StringTraceValue(val.toString());
		}
		public StringIntervalTraceValue<T> createIntervalTraceValue(T... traces) {
			return new StringIntervalTraceValue<T>(traces);
		}		
	}
	/**
	 * <p>Title: StringsTraceValueFactory</p>
	 * <p>Description: TraceValue factory for Strings</p> 
	 */
	public static class StringsTraceValueFactory<T extends StringsTraceValue> implements ITraceValueFactory<T> {
		public Class<? extends ITraceValue> getTraceValueClass() {
			return StringsTraceValue.class;
		}		
		public StringsTraceValue createTraceValue(Object val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new StringsTraceValue(val.toString());
		}
		public StringsTraceValue createTraceValue(Number val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new StringsTraceValue(val.toString());
		}
		public StringsIntervalTraceValue<T> createIntervalTraceValue(T... traces) {
			return new StringsIntervalTraceValue<T>(traces);
		}		
	}
	
	/**
	 * <p>Title: ByteArrayTraceValueFactory</p>
	 * <p>Description: TraceValue factory for ByteArrays</p> 
	 */
	public static class ByteArrayTraceValueFactory<T extends ByteArrayTraceValue> implements ITraceValueFactory<T> {
		public Class<? extends ITraceValue> getTraceValueClass() {
			return ByteArrayTraceValue.class;
		}		
		public ByteArrayTraceValue createTraceValue(Object val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			byte[] arr = null;
			if(val instanceof byte[]) {
				arr = (byte[])val;
			} else if(val instanceof Byte[]) {
				Byte[] barr = (Byte[])val;
				arr = new byte[barr.length];
				for(int i = 0; i < barr.length; i++) {
					arr[i] = barr[i];
				}				
			} else if(val instanceof CharSequence) {
				arr = val.toString().getBytes();
			}  else if(val instanceof ByteArrayOutputStream) {
				arr = ((ByteArrayOutputStream)val).toByteArray();
			}  else if(val instanceof InputStream) {
				try {
					InputStream is = (InputStream)val;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int readBytes = -1;
					while((readBytes = is.read(buffer))!=-1) {
						baos.write(buffer, 0, readBytes);
					}
					arr = baos.toByteArray();
				} catch (Exception e) {
					throw new RuntimeException("Failed to read ByteArray from input value", e);
				}
			} else {
				arr = ExternalizationHelper.serialize(val);
			}
			return new ByteArrayTraceValue(arr);
		}
		public ByteArrayTraceValue createTraceValue(Number val) {
			if(val==null) throw new IllegalArgumentException("Passed value was null", new Throwable());
			return new ByteArrayTraceValue(ExternalizationHelper.serialize(val));
		}
		public ByteArrayIntervalTraceValue<T> createIntervalTraceValue(T... traces) {
			return new ByteArrayIntervalTraceValue<T>(traces);
		}		
	}

	
	
}
