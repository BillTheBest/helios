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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.helios.helpers.StringHelper;


/**
 * <p>Title: ObjectHierarchyTree</p>
 * <p>Description: A hierarchical object reference tree that supports wildcard searches and matching on the hierarchy.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ObjectHierarchyTree<K> implements Serializable  {
	/**  */
	private static final long serialVersionUID = 2614593833038941064L;

	protected Map<String, HierarchyNode<K>> hierarchy = new ConcurrentHashMap<String, HierarchyNode<K>>();
	
	/**	The name space delimeter */
	protected String delimeter = null;
	/**	The name space escaped delimeter */
	protected String escaped_delimeter = null;
	
	/** The name space segment wild card */
	protected String wildCard = null;
	/** The default value for sparse lookups */
	protected K defaultValue = null;
	/** The sparse value for incidental nodes */
	protected K sparseValue = null;
	/** Average Lookup Time */
	protected AtomicLong averageLookupTime = new AtomicLong(0L);
	/** one reading taken */
	protected AtomicBoolean oneReading = new AtomicBoolean(false);
	/** Generic Empty Array */
	protected final K[] emptyArr = (K[] ) new Object[]{};
	
	
	
	
	/**
	 * Creates a new ObjectHierarchyTree
	 * @param delimeter The name space delimeter
	 * @param wildCard The name space segment wild card
	 * @param defaultValue The default value for sparse lookups
	 * @param sparseValue The value used for incidental nodes placed in the tree to complete the navigation to a progeny node.
	 */
	public ObjectHierarchyTree(String delimeter, String wildCard, K defaultValue, K sparseValue) {
		this.wildCard = wildCard;
		this.escaped_delimeter = Pattern.quote(delimeter);
		this.delimeter = delimeter;
		this.defaultValue = defaultValue;
		this.sparseValue = sparseValue;
	}
	
	/**
	 * Returns the number of nodes in the whole tree.
	 * @return The number of nodes in the tree.
	 */
	public int getTreeSize() {
		int tSize = 0;
		for(HierarchyNode<K> node: hierarchy.values()) {
			tSize++;
			tSize += getHierarchySize(node);
		}
		
		return tSize;
	}
	
	/**
	 * Recursive method to find the total number of child nodes in a given node. 
	 * @param rootNode The HierarchyNode to size.
	 * @return The total number of children nodes in the passed node.
	 */
	public int getHierarchySize(HierarchyNode<K> rootNode) {
		if(rootNode==null) return 0;
		int totalSize = rootNode.getChildren().size();
		for(HierarchyNode<K> node: rootNode.getChildren().values()) {
			totalSize += getHierarchySize(node);
		}
		return totalSize;
	}
	
	/**
	 * Returns a set of all nodes in the tree.
	 * @param dropSparseNodes Filters out sparse nodes.
	 * @return A set of HierarchyNodes.
	 */
	public Set<HierarchyNode<K>>  getAllNodes(boolean dropSparseNodes) {
		Set<HierarchyNode<K>> set = new LinkedHashSet<HierarchyNode<K>>(getTreeSize());
		recurse(hierarchy.values(), set, dropSparseNodes);
		return set;
	}
	
	/**
	 * Recurses the hierarchy and adds each located node to the set.
	 * @param values
	 * @param set
	 * @param dropSparseNodes Filters out sparse nodes.
	 * @return
	 */
	protected Set<HierarchyNode<K>> recurse(Collection<HierarchyNode<K>> values, Set<HierarchyNode<K>> set, boolean dropSparseNodes) {
		set.addAll(values);
		for(HierarchyNode<K> node: values) {
			if(node.children!=null && node.member !=null && (!dropSparseNodes || !node.member.equals(sparseValue))) {
				recurse(node.children.values(), set, dropSparseNodes);
			}
		}
		return set;
	}
	
	
	
	/**
	 * Empties the hierarchy.
	 */
	public void clear() {
		hierarchy.clear();
	}
	

	/**
	 * Removes all the children from the passed node.
	 * @param rootNode The node to clear the children from.
	 */
	public void clear(HierarchyNode<K> rootNode) {
		rootNode.getChildren().clear();
	}
	
	
	/**
	 * Locates the name space node and sets the value.
	 * If the node does not exist, it is created.
	 * @param name The name space.
	 * @param value The value to set the node to.
	 * @return the created or updated node.
	 */
	public HierarchyNode<K> setMember(String name, K value) {
		String[] segments  = null;
		if(!name.contains(delimeter)) {
			segments = new String[]{name};
		} else {
			segments = name.split(escaped_delimeter);
		}		
		HierarchyNode<K> node = null;
		HierarchyNode<K> par = null;
		Map<String, HierarchyNode<K>> currentMap = hierarchy;		 
		for(String s: segments) {			
			node = currentMap.get(s);
			if(node==null) {
				node = new HierarchyNode<K>(sparseValue, par, s);
				currentMap.put(s, node);				
			}	
			par = node;
			currentMap = node.getChildren();
		}
		node.setMember(value);
		return node;
	}
	

	
	/**
	 * Gets the value from the tree navigated to by the namespace.
	 * If the node does not exist, the default value is returned.
	 * @param name
	 * @return The located node's value or null.
	 */
	public K getValue(String name) {
		long start = System.currentTimeMillis();
		String[] segments = name.split(delimeter);
		HierarchyNode<K> node = null;
		HierarchyNode<K> node2 = null;
		Map<String, HierarchyNode<K>> currentMap = hierarchy;
		
		for(String s: segments) {
			node = lookup(s, currentMap);
			if(node==null) {
				if(node2!=null) {
					return node2.getMember();
				} else {
					return defaultValue;
				}
			}
			else {
				currentMap = node.getChildren();
				node2 = node;
			}
		}
		long elapsed = System.currentTimeMillis()-start;
		if(!oneReading.get()) {
			oneReading.set(true);
			averageLookupTime.set(elapsed);
		} else {
			long currentAvg = averageLookupTime.get();
			if((currentAvg + elapsed)==0L) {
				averageLookupTime.set(0L);
			} else {
				averageLookupTime.set((currentAvg + elapsed)/2);
			}									
		}
		return node.getMember();
	}
	
	/**
	 * Returns the node navigated to by the name space passed.
	 * @param name The name space to retrieve the node from.
	 * @return The node found or null if it does not exist.
	 */
	public HierarchyNode<K> getNode(String name) {
		
		String[] segments = name.split(escaped_delimeter);
		HierarchyNode<K> node = null;		
		Map<String, HierarchyNode<K>> currentMap = hierarchy;
		
		for(String s: segments) {
			node = lookup(s, currentMap);
			if(node==null) {
				break;
			}
			else {
				currentMap = node.getChildren();				
			}
		}
		return node;
	}
	
	
	/**
	 * Locates the node matching the passed compound name
	 * and walks up the hierarchy until a match is found.
	 * @param name The full compound name.
	 * @return The first matching node or null if one is not found.
	 */
	public HierarchyNode<K> getNodeMatch(String name) {
		HierarchyNode<K> node = getNode(name);		
		if(node!=null) return node;
		String[] segments = name.split(escaped_delimeter);
		if(segments.length > 1) {
			String adjustedName = null;
			while(node==null) {	
				segments = StringHelper.truncate(segments,1);
				adjustedName = StringHelper.flattenArray(delimeter, segments);
				node = getNode(adjustedName);
				if(segments.length == 1) break;
			}
		}
		
		return node;
	}
	
	/**
	 * Searches the entire tree for nodes matching the passed pattern which can contain wildcards.
	 * @param pattern A search pattern
	 * @param discardSparseNodes Instructs the search to discard sparse nodes.
	 * @return A set of matching nodes.
	 */
	public Set<HierarchyNode<K>> search(String pattern, boolean discardSparseNodes) {
		Set<HierarchyNode<K>> set = new LinkedHashSet<HierarchyNode<K>>();
		if(pattern==null || pattern.length() < 1) return set;
		//String[] segments = pattern.split(escaped_delimeter);
		String[] segments = null;
		if(delimeter.equals("\\.")) {
			segments = pattern.split(delimeter);
		} else {
			segments = pattern.split(escaped_delimeter);
		}
		
		StringBuilder b = new StringBuilder();
		HierarchyNode<K> node = null;
		if(segments==null || segments.length<1) return set;
		if(segments.length==1 && segments[0].equals(wildCard)) return getAllNodes(discardSparseNodes);
		
		String s = segments[0];
		Collection<HierarchyNode<K>> level = search(hierarchy, s, discardSparseNodes);
		if(level.isEmpty()) return Collections.emptySet();
		for(int i = 1; i <= segments.length; i++) {
			s = segments[i];
			level = search(hierarchy, s, discardSparseNodes);
			if(level.isEmpty()) return Collections.emptySet();			
		}
		set.addAll(level);
		return set;
	}
	
	public Collection<HierarchyNode<K>> search(Map<String, HierarchyNode<K>> level, String pattern, boolean discard) {
		if(wildCard.equals(pattern)) {
			return stripSparses(discard, level.values());
		}
		HierarchyNode<K> node = level.get(pattern);
		if(node==null || (discard && isNodeSparse(node))) return Collections.emptyList();
		return new ArrayList<HierarchyNode<K>>(Arrays.asList(node));
	}
	
	public boolean isNodeSparse(HierarchyNode<K> node) {
		K k = node.getMember();
		if(sparseValue==null && k==null) return true;
		return sparseValue.equals(k);
	}
	
	protected Collection<HierarchyNode<K>> stripSparses(boolean discardSparseNodes, final Collection<HierarchyNode<K>> nodes) {
		if(discardSparseNodes) { 
			for(Iterator<HierarchyNode<K>> iter = nodes.iterator(); iter.hasNext();) {
				HierarchyNode<K> node = iter.next();
				if(isNodeSparse(node)) iter.remove();
			}
		}
		return nodes;
	}
	
	public Collection<HierarchyNode<K>> getChildren(Map<String, HierarchyNode<K>> map, String key, boolean discardSparseNodes) {
		Collection<HierarchyNode<K>> results = new ArrayList<HierarchyNode<K>>();
		if(key.equals(wildCard)) {
			return stripSparses(discardSparseNodes, map.values());
		}
		HierarchyNode<K> node = map.get(key);
		if(node==null || (discardSparseNodes && isNodeSparse(node))) return Collections.emptyList();
		results.add(node);
		return results;
	}
	
	public Collection<HierarchyNode<K>> getChildren(HierarchyNode<K> parent, String key, boolean discardSparseNodes) {
		Collection<HierarchyNode<K>> results = new ArrayList<HierarchyNode<K>>();
		if(key.equals(wildCard)) {
			results = parent.getChildren().values();
			if(results.isEmpty()) return results;
			return stripSparses(discardSparseNodes, results);
		}
		HierarchyNode<K> node = parent.getChildren().get(key);
		if(node==null || (discardSparseNodes && isNodeSparse(node))) return Collections.emptyList();
		results.add(node);
		return results;
	}

	
	/**
	 * Returns an array of the values starting from the node identified by the passed name and climbing the tree.
	 * @param name The name of the array to start from.
	 * @return An array of values.
	 */
	public K[] getValueArray(String name) {
		return getValueArray(name, 0,0);
	}
	
	/**
	 * Returns an array of the values starting from the node identified by the passed name and climbing the tree.
	 * The first <code>trim</code> values in the array will be removed and the last <code>truncate</code> values
	 * will be removed from the end of the array.
	 * @param name The name of the node to start from.
	 * @param trim The number of items to remove from the start of the array.
	 * @param truncate The number of items to remove from the end of the array.
	 * @return An array of values.
	 */
	public K[] getValueArray(String name, int trim, int truncate) {
		LinkedList<K> q = new LinkedList<K>();
		HierarchyNode<K> node = getNode(name);
		while(true) {			
			if(node!=null) {
				q.addFirst(node.getMember());
			} else {
				break;
			}
			node = node.getParent();
		}
		
		if(q.size()<1) {
			return null;
		}
		K[] arr = q.toArray((K[])Array.newInstance(q.peek().getClass(), q.size()));
		arr = StringHelper.truncate(arr,truncate);
		arr = StringHelper.trim(arr,trim);
		return arr;
	}
	
	/**
	 * Looks up a child node in a parent node.
	 * If the exact match is not found, a wildcard is looked up.
	 * If neither the exact match or wildcard is fund, returns null.
	 * @param key The name space segment.
	 * @param map The map of nodes to search in.
	 * @return A located node or null.
	 */
	protected HierarchyNode<K> lookup(String key, Map<String, HierarchyNode<K>> map) {
		HierarchyNode<K> node = map.get(key);
		if(node != null) return node;
		node = map.get(wildCard);
		return node;
	}
	
	/**
	 * Generates a string representation of the sparse object tree.
	 * @return A map of the object tree.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder("Hierarchy\n");
		int depth = 0;
		Set<Entry<String, HierarchyNode<K>>>  children = hierarchy.entrySet();
		for(Entry<String, HierarchyNode<K>> entry: children) {
			printNode(entry.getValue(), entry.getKey(), buff, depth);
		}		
		return buff.toString();
	}
	
	/**
	 * Returns a string representation of one node.
	 * @param nodeName The name space of the node to render.
	 * @return A map of the object tree from the located node down.
	 */
	public String printDetails(String nodeName) {
		StringBuilder buff = new StringBuilder("Node\n");
		HierarchyNode<K> node = getNode(nodeName);
		if(node==null) {
			buff.append("Empty");
		} else {
			int depth = 0;
			Set<Entry<String, HierarchyNode<K>>>  children = node.getChildren().entrySet();
			for(Entry<String, HierarchyNode<K>> entry: children) {
				printNode(entry.getValue(), entry.getKey(), buff, depth);
			}					
		}
		return buff.toString();
		
	}
	
	/**
	 * Returns the average lookup time for the hierarchy tree in milliseconds.
	 * @return The average lookup time.
	 */
	public long getAverageLookupTime() {
		return averageLookupTime.get();
	}
	
	/**
	 * Generates a string representation of a node.
	 * @param node The node to render.
	 * @param nodeKey The key of the node.
	 * @param buff The buffer to append to append the rendered text into.
	 * @param depth The current depth of the node.
	 */
	protected void printNode(HierarchyNode<K> node, String nodeKey, StringBuilder buff, int depth) {
		buff.append(indent(depth));
		buff.append(nodeKey);
		buff.append(":");
		buff.append(node.getMember());
		buff.append("\n");
		Set<Entry<String, HierarchyNode<K>>>  children = node.getChildren().entrySet();
		depth++;
		for(Entry<String, HierarchyNode<K>> entry: children) {
			String name = entry.getKey();
			HierarchyNode<K> entryNode = entry.getValue();
			printNode(entryNode, name, buff, depth);
		}
		depth--;
	}
	
	/**
	 * Generates an indentation for formating the node rendering.
	 * @param i The depth of the indentation.
	 * @return A string of the correct width and content to generate the indentation.
	 */
	protected String indent(int i) {
		StringBuilder buff = new StringBuilder();
		for(int x = 0; x < i; x++) {
			buff.append("  ");
		}
		return buff.toString();
	}
	

	/**
	 * A static test of the tree.
	 * @param args
	 */
	public static void main(String[] args) {
		ObjectHierarchyTree<Boolean> hier = new ObjectHierarchyTree<Boolean>("\\.", "*", Boolean.TRUE, Boolean.FALSE);
		hier.setMember("A", Boolean.TRUE);
		hier.setMember("A.B", Boolean.FALSE);
		hier.setMember("A.B.C", Boolean.TRUE);
		hier.setMember("X.*.Z", Boolean.FALSE);
		
		hier.setMember("L.*.M", Boolean.FALSE);
		hier.setMember("L.O.M", Boolean.TRUE);
		
		log("TreeSize:" + hier.getTreeSize());
		
		log(hier.toString());
		log("============");
		log("A:" + hier.getValue("A"));
		log("A.B:" + hier.getValue("A.B"));
		log("A.B:" + hier.getValue("A.B"));
		log("A.B.C:" + hier.getValue("A.B.C"));
		log("A.B.D:" + hier.getValue("A.B.D"));
		log("A.B.D.A.B.D.A.B.D.A.B.D.A.B.D:" + hier.getValue("A.B.D.A.B.D.A.B.D.A.B.D.A.B.D"));
		log("A.B.C.D:" + hier.getValue("A.B.C.D"));
		log("D:" + hier.getValue("D"));
		log("X:" + hier.getValue("X"));
		log("X.Y:" + hier.getValue("X.Y"));
		log("X.Y.Z:" + hier.getValue("X.Y.Z"));
		log("L.A.M:" + hier.getValue("L.A.M"));
		log("L.A.M.A:" + hier.getValue("L.A.M.A"));
		log("L.O.M:" + hier.getValue("L.O.M"));
		log("L.O.M.O:" + hier.getValue("L.O.M.O"));
		
		log("==========================");
		
		log(hier.search("*.B.*", true));
		
	}
		
	
	static boolean p = true;
	
	public static void log(Object message) {
		if(p)System.out.println(message);
	}

}
