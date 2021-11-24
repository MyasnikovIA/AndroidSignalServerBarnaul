package ru.miacomsoft.androidsignalserverbarnaul.Lib;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;


import androidx.annotation.RequiresApi;

import net.arnx.jsonic.JSON;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by myasnikov on 25.12.15.
 */
public class Sys {

    public static HashMap<String, String> MESSAGE_LIST = new HashMap<String, String>(10, (float) 0.5);
    public static Hashtable<String, Socket> DeviceSocket = new Hashtable<String, Socket>(10, (float) 0.5);
    public static Hashtable<String, InputStreamReader> DeviceIStream = new Hashtable<String, InputStreamReader>(10, (float) 0.5);
    public static Hashtable<String, OutputStream> DeviceOStream = new Hashtable<String, OutputStream>(10, (float) 0.5);
    public static Hashtable<String, String> DevicePass = new Hashtable<String, String>(10, (float) 0.5);


    public static Hashtable<String, Object> toMap(JSONObject jsonobj) throws JSONException {
        Hashtable<String, Object> map = new Hashtable<String, Object>();
        Iterator<String> keys = jsonobj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonobj.get(key);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public static void writeFile(Context context, String FileName, HashMap<String, String> msg) {
        try {
            // отрываем поток для записи
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(FileName, context.MODE_PRIVATE)));
            // пишем данные
            bw.write(JSON.encode(msg));
            // закрываем поток
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, String> readFile(Context context, String FileName) {
        StringBuffer sb = new StringBuffer();
        try {
            // открываем поток для чтения
            BufferedReader br = new BufferedReader(new InputStreamReader(context.openFileInput(FileName)));
            String str = "";
            // читаем содержимое
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, String> decode = JSON.decode(sb.toString());
        return decode;
    }

    /**
     * Отправить список Контент провайдеров
     */
    public static void sendListProvider(Context context, String FiltrText) {

        try {
            System.out.write(("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Access-Control-Allow-Credentials: true\r\n"
                    + "Access-Control-Expose-Headers: FooBar\r\n"
                    + "Connection: close\r\n"
                    + "Server: HTMLserver\r\n\r\n").getBytes());
            System.out.flush();
            /*
            for (PackageInfo pack : context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        String providerString = provider.authority;
                        if (providerString != null) {
                            if (providerString.contains(FiltrText)) {
                                System.out.write(("<a href='" + providerString + "'>" + providerString +"</a>&nbsp&nbsp&nbsp&nbsp"+provider.processName+"&nbsp&nbsp"+provider.name+" <br>").getBytes());
                                System.out.flush();

                            }
                        }
                    }
                }
            }*/
            System.out.write(("Page not found").getBytes());
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void sendJson(String jsonObject) {
        try {
            System.out.write("HTTP/1.1 200 OK\r\n".getBytes());
            // дата создания в GMT
            DateFormat df = DateFormat.getTimeInstance();
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            // Длина файла
            System.out.write(("Content-Length: " + jsonObject.length() + "\r\n").getBytes());
            System.out.write(("Content-Type: application/x-javascript; charset=utf-8\r\n").getBytes());
            // Остальные заголовки
            System.out.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            System.out.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
            System.out.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            System.out.write("Connection: close\r\n".getBytes());
            System.out.write("Server: HTMLserver\r\n\r\n".getBytes());
            System.out.write(jsonObject.getBytes(), 0, jsonObject.length());
            System.out.flush();
            // завершаем соединение
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(Sys.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void sendJson(OutputStream os, String jsonObject) {
        try {
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            // дата создания в GMT
            DateFormat df = DateFormat.getTimeInstance();
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            // Длина файла
            os.write(("Content-Length: " + jsonObject.length() + "\r\n").getBytes());
            os.write(("Content-Type: application/x-javascript; charset=utf-8\r\n").getBytes());
            // Остальные заголовки
            os.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            os.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
            os.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            os.write("Connection: close\r\n".getBytes());
            os.write("Server: HTMLserver\r\n\r\n".getBytes());
            Log.d("TAG",jsonObject );
            os.write(jsonObject.getBytes(Charset.forName("UTF-8")));
            // os.write(jsonObject.getBytes(), 0, jsonObject.length());
            os.write(0);
            os.flush();
            // завершаем соединение
            // System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(Sys.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void sendAssestFile(OutputStream os, Context context, String zapros) {
        try {
            String TypeCont = ContentType(new File(zapros));
            // Первая строка ответа
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            // Длина файла
            os.write(("Content-Type: " + TypeCont + "; charset=utf-8\r\n").getBytes());
            // Остальные заголовки
            os.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            os.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
            os.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            os.write("Connection: close\r\n".getBytes());
            os.write("Server: HTMLserver\r\n\r\n".getBytes());
            if ((TypeCont.equals("text/html"))
                    || (TypeCont.equals("text/plain"))
                    || (TypeCont.equals("text/css"))
                    || (TypeCont.equals("text/xml"))
                    || (TypeCont.equals("application/x-javascript"))) {
                // обработка текстового контента
                int bufferSize = 1024;
                char[] buffer = new char[bufferSize];
                StringBuilder out = new StringBuilder();
                Reader in = new InputStreamReader(context.getAssets().open(zapros), StandardCharsets.UTF_8);
                for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                    out.append(buffer, 0, numRead);
                }
                // return out.toString();
                os.write(out.toString().getBytes(), 0, out.toString().getBytes().length);
            } else {
                // Отправка бинарного файла
                InputStream in = context.getAssets().open(zapros);
                int read;
                byte[] buffer = new byte[4096];
                while ((read = in.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
                in.close();
                os.flush();
            }
            // System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        } catch (IOException ex) {
            Logger.getLogger(Sys.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public static void sendRawFile(File pageFile) {
        try {
            FileReader fileInput = new FileReader(pageFile);
            String Code = fileInput.getEncoding();
            fileInput.close();
            String TypeCont = ContentType(pageFile);
            // Первая строка ответа
            System.out.write("HTTP/1.1 200 OK\r\n".getBytes());
            // дата создания в GMT
            DateFormat df = DateFormat.getTimeInstance();
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            // Время последней модификации файла в GMT
            System.out.write(("Last-Modified: " + df.format(new Date(pageFile.lastModified())) + "\r\n").getBytes());
            // Длина файла
            System.out.write(("Content-Length: " + pageFile.length() + "\r\n").getBytes());
            System.out.write(("Content-Type: " + TypeCont + "; ").getBytes());
            System.out.write(("charset=" + Code + "\r\n").getBytes());
            // Остальные заголовки
            System.out.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            System.out.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
            System.out.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            System.out.write("Connection: close\r\n".getBytes());
            System.out.write("Server: HTMLserver\r\n\r\n".getBytes());
            if (TypeCont.equals("text/html")) {


            } else {
                // Отправка бинарного файла
                FileInputStream fis = new FileInputStream(pageFile.getAbsolutePath());
                int lengRead = 1;
                byte buf[] = new byte[1024];
                while ((lengRead = fis.read(buf)) != -1) {
                    System.out.write(buf, 0, lengRead);
                }
                // закрыть файл
                fis.close();
            }
            // завершаем соединение
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        } catch (IOException ex) {
            Logger.getLogger(Sys.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private static String ContentType(File pageFile) {
        String ras = null;
        // путь без файла
        String Dir = pageFile.getPath().replace(pageFile.getName(), "").toLowerCase();
        // имя файла с расширением
        String FileName = pageFile.getName();
        // расширение файла
        String rashirenie = FileName.substring(FileName.lastIndexOf(".") + 1);
        // путь к файлу + имя файла - расширение файла
        String DirFile = pageFile.getPath().replace("." + rashirenie, "");
        // имя файла без расширения
        String File2 = FileName.replace("." + rashirenie, "");
        rashirenie = rashirenie.toLowerCase();// преобразуем в нижний регистр
        if (rashirenie.equals("css")) {
            return "text/css";
        }
        if (rashirenie.equals("js")) {
            return "application/x-javascript";
        }
        if (rashirenie.equals("xml") || rashirenie.equals("dtd")) {
            return "text/xml";
        }
        if ((rashirenie.equals("txt")) || (rashirenie.equals("inf")) || (rashirenie.equals("nfo"))) {
            return "text/plain";
        }
        if ((rashirenie.equals("html")) || (rashirenie.equals("htm")) || (rashirenie.equals("shtml")) || (rashirenie.equals("shtm")) || (rashirenie.equals("stm")) || (rashirenie.equals("sht"))) {
            return "text/html";
        }
        if ((rashirenie.equals("mpeg")) || (rashirenie.equals("mpg")) || (rashirenie.equals("mpe"))) {
            return "video/mpeg";
        }
        if ((rashirenie.equals("ai")) || (rashirenie.equals("ps")) || (rashirenie.equals("eps"))) {
            return "application/postscript";
        }
        if (rashirenie.equals("rtf")) {
            return "application/rtf";
        }
        if ((rashirenie.equals("au")) || (rashirenie.equals("snd"))) {
            return "audio/basic";
        }
        if ((rashirenie.equals("bin")) || (rashirenie.equals("dms")) || (rashirenie.equals("lha")) || (rashirenie.equals("lzh")) || (rashirenie.equals("class")) || (rashirenie.equals("exe"))) {
            return "application/octet-stream";
        }
        if (rashirenie.equals("doc")) {
            return "application/msword";
        }
        if (rashirenie.equals("pdf")) {
            return "application/pdf";
        }
        if (rashirenie.equals("ppt")) {
            return "application/powerpoint";
        }
        if ((rashirenie.equals("smi")) || (rashirenie.equals("smil")) || (rashirenie.equals("sml"))) {
            return "pplication/smil";
        }
        if (rashirenie.equals("zip")) {
            return "application/zip";
        }
        if ((rashirenie.equals("midi")) || (rashirenie.equals("kar"))) {
            return "audio/midi";
        }
        if ((rashirenie.equals("mpga")) || (rashirenie.equals("mp2")) || (rashirenie.equals("mp3"))) {
            return "audio/mpeg";
        }
        if (rashirenie.equals("wav")) {
            return "audio/x-wav";
        }
        if (rashirenie.equals("ief")) {
            return "image/ief";
        }

        if ((rashirenie.equals("jpeg")) || (rashirenie.equals("jpg")) || (rashirenie.equals("jpe"))) {
            return "image/jpeg";
        }
        if (rashirenie.equals("png")) {
            return "image/png";
        }
        if (rashirenie.equals("ico")) {
            return "image/x-icon";
        }
        if ((rashirenie.equals("tiff")) || (rashirenie.equals("tif"))) {
            return "image/tiff";
        }
        if ((rashirenie.equals("wrl")) || (rashirenie.equals("vrml"))) {
            return "model/vrml";
        }
        if (rashirenie.equals("avi")) {
            return "video/x-msvideo";
        }
        if (rashirenie.equals("flv")) {
            return "video/x-flv";
        }
        if (rashirenie.equals("ogg")) {
            return "video/ogg";
        }
        return "application/octet-stream";
    }


}
