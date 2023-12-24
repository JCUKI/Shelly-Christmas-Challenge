let SCAN_DURATION = BLE.Scanner.INFINITE_SCAN;
let ACTIVE_SCAN = true;

SHELLY_BLU_CACHE = {}
function scanCB(ev, res) {
  if (ev !== BLE.Scanner.SCAN_RESULT) return;
  
  // skip if we have already found this device
  if (typeof SHELLY_BLU_CACHE[res.addr] !== 'undefined') return;
  
      
  SHELLY_BLU_CACHE[res.addr] = res.local_name;
  MQTT.publish("ShellyTopic", res.rssi.toString(), 1, false)  
}

BLE.Scanner.Start({ duration_ms: SCAN_DURATION, active: true }, scanCB);