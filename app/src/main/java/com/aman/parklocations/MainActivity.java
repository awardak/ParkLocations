package com.aman.parklocations;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener{
    private final String TAG = "MainAct";
    private GoogleApiClient googleApiClient;
    private final int PERMISSION_CODE = 123;
    private RecyclerView recyclerView;
    ParkAdapter parkAdapter;
    DataRetriever dataRetriever;

    private final String URL_BASE = "https://data.sfgov.org/resource/94uf-amnx.json";
    private List<Park> parkList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // build GoogleApiClient to use LocationServices API
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        // set up RecyclerView
        recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // set up Adapter for RecyclerView
        dataRetriever = new DataRetriever(getApplicationContext());
        parkAdapter = new ParkAdapter(dataRetriever.getParkList());
        recyclerView.setAdapter(parkAdapter);

        retrieve();
    }

    private class ParkAdapter extends RecyclerView.Adapter<ParkViewHolder> {
        private List<Park> parks;

        public ParkAdapter(List<Park> parks) {
            this.parks = parks;
        }

        @Override
        public ParkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_park, parent, false);
            return new ParkViewHolder(listItem);
        }

        @Override
        public void onBindViewHolder(ParkViewHolder holder, int position) {
            Park park = parks.get(position);
            holder.bindData(park);
        }

        @Override
        public int getItemCount() {
            return parks.size();
        }
    }
    private class ParkViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private TextView parkName;
        private TextView parkMgr;
        private TextView parkEmail;
        private TextView parkPhone;

        public ParkViewHolder(View itemView) {
            super(itemView);

            parkName = (TextView)itemView.findViewById(R.id.parkName);
            parkMgr = (TextView)itemView.findViewById(R.id.parkMgr);
            parkEmail = (TextView)itemView.findViewById(R.id.parkEmail);
            parkPhone = (TextView)itemView.findViewById(R.id.parkPhone);
        }

        public void bindData(Park park) {
            parkName.setText(park.getName());
            parkMgr.setText(park.getManagerName());
            parkEmail.setText(park.getEmail());
            parkPhone.setText(park.getPhone());
        }

        @Override
        public void onClick(View view) {

        }
    }

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
                parkAdapter = new ParkAdapter(parkList);
                recyclerView.setAdapter(parkAdapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "ERROR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(getApplicationContext()).add(jsonArrayRequest);
    }

    /* this callback is called when GoogleApiClient is successfully connected */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // if we don't have location permission, request it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
            Log.v(TAG, "requesting permissions");

        } else {
            startLocationUpdates();
        }
    }

    /* this callback is called after the user is asked for location permissions */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    Log.v(TAG, "permission denied by user");
                }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended() called");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, "onLocationChanged() called - lat = " + location.getLatitude() + " lon = " + location.getLongitude());
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed() called");
    }

    private void startLocationUpdates() {
        Log.v(TAG, "startLocationUpdates() called");

        try {
            LocationRequest locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } catch (SecurityException exc) {
            Log.e(TAG, "location security exception");
        }
    }
}
