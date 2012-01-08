package org.helios.cache.softref;

/**
 * <p>Title: ReferenceEnqueueListener</p>
 * <p>Description: Defines a class that is notified that an instance of <code>IdentifiedSoftReference</code> has been enqueued and executes some useful action with the garbage collected referent's identity.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.cache.softref.ReferenceEnqueueListener</code></p>
 */
public interface ReferenceEnqueueListener {
	/**
	 * Callback that occurs when a IdentifiedSoftReference is enqueued after its referent is garbage collected.
	 * @param enqueuedRef
	 */
	public void onEnqueuedReference(IdentifiedSoftReference<?> enqueuedRef);
}
