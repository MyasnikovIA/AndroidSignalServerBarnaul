package ru.miacomsoft.androidsignalserverbarnaul.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.HashMap;

import ru.miacomsoft.androidsignalserverbarnaul.Lib.Sys;
import ru.miacomsoft.androidsignalserverbarnaul.MainActivity;


public class OnBootReceiver extends BroadcastReceiver {
    private HashMap<String, String> Setup = new HashMap<String, String>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            //  запуск сервиса
            // context.startService(new Intent(context, ServiceExample.class));

            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            Toast.makeText(context, "Boot Signal Server", Toast.LENGTH_SHORT).show();
            /*
            HashMap<String, String> Setup = Sys.readFile(context, "conf.ini");
            if (Setup != null) {
               // if (Setup.get("run").equals("1")) {
                     //Intent serviceLauncher = new Intent(context, ServiceExample.class);
                    //context.startService(serviceLauncher);
                     Intent serviceLauncher = new Intent(context, MainActivity.class);
                     context.startService(serviceLauncher);
               // }
            }
             */
        }
    }
}
