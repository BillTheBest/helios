package test.org.helios.collectors.spring;

import java.util.HashMap;
import java.util.Iterator;

import org.helios.collectors.exceptions.CollectorException;
import org.helios.collectors.apache.ApacheModStatusCollector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

public class ApacheModStatusCollectorTest {
	protected BeanFactory beanFactory = null;
	protected HashMap<String, ApacheModStatusCollector> apacheCollectors = null;
	
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
		// Call Start on each ApacheModStatusCollector
		Iterator<ApacheModStatusCollector> iter = apacheCollectors.values().iterator();
		while(iter.hasNext()){
			ApacheModStatusCollector apacheC = (ApacheModStatusCollector)iter.next();
			apacheC.start();
		}		
		// Now let's call collect on each one of them in a loop
		for(int i = 0; i < 1000; i++) {
			try {
				iter = apacheCollectors.values().iterator();
				while(iter.hasNext()){
					ApacheModStatusCollector apacheC = (ApacheModStatusCollector)iter.next();
					System.out.println(apacheC.toString());
					apacheC.collect();
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
	 * @return the apacheCollectors
	 */
	public HashMap<String, ApacheModStatusCollector> getApacheCollectors() {
		return apacheCollectors;
	}

	/**
	 * @param apacheCollectors the apacheCollectors to set
	 */
	public void setApacheCollectors(HashMap<String, ApacheModStatusCollector> apacheCollectors) {
		this.apacheCollectors = apacheCollectors;
	}
}
