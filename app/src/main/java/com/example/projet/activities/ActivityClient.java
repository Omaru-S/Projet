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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projet.R;
import com.example.projet.bluetooth.SocketManager;
import com.example.projet.utils.Constants;

import java.io.IOException;
import java.util.Set;

/**
 * Activité client pour gérer la connexion Bluetooth avec un serveur.
 */
public class ActivityClient extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket mmSocket;
    private Handler handler = new Handler();
    private TextView connectionStatusTextView;

    /**
     * Initialise l'activité, configure l'interface utilisateur et démarre la connexion Bluetooth.
     * @param savedInstanceState État de l'instance précédemment sauvegardé, ou null si l'activité est lancée pour la première fois.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        initializeBluetoothConnection();
    }

    /**
     * Configure et tente une connexion Bluetooth avec un appareil serveur prédéfini.
     */
    @SuppressLint("MissingPermission")
    private void initializeBluetoothConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e("ActivityClient", "Échec de l'obtention de l'adaptateur Bluetooth.");
            return;
        }

        // Récupère et connecte à un appareil Bluetooth appairé.
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            Log.d("ActivityClient", "Recherche d'appareils appairés.");
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("Redmi_serv")) {
                    try {
                        mmSocket = device.createRfcommSocketToServiceRecord(Constants.SERVEUR_UUID);
                        bluetoothAdapter.cancelDiscovery();
                        mmSocket.connect();
                        SocketManager.getInstance().setBluetoothSocket(mmSocket);
                        runOnUiThread(() -> connectionStatusTextView.setText("Connecté au serveur!"));
                        startActivity(new Intent(ActivityClient.this, ConnectedDevicesActivity.class));
                        Log.i("ActivityClient", "Connexion réussie au serveur.");
                        break;
                    } catch (IOException connectException) {
                        Log.e("ActivityClient", "Impossible de se connecter au serveur", connectException);
                        closeSocketOnError();
                    }
                }
            }
        } else {
            Log.e("ActivityClient", "Aucun appareil appairé trouvé.");
        }
    }

    /**
     * Ferme le socket Bluetooth en cas d'erreur lors de la tentative de connexion.
     */
    private void closeSocketOnError() {
        try {
            if (mmSocket != null) {
                mmSocket.close();
            }
        } catch (IOException closeException) {
            Log.e("ActivityClient", "Impossible de fermer le socket client", closeException);
        }
    }

    /**
     * Nettoie les ressources lors de la destruction de l'activité, notamment en fermant le socket Bluetooth.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (SocketManager.getInstance().getBluetoothSocket() != null) {
            SocketManager.getInstance().closeSocket();
            Log.i("ActivityClient", "Socket fermé lors de la destruction de l'activité.");
        }
    }
}
