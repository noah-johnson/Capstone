package com.slackpack.nfc;

import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
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
import android.widget.Toast;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements Listener, View.OnClickListener {
    
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
    private Button mBtWrite;
    private Button mBtRead;

    // Date variables
    private int mYear, mMonth, mDay;

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

        initViews();
        initNFC();

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
        mBtWrite = (Button) findViewById(R.id.btn_write);
        mBtRead = (Button) findViewById(R.id.btn_read);

        // Set up listeners for elements that can be tapped
        mEtPatientDob.setOnClickListener(this);
        mBtWrite.setOnClickListener(view -> showWriteFragment());
        mBtRead.setOnClickListener(view -> showReadFragment());
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
                                            mEtChiefComplaint.getText().toString();
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
