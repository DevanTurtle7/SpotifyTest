/**
 * A helper class that communicates with the Philips hue bridge and API.
 *
 * @author Devan Kavalchek
 */

package com.devankav.spotifyhue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HueConnector {

    public static final String PREFIX = "http://";
    public static final String SUFFIX = "/api";
    public static final String DISCOVERY_URL = "https://discovery.meethue.com";

    private GlobalRequestQueue queue;
    private SharedPreferences sharedPreferences;

    /**
     * The constructor
     * @param context The application context. Used to make JSON requests and access shared preferences
     */
    public HueConnector(Context context) {
        this.queue = new GlobalRequestQueue(context); // Create a new instance of the request queue
        this.sharedPreferences = context.getSharedPreferences("bridgeMem", Context.MODE_PRIVATE); // Get the shared preferences instance
    }

    /**
     * Attempts to reconnect to a bridge that has previously been connected to
     * @param ip The IP address of the bridge
     * @param username The username used for the bridge
     * @return The status of the bridge (whether or not it connected)
     */
    public BridgeStatus reconnect(String ip, String username) {
        return null;
    }

    /**
     * Attempts to connect to a bridge
     * @param ip The IP address of the bridge
     * @return The status of the bridge (whether or not it connected)
     */
    public BridgeStatus connect(String ip) {
        final BridgeStatus result = new BridgeStatus(); // Initialize the bridge state result

        // Create a new response listener
        Response.Listener<JSONArray> listener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    JSONObject body = response.getJSONObject(0); // Get the object that was returned

                    if (body.get("error") != null) { // Check if there was an error
                        // TODO: Add if statements for errors
                        result.updateState(BridgeState.LINK_BUTTON_NOT_PRESSED);
                    } else {
                        result.updateState(BridgeState.CONNECTED);

                        // Save the bridge information in shared preferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        // TODO: Write bridge info to shared prefs
                    }
                } catch (JSONException e) {
                    result.updateState(BridgeState.FAILED_TO_CONNECT);

                    if (e.getMessage() != null) {
                        Log.e("HueConnector", e.getMessage());
                    } else {
                        Log.e("HueConnector", "There was an unknown error while trying to connect.");
                    }
                }
            }
        };

        // Create a new error listener
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                result.updateState(BridgeState.FAILED_TO_CONNECT);

                // Print the error message
                if (error.getMessage() != null) {
                    Log.e("HueConnector", error.getMessage());
                } else {
                    Log.e("HueConnector", "There was an unknown error getting all bridges.");
                }
            }
        };

        try {
            // Create a new json request
            String url = PREFIX + ip + SUFFIX;

            // Create the body of the JSON call
            JSONObject body = new JSONObject();
            String device = Build.MODEL;
            body.put("devicetype", "spotify_hue#" + device);

            JsonArrayBodyRequest jsonRequest = new JsonArrayBodyRequest(Request.Method.POST, url, body, listener, errorListener);

            queue.getRequestQueue().add(jsonRequest); // Make the JSON call
        } catch (JSONException e) {
            result.updateState(BridgeState.FAILED_TO_CONNECT);

            if (e.getMessage() != null) {
                Log.e("HueConnector", e.getMessage());
            } else {
                Log.e("HueConnector", "There was an unknown error");
            }
        }

        return result;
    }

    /**
     * Returns a map containing all of the bridges on the network. Can also update a list view when given
     * an ArrayAdapter and an ArrayList.
     * @param adapter
     * @param arrayList
     * @return A map containing all of the bridges on the network.
     */
    public Map<String, String> getAllBridges(@Nullable ArrayAdapter adapter, @Nullable ArrayList<String> arrayList) {
        Map<String, String> bridges = new HashMap<String, String>(); // Create a new HashMap

        // Create a new listener
        Response.Listener<JSONArray> listener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for (int i = 0; i < response.length(); i++) { // Iterate over all the JSON objects in the response
                    try {
                        JSONObject current = response.getJSONObject(i); // Get the JSON object at the current index
                        String id = current.getString("id"); // Get the id of the bridge
                        String ipAddress = current.getString("internalipaddress"); // Get the ip address of the bridge

                        bridges.put(id, ipAddress); // Add the bridge info the map

                        if (adapter != null && arrayList != null) { // Check if there was an array and an adapter passed in
                            arrayList.add(ipAddress); // Add the bridge to the list
                            adapter.notifyDataSetChanged(); // Notify the adapter
                        }
                    } catch (JSONException e) {
                        if (e.getMessage() != null) {
                            Log.e("HueConnector", e.getMessage());
                        } else {
                            Log.e("HueConnector", "There was an unexpected error while getting all bridges");
                        }
                    }
                }
            }
        };

        // Create a new error listener
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.getMessage() != null) {
                    Log.e("HueConnector", error.getMessage());
                } else {
                    Log.e("HueConnector", "There was an unknown error getting all bridges.");
                }
            }
        };

        JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, DISCOVERY_URL, null, listener, errorListener);
        queue.getRequestQueue().add(jsonRequest); // Make the JSON call

        return bridges;
    }
}
