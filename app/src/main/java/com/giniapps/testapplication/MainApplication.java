package com.giniapps.testapplication;

import android.app.Application;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainApplication extends Application {

    public FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle eventParams = new Bundle();
        eventParams.putString("name", "param name");
        eventParams.putString("screen", "param screen");

        firebaseAnalytics.logEvent("custom_log_analytics", eventParams );
    }
}
