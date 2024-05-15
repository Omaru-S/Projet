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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.projet.R;
import com.example.projet.bluetooth.ResponseCallback;
import com.example.projet.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ActivityServeur extends AppCompatActivity {
    private BluetoothServerSocket serverSocket;
    private Handler handler = new Handler(); // For updating UI or passing data
    // Unique UUID for this application, replace with your own UUID
    private TextView connectionStatusTextView;
    private static final String NAME = "BluetoothDemo";
    private RequestQueue requestQueue;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serveur);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        requestQueue = Volley.newRequestQueue(this);
        initializeBluetoothServer();
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetoothServer() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, Constants.SERVEUR_UUID);
            Thread acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // This call blocks until a connection is established or an exception occurs
                        BluetoothSocket socket = serverSocket.accept();
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
                            //serverSocket.close();
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
        Thread thread = new Thread(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                OutputStream outputStream = socket.getOutputStream();

                String request;
                // Continue reading requests line-by-line until the stream is closed or an error occurs
                while ((request = reader.readLine()) != null) {

                    if (request.endsWith("CHANGE_STATE")) {
                        int deviceId = Integer.parseInt(request.replace("CHANGE_STATE", "").trim());
                        Log.d("ActivityServeur","Change state of" + String.valueOf(deviceId));
                        changeDeviceState(deviceId, outputStream);
                    }
                    if ("GET_DEVICES".equals(request.trim())) {
                        getDevices(new ResponseCallback() {
                            @Override
                            public void onResponse(JSONArray response) {
                                try {
                                    String jsonResponse = response.toString();
                                    int chunkSize = 512; // Define a suitable chunk size
                                    int start = 0;
                                    while (start < jsonResponse.length()) {
                                        int end = Math.min(jsonResponse.length(), start + chunkSize);
                                        String chunk = jsonResponse.substring(start, end);
                                        outputStream.write((chunk + "\nEND_OF_THE_CHUNK\n").getBytes());
                                        start += chunkSize;
                                    }
                                    outputStream.write("END_OF_MESSAGE\n".getBytes());
                                    outputStream.flush();
                                } catch (IOException e) {
                                    Log.e("ActivityServeur", "Failed to send JSONArray", e);
                                }
                            }

                            @Override
                            public void onError(VolleyError error) {
                                try {
                                    outputStream.write("Error fetching devices".getBytes());
                                    outputStream.flush();
                                } catch (IOException e) {
                                    Log.e("ActivityServeur", "Failed to send error message", e);
                                }
                            }
                        });
                    }
                }
            } catch (IOException e) {
                Log.e("ActivityServeur", "Error managing socket", e);
            }
        });
        thread.start();
    }


    // This is a placeholder method to simulate fetching devices data
    private void getDevices(ResponseCallback callback) {
        Log.d("DeviceFetch", "Starting to fetch devices.");

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, Constants.completeURL, null,
                response -> {
                    JSONArray newJsonArray = new JSONArray(); // Move inside the response handler
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject device = response.getJSONObject(i);
                            JSONObject newDevice = new JSONObject();
                            // Extract and put only necessary fields
                            newDevice.put("NAME", device.getString("NAME"));
                            newDevice.put("BRAND", device.getString("BRAND"));
                            newDevice.put("MODEL",device.getString("MODEL"));
                            newDevice.put("DATA", device.getString("DATA"));
                            newDevice.put("STATE", device.getInt("STATE"));
                            newDevice.put("AUTONOMY", device.getInt("AUTONOMY"));
                            newDevice.put("ID", device.getInt("ID"));

                            newJsonArray.put(newDevice);
                        }
                        if (callback != null) {
                            callback.onResponse(newJsonArray);
                        }
                    } catch (JSONException e) {
                        Log.e("DeviceFetch", "Error parsing JSON", e);
                        if (callback != null) {
                            callback.onError(new VolleyError(e));
                        }
                    }
                },
                error -> {
                    Log.e("DeviceFetch", "Error fetching data: " + error.toString());
                    if (callback != null) {
                        callback.onError(error);
                    }
                });

        requestQueue.add(jsonArrayRequest);
    }
    private void changeDeviceState(int deviceId, OutputStream outputStream) {
        Log.d("ActivityServeur", "Preparing to send POST request for device state change");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.url,
                response -> {
                    try {
                        Log.d("ActivityServeur", "POST request successful, response: " + response);
                        outputStream.write(("POST_SUCCESS\n" + response + "\n").getBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        Log.e("ActivityServeur", "Failed to send POST_SUCCESS and response back to client", e);
                    }
                },
                error -> {
                    Log.e("ActivityServeur", "Error in POST request: " + error.toString());
                    try {
                        outputStream.write("POST_FAILED\n".getBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        Log.e("ActivityServeur", "Failed to send POST_FAILED message to client", e);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("deviceId", String.valueOf(deviceId));
                params.put("houseId",Constants.houseId);
                params.put("action", "turnOnOff");
                Log.d("ActivityServeur", "POST params: deviceId=" + deviceId + ", action=turnOnOff");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                Log.d("ActivityServeur", "Headers set for POST request");
                return headers;
            }
        };
        requestQueue.add(stringRequest);
        Log.d("ActivityServeur", "POST request added to the queue");
    }

}
