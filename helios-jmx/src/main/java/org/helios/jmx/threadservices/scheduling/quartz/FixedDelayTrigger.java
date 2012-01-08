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
package org.helios.jmx.threadservices.scheduling.quartz;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;

/**
 * <p>Title: FixedDelayTrigger</p>
 * <p>Description: A scheduling trigger for fixed delay scheduling where a task must be fired on the specified frequency between the end of one task and the beginning of the next.
 * This is fairly much asking for trouble, but the assumption is that these tasks will complete before the next fire.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class FixedDelayTrigger extends SimpleTrigger {
	public static final Date WAY_IN_FUTURE = new Date(Long.MAX_VALUE); 
	protected Scheduler originalScheduler = null;
	/**
	 * 
	 */
	public FixedDelayTrigger() {
		//getParent();
	}


	/**
	 * @param name
	 * @param group
	 */
	public FixedDelayTrigger(String name, String group) {
		super(name, group);
	}

	/**
	 * @param name
	 * @param group
	 * @param startTime
	 */
	public FixedDelayTrigger(String name, String group, Date startTime) {
		super(name, group, startTime);
	}

	/**
	 * @param name
	 * @param group
	 * @param repeatCount
	 * @param repeatInterval
	 */
	public FixedDelayTrigger(String name, String group, int repeatCount,
			long repeatInterval) {
		super(name, group, repeatCount, repeatInterval);
	}

	/**
	 * @param name
	 * @param group
	 * @param startTime
	 * @param endTime
	 * @param repeatCount
	 * @param repeatInterval
	 */
	public FixedDelayTrigger(String name, String group, Date startTime,
			Date endTime, int repeatCount, long repeatInterval) {
		super(name, group, startTime, endTime, repeatCount, repeatInterval);
	}

	/**
	 * @param name
	 * @param group
	 * @param jobName
	 * @param jobGroup
	 * @param startTime
	 * @param endTime
	 * @param repeatCount
	 * @param repeatInterval
	 */
	public FixedDelayTrigger(String name, String group, String jobName,
			String jobGroup, Date startTime, Date endTime, int repeatCount,
			long repeatInterval) {
		super(name, group, jobName, jobGroup, startTime, endTime, repeatCount,
				repeatInterval);
	}

    public void triggered(org.quartz.Calendar calendar) {
        super.triggered(calendar);
        setNextFireTime(WAY_IN_FUTURE);
    }

    public int executionComplete(JobExecutionContext context, JobExecutionException result) {
        int superResult = super.executionComplete(context, result);

        Date nextFireTime = new Date(System.currentTimeMillis() + getRepeatInterval());
        setNextFireTime(nextFireTime);        
        this.setStartTime( nextFireTime );
        try {
        	originalScheduler.rescheduleJob(this.getName(), this.getGroup(), this);
        } catch ( SchedulerException se ) {
        }

        return superResult;
    } 	
	
	public static void main(String[] args) {
		try {
			log("Test FixedDelayTrigger");
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
			log("Scheduler Started");
			JobDetail jobDetail =
                new JobDetail("FixedRateTriggerJob",
                     Scheduler.DEFAULT_GROUP,
                     QuartzExecutionTask.class);
			Pauser pauser = new Pauser(3000);
			jobDetail.getJobDataMap().put(QuartzExecutionTask.TASK_TYPE, QuartzExecutionTaskType.CALLABLE);
			jobDetail.getJobDataMap().put(QuartzExecutionTask.TASK, pauser);
			Date startDate = TriggerUtils.getNextGivenSecondDate(new Date(), 10);
			Date endDate = TriggerUtils.getEvenHourDate(new Date());
			log("Start Time:" + startDate);
			log("End Time:" + endDate);
			FixedDelayTrigger frt = new FixedDelayTrigger("FixedDelayTrigger", "FixedRateTriggerGroup", startDate, null, SimpleTrigger.REPEAT_INDEFINITELY , 10000);
			scheduler.scheduleJob(jobDetail, frt);
			log("Job Scheduled");
			Thread.currentThread().join();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void log(Object message) {
		System.out.println("[" + new Date() + "]" + message);
	}


	/**
	 * @return the originalScheduler
	 */
	public Scheduler getOriginalScheduler() {
		return originalScheduler;
	}


	/**
	 * @param originalScheduler the originalScheduler to set
	 */
	public void setOriginalScheduler(Scheduler originalScheduler) {
		this.originalScheduler = originalScheduler;
	}
	

}

class Pauser implements Callable {
	protected long pauseTime = 10000;
	/**
	 * @param pauseTime
	 */
	public Pauser(long pauseTime) {
		super();
		this.pauseTime = pauseTime;
	}
	public Object call() throws Exception {
		log("Starting pause [" + pauseTime + "]\t(" + Thread.currentThread().toString() + ")");
		Thread.sleep(pauseTime);
		log("Pause Complete [" + pauseTime + "]");
		return null;
	}
	
	public static void log(Object message) {
		System.out.println("[" + new Date() + "]" + message);
	}
	
}
