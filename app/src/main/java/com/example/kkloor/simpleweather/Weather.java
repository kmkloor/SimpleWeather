package com.example.kkloor.simpleweather;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Weather object. Used to store data about a single forecast item
 */
public class Weather{
    private String city, temp, icon, description, clouds, time, humidity, wind, timeOfDay ;
    private boolean night;
    private int day;
    private long timeStamp;


    public String getCity() { return city; }
    public String getTemp() { return temp; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public String getClouds() { return clouds; }
    public String getHumidity() { return humidity; }
    public String getWind() { return wind; }
    public String getTime() { return time; }
    public String getTimeOfDay() { return timeOfDay; }
    public boolean isNight() {return night;}
    public int getDay() {return day;}
    public long getTimeStamp() {return timeStamp;}


    public Weather(String city, String temp, String icon, String description,
                   String clouds, long timeStamp, boolean night, String humidity, String wind)  {
        this.city = city.toUpperCase();
        this.temp = trimString(temp) + "\u2109";
        this.icon = icon;
        this.night = night;
        this.description = description.toUpperCase();
        this.clouds = trimString(clouds) + "% cloud cover";
        this.humidity = trimString(humidity) + "% humidity";
        this.wind = trimString(wind) + " mph wind";
        this.timeStamp = timeStamp;
        SimpleDateFormat dfH = new SimpleDateFormat("h:mm a");
        this.time = dfH.format(timeStamp);
        this.night = night;
        Date date = new Date(timeStamp);
        SimpleDateFormat dfD= new SimpleDateFormat("dd");
        this.day = Integer.parseInt(dfD.format(date));
        this.timeOfDay = timeOfDay(timeStamp);
    }

    //Convert the Unix timestamp to a time of day string for UI purposes
    private String timeOfDay(long timeStamp) {
        Date current = new Date(timeStamp);
        SimpleDateFormat df = new SimpleDateFormat("HH");
        int timeCurrent = Integer.parseInt(df.format(current));
        if(timeCurrent <= 4){
            timeOfDay = "night";
            this.day = this.day - 1;
        }
        else if(timeCurrent <= 10 ){
            timeOfDay = "morning";
        }
        else if(timeCurrent <= 16 ){
            timeOfDay = "afternoon";
        }
        else if(timeCurrent <= 22 ){
            timeOfDay = "evening";
        }
        else {
            timeOfDay = "night";
        }
        return timeOfDay;
    }

    //Trim to whole numbers
    private String trimString(String input){
        if(input.contains(".")){
            int i = input.indexOf(".");
            return input.substring(0, i);
        }
        return input;
    }


}



