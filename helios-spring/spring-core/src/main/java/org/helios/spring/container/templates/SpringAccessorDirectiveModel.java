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
package org.helios.spring.container.templates;

import java.beans.Introspector;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.helios.helpers.BeanHelper;
import org.helios.reflection.PrivateAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * <p>Title: SpringAccessorDirectiveModel</p>
 * <p>Description: A FreeMarker directive that exposes internal constructs </p>
 * <p>Parameter Options:<ul>
 * 	<li><b>serviceType</b>: The service type to access.<ul>
 * 		<li><b>counter</b>: An internal counter in scope for a single template.<ul>
 * 			<li><b>counterName</b>: The name of the counter to increment and retrieve from.</li>
 * 		</ul></li>
 * 		<li><b>spring</b>: An accessor to retrieve configuration from an existing bean.<ul>
 *          <li><b>beanName</b>: The target spring bean name to access.</li>
 * 			<li><b>springType</b>: The target source to access.<ul>
 * 				<li><b>bean</b>: Uses the spring bean to execute accessors against.</li>
 * 				<li><b>descriptor</b>: Uses the spring bean's descriptor to execute accessors against.</li>
 *          </ul></li>
 *          <li><b>springAttr</b>: The atribute of the target spring bean name to access.</li>
 *      </ul></li>    
 * 		<li><b>jmx</b>: An accessor to retrieve attributes from registered MBeans.<ul>
 * 			<li><b>objectName</b>: The JMX object name of the MBean to access.</li>
 * 			<li><b>attributeName</b>: The attribute name to retrieve from the named MBean.</li>
 * 		</ul></li>
 * 		<li><b>provider</b>: An accessor to a template provider that provides pojos containing the template data parameters.<ul>
 * 			<li><b>serviceName</b>: The Spring bean name of a template provider.</li>
 * 			<li><b>providerName</b>: The parameter to the template provider.</li>
 * 		</ul></li>
 * 	</li></ul>
 * </ul></p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class SpringAccessorDirectiveModel implements TemplateDirectiveModel {
	public static enum ParamServiceType {counter, spring, jmx, provider};
	public static enum SpringType {bean, descriptor};
	public static final String PARAM_SERVICE_TYPE = "servicetype";
	public static final String PARAM_SPRING_TYPE = "springtype";
	public static final String PARAM_SPRING_ATTR = "springattr";
	public static final String PARAM_COUNTER_NAME = "countername";
	public static final String PARAM_SPRING_BEAN_NAME = "beanname";
	public static final String PARAM_JMX_OBJECT_NAME = "objectname";
	public static final String PARAM_JMX_ATTRIBUTE_NAME = "attributename";
	public static final String PARAM_TEMPLATE_PROVIDER_NAME = "templatename";
	
	public static final String PARAM_PROVIDER_KEY = "key";
	public static final String PARAM_PROVIDER_KEY_INDEX = "index";
	
	protected static final Logger LOG = Logger.getLogger(SpringAccessorDirectiveModel.class);
	
	protected AbstractApplicationContext appContext = null;
	
	/**
	 * @param appContext
	 */
	public SpringAccessorDirectiveModel(ApplicationContext appContext) {
		this.appContext = (AbstractApplicationContext) appContext;
	}

	/**
	 * Executes this user-defined directive; called by FreeMarker when the user-defined directive is called in the template. 
	 * @param env the current processing environment. Note that you can access the output Writer by Environment.getOut().
	 * @param params the parameters (if any) passed to the directive as a map of key/value pairs where the keys are String-s and the values are TemplateModel instances. This is never null. If you need to convert the template models to POJOs, you can use the utility methods in the DeepUnwrap class.
	 * @param loopVars an array that corresponds to the "loop variables", in the order as they appear in the directive call. ("Loop variables" are out-parameters that are available to the nested body of the directive; see in the Manual.) You set the loop variables by writing this array. The length of the array gives the number of loop-variables that the caller has specified. Never null, but can be a zero-length array.
	 * @param body an object that can be used to render the nested content (body) of the directive call. If the directive call has no nested content (i.e., it is like [@myDirective /] or [@myDirective][/@myDirective]), then this will be null. 
	 * @throws TemplateException
	 * @throws IOException
	 * @see freemarker.template.TemplateDirectiveModel#execute(freemarker.core.Environment, java.util.Map, freemarker.template.TemplateModel[], freemarker.template.TemplateDirectiveBody)
	 */
	@SuppressWarnings("unchecked")
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, String> parameters = new HashMap<String, String>();
		Iterator paramIter = params.entrySet().iterator();		 
        while (paramIter.hasNext()) {
            Map.Entry ent = (Map.Entry) paramIter.next();            
            String key = (String) ent.getKey();
            String value = ent.getValue().toString();
            parameters.put(key.toLowerCase(), value);
        }
        String serviceType = parameters.get(PARAM_SERVICE_TYPE);
        ParamServiceType requestType = null;
        try {  requestType = ParamServiceType.valueOf(serviceType); } catch (Exception e) { LOG.error("Unrecognized service type [" + serviceType + "]"); }
        LOG.info("Request Type:" + requestType);     
        try {
	        if(requestType.equals(ParamServiceType.spring)) {
	        	processSpringRequest(parameters, env.getOut());
	        } else if(requestType.equals(ParamServiceType.provider)) {
	        	processProviderRequest(env, parameters, env.getOut());
	        }
        } catch (Exception e) {
        	LOG.error("Failed to process template request", e);
        }
        
        if(body!=null) body.render(env.getOut());
	}
	
	protected void processProviderRequest(Environment env, Map<String, String> params, Writer out) throws Exception  {
		LOG.info("Processing Provider Request");
//		public static final String PARAM_PROVIDER_KEY = "key";
//		public static final String PARAM_PROVIDER_KEY_INDEX = "index";
		
		String key = params.get(PARAM_PROVIDER_KEY);
		if(key==null) {
			throw new Exception("Provider Request Key was Null");
		}
		Object providerObj = env.getVariable("hpprovider");
		if(providerObj!= null) {
			LOG.info("Provider Object:" + providerObj.getClass().getName());
		}
		String indexVal = params.get(PARAM_PROVIDER_KEY_INDEX);

		int index = -1;
		try {
			if(indexVal != null) index = Integer.parseInt(indexVal);
		} catch (Exception e) {
			throw new Exception("Provider Request had invalid index value [" + indexVal + "]");
		}
		
	}
	
	protected void processSpringRequest(Map<String, String> params, Writer out) throws Exception  {
		LOG.info("Processing Spring Request");
		String st = params.get(PARAM_SPRING_TYPE);
		String beanName = params.get(PARAM_SPRING_BEAN_NAME);
		if(beanName==null) {
			throw new Exception("No [" + PARAM_SPRING_BEAN_NAME + "] defined for spring request");
		}
		String attrName = params.get(PARAM_SPRING_ATTR);
		if(attrName==null) {
			throw new Exception("No [" + PARAM_SPRING_ATTR + "] defined for spring request");
		}		
		SpringType springType = null;
		
		if(st==null) throw new Exception("Spring service type requested but no [" + PARAM_SPRING_TYPE + "] defined");
		try {  springType = SpringType.valueOf(st); } catch (Exception e) { LOG.error("Unrecognized spring type [" + st + "]"); throw e; }
		LOG.info("Spring Request:\n\tType:[" + springType + "]\n\tBean Name:" + beanName + "\n\tAttribute:" + attrName);
		Object target = null;
		if(springType.equals(SpringType.bean)) {
			target = appContext.getBean(beanName);
		} else if(springType.equals(SpringType.descriptor)) {
			target = appContext.getBeanFactory().getBeanDefinition(beanName);
		} else {
			throw new Exception("Unsupported spring type [" + springType + "]");
		}
		
		Object retValue = BeanHelper.getAttribute(attrName, target);
		//Object retValue = PrivateAccessor.invoke(target, "get" + attrName);
		
		out.append(retValue.toString());

	}
}
