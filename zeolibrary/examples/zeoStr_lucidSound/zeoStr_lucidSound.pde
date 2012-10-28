/*
 * ZeoLibrary example: zeoStr_lucidSound
 *
 * Connect Zeo Bedside Display via serial port
 * 
 * Plays mp3-file 5 minutes into REM sleep
 * to help trigger lucid dreaming
 */

import processing.serial.*;
import src.zeo.library.*;
import ddf.minim.*;


// sound starts after 5 minutes into REM stage
int remDelay = 5;

// duration of sound to play, in seconds
int playDuration = 180;

// name of mp3 soundfile, placed in data folder
String soundFile = "BM207.mp3";  


ZeoStream zeo;    // stream object
int sleepStage = 0;
int counter = 0;
int remCounter = 0;

PFont myFont;

AudioPlayer player;
Minim minim;

// colors and names for WAKE, REM, LIGHT and DEEP sleep stages
color[] stageColor = { color(255,255,255), color(255,0,0), color(50,255,50), color(150,150,150), color(0,150,0) };
String[] stageName = { "", "Wake", "REM", "Light", "Deep" };


void setup() {
  
  size(200,150);
  
  myFont = createFont("", 20);
  textFont(myFont);
  
  minim = new Minim(this);
  // load a file, give the AudioPlayer buffers that are 2048 samples long
  player = minim.loadFile(soundFile, 2048);
  // play soundfile on startup, to test and to adjust sound volume
  player.play();
  
  
  // print serial ports
  println(Serial.list());
  // select serial port for ZEO
  zeo = new ZeoStream(this, Serial.list()[1] );
  zeo.debug = false;
  // start to read data from serial port
  zeo.start();
}


void draw() {
  
  // background color represents sleep stage
  background(stageColor[sleepStage]);
  fill(210);
  text(getTime(), 20, 40);    // print time
  text(stageName[sleepStage], 20, 70);  // print sleep stage

  if(remCounter > 0) {
    remCounter--;
    
    if(remCounter <=0) {
      if(player.isPlaying()) {
        text("sound is playing!", 20, 100);
        // if sound is playing, STOP sound
        REMstop(); 
      } else {
        // if no sound is playing, START sound
        REMevent();
      }
    }
  }

}

// triggers when a new data package is received
public void zeoSliceEvent(ZeoStream z) {

}

// should trigger every 30 seconds
public void zeoSleepStateEvent(ZeoStream z) {
  if(sleepStage != z.sleepState) {
    // change of sleep stage
    if(z.sleepState == 2) {
      // new state = REM!
      remCounter = (int) frameRate*60*remDelay;  // wait 5 minutes
    } else {
      // cancel remCounter in case a change occures, before 5 minutes
      REMstop();
    }
  }
  sleepStage = z.sleepState; 
}

// start sound 5 minutes into REM sleep
public void REMevent() {
  // turn on music!
  println(getTime()+"\tREM sleep, start playing sound");
  player.loop();
  remCounter = (int) frameRate*playDuration;  // play for 180 seconds
}

// stop sound
public void REMstop() {
  if(player.isPlaying()) {
    player.pause();
    println(getTime()+"\tpause playing");
  }
  remCounter = 0;
}

// always close Minim audio classes when you are done with them
void stop() {
  player.close();
  minim.stop();
  super.stop();
}


String getTime() {
  return hour()+":"+minute();
}
