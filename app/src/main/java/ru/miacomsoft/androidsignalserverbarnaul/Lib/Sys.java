package ru.miacomsoft.androidsignalserverbarnaul.Lib;


import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Created by myasnikov on 25.12.15.
 */
public class Sys {

    public static HashMap<String, String> MESSAGE_LIST = new HashMap<String, String>(10, (float) 0.5);
    public static Hashtable<String, Socket> DeviceSocket = new Hashtable<String, Socket>(10, (float) 0.5);
    public static Hashtable<String, InputStreamReader> DeviceIStream = new Hashtable<String, InputStreamReader>(10, (float) 0.5);
    public static Hashtable<String, OutputStream> DeviceOStream = new Hashtable<String, OutputStream>(10, (float) 0.5);
    public static Hashtable<String, String> DevicePass = new Hashtable<String, String>(10, (float) 0.5);

}
