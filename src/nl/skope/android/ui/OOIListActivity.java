/*
 * SkopeActivity
 * 
 * Version information
 *
 * Date
 * 
 * Copyright notice
 */
package nl.skope.android.ui;

import java.util.List;

import nl.skope.android.R;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.User;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.util.Type;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class OOIListActivity extends BaseActivity {	  
	private static final String TAG = OOIListActivity.class.getName();
	
    private ObjectOfInterestList mObjectOfInterestList = null;
    private UserArrayAdapter mObjectOfInterestListAdapter;

    protected Dialog mSplashDialog;
    
    private ProgressBar mProgressBar;
    
	private OnClickListener mStatusClickListener = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// Create input filter to limit text length
			InputFilter[] FilterArray = new InputFilter[1];
			FilterArray[0] = new InputFilter.LengthFilter(60);
						
			final EditText statusEditText = new EditText(OOIListActivity.this);
			statusEditText.setSingleLine(false);
			statusEditText.setLines(2);
			statusEditText.setGravity(Gravity.TOP);
			statusEditText.setMaxLines(2);
			statusEditText.setText(getCache().getUser().getStatus());
			statusEditText.setFilters(FilterArray);
			
	        new AlertDialog.Builder(OOIListActivity.this)
	        .setTitle("Update Status")
	        //.setMessage("Including hash tags (#) will allow you to filter the results by tag")
	        .setView(statusEditText)
	        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	TextView nameTextView = (TextView) v.findViewById(R.id.user_status);
	            	getCache().getUser().setStatus(statusEditText.getText().toString());
	                nameTextView.setText(getCache().getUser().getStatus());
	                getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	            }
	        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                // Do nothing.
	            }
	        }).show();
		}
	};	

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
		            	i.setClassName("nl.skope.android",
		            				   "nl.skope.android.ui.OOIListMapActivity");
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
			// Redirect to list activity
	        Intent i = new Intent(OOIListActivity.this, OOIDetailMapActivity.class);
        	Bundle bundle = new Bundle();
	        bundle.putParcelable("USER", mObjectOfInterestListAdapter.getItem(position));
	        i.putExtras(bundle);
			startActivity(i);
		}
	};	

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	// Set the main layout
    	setContentView(R.layout.main);
    	
    	// Hide the progress bar
    	mProgressBar = (ProgressBar) findViewById(R.id.titleProgressBar);
    	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
    	
    	//((View) findViewById(R.id.listview)).setOnLongClickListener(mLongClickListener);
    	
        // When user clicks on bar, user can update status
        ((TextView) findViewById(R.id.user_status)).setOnClickListener(mStatusClickListener);
    	
    	// Set up the list adapter
        mObjectOfInterestList = new ObjectOfInterestList();
        mObjectOfInterestListAdapter = new UserArrayAdapter(OOIListActivity.this, R.layout.skope_view, mObjectOfInterestList);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(mObjectOfInterestListAdapter); 
        listView.setOnItemClickListener(mOOISelectListener);

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
            	// If list empty show load bar
            	if (mObjectOfInterestList.size() == 0) { 
            		mProgressBar.setVisibility(ProgressBar.VISIBLE);
            	}
                break;

            case FIND_OBJECTS_OF_INTEREST_FINISHED:
            	updateListFromCache();
            	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            	break;
            	
            case UNDETERMINED_LOCATION:
            	// Do update list, to make sure last known time is up to date
            	updateListFromCache();
            	
            	// Display alert
            	String message;
            	if (getCache().isLocationProviderAvailable()) {
            		message = getResources().getString(R.string.no_location_message);
            	} else {
            		message = getResources().getString(R.string.no_location_message_disabled);
            	}
            	
            	new AlertDialog.Builder(this)
    	        .setTitle(getResources().getString(R.string.no_location_title))
    	        .setMessage(message)
    	        .setPositiveButton(getResources().getString(R.string.no_location_ok), new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int whichButton) {
    	            	// Redirect the user to the system mLocation settings menu
                		Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                		startActivity(intent);
                		dialog.dismiss();
    	            }
    	        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int whichButton) {
    	                // Do nothing.
    	            }
    	        }).show();
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
    
    private class UserArrayAdapter extends ArrayAdapter<User> {
    	private LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	
    	OnImageLoadListener mProfilePictureListener = new OnImageLoadListener() {
			@Override
			public void onImageLoaded(Bitmap thumbnail) {
				UserArrayAdapter.this.notifyDataSetChanged();
			}
		};
    	
    	public UserArrayAdapter(Context context, int textViewResourceId,
    			List<User> objects) {
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
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                convertView.setTag(holder);
            } else {
            	holder = (ViewHolder) convertView.getTag();
            }             
            
			User ooi = getItem(position);
            
            if (ooi != null) {
                if (holder.nameText != null) {
                	holder.nameText.setText(ooi.createName());                            
                }
                
                if (holder.distanceText != null) {
                	holder.distanceText.setText("Distance: " + String.valueOf(ooi.createLabelDistance())
                			+ " - " + ooi.createLabelTimePassedSinceLastUpdate());
                }
                
                if (holder.icon != null) {
                	holder.icon.setImageBitmap(ooi.getProfilePicture()); // even when null, otherwise previous values remain
            		// Lazy loading
                	if (ooi.getProfilePicture() == null) {
                		ooi.loadProfilePicture(mProfilePictureListener);
                	}
                }
            }
            
            return convertView;
        }
    }
    
    protected void updateStatusBar() {
        TextView nameText = (TextView) findViewById(R.id.user_name);
        TextView statusText = (TextView) findViewById(R.id.user_status);
        ImageView icon = (ImageView) findViewById(R.id.user_icon);
        User user = getCache().getUser();
        
        if (nameText != null) {
        	nameText.setText(user.createName());                         
        }
        
        if (statusText != null) {
        	if (user.getStatus() != null && !user.getStatus().equals("")) {
            	statusText.setText(user.getStatus());
        	} else {
        		statusText.setText(getResources().getText(R.string.home_status_empty));
        	}
        }
        
        if (icon != null) {
        	icon.setImageBitmap(user.getProfilePicture());
        }
        
    	
    }
    
	@Override
	public void onResume() {
		super.onResume();
		if (checkCacheSanity()) {
			getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
			updateStatusBar();
		}
	}
}