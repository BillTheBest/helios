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
package org.helios.collectors;

import org.helios.collectors.AbstractCollector.CollectorState;
import org.helios.collectors.exceptions.CollectorException;
import org.helios.collectors.exceptions.CollectorInitException;
import org.helios.collectors.exceptions.CollectorStartException;

/**
 * <p>Title: ICollector</p>
 * <p>Description: Interface for abstract base class for each Helios Collectors.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public interface ICollector {

	public void preInit() throws CollectorInitException;
	public void init() throws CollectorInitException;
	public void initCollector() throws CollectorInitException;
	public void postInit() throws CollectorInitException;
	
	public void start() throws CollectorStartException, CollectorInitException;
	public void preStart() throws CollectorStartException;
	public void startCollector() throws CollectorStartException;
	public void postStart() throws CollectorStartException;;	

	public void collect() throws CollectorException;
	public void preCollect();
	public CollectionResult collectCallback();
	public void postCollect();	
	
	public void stop() throws CollectorException;
	public void preStop();
	public void stopCollector();
	public void postStop();
	
	public void reset() throws CollectorException;
	public void preReset();
	public void resetCollector();
	public void postReset();	
	
	public void destroy() throws CollectorException;
	public void preDestroy();
	public void destroyCollector();
	public void postDestroy();	
	
	public boolean isRunning();
	public CollectorState getState();
	public String displayInternalLog();

}
