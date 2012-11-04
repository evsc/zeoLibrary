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
import processing.serial.*;
import java.lang.reflect.*;
import java.util.Date;

import src.zeo.library.ZeoSlice;


/**
 * The ZeoStream class reads and parses the serial data 
 * coming from the Zeo Sleep Manager.
 * 
 * 
 * @author evsc
 * 
 */

public class ZeoStream implements Runnable {
	
	private PApplet myParent;
	
	private boolean running = false;	
	private Thread t;
	
	/**
	 * frequency for serial port reading, in milliseconds
	 * default: 100ms
	 */
	public int updateRate = 100;
	
	public static final String VERSION = "##library.prettyVersion##";
	
	/**
	 * Serial port
	 */
	public Serial myPort;
	
	/**
	 * Baud Rate, default 38400
	 */
	public int baud = 38400;
	
	/** 
	 * Buffer for incoming data
	 */
	private byte[] inBuffer;
	
	/**
	 * Current timestamp
	 */
	private long _timestamp;	
	
	/**
	 * Version of Zeo protocol
	 */
	private long _version;
	
	/**
	 * tmp slice, to be filled directly from serial data
	 */
	private ZeoSlice _slice;
	
	/**
	 * current ZeoSlice, accessible from sketch
	 */
	public ZeoSlice slice;
	
	public boolean debug = true;
	
	/**
	 * Current sleep state
	 * 
	 * 0 ... undefined
	 * 1 ... awake
	 * 2 ... REM
	 * 3 ... light sleep
	 * 4 ... deep sleep
	 */
	public int sleepState;
	
	Method zeoSliceEventMethod;
	Method zeoSleepStateEventMethod;
	
	/**
	 * ZeoStream Constructor with Serial Port definition, 
	 * usually called in the setup() method of your sketch 
	 * to initialize library and enable serial communication.
	 * 
	 * @param theParent
	 */
	public ZeoStream(PApplet theParent, String portName) {
		myParent = theParent;
		myParent.registerDispose(this);
		startSerial(portName);
		welcome();
		
		inBuffer = new byte[0];
		
		_slice = new ZeoSlice();
		slice = new ZeoSlice();
		
		// check to see if host applet implements
		// public void zeoSliceEvent(ZeoStream z)
		try {
			zeoSliceEventMethod =
				myParent.getClass().getMethod("zeoSliceEvent", 
						new Class[] { ZeoStream.class } );
		} catch (Exception e) {
	    	System.err.println("zeoSliceEvent() method not defined. ");
	    }
		
		try {
			zeoSleepStateEventMethod =
				myParent.getClass().getMethod("zeoSleepStateEvent", 
						new Class[] { ZeoStream.class } );
		} catch (Exception e) {
	    	System.err.println("zeoSleepStateEvent() method not defined. ");
	    }
	}
	
	/**
	 * Setup serial communication
	 * 
	 * @param port
	 * the number of the serial port
	 */
	private void startSerial(String portName) {
		myPort = new Serial(myParent, portName, baud);
	}
	
	/**
	 * function to start thread that continuously 
	 * tries to read data from the serial port
	 * (thread because serialEvent doesn't work inside library)
	 */
	public void start() {
		running = true;
		t = new Thread(this);
		t.start();
	}
	
	/**
	 * Run function, executes serialRead according to updateRate frequency.
	 * default: 100ms delay between actions
	 */
	public void run() {
		while(running) {
			readSerial();
			myParent.delay(updateRate);
		}
	}
	
	/**
	 * stops execution of thread
	 */
	public void quit() {
		running = false;
		t= null;
	}
	
	/**
	 * dispose function, simpy calls quit()
	 */
	public void dispose() {
		quit();
	}
	
	/**
	 * Trigger Event when complete slice of data has been received
	 */
	public void triggerZeoSliceEvent() {
	    if (zeoSliceEventMethod != null) {
		    try {
		    	zeoSliceEventMethod.invoke(myParent, new Object[] { this });
		    } catch (Exception e) {
		      System.err.println("Disabling zeoSliceEvent() because of an error.");
		      e.printStackTrace();
		      zeoSliceEventMethod = null;
		    }
	    }
	}
	
	/**
	 * Trigger Event when new sleepstate has been received
	 */
	public void triggerZeoSleepStateEvent() {
	    if (zeoSleepStateEventMethod != null) {
		    try {
		    	zeoSleepStateEventMethod.invoke(myParent, new Object[] { this });
		    } catch (Exception e) {
		      System.err.println("Disabling zeoSleepStateEvent() because of an error.");
		      e.printStackTrace();
		      zeoSleepStateEventMethod = null;
		    }
	    }
	}
	
	/**
	 * readSerial, called by run() function at updateRate frequency
	 * checks serial port for new data, default: every 100ms
	 */
	private void readSerial() {
		while(myPort.available() > 0) {
			byte[] newBuffer = new byte[512];
			int n = myPort.readBytes(newBuffer);
			newBuffer = PApplet.subset(newBuffer,0,n);
			if(debug) System.out.println("readSerial() reads in "+n+" bytes");
			if(newBuffer != null) {
				inBuffer = PApplet.concat(inBuffer, newBuffer);
				if(inBuffer.length > 0) checkBuffer();
			}
		}
	}
	
	/**
	 * checkInput, searches input buffer for start characters
	 * and passes on packets to parsing,
	 * cleans out buffer only if parsing worked
	 */
	
	private void checkBuffer() {
		int startpacket = 0;
		for(int i=1; i<inBuffer.length-1; i++) {
		  if(inBuffer[i] == (byte) 'A' && inBuffer[i+1] == (byte) '4') {
		      byte[] onePart = PApplet.subset(inBuffer, startpacket, i-startpacket);
		      parseInput(onePart);
		      startpacket = i;
		   }
		}
		byte[] lastPart = PApplet.subset(inBuffer, startpacket, inBuffer.length-startpacket);
		boolean deletelastbuffer = parseInput(lastPart);
		if(deletelastbuffer) inBuffer = new byte[0];
		else inBuffer = PApplet.subset(lastPart,0);
	}
	
	
	/**
	 * try to parse package according to serial protocol
	 * 
	 * @param input
	 * @return true if data is valid or data is useless, 
	 * false if data seems valid yet is incomplete
	 */
	private boolean parseInput(byte[] input) {
		int p = 2;	// pointer to first relevant character in input array
		// at least 13 bytes
		if(input.length > 12 && input.length < 500) {
			if(!(input[0] == (byte) 'A' && input[1] == (byte) '4')) return true;
			
			int checkSum = getByte(input[p]);
			if(debug) System.out.println("checkSum:\t\t"+checkSum);
			
			int dataLength = getByte(input[p+1]) + ( getByte(input[p+2]) << 8 );
			if(debug) System.out.println("dataLength:\t"+dataLength); 
			
			// ignore weird packets (usually 1st)
			if(dataLength > 1000) {
				if(debug) System.out.println();
				return true;	// return to clean out buffer
			}
			
			int invDataLength = getByte(input[p+3]) + ( getByte(input[p+4]) << 8 );
		    int check = ~invDataLength & 0xff;
		    // TODO inverse check only works for 1-byte numbers  
			
		    int timestampLow = getByte(input[p+5]);
		    int timestampSubseconds = getByte(input[p+6]) + ( getByte(input[p+7]) << 8 );
		    float subseconds = timestampSubseconds / 65535.0f;
		    if(debug) System.out.println("timestamp:\t\t"+timestampLow+ " + "+ PApplet.nf(subseconds,1,2));
		    
		    int sequenceNo = getByte(input[p+8]);
		    if(debug) System.out.println("sequenceNo:\t"+sequenceNo);
		
		    byte[] data; 
		    try {
		      data = PApplet.subset(input, p+9, dataLength);
		    } catch (Exception e) {
		      if(debug) System.out.println("ERROR: array not long enough for dataLength variable");
		      return false;
		    }
		    
		    int dataType = getByte(data[0]);
		    String dataTypeStr = getDataType(dataType);
		    if(debug) System.out.println("dataType:\t\t"+dataType+ " ("+dataTypeStr+")");
		    
		    int sum = 0;
		    for(int i=0; i<data.length; i++) {
		      sum+= getByte(data[i]);
		    }
		    if(debug) System.out.print("sum:\t\t");
		    if((sum%256) == checkSum) {
		    	if(debug) System.out.println("VALID");
		    } else {
		    	if(debug) System.out.println("SUM ERROR");
		    	return true;	// return to clear out buffer
		    }
		    
		    if(debug) System.out.print("data:\t\t");
		    for(int i=0; i<data.length; i++) {
		      if(debug) System.out.print(getByte(data[i])+" ");
		    }
		    if(debug) System.out.println();
		    
		    if(dataTypeStr == "ZeoTimestamp") {
		    	_timestamp = getByte(data[1]) + (getByte(data[2]) << 8) + (getByte(data[3]) << 16) + (getByte(data[4]) << 24);
		    	if(debug) System.out.println("_timestamp:\t"+_timestamp);
		    }
		    
		    if(dataTypeStr == "Version") {
		        _version = getByte(data[1]) + (getByte(data[2]) << 8) + (getByte(data[3]) << 16) + (getByte(data[4]) << 24);
		        if(debug) System.out.println("_version:\t"+_version);
		    }
		    
		 // skip packet until version and timestamps arrive
		    if(_timestamp == 0 || _version == 0) {
		        if(debug) System.out.println();
		        return true;	// return and clear buffer  
		    }
		    
		 // construct full timestamp
		    long timestamp = 0;
		    if((_timestamp & 0xff) == timestampLow) timestamp = _timestamp;
		    else if(((_timestamp -1) & 0xff) == timestampLow) timestamp = _timestamp - 1;
		    else if(((_timestamp +1) & 0xff) == timestampLow) timestamp = _timestamp + 1;
		    else timestamp = _timestamp;
		    
		    
		    if(debug) {
		    	Date ts = new Date(timestamp);
		    	System.out.println("date:\t\t"+ts);
		    }
		    
		    // pass on data
		    _slice.setTime(timestamp);

		    if(dataTypeStr == "FrequencyBins") {
		      _slice.setBins(data);
		    }
		    
		    if(dataTypeStr == "SleepStage") {
		    	sleepState = getByte(data[1]) + (getByte(data[2]) << 8) + (getByte(data[3]) << 16) + (getByte(data[4]) << 24);
		        if(debug) System.out.println("sleepstage:\t"+sleepState);
		        triggerZeoSleepStateEvent();
		    }
		    
		    _slice.setSleepState(sleepState);
		    
		    if(dataTypeStr == "Waveform") {
		        _slice.setWaveForm(data);
		    }
		    
		    if(dataTypeStr == "Impedance") {
		    	_slice.impedance = (long) getByte(data[1]) + (getByte(data[2]) << 8) + (getByte(data[3]) << 16) + (getByte(data[4]) << 24);
		    }
		    
		    if(dataTypeStr == "BadSignal") {
		    	_slice.badSignal = (long) getByte(data[1]) + (getByte(data[2]) << 8) + (getByte(data[3]) << 16) + (getByte(data[4]) << 24);
		    }
		    
		    if(dataTypeStr == "SQI") {
		    	_slice.SQI = (long) getByte(data[1]) + (getByte(data[2]) << 8) + (getByte(data[3]) << 16) + (getByte(data[4]) << 24);
		    }
		    
		    if(dataTypeStr == "SliceEnd") {
		        // set public slice to tmp slice
		        slice = _slice;
		        // empty _slice
		        _slice = new ZeoSlice();
		        
		        triggerZeoSliceEvent();
		    }
		      
		    if(debug) System.out.println();  
		    return true;
		} else return false; // return and keep buffer, because not long enough
		    
	}
	
	private int getByte(byte b) {
	  int v = (int) b;
	  if(v<0) v+=256;
	  return v;
	}
	

	
	/**
	 * map event identifier number to event name
	 * 
	 * @param t
	 * @return String of event name
	 */
	private String getDataType(int t) {
	  switch(t) {
	    case 0x00:  return "Event";
	    case 0x02:  return "SliceEnd";
	    case 0x03:  return "Version";
	    case 0x80:  return "Waveform";
	    case 0x83:  return "FrequencyBins";
	    case 0x84:  return "SQI";
	    case 0x8A:  return "ZeoTimestamp";
	    case 0x97:  return "Impedance";
	    case 0x9C:  return "BadSignal";
	    case 0x9D:  return "SleepStage";
	    default:    return "-";
	  }
	}
	
	/**
	 * map frequency bin number to name
	 * 
	 * @param t number of frequency bin (0-6)
	 * @return wave name and frequency range
	 */
	public String nameFrequencyBin(int t) {
	  switch(t) {
	    case 0x00:  return "Delta (2-4)";
	    case 0x01:  return "Theta (4-8)";
	    case 0x02:  return "Alpha (8-13)";
	    case 0x03:  return "Beta1 (13-18)";
	    case 0x04:  return "Beta2 (18-21)";
	    case 0x05:  return "Beta3 (11-14)";
	    case 0x06:  return "Gamma (30-50)";
	    default:    return "-";
	  }
	}
	
	
	private void welcome() {
		System.out.println("##library.name## ##library.prettyVersion## by ##author##");
		System.out.println("-------------------------------------------------------\n");
	}

}
