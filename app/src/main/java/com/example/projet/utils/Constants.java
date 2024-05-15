package com.example.projet.utils;

import java.util.UUID;

public class Constants {
    public static final String url = "https://www.bde.enseeiht.fr/~bailleq/smartHouse/api/v1/devices/";
    public static final String houseId = "35";
    public static final String completeURL = url + houseId;
    public static final UUID SERVEUR_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
    public static final UUID CLIENT_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

}