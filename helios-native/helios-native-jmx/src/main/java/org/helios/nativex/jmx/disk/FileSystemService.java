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
package org.helios.nativex.jmx.disk;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.DoubleRollingCounter;
import org.helios.jmxenabled.counters.IntegerRollingCounter;
import org.helios.jmxenabled.counters.LongDeltaRollingCounter;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;

/**
 * <p>Title: FileSystemService</p>
 * <p>Description: MBean service created to monitor a file system </p>
 * <p>By default, if the <b><code>{@link FileSystemType}</code></b> is  <b><code>TYPE_NONE</code></b>, meaning the file system type is not recognized,
 * the discovered file system will not have a monitor created for it. This behaviour can be overriden by setting the system property 
 * <b><code>helios.filesystems.enable.unrecognized</code></b> to case-ins <b><code>true</code></b>  in which case all discovered file systems
 * will be monitored by default.</p> 
 * <p>Alternatively, any recognized file system will have a monitor created for it, and the published MBeans for each file system can create 
 * a monitor for an unrecognized (or recently added) file system by invoking the <b><code>registerNewFileSystem</code></b> JMX operation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.nativex.jmx.disk.FileSystemService</code></p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class FileSystemService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = 7429775590478468000L;
	/** The file system root */
	protected final String deviceName;
	/** the filesystem instance for this file system */
	protected final FileSystem fileSystem;
	
	/** the filesystem usage object for this file system */
	protected final FileSystemUsage usage;
	
	/** The file system device name */
	protected final String devName;
	/** The file system directory name */
	protected final String dirName;
	/** The file system options */
	protected final String options;
	/** The file system type */
	protected final String type;
	/** The file system category */
	protected final String category;
	
	/** Disk Writes Counter */
	protected final LongDeltaRollingCounter diskWritesCounter = new LongDeltaRollingCounter("DiskWrites", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Disk Writes Bytes Counter */
	protected final LongDeltaRollingCounter diskWriteBytesCounter = new LongDeltaRollingCounter("DiskWriteBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Disk Reads Counter */
	protected final LongDeltaRollingCounter diskReadsCounter = new LongDeltaRollingCounter("DiskReads", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Disk Read Bytes Counter */
	protected final LongDeltaRollingCounter diskReadBytesCounter = new LongDeltaRollingCounter("DiskReadBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Available KBytes Counter */
	protected final LongRollingCounter availableCounter = new LongRollingCounter("AvailableKBytes", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Available Disk Queue Counter */
	protected final DoubleRollingCounter diskQueueCounter = new DoubleRollingCounter("DiskQueue", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Disk Service Time Counter */
	protected final DoubleRollingCounter diskServiceTimeCounter = new DoubleRollingCounter("DiskServiceTime", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Disk Space Free Percent Counter */
	protected final IntegerRollingCounter diskSpaceFreeCounter = new IntegerRollingCounter("DiskSpaceFree", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Disk Space Used Percent Counter */
	protected final IntegerRollingCounter diskSpaceUsedCounter = new IntegerRollingCounter("DiskSpaceUsed", DEFAULT_ROLLING_SIZE, registerGroup);
	/** Time until full Counter */
	protected final LongRollingCounter timeUntilFullCounter = new LongRollingCounter("SecondsUntilFull", DEFAULT_ROLLING_SIZE, registerGroup);
	
	/** The timestamp of the last sample */
	protected final AtomicReference<Long> lastSampleTs = new AtomicReference<Long>(null); 
	
	/** System property or environmental variable name to enable monitoring of unrecognized file system types */
	public static final String ENABLE_NONE_PROP = "helios.filesystems.enable.unrecognized";
	

	/**
	 * Creates a new FileSystemService with a derived file system usage scheme 
	 * @param deviceName The file system root name
	 */
	public FileSystemService(String deviceName) {
		this(deviceName, null);
	}

	
	/**
	 * Creates a new FileSystemService 
	 * @param deviceName The file system root name
	 * @param fsu The designated FileSystemUsage gatherer
	 */
	public FileSystemService(String deviceName, FileSystemUsage fsu) {
		super();
		this.deviceName = deviceName;
		FileSystemType type = null;
		String sysType = null;
		fileSystem = HeliosSigar.getInstance().getFileSystemMap().getFileSystem(deviceName);
		type = FileSystemType.typeForCode(fileSystem.getType());
		sysType = fileSystem.getSysTypeName();
		if(fsu==null) {
			usage = type.equals(FileSystemType.TYPE_NETWORK) ? HeliosSigar.getInstance().getMountedFileSystemUsage(deviceName) :  HeliosSigar.getInstance().getFileSystemUsage(deviceName);
		} else {
			usage = fsu;
		}
		devName = fileSystem.getDevName();
		dirName = fileSystem.getDirName();
		options = fileSystem.getOptions();
		this.type = fileSystem.getSysTypeName();
		category = FileSystemType.typeForCode(fileSystem.getType()).name(); 			
		this.scheduleSampling();
		registerCounterMBean("fileSystem", deviceName, "category", type.getNiceName(), "type", sysType);
		initPerfCounters();
	}
	
	/**
	 * Manually adds a new file system that was not picked up on boot
	 * @param name The name of the file system or disk.
	 * @return true if successful
	 */
	@JMXOperation(name="registerNewFileSystem", description="Manually adds a new file system that was not picked up on boot")
	public boolean registerNewFileSystem(@JMXParameter(name="deviceName", description="The name of the file system or disk")String deviceName) {
		if(deviceName==null || "".equals(deviceName)) return false;
		try {
			new FileSystemService(deviceName);
			return true;
		} catch (Exception e) {
			log.warn("Failed to register new file system [" + deviceName + "]", e);
			return false;
		}
	}
	
	/**
	 * Executes the sampling for this counter.
	 */
	public void run() {
		try {
			usage.gather(sigar, deviceName);
			diskWritesCounter.put(usage.getDiskWrites());
			diskWriteBytesCounter.put(usage.getDiskWriteBytes());
			diskReadsCounter.put(usage.getDiskReads());
			diskReadBytesCounter.put(usage.getDiskReadBytes());
			availableCounter.put(usage.getAvail());
			diskQueueCounter.put(usage.getDiskQueue());
			diskServiceTimeCounter.put(usage.getDiskServiceTime());
			int usedPercent = doubleToIntPercent(usage.getUsePercent());
			diskSpaceFreeCounter.put(100-usedPercent);			
			diskSpaceUsedCounter.put(usedPercent);
			if(lastSampleTs.get()!=null) {
				timeUntilFullCounter.put(secondsUntilFull());
			}
			lastSampleTs.set(System.currentTimeMillis());
			//
			
			
		} catch (Exception e) {
			log.error("Failed to gather on service [" + objectName + "]", e);
			throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
		}
	}
	
	/**
	 * Returns the estimated time (in s) until the disk is full based on the rate of writes.
	 * @return the estimated number of seconds until this device is full
	 */
	protected long secondsUntilFull() {
		long avgWrites = diskWriteBytesCounter.getAverage();
		long available = availableCounter.getLastValue();
		long elapsed = System.currentTimeMillis() - lastSampleTs.get();
		if(available<1) return 0;
		if(avgWrites==0) return -1;
		long writesLeft = available / avgWrites;
		long msLeft = writesLeft * elapsed;
		return TimeUnit.SECONDS.convert(msLeft, TimeUnit.MILLISECONDS);
		
	}
	
	/**
	 * Returns the Total free Kbytes on filesystem available to caller.
	 * @return free Kbytes 
	 */
	@JMXAttribute(name="Available", description="The Total free Kbytes on filesystem available to caller", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getAvailable() {
		return availableCounter.getLastValue();
	}
	
	/**
	 * Returns the Disk Queue Depth
	 * @return the Disk Queue Depth
	 */
	@JMXAttribute(name="DiskQueue", description="The Disk Queue Depth", mutability=AttributeMutabilityOption.READ_ONLY)
	public double getDiskQueue() {
		return diskQueueCounter.getLastValue();
	}

	/**
	 * Returns the rate of Physical Disk Reads
	 * @return the rate of Physical Disk Reads
	 */
	@JMXAttribute(name="DiskReadRate", description="The Rate of Physical Disk Reads", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getDiskReads() {
		return diskReadsCounter.getLastValue();
	}
	
	/**
	 * Returns the rate of Physical Bytes Read
	 * @return the rate of Physical Bytes Read
	 */
	@JMXAttribute(name="DiskReadBytesRate", description="The Rate of Physical Bytes Read", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getDiskReadBytes() {
		return diskReadBytesCounter.getLastValue();
	}
	
	/**
	 * Returns the rate of Physical Disk Writes
	 * @return the rate of Physical Disk Writes
	 */
	@JMXAttribute(name="DiskWriteRate", description="The Rate of Physical Disk Writes", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getDiskWrites() {
		return diskWritesCounter.getLastValue();
	}
	
	/**
	 * Returns the estimated time in seconds until the disk is full
	 * @return the estimated time in seconds until the disk is full. -1 is returned if no estimate has been calculated yet.
	 */
	@JMXAttribute(name="SecondsUntilFull", description="The estimated time in seconds until the disk is full", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSecondsUntilFull() {
		if(timeUntilFullCounter.getSize()<1) {
			return -1;
		}
		return timeUntilFullCounter.getLastValue();
	}
	
	
	/**
	 * Returns the rate of Physical Bytes Written
	 * @return the rate of Physical Bytes Written
	 */
	@JMXAttribute(name="DiskWriteBytesRate", description="The Rate of Physical Bytes Written", mutability=AttributeMutabilityOption.READ_ONLY)	
	public long getDiskWriteBytes() {
		return diskWriteBytesCounter.getLastValue();
	}
	
	/**
	 * Returns the Disk Service Time
	 * @return the Disk Service Time
	 */
	@JMXAttribute(name="DiskServiceTime", description="The Disk Service Time", mutability=AttributeMutabilityOption.READ_ONLY)	
	public double getDiskServiceTime() {
		return diskServiceTimeCounter.getLastValue();
	}
	
	/**
	 * Returns the percentage free space on the disk
	 * @return the diskSpaceFreeCounter
	 */
	@JMXAttribute(name="DiskSpaceFreePercent", description="The Disk Space Free %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getDiskSpaceFreePercent() {
		return diskSpaceFreeCounter.getLastValue();
	}
	
	/**
	 * Returns the percentage used space on the disk
	 * @return the diskSpaceUsedCounter
	 */
	@JMXAttribute(name="DiskSpaceUsedPercent", description="The Disk Space Used %", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getDiskSpaceUsedPercent() {
		return diskSpaceUsedCounter.getLastValue();
	}
	
	/**
	 * Returns the file system name
	 * @return the file system name
	 */
	@JMXAttribute(name="FileSystemName", description="The File System Name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getFileSystemName() {
		return deviceName;
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Iterates through all discovered file systems and determines the best way to publish a monitor service for each.
	 * Different OSs respond differently to sigar file system directives, so a few different options are attempted for each file system:<ul>
	 * <li>If the FS Type is <b>NETWORK</b> or the type name is <b>remote</b>, <b><code>getMountedFileSystemUsage</code></b> is used to get the usage passing <b>DirName</b> as the key. 
	 * If the usage indicates  <b>getFiles</b> and <b>getTotal</b> of zero, the file system is not monitored.</li>
	 * <li>If the FS Type is anything else, <b><code>getFileSystemUsage</code></b>  is used to get the usage passing <b>DevName</b> and backing out to using <b>DirName</b> as the key.
	 * Backout occurs if there is an exception (or null) getting the usage, or the usage indicates <b>getFiles</b> and <b>getTotal</b> of zero. If neither produce a different result,
	 * the file system is not monitored.</li>
	 * </ul>
	 */
	public static void boot() {
		for(FileSystem fs: HeliosSigar.getInstance().getFileSystemList()) {
			FileSystemUsage fsu = null;
			boolean useDev = true;
			try {
				if(FileSystemType.TYPE_NETWORK.getCode()==fs.getType() || "remote".equalsIgnoreCase(fs.getTypeName())) {
					fsu = HeliosSigar.getInstance().getMountedFileSystemUsageOrNull(fs.getDirName());
					useDev = false;
				} else {
					fsu = HeliosSigar.getInstance().getFileSystemUsageOrNull(fs.getDirName());
					if(!isFileSystemActive(fsu)) {
						fsu = HeliosSigar.getInstance().getFileSystemUsageOrNull(fs.getDevName());
						useDev = true;
					} else {
						useDev = false;
					}
				}
				if(isFileSystemActive(fsu)) {
					new FileSystemService(useDev ? fs.getDevName() : fs.getDirName(), fsu);
				}
			} catch (Exception e) {}

		}
	}
	
	/**
	 * Determines if the file system represented by the passed FileSystemUsage should be monitored. 
	 * @param fsu the FileSystemUsage
	 * @return true if the file system should be monitored.
	 */
	protected static boolean isFileSystemActive(FileSystemUsage fsu) {
		if(fsu==null) return false;
		return !(fsu.getTotal()<1 && fsu.getFiles()<1);
	}


	/**
	 * Returns the file system device name
	 * @return the devName
	 */
	@JMXAttribute(name="DeviceName", description="The file system device name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDeviceName() {
		return devName;
	}


	/**
	 * Returns the file system mounted directory name
	 * @return the dirName
	 */
	@JMXAttribute(name="DirectoryName", description="The file system mounted directory name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getDirectoryName() {
		return dirName;
	}


	/**
	 * Returns the file system options
	 * @return the options
	 */
	@JMXAttribute(name="Options", description="The file system options", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getOptions() {
		return options;
	}


	/**
	 * Returns the file system type
	 * @return the type
	 */
	@JMXAttribute(name="Type", description="The file system type", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getType() {
		return type;
	}


	/**
	 * Returns the file system category
	 * @return the category
	 */
	@JMXAttribute(name="Category", description="The file system category", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getCategory() {
		return category;
	}
	

}
