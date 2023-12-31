# Shelly-Christmas-Challenge: The Indoor Positioning System

## Overview
The Indoor Positioning System focuses on the usage of smart Shelly devices as Bluetooth beacons, which are detected by Android smartphones. Smart phone application considers the known locations of the beacons and their Received Signal Strength Indicator (\textit{RSSI}), to compute approximate location of the user. In this README file there are listed basic requirements and application usage. 
The details are documented in the [README_DOCUMENTATION/documentation.pdf](README_DOCUMENTATION/documentation.pdf)

## MQTT Broker
* [Eclipse Mosquitto](https://www.youtube.com/watch?v=72u6gIkeqUc)
Firstly, MQTT Broker was established. I used open source MQTT broker \href{https://mosquitto.org/}{Eclipse Mosquitto}. Setup was done according to \href{https://www.youtube.com/watch?v=72u6gIkeqUc}{YouTube tutorial}

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
1. Requirements
1. Permission: WiFi, Bluetooth, Location

## Algorithm overview