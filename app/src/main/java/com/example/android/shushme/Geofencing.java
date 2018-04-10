package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.List;

public class Geofencing implements ResultCallback<Status> {

    private Context mContext;
    private GoogleApiClient mClient;
    private List<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    public static final String TAG = Geofencing.class.getSimpleName();
    private static final float GEOFENCE_RADIUS = 50; // 50 meters
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours


    public Geofencing(Context context, GoogleApiClient client) {
        this.mClient = client;
        this.mContext = context;
    }


    public void updateGeofencesList(PlaceBuffer places) {
        if (places == null && places.getCount() == 0) return;

        for (Place place : places) {
            String placeID = place.getId();
            double latitude = place.getLatLng().latitude;
            double longitude = place.getLatLng().longitude;
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeID)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            mGeofenceList.add(geofence);
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    public void registerAllGeofences() {
        if (mClient == null || mClient.isConnected() || mGeofenceList == null || mGeofenceList.size() == 0)
            return;

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mClient,
                    getGeofencingRequest()
                    , getGeofencePendingIntent())
                    .setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, securityException.getMessage());
        }
    }


    public void unRegisterAllGeofences() {
        if (mClient == null || !mClient.isConnected()) return;

        LocationServices.GeofencingApi.removeGeofences(
                mClient,
                getGeofencePendingIntent())
                .setResultCallback(this);

    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.e(TAG, String.format("Error adding/removing geofence : %s",
                               status.getStatus().toString()));
    }
}
