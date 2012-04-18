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
package org.helios.collectors.notification;

//import javax.management.Notification;
//import javax.management.ObjectName;
//
//import com.sdicons.json.mapper.JSONMapper;
//import com.sdicons.json.mapper.MapperException;
//import com.sdicons.json.model.JSONValue;

/**
 * <p>Title: NotificationMarshaller</p>
 * <p>Description: Implementation to transform jmx notification object to various other formats like JSON, String, XML etc.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class NotificationMarshaller{

//	public static JSONValue toJSON(Notification notification) throws NotificationMarshallerException {
//    	try{
//    		return JSONMapper.toJSON(notification);
//    	}catch(MapperException mex){
//    		throw new NotificationMarshallerException(mex);
//    	}
//	}
	
//	   "message" : "STOPPED",
//	   "sequenceNumber" : 2,
//	   "source" :
//	      {
//	         "canonicalKeyPropertyListString" : "name=LocalCollector,type=OSCollector",
//	         "canonicalName" : "org.helios.collectors:name=LocalCollector,type=OSCollector",
//	         "domain" : "org.helios.collectors",
//	         "domainPattern" : false,
//	         "keyPropertyList" :
//	            {
//	               "name" : "LocalCollector",
//	               "type" : "OSCollector"
//	            },
//	         "keyPropertyListString" : "type=OSCollector,name=LocalCollector",
//	         "pattern" : false,
//	         "propertyPattern" : false
//	      },
//	   "timeStamp" : 1237406154085,
//	   "type" : "org.helios.collectors.AbstractCollector.CollectorState",
//	   "userData" : null
	
	
//	public static String toString(Notification notification) throws NotificationMarshallerException {
//		StringBuilder builder = new StringBuilder("");
//		builder.append("message^"+notification.getMessage()+"\n");
//		builder.append("sequenceNumber^"+notification.getSequenceNumber()+"\n");
//		builder.append("timestamp^"+notification.getTimeStamp()+"\n");
//		builder.append("type^"+notification.getType()+"\n");
//		builder.append("userData^"+notification.getUserData()==null?"null":notification.getUserData()+"\n");
//		if(notification.getSource() instanceof ObjectName){
//			ObjectName oName = (ObjectName)notification.getSource();
//			builder.append("\tcanonicalKeyPropertyListString^"+oName.getCanonicalKeyPropertyListString()+"\n");
//			builder.append("\tcanonicalName^"+oName.getCanonicalName()+"\n");
//			builder.append("\tdomain^"+oName.getDomain()+"\n");
//			builder.append("\tkeyPropertyListString^"+oName.getKeyPropertyListString()+"\n");
//			builder.append("\tpattern^"+oName.isPattern()+"\n");
//			builder.append("\tdomainPattern^"+oName.isDomainPattern()+"\n");
//			builder.append("\tpropertyPattern^"+oName.isPropertyPattern()+"\n");
//		}
//		return builder.toString();
//	}

}
