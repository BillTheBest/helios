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
package org.helios.spring.container;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;

/**
 * <p>Title: CLOptions</p>
 * <p>Description: Command line option processor for HeliosContainerMain.</p>
 * <p>Sets the following options when bootstraping the container:<ul>
 * <li>Configuration directories</li>
 * <li>Library directories</li>
 * <li>Classpath resource directories</li>
 * <li>Classloader isolation</li>
 * <li>Daemon thread control</li>
 * <li>Logging configuration</li>
 * <li>Isolated subcontainers</li>
 * <li>URL classloader stubs</li>
 * <li>Primordial configurator</li>
 * <li>Default configuration supression</li>
 * <li>Auto Spring bean JMX surfacing</li>
 * <li>Remote config load</li>
 *  </ul></p>
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.spring.container.CLOptions</code></p>
 */

public class CLOptions {
	
	/** Directories that will be recursed for jar files to add to the container classpath. */
	@Option(name="-r",usage="recursively run something")
	protected List<String> libDirectories = new ArrayList<String>();
}

/*
    @Option(name="-r",usage="recursively run something")
    private boolean recursive;

    @Option(name="-o",usage="output to this file")
    private File out;

    @Option(name="-str")        // no usage
    private String str = "(default value)";

    @Option(name="-n",usage="usage can have new lines in it\n and also it can be long")
    private int num;

    // receives other command line parameters than options
    @Argument
    private List arguments = new ArrayList();

	//==================================================================
	 
try {
    parser.parseArgument(args);
} catch( CmdLineException e ) {
    System.err.println(e.getMessage());
    System.err.println("java -jar myprogram.jar [options...] arguments...");
    parser.printUsage(System.err);
    return;
}
 
 //===================================================================
  
HeliosSpring 1.0-SNAPSHOT
	Usage: java org.helios.spring.container.HeliosContainerMain [-help]: Prints this banner.
	Usage: java org.helios.spring.container.HeliosContainerMain [-conf <configuration directory>] [-lib <jar directory>] [-cpd <directory>] [-isolate]
	-conf and -lib can be repeated more than once.
	-lib will recursively search the passed directory and add any located jar files to the container's classpath.
	-cpd will add the passed directory to the container's classpath.
	-isolate configures the container classpath in a seperate class loader. By default, -cpd and -lib will append to the classpath.
	-daemon keeps the container JVM alive even in the absence of any non-daemon threads. 
	-sd supresses default configurations for lib, cpd and conf. 
	-log4j <log4j xml config file> Configures logging from the specified file.
	Default Settings (use -sd to supress) 
	================
	Conf:	[./conf]
	CP:	[./conf/resource, ./classes]
	Lib:	[./lib, ./libs]
	Log4j:	./conf/log4j/log4j.xml
   

    CL JavaDoc:  java -jar args4j-tools.jar path/to/my/OptionBean.java

	 

*/