package com.example.rim.firstapp;

import android.app.Activity;
import android.app.AlertDialog;

import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends Activity {
    public static final String MIME_TEXT_PLAIN = "text/plain";
    BluetoothAdapter BA;
    TextView txtNfc;
    Set<BluetoothDevice> bDevice;
    EditText etInput;

    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtNfc=(TextView)findViewById(R.id.textView_explanation);

        nfcAdapter=NfcAdapter.getDefaultAdapter(getApplicationContext());

        if(nfcAdapter.isEnabled())
        {
            Toast.makeText(this,"NFC enable", Toast.LENGTH_LONG).show();
        }else{
            finish();
        }

        handlarIntent(getIntent());


        BA = BluetoothAdapter.getDefaultAdapter();

        if (BA.isEnabled()) {
            Toast.makeText(this, "bluetooth is enabled", Toast.LENGTH_LONG).show();
        } else {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, 0);
        }

    }

    public static void setupForegroundDispatch(final Activity activity,NfcAdapter nfcAdapter){
        final Intent intent=new Intent(activity.getApplicationContext(),activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent=PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter intentFilter[]=new IntentFilter[1];
        String[][] techList=new String[][]{};

        intentFilter[0]=new IntentFilter();
        intentFilter[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        intentFilter[0].addCategory(Intent.CATEGORY_DEFAULT);

        try{
            intentFilter[0].addDataType(MIME_TEXT_PLAIN);
        }catch (Exception e){
            e.printStackTrace();
        }

        nfcAdapter.enableForegroundDispatch(activity,pendingIntent,intentFilter,techList);
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }



    @Override
    public void onResume(){
        super.onResume();
        setupForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, nfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handlarIntent(intent);
    }

    public void handlarIntent(Intent intent)
    {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d("TAGSAMPLE", "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }


    public void visible(View v) {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        bDevice = BA.getBondedDevices();


    }
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e("NFCTEST", "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            String t="UTF-8";

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? t : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                txtNfc.setText("Read content: " + result);
            }
        }
    }
}


