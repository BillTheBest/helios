/**
 * 
 */
package org.helios.ssh.tunnel;

/**
 * <p>Title: PortTunnel</p>
 * <p>Description: </p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.ssh.tunnel.PortTunnel</code></p>
 */

public interface PortTunnel {

	public String getLocalPort();

	public String getLocalHostName();

}
