package com.slackpack.nfc;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements Listener, View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    
    public static final String TAG = MainActivity.class.getSimpleName();

    // Element objects
    private Spinner mSpinnerResponderLevel;
    private RadioGroup mRadioResponderLevel;
    private RadioButton mRadioButtonOne;
    private RadioButton mRadioButtonTwo;
    private EditText mEtResponderId;
    private EditText mEtFirstName;
    private EditText mEtLastName;
    private EditText mEtPatientDob;
    private EditText mEtChiefComplaint;
    private EditText mEtLocation; //TODO: do we want user to be able to edit location or solely pull from phone location data?
    private Button mBtLocation;
    private Button mBtWrite;
    private Button mBtRead;
    private final Looper looper = null;

    // Date variables
    private int mYear, mMonth, mDay;

    //Managers
    LocationManager locationManager;

    LocationListener locationListener;

    //Location Data
    Location currLocation;

    // Location Permission
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 0;

    // Context
    private Context mContext;

    // Fragment objects
    private NFCWriteFragment mNfcWriteFragment;
    private NFCReadFragment mNfcReadFragment;

    // Boolean variables to track the state read/write dialogs being displayed
    private boolean isDialogDisplayed = false;
    private boolean isWrite = false;

    private NfcAdapter mNfcAdapter;

    // Activities are initialized here
    // This is basically the main() function for the app
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("app started", "can you see meeeeeeeeeeeeeee");

        initViews();
        initNFC();

        mContext = getApplicationContext();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currLocation = location;
                Log.d("Location changes", location.toString());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("Status Changed", String.valueOf(status));
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("Provider Enabled", provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("Provider Disabled", provider);
            }
        };

        initLocationUpdates();
    }

    private void initViews() {

        // Initialize elements here using their id set in activity_main.xml
        mSpinnerResponderLevel = (Spinner) findViewById(R.id.spinner_responder_level);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.responder_level_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerResponderLevel.setAdapter(adapter);

        mRadioResponderLevel = (RadioGroup) findViewById(R.id.radio_responder_level);
        mRadioButtonOne = (RadioButton) findViewById(R.id.radio_one);
        mRadioButtonTwo = (RadioButton) findViewById(R.id.radio_two);

        mEtResponderId = (EditText) findViewById(R.id.et_responder_id);
        mEtFirstName = (EditText) findViewById(R.id.et_patient_first_name);
        mEtLastName = (EditText) findViewById(R.id.et_patient_last_name);
        mEtPatientDob = (EditText) findViewById(R.id.et_patient_dob);
        mEtChiefComplaint = (EditText) findViewById(R.id.et_patient_chief_complaint);
        mEtLocation = (EditText) findViewById(R.id.et_location);
        mBtLocation = (Button) findViewById(R.id.btn_location);
        mBtWrite = (Button) findViewById(R.id.btn_write);
        mBtRead = (Button) findViewById(R.id.btn_read);


        // Set up listeners for elements that can be tapped
        mEtPatientDob.setOnClickListener(this);
        mBtWrite.setOnClickListener(view -> showWriteFragment());
        mBtRead.setOnClickListener(view -> showReadFragment());
        mBtLocation.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    private void clearForm(ViewGroup group) {
        for (int i = 0, count = group.getChildCount(); i < count; ++i) {
            View view = group.getChildAt(i);
            if (view instanceof EditText) {
                ((EditText)view).setText("");
            }

            if (view instanceof ViewGroup && (((ViewGroup)view).getChildCount() > 0))
                clearForm((ViewGroup)view);

            if (view instanceof RadioGroup) {
                ((RadioGroup)view).clearCheck();
            }

            if (view instanceof Spinner) {
                ((Spinner)view).setSelection(0);
            }

            if (view instanceof TextView) {
                ((TextView) view).setText("");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {
        case R.id.clear_button:
            clearForm((ViewGroup) findViewById(R.id.scroll_id));
            return(true);
    }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onClick(View view) {

        // When the dob EditText is tapped this calendar view
        // will be created programmatically in java and displayed
        // to the user TODO: Cleanup code warnings
        if (view == mEtPatientDob) {
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);


            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year,
                                              int monthOfYear, int dayOfMonth) {

                            mEtPatientDob.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);

                        }
                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }

        if (view == mBtLocation) {
            Log.d("Click", "Location Button Clicked");
            getCurrLocation();
            if (currLocation == null) {
                mEtLocation.setText("Need Location Data");
            }

            else {
                mEtLocation.setText(currLocation.toString());

            }
        }
    }


    //The code below this deals with getting the location data

    private Boolean isLocationEnabled() {

        int locationMode = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE,
                0
        );

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    // Updates location member of MainActivity; displays alertbox if
    private void getCurrLocation() {
        Log.d("get curr", "location called");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && isLocationEnabled()){
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

            locationManager.requestSingleUpdate(criteria, locationListener, looper);
            currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            Log.d("im", "over here");
        }

        else  {
            //TODO: below doesnt work
            Log.d("build that", "dialog box");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Your Device's GPS is Disable")
                    .setCancelable(false)
                    .setTitle("** Gps Status **")
                    .setPositiveButton("Gps On",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // finish the current activity
                                    //AlertBoxAdvance.this.finish();
                                    Intent myIntent = new Intent(
                                            Settings.ACTION_SECURITY_SETTINGS);
                                    startActivity(myIntent);
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // cancel the dialog box
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }

    }


    public void requestLocationPermissions() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            android.support.design.widget.Snackbar.make(mBtLocation, R.string.give_location_permission,
                    android.support.design.widget.Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                }
            }).show();

        } else {
            android.support.design.widget.Snackbar.make(mBtLocation, R.string.location_unavailable, android.support.design.widget.Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == MY_PERMISSIONS_REQUEST_FINE_LOCATION) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                android.support.design.widget.Snackbar.make(mBtLocation, R.string.location_permission_granted,
                        android.support.design.widget.Snackbar.LENGTH_SHORT)
                        .show();

            } else {
                // Permission request was denied.
                android.support.design.widget.Snackbar.make(mBtLocation, R.string.location_permission_denied,
                        android.support.design.widget.Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }



    private void initLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && isLocationEnabled()){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,60000,100,locationListener);

        }
    }


    // Nothing below here really needs to be touched as it contains
    // the listener and fragment calls for the read/write buttons
    // and handles NFC handshakes etc. The exception is in the onNewIntent
    // method where we build the string to be written to the tag

    private void initNFC(){

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }


    private void showWriteFragment() {

        isWrite = true;

        mNfcWriteFragment = (NFCWriteFragment) getFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);

        if (mNfcWriteFragment == null) {

            mNfcWriteFragment = NFCWriteFragment.newInstance();
        }
        mNfcWriteFragment.show(getFragmentManager(),NFCWriteFragment.TAG);

    }

    private void showReadFragment() {

        mNfcReadFragment = (NFCReadFragment) getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);

        if (mNfcReadFragment == null) {

            mNfcReadFragment = NFCReadFragment.newInstance();
        }
        mNfcReadFragment.show(getFragmentManager(),NFCReadFragment.TAG);

    }

    @Override
    public void onDialogDisplayed() {

        isDialogDisplayed = true;
    }

    @Override
    public void onDialogDismissed() {

        isDialogDisplayed = false;
        isWrite = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected,tagDetected,ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if(mNfcAdapter!= null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mNfcAdapter!= null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        Log.d(TAG, "onNewIntent: "+intent.getAction());

        if(tag != null) {
            Toast.makeText(this, getString(R.string.message_tag_detected), Toast.LENGTH_SHORT).show();
            Ndef ndef = Ndef.get(tag);

            if (isDialogDisplayed) {

                if (isWrite) {

                    // Add field values here as they're created
                    String messageToWrite = mEtResponderId.getText().toString() +
                                            "\n" +
                                            mSpinnerResponderLevel.getSelectedItem().toString() +
                                            "\n" +
                                            ((RadioButton)findViewById(mRadioResponderLevel.getCheckedRadioButtonId())).getText().toString() +
                                            "\n" +
                                            mEtFirstName.getText().toString() +
                                            "\n" +
                                            mEtLastName.getText().toString() +
                                            "\n" +
                                            mEtPatientDob.getText().toString() +
                                            "\n" +
                                            mEtLocation.getText().toString() +
                                            "\n" +
                                            mEtChiefComplaint.getText().toString();
                    //TODO: add new fields
                    mNfcWriteFragment = (NFCWriteFragment) getFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);
                    mNfcWriteFragment.onNfcDetected(ndef,messageToWrite);

                } else {

                    mNfcReadFragment = (NFCReadFragment)getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);
                    mNfcReadFragment.onNfcDetected(ndef);
                }
            }
        }
    }
}
