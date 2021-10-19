package com.giniapps.testapplication;

import android.util.Log;

public class B {

    final String TAG = B.class.getSimpleName();

    C c;

    public B(){
        System.out.println("hello from the other side");
    }

    public void setC(C c){
        this.c = c;
    }

    C getC() {
        return c;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finalize: B");
    }

    void checkC() throws InterruptedException {
        Log.d(TAG, "checkC: ");
        Thread.sleep(1000);
        getC().checkB();
    }

}
