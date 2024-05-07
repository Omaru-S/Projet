package com.example.projet.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projet.R;
import com.example.projet.bluetooth.BluetoothConnectionManager;
import com.example.projet.utils.Constants;

import java.io.IOException;
import java.util.Set;

public class ActivityClient extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket mmSocket;
    private Handler handler = new Handler();
    private TextView connectionStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        initializeBluetoothConnection();
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetoothConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("Redmi_serv")) {
                    try {
                        mmSocket = device.createRfcommSocketToServiceRecord(Constants.SERVEUR_UUID);
                        bluetoothAdapter.cancelDiscovery();
                        mmSocket.connect();
                        Log.i("BluetoothClient", "Successfully connected to the server!");
                        runOnUiThread(() -> connectionStatusTextView.setText("Connecté au serveur!"));

                        BluetoothConnectionManager.setSocket(mmSocket); // Save the socket

                        Intent intent = new Intent(ActivityClient.this, ConnectedDevicesActivity.class);
                        startActivity(intent);
                        break;
                    } catch (IOException connectException) {
                        Log.e("BluetoothClient", "Could not connect to the server", connectException);
                        try {
                            mmSocket.close();
                        } catch (IOException closeException) {
                            Log.e("BluetoothClient", "Could not close the client socket", closeException);
                        }
                    }
                }
            }
        }
    }
}
