package com.aman.parklocations;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aman.parklocations.model.Park;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener{
    public static final String TAG = "MainActivity";
    private final int PERMISSION_CODE = 123;

    private GoogleApiClient googleApiClient;
    private RecyclerView recyclerView;
    ParkAdapter parkAdapter;
    TextView tvEmpty;               // textview used in case we can't download park data or get user's location

    /* shared preferences are used to store user's current lat/lon in case we want to use them elsewhere */
    private SharedPreferences sharedPreferences;

    /* The following booleans provide an easy way to record when both:
        - data has been retrieved from API and
        - current location has been detected

        Only then do we update the UI.
     */
    private boolean dataRetrieved = false;
    private boolean locationChanged = false;

    private final String URL_BASE = "https://data.sfgov.org/resource/z76i-7s65.json";

    /* ideally, the list of parks should be in some singleton, but this app is small enough for it to be here */
    private List<Park> parkList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        tvEmpty = (TextView)findViewById(R.id.empty);

        // if no network connection, display error and return
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            setTvEmpty("Network Connection Failed");
            return;
        }

        // build GoogleApiClient to use LocationServices API
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        // set up RecyclerView
        recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        parkAdapter = new ParkAdapter(parkList);
        recyclerView.setAdapter(parkAdapter);

        // retrieve park data from the API endpoint
        retrieveData();
    }

    /* The UI is updated only after data has been retrieved AND user location has been determined */
    private void updateUI() {
        if (dataRetrieved && locationChanged) {
            sortData();
            // recyclerView.setAdapter(new ParkAdapter(parkList));
            parkAdapter.notifyDataSetChanged();
        }
    }

    private void setTvEmpty(String msg) {
        tvEmpty.setText(msg);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    /*
    retrieveData() uses the Volley library to retrieve park data and stores that data in a list
     */
    public void retrieveData() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL_BASE, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    /* skip first item in response because it's just header text */
                    for (int i = 1; i < response.length(); ++i) {
                        JSONObject obj = response.getJSONObject(i);

                        String name = obj.getString("parkname");
                        String mgr = obj.getString("psamanager");
                        String email = obj.getString("email");
                        String number = obj.getString("number");

                        /* some of the objects in the response don't have a location, hence, use optJSONObject() */
                        JSONObject locObj = obj.optJSONObject("location_1");

                        String latitude = null;
                        String longitude = null;
                        if (locObj != null) {
                            latitude = locObj.getString("latitude");
                            longitude = locObj.getString("longitude");
                        }

                        parkList.add(new Park(name, mgr, email, number, latitude, longitude));
                    }

                    dataRetrieved = true;
                    updateUI();

                } catch (JSONException e) {
                    Log.e(TAG, "ERROR: " + e.getLocalizedMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "ERROR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(getApplicationContext()).add(jsonArrayRequest);
    }

    private void sortData() {
        double user_latitude = Double.valueOf(sharedPreferences.getString("latitude", "-1"));
        double user_longitude = Double.valueOf(sharedPreferences.getString("longitude", "-1"));

        // for each park, record the distance of the park from the user's current location
        for (Park park : parkList) {
            String latitude = park.getLatitude();
            String longitude = park.getLongitude();

            // some of the parks don't have lat/lon info; the distance we set for these is the max
            if (latitude == null || longitude == null) {
                park.setDistanceFromUser(Float.MAX_VALUE);
            } else {
                double park_latitude = Double.valueOf(latitude);
                double park_longitude = Double.valueOf(longitude);
                float results[] = new float[1];
                Location.distanceBetween(user_latitude, user_longitude, park_latitude, park_longitude, results);
                park.setDistanceFromUser(results[0]);
            }
        }

        // sort the list according to the distances
        Collections.sort(parkList, new Comparator<Park>() {
            @Override
            public int compare(Park p1, Park p2) {
                return (int) (p1.getDistanceFromUser() - p2.getDistanceFromUser());
            }
        });
    }

    /* this callback is called when GoogleApiClient is successfully connected */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // if we don't have location permission, request it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);

        } else {
            startLocationUpdates();
        }
    }

    /* this callback is called when the user is asked for location permissions */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    setTvEmpty("Denied Location Permission");
                }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended() called");
        setTvEmpty("Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed() called");
        setTvEmpty("Connection Failed");
    }

    /* called if user agrees to allow location permission */
    private void startLocationUpdates() {
        try {
            LocationRequest locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } catch (SecurityException exc) {
            Log.e(TAG, "ERROR: " + exc.getLocalizedMessage());
            setTvEmpty("Internal Error");
        }
    }

    /* called when user location has changed; saves latitude and longitude in shared preferences
        in case we want to use it elsewhere
     */
    @Override
    public void onLocationChanged(Location location) {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("latitude", Double.toString(location.getLatitude()));
        editor.putString("longitude", Double.toString(location.getLongitude()));

        // test data: 37.754338, -122.473461

        editor.commit();

        locationChanged = true;
        updateUI();
    }
}
