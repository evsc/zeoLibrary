/*
 * Simple ZeoRead drawing example
 * Graph Total_Z value over the total day_span duration
 */



import src.zeo.library.*;

ZeoReader reader;

PFont myFont;

void setup() {
  size(700,400);
  reader = new ZeoReader(this, "zeodata.csv");
  
  myFont = createFont("", 10);
  textFont(myFont);

  noLoop();
}


void draw() {
  background(255);
  
  int border = 30;
  int legend = 20;
  int graphw = width - 2*border - legend;
  int graphh = 300;
  float scaleY = graphh/(60*12.0);
  
  noFill();
  stroke(150);
  pushMatrix();
  translate(border,border);
  
  // draw graph legend
  for(int i=0; i<10; i++) line(6,graphh-i*scaleY*60,legend-2,graphh-i*scaleY*60);
  fill(0);
  noStroke();
  textAlign(RIGHT, CENTER);
  text("Total Sleep in hours", width/2, graphh+20);
  for(int i=0; i<10; i++) text(i, 0, graphh-i*scaleY*60);
  

  translate(legend,0);
  float scaleX = graphw/ (float) reader.day_span;
  
  // draw average total_z value
  stroke(255,0,0);
  noFill();
  line(0,graphh-reader.getAverageTotalZ()*scaleY,graphw,graphh-reader.getAverageTotalZ()*scaleY);
  
  // draw graph
  fill(0);
  noStroke();
  int total_z;
  int day_r;
  for(int i=0; i<reader.nights; i++) {
    rect(reader.night[i].day_relative*scaleX, graphh+1, 1, -reader.night[i].total_z*scaleY);
  }

  popMatrix();
}