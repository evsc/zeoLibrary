package src.zeo.library;

import processing.core.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Date.html
// http://docs.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html


/**
 * ZeoNight class,
 * contains all sleep data information and 
 * most of your Zeo sleep diary information
 * 
 * @author evsc 
 * 
 */

public class ZeoNight {
	
	private ZeoReader mr;				// myReader
	
	/**
	 * date of night (always date from previous day)
	 */
	public Date date;

	/**
	 * Month, with 0 representing January
	 */
	public int month;
	
	/**
	 * year
	 */
	public int year;
	
	/**
	 * Day of the week
	 * 0 = Sunday 
	 * 1 = Monday
	 * 2 = Tuesday
	 * 3 = Wednesday
	 * 4 = Thursday
	 * 5 = Friday
	 * 6 = Saturday
	 */
	public int day_of_week;	
	
	/**
	 * day relative to first day of array of nights
	 */
	public int day_relative;

	/**
	 * ZQ score
	 */
	public int zq; 
	
	/**
	 * total sleep minutes
	 */
	public int total_z;		
	
	/**
	 * minutes to fall asleep
	 */
	public int time_to_z;	
	
	/**
	 * minutes in wake
	 */
	public int time_in_wake;
	
	/**
	 * minutes in rem
	 */
	public int time_in_rem;	
	
	/**
	 * minutes in light sleep
	 */
	public int time_in_light;	
	
	/**
	 * minutes in deep sleep
	 */
	public int time_in_deep;	
	
	/**
	 * number of awakenings
	 */
	public int awakenings;	
	
	/**
	 * Total minutes between falling asleep and waking up.
	 * Different to total_z, as it also counts the waking 
	 * minutes during the night
	 */
	public int sleep_duration;	
	
	/**
	 * Moment of sleep onset, when the sleeper goes
	 * from a wake state into one of the sleep states,
	 * counted in minutes
	 */
	public int sleep_onset_min;
	
	/**
	 * Rise time in the morning, counted in minutes
	 */
	public int sleep_rise_min;
	
	/**
	 * Start of the night, when the headband is activated
	 * and starts to record states, counted in minutes
	 */
	public int start_night_min;
	
	/**
	 * End of the night, when the headband is deactivated, 
	 * counted in minutes
	 */
	public int end_night_min;
	
	/**
	 *  The date and time corresponding to the first moment in a night of sleep
is recorded. This time is always aligned to a 5-minute boundary. */
	public Date start_of_night;
	
	/**
	 *  The date and time at which no further sleep data was collected for that
night. */
	public Date end_of_night;	
	
	/**
	 *  The date and time the user awoke. This is computed as "the time of
day at the end of the last 5 minute block of sleep in the sleep graph." If
no sleep is present in the sleep graph, the value is null */
	public Date rise_time;

	/**
	 *  The date and time when a first not-wake sleep state occurs 
	 */
	public Date sleep_onset;		
	
	/**
	 *  Indicates the reason for the most recent alarm ringing.
	 * 	- 0 - REM to NREM Transition
	 * 	- 1 - NREM to REM Transition
	 * 	- 2 - Wake while awake
	 * 	- 3 - Prevent waking from Deep sleep
	 * 	- 4 - End of Wake Window
	 * 	- 5 - No Alarm 
	*/
	public int alarm_reason;	
	
	/**
	 *  Indicates which alarm type is enabled.
	 * 	- 0 - standard wake
	 * 	- 1 - SmartWake */
	public int alarm_type;
	
	/**
	 *  Indicates the user's perception of how they slept that night. Null
indicates they entered no rating.
	 * 	- 1 - Terribly
	 * 	- 2 - Poorly
	 * 	- 3 - Okay
	 * 	- 4 - Well
	 * 	- 5 - Great
	 */
	public int morning_feel;
	
	
	/*
	 *  sleep stealer information (0-3 or null)
	 */
	public int ss_fall_asleep;
	public int ss_aniticipation;
	public int ss_tension;
	public int ss_comfort;
	public int ss_noise;
	public int ss_light;
	public int ss_temperature;
	public int ss_familiar;
	public int ss_bedroom;
	public int ss_disruption;
	public int ss_hot_flashes;
	public int ss_dreams;
	public int ss_fullness;
	public int ss_hunger;
	public int ss_heartburn;
	public int ss_caffeine;
	public int ss_alcohol;
	public int ss_thirst;
	public int ss_restroom;
	public int ss_wind_down;
	public int ss_sleepiness;
	public int ss_exercise;
	public int ss_time_before_bed;
	public int ss_conversations;
	public int ss_activity_level;
	public int ss_late_work;
	  
	// custom sleep stealer information (0-3 or null)
	public int sscf_1;
	public int sscf_2;
	public int sscf_3;
	public int sscf_4;
	public int sscf_5;
	public int sscf_6;
	public int sscf_7;
	public int sscf_8;
	public int sscf_9;
	public int sscf_10;
	public int sscf_11;
	public int sscf_12;
	public int sscf_13;
	public int sscf_14;
	public int sscf_15;
	public int sscf_16;
	public int sscf_17;
	public int sscf_18;
	public int sscf_19;
	public int sscf_20;
	public int sscf_21;

	
	/**
	 *  A 5-minute sleep graph containing a space-separated
		list of numbers. Each number represents a 5-minute
		time period (ex. a 6-hour sleep graph would have 72
		integers). The sleep stages are encoded as:
		- 0 - undefined
		- 1 - Wake
		- 2 - REM
		- 3 - Light
		- 4 - Deep
	 */
	public int sleep_graph_5min[];
	
	/**
	 *  A 30-second sleep graph containing a spaceseparated
		list of numbers. Each number represents a
		30-second time period
		The sleep stages are encoded as:
		- 0 - undefined
		- 1 - wake
		- 2 - REM
		- 3 - Light
		- 4 - Deep
	 */
	public int sleep_graph_30sec[];
	
	/**
	 * Constructor
	 * 
	 * @param theReader reference to ZeoReader object
	 */
	public ZeoNight(ZeoReader theReader) {
		mr = theReader;
	}
	
	/**
	 * set main date of night
	 * 
	 * @param input 
	 * 			date in string form
	 */
	@SuppressWarnings("deprecation")
	public void setDate(String input) {
		date = getDate(input, true);
	    day_of_week = date.getDay();
	    month = date.getMonth();
	    year = date.getYear();
//	    System.out.println("ZeoNight "+printDate(date, true));
	}
	
	/**
	 * convert string to date
	 */
	private Date getDate(String input, boolean simple) {
		DateFormat stampFormat;
		if(simple) {
			stampFormat = new SimpleDateFormat("MM/dd/yyyy"); 
		} else {
			stampFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		} 
	    Date initialDate = null;

	    try {  
	      initialDate = stampFormat.parse(input);
	    } catch (Exception e) { 
	      System.out.println("Unable to parse date stamp '"+input+"'");
	      return null;
	    }
	    long initialDateMillis = initialDate.getTime();
	    return new Date(initialDateMillis);
	}
	
	public void setStartOfNight(String input) {
		start_of_night = getDate(input, false);
		sleep_onset = new Date(start_of_night.getTime() + time_to_z*1000*60);
	}
	
	public void setEndOfNight(String input) {
		end_of_night = getDate(input, false);
	}
	
	public void setRiseTime(String input) {
		rise_time = getDate(input, false);
//		System.out.println("ZeoNight | rise_time "+printDate(rise_time, true));
		
		calculateMinutes();
	}
	
	private void calculateMinutes() {
		sleep_onset_min = (int) ((sleep_onset.getTime() - date.getTime())/(1000*60) );
		sleep_rise_min = (int) ((rise_time.getTime() - date.getTime())/(1000*60) );
		sleep_duration = (int) ((rise_time.getTime() - sleep_onset.getTime())/(1000*60) );
		start_night_min = (int)  ((start_of_night.getTime() - date.getTime())/(1000*60) );
		end_night_min = (int)  ((end_of_night.getTime() - date.getTime())/(1000*60) );
	}
	
	
	/**
	 * Verify if nights falls within or without the selection filter
	 * 
	 * @return true/false
	 */
	public boolean isRegular() {
		if(sleep_onset_min > mr.regularMinHour && sleep_onset_min < mr.regularMaxHour && sleep_duration > mr.regularMinLength && sleep_duration < mr.regularMaxLength) return true;
	    else return false;
	}
	
	/**
	 * Returns the date of the night in SimpleDateFormat"dd/MM/yyyy"
	 * 
	 * @return formatted date of night in String
	 */
	public String returnDateString() {
		return printDate(date, false);
	}
	
	private String printDate(Date d, boolean full) {
		DateFormat niceFormat;
		if(full) {
	    	niceFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");
		} else {
			niceFormat = new SimpleDateFormat("dd/MM/yyyy");
		}
	    return niceFormat.format(d);
	}
	
	public void setSleepGraph5min(String input, boolean cutOffWake) {
		if(input != null && input.length() > 0) {
			String[] p = PApplet.split(input, " ");
			
			int temp[] = new int[p.length];
			int j = 0;
			boolean cut = cutOffWake;
			int v;
			for(int i=0; i<p.length; i++) {
				v = Integer.parseInt(p[i]);
				if(cut) {
					if(v>1) {
						cut = false;
						temp[j++] = v;
					}
				} else {
					temp[j++] = v;
				}
			}
			sleep_graph_5min = new int[j];
			for(int i=0; i<j; i++) sleep_graph_5min[i] = temp[i];
		}
	}
	
	public void setSleepGraph30sec(String input, boolean cutOffWake) {
		if(input != null && input.length() > 0) {
			String[] p = PApplet.split(input, " ");
			
			int temp[] = new int[p.length];
			int j = 0;
			boolean cut = cutOffWake;
			int v;
			for(int i=0; i<p.length; i++) {
				v = Integer.parseInt(p[i]);
				if(cut) {
					if(v>1) {
						cut = false;
						temp[j++] = v;
					}
				} else {
					temp[j++] = v;
				}
			}
			sleep_graph_30sec = new int[j];
			for(int i=0; i<j; i++) sleep_graph_30sec[i] = temp[i];
		}
	}

	public void setDayRelative(Date d0) {
		day_relative = (int) ((date.getTime() - d0.getTime())/(1000*60*60*24) );
	}
	
}
