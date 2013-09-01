/*
 * ZeoLibrary example: zeo_graphStates5min
 *
 * Draw the sleep_graph_5min array for all nights,
 * with colors representing the sleep states
 *
 */



import src.zeo.library.*;

ZeoReader reader;

PFont myFont;

color color1 = color(255,0,0);  // wake state
color color2 = color(50,255,50);  // REM state
color color3 = color(150,150,150);  // light sleep
color color4 = color(0,150,0);  // deep sleep

void setup() {
  size(600,1000);
  
  // if CSV file
  reader = new ZeoReader(this, "zeodata.csv");
  // if CAT file
//  reader = new ZeoReader(this);
//  reader.readDatFile(sketchPath("data/ZEOSLEEP.DAT"));
  
  myFont = createFont("", 10);
  textFont(myFont);

  noLoop();
  smooth();
}


void draw() {
  background(255);
  
  int border = 30;
  int legend = 20;
  int graphw = width - 2*border - 100;
  int graphh = height - 2*border - legend;
  float scaleX = graphw/(60*12.0);                // scale to fit 12hours
  float scaleY = graphh/ (float) reader.day_span; // height of one night


  pushMatrix();
  translate(border,border);
  
  // draw graph legend
  noFill(); stroke(150);
  for(int i=0; i<10; i++) line(i*scaleX*60,5,i*scaleX*60,legend-5);
  
  fill(0); noStroke();
  textAlign(CENTER);
  for(int i=0; i<10; i++) text(i, i*scaleX*60, 0);
  
  textAlign(LEFT);
  text("Sleep States", width-150, 0);
  fill(color1); text("Wake", width-150, 20);
  fill(color2); text("REM", width-150, 35);
  fill(color3); text("Light Sleep", width-150, 50);
  fill(color4); text("Deep Sleep", width-150, 65);
  
  // draw graph
  translate(0,legend);
  for(int i=0; i<reader.nights; i++) {
    pushMatrix();
    translate(0, reader.night[i].day_relative*scaleY);

    for(int m=0; m<reader.night[i].sleep_graph_5min.length; m++) {
      int state = reader.night[i].sleep_graph_5min[m];
      switch(state) {
        case 1: fill(color1);  // wake state
                break;
        case 2: fill(color2);   // REM state
                break;
        case 3: fill(color3);  // light sleep
                break;
        case 4: fill(color4);  // deep sleep
                break;
      }
      if(state>0)  {            // don't draw undefined state (0)
        rect(m*5*scaleX, 0, 4*scaleX, 2);
      }
    }

    popMatrix();
  }

  popMatrix();
}