package ru.miacomsoft.androidsignalserverbarnaul.Lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Time;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 *  Самописная ORM для работы с SQLLite
 *
 * //  делаем запрос
 * //  SELECT * FROM (   SELECT * FROM "+ TabName +"  ORDER BY id DESC    ) topfunny LIMIT 1;
 * //  String sqlPole = "SELECT * FROM (   SELECT * FROM " + TabName + "  ORDER BY id DESC    ) topfunny LIMIT 1";
 *
 * @code {
 * DBHelper sqlite = new DBHelper(this); // создаем экземпляр класса
 * sqlite.dropTable("test");             // Удаление таблицы
 * <p>
 * JSONObject raw = new JSONObject();
 * try {
 * raw.put("HostCol", "128.0.24.172");
 * raw.put("PortCol", 8266);
 * raw.put("floatCol", 1.2);
 * raw.put("doubleCol", 1.200001d);
 * sqlite.insertJson("test", raw);
 * } catch (JSONException e) {
 * e.printStackTrace();
 * }
 * <p>
 * JSONArray arr = sqlite.sql("select * from test",null);
 * Log.i("SQL", arr.toString());
 * Log.i("SQL", sqlite.getJson("test",1).toString());
 * <p>
 * <p>
 * JSONObject readJson = sqlite.getJson("test", 1);
 * Log.i("SQL", readJson.toString());
 * <p>
 * sqlite.updateJson("test", readJson);
 * <p>
 * DBHelper sqlite; // Объявляем экземпляр класса
 * sqlite = new DBHelper(context); // создаем экземпляр класса
 * <p/>
 * // Создаем хэш таблицу, по которой будут создана таблица SQLite
 * Hashtable<String, Object> raw2 = new Hashtable<String, Object>(10, (float) 0.5);
 * raw2.put("Host", "212.164.223.134");
 * raw2.put("Port", "7002");
 * raw2.put("NameSpace", "User");
 * raw2.put("User", "_SYSTEM");
 * raw2.put("Pass", "sys");
 * Hashtable<Object, Hashtable<String, Object>> raw2List = new Hashtable<Object,Hashtable<String, Object>>(10, (float) 0.5);
 * <p/>
 * // Добавляем запись в SQList , если таблица несозданна , она создастся по образу этой ХЭШ таблице
 * sqlite.addRaw("Имя таблицы", raw2);
 * <p/>
 * // Добавляем группу записей в SQList , если таблица несозданна , она создастся по образу этой ХЭШ таблице
 * sqlite.addRawList("Имя таблицы", raw2List);
 * <p/>
 * // получить количество записей в таблице
 * int count = sqlite.getCountRaw("Имя таблицы");
 * <p/>
 * // Удалить таблицу
 * sqlite.dropTable("Имя таблицы");
 * <p/>
 * // Удалить все записи в таблице
 * sqlite.delRaw("ConnectConf", "");
 * <p/>
 * // Удалить записи в таблице по условию
 * sqlite.delRaw("ConnectConf", " id=1 ");
 * }
 * Created by Администратор on 06.08.15.
 */
public class SQLLiteORM extends SQLiteOpenHelper {

    Context context;
    public String IdRaw = "";

    public SQLLiteORM(Context context) {
        super(context, "AppDB", null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
//-------------------------------------------------------------------------------------------

    /**
     * создать таблицу в БД
     *
     * @param TabName    - имя таблицы
     * @param Tab        - Hashtable<"название поля", "значение"> из "значение"  получаем тип поля
     * @param dropOldTab - флаг для удаления старой таблицы
     */
    public boolean createTab(String TabName, Hashtable<String, Object> Tab, boolean dropOldTab) {
        boolean res = false;
        // SELECT count(*) FROM sqlite_master WHERE type='table' AND name='table_name';
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (dropOldTab == true) {
                // удаление таблицы
                db.execSQL("drop table if exists " + TabName);
            }
            db.execSQL(createSqlTab(TabName, Tab));
            db.close();
            dbHelper.close();
            res = true;
        } catch (Exception ex) {
            res = false;
        }
        return res;
    }

    /**
     * Удаление таблицы из базы данных
     *
     * @param TabName - имя талицы
     */
    public void dropTable(String TabName) {
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("drop table if exists " + TabName);
            db.close();
            dbHelper.close();
        } catch (Exception ex) {
            //   Logger.getLogger(SQLiteProxyAndroid.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Функция создания SQL запроса для создания таблицы
     *
     * @param TabName - имя таблицы
     * @param Tab     - Hashtable<"название поля", "значение"> из "значение"  получаем тип поля
     * @return
     */
    public String createSqlTab(String TabName, Hashtable<String, Object> Tab) {
        final StringBuffer sb = new StringBuffer();
        int numPole = 0;
        sb.append("create table if not exists " + TabName + " ( ");
        sb.append(" id integer primary key autoincrement,");
        for (Enumeration Eraw = Tab.keys(); Eraw.hasMoreElements(); ) {
            numPole++;
            Object key = Eraw.nextElement();
            String typ = Tab.get(key).getClass().getSimpleName();
            if (typ.equals("Byte[]")) {
                typ = "Blob";
            }
            if (typ.equals("Long")) {
                typ = "Bigint";
            }
            if (numPole == 1) {
                sb.append(" " + key + "  " + typ + " ");
            } else {
                sb.append(" , " + key + "  " + typ + " ");
            }
        }
        sb.append(" );");
        return sb.toString();
    }

    /**
     * Функция создания SQL запроса для создания таблицы
     *
     * @param TabName - имя таблицы
     * @param Tab     - Hashtable<"название поля", "значение"> из "значение"  получаем тип поля
     * @return
     */
    public String createSqlTabJson(String TabName, JSONObject Tab) {
        final StringBuffer sb = new StringBuffer();
        Iterator<String> iterator = Tab.keys();
        int numPole = 0;
        sb.append("create table if not exists " + TabName + " ( ");
        sb.append(" id integer primary key autoincrement,");
        while (iterator.hasNext()) {
            numPole++;

            String key = iterator.next();
            Object value = null;
            try {
                value = Tab.get(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String dataType = value.getClass().getSimpleName();
            String typ = "TEXT";
            if (dataType.equalsIgnoreCase("Integer")) {
                typ = "INT";
            } else if (dataType.equalsIgnoreCase("Long")) {
                typ = "BIGINT";
            } else if (dataType.equalsIgnoreCase("Float")) {
                typ = "REAL";
            } else if (dataType.equalsIgnoreCase("Double")) {
                typ = "REAL";
                // } else if (dataType.equalsIgnoreCase("Boolean")) {
                //     // придумать решение для хронения boolean значения
                //     typ = " BOOLEAN CHECK (" + key + " IN (0,1,null)";
            } else if (dataType.equalsIgnoreCase("String")) {
                typ = "TEXT";
            }
            if (numPole == 1) {
                sb.append(" " + key + "  " + typ + " ");
            } else {
                sb.append(" , " + key + "  " + typ + " ");
            }
        }
        sb.append(" );");
        //Log.i("SQL", sb.toString());
        return sb.toString();
    }

    /**
     * вставить JSON объект в таблицу
     *
     * @param TabName
     * @param Tab
     * @return
     */
    public boolean insertJson(String TabName, JSONObject Tab) {
        boolean ret = false;
        // SELECT count(*) FROM sqlite_master WHERE type='table' AND name='table_name';
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // создаем таблицу если она отсутствует
            db.execSQL(createSqlTabJson(TabName, Tab));
            IdRaw = String.valueOf(db.insert(TabName, null, getContentFromHashTabJson(Tab)));
            ret = true;
        } catch (Exception ex) {
            ret = false;
        }
        return ret;
    }

    public boolean updateJson(String TabName, JSONObject Tab) {
        boolean ret = false;
        // SELECT count(*) FROM sqlite_master WHERE type='table' AND name='table_name';
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = getContentFromHashTabJson(Tab);
            //  внести изменения во все записи
            IdRaw = String.valueOf(db.update(TabName, cv, null, null));
            cv.clear();
            ret = true;
        } catch (Exception ex) {
            ret = false;
        }
        return ret;
    }


    /**
     * Добавить одну строчку в таблицу
     *
     * @param TabName
     * @param Tab     Hashtable<"имя поля", "Значение">
     */
    public boolean addRaw(String TabName, Hashtable<String, Object> Tab) {
        boolean ret = false;
        // SELECT count(*) FROM sqlite_master WHERE type='table' AND name='table_name';
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // создаем таблицу если она отсутствует
            db.execSQL(createSqlTab(TabName, Tab));
            IdRaw = String.valueOf(db.insert(TabName, null, getContentFromHashTab(Tab)));
            ret = true;
        } catch (Exception ex) {
            ret = false;
        }
        return ret;
    }

    /**
     * преобразование из таблицы в контент
     *
     * @param Tab
     * @return
     */
    public ContentValues getContentFromHashTab(Hashtable<String, Object> Tab) {
        ContentValues values = new ContentValues();
        Enumeration names = Tab.keys();
        for (Enumeration Eraw = Tab.keys(); Eraw.hasMoreElements(); ) {
            Object key = Eraw.nextElement();
            String typ = Tab.get(key).getClass().getSimpleName();
            if (typ.equals("String")) {
                values.put((String) key, String.valueOf(Tab.get(key)));
            }

            if (typ.equals("Float")) {
                values.put((String) key, (Float) Tab.get(key));
            }

            if (typ.equals("Double")) {
                values.put((String) key, (Double) Tab.get(key));
            }

            if (typ.equals("Integer")) {
                values.put((String) key, (Integer) Tab.get(key));
            }
            if (typ.equals("Long")) {
                values.put((String) key, (Long) Tab.get(key));
            }

            if (typ.equals("Time")) {
                values.put((String) key, String.valueOf((Time) Tab.get(key)));
                //   tv.append("addraw" + key + "-" + typ + "\r\n");
            }

            if (typ.equals("Date")) {
                values.put((String) key, String.valueOf((java.sql.Date) Tab.get(key)));
                //    tv.append("addraw" + key + "-" + typ + "\r\n");
            }

            if (typ.equals("Byte[]")) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ObjectOutputStream o = null;
                try {
                    o = new ObjectOutputStream(b);
                    o.writeObject(Tab.get(key));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                values.put((String) key, b.toByteArray());
                //   tv.append("addraw" + key + "-" + typ + "\r\n");
            }
        }
        return values;
    }


    public ContentValues getContentFromHashTabJson(JSONObject Tab) {
        ContentValues values = new ContentValues();
        Iterator<String> iterator = Tab.keys();
        while (iterator.hasNext()) {
            try {
                String key = iterator.next();
                Object value = null;
                value = Tab.get(key);
                String dataType = value.getClass().getSimpleName();
                //Log.i("SQL", "dataType:" + dataType + "  " + key + "=" + Tab.get(key));
                if (dataType.equalsIgnoreCase("Integer")) {
                    values.put((String) key, (Integer) Tab.get(key));
                } else if (dataType.equalsIgnoreCase("Long")) {
                    values.put((String) key, (Long) Tab.get(key));
                } else if (dataType.equalsIgnoreCase("Float")) {
                    values.put((String) key, (Float) Tab.get(key));
                } else if (dataType.equalsIgnoreCase("Double")) {
                    values.put((String) key, Float.valueOf(String.valueOf(Tab.get(key))));
                    //  } else if (dataType.equalsIgnoreCase("Boolean")) {
                    //      // придумать решение для хронения boolean значения
                    //          if (((Boolean) Tab.get(key)) == true) {
                    //              values.put((String) key, 1);
                    //          } else {
                    //              values.put((String) key, 0);
                    //          }
                } else if (dataType.equalsIgnoreCase("String")) {
                    values.put((String) key, String.valueOf(Tab.get(key)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return values;
    }

    /**
     * Добавить группу строк в таблицу
     *
     * @param TabName
     * @param Tab
     */
    public void addRawList(String TabName, Hashtable<Object, Hashtable<String, Object>> Tab) {
        for (Enumeration Eraw = Tab.keys(); Eraw.hasMoreElements(); ) {
            Object key = Eraw.nextElement();
            // String typ = Tab.get(key).getClass().getSimpleName();
            Hashtable<String, Object> raw = Tab.get(key);
            addRaw(TabName, raw);
        }
    }


    /**
     * Проверка существования таблицы в БД
     *
     * @param TabName
     * @return
     */
    public boolean getIsExistTab(String TabName) {
        boolean isExist = false;
        SQLLiteORM dbHelper = new SQLLiteORM(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE name=\"" + TabName + "\"", null);
        if (cursor.moveToFirst() == true) {
            if (cursor.getCount() > 0) {
                isExist = true;
            } else {
                isExist = false;
            }
        } else {
            isExist = false;
        }
        cursor.close();
        dbHelper.close();
        return isExist;
    }

    /**
     * Получаем количество записей в таблице
     *
     * @param TabName - имя таблицы
     * @return
     */
    public int getCountRaw(String TabName) {
        if (getIsExistTab(TabName) == false) {
            return -1;
        }
        int count = -1;
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor cursor = db.rawQuery("select count(*) from " + TabName, null);
            if (cursor.moveToFirst() == true) {
                count = cursor.getInt(0);
            } else {
                count = -1;
            }
            dbHelper.close();
        } catch (Exception e) {
            count = -1;
        }
        return count;
    }

    private Hashtable<String, String> getColumnName(String TabName) {
        SQLLiteORM dbHelper = new SQLLiteORM(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // получаем список полей
        Hashtable<String, String> pole = new Hashtable<String, String>(10, (float) 0.5);
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + TabName + ")", null);
        int nameIdx = cursor.getColumnIndexOrThrow("name");
        int typeIdx = cursor.getColumnIndexOrThrow("type");
        int notNullIdx = cursor.getColumnIndexOrThrow("notnull");
        int dfltValueIdx = cursor.getColumnIndexOrThrow("dflt_value");
        int ind = -1;
        while (cursor.moveToNext()) {
            ind++;
            String nameTyp = cursor.getString(typeIdx); // получиь тип поля
            String name = cursor.getString(nameIdx);   // получить название поля
            pole.put(name, nameTyp);
        }
        return pole;
    }

    /**
     * преобразовать из курсора в Жэш таб
     *
     * @param cursor
     * @param pole
     * @return
     */
    public Hashtable<Object, Hashtable<String, Object>> getHashTabFromCursor(Cursor cursor, Hashtable<String, String> pole) {
        final Hashtable<Object, Hashtable<String, Object>> rawListres = new Hashtable<Object, Hashtable<String, Object>>(10, (float) 0.5);
        final Hashtable<String, Object> rawRes = new Hashtable<String, Object>(10, (float) 0.5);
        String[] colNames = cursor.getColumnNames();
        int colnum = colNames.length;
        int numRec = 0;
        while (cursor.moveToNext()) {
            rawRes.clear();
            IdRaw = cursor.getString(1);
            numRec++;
            for (int i = 0; i < colnum; i++) {
                String typ = pole.get(colNames[i]);
                // tv.append("read--" + colNames[i] + " typ " + typ + "\r\n");
                // tv.append("read--"+poleName.get(i) +" typ "+poleTyp.get(i)+"\r\n");
                if (typ.equals("String")) {
                    rawRes.put(colNames[i], cursor.getString(i));
                    //    tv.append("read--" + colNames[i] + " typ " + typ + "\r\n");
                }
                if (typ.equals("Double")) {
                    rawRes.put(colNames[i], cursor.getDouble(i));
                    //    tv.append("read--" + colNames[i] + " typ " + typ + "\r\n");
                }
                if (typ.equals("Integer")) {
                    rawRes.put(colNames[i], cursor.getInt(i));
                    //    tv.append("read--" + colNames[i] + " typ " + typ + "\r\n");
                }
                if (typ.equals("Float")) {
                    rawRes.put(colNames[i], cursor.getFloat(i));
                    //    tv.append("read--" + colNames[i] + " typ " + typ + "\r\n");
                }
                if (typ.equals("Bigint")) {
                    rawRes.put(colNames[i], cursor.getLong(i));
                    //    tv.append("read--" + colNames[i] + " typ " + typ + "\r\n");
                }
                //    if (poleTyp.get(i).equals("Date")) {
                //       rawRes.put(poleName.get(i), cursor.getString(i));
                //   }
                //   if (poleTyp.get(i).equals("Time")) {
                //      rawRes.put(poleName.get(i), cursor.getString(i));
                //  }
                //
                if (typ.equals("Blob")) {
                    rawRes.put(colNames[i], cursor.getBlob(i));
                    //   tv.append("read--" + colNames[i] + " typ " + typ + "\r\n");
                }
            }
            rawListres.put(numRec, rawRes);
            //   tv.append(numRec + "\r\n");
        }
        return rawListres;
    }

    /**
     * Получить одну запись по условию
     *
     * @param TabName
     * @param Where
     * @return
     */
    public Hashtable<String, Object> getRaw(String TabName, String Where) {
        // если в запросе нет ограничения количество записей, тогда добавляем его
        /*
        if (!Where.contains("LIMIT")) {
            Where = Where + " LIMIT 1 ";
        }
        */
        Hashtable<Object, Hashtable<String, Object>> ravList = new Hashtable<Object, Hashtable<String, Object>>(10, (float) 0.5);
        ravList = getRawList(TabName, Where);
        Enumeration Eraw = ravList.keys();
        Object key = Eraw.nextElement();
        return ravList.get(key);
    }

    /**
     * Получить список результатов по условию
     *
     * @param TabName
     * @param Where   - текстовая строка для SQL запроса после ckjdf WHERE
     * @return
     */
    public Hashtable<Object, Hashtable<String, Object>> getRawList(String TabName, String Where) {

        SQLLiteORM dbHelper = new SQLLiteORM(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // получаем список полей
        Hashtable<String, String> pole = getColumnName(TabName);

        // делаем запрос
        String sqlPole = "";
        if (Where.length() > 0) {
            sqlPole = "select  * from " + TabName + " where " + Where;
        } else {
            sqlPole = "select  * from " + TabName;
        }
        Cursor cursor = db.rawQuery(sqlPole, null);
        final Hashtable<Object, Hashtable<String, Object>> rawListres = getHashTabFromCursor(cursor, pole);
        return rawListres;
    }


    /***
     *  Получить строки из SQL запроса
     * @param sqlQuery - текст запроса
     * @param selectionArgs - Входящие аргумены
     * @return
     */
    public JSONArray sql(String sqlQuery, String[] selectionArgs) {
        SQLLiteORM dbHelper = new SQLLiteORM(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(sqlQuery, selectionArgs);
        return getHashTabFromCursorEny(cursor);
    }

    /***
     * Удаление записи в таблице
     * @param tabName
     * @param id
     * @return
     */
    public boolean del(String tabName, int id) {
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String sql = "DELETE FROM " + tabName + " WHERE id = " + id;
            Cursor cursor = db.rawQuery(sql, null);
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            return false;
        }
        return true;
    }


    public JSONObject getJson(String tabName, int id) {
        SQLLiteORM dbHelper = new SQLLiteORM(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from " + tabName + " where id=" + id, null);
        final JSONArray tmp = getHashTabFromCursorEny(cursor);
        if (tmp.length() > 0) {
            try {
                return (JSONObject) tmp.get(0);
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Получить список объектов из курсора ХЭШ
     *
     * @param cursor
     * @return
     */
    public JSONArray getHashTabFromCursorEny(Cursor cursor) {
        final JSONArray rawListres = new JSONArray();
        final JSONObject rawRes = new JSONObject();
        String[] colNames = cursor.getColumnNames();
        int colnum = colNames.length;
        int numRec = 0;
        while (cursor.moveToNext()) {
            IdRaw = cursor.getString(1);
            numRec++;
            for (int i = 0; i < colnum; i++) {
                //Log.i("SQL", cursor.getType(i) + "  " + colNames[i] + "=" + cursor.getString(i));
                try {
                    rawRes.put(colNames[i], cursor.getString(i).replace(".", ","));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    // 1 - int
                    if (Integer.valueOf(cursor.getType(i)) == 1) {
                        rawRes.put(colNames[i], cursor.getInt(i));
                    } else if (Integer.valueOf(cursor.getType(i)) == 3) {                 // 3 - String
                        rawRes.put(colNames[i], cursor.getString(i));
                    } else if (Integer.valueOf(cursor.getType(i)) == 2) { // 2 - Double
                        rawRes.put(colNames[i], cursor.getFloat(i));
                        // Log.i("SQL", cursor.getType(i) + "  " + colNames[i] + "=" + Double.valueOf(cursor.getString(i)));
                    } else {
                        rawRes.put(colNames[i], cursor.getString(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
            rawListres.put(rawRes);
            //   tv.append(numRec + "\r\n");
        }
        return rawListres;
    }


    /**
     * Удаляем  запись по ID если id пустое , тогда удаляем запись последней операции
     * если условия пустые , тогда изменяются все записи
     *
     * @param TabName
     */
    public void delRaw(String TabName, String Where) {
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (Where.length() > 0) {
                int clearCount = db.delete(TabName, Where, null);
            } else {
                int clearCount = db.delete(TabName, null, null);
            }
            dbHelper.close();
        } catch (Exception e) {

        }
    }

    /**
     * удаляем записи по условию, если условие пустое , удаляем все записи
     *
     * @param TabName
     * @param Where
     */
    public void delRawList(String TabName, String Where) {
        int count = -1;
        try {
            SQLLiteORM dbHelper = new SQLLiteORM(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (Where.length() > 0) {
                int delCount = db.delete(TabName, Where, null);
            } else {
                int delCount = db.delete(TabName, null, null);
            }
            dbHelper.close();
        } catch (Exception e) {
            count = -1;
        }


    }

    /**
     * Внести изсенения во все записи по условию
     * если условие пустое, тогда изменяются все записи
     *
     * @param TabName
     * @param Tab
     * @param Where
     */
    public void updateRaw(String TabName, Hashtable<String, Object> Tab, String Where) {
        SQLLiteORM dbHelper = new SQLLiteORM(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //   int updCount = db.update("mytable", cv, "id = ?",  new String[] { id });
        // создаем таблицу если она отсутствует
        db.execSQL(createSqlTab(TabName, Tab));
        ContentValues cv = getContentFromHashTab(Tab);
        if (Where.length() > 0) {
            IdRaw = String.valueOf(db.update(TabName, cv, Where, null));
        } else {
            //  внести изменения во все записи
            IdRaw = String.valueOf(db.update(TabName, cv, null, null));
        }

        cv.clear();
        dbHelper.close();
    }


}
