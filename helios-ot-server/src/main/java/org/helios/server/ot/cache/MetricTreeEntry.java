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
package org.helios.server.ot.cache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.helios.ot.trace.MetricId;

/**
 * <p>Title: MetricTreeEntry</p>
 * <p>Description: Entry container class for MetricTreeCache to support the hierarchical structure of the metric tree.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.server.ot.cache.MetricTreeEntry</code></p>
 */
@XmlRootElement(name="treeEntry")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class MetricTreeEntry implements Serializable {
	/** The cache key */
	private String key;
	/** The parent cache key */
	private String parentKey;
	
	/** The full sub keys owned by this entry keyed by the relative key */
	private final Map<String, String> children = new ConcurrentHashMap<String, String>();
	/** The child full MetricId nodes of this entry */
	private final Map<String, MetricId> childNodes = new ConcurrentHashMap<String, MetricId>();
	/** Flag indicating if the child has its own children */
	private final Map<String, Boolean> childrenHaveChildren = new ConcurrentHashMap<String, Boolean>();
	/** Flag indicating if the child has nodes */
	private final Map<String, Boolean> childrenHaveNodes = new ConcurrentHashMap<String, Boolean>();
	
	/** Indicates if this is a leaf or a node */
	private boolean nodeEntry;	
	/** The metric id if this is a node */
	private MetricId node;
	
	
	/**
	 * Returns an array of DynaTreeNodes representing this metric entry
	 * @return an array of DynaTreeNodes
	 */
	public DynaTreeMetricNode[] getDynaTreeNodes() {
		Set<DynaTreeMetricNode> nodes = new HashSet<DynaTreeMetricNode>(children.size());
		for(Map.Entry<String, String> entry: children.entrySet()) {
			nodes.add(new DynaTreeMetricNode(
					entry.getKey(), 	// the relatve key
					entry.getValue(),  // the full sub key
					childrenHaveChildren.get(entry.getKey()),  // indicates if the child has children
					!childrenHaveNodes.get(entry.getKey()) // if the child is a node or has nodes, hideCheckBox should be false
			));
		}
		return nodes.toArray(new DynaTreeMetricNode[nodes.size()]);
	}
	
	/*
	 * Need to implement:
	 * titles:  relative keys
	 * keys:	fully qualified paths
	 * nodes:	booleans indicating if each entry is a node
	 * nodes:	booleans indicating if each entry contains a node
	 */
	/**
	 * <p>Title: DynaTreeMetricNode</p>
	 * <p>Description: Custom representation of a MetricTreeEntry intended for exposing nodes for a Dynatree</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.server.ot.cache.MetricTreeEntry.DynaTreeMetricNode</code></p>
	 */
	@XmlRootElement(name="dynatreeNode")
	@XmlAccessorType(XmlAccessType.FIELD)	
	public static class DynaTreeMetricNode implements Serializable {
		/** The node title */
		@XmlElement
		public final String title;
		/** The node key as the fully qualified node name, also referred to as the path */
		@XmlElement
		public final String key;
		/** Same as the node key */
		@XmlElement
		public final String tooltip;
		/** Constant. Nodes always load lazilly */
		@XmlElement
		public final boolean isLazy = true;
		/** Indicates if this is a folder (or a node) */
		@XmlElement
		public final boolean isFolder;
		/** Metric Id nodes and folders containing nodes have the check box enabled */
		@XmlElement
		public final boolean hideCheckbox;

		/**
		 * Creates a new DynaTreeMetricNode
		 * @param title The node title 
		 * @param key The node key as the fully qualified node name, also referred to as the path
		 * @param isFolder Indicates if this is a folder (or a node)
		 * @param hideCheckbox Metric Id nodes and folders containing nodes have the check box enabled
		 */
		public DynaTreeMetricNode(String title, String key,	boolean isFolder, boolean hideCheckbox) {
			super();
			this.title = title;
			this.key = key;
			this.tooltip = key;
			this.isFolder = isFolder;
			this.hideCheckbox = hideCheckbox;
		}

		/**
		 * Mandatory but unused parameterless DynaTreeMetricNode ctor
		 */
		public DynaTreeMetricNode() {			
			this.title = null;
			this.key = null;
			this.tooltip = null;
			this.isFolder = false;
			this.hideCheckbox = false;
		}

		/**
		 * Constructs a <code>String</code> with key attributes in name = value format.
		 * @return a <code>String</code> representation of this object.
		 */
		@Override
		public String toString() {
		    final String TAB = "\n\t";
		    StringBuilder retValue = new StringBuilder("DynaTreeMetricNode [")
		        .append(TAB).append("title = ").append(this.title)
		        .append(TAB).append("key = ").append(this.key)
		        .append(TAB).append("tooltip = ").append(this.tooltip)
		        .append(TAB).append("isLazy = ").append(this.isLazy)
		        .append(TAB).append("isFolder = ").append(this.isFolder)
		        .append(TAB).append("hideCheckbox = ").append(this.hideCheckbox)
		        .append("\n]");    
		    return retValue.toString();
		}
		
		
	}
	
	
	/**
	 * Retrieves the keyed entry from the cache
	 * @param keyPref The compound key
	 * @param parentKey The parent key
	 * @param node The MetricId
	 * @param metricIdCache The cache
	 * @return The keyed entry
	 */
	public static MetricTreeEntry getOrCreate(CharSequence keyPref, CharSequence parentKey, MetricId node, Cache metricIdCache) {
		MetricTreeEntry entry = null;
		String key = keyPref.toString();
		Element element = metricIdCache.get(key);		
		if(element==null) {
			synchronized(metricIdCache) {
				element = metricIdCache.get(key);
				if(element==null) {
					entry = new MetricTreeEntry(key, parentKey, node);
					element = new Element(key, entry);
					metricIdCache.put(element);
				} else {
					entry = (MetricTreeEntry)element.getObjectValue();
				}
			}			
		} else {
			entry = (MetricTreeEntry)element.getObjectValue();
		}
		if(entry==null) {
			throw new RuntimeException("Failed to create entry for [" + keyPref + "]", new Throwable());
		}
		return entry;
	}
	
	/**
	 * Creates a new MetricTreeEntry
	 * @param key The cache key
	 * @param parentKey The parent key
	 * @param node The metric id if this is a node. Null if it is a leaf.
	 */
	private MetricTreeEntry(final CharSequence key, final CharSequence parentKey, final MetricId node) {		
		this.key = key.toString();
		this.parentKey = parentKey.toString();
		this.node = node;
		nodeEntry = this.node!=null;
	}
	
	public MetricTreeEntry() {
		
	}
	
	/**
	 * Returns an array of this entry's relative sub keys
	 * @return an array of this entry's relative sub keys
	 */
	@XmlElement(name="subkeys")
	public String[] getRelativeSubKeys() {
		return children.keySet().toArray(new String[children.size()]);		
	}
	
	/**
	 * Returns an array of this entry's child node keys
	 * @return an array of this entry's child node keys
	 */
	@XmlElement(name="nodekeys")
	public String[] getNodeKeys() {
		return childNodes.keySet().toArray(new String[childNodes.size()]);
	}
	
	/**
	 * Returns an array of this entry's child nodes
	 * @return an array of this entry's child nodes
	 */
	@XmlElement(name="nodes")
	public MetricId[] getNodes() {
		return childNodes.values().toArray(new MetricId[childNodes.size()]);
	}	
	
	
	/**
	 * Returns an array of this entry's full sub keys
	 * @return an array of this entry's full sub keys
	 */
	@XmlElement(name="fullsubkeys")
	public String[] getFullSubKeys() {
		return children.values().toArray(new String[children.size()]);
	}	
	
	/**
	 * Returns this entry's MetricId node
	 * @return a MetricId if this is a node, null if it is a leaf
	 */
	@XmlElement(name="metricId")
	public MetricId getMetricId() {
		return node;
	}
	
	/**
	 * Adds a new child subKey
	 * @param subRelativeKey The relative subKey to add
	 * @param subFullKey The full subKey to add
	 * @return This entry
	 */	
	public MetricTreeEntry addSubKey(CharSequence subRelativeKey, CharSequence subFullKey) {
		if(subRelativeKey==null) throw new IllegalArgumentException("The passed subRelativeKey was null", new Throwable());
		if(subFullKey==null) throw new IllegalArgumentException("The passed subFullKey was null", new Throwable());
		children.put(subRelativeKey.toString(), subFullKey.toString());
		childrenHaveChildren.put(subRelativeKey.toString(), true);
		if(!childrenHaveNodes.containsKey(subRelativeKey.toString())) {
			childrenHaveNodes.put(subRelativeKey.toString(), false);
		}
		return this;
	}
	
	/**
	 * Marks a child (relative sub key) as having children (or not)
	 * @param subRelativeKey The child relative sub key
	 * @param hasChildren true if the child has children, false otherwise
	 * @return this entry
	 */
	public MetricTreeEntry addChildHasChildren(CharSequence subRelativeKey, boolean hasChildren) {
		childrenHaveChildren.put(subRelativeKey.toString(), hasChildren);
		return this;
	}
	
	/**
	 * Marks a child (relative sub key) as having metric id nodes (or not)
	 * @param subRelativeKey The child relative sub key
	 * @param hasChildren true if the child has metric id nodes, false otherwise
	 * @return this entry
	 */
	public MetricTreeEntry addChildHasNodes(CharSequence subRelativeKey, boolean hasChildren) {
		childrenHaveNodes.put(subRelativeKey.toString(), hasChildren);
		return this;
	}
	
	
	
	/**
	 * Adds a new child MetricId node
	 * @param subRelativeKey The relative subKey to add
	 * @param metricId The metric Id to add
	 * @return This entry
	 */	
	public MetricTreeEntry addNode(CharSequence subRelativeKey, MetricId metricId) {
		if(subRelativeKey==null) throw new IllegalArgumentException("The passed subRelativeKey was null", new Throwable());
		if(metricId==null) throw new IllegalArgumentException("The passed metricId was null", new Throwable());
		childNodes.put(subRelativeKey.toString(), metricId);
		childrenHaveNodes.put(subRelativeKey.toString(), true);
		childrenHaveChildren.put(subRelativeKey.toString(), false);
		return this;
	}
	
	
	/**
	 * Sets the absolute node for this entry
	 * @param metricId The metricId node
	 */
	public void setMetricId(MetricId metricId) {
		if(metricId==null) throw new IllegalArgumentException("The passed metricId was null", new Throwable());
		node=metricId;		
		nodeEntry=true;
	}
	

	/**
	 * Returns the entry key
	 * @return the entry key
	 */
	@XmlElement(name="key")
	public String getKey() {
		return key;
	}

	/**
	 * Indicates if this is a leaf or a node
	 * @return true if this is a node, false if it is a leaf
	 */
	@XmlElement(name="isnode")
	public boolean isNode() {
		return nodeEntry;
	}
	
	/**
	 * Indicates if this leaf node contains nodes
	 * @return true if this leaf node contains nodes, false otherwise
	 */
	@XmlElement(name="hasnodes")
	public boolean isNodeParent() {
		return !childNodes.isEmpty();
	}
	
	/**
	 * Indicates if this leaf node has a any children
	 * @return true if this leaf node has children, false otherwise
	 */
	@XmlElement(name="parent")
	public boolean isParent() {
		return !childNodes.isEmpty() || !children.isEmpty();
	}

	/**
	 * Constructs a <code>String</code> with key attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("MetricTreeEntry [")
	        .append(TAB).append("key = ").append(this.key)
	        .append(TAB).append("childrenKeys = [");
	    	for(Map.Entry<String, String> key: children.entrySet()) {
	    		String id = key.getKey();
	    		retValue.append("\n\t\t").append(id).append(":").append(key.getValue()).append("  Folder:").append(childrenHaveChildren.get(id)).append("  Has/Is Nodes:").append(childrenHaveNodes.get(id));
	    	}
	    	retValue.append(TAB).append("]");
	    	retValue.append(TAB).append("hasNodes = ").append(this.isNodeParent());
	    	if(isNodeParent()) {
		        retValue.append(TAB).append("childrenNodes = [");
		    	for(Map.Entry<String, MetricId> key: childNodes.entrySet()) {
		    		retValue.append("\n\t\t").append(key.getKey()).append(":").append(key.getValue());
		    	}
		    	retValue.append(TAB).append("]");
	    	}	    	
	        retValue.append(TAB).append("nodeEntry = ").append(this.nodeEntry)
	        .append(TAB).append("isNode = ").append(this.node)	        
	        .append("\n]");    
	    return retValue.toString();
	}

	/**
	 * Returns the parent key
	 * @return the parentKey
	 */
	public String getParentKey() {
		return parentKey;
	}
	
	
	
}
