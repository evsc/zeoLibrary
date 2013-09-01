/*-----------------------------------------------------------------------------
Copyright (c) 2010, Zeo, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright 
      notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above 
      copyright notice, this list of conditions and the following 
      disclaimer in the documentation and/or other materials provided 
      with the distribution.

 * Neither the name of Zeo, Inc. nor the names of its contributors 
      may be used to endorse or promote products derived from this 
      software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ZEO, INC. BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE.
------------------------------------------------------------------------------*/
package src.com.myzeo.decoder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Core functionality for decoding a Zeo data record. This class decodes the
 * saved data records from firmware 2.6.3O only. It cannot decode earlier
 * versions.
 */
public class ZeoData implements Comparable<ZeoData> {

	/*************************************************************************
	 * defines
	 */

	/**
	 * Use a default timezone value of UTC/GMT so that the user's timestamp
	 * values aren't adjusted in any way for time zone variations. Thus the
	 * times show up with the same relative values they had on Zeo.
	 */
	private static final TimeZone DEFAULT_TIMEZONE = TimeZone
			.getTimeZone("UTC");

	/**
	 * The maximum number of seconds after power up that we will recognize a
	 * record as the first record recorded following the power up.
	 */
	private static final long FIRST_RECORD_TIMEOUT = 10;

	/** All sleep data records must start with the following identifier string. */
	public static final byte[] IDENTIFIER = new byte[] { (byte) 'S',
			(byte) 'L', (byte) 'E', (byte) 'E', (byte) 'P', (byte) '\0' };

	/** A two byte format version value follows the identifier. */
	public static final int IDENTIFIER_SIZE = 6;
	public static final int VERSION_SIZE = 2;

	public static final int HEADER_SIZE = IDENTIFIER_SIZE + VERSION_SIZE;

	/**
	 * The version value is enumerated here. The value is a little endian
	 * number.
	 */
	public static final byte[] V22 = { (byte) 0x16, (byte) 0x00 }; // 21

	/**
	 * The record version size.
	 */
	public static final int V22_SIZE = 1680;

	/* Record specific defines */

	/**
	 * The number of events associated with alarm ringing that are saved.
	 */
	private static final int ALARM_EVENTS_SAVED = 2;

	/**
	 * Number of characters stored for the assert function name (including a
	 * NULL terminator).
	 */
	private static final int ASSERT_NAME_MAX = 20;

	/** The number of events saved. */
	private static final int EVENTS_SAVED = 4;

	/**
	 * Amount of storage in bytes for the headband impedance. Each stored value
	 * represents an average over a 10 minute period. The total amount of
	 * storage is 24 hours of 10 minute averages.
	 */
	private static final int HEADBAND_IMPEDANCE_SIZE = 144;

	/**
	 * Amount of storage in bytes for the packets lost over a 10 minute period
	 * for the last 24 hours. Each 10 minute period is the number of packets
	 * lost / 5 capped at the value 255.
	 */
	private static final int HEADBAND_PACKETS_SIZE = 144;

	/** Amount of storage in bytes for the headband RSSI. */
	private static final int HEADBAND_RSSI_SIZE = 144;

	/**
	 * Amount of storage in bytes for the headband status. Each stored value
	 * represents an average over a 10 minute period for the last 24 hours. For
	 * each byte contained in an array of size HEADBAND_STATUS_SIZE status
	 * values of 2 bits in width are packed together with the earliest value
	 * holding the least significant bits.
	 */
	private static final int HEADBAND_STATUS_SIZE = 36;

	/**
	 * Maximum number of bytes there can be in a record. This value should be
	 * larger than the size of all past sleep record versions. This value is
	 * used to limit how far we search for another record.
	 */
	public static final int MAX_RECORD_BYTES = 2000;

	/** The number of events associated with alarm snoozing that are saved. */
	private static final int SNOOZE_EVENTS_SAVED = 9;

	/**
	 * Generic Zeo defines that correspond to Zeo's time definition, hypnogram
	 * lengths and the like. Please note that the ordering of these definitions
	 * is forced due to Java's insistence on not having static variables forward
	 * referencing other static variables.
	 */

	/*
	 * Time conversion constants. For computing seconds per month, we use 30
	 * days per month to get the length of an "average" month rather than using
	 * the DAYS_PER_MONTH_MAX value which would give the length of the longest
	 * month.
	 */
	public static final int DAYS_PER_MONTH = (30);
	public static final int DAYS_PER_MONTH_MAX = (31);
	public static final int DAYS_PER_WEEK = (7);
	public static final int HOURS_PER_DAY = (24);
	public static final int MINUTES_PER_HOUR = (60);
	public static final int MONTHS_PER_YEAR = (12);
	public static final int MSEC_PER_SECOND = (1000);

	public static final int SECONDS_PER_EPOCH = (30);
	public static final int SECONDS_PER_MINUTE = (60);

	public static final int SECONDS_PER_HOUR = (MINUTES_PER_HOUR * SECONDS_PER_MINUTE); // 3600
	public static final int SECONDS_PER_DAY = (HOURS_PER_DAY * SECONDS_PER_HOUR); // 86400

	public static final int SECONDS_PER_MONTH = (DAYS_PER_MONTH * SECONDS_PER_DAY);
	public static final int SECONDS_PER_WEEK = (DAYS_PER_WEEK * SECONDS_PER_DAY);

	public static final int EPOCHS_PER_MINUTE = (SECONDS_PER_MINUTE / SECONDS_PER_EPOCH); // 2
	public static final int EPOCHS_PER_HOUR = (SECONDS_PER_HOUR / SECONDS_PER_EPOCH); // 120

	/*
	 * This module maintains two hypnograms referred to as Base and Display
	 * hypnograms. The Base hypnogram points each represent 30-second sleep
	 * epochs. The Display hypnogram points each represent 5 minutes.
	 * 
	 * HYP_BASE_LENGTH Max number of Base hypnogram points. HYP_BASE_PER_DISPLAY
	 * How many Base hypnogram values are combined to make a Display hypnogram
	 * value. HYP_BASE_STEP How many seconds between Base hypnogram points.
	 * 
	 * HYP_DISPLAY_LENGTH Max number of Display hypnogram points.
	 * HYP_DISPLAY_STEP How many seconds between Display hypnogram points.
	 * 
	 * HYP_SECONDS_MAX Maximum seconds of data in hypnogram for a night.
	 */
	public static final int HYP_BASE_STEP = (SECONDS_PER_EPOCH);
	public static final int HYP_DISPLAY_STEP = (5 * SECONDS_PER_MINUTE);

	public static final int HYP_SECONDS_MAX = (16 * SECONDS_PER_HOUR);

	public static final int HYP_BASE_LENGTH = (HYP_SECONDS_MAX / HYP_BASE_STEP);
	public static final int HYP_BASE_PER_DISPLAY = (HYP_DISPLAY_STEP / HYP_BASE_STEP);

	public static final int HYP_DISPLAY_LENGTH = (HYP_SECONDS_MAX / HYP_DISPLAY_STEP);

	/*************************************************************************
	 * variables
	 */
	public boolean is_nap = false; // This record is a nap

	private int base_hypnogram_count; // Count of hypnogram values
	private long crc; // CRC16 value of record
	private Calendar current_time; // Time the record was written.
	private Calendar end_of_night; // End time for night (or null)

	@SuppressWarnings("unused")
	private boolean reset_record; // True for first reset record
	private Calendar sleep_date; // Decoder assigned "day of sleep"
	private Calendar start_of_night; // Start time for night (or null)

	/* Bit fields from data record */
	private boolean airplane_mode;
	private AlarmReason alarm_reason; // Reason alarm was triggered.
	private byte backlight;
	private ClockMode clock_mode; // 12/24 Hour selection
	private boolean sleep_valid; // Indicates sleep-data-valid condition
	private int snooze_time;
	private WakeTone wake_tone; // Wake tone selection
	private int wake_window; // Size of the wake window
	private boolean wdt_reset; // Did watchdog reset occur
	private WriteReason write_reason; // Reason record was saved to card
	private boolean zeo_wake_on; // Is Zeo handling wake

	/* Device History from data record */
	private Calendar airplane_off; // Last time airplane mode disengaged
	private Calendar airplane_on; // Last time airplane mode engaged

	private TimeChange[] alarm_change = new TimeChange[EVENTS_SAVED];

	private char[] assert_function_name = new char[ASSERT_NAME_MAX];
	private int assert_line_number;

	private Calendar factory_reset;
	private long headband_id;

	private short[] headband_impedance = new short[HEADBAND_IMPEDANCE_SIZE];
	private short[] headband_packets = new short[HEADBAND_PACKETS_SIZE];
	private byte[] headband_rssi = new byte[HEADBAND_RSSI_SIZE];
	private short[] headband_status = new short[HEADBAND_STATUS_SIZE];

	private int id_hw; // HW revision value
	private int id_sw; // SW revision value
	private int format_version; // Data format version

	private TimeChange[] rtc_change = new TimeChange[EVENTS_SAVED];

	private Calendar sensor_life_reset;
	private Calendar sleep_stat_reset;

	/* Sleep Information from data record */
	private Calendar[] alarm_ring = new Calendar[ALARM_EVENTS_SAVED];
	private Calendar[] alarm_snooze = new Calendar[SNOOZE_EVENTS_SAVED];
	private Calendar alarm_off;
	private int awakenings;
	private int awakenings_average;
	private int sleep_rating;
	private int time_in_deep;
	private int time_in_deep_average;
	private int time_in_deep_best;
	private int time_in_light;
	private int time_in_light_average;
	private int time_in_rem;
	private int time_in_rem_average;
	private int time_in_rem_best;
	private int time_in_wake;
	private int time_in_wake_average;
	private int time_to_z;
	private int time_to_z_average;
	private int total_z;
	private int total_z_average;
	private int total_z_best;
	private int zq_score;
	private int zq_score_average;
	private int zq_score_best;

	/*
	 * Data variables that track the display hypnograms forced stage and where
	 * in the hypnogram it was stored.
	 */
	private int display_hypnogram_forced_index;
	private int display_hypnogram_forced_stage;

	/*
	 * The start time of the hypnograms which is not necessarily the same as the
	 * start time for the night of sleep.
	 */
	private Calendar hypnogram_start_time;

	private byte[] base_hypnogram = new byte[HYP_BASE_LENGTH];

	/* Generated display hypnogram. */
	private byte[] display_hypnogram = new byte[HYP_DISPLAY_LENGTH];
	private int display_hypnogram_count;

	/* Derived sleep information. */
	private Calendar alarm_set_time; // Time alarm was set for
	private Calendar rise_time; // When user got up

	/*************************************************************************
	 * methods
	 */

	/**
	 * Construct a Zeo data record given the input stream and data format
	 * version identification.
	 * 
	 * @param in
	 *            The data input stream that will be read to construct the
	 *            record.
	 * @param version
	 *            The version number of the record to decode.
	 */
	public ZeoData(ByteBuffer in, int version) {
		int alarm_ring_count;
		Calendar alarm_ring_time;
		long alarm_ring_value;
		long change_time[] = new long[EVENTS_SAVED];
		long change_value[] = new long[EVENTS_SAVED];
		long current_timestamp;
		int tmp;

		/* Remember format version */
		format_version = version;

		/* Set the byte order to little endian. */
		in.order(ByteOrder.LITTLE_ENDIAN);

		/* Grab the time the record was written. */
		current_timestamp = read_uint32(in);
		current_time = timestamp_to_calendar(current_timestamp);

		/*
		 * Get the record's CRC16 value and overwrite the CRC value with zeroes
		 * so we are ready to compute the CRC of the buffer in a way that
		 * matches how the base station computed the CRC.
		 */
		int crc_index = in.position();
		crc = read_uint32(in);
		in.putInt(crc_index, 0);

		/* Device History packed values (bit fields) */
		tmp = (int) read_uint32(in);

		airplane_mode = ((tmp & 1) == 1) ? true : false;
		alarm_reason = AlarmReason.convert((tmp >> 1) & 0x7);
		backlight = (byte) ((tmp >> 4) & 0xf);
		clock_mode = (((tmp >> 8) & 1) == 1) ? ClockMode.HOUR_12
				: ClockMode.MILITARY;
		sleep_valid = (((tmp >> 9) & 1) == 1) ? true : false;
		snooze_time = ((tmp >> 10) & 0x1f);
		wake_tone = WakeTone.convert((tmp >> 15) & 0x7);
		wake_window = ((tmp >> 18) & 0x3f);
		write_reason = WriteReason.convert((tmp >> 24) & 0x7);
		zeo_wake_on = (((tmp >> 27) & 1) == 1) ? true : false;
		wdt_reset = (((tmp >> 28) & 1) == 1) ? true : false;

		/* Device history. */
		airplane_off = timestamp_to_calendar(read_uint32(in));
		airplane_on = timestamp_to_calendar(read_uint32(in));

		for (int i = 0; i < EVENTS_SAVED; i++) {
			change_time[i] = read_uint32(in);
		}
		for (int i = 0; i < EVENTS_SAVED; i++) {
			change_value[i] = read_uint32(in);
		}
		for (int i = 0; i < EVENTS_SAVED; i++) {
			alarm_change[i] = new TimeChange(change_time[i], change_value[i]);
		}

		for (int i = 0; i < ASSERT_NAME_MAX; i++) {
			assert_function_name[i] = (char) read_uint8(in);
		}
		assert_line_number = read_int32(in);

		factory_reset = timestamp_to_calendar(read_uint32(in));
		headband_id = read_uint32(in);
		for (int i = 0; i < HEADBAND_IMPEDANCE_SIZE; i++) {
			headband_impedance[i] = read_uint8(in);
		}
		for (int i = 0; i < HEADBAND_PACKETS_SIZE; i++) {
			headband_packets[i] = read_uint8(in);
		}
		for (int i = 0; i < HEADBAND_RSSI_SIZE; i++) {
			headband_rssi[i] = read_int8(in);
		}
		for (int i = 0; i < HEADBAND_STATUS_SIZE; i++) {
			headband_status[i] = read_uint8(in);
		}
		id_hw = read_uint16(in);
		id_sw = read_uint16(in);

		for (int i = 0; i < EVENTS_SAVED; i++) {
			change_time[i] = read_uint32(in);
		}
		for (int i = 0; i < EVENTS_SAVED; i++) {
			change_value[i] = read_uint32(in);
		}
		for (int i = 0; i < EVENTS_SAVED; i++) {
			rtc_change[i] = new TimeChange(change_time[i], change_value[i]);
		}
		sensor_life_reset = timestamp_to_calendar(read_uint32(in));
		sleep_stat_reset = timestamp_to_calendar(read_uint32(in));

		/* Sleep Information */

		/*
		 * Read in the alarm ring information. Here we only store the first and
		 * last alarm ring.
		 */
		alarm_ring_count = ALARM_EVENTS_SAVED;

		/*
		 * Make sure second alarm ring entry value defaults to being 0 since we
		 * might not fill it in.
		 */
		alarm_ring[1] = timestamp_to_calendar(0);

		for (int i = 0; i < alarm_ring_count; i++) {
			alarm_ring_value = read_uint32(in);
			alarm_ring_time = timestamp_to_calendar(alarm_ring_value);

			/*
			 * Store the first ring time value in the first entry and the last
			 * non-zero value in the second entry.
			 */
			if (i == 0) {
				alarm_ring[0] = alarm_ring_time;
			} else if (alarm_ring_value != 0) {
				alarm_ring[1] = alarm_ring_time;
			}
		}

		/* Read in array of snooze time values */
		for (int i = 0; i < SNOOZE_EVENTS_SAVED; i++) {
			alarm_snooze[i] = timestamp_to_calendar(read_uint32(in));
		}
		alarm_off = timestamp_to_calendar(read_uint32(in));

		awakenings = read_uint16(in);
		awakenings_average = read_uint16(in);
		end_of_night = timestamp_to_calendar(read_uint32(in));
		start_of_night = timestamp_to_calendar(read_uint32(in));
		time_in_deep = read_uint16(in);
		time_in_deep_average = read_uint16(in);
		time_in_deep_best = read_uint16(in);
		time_in_light = read_uint16(in);
		time_in_light_average = read_uint16(in);
		time_in_rem = read_uint16(in);
		time_in_rem_average = read_uint16(in);
		time_in_rem_best = read_uint16(in);
		time_in_wake = read_uint16(in);
		time_in_wake_average = read_uint16(in);
		time_to_z = read_uint16(in);
		time_to_z_average = read_uint16(in);
		total_z = read_uint16(in);
		total_z_average = read_uint16(in);
		total_z_best = read_uint16(in);
		zq_score = read_uint16(in);
		zq_score_average = read_uint16(in);
		zq_score_best = read_uint16(in);

		/* Read in the display hypnogram forced stage and its index. */
		display_hypnogram_forced_index = read_uint16(in);

		/* Uses a forced stage value and no padding. */
		display_hypnogram_forced_stage = read_uint16(in);

		/* Read the start time for the hypnograms. */
		hypnogram_start_time = timestamp_to_calendar(read_uint32(in));

		/* Read in sleep rating value, then 7 bytes of padding. */
		sleep_rating = 0;
		sleep_rating = read_uint8(in);
		read_uint8(in);
		read_uint8(in);
		read_uint8(in);
		read_uint32(in);

		/* Read in the base hypnogram. */
		base_hypnogram_count = (int) read_uint32(in);
		int hypnogram_data;
		for (int i = 0; i < HYP_BASE_LENGTH; i += 2) {
			/*
			 * Read in a byte for the hypnogram data and convert it to two
			 * values that are stored in the hypnogram.
			 */
			hypnogram_data = read_uint8(in);
			base_hypnogram[i] = (byte) (hypnogram_data & 0xf);
			base_hypnogram[i + 1] = (byte) (hypnogram_data >> 4);
		}

		/* Make sure we processed the complete record */
		assert (in.remaining() == 0);

		/* Create the display hypnogram from the base hypnogram. */
		make_display_hypnogram_from_base();

		/* Compute the rise time from the display hypnogram. */
		rise_time = compute_rise_time();

		/* Compute the alarm set time from device history. */
		alarm_set_time = compute_alarm_set_time();
	}

	/**
	 * Generate a human readable string containing a calendar.
	 * 
	 * @param calendar
	 *            The calendar that is to be converted to a human readable
	 *            string
	 * 
	 * @return a String containing the calendar's value.
	 */
	static String calendar_to_human_string(Calendar calendar) {
		if (calendar == null) {
			return null;
		} else {
			StringWriter txt = new StringWriter();
			PrintWriter out = new PrintWriter(txt);

			int day = calendar.get(Calendar.DAY_OF_MONTH);
			int month = calendar.get(Calendar.MONTH) + 1;
			int year = calendar.get(Calendar.YEAR);

			/* If the date isn't the start of the epoch then print it */
			if (year != 1970 || month != 1 || day != 1) {
				out.printf("%04d-%02d-%02d T ", year, month, day);
			}
			out.printf("%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY),
					calendar.get(Calendar.MINUTE),
					calendar.get(Calendar.SECOND));

			/* Print the UNIX timestamp value. */
			out.printf(" (%d)", calendar.getTimeInMillis() / MSEC_PER_SECOND);

			return txt.toString();
		}
	}

	/**
	 * Generates a string containing xml for the given calendar value.
	 * 
	 * @param calendar
	 *            The calendar value that is to be converted to XML.
	 * 
	 * @return a String containing an xml representation of the calendar value.
	 */
	static String calendar_to_xml(Calendar calendar) {
		if (calendar == null) {
			return "";
		} else {
			StringWriter txt = new StringWriter();
			PrintWriter out = new PrintWriter(txt);

			out.printf("<year>%d</year>", calendar.get(Calendar.YEAR));
			out.println();
			out.printf("<month>%d</month>", calendar.get(Calendar.MONTH) + 1);
			out.println();
			out.printf("<day>%d</day>", calendar.get(Calendar.DAY_OF_MONTH));
			out.println();
			out.printf("<hour>%d</hour>", calendar.get(Calendar.HOUR_OF_DAY));
			out.println();
			out.printf("<minute>%d</minute>", calendar.get(Calendar.MINUTE));
			out.println();
			out.printf("<second>%d</second>", calendar.get(Calendar.SECOND));
			out.println();
			return txt.toString();
		}
	}

	/**
	 * Implementation of comparable for sorting records based on sleep start
	 * times. It returns whether or not this record is "smaller", "equal to", or
	 * "larger" than the other record. This determination is based on the
	 * start_of_night values. Records with null start_of_night are considered
	 * smaller than all others. If both records have null start times they are
	 * considered equal. Please note that "compareTo == 0" and equals are not
	 * one and the same when examining ZeoRecords. The "compareTo == 0" in this
	 * context means that both records have the same start time.
	 * 
	 * @param other
	 *            The ZeoData record to compare to this record
	 * 
	 * @return -1 if this record is smaller than the other record, 0 if the
	 *         records have equal start times, 1 if this record is larger than
	 *         the other record.
	 */
	public int compareTo(ZeoData other) {
		Calendar other_start_of_night;

		/* The comparison is based on the start_of_night values */
		other_start_of_night = other.get_start_of_night();

		/* Handle cases where one or both nights are incomplete */
		if (start_of_night == null && other_start_of_night == null) {
			return 0;
		} else if (start_of_night == null) {
			return -1;
		} else if (other_start_of_night == null) {
			return 1;
		}

		/* The result is primarily determined based on night start times. */
		if (start_of_night.before(other_start_of_night)) {
			return -1;
		} else if (start_of_night.after(other_start_of_night)) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Compares two records and indicates whether or not the given record is
	 * "larger" or "smaller" than some other record. This comparison is made
	 * based on the "length of sleep" the record represents. The length is
	 * primarily the time between the start and end of the night. If this night
	 * has a shorter length it's smaller. If it has a longer length it's larger.
	 * If both nights have the same length based on start and end times then the
	 * hypnogram count values are examined. In this case, if this night has a
	 * smaller count than the other night it's smaller. If it has a larger count
	 * it's larger. Only when both the night elapsed time and hypnogram counts
	 * are the same are the nights "equal". Please note that
	 * "compareLength == 0" and equals are not at all the same when examining
	 * ZeoRecords. The "compareLength == 0" in this context means that both
	 * records represent the same length of time. However, they may not even
	 * represent the same sleep episode.
	 * 
	 * If the condition exists that one or the other start or end of night
	 * values is undefined then whatever night has an undefined values is deemed
	 * shorter than the other. If both records have undefined values then we
	 * assume the records are equal.
	 * 
	 * @param other
	 *            The ZeoData record to compare to this record.
	 * 
	 * @return -1 if this record has shorter sleep time than the other record, 0
	 *         if this records have equal length sleep time (or undefined), 1 if
	 *         this record has longer sleep time than the other record.
	 */
	public int compareLength(ZeoData other) {
		Calendar other_end_of_night;
		int other_hypnogram_count;
		long other_length;
		Calendar other_start_of_night;

		int this_hypnogram_count;
		long this_length;

		other_start_of_night = other.get_start_of_night();
		other_end_of_night = other.get_end_of_night();

		/*
		 * Handle cases where one or both nights are incomplete. We only need to
		 * check the end_of_night values since start_of_night will always be set
		 * if end_of_night is.
		 */
		if (end_of_night == null && other_end_of_night == null) {
			return 0;
		} else if (end_of_night == null) {
			return -1;
		} else if (other_end_of_night == null) {
			return 1;
		}

		assert (start_of_night != null && other_start_of_night != null);

		/* Determine the length of time for each night */
		other_length = other_end_of_night.getTimeInMillis()
				- other_start_of_night.getTimeInMillis();
		this_length = end_of_night.getTimeInMillis()
				- start_of_night.getTimeInMillis();

		/* The result is primarily determined based on night end times. */
		if (this_length < other_length) {
			return -1;
		} else if (this_length > other_length) {
			return 1;
		} else {
			/* Lengths are the same so go by hypnogram count values */
			other_hypnogram_count = other.get_base_hypnogram_count();
			this_hypnogram_count = base_hypnogram_count;

			if (this_hypnogram_count < other_hypnogram_count) {
				return -1;
			} else if (this_hypnogram_count > other_hypnogram_count) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	/**
	 * Compute the set alarm time that was in use for the night of sleep
	 * represented by this record. If the alarm was turned on, the set alarm
	 * time as a full date and time is returned. if the alarm was turned off,
	 * null is returned. If the alarm rang, the most recent alarm change
	 * preceding the alarm ring is used. If the alarm did not ring, the most
	 * recent alarm change preceding the end of night is used. If no alarm
	 * change matching the criteria can be found (due to later alarm changes
	 * overwriting the alarm change we want), null is returned. Note that null
	 * can be returned even if the alarm did ring.
	 * 
	 * @return a calendar object representing the set alarm time and date, or
	 *         null.
	 */
	public Calendar compute_alarm_set_time() {
		Calendar change_time; // When alarm was changed
		Calendar cutoff; // Latest alarm change time valid for night
		TimeChange latest_change; // Last alarm change that applies to night

		/*
		 * Don't try to compute an alarm set time for a record that doesn't have
		 * a valid start_of_night and end_of_night.
		 */
		if (start_of_night == null || end_of_night == null) {
			return null;
		}

		/*
		 * Compute the cutoff point after which an alarm change no longer
		 * applies to this night. We will return the latest alarm change
		 * preceding this point.
		 */
		if (alarm_ring[0] == null) {
			/*
			 * If the alarm did not ring, the end of night is the cutoff point.
			 */
			cutoff = end_of_night;
		} else {
			/*
			 * If the alarm did ring, the earliest alarm ring time is the cutoff
			 * point.
			 */
			cutoff = alarm_ring[0];
		}

		/*
		 * Find the most recent alarm change preceding the cutoff time computed
		 * above.
		 */
		latest_change = null;
		for (int i = 0; i < EVENTS_SAVED; i++) {
			/* Get time that alarm was changed for this alarm entry */
			change_time = alarm_change[i].getTimeAsCalendar();

			/*
			 * Any entry in the alarm_change list with a null change time, other
			 * than the first entry, is empty and is to be ignored. Once one
			 * empty entry is found we know the rest are empty too.
			 */
			if (change_time == null && i > 0) {
				break;
			}

			/*
			 * We only check alarm records that occurred before the "cutoff"
			 * time. The first alarm change startup record (which has a null
			 * "change time" value) is considered too since it must have been
			 * recorded before the start of the night.
			 */
			if (change_time == null || change_time.before(cutoff)) {
				/*
				 * Remember the alarm change record with the latest change time
				 * satisfying the cutoff time criteria. There are special cases
				 * when we have not stored any other alarm change record or when
				 * the alarm change we stored was the startup one with a null
				 * "change time".
				 */
				if (latest_change == null
						|| latest_change.getTimeAsCalendar() == null
						|| change_time.after(latest_change.getTimeAsCalendar())) {
					/* Save this entry as the latest change */
					latest_change = alarm_change[i];
				}
			}
		}

		if (latest_change == null) {
			/*
			 * Return null if we didn't find any alarm change preceding the
			 * cutoff time.
			 */
			return null;
		} else if (latest_change.getValueAsCalendar() == null) {
			/* Return null if the alarm was turned off. */
			return null;
		} else {
			/*
			 * If the alarm was turned on, take the time of day and convert it
			 * to a full date and time, using the date of this night of sleep.
			 * This adjusts for the fact that alarm change values are stored as
			 * relative times
			 */
			Calendar calendar = latest_change.getValueAsCalendar();

			/*
			 * Start by setting the date of the alarm to the date of the night's
			 * start time.
			 */
			calendar.set(Calendar.DAY_OF_MONTH,
					start_of_night.get(Calendar.DAY_OF_MONTH));
			calendar.set(Calendar.MONTH, start_of_night.get(Calendar.MONTH));
			calendar.set(Calendar.YEAR, start_of_night.get(Calendar.YEAR));

			/*
			 * If this date/time precedes the start of night, advance it by 1
			 * day to account for cases where the alarm was set for early on the
			 * next day. Note that we make an assumption here that a night of
			 * sleep is never longer than 24 hours.
			 */
			if (calendar.before(start_of_night)) {
				calendar.add(Calendar.DAY_OF_MONTH, 1);
			}

			return calendar;
		}
	}

	/**
	 * Return the rise time as a Calendar object. The rise time indicates when
	 * the user got up and is computed as "the time of day at the end of the
	 * last 5 minute block of sleep in the display hypnogram." If no sleep is
	 * present in the hypnogram, the result is null.
	 * 
	 * @return a calendar object representing the rise time.
	 */
	public Calendar compute_rise_time() {
		boolean found_sleep = false;
		int index;

		/*
		 * Don't try to compute a rise time for a record that doesn't have a
		 * valid hypnogram_start_time.
		 */
		if (hypnogram_start_time == null) {
			return null;
		}

		/*
		 * Find the index of the last bin of sleep in the display hypnogram, if
		 * any sleep is present.
		 */
		for (index = display_hypnogram_count - 1; index >= 0; index--) {
			if (SleepStage.convert(display_hypnogram[index]).is_sleep()) {
				found_sleep = true;
				break;
			}
		}

		/* If no sleep was present, return null. */
		if (!found_sleep) {
			return null;
		}

		/*
		 * Compute the time at the end of the last bin of sleep we detected. We
		 * determined the index of the last display hypnogram bin containing
		 * sleep. We add 1 to that index to get the time at the end of the bin.
		 * We offset the hypnogram start time by this number of display bins
		 * (which are each HYP_DISPLAY_STEP seconds long) to get the needed rise
		 * time.
		 */
		Calendar calendar = (Calendar) hypnogram_start_time.clone();
		calendar.add(Calendar.SECOND, HYP_DISPLAY_STEP * (index + 1));
		return calendar;
	}

	/**
	 * Determine display hypnogram bin value from a set of sleep stage values.
	 * 
	 * The value is typically based on the "mode" of a number of sleep stage
	 * values. Mode is defined as the most common value from a set of values.
	 * However, the result is undefined (STAGE_UNDEF) only if all the input
	 * values are undefined. If a single Wake value is present in the input, it
	 * overrides the mode and the output is Wake.
	 * 
	 * In determining the "mode", if more than one value is the most common, the
	 * first one is taken, in this order: Wake, REM, Light, Deep.
	 * 
	 * @param values
	 *            Array of sleep stage values to base the result on.
	 * @param num_values
	 *            How many values are in 'values' array.
	 * 
	 * @return the value for the display hypnogram bin.
	 */
	private SleepStage determine_bin_value(SleepStage[] values, int num_values) {
		/* Counts of values by stage */
		int[] counts = new int[SleepStage.values().length];
		int i;
		int max_count; // Highest count
		SleepStage result; // Mode value to return

		/* Count how many occurrences there are of each sleep stage */
		for (i = 0; i < num_values; i++) {
			counts[values[i].ordinal()]++;
		}

		/* If all the values are undefined then the result is undefined. */
		if (counts[SleepStage.UNDEFINED.ordinal()] == num_values) {
			return SleepStage.UNDEFINED;
		}

		/* If any of the values is Wake, the the result is SleepStage.WAKE. */
		if (counts[SleepStage.WAKE.ordinal()] != 0) {
			return SleepStage.WAKE;
		}

		/*
		 * Find first max count. We can skip STAGE_WAKE since it was already
		 * handled. Start by assuming result is STAGE_REM.
		 */
		max_count = counts[SleepStage.REM.ordinal()];
		result = SleepStage.REM;

		for (i = SleepStage.LIGHT.ordinal(); i < SleepStage.values().length; i++) {
			if (max_count < counts[i]) {
				max_count = counts[i];
				result = SleepStage.convert(i);
			}
		}
		return result;
	}

	/**
	 * Generate a human readable string containing an epoch value
	 * 
	 * Show the raw epochs and a translation to hour:minute.
	 * 
	 * @param epochs
	 *            The epoch value that is to be converted to a human readable
	 *            string
	 * 
	 * @return a String containing the converted value.
	 */
	private String epochs_to_human_string(int epochs) {
		StringWriter txt = new StringWriter();
		PrintWriter out = new PrintWriter(txt);

		int hours;
		int minutes;

		/*
		 * Use rounding to convert epochs to minutes to match the conversion the
		 * Zeo code uses.
		 */
		minutes = (epochs + 1) / 2;

		hours = minutes / 60;
		minutes = minutes % 60;

		out.printf("%2d:%02d (%4d epochs)", hours, minutes, epochs);
		return txt.toString();
	}

	/**
	 * Returns airplane mode flag.
	 * 
	 * @return the airplane mode flag
	 */
	public boolean get_airplane_mode() {
		return airplane_mode;
	}

	/**
	 * Returns the last time airplane mode was disengaged, or null if it has not
	 * been disengaged.
	 * 
	 * @return the last time airplane mode was disengaged
	 */
	public Calendar get_airplane_off() {
		return airplane_off;
	}

	/**
	 * Returns the last time airplane mode was engaged, or null if it has not
	 * been engaged.
	 * 
	 * @return the last time airplane mode was engaged
	 */
	public Calendar get_airplane_on() {
		return airplane_on;
	}

	/**
	 * Returns the list of alarm changes, as an array of TimeChange objects.
	 * 
	 * @return the list of alarm changes
	 */
	public TimeChange[] get_alarm_change() {
		return alarm_change;
	}

	/**
	 * Returns the time the alarm was turned off, or null if it was not turned
	 * off.
	 * 
	 * @return the time the alarm was turned off
	 */
	public Calendar get_alarm_off() {
		return alarm_off;
	}

	/**
	 * Returns reason alarm was triggered.
	 * 
	 * @return the reason alarm was triggered
	 */
	public AlarmReason get_alarm_reason() {
		return alarm_reason;
	}

	/**
	 * Returns a list of the first and last (if more than one) times the alarm
	 * rang.
	 * 
	 * @return the times the alarm rang
	 */
	public Calendar[] get_alarm_ring() {
		return alarm_ring;
	}

	/**
	 * Returns the time the alarm was set for.
	 * 
	 * @return the time the alarm was set for
	 */
	public Calendar get_alarm_set_time() {
		return alarm_set_time;
	}

	/**
	 * Returns a list of the times the snooze button was pressed
	 * 
	 * @return the times the snooze button was pressed
	 */
	public Calendar[] get_alarm_snooze() {
		return alarm_snooze;
	}

	/**
	 * Returns the name of the function where assert occurred, if an assert
	 * occurred and triggered a watchdog reset.
	 * 
	 * @return the name of the function where assert occurred
	 */
	public char[] get_assert_function_name() {
		return assert_function_name;
	}

	/**
	 * Returns the line number on which an assert occurred, if an assert
	 * occurred and triggered a watchdog reset.
	 * 
	 * @return the line number on which an assert occurred
	 */
	public int get_assert_line_number() {
		return assert_line_number;
	}

	/**
	 * Returns the number of awakenings for the current record.
	 * 
	 * @return the number of awakenings for the current record
	 */
	public int get_awakenings() {
		return awakenings;
	}

	/**
	 * Returns the average number of awakenings from sleep history.
	 * 
	 * @return the average number of awakenings from sleep history.
	 */
	public int get_awakenings_average() {
		return awakenings_average;
	}

	/**
	 * Returns the backlight brightness setting.
	 * 
	 * @return the backlight brightness setting
	 */
	public byte get_backlight() {
		return backlight;
	}

	/**
	 * Returns the base hypnogram as an array of integers.
	 * 
	 * @return the base hypnogram
	 */
	public byte[] get_base_hypnogram() {
		return base_hypnogram;
	}

	/**
	 * Returns the base_hypnogram_count value for the record.
	 * 
	 * @return the count value.
	 */
	public int get_base_hypnogram_count() {
		return base_hypnogram_count;
	}

	/**
	 * Returns the 12/24 hour clock selection.
	 * 
	 * @return the 12/24 hour clock selection
	 */
	public ClockMode get_clock_mode() {
		return clock_mode;
	}

	/**
	 * Returns the CRC16 value that was in the record.
	 * 
	 * @return the crc value read from the most recent record.
	 */
	public long get_crc() {
		return crc;
	}

	/**
	 * Returns the time that the record was output.
	 * 
	 * @return a calendar value that is the time the record was output.
	 */
	public Calendar get_current_time() {
		return current_time;
	}

	/**
	 * Returns the display hypnogram as an array of integers.
	 * 
	 * @return the display hypnogram
	 */
	public byte[] get_display_hypnogram() {
		return display_hypnogram;
	}

	/**
	 * Returns the display_hypnogram_count value for the record.
	 * 
	 * @return the count value.
	 */
	public int get_display_hypnogram_count() {
		return display_hypnogram_count;
	}

	/**
	 * Returns the index of the display hypnogram bin that was forced to a
	 * specific stage, or 0 if there is no such bin.
	 * 
	 * @return the index of the forced display hypnogram bin
	 */
	public int get_display_hypnogram_forced_index() {
		return display_hypnogram_forced_index;
	}

	/**
	 * Returns the index of the display hypnogram bin that was forced to a
	 * specific stage.
	 * 
	 * @return the stage of the forced display hypnogram bin
	 */
	public int get_display_hypnogram_forced_stage() {
		return display_hypnogram_forced_stage;
	}

	/**
	 * Returns the Zeo record's end of night which can be used for comparison
	 * purposes.
	 * 
	 * @return a calendar value that is the end of the record's night.
	 */
	public Calendar get_end_of_night() {
		return end_of_night;
	}

	/**
	 * Returns the last time a factory reset was performed, or null if it has
	 * not been performed.
	 * 
	 * @return the last time a factory reset was performed
	 */
	public Calendar get_factory_reset() {
		return factory_reset;
	}

	/**
	 * Returns the version of the sleep record data format.
	 * 
	 * @return the version of the sleep record data format
	 */
	public int get_format_version() {
		return format_version;
	}

	/**
	 * Returns the serial number of the most recently docked headband.
	 * 
	 * @return the serial number of the most recently docked headband
	 */
	public long get_headband_id() {
		return headband_id;
	}

	/**
	 * Returns an array of headband impedance values collected over the past 24
	 * hours.
	 * 
	 * @return an array of headband impedance values
	 */
	public short[] get_headband_impedance() {
		return headband_impedance;
	}

	/**
	 * Returns an array of headband packet loss values collected over the past
	 * 24 hours.
	 * 
	 * @return an array of headband packet loss values
	 */
	public short[] get_headband_packets() {
		return headband_packets;
	}

	/**
	 * Returns an array of headband received signal strength values collected
	 * over the past 24 hours.
	 * 
	 * @return an array of headband received signal strength values
	 */
	public byte[] get_headband_rssi() {
		return headband_rssi;
	}

	/**
	 * Returns an array of headband docking status values collected over the
	 * past 24 hours.
	 * 
	 * @return an array of headband docking status values
	 */
	public short[] get_headband_status() {
		return headband_status;
	}

	/**
	 * Returns the hypnogram start time.
	 * 
	 * @return the hypnogram start time
	 */
	public Calendar get_hypnogram_start_time() {
		return hypnogram_start_time;
	}

	/**
	 * Returns the Zeo hardware version.
	 * 
	 * @return the Zeo hardware version
	 */
	public int get_id_hw() {
		return id_hw;
	}

	/**
	 * Returns the Zeo software version.
	 * 
	 * @return the Zeo software version
	 */
	public int get_id_sw() {
		return id_sw;
	}

	/**
	 * Returns the time the user got up.
	 * 
	 * @return the time the user got up
	 */
	public Calendar get_rise_time() {
		return rise_time;
	}

	/**
	 * Returns the list of real-time clock changes, as an array of TimeChange
	 * objects.
	 * 
	 * @return the list of real-time clock changes
	 */
	public TimeChange[] get_rtc_change() {
		return rtc_change;
	}

	/**
	 * Returns the last time the sensor life counter was reset, or null if it
	 * has not been reset.
	 * 
	 * @return the last time the sensor life counter was reset
	 */
	public Calendar get_sensor_life_reset() {
		return sensor_life_reset;
	}

	/**
	 * Returns the sleep_date for the current record.
	 * 
	 * @return a calendar value indicating the sleep_date (see set_sleep_date).
	 */
	public Calendar get_sleep_date() {
		return sleep_date;
	}

	/**
	 * Returns the sleep rating for the current record.
	 * 
	 * @return the sleep rating for the current record
	 */
	public int get_sleep_rating() {
		return sleep_rating;
	}

	/**
	 * Returns the last time the sleep history was reset, or null if it has not
	 * been reset.
	 * 
	 * @return the last time the sleep history was reset
	 */
	public Calendar get_sleep_stat_reset() {
		return sleep_stat_reset;
	}

	/**
	 * Returns the sleep valid flag.
	 * 
	 * @return the sleep valid flag
	 */
	public boolean get_sleep_valid() {
		return sleep_valid;
	}

	/**
	 * Returns the snooze time setting in minutes.
	 * 
	 * @return the snooze time setting in minutes
	 */
	public int get_snooze_time() {
		return snooze_time;
	}

	/**
	 * Returns the Zeo record's start of night which can be used for comparison
	 * purposes.
	 * 
	 * @return a calendar value that is the start of the record's night.
	 */
	public Calendar get_start_of_night() {
		return start_of_night;
	}

	/**
	 * Returns the time in deep for the current record.
	 * 
	 * @return the time in deep for the current record
	 */
	public int get_time_in_deep() {
		return time_in_deep;
	}

	/**
	 * Returns the average time in deep from sleep history.
	 * 
	 * @return the average time in deep
	 */
	public int get_time_in_deep_average() {
		return time_in_deep_average;
	}

	/**
	 * Returns the best time in deep from sleep history.
	 * 
	 * @return the best time in deep
	 */
	public int get_time_in_deep_best() {
		return time_in_deep_best;
	}

	/**
	 * Returns the time in light for the current record.
	 * 
	 * @return the time in light for the current record
	 */
	public int get_time_in_light() {
		return time_in_light;
	}

	/**
	 * Returns the average time in light from sleep history.
	 * 
	 * @return the average time in light
	 */
	public int get_time_in_light_average() {
		return time_in_light_average;
	}

	/**
	 * Returns the time in rem for the current record.
	 * 
	 * @return the time in rem for the current record
	 */
	public int get_time_in_rem() {
		return time_in_rem;
	}

	/**
	 * Returns the average time in rem from sleep history.
	 * 
	 * @return the average time in rem
	 */
	public int get_time_in_rem_average() {
		return time_in_rem_average;
	}

	/**
	 * Returns the best time in rem from sleep history.
	 * 
	 * @return the best time in rem
	 */
	public int get_time_in_rem_best() {
		return time_in_rem_best;
	}

	/**
	 * Returns the time in wake for the current record.
	 * 
	 * @return the time in wake for the current record
	 */
	public int get_time_in_wake() {
		return time_in_wake;
	}

	/**
	 * Returns the average time in wake from sleep history.
	 * 
	 * @return the average time in wake
	 */
	public int get_time_in_wake_average() {
		return time_in_wake_average;
	}

	/**
	 * Returns the time to sleep for the current record.
	 * 
	 * @return the time to sleep for the current record
	 */
	public int get_time_to_z() {
		return time_to_z;
	}

	/**
	 * Returns the average time to sleep from sleep history.
	 * 
	 * @return the average time to sleep
	 */
	public int get_time_to_z_average() {
		return time_to_z_average;
	}

	/**
	 * Returns the total sleep time for the current record.
	 * 
	 * @return the total sleep time for the current record
	 */
	public int get_total_z() {
		return total_z;
	}

	/**
	 * Returns the average total sleep time from sleep history.
	 * 
	 * @return the average total sleep time
	 */
	public int get_total_z_average() {
		return total_z_average;
	}

	/**
	 * Returns the best total sleep time from sleep history.
	 * 
	 * @return the best total sleep time
	 */
	public int get_total_z_best() {
		return total_z_best;
	}

	/**
	 * Returns the selected wake tone.
	 * 
	 * @return the selected wake tone
	 */
	public WakeTone get_wake_tone() {
		return wake_tone;
	}

	/**
	 * Returns the selected wake window in minutes.
	 * 
	 * @return the selected wake window in minutes
	 */
	public int get_wake_window() {
		return wake_window;
	}

	/**
	 * Returns whether a watchdog reset occurred.
	 * 
	 * @return whether a watchdog reset occurred
	 */
	public boolean get_wdt_reset() {
		return wdt_reset;
	}

	/**
	 * Returns the reason this record was written to the card.
	 * 
	 * @return the reason this record was written to the card
	 */
	public WriteReason get_write_reason() {
		return write_reason;
	}

	/**
	 * Returns whether SmartWake was enabled.
	 * 
	 * @return whether SmartWake was enabled
	 */
	public boolean get_zeo_wake_on() {
		return zeo_wake_on;
	}

	/**
	 * Returns the ZQ score for the current record.
	 * 
	 * @return the ZQ score for the current record
	 */
	public int get_zq_score() {
		return zq_score;
	}

	/**
	 * Returns the average ZQ score from sleep history.
	 * 
	 * @return the average ZQ score
	 */
	public int get_zq_score_average() {
		return zq_score_average;
	}

	/**
	 * Returns the best ZQ score from sleep history.
	 * 
	 * @return the best ZQ score
	 */
	public int get_zq_score_best() {
		return zq_score_best;
	}

	/*
	 * Flag "reset" records. We require the record to signal a watchdog reset
	 * and be recorded with the "Card Insert" reason (which is used to record
	 * the power up record). We also require that the first RTC entry to be the
	 * startup RTC entry (as indicated by a "change time" value of 0) and that
	 * the startup time was less than FIRST_RECORD_TIMEOUT seconds before the
	 * time this record was recorded.
	 * 
	 * @return true if this record is first record after watchdog reset.
	 */
	public boolean is_reset_record() {
		long change_time; /* Time of first RTC change in seconds */
		long change_value; /* First RTC value in seconds */
		long current_timestamp; /* Current record time in seconds */

		change_time = rtc_change[0].getTime();
		change_value = rtc_change[0].getValue();
		current_timestamp = current_time.getTimeInMillis() / MSEC_PER_SECOND;

		if (wdt_reset && write_reason == WriteReason.FS_REASON_CARD_INSERT
				&& change_time == 0 && current_timestamp > change_value
				&& (current_timestamp - change_value) < FIRST_RECORD_TIMEOUT) {

			/*
			 * This record indicates that a watchdog reset occurred and was
			 * recorded within 10 seconds of power up.
			 */
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Tests if this record's write reason is FS_REASON_SLEEP_RATED.
	 * 
	 * @return true if write_reason equals FS_REASON_SLEEP_RATED.
	 */
	public boolean is_sleep_rating_record() {
		return write_reason == WriteReason.FS_REASON_SLEEP_RATED;
	}

	/**
	 * Compute the display hypnogram from the base hypnogram.
	 * 
	 * This function generates the entire display hypnogram. The only inputs
	 * required to generate the display hypnogram are the base hypnogram, and
	 * optionally the location and value of one forced stage.
	 * 
	 * As a side effect the array display_hypnogram will be modified by a call
	 * to this function.
	 */
	private void make_display_hypnogram_from_base() {
		SleepStage display_value = SleepStage.UNDEFINED;
		int i;
		int i_base;
		int i_display;
		int n_display;
		SleepStage stage;
		SleepStage[] stages = new SleepStage[HYP_BASE_PER_DISPLAY];

		/*
		 * Determine the number of display hypnogram bins to fill in. Only fill
		 * in bins for which all 10 base hypnograms are filled in, ignoring
		 * remaining base hypnogram elements.
		 */
		n_display = base_hypnogram_count / HYP_BASE_PER_DISPLAY;

		/* Fill in each display hypnogram bin */
		for (i_display = 0; i_display < n_display; i_display++) {
			/* Collect the base hypnogram values */
			i_base = i_display * HYP_BASE_PER_DISPLAY;
			for (i = 0; i < HYP_BASE_PER_DISPLAY; i++, i_base++) {
				stage = SleepStage.convert(base_hypnogram[i_base]);

				/* Convert all types of Deep sleep to the standard value */
				if (stage == SleepStage.DEEP_2) {
					stage = SleepStage.DEEP;
				}
				/* Store value for use in mode calculation */
				stages[i] = stage;
			}

			/* Store the next value in the Display hypnogram */
			display_value = determine_bin_value(stages, HYP_BASE_PER_DISPLAY);
			display_hypnogram[i_display] = (byte) display_value.ordinal();
		}

		/*
		 * If a display hypnogram bin was forced to a specific stage, set it
		 * here.
		 */
		if (display_hypnogram_forced_index > 0) {
			display_hypnogram[display_hypnogram_forced_index] = (byte) display_hypnogram_forced_stage;
		}

		/* The hypnogram size reflects all the data stored in it */
		display_hypnogram_count = i_display;
	}

	/**
	 * Read in an 8 bit signed integer and return the value as a char (which is
	 * signed).
	 * 
	 * @param in
	 *            The input stream to read the value from.
	 * 
	 * @return a value reflecting a signed 8 bit value from input file.
	 */
	private byte read_int8(ByteBuffer in) {
		return in.get();
	}

	/**
	 * Read in an 8 bit unsigned integer and return the value as a short to work
	 * around Java's lack of unsigned data types.
	 * 
	 * @param in
	 *            The input stream to read the value from.
	 * 
	 * @return a value reflecting an unsigned 8 bit value from input file.
	 */
	private short read_uint8(ByteBuffer in) {
		short value;

		value = (short) (in.get() & 0xff);
		return value;
	}

	/**
	 * Read in a 16 bit little endian unsigned integer and return the value as
	 * an int to work around Java's lack of unsigned data types.
	 * 
	 * @param in
	 *            The input stream to read the value from.
	 * 
	 * @return a value reflecting an unsigned 16 bit value from input file.
	 */
	private int read_uint16(ByteBuffer in) {
		int value;

		value = in.get() & 0xff;
		value |= (in.get() & 0xff) << 8;
		return value;
	}

	/**
	 * Read in a 32 bit little endian signed integer and return the value as an
	 * integer.
	 * 
	 * @param in
	 *            The input stream to read the value from.
	 * 
	 * @return a value reflecting a signed 32 bit value from input file.
	 */
	private int read_int32(ByteBuffer in) {
		int value;

		value = in.get() & 0xff;
		value |= (in.get() & 0xff) << 8;
		value |= (in.get() & 0xff) << 16;
		value |= (in.get() & 0xff) << 24;
		return value;
	}

	/**
	 * Read in a 32 bit little endian unsigned integer and return the value as a
	 * long to work around Java's lack of unsigned data types.
	 * 
	 * @param in
	 *            The input stream to read the value from.
	 * 
	 * @return a value reflecting an unsigned 32 bit value from input file.
	 */
	private long read_uint32(ByteBuffer in) {
		long value;

		value = in.get() & 0xff;
		value |= (in.get() & 0xff) << 8;
		value |= (in.get() & 0xff) << 16;
		value |= (in.get() & 0xff) << 24;
		return value;
	}

	/**
	 * Indicates whether or not the other_record is from the same "night" as
	 * this record. This is accomplished by examining the two nights sleep_date
	 * values to see if they are the same. The assumption is that the sleep_date
	 * values for the record have been set.
	 * 
	 * @return true if this night and the other_record night have the same
	 *         sleep_date (ie. are from the same day of sleep).
	 */
	public boolean same_night(ZeoData other_record) {
		if (sleep_date != null
				&& sleep_date.equals(other_record.get_sleep_date())) {
			return true;
		}
		return false;
	}

	/**
	 * Indicates whether other_record has the same start time as this record.
	 * 
	 * @return true if this night and other_record night have same start time.
	 */
	public boolean same_start_time(ZeoData other_record) {
		if (start_of_night != null
				&& start_of_night.equals(other_record.get_start_of_night())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Set the sleep_date for the current record. This determines the "day of
	 * sleep" for the record. Days of sleep start at 6am and run till the
	 * following day at 6am. Subtracting 6 hours off the start of night value,
	 * converting that to a calendar value and then forcing the time to 6am
	 * completes the conversion.
	 */
	public void set_sleep_date() {
		if (start_of_night == null) {
			sleep_date = null;
		} else {
			sleep_date = (Calendar) start_of_night.clone();

			/*
			 * Adjust time back 6 hours to align sleep days with normal days.
			 * This will set the proper date for the sleep day.
			 */
			sleep_date.add(Calendar.HOUR_OF_DAY, -6);

			/* Force the calendar hour to 6am. */
			sleep_date.set(Calendar.HOUR_OF_DAY, 6);
			sleep_date.set(Calendar.MINUTE, 0);
			sleep_date.set(Calendar.SECOND, 0);
		}
	}

	/**
	 * Given a unix_timestamp, return a Calendar object that corresponds to that
	 * timestamp.
	 * 
	 * @param unix_timestamp
	 *            A UNIX timestamp which is seconds since the epoch (ie. January
	 *            1st 1970 UTC).
	 * 
	 * @return a Calendar value that is equivalent to the given unix_timestamp.
	 */
	static Calendar timestamp_to_calendar(long unix_timestamp) {
		if (unix_timestamp == 0) {
			return null;
		} else {
			/* Convert to a Calendar object using UTC timezone */
			Calendar calendar = new GregorianCalendar(DEFAULT_TIMEZONE);
			calendar.setTimeInMillis(unix_timestamp * MSEC_PER_SECOND);
			return calendar;
		}
	}

	/**
	 * Generate a human readable representation of the Zeo record.
	 * 
	 * @return a string that contains a record in a human readable key-value
	 *         pair form.
	 */
	public String toHuman() {
		SleepRating sleep_rating_enum;

		StringWriter txt = new StringWriter();
		PrintWriter out = new PrintWriter(txt);

		/*
		 * Define an array used to convert sleep stage values to displayed sleep
		 * stages. We want to show a single letter for each of the sleep stages
		 * instead of just the numeric values.
		 */
		char[] hypnogram_chars = { '.', 'w', 'r', 'l', 'd', '?', 'L' };

		/* Convert numeric sleep rating to enumerated value */
		sleep_rating_enum = SleepRating.convert(sleep_rating);

		out.printf("record version= %d", format_version);
		out.println();
		out.printf("current_time  = %s", calendar_to_human_string(current_time));
		out.println();
		out.printf("crc           = %s", crc);
		out.println();
		out.printf("is_nap        = %s", is_nap);
		out.println();
		out.printf("sleep_date    = %s", calendar_to_human_string(sleep_date));
		out.println();
		out.printf("airplane_mode = %s", airplane_mode);
		out.println();
		out.printf("alarm_reason  = %s", alarm_reason);
		out.println();
		out.printf("backlight     = %s", backlight);
		out.println();
		out.printf("clock_mode    = %s", clock_mode);
		out.println();
		out.printf("sleep_valid   = %s", sleep_valid);
		out.println();
		out.printf("snooze_time   = %s", snooze_time);
		out.println();
		out.printf("wake_tone     = %s", wake_tone.ordinal());
		out.println();
		out.printf("wake_window   = %s", wake_window);
		out.println();
		out.printf("write_reason  = %s", write_reason);
		out.println();
		out.printf("zeo_wake_on   = %s", zeo_wake_on);
		out.println();
		out.printf("wdt_reset     = %s", wdt_reset);
		out.println();

		out.printf("airplane_off  = %s", calendar_to_human_string(airplane_off));
		out.println();
		out.printf("airplane_on   = %s", calendar_to_human_string(airplane_on));
		out.println();
		for (int i = 0; i < EVENTS_SAVED; i++) {
			out.printf("alarm_change  = %s", alarm_change[i]);
			out.println();
		}
		out.printf("assert_function_name = ");
		for (int i = 0; i < ASSERT_NAME_MAX; i++) {
			if (assert_function_name[i] == 0) {
				break;
			}
			out.printf("%c", assert_function_name[i]);
		}
		out.println();
		out.printf("assert_line_number   = %d", assert_line_number);
		out.println();
		out.printf("factory_reset = %s",
				calendar_to_human_string(factory_reset));
		out.println();
		out.printf("headband_id   = %s", headband_id);
		out.println();
		out.printf("headband_impedance = ");
		for (int i = 0; i < HEADBAND_IMPEDANCE_SIZE; i++) {
			out.printf("%3d ", headband_impedance[i]);
		}
		out.println();
		out.printf("headband_packets   = ");
		for (int i = 0; i < HEADBAND_PACKETS_SIZE; i++) {
			out.printf("%3d ", headband_packets[i]);
		}
		out.println();
		out.printf("headband_rssi      = ");
		for (int i = 0; i < HEADBAND_RSSI_SIZE; i++) {
			out.printf("%3d ", headband_rssi[i]);
		}
		out.println();
		out.printf("headband_status    = ");
		for (int i = 0; i < HEADBAND_STATUS_SIZE; i++) {
			/*
			 * Print the four values packed into each byte of status data. The
			 * packing order is defined in the fs_save_scalar function in the
			 * fs_save_data.c base source file.
			 */
			out.printf("%3d ", (headband_status[i] >> 0) & 0x3);
			out.printf("%3d ", (headband_status[i] >> 2) & 0x3);
			out.printf("%3d ", (headband_status[i] >> 4) & 0x3);
			out.printf("%3d ", (headband_status[i] >> 6) & 0x3);
		}
		out.println();

		out.printf("id_hw                 = %s", id_hw);
		out.println();
		out.printf("id_sw                 = %s", id_sw);
		out.println();
		for (int i = 0; i < EVENTS_SAVED; i++) {
			out.printf("rtc_change            = %s", rtc_change[i]);
			out.println();
		}
		out.printf("sensor_life_reset     = %s",
				calendar_to_human_string(sensor_life_reset));
		out.println();
		out.printf("sleep_stat_reset      = %s",
				calendar_to_human_string(sleep_stat_reset));
		out.println();

		/* Sleep Information */
		for (int i = 0; i < ALARM_EVENTS_SAVED; i++) {
			out.printf("alarm_ring            = %s",
					calendar_to_human_string(alarm_ring[i]));
			out.println();
		}
		for (int i = 0; i < SNOOZE_EVENTS_SAVED; i++) {
			out.printf("alarm_snooze          = %s",
					calendar_to_human_string(alarm_snooze[i]));
			out.println();
		}
		out.printf("alarm_off             = %s",
				calendar_to_human_string(alarm_off));
		out.println();
		out.printf("alarm_set_time        = %s",
				calendar_to_human_string(alarm_set_time));
		out.println();
		out.printf("awakenings            = %s", awakenings);
		out.println();
		out.printf("awakenings_average    = %s", awakenings_average);
		out.println();
		out.printf("start_of_night        = %s",
				calendar_to_human_string(start_of_night));
		out.println();
		out.printf("end_of_night          = %s",
				calendar_to_human_string(end_of_night));
		out.println();
		out.printf("rise_time             = %s",
				calendar_to_human_string(rise_time));
		out.println();
		out.printf("sleep_rating          = %s (%s)", sleep_rating,
				sleep_rating_enum);
		out.println();
		out.printf("time_in_deep          = %s",
				epochs_to_human_string(time_in_deep));
		out.println();
		out.printf("time_in_deep_average  = %s",
				epochs_to_human_string(time_in_deep_average));
		out.println();
		out.printf("time_in_deep_best     = %s",
				epochs_to_human_string(time_in_deep_best));
		out.println();
		out.printf("time_in_light         = %s",
				epochs_to_human_string(time_in_light));
		out.println();
		out.printf("time_in_light_average = %s",
				epochs_to_human_string(time_in_light_average));
		out.println();
		out.printf("time_in_rem           = %s",
				epochs_to_human_string(time_in_rem));
		out.println();
		out.printf("time_in_rem_average   = %s",
				epochs_to_human_string(time_in_rem_average));
		out.println();
		out.printf("time_in_rem_best      = %s",
				epochs_to_human_string(time_in_rem_best));
		out.println();
		out.printf("time_in_wake          = %s",
				epochs_to_human_string(time_in_wake));
		out.println();
		out.printf("time_in_wake_average  = %s",
				epochs_to_human_string(time_in_wake_average));
		out.println();
		out.printf("time_to_z             = %s",
				epochs_to_human_string(time_to_z));
		out.println();
		out.printf("time_to_z_average     = %s",
				epochs_to_human_string(time_to_z_average));
		out.println();
		out.printf("total_z               = %s",
				epochs_to_human_string(total_z));
		out.println();
		out.printf("total_z_average       = %s",
				epochs_to_human_string(total_z_average));
		out.println();
		out.printf("total_z_best          = %s",
				epochs_to_human_string(total_z_best));
		out.println();
		out.printf("zq_score              = %s", zq_score);
		out.println();
		out.printf("zq_score_average      = %s", zq_score_average);
		out.println();
		out.printf("zq_score_best         = %s", zq_score_best);
		out.println();

		/*
		 * Print display hypnogram forced stage and the index in the hypnogram
		 * where the stage is forced.
		 */
		out.printf("display_hypnogram_forced_index = %s",
				display_hypnogram_forced_index);
		out.println();
		out.printf("display_hypnogram_forced_stage = %s",
				display_hypnogram_forced_stage);
		out.println();

		/* Print the starting timestamp for the hypnograms. */
		out.printf("hypnogram_start_time  = %s",
				calendar_to_human_string(hypnogram_start_time));
		out.println();

		out.printf("base_hypnogram_count  = %s", base_hypnogram_count);
		out.println();
		out.write("base_hypnogram = ");
		for (int i = 0; i < base_hypnogram_count; i++) {
			out.printf("%c", hypnogram_chars[base_hypnogram[i]]);
		}
		out.println();

		out.printf("display_hypnogram_count = %s", display_hypnogram_count);
		out.println();
		out.write("display hypnogram = ");
		for (int i = 0; i < display_hypnogram_count; i++) {
			out.printf("%c", hypnogram_chars[display_hypnogram[i]]);
		}
		out.println();

		/* Add an extra blank line between records */
		out.println();
		out.flush();

		return txt.toString();
	}

	/**
	 * Generate a XML representation of the Zeo record.
	 * 
	 * @return a string that contains the XML representation of the Zeo record.
	 */
	public String toXML() {
		StringWriter txt = new StringWriter();
		PrintWriter out = new PrintWriter(txt);

		out.printf("<sleep_record version=\"%d\">", format_version);
		out.println();
		out.println("<current_time>");
		out.write(calendar_to_xml(current_time));
		out.println("</current_time>");
		out.printf("<crc>%s</crc>", crc);
		out.println();
		out.printf("<is_nap>%s</is_nap>", is_nap ? 1 : 0);
		out.println();
		out.println("<sleep_date>");
		out.write(calendar_to_xml(sleep_date));
		out.println("</sleep_date>");

		/* Device History */
		out.println("<device_history>");
		/* Packed values. */
		out.printf("<airplane_mode>%s</airplane_mode>", airplane_mode ? 1 : 0);
		out.println();
		out.printf("<alarm_reason>%s</alarm_reason>", alarm_reason.ordinal());
		out.println();
		out.printf("<backlight>%s</backlight>", backlight);
		out.println();
		out.printf("<clock_mode>%s</clock_mode>", clock_mode.ordinal());
		out.println();
		out.printf("<sleep_valid>%s</sleep_valid>", sleep_valid);
		out.println();
		out.printf("<snooze_time>%s</snooze_time>", snooze_time);
		out.println();
		out.printf("<wake_tone>%s</wake_tone>", wake_tone.ordinal());
		out.println();
		out.printf("<wake_window>%s</wake_window>", wake_window);
		out.println();
		out.printf("<write_reason>%s</write_reason>", write_reason.ordinal());
		out.println();
		out.printf("<zeo_wake_on>%s</zeo_wake_on>", zeo_wake_on ? 1 : 0);
		out.println();
		out.printf("<wdt_reset>%s</wdt_reset>", wdt_reset ? 1 : 0);
		out.println();

		out.println("<airplane_off>");
		out.write(calendar_to_xml(airplane_off));
		out.println("</airplane_off>");
		out.println("<airplane_on>");
		out.write(calendar_to_xml(airplane_on));
		out.println("</airplane_on>");
		out.println("<alarm_change>");
		for (int i = 0; i < EVENTS_SAVED; i++) {
			out.write(alarm_change[i].toXML());
		}
		out.println("</alarm_change>");
		out.println("<assert_function_name>");
		for (int i = 0; i < ASSERT_NAME_MAX; i++) {
			if (assert_function_name[i] == 0) {
				break;
			}
			out.printf("%c", assert_function_name[i]);
		}
		out.println("</assert_function_name>");
		out.printf("<assert_line_number>%d</assert_line_number>",
				assert_line_number);
		out.println();
		out.println("<factory_reset>");
		out.write(calendar_to_xml(factory_reset));
		out.println("</factory_reset>");
		out.printf("<headband_id>%d</headband_id>", headband_id);
		out.println();
		out.println("<headband_impedance>");
		for (int i = 0; i < HEADBAND_IMPEDANCE_SIZE; i++) {
			out.printf("%d ", headband_impedance[i]);
		}
		out.println();
		out.println("</headband_impedance>");
		out.println("<headband_packets>");
		for (int i = 0; i < HEADBAND_PACKETS_SIZE; i++) {
			out.printf("%d ", headband_packets[i]);
		}
		out.println();
		out.println("</headband_packets>");
		out.println("<headband_rssi>");
		for (int i = 0; i < HEADBAND_RSSI_SIZE; i++) {
			out.printf("%d ", headband_rssi[i]);
		}
		out.println();
		out.println("</headband_rssi>");
		out.println("<headband_status>");
		for (int i = 0; i < HEADBAND_STATUS_SIZE; i++) {
			/*
			 * Print the four values packed into each byte of status data. The
			 * packing order is defined in the fs_save_scalar function in the
			 * fs_save_data.c base source file.
			 */
			out.printf("%d ", (headband_status[i] >> 0) & 0x3);
			out.printf("%d ", (headband_status[i] >> 2) & 0x3);
			out.printf("%d ", (headband_status[i] >> 4) & 0x3);
			out.printf("%d ", (headband_status[i] >> 6) & 0x3);
		}
		out.println();
		out.println("</headband_status>");
		out.printf("<id_hw>%s</id_hw>", id_hw);
		out.println();
		out.printf("<id_sw>%s</id_sw>", id_sw);
		out.println();
		out.println("<rtc_change>");
		for (int i = 0; i < EVENTS_SAVED; i++) {
			out.write(rtc_change[i].toXML());
		}
		out.println("</rtc_change>");
		out.println("<sensor_life_reset>");
		out.write(calendar_to_xml(sensor_life_reset));
		out.println("</sensor_life_reset>");
		out.println("<sleep_stat_reset>");
		out.write(calendar_to_xml(sleep_stat_reset));
		out.println("</sleep_stat_reset>");

		out.println("</device_history>");

		/* Sleep Information */
		out.println("<sleep_information>");
		out.println("<alarm_ring>");
		for (int i = 0; i < ALARM_EVENTS_SAVED; i++) {
			out.println("<ring>");
			out.write(calendar_to_xml(alarm_ring[i]));
			out.println("</ring>");
		}
		out.println("</alarm_ring>");
		out.println("<alarm_snooze>");
		for (int i = 0; i < SNOOZE_EVENTS_SAVED; i++) {
			out.println("<snooze>");
			out.write(calendar_to_xml(alarm_snooze[i]));
			out.println("</snooze>");
		}
		out.println("</alarm_snooze>");
		out.println("<alarm_off>");
		out.write(calendar_to_xml(alarm_off));
		out.println("</alarm_off>");
		out.println("<alarm_set_time>");
		out.write(calendar_to_xml(alarm_set_time));
		out.println("</alarm_set_time>");
		out.printf("<awakenings>%s</awakenings>", awakenings);
		out.println();
		out.printf("<awakenings_average>%s</awakenings_average>",
				awakenings_average);
		out.println();
		out.println("<end_of_night>");
		out.write(calendar_to_xml(end_of_night));
		out.println("</end_of_night>");
		out.println("<rise_time>");
		out.write(calendar_to_xml(rise_time));
		out.println("</rise_time>");
		out.println("<start_of_night>");
		out.write(calendar_to_xml(start_of_night));
		out.println("</start_of_night>");
		out.printf("<sleep_rating>%s</sleep_rating>", sleep_rating);
		out.println();
		out.printf("<time_in_deep>%s</time_in_deep>", time_in_deep);
		out.println();
		out.printf("<time_in_deep_average>%s</time_in_deep_average>",
				time_in_deep_average);
		out.println();
		out.printf("<time_in_deep_best>%s</time_in_deep_best>",
				time_in_deep_best);
		out.println();
		out.printf("<time_in_light>%s</time_in_light>", time_in_light);
		out.println();
		out.printf("<time_in_light_average>%s</time_in_light_average>",
				time_in_light_average);
		out.println();
		out.printf("<time_in_rem>%s</time_in_rem>", time_in_rem);
		out.println();
		out.printf("<time_in_rem_average>%s</time_in_rem_average>",
				time_in_rem_average);
		out.println();
		out.printf("<time_in_rem_best>%s</time_in_rem_best>", time_in_rem_best);
		out.println();
		out.printf("<time_in_wake>%s</time_in_wake>", time_in_wake);
		out.println();
		out.printf("<time_in_wake_average>%s</time_in_wake_average>",
				time_in_wake_average);
		out.println();
		out.printf("<time_to_z>%s</time_to_z>", time_to_z);
		out.println();
		out.printf("<time_to_z_average>%s</time_to_z_average>",
				time_to_z_average);
		out.println();
		out.printf("<total_z>%s</total_z>", total_z);
		out.println();
		out.printf("<total_z_average>%s</total_z_average>", total_z_average);
		out.println();
		out.printf("<total_z_best>%s</total_z_best>", total_z_best);
		out.println();
		out.printf("<zq_score>%s</zq_score>", zq_score);
		out.println();
		out.printf("<zq_score_average>%s</zq_score_average>", zq_score_average);
		out.println();
		out.printf("<zq_score_best>%s</zq_score_best>", zq_score_best);
		out.println();

		/*
		 * Print XMLized version of the forced index and stage for the display
		 * hypnogram.
		 */
		out.printf("<display_hypnogram_forced_index>" + "%s"
				+ "</display_hypnogram_forced_index>",
				display_hypnogram_forced_index);
		out.println();
		out.printf("<display_hypnogram_forced_stage>" + "%s"
				+ "</display_hypnogram_forced_stage>",
				display_hypnogram_forced_stage);
		out.println();

		/* Print the XMLized version of the hypnogram starting time. */
		out.println("<hypnogram_start_time>");
		out.write(calendar_to_xml(hypnogram_start_time));
		out.println("</hypnogram_start_time>");

		out.printf("<display_hypnogram_count>%s</display_hypnogram_count>",
				display_hypnogram_count);
		out.println();
		out.println("<display_hypnogram>");
		for (int i = 0; i < display_hypnogram_count; i++) {
			out.printf("%d ", display_hypnogram[i]);
		}
		out.println();
		out.println("</display_hypnogram>");
		out.printf("<base_hypnogram_count>%s</base_hypnogram_count>",
				base_hypnogram_count);
		out.println();
		out.println("<base_hypnogram>");
		for (int i = 0; i < base_hypnogram_count; i++) {
			out.printf("%d ", base_hypnogram[i]);
		}
		out.println();
		out.println("</base_hypnogram>");

		out.println("</sleep_information>");
		out.println("</sleep_record>");
		out.flush();

		return txt.toString();
	}
}
