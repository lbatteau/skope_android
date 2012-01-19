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

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.skope.skope.R;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ObjectOfInterestList;
import com.skope.skope.application.User.OnThumbnailLoadListener;
import com.skope.skope.util.Type;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class OOIListActivity extends BaseActivity {	  
	private static final String TAG = OOIListActivity.class.getName();
	
    private ObjectOfInterestList mObjectOfInterestList = null;
    private ObjectOfInterestArrayAdapter mObjectOfInterestListAdapter;

    protected Dialog mSplashDialog;
    
    private ProgressBar mProgressBar;
    
	// Create an anonymous implementation of OnClickListener
	private OnLongClickListener mLongClickListener = new OnLongClickListener() {
		@Override
	    public boolean onLongClick(View v) {
	    	final CharSequence[] items = {"Everything", "Map"};

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
	
	private OnItemClickListener mOOISelectListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			// Store selected position in shared cache
			getCache().getObjectOfInterestList().setSelectedPosition(position);
			
			// Redirect to list activity
	        Intent i = new Intent();
        	i.setClassName("com.skope.skope",
        				   "com.skope.skope.ui.OOIDetailMapActivity");
        	startActivity(i);
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
    	
    	((ListView) findViewById(R.id.list)).setOnItemClickListener(mOOISelectListener);
    	((View) findViewById(R.id.listview)).setOnLongClickListener(mLongClickListener);
    	
    	// Set up the list adapter
        mObjectOfInterestList = new ObjectOfInterestList();
        mObjectOfInterestListAdapter = new ObjectOfInterestArrayAdapter(OOIListActivity.this, R.layout.skope_view, mObjectOfInterestList);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(mObjectOfInterestListAdapter); 

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
		}
    	mObjectOfInterestListAdapter.notifyDataSetChanged();
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
            	Toast.makeText(this, "Location currently unavailable", Toast.LENGTH_LONG).show();
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
    
    public static class ViewHolder {
		public TextView nameText;
		public TextView distanceText;
		public TextView lastUpdateText;
		public ImageView icon;

	}
    
    private class ObjectOfInterestArrayAdapter extends ArrayAdapter<ObjectOfInterest> {
    	private LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	
    	OnThumbnailLoadListener mThumbnailListener = new OnThumbnailLoadListener() {
			@Override
			public void onThumbnailLoaded(Bitmap thumbnail) {
				ObjectOfInterestArrayAdapter.this.notifyDataSetChanged();
			}
		};
    	
    	public ObjectOfInterestArrayAdapter(Context context, int textViewResourceId,
    			List<ObjectOfInterest> objects) {
    		super(context, textViewResourceId, objects);
    	}

		@Override
        public View getView(int position, View convertView, final ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
                convertView = (View) mInflater.inflate(R.layout.skope_item, null);
                holder = new ViewHolder();
                holder.nameText = (TextView) convertView.findViewById(R.id.name_text);
                holder.distanceText = (TextView) convertView.findViewById(R.id.distance_text);
                holder.lastUpdateText = (TextView) convertView.findViewById(R.id.last_update_text);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                convertView.setTag(holder);
            } else {
            	holder = (ViewHolder) convertView.getTag();
            }             
            
            ObjectOfInterest ooi = getItem(position);
            
            if (ooi != null) {
                if (holder.nameText != null) {
                	holder.nameText.setText(ooi.createName());                            
                }
                
                if (holder.distanceText != null) {
                	holder.distanceText.setText("Distance: " + String.valueOf(ooi.createLabelDistance()));
                }
                
                if (holder.lastUpdateText != null) {
                	holder.lastUpdateText.setText("Last update: " + ooi.createLabelTimePassedSinceLastUpdate());
                }
                
                if (holder.icon != null) {
                	holder.icon.setImageBitmap(ooi.getThumbnail()); // even when null, otherwise previous values remain
            		// Lazy loading
                	if (ooi.getThumbnail() == null) {
                		ooi.loadThumbnail(mThumbnailListener);
                	}
                }
            }
            
            return convertView;
        }
    }
    
	@Override
	public void onResume() {
		super.onResume();
		updateListFromCache();    	
	}
}