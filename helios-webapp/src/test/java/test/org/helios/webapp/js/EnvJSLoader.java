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
package test.org.helios.webapp.js;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import org.helios.helpers.FileHelper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * <p>Title: EnvJSLoader</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.webapp.js.EnvJSLoader</code></p>
 */

public class EnvJSLoader {

	/** The name of the file server root directory */
	protected final String fileServerRoot;
	/** The port the file server will listen on */
	protected final int port;
	/** The HTTP File Server Instance */
	protected final HttpServer server;
	/** The file server handler */
	protected final HttpHandler handler;
	/** The script to initialize the env-js browser emulator */
	protected final String initJsScript;
	/** The env-js context */
	protected final Context cx;
	/** The gloabl scope for the context */
	protected final Global global;
	/** The eval function from the JS Engine */
	protected final Function feval;
	/** The EnvJSLoader singleton instance */
	private static volatile EnvJSLoader instance = null;
	/** The EnvJSLoader singleton ctor lock */
	private static final Object lock = new Object();
	/** Serial number factory for http server thread names */
	private static final AtomicInteger serial = new AtomicInteger(0);
	
	/**
	 * Creates the EnvJSLoader singleton instance 
	 * @param fileServerRoot The name of the file server root directory
	 * @param port The port the file server will listen on
	 * @param initJsScript The script to initialize the env-js browser emulator
	 */
	private EnvJSLoader(String fileServerRoot, int port, String initJsScript) throws Exception {
		
		this.fileServerRoot = fileServerRoot;
		this.port = port;
		this.handler = new FileRequestHandler(this.fileServerRoot);
		InetSocketAddress addr = new InetSocketAddress(port);
	    server = HttpServer.create(addr, 0);
	    server.createContext("/", handler);
	    server.setExecutor(Executors.newCachedThreadPool(new ThreadFactory(){
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "EnvJSLoader-HttpServer-Thread#" + serial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
	    }));
	    server.start();
	    log("Server is listening on port [" + port + "]" );
	    if(initJsScript!=null) {
	    	this.initJsScript = initJsScript;
	    } else {
	    	this.initJsScript = "./src/test/resources/env-js/env.rhino.js";
	    }
		
		cx = ContextFactory.getGlobal().enterContext();
		cx.setOptimizationLevel(-1);
		cx.setLanguageVersion(Context.VERSION_1_5);
		global = Main.getGlobal();
		global.init(cx);
		Main.processSource(cx, this.initJsScript);
		log("Loaded env-js");
		feval = getJsFunction("eval");
		eval("Envjs.scriptTypes['text/javascript'] = true;");
		log("Enabled javascript loads");
		log("env-js engine startup complete.");
	}
	
	/**
	 * Acquires the singleton instance of the EnvJSLoader
	 * @param fileServerRoot The name of the file server root directory
	 * @param port The port the file server will listen on
	 * @param initJsScript The script to initialize the env-js browser emulator
	 * @return the EnvJSLoader instance
	 * @throws Exception
	 */
	public static EnvJSLoader getInstance(String fileServerRoot, int port, String initJsScript) throws Exception {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new EnvJSLoader(fileServerRoot, port, initJsScript); 
				}
			}
		} 
		return instance;
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public void stop() {
		log("Stopping http server");
		if(server != null) {
			server.stop(1);
			log("Server stopped");
		}		
	}
	
	public static void main(String[] args) throws Exception {
		
		EnvJSLoader loader = EnvJSLoader.getInstance("./src/main/webapp", 5555, null);
		try {
			loader.loadDocument("test.html");
			loader.loadJs("./src/test/resources/data/bands.js");
			log(loader.printNavMap(loader.getNavMap("bands"), ".*Other.*name.*"));
			Object[] genArr = loader.getJsArray("bands", "The Beatles", "Tags", "Style", "Music", "Genres");
			log("Array:" + Arrays.toString(genArr));
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			loader.stop();
		}		
	}
	
	/**
	 * Converts a nav map to a readable string
	 * @param map The nav map
	 * @return the nav map rendered as a astring
	 */
	public String printNavMap(Map<String, Object> map, String...matchPatterns) {
		Set<Pattern> patterns = new HashSet<Pattern>(matchPatterns==null ? 0 : matchPatterns.length);
		for(String p: matchPatterns) {
			patterns.add(Pattern.compile(p));
		}
		StringBuilder b = new StringBuilder();
		for(Map.Entry<String, Object> entry: map.entrySet()) {
			if(patterns.size()>0) {
				for(Pattern p: patterns) {
					if(p.matcher(entry.getKey()).matches()) {
						b.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
						break;
					}
				}
			} else {
				b.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
			}
			
		}
		return b.toString();
	}
	
	/**
	 * Returns a navigational map for the object referenced by the passed name array
	 * @param names the hierarchical name array
	 * @return the nav map for the object
	 */
	public Map<String, Object> getNavMap(String...names) {		
		Map<String, Object> navMap = new HashMap<String, Object>();
		Object bands = getJsObj(names);
		Object obj = invoke(getJsFunction("recurseData"), bands, null, null, null, null);
		NativeObject no = (NativeObject) obj; 
		for(Object id: no.getAllIds()) {
			navMap.put(id.toString(), no.get(id.toString(), no));
		}
		return navMap;
	}
	
	public static Map<String, Object> convertToMap(NativeObject obj) {
		Map<String, Object> navMap = new HashMap<String, Object>();
		for(Object id: obj.getAllIds()) {
			navMap.put(id.toString(), obj.get(id.toString(), obj));
		}		
		return navMap;
	}
	
	
	
	/**
	 * Navigates the JS Engine DOM according to the passed names and retrieves the located object
	 * @param names The nested names to navigate to the target object
	 * @return the located object
	 */
	public Object getJsObj(String...names) {
		if(names==null ||names.length<1) {
			throw new RuntimeException("No names or sub-names provided");
		} else {
			Object ctx = null;
			StringBuilder b = new StringBuilder("");
			for(String s: names) {
				if(b.length()>1) {
					b.append("/");
				}
				b.append(s);
				try {
					ctx = ((ScriptableObject)((ctx==null) ? global : ctx)).get(s, global);
				} catch (Exception e) {
					throw new RuntimeException("Failed to resolve [" + b.toString() + "]" , e);
				}
			}
			return ctx;
		}
	}
	
	/**
	 * Builds a JS statement from the passed fragments and executes in the global context of the JS Engine
	 * @param frags Fragments of a JS statement that will be concatenated and executed
	 * @return the return value of the eval call
	 */
	public Object eval(CharSequence...frags) {
		if(frags==null || frags.length<1) return null;
		StringBuilder b = new StringBuilder();
		for(CharSequence frag: frags) {
			b.append(frag);
		}
		return feval.call(cx, global, global, new Object[]{b.toString()});
	}
	
	/**
	 * Invokes the passed method
	 * @param f The extracted method
	 * @param args The arguments to the function
	 * @return the return value of the function call
	 */
	public Object invoke(Function f, Object...args) {
		return f.call(cx, global, global, args);
	}
	
	/**
	 * Navigates the JS Engine DOM according to the passed names and retrieves the located function
	 * @param names The nested names to navigate to the target function
	 * @return the located function
	 */
	public Function getJsFunction(String...names) {
		return (Function) getJsObj(names);
	}
	
	/**
	 * Retrieves the referenced object casted as a NativeObject
	 * @param names The nested names to navigate to the target function
	 * @return the located NativeObject
	 */
	public NativeObject getJsObject(String...names) {
		return (NativeObject) getJsObj(names);
	}
	
	public Object[] getJsArray(String...names) {
		Object obj = getJsObj(names);
		if(obj instanceof Undefined) {
			throw new RuntimeException("No object resolved at [" + Arrays.toString(names) + "]");
		}
		if(!(obj instanceof NativeArray)) {
			throw new RuntimeException("Object resolved at [" + Arrays.toString(names) + "] not an array");
		}
		NativeArray arr = (NativeArray) obj;
		int length = (int)arr.getLength();
		Object[] jarr = new Object[length];
		for(int i = 0; i < length; i++) {
			jarr[i] = arr.get(i, arr);
		}
		return jarr;
	}
	
	public static Object[] convertArray(NativeArray arr) {
		if(arr==null) return new Object[]{};
		Object[] newarr = new Object[(int)arr.getLength()];
		for(int i = 0; i < newarr.length; i++) {
			newarr[i] = arr.get(i, arr);
		}
		return newarr;
	}
	
	
	/**
	 * Loads the specified document as the main document in the JS engine
	 * @param uri The URI of the file. eg. <b><code>/test.html</code></b>
	 */
	public void loadDocument(String uri) {
		while(uri.startsWith("/")) {
			uri.replaceFirst("/", "");
		}
		String statement = "window.location = 'http://localhost:" + port + "/" + uri + "'";
		Object result = eval(statement);
		log("Document Loaded [" + result + "]");
	}
	
	/**
	 * Loads a JavaScript command file into the JS Engine
	 * @param fileName The name of the file to load
	 */
	public void loadJs(String fileName) {
		File f = new File(fileName);
		if(!f.canRead()) throw new RuntimeException("Cannot read file [" + fileName + "]");
		Main.processSource(cx, fileName);
	}
	

}

/**
 * <p>Title: FileRequestHandler</p>
 * <p>Description: Built in HTTP Server File request handler.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 */
class FileRequestHandler implements HttpHandler {
	/** The file request root directory */
	protected final File fileBase;
	/**
	 * Creates a new FileRequestHandler
	 * @param baseDir The root directory of the file serve
	 */
	public FileRequestHandler(CharSequence baseDir) {
		fileBase = new File(baseDir.toString());
	}
	
	public static void log(Object msg) {
		System.out.println("[" + Thread.currentThread().toString() + "]" + msg);
	}	
	/**
	 * Handles requests for files.
	 * @param exchange The request
	 * @throws IOException
	 */
	public void handle(HttpExchange exchange) throws IOException {
		String fileName = exchange.getRequestURI().toString();
		// URI will be /js/foo.js
		
		OutputStream responseBody = exchange.getResponseBody();
		if(fileName!=null) {
			log("Requested File [" + fileName + "]");
			File rFile = new File(fileBase.getAbsolutePath() + fileName);
			if(!rFile.canRead() || !rFile.isFile()) {
				exchange.sendResponseHeaders(404, 0);			
				responseBody.close();				
			}
			byte[] bytes = FileHelper.getBytesFromUrl(rFile.toURI().toURL());
			Headers responseHeaders = exchange.getResponseHeaders();
			String mimeType = MimetypesFileTypeMap .getDefaultFileTypeMap().getContentType(rFile);
			responseHeaders.set("Content-Type", mimeType);
			exchange.sendResponseHeaders(200, bytes.length);	
			responseBody.write(bytes);
			responseBody.close();
			
		} else {
			exchange.sendResponseHeaders(404, 0);			
			responseBody.close();
		}
	}
}
