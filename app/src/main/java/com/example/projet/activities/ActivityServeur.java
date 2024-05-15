/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.projet.activities;

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

/**
 * Classe serveur pour la gestion des connexions Bluetooth et des requêtes réseau pour le changement d'état des dispositifs connectés.
 */
public class ActivityServeur extends AppCompatActivity {
    private BluetoothServerSocket serverSocket;
    private Handler handler = new Handler();
    private TextView connectionStatusTextView;
    private static final String NAME = "BluetoothDemo";
    private RequestQueue requestQueue;

    /**
     * Initialise l'activité, les composants de l'interface utilisateur et le serveur Bluetooth.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serveur);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        requestQueue = Volley.newRequestQueue(this);
        initializeBluetoothServer();
    }

    /**
     * Initialise et démarre le serveur Bluetooth pour accepter les connexions.
     */
    @SuppressLint("MissingPermission")
    private void initializeBluetoothServer() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, Constants.SERVEUR_UUID);
            Thread acceptThread = new Thread(() -> {
                try {
                    BluetoothSocket socket = serverSocket.accept();
                    if (socket != null) {
                        Log.i("ActivityServeur", "Connexion réussie avec le client!");
                        runOnUiThread(() -> connectionStatusTextView.setText("Connecté au client!"));
                        manageConnectedSocket(socket);
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    Log.e("ActivityServeur", "Échec de la méthode accept du socket", e);
                }
            });
            acceptThread.start();
        } catch (IOException e) {
            Log.e("ActivityServeur", "Échec de la méthode listen du socket", e);
        }
    }

    /**
     * Gère la connexion Bluetooth avec le client.
     * @param socket Le socket Bluetooth connecté avec le client.
     */
    private void manageConnectedSocket(BluetoothSocket socket) {
        Thread thread = new Thread(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                OutputStream outputStream = socket.getOutputStream();
                String request;
                while ((request = reader.readLine()) != null) {
                    handleClientRequest(request, outputStream);
                }
            } catch (IOException e) {
                Log.e("ActivityServeur", "Erreur de gestion du socket", e);
            }
        });
        thread.start();
    }

    /**
     * Traite les requêtes envoyées par le client.
     * @param request La requête envoyée par le client.
     * @param outputStream Le flux de sortie pour répondre au client.
     */
    private void handleClientRequest(String request, OutputStream outputStream) {
        if (request.endsWith("CHANGE_STATE")) {
            int deviceId = Integer.parseInt(request.replace("CHANGE_STATE", "").trim());
            Log.d("ActivityServeur", "Changement d'état demandé pour l'appareil " + deviceId);
            changeDeviceState(deviceId, outputStream);
        }
        if ("GET_DEVICES".equals(request.trim())) {
            getDevices(new ResponseCallback() {
                @Override
                public void onResponse(JSONArray response) {
                    sendJsonResponse(response, outputStream);
                }

                @Override
                public void onError(VolleyError error) {
                    sendErrorResponse(outputStream);
                }
            });
        }
    }

    /**
     * Envoie les données des appareils au client en format JSON.
     * @param response La réponse JSON contenant les données des appareils.
     * @param outputStream Le flux de sortie pour envoyer la réponse.
     */
    private void sendJsonResponse(JSONArray response, OutputStream outputStream) {
        try {
            String jsonResponse = response.toString();
            int chunkSize = 512; // Taille de morceau appropriée
            for (int start = 0; start < jsonResponse.length(); start += chunkSize) {
                int end = Math.min(jsonResponse.length(), start + chunkSize);
                String chunk = jsonResponse.substring(start, end);
                outputStream.write((chunk + "\nEND_OF_THE_CHUNK\n").getBytes());
            }
            outputStream.write("END_OF_MESSAGE\n".getBytes());
            outputStream.flush();
        } catch (IOException e) {
            Log.e("ActivityServeur", "Échec de l'envoi de JSONArray", e);
        }
    }

    /**
     * Envoie une réponse d'erreur si la récupération des données échoue.
     * @param outputStream Le flux de sortie pour envoyer l'erreur.
     */
    private void sendErrorResponse(OutputStream outputStream) {
        try {
            outputStream.write("Erreur lors de la récupération des appareils".getBytes());
            outputStream.flush();
        } catch (IOException e) {
            Log.e("ActivityServeur", "Échec de l'envoi du message d'erreur", e);
        }
    }

    /**
     * Récupère les données des appareils depuis un serveur distant.
     * @param callback Le callback pour gérer les réponses ou erreurs de la requête.
     */
    private void getDevices(ResponseCallback callback) {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, Constants.completeURL, null,
                response -> processDevicesResponse(response, callback),
                error -> {
                    Log.e("ActivityServeur", "Erreur lors de la récupération des données : " + error.toString());
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
        requestQueue.add(jsonArrayRequest);
    }

    /**
     * Traite la réponse du serveur distant et extrait les données des appareils.
     * @param response La réponse JSON contenant les données des appareils.
     * @param callback Le callback pour envoyer les données extraites.
     */
    private void processDevicesResponse(JSONArray response, ResponseCallback callback) {
        try {
            JSONArray newJsonArray = new JSONArray();
            for (int i = 0; i < response.length(); i++) {
                JSONObject device = response.getJSONObject(i);
                JSONObject newDevice = createDeviceJson(device);
                newJsonArray.put(newDevice);
            }
            callback.onResponse(newJsonArray);
        } catch (JSONException e) {
            Log.e("ActivityServeur", "Erreur d'analyse JSON", e);
            if (callback != null) {
                callback.onError(new VolleyError(e));
            }
        }
    }

    /**
     * Crée un objet JSON pour un appareil spécifique.
     * @param device Le JSONObject original contenant les détails de l'appareil.
     * @return Un nouveau JSONObject avec les détails formatés.
     * @throws JSONException Si une erreur de formatage survient.
     */
    private JSONObject createDeviceJson(JSONObject device) throws JSONException {
        JSONObject newDevice = new JSONObject();
        newDevice.put("NAME", device.getString("NAME"));
        newDevice.put("BRAND", device.getString("BRAND"));
        newDevice.put("MODEL", device.getString("MODEL"));
        newDevice.put("DATA", device.getString("DATA"));
        newDevice.put("STATE", device.getInt("STATE"));
        newDevice.put("AUTONOMY", device.getInt("AUTONOMY"));
        newDevice.put("ID", device.getInt("ID"));
        return newDevice;
    }

    /**
     * Envoie une requête POST pour changer l'état d'un appareil spécifique.
     * @param deviceId L'identifiant de l'appareil dont l'état doit être modifié.
     * @param outputStream Le flux de sortie pour envoyer la réponse au client.
     */
    private void changeDeviceState(int deviceId, OutputStream outputStream) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.url,
                response -> {
                    try {
                        outputStream.write(("POST_SUCCESS\n" + response + "\n").getBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        Log.e("ActivityServeur", "Échec de l'envoi de la réponse POST_SUCCESS au client", e);
                    }
                },
                error -> {
                    try {
                        outputStream.write("POST_FAILED\n".getBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        Log.e("ActivityServeur", "Échec de l'envoi du message POST_FAILED au client", e);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("deviceId", String.valueOf(deviceId));
                params.put("houseId", Constants.houseId);
                params.put("action", "turnOnOff");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        requestQueue.add(stringRequest);
    }
}
