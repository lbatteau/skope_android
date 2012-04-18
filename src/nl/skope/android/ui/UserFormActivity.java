package nl.skope.android.ui;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import nl.skope.android.application.Cache;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.application.UserPhoto;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import nl.skope.android.R;

public class UserFormActivity extends BaseActivity {
	private static final String TAG = UserFormActivity.class.getSimpleName();

	private static class UserForm {
		public String firstName, lastName, dateOfBirth, homeTown, work_job_title,
		work_company, education_study, education_college, gender, 
		relationship_status;
		public boolean isGenderPublic, isDateOfBirthPublic;
	}

	private class UpdateTask extends AsyncTask<Object, Void, CustomHttpClient> {
		private ProgressDialog dialog = new ProgressDialog(UserFormActivity.this);

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Contacting server...");
			this.dialog.show();
		}

		protected CustomHttpClient doInBackground(Object... args) {
			int userId = getCache().getUser().getId();
			String username = (String) args[0];
			String password = (String) args[1];
			String serviceUrl = getCache().getProperty("skope_service_url") + "/user/" + userId + "/";
			
			// Set up HTTP client
	        CustomHttpClient client = new CustomHttpClient(serviceUrl, getApplicationContext());
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(username, password);
	        
			// Add POST parameters
			UserForm form = (UserForm) args[2];
			client.addParam("first_name", form.firstName);
			client.addParam("last_name", form.lastName);
			client.addParam("date_of_birth", form.dateOfBirth);
			client.addParam("gender", form.gender);
			client.addParam("relationship_status", form.relationship_status);
			client.addParam("home_town", form.homeTown);
			client.addParam("work_job_title", form.work_job_title);
			client.addParam("work_company", form.work_company);
			client.addParam("education_study", form.education_study);
			client.addParam("education_college", form.education_college);
			client.addParam("is_gender_public", form.isGenderPublic ? "on" : "");
			client.addParam("is_date_of_birth_public", 
										form.isDateOfBirthPublic ? "on" : "");

			// Send HTTP request to web service
			try {
				client.execute(RequestMethod.PUT);
			} catch (Exception e) {
				// Most exceptions already handled by client
				e.printStackTrace();
			}

			// Return server response
			return client;
		}

		protected void onPostExecute(CustomHttpClient client) {
			this.dialog.dismiss();

			// Check HTTP response code
			int httpResponseCode = client.getResponseCode();
			// Check for server response
			if (httpResponseCode == 0) {
				// No server response
				Toast.makeText(UserFormActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
				return;
			} else if (httpResponseCode == HttpStatus.SC_OK) {
		        User user;
		        try {
		        	JSONObject jsonResponse = new JSONObject(client.getResponse());
		        	user = new User(jsonResponse);
		        	user.setCache(getCache());
					getCache().setUser(user);
		        } catch (JSONException e) {
					// Log exception
					Log.e(TAG, e.toString());
					Toast.makeText(UserFormActivity.this, "Invalid content", Toast.LENGTH_SHORT).show();
					return;
				}
		        finish();
				
			} else {
				// Server returned error code
				switch (client.getResponseCode()) {
				case HttpStatus.SC_UNAUTHORIZED:
					// Login not successful, authorization required
					Toast.makeText(UserFormActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
					break;
				case HttpStatus.SC_REQUEST_TIMEOUT:
				case HttpStatus.SC_BAD_GATEWAY:
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					// Connection timeout
					Toast.makeText(
							UserFormActivity.this,
							getResources().getText(R.string.error_connection_failed), Toast.LENGTH_SHORT).show();
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					Toast.makeText(
							UserFormActivity.this,
							getResources().getText(R.string.error_server_error), Toast.LENGTH_SHORT).show();
					break;
				case HttpStatus.SC_BAD_REQUEST:
					// Validation failed, extract form errors from response
					JSONObject jsonResponse = null;
					try {
						jsonResponse = new JSONObject(client.getResponse());
					} catch (JSONException e) {
						// Log exception
						Log.e(TAG, e.toString());
						Toast.makeText(UserFormActivity.this, "Invalid form", Toast.LENGTH_SHORT).show();
						return;
					}

					if (jsonResponse.length() > 0) {
						JSONArray fields = jsonResponse.names();
						try {
							JSONArray errorList = jsonResponse .getJSONArray(fields.getString(0));
							String error = errorList.getString(0);
							Toast.makeText(UserFormActivity.this, error, Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
						break;
					}
					break;
				default:
					Toast.makeText(UserFormActivity.this, String.valueOf(httpResponseCode), Toast.LENGTH_SHORT).show();
					break;
				}
				return;
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// Load layout
		setContentView(R.layout.user_form);

		// Fill form with user info
		User user = getCache().getUser();
		user.fillUserForm(this);
		
	    // Save button action
		Button saveButton = (Button) findViewById(R.id.button_save);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Collect fields
				UserForm form = new UserForm();
				TextView firstName = (TextView) findViewById(R.id.first_name);
				form.firstName = firstName.getText().toString();
				TextView lastName = (TextView) findViewById(R.id.last_name);
				form.lastName = lastName.getText().toString();
				
				RadioGroup genderGroup = (RadioGroup) findViewById(R.id.gender);
				int checkedGenderId = genderGroup.getCheckedRadioButtonId();
				if (checkedGenderId >= 0) {
					String gender = ((RadioButton) findViewById(checkedGenderId))
														.getText().toString();
					form.gender = gender;
				}

				
				CheckBox genderShowProfile = (CheckBox) findViewById(R.id.gender_show_profile);
				form.isGenderPublic = genderShowProfile.isChecked();
				
				DatePicker dateOfBirthPicker = (DatePicker) findViewById(R.id.date_picker);
				Calendar calendar = new GregorianCalendar();
				calendar.set(dateOfBirthPicker.getYear(), 
								dateOfBirthPicker.getMonth(), 
								dateOfBirthPicker.getDayOfMonth());
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				form.dateOfBirth = df.format(calendar.getTime());
				
				CheckBox birthdayShowProfile = (CheckBox) findViewById(R.id.birthday_show_profile);
				form.isDateOfBirthPublic = birthdayShowProfile.isChecked();
				
				EditText homeTownEdit = (EditText) findViewById(R.id.home_town);
				form.homeTown = homeTownEdit.getText().toString();
				
				Spinner relationship = (Spinner) findViewById(R.id.relationship);
				String value = (String) relationship.getSelectedItem();
				if (value != null && !value.equals("")) {
					form.relationship_status = (String) relationship.getSelectedItem();
				} else {
					form.relationship_status = "";
				}
				
				EditText edit = (EditText) findViewById(R.id.work_job_title);
				form.work_job_title = edit.getText().toString();
				
				edit = (EditText) findViewById(R.id.work_company);
				form.work_company = edit.getText().toString();
				
				edit = (EditText) findViewById(R.id.education_study);
				form.education_study = edit.getText().toString();
				
				edit = (EditText) findViewById(R.id.education_college);
				form.education_college = edit.getText().toString();
								
				String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
				String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
				Object[] params = {username, password, form};
				new UpdateTask().execute(params);
			}
		});

	    // Cancel button action
		Button cancelButton = (Button) findViewById(R.id.button_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	public void replaceContentView(String id, Intent newIntent) {
	    View view = ((ActivityGroup) this.getParent())
	            .getLocalActivityManager()
	            .startActivity(id,
	                    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
	            .getDecorView();
	    ((Activity) this).setContentView(view);
	}

	@Override
	protected void onResume() {
		super.onResume();
		User user = getCache().getUser();
		user.fillUserForm(this);
	}

}
