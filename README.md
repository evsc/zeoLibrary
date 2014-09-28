zeoLibrary
==========

Processing Lib to import, stream and analyse your Zeo Sleep Data



You can use this library for either reading and analysing the .csv files exported from the Zeo website, or to stream data live from your Zeo Bedside Display.

ZeoStream
==========
Stream live data from the serial port of your Zeo Bedside Display (note: company went out of business). For the correct hardware setup follow the instructions at [Zeo Raw Data Library - Getting Started](http://www.sleepstreamonline.com/rdl/starting.html). It boils down to installing the 2.6.3R firmware, and slightly modifying a FTDI cable. 

About every second a ZeoSlice object is read and communicates current raw brainwave data and processed frequency bin data. About every 30 seconds the Sleep Manager communicates the average current sleep stage.

<p align="center">
	<img src="https://raw.githubusercontent.com/evsc/zeoLibrary/master/zeostream.png"/>
</p>


ZeoReader
==========
The ZeoReader class imports and parses "zeodata.csv" and "ZEOSLEEP.DAT" files, cleans out unusable nights, and creates ZeoNight objects for every night. Each ZeoNight object holds sleep data information that originated from the Zeo Sleep Manager sensor readings, and sleep diary information that has been entered through the Zeo website (if .csv). The library gives you easy access to that data and offers simple functions to compute averages for various sleep data values (total sleep, REM, light sleep, deep sleep, sleep onset time, awakenings, ...)

"ZEOSLEEP.DAT" files can be taken directly off the Zeo Bedside Display's memory card, if the device has been updated to firmware v2.6.3O.


<p align="center">
	<img src="https://raw.githubusercontent.com/evsc/zeoLibrary/master/zeoreader.png"/>
</p>

Install
=======

1.  Download latest zeolibrary zip file in [releases](https://github.com/evsc/zeoLibrary/releases)
2.  Download [Processing](http://processing.org/download/?processing)
3.  Install zeolibrary by following this [guide](http://www.learningprocessing.com/tutorials/libraries/) 
4.  Open Processing and open some of the zeolibrary examples via File>Examples>Contributed Libaries>Zeolibary 


Examples
========
http://www.evsc.net/tag/zeolibrary


External Links
==============
http://eric-blue.com/2013/06/09/life-beyond-zeo/comment-page-1/

