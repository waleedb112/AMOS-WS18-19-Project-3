package com.gr03.amos.bikerapp;

import android.content.Context;
import android.widget.Toast;

import com.gr03.amos.bikerapp.Models.Event;
import com.gr03.amos.bikerapp.Models.Friend;
import com.gr03.amos.bikerapp.Models.Route;
import com.gr03.amos.bikerapp.Models.User;
import com.gr03.amos.bikerapp.Models.Message;
import com.gr03.amos.bikerapp.NetworkLayer.HttpGetTask;
import com.gr03.amos.bikerapp.NetworkLayer.HttpPostTask;
import com.gr03.amos.bikerapp.NetworkLayer.HttpPutTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import io.realm.Realm;

public class Requests {

    public static final String HOST = "10.0.2.2";
    public static final String PORT = "8080";

    public static JSONObject getResponse(String urlTail, JSONObject json){
        return getResponse(urlTail, json, "POST");
    }

    public static JSONObject getResponse(String urlTail, JSONObject json, String method) {
        try {
            switch (method) {
                case "POST":
                    return new HttpPostTask(urlTail).execute(json.toString()).get();
                case "GET":
                    return new HttpGetTask().execute(urlTail).get();
                case "PUT":
                    return new HttpPutTask(urlTail).execute(json.toString()).get();
                default:
                    return null;
            }
        }catch (InterruptedException | ExecutionException ex){
            ex.printStackTrace();

        }
        return null;
    }

    private static void executeRealmResponse(String urlTail, String jsonName, Class type,Context context){
        try {
            //JsonObject jsonObject = new GetJson().AsJSONObject("http://" + HOST + ":" + PORT + "/RESTfulWebserver/services/" + urlTail);
            //JSONObject obj = new JSONObject(String.valueOf(jsonObject));
            JSONObject obj = null;
            try {
                obj = new HttpGetTask().execute(urlTail).get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(obj == null){
                //Toast.makeText(context,"Service not reachable", Toast.LENGTH_LONG).show();
                return;
            }
            JSONArray jsonString = obj.getJSONArray(jsonName);

            Realm.init(context);
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.createOrUpdateAllFromJson(type, jsonString);
            realm.commitTransaction();
            realm.close();


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static void getJsonResponse(String urlTail, Context context) {
        executeRealmResponse(urlTail, "event", Event.class, context);
    }

    public static void getJsonResponseForRoutes(String urlTail, Context context) {
        executeRealmResponse(urlTail, "route", Route.class, context);
    }

    public static void getJsonResponseForFriends(String urlTail, Context context) {
        executeRealmResponse(urlTail, "friends", Friend.class, context);
    }

    public static void getJsonResponseForFriendsRoutes(String urlTail, Context context) {
        executeRealmResponse(urlTail, "friend", Friend.class, context);

    }

    public static void getJsonResponseForUser(String urlTail, Context context) {
        executeRealmResponse(urlTail, "user", User.class, context);
    }

    public static void getJsonResponseForChat(String urlTail, int chatId, Context context) {
        executeRealmResponse(urlTail + "/" + chatId, "Chat", Message.class, context);
    }

    public static JSONObject getJSONResponse(String tail, JSONObject request, String method) {
        return getResponse(tail, request, method);
    }

    public static String getUrl(String urlTail){
        return "http://" + HOST + ":" + PORT + "/RESTfulWebserver/services/" + urlTail;
    }
}