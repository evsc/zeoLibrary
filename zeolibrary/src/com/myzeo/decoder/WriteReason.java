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
 * Specifies the reason this record was written to the SD card.
 */
public enum WriteReason {
	/**
	 * Sleep algorithm transitioned from active mode to tentative idle mode.
	 */
	FS_REASON_TENTATIVE_NIGHT_END,

	/**
	 * Sleep algorithm transitioned from tentative idle mode to idle mode.
	 */
	FS_REASON_NIGHT_END,

	/**
	 * A ringing alarm was turned off, either by the user or because it timed
	 * out.
	 */
	FS_REASON_ALARM_OFF,

	/**
	 * Unit was powered on with SD card inserted, or SD card was inserted in a
	 * unit that was already powered on.
	 */
	FS_REASON_CARD_INSERT,

	/**
	 * Record is written out at noon every day.
	 */
	FS_REASON_24_HOUR_UPDATE,

	/**
	 * When the user enters a 'morning feel' sleep rating on the bedside
	 * display, a write is triggered.
	 */
	FS_REASON_SLEEP_RATED;

	public static WriteReason convert(int i) {
		return values()[i];
	}
}
