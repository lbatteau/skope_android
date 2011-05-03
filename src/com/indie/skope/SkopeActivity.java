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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.indie.http.BMPFromURL;
import com.indie.http.RestClient;
import com.indie.http.RestClient.RequestMethod;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class SkopeActivity extends ListActivity implements LocationListener {	  
	private static final String TAG = SkopeActivity.class.getName();
	private static final int DIALOG_ENABLEGPS_ID = 0;
	private static final int DIALOG_NONETWORK_ID = 1;

	
    private ProgressDialog m_progressDialog = null;
    private ArrayList<Skope> m_skopeList = null;
    private SkopeArrayAdapter m_skopeArrayAdapter;
    private Runnable m_viewSkopes;	/** Called when the activity is first created. */
    private Properties m_properties;
    private LocationManager m_locationManager;
    private String m_locationProvider;
    private Location m_currentLocation;
	
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
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
    }
    
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        switch(id) {
        case DIALOG_ENABLEGPS_ID:
            builder.setMessage("Yout GPS seems to be disabled, do you want to enable it?")
                   .setCancelable(false)
                   .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                       public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                    	   if(!m_locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER ))
                    	   {
                    	       Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    	       startActivity(myIntent);
                    	   }

                       }
                   })
                   .setNegativeButton("No", new DialogInterface.OnClickListener() {
                       public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                       }
                   });
            dialog = builder.create();
            break;
        case DIALOG_NONETWORK_ID:
        	// Response not present
        	builder.setTitle("Network failure");
        	builder.setMessage("This application requires an internet connection to run")
        	       .setCancelable(false)
        	       .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                SkopeActivity.this.finish();
        	           }
        	       });
        	dialog = builder.create();
        default:
            dialog = null;
        }
        return dialog;
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
		double lat = m_currentLocation.getLatitude();
		double lng = m_currentLocation.getLongitude();
		
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
        	// Most exceptions already handled by client
            e.printStackTrace();
        }
        
        String response = client.getResponse();
        
        // Check if response present
        if (response == null) {
        	showDialog(DIALOG_NONETWORK_ID);
        } else {
        	// Extract JSON data from response
	        try {
	        	jsonResponse = new JSONArray(response);
			} catch (JSONException e) {
				// Log exception
				Log.e(TAG, e.toString());
			}
			
			// Copy the JSON list of objects to our Skope ArrayList
			m_skopeList.clear();
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
        }
		
		runOnUiThread(returnRes);
	}
	
	private void update() {
        // While the thread is running a progress dialog should show. 
        // It will be dismissed by a separate runnable class, that is
        // run after the skopes are retrieved.
        m_progressDialog = ProgressDialog.show(SkopeActivity.this,    
              "", "Loading", true);

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
                        BMPFromURL myBmpFromURL = new BMPFromURL("https://mail.google.com/mail/photos/img/photos/public/AIbEiAIAAABECK-H87OtqeaB2QEiC3ZjYXJkX3Bob3RvKihhNmJlZTAxZGI2MGQyN2NkNzhkMzM3MTAzMmEzM2Q1MjliYWVjODZmMAG9pURXyexr2uUroCRP6EdtZ72tdw");
                        Bitmap myBitmap = myBmpFromURL.getMyBitmap();
                        
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
		if (!m_locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			showDialog(DIALOG_NONETWORK_ID);
		} else {
			m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		}
	
		if (!m_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			//showDialog(DIALOG_ENABLEGPS_ID);		
		} else {
			removeDialog(DIALOG_ENABLEGPS_ID);
			m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		
    }

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		m_locationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(Location location) {
		if (isBetterLocation(location, m_currentLocation)) {
			m_currentLocation = location;
			update();
		}
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

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}}