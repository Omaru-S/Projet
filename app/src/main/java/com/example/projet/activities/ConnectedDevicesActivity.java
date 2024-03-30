package com.example.projet.activities;

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
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.projet.R;
import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConnectedDevicesActivity extends AppCompatActivity {

    private LinearLayout devicesLayout;
    private RequestQueue requestQueue;
    private Handler handler = new Handler();
    private Runnable updateDevicesRunnable;
    private final String houseId = "35";
    // The URL endpoint for POST requests - replace with actual URL endpoint
    private final String postUrl = "https://www.bde.enseeiht.fr/~bailleq/smartHouse/api/v1/devices/35";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);
        devicesLayout = findViewById(R.id.devicesLayout);
        requestQueue = Volley.newRequestQueue(this);
        // Define the Runnable task for fetching and updating devices
        updateDevicesRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("ConnectedDevicesActivity", "Fetching and updating device information");
                fetchDevices();
                // Schedule the next execution
                handler.postDelayed(this, 10000); // 10 seconds delay
            }
        };

        // Start the periodic updates
        handler.post(updateDevicesRunnable);
    }

    private void fetchDevices() {
        String url = "https://www.bde.enseeiht.fr/~bailleq/smartHouse/api/v1/devices/35";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject device = response.getJSONObject(i);
                                String name = device.getString("NAME");
                                String brandModel = device.getString("BRAND") + " " + device.getString("MODEL");
                                String data = device.getString("DATA");
                                boolean state = device.getInt("STATE") == 1;
                                int autonomy = device.getInt("AUTONOMY");
                                int deviceId = device.getInt("ID");

                                View deviceView = createDeviceView(name, brandModel, data, autonomy, state, deviceId);
                                devicesLayout.addView(deviceView);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Handle error
            }
        });

        requestQueue.add(jsonArrayRequest);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the periodic updates to avoid memory leaks
        if (handler != null && updateDevicesRunnable != null) {
            handler.removeCallbacks(updateDevicesRunnable);
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
                // Determine the new state based on the button's current text, not the isOn variable
                boolean newState = stateButton.getText().toString().equals("OFF");
                toggleDeviceState(deviceId, newState, stateButton);
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
    private void toggleDeviceState(int deviceId, final boolean turnOn, final Button button) {
        String action = turnOn ? "turnOn" : "turnOff";
        StringRequest postRequest = new StringRequest(Request.Method.POST, postUrl + houseId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // On successful response, update the button text
                        button.setText(turnOn ? "ON" : "OFF");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("deviceId", String.valueOf(deviceId));
                params.put("houseId", houseId);
                params.put("action", action);
                return params;
            }
        };
        requestQueue.add(postRequest);
        Log.i("ConnectedDevicesActivity", "Request succes");
    }
}

