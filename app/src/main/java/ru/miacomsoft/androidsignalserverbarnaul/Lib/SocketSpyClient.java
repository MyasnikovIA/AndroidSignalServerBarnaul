package ru.miacomsoft.androidsignalserverbarnaul.Lib;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Клиент  коммутатора  установленого на белом IP адресе
 *
 *
 *         sp = new WhiteIPswitchClient(getBaseContext(),"128.0.24.172", 8266,"HUAWEI_ATI","","") {
 *             @Override
 *             public void commandLine(JSONObject jsonObject) {
 *                 Log.d("TEST", "Команда:" + jsonObject.toString());
 *             }
 *         };
 *         sp.start();
 *         sp.onMessage((String msg) -> {
 *             Log.d("TEST", "partial_results: " + msg);
 *         });
 *         sp.onMessage((JSONObject msg) -> {
 *             Log.d("TEST", "JSONObject msg: " + msg.toString());
 *         });
 *
 *
 */
public class SocketSpyClient {

    /// интерфейсы для вкладываемых методов
    public interface CallbackMethod {
        public void call(String msg);
    }
    public interface CallbackMethodJson {
        public void call(JSONObject msg);
    }

    Thread Thread1 = null;
    Thread ThreadRunClient = null;
    private String SERVER_IP = "128.0.24.172";
    private int SERVER_PORT = 8266;

    private InputStream is;
    private OutputStream os;
    private PrintWriter output;
    private BufferedReader input;
    private Socket socket;
    private OutputStream outputStream;
    private Boolean process = false;
    private String fromDeviceName = "";
    private String fromDevicePass = "";
    private Context context;
    private String DeviceName = ""; // имя этого устройства
    private String DevicePass = ""; // пароль этого устройства
    private String RouterPass = ""; // пороль коммутационного сервера
    private PackageManager packageManager = null;

    private ArrayList<CallbackMethod> callbackMethod = new ArrayList<CallbackMethod>();
    private ArrayList<CallbackMethodJson> callbackMethodJson = new ArrayList<CallbackMethodJson>();


    public SocketSpyClient(Context context, String host, int port, String DeviceName, String DevicePass, String RouterPass) {
        this.context = context;
        this.SERVER_IP = host;
        this.SERVER_PORT = port;
        this.DeviceName = DeviceName;
        this.DevicePass = DevicePass;
        this.RouterPass = RouterPass;
    }

    /**
     * Обработчик входящих сообщений для переопределения при инициализации экземпляра класса
     * @param jsonObject
     */
    public void commandLine(JSONObject jsonObject){
       //https://jar-download.com/artifacts/net.arnx/jsonic/1.3.10/source-code/net/arnx/jsonic/JSON.java
    }

    /***
     * Запуск слушателя команд из комутационного сервера
     */
    public void start() {
        // Toast.makeText(context, "Start Signal Client", Toast.LENGTH_SHORT).show();
        Thread1 = new Thread(new ThreadRun());
        Thread1.start();
    }

    /**
     * Остановка  клиента
     */
    public void stop() {
        process = false;
    }

    /**
     * отправка сообщения сигнальному серверу
     */
    public void send(String cmd) {
        try {
            outputStream.write((cmd + "\r\n\r\n").getBytes());
        } catch (IOException e) {
        }
    }


    /**
     * Привязка метода который будет вызываться при получении сообщения в формате String
     * @param method
     */
    public void onMessage(CallbackMethod method) {
        callbackMethod.add(method);
    }

    /**
     *  Привязка метода который будет вызываться при получении сообщения в формате JSON
     *
     * @param method
     */
    public void onMessage(CallbackMethodJson method) {
        callbackMethodJson.add(method);
    }



    /***
     * Обработка входящих команд от сервера
     */
    class ThreadComandLine implements Runnable {
        private JSONObject jsonObject ;

        public ThreadComandLine( JSONObject jsonObject  ) {
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            for (int i = 0; i < callbackMethodJson.size(); i++) {
                callbackMethodJson.get(i).call(jsonObject);
            }
            commandLine(jsonObject);
            // if (cmd.indexOf("info") != -1){
            //     try {
            //         outputStream.write(("connect:" + DeviceName+"\r\n").getBytes());
            //         outputStream.write(("===========\r\n").getBytes());
            //         outputStream.write(("get:applist\r\n").getBytes());
            //         outputStream.write(("run:com.android.chrome\r\n").getBytes());
            //         outputStream.write(("run:Chrome\r\n").getBytes());
            //         outputStream.write(("sey: сообщение которое будет озвучено уч\r\n").getBytes());
            //         outputStream.write(("===========\r\n").getBytes());
            //     } catch (IOException e) {
            //     }
            // }

        }
    }


    /***
     * Обработчик входящих команд (в паралельном потоке)
     */
    class ThreadTEstRead implements Runnable {
        @Override
        public void run() {
            int ind = 0;
            try {
                ind++;
                Log.d("LOG_TAG", socket.isConnected() + " ind = " + ind);
                InputStreamReader isr = new InputStreamReader(is);
                int charInt;
                StringBuffer sbTmp = new StringBuffer();
                ByteArrayOutputStream bufferRaw = new ByteArrayOutputStream();
                while (((charInt = isr.read()) !=-1)&&(process == true)) {
                    if (socket.isConnected() == false) {
                        break;
                    }

                    if (charInt == 0) {
                        Log.d("TEST", sbTmp.toString());
                        if ((sbTmp.toString().indexOf("{")==-1) && (sbTmp.toString().indexOf("[")==-1)){
                            for (int i = 0; i < callbackMethod.size(); i++) {
                                callbackMethod.get(i).call(sbTmp.toString());
                            }
                        } else {
                            //Log.d("TEST",  sbTmp.toString() );
                            try {
                                JSONObject jsonObject = new JSONObject( sbTmp.toString() );
                                new Thread(new ThreadComandLine(jsonObject)).start();
                            }catch (JSONException err){
                                Log.d("TEST", err.toString());
                            }
                        }
                        sbTmp.setLength(0);
                    } else {
                        // Log.d("TEST", String.valueOf(charInt)+"-"+(char) charInt);
                        sbTmp.append((char) charInt);
                    }
                }
                process = false;
                // tts.speak("Разрав соединения с сигнальным сервером", TextToSpeech.QUEUE_FLUSH, null);
                Log.d("TEST", "Disconnect");
            } catch (Exception e) {
                process = false;
                //e.printStackTrace();
            }
        }
    }

    /**
     *  Запуск  основного потока Сокет клиента
     */
    class ThreadExec implements Runnable {
        @Override
        public void run() {
            if (process == false) {
                process = true;
                try {
                    socket = new Socket(SERVER_IP, SERVER_PORT);
                    is = socket.getInputStream();
                    os = socket.getOutputStream();
                    outputStream = socket.getOutputStream();
                    outputStream.write((DeviceName + "\r\n" + DevicePass + "\r\n"+RouterPass+"\r\n\r\n").getBytes());
                    pause(3000);
                    // String message = input.readLine();
                    new Thread(new ThreadTEstRead()).start();
                    Log.d("TEST", " connect:" + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
                    // tts.speak("Установлена связь с сигнальным сервером", TextToSpeech.QUEUE_FLUSH, null);
                } catch (UnknownHostException ex) {
                    process = false;
                    Log.d("TEST", "Server not found: " + ex.getMessage());
                } catch (IOException ex) {
                    if (process == true) {
                        // tts.speak("нет связи с сигнальным сервером", TextToSpeech.QUEUE_FLUSH, null);
                        Log.d("TEST", "Нет связи с сигнальным сервером");
                    }
                    process = false;
                    Log.d("TEST", "I/O error: " + ex.getMessage());
                }
            }
        }
    }

    /**
     *  Контроллер постоянного перезапуска Сокет клиента
     */
    class ThreadRun implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (process == false) {
                    new Thread(new ThreadExec()).start();
                }
                pause(10000);
            }
        }
    }

    /**
     * Пауза при выполнении потока
     * @param ms
     */
    public static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.err.format("IOException: %s%n", e);
        }
    }

}
