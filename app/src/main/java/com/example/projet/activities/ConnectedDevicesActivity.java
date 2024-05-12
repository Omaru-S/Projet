package com.example.projet.activities;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Typeface;
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
            Log.d("ConnectedDevicesActivity", "Thread started, listening for response.");
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d("ConnectedDevicesActivity", "Read line: " + line);
                    if ("END_OF_MESSAGE".equals(line.trim())) {
                        Log.d("ConnectedDevicesActivity", "End of message detected.");
                        break;
                    } else if ("END_OF_THE_CHUNK".equals(line.trim())) {
                        Log.d("ConnectedDevicesActivity", "End of chunk detected.");
                    } else {
                        response.append(line); // Append line directly
                    }
                }
                Log.d("ConnectedDevicesActivity", "Final response: " + response.toString());

                try {
                    JSONArray jsonArray = new JSONArray(response.toString()); // Assuming the response string is a valid JSON array
                    runOnUiThread(() -> onResponse(jsonArray));
                } catch (JSONException e) {
                    Log.e("ConnectedDevicesActivity", "Invalid JSON format received, retrying...", e);
                    requestRetry();
                }
            } catch (IOException e) {
                Log.e("ConnectedDevicesActivity", "Error reading from socket", e);
            }
        });
        thread.start();
    }

    private void requestRetry() {
        Log.d("ConnectedDevicesActivity", "Retrying data request due to previous failure.");
        handler.postDelayed(this::requestDeviceData, 100); // Retry after 0.1 second
    }

    private void onResponse(JSONArray response) {
        Log.d("ConnectedDevicesActivity", "Processing JSON response on UI thread. Number of devices: " + response.length());
        Log.d("hap",response.toString());
        devicesLayout.removeAllViews(); // Clear existing views
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject device = response.getJSONObject(i);

                // Use a default value or log a warning if the expected key is not present
                String name = device.optString("NAME", "Unknown Name"); // Use "Unknown Name" if "NAME" key is missing
                String brand = device.optString("BRAND", "Unknown Brand");
                String model = device.optString("MODEL", "");
                String data = device.optString("DATA", "No data available");
                int state = device.optInt("STATE", 0); // Default to false (0) if "STATE" key is missing
                int autonomy = device.optInt("AUTONOMY", -1); // Use -1 to indicate missing data
                int deviceId = device.optInt("ID", -1); // Use -1 to indicate missing data

                //Log.d("ConnectedDevicesActivity", "Creating view for device: " + name);
                View deviceView = createDeviceView(name, brand + " " + model, data, autonomy, state, deviceId);
                devicesLayout.addView(deviceView);
            }
            //Log.d("ConnectedDevicesActivity", "All devices added to layout.");
        } catch (JSONException e) {
            //Log.e("ConnectedDevicesActivity", "JSON Parsing error", e);
        }
    }




    private View createDeviceView(String name, String brandModel, String data, int autonomy, int isOn, int deviceId) {
        Context context = this; // Context for view creation
        RelativeLayout layout = new RelativeLayout(context);
        layout.setPadding(16, 16, 16, 16); // Padding for the layout

        // Name TextView
        TextView nameTextView = new TextView(context);
        nameTextView.setId(View.generateViewId()); // Unique ID for layout referencing
        nameTextView.setText(name);
        nameTextView.setTextSize(18); // Text size
        nameTextView.setTypeface(null, Typeface.BOLD); // Bold text for the name
        RelativeLayout.LayoutParams paramsName = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsName.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        paramsName.addRule(RelativeLayout.ALIGN_PARENT_START);
        layout.addView(nameTextView, paramsName);

        // BrandModel and Autonomy/Data TextView
        TextView brandModelTextView = new TextView(context);
        brandModelTextView.setId(View.generateViewId());
        String autonomyText = autonomy == -1 ? "No Battery" : "Autonomy: " + autonomy + "%";
        brandModelTextView.setText(brandModel );
        brandModelTextView.setTextSize(16);
        RelativeLayout.LayoutParams paramsBrandModel = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsBrandModel.addRule(RelativeLayout.BELOW, nameTextView.getId());
        paramsBrandModel.addRule(RelativeLayout.ALIGN_START, nameTextView.getId());
        layout.addView(brandModelTextView, paramsBrandModel);

        // Data TextView just below BrandModel
        TextView dataTextView = new TextView(context);
        dataTextView.setTextSize(16); // Same text size as brand/model for consistency
        dataTextView.setText(autonomyText + " - " + data);
        RelativeLayout.LayoutParams paramsData = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsData.addRule(RelativeLayout.BELOW, brandModelTextView.getId());
        paramsData.addRule(RelativeLayout.ALIGN_START, brandModelTextView.getId());
        layout.addView(dataTextView, paramsData);

        // State Button
        Button stateButton = new Button(context);
        stateButton.setId(View.generateViewId());
        stateButton.setText(isOn == 1 ? "ON" : "OFF"); // Set text based on isOn value
        stateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDeviceState(deviceId, isOn == 1 ? 0 : 1, stateButton); // Toggle state
            }
        });
        RelativeLayout.LayoutParams paramsButton = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsButton.addRule(RelativeLayout.ALIGN_PARENT_END);
        paramsButton.addRule(RelativeLayout.CENTER_VERTICAL);
        layout.addView(stateButton, paramsButton);

        return layout;
    }



    private void toggleDeviceState(int deviceId, int turnOn, Button button) {
        // This method would need to be updated to use Bluetooth communication or a local method, as necessary.
        button.setText(turnOn == 1 ? "ON" : "OFF"); // Simulate immediate response for UI
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateDevicesRunnable != null) {
            handler.removeCallbacks(updateDevicesRunnable);
        }
    }
}
