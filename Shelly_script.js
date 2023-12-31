//Location specific constants
//Shelly in the living room
X = 805
Y = 700
FLOOR = 0

let srcInfo = Shelly.getDeviceInfo();

let SCAN_DURATION = BLE.Scanner.INFINITE_SCAN;
let ACTIVE_SCAN = true;

SHELLY_BLU_CACHE = {};

MAX_NUMBER_OF_MESSAGES = 1000;
let currentMessageIndex = 0;
SHELLY_MSG_CACHE = [];

HTTPServer.registerEndpoint('testserver', 
  function (req, res) 
  {

    // check request and comapare the querystring
    if (req.query === 'devices') 
    {
        // response with some text
        res.body = JSON.stringify(SHELLY_BLU_CACHE);
        res.code = 200;
        res.send();
    } 
    if (req.query === 'messages') 
    {
        // response with some text
        res.body = '';
        for (let i = 0; i < SHELLY_MSG_CACHE.length; i++) 
        {
          res.body += SHELLY_MSG_CACHE [i] +'\r\n';
        } 

        res.code = 200;
        res.send();
    } 
    else 
    {
        res.body = 'Shelly Webserver';
        res.code = 200;
        res.send();
    }
});

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
  sendString +=  res.rssi.toString()+"##"; 
  
  sendString +=  srcInfo.id; 
   a = res.local_name;
  SHELLY_BLU_CACHE[res.addr] = res.local_name;
  MQTT.publish("ShellyTopic", sendString, 1, false)  
  
  if(SHELLY_MSG_CACHE.length >=  MAX_NUMBER_OF_MESSAGES)
  {
    SHELLY_MSG_CACHE[currentMessageIndex] = sendString;
    currentMessageIndex += 1;
    currentMessageIndex  = currentMessageIndex%MAX_NUMBER_OF_MESSAGES;
  }
  else 
  {
    SHELLY_MSG_CACHE.push(sendString)
  }  
  //console.log(sendString);
}

BLE.Scanner.Start({ duration_ms: SCAN_DURATION, active: true }, scanCB);