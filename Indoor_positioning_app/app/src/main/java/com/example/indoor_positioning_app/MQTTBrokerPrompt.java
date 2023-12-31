package com.example.indoor_positioning_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class MQTTBrokerPrompt {

    private Context _baseContext = null;
    private Context _activityContext = null;
    private AlertDialog.Builder _alertDialogBuilder;
    private AlertDialog.Builder _alert = null;

    private MQTTHelper _mqttHelper = null;

    private String _ip = "", _port="";

    public String Ip()
    {
        return _ip;
    }

    public String Port()
    {
        return _port;
    }

    public void Ip(String ip)
    {
        _ip = ip;
    }

    public String Port(String port)
    {
        return _port = port;
    }
    public void MqttHelper(MQTTHelper mqttHelper)
    {
        _mqttHelper = mqttHelper;
    }
    public MQTTBrokerPrompt(Context baseContext, Context activityContext, MQTTHelper mqttHelper)
    {
        _activityContext = activityContext;
        _baseContext = baseContext;
        _mqttHelper = mqttHelper;
    }

    private void BuildDialog()
    {
        LayoutInflater inflater = (LayoutInflater) _activityContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View alertLayout = inflater.inflate(R.layout.alert_dialog, null);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) final TextInputEditText etIp = alertLayout.findViewById(R.id.IP);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) final TextInputEditText etPort = alertLayout.findViewById(R.id.PORT);

        etIp.setText(_ip.toString());
        etPort.setText(_port.toString());

        _alert = new AlertDialog.Builder(_activityContext);
        _alert.setTitle("MQTT Broker");
        // this is set the view from XML inside AlertDialog
        _alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        _alert.setCancelable(false);
        _alert.setNegativeButton("Cancel", (dialog, which) -> Toast.makeText(_activityContext, "Cancel clicked", Toast.LENGTH_SHORT).show());

        _alert.setPositiveButton("Done", (dialog, which) -> {
            _ip = etIp.getText().toString();
            _port = etPort.getText().toString();
            _mqttHelper.MQTTSubscribe(_ip, _port);

            Toast.makeText(_activityContext, "IP: " + _ip + " _port: " + _port, Toast.LENGTH_LONG).show();
        });
    }

    public void Show()
    {
        if((_activityContext == null) || (_baseContext == null))
        {
            Log.d("MQTTBrokerPrompt", "Show: context not provided");
        }

        BuildDialog();
        // create alert dialog
        AlertDialog dialog = _alert.create();
        //show it
        dialog.show();
    }
}
