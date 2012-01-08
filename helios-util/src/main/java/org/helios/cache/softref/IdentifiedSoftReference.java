package org.helios.cache.softref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>Title: IdentifiedSoftReference</p>
 * <p>Description: Extension of <code>java.lang.ref.SoftReference</code> that maintains the identity of the contained object to clear references after GC enqueueing.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.cache.softref.IdentifiedSoftReference</code></p>
 */
public class IdentifiedSoftReference<T> extends SoftReference<T> {
	/** The referent identity  */
	protected final Object referentIdentity;
	/** an array of enqueue event listeners */
	protected final Set<ReferenceEnqueueListener> listeners = new CopyOnWriteArraySet<ReferenceEnqueueListener>();
	/** the class name of the referent */
	protected final String className;
	

	/**
	 * Creates a new IdentifiedSoftReference. 
	 * @param referent The object that the reference refers to.
	 * @param q The reference queue where this reference will be writen to when the referent is garbage collected.
	 * @param referentIdentity The identitiy of the referent which will survive the referent garbage collection and be accessible through the queued reference. 
	 */
	protected IdentifiedSoftReference(T referent, ReferenceQueue<? super T> q, Object referentIdentity, ReferenceEnqueueListener...listeners) {
		super(referent, q);
		className = referent.getClass().getName();
		if(listeners!=null) {
			Collections.addAll(this.listeners, listeners);
		}
		if(referentIdentity!=null && referent!=null) {
			if(System.identityHashCode(referent)==System.identityHashCode(referentIdentity)) throw new RuntimeException("The referent identitiy cannot be the same object as the referent.");
		}
		this.referentIdentity = referentIdentity;
	}
	
	/**
	 * Returns the className of the referent.
	 * @return the className of the referent.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns an array of the enqueue event listeners.
	 * @return an array of listeners.
	 */
	public ReferenceEnqueueListener[] getListeners() {
		return listeners.toArray(new ReferenceEnqueueListener[listeners.size()]); 
	}
	
	/**
	 * Removes the specified enqueue listener from the reference.
	 * @param listener The listener to remove
	 */
	public void removeListener(ReferenceEnqueueListener listener) {
		if(listener != null && listeners.contains(listener)) {
			listeners.remove(listener);
		}
	}
	
	/**
	 * Registers the specified enqueue listener with the reference.
	 * @param listener The listener to register
	 */
	public void registerListener(ReferenceEnqueueListener listener) {
		if(listener != null && listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	

	/**
	 * Returns the identitiy of the referent.
	 * @return the referentIdentity
	 */
	public Object getReferentIdentity() {
		return referentIdentity;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("IdentifiedSoftReference [");    
	    retValue.append(TAB).append("referent Type=").append(this.className);
	    retValue.append(TAB).append("referentIdentity=").append(this.referentIdentity);    
	    retValue.append(TAB).append("queued=").append(this.isEnqueued());
	    retValue.append(TAB).append("listener count=").append(this.listeners.size());
	    if(!this.isEnqueued()) {
	    	retValue.append(TAB).append("referent=").append(this.get());
	    }
	    retValue.append("\n]");
	    return retValue.toString();
	}

}
