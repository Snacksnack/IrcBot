package irc.worker;

import irc.main.Utils;

import java.io.ByteArrayOutputStream;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;


/**
 * Calls google for weather information for the given location, parses that and
 * returns it.
 * 
 * @author snack
 * 
 */
public class WeatherAPI {

	/**
	 * Connect with wunderground, parses xml and returns the given data
	 * 
	 * @param location the location to search
	 * @return correct data, or an error
	 */
	public static final String[] getWeather(String location) {
		String data[] = new String[9];
		try {
			String fixedLoc = location.replaceAll(" ", "+");
			fixedLoc = fixedLoc.substring(0, 1).toUpperCase() + fixedLoc.substring(1);
			ByteArrayOutputStream output = Utils.httpRequest("https://api.wunderground.com/api/bf6fcb121000e936/geolookup/conditions/q/" + fixedLoc + ".json");
			// Cut first char, it's a newline and bugs the serializer!
			JSONObject jo = (JSONObject) JSONSerializer.toJSON(output.toString().substring(1));
			JSONObject loc = jo.getJSONObject("location");
			data[0] = loc.getString("city") + ", " + loc.getString("country_name");
			JSONObject obs = jo.getJSONObject("current_observation");
			data[1] = obs.getString("weather");
			data[2] = obs.getString("temp_c");
			data[3] = obs.getString("temp_f");
			data[4] = obs.getString("wind_dir");
			// USA gets mph
			if (loc.getString("country_name").equals("USA")) {
				data[5] = "MPH";
				data[6] = obs.getString("wind_mph");
				data[7] = obs.getString("wind_gust_mph");
			} else {
				data[5] = "KPH";
				data[6] = obs.getString("wind_kph");
				data[7] = obs.getString("wind_gust_kph");
			}
			// Humidity
			data[8] = obs.getString("relative_humidity");
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
		return data;
	}
}
