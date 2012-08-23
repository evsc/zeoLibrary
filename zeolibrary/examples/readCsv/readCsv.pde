/*
 * Simple ZeoRead example
 * Read in .csv file and compute various average data values
 */



import src.zeo.*;

ZeoReader reader;

void setup() {
  
  reader = new ZeoReader(this);
//  reader.setCutOff(true);
  reader.readFile("zeodata.csv");
  
  reader.setFilter(true);
  reader.setFilterHours(21,29,4,12);
  
  println();
  println("Average ZQ score: \t\t"+reader.getAverageZq());
  println("Average total sleep: \t"+ reader.getAverageTotalZ()+" min ("+formatMinutes(reader.getAverageTotalZ())+")");
  println("Average time to fall asleep: \t"+reader.getAverageTimeToZ()+" min");
  println("Average time in wake: \t"+reader.getAverageTimeInWake()+" min");
  println("Average time in REM: \t"+reader.getAverageTimeInRem()+" min ("+formatMinutes(reader.getAverageTimeInRem())+")");
  println("Average time in light sleep: \t"+reader.getAverageTimeInLight()+" min ("+formatMinutes(reader.getAverageTimeInLight())+")");
  println("Average time in deep sleep: \t"+reader.getAverageTimeInDeep()+" min ("+formatMinutes(reader.getAverageTimeInDeep())+")");
  println("Average number of awakenings: \t"+nf(reader.getAverageAwakenings(),0,2));
  println("Average sleep duration: \t"+reader.getAverageDuration()+" min ("+formatMinutes(reader.getAverageDuration())+")");
  
  println();
  
  println("Average bedtime: \t\t"+formatMinutes(reader.getAverageStart()));
  println("Average sleep onset: \t"+formatMinutes(reader.getAverageOnset()));
  println("Average rise-time: \t\t"+formatMinutes(reader.getAverageRise()));
  println("Average end of night: \t"+formatMinutes(reader.getAverageEnd()));
}

String formatMinutes(int input) {
  // format minutes into clock-time
  int hours = (int)(Math.floor(input/60))%24;
  int minutes = input%60;
  return nf(hours,2,0) + ":"+nf(minutes,2,0);
}