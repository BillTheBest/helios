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
package org.helios.ot.generic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: GenericMetricFactory</p>
 * <p>Description: A factory for registering {@link IGenericMetricTranslator}s and executing foreign metric conversions.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.ot.generic.GenericMetricFactory</code></p>
 */

public class GenericMetricFactory {
	/** A map of {@link IGenericMetricTranslator}s keyed by the full class name of the type the translator supports the translation of. */
	private static final Map<String, IGenericMetricTranslator> translators = new ConcurrentHashMap<String, IGenericMetricTranslator>();
	
	/** An empty IGenericMetric array */
	public static final IGenericMetric[] EMPTY_ARR = new IGenericMetric[0];
	
	/**
	 * Registers a new or replaces an existing metric translator for the passed class name
	 * @param className The class name of the type the translator supports the translation of
	 * @param translator The translator to convert instances of the named classes to instances of {@link IGenericMetric}.
	 */
	public static void registerTranslator(String className, IGenericMetricTranslator translator) {
		if(className==null) throw new IllegalArgumentException("The passed class name was null", new Throwable());
		if(translator==null) throw new IllegalArgumentException("The passed translator was null", new Throwable());
		translators.put(className.trim(), translator);
	}
	
	/**
	 * Converts the passed array of foreign metric representations into an array of {@llink IGenericMetric}s
	 * @param metrics an array of foreign metric representations 
	 * @return an array of {@llink IGenericMetric}s
	 */
	public static IGenericMetric[] translate(Object...metrics) {
		if(metrics==null || metrics.length<1) return EMPTY_ARR;
		IGenericMetricTranslator translator = translators.get(metrics[0].getClass().getName());
		if(translator==null) throw new IllegalStateException("No translator registered for foreign metric type [" + metrics[0].getClass().getName() + "]", new Throwable());
		return translator.translate(metrics);
	}
}
