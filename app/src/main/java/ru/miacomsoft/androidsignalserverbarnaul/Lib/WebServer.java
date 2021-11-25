package ru.miacomsoft.androidsignalserverbarnaul.Lib;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Для SDK ver 21
 *
 *
 *             webServer = new WebServer(this);
 *             webServer.Start(8266);
 *
 *
 *               // Перехват обработки терминального общения с клиентом
 *              webServer.onTerminal((WebServer.TerminalStruct term)->{
 *                  while (term.isConnect()) {
 *                      StringBuffer sb = new StringBuffer();
 *                      while ((sb = term.readText()).toString().length() != 0) {
 *                          Log.d("LOG_TAG", "Полученный текст от клиента "+sb.toString());
 *                          term.write("Отправка ответа");
 *                      }
 *                  }
 *              });
 *
 *
 *             // перехвать после чтения очередного сообщения от клиента
 *             webServer.onTerminalLoop((WebServer.TerminalStruct term)->{
 *                 Log.d("LOG_TAG", term.headText);
 *                 term.write("Отправка ответа:"+term.headText);
 *                 textView.setText(term.headText);
 *             });
 *
 *
 *             // обработка HTML запроса (любых не описанных)
 *             webServer.onPage((JSONObject Head, WebServer.Response res)->{
 *                 res.JSON("{\"OK\":\"--"+Head.getString("Query")+"----\"}");
 *             });
 *
 *
 *
 *             // Обработка URL пути
 *             webServer.onPage("index.html",".HTML",(JSONObject Head,WebServer.Response res)->{
 *                 res.Head();
 *                 res.Body("<h1>sgdfgsdfgsd;fghhhhhhhhhhhhhhhhhhhhhhhhhhhk;sdkfgsd;lgk;sd</h1>");
 *                 res.Body("sgdfgsdfgsd;fgk;sdkfgsd;lgk;sd");
 *                 res.End();
 *             });
 *
 *
 */
public class WebServer {

    private static Context context;
    public static int port = 8266;
    static private boolean process = false;
    private static String DefaultHost = "";
    static public String CharSet = "utf-8";
    static public File sdcard;
    private static String IPmac = "";
    private static int numComp = 0;
    private static DBHelper DB;
    private CallbackSocketConnect callbackSocketConnect = null;
    private CallbackSocketConnectIO callbackSocketConnectIO = null;
    public CallbackSocketReadHead callbackSocketReadHead = null;
    public CallbackSocketTerminal callbackSocketTerminal = null;
    public CallbackSocketTerminalLoop callbackSocketTerminalLoop = null;
    public CallbackSocketPage callbackSocketPage = null;
    private HashMap<String, PageObj> pagesList = new HashMap<String, PageObj>(10, (float) 0.5);
    public class PageObj {
        CallbackSocketPage callback;
        String ContentType;
    }

    public interface CallbackSocketConnect {
        public void call(Socket socket);
    }

    public interface CallbackSocketConnectIO {
        public void call(Socket socket, InputStream is, OutputStream os);
    }

    public interface CallbackSocketReadHead {
        public void call(JSONObject Headers);
    }

    public interface CallbackSocketTerminal {
        public void call(TerminalStruct terminalStruct) throws IOException;
    }

    public interface CallbackSocketTerminalLoop {
        public void call(TerminalStruct terminalStruct) throws JSONException, IOException;
    }

    public interface CallbackSocketPage {
        public void call(JSONObject Headers, Response response) throws JSONException, IOException;
    }

    public WebServer(Context context) {
        this.context = context;
        DB = new DBHelper(context);
    }

    /**
     * Обработчик подключений
     *
     * @param callbackSocketConnect
     */
    public void onConnect(CallbackSocketConnect callbackSocketConnect) {
        this.callbackSocketConnect = callbackSocketConnect;
    }


    public void onReadHead(CallbackSocketReadHead callbackSocketReadHead) {
        this.callbackSocketReadHead = callbackSocketReadHead;
    }

    public void onTerminal(CallbackSocketTerminal callbackSocketTerminal) {
        this.callbackSocketTerminal = callbackSocketTerminal;
    }

    public void onTerminalLoop(CallbackSocketTerminalLoop callbackSocketTerminalLoop) {
        this.callbackSocketTerminalLoop = callbackSocketTerminalLoop;
    }

    public void onPage(String query, CallbackSocketPage callbackSocketPage) {
        PageObj pageObj = new PageObj();
        pageObj.callback = callbackSocketPage;
        pageObj.ContentType = ContentType(new File(query));
        this.pagesList.put(query, pageObj);
    }

    public void onPage(String query, String contentType, CallbackSocketPage callbackSocketPage) {
        PageObj pageObj = new PageObj();
        pageObj.callback = callbackSocketPage;
        if (contentType.indexOf(".") != -1) {
            pageObj.ContentType = ContentType(new File("page" + contentType));
        } else {
            pageObj.ContentType = contentType;
        }
        this.pagesList.put(query, pageObj);
    }

    public void onPage(CallbackSocketPage callbackSocketPage) {
        this.callbackSocketPage = callbackSocketPage;
    }

    public void Start(int port) throws JSONException {
        JSONObject setup = new JSONObject();
        setup.put("Port", port);
        Start(setup);
    }

    public void Start(JSONObject Setup) throws JSONException {
        if (Setup.has("DefaultHost")) {
            this.DefaultHost = Setup.getString("DefaultHost");
        }
        if (Setup.has("Port")) {
            this.port = Setup.getInt("Port");
        }
        if (Setup.has("CharSet")) {
            this.CharSet = Setup.getString("CharSet");
        }
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
        Log.d("LOG_TAG", "IPmac "+IPmac);


        // Toast.makeText(context, "Start Web Server", Toast.LENGTH_LONG).show();
        Thread myThready;
        myThready = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(WebServer.port);
                    while (process == true) {
                        numComp++;
                        Socket socket = ss.accept();
                        if (callbackSocketConnect != null) {
                            callbackSocketConnect.call(socket);
                        } else if (callbackSocketConnectIO != null) {
                            callbackSocketConnectIO.call(socket, socket.getInputStream(), socket.getOutputStream());
                        } else {
                            new Thread(new SocketProcessor(socket)).start();
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });
        myThready.start();    //Запуск потока

    }


    private class SocketProcessor implements Runnable {
        private Socket socket;
        private InputStream is;
        private OutputStream os;
        private String contentZapros = "";
        private JSONObject Headers = new JSONObject();
        private ByteArrayOutputStream PostByte = new ByteArrayOutputStream();

        private SocketProcessor(Socket socket) throws Throwable {
            this.socket = socket;
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            String Adress = socket.getRemoteSocketAddress().toString();
            Headers.put("RemoteIPAdress", Adress.split(":")[0]);
            Headers.put("OutputStream", os);
            Headers.put("InputStream", is);
            Headers.put("Socket", socket);
            Headers.put("DB", DB);
            Headers.put("IP", Adress.split(":")[0]);
            Headers.put("NameComp", socket.getInetAddress().getCanonicalHostName());
            Adress = Adress.split(":")[0];
            // Adress = Adress.substring(1, Adress.length());
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void run() {
            try {
                if (callbackSocketReadHead != null) {
                    callbackSocketReadHead.call(Headers);
                } else {
                    if (readInputHeaders()) {
                        //Log.d("LOG_TAG", Headers.getString("Query"));
                        String str = Headers.getString("Query");
                        if (pagesList.containsKey(str)) {
                            PageObj pageObj = pagesList.get(str);
                            Headers.put("ContentType", pageObj.ContentType);
                            pageObj.callback.call(Headers, new Response(Headers));
                        } else if (callbackSocketPage != null) {
                            callbackSocketPage.call(Headers, new Response(Headers));
                        } else {
                            writeResponse();
                        }
                    }
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


        private boolean readInputHeaders() throws IOException, JSONException {
            StringBuffer sb = new StringBuffer();
            StringBuffer sbTmp = new StringBuffer();
            int charInt;
            InputStreamReader isr = new InputStreamReader(is);
            // Читаем заголовок HTML до двух символов "\n"()
            while ((charInt = isr.read()) > 0) {
                if (socket.isConnected() == false) {
                    return false;
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
            // разбираем заголовок
            int indLine = 0;
            for (String TitleLine : sb.toString().split("\r\n")) {
                indLine++;
                if (indLine == 1) { //
                    if (((TitleLine.indexOf("GET ") == -1) && (TitleLine.indexOf("HTTP/1.1") == -1))
                            || ((TitleLine.indexOf("POST ") == -1) && (TitleLine.indexOf("HTTP/1.1") == -1))
                            && (TitleLine.indexOf("OPTIONS ") == -1)
                    ) {
                        // Если не HTML запрос, тогда обрабатываем как  терминальное подключение
                        // terminalQuery(isr, os, sb.toString());
                        TerminalStruct terminalStruct = new TerminalStruct(DB);
                        String rowText = sb.toString();
                        rowText = rowText.replace("\n", "");
                        String[] linesArr = rowText.split("\r");
                        if (linesArr.length > 0) {
                            terminalStruct.DeviceNameClient = linesArr[0];
                        }
                        if (linesArr.length > 1) {
                            terminalStruct.DevicePassClient = linesArr[1];
                        }
                        if (linesArr.length > 2) {
                            terminalStruct.RouterPassClient = linesArr[2];
                        }
                        terminalStruct.inputStream = is;
                        terminalStruct.socket = socket;
                        terminalStruct.outputStream = os;
                        terminalStruct.headText = sb.toString();
                        terminalStruct.DB = DB;
                        terminalStruct.countQuery++;
                        if (callbackSocketTerminal != null) {
                            callbackSocketTerminal.call(terminalStruct);
                        }else{
                            if (callbackSocketTerminalLoop != null) {
                                callbackSocketTerminalLoop.call(terminalStruct);
                            }
                            // Log.d("LOG_TAG", "\r\n2\r\n");
                            while (terminalStruct.isConnect()) {
                                 StringBuffer sbSub2 = new StringBuffer();
                                 while ((sbSub2 = terminalStruct.readText()).toString().length() != 0) {
                                     if (terminalStruct.getStatusExut())return false;
                                     terminalStruct.countQuery++;
                                     terminalStruct.headText = sbSub2.toString();
                                     if (callbackSocketTerminalLoop != null) {
                                         callbackSocketTerminalLoop.call(terminalStruct);
                                     }
                                 }
                            }
                        }
                        return false;
                    }
                    if (TitleLine.indexOf("GET ") == -1) {
                        Headers.put("Method", "GET");
                    } else if (TitleLine.indexOf("POST ") == -1) {
                        Headers.put("Method", "POST");
                    }
                    TitleLine = TitleLine.replaceAll("GET /", "");
                    TitleLine = TitleLine.replaceAll("POST /", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.1", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.0", "");
                    contentZapros = java.net.URLDecoder.decode(TitleLine, "UTF-8");
                    if (contentZapros.indexOf("?") != -1) {
                        String tmp = contentZapros.substring(0, contentZapros.indexOf("?") + 1);
                        String param = contentZapros.replace(tmp, "");
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
                        contentZapros = "index.html";
                    }
                    Headers.put("Query", contentZapros);
                    Headers.put("RootPath", sdcard.getAbsolutePath());
                    File pathPege = new File(sdcard.getAbsolutePath() + "/" + contentZapros);
                    Headers.put("AbsalutZapros", pathPege.getAbsolutePath());
                    // Если в запросе встречается символ "=" тогда тоже разбиваем Key=Val помещаем в параметры заголовка
                    if (contentZapros.indexOf("=") != -1) {
                        int indParam = 0;
                        for (String par : contentZapros.split("&")) {
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
                    }
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
            if (Headers.has("Content-Type") == true) {
                // Content-Type: text/html; charset=windows-1251
                if (Headers.getString("Content-Type").split("charset=").length == 2) {
                    String Koderovka = Headers.getString("Content-Type").toString().split("charset=")[1];
                    Headers.put("CharsetInput", Koderovka);
                    //  Json.put("Charset", Koderovka);
                }
            }

            if (Headers.has("Content-Length") == true) {
                int lengAll = Headers.getInt("Content-Length");
                StringBuffer sbTmpPost = new StringBuffer();

                // CharBuffer sbPost = CharBuffer.allocate(lengAll);
                int charInt1 = -1;
                while ((charInt1 = isr.read()) > 0) {
                    PostByte.write(charInt1);
                    sbTmpPost.append((char) charInt1);
                    lengAll--;
                    if (lengAll == 0) {
                        break;
                    }
                    if (socket.isConnected() == false) {
                        return false;
                    }
                }
                Headers.put("PostBodyText", sbTmpPost.toString());
                Headers.put("PostBodyByte", PostByte);

                // PostByte = baos.toByteArray();
                // Headers.put("PostBodyText", JSON.encode(baos));
                // Headers.put("PostBodyByte", baos.toByteArray());
                // LocalMessage.put("PostBodyByte", baos.toByteArray());
                // LocalMessage.put("PostBodyText", sbTmpPost.toString());
            }
            // Парсим Cookie если он есть
            if (Headers.has("Cookie") == true) {
                String Cookie = Headers.getString("Cookie");
                Cookie = Cookie.substring(1, Cookie.length());// убираем лишний пробел сначала строки
                for (String elem : Cookie.split("; ")) {
                    String[] val = elem.split("=");
                    Headers.put(val[0], val[1]);
                    val[0] = val[0].replace(" ", "_");
                    Headers.put(val[0], val[1]);
                    Headers.put(val[0], val[1]);
                    // inParam.put(val[0], val[1]);
                }
            }

            sb.setLength(0);
            return true;
        }

        private void writeResponse() {
            sendJson(os, "{\"ok\":true}");
        }
    }


    public static void sendHtml(JSONObject Head, String content) {
        try {
            OutputStream os = (OutputStream) Head.get("OutputStream");
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            // дата создания в GMT
            DateFormat df = DateFormat.getTimeInstance();
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            // Длина файла
            os.write(("Content-Length: " + content.length() + "\r\n").getBytes());
            os.write(("Content-Type: text/html; charset=utf-8\r\n").getBytes());
            // Остальные заголовки
            os.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            os.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
            os.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
            os.write("Connection: close\r\n".getBytes());
            os.write("Server: HTMLserver\r\n\r\n".getBytes());
            os.write(content.getBytes(Charset.forName("UTF-8")));
            os.write(0);
            os.flush();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Класс для группировки методов запускаемых внутри
     */
    public class Response {
        private JSONObject Head;

        public Response(JSONObject Head) throws JSONException {
            this.Head = Head;
            Iterator<String> keys = this.Head.keys();
            JSONObject clone = new JSONObject(this.Head.toString());
        }
        public JSONObject shallowCopy(JSONObject original) {
            JSONObject copy = new JSONObject();
            for ( Iterator<String> iterator = original.keys(); iterator.hasNext(); ) {
                String      key     = iterator.next();
                JSONObject  value   = original.optJSONObject(key);
                try {
                    copy.put(key, value);
                } catch ( JSONException e ) {
                    //TODO process exception
                }
            }
            return copy;
        }

        public void Head() throws IOException, JSONException {
                OutputStream os = (OutputStream) Head.get("OutputStream");
                String pageFile = Head.getString("Query");
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                // дата создания в GMT
                DateFormat df = DateFormat.getTimeInstance();
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                // Длина файла
                String TypeCont = Head.getString("ContentType");
                os.write(("Content-Type: " + TypeCont + "; charset=utf-8\r\n").getBytes());
                // Остальные заголовки
                os.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                os.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                os.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n\r\n".getBytes());
        }

        public void Body(String content) throws IOException, JSONException {
                OutputStream os = (OutputStream) Head.get("OutputStream");
                os.write(content.getBytes(Charset.forName("UTF-8")));
        }

        public void End() {
            try {
                OutputStream os = (OutputStream) Head.get("OutputStream");
                //os.write(0);
                os.flush();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        public void JSON(String jsonObject) {
            try {
                OutputStream os = (OutputStream) Head.get("OutputStream");
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
                //Log.d("TAG", jsonObject);
                os.write(jsonObject.getBytes(Charset.forName("UTF-8")));
                // os.write(jsonObject.getBytes(), 0, jsonObject.length());
                os.write(0);
                os.flush();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        public void sendRawFile(File pageFile) {
            try {
                OutputStream os = (OutputStream) Head.get("OutputStream");
                FileReader fileInput = new FileReader(pageFile);
                String Code = fileInput.getEncoding();
                fileInput.close();
                String TypeCont = Head.getString("ContentType");
                // Первая строка ответа
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                // дата создания в GMT
                DateFormat df = DateFormat.getTimeInstance();
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                // Время последней модификации файла в GMT
                os.write(("Last-Modified: " + df.format(new Date(pageFile.lastModified())) + "\r\n").getBytes());
                // Длина файла
                os.write(("Content-Length: " + pageFile.length() + "\r\n").getBytes());
                os.write(("Content-Type: " + TypeCont + "; ").getBytes());
                os.write(("charset=" + Code + "\r\n").getBytes());
                // Остальные заголовки
                os.write("Access-Control-Allow-Origin: *\r\n".getBytes());
                os.write("Access-Control-Allow-Credentials: true\r\n".getBytes());
                os.write("Access-Control-Expose-Headers: FooBar\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n\r\n".getBytes());
                // Отправка бинарного файла
                FileInputStream fis = new FileInputStream(pageFile.getAbsolutePath());
                int lengRead = 1;
                byte buf[] = new byte[1024];
                while ((lengRead = fis.read(buf)) != -1) {
                    os.write(buf, 0, lengRead);
                }
                // закрыть файл
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    /***
     * Структура объекта при обраблтке терминального подключения
     */
    public class TerminalStruct {

        public DBHelper DB;
        public String headText;
        public int countQuery=0;
        public Socket socket;
        public InputStream inputStream;
        public OutputStream outputStream;
        public String DeviceNameSendTo = "";
        public String DeviceNameSend = "";
        public String DeviceNameClient = "";
        public String DevicePassClient = "";
        public String RouterPassClient = "";
        private boolean isExit =false;
        JSONObject JSONObj = new JSONObject();
        JSONArray JSONArr = new JSONArray();


        public TerminalStruct(DBHelper DB){
            this.DB = DB;
        }

        public boolean isConnect() throws IOException {
            return socket.isConnected();
        }

        public void write(String string) throws IOException {
            outputStream.write(string.getBytes());
            outputStream.flush();
        }
        public void write(int _byte) throws IOException {
            outputStream.write(_byte);
            outputStream.flush();
        }

        public StringBuffer readText() throws IOException {
            int subcharInt;
            StringBuffer sbSubTmp = new StringBuffer();
            StringBuffer sbSub = new StringBuffer();
            while ((subcharInt = inputStream.read()) != -1) {
                if (socket.isConnected() == false) break;
                if (subcharInt == 0) break;
                sbSubTmp.append((char) subcharInt);
                if (sbSubTmp.toString().indexOf("\n") != -1) {
                    if (sbSubTmp.toString().length() == 2) {
                        break; // чтение заголовка окончено
                    }
                    sbSubTmp.setLength(0);
                }
                sbSub.append((char) subcharInt);
            }
            return sbSub;
        }
        public ByteArrayOutputStream readByteArray() throws IOException {
            ByteArrayOutputStream PostByte = new ByteArrayOutputStream();
            int subcharInt;
            while ((subcharInt = inputStream.read()) != -1) {
                if (socket.isConnected() == false) break;
                if (subcharInt == 0) break;
                PostByte.write(subcharInt);
            }
            return PostByte;
        }

        public boolean getStatusExut() {
            return isExit;
        }

        public boolean exit() {
            isExit= !isExit;
            return isExit;
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
            //Log.d("TAG", jsonObject);
            os.write(jsonObject.getBytes(Charset.forName("UTF-8")));
            // os.write(jsonObject.getBytes(), 0, jsonObject.length());
            os.write(0);
            os.flush();
            // завершаем соединение
            // System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, e);
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
