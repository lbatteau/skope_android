package com.skope.skope.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class CustomHttpClient {

	/**
	 * @see http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html
	 */
	public static class FlushedInputStream extends FilterInputStream {
	    public FlushedInputStream(InputStream inputStream) {
	        super(inputStream);
	    }

	    @Override
	    public long skip(long n) throws IOException {
	        long totalBytesSkipped = 0L;
	        while (totalBytesSkipped < n) {
	            long bytesSkipped = in.skip(n - totalBytesSkipped);
	            if (bytesSkipped == 0L) {
	                  int b = read();
	                  if (b < 0) {
	                      break;  // we reached EOF
	                  } else {
	                      bytesSkipped = 1; // we read one byte
	                  }
	           }
	            totalBytesSkipped += bytesSkipped;
	        }
	        return totalBytesSkipped;
	    }
	}
	
	private static final String TAG = "CustomHttpClient";

	public enum RequestMethod {
		GET, POST, PUT, DELETE
	}
	
	private ArrayList<NameValuePair> params;
	private ArrayList<NameValuePair> headers;
	private ArrayList<NameValuePair> files;
	
	private String url;

	private int responseCode;
	private String message;

	private String response;

	private boolean mUseBasicAuthentication;
	private String mUsername;
	private String mPassword;
	
	private Context mContext;

	public String getResponse() {
		return response;
	}

	public String getErrorMessage() {
		return message;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public CustomHttpClient(String url, Context context) {
		this.url = url;
		mContext = context;
		params = new ArrayList<NameValuePair>();
		headers = new ArrayList<NameValuePair>();
		files = new ArrayList<NameValuePair>();
		mUseBasicAuthentication = false;
	}

	public void addParam(String name, String value) {
		params.add(new BasicNameValuePair(name, value));
	}
	
	public void addBitmapUri(String name, Uri bitmap) {
		files.add(new BasicNameValuePair(name, bitmap.toString()));
	}

	public void addHeader(String name, String value) {
		headers.add(new BasicNameValuePair(name, value));
	}

	public void setUsernamePassword(String username, String password) {
		this.mUsername = username;
		this.mPassword = password;
	}

	public void setUseBasicAuthentication(boolean useBasicAuthentication) {
		mUseBasicAuthentication = useBasicAuthentication;
	}

	public void execute(RequestMethod method) throws Exception {
		switch (method) {
		case GET: {
			// add parameters
			String combinedParams = "";
			if (!params.isEmpty()) {
				combinedParams += "?";
				for (NameValuePair p : params) {
					String paramString = p.getName() + "=";
					if (p.getValue() != null) {
						paramString += URLEncoder.encode(p.getValue(), "UTF-8"); 
					}
					if (combinedParams.length() > 1) {
						combinedParams += "&" + paramString;
					} else {
						combinedParams += paramString;
					}
				}
			}

			HttpGet request = new HttpGet(url + combinedParams);

			// add headers
			for (NameValuePair h : headers) {
				request.addHeader(h.getName(), h.getValue());
			}

			executeRequest(request, url);
			break;
		}
		case PUT: {
			HttpPut request = new HttpPut(url);

			// add headers
			for (NameValuePair h : headers) {
				request.addHeader(h.getName(), h.getValue());
			}

			if (!params.isEmpty()) {
				request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			}
			
			executeRequest(request, url);
			break;
		}
		case POST: {
			HttpPost request = new HttpPost(url);

			// add headers
			for (NameValuePair h : headers) {
				request.addHeader(h.getName(), h.getValue());
			}

			if (files.isEmpty()) {
				if (!params.isEmpty()) {
					request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				}
			} else {
				// Got files, make multipart form
				MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
				// Add regular params
				for(int index=0; index < params.size(); index++) {
		            entity.addPart(params.get(index).getName(), new StringBody(params.get(index).getValue()));
		        }
				// Add files
				for(NameValuePair file : files) {
					// Read name, uri
					String name = file.getName();
					Uri uri = Uri.parse(file.getValue());
					
					// Load bitmap
					WeakReference<Bitmap> bmp = new WeakReference<Bitmap>(
							MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri));
					
					// Convert bitmap to byte array
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bmp.get().compress(Bitmap.CompressFormat.JPEG, 80, stream);
					byte[] byteArray = stream.toByteArray();
					
					// Generate random uuid filename
					String filename = UUID.randomUUID().toString();
					
					// Add bitmap to entity
					entity.addPart(name, new ByteArrayBody(byteArray, filename + ".jpg"));
					
					// Remove temporary file at uri
					File tmpFile = new File(uri.getPath());
					tmpFile.delete();
		        }
				request.setEntity(entity);
			}

			executeRequest(request, url);
			break;
		}
		}
	}

	private void executeRequest(HttpUriRequest request, String url) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
		HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

		HttpClient client = new DefaultHttpClient();

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
		socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		registry.register(new Scheme("https", socketFactory, 443));
		SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
		DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());		

		// Set verifier
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

		// Add credentials
		if (mUseBasicAuthentication) {
			request.addHeader(BasicScheme
					.authenticate(new UsernamePasswordCredentials(mUsername,
							mPassword), "UTF-8", false));
		}

		HttpResponse httpResponse;

		try {

			httpResponse = httpClient.execute(request);
			responseCode = httpResponse.getStatusLine().getStatusCode();
			message = httpResponse.getStatusLine().getReasonPhrase();

			HttpEntity entity = httpResponse.getEntity();

			if (entity != null) {

				InputStream instream = entity.getContent();
				response = convertStreamToString(instream);

				// Closing the input stream will trigger connection release
				instream.close();
			}

		} catch (ClientProtocolException e) {
			client.getConnectionManager().shutdown();
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			client.getConnectionManager().shutdown();
			Log.e(TAG, e.toString());
		}
	}

	private static String convertStreamToString(InputStream is) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}