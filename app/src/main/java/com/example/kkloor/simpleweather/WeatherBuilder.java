
package com.example.kkloor.simpleweather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class WeatherBuilder {

    private Weather currentWeather;
    private ArrayList<Weather> upcomingWeather;
    private String city;
    private String update;
    private long sunrise;
    private long sunset;
    private long timeConversion;
    private long localTime;
    static String timeAPI = "HKHSQ3FK9KXR";

    public Weather getCurrentWeather(){
        return currentWeather;
    }
    public ArrayList<Weather> getUpcomingWeather(){
        return upcomingWeather;
    }
    public String getUpdate() {
        return update;
    }

    public void buildWeather(String queryC, String queryF){
        buildCurrent(queryC);
        buildForecast(queryF);
    }
   // retrieve OpenWeatherAPI upcoming forecast
    private void buildForecast(String query){
        upcomingWeather = new ArrayList<Weather>();
        JSONObject forecast = urlHelper(query);
        try {
            JSONArray array = forecast.getJSONArray("list");
            for(int i = 0; i < array.length(); i=i+2){
                Weather weather = createWeather((array.getJSONObject(i)), sunrise, sunset);
                if(!(weather.getTimeOfDay().equals(currentWeather.getTimeOfDay())
                        && weather.getDay() == currentWeather.getDay())
                        && weather.getTimeStamp() > localTime + 3600) {
                    upcomingWeather.add(weather);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //retrieve OpenWeatherAPI current forecast, set weatherBuilder timeConversion, city, sunrise
    // & sunset
    private void buildCurrent(String query){
        JSONObject current = urlHelper(query);
        try {
            String lat = current.getJSONObject("coord").getString("lat");
            String lon = current.getJSONObject("coord").getString("lon");
            this.timeConversion = convertTime(lat, lon);
            this.city = current.getString("name");
            this.sunrise = (current.getJSONObject("sys").getLong("sunrise") * 1000L ) + timeConversion;
            this.sunset =  (current.getJSONObject("sys").getLong("sunset") * 1000L ) + timeConversion;
            currentWeather = createWeather(current, sunrise, sunset);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // turn JSON object into weather object
    private Weather createWeather(JSONObject obj, long sunrise, long sunset) {
        String temp, icon, description, clouds, humidity;
        long timeStamp;
        boolean night;
        int wind;
        temp = icon = description = clouds = humidity = "";
        timeStamp = wind =  0;
        night = false;
        try {
            description = obj.getJSONArray("weather").getJSONObject(0).getString("description");
            temp = obj.getJSONObject("main").getString("temp");
            humidity = obj.getJSONObject("main").getString("humidity");
            clouds = obj.getJSONObject("clouds").getString("all");
            wind = obj.getJSONObject("wind").getInt("speed");
            timeStamp = (obj.getLong("dt") * 1000L) + timeConversion;
            night = setTime(sunrise, sunset, timeStamp);
            icon = setWeatherIcon(night,
                    obj.getJSONArray("weather").getJSONObject(0).getInt("id"),
                    wind);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new Weather(city, temp, icon, description, clouds, timeStamp, night,
                humidity, String.valueOf(wind));
    }

    //retrieve time difference from gmt to change all forecasts to appropriate local time
    private long convertTime(String lat, String lon) {
        long newTime;
        this.localTime = newTime = 0;
        String query = String.format("http://api.timezonedb.com/v2.1/get-time-zone?key=%s&format=json&by=position&lat=%s&lng=%s", timeAPI, lat, lon);
        System.out.println(query);

        JSONObject timeZone = urlHelper(query);
        try{
            newTime = timeZone.getLong("gmtOffset");
            this.localTime = (timeZone.getLong("timestamp") * 1000L);
            Date date = new Date((long)localTime);
            SimpleDateFormat df = new SimpleDateFormat("h:mm a");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.update = df.format(date);
            return newTime;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return newTime;
    }

    //determine if currentTime is day or night based on local sunrise and sunset times
    private boolean setTime(long sunrise, long sunset, long currentTime){
        long days = (currentTime - sunrise) / 3600000 / 24;
        days = days * 3600000 * 24;
        if(currentTime < sunrise + days || currentTime > sunset + days) {
            return true;
        }
        return false;
    }

    //chose weather icon text based on time of day, predicted weather and wind
    private String setWeatherIcon(boolean night, int realID, int wind) {
        int id = realID / 10;
        if(id >= 70 && id < 78 ) {
            id = 7;
        }
        StringBuilder sb = new StringBuilder();
        if(night) {
            sb.append("wi_night_alt_");
        }
        else{
            sb.append("wi_day_");
        }
        switch(id) {
            case 20 : sb.append("thunderstorm");
                break;
            case 21 : sb.append("lightening");
                break;
            case 23 : sb.append("storm_showers");
                break;
            case 30 : sb.append("rain_mix");
                break;
            case 31 : sb.append("showers");
                break;
            case 32 : sb.append("showers");
                break;
            case 50 :
                if(realID < 502) {
                    sb.append("showers");
                } else {
                    sb.append("rain");
                }
                break;
            case 51 : sb.append("sleet");
                break;
            case 52 : sb.append("showers");
                break;
            case 53 : sb.append("showers");
                break;
            case 60 : sb.append("snow");
                break;
            case 61 : sb.append("sleet");
                break;
            case 62 : sb.append("snow");
                break;
            case 7 : if(night) {
                return "wi_night_fog";
            }
                sb.append("fog");
                break;
            case 78 : return "wi_tornado";
            case 80 : if(realID == 800 && !night) {
                return "wi_day_sunny";
            }
            else if(realID == 800){
                return "wi_night_clear";
            }
                sb.append("cloudy");
                if (wind >= 5) {
                    sb.append("_gusts");
                }
                break;
        }
        return sb.toString();
    }

    //Condense the JSON API query method that is used in buildCurrent, buildForecast and convertTime
    private JSONObject urlHelper(String query) {
        URL url;
        HttpURLConnection connection = null;
        JSONObject object = null;
        try {
            url = new URL(query);

            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(false);

            BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                    Charset.forName("UTF-8")));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }

            String s = sb.toString();
            object = new JSONObject(s);
        }
        catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        return object;
    }
}
