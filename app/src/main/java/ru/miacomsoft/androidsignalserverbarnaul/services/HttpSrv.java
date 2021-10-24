package ru.miacomsoft.androidsignalserverbarnaul.services;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;

import net.arnx.jsonic.JSON;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.miacomsoft.androidsignalserverbarnaul.Lib.Sys;


/**
 * @author myasnikov
 */
public class HttpSrv {
    private static Context context;

    public HttpSrv(Context context) {
        this.context = context;
    }

    private static String UserName;
    private static String UserPass;

    private static int numComp = 0;
    private static String IPmac = "";
    private static String DefaultHost = "";

    public static int port = 8266;
    public static boolean IsAutorization = true;
    static public boolean process = false;
    static public File sdcard;
    static public String CharSet = "utf-8";

    //            String authString = "user" + ":" + "123";
    //            code = new String(Base64.encode(authString.getBytes()));

    public void Start(HashMap<String, String> Setup) {
        this.UserName = Setup.get("UserName");
        this.UserPass = Setup.get("UserPass");
        this.DefaultHost = Setup.get("DefaultHost");
        this.port = Integer.valueOf(Setup.get("UserPort"));
        if (Setup.get("CharSet") != null) {
            this.CharSet = Setup.get("CharSet");
        }
        IsAutorization = false;
        Setup.get("StartPath");
        process = true;
        sdcard = Environment.getExternalStorageDirectory();
        //sdcard = new File(Setup.get("StartPath"));
        // получаем IP адрес и MAC адрес сервера
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            IPmac = ip.getHostAddress() + "|" + new String(network.getHardwareAddress());
        } catch (Exception ex) {
            IPmac = "NoIP|Nomac";
        }

        // Toast.makeText(context, "Start Web Server", Toast.LENGTH_LONG).show();
        Thread myThready;
        myThready = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(HttpSrv.port);
                    while (process == true) {
                        numComp++;
                        Socket socket = ss.accept();
                        new Thread(new SocketProcessor(socket)).start();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });
        myThready.start();    //Запуск потока

    }

    /**
     * Остановить сервер
     */
    public void Stop() {
        process = false;
        // Toast.makeText(context, "Stop File Web Server", Toast.LENGTH_LONG).show();
    }

    private static class SocketProcessor implements Runnable {


        private static Object StandardLog;
        private Socket socket;
        private InputStream is;
        private OutputStream os;
        private String contentZapros = "";

        private SocketProcessor(Socket socket) throws Throwable {
            this.socket = socket;
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            Headers.clear();
            String Adress = socket.getRemoteSocketAddress().toString();
            Headers.put("RemoteIPAdress", Adress);
            Adress = Adress.split(":")[0];
            // Adress = Adress.substring(1, Adress.length());
        }

        public void run() {
            try {
               if (readInputHeaders()) {
                   writeResponse();
               }
            } catch (Throwable t) {
            } finally {
                try {
                    socket.close();
                } catch (Throwable t) {
                    /*do nothing*/
                }
            }
        }

        private HashMap<String, Object> Headers = new HashMap<String, Object>(10, (float) 0.5);
        private HashMap<String, Object> LocalMessage = new HashMap<String, Object>(10, (float) 0.5);
        private HashMap<String, String> inParam = new HashMap<String, String>(10, (float) 0.5);
        private byte[] PostByte = new byte[0];
        private String Koderovka = "";
        //  private String getCommand = "";
        private String getCmd = "";

        /**
         * Чтение входных данных от клиента
         *
         * @throws IOException
         */
        private boolean readInputHeaders() throws IOException {
            // FileWriter outLog = new FileWriter(rootPath + "\\log.txt", true); //the true will append the new data
            //  outLog.write("add a line\n");//appends the string to the file
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            contentZapros = "";

            StringBuffer sbInData = new StringBuffer();
            int numLin = 0;
            InputStreamReader isr = new InputStreamReader(is);
            int charInt;
            char[] charArray = new char[1024];
            // Читаем заголовок
            StringBuffer sb = new StringBuffer();
            StringBuffer sbTmp = new StringBuffer();
            while ((charInt = isr.read()) > 0) {
                if (socket.isConnected() == false) {
                    return false;
                }
                //   System.out.write((char) charInt);
                //  outLog.write((char) charInt);
                sbTmp.append((char) charInt);
                if (sbTmp.toString().indexOf("\n") != -1) {
                    if (sbTmp.toString().length() == 2) {
                        break; // чтение заголовка окончено
                    }
                    sbTmp.setLength(0);
                }
                sb.append((char) charInt);
            }

            int indLine = 0;
            String htmlQuery = "";
            for (String TitleLine : sb.toString().split("\r\n")) {
                indLine++;
                if (indLine == 1) {
                    if (((TitleLine.indexOf("GET ") == -1) && (TitleLine.indexOf("HTTP/1.1") == -1))
                            || ((TitleLine.indexOf("POST ") == -1) && (TitleLine.indexOf("HTTP/1.1") == -1))
                            && (TitleLine.indexOf("OPTIONS ") == -1)
                    ) {
                        // Если не HTML запрос, тогда обрабатываем как  терминальное подключение
                        PrintStream out = new PrintStream(os);
                        terminalQuery(isr, os, sb.toString());
                        return false;
                    }
                    if (TitleLine.indexOf("OPTIONS ") != -1) {
                        System.out.println("---------------------------------");
                        System.out.println(sb.toString());
                        System.out.println("---------------------------------");
                        crossDomain(os);
                        return false;
                    }
                    TitleLine = TitleLine.replaceAll("GET /", "");
                    TitleLine = TitleLine.replaceAll("POST /", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.1", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.0", "");
                    contentZapros = java.net.URLDecoder.decode(TitleLine, "UTF-8");

                    // Json.put("ContentZapros", contentZapros);
                    // System.out.println("=-=" + contentZapros);
                    if (contentZapros.indexOf("?") != -1) {
                        String tmp = contentZapros.substring(0, contentZapros.indexOf("/?") + 2);
                        String param = contentZapros.replace(tmp, "");
                        getCmd = param;
                        Headers.put("ParamAll", param);
                        contentZapros = tmp.substring(0, tmp.length());
                        int indParam = 0;
                        for (String par : param.split("&")) {
                            String[] val = par.split("=");
                            if (val.length == 2) {
                                Headers.put(val[0], val[1]);
                                val[0] = val[0].replace(" ", "_");
                                Headers.put(val[0], val[1]);
                            } else {
                                indParam++;
                                Headers.put("Param" + String.valueOf(indParam), val[0]);
                            }
                        }
                        contentZapros = tmp.substring(0, tmp.length() - 2);//.toLowerCase()
                    }

                    if (contentZapros.length() == 0) {
                        contentZapros = "DefaultHost";
                    }
                    Headers.put("Zapros", contentZapros);
                    Headers.put("RootPath", sdcard.getAbsolutePath());
                    File pathPege = new File(sdcard.getAbsolutePath() + "/" + contentZapros);
                    Headers.put("AbsalutZapros", pathPege.getAbsolutePath());

                } else {
                    if (TitleLine == null || TitleLine.trim().length() == 0) {
                        break;
                    }
                    if (TitleLine.split(":").length > 0) {
                        String val = TitleLine.split(":")[0];
                        val = val.replace(" ", "_");
                        Headers.put(val, TitleLine.replace(TitleLine.split(":")[0] + ":", ""));
                    }
                }
            }

            //
            // кодировка входных данных
            if (Headers.containsKey("Content-Type") == true) {
                // Content-Type: text/html; charset=windows-1251
                if (Headers.get("Content-Type").toString().split("charset=").length == 2) {
                    Koderovka = Headers.get("Content-Type").toString().split("charset=")[1];
                    Headers.put("Charset", Koderovka);
                    //  Json.put("Charset", Koderovka);
                }
            }
            //  outLog.write("\n--POST--\n");
            if (Headers.containsKey("Content-Length") == true) {
                // Читаем тело пост запроса
                String lengStr = Headers.get("Content-Length").toString();
                // outLog.write("\n--1 " + lengStr + "--\n");
                lengStr = lengStr.replace(" ", "");
                int lengAll = Integer.parseInt(lengStr);
                // outLog.write("\n--2 " + lengAll + "--\n");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                StringBuffer sbTmpPost = new StringBuffer();

                // CharBuffer sbPost = CharBuffer.allocate(lengAll);
                int charInt1 = -1;
                while ((charInt1 = isr.read()) > 0) {
                    baos.write(charInt1);
                    sbTmpPost.append((char) charInt1);
                    lengAll--;
                    if (lengAll == 0) {
                        break;
                    }
                    if (socket.isConnected() == false) {
                        return false;
                    }
                }
                PostByte = baos.toByteArray();
                Headers.put("PostBodyText", JSON.encode(baos));
                Headers.put("PostBodyByte", baos.toByteArray());
                LocalMessage.put("PostBodyByte", baos.toByteArray());
                LocalMessage.put("PostBodyText", sbTmpPost.toString());
            }
            // Парсим Cookie если он есть
            if (Headers.containsKey("Cookie") == true) {
                String Cookie = Headers.get("Cookie").toString();
                Cookie = Cookie.substring(1, Cookie.length());// убираем лишний пробел сначала строки
                for (String elem : Cookie.split("; ")) {
                    String[] val = elem.split("=");
                    try {
                        Headers.put(val[0], val[1]);
                        val[0] = val[0].replace(" ", "_");
                        Headers.put(val[0], val[1]);
                        Headers.put(val[0], val[1]);
                        inParam.put(val[0], val[1]);
                    } catch (Exception e) {

                    }
                }
            }
            if (Headers.containsKey("Zapros") == true) {
                String Zapros = Headers.get("Zapros").toString();
                for (String elem : Zapros.split("&")) {
                    String[] val = elem.split("=");
                    try {
                        Headers.put(val[0], val[1]);
                        val[0] = val[0].replace(" ", "_");
                        Headers.put(val[0], val[1]);
                        LocalMessage.put(val[0], val[1]);
                    } catch (Exception e) {
                    }
                }
            }
            //  PrintWriter pw = new PrintWriter(new FileWriter("C:\\Intel\\srvLogInData.xml"));
            //  pw.write(sb.toString());
            //  pw.close();
            System.out.println("---------------------------------");
            System.out.println(sb.toString());
            System.out.println("OK");
            System.out.println("---------------------------------");
            sb.setLength(0);

            return true;
        }


        /**
         * метод отправки ответа клиенту
         */
        private void writeResponse() {
            if (socket.isConnected() == false) {
                return;
            }
            PrintStream out = new PrintStream(os);
            System.setOut(out);
            System.setErr(out);
            // String PathStr = Headers.get("AbsalutZapros").toString();
            // оставить сообщение для устройства
            if (Headers.containsKey("push") == true) {
                String devName = Headers.get("push").toString();
                Sys.MESSAGE_LIST.put(devName, JSON.encode(LocalMessage));
                Sys.sendJson("{\"ok\":true}");
                return;
            }

            if (Headers.containsKey("pop") == true) {
                String devName = Headers.get("pop").toString();
                if (Sys.MESSAGE_LIST.containsKey(devName) == true) {
                    Sys.sendJson(Sys.MESSAGE_LIST.get(devName).toString());
                    Sys.MESSAGE_LIST.remove(devName);
                } else {
                    Sys.sendJson("{\"ok\":false,\"error\":\"no message\"}");
                }
                return;
            }
            if (Headers.containsKey("send") == true) {
                String devName = Headers.get("send").toString();
                if (Sys.DeviceOStream.containsKey(devName) == true) {
                    try {
                        OutputStream osDst = Sys.DeviceOStream.get(devName);
                        osDst.write(JSON.encode(LocalMessage).getBytes());
                        osDst.flush();
                        Sys.sendJson("{\"ok\":true}");
                    } catch (IOException e) {
                        Sys.sendJson("{\"ok\":false,\"error\":\"send " + devName + " error\"}\r\n");
                        e.printStackTrace();
                    }
                } else {
                    Sys.sendJson("{\"ok\":false,\"error\":\"device " + devName + " not found\"}\r\n");
                }
                return;
            }

            Sys.sendJson(JSON.encode(Headers) + "  " + JSON.encode(LocalMessage));

             /*
            File pathPege = new File(PathStr);
            if (pathPege.exists() && !pathPege.isDirectory()) {
                Sys.sendRawFile(pathPege);
            } else {
                CreateCompId();
                try {
                    // Headers.put("Zapros", "WebFileManagerApp.htm");
                    if (sendContentProvidr(Headers.get("Zapros").toString()) == false) {
                        Sys.sendListProvider(context, ".htm");
                    }
                } catch (Exception ex) {
                    System.out.println("Error" + ex.toString());
                }
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            }
            */
        }


        /**
         * Создание иденификатора подключаемого компьютера  и сохранение его в куках клиенской машины
         */
        public void CreateCompId() {
            //
            // создаем идентификатор компьютера , сохраняем его в Кукисах и перезагружаем страницу
            if (Headers.containsKey("WORCSTATIONID") == false) {
                try {
                    Date currentDate = new Date();
                    Long time = currentDate.getTime();
                    String IDcomp = getMD5(numComp + IPmac + time);
                    String initWORCSTation = ""
                            + "<script>"
                            + "    function setCookie(cname, cvalue, exdays) { var d = new Date(); d.setTime(d.getTime() + (exdays)); var expires = 'expires='+d.toUTCString();   		document.cookie = cname + '=' + cvalue + '; ' + expires;} \n"
                            + "    setCookie('WORCSTATIONID', '" + IDcomp + "', 157680000); "
                            + "    window.location.href=window.location.toString();"
                            + "</script>"; //31536000
                    os.write(("HTTP/1.1 200 OK\r\n").getBytes());
                    os.write(("Content-Type: text/html; ").getBytes());
                    os.write(("Content-Length: " + initWORCSTation.length() + "\r\n").getBytes());
                    os.write(("charset=utf-8\r\n").getBytes());
                    os.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                    os.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                    os.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
                    os.write("Connection: close\r\n".getBytes());
                    os.write("Server: HTMLserver\r\n\r\n".getBytes());
                    os.write(initWORCSTation.getBytes());
                    return;
                } catch (Exception ex) {
                    System.err.println("Error create ID comp:" + ex.toString());
                    return;
                }
            }
        }


        /**
         * Найти провайдера по имени, и если он есть, тогда запустить его
         *
         * @param ProvierName
         * @return
         */
        private boolean sendContentProvidr(String ProvierName) {
            boolean isContentOk = false;
            for (PackageInfo pack : context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        String providerString = provider.authority;
                        if (providerString != null) {
                            String providerLow = providerString.toLowerCase();
                            String zapr = ProvierName.toLowerCase();
                            if (providerLow.equals(zapr)) {
                                ContentResolver cr = context.getContentResolver();
                                Uri CONTACT_URI = Uri.parse("content://" + providerString);

                                Bundle Head = new Bundle();
                                for (String key : Headers.keySet()) {
                                    Head.putString(key, Headers.get(key).toString());
                                }
                                if (Headers.containsKey("PostBodyByte")) {
                                    Head.putByteArray("PostBodyByte", PostByte);
                                }
                                Head.putString("CharSet", CharSet);

                                Bundle callRes = cr.call(CONTACT_URI, providerString, JSON.encode(Headers), Head);
                                if (callRes != null) {
                                    try {
                                        // byte [] res = callRes.getByteArray(providerString);
                                        // if (res != null) {
                                        //     System.out.write(res);
                                        //     System.out.flush();
                                        //     return true;
                                        // }
                                        byte[] res = callRes.getByteArray("return");
                                        if (res != null) {
                                            System.out.write(res);
                                            System.out.flush();
                                            return true;
                                        }
                                        System.out.write(JSON.encode(callRes).getBytes());
                                        System.out.flush();
                                    } catch (IOException e) {
                                        System.err.print("Error:" + e.toString());
                                    }
                                }
                                isContentOk = true;
                                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
                            }
                        }
                    }
                }
            }
            return isContentOk;

        }


        /**
         * Закодировать строку кодировкой MD5
         *
         * @param input
         * @return
         */
        private String getMD5(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] messageDigest = md.digest(input.getBytes());
                BigInteger number = new BigInteger(1, messageDigest);
                String hashtext = number.toString(16);
                // Now we need to zero pad it if you actually want the full 32 chars.
                while (hashtext.length() < 32) {
                    hashtext = "0" + hashtext;
                }
                return hashtext;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private String getJsonString(Hashtable<String, String> map) {
            String jsonString = "{";
            Set<Map.Entry<String, String>> entries = map.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                jsonString = jsonString + "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\",";
            }
            int i = jsonString.lastIndexOf(",");
            jsonString = jsonString.substring(0, i);
            jsonString += "}";
            return jsonString;
        }

        private void sendInfo() {

            try {

                File[] paths = sdcard.listFiles();
                for (File path : paths) {// перебираем список файлов в каталоге
                    if (!path.isDirectory()) {// если фацл
                        // System.out.println("   <a onmousedown=\"OnButtonDownFile(event, this, 'contextMenuId')\"   style=\"color: rgb(0, 100, 0);  cursor: pointer;  \" href=\"http://" + Host + "/" + DirShref + "\"   id=\"" + path.getAbsolutePath() + "\">&nbsp&nbsp&nbsp&nbsp" + path.getName() + "___</a><br>");
                    } else {
                        String DirShref = path.getAbsolutePath();
                        System.out.println("   <a onmousedown=\"OnButtonDownFile(event, this, 'contextMenuId')\"   style=\"color: rgb(0, 100, 0);  cursor: pointer;  \" href=\"" + path.getAbsolutePath().replace(sdcard.getAbsolutePath(), "") + "\"   id=\"" + path.getAbsolutePath() + "\">&nbsp&nbsp&nbsp&nbsp" + path.getName() + "___</a><br>");
                    }
                }

                System.out.write(("<br>").getBytes());
                System.out.write(("<br>").getBytes());
                String ProvierName = Headers.get("Zapros").toString();
                        /*
                        StringBuffer ProvierNameInvert = new StringBuffer();
                        if (ProvierName.indexOf(".") != -1) {
                            String[] element = ProvierName.split(".");
                            for (int ind = element.length; ind > 0; ind--) {
                                ProvierNameInvert.append(element[ind] + ".");
                            }
                            ProvierName = ProvierNameInvert.toString().substring(0, ProvierNameInvert.toString().length() - 1);
                        }
                        */
                System.out.write(("ProvierName - " + ProvierName + "<br>").getBytes());
                System.out.write(("<br>").getBytes());
                Properties p = System.getProperties();
                System.out.write((Headers.toString() + "<br>").getBytes());
                System.out.write(("<br>").getBytes());
                System.out.write(("<br>").getBytes());
                System.out.write(("<br>").getBytes());
                System.out.write(("<br>").getBytes());
                System.out.write(("Java Runtime Environment version: " + p.getProperty("java.version") + "<br>").getBytes());
                System.out.write(("Java Runtime Environment vendor: " + p.getProperty("java.vendor") + "<br>").getBytes());
                System.out.write(("Java vendor URL: " + p.getProperty("java.vendor.url") + "<br>").getBytes());
                System.out.write(("Java installation directory: " + p.getProperty("java.home") + "<br>").getBytes());
                System.out.write(("Java Virtual Machine specification version: " + p.getProperty("java.vm.specification.version") + "<br>").getBytes());
                System.out.write(("Java Virtual Machine specification vendor: " + p.getProperty("java.vm.specification.vendor") + "<br>").getBytes());
                System.out.write(("Java Virtual Machine specification name: " + p.getProperty("java.vm.specification.name") + "<br>").getBytes());
                System.out.write(("Java Virtual Machine implementation version: " + p.getProperty("java.vm.version") + "<br>").getBytes());
                System.out.write(("Java Virtual Machine implementation vendor: " + p.getProperty("java.vm.vendor") + "<br>").getBytes());
                System.out.write(("Java Virtual Machine implementation name: " + p.getProperty("java.vm.name") + "<br>").getBytes());
                System.out.write(("Java Runtime Environment specification version: " + p.getProperty("java.specification.version") + "<br>").getBytes());
                System.out.write(("Java Runtime Environment specification vendor: " + p.getProperty("java.specification.vendor") + "<br>").getBytes());
                System.out.write(("Java Runtime Environment specification name: " + p.getProperty("java.specification.name") + "<br>").getBytes());
                System.out.write(("Java class format version number: " + p.getProperty("java.class.version") + "<br>").getBytes());
                System.out.write(("Java class path: " + p.getProperty("java.class.path") + "<br>").getBytes());
                System.out.write(("List of paths to search when loading libraries: " + p.getProperty("java.library.path") + "<br>").getBytes());
                System.out.write(("Default temp file path: " + p.getProperty("java.io.tmpdir") + "<br>").getBytes());
                System.out.write(("Name of JIT compiler to use: " + p.getProperty("java.compiler") + "<br>").getBytes());
                System.out.write(("Path of extension directory or directories: " + p.getProperty("java.ext.dirs") + "<br>").getBytes());
                System.out.write(("Operating system name: " + p.getProperty("os.name") + "<br>").getBytes());
                System.out.write(("Operating system architecture: " + p.getProperty("os.arch") + "<br>").getBytes());
                System.out.write(("Operating system version: " + p.getProperty("os.version") + "<br>").getBytes());
                System.out.write(("File separator (\"/\" on UNIX): " + p.getProperty("file.separator") + "<br>").getBytes());
                System.out.write(("Path separator (\":\" on UNIX): " + p.getProperty("path.separator") + "<br>").getBytes());
                System.out.write(("Line separator (\"\\n\" on UNIX): " + p.getProperty("line.separator") + "<br>").getBytes());
                System.out.write(("User's account name: " + p.getProperty("user.name") + "<br>").getBytes());
                System.out.write(("User's home directory: " + p.getProperty("user.home") + "<br>").getBytes());
                System.out.write(("User's current working directory: " + p.getProperty("user.dir") + "<br>").getBytes());
            } catch (Exception ex) {
                System.err.println("Error send info:" + ex.toString());
            }
        }


        private boolean author() {
            try {
                if (!Headers.containsKey("Author")) {
                    System.out.write(("HTTP/1.1 401 OK\r\n"
                            + "Content-type: text/html; charset=utf8\r\n"
                            + "WWW-Authenticate: Basic realm=\"Cache\""
                            + "\r\n\r\n"
                            + "<html>\r\n"
                            + "  <head>\r\n"
                            + "      <meta charset='UTF-8'>\r\n"
                            + "  </head>\r\n"
                            + "   <body><h2></h2>\r\n"
                            + "        <p> Error autor</p>\r\n"
                            + "   </body>\r\n"
                            + "</html>\r\n").getBytes());
                    return false;
                }

                String Author = new String(Base64.decode(Headers.get("Author").toString(), Base64.DEFAULT));
                if (Author.split(":").length != 2) {
                    System.out.write(("HTTP/1.1 401 OK\r\n"
                            + "Content-type: text/html; charset=utf8\r\n"
                            + "WWW-Authenticate: Basic realm=\"Cache\""
                            + "\r\n\r\n"
                            + "<html>\r\n"
                            + "  <head>\r\n"
                            + "      <meta charset='UTF-8'>\r\n"
                            + "  </head>\r\n"
                            + "   <body><h2></h2>\r\n"
                            + "        <p> Error autor</p>\r\n"
                            + "   </body>\r\n"
                            + "</html>\r\n").getBytes());
                    return false;
                }
                if ((!Author.split(":")[0].equals(UserName)) || (!Author.split(":")[1].equals(UserPass))) {
                    System.out.write(("HTTP/1.1 401 OK\r\n"
                            + "Content-type: text/html; charset=utf8\r\n"
                            + "WWW-Authenticate: Basic realm=\"Cache\""
                            + "\r\n\r\n"
                            + "<html>\r\n"
                            + "  <head>\r\n"
                            + "      <meta charset='UTF-8'>\r\n"
                            + "  </head>\r\n"
                            + "   <body><h2></h2>\r\n"
                            + "        <p> Error autor</p>\r\n"
                            + "   </body>\r\n"
                            + "</html>\r\n").getBytes());
                    return false;
                }

            } catch (Exception ex) {
                System.err.println("Error author:" + ex.toString());
            }
            return true;
        }


        private void terminalQuery(InputStreamReader isr, OutputStream os, String rowText) {
            String DeviceNameClient = rowText.replace("\n", "").replace("\r", "");
            try {
                os.write(("\r\nWelcom|" + DeviceNameClient + "|\r\n").getBytes());
                os.flush();
                rebootOneDevice(DeviceNameClient);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Sys.DeviceIStream.put(DeviceNameClient, isr);
            Sys.DeviceOStream.put(DeviceNameClient, os);
            Sys.DeviceSocket.put(DeviceNameClient, socket);

            while (socket.isConnected()) {
                try {
                    int charInt;
                    // Читаем заголовок
                    Hashtable<String, Object> Json = new Hashtable<String, Object>(10, (float) 0.5);
                    StringBuffer sbTmp = new StringBuffer();
                    StringBuffer sb = new StringBuffer();
                    while ((charInt = isr.read()) > 0) {
                        if (socket.isConnected() == false) {
                            return;
                        }
                        sbTmp.append((char) charInt);
                        if (sbTmp.toString().indexOf("\n") != -1) {
                            if (sbTmp.toString().length() == 2) {
                                break; // чтение заголовка окончено
                            }
                            sbTmp.setLength(0);
                        }
                        sb.append((char) charInt);
                    }
                    String lineOne = sb.toString().replace("\n", "").replace("\r", "");
                    if ((lineOne.indexOf("exit") != -1) && (lineOne.length() == 4)
                            || (sb.toString().indexOf("{\"exit\":\"exit\"}") != -1)
                    ) {
                        rebootOneDevice(DeviceNameClient);
                        return;
                    }

                    if ((lineOne.indexOf("ping") != -1) && (lineOne.length() == 4)) {
                        os.write(("ping\r\n").getBytes());
                        continue;
                    }
                    // получить список подключенных устройств
                    if ((lineOne.indexOf("list") != -1) && (lineOne.length() == 4)) {
                        Set<String> keys = Sys.DeviceSocket.keySet();
                        os.write(("\r\n").getBytes());
                        for (String key : keys) {
                            os.write((key + "  (" + key.length() + " ").getBytes());
                            if (Sys.DeviceSocket.get(key).isConnected()) {
                                os.write((" connect ").getBytes());
                            } else {
                                os.write((" disconnect ").getBytes());
                                //DeviceIO.remove(key);
                                //DeviceSocket.remove(key);
                            }
                            os.write((")\r\n").getBytes());
                        }
                        continue;
                    }

                    //  обработка запроса
                    // парсим входящий заголовок
                    if ((sb.toString().indexOf("{") != -1) && (sb.toString().indexOf("}") != -1)) {
                        try {
                            JSONObject jsonObject = new JSONObject(sb.toString());
                            Json = Sys.toMap(jsonObject);
                        } catch (JSONException e) {
                            Json.put("message", sb.toString());
                        }
                    } else {
                        int nimLine = 0;
                        for (String TitleLine : sb.toString().split("\r")) {
                            if (TitleLine.split(":").length > 0) {
                                TitleLine = TitleLine.replace("\n", "");
                                String val = TitleLine.split(":")[0];
                                val = val.replace(" ", "_");
                                if (val.length() > 0) {
                                    Json.put(val, TitleLine.replace(TitleLine.split(":")[0] + ":", ""));
                                }
                            } else {
                                nimLine++;
                                Json.put("line" + nimLine, TitleLine);
                            }
                        }
                    }
                    // отправка сообщения для устройства
                    if (Json.containsKey("push") == true) {
                        Json.put("from", DeviceNameClient);
                        String deviceName = Json.get("push").toString();
                        Sys.MESSAGE_LIST.put(deviceName, Json.toString());
                        os.write(("{\"ok\":true}\r\n").getBytes());
                        os.flush();
                        continue;
                    }
                    // получение сообщения для устройства
                    if (Json.containsKey("pop") == true) {
                        String deviceName = Json.get("pop").toString();
                        if (Sys.MESSAGE_LIST.containsKey(deviceName) == true) {
                            os.write((Sys.MESSAGE_LIST.get(deviceName).toString() + "\r\n").getBytes());
                            Sys.MESSAGE_LIST.remove(deviceName);
                        } else {
                            os.write(("{\"ok\":false,\"error\":\"no message\"}\r\n").getBytes());
                            os.flush();
                        }
                        continue;
                    }
                    // прямая отправка сообщения для устройства, если оно в сети
                    if (Json.containsKey("send") == true) {
                        Json.put("from", DeviceNameClient);
                        String deviceNameTo = Json.get("send").toString();
                        if (Sys.DeviceOStream.containsKey(deviceNameTo) == true) {
                            OutputStream osDst = Sys.DeviceOStream.get(deviceNameTo);
                            osDst.write(Json.toString().getBytes());
                            osDst.flush();
                        } else {
                            os.write(("{\"ok\":false,\"error\":\"device " + deviceNameTo + "not found\"}\r\n").getBytes());
                            os.flush();
                        }
                        continue;
                    }
                    if (Json.containsKey("stream") == true) {
                        Json.put("from", DeviceNameClient);
                        String deviceNameTo = Json.get("stream").toString();
                        if (Sys.DeviceOStream.containsKey(deviceNameTo) == true) {
                            OutputStream osDst = Sys.DeviceOStream.get(deviceNameTo);
                            InputStreamReader isDst = Sys.DeviceIStream.get(deviceNameTo);
                            Socket socDst = Sys.DeviceSocket.get(deviceNameTo);
                            dataExchangeTr(socket, isr, os, socDst, isDst, osDst);
                            osDst.write(Json.toString().getBytes());
                            osDst.flush();
                        } else {
                            os.write(("{\"ok\":false,\"error\":\"device " + deviceNameTo + "not found\"}\r\n").getBytes());
                            os.flush();
                        }
                        continue;

                    }
                     // os.write(("==============\r\n|" + sb.toString() + "|\r\n" + Json.toString() + "\r\n===================\r\n").getBytes());
                    sb.setLength(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            rebootOneDevice(DeviceNameClient);
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
                }
            }
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


        /**
         *
         */
        public void crossDomain(OutputStream os) {
            try {



                os.write(("HTTP/1.1 200 OK\n" +
                        "Date: Mon, 01 Dec 2008 00:23:53 GMT\n" +
                        "Server: Apache/2.0.61\n" +
                        "Keep-Alive: timeout=2, max=100\n" +
                        "Connection: Keep-Alive\n" +
                        "Access-Control-Allow-Origin: *\n" +
                        "Access-Control-Allow-Credentials: true\n" +
                        "Access-Control-Expose-Headers: FooBar\n" +
                        "Content-Type: text/html; charset=utf-8\n" +
                        "Transfer-Encoding: chunked\r\n\r\n").getBytes());
                /*
                os.write(("HTTP/1.1 200 OK\r\n" +
                        "Date: Mon, 01 Dec 2008 01:15:39 GMT\r\n" +
                        "Server: Apache/2.0.61 (Unix)\r\n" +
                        "Access-Control-Allow-Origin: http://128.0.24.172:8266\r\n" +
                        "Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: X-PINGOTHER, Content-Type\r\n" +
                        "Access-Control-Max-Age: 86400\r\n" +
                        "Vary: Accept-Encoding, Origin\r\n" +
                        "Content-Encoding: gzip\r\n" +
                        "Content-Length: 0\r\n" +
                        "Keep-Alive: timeout=2, max=100\r\n" +
                        "Connection: Keep-Alive\r\n" +
                        "Content-Type: text/plain\r\n\r\n").getBytes());

                 */
            } catch (Exception ex) {
                System.err.println("Error: " + ex.toString());
                return;
            }
        }
    }


}