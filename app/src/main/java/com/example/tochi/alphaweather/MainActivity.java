package com.example.tochi.alphaweather;


import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends ListActivity {

    private static double pLat;
    private static double pLong;

    TextView temp_F1;
    TextView temp_C1;
    TextView weather1;
    TextView city_text;
    TextView day_time;
    TextView temp_view;
    TextView week_day;

    String bestprovider;
    String PREFS = "PREFS";

    String city_state = "";
    String image = "";
    String weather = "";
    String day = "";
    String temp_C ="";
    String temp_F ="";
    String zipcode;

    String ImageUrl = "icon_url";
    String DaysOfTheWeek= "weekday";
    String HighestTem = "high";
    String LowestTem = "low";
    String Fahrenheit_High = "fahrenheit";
    String Celsius_High = "celsius";
    String Fahrenheit_Low = "fahrenheit";
    String Celsius_Low = "celsius";

    private ProgressDialog pDialog;

    private final String TAG = "MainActivity";
    private final Criteria criteria = new Criteria();

    private LocationManager lm;
    LocationListener ll = new myLocationListener();

    private Bitmap bitmap;
    private ImageView imgV;
    Bundle weather_state;

    JSONArray forecastday = null;
    boolean temp_selection;
    ListAdapter adapter;

    ArrayList<HashMap<String, String>> firstList = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> secondList = new ArrayList<HashMap<String, String>>();

    private static String url_conditions = "";
    JSONObject current_observation;

    private static String url_forecast = "";
    boolean temp_preference_user;
    private static int update_freq;

    @Override
    public void onStart() {

        super.onStart();
        SharedPreferences pf = getSharedPreferences("PREFS", 0);
        pLat = pf.getFloat("lat", 0);
        pLong = pf.getFloat("long", 0);
        Log.i(TAG, "on start called");
    }
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences pf = getSharedPreferences("PREFS", 0);
        pLat = pf.getFloat("lat", 0);
        pLong = pf.getFloat("long", 0);

        if (checkNetwork() == true) {

//           new GetWeatherInfo().execute();
            ActivityUpdate();

        }
        if (checkNetwork() == false) {
            new AlertDialog.Builder(this)
                    .setTitle("Network Access")
                    .setMessage("Internet Access not Detected. Please connect your device")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "on Destroy called");
        SharedPreferences pf = getSharedPreferences("PREFS", 0);

        SharedPreferences.Editor editor = pf.edit();
        editor.putFloat("lat", (float) pLat);
        editor.putFloat("long", (float) pLong);
        editor.commit();
    }
    @Override
    public void onStop() {
        super.onStop();

        SharedPreferences pf = getSharedPreferences("PREFS", 0);

        SharedPreferences.Editor editor = pf.edit();
        editor.putFloat("lat", (float) pLat);
        editor.putFloat("long", (float) pLong);
        editor.commit();
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "on Pause called");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "on create called");

        doDirtyWork();
    }
    private void doDirtyWork(){

        SharedPreferences pf = getSharedPreferences("PREFS", 0);
        pLat = pf.getFloat("lat", 0);
        pLong = pf.getFloat("long", 0);

        SharedPreferences val = PreferenceManager.getDefaultSharedPreferences(this);

        temp_preference_user = val.getBoolean("temp_preference_user", false);
        zipcode = val.getString("zipcode_user", "");

        String getRefresh = val.getString("update_frequency_user", "60");
        update_freq = Integer.parseInt(getRefresh) * 1000 * 60;

        city_text = (TextView) findViewById(R.id.city);
        temp_view = (TextView) findViewById(R.id.temperature);
        weather1 = (TextView) findViewById(R.id.description);
        day_time = (TextView) findViewById(R.id.day);
        week_day = (TextView) findViewById(R.id.weekday);

        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/RemachineScript_Personal_Use.ttf");
        Typeface temp_face = Typeface.createFromAsset(getAssets(), "fonts/Honey_Script_SemiBold.ttf");
        Typeface face_Two = Typeface.createFromAsset(getAssets(), "fonts/VertigoPlusFLF-Bold.ttf");

        city_text.setTypeface(face);
        temp_view.setTypeface(temp_face);
        weather1.setTypeface(face_Two);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(true);

        bestprovider = lm.getBestProvider(criteria, true);
        Location location = lm.getLastKnownLocation(bestprovider);

        if (location!= null && zipcode.length() < 5) {
            updateWithNewLocation(location);
        }
        if (location == null){
            lm.requestLocationUpdates(bestprovider, 0, 0, ll);
        }
        lm.requestLocationUpdates(bestprovider, update_freq, 400, ll);

        //Checking to see if the zipcode has been properly entered by the user
        //If a wrong zipcode is entered, the latitude and longitude is used
        if (zipcode.length() < 5){
            url_conditions = "http://api.wunderground.com/api/88732e8702ac67ac/conditions/q/" + String.valueOf(pLat) + "," + String.valueOf(pLong) + ".json";
            url_forecast = "http://api.wunderground.com/api/88732e8702ac67ac/forecast10day/q/" + String.valueOf(pLat) + "," + String.valueOf(pLong) + ".json";
        }
        if (zipcode.length() == 5) {
            url_conditions = "http://api.wunderground.com/api/88732e8702ac67ac/conditions/q/" + zipcode + ".json";
            url_forecast = "http://api.wunderground.com/api/88732e8702ac67ac/forecast10day/q/" + zipcode + ".json";
        }
        if (checkNetwork() == true) {
            new GetWeatherInfo().execute();
        }
        if (checkNetwork() == false) {
            new AlertDialog.Builder(this)
                    .setTitle("Network Access")
                    .setMessage("Internet Access not Detected. Please connect your device")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }
    }
    private class GetWeatherInfo extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog

            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Loading...");
            pDialog.setCancelable(false);
            pDialog.show();
        }
        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();
            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url_conditions, ServiceHandler.GET);
            String jsonStr_fore = sh.makeServiceCall(url_forecast, ServiceHandler.GET);

            Log.i(TAG, "> " + jsonStr);
            Log.i(TAG, "> " + jsonStr_fore);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONObject jsonObj_fore = new JSONObject(jsonStr_fore);

                    current_observation = jsonObj.getJSONObject("current_observation");

                    weather = current_observation.getString("weather");
                    day = current_observation.getString("observation_time");
                    temp_F = current_observation.getString("temp_f");
                    temp_C = current_observation.getString("temp_c");
                    image = current_observation.getString("icon_url");

                    JSONObject j = current_observation.getJSONObject("display_location");
                    city_state = j.getString("full");

                    JSONObject forc = jsonObj_fore.getJSONObject("forecast");

                    JSONObject simfor = forc.getJSONObject("simpleforecast");
                    forecastday  = simfor.getJSONArray("forecastday");

                    for(int i = 0; i < forecastday.length(); i++){

                        JSONObject c = forecastday.getJSONObject(i);
                        String iconimg = c.getString("icon_url");

                        //Hash Map
                        HashMap<String, String> maps = new HashMap<String, String>();

                        JSONObject date = c.getJSONObject("date");
                        String day = date.getString("weekday");
                        maps.put("weekday", day);

                        JSONObject high = c.getJSONObject("high");
                        String highF = high.getString(Fahrenheit_High);
                        String highC = high.getString(Celsius_High);
                        maps.put("Fahrenheit_High", "H/ " + highF);
                        maps.put("Celsius_High", "H/ " + highC);

                        JSONObject low = c.getJSONObject("low");
                        String lowF = low.getString(Fahrenheit_Low);
                        String lowC = low.getString(Celsius_Low);
                        maps.put("Fahrenheit_Low", "L/ " + lowF);
                        maps.put("Celsius_Low", "L/ " + lowC);

                        firstList.add(maps);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Couldn't get any data from the url");
            }
            bitmap = getBitmapFromURL(image);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            ActivityUpdate();
            showListViewInfo();
        }
    }
    private void ActivityUpdate(){
        if (zipcode.length() < 5){
            url_conditions = "http://api.wunderground.com/api/88732e8702ac67ac/conditions/q/" + String.valueOf(pLat) + "," + String.valueOf(pLong) + ".json";
            url_forecast = "http://api.wunderground.com/api/88732e8702ac67ac/forecast10day/q/" + String.valueOf(pLat) + "," + String.valueOf(pLong) + ".json";
        }
        if (zipcode.length() == 5) {
            url_conditions = "http://api.wunderground.com/api/88732e8702ac67ac/conditions/q/" + zipcode + ".json";
            url_forecast = "http://api.wunderground.com/api/88732e8702ac67ac/forecast10day/q/" +
                    zipcode + ".json";
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        temp_preference_user = pref.getBoolean("temp_preference_user", false);
        city_text.setText(city_state);
        weather1.setText(weather);

        if (temp_preference_user == true){
            temp_view.setText(temp_C + "ºC");
        }
        if (temp_preference_user == false) {
            temp_view.setText(temp_F + "ºF");
        }

        day_time = (TextView) findViewById(R.id.day);
        day_time.setText(day);
        imgV = (ImageView) findViewById(R.id.weather_icon);

        //bitmap = getBitmapFromURL(image);
        imgV.setImageBitmap(bitmap);
        // new LoadImage().execute(image);
    }
    private void updateWithNewLocation(Location location) {
        if (location != null) {
            pLong = location.getLongitude();
            pLat = location.getLatitude();

            Toast.makeText(
                    getApplicationContext(),
                    "Latitude: " + pLat + " Longitude: "
                            + pLong, Toast.LENGTH_SHORT)
                    .show();
        }
    }
    private Bitmap getBitmapFromURL(String image) {
        try {
            URL iconUrl = new URL(image);
            HttpURLConnection c = (HttpURLConnection) iconUrl.openConnection();
            c.setDoInput(true);
            c.connect();
            InputStream input = c.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        }
        catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return null;
        }
    }
    private void showListViewInfo(){

        if (secondList.size() > 0) {
            secondList.clear();
        }
        SharedPreferences value = PreferenceManager.getDefaultSharedPreferences(this);
        temp_selection = value.getBoolean("temp_preference_user", false);
        String getDays = value.getString("forecastdays_user", "3");
        int numberOfDays = Integer.parseInt(getDays);

        for (int i = 0; i < numberOfDays; i++){
            secondList.add(firstList.get(i));
        }

        if(temp_selection == false){
            adapter = new SimpleAdapter(this, secondList,
                    R.layout.weatherlistview, new String[] {DaysOfTheWeek, "Fahrenheit_High", "Fahrenheit_Low", ImageUrl},
                    new int[] {R.id.weekday, R.id.highestTem, R.id.lowestTem, R.id.ImageDay});
        }
        if(temp_selection == true){
            adapter = new SimpleAdapter(this, secondList,
                    R.layout.weatherlistview, new String[] {DaysOfTheWeek, "Celsius_High", "Celsius_Low", ImageUrl},
                    new int[] {R.id.weekday, R.id.highestTem, R.id.lowestTem, R.id.ImageDay});
        }
        ((SimpleAdapter) adapter).setViewBinder(new MyBinder());
        setListAdapter(adapter);
    }
    public class MyBinder implements ViewBinder, android.widget.SimpleAdapter.ViewBinder{
        public boolean setViewValue(View view, Object data, String textRepresentation) {
            if(view.getId() == R.id.ImageDay){
                ImageView iview = (ImageView) view;
                String stringval = (String) data;
                bitmap = getBitmapFromURL(stringval);
                iview.setImageBitmap(bitmap);
                return true;
            }
            return false;
        }
        @Override
        public boolean setViewValue(View arg0, Cursor arg1, int arg2) {
            // TODO Auto-generated method stub
            return false;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.refresh_actionbar:
                refresh_action();
                break;
            case R.id.settings_actionbar:
                settings_Action();
                break;
        }
        return true;
    }
    public void refresh_action()
    {
        this.onCreate(weather_state);
    }
    public void settings_Action() {

        Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(myIntent);
    }
    class myLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(
                    getApplicationContext(),
                    "Provider is Enabled", Toast.LENGTH_SHORT)
                    .show();
        }
        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(
                    getApplicationContext(),
                    "Provider is Disabled", Toast.LENGTH_SHORT)
                    .show();
        }
    }
    public boolean checkNetwork() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnectedOrConnecting())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnectedOrConnecting())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}

