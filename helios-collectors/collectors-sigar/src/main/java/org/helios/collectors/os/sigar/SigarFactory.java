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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarLoader;

/**
 * <p>Title: SigarFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SigarFactory {

	/** Single JVM Sigar reference */
	protected static volatile Sigar sigar = null;
	/** Singleton lock */
	protected static Object lock = new Object();
	/** Override temp directory property name */
	protected static final String DEPLOY_DIR_PROP="org.helios.sigar.libdir";
	
	public static Sigar getSigar() {
		if(sigar!=null) return sigar;
		synchronized(lock) {
			if(sigar!=null) return sigar;
			else {
				try {
					loadSigarLib();
					if(sigar!=null) return sigar;
					else throw new Exception("Failed to load native library.");
				} catch (Exception e) {
					throw new RuntimeException("Failed to load native library.", e);
				}
			}
		}
	}	
	
    protected static void loadSigarLib() throws Exception {
    	String outDirName = System.getProperty(DEPLOY_DIR_PROP, System.getProperty("java.io.tmpdir"));
    	File outDir = new File(outDirName);
    	if(!outDir.isDirectory()) {
    		outDir = new File(System.getProperty("java.io.tmpdir"));
    	}
    	if(!outDir.isDirectory()) {
    		throw new Exception("Could not locate writable directory for native library");
    	}
    	String libName = getPlatformLibraryName();
        File file = new File(outDir.getCanonicalPath() + File.separator + libName);
        if(!file.exists()) {
	        BufferedInputStream bis = new BufferedInputStream(SigarFactory.class.getClassLoader().getResourceAsStream("native/" + libName));
	        FileOutputStream fos = new FileOutputStream(file, false);
	        BufferedOutputStream bos = new BufferedOutputStream(fos);
	        byte[] buff = new byte[102400];
	        int bytesRead = 0;
	        while((bytesRead = bis.read(buff)) != -1) {
	            bos.write(buff, 0, bytesRead);               
	        }
	        bis.close();
	        bos.flush();
	        fos.flush();
	        fos.close();
        }
        System.load(file.getAbsolutePath());
        PrintStream ep = System.err;
        PrintStream op = System.out;      
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            sigar = new Sigar();
            System.setErr(ep);
        } catch (Throwable e) {
        	throw new Exception("Failed to load native library [" + libName + "]", e);
        } finally {
            System.setErr(ep);
            System.setOut(op);           
        }
    }
    
    public static String getSigarVersion() {
    	return Sigar.VERSION_STRING;
    }
    	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("HeliosSigarFactory");
		try {
			log("Platform Library:" + getPlatformLibraryName());
			Sigar sigar = getSigar();
			log("Acquired Sigar Reference");
			long pid = sigar.getPid();
			log("This process PID:" + pid);
			String proc = sigar.getProcExe(pid).toString();
			log("This process:[" + proc + "]");
			StringBuilder b = new StringBuilder("\n\t=============\n\tSystem Memory Info\n\t=============");
			Mem mem = sigar.getMem();
			b.append("\n\t\tFree %:").append(mem.getFreePercent());
			b.append("\n\t\tUsed %:").append(mem.getUsedPercent());
			b.append("\n\t\tTotal:").append(mem.getTotal());
			log(b);
			
			b = new StringBuilder("\n\t=============\n\tCPU Info\n\t=============");
			int i = 0;
			for(CpuInfo cinfo: sigar.getCpuInfoList()) {
				b.append("\n\t\tCPU:").append(i);
				b.append("\n\t\t\tModel:").append(cinfo.getVendor()).append(":").append(cinfo.getModel());
				b.append("\n\t\t\tSpeed:").append(cinfo.getMhz());
				b.append("\n\t\t\tCache:").append(cinfo.getCacheSize());
			}
			log(b);
			b = new StringBuilder("\n\t=============\n\tNIC Info\n\t=============");
			for(String nic: sigar.getNetInterfaceList()) {
				NetInterfaceConfig nconf = sigar.getNetInterfaceConfig(nic);
				b.append("\n\t\tNIC:").append(nic);
				b.append("\n\t\t\tDescription:").append(nconf.getDescription());
				b.append("\n\t\t\tAddress:").append(nconf.getAddress());
				b.append("\n\t\t\tMAC:").append(nconf.getHwaddr());				
			}
			log(b);
			log("TCP Stats:" + sigar.getTcp().toString());
		} catch (Exception e) {
			System.err.println("Sigar Test Failure. Stack Trace Follows...");
			e.printStackTrace();
		}
	}
	
	public static String getPlatformLibraryName() {
		PrintStream current = System.out;
		try {
			System.setErr(new PrintStream(new ByteArrayOutputStream()));
			return SigarLoader.getNativeLibraryName();
		} catch (Exception e) {
			return null;
		} finally {
			System.setErr(current);
		}
	}
	
	public static void log(Object o) {
		System.out.println(o);
	}

}
