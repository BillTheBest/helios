package test.org.helios.collectors.spring;

import java.util.HashMap;
import java.util.Iterator;

import org.helios.collectors.exceptions.CollectorException;
import org.helios.collectors.webservice.WebServiceCollector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 * <p>Title: WebServiceCollectorTest </p>
 * <p>Description: Test WebServiceCollectors</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class WebServiceCollectorTest {
	protected BeanFactory beanFactory = null;
	protected HashMap<String, WebServiceCollector> wsCollectors = null;
	
	/**
	 * Sets the Spring BeanFactory
	 * @param beanFactory The Spring BeanFactory
	 * @throws BeansException
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
	
	public void startTest() throws CollectorException{
		// Call Start on each WebServiceCollector
		Iterator<WebServiceCollector> iter = wsCollectors.values().iterator();
		while(iter.hasNext()){
			WebServiceCollector wsC = (WebServiceCollector)iter.next();
			wsC.start();
		}		
		// Now let's call collect on each one of them in a loop
		for(int i = 0; i < 1000; i++) {
			try {
				iter = wsCollectors.values().iterator();
				while(iter.hasNext()){
					WebServiceCollector wsC = (WebServiceCollector)iter.next();
					System.out.println(wsC.toString());
					wsC.collect();
				}
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}	
	
	public static void log(Object message) {
		System.out.println(message);
	}

	/**
	 * @return the wsCollectors
	 */
	public HashMap<String, WebServiceCollector> getWsCollectors() {
		return wsCollectors;
	}

	/**
	 * @param wsCollectors the wsCollectors to set
	 */
	public void setWsCollectors(HashMap<String, WebServiceCollector> wsCollectors) {
		this.wsCollectors = wsCollectors;
	}
}
