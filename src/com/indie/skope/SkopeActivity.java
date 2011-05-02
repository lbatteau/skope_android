/*
 * SkopeActivity
 * 
 * Version information
 *
 * Date
 * 
 * Copyright notice
 */
package com.indie.skope;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.indie.http.RestClient;
import com.indie.http.RestClient.RequestMethod;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class SkopeActivity extends ListActivity implements LocationListener {	  
	private final String TAG = "SkopeActivity";
	
    private ProgressDialog m_progressDialog = null;
    private ArrayList<Skope> m_skopeList = null;
    private SkopeArrayAdapter m_skopeArrayAdapter;
    private Runnable m_viewSkopes;	/** Called when the activity is first created. */
    private Properties m_properties;
    private LocationManager m_locationManager;
    private String m_locationProvider;
	
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	double latitude, longitude;
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Our custom list adapter SkopeArrayAdapter puts our Skope data into the view
        m_skopeList = new ArrayList<Skope>();
        this.m_skopeArrayAdapter = new SkopeArrayAdapter(this, R.layout.skope_view, new ArrayList<Skope>());
        setListAdapter(this.m_skopeArrayAdapter);
        
        // Set up preferences
        Resources resources = this.getResources();
        AssetManager assetManager = resources.getAssets();

        // Read from the /assets directory
        try {
            InputStream inputStream = assetManager.open("skope.properties");
            m_properties = new Properties();
            m_properties.load(inputStream);
            Log.i(TAG, "properties: " + m_properties);            
        } catch (IOException e) {
            Log.e(TAG, "Failed to open skope property file");
            e.printStackTrace();
        }        
        
        // Get the location manager
		m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the location provider
		// Use default
		Criteria criteria = new Criteria();
		//m_locationProvider = m_locationManager.getBestProvider(criteria, false);
		m_locationProvider = LocationManager.GPS_PROVIDER;
		Location location = m_locationManager.getLastKnownLocation(m_locationProvider);

		// List all providers:
		List<String> providers = m_locationManager.getAllProviders();
		for (String provider : providers) {
			LocationProvider info = m_locationManager.getProvider(provider);
			Log.i(TAG, info.toString());
		}
		
		// Initialize the location fields
		if (location != null) {
			System.out.println("Provider " + m_locationProvider + " has been selected.");
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			Log.i(TAG, "Latitude: " + String.valueOf(latitude));
			Log.i(TAG, "Longitude: " + String.valueOf(longitude));
		} else {
			Log.e(TAG, "Location provider not available");
		}
    }
    
    /**
     * This inner class is run after the Skope web service has been called.
     * It checks if there are any skopes present, and adds them to the 
     * list adapter. After that it dismisses the progress dialog.
     *
     * @author  Lukas Batteau
     */
    private Runnable returnRes = new Runnable() {

        @Override
        public void run() {
        	m_skopeArrayAdapter.clear();
            if(m_skopeList != null && m_skopeList.size() > 0){
            	m_skopeArrayAdapter.notifyDataSetChanged();
                for(int i=0; i<m_skopeList.size(); i++)
                	m_skopeArrayAdapter.add(m_skopeList.get(i));
            }
            m_progressDialog.dismiss();
            m_skopeArrayAdapter.notifyDataSetChanged();
        }
    };


	private void findSkopes() {
		JSONArray jsonResponse = new JSONArray();
		
		// Send current location when retrieving skopes to minimize network load
		Location location = m_locationManager.getLastKnownLocation(m_locationProvider);
		double lat = location.getLatitude();
		double lng = location.getLongitude();
		
		// Set up HTTP client
        RestClient client = new RestClient(m_properties.getProperty("skopeURL"));
        client.AddParam("userId", "1");
        client.AddParam("range", "1000");
        client.AddParam("lat", String.valueOf(lat));
        client.AddParam("lng", String.valueOf(lng));
         
        // Send HTTP request to web service
        try {
            client.Execute(RequestMethod.GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
         
        String response = client.getResponse();
        
        try {
        	jsonResponse = new JSONArray(response);
		} catch (JSONException e) {
			// Log exception
			Log.e(TAG, e.toString());
		}
		
		for (int i=0; i < jsonResponse.length(); i++) {
			try {
				Skope skope = new Skope();
				skope.setUserName(jsonResponse.getJSONObject(i).getString("user_name"));
				skope.setUserEmail(jsonResponse.getJSONObject(i).getString("user_email"));
				m_skopeList.add(skope);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		runOnUiThread(returnRes);
	}
	
	private void update(double latitude, double longitude) {
		// Create runnable class to retrieve skopes in separate thread.  
        m_viewSkopes = new Runnable() {
        	@Override
        	public void run() {
        		findSkopes();
        	}
        };        
        
        // Create and execute thread
        Thread thread =  new Thread(null, m_viewSkopes, "MagentoBackground");
        thread.start();

        // While the thread is running a progress dialog should show. 
        // It will be dismissed by a separate runnable class, that is
        // run after the skopes are retrieved.
        m_progressDialog = ProgressDialog.show(SkopeActivity.this,    
              "Please wait...", "Retrieving data ...", true);
	}
    

    private class SkopeArrayAdapter extends ArrayAdapter<Skope> {
    	private List<Skope> mSkopeList;
    	
    	public SkopeArrayAdapter(Context context, int textViewResourceId,
    			List<Skope> objects) {
    		super(context, textViewResourceId, objects);
    		this.mSkopeList = objects;
    	}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.skope_item, null);
                }
                
                Skope skope = mSkopeList.get(position);
                if (skope != null) {
                        TextView tt = (TextView) v.findViewById(R.id.toptext);
                        TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                        if (tt != null) {
                              tt.setText("Name: "+skope.getUserName());                            }
                        if(bt != null){
                              bt.setText("Email: "+ skope.getUserEmail());
                        }
                }
                return v;
        }
    }
    
	/* Request updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
		m_locationManager.requestLocationUpdates(m_locationProvider, 400, 1, this);
		Location location = m_locationManager.getLastKnownLocation(m_locationProvider);
		update(location.getLatitude(), location.getLongitude());		
    }

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		m_locationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(Location location) {
		double lat = location.getLatitude();
		double lng = location.getLongitude();
		Log.i(TAG, "Latitude: " + String.valueOf(lat));
		Log.i(TAG, "Longitude: " + String.valueOf(lng));
		Toast.makeText(this, "Location changed: " + "Lat  " + String.valueOf(lat) + "Long " + String.valueOf(lng),
				Toast.LENGTH_LONG).show();
		update(lat, lng);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(this, "Enabled new provider " + provider,
				Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, "Disenabled provider " + provider,
				Toast.LENGTH_SHORT).show();
	}    
}