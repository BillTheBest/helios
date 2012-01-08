/**
 * Code Sample for
 * Runtime Performance & Availability Monitoring for Java Systems.
 * IBM DevelperWorks.
 * Nicholas Whitehead (whitehead.nicholas@gmail.com)
 */

package org.helios.collectors.snmp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.zip.Deflater;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.helios.collectors.exceptions.CollectorStartException;
import org.helios.tracing.HeliosTracerInstanceFactory;
import org.helios.tracing.ITracer;
import org.helios.tracing.OpenTraceManager;
//import org.runtimemonitoring.spring.rendering.IRenderer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

/**
 * <p>Title: SpringCollector</p>
 * <p>Description: The bootsrap class for the Spring Collector container</p>
 * @author whitehead.nicholas@gmail.com
 * @version  $Revision$
 */

public class SpringCollector extends TimerTask implements FilenameFilter {
	protected static Logger log = Logger.getLogger(SpringCollector.class);
	//protected DefaultListableBeanFactory appContext = null;
	protected FileSystemXmlApplicationContext appContext = null;
	private	SnmpCollector	snmpCollector;
	/**
	 * Creates a new SpringCollector container.
	 * @param configDir The Spring configuration XML file directory.
	 */
	public SpringCollector(File configDir) {
		
		try {
			log.info("Creating SpringCollector Container using config directory:[" + configDir + "]");
			if(!configDir.exists() || !configDir.isDirectory()) {
				throw new RuntimeException("The directory [" + configDir + "] does not exist or is not a directory");
			}
			String[] locatedFiles =  configDir.list(this);
			for(int i = 0; i < locatedFiles.length; i++) {
				locatedFiles[i] = configDir.getPath() + File.separator + locatedFiles[i];
				log.info("Located Spring Config File:[" + locatedFiles[i] + "]");
			}
			appContext = new FileSystemXmlApplicationContext();
			appContext.refresh();
			// ===================================================
			// replace with a properties file.		
			// ===================================================
			
			appContext.setConfigLocations(locatedFiles);
			appContext.refresh();
			snmpCollector = (SnmpCollector)appContext.getBean("snmpCollector");
			
		} catch (Exception e) {
			log.error("Failed to initialize ApplicationContext", e);
			e.printStackTrace();
			throw new RuntimeException("Failed to initialize ApplicationContext", e);
		}
	}

	public void doWork() throws CollectorStartException{
		try {
			OpenTraceManager.getInstance().startOpenTrace();		
			ITracer localTracer = HeliosTracerInstanceFactory.getInstance();
//			VirtualTracer vtracer = (VirtualTracer)localTracer.getVirtualTracer("www.heliosdev.org", "RemoteHeliosAgent");
			//log("Starting Sampling Loop");
			int i = 1000;
			//Random random = new Random(System.nanoTime());
			while(true) {
	/*			
				long cpuTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				long userTime = ManagementFactory.getThreadMXBean().getCurrentThreadUserTime();
				vtracer.traceDelta(cpuTime, "Current Thread CPU Time (ns)", "JVM", "Threads", "Current Thread");
				vtracer.traceDelta(userTime, "Current Thread User Time (ns)", "JVM", "Threads", "Current Thread");
				localTracer.traceDelta(cpuTime, "Current Thread CPU Time (ns)", "JVM", "Threads", "Current Thread");
				localTracer.traceDelta(userTime, "Current Thread User Time (ns)", "JVM", "Threads", "Current Thread");				
				vtracer.traceDelta(cpuTime, "Elapsed Time", "JVM", "Threads", "Current Thread");
				localTracer.traceDelta(cpuTime, "Elapsed Time", "JVM", "Threads", "Current Thread");
				localTracer.traceDelta(i, "Sequence", "JVM", "Threads", "Current Thread");
				vtracer.traceDelta(i, "Sequence", "JVM", "Threads", "Current Thread");				
				i+=random.nextInt(100);
				Thread.currentThread().join(1000);
				log("Sequence Value:" + i);
		*/
			}
		} catch (Exception e) {
			//log("TEST FAILED. Stack trace follows:");
			e.printStackTrace();
			return;
		}
		/*
		//  Emulate version info
		snmpCollector.getCollectorVersion();
		//	Emulate startup
		try {
			snmpCollector.startCollector();
			Timer	timer = new Timer();
			timer.scheduleAtFixedRate(this, 0, 1000);
			for(int x=0;x<10;++x) {
				synchronized (this) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			timer.cancel();
			//	Exit
			//snmpCollector.closeCollector();
			snmpCollector = null;
		} catch (CollectorStartException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if(snmpCollector != null); //snmpCollector.closeCollector();
			log.info("Exiting collector");
		}
		*/
	}
	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		synchronized (this) {
			snmpCollector.collectCallback();
			this.notify();
		}
	}

	public boolean accept(File dir, String name) {		
		return name.toUpperCase().endsWith(".XML");
	}
	
	public byte[] compressByteArray(byte[] input) {
		    Deflater compressor = new Deflater();
		    compressor.setLevel(Deflater.BEST_COMPRESSION);
		    compressor.setInput(input);
		    compressor.finish();
		    ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
		    byte[] buf = new byte[8096];
		    while (!compressor.finished()) {
		        int count = compressor.deflate(buf);
		        bos.write(buf, 0, count);
		    }
		    try {
		        bos.close();
		    } catch (IOException e) {
		    }
		    byte[] compressedData = bos.toByteArray();
		    return compressedData;
	}

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpringCollector collector=null;
		log.info("SpringCollector BootStrap");
		if(args.length < 1) {
			log.error("Usage: SpringCollector <configFile directory>");
			return;
		}
		String configFile = args[0];
		File f = new File(configFile);
		if(!f.exists()) {
			log.error("Could not read config file directory:" + configFile);
			return;
		}
		log.info("Starting with config file directory:" + configFile);
		try {
			collector = new SpringCollector(f);
		} catch (Exception e) {
			log.error("Failed to boot strap", e);
			return;
		}
		log.info("Starting SNMP collection");
		try {
			collector.doWork();
		} catch (CollectorStartException e) {
			log.error("Failed to do work",e);
		}
	}	

}
