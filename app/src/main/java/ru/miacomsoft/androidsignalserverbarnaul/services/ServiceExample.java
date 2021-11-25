package ru.miacomsoft.androidsignalserverbarnaul.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class ServiceExample extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        // Toast.makeText(getApplicationContext(), "Boot Signal Server", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {


    }
}
