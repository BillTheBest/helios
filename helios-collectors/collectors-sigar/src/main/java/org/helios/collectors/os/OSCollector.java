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
package org.helios.collectors.os;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helios.collectors.AbstractCollector;
import org.helios.collectors.CollectionResult;
import org.helios.collectors.exceptions.CollectorStartException;
import org.helios.helpers.StringHelper;
import org.helios.jmx.dynamic.annotations.JMXAttribute;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.jmx.dynamic.annotations.options.AttributeMutabilityOption;

//import org.josql.Query;
//import org.josql.QueryExecutionException;
//import org.josql.QueryParseException;
//import org.josql.QueryResults;

/**
 * <p>Title: OSCollector</p>
 * <p>Description: Collector for platform level statistics like CPU, Memory, Disk Usage, Process Info etc.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@JMXManagedObject (declared=false, annotated=true)
public class OSCollector extends AbstractCollector {

	/** OSCollector collector version */
	private static final String OS_COLLECTOR_VERSION="0.1";

//	protected String matchProcessQuery = "SELECT * FROM " +
//										 "org.helios.collectors.os.ProcessInfo " +
//										 "WHERE regexp(regexBase,'@REGEX_TOKEN@')";
	protected String[] processMatcher = null;
	protected Map<Long, ProcessInfo> tracedProcesses = new HashMap<Long,ProcessInfo>();
	protected Map<String, ServiceInfo> tracedServices = new HashMap<String,ServiceInfo>();
	
	protected List matchedProcesses = new ArrayList();
	
	protected String[] serviceMatcher = null;
	protected boolean traceStaticInfo = false;
	/** This flag is used to restrict tracing of OS Level Information to one time.  */
	protected boolean staticInfoTraced = false;
	protected Map<Long,ServiceInfo> matchedServices = new HashMap<Long,ServiceInfo>();
	
	protected ISystem systemGateway = null;
	
	public OSCollector(org.helios.collectors.os.ISystem systemGateway) {
		this.systemGateway = systemGateway;
	}
	
	/* 
	 * 
	 */
	public CollectionResult collectCallback() {
		long startTime=System.currentTimeMillis();
		CollectionResult result = new CollectionResult();
		//List<ProcessInfo> allProcesses;
		//List<ServiceInfo> allServices;
		ProcessInfo[] processArray = {};
		try{
			if(traceStaticInfo && !staticInfoTraced){
				traceOSInfo();				
				traceNetworkInfo();
				staticInfoTraced = true;
			}
			traceMemoryInfo();
			traceCPUInfo();
			traceFSUsageInfo();
			
			if(processMatcher!=null && processMatcher.length>0){
				matchedProcesses = systemGateway.getMatchedProcessList(processMatcher);
				log.info("*** Number of Matched processes - " + matchedProcesses.size());
				processArray = (ProcessInfo[])matchedProcesses.toArray(new ProcessInfo[0]);
				for(ProcessInfo pInfo: processArray){
		            traceMatchedProcess(pInfo);
		    		tracedProcesses.put(pInfo.getPid(), pInfo);
				}
			}
			// Let's find out if there's any process that has gone offline since last tracing interval
			traceOfflineProcesses(tracedProcesses);
			
			if(System.getProperty("os.name").indexOf("Win")!=-1 && serviceMatcher!=null && serviceMatcher.length>0){
				int totalMatches=0;
				for(String serviceName:serviceMatcher){
					traceMatchedServices(getServiceInfoByName(serviceName));
		            totalMatches++;
				}
				log.info("*** Number of matched services traced " + totalMatches);
			}
			
			tracer.trace(new Date(systemGateway.getUptime()), "System Uptime", tracingNameSpace);
			result.setResultForLastCollection(CollectionResult.Result.SUCCESSFUL);
		}catch(MethodNotSupportedException mnsex){
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			result.setAnyException(mnsex);
		}catch(OSException osex){
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			result.setAnyException(osex);
		}finally{
			tracer.traceSticky(System.currentTimeMillis()-startTime, "Elapsed Time", tracingNameSpace);
		}
		return result;
	}

	private ServiceInfo getServiceInfoByName(String serviceName) {
		ServiceInfo sInfo = null;
		try{
			sInfo = systemGateway.getServiceByName(serviceName);
		}catch(OSException osex){
			if(logErrors)
				log.error("An error occured while collecting CPUInfo information for OSCollector: ["+this.getBeanName()+"]", osex);
		}catch(MethodNotSupportedException mniex){
			if(logErrors)
				log.error("This method is not supported by current implementation of ISystem interface passed to the OSCollector : ["+this.getBeanName()+"]", mniex);
		}
		return sInfo;
	}

	protected void traceOfflineProcesses(Map<Long, ProcessInfo> tracedProcesses) {
		if(tracedProcesses==null || tracedProcesses.isEmpty())
			return;
		Set<Long> keys = tracedProcesses.keySet();
		Iterator<Long> iterator = keys.iterator();
		while(iterator.hasNext()){
			Long pid = iterator.next();
			ProcessInfo pInfo = tracedProcesses.get(pid);
        	if(!pInfo.isTraced()){
        		log.info("************ Marking Process: " + pInfo.getName()+ " as offline");
        		//These process were detected online in the past but may have gone offline - so trace Availability accordingly
        		tracer.traceSticky(0, "Availability",StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
        		tracer.trace("0", "PID", StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
        		tracer.traceSticky(0l, "Memory",StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
        		tracer.traceSticky(0l, "Threads", StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
        		tracer.trace(0, "Start Time", StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
        		tracedProcesses.remove(pid);
        	}else{
        		//reset flag for next tracing interval
        		pInfo.setTraced(false);
        		tracedProcesses.put(pid, pInfo);
        	}
		}
	}

	/**
	 * @param pInfo
	 */
	protected void traceMatchedProcess(ProcessInfo pInfo) {
		log.trace(pInfo);
		tracer.traceSticky(1, "Availability",StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
		tracer.trace(pInfo.getPid()+"", "PID", StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
		tracer.traceSticky(pInfo.getMemory(), "Memory", StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
		tracer.traceSticky(pInfo.getThreads(), "Threads", StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
		tracer.trace(new Date(pInfo.getStartTime()), "Start Time", StringHelper.append(tracingNameSpace, true, "Process",pInfo.getName()));
		pInfo.setTraced(true);
	}

	protected void traceCPUInfo() throws OSException, MethodNotSupportedException{
		try{
			CPUInfo cpuInfo = systemGateway.getCPUInfo();
			tracer.trace(cpuInfo.getTotalProcessors(),"Total Processors",StringHelper.append(tracingNameSpace, true, "CPU"));
			tracer.trace((long)(cpuInfo.getTotalUtilization()),"Total Utilization %",StringHelper.append(tracingNameSpace, true, "CPU"));
			
			List<ProcessorUsageInfo> pInfoList = cpuInfo.getProcessorUsageInfo();
			int i=0;
			for(ProcessorUsageInfo pInfo: pInfoList){
				tracer.traceSticky((long)(pInfo.getUser()),"Utilization %",StringHelper.append(tracingNameSpace, true, "CPU", "Processor_"+i));
				i++;
			}
		}catch(OSException osex){
			if(logErrors)
				log.error("An error occured while collecting CPUInfo information for OSCollector: ["+this.getBeanName()+"]", osex);
			throw osex;
		}catch(MethodNotSupportedException mniex){
			if(logErrors)
				log.error("This method is not supported by current implementation of ISystem interface passed to the OSCollector : ["+this.getBeanName()+"]", mniex);
			throw mniex;
		}		
	}

	protected void traceMatchedServices(ServiceInfo sInfo){
        log.debug(sInfo);
        tracer.trace(sInfo.getStatus().equalsIgnoreCase("Running")?1:0, "Availability", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        tracer.trace(sInfo.getDisplayName(), "Display Name", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        tracer.trace(sInfo.getStartupType(), "Startup Type", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        tracer.trace(sInfo.getStatus(), "Status", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        if(sInfo.getPid()>0){
        	tracer.trace(sInfo.getPid()+"", "PID", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        	tracer.trace(new Date(sInfo.getStartTime()), "Start Time", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
            tracer.traceSticky(sInfo.getMemory(), "Memory", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
            tracer.traceSticky(sInfo.getThreads(), "Threads", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        }else{
        	tracer.trace("-1", "PID", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        	tracer.trace(new Date(0), "Start Time", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
            tracer.traceSticky(0l, "Memory", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
            tracer.traceSticky(0l, "Threads", StringHelper.append(tracingNameSpace, true, "Service",sInfo.getServiceName()));
        }
        tracedServices.put(sInfo.getServiceName(), sInfo);

	}	

	/*
	 * Returns the current version of OSCollector 
	 */
	@JMXAttribute (name="CollectorVersion", description="Displays OS Collector Version", mutability=AttributeMutabilityOption.READ_ONLY)
	public String getCollectorVersion() {
		return "OSCollector v. " +  OS_COLLECTOR_VERSION;
	}


	public void startCollector() throws CollectorStartException {
		if(systemGateway==null)
			throw new CollectorStartException("An error occured while starting OSCollector ["+this.getBeanName()+"]");
	}
	
	public void traceOSInfo() throws OSException, MethodNotSupportedException{
		try{
			OSInfo osInfo = systemGateway.getOSInfo();
			if(osInfo!=null){
				tracer.trace(osInfo.getArchitecture(),"Architecture",StringHelper.append(tracingNameSpace,true,"Operating System"));
				tracer.trace(osInfo.getName(),"Name",StringHelper.append(tracingNameSpace,true,"Operating System"));
				tracer.trace(osInfo.getVersion(),"Version",StringHelper.append(tracingNameSpace,true,"Operating System"));
				tracer.trace(osInfo.getVendor(),"Vendor",StringHelper.append(tracingNameSpace,true,"Operating System"));
				tracer.trace(osInfo.getVendorVersion(),"Vendor Version",StringHelper.append(tracingNameSpace,true,"Operating System"));
				tracer.trace(osInfo.getDescription(),"Description",StringHelper.append(tracingNameSpace,true,"Operating System"));
				tracer.trace(osInfo.getPatchLevel(),"Patch Level",StringHelper.append(tracingNameSpace,true,"Operating System"));
				tracer.trace(osInfo.getDataModel(),"Data Model",StringHelper.append(tracingNameSpace,true,"Operating System"));
			}
		}catch(OSException osex){
			if(logErrors)
				log.error("An error occured while collecting OS information for OSCollector: ["+this.getBeanName()+"]", osex);
			//throw osex;
		}catch(MethodNotSupportedException mniex){
			if(logErrors)
				log.error("This method is not supported by current implementation of ISystem interface passed to the OSCollector : ["+this.getBeanName()+"]", mniex);
			//throw mniex;
		}
	}
	
	public void traceNetworkInfo() throws OSException, MethodNotSupportedException{
		try{
			NetworkInfo netInfo = systemGateway.getNetworkInfo();
			if(netInfo!=null){
				tracer.trace(netInfo.getDefaultGateway(),"Default Gateway",StringHelper.append(tracingNameSpace,true,"Network","Configuration"));
				tracer.trace(netInfo.getPrimaryDNS(),"Primary DNS",StringHelper.append(tracingNameSpace,true,"Network","Configuration"));
				tracer.trace(netInfo.getSecondaryDNS(),"Secondary DNS",StringHelper.append(tracingNameSpace,true,"Network","Configuration"));
				tracer.trace(netInfo.getDomain(),"Domain Name",StringHelper.append(tracingNameSpace,true,"Network","Configuration"));
				tracer.trace(netInfo.getHost(),"Host Name",StringHelper.append(tracingNameSpace,true,"Network","Configuration"));
				
				List<NetworkInterface> interfaces = netInfo.getInterfaces();
				for(NetworkInterface ni: interfaces){
					tracer.trace(ni.getDescription(),"Description",StringHelper.append(tracingNameSpace, true, "Network","Interface", ni.getName()));
					tracer.trace(ni.getType(),"Type",StringHelper.append(tracingNameSpace, true, "Network","Interface", ni.getName()));
					tracer.trace(ni.getAddress(),"Address",StringHelper.append(tracingNameSpace, true, "Network","Interface", ni.getName()));
					tracer.trace(ni.getHardwareAddress(),"Hardware Address",StringHelper.append(tracingNameSpace, true, "Network","Interface", ni.getName()));
					tracer.trace(ni.getBroadcast(),"Broadcast",StringHelper.append(tracingNameSpace, true, "Network","Interface", ni.getName()));
				}
			}
		}catch(OSException osex){
			if(logErrors)
				log.error("An error occured while collecting Network information for OSCollector: ["+this.getBeanName()+"]", osex);
			//throw osex;
		}catch(MethodNotSupportedException mniex){
			if(logErrors)
				log.error("This method is not supported by current implementation of ISystem interface passed to the OSCollector : ["+this.getBeanName()+"]", mniex);
			//throw mniex;
		}
	}

	public void traceMemoryInfo() throws OSException, MethodNotSupportedException{
		try{
			MemoryInfo mInfo = systemGateway.getMemoryInfo();
			tracer.traceSticky(mInfo.getTotal(),"Total",StringHelper.append(tracingNameSpace, true, "Memory"));
			tracer.traceSticky(mInfo.getUsed(),"Used",StringHelper.append(tracingNameSpace, true, "Memory"));
			tracer.traceSticky(mInfo.getFree(),"Free",StringHelper.append(tracingNameSpace, true, "Memory"));
			tracer.traceSticky((int)(mInfo.getUsedPercent()),"Used %",StringHelper.append(tracingNameSpace, true, "Memory"));
		}catch(OSException osex){
			if(logErrors)
				log.error("An error occured while collecting Memory information for OSCollector: ["+this.getBeanName()+"]", osex);
			//throw osex;
		}catch(MethodNotSupportedException mniex){
			if(logErrors)
				log.error("This method is not supported by current implementation of ISystem interface passed to the OSCollector : ["+this.getBeanName()+"]", mniex);
			//throw mniex;
		}
	}	
	
	public void traceFSUsageInfo() throws OSException, MethodNotSupportedException{
		try{
			List<FileSystemInfo> results = systemGateway.getFileSystemInfo();
			if(results!=null){
				for(FileSystemInfo fInfo: results){
			        tracer.trace(fInfo.getSystemType() + "-" + fInfo.getType(),"Type",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
			        tracer.trace(fInfo.getMountedOn(),"Mounted On",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
					tracer.traceSticky(fInfo.getTotal(),"Total (kb)",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
					tracer.traceSticky(fInfo.getFree(),"Free (kb)",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
			        tracer.traceSticky(fInfo.getUsed(),"Used (kb)",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
			        tracer.trace((int)(fInfo.getUsedPercentage()),"Used %",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
			        tracer.traceSticky(fInfo.getTotalReads(),"Total Reads",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
			        tracer.traceSticky(fInfo.getTotalWrites(),"Total Writes",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
			        tracer.traceSticky(fInfo.getTotalBytesRead(),"Total Bytes Read",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
			        tracer.traceSticky(fInfo.getTotalBytesWritten(),"Total Bytes Written",StringHelper.append(tracingNameSpace, true, "Disk Usage", filterSpecial(fInfo.getDeviceName())));
				}
			}			
		}catch(OSException osex){
			if(logErrors)
				log.error("An error occured while collecting FileSystem information for OSCollector: ["+this.getBeanName()+"]", osex);
			//throw osex;
		}catch(MethodNotSupportedException mniex){
			if(logErrors)
				log.error("This method is not supported by current implementation of ISystem interface passed to the OSCollector : ["+this.getBeanName()+"]", mniex);
			//throw mniex;
		}

 	}

    /**
     * This method will be removed once OpenTracer implements filtering special character
     * @param source
     * @return
     */
    private String filterSpecial(String source){
    	String result = "";
    	if(source!=null){
    		result=source.replace(':', ';');
    	}
    	return result;
    }
    
	/**
	 * @return the processMatcher
	 */
	public String[] getProcessMatcher() {
		return processMatcher.clone();
	}

	/**
	 * @param processMatcher the processMatcher to set
	 */
	public void setProcessMatcher(String[] processMatcher) {
		this.processMatcher = processMatcher;
	}


	/**
	 * @return the staticInfoProcessed
	 */
	public boolean isStaticInfoProcessed() {
		return staticInfoTraced;
	}


	/**
	 * @return the serviceMatcher
	 */
	public String[] getServiceMatcher() {
		return serviceMatcher.clone();
	}

	/**
	 * @param serviceMatcher the serviceMatcher to set
	 */
	public void setServiceMatcher(String[] serviceMatcher) {
		this.serviceMatcher = serviceMatcher;
	}

	/**
	 * @return the systemGateway
	 */
	public ISystem getSystemGateway() {
		return systemGateway;
	}

	/**
	 * @param systemGateway the systemGateway to set
	 */
	public void setSystemGateway(ISystem systemGateway) {
		this.systemGateway = systemGateway;
	}

	/**
	 * @return the traceStaticInfo
	 */
	public boolean isTraceStaticInfo() {
		return traceStaticInfo;
	}

	/**
	 * @param traceStaticInfo the traceStaticInfo to set
	 */
	public void setTraceStaticInfo(boolean traceStaticInfo) {
		this.traceStaticInfo = traceStaticInfo;
	}

}
