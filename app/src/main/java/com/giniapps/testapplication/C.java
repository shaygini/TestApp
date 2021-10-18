package com.giniapps.testapplication;

import android.util.Log;

public class C {

    final String TAG = C.class.getSimpleName();

    int counter = 0;

    B b;

    public C() {

    }

    public void setB(B b) {
        this.b = b;
    }

    public B getB() {
        return b;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finalize: C");
    }

    void checkB() throws InterruptedException {
        Log.d(TAG, "checkB: ");
        counter = counter + 1;
        if(counter < 5) {
            getB().checkC();
        }
    }
}
