/*
 * SkopeActivity
 * 
 * Version information
 *
 * Date
 * 
 * Copyright notice
 */
package com.indie.skope.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.indie.skope.R;
import com.indie.skope.application.SkopeApplication;
import com.indie.skope.utils.Type;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class SkopeListActivity extends BaseActivity {	  
	private static final String TAG = SkopeListActivity.class.getName();
	
	private ProgressDialog mProgressDialog = null;
    private ArrayList<ObjectOfInterest> mObjectOfInterestList = null;
    private ObjectOfInterestArrayAdapter mObjectOfInterestListAdapter;

	private Properties mProperties;
    private SharedPreferences mPreferences;
    private ProgressBar mTitleProgressBar;
    
    protected Dialog mSplashDialog;
    
    private ProgressBar mProgressBar;
    
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
    	
    	// Set up buttons
    	((Button) findViewById(R.id.button_mapview)).setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(final View view) {
    			Intent i = new Intent();
            	i.setClassName("com.indie.skope",
            				   "com.indie.skope.ui.SkopeMapActivity");
            	startActivity(i);
	        }
    	});    	
        
    	// Set up the list adapter
        mObjectOfInterestList = getCache().getObjectOfInterestList();
        mObjectOfInterestListAdapter = new ObjectOfInterestArrayAdapter(SkopeListActivity.this, R.layout.skope_view, mObjectOfInterestList);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(mObjectOfInterestListAdapter);            
        
        mPreferences = this.getSharedPreferences("skopePreferences", MODE_WORLD_READABLE);
        SharedPreferences.Editor prefsEditor = mPreferences.edit();
        prefsEditor.putString(SkopeApplication.PREFS_USER, "lukas");
        prefsEditor.putInt(SkopeApplication.PREFS_RANGE, 1000);
        prefsEditor.commit();   
        
        if (!getServiceQueue().hasServiceStarted()) {
            showSplashScreen();
        }
        
        getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
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
                SkopeListActivity.this.finish();    
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
            	mProgressBar.setVisibility(ProgressBar.VISIBLE);
                break;

            case FIND_OBJECTS_OF_INTEREST_FINISHED:
            	mObjectOfInterestListAdapter.notifyDataSetChanged();
            	removeSplashScreen();
            	mProgressBar.setVisibility(ProgressBar.GONE);
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
                        TextView textView = (TextView) v.findViewById(R.id.toptext);
                        TextView bottomText = (TextView) v.findViewById(R.id.bottomtext);
                        ImageView icon = (ImageView) v.findViewById(R.id.icon);
                        
                        if (textView != null) {
                              textView.setText("Name: " + skope.getUserName());                            }
                        
                        if (bottomText != null) {
                        	bottomText.setText("Email: " + skope.getUserEmail());
                        }
                        
                        if (icon != null) {
                        	icon.setImageBitmap(skope.getThumbnail());
                        }
                        
                }
                return v;
        }
    }
    
	/* Request updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
    }

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
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
	    case R.id.user:
	    	final EditText userInput = new EditText(this);
	    	
	    	AlertDialog.Builder userBuilder = new AlertDialog.Builder(this);
	    	userBuilder.setTitle("Set username");
	    	userBuilder.setView(userInput);
	    	userBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int whichButton) {
	    		  Editable value = userInput.getText();
	    		  SharedPreferences.Editor prefsEditor = mPreferences.edit();
	    	        prefsEditor.putString(SkopeApplication.PREFS_USER, value.toString());
	    	        prefsEditor.commit();
	    		  }
	    		});

	    	userBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    		  public void onClick(DialogInterface dialog, int whichButton) {
	    		    // Canceled.
	    		  }
	    		});

	    	userBuilder.show();
	    	getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	        return true;
	    case R.id.range:
	    	final EditText rangeInput = new EditText(this);
	    	
	    	AlertDialog.Builder rangeMenuBuilder = new AlertDialog.Builder(this);
	    	rangeMenuBuilder.setTitle("Set range");
	    	rangeMenuBuilder.setView(rangeInput);
	    	rangeMenuBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int whichButton) {
	    		  int value = Integer.valueOf(rangeInput.getText().toString());
	    		  SharedPreferences.Editor prefsEditor = mPreferences.edit();
	    	        prefsEditor.putInt(SkopeApplication.PREFS_RANGE, value);
	    	        prefsEditor.commit();
	    		  }
	    		});

	    	rangeMenuBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    		  public void onClick(DialogInterface dialog, int whichButton) {
	    		    // Canceled.
	    		  }
	    		});
	    	
	    	rangeMenuBuilder.show();
	    	getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    	return true;
	    case R.id.refresh:
	    	getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    	return true;
	    case R.id.phone_number:
	    	TelephonyManager tMgr =(TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
	    	AlertDialog.Builder phoneBuilder = new AlertDialog.Builder(this);
	    	phoneBuilder.setMessage("Your phone number is " + tMgr.getLine1Number())
	    	       		.setCancelable(false)
		    	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    	                dialog.cancel();
		    	           }
		    	       }).show();	    	
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}