/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package src.zeo.library;

import processing.core.*;

import java.util.Date;


/**
 * The ZeoReader class imports and parses Zeo Sleep Data export files (.csv)
 * and creates an array of ZeoNight objects that can be accessed and analysed.
 * 
 * @example zeo_readCsv 
 * @example zeo_graphTotalZ 
 * @example zeo_averageNight
 * @example zeo_graphComparison
 * @example zeo_graphStates5min
 * 
 * @author evsc
 * 
 */

public class ZeoReader {
	
	// myParent is a reference to the parent sketch
	private PApplet myParent;
	
	public static final String VERSION = "##library.prettyVersion##";
	
	private String FileName = "";
	
	public ZeoNight night[];
	public int nights = 0;			// total number of recorded nights
	public int day_span = 0;		// number of days between first and last night
	
	private boolean cutOffWake = true;
	private boolean useRegularOnly = false;
	
	/**
	 * minimum sleep onset time for selection filter, 
	 * in minutes (default: 20*60)
	 */
	public int regularMinHour = 20*60;		// earliest hour to go to bed, 8pm
	
	/**
	 * maximum sleep onset time for selection filter, 
	 * in minutes (default: 30*60)
	 */
	public int regularMaxHour = 30*60;		// latest hour to go to bed, 6am
	
	/**
	 * minimum sleep duration for selection filter, 
	 * in minutes (default: 4*60)
	 */
	public int regularMinLength = 4*60;		// in minutes
	
	/**
	 * maximum sleep duration for selection filter, 
	 * in minutes (default: 10*60)
	 */
	public int regularMaxLength = 10*60;	// in minutes
	
	/**
	 * Normal Constructor, usually called in the setup() method in your sketch to
	 * initialize library.
	 * 
	 * @param theParent
	 */
	public ZeoReader(PApplet theParent) {
		myParent = theParent;
		welcome();
	}
	
	/**
	 * Constructor version, usually called in setup(), 
	 * is given filename of Zeo Sleep Data export (.csv),
	 * initializes library and immediately imports
	 * and parses the data file
	 * 
	 * @param theParent
	 * @param file_name
	 * 			filename, should have .csv ending 
	 */
	public ZeoReader(PApplet theParent, String file_name) {
		myParent = theParent;
		welcome();
		readFile(file_name);
	}
	
	/**
	 * Imports and parses Zeo Sleep data from .csv file
	 * 
	 * @param file_name
	 * 				filename, should have .csv ending 
	 */
	public void readFile(String file_name) {
		FileName = file_name;
		System.out.println("|| \t Read in file '"+FileName+"'");
		
		String lines[] = myParent.loadStrings(FileName);
		nights = lines.length-1;
		System.out.println("|| \t Loading " + nights + " recorded nights ...");
		
		night = new ZeoNight[lines.length-1];
		
		int n = 0;
		for (int i=1; i < lines.length; i++) {
			
			String[] p = PApplet.split(lines[i], '"');
			
			String pp;
			if(p.length>1) {
				pp = p[0] + p[2];
			} else {
				pp = p[0];
			}
			
			String[] q = PApplet.split(pp, ",");
			
			if(q[1].length() > 0) {
				night[n] = new ZeoNight(this);
				night[n].setDate(q[0]);
				
				night[n].zq = getInt(q[1]);
				night[n].total_z = getInt(q[2]);
				night[n].time_to_z = getInt(q[3]);
				night[n].time_in_wake = getInt(q[4]);
				night[n].time_in_rem = getInt(q[5]);
				night[n].time_in_light = getInt(q[6]);
				night[n].time_in_deep = getInt(q[7]);
				night[n].awakenings = getInt(q[8]);
				
				night[n].setStartOfNight(q[9]);
				night[n].setEndOfNight(q[10]);
				night[n].setRiseTime(q[11]);
				
				night[n].alarm_reason = getInt(q[12]);
				night[n].alarm_type = getInt(q[16]);
				night[n].morning_feel = getInt(q[22]);
				
				night[n].ss_fall_asleep = getInt(q[27]);
				night[n].ss_aniticipation = getInt(q[28]);
				night[n].ss_tension = getInt(q[29]);
				night[n].ss_comfort = getInt(q[30]);
				night[n].ss_noise = getInt(q[31]);
				night[n].ss_light = getInt(q[32]);
				night[n].ss_temperature = getInt(q[33]);
				night[n].ss_familiar = getInt(q[34]);
				night[n].ss_bedroom = getInt(q[35]);
				night[n].ss_disruption = getInt(q[36]);
				night[n].ss_hot_flashes = getInt(q[37]);
				night[n].ss_dreams = getInt(q[38]);
				night[n].ss_fullness = getInt(q[39]);
				night[n].ss_hunger = getInt(q[40]);
				night[n].ss_heartburn = getInt(q[41]);
				night[n].ss_caffeine = getInt(q[42]);
				night[n].ss_alcohol = getInt(q[43]);
				night[n].ss_thirst = getInt(q[44]);
				night[n].ss_restroom = getInt(q[45]);
				night[n].ss_wind_down = getInt(q[46]);
				night[n].ss_sleepiness = getInt(q[47]);
				night[n].ss_exercise = getInt(q[48]);
				night[n].ss_time_before_bed = getInt(q[49]);
				night[n].ss_conversations = getInt(q[50]);
				night[n].ss_activity_level = getInt(q[51]);
				night[n].ss_late_work = getInt(q[52]);

				night[n].sscf_1 = getInt(q[53]);
				night[n].sscf_2 = getInt(q[54]);
				night[n].sscf_3 = getInt(q[55]);
				night[n].sscf_4 = getInt(q[56]);
				night[n].sscf_5 = getInt(q[57]);
				night[n].sscf_6 = getInt(q[58]);
				night[n].sscf_7 = getInt(q[59]);
				night[n].sscf_8 = getInt(q[60]);
				night[n].sscf_9 = getInt(q[61]);
				night[n].sscf_10 = getInt(q[62]);
				night[n].sscf_11 = getInt(q[63]);
				night[n].sscf_12 = getInt(q[64]);
				night[n].sscf_13 = getInt(q[65]);
				night[n].sscf_14 = getInt(q[66]);
				night[n].sscf_15 = getInt(q[67]);
				night[n].sscf_16 = getInt(q[68]);
				night[n].sscf_17 = getInt(q[69]);
				night[n].sscf_18 = getInt(q[70]);
				night[n].sscf_19 = getInt(q[71]);
				night[n].sscf_20 = getInt(q[72]);
				night[n].sscf_21 = getInt(q[73]);
				
				night[n].setSleepGraph5min(q[74], cutOffWake);
				night[n].setSleepGraph30sec(q[75], cutOffWake);
				n++;
			}
		}
		nights = n;
		System.out.println("|| \t Imported " + nights + " proper nights");
	
		Date day0 = new Date(night[0].date.getTime());
		for(int i=1; i<nights; i++) {
		    night[i].setDayRelative(day0);
		}
		day_span = night[nights-1].day_relative;
		System.out.println("|| \t from a total span of " + day_span +" days \n");
	}
	
	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public String version() {
		return VERSION;
	}
		
	/**
	 * Define if the sleep graph data starts with sleep states (2,3,4) 
	 * or if undefined (0) and wake (1) states should be included
	 * at the beginning of the arrays
	 * 
	 * @param cut
	 * 			true: cut off non-sleep states at beginning of sleepgraph arrays (=default)
	 */
	public void setCutOff(boolean cut) {
		cutOffWake = cut;
	}
	
	/**
	 * Toggle filter on or off. The filter selection will influence
	 * all future function-calls to ZeoReader, and nights excluded 
	 * by the filter won't be included in functions like getAverageZq(),
	 * getAverageTotalZ(), getAverageTimeInWake(), ...
	 * 
	 * The filter can be used to exclude extreme nights (jetlag nights, 
	 * nights with lost data,..) that would only falsify average values.
	 * 
	 * @param v
	 * TRUE: turn filter on - FALSE: turn filter off
	 */
	public void setFilter(boolean v) {
		useRegularOnly = v;
		System.out.println("|| \t useFilter:\t" + useRegularOnly);
	}
	
	/**
	 * Computate average ZQ score for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * @return
	 * average ZQ score
	 */
	public int getAverageZq() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].zq;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	/**
	 * Computate average total sleep duration for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average sleep duration in minutes
	 */
	public int getAverageTotalZ() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].total_z;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	/**
	 * Computate average time to fall asleep for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average time to fall asleep in minutes
	 */
	public int getAverageTimeToZ() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].time_to_z;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	/**
	 * Computate average time spent in wake for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average time spent in wake in minutes
	 */
	public int getAverageTimeInWake() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].time_in_wake;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	/**
	 * Computate average REM time for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average REM time in minutes
	 */
	public int getAverageTimeInRem() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].time_in_rem;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	
	/**
	 * Computate average Light Sleep time for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average Light Sleep time in minutes
	 */
	public int getAverageTimeInLight() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].time_in_light;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	
	/**
	 * Computate average Deep Sleep time for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average Deep Sleep time in minutes
	 */
	public int getAverageTimeInDeep() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].time_in_deep;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	
	/**
	 * Computate average duration between sleep onset and rise time
	 *  for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average duration in minutes
	 */
	public int getAverageDuration() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].sleep_duration;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	/**
	 * Computate average start time of night for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average start time in minutes
	 */
	public int getAverageStart() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].start_night_min;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	/**
	 * Computate average end time of night for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average end time in minutes
	 */
	public int getAverageEnd() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].end_night_min;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}

	/**
	 * Computate average sleep onset for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average sleep onset in minutes
	 */
	public int getAverageOnset() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].sleep_onset_min;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}

	/**
	 * Computate average rise time for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average rise time in minutes
	 */
	public int getAverageRise() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].sleep_rise_min;
				count++;
			}
		}
		if(count>0) return addAll/count;
		else return -1;
	}
	
	/**
	 * Computate average number of awakenings for all active nights.
	 * (If filter active, nights outside the filter parameters are not
	 * included in the selection). 
	 * 
	 * @return
	 * average number of awakenings
	 */
	public float getAverageAwakenings() {
		int addAll = 0;
		int count = 0;
		for(int i=0; i<nights; i++) {
			if(!useRegularOnly || night[i].isRegular()) {
				addAll += night[i].awakenings;
				count++;
			}
		}
		if(count>0) return addAll/ (float) count;
		else return -1;
	}
	
	/**
	 * Set the parameter that defines the selection filter.
	 * All values are in minutes, value 0 represents 0am on the
	 * date of the ZeoNight. 
	 * value 60*22 represents 10pm on the date of the Zeonight.
	 * 
	 * @param minh
	 * 		minimum sleep onset time, in minutes (default: 20*60)
	 * @param maxh
	 * 		maximum sleep onset time, in minutes (default: 30*60)
	 * @param minl
	 * 		minimum sleep duration in minutes (default: 4*60)
	 * @param maxl
	 * 		maximum sleep duration in minutes (default: 10*60)
	 */
	public void setFilterHours(int minh, int maxh, int minl, int maxl) {
		regularMinHour = minh*60;
		regularMaxHour = maxh*60;
		regularMinLength = minl*60;
		regularMaxLength = maxl*60;
		System.out.println("|| \t Filter: \tBedtime btw. " + minh%24 + "-"+maxh%24 +" o'clock");
		System.out.println("|| \t\tLength btw. " + minl + "-"+maxl +" hours");
	}
	
	
	private void welcome() {
		System.out.println("##library.name## ##library.prettyVersion## by ##author##");
		System.out.println("--------------------------------------------------------\n");
	}
	
	private int getInt(String input) {
		if(input != null && input.length() > 0) return Integer.parseInt(input);
		else return -1;
	}

}
