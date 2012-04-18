package nl.skope.android.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import nl.skope.android.application.Cache;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.UserPhoto;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import nl.skope.android.R;

public class UserSignupActivity extends BaseActivity {
	private static final String TAG = UserSignupActivity.class.getSimpleName();
	private static String VALIDATION_MESSAGE_REQUIRED = "This field is required.";

	/** ID for date picker dialog */
	private final static int DIALOG_DATE_PICKER = 0;
	private final static int DIALOG_VERIFICATION_SENT = 1;

	private EditText mEmailEdit, mPassword1Edit, mPassword2Edit;

	/** The verification message is returned by the server */
	private static String mVerificationMessage;

	private static class UserSignupForm {
		public String email, password1, password2, firstName, lastName,
		dateOfBirth, gender;
	}

	private class SignupTask extends AsyncTask<UserSignupForm, Void, CustomHttpClient> {
		private ProgressDialog dialog = new ProgressDialog(UserSignupActivity.this);
		private String mURL = getCache().getProperty("skope_service_url") + "signup/";
		private UserSignupForm mForm;

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Contacting server...");
			this.dialog.show();
		}

		protected CustomHttpClient doInBackground(UserSignupForm... args) {
			// Set up HTTP client with url as argument
			CustomHttpClient client = new CustomHttpClient(mURL, getApplicationContext());

			// Add POST parameters
			mForm = args[0];
			client.addParam("email", mForm.email.toLowerCase());
			client.addParam("password1", mForm.password1);
			client.addParam("password2", mForm.password2);
			client.addParam("first_name", mForm.firstName);
			client.addParam("last_name", mForm.lastName);
			client.addParam("date_of_birth", mForm.dateOfBirth);
			client.addParam("gender", mForm.gender);

			// Send HTTP request to web service
			try {
				client.execute(RequestMethod.POST);
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
				Toast.makeText(UserSignupActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
				return;
			} else if (httpResponseCode == HttpStatus.SC_CREATED) {
				// The verification message is returned in the response
				mVerificationMessage = client.getResponse();

				// Store credentials
				SharedPreferences.Editor prefsEditor = getCache().getPreferences().edit();
				prefsEditor.putString(SkopeApplication.PREFS_USERNAME, mForm.email);
				prefsEditor.putString(SkopeApplication.PREFS_PASSWORD, mForm.password1);
				prefsEditor.commit();

				showDialog(DIALOG_VERIFICATION_SENT);
			} else {
				// Server returned error code
				switch (client.getResponseCode()) {
				case HttpStatus.SC_UNAUTHORIZED:
					// Login not successful, authorization required
					Toast.makeText(UserSignupActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
					break;
				case HttpStatus.SC_REQUEST_TIMEOUT:
				case HttpStatus.SC_BAD_GATEWAY:
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					// Connection timeout
					Toast.makeText(
							UserSignupActivity.this,
							getResources().getText(R.string.error_connection_failed), Toast.LENGTH_SHORT).show();
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					Toast.makeText(
							UserSignupActivity.this,
							getResources().getText(R.string.error_server_error), Toast.LENGTH_LONG).show();
					break;
				case HttpStatus.SC_BAD_REQUEST:
					// Validation failed, extract form errors from response
					JSONObject jsonResponse = null;
					try {
						jsonResponse = new JSONObject(client.getResponse());
					} catch (JSONException e) {
						// Log exception
						Log.e(TAG, e.toString());
						Toast.makeText(UserSignupActivity.this, "Invalid form", Toast.LENGTH_SHORT).show();
						return;
					}

					if (jsonResponse.length() > 0) {
						JSONArray fields = jsonResponse.names();
						try {
							JSONArray errorList = jsonResponse .getJSONArray(fields.getString(0));
							String error = errorList.getString(0);
							if (error.equals(VALIDATION_MESSAGE_REQUIRED)) {
								Toast.makeText(
										UserSignupActivity.this, getResources() .getString(
												R.string.signup_validation_required_fields),
												Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(UserSignupActivity.this, error, Toast.LENGTH_LONG).show();
							}
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
						break;
					}
					break;
				default:
					Toast.makeText(UserSignupActivity.this, "Error code " + httpResponseCode, Toast.LENGTH_SHORT).show();
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

		// load up the layout
		setContentView(R.layout.user_signup);

		// Sign up form
		final UserSignupForm mForm = new UserSignupActivity.UserSignupForm();

		// Date picker
		EditText dateOfBirthEdit = (EditText) findViewById(R.id.date_of_birth);
		dateOfBirthEdit.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					showDialog(DIALOG_DATE_PICKER);
				}
				return false;
			}
		});

		dateOfBirthEdit.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showDialog(DIALOG_DATE_PICKER);
				} else {
					dismissDialog(DIALOG_DATE_PICKER);
				}
			}
		});

		// Sign up button action
		Button signup = (Button) findViewById(R.id.signup_button);
		signup.setOnClickListener(new OnClickListener() {
			public void onClick(View viewParam) {
				mEmailEdit = (EditText) findViewById(R.id.signup_email);
				mForm.email = (mEmailEdit).getText().toString();
				mPassword1Edit = (EditText) findViewById(R.id.password1);
				mForm.password1 = (mPassword1Edit).getText().toString();
				mPassword2Edit = (EditText) findViewById(R.id.password2);
				mForm.password2 = (mPassword2Edit).getText().toString();
				mForm.firstName = ((EditText) findViewById(R.id.first_name))
				.getText().toString();
				mForm.lastName = ((EditText) findViewById(R.id.last_name))
				.getText().toString();

				String dateOfBirthUnformatted = ((EditText) findViewById(R.id.date_of_birth))
				.getText().toString();
				SimpleDateFormat df = new SimpleDateFormat("M-d-yyyy");
				try {
					Date dateOfBirth = df.parse(dateOfBirthUnformatted);
					df.applyPattern("yyyy-MM-dd");
					mForm.dateOfBirth = df.format(dateOfBirth);
				} catch (ParseException e) {
					Log.e(TAG, "Couldn't parse "
							+ dateOfBirthUnformatted);
				}

				RadioGroup genderGroup = (RadioGroup) findViewById(R.id.gender);
				int checkedGenderId = genderGroup.getCheckedRadioButtonId();
				if (checkedGenderId >= 0) {
					String gender = ((RadioButton) findViewById(checkedGenderId))
														.getText().toString();
					mForm.gender = gender;
				}

				new SignupTask().execute(mForm);
			}
		});

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_DATE_PICKER:
			// do the work to define the pause Dialog
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.dialog_date_picker,
					null);
			builder = new AlertDialog.Builder(this)
			.setView(layout)
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int whichButton) {
					DatePicker datePicker = (DatePicker) layout
					.findViewById(R.id.date_picker);
					datePicker.clearFocus();
					int year = datePicker.getYear();
					int month = datePicker.getMonth();
					int day = datePicker.getDayOfMonth();
					EditText dateOfBirth = (EditText) findViewById(R.id.date_of_birth);
					dateOfBirth.setText((month + 1) + "-" + day
							+ "-" + year);
				}
			})
			.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					dialog.dismiss();
				}
			});
			break;
		case DIALOG_VERIFICATION_SENT:
			builder = new AlertDialog.Builder(this).setMessage(
					mVerificationMessage).setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Redirect to list activity
							Intent i = new Intent();
							i.setClassName("nl.skope.android",
							"nl.skope.android.ui.LoginActivity");
							startActivity(i);
						}
					});
		default:

		}
		return builder.create();
	}

}
