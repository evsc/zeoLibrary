/*
 * ZeoLibrary example: zeoStr_frequencyBins
 *
 * Connect Zeo Bedside Display via serial port
 * 
 * Display values of 7 Frequency Bins:
 * 0: delta (2-4Hz)
 * 1: theta (4-8Hz)
 * 2: alpha (8-13Hz)
 * 3: beta1 (13-18Hz)
 * 4: beta2 (18-21Hz)
 * 5: beta3 (11-14Hz, sleep spindels)
 * 6: gamma (30-50Hz)
 */



import processing.serial.*;

import src.zeo.library.*;

ZeoStream zeo;    // stream object
ZeoSlice _slice;  // latest zeo data package

PFont myFont;

// colors for frequency bins
color[] binColor = { color(50,50,50), color(40,40,200), color(150,0,200), color(0,250,150), color(50,200,50), color(200,250,0), color(250,100,50) };


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
  
  int border = 10;
  int legend = 20;
  int graphw = width - 2*border;
  int graphh = height - 2*border - legend;
  
  pushMatrix();
  translate(border, border);
  
  float scaleX = graphw / 7.0f;  // 7 frequency bins
  float scaleY = graphh / 15.0;  // scale height of frequency bins
  
  // draw legend
  fill(0); noStroke();
  textAlign(CENTER, TOP);
  textLeading(10);
  for(int i=0; i<7; i++) text(zeo.nameFrequencyBin(i), (i+0.0)*scaleX, graphh, 45, 40);
  
  // draw bins
  for(int i=0; i<7; i++) {
    fill(binColor[i]);
    rect(i*scaleX, graphh, scaleX, (float) _slice.frequencyBin[i]*scaleY*-1);
  }
  
  popMatrix();
}

// triggers when a new data package is received
public void zeoSliceEvent(ZeoStream z) {
  _slice = z.slice;
}

// should trigger every 30 seconds
public void zeoSleepStateEvent(ZeoStream z) {
  println("zeoSleepStateEvent "+z.sleepState);  
}