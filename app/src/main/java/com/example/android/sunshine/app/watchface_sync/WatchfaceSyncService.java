package com.example.android.sunshine.app.watchface_sync;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

/**
 * Created by Rohan on 30-Dec-16.
 */

public class WatchfaceSyncService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //    private static final String LOG_TAG = WatchfaceSyncService.class.getSimpleName();
    private static final String LOG_TAG = "xxxxxxx";

    private static final String WEATHER_PATH = "/update-weather";

    private static final String[] WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    static WatchfaceSyncService watchfaceSyncService;
    static GoogleApiClient mGoogleApiClient;
    static Context mContext;


    private WatchfaceSyncService(Context context) {

        mContext = context;

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.e(LOG_TAG, "mGoogleApiClient.connect()");
        mGoogleApiClient.connect();
    }

    public static WatchfaceSyncService getInstance(Context context) {
        if (watchfaceSyncService == null) {
            watchfaceSyncService = new WatchfaceSyncService(context);
        }
        return watchfaceSyncService;
    }

    public void updateWatchface() {

        Uri weatherUri = WeatherContract.WeatherEntry.CONTENT_URI;

        // we'll query our contentProvider, as always
        Cursor cursor = mContext.getContentResolver().query(weatherUri, WEATHER_PROJECTION, null, null, null);
        Log.e(LOG_TAG, cursor.getCount() + "");

        if (cursor.moveToFirst()) {
            Log.e(LOG_TAG, "cursor");
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);

            double max = cursor.getDouble(INDEX_MAX_TEMP);
            int maxTemp = (int) Math.round(max);

            double min = cursor.getDouble(INDEX_MIN_TEMP);
            int minTemp = (int) Math.round(min);

            int iconId = SunshineWeatherUtils.getSmallArtResourceIdForWeatherCondition(weatherId);
            Bitmap iconBitmap = BitmapFactory.decodeResource(mContext.getResources(), iconId);

            PutDataMapRequest mapRequest = PutDataMapRequest.create(WEATHER_PATH).setUrgent();
            mapRequest.getDataMap().putString("max_temp", maxTemp + "°");
            mapRequest.getDataMap().putString("min_temp", minTemp + "°");
            mapRequest.getDataMap().putAsset("weather_icon", createAssetFromBitmap(iconBitmap));
            mapRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

            PutDataRequest request = mapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallbacks<DataApi.DataItemResult>() {
                @Override
                public void onSuccess(DataApi.DataItemResult dataItemResult) {
                    Log.e(LOG_TAG, "Data from app to wear sent successfully");
                }

                @Override
                public void onFailure(Status status) {
                    Log.e(LOG_TAG, "Data transmission unsuccessful");
                }
            });
        }

    }


    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(LOG_TAG, "onConnected()");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LOG_TAG, "onConnectionSuspended()");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "onConnectionFailed() " + connectionResult.getErrorMessage() + " " + connectionResult.getErrorCode());
    }
}
