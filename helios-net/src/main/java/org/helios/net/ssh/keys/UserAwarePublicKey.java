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
package org.helios.net.ssh.keys;

import java.security.PublicKey;

/**
 * <p>Title: UserAwarePublicKey</p>
 * <p>Description: A decoded public key that provides a reference to a user name and domain</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.net.ssh.keys.UserAwarePublicKey</code></p>
 */
public class UserAwarePublicKey implements PublicKey {
	/**  */
	private static final long serialVersionUID = 584844634977064095L;
	/** The wrapped public key */
	protected final PublicKey publicKey;
	/** The associated user name */
	protected final String userName;
	/** The user's domain */
	protected final String userDomain;
	
	/**
	 * Creates a new UserAwarePublicKey
	 * @param publicKey The wrapped public key
	 * @param userName The associated user name
	 * @param userDomain The user's domain
	 */
	public UserAwarePublicKey(PublicKey publicKey, String userName, String userDomain) {
		this.publicKey = publicKey;
		this.userName = userName;
		this.userDomain = userDomain;
	}

	/**
	 * {@inheritDoc}
	 * @see java.security.Key#getAlgorithm()
	 */
	public String getAlgorithm() {
		return publicKey.getAlgorithm();
	}

	/**
	 * {@inheritDoc}
	 * @see java.security.Key#getFormat()
	 */
	public String getFormat() {
		return publicKey.getFormat();
	}

	/**
	 * {@inheritDoc}
	 * @see java.security.Key#getEncoded()
	 */
	public byte[] getEncoded() {
		return publicKey.getEncoded();
	}

	/**
	 * Returns the user name
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Returns the user domain
	 * @return the userDomain
	 */
	public String getUserDomain() {
		return userDomain;
	}
	
	/**
	 * Returns the user's fully qualified name
	 * @return the user's fully qualified name
	 */
	public String getFullName() {
		return new StringBuilder(userName).append("@").append(userDomain).toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((publicKey == null) ? 0 : publicKey.hashCode());
		if(userDomain!=null && userName!=null) {
			result = prime * result
					+ ((userDomain == null) ? 0 : userDomain.hashCode());
			result = prime * result
					+ ((userName == null) ? 0 : userName.hashCode());
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(!(obj instanceof PublicKey)) {
			return false;
		} else {
			if (getClass() != obj.getClass() || userName==null || userDomain==null) {
				return publicKey.equals(obj);
			}
			UserAwarePublicKey other = (UserAwarePublicKey) obj;
			if (publicKey == null) {
				if (other.publicKey != null)
					return false;
			} else if (!publicKey.equals(other.publicKey))
				return false;
			if (userDomain == null) {
				if (other.userDomain != null)
					return false;
			} else if (!userDomain.equals(other.userDomain))
				return false;
			if (userName == null) {
				if (other.userName != null)
					return false;
			} else if (!userName.equals(other.userName))
				return false;
			return true;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder b = new StringBuilder("PublicKey [");
		if(userDomain!=null && userName!=null) {
			b.append("\n\tUser:").append(userName).append("@").append(userDomain);
		}
		b.append("\n\tAlgorithm:").append(publicKey.getAlgorithm());
		b.append("\n\tFormat:").append(publicKey.getFormat());
		b.append("\n\tSize:").append(publicKey.getEncoded().length);
		b.append("\n]");
		return b.toString();
	}

	/**
	 * Returns the wrapped public key
	 * @return the publicKey
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}
}
