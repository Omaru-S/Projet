package com.example.projet.activities;// Imports
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projet.R;
import com.example.projet.utils.Constants;

import java.io.IOException;
import java.util.UUID;

public class ActivityServeur extends AppCompatActivity {
    private BluetoothServerSocket serverSocket;
    private Handler handler = new Handler(); // For updating UI or passing data
    // Unique UUID for this application, replace with your own UUID
    private TextView connectionStatusTextView;
    private static final String NAME = "BluetoothDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serveur);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        initializeBluetoothServer();
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetoothServer() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i("BluetoothServer", "1");
        try {
            Log.i("BluetoothServer", "2");
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, Constants.SERVEUR_UUID);
            Log.i("BluetoothServer", "3");
            Thread acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i("BluetoothServer", "5");
                        // This call blocks until a connection is established or an exception occurs
                        BluetoothSocket socket = serverSocket.accept();
                        Log.i("BluetoothServer", "6");
                        if (socket != null) {
                            // A connection was accepted
                            Log.i("BluetoothServer", "Successfully connected to client!");
                            // You can now manage the connection (in a separate thread)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectionStatusTextView.setText("Connecté au client!");
                                }
                            });
                            manageConnectedSocket(socket);

                            // Close the server socket as you only want to connect to one client
                            serverSocket.close();
                        }
                    } catch (IOException e) {
                        Log.e("BluetoothServer", "Socket's accept method failed", e);
                    }
                }
            });
            acceptThread.start();
        } catch (IOException e) {
            Log.e("BluetoothServer", "Socket's listen method failed", e);
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        // Handle the connection in a separate thread
        // Example: start a thread to manage the connection
    }
}
