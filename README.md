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
4. Run the script
5. Proper functioning can be tested with [MQTT Explorer](http://mqtt-explorer.com/))
6. Additionally, user can view messages through *GET* request: [ShellyIP]/script/[script_number]/testserver?messages
![Screenshot of GET request.](/Images/Shelly_HTTP_GET.JPG "Screenshot of GET request")
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
<img src="/Images/UI.jpg" width="50%" height="50%" title="User interface" alt="User interface">

Toolbar allows the user to show devices, grid and change the IP and PORT of the MQTT broker. 

<p float="left">
  <img src="/Images/drop-down.jpg" width="200" title="Drop down" alt="Drop down">
  <img src="/Images/shown_devices.jpg" width="300" title="Shown devices" alt="Shown devices">
  <img src="/Images/grided_area.jpg" width="300" title="Gridded area" alt="Gridded area">
</p>

When button is clicked the algorithm which calculates the position of the user is executed.
## Algorithm overview
1. Each Shelly device utilized its Bluetooth capability to scan for other Shelly devices and obtain their *RSSI* value
<img src="/Images/shelly_RSSI.svg" title="RSSI between Shelly" alt="RSSI between Shelly">

2. <i>RSSI</i> values are published to MQTT Broker
3. Smartphone subscribe to MQTT Broker and obtains RSSI values from Shelly devices 
4. Smartphone scans through Bluetooth for Shelly devices to obtain *RSSI* (as detected from his own location)
<img src="/Images/RSSI_exchange.png" title="RSSI between Shelly and smartphone" alt="RSSI between Shelly and smartphone">

5. Inverse Distance Weighting is utilized to create grids (for each Shelly) of interpolated *RSSI* values
6. *RSSI* values obtained from smartphone scans are compared against grid cells to find optimal position
<img src="/Images/algorithm.png" title="Algorithm" alt="Algorithm">

7. Position is shown as yellow dot, on the TouchImageView
<img src="/Images/ShownPosition.JPG" title="Algorithm" alt="Algorithm">