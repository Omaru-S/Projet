package com.example.projet.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.projet.R;
import com.example.projet.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ActivityServeur extends AppCompatActivity {
    private BluetoothServerSocket serverSocket;
    private Handler handler = new Handler(); // For updating UI or passing data
    private TextView connectionStatusTextView;
    private static final String NAME = "BluetoothDemo";
    private final String houseId = "19";
    private final String postUrl = "https://www.bde.enseeiht.fr/~bailleq/smartHouse/api/v1/devices/19";
    private RequestQueue requestQueue;
    private BluetoothSocket connectedSocket;

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
                        BluetoothSocket socket = serverSocket.accept();
                        Log.i("BluetoothServer", "6");
                        if (socket != null) {
                            Log.i("BluetoothServer", "Successfully connected to client!");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectionStatusTextView.setText("Connecté au client!");
                                }
                            });
                            connectedSocket = socket;
                            manageConnectedSocket(socket);

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

    private void manageConnectedSocket(final BluetoothSocket socket) {
        Thread connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    PrintWriter writer = new PrintWriter(outputStream, true);
                    String received;

                    while ((received = reader.readLine()) != null) {
                        Log.i("BluetoothClient", "Received from server: " + received);
                        // Handle server responses here
                    }

                    // End of stream detected, log and close socket
                    Log.i("BluetoothClient", "End of stream detected. Closing socket.");
                    socket.close();

                } catch (IOException e) {
                    Log.e("BluetoothClient", "Error managing connected socket", e);

                    try {
                        // Close the socket gracefully if there's an error
                        socket.close();
                    } catch (IOException closeException) {
                        Log.e("BluetoothClient", "Error closing socket", closeException);
                    }
                }
            }
        });
        connectionThread.start();
    }


    private void fetchDevices(final PrintWriter writer) {
        String url = "https://www.bde.enseeiht.fr/~bailleq/smartHouse/api/v1/devices/19";
        Log.i("marche","ta race");
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.e("marche","ta race");
                        try {
                            Log.e("marche","ta race");
                            for (int i = 0; i < response.length(); i++) {
                                Log.e("test", String.valueOf(i));
                                JSONObject device = response.getJSONObject(i);
                                String name = device.getString("NAME");
                                String brandModel = device.getString("BRAND") + " " + device.getString("MODEL");
                                String data = device.getString("DATA");
                                boolean state = device.getInt("STATE") == 1;
                                int autonomy = device.getInt("AUTONOMY");
                                int deviceId = device.getInt("ID");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("BluetoothServer", "Error fetching devices", error);
                writer.println("Error fetching devices.");
            }
        });

        requestQueue.add(jsonArrayRequest);
    }

    private void toggleDeviceState(int deviceId, final boolean turnOn, final Button button) {
        String action = "turnOnOff";
        StringRequest postRequest = new StringRequest(Request.Method.POST, postUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        button.setText(turnOn ? "ON" : "OFF");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
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
