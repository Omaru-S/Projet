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

/**
 * Gère l'activité liée aux appareils connectés, permettant de visualiser et interagir avec ces derniers.
 */
public class ConnectedDevicesActivity extends AppCompatActivity {

    private LinearLayout devicesLayout;
    private Handler handler = new Handler();
    private Runnable updateDevicesRunnable;

    /**
     * Initialise l'activité, définit la vue, et commence la mise à jour périodique des données des appareils.
     * @param savedInstanceState L'état de l'instance sauvegardé.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);
        devicesLayout = findViewById(R.id.devicesLayout);

        requestDeviceData();

        updateDevicesRunnable = new Runnable() {
            @Override
            public void run() {
                requestDeviceData();
                handler.postDelayed(this, 10000);
                Log.d("ConnectedDevicesActivity", "Mise à jour régulière des données des appareils demandée.");
            }
        };
        handler.post(updateDevicesRunnable);
    }

    /**
     * Envoie une demande de données au serveur via un socket Bluetooth.
     */
    private void requestDeviceData() {
        BluetoothSocket socket = SocketManager.getInstance().getBluetoothSocket();
        if (socket != null) {
            if(socket.isConnected()){
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write("GET_DEVICES\n".getBytes());
                    outputStream.flush();
                    Log.d("ConnectedDevicesActivity", "Demande envoyée au serveur.");
                    listenForResponse(socket);
                } catch (IOException e) {
                    Log.e("ConnectedDevicesActivity", "Échec de l'envoi de la demande", e);
                }
            } else {
                Log.e("ConnectedDevicesActivity", "Le socket n'est pas connecté.");
            }
        }
    }

    /**
     * Écoute les réponses du serveur et traite les données JSON reçues.
     * @param socket Le socket Bluetooth utilisé pour la communication.
     */
    private void listenForResponse(BluetoothSocket socket) {
        Thread thread = new Thread(() -> {
            Log.d("ConnectedDevicesActivity", "Thread démarré, en attente de réponse.");
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if("POST_SUCCESS".equals(line.trim()) || "POST_FAILED".equals(line.trim())){
                        Log.d("ConnectedDevicesActivity", "Succès de la requête POST détectée.");
                        requestRetry();
                    }
                    else if ("END_OF_MESSAGE".equals(line.trim())) {
                        Log.d("ConnectedDevicesActivity", "Fin du message détectée.");
                        break;
                    } else if ("END_OF_THE_CHUNK".equals(line.trim())) {
                        Log.d("ConnectedDevicesActivity", "Fin du segment détectée.");
                    } else {
                        response.append(line);
                    }
                }
                try {
                    JSONArray jsonArray = new JSONArray(response.toString());
                    runOnUiThread(() -> onResponse(jsonArray));
                } catch (JSONException e) {
                    Log.e("ConnectedDevicesActivity", "Format JSON invalide reçu, nouvelle tentative...", e.getCause());
                    requestRetry();
                }
            } catch (IOException e) {
                Log.e("ConnectedDevicesActivity", "Erreur de lecture du socket", e);
            }
        });
        thread.start();
    }

    /**
     * Programme une nouvelle tentative en cas d'échec de la demande précédente.
     */
    private void requestRetry() {
        Log.d("ConnectedDevicesActivity", "Nouvelle tentative de demande de données suite à un échec précédent.");
        handler.postDelayed(this::requestDeviceData, 300);
    }

    /**
     * Traite la réponse JSON et met à jour l'interface utilisateur en conséquence.
     * @param response Le tableau JSON contenant les données des appareils.
     */
    private void onResponse(JSONArray response) {
        Log.d("ConnectedDevicesActivity", "Traitement de la réponse JSON sur le thread UI. Nombre d'appareils: " + response.length());
        devicesLayout.removeAllViews();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject device = response.getJSONObject(i);

                String name = device.optString("NAME", "Nom inconnu");
                String brand = device.optString("BRAND", "Marque inconnue");
                String model = device.optString("MODEL", "");
                String data = device.optString("DATA", "Aucune donnée disponible");
                int state = device.optInt("STATE", 0);
                int autonomy = device.optInt("AUTONOMY", -1);
                int deviceId = device.optInt("ID", -1);

                View deviceView = createDeviceView(name, brand + " " + model, data, autonomy, state, deviceId);
                devicesLayout.addView(deviceView);
            }
            Log.d("ConnectedDevicesActivity", "Tous les appareils ont été ajoutés au layout.");
        } catch (JSONException e) {
            Log.e("ConnectedDevicesActivity", "Erreur d'analyse JSON", e);
            requestRetry();
        }
    }

    /**
     * Crée une vue pour un appareil spécifique et l'ajoute à l'interface utilisateur.
     * @param name Le nom de l'appareil.
     * @param brandModel La marque et le modèle de l'appareil.
     * @param data Les données supplémentaires de l'appareil.
     * @param autonomy L'autonomie de l'appareil.
     * @param isOn L'état de l'appareil (allumé ou éteint).
     * @param deviceId L'identifiant de l'appareil.
     * @return La vue créée pour l'appareil.
     */
    private View createDeviceView(String name, String brandModel, String data, int autonomy, int isOn, int deviceId) {
        Context context = this;
        RelativeLayout layout = new RelativeLayout(context);
        layout.setPadding(16, 16, 16, 16);

        TextView nameTextView = new TextView(context);
        nameTextView.setId(View.generateViewId());
        nameTextView.setText(name);
        nameTextView.setTextSize(18);
        nameTextView.setTypeface(null, Typeface.BOLD);
        RelativeLayout.LayoutParams paramsName = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsName.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        paramsName.addRule(RelativeLayout.ALIGN_PARENT_START);
        layout.addView(nameTextView, paramsName);

        TextView brandModelTextView = new TextView(context);
        brandModelTextView.setId(View.generateViewId());
        String autonomyText = autonomy == -1 ? "Pas de batterie" : "Autonomie: " + autonomy + "%";
        brandModelTextView.setText(brandModel);
        brandModelTextView.setTextSize(16);
        RelativeLayout.LayoutParams paramsBrandModel = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsBrandModel.addRule(RelativeLayout.BELOW, nameTextView.getId());
        paramsBrandModel.addRule(RelativeLayout.ALIGN_START, nameTextView.getId());
        layout.addView(brandModelTextView, paramsBrandModel);

        TextView dataTextView = new TextView(context);
        dataTextView.setTextSize(16);
        dataTextView.setText(autonomyText + " - " + data);
        RelativeLayout.LayoutParams paramsData = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsData.addRule(RelativeLayout.BELOW, brandModelTextView.getId());
        paramsData.addRule(RelativeLayout.ALIGN_START, brandModelTextView.getId());
        layout.addView(dataTextView, paramsData);

        Button stateButton = new Button(context);
        stateButton.setId(View.generateViewId());
        stateButton.setText(isOn == 1 ? "ON" : "OFF");
        stateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDeviceState(deviceId, isOn == 1 ? 0 : 1, stateButton);
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

    /**
     * Bascule l'état d'un appareil entre allumé et éteint.
     * @param deviceId L'identifiant de l'appareil.
     * @param turnOn Indique si l'appareil doit être allumé (1) ou éteint (0).
     * @param button Le bouton qui déclenche l'action.
     */
    private void toggleDeviceState(int deviceId, int turnOn, Button button) {
        String command = deviceId + " CHANGE_STATE\n";
        BluetoothSocket socket = SocketManager.getInstance().getBluetoothSocket();
        if (socket != null && socket.isConnected()) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(command.getBytes());
                outputStream.flush();
                Log.d("ConnectedDevicesActivity", "Demande de changement d'état envoyée pour l'appareil ID: " + deviceId);
            } catch (IOException e) {
                Log.e("ConnectedDevicesActivity", "Échec de l'envoi de la demande de changement d'état", e);
            }
        }
        button.setText(turnOn == 1 ? "ON" : "OFF");
    }

    /**
     * Nettoie et retire les callbacks lors de la destruction de l'activité.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateDevicesRunnable != null) {
            handler.removeCallbacks(updateDevicesRunnable);
            Log.d("ConnectedDevicesActivity", "Runnable d'actualisation des appareils retiré");
        }
    }
}
