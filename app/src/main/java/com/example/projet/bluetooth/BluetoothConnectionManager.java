package com.example.projet.bluetooth;

import android.bluetooth.BluetoothSocket;

public class BluetoothConnectionManager {
    private static BluetoothSocket socket;

    public static BluetoothSocket getSocket() {
        return socket;
    }

    public static void setSocket(BluetoothSocket socket) {
        BluetoothConnectionManager.socket = socket;
    }
}
