/*
 * ZeoLibrary example: zeoStr_displayWave
 *
 * Connect Zeo Bedside Display via serial port
 * 
 * Display raw time-domain brainwave with samplerate 128Hz.
 * Needs 60Hz filter
 */
 
 
 

import processing.serial.*;

import src.zeo.library.*;

ZeoStream zeo;    // stream object
ZeoSlice _slice;  // latest zeo data package

PFont myFont;


void setup() {
  
  size(600,450);
  
  myFont = createFont("", 10);
  textFont(myFont);
  
  _slice = new ZeoSlice();  
  
  // print serial ports
  println(Serial.list());
  // select serial port for ZEO
  zeo = new ZeoStream(this, Serial.list()[1] );
  zeo.debug = false;
  // start to read data from serial port
  zeo.start();
}


void draw() {
  
  background(255);
  
  float scaleX = width / 255.0f;  // waveband array has 256 values
  float scaleY = height / 500.0;  // scale height
  
  stroke(255,0,0); noFill();
  beginShape();
  for(int i=0; i<256; i++) vertex(i*scaleX, height/2 + _slice.waveForm[i]*scaleY);
  endShape();

}

public void zeoSliceEvent(ZeoStream z) {
  _slice = z.slice;
}

public void zeoSleepStateEvent(ZeoStream z) {
  println("zeoSleepStateEvent "+z.sleepState);  
}
