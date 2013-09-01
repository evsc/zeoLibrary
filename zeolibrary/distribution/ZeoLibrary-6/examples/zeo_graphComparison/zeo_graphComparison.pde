/*
 * ZeoLibrary example: zeo_graphComparison
 *
 * Draw the sleep_graph_30sec array for one night
 * and compare it to the sleep_graph_5min plot
 * 
 * press UP, RIGHT to display next night
 * press DOWN, LEFT to display previous night
 * press SPACE to circle through all nights
 */



import src.zeo.library.*;

ZeoReader reader;

PFont myFont;

int displayNight = 0;  // circle through all nights
boolean play = false;

color color1 = color(255,0,0);  // wake state
color color2 = color(50,255,50);  // REM state
color color3 = color(150,150,150);  // light sleep
color color4 = color(0,150,0);  // deep sleep

void setup() {
  size(1400,500);
  reader = new ZeoReader(this);
  reader.setCutOff(false);   // deactivate cutoff, for true comparison of 5min and 30sec graph

  // if CSV file
  reader = new ZeoReader(this, "zeodata.csv");
  // if CAT file
//  reader = new ZeoReader(this);
//  reader.readDatFile(sketchPath("data/ZEOSLEEP.DAT"));
  
  myFont = createFont("", 10);
  textFont(myFont);

  frameRate(20);
}


void draw() {
  if(displayNight >= reader.nights) displayNight = 0;
  else if(displayNight < 0) displayNight = reader.nights-1;
  
  background(255);
  
  int border = 30;
  int legend = 70;
  int graphw = width - 2*border - 100;
  int graphh = height - 2*border - legend;
  float scaleX = graphw/(60*10.0);                // scale to fit 12hours
  float scaleY = graphh/ 6.0;                     // fit 1-4 states


  pushMatrix();
  translate(border,border);
  
  // draw graph legend
  noFill(); stroke(200);
  for(int i=0; i<10; i++) line(legend+i*scaleX*60,10,legend+i*scaleX*60,graphh+legend-scaleY-10);
  for(int i=1; i<5; i++) line(legend-15,legend+i*scaleY,legend-6,legend+i*scaleY);
  
  fill(0); noStroke();
  textAlign(CENTER);
  for(int i=0; i<10; i++) text(i+"h", legend+i*scaleX*60, 0);

  
  // draw sleep state scale
  textAlign(CENTER, CENTER);
  text("Sleep States for "+reader.night[displayNight].date.toString().substring(0,10), legend+graphw/2, graphh+50);
  textAlign(RIGHT, CENTER);
  fill(color1); text("Wake", legend-20, legend+1*scaleY);
  fill(color2); text("REM", legend-20, legend+2*scaleY);
  fill(color3); text("Light Sleep", legend-20, legend+3*scaleY);
  fill(color4); text("Deep Sleep", legend-20, legend+4*scaleY);
  
  // draw graph
  translate(legend,legend);

  int i = displayNight;   // pick one night
  
  // 30second graph
  for(int m=0; m<reader.night[i].sleep_graph_30sec.length; m++) {
    int state = reader.night[i].sleep_graph_30sec[m];
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
      rect(m*scaleX*0.5, graphh-scaleY, scaleX*0.5, -(graphh-(state+1)*scaleY));
    }
  }
  
  // 5minute graph, for comparison
  beginShape();
  noFill();
  stroke(0);
  for(int m=0; m<reader.night[i].sleep_graph_5min.length; m++) {
    int state = reader.night[i].sleep_graph_5min[m];
    if(state>0)  {            // don't draw undefined state (0)
      vertex(m*scaleX*5,  state*scaleY - 10);
      vertex((m+1)*scaleX*5, state*scaleY - 10);
    }
  }
  endShape();


  popMatrix();
  
  if(play) displayNight++;

}





void keyReleased() {

  if(key == 'p') {
    Date d = new Date();
    DateFormat niceFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
    saveFrame("graphComparison-"+niceFormat.format(d)+".png"); 
    println("printed frame");
  } else if(key == ' ') {
    play = !play;
  }
  
}

void keyPressed() {
  if(keyCode == UP || keyCode == RIGHT) {
    displayNight++;
  } else if(keyCode == DOWN || keyCode == LEFT) {
    displayNight--;
  }
}