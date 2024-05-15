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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projet.R;

/**
 * Activité principale de l'application, permettant de sélectionner le mode serveur ou client.
 * Cette activité offre une interface utilisateur pour naviguer vers les fonctionnalités spécifiques du serveur ou du client.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation et gestion des interactions avec les boutons de l'interface utilisateur.
        try {
            Button serverButton = findViewById(R.id.serverButton);
            serverButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("MainActivity", "Clic sur le bouton serveur");
                    // Lancement de l'activité serveur.
                    Intent intent = new Intent(MainActivity.this, ActivityServeur.class);
                    startActivity(intent);
                }
            });

            Button clientButton = findViewById(R.id.clientButton);
            clientButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("MainActivity", "Clic sur le bouton client");
                    // Lancement de l'activité client.
                    Intent intent = new Intent(MainActivity.this, ActivityClient.class);
                    startActivity(intent);
                }
            });

            Log.d("MainActivity", "Les boutons ont été initialisés avec succès");
        } catch (Exception e) {
            Log.e("MainActivity", "Erreur lors de l'initialisation des boutons", e);
        }
    }
}
