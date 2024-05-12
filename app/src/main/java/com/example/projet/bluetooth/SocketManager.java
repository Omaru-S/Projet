package com.example.projet.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

public class SocketManager {
    private BluetoothSocket bluetoothSocket;
    private static final SocketManager instance = new SocketManager();

    private SocketManager() {
    }

    public static SocketManager getInstance() {
        return instance;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket socket) {
        this.bluetoothSocket = socket;
    }

    public void closeSocket() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("SocketManager", "Could not close the socket", e);
            }
        }
    }
}

