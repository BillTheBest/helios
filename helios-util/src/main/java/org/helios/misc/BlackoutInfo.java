package org.helios.misc;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * <p>Title: BlackoutInfo</p>
 * <p>Description: Class that stores blackout period information </p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class BlackoutInfo {

	/** Internal flag to indicate whether the specified black period span beyond midnight into next day  */
	private boolean spanMidnight = false;
	/** Parsed value of hour for start of the blackout period */
    private int startHour = 0;
    /** Parsed value of minute for start of the blackout period */
    private int startMinute = 0;
    /** Parsed value of hour for end of the blackout period */
    private int endHour = 0;
    /** Parsed value of minute for start of the blackout period */
    private int endMinute = 0;
	/** This flag is set to true only when blackout range validations have passed. */
	private boolean validRange = false;
	
	Logger log = Logger.getLogger(BlackoutInfo.class);
	/**
	 * Creates a new 
	 * @param blackoutStart
	 * @param blackoutEnd
	 * @param beanName
	 */
	public BlackoutInfo(String blackoutStart, String blackoutEnd, String beanName) {
        String[] startSplit = blackoutStart.split(":");
        if(startSplit!=null && startSplit.length != 2){
            log.warn("Blackout period ignored for bean " + beanName + " as start time is invalid - " + blackoutStart + " It should be in 24 hour HH:MM format.");
        }else{
            try{
                startHour = Integer.parseInt(startSplit[0]);
                startMinute = Integer.parseInt(startSplit[1]);
                if(startHour < 0 || startHour > 23 || startMinute < 0 || startMinute > 59)
                    throw new NumberFormatException();
            }catch(NumberFormatException nex){
            	log.warn("Blackout period ignored for bean " + beanName + " as start time is invalid - " + blackoutStart + " It should be in 24 hour HH:MM format.");
            }
        }
       
        String[] endSplit = blackoutEnd.split(":");    
        if(endSplit!=null && endSplit.length != 2){
            log.warn("Blackout period ignored for bean " + beanName + " as end time is invalid - " + blackoutEnd + " It should be in 24 hour HH:MM format.");
        }else{
            try{
                endHour = Integer.parseInt(endSplit[0]);
                endMinute = Integer.parseInt(endSplit[1]);
                if(endHour < 0 || endHour > 23 || endMinute < 0 || endMinute > 59)
                    throw new NumberFormatException();
            }catch(NumberFormatException nex){
                log.warn("Blackout period ignored for bean " + beanName + " as end time is invalid - " + blackoutEnd + " It should be in 24 hour HH:MM format.");
            }
        }
       
        if(endHour < startHour){
            spanMidnight = true;
        }else if(endHour == startHour && endMinute < startMinute){
            spanMidnight = true;
        }    
        
        validRange = true;   
	}

    /**
     * Checks whether the blackout window is currently active
     * @return boolean flag - true for active blackout window, false otherwise
     */
    public boolean isBlackoutActive() {
        Date currDate, beginPeriod, endPeriod = null;
        Calendar currCalendar = Calendar.getInstance();
        currDate = currCalendar.getTime();
        currCalendar.set(Calendar.HOUR_OF_DAY, startHour);
        currCalendar.set(Calendar.MINUTE, startMinute);
        currCalendar.set(Calendar.SECOND,0);
        beginPeriod = currCalendar.getTime();
        currCalendar.set(Calendar.HOUR_OF_DAY, endHour);
        currCalendar.set(Calendar.MINUTE, endMinute);
        currCalendar.set(Calendar.SECOND,0);
        if(spanMidnight){
            currCalendar.roll(Calendar.DATE, true);
        }
        endPeriod = currCalendar.getTime();
        if(currDate.after(beginPeriod) && currDate.before(endPeriod)){
            return true;
        }else{
            return false;
        }
    }	
	
	/**
	 * @return the spanMidnight
	 */
	public boolean isSpanMidnight() {
		return spanMidnight;
	}

	/**
	 * @return the validRange
	 */
	public boolean isValidRange() {
		return validRange;
	}    
	
}
