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

import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: TemplateProviderImpl</p>
 * <p>Description: Basic bean implementation of the TemplateProvider interface.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class TemplateProviderImpl implements TemplateProvider {
	protected Set<Provision> provisions = new HashSet<Provision>();
	
	/**
	 * 
	 */
	public TemplateProviderImpl() {
		
	}
	
	/**
	 * @param provisions
	 */
	public TemplateProviderImpl(Set<Provision> provisions) {
		super();
		this.provisions = provisions;
	}

	/**
	 * Returns an array of the configured provisions.
	 * @return An array of provisions
	 * @see org.helios.spring.container.templates.TemplateProvider#getProvisions()
	 */
	public Provision[] getProvisions() {
		return provisions.toArray(new Provision[provisions.size()]);
	}

	/**
	 * @param provisions the provisions to set
	 */
	public void setProvisions(Set<Provision> provisions) {
		this.provisions = provisions;
	}

}
