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

import com.android.volley.VolleyError;
import org.json.JSONArray;

/**
 * Interface définissant les callbacks pour les réponses des requêtes réseau.
 * Cette interface permet de gérer les réponses obtenues suite aux appels réseau ou les erreurs rencontrées.
 */
public interface ResponseCallback {
    /**
     * Callback appelé lorsqu'une réponse est reçue avec succès.
     * @param response La réponse sous forme de JSONArray reçue du serveur ou de l'API.
     */
    void onResponse(JSONArray response);

    /**
     * Callback appelé lorsqu'une erreur est survenue pendant la requête.
     * @param error L'erreur retournée par la requête, encapsulée dans un objet VolleyError.
     */
    void onError(VolleyError error);
}
