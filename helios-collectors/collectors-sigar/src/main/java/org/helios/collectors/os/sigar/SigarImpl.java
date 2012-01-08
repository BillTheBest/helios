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
package org.helios.collectors.os.sigar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.helios.collectors.os.CPUInfo;
import org.helios.collectors.os.FileSystemInfo;
import org.helios.collectors.os.MemoryInfo;
import org.helios.collectors.os.MethodNotSupportedException;
import org.helios.collectors.os.NetworkInfo;
import org.helios.collectors.os.NetworkInterface;
import org.helios.collectors.os.OSException;
import org.helios.collectors.os.OSInfo;
import org.helios.collectors.os.ProcessInfo;
import org.helios.collectors.os.ProcessorUsageInfo;
import org.helios.collectors.os.ServiceInfo;
import org.helios.collectors.os.sigar.SigarFactory;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NfsFileSystem;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;

/**
 * <p>Title: SigarImpl </p>
 * <p>Description: ISystem Implementation to expose local OS statistics using SIGAR API</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class SigarImpl implements SigarImplMBean, Serializable {

	private static final long serialVersionUID = 6579712981726226755L;
	protected Logger log = Logger.getLogger(SigarImpl.class);
	private static final String ptqlPrefix="State.Name.re=";

	public List<ProcessInfo> getProcessList() throws MethodNotSupportedException, OSException{
		long[] pids = {};
		StringBuilder pidsWithError = new StringBuilder("");
		List<ProcessInfo> matches = new ArrayList<ProcessInfo>();
		try{
			pids = SigarFactory.getSigar().getProcList();
		}catch(SigarException sex){
			throw new OSException("An error occured while getting list of processes.",sex);
		}
		if(pids!=null){
			for (int i = 0; i < pids.length; i++) {
				long pid = pids[i];
				try{
					ProcessInfo pInfo = extractProcessInfo(pid);
					if(pInfo!=null)
						matches.add(pInfo);
				}catch(Exception ex){
					pidsWithError.append("[" + pid + "] ");
				}
			}
		}
		if(pidsWithError.length()>0)
			log.debug("Could not collect information for process(es) with ID: " + pidsWithError.toString());
		return matches;
	}
	
	public List<ProcessInfo> getMatchedProcessList(String[] processMatcher) throws MethodNotSupportedException, OSException{
		long[] pids = {};
		List<ProcessInfo> matches = new ArrayList<ProcessInfo>();
		try{
			for(String processName: processMatcher){
				pids = ProcessFinder.find(SigarFactory.getSigar(), ptqlPrefix+processName);
				if(pids!=null){
					for (int i = 0; i < pids.length; i++) {
						long pid = pids[i];
						try{
							ProcessInfo pInfo = extractProcessInfo(pid);
							if(pInfo!=null)
								matches.add(pInfo);
						}catch(Exception ex){
							log.debug("** Unable to get complete information for pid: " + pid);
						}
					}
				}
			}
		}catch(SigarException sex){
			throw new OSException("An error occured while getting list of processes.",sex);
		}
		
		return matches;
	}	
	
	public ServiceInfo getServiceByName(String name) throws MethodNotSupportedException, OSException {
		ServiceInfo sInfo = new ServiceInfo();
		Service service = null;
		if(name!=null && name.length()>0){
			try{
				service = new Service(name);
				if(service.getStatusString().equalsIgnoreCase("Running")){
					long pid = SigarFactory.getSigar().getServicePid(name);
					ProcessInfo pInfo = extractProcessInfo(pid);
					if(pInfo!=null){
						sInfo = new ServiceInfo(pInfo);
					}	
				}else{
					log.trace("Service ["+name+"] is currently not running.");
					sInfo = new ServiceInfo();
					sInfo.setPid(-1);
				}
				sInfo.setServiceName(name);
				sInfo.setStatus(service.getStatusString());
				ServiceConfig sConfig = service.getConfig();
				sInfo.setDisplayName(sConfig.getDisplayName());
				sInfo.setDependencies(sConfig.getDependencies());
				sInfo.setStartupType(sConfig.getStartTypeString());
				service.close();
				
			}catch(SigarException sex){
				if(service!=null)
					service.close();
				throw new OSException(sex);
			}			
		}
		return sInfo;
	}
	
	public List<ServiceInfo> getServiceList() throws MethodNotSupportedException, OSException {
		List allServices = new ArrayList();
		StringBuilder servicesWithError = new StringBuilder("");
		try{
			allServices = Service.getServiceNames();
		}catch(Win32Exception wex){
			throw new OSException(wex);
		}
		List<ServiceInfo> result = new ArrayList<ServiceInfo>();
		for(int i=0; i<allServices.size(); i++){
			try{				
				result.add(getServiceByName((String)allServices.get(i)));
			}catch(Exception sex){
				servicesWithError.append("[" + allServices.get(i) + "] ");
				continue;
			}
		}
		if(servicesWithError.length()>0)
			log.debug("Could not collect information for service(s): " + servicesWithError.toString());
		return result;
	}	
	
	private ProcessInfo extractProcessInfo(long pid) throws SigarException{
		Sigar sigar = SigarFactory.getSigar();
		String[] arg = sigar.getProcArgs(pid);
		ProcessInfo pInfo = new ProcessInfo();
		pInfo.setPid(pid);
		pInfo.setArguments(arg);
		ProcExe exe = sigar.getProcExe(pid);
		pInfo.setExeName(exe.getName());
		pInfo.setWorkingDir(exe.getCwd());
		ProcCredName credName = sigar.getProcCredName(pid);
	    pInfo.setUser(credName.getUser());
	    pInfo.setGroup(credName.getGroup());
	    ProcTime time = sigar.getProcTime(pid);
	    pInfo.setStartTime(time.getStartTime());
	    ProcMem mem = sigar.getProcMem(pid);
	    pInfo.setMemory(mem.getResident());
	    pInfo.setDescription(ProcUtil.getDescription(sigar, pid));
	    ProcState procState = sigar.getProcState(pid);
	    pInfo.setName(procState.getName());
	    pInfo.setState(getStateString(procState.getState()));
	    pInfo.setPriority(procState.getPriority());
	    pInfo.setThreads(procState.getThreads());
		pInfo.createRegexBase();
		return pInfo;
	}
	
    public static String getStateString(char state) {
        switch (state) {
          case ProcState.SLEEP:
            return "Sleeping";
          case ProcState.RUN:
            return "Running";
          case ProcState.STOP:
            return "Suspended";
          case ProcState.ZOMBIE:
            return "Zombie";
          case ProcState.IDLE:
            return "Idle";
          default:
            return String.valueOf(state);
        }
    }	

	public CPUInfo getCPUInfo() throws MethodNotSupportedException, OSException{
		Sigar sigar = SigarFactory.getSigar();
		List<ProcessorUsageInfo> processorList = new ArrayList<ProcessorUsageInfo>();
		CpuInfo[] cpuList = null;
		CpuInfo cpu = null;
		CpuPerc[] proUsage = null;
		CPUInfo cpuInfo = new CPUInfo();
		try{
			cpuList = sigar.getCpuInfoList();
			cpu = cpuList[0];
			proUsage =  sigar.getCpuPercList();
			cpuInfo.setTotalUtilization(Math.ceil(sigar.getCpuPerc().getCombined()*100.0));
		}catch(SigarException sex){
			throw new OSException(sex);
		}
		
		if(cpu!=null && proUsage!=null){
			cpuInfo.setVendor(cpu.getVendor());
			cpuInfo.setModel(cpu.getModel());
			cpuInfo.setMhz(cpu.getMhz());
			cpuInfo.setCpuSockets(cpu.getTotalSockets());
			cpuInfo.setCoresPerCpu(cpu.getCoresPerSocket());
			cpuInfo.setTotalProcessors(cpu.getTotalSockets()*cpu.getCoresPerSocket());
			for(CpuPerc perc: proUsage){
				ProcessorUsageInfo pInfo = new ProcessorUsageInfo();
				pInfo.setUser(Math.ceil(perc.getUser()*100.0));
				pInfo.setSys(Math.ceil(perc.getSys()*100.0));
				pInfo.setIdle(Math.ceil(perc.getIdle()*100.0));
				pInfo.setWait(Math.ceil(perc.getWait()*100.0));
				pInfo.setNice(Math.ceil(perc.getNice()*100.0));
				processorList.add(pInfo);
			}
			cpuInfo.setProcessorUsageInfo(processorList);
		}
        return cpuInfo;
	}

	public List<FileSystemInfo> getFileSystemInfo() throws MethodNotSupportedException, OSException{
		Sigar sigar = SigarFactory.getSigar();
		List<FileSystemInfo> matches = new ArrayList<FileSystemInfo>();
        FileSystem[] fSystems;
        try{
        	fSystems = sigar.getFileSystemList();
        }catch(SigarException sex){
        	throw new OSException(sex);
        }	
        for (int i=0; i<fSystems.length; i++) {
        	FileSystem fs = fSystems[i];
            FileSystemUsage usage = null;
        	try{
                if (fSystems[i] instanceof NfsFileSystem) {
                    NfsFileSystem nfs = (NfsFileSystem)fs;
                    if (!nfs.ping()) {
                        continue;
                    }
                }
                usage = sigar.getMountedFileSystemUsage(fs.getDirName());
            } catch (SigarException e) {
            	log.debug("Could not collect information for FileSystem: " + fs.getDirName());
            }
            if(usage!=null){
	            FileSystemInfo fInfo = new FileSystemInfo();
	            fInfo.setTotal(usage.getTotal());
	            fInfo.setUsed(usage.getUsed());
	            fInfo.setUsedPercentage(usage.getUsePercent() * 100);
		        fInfo.setFree(usage.getAvail());
		        fInfo.setFreePercentage( 100 - (usage.getUsePercent() * 100));
		        fInfo.setMountedOn(fs.getDirName());
		        fInfo.setSystemType(fs.getSysTypeName());
		        fInfo.setType(fs.getTypeName());
		        fInfo.setTotalBytesRead(usage.getDiskReadBytes());
		        fInfo.setTotalBytesWritten(usage.getDiskWriteBytes());
		        fInfo.setTotalReads(usage.getDiskReads());
		        fInfo.setTotalWrites(usage.getDiskWrites());
		        fInfo.setDeviceName(fs.getDevName());

		        matches.add(fInfo);
            }
        }
        return matches;
	}

	public MemoryInfo getMemoryInfo() throws MethodNotSupportedException, OSException{
		try{
			Mem mem = SigarFactory.getSigar().getMem();
			MemoryInfo mInfo = new MemoryInfo();
			mInfo.setTotal(mem.getTotal());
			mInfo.setUsed(mem.getUsed());
			mInfo.setFree(mem.getFree());
			mInfo.setFreePercent(mem.getFreePercent());
			mInfo.setUsedPercent(mem.getUsedPercent());
			return mInfo;
		}catch(SigarException sex){
			throw new OSException(sex);
		}
	}

	public NetworkInfo getNetworkInfo() throws MethodNotSupportedException, OSException{
		try{
			Sigar sigar = SigarFactory.getSigar();
			NetInfo ni = sigar.getNetInfo();
			NetworkInfo nInfo = new NetworkInfo();
			nInfo.setDefaultGateway(ni.getDefaultGateway());
			nInfo.setPrimaryDNS(ni.getPrimaryDns());
			nInfo.setSecondaryDNS(ni.getSecondaryDns());
			nInfo.setDomain(ni.getDomainName());
			nInfo.setHost(ni.getHostName());
			
			String[] interfaces = sigar.getNetInterfaceList();
			for(String i: interfaces){
				NetInterfaceConfig nic = sigar.getNetInterfaceConfig(i);
				NetworkInterface nInterface = new NetworkInterface();
				nInterface.setName(nic.getName());
				nInterface.setDescription(nic.getDescription());
				nInterface.setType(nic.getType());
				nInterface.setAddress(nic.getAddress());
				nInterface.setHardwareAddress(nic.getHwaddr());
				nInterface.setBroadcast(nic.getBroadcast());
				nInfo.getInterfaces().add(nInterface);
			}
			return nInfo;
		}catch(SigarException sex){
			throw new OSException(sex);
		}
	}

	public OSInfo getOSInfo() throws MethodNotSupportedException, OSException{
		try{
			OperatingSystem os = OperatingSystem.getInstance();
			os.gather(SigarFactory.getSigar());
			OSInfo osInfo = new OSInfo();
			osInfo.setArchitecture(os.getArch());
			osInfo.setName(os.getName());
			osInfo.setVendor(os.getVendor());
			osInfo.setVendorVersion(os.getVendorVersion());
			osInfo.setPatchLevel(os.getPatchLevel());
			osInfo.setDescription(os.getDescription());
			osInfo.setVersion(os.getVersion());
			osInfo.setDataModel(os.getDataModel());
			return osInfo;
		}catch(SigarException sex){
			throw new OSException(sex);
		}
	}
	
	public long getUptime() throws MethodNotSupportedException, OSException {
		try{
			return (long)(System.currentTimeMillis() - (SigarFactory.getSigar().getUptime().getUptime()*1000) );
		}catch(SigarException sex){
			throw new OSException(sex);
		}
	}
	
}
