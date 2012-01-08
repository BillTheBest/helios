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
package org.helios.spring.container.templates.hibernate;

import java.util.Collections;

import org.apache.log4j.Logger;
import org.helios.spring.container.templates.Provision;
import org.helios.spring.container.templates.TemplateProvider;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.BeanNameAware;

/**
 * <p>Title: HibernateTemplateProvider</p>
 * <p>Description: TemplateProvider implementation that aquires provision configuration from Hibernate.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
//@Transactional
public class HibernateTemplateProvider implements TemplateProvider, BeanNameAware {
	protected Logger log = Logger.getLogger(getClass());
	protected SessionFactory sessionFactory = null;
	protected String beanName = null;
	protected Provision[] provisions = null;
	protected String contextName = null;
	
	/**
	 * @param contextName the contextName to set
	 */
	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	//@Transactional(readOnly=true)
	public void start() {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			//session = sessionFactory.getCurrentSession();
			if(log.isDebugEnabled()) log.debug("Acquired Hibernate Session [" + session + "]");
			Query nq = session.getNamedQuery("ContextByName");
			if(log.isDebugEnabled()) log.debug("Acquired Named Query [" + nq + "]");
			nq.setString(0, contextName);
			Context context = (Context)nq.uniqueResult();
			if(log.isDebugEnabled()) log.debug("Acquired Context from Hibernate [" + context + "]");
			provisions = context.getBeans().toArray(new Provision[0]);
			if(log.isDebugEnabled()) log.debug("Generated [" + provisions.length + "] provisions.");
			
		} catch (Exception e) {
			log.error("Failed to create provision facotry", e);
		} finally {
			try { session.close(); } catch (Exception e) {}
		}
	}

	/**
	 * @return
	 * @see org.helios.spring.container.templates.TemplateProvider#getProvisions()
	 */
	public Provision[] getProvisions() {
		Provision[] p = new Provision[provisions.length];
		System.arraycopy(provisions, 0, p, 0, provisions.length);
		return p;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
		log = Logger.getLogger(getClass().getName() + "." + beanName);
		
	}

	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

}
