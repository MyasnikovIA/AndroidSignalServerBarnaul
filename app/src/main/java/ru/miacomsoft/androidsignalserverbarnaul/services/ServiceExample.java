package ru.miacomsoft.androidsignalserverbarnaul.services;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.text.format.Formatter;
import android.widget.Toast;



public class ServiceExample extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getApplicationContext(), "Boot Signal Server", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {


    }
}
