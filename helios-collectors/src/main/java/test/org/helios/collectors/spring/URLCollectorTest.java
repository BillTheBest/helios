
package test.org.helios.collectors.spring;

import java.util.HashMap;
import java.util.Iterator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.helios.collectors.exceptions.CollectorException;
import org.helios.collectors.url.URLCollector;

/**
 * <p>Title: URLCollectorTest</p>
 * <p>Description: Test URLCollectors</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class URLCollectorTest {
	protected BeanFactory beanFactory = null;
	protected HashMap<String, URLCollector> urlCollectors = null;
	
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
		// Call Start on each URlCollector
		Iterator<URLCollector> iter = urlCollectors.values().iterator();
		while(iter.hasNext()){
			URLCollector urlC = (URLCollector)iter.next();
			urlC.start();
		}		
		// Now let's call collect on each one of them in a loop
		for(int i = 0; i < 1000; i++) {
			try {
				iter = urlCollectors.values().iterator();
				while(iter.hasNext()){
					URLCollector urlC = (URLCollector)iter.next();
					urlC.collect();
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
	 * @return the urlCollectors
	 */
	public HashMap<String, URLCollector> getUrlCollectors() {
		return urlCollectors;
	}

	/**
	 * @param urlCollectors the urlCollectors to set
	 */
	public void setUrlCollectors(HashMap<String, URLCollector> urlCollectors) {
		this.urlCollectors = urlCollectors;
	}

	
}
