package com.aman.parklocations;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 7/30/16.
 */
public class DataRetriever {
    private Context context;
    private final String TAG = "DataRetriever";
    private final String URL_BASE = "https://data.sfgov.org/resource/94uf-amnx.json";
    private List<Park> parkList = new ArrayList<>();

    public DataRetriever(Context context) {
        this.context = context;
    }

    /*
    retrieve() uses the Volley library to retrieve park data from the API endpoint and stores that data in a list.
     */
    public void retrieve() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL_BASE, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    Log.v(TAG, "response.length(): " + response.length());

                    for (int i = 1; i < response.length(); ++i) {
                        JSONObject obj = response.getJSONObject(i);

                        Park park = new Park(obj.getString("parkname"),
                                obj.getString("psamanager"),
                                obj.getString("email"),
                                obj.getString("number"));

                        parkList.add(park);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "ERROR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(context).add(jsonArrayRequest);
    }

    public List<Park> getParkList() {
        return parkList;
    }
}
