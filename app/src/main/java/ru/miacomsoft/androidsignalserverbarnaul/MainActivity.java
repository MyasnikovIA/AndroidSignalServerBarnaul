package ru.miacomsoft.androidsignalserverbarnaul;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.Formatter;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.HashMap;


import ru.miacomsoft.androidsignalserverbarnaul.Lib.Sys;
import ru.miacomsoft.androidsignalserverbarnaul.services.HttpSrv;


public class MainActivity extends AppCompatActivity {

/*

<center>
<script type="text/javascript" >
var xhr = new XMLHttpRequest()
xhr.open( 'GET','http://128.0.24.172:8266/pop=test', true )
xhr.onreadystatechange = function() {
  if (xhr.readyState != 4) { return; }
  if (xhr.status === 200) {
    console.log('result', xhr.responseText)
  } else {
    console.log('err', xhr.responseText)
  }
}
xhr.send()
</script>
</center>

http://128.0.24.172:8266/push=test&msg=eeeeeeeeeeeeeeeeeeee
http://128.0.24.172:8266/pop=test
{"push":"test2","msg":"sadflkjasdl;fjas;ldfjl;asjfl;as"}
{"exit":"exit"}
exit
list
{"pop":"test"}
{"send":"test","msg":"sadflkjasdl;fjas;ldfjl;asjfl;as"}
{"send":"test3","msg":"sadflkjasdl;fjas;ldfjl;asjfl;as"}

*/
    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyApp::MyWakelockTag");
        wakeLock.acquire();

        // Разблокировать экран
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

        TextView text = (TextView) findViewById(R.id.textView);
        boolean onConnect = false;
        String ipAddress="";
        // Подключение к WIFI точке деоступа
        WifiConfiguration wifiConfig = new WifiConfiguration();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        // wifiConfig.SSID = String.format("\"%s\"", "a616mm");
        // wifiConfig.SSID = String.format("\"%s\"", "ELTEX-87A2"); // Имя WIFI точки доступа
        // wifiConfig.preSharedKey = String.format("\"%s\"", "XXXXXXXX"); // Пароль для полдключения к точки доступа

        wifiConfig.SSID = String.format("\"%s\"", "ELTEX-87A2"); // Имя WIFI точки доступа
        wifiConfig.preSharedKey = String.format("\"%s\"", "XXXXXXXX"); // Пароль для полдключения к точки доступа

        //wifiManager.disconnect();
        //int netId = wifiManager.addNetwork(wifiConfig);
        //wifiManager.enableNetwork(netId, true);
        //wifiManager.reconnect();
        while (onConnect == false) {
            WifiInfo info = wifiManager.getConnectionInfo();
            int ip = info.getIpAddress();
            if (ip != 0) {
                ipAddress = Formatter.formatIpAddress(ip);
                text.setText(ipAddress);
                text.append("\nSSID WIFI: ");
                text.append(info.getSSID());
                text.append("\nMAC DEVICE: ");
                text.append(info.getMacAddress());
                onConnect = true;
            } else {
                text.append("\n.");
            }
          //  pause(3000); // пауза 3 секунды
        }


        HashMap<String, String> Setup = Sys.readFile(this, "conf.ini");
        Setup.put("UserName", "");
        Setup.put("UserPass", "");
        Setup.put("UserPort", "8266");
        Setup.put("Interval", "10000");
        Setup.put("DefaultHost", "");
        Setup.put("run", "1");
        HttpSrv http = new HttpSrv(this);
        http.Start(Setup);

    }

}