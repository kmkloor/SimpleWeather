# SimpleWeather
App that uses phone location services or manually entered ZIP code to display current and upcoming weather. 

Uses <a href="https://openweathermap.org/api">OpenWeatherMap API</a> and <a href="https://timezonedb.com/api">TimeZoneDB API</a> with JSON parsing. Location methods taken from <a href = "https://github.com/googlesamples/android-play-location/tree/master/LocationUpdates">Google Samples - Android Play Location Updates</a>.

## Install
Change timeAPI in [WeatherBuilder](app/src/main/java/com/example/kkloor/simpleweather/WeatherBuilder.java) and API in [MainActivity](app/src/main/java/com/example/kkloor/simpleweather/MainActivity.java).

## Thoughts
My first time working with Android Studio and making an app. Want to explore layout methods more thoroughly - didn't get past LayoutInflater but want to implement a dark mode theme change for night weather. I believe the instance/shared preferences methods work but would like to test on an actual device instead of emulator.
      
