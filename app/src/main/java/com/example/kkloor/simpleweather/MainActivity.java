
package com.example.kkloor.simpleweather;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

     //Code used in requesting runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    //Constant used in the location settings dialog.
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

     // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 100000;

     //The fastest rate for active location updates. Exact. Updates will never be more frequent
     //than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

     //Provides access to the Fused Location Provider API.
    private FusedLocationProviderClient mFusedLocationClient;

    //Provides access to the Location Settings API.
    private SettingsClient mSettingsClient;

     //Stores parameters for requests to the FusedLocationProviderApi.
    private LocationRequest mLocationRequest;

     //Stores the types of location services the client is interested in using. Used for checking
     //settings to determine if the device has optimal location settings.
    private LocationSettingsRequest mLocationSettingsRequest;

     // Callback for Location events.
    private LocationCallback mLocationCallback;

     //Represents a geographical location.
    private Location mCurrentLocation;

    // UI Widgets.
    private Button mGPSButton;
    private TextView mLastUpdateTimeTextView;
    private TextView mCityView;
    private TextView mWeatherDetails, mWeatherSpecifics;

     //Tracks the status of the location updates request. Value changes when the user presses the
     // Start Updates and Stop Updates buttons.
    private Boolean mRequestingLocationUpdates;

    // Time when the location was updated represented as a String.
    private String mLastUpdateTime;

     //Last zipcode used.
    private String mLastZip;
     //Replace with your own API from OpenWeatherMap.org.
    static String API = "26444d1815f1c83b990c2f843147f3ca";

    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
    private final static String KEY_LAST_ZIP = "last-zip";
    private static final String MyPREFERENCES = "MyPrefs" ;

    private SharedPreferences sharedpreferences;

    private Typeface weatherFont;
    private TextView weatherIcon;
    private ConcurrentHashMap<String, String> weatherMap;
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        setContentView(R.layout.main_activity);

        //load weather icon map
        makeWeatherMap(getResources());

        // Locate the UI widgets.
        mGPSButton = (Button) findViewById(R.id.gps_button);
        mCityView = (TextView) findViewById(R.id.city_text);
        mWeatherDetails = (TextView) findViewById(R.id.weather_details);
        mWeatherSpecifics = (TextView) findViewById(R.id.weather_specifics);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.last_update_time_text);
        weatherIcon = (TextView)findViewById(R.id.weather_icon);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        mLastZip = "";


        load(this.getApplicationContext());
        if(validZip(mLastZip) && !mRequestingLocationUpdates){
            updateUI();
            inputZip(mLastZip);
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        updateUI();
    }

    private void zipDialog(Context c) {
        final EditText editZIP = new EditText(c);
        AlertDialog dialog = new AlertDialog.Builder(c)
                .setTitle("Add your location")
                .setMessage("Enter ZIP code")
                .setView(editZIP)
                .setPositiveButton("Find", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String zip = String.valueOf(editZIP.getText());
                        inputZip(zip);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void makeWeatherMap(Resources res){
        String[] weatherKeys = res.getStringArray(R.array.weather_keys);
        String[] weatherValues = res.getStringArray(R.array.weather_values);
        weatherMap = new ConcurrentHashMap<String, String>();
        for(int i = 0; i < weatherKeys.length; i++) {
            weatherMap.put(weatherKeys[i], weatherValues[i]);
        }
    }


    //Location Request Methods

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI();
            }
        };
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        updateUI();
                        break;
                }
                break;
        }
    }

    //Button Methods

    /**
     * Handles the GPS Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startGPSButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Enter Zip button, and requests removal of location updates.
     */
    public void startZipButtonHandler(View view) {
        if(!validZip(mLastZip) || !mRequestingLocationUpdates) {
            zipDialog(MainActivity.this);
        }
        mRequestingLocationUpdates = false;
        setButtonsEnabledState();
        stopLocationUpdates();
        if(validZip(mLastZip)){
            inputZip(mLastZip);
        }
		else{
			clearWeather();
		}
    }

    //Checks if zip exists and is valid
    private boolean validZip(String zip){
        if(zip != null && !zip.isEmpty()){
            String regex = "^[0-9]{5}(?:-[0-9]{4})?$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(zip);
            return matcher.matches();
        }
        return false;
    }

    private void inputZip(String zip){
        save("KEY_LAST_ZIP", zip);
        save("REQUESTING_LOCATION_UPDATES", mRequestingLocationUpdates.toString());
        mLastZip = zip;
        String queryF = String.format("http://api.openweathermap.org/data/2.5/forecast?units=imperial&zip=%s,%s&appid=%s", zip, "US", API);
        String queryC = String.format("http://api.openweathermap.org/data/2.5/weather?units=imperial&zip=%s,%s&appid=%s", zip, "US", API);
        retrieveWeather(queryC, queryF);
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateUI();
                    }
                });
    }

    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
    }

    /**
     * The GPS button is enabled if the user is not requesting location updates. T
     */
    private void setButtonsEnabledState() {
		
        if (mRequestingLocationUpdates) {
            mGPSButton.setEnabled(false);
        } else {
            mGPSButton.setEnabled(true);
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null && mRequestingLocationUpdates == true) {
            double lat = mCurrentLocation.getLatitude();
            double lon = mCurrentLocation.getLongitude();
            String queryF = String.format("http://api.openweathermap.org/data/2.5/forecast?units=imperial&lat=%f&lon=%f&appid=%s", lat, lon, API);
            String queryC = String.format("http://api.openweathermap.org/data/2.5/weather?units=imperial&lat=%f&lon=%f&appid=%s", lat, lon, API);
            retrieveWeather(queryC, queryF);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                        setButtonsEnabledState();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        load(this.getApplicationContext());
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
        updateUI();
    }





    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    // Retrieve weather from weatherBuilder
    private void retrieveWeather(String queryC, String queryF){
		clearWeather();
        WeatherBuilder weatherBuilder = new WeatherBuilder();
        weatherBuilder.buildWeather(queryC, queryF);
        Weather currentWeather = weatherBuilder.getCurrentWeather();
        ArrayList<Weather> weatherList = weatherBuilder.getUpcomingWeather();
        if(currentWeather != null) {
            mCityView.setText(currentWeather.getCity());
            mWeatherDetails.setText(currentWeather.getDescription());
            String current = currentWeather.getTemp() + "\n" + currentWeather.getClouds()
                    + "\n" + currentWeather.getWind() + "\n" + currentWeather.getHumidity();
            mWeatherSpecifics.setText(current);
            mLastUpdateTimeTextView.setText(weatherBuilder.getUpdate());
            weatherIcon.setTypeface(weatherFont);
            weatherIcon.setText(weatherMap.get(currentWeather.getIcon()));
        }
        if(weatherList != null && weatherList.size() > 1) {
            LinearLayout todayContainer = (LinearLayout) findViewById(R.id.container);
            LinearLayout tomorrowContainer = (LinearLayout) findViewById(R.id.container);
            LinearLayout nextContainer = (LinearLayout) findViewById(R.id.container);
            inflateContainer(todayContainer, weatherList, currentWeather.getDay(), "today");
            inflateContainer(tomorrowContainer, weatherList, currentWeather.getDay()+1, "tomorrow");
            inflateContainer(nextContainer, weatherList, currentWeather.getDay()+2, "next");
        }
    }

    private void inflateContainer(LinearLayout container, ArrayList<Weather> weatherList, int day, String today){
        ArrayList<Weather> trimWeather = new ArrayList<>();
        for(Weather weather: weatherList){
            if(day == weather.getDay() || (today.equals("today") && day - 1 == weather.getDay())){
                trimWeather.add(weather);
            }
        }
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(trimWeather.size() > 0){
            View v = inflater.inflate(R.layout.day, null);
            TextView forecastDay = v.findViewById(R.id.forecast_day);
            forecastDay.setText(today);
            container.addView(v);
        }
        for (int v = 0; v < trimWeather.size(); v++) {
            View f = inflater.inflate(R.layout.forecast, null);
            TextView forecastDate = f.findViewById(R.id.forecast_date);
            TextView forecastIcon = f.findViewById(R.id.forecast_icon);
            TextView forecastDetails = f.findViewById(R.id.forecast_details);
            TextView forecastTemp = f.findViewById(R.id.forecast_temp);
            forecastDate.setText(trimWeather.get(v).getTimeOfDay());
            forecastIcon.setTypeface(weatherFont);
            forecastIcon.setText(weatherMap.get(trimWeather.get(v).getIcon()));
            forecastDetails.setText(trimWeather.get(v).getDescription());
            forecastTemp.setText(weatherList.get(v).getTemp());
            container.addView(f);
        }
    }
	
	public void clearWeather(){
			mCityView.setText("");
            mWeatherDetails.setText("");
            mLastUpdateTimeTextView.setText("");
            weatherIcon.setText("");
        mWeatherSpecifics.setText("");
            LinearLayout container = (LinearLayout) findViewById(R.id.container);
            int v = container.getChildCount();
            while (v > 0) {
				container.removeViewAt(0);
                v = container.getChildCount();
            }
		
	}



    //SAVE METHODS - shared pref & instance state

    public void load(Context context){
        sharedpreferences = context.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        String zip = sharedpreferences.getString(KEY_LAST_ZIP, "No zip");
        if (zip != null && !zip.isEmpty()) {
            mLastZip = zip;
        }
    }

    public void load(Bundle savedInstanceState){
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }

        }
    }

    public void save(Bundle savedInstanceState){
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
    }

    public void save(String key, String input){
        //save most recent zip
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(key, input);
        editor.apply();
        editor.commit();
    }

    //Instance State Methods

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        load(savedInstanceState);
        updateUI();

    }

    /**
     * Stores activity data in the Bundle.
     */
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        save(savedInstanceState);

    }
    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates to save battery.
        stopLocationUpdates();
    }

}
