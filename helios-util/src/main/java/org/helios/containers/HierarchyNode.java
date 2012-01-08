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
package org.helios.containers;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



/**
 * <p>Title: HierarchyNode</p>
 * <p>Description: A hierarchy management container class.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class HierarchyNode<K> implements Serializable {
	/**  */
	private static final long serialVersionUID = -7192593578480702063L;
	/**	 The value of the node */
	protected K member = null;
	/**	 The node key segment */
	protected String key  = null;
	
	/** The parent of this node */
	protected HierarchyNode<K> parent = null;
	/** A map of the node's children */
	protected Map<String, HierarchyNode<K>> children = new ConcurrentHashMap<String, HierarchyNode<K>>();
	
	/**
	 * Creates a new node.
	 * @param member The member value of the node.
	 */
	public HierarchyNode(K member) {
		this.member = member;
	}
	
	/**
	 * Creates a new node.
	 * @param member The member value of the node.
	 * @param parent The parent node
	 * @param key The key segment
	 */
	public HierarchyNode(K member, HierarchyNode<K> parent, String key) {
		this.member = member;
		this.parent = parent;
		this.key = key;
	}
	
	
	
	
	/**
	 * Returns the value of the node.
	 * @return The value of the node.
	 */
	public K getMember() {
		return member;
	}
	
	/**
	 * Sets the value of the node.
	 * @param value The value of the node.
	 */
	public void setMember(K value) {
		member = value;
	}
	
	/**
	 * Returns the children of the node.
	 * @return A map of the children.
	 */
	public Map<String, HierarchyNode<K>> getChildren() {
		return children;
	}
	
	/**
	 * Sets the children of the node.
	 * @param children A map of the children of the node.
	 */
	public void setChildren(Map<String, HierarchyNode<K>> children) {
		this.children = children;
		for(HierarchyNode<K> child: this.children.values()) {
			child.parent = this;
		}
	}
	
	/**
	 * Adds a child node to this node.
	 * @param name The key segment 
	 * @param value The value to be associated with the child node
	 * @return the created child node.
	 */
	public HierarchyNode<K> addChild(String name, K value) {
		HierarchyNode<K> child = new HierarchyNode<K>(value);
		child.parent = this;
		child.key = name;
		children.put(name, child);
		return child;
	}
	
	/**
	 * Returns this node's parent node, or null if it has no parent.
	 * @return the parent node.
	 */
	public HierarchyNode<K> getParent() {
		return parent;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("HierarchyNode [");    
	    retValue.append(TAB).append("value=").append(this.member);	    
	    retValue.append(TAB).append("key=").append(this.key);
	    retValue.append(TAB).append("parent=").append(this.parent==null ? "None" : this.parent.member);    
	    retValue.append(TAB).append("direct children count=").append(this.children==null ? 0 : this.children.size());    
	    retValue.append("\n]");
	    return retValue.toString();
	}

	/**
	 * The key segment for this node.
	 * @return the key
	 */
	public String getKey() {
		return key;
	}




	
	
}
