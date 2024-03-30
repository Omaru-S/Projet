package com.example.projet.activities;// Imports
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
import com.example.projet.utils.Constants;

import java.io.IOException;
import java.util.Set;

public class ActivityClient extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket mmSocket;
    private Handler handler = new Handler(); // For updating UI or passing data
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
                if(device.getName().equals("Redmi_serv")) {
                    try {
                        mmSocket = device.createRfcommSocketToServiceRecord(Constants.SERVEUR_UUID);
                        bluetoothAdapter.cancelDiscovery(); // Always cancel discovery because it will slow down a connection
                        mmSocket.connect(); // Attempt to connect to the remote device
                        Log.i("BluetoothClient", "Successfully connected to the server!");
                        // You can now manage the connection (in a separate thread)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectionStatusTextView.setText("Connecté au serveur!");
                            }
                        });
                        manageConnectedSocket(mmSocket);
                        Intent intent = new Intent(ActivityClient.this, ConnectedDevicesActivity.class);
                        startActivity(intent);

                        break; // Exit the loop once connected
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

    private void manageConnectedSocket(BluetoothSocket socket) {
        // Handle the connection in a separate thread
        // Example: start a thread to manage the connection
    }
}
