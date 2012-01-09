package com.skope.skope.ui;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ServiceQueue;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;
import com.skope.skope.utils.Type;

public class MainTabActivity extends TabActivity {
	private SkopeApplication mApplication;
	private Cache mCache;
	private ServiceQueue mServiceQueue;    

	private OnClickListener mStatusClickListener = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			// Create input filter to limit text length
			InputFilter[] FilterArray = new InputFilter[1];
			FilterArray[0] = new InputFilter.LengthFilter(60);
						
			final EditText statusEditText = new EditText(MainTabActivity.this);
			statusEditText.setSingleLine(false);
			statusEditText.setLines(2);
			statusEditText.setGravity(Gravity.TOP);
			statusEditText.setMaxLines(2);
			statusEditText.setText(mCache.getUser().getStatus());
			statusEditText.setFilters(FilterArray);
			
	        new AlertDialog.Builder(MainTabActivity.this)
	        .setTitle("Update Status")
	        .setMessage("Including hash tags (#) will allow you to filter the results by tag")
	        .setView(statusEditText)
	        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	TextView nameTextView = (TextView) v.findViewById(R.id.user_status);
	            	mCache.getUser().setStatus(statusEditText.getText().toString());
	                nameTextView.setText(mCache.getUser().getStatus());
	                mServiceQueue.postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	            }
	        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                // Do nothing.
	            }
	        }).show();
		}
	};	

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.tabs);
	    
	    mApplication = (SkopeApplication) getApplication();
	    mCache = mApplication.getCache();
	    mServiceQueue = mApplication.getServiceQueue();
	    Resources res = getResources();
	    
	    // Set up the user bar
    	User user = mCache.getUser();
    	
    	// Cache empty?
    	if (user == null) {
    		// Redirect to login screen
    		Intent i = new Intent();
        	i.setClassName("com.skope.skope",
        				   "com.skope.skope.ui.LoginActivity");
        	startActivity(i);
        	finish();
        	return;
    	}
    	
        TextView nameText = (TextView) findViewById(R.id.user_name);
        TextView statusText = (TextView) findViewById(R.id.user_status);
        ImageView icon = (ImageView) findViewById(R.id.user_icon);
        
        if (nameText != null) {
        	nameText.setText(user.createName());                         
        }
        
        if (statusText != null) {
        	if (user.getStatus() != null) {
            	statusText.setText(user.getStatus());
        	} else {
        		statusText.setText(res.getText(R.string.home_status_empty));
        	}
        }
        
        if (icon != null) {
        	icon.setImageBitmap(user.getThumbnail());
        }
        
        // When user clicks on bar, user can update status
        ((TextView) findViewById(R.id.user_status)).setOnClickListener(mStatusClickListener);
        
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Reusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View tabList = inflater.inflate(R.layout.tab_list, null); 
	    View tabMap = inflater.inflate(R.layout.tab_map, null);
	    View tabProfile = inflater.inflate(R.layout.tab_profile, null);
	    View chatProfile = inflater.inflate(R.layout.tab_chat, null);
	    View favoritesProfile = inflater.inflate(R.layout.tab_favorites, null);
	    
	    // Initialize a TabSpec for each tab and add it to the TabHost

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, OOIListActivity.class);
	    spec = tabHost.newTabSpec("list").setIndicator(tabList)
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, OOIListMapActivity.class);
	    spec = tabHost.newTabSpec("map").setIndicator(tabMap)
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, UserProfileActivity.class);
	    spec = tabHost.newTabSpec("profile").setIndicator(tabProfile).setContent(intent);
        tabHost.addTab(spec);
	    
	    spec = tabHost.newTabSpec("chat").setIndicator(chatProfile).setContent(intent);
        tabHost.addTab(spec);
	    
	    spec = tabHost.newTabSpec("favorites").setIndicator(favoritesProfile).setContent(intent);
        tabHost.addTab(spec);
	    
	    tabHost.setCurrentTab(0);
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.skope_menu, menu);
	    return true;
	}
	
	

}
