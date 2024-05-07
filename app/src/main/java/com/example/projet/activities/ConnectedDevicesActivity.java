package com.example.projet.activities;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.projet.R;
import com.example.projet.bluetooth.BluetoothConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ConnectedDevicesActivity extends AppCompatActivity {

    private LinearLayout devicesLayout;
    private RequestQueue requestQueue;
    private Handler handler = new Handler();
    private Runnable updateDevicesRunnable;
    private List<Device> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);
        devicesLayout = findViewById(R.id.devicesLayout);
        requestQueue = Volley.newRequestQueue(this);

        // Start Bluetooth data receiver
        startBluetoothReceiverThread();

        // Define the Runnable task for fetching and updating devices
        updateDevicesRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("ConnectedDevicesActivity", "Fetching and updating device information");
                sendFetchCommand();
                // Schedule the next execution
                handler.postDelayed(this, 10000); // 10 seconds delay
            }
        };

        // Start the periodic updates
        handler.post(updateDevicesRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the periodic updates to avoid memory leaks
        if (handler != null && updateDevicesRunnable != null) {
            handler.removeCallbacks(updateDevicesRunnable);
        }
    }

    private void startBluetoothReceiverThread() {
        BluetoothSocket socket = BluetoothConnectionManager.getSocket();
        if (socket != null) {
            Thread receiverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream inputStream = socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        String received;

                        while ((received = reader.readLine()) != null) {
                            Log.i("ConnectedDevicesActivity", "Received: " + received);
                            updateDevicesLayout(parseDevices(received));
                        }
                    } catch (IOException e) {
                        Log.e("ConnectedDevicesActivity", "Error reading from socket", e);
                    }
                }
            });
            receiverThread.start();
        }
    }

    private List<Device> parseDevices(String response) {
        List<Device> devices = new ArrayList<>();
        String[] deviceLines = response.split("\n");
        for (String line : deviceLines) {
            String[] parts = line.split(":");
            if (parts.length >= 6) {
                try {
                    int deviceId = Integer.parseInt(parts[1].trim());
                    String name = parts[2].trim();
                    String brandModel = parts[3].trim();
                    boolean state = "ON".equalsIgnoreCase(parts[4].trim());
                    String data = parts[5].trim();
                    int autonomy = Integer.parseInt(parts[6].trim());

                    devices.add(new Device(deviceId, name, brandModel, state, data, autonomy));
                } catch (NumberFormatException e) {
                    Log.e("ConnectedDevicesActivity", "Error parsing device data", e);
                }
            }
        }
        return devices;
    }

    private void updateDevicesLayout(List<Device> newDevices) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                devicesLayout.removeAllViews();
                devices.addAll(newDevices);
                for (Device device : devices) {
                    View deviceView = createDeviceView(device);
                    devicesLayout.addView(deviceView);
                    Log.i("hap",device.name);
                }
            }
        });
    }

    private View createDeviceView(Device device) {
        RelativeLayout layout = new RelativeLayout(this);

        // Name TextView
        TextView nameTextView = new TextView(this);
        nameTextView.setText(device.name);
        RelativeLayout.LayoutParams paramsName = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsName.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        paramsName.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layout.addView(nameTextView, paramsName);

        // Data TextView
        TextView dataTextView = new TextView(this);
        dataTextView.setText(device.brandModel + " " + device.data + (device.autonomy >= 0 ? " Autonomy: " + device.autonomy + "%" : ""));
        RelativeLayout.LayoutParams paramsData = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsData.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        paramsData.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        layout.addView(dataTextView, paramsData);

        // State Button
        final Button stateButton = new Button(this);
        stateButton.setText(device.state ? "ON" : "OFF");
        stateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newState = stateButton.getText().toString().equals("OFF");
                toggleDeviceState(device.deviceId, newState, stateButton);
            }
        });

        RelativeLayout.LayoutParams paramsButton = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsButton.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        paramsButton.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layout.addView(stateButton, paramsButton);

        return layout;
    }

    private void sendFetchCommand() {
        BluetoothSocket socket = BluetoothConnectionManager.getSocket();
        if (socket != null) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                writer.println("fetch");
            } catch (IOException e) {
                Log.e("ConnectedDevicesActivity", "Error sending fetch command", e);
            }
        } else {
            Log.e("ConnectedDevicesActivity", "No Bluetooth socket available");
        }
    }

    private void toggleDeviceState(int deviceId, final boolean turnOn, final Button button) {
        // Your existing toggleDeviceState implementation
    }

    // Device class to hold device information
    private static class Device {
        int deviceId;
        String name;
        String brandModel;
        boolean state;
        String data;
        int autonomy;

        public Device(int deviceId, String name, String brandModel, boolean state, String data, int autonomy) {
            this.deviceId = deviceId;
            this.name = name;
            this.brandModel = brandModel;
            this.state = state;
            this.data = data;
            this.autonomy = autonomy;
        }
    }
}
