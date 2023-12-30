package com.example.indoor_positioning_app;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.QoS;

public class MQTTHelper {
    private MqttAndroidClient mqttAndroidClient = null;
    private Context _applicationContext;
    private Set<String> _uniqueShellyList = null;

    public MQTTHelper(Context applicationContext)
    {
        _applicationContext = applicationContext;
    }

    public Set<String> UniqueShellyList()
    {
        return _uniqueShellyList;
    }

    public Hashtable<String, MQTTData> mqttDataDict = null;

    public void MQTTSubscribe() {
        mqttAndroidClient = new MqttAndroidClient(_applicationContext, "tcp://192.168.5.15:1883", MqttClient.generateClientId());
        mqttDataDict = new Hashtable<String, MQTTData>();
        _uniqueShellyList = new LinkedHashSet<>();

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    Log.d("MQTT", "Reconnected: " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    SubscribeToTopic();
                } else {
                    Log.d("MQTT", "Connected: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, "topic: " + topic + ", msg: " + new String(message.getPayload()));

                MQTTData data = new MQTTData();

                data.SetData(new String(message.getPayload()));

                String dictionaryKey = data.SrcShellly() + "_" + data.DetectedShellly();
                mqttDataDict.put(dictionaryKey, data);
                _uniqueShellyList.add(data.SrcShellly());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        Log.d("MQTT", "Connected");

        mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                disconnectedBufferOptions.setBufferEnabled(true);
                disconnectedBufferOptions.setBufferSize(100);
                disconnectedBufferOptions.setPersistBuffer(false);
                disconnectedBufferOptions.setDeleteOldestMessages(false);
                mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                SubscribeToTopic();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("MQTT", "Failed to connect");
            }
        });
    }

    void SubscribeToTopic() {
        mqttAndroidClient.subscribe("ShellyTopic", QoS.AtLeastOnce.getValue(), null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("MQTT", "onSuccess: ");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("MQTT", "onFailure: ");
            }
        });
    }

}
