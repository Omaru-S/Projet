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

package com.example.projet.utils;

import java.util.UUID;

/**
 * Fournit des constantes utilisées pour configurer les requêtes au serveur.
 * Cette classe contient les URL de base, identifiants et autres constantes nécessaires pour interagir avec l'API du serveur.
 */
public class Constants {
    /** L'URL de base pour les requêtes API concernant les dispositifs. */
    public static final String url = "https://www.bde.enseeiht.fr/~bailleq/smartHouse/api/v1/devices/";

    /** L'identifiant de la maison utilisé dans les requêtes API. */
    public static final String houseId = "35";

    /** L'URL complète combinant l'URL de base et l'identifiant de la maison. */
    public static final String completeURL = url + houseId;

    /** L'UUID unique utilisé pour identifier le serveur dans les communications réseau. */
    public static final UUID SERVEUR_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
}
