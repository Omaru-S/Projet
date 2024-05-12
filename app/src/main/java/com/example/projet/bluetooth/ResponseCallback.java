package com.example.projet.bluetooth;

import com.android.volley.VolleyError;

import org.json.JSONArray;

public interface ResponseCallback {
    void onResponse(JSONArray response);
    void onError(VolleyError error);
}
