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

import com.example.projet.R;
import com.example.projet.bluetooth.SocketManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ConnectedDevicesActivity extends AppCompatActivity {

    private LinearLayout devicesLayout;
    private Handler handler = new Handler();
    private Runnable updateDevicesRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);
        devicesLayout = findViewById(R.id.devicesLayout);

        requestDeviceData(); // Request data when activity starts

        updateDevicesRunnable = new Runnable() {
            @Override
            public void run() {
                requestDeviceData(); // Periodically request updated data
                handler.postDelayed(this, 10000); // Update every 10 seconds
            }
        };
        handler.post(updateDevicesRunnable);
    }

    private void requestDeviceData() {
        BluetoothSocket socket = SocketManager.getInstance().getBluetoothSocket();
        if (socket != null) {
            if(socket.isConnected()){
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write("GET_DEVICES\n".getBytes());
                    outputStream.flush();
                    Log.d("ConnectedDevicesActivity", "Request sent to server.");
                    listenForResponse(socket);
                } catch (IOException e) {
                    Log.e("ConnectedDevicesActivity", "Failed to send request", e);
                }
            } else {
                Log.e("ActivityServeur", "Socket is not connected.");
            }
        }
    }

    private void listenForResponse(BluetoothSocket socket) {
        Thread thread = new Thread(() -> {
            try {
                Log.d("ConnectedDevicesActivity", "Starting to listen for Bluetooth data.");
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("END_OF_MESSAGE".equals(line)) {
                        break;  // Break the loop when end marker is received
                    }
                    response.append(line);
                }
                Log.d("ConnectedDevicesActivity", "Received string: " + response.toString());
                JSONArray jsonArray = new JSONArray(response.toString());
                runOnUiThread(() -> onResponse(jsonArray));
            } catch (IOException e) {
                Log.e("ConnectedDevicesActivity", "Error in input stream or reading data", e);
            } catch (JSONException e) {
                Log.e("ConnectedDevicesActivity", "Error parsing JSON", e);
            }
        });
        thread.start();
    }


    private void onResponse(JSONArray response) {
        Log.d("ConnectedDevicesActivity", "Processing JSON response on UI thread.");
        devicesLayout.removeAllViews(); // Clear existing views
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject device = response.getJSONObject(i);
                String name = device.getString("NAME");
                String brandModel = device.getString("BRAND") + " " + device.getString("MODEL");
                String data = device.getString("DATA");
                boolean state = device.getInt("STATE") == 1;
                int autonomy = device.getInt("AUTONOMY");
                int deviceId = device.getInt("ID");

                Log.d("ConnectedDevicesActivity", "Creating view for device: " + name);
                View deviceView = createDeviceView(name, brandModel, data, autonomy, state, deviceId);
                devicesLayout.addView(deviceView);
            }
            Log.d("ConnectedDevicesActivity", "All devices added to layout.");
        } catch (JSONException e) {
            Log.e("ConnectedDevicesActivity", "JSON Parsing error", e);
        }
    }


    private View createDeviceView(String name, String brandModel, String data, int autonomy, boolean isOn, int deviceId) {
        RelativeLayout layout = new RelativeLayout(this);

        // Name TextView
        TextView nameTextView = new TextView(this);
        nameTextView.setText(name);
        RelativeLayout.LayoutParams paramsName = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsName.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        paramsName.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layout.addView(nameTextView, paramsName);

        // Data TextView
        TextView dataTextView = new TextView(this);
        dataTextView.setText(brandModel + " " + data + (autonomy >= 0 ? " Autonomy: " + autonomy + "%" : ""));
        RelativeLayout.LayoutParams paramsData = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsData.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        paramsData.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        layout.addView(dataTextView, paramsData);

        // State Button
        final Button stateButton = new Button(this);
        stateButton.setText(isOn ? "ON" : "OFF");
        stateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDeviceState(deviceId, !isOn, stateButton);
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

    private void toggleDeviceState(int deviceId, boolean turnOn, Button button) {
        // This method would need to be updated to use Bluetooth communication or a local method, as necessary.
        button.setText(turnOn ? "ON" : "OFF"); // Simulate immediate response for UI
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateDevicesRunnable != null) {
            handler.removeCallbacks(updateDevicesRunnable);
        }
    }
}
