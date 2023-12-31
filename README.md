# Shelly-Christmas-Challenge: The Indoor Positioning System
## Overview
The Indoor Positioning System focuses on the usage of smart Shelly devices as Bluetooth beacons, which are detected by Android smartphones. Smart phone application considers the known locations of the beacons and their Received Signal Strength Indicator (*RSSI*), to compute approximate location of the user. In this README file, there are listed basic requirements and application usage. 
The details are documented in the [README_DOCUMENTATION/documentation.pdf](README_DOCUMENTATION/documentation.pdf)
## MQTT Broker
Since our application uses MQTT, MQTT Broker has to be established. This can be done with [Eclipse Mosquitto (youtube tutorial)](https://www.youtube.com/watch?v=72u6gIkeqUc)
## Shelly device
### Requirements
Shelly devices are required to have:
* Bluetooth
* Support for MQTT
* Support for scripting
### Setup
1. Enable MQTT (help can be found [there](https://www.youtube.com/watch?v=Cou6PT8nF3E))
2. Copy *[Shelly_script.js](Shelly_script.js)* into the Shelly device
3. Change hardcoded values of the device location (variables *X*, *Y*, *FLOOR*)
4. Run the script (Proper functioning can be tested with [MQTT Explorer](http://mqtt-explorer.com/))
## Android application
### Requirements
* Android OS
* minimal sdk version 33
* enabled Bluetooth, location, WiFi (in case the MQTT broker is on the same network)
 
### User interface
User interface interface consists of:
* toolbar
* [TouchImageView](https://github.com/MikeOrtiz/TouchImageView), which shows floor plan
* start button
<img src="/Images/UI.jpg" width="50%" height="50%" title="User interface">

Toolbar allows the user to show devices and grid


<p float="left">
  <img src="/Images/drop-down.jpg" width="100" title="Drop down">
  <img src="/Images/shown_devices.jpg" width="100" title="Shown devices">
  <img src="/Images/grided_area.jpg" width="100" title="Gridded area">
</p>







## Algorithm overview