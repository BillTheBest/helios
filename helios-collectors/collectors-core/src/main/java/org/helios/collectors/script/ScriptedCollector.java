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
package org.helios.collectors.script;

import org.helios.collectors.AbstractCollector;
import org.helios.collectors.CollectionResult;
import org.helios.collectors.CollectionResult.Result;
import org.helios.collectors.exceptions.CollectorStartException;
import org.helios.jmx.dynamic.annotations.JMXManagedObject;
import org.helios.scripting.manager.script.ScriptInstance;
import org.helios.version.VersionHelper;

/**
 * <p>Title: ScriptedCollector</p>
 * <p>Description: Collector that delegates to a configured script</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collectors.script.ScriptedCollector</code></p>
 */
@JMXManagedObject (declared=false, annotated=true)
public class ScriptedCollector extends AbstractCollector {
	/** The script to execute */
	protected ScriptInstance scriptInstance = null;

	/**  */
	private static final long serialVersionUID = -2260247752887722164L;

	/**
	 * Creates a new ScriptedCollector
	 */
	public ScriptedCollector() {
		super();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collectors.AbstractCollector#getCollectorVersion()
	 */
	@Override
	public String getCollectorVersion() {
		return VersionHelper.getHeliosVersion(this);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collectors.AbstractCollector#startCollector()
	 */
	@Override
	public void startCollector() throws CollectorStartException {
		// Compile Script
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collectors.AbstractCollector#collectCallback()
	 */
	@Override
	public CollectionResult collectCallback() {
		scriptInstance.getBindings().put("collector", this);
		scriptInstance.getBindings().put("tracer", getTracer());
		scriptInstance.getBindings().put("log", log);
		scriptInstance.exec();
		return new CollectionResult().setResultForLastCollection(Result.SUCCESSFUL);
	}

}