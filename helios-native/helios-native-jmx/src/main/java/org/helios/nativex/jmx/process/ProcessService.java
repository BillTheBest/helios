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
package org.helios.nativex.jmx.process;


import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.helios.helpers.JMXHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.JMXOperation;
import org.helios.jmx.dynamic.annotations.JMXParameter;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;
import org.helios.jmxenabled.counters.IntegerRollingCounter;
import org.helios.jmxenabled.counters.LongDeltaRollingCounter;
import org.helios.jmxenabled.counters.LongRollingCounter;
import org.helios.nativex.agent.NativeAgentAttacher;
import org.helios.nativex.jmx.AbstractNativeCounter;
import org.helios.nativex.sigar.HeliosSigar;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcFd;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.ProcUtil;

/**
 * <p>Title: ProcessService</p>
 * <p>Description: Native OS Process Monitor. One MBean per process.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p>org.helios.nativex.jmx.process.ProcessService</p>
 */
@JMXManagedObject(annotated=true, declared=false)
public class ProcessService extends AbstractNativeCounter {
	/**  */
	private static final long serialVersionUID = 6392479946220466342L;
	/** The process state stats gatherer */
	protected final ProcState procState;
	/** The process time stats gatherer */
	protected final ProcTime procTime;
	/** The process exe stats gatherer */
	protected final ProcExe procExe;
	/** The process memory stats gatherer */
	protected final ProcMem procMem;
	/** The process file descriptor stats gatherer */
	protected final ProcFd procFd;
	/** The process percent cpu stats gatherer */
	protected final ProcCpu procCpu;
	
	/** The process name */
	protected final String processName;
	/** The process path (the fully name including directory) */
	protected final String processPath;
	/** The process working directory */
	protected final String processWorkingDir;
	
	/** The process start timestamp */
	protected final long startTime;
	/** The process start date */
	protected final Date startDate;
	
	
	/** the process id */
	protected final long pid;
	
	/** The process Priority */
	protected final IntegerRollingCounter priorityCounter = new IntegerRollingCounter("ProcExePriorityCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Threads */
	protected final LongRollingCounter threadsCounter = new LongRollingCounter("ProcExeThreadsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Ppid */
	protected final LongRollingCounter ppidCounter = new LongRollingCounter("ProcExePpidCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Tty */
	protected final IntegerRollingCounter ttyCounter = new IntegerRollingCounter("ProcExeTtyCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Nice */
	protected final IntegerRollingCounter niceCounter = new IntegerRollingCounter("ProcExeNiceCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Processor */
	protected final IntegerRollingCounter processorCounter = new IntegerRollingCounter("ProcExeProcessorCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Size memory utilization */
	protected final LongRollingCounter sizeCounter = new LongRollingCounter("ProcExeSizeCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Resident memory utilization */
	protected final LongRollingCounter residentCounter = new LongRollingCounter("ProcExeResidentCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Share memory utilization */
	protected final LongRollingCounter shareCounter = new LongRollingCounter("ProcExeShareCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process non-i/o page fault count */
	protected final LongRollingCounter minorFaultsCounter = new LongRollingCounter("ProcExeMinorFaultsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process i/o page fault count */
	protected final LongRollingCounter majorFaultsCounter = new LongRollingCounter("ProcExeMajorFaultsCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process total page fault count */
	protected final LongRollingCounter pageFaultsCounter = new LongRollingCounter("ProcExePageFaultsCounter", DEFAULT_ROLLING_SIZE, registerGroup);

	/** The process The process non-i/o page fault rate */
	protected final LongDeltaRollingCounter minorFaultsRate= new LongDeltaRollingCounter("ProcExeMinorFaultsRate", DEFAULT_ROLLING_SIZE, registerGroup);	
	/** The process The process i/o page fault rate */
	protected final LongDeltaRollingCounter majorFaultsRate = new LongDeltaRollingCounter("ProcExeMajorFaultsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process The process total page fault rate */
	protected final LongDeltaRollingCounter pageFaultsRate = new LongDeltaRollingCounter("ProcExePageFaultsRate", DEFAULT_ROLLING_SIZE, registerGroup);
	
	
	/** The process total number of open file descriptors counter */
	protected final LongRollingCounter openFileDescCounter = new LongRollingCounter("OpenFileDescCounter", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process total number of loaded modules counter */
	protected final IntegerRollingCounter loadedModulesCounter = new IntegerRollingCounter("LoadedModulesCounter", DEFAULT_ROLLING_SIZE, registerGroup);	
	/** A list of the process loaded modules */
	protected final Set<String> loadedModules = new CopyOnWriteArraySet<String>();
	/** A map of the process environment */
	protected final Map<Object, Object> processEnvironment;
	/** The process owner user name */
	protected final String userOwner;
	/** The process owner group name */
	protected final String groupOwner;
	/** The process description */
	protected final String description;
	/** The process Java main class */
	protected final String javaMain;
	/** The process arguments */
	protected final String[] processArgs;
	/** The process Total CPU utilization */
	protected final LongDeltaRollingCounter totalCPURate = new LongDeltaRollingCounter("ProcExeTotalCPURate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process User CPU utilization */
	protected final LongDeltaRollingCounter userCPURate = new LongDeltaRollingCounter("ProcExeUserCPURate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process Sys CPU utilization */
	protected final LongDeltaRollingCounter sysCPURate = new LongDeltaRollingCounter("ProcExeSysCPURate", DEFAULT_ROLLING_SIZE, registerGroup);
	/** The process CPU utilization percentage */
	protected final IntegerRollingCounter cpuPercent = new IntegerRollingCounter("ProcExeCPUPercent", DEFAULT_ROLLING_SIZE, registerGroup);
	
	
	/**
	 * Creates a new ProcessService 
	 * @param pid The process ID of the process to monitor
	 */
	public ProcessService(long pid) {
		super();
		this.pid = pid;
		procCpu = HeliosSigar.getInstance().getProcCpu(pid);
		procState = HeliosSigar.getInstance().getProcState(pid);		
		procTime = HeliosSigar.getInstance().getProcTime(pid);
		procExe = HeliosSigar.getInstance().getProcExe(pid);
		procMem = HeliosSigar.getInstance().getProcMem(pid);
		procFd = HeliosSigar.getInstance().getProcFd(pid);
		processName = procState.getName();
		processPath = procExe.getName();
		processWorkingDir = procExe.getCwd();
		startTime = procTime.getStartTime();
		startDate = new Date(startTime);
		updateModules();
		processEnvironment = setEnvironment();
		String[] creds = getUserGroup();
		userOwner = creds[0];
		groupOwner = creds[1];
		description = getDescription();
		javaMain = getJavaMainClass();
		processArgs = HeliosSigar.getInstance().getProcArgs(pid);
		run();  // pre-inits the delta counters.
		this.scheduleSampling();		
//		counters.put(sysCPURate.getName(), sysCPURate);
//		counters.put(userCPURate.getName(), userCPURate);
//		counters.put(totalCPURate.getName(), totalCPURate);
		registerCounterMBean("service", "ProcessService", "pid", "" + pid, "name", procState.getName());
		initPerfCounters();
	}
	
	
	/**
	 * Gathers and increments OS process stats
	 */
	@Override
	public void run() {
		try {
			procState.gather(sigar, pid);
			procMem.gather(sigar, pid);
			procFd.gather(sigar, pid);
			procTime.gather(sigar, pid);	
			procCpu.gather(sigar, pid);
			priorityCounter.put(procState.getPriority());
			threadsCounter.put(procState.getThreads());
			ppidCounter.put(procState.getPpid());
			ttyCounter.put(procState.getTty());
			niceCounter.put(procState.getNice());
			processorCounter.put(procState.getProcessor());		
			sizeCounter.put(procMem.getSize());
			residentCounter.put(procMem.getResident());
			shareCounter.put(procMem.getShare());
			long minorFaults = procMem.getMinorFaults();
			long majorFaults = procMem.getMajorFaults();
			long totalFaults = procMem.getPageFaults();
			minorFaultsCounter.put(minorFaults);
			majorFaultsCounter.put(majorFaults);
			pageFaultsCounter.put(totalFaults);
			minorFaultsRate.put(minorFaults);
			majorFaultsRate.put(majorFaults);
			pageFaultsRate.put(totalFaults);			
			openFileDescCounter.put(procFd.getTotal());
			totalCPURate.put(procTime.getTotal());
			userCPURate.put(procTime.getUser());
			sysCPURate.put(procTime.getSys());	
			double cp = procCpu.getPercent();
			//log("PID: [" + pid + "] CPU%:" + cp );
			cpuPercent.put(doubleToIntPercent(cp));
			updateModules();
		} catch (Exception e) {
			if(e!=null && e.getMessage()!=null && e.getMessage().contains("")) {
				log.info("Process [" + pid + "] has terminated. Unregistering [" + objectName + "]");
				try { server.unregisterMBean(objectName); } catch (Exception ex) {}
			} else {
				log.error("Failed to gather on service [" + objectName + "]", e);
				throw new RuntimeException("Failed to gather on service [" + objectName + "]", e);
			}
		}
	}
	
	
	
	/**
	 * Gets the process description
	 * @return the process description
	 */
	protected String getDescription() {
		try {
			return ProcUtil.getDescription(sigar, pid);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Gets the Java main class name
	 * @return the Java main class name
	 */
	protected String getJavaMainClass() {
		try {
			return ProcUtil.getJavaMainClass(sigar, pid);
		} catch (Exception e) {
			return null;
		}
	}
	
	
	/**
	 * Retrieves the process user and group owners
	 * @return A string array 0: User, 1: Group
	 */
	protected String[] getUserGroup() {
		ProcCredName pcn = null;
		String[] creds = new String[]{null, null};
		try { pcn = HeliosSigar.getInstance().getProcCredName(pid); } catch (Exception e) {};
		if(pcn!=null) {
			try { creds[0] = pcn.getUser(); } catch (Exception e) {}
			try { creds[1] = pcn.getGroup(); } catch (Exception e) {}
		}		
		return creds;
	}
	
	/**
	 * Creates a map of the process environment
	 * @return a map of the process environment
	 */
	protected Map<Object, Object> setEnvironment() {
		Map<Object, Object> env = (Map<Object, Object>) HeliosSigar.getInstance().getProcEnv(pid);
		try {
			CompositeType cType = new CompositeType(
					"EnvironmentEntry", 
					"An environmental variable of a running process", 
					new String[]{"key", "value"}, 
					new String[]{"The variable key", "The variable value"}, 
					new OpenType[]{SimpleType.STRING, SimpleType.STRING}
			);
			TabularDataSupport tds = new TabularDataSupport(
					new TabularType(
							"ProcessEnvironment", 
							"The environmental variables of a running process", 
							cType,
							new String[]{"key"}
					),
					env.size(), 0.75f
			);
			for(Map.Entry<Object, Object> entry: env.entrySet()) {
				tds.put(new CompositeDataSupport(cType, new String[]{"key", "value"}, new Object[]{entry.getKey(), entry.getValue()}));
			}
			
			return tds;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return Collections.unmodifiableMap(env);
		}
		
	}
	
	/**
	 * Updates the loaded modules
	 */
	protected void updateModules() {
		List<String> modules = (List<String>) HeliosSigar.getInstance().getProcModules(pid);
		loadedModules.retainAll(modules);
		loadedModules.addAll(modules);
		loadedModulesCounter.put(loadedModules.size());
	}
	
	
	/**
	 * Bootstraps this service for this JVM's process 
	 */
	public static void boot() {
		new ProcessService(HeliosSigar.getInstance().getPid());
//		for(Long pid: NativeAgentAttacher.getPlatformJVMPids()) {
//			new ProcessService(pid);
//		}
	}
	
	/**
	 * Returns the process State 
	 * @return the state name of the process
	 */
	@JMXAttribute(name="State", description="The process state", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getState() {
		return ProcessState.forCode(procState.getState()).name();
	}
	
	/**
	 * Returns the process Priority 
	 * @return a counter value
	 */
	@JMXAttribute(name="Priority", description="The process Priority", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getPriority() {
		return priorityCounter.getLastValue();
	}

	/**
	 * Returns the process Threads 
	 * @return a counter value
	 */
	@JMXAttribute(name="Threads", description="The process Threads", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getThreads() {
		return threadsCounter.getLastValue();
	}

	/**
	 * Returns the process Ppid 
	 * @return a counter value
	 */
	@JMXAttribute(name="Ppid", description="The process Ppid", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPpid() {
		return ppidCounter.getLastValue();
	}

	/**
	 * Returns the process Tty 
	 * @return a counter value
	 */
	@JMXAttribute(name="Tty", description="The process Tty", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getTty() {
		return ttyCounter.getLastValue();
	}

	/**
	 * Returns the process Nice 
	 * @return a counter value
	 */
	@JMXAttribute(name="Nice", description="The process Nice", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getNice() {
		return niceCounter.getLastValue();
	}

	/**
	 * Returns the process Processor 
	 * @return a counter value
	 */
	@JMXAttribute(name="Processor", description="The process Processor", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getProcessor() {
		return processorCounter.getLastValue();
	}

	/**
	 * Returns the process executable image name
	 * @return the processName
	 */
	@JMXAttribute(name="ProcessName", description="The process executable image name", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProcessName() {
		return processName;
	}

	/**
	 * Returns the process start time
	 * @return the startTime
	 */
	@JMXAttribute(name="StartTime", description="The process start time", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Returns the process start date
	 * @return the startDate
	 */
	@JMXAttribute(name="StartDate", description="The process start date", mutability=AttributeMutabilityOption.READ_ONLY)
	public Date getStartDate() {
		return startDate;
	}
	
	/**
	 * Returns the process up time in ms.
	 * @return the up time
	 */
	@JMXAttribute(name="UpTime", description="The process up time in ms.", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUpTime() {
		return System.currentTimeMillis()-startTime;
	}

	/**
	 * Returns the process executable image path
	 * @return the processPath
	 */
	@JMXAttribute(name="ProcessPath", description="Returns the process executable image path", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProcessPath() {
		return processPath;
	}

	/**
	 * Returns the process working directory
	 * @return the processWorkingDir
	 */
	@JMXAttribute(name="WorkingDirectory", description="Returns the process working directory", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getWorkingDirectory() {
		return processWorkingDir;
	}
	
	/**
	 * Returns the process memory Size 
	 * @return a counter value
	 */
	@JMXAttribute(name="SizeMemory", description="The process memory Size", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSizeMemory() {
		return sizeCounter.getLastValue();
	}

	/**
	 * Returns the process memory Resident 
	 * @return a counter value
	 */
	@JMXAttribute(name="ResidentMemory", description="The process memory Resident", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getResidentMemory() {
		return residentCounter.getLastValue();
	}

	/**
	 * Returns the process memory Share 
	 * @return a counter value
	 */
	@JMXAttribute(name="SharedMemory", description="The process memory Share", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSharedMemory() {
		return shareCounter.getLastValue();
	}

	/**
	 * Returns the total number of memory non-io page faults 
	 * @return the total number of memory non-io page faults
	 */
	@JMXAttribute(name="TotalMinorFaults", description="The total number of memory non-io page faults", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalMinorFaults() {
		return minorFaultsCounter.getLastValue();
	}

	/**
	 * Returns the total number of memory io page faults 
	 * @return the total number of memory io page faults
	 */
	@JMXAttribute(name="TotalMajorFaults", description="The total number of memory io page faults", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalMajorFaults() {
		return majorFaultsCounter.getLastValue();
	}

	/**
	 * Returns the total number of all memory page faults 
	 * @return the total number of all memory page faults
	 */
	@JMXAttribute(name="TotalPageFaults", description="The total number of all memory page faults", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalPageFaults() {
		return pageFaultsCounter.getLastValue();
	}
	
	/**
	 * Returns total number of open file descriptors
	 * @return the total number of open file descriptors
	 */
	@JMXAttribute(name="OpenFileDescriptors", description="The process total number of open file descriptors", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getOpenFileDescriptors() {
		return openFileDescCounter.getLastValue();
	}
	
	/**
	 * Returns the process loaded module names
	 * @return the process loaded module names
	 */
	@JMXAttribute(name="LoadedModules", description="The process loaded module names", mutability=AttributeMutabilityOption.READ_ONLY)
	public Collection<String> getLoadedModules() {
		return Collections.unmodifiableSet(loadedModules);
	}
	
	/**
	 * Returns the process number of loaded modules
	 * @return the process number of loaded modules
	 */
	@JMXAttribute(name="LoadedModuleCount", description="The process number of loaded modules", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getLoadedModuleCount() {
		return loadedModulesCounter.getLastValue();
	}
	
	/**
	 * Returns a map of the process environment
	 * @return a map of the process environment
	 */
	@JMXAttribute(name="Environment", description="The map of the process environment", mutability=AttributeMutabilityOption.READ_ONLY)
	public TabularData getEnvironment() {
		try {
			return (TabularData) processEnvironment;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * The process user owner
	 * @return the userOwner
	 */
	@JMXAttribute(name="User", description="The process user owner", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getUser() {
		return userOwner;
	}

	/**
	 * The process group owner
	 * @return the groupOwner
	 */
	@JMXAttribute(name="Group", description="The process group owner", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getGroup() {
		return groupOwner;
	}

	/**
	 * Returns the Java Main Class name
	 * @return the Java Main class name or null if this process is not a JVM
	 */
	@JMXAttribute(name="JavaMain", description="The Java Main class name or null if this process is not a JVM", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getJavaMain() {
		return javaMain;
	}
	
	/**
	 * Returns the process description
	 * @return the process description
	 */
	@JMXAttribute(name="ProcessDescription", description="The process description", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getProcessDescription() {
		return description;
	}

	/**
	 * Returns the process arguments
	 * @return the process arguments
	 */
	@JMXAttribute(name="ProcessArguments", description="The process arguments", mutability=AttributeMutabilityOption.READ_ONLY)	
	public String[] getProcessArguments() {
		return processArgs;
	}

	/**
	 * Returns the rate of memory non-io page faults
	 * @return the rate of memory non-io page faults
	 */
	@JMXAttribute(name="MinorPageFaultsRate", description="The rate of memory non-io page faults", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMinorPageFaultsRate() {
		return minorFaultsRate.getLastValue();
	}

	/**
	 * Returns the rate of memory io page faults
	 * @return the rate of memory io page faults
	 */
	@JMXAttribute(name="MajorPageFaultsRate", description="The rate of memory io page faults", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getMajorPageFaultsRate() {
		return majorFaultsRate.getLastValue();
	}

	/**
	 * Returns the rate of memory total page faults
	 * @return the rate of memory total page faults
	 */
	@JMXAttribute(name="PageFaultsRate", description="The rate of memory total page faults", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getPageFaultsRate() {
		return pageFaultsRate.getLastValue();
	}

	/**
	 * Returns the process Total CPU Utilization Rate 
	 * @return the rate of Total CPU time 
	 */
	@JMXAttribute(name="TotalCPURate", description="The process CPU Total utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getTotalCPURate() {
		return totalCPURate.getLastValue();
	}


	/**
	 * Returns the process User CPU Utilization Rate 
	 * @return the rate of User CPU time 
	 */
	@JMXAttribute(name="UserCPURate", description="The process CPU User utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getUserCPURate() {
		return userCPURate.getLastValue();
	}

	/**
	 * Returns the process Sys CPU Utilization Rate 
	 * @return the rate of Sys CPU time 
	 */
	@JMXAttribute(name="SysCPURate", description="The process CPU Sys utilization rate", mutability=AttributeMutabilityOption.READ_ONLY)
	public long getSysCPURate() {
		return sysCPURate.getLastValue();
	}

	/**
	 * Returns the last gathered process percentage CPU Utilization 
	 * @return the last gathered process percentage CPU Utilization
	 */
	@JMXAttribute(name="LastCPUPercentUtilization", description="The last gathered process percentage CPU Utilization", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getLastCPUPercentUtilization() {
		return cpuPercent.getLastValue();
	}
	
	/**
	 * Returns the rolling average of the process percentage CPU Utilization 
	 * @return the rolling average of the process percentage CPU Utilization
	 */
	@JMXAttribute(name="AvgCPUPercentUtilization", description="The rolling average of the process percentage CPU Utilization", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getAvgCPUPercentUtilization() {
		return cpuPercent.getAverage();
	}
	
	/**
	 * Returns the rolling maximum of the process percentage CPU Utilization 
	 * @return the rolling maximum of the process percentage CPU Utilization
	 */
	@JMXAttribute(name="MaxCPUPercentUtilization", description="The rolling maximum of the process percentage CPU Utilization", mutability=AttributeMutabilityOption.READ_ONLY)
	public int getMaxCPUPercentUtilization() {
		return cpuPercent.getRangeMax();
	}
	
	/**
	 * Starts a new process service for the process identified by the passed PID
	 * @param pid The pid of the process to start a new process service for
	 * @return true if the service is successfully started.
	 */
	@JMXOperation(name="newProcessService", description="Starts a new process service for the process identified by the passed PID")
	public boolean newProcessService(@JMXParameter(name="PID", description="The pid of the process to start a new process service for")long pid) {
		try {
			if(JMXHelper.getHeliosMBeanServer().queryNames(JMXHelper.objectName(getClass().getPackage().getName() + ":service=ProcessService,pid=" + pid + ",*"), null).size()>0) {
				return false;
			} else {
				new ProcessService(pid);
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	
}
