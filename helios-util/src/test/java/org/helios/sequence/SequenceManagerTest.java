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
package org.helios.sequence;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * <p>Title: SequenceManagerTest</p>
 * <p>Description: Basic default file store sequence manager test cases</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.sequence.SequenceManagerTest</code></p>
 */

public class SequenceManagerTest extends TestCase {
	protected SequenceManager sm = null;
	protected final static Logger log = Logger.getLogger(SequenceManagerTest.class);
	static {
		BasicConfigurator.configure();
	}
	/**
	 * @throws java.lang.Exception
	 */
	protected void setUp() throws Exception {
		super.setUp();
		sm = new SequenceManager();		
		sm.purge();
		sm = new SequenceManager();
	}

	/**
	 * @throws java.lang.Exception
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		sm.purge();
		sm = null;
	}
	
	/**
	 * Basic test to validate an empty sequence cache on start
	 */
	public void testCreateNewEmptyStore() {
		int size = sm.getSequenceNames().length;
		Assert.assertEquals("A new sequence manager has zero entries", 0, size);
	}
	
	/**
	 * This test validates that an empty file can be reloaded 
	 */
	public void testSequenceReload() {
		sm = new SequenceManager();
	}
	
	/**
	 * This test validates that a new sequence is saved and reloaded correctly
	 * @TODO: Still fails sporadically
	 */
	public void xxxNewSequenceLoad() {
		sm.purge();
		String name = "TestNewSequence";
		int batchSize = 10;
		int lowThreshold = 2;
		long initialSeed = 0;
		Sequence sq = sm.addSequence(name, batchSize, lowThreshold, initialSeed, sm);
		Assert.assertNotNull("Sequence is not null", sq);
		Assert.assertEquals("Name equals [" + name + "]", name, sq.getName());
		Assert.assertEquals("Batch size is [" + batchSize + "]", batchSize, sq.getBatchSize());
		Assert.assertEquals("Low Threshold is [" + lowThreshold + "]", lowThreshold, sq.getLowThreshold());
		Assert.assertEquals("Current is [" + initialSeed  + "]", initialSeed, sq.current());
		Assert.assertEquals("HWM equals [" + (initialSeed + batchSize) + "]", (initialSeed + batchSize), sq.highwater());
		// =================================
		// triggers a load, current will load [batchSize] higher
		// =================================
		sm = new SequenceManager();
		sq = sm.getSequence(name);
		// =================================
		Assert.assertNotNull("Sequence is not null", sq);
		Assert.assertEquals("Name equals [" + name + "]", name, sq.getName());
		Assert.assertEquals("Batch size is [" + batchSize + "]", batchSize, sq.getBatchSize());
		Assert.assertEquals("Low Threshold is [" + lowThreshold + "]", lowThreshold, sq.getLowThreshold());
		Assert.assertEquals("Current is [" + (initialSeed + batchSize)  + "]", (initialSeed + batchSize), sq.current());
		Assert.assertEquals("HWM equals [" + (initialSeed + (batchSize*2)) + "]", (initialSeed + (batchSize*2)), sq.highwater());
		long newSeed = sq.current();
		// =================================
		// triggers a reload
		// =================================
		
		for(int i = 0; i < (batchSize+1); i++) {
			long value = sq.next();
			Assert.assertEquals("Next value is [" + (batchSize+initialSeed+i+1) + "]", (batchSize+initialSeed+i+1), value);
			log.info("NextValue:" + value);
		}
		//Assert.assertEquals("Reload Count is [1]", 1, sq.getReloadCounter());
		// =================================
		// triggers a reload, so the new loaded seed will be (newSeed + batchSize)
		// =================================
		sm = new SequenceManager();
		sq = sm.getSequence(name);
		// =================================
		Assert.assertNotNull("Sequence is not null", sq);
		Assert.assertEquals("Name equals [" + name + "]", name, sq.getName());
		Assert.assertEquals("Batch size is [" + batchSize + "]", batchSize, sq.getBatchSize());
		Assert.assertEquals("Low Threshold is [" + lowThreshold + "]", lowThreshold, sq.getLowThreshold());
		Assert.assertEquals("Current is [" + (newSeed + (batchSize*2)) + "]", (newSeed + (batchSize*2)), sq.current());
		Assert.assertEquals("HWM equals [" + (initialSeed + (batchSize*4)) + "]", (initialSeed + (batchSize*4)), sq.highwater());
		
	}
	
	/**
	 * This test validates that a sequence reload saves the next batch seed
	 */
	public void testSequenceReloadSeedSave() {
		sm.purge();
		String name = "SequenceReloadSeedSave";
		int batchSize = 10;
		int lowThreshold = 2;
		long initialSeed = 0;
		int loops = 15;
		Sequence sq = sm.addSequence(name, batchSize, lowThreshold, initialSeed, sm);
		
		Assert.assertNotNull("Sequence is not null", sq);
		for(int i = 0; i < loops; i++) {
			long value = sq.next();
			Assert.assertEquals("Value is [" + (i+1) + "]", (i+1), value);
		}
		sm = new SequenceManager();
		sq = sm.getSequence(name);
		Assert.assertNotNull("Sequence is not null", sq);
		Assert.assertEquals("Current is [" + 20 + "]", 20, sq.current());
	}

}
