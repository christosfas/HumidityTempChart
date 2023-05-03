package com.example.humiditytempchart;

import java.util.Collections;
import java.util.List;

public class User {

    public String email;
    public String id;
    public List<String> mac;


    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String id, String email, List<String> mac) {
        this.id = id;
        this.email = email;
        this.mac = mac;
    }

    public User(String id, String email, String mac) {
        this.id = id;
        this.email = email;
        this.mac = Collections.singletonList(mac);
    }

}
