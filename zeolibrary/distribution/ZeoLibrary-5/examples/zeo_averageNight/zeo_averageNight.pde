/*
 * ZeoLibrary example: zeo_averageNight
 *
 * Draw a graph representing the average
 * sleep state in 5 minutes segments 
 *
 */



import src.zeo.library.*;

ZeoReader reader;

PFont myFont;


color color1 = color(200,0,100); 
color color4 = color(0,100,255);    
color color2 = color(255,0,0); 
color color3 = color(100,0,255);     

int border = 60;
int legend = 80;
int graphw;
int graphh;
int h = 8;    // hours to display horizontally
float scaleY;   
float scaleX; 

void setup() {
  size(800,400);
  reader = new ZeoReader(this, "zeodata.csv");
  
  myFont = createFont("", 10);
  textFont(myFont);

  noLoop();
  smooth();
}


void draw() {
  background(255);
  
  graphw = width - 2*border - legend*2;
  graphh = height- 2*border;
  scaleY = graphh/5.0;         // allow for states 1 to 4
  scaleX = graphw/(h*12.0);    // span of 6 hours

  pushMatrix();
  translate(border,border);
  
  // draw graph legend
  noFill();
  stroke(150);
  for(int i=1; i<5; i++) line(legend-15,i*scaleY,legend-6,i*scaleY);
  for(int i=0; i<=h*12; i++) {
    if(i%12 == 0) stroke(200); else stroke(240);
    line(legend + i*scaleX, 0, legend + i*scaleX, 5*scaleY);
  }

  // draw title
  fill(0);
  noStroke();
  textAlign(CENTER, CENTER);
  text("Average sleep states over the course of the night", legend+graphw/2, graphh+20);
  
  // draw sleep state scale
  textAlign(RIGHT, CENTER);
  text("Wake", legend-20, 1*scaleY);
  text("REM", legend-20, 2*scaleY);
  text("Light Sleep", legend-20, 3*scaleY);
  text("Deep Sleep", legend-20, 4*scaleY);
  
  // draw color legend
  textAlign(LEFT);
  fill(0); text("All Nights", graphw+legend+10, 20);
  fill(color1); text("Bedtime 11pm-12pm", graphw+legend+10, 35);
  fill(color2); text("Bedtime 12pm-1am", graphw+legend+10, 50);
  fill(color3); text("Bedtime 1am-2am", graphw+legend+10, 65);
  fill(color4); text("Bedtime 2am-3am", graphw+legend+10, 80);
  
  
  
  // draw graph
  translate(legend,0);
  
  reader.setFilterHours(10,33,0,20);  // all nights
  noFill(); stroke(0); 
  drawAverageState();
  
  reader.setFilterHours(23,24,0,20);  // bedtime btw. 11-12pm
  stroke(color1);
  drawAverageState();
  
  reader.setFilterHours(24,25,0,20);  // bedtime btw. 12pm-1am
  stroke(color2);
  drawAverageState();
  
  reader.setFilterHours(25,26,0,20);  // bedtime btw. 1am-2am
  stroke(color3);
  drawAverageState();
  
  reader.setFilterHours(26,27,0,20);  // bedtime btw. 12m-3am
  stroke(color4);
  drawAverageState();


  popMatrix();
}


void drawAverageState() {
  
  beginShape();
  
  for(int i=0; i<h*12; i++) {
    
    int addall = 0;
    int counter = 0;
    for(int n=0; n<reader.nights; n++) {
      if(reader.night[n].isRegular() ) {  // only allow nights within filter settings
        if(reader.night[n].sleep_graph_5min.length >= h*12) {
          int state = reader.night[n].sleep_graph_5min[i];
          if(state >= 1 && state <= 4) {    // exclude undefined values
            addall += state;
            counter++;
          }
        }
      }
    }
     
    if(counter>0) {
      float averageState = addall/(float) counter;
      
      vertex(i*scaleX, averageState*scaleY);  // draw state  
    }
    
  }
  
  endShape();
  
}