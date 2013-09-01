/*
 * ZeoLibrary example: zeoStr_monitor
 *
 * Connect Zeo Bedside Display via serial port
 * 
 * Displays:
 * current frequency bin levels
 * current raw brainwave data
 * 30 minute history of frequency bin levels
 * 30 minute history of sleep stages
 */

import processing.serial.*;
import src.zeo.library.*;

PFont myFont;

ZeoStream zeo;    // stream object

int maxmemory = 1800;  // 30 minutes
ArrayList slices;      // keep last X slices in arraylist
int sleepStage = -1;

color[] binColor = { color(50,50,50), color(40,40,200), color(150,0,200), color(0,250,150), color(50,200,50), color(200,250,0), color(250,100,50) };
color[] stageColor = { color(255,255,255), color(255,0,0), color(50,255,50), color(150,150,150), color(0,150,0) };
String[] stageName = { "", "Wake", "REM", "Light", "Deep" };

boolean recording = false;  // if set to true, saves screenshots of software every 30 minutes
int counter = 0;

void setup() {
  frameRate(5);    // we don't really need this to be fast
  size(900,680);
  
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
  
  smooth();
}


void draw() {
  
  background(255);
  int border = 30;
  
  // prevents the software from stopping due to errors during the night
  try {
    drawBins(border,border,300,200);
    drawWave(border*2+300, border, 510,200);
    drawStage(border, 200+border*2, width-border*2, 60);
    drawBinGraph(border, 290+border*2, width-border*2, 300);
  } catch(Exception e) {
    println(getTime() +"\tproblems");
  }
  
  textAlign(LEFT);
  fill(0);
  text(getTime(), 10,16);
  
  if(counter < frameRate*1800 ) {  // every maxmemory
    counter ++;
  } else {
    screenshot();
    counter = 0;
  }
}


public void zeoSliceEvent(ZeoStream z) {
  ZeoSlice s = new ZeoSlice();
  s = z.slice;
  slices.add(s);

  while(slices.size() > maxmemory) {
    slices.remove(0);
  }
}

public void zeoSleepStateEvent(ZeoStream z) {
  sleepStage = z.sleepState; 
}


// draw frame around each of the displays
void drawFrame(int w, int h) {
  noFill(); stroke(200);
  rect(0,0,w,h);
}

// draw snapshot of current frequency bin levels
void drawBins(int x, int y, int w, int h) {  
  pushMatrix();
  translate(x, y);
  drawFrame(w,h);
  
  int legend = 30;
  int graphh = h - legend;
  int graphw = w;
  
  float scaleX = graphw / 7.0f;  // 7 frequency bins
  float scaleY = graphh / 15.0;  // scale height of frequency bins
  
  // draw legend
  fill(0); noStroke();
  textAlign(CENTER, TOP);
  textLeading(10);
  for(int i=0; i<7; i++) text(zeo.nameFrequencyBin(i), (i+0.0)*scaleX, graphh, 45, 40);
  
  // draw bins
  if(slices.size() > 0) {
    ZeoSlice zs = (ZeoSlice) slices.get(slices.size()-1);
    for(int i=0; i<7; i++) {
      fill(binColor[i]);
      rect(i*scaleX, graphh, scaleX, (float) zs.frequencyBin[i]*scaleY*-1);
    }
  }
  
  popMatrix();
}


// draw snapshot of raw brain wave data
void drawWave(int x, int y, int w, int h) {  
  int waveno = 1;  // display value from last X slices
  
  pushMatrix();
  translate(x, y);
  drawFrame(w,h);
  
  int legend = 30;
  int graphh = h - legend;
  int graphw = w;
  
  float scaleX = graphw / (255.0f * waveno);  // 
  float scaleY = graphh / 100.0f;  // 
  
  
  if(slices.size() > 0) {
    
    int m = min(waveno, slices.size());
    
    if(m < waveno) scaleX = graphw / (255.0f * m);
    stroke(0); noFill();
    beginShape();
    
    float xv = graphw;
    for(int z=0; z<m; z++) {
      ZeoSlice zs = (ZeoSlice) slices.get(slices.size()-z-1);
      for(int i=0; i<256; i++) {
        vertex(xv, h/2 + zs.waveForm[i]*scaleY);
        xv -= scaleX;
      }
    }
    endShape();
  }

  popMatrix();
}


// draw 30 minute history of sleep stages
void drawStage(int x, int y, int w, int h) {

  pushMatrix();
  translate(x, y);
  drawFrame(w,h);
  
  int legend = 40;
  int graphh = h;
  int graphw = w-legend;
  
  int mem = maxmemory;  // how many slices to display
  
  float scaleX = graphw / (float) mem;  // 
  float scaleY = graphh / 5.0f;  // 
  
  textAlign(RIGHT, CENTER);
  for(int i=1; i<5; i++) {
    stroke(0); noFill();
    line(legend, i*scaleY, legend-5, i*scaleY);
    fill(0); noStroke();
    text(stageName[i], legend-10, i*scaleY );
  }
  
  noStroke();
  if(slices.size() > 0) {
    
    int m = slices.size();
    textAlign(LEFT);
    for(int i=0; i<m; i++) {
      ZeoSlice zs = (ZeoSlice) slices.get(slices.size()-i-1);
      int stage = zs.sleepState;
      fill(stageColor[stage]);
      rect(w-i*scaleX, graphh, -scaleX, -(5-stage)*scaleY);
    }
  }
  
  popMatrix();
}
  
  
  
  
// draw 30 minute history of frequency bin levels
void drawBinGraph(int x, int y, int w, int h) {

  pushMatrix();
  translate(x, y);
  drawFrame(w,h);
  
  int legend = 40;
  int graphh = h;
  int graphw = w-legend;

  
  int mem = maxmemory;  // how many slices to display
  
  float scaleX = graphw / (float) mem;  // 
  float scaleY = graphh / 8.0f;  // 
  
  textAlign(RIGHT, CENTER);
  for(int i=0; i<7; i++) {
    fill(binColor[i]);
    text(zeo.nameFrequencyBin(i), 0, (i)*scaleY, 45, 40 );
  }
  
  scaleY = graphh / 15.0f;
  strokeWeight(2.0);
  noFill();
  if(slices.size() > 1) {
    
    int m = min(mem, slices.size());
    
    for(int b=0; b<7; b++) {
      stroke(binColor[b]); 
      beginShape();
      for(int i=0; i<m; i++) {
        ZeoSlice zs = (ZeoSlice) slices.get(slices.size()-i-1);
        float v;
        try {
          v = (float) zs.frequencyBin[b];
          if(b==6) v*= 10;
        } catch (Exception c) {
          v = 0;
        }
        vertex(w-i*scaleX, graphh-v*scaleY);
      }
      endShape();
    }
  }
  strokeWeight(1.0);

  popMatrix();
}

String getTime() {
  return hour()+":"+minute();
}

void screenshot() {
  Date d = new Date();
  DateFormat niceFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
  String niceDate = niceFormat.format(d);
  saveFrame("zeoStr-monitor_"+niceDate+".png"); 
  println(getTime()+"\tprinted frame: zeoStr-monitor_"+niceDate+".png");
}
