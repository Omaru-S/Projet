package com.example.projet.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.bluetooth.BluetoothSocket;

public class BluetoothCommunicationThread extends Thread {
    private static BluetoothCommunicationThread instance;
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    private BluetoothCommunicationThread(BluetoothSocket socket) {
        this.socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            // Handle exception
        }

        inputStream = tmpIn;
        outputStream = tmpOut;
    }

    public static synchronized BluetoothCommunicationThread getInstance(BluetoothSocket socket) {
        if (instance == null) {
            instance = new BluetoothCommunicationThread(socket);
        }
        return instance;
    }

    public void run() {
        byte[] buffer = new byte[1024]; // buffer store for the stream
        int bytes; // bytes returned from read()

        while (true) {
            try {
                // Read from the InputStream
                bytes = inputStream.read(buffer);
                // Send the obtained bytes to the UI or handler
                // handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                break;
            }
        }
    }

    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            // Handle exception
        }
    }

    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            // Handle exception
        }
    }
}
