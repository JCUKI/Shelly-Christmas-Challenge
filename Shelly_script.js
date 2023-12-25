//Location specific constants
//Shelly in the bedroom
X = 1075
Y = 0
FLOOR = 2

//let info = Shelly.getDeviceInfo();
//console.log(info);

let SCAN_DURATION = BLE.Scanner.INFINITE_SCAN;
let ACTIVE_SCAN = true;

function scanCB(ev, res) {
  if (ev !== BLE.Scanner.SCAN_RESULT) return;

  //if we didnt find Shelly device
  if (typeof res.local_name !== 'string') return;
  if (res.local_name.indexOf("Shelly") < 0) return;
          
  let sendString = res.addr + "##";
  
  //send source location
  sendString += X.toString()+ "##" + Y.toString() + "##" + FLOOR.toString()+ "##";
  
  /*if (typeof res.local_name !== 'string')//in case other devices are used 
  {
    sendString += "undefined##"; 
  }
  else*/  
  {
    sendString +=  res.local_name + "##"; 
  } 
  sendString +=  res.rssi.toString(); 
    
  MQTT.publish("ShellyTopic", sendString, 1, false)  
  //console.log(sendString);
}

BLE.Scanner.Start({ duration_ms: SCAN_DURATION, active: true }, scanCB);