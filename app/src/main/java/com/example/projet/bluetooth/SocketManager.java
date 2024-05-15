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

package com.example.projet.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

/**
 * Gère les connexions Bluetooth en encapsulant le socket Bluetooth.
 * Permet de créer une instance unique (singleton) pour centraliser la gestion du socket Bluetooth à travers l'application.
 */
public class SocketManager {
    private BluetoothSocket bluetoothSocket;
    private static final SocketManager instance = new SocketManager();

    /** Constructeur privé pour empêcher l'instanciation externe et garantir le modèle singleton. */
    private SocketManager() {
        Log.d("SocketManager", "Instance de SocketManager créée");
    }

    /**
     * Retourne l'instance unique du gestionnaire de socket.
     * @return l'unique instance de SocketManager
     */
    public static SocketManager getInstance() {
        Log.i("SocketManager", "Obtention de l'instance de SocketManager");
        return instance;
    }

    /**
     * Retourne le socket Bluetooth actuel.
     * @return le socket Bluetooth actuellement configuré
     */
    public BluetoothSocket getBluetoothSocket() {
        Log.i("SocketManager", "Obtention du socket Bluetooth");
        return bluetoothSocket;
    }

    /**
     * Configure le socket Bluetooth avec un nouveau socket.
     * @param socket le nouveau socket Bluetooth à utiliser
     */
    public void setBluetoothSocket(BluetoothSocket socket) {
        if (socket != null) {
            Log.i("SocketManager", "Le socket Bluetooth est configuré");
        } else {
            Log.e("SocketManager", "Tentative de configuration d'un socket Bluetooth null");
        }
        this.bluetoothSocket = socket;
    }

    /**
     * Ferme le socket Bluetooth s'il est ouvert.
     * Attrape et logue les exceptions pour éviter les crashs dus à des fermetures de socket échouées.
     */
    public void closeSocket() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                Log.i("SocketManager", "Socket Bluetooth fermé avec succès");
            } catch (IOException e) {
                Log.e("SocketManager", "Impossible de fermer le socket", e);
            }
        } else {
            Log.d("SocketManager", "Aucun socket à fermer");
        }
    }
}
