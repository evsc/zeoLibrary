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

/**
 * Indicates the reason for the most recent alarm ringing.
 * <p>
 * REM_TO_NREM_TRANSITION, NREM_TO_REM_TRANSITION, WAKE_ON_WAKE, and DEEP_RISING
 * only result from the SmartWake function. END_OF_WAKE_WINDOW results from
 * either reaching the end of the wake window in SmartWake or reaching the alarm
 * time in standard wake. NO_ALARM indicates that the alarm has not rung since
 * the alarm reason was cleared.
 * <p>
 * The alarm reason is cleared to NO_ALARM on power-on, and is cleared to
 * NO_ALARM when a new night of sleep begins.
 */
public enum AlarmReason {
	REM_TO_NREM_TRANSITION, NREM_TO_REM_TRANSITION, WAKE_ON_WAKE, DEEP_RISING, END_OF_WAKE_WINDOW, NO_ALARM;

	public static AlarmReason convert(int i) {
		return values()[i];
	}
}
