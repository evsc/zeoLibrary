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
import java.util.Calendar;

/**
 * Representation of when a time change occurred and what the new value of the
 * time was.
 * 
 * The clock or alarm time can be changed on the device. This class attempts to
 * capture those time changes through saving the time that the event occurred
 * along with the new time value.
 */
public class TimeChange {
	private Calendar change_time; /* Time when value changed */
	private long change_timestamp; /* Timestamp when value changed */
	private Calendar value; /* New value time was set to */
	private long value_timestamp; /* Timestamp time was set to */

	/**
	 * Construct a time change value from the time the change occurred and with
	 * the new value.
	 * 
	 * @param change_parm
	 *            A UNIX timestamp that represents when the time change
	 *            occurred.
	 * @param value_parm
	 *            The new value for the clock.
	 */
	public TimeChange(long change_parm, long value_parm) {
		change_timestamp = change_parm;
		value_timestamp = value_parm;

		change_time = ZeoData.timestamp_to_calendar(change_timestamp);
		value = ZeoData.timestamp_to_calendar(value_timestamp);
	}

	/**
	 * Returns the timestamp for when value was changed (in seconds).
	 * 
	 * @return the timestamp for when value was changed (in seconds).
	 */
	public long getTime() {
		return change_timestamp;
	}

	/**
	 * Returns the time for when value was changed (as a Calendar), or null.
	 * 
	 * @return the time for when value was changed (as a Calendar), or null.
	 */
	public Calendar getTimeAsCalendar() {
		if (change_time == null) {
			return null;
		} else {
			return (Calendar) change_time.clone();
		}
	}

	/**
	 * Returns the value time was changed to (in seconds).
	 * 
	 * @return the value time was changed to (in seconds).
	 */
	public long getValue() {
		return value_timestamp;
	}

	/**
	 * Returns the value time was changed to (as a Calendar), or null.
	 * 
	 * @return the value time was changed to (as a Calendar), or null.
	 */
	public Calendar getValueAsCalendar() {
		if (value == null) {
			return null;
		} else {
			return (Calendar) value.clone();
		}
	}

	/**
	 * Generates a string representation of the time change.
	 * 
	 * @return a string representation of the time change.
	 */
	public String toString() {
		return "value: " + ZeoData.calendar_to_human_string(value)
				+ " changed: " + ZeoData.calendar_to_human_string(change_time);
	}

	/**
	 * Generates an XML string representation of the time change.
	 * 
	 * @return an XML string that captures the time change.
	 */
	public String toXML() {
		StringWriter txt = new StringWriter();
		PrintWriter out = new PrintWriter(txt);

		out.println("<change_time>");
		out.println("<new_value>");
		out.write(ZeoData.calendar_to_xml(value));
		out.println("</new_value>");
		out.println("<time_changed>");
		out.write(ZeoData.calendar_to_xml(change_time));
		out.println("</time_changed>");
		out.println("</change_time>");
		return txt.toString();
	}
}
