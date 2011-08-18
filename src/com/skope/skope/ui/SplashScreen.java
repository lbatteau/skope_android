package com.skope.skope.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ProgressBar;

import com.skope.skope.R;
import com.skope.skope.utils.Type;

public class SplashScreen extends BaseActivity {
	
	private ProgressBar m_progressBar;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

    	// Enable custom titlebar
    	requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    	
		setContentView(R.layout.splash);    		
        
    	// Set the custom titlebar
    	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
                R.layout.titlebar);
    	
    	
    	// Hide the progress bar
    	m_progressBar = (ProgressBar) findViewById(R.id.titleProgressBar);
    	m_progressBar.setVisibility(ProgressBar.GONE);
    	
        getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
    }
	
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
            	m_progressBar.setVisibility(ProgressBar.VISIBLE);
                break;

            case FIND_OBJECTS_OF_INTEREST_FINISHED:
            	Intent i = new Intent();
            	i.setClassName("com.skope.skope",
            				   "com.skope.skope.ui.SkopeListActivity");
            	startActivity(i);
            	break;

            default:
                /** Let the BaseActivity handle other message types. */
                super.post(type, bundle);
                break;
        }
    }
    
	
}
