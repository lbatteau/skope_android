package com.skope.skope.ui;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;

public class MainTabActivity extends TabActivity {
	SkopeApplication mApplication;
	Cache mCache;

	private OnClickListener mStatusClickListener = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			final EditText statusEditText = new EditText(MainTabActivity.this);
	        new AlertDialog.Builder(MainTabActivity.this)
	        .setTitle("Update Status")
	        .setView(statusEditText)
	        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	TextView nameTextView = (TextView) v.findViewById(R.id.user_status);
	            	mCache.getUser().setStatus(statusEditText.getText().toString());
	                nameTextView.setText(mCache.getUser().getStatus());	                
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
	    
	    // Set up the user bar
    	User user = mCache.getUser();
        TextView nameText = (TextView) findViewById(R.id.user_name);
        TextView statusText = (TextView) findViewById(R.id.user_status);
        ImageView icon = (ImageView) findViewById(R.id.user_icon);
        
        if (nameText != null) {
        	nameText.setText(user.createName());                         
        }
        
        if (statusText != null) {
        	statusText.setText(user.getStatus());
        }
        
        if (icon != null) {
        	icon.setImageBitmap(user.getThumbnail());
        }
        
        // When user clicks on bar, user can update status
        ((LinearLayout) findViewById(R.id.user_bar)).setOnClickListener(mStatusClickListener);
        
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Reusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, OOIListActivity.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("list").setIndicator("List")
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, OOIListMapActivity.class);
	    spec = tabHost.newTabSpec("map").setIndicator("Map")
	                  .setContent(intent);
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
