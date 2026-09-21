package com.example.ronda;

import android.app.Application;

public class RondaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.initialize(this);
    }
}