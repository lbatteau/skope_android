/*
 * SkopeActivity
 * 
 * Version information
 *
 * Date
 * 
 * Copyright notice
 */
package com.skope.skope.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.skope.skope.R;
import com.skope.skope.utils.Type;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class OOIListActivity extends BaseActivity {	  
	private static final String TAG = OOIListActivity.class.getName();
	
    private ArrayList<ObjectOfInterest> mObjectOfInterestList = null;
    private ObjectOfInterestArrayAdapter mObjectOfInterestListAdapter;

    protected Dialog mSplashDialog;
    
    private ProgressBar mProgressBar;
    
	// Create an anonymous implementation of OnClickListener
	private OnLongClickListener mLongClickListener = new OnLongClickListener() {
		@Override
	    public boolean onLongClick(View v) {
	    	final CharSequence[] items = {"List", "Map"};

	    	AlertDialog.Builder builder = new AlertDialog.Builder(OOIListActivity.this);
	    	//builder.setTitle("Pick a color");
	    	builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int item) {
	    	    	switch(item) {
	    	    	case 0:
	    	    		break;
	    	    	case 1:
	    	    		Intent i = new Intent();
		            	i.setClassName("com.skope.skope",
		            				   "com.skope.skope.ui.OOIListMapActivity");
		            	i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		            	startActivity(i);
		            	finish();
	    	    	}
	    	    	
	    	    }
	    	});
	    	AlertDialog alert = builder.create();
	    	alert.show();
	    	
	    	return true;
	    }
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	// Enable custom titlebar
    	//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    	
    	// Set the main layout
    	setContentView(R.layout.main);
    	
    	// Set the custom titlebar
    	//getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
        //        R.layout.titlebar);
    	
    	// Fill the custom titlebar title
    	//TextView title = (TextView) findViewById(R.id.title);
    	//title.setText("Skope");
    	
    	// Hide the progress bar
    	mProgressBar = (ProgressBar) findViewById(R.id.titleProgressBar);
    	mProgressBar.setVisibility(ProgressBar.GONE);
    	
    	/*
    	// Set up buttons
    	((Button) findViewById(R.id.button_mapview)).setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(final View view) {
    			Intent i = new Intent();
            	i.setClassName("com.skope.skope",
            				   "com.skope.skope.ui.OOIListMapActivity");
            	startActivity(i);
	        }
    	});
    	*/
    	
    	((View) findViewById(R.id.listview)).setOnLongClickListener(mLongClickListener);
        
    	// Set up the list adapter
        mObjectOfInterestList = new ArrayList<ObjectOfInterest>();
        mObjectOfInterestListAdapter = new ObjectOfInterestArrayAdapter(OOIListActivity.this, R.layout.skope_view, mObjectOfInterestList);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(mObjectOfInterestListAdapter);         
        
        updateListFromCache();

        if (!getServiceQueue().hasServiceStarted()) {
            //showSplashScreen();
            getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
        }
    }

	private void updateListFromCache() {
		mObjectOfInterestList.clear();
    	ObjectOfInterestList cacheList = getCache().getObjectOfInterestList();
        if (cacheList != null && !cacheList.isEmpty()) {
        	// Cache contains items
        	mObjectOfInterestList.addAll(cacheList);
        	mObjectOfInterestListAdapter.notifyDataSetChanged();
        }
	}
    
    /*
    
    private void quitOptionalDialog(int title, int message) {
        //Ask the user if they want to quit
        new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Stop the activity
                OOIListActivity.this.finish();    
            }

        })
        .setNegativeButton(R.string.yes, null)
        .show();
    }
    
     * 
     */

    /***
     * Override the post method to receive incoming messages from the Service.
     *
     * @param type Message type.
     * @param bundle Optional Bundle of extra information, NULL otherwise.
     */
    @Override
    public final void post(final Type type, final Bundle bundle) {
    	switch (type) {
            case FIND_OBJECTS_OF_INTEREST_START:
            	//mProgressBar.setVisibility(ProgressBar.VISIBLE);
                break;

            case FIND_OBJECTS_OF_INTEREST_FINISHED:
            	updateListFromCache();
            	//removeSplashScreen();
            	//mProgressBar.setVisibility(ProgressBar.GONE);
            	break;
            	
            case UNDETERMINED_LOCATION:
            	mProgressBar.setVisibility(ProgressBar.VISIBLE);
            	Toast.makeText(this, "Finding your location...", Toast.LENGTH_LONG).show();
            	break;
            	
            case LOCATION_CHANGED:
            	//Toast.makeText(this, "Location updated", Toast.LENGTH_LONG).show();
            	break;

            default:
                /** Let the BaseActivity handle other message types. */
                super.post(type, bundle);
                break;
        }
    }
    
    private class ObjectOfInterestArrayAdapter extends ArrayAdapter<ObjectOfInterest> {
    	private List<ObjectOfInterest> m_ooiList;
    	
    	public ObjectOfInterestArrayAdapter(Context context, int textViewResourceId,
    			List<ObjectOfInterest> objects) {
    		super(context, textViewResourceId, objects);
    		this.m_ooiList = objects;
    	}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.skope_item, null);
            }
            
            ObjectOfInterest skope = m_ooiList.get(position);
            if (skope != null) {
                    TextView userNameText = (TextView) v.findViewById(R.id.username_text);
                    TextView emailText = (TextView) v.findViewById(R.id.email_text);
                    TextView distanceText = (TextView) v.findViewById(R.id.distance_text);
                    TextView lastUpdateText = (TextView) v.findViewById(R.id.last_update_text);
                    ImageView icon = (ImageView) v.findViewById(R.id.icon);
                    
                    if (userNameText != null) {
                          userNameText.setText(skope.getUserName());                            }
                    
                    if (emailText != null) {
                    	emailText.setText("Email: " + skope.getUserEmail());
                    }
                    
                    if (distanceText != null) {
                    	distanceText.setText("Distance: " + String.valueOf(skope.createLabelDistance()));
                    }
                    
                    if (lastUpdateText != null) {
                    	lastUpdateText.setText("Last update: " + skope.createLabelTimePassedSinceLastUpdate());
                    }
                    
                    if (icon != null) {
                    	icon.setImageBitmap(skope.getThumbnail());
                    }
                    
            }
            
            return v;
        }
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.skope_menu, menu);
	    return true;
	}
	
	/**
	 * Shows the splash screen over the full Activity
	 */
	protected void showSplashScreen() {
	    mSplashDialog = new Dialog(this, R.style.SplashScreen);
	    mSplashDialog.setContentView(R.layout.splash);
	    mSplashDialog.setCancelable(false);
	    mSplashDialog.show();
	}	
	
	/**
	 * Removes the Dialog that displays the splash screen
	 */
	protected void removeSplashScreen() {
	    if (mSplashDialog != null) {
	        mSplashDialog.dismiss();
	        mSplashDialog = null;
	    }
	}
	 
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.quit:
	    	getServiceQueue().stopService();
            this.finish();
            return true;
	    case R.id.refresh:
	    	getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    	return true;   	
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateListFromCache();    	
	}
}