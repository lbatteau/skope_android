package nl.skope.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Utility {
	public static Bitmap getBitmapFromURL(String src) {
	    try {
	        URL url = new URL(src);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);
	        input.close();
	        connection.disconnect();
	        return myBitmap;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
	
	public static boolean isDateSameDay(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
	}
	
	public static boolean isYesterday(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		Date today = new Date();
		Calendar yesterdayCalendar = Calendar.getInstance();
		yesterdayCalendar.setTime(today);
		yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1);
		return isDateSameDay(calendar.getTime(), yesterdayCalendar.getTime());
		
	}
	

	
}
