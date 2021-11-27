package ru.miacomsoft.androidsignalserverbarnaul;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Set;

import ru.miacomsoft.androidsignalserverbarnaul.Lib.Sys;
import ru.miacomsoft.androidsignalserverbarnaul.Lib.WebServer;


public class MainActivity extends AppCompatActivity {
    WebServer webServer;


    protected PowerManager.WakeLock mWakeLock;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyApp::MyWakelockTag");
        this.mWakeLock.acquire();

        // Разблокировать экран
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

        TextView text = (TextView) findViewById(R.id.textView);
        boolean onConnect = false;
        String ipAddress = "";
        WifiConfiguration wifiConfig = new WifiConfiguration();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiConfig.SSID = String.format("\"%s\"", "ELTEX-87A2"); // Имя WIFI точки доступа
        wifiConfig.preSharedKey = String.format("\"%s\"", "XXXXXXXX"); // Пароль для полдключения к точки доступа
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
        }

        try {
            // Запуск вэб сервера
            webServer = new WebServer(this);
            webServer.Start(8266);

            // перехвать полсе чтения очередного сообщения от клиента
            webServer.onTerminalLoop((WebServer.TerminalStruct term)->{
                if(term.countQuery == 1) {
                    //String DeviceNameClient = rowText.replace("\n", "").replace("\r", "");
                    try {
                        term.write("\r\n{\"Register\":\"" + term.DeviceNameClient + "\"}\r\n");
                        rebootOneDevice(term.DeviceNameClient);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Sys.DeviceIStream.put(term.DeviceNameClient, new InputStreamReader(term.inputStream));
                    Sys.DeviceOStream.put(term.DeviceNameClient, term.outputStream);
                    Sys.DeviceSocket.put(term.DeviceNameClient, term.socket);
                    Sys.DevicePass.put(term.DeviceNameClient, term.DevicePassClient);
                } else {
                    String cmd = term.headText.replace("\r","").replace("\n","");
                    if ((cmd.indexOf("exit") != -1) && (cmd.length() == 4)
                            || (cmd.toString().indexOf("{\"exit\":\"exit\"}") != -1)
                    ) {
                        rebootOneDevice(term.DeviceNameClient);
                        term.exit();
                        return;
                    }

                    if ((cmd.indexOf("ping") != -1) && (cmd.length() == 4)) {
                        term.write("ping\r\n");
                        term.write(0);
                        return;
                    }

                    // получить список подключенных устройств
                    if ((cmd.indexOf("list") != -1) && (cmd.length() == 4)) {
                        Set<String> keys = Sys.DeviceSocket.keySet();
                        term.write("\r\n");
                        term.write("[");
                        int ind = 0;
                        for (String key : keys) {
                            ind++;
                            if (ind>1) {
                                term.write(",\""+key + "\"");
                            } else {
                                term.write("\""+key + "\"");
                            }
                        }
                        term.write("]\r\n");
                        term.write(0);
                        return;
                    }
                    JSONObject jsonObject = null;
                    if ((cmd.indexOf("{") != -1) && (cmd.indexOf("}") != -1)) {
                        try {
                            jsonObject = new JSONObject(cmd);
                        } catch (JSONException e) {
                            jsonObject = new JSONObject();
                            jsonObject.put("message",cmd);
                        }
                    } else {
                        jsonObject = new JSONObject();
                        int nimLine = 0;
                        for (String TitleLine :term.headText.split("\r")) {
                            if (TitleLine.split(":").length > 0) {
                                TitleLine = TitleLine.replace("\n", "");
                                String val = TitleLine.split(":")[0];
                                val = val.replace(" ", "_");
                                if (val.length() > 0) {
                                    jsonObject.put(val, TitleLine.replace(TitleLine.split(":")[0] + ":", ""));
                                }
                            } else {
                                nimLine++;
                                jsonObject.put("" + nimLine, TitleLine);
                            }
                        }
                    }
                    Log.d("LOG_TAG","jsonObject.toString() "+jsonObject.toString());

                    // отправка сообщения для устройства
                    if (jsonObject.has("push") == true) {
                        jsonObject.put("from", term.DeviceNameClient);
                        term.DeviceNameSendTo = jsonObject.getString("push");
                        jsonObject.remove("push");
                        Sys.MESSAGE_LIST.put(term.DeviceNameSendTo,jsonObject.toString());
                        term.write("{\"ok\":true}\r\n");
                        term.write(0);
                        return;
                    }

                    // получение сообщения для устройства
                    if (jsonObject.has("pop") == true) {
                        String deviceName = jsonObject.getString("pop");
                        if (Sys.MESSAGE_LIST.containsKey(deviceName) == true) {
                            term.write(Sys.MESSAGE_LIST.get(deviceName).toString() + "\r\n");
                            Sys.MESSAGE_LIST.remove(deviceName);
                        } else {
                            term.write("{\"ok\":false,\"error\":\"no message\"}\r\n");
                        }
                        term.write(0);
                        return;
                    }

                    // прямая отправка сообщения для устройства, если оно в сети
                    if (jsonObject.has("send") == true) {
                        term.DeviceNameSendTo = jsonObject.getString("send");
                        jsonObject.remove("send");
                        jsonObject.put("from", term.DeviceNameClient);
                        try {
                            OutputStream osDst = Sys.DeviceOStream.get(term.DeviceNameSendTo);
                            osDst.write((jsonObject.toString()).getBytes());
                            osDst.write(0);
                            osDst.flush();
                            term.write("{\"ok\":true}");
                        } catch (IOException e) {
                            term.write("{\"ok\":false,\"error\":\"send '" + term.DeviceNameSendTo + "' error\"}\r\n");
                        }
                        term.write(0);
                        return;
                    }


                    if (jsonObject.has("stream") == true) {
                        jsonObject.put("from", term.DeviceNameClient);
                        term.DeviceNameSendTo = jsonObject.getString("stream");
                        jsonObject.remove("stream");
                        if (Sys.DeviceOStream.containsKey(term.DeviceNameSendTo) == true) {
                            OutputStream osDst = Sys.DeviceOStream.get(term.DeviceNameSendTo);
                            InputStreamReader isDst = Sys.DeviceIStream.get(term.DeviceNameSendTo);
                            Socket socDst = Sys.DeviceSocket.get(term.DeviceNameSendTo);
                            InputStreamReader isr = new InputStreamReader(term.inputStream);
                            int charIn;
                            int charOu;
                            // Переписать
                            while (term.socket.isConnected()) {
                                if ((charIn = isr.read()) !=-1 ) {
                                    osDst.write(charIn);
                                }
                                if ((charOu = isDst.read()) !=-1 ) {
                                    term.write(charOu);
                                }
                            }
                            term.write(jsonObject.toString());
                            term.write(0);
                            osDst.flush();
                        } else {
                            term.write("{\"ok\":false,\"error\":\"device '" + term.DeviceNameSendTo + "' not found\"}\r\n");
                        }
                        term.write(0);
                        return;
                    }
                    // Если получатель определен, тогда все остальные сообщения отпр
                    if (term.DeviceNameSendTo.length()>0) {
                        jsonObject.put("from", term.DeviceNameClient);
                        if (Sys.DeviceOStream.containsKey(term.DeviceNameSendTo) == true) {
                            OutputStream osDst = Sys.DeviceOStream.get(term.DeviceNameSendTo);
                            osDst.write(jsonObject.toString().getBytes());
                            osDst.write(0);
                            osDst.flush();
                        } else {
                            term.write("{\"ok\":false,\"error\":\"device '" +term.DeviceNameSendTo+ "' not found\"}\r\n");
                        }
                        term.write(0);
                        return;
                    }

                }
                Log.d("LOG_TAG", term.headText);
                //term.write("Отправка ответа:"+term.headText+":");
            });

            // обработка HTML запроса (любых не описанных)
            webServer.onPage((JSONObject Head,WebServer.Response res)->{
                JSONObject jsonObj = new JSONObject();

                if (Head.has("ListDevice") == true) {
                    Set<String> keys = Sys.DeviceOStream.keySet();
                    jsonObj.put("ok", "true");
                    int ind = 0;
                    for (String key : keys) {
                        Socket soc = Sys.DeviceSocket.get(key);
                        if (soc.isConnected()) {
                            ind++;
                            jsonObj.put("Dev" + ind, key);
                        }
                    }
                    res.JSON(jsonObj.toString());
                    return;
                }
                // оставить сообщение для устройства
                if (Head.has("push") == true) {
                    String devName = Head.getString("push");
                    Head.remove("push");
                    Sys.MESSAGE_LIST.put(devName,Head.toString());
                    res.JSON("{\"ok\":true}");
                    return;
                }

                if (Head.has("pop") == true) {
                    String devName = Head.getString("pop");
                    Head.remove("pop");
                    if (Sys.MESSAGE_LIST.containsKey(devName) == true) {
                        res.JSON(Sys.MESSAGE_LIST.get(devName));
                        Sys.MESSAGE_LIST.remove(devName);
                    } else {
                        res.JSON( "{\"ok\":false,\"error\":\"no message\"}");
                    }
                    return;
                }

                if (Head.has("send") == true) {
                    String devName = Head.getString("send");
                    Head.remove("send");
                    if (Sys.DeviceOStream.containsKey(devName) == true) {
                        try {
                            OutputStream osDst = Sys.DeviceOStream.get(devName);
                            osDst.write((Head.toString()).getBytes());
                            osDst.write(0);
                            osDst.flush();
                            res.JSON("{\"ok\":true}");
                        } catch (IOException e) {
                            res.JSON("{\"ok\":false,\"error\":\"send '" + devName + "' error\"}\r\n");
                            e.printStackTrace();
                        }
                    } else {
                        res.JSON("{\"ok\":false,\"error\":\"device '" + devName + "' not found\"}\r\n");
                    }
                    return;
                }
                res.JSON(Head.toString());
                // res.JSON("{\"OK\":\"--"+res.Property.getString("Query")+"----\"}");
            });

            // Обработка URL пути
            webServer.onPage("index.html",".HTML",(JSONObject Head, WebServer.Response res)->{
                res.Head();
                res.Body("<center><h1>Кросс доменный коммутатор</h1></center>" +
                        "<center><a href='https://github.com/MyasnikovIA/AndroidSignalServerBarnaul'>SRC GitHub</a></center>");
                res.End();
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void rebootOneDevice(String DevName) {
        Set<String> keys = Sys.DeviceOStream.keySet();
        for (String key : keys) {
            if (key.equals(DevName) == true) {
                OutputStream osDst = Sys.DeviceOStream.get(key);
                Socket soc = Sys.DeviceSocket.get(key);
                if (soc.isConnected()) {
                    try {
                        osDst.write(" Kill connect \r\n".getBytes());
                        osDst.write(0);
                        soc.shutdownInput();
                        soc.shutdownOutput();
                        soc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Sys.DeviceOStream.remove(key);
                Sys.DeviceIStream.remove(key);
                Sys.DeviceSocket.remove(key);
                Sys.DevicePass.remove(key);
            }
        }
    }
    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }

    /**
     * Функция для обмена данными между потоками
     *
     * @param isr
     * @param os
     * @param isDst
     * @param osDst
     */
    public void dataExchangeTr(Socket socketSrc, InputStreamReader isr, OutputStream os, Socket socDst, InputStreamReader isDst, OutputStream osDst) {
        int charInt;
        // дописать в создане потока на прямую передачу потоков
        try {
            while (true) {
                if ((charInt = isr.read()) > 0) {
                    osDst.write(charInt);
                }
                if ((charInt = isDst.read()) > 0) {
                    os.write(charInt);
                }
                if (socketSrc.isConnected() == false) {
                    break;
                }
                if (socDst.isConnected() == false) {
                    break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}