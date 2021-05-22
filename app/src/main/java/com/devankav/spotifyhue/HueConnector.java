package com.devankav.spotifyhue;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HueConnector {

    public static final String PREFIX = "http://";
    public static final String SUFFIX = "/api";
    public static final String DISCOVERY_URL = PREFIX + "discovery.meethue.com";

    private GlobalRequestQueue queue;

    public HueConnector(Context context) {
        this.queue = new GlobalRequestQueue(context);
    }

    public void reconnect(String ip, String username) {

    }

    public BridgeStatus connect(String ip) {
        final BridgeStatus[] result = {BridgeStatus.FAILED_TO_CONNECT};

        Response.Listener<JSONArray> listener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Log.d("HueConnector", response.toString());
                try {
                    JSONObject body = response.getJSONObject(0);

                    if (body.get("error") != null) {
                        result[0] = BridgeStatus.LINK_BUTTON_NOT_PRESSED;
                    } else {
                        result[0] = BridgeStatus.CONNECTED;
                    }

                    Log.d("HueConnector", body.get("error").toString());
                    //Log.d("HueConnector", body.get("type").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("HueConnector", error.getMessage());
            }
        };

        try {
            String url = PREFIX + ip + SUFFIX;
            JSONObject body = new JSONObject();
            body.put("devicetype", "spotify_hue#android");
            JsonArrayBodyRequest jsonRequest = new JsonArrayBodyRequest(Request.Method.POST, url, body, listener, errorListener);
            queue.getRequestQueue().add(jsonRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result[0];
    }

    public Map<String, String> getAllBridges() {
        Map<String, String> bridges = new HashMap<String, String>();

        Response.Listener<JSONArray> listener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject current = response.getJSONObject(i);
                        String id = current.getString("id");
                        String ipAddress = current.getString("internalipaddress");

                        bridges.put(id, ipAddress);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("HueConnector", error.getMessage());
            }
        };

        JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, DISCOVERY_URL, null, listener, errorListener);

        return bridges;
    }
}
