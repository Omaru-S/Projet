package com.example.projet.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projet.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button serverButton = findViewById(R.id.serverButton);
        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start the ActivityServeur
                Intent intent = new Intent(MainActivity.this, ActivityServeur.class);
                startActivity(intent);
            }
        });

        Button clientButton = findViewById(R.id.clientButton);
        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start the ActivityClient
                Intent intent = new Intent(MainActivity.this, ActivityClient.class);
                startActivity(intent);
            }
        });
    }
}
