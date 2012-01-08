/**
 * 
 */
package org.helios;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;

/**
 * @author nwhitehead
 *
 */
@Ignore
public class TestServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		StringBuilder b = new StringBuilder();
//		b.append("ConentType:[").append(req.getContentType()).append("]\n");
//		b.append("ContextPathType:[").append(req.getContextPath()).append("]\n");
//		b.append("ContextPath:[").append(req.getContextPath()).append("]\n");
//		b.append("LocalAddress:[").append(req.getLocalAddr()).append("]\n");
//		b.append("LocalName:[").append(req.getLocalName()).append("]\n");
//		b.append("LocalPort:[").append(req.getLocalPort()).append("]\n");
//		b.append("Method:[").append(req.getMethod()).append("]\n");
		b.append("PathInfo:[").append(req.getPathInfo()).append("]\n");
//		b.append("PathTranslated:[").append(req.getPathTranslated()).append("]\n");
//		b.append("Protocol:[").append(req.getProtocol()).append("]\n");
		b.append("QueryString:[").append(req.getQueryString()).append("]\n");
//		b.append("RemoteAddress:[").append(req.getRemoteAddr()).append("]\n");
//		b.append("RemoteHost:[").append(req.getRemoteHost()).append("]\n");
//		b.append("RemotePort:[").append(req.getRemotePort()).append("]\n");
//		b.append("RemoteUser:[").append(req.getRemoteUser()).append("]\n");
		b.append("RequestURI:[").append(req.getRequestURI()).append("]\n");
		b.append("RequestURL:[").append(req.getRequestURL()).append("]\n");
//		b.append("Scheme:[").append(req.getScheme()).append("]\n");
//		b.append("ServerName:[").append(req.getServerName()).append("]\n");
//		b.append("ServerPort:[").append(req.getServerPort()).append("]\n");
		b.append("ServletPath:[").append(req.getServletPath()).append("]\n");
		
		byte[] content = b.toString().getBytes();
		resp.setContentLength(content.length);
		OutputStream os = resp.getOutputStream();
		os.write(content);
		os.flush();
		os.close();
	}
}
