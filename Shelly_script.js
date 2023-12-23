Timer.set(1000, true, function() {
MQTT.publish("ShellyTopic", "test1", 1, false)
});