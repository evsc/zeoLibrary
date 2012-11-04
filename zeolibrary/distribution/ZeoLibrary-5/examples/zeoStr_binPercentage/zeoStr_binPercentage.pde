/*
 * ZeoLibrary example: zeoStr_binPercentage
 *
 * Connect Zeo Bedside Display via serial port
 * 
 * Display values of 7 Frequency Bins as percentages, 
 * changing over the last 30 seconds.
 */



import processing.serial.*;

import src.zeo.library.*;

ZeoStream zeo;    // stream object

int maxmemory = 120;
ArrayList slices;  // keep last 30 slices in arraylist

PFont myFont;

// colors for frequency bins
color[] binColor = { color(50,50,50), color(40,40,200), color(150,0,200), color(0,250,150), color(50,200,50), color(200,250,0), color(250,100,50) };


void setup() {
  
  size(900,450);
  
  myFont = createFont("", 10);
  textFont(myFont);
  
  slices = new ArrayList();  
  
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
  
  int border = 30;
  int legend = 50;
  int graphw = width - 2*border - legend;
  int graphh = height - 2*border;
  
  float scaleX = graphw / (float) maxmemory;  
  float scaleY = graphh / 7.0f;  // 7 frequency bins
  
  pushMatrix();
  translate(border, border);
  
  // draw legend
  fill(0); noStroke();
  textAlign(RIGHT);
  textLeading(10);
  for(int i=0; i<7; i++) {
    fill(binColor[i]);
    text(zeo.nameFrequencyBin(i), 0, graphh-(i+1)*scaleY, 45, 40);
  }
  

  // draw bins
  translate(legend, 0);
  scaleY = graphh / 1.0f;  // represents 100%

  for(int i=0; i<slices.size(); i++) {
    ZeoSlice zs = (ZeoSlice) slices.get(i);
    
    // first add all bin-values up
    float sum = 0;
    for(int j=0; j<7; j++) sum+= (float) zs.frequencyBin[j];
    
    float y = graphh;  // start y position, always add bin height
    for(int j=0; j<7; j++) {
      fill(binColor[j]);
      float v = (float) zs.frequencyBin[j] / sum;
      rect(i*scaleX, y, scaleX, v * scaleY * -1);
      y -= v*scaleY;
    }
  }

  
  popMatrix();
}

public void zeoSliceEvent(ZeoStream z) {
  ZeoSlice _slice = new ZeoSlice();
  _slice = z.slice;
  slices.add(_slice);
  
  while(slices.size() > maxmemory) {
    slices.remove(0);
  }
}

public void zeoSleepStateEvent(ZeoStream z) {
  println("zeoSleepStateEvent "+z.sleepState);  
}
