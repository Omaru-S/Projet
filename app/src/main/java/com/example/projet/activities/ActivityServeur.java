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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
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
        Log.i("ST", "Start Thread");
        Thread thread = new Thread(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                OutputStream outputStream = socket.getOutputStream();

                Log.d("ActivityServeur", "Waiting for client request...");
                String request = reader.readLine();
                if (request != null) {
                    Log.d("ActivityServeur", "Received request: " + request);
                    // Handle different requests
                    if ("GET_DEVICES".equals(request)) {
                        getDevices(new ResponseCallback() {
                            @Override
                            public void onResponse(JSONArray response) {
                                try {
                                    String jsonResponse = response.toString();
                                    outputStream.write((jsonResponse + "\n").getBytes());
                                    outputStream.write("END_OF_MESSAGE\n".getBytes());
                                    outputStream.flush();

                                    outputStream.flush();
                                    Log.d("ActivityServeur", "Sent JSONArray to client.");
                                } catch (IOException e) {
                                    Log.e("ActivityServeur", "Failed to send JSONArray", e);
                                }
                            }

                            @Override
                            public void onError(VolleyError error) {
                                Log.e("ActivityServeur", "Failed to fetch JSONArray", error);
                                try {
                                    outputStream.write("Error fetching devices".getBytes());
                                    outputStream.flush();
                                } catch (IOException e) {
                                    Log.e("ActivityServeur", "Failed to send error message", e);
                                }
                            }
                        });
                    }
                } else {
                    Log.d("ActivityServeur", "No request received. Client may have disconnected.");
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
        String url = "https://www.bde.enseeiht.fr/~bailleq/smartHouse/api/v1/devices/35";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    JSONArray newJsonArray = new JSONArray(); // Move inside the response handler
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject device = response.getJSONObject(i);
                            JSONObject newDevice = new JSONObject();
                            // Extract and put only necessary fields
                            newDevice.put("name", device.getString("NAME"));
                            newDevice.put("brandModel", device.getString("BRAND") + " " + device.getString("MODEL"));
                            newDevice.put("data", device.getString("DATA"));
                            newDevice.put("state", device.getInt("STATE") == 1);
                            newDevice.put("autonomy", device.getInt("AUTONOMY"));
                            newDevice.put("id", device.getInt("ID"));

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

}
