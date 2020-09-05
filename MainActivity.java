package com.internationaltechnovation.infotap;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView home,analytics,scan,activate;
    WebView wv;

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;
    String[][] techList;
    private boolean isToScan = false,isToWrite=false;
    private String urli="";

    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    private final static int FILECHOOSER_RESULTCODE = 1;

    int c=0;//DEBUG

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        home=findViewById(R.id.home);
        analytics=findViewById(R.id.analytics);
        scan=findViewById(R.id.scan);
        activate=findViewById(R.id.activate);
        wv=findViewById(R.id.webView);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setAllowFileAccess(true);
        wv.getSettings().setAllowContentAccess(true);
        wv.setWebChromeClient(new WebChromeClient()
        {



            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e("Webview", "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }

        });
        //INITIAL
        wv.loadUrl("https://www.infotap.co/account/new-page");

        home.setOnClickListener(this);
        analytics.setOnClickListener(this);
        scan.setOnClickListener(this);
        activate.setOnClickListener(this);
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        this.openFileChooser(uploadMsg, "*/*");
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        this.openFileChooser(uploadMsg, acceptType, null);
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {
            Uri[] results = null;
            //Check if response is positive
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return;
                    }
                    if (intent == null) {
                        //Capture Photo if no image available
                        if (mCM != null) {
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    // Create an image file
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.home:
                wv.loadUrl("https://www.infotap.co/account/new-page");
                break;
            case R.id.activate:
                //WRITE TO NFC TAGS
                try {
                    nfcAdapter = NfcAdapter.getDefaultAdapter(this);
                    if (nfcAdapter != null) {
                        if (nfcAdapter.isEnabled()) {
                            urli=wv.getUrl();
                            if(TextUtils.isEmpty(urli)){
                                Toast.makeText(MainActivity.this, "Cannot write empty URL to Tags!",Toast.LENGTH_LONG).show();
                            }
                            else {
                                isToWrite=true;
                                isToScan=false;
                                pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this,
                                        getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                                IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                                intentFiltersArray = new IntentFilter[]{ndef};
                                techList = new String[][] {{ android.nfc.tech.Ndef.class.getName()}};
                                nfcAdapter.enableForegroundDispatch(MainActivity.this, pendingIntent, intentFiltersArray, techList);
                                findViewById(R.id.busy).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if(nfcAdapter!=null) {
                                            nfcAdapter.disableForegroundDispatch(MainActivity.this);
                                        }
                                        findViewById(R.id.busy).setVisibility(View.GONE);
                                        ((TextView)findViewById(R.id.busyStatus)).setText("Searching for InfoTags...");
                                        isToWrite=false;
                                    }
                                });
                                //OPEN DIALOG TO DISPLAY WRITING IN PROGRESS
                                findViewById(R.id.busy).setVisibility(View.VISIBLE);
                                ((TextView)findViewById(R.id.busyStatus)).setText("Searching for InfoTags to write...");
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "NFC is Disabled", Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Seems like your Phone has no NFC", Toast.LENGTH_LONG).show();
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case R.id.analytics:
                wv.loadUrl("https://www.infotap.co/account/blank-1");
                break;
            case R.id.scan:
                try {
                    nfcAdapter = NfcAdapter.getDefaultAdapter(this);
                    if (nfcAdapter != null) {
                        if (nfcAdapter.isEnabled()) {
                            pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this,
                                    getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                            intentFiltersArray = new IntentFilter[]{ndef};
                            techList = new String[][] {{ android.nfc.tech.Ndef.class.getName()}};
                            isToScan=true;
                            isToWrite=false;
                            nfcAdapter.enableForegroundDispatch(MainActivity.this, pendingIntent, intentFiltersArray, techList);
                            findViewById(R.id.busy).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(nfcAdapter!=null) {
                                        nfcAdapter.disableForegroundDispatch(MainActivity.this);
                                    }
                                    findViewById(R.id.busy).setVisibility(View.GONE);
                                    isToScan=false;
                                }
                            });
                            findViewById(R.id.busy).setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(MainActivity.this, "NFC is Disabled", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Seems like your Phone has no NFC", Toast.LENGTH_LONG).show();
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (isToScan) {
            findViewById(R.id.busy).setVisibility(View.GONE);
            isToScan = false;
            String text="";
            try {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Ndef ndef = Ndef.get(tag);
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage.getRecords() != null && ndefMessage.getRecords()[0]!=null) {
                    text = ndefMessage.getRecords()[0].toUri().toString();
                }
                ndef.close();
                if (text.startsWith("http://") || text.startsWith("https://")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(this, "This is not a Proper URL", Toast.LENGTH_LONG).show();
                }
            }
            catch (Exception e){
                Toast.makeText(this, "Something went Wrong : "+e.getMessage(), Toast.LENGTH_LONG).show();
            }

//            Parcelable[] ndefMessageArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_TAG);
//            if (ndefMessageArray != null) {
//                NdefMessage ndefMessage = (NdefMessage) ndefMessageArray[0];
//                byte[] payload = ndefMessage.getRecords()[0].getPayload();
//                byte[] textArray = Arrays.copyOfRange(payload, (int) payload[0] + 1, payload.length);
//                text = new String(textArray);
//
//                if (text.startsWith("http://") || text.startsWith("https://")) {
//                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
//                    startActivity(browserIntent);
//                } else {
//                    Toast.makeText(this, "This is not a Proper URL", Toast.LENGTH_LONG).show();
//                }
//            }
        }
        else if(isToWrite) {
            isToWrite=false;
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                ((TextView)findViewById(R.id.busyStatus)).setText("Writing to Tag...");
                Ndef ndef = Ndef.get(tag);
                if (ndef != null) {
                    try {
                        ndef.connect();
                        NdefRecord mimeRecord = NdefRecord.createUri(urli);
                        ndef.writeNdefMessage(new NdefMessage(mimeRecord));
                        ndef.close();
                        //Write Successful
                        Toast.makeText(this, "Write Successful!", Toast.LENGTH_LONG).show();
                    } catch (IOException | FormatException e) {
                        Toast.makeText(this, "Write Failed!"+e.getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        findViewById(R.id.busy).setVisibility(View.GONE);
                        ((TextView)findViewById(R.id.busyStatus)).setText("Scanning for InfoTags...");
                    }
                }
            }
            else {
                Toast.makeText(this, "Tag Error", Toast.LENGTH_LONG).show();
            }
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);//ADDED TO SEE IF THIS CRASHES
        }
        super.onResume();
    }

    private void writeToNfc(Ndef ndef, String message){
        if (ndef != null) {
            try {
                ndef.connect();
                NdefRecord mimeRecord = NdefRecord.createUri(message);
                ndef.writeNdefMessage(new NdefMessage(mimeRecord));
                ndef.close();
                //Write Successful
                Toast.makeText(this, "Write Successful!", Toast.LENGTH_LONG).show();
            } catch (IOException | FormatException e) {
                Toast.makeText(this, "Write Failed!"+e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                findViewById(R.id.busy).setVisibility(View.GONE);
                ((TextView)findViewById(R.id.busyStatus)).setText("Scanning for InfoTags...");
                if (nfcAdapter != null) {
                    nfcAdapter.disableForegroundDispatch(this);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        findViewById(R.id.busy).setVisibility(View.GONE);
//        isToScan=false;
//        isToWrite=false;

        super.onPause();
    }
}