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
package org.helios.spring.container.templates.provider.persistence;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.helios.helpers.StringHelper;
import org.helios.spring.container.templates.provider.ITemplateProvider;
import org.helios.spring.container.templates.provider.ITemplateProvision;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * <p>Title: HibernateTemplateProviderService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class HibernateTemplateProviderService implements ITemplateProvider {
	protected SessionFactory sessionFactory = null;
	protected Logger log = Logger.getLogger(getClass());
	
	/**
	 * @param sessionFactory
	 */
	public HibernateTemplateProviderService(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		log.info("Instantiated HibernateProviderService");
	}


	/**
	 * @param name
	 * @return
	 * @see org.helios.spring.container.templates.provider.ITemplateProvider#getProvisions(java.lang.String)
	 */
	public Set<ITemplateProvision> getProvisions(String name) {
		Session session = null;
		//Transaction tx = null;
		try {
			session = sessionFactory.openSession();
			//tx = session.beginTransaction();
			Query query = session.getNamedQuery("GetProvisionByName");
			
			Provision p = null;
			do {
				query.setString(0, name);
				p = (Provision)query.uniqueResult();
				if(p==null) {
					String[] frags = name.split("\\.");
					if(frags!=null && frags.length > 0) {
						name = StringHelper.flattenArray(StringHelper.compileRange("0-" + (frags.length-2)), ".", frags);
					} else {
						log.warn("No provision set found for name [" + name + "]");
						return null;
					}					
				}
			} while(p==null);
			Set<ITemplateProvision> it = new HashSet<ITemplateProvision>(p.getProvisionSets());
			return it;
		} catch (Exception e) {
			log.error("Failed to retrieve provisions for [" + name + "]");
		} finally {
			//try { tx.commit(); } catch (Exception e) {}
			try { session.close(); } catch (Exception e) {}
		}
		return null;
	}

}
