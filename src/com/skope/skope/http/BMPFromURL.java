package com.skope.skope.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BMPFromURL {
	private Bitmap bitmap;	

	public BMPFromURL(String imageURL) {
		URL url = null;
		try {
			url = new URL(imageURL);
		} catch (MalformedURLException error) {
			error.printStackTrace();
		}

		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap bmp = BitmapFactory.decodeStream(input);
			bitmap = Bitmap.createScaledBitmap(bmp, 80, 80, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Bitmap getMyBitmap() {
		return bitmap;
	}
}
