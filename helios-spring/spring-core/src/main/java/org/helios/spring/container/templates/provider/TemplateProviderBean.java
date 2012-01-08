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
package org.helios.spring.container.templates.provider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.helios.spring.container.templates.provider.persistence.Provision;

/**
 * <p>Title: TemplateProviderBean</p>
 * <p>Description: A basic implementation of an <code>ITemplateProvider</code> as a bean.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class TemplateProviderBean implements ITemplateProvider {
	/**
	 * Bands
	 *      Beatles
	 *             NVP 1
	 *             NVP 2
	 *             NVP n
	 * 
	 */
			  // "Bands"   Set<Bands>
	protected Map<String, Provision> provisions = new HashMap<String, Provision>();
	
	/**
	 * Constructs an empty TemplateProviderBean
	 */
	public TemplateProviderBean() {}
	
	/**
	 * Constructs a TemplateProviderBean and loads it with the contents of the passed map.
	 * @param provs
	 */
	public TemplateProviderBean(Set<Map<String, Provision>> provs) {
		for(Map<String, Provision> prov: provs) {
			provisions.putAll(prov);
		}		
	}
	
//	/**
//	 * Builds a TemplateProviderBean from the passed map of maps using the default ITemplateProvision.
//	 * @param provisionMaps A map of maps.
//	 * @return A new TemplateProviderBean
//	 */											// Bands     //Beatles   //Set<nvp>
//	public static TemplateProviderBean build(Map<String, Map<String, Map<String, Object>>> provisionMaps) {
//		TemplateProviderBean tpb = new TemplateProviderBean();
//		for(Map.Entry<String, Map<String, Map<String, Object>>> bands: provisionMaps.entrySet()) {
//			Map<String, Set<ITemplateProvision>> myBands = new HashMap<String, Set<ITemplateProvision>>();
//			tpb.provisions.put(bands.getKey(), myBands);
//			for(Map.Entry<String,Map<String,Object>> band: bands.getValue().entrySet()) {
//				Set<ITemplateProvision> myBand = new HashSet<ITemplateProvision>();
//				myBands.put(band.getKey(), myBand);
//				ITemplateProvision prov = new TemplateProvisionImpl(band.getKey(), band.getValue());
//				myBand.add(prov);
//			}
//		}		
//		return tpb;
//	}
	
	/**
	 * @param name
	 * @param provs
	 */
	public void addProvisions(String name, ITemplateProvision...provs) {
//		Set<ITemplateProvision> set = provisions.get(name);
//		if(set==null) {
//			set = new HashSet<ITemplateProvision>();
//			provisions.put(name, set);
//		}
//		if(provs!=null) {
//			Collections.addAll(set, provs);
//		}
	}
	
	/**
	 * @param name
	 * @param provs
	 */
	public void addProvisions(String name, Set<ITemplateProvision> provs) {
		if(provs!=null) {
			addProvisions(name, provs.toArray(new ITemplateProvision[provs.size()]));
		}
	}
	
	

	
	
	/**
	 * Gets a set of provisions for the passed name.
	 * @param name The logical name of the provision.
	 * @return A set of provisions which may be zero sized. If the name is not bound, returns null.
	 * @see org.helios.spring.container.templates.provider.ITemplateProvider#getProvisions(java.lang.String)
	 */
	
	/*  Set<Bands> getProvisions(band) */
	
	public Set<ITemplateProvision> getProvisions(String name) {		
		return new HashSet<ITemplateProvision>(provisions.get(name).getProvisionSets());
	}

}
