package src.zeo.library;

/**
 * ZeoSlice class,
 * represents data received from Zeo Sleep Device
 * within ~1 second: waveForm, frequencyBin, sleepState,
 * impedance, SQI, badData, timestamp
 * 
 * @author evsc 
 * 
 */

public class ZeoSlice {

//	private PApplet myParent;
//	private ZeoStream zs;
	
	/**
	 * raw waveform data, needs 60Hz filtering
	 * array with 256 float values
	 */
	public float[] waveForm;
	
	/**
	 * array with values for 7 frequency bins:
	 * 
	 * 0: delta (2-4Hz)
	 * 1: theta (4-8Hz)
	 * 2: alpha (8-13Hz)
	 * 3: beta1 (13-18Hz)
	 * 4: beta2 (18-21Hz)
	 * 5: beta3 (11-14Hz, sleep spindels)
	 * 6: gamma (30-50Hz)
	 */
	public float[] frequencyBin;
	
	/**
	 * represents most recent sleep state:
	 * 
	 *	- 0 - undefined
	 *	- 1 - Wake
	 *	- 2 - REM
	 *	- 3 - Light
	 *	- 4 - Deep
	 */
	public int sleepState;
	
	/**
	 * Zeo headband impedance value
	 */
	public long impedance;
	
	/**
	 * 
	 */
	public long SQI;
	
	/**
	 * 
	 */
	public long badSignal;
	
	/**
	 * Zeo unix time (1970?)
	 */
	public long timestamp;
	
	
	/**
	 * Constructor
	 * 
	 */
	public ZeoSlice() {
		frequencyBin = new float[7];
		for(int i=0; i<7; i++) frequencyBin[i] = 0.0f;
		
		waveForm = new float[256];
		for(int i=0; i<256; i++) waveForm[i] = 0.0f;
		
	}
	
	public void setTime(long t) {
		timestamp = t;
	}
	
	public void setBins(byte[] data) {
		for(int i=0; i<7; i++) {
	      int intValue = getByte(data[1+i*2]) + (getByte(data[2+i*2]) << 8);
	      float realValue = (float)intValue / 1000.f;
	      frequencyBin[i] = realValue;
	    }
	}
	
	public void setWaveForm(byte[] data) {
		for(int i=0; i<256; i+=2) {
	      int intValue = (data[1+i]) + ((data[2+i]) << 8);
	      float realValue = (float)(intValue) / 100.f;
	      waveForm[i] = realValue;
	    }
	}
	
	public void setSleepState(int ss) {
		sleepState = ss;
	}
	
	private int getByte(byte b) {
	  int v = (int) b;
	  if(v<0) v+=256;
	  return v;
	}
	
}
