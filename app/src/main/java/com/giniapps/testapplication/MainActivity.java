package com.giniapps.testapplication;

import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    B b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b = new B();
        C c = new C();
        b.setC(c);
        c.setB(b);

        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    b.checkC();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(10000);
                    Log.d("MainActivity", "run: after sleep 10 seconds");
                    //b = null;
                    System.gc ();
                    System.runFinalization ();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();


        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(20000);
                    Log.d("MainActivity", "run: after sleep 20 seconds");
                    b = null;
                    System.gc ();
                    System.runFinalization ();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}