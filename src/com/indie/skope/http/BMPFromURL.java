package com.indie.skope.http;

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
			bitmap = BitmapFactory.decodeStream(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Bitmap getMyBitmap() {
		return bitmap;
	}
}
