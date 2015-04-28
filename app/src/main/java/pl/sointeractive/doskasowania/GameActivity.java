package pl.sointeractive.doskasowania;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class GameActivity extends ActionBarActivity {

    private BeaconManager beaconManager = new BeaconManager(GameActivity.this);
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);
    private static final String TAG = GameActivity.class.getSimpleName();
    private boolean isproccesing=false;
    private TextView timer;
    private final long major=50000;
    private final long minor=2;
    private CountDownTimer countDownTimer;
    private Button deactiveBtn;
    private WebView webView;
    private double DISTANCE_10_CM = 0.1;
    private volatile double currentBeaconDistance=-1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupBluetooth();
        setUpView();
        setUpListeners();
        setUpWebView();

        // Should be invoked in #onCreate.
        beaconManager.setForegroundScanPeriod(2000,0);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {

                if(isproccesing== false) {
                    isproccesing=true;
                    Log.e(TAG, "Ranged beacons: " + beacons);
                    for(final Beacon b: beacons) {
                        Utils.Proximity proximity = Utils.computeProximity(b);
                       // double distance = Utils.computeAccuracy(b); //radius = 0.1 m between device and beacon
                        currentBeaconDistance = Utils.computeAccuracy(b);

                        if (
                               proximity == Utils.Proximity.IMMEDIATE
                                  //distance < DISTANCE_10_CM
                                        && b.getMajor()==major && b.getMinor()==minor) {

//                            runOnUiThread(new Runnable() {
//                                public void run() {
//                                    Toast.makeText(getApplicationContext(), "BOMBA", Toast.LENGTH_SHORT).show();
//                                }
//                            });

                            //znaleziono
                            webView.loadUrl("file:///android_asset/bomb app/output/04/bomb-app_04.html");
                            deactiveBtn.setVisibility(View.VISIBLE);

                            Log.e(TAG, "entered in (IMMEDIATE)minor " + b.getMinor());
                            break;

                        }
                        else if(b.getMinor()==major && b.getMinor()==minor) {
                            break;
                        }
                        else {
                            //Toast.makeText(GameActivity.this,"FIND",Toast.LENGTH_LONG).show();
//                            runOnUiThread(new Runnable() {
//                                public void run() {
//                                    Toast.makeText(getApplicationContext(), "FIND", Toast.LENGTH_SHORT).show();
//                                }
//                            });
                            //szukanie
                            webView.loadUrl("file:///android_asset/bomb app/output/03/bomb-app_03.html");
                            deactiveBtn.setVisibility(View.INVISIBLE);
                        }

                   }
                    isproccesing=false;
                }

            }
        });
    }

    private void setupBluetooth() {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }

    private void setUpWebView() {

        webView.getSettings().setJavaScriptEnabled(true);

        //disabling scroll view
        webView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return(event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        //szukanie
        webView.loadUrl("file:///android_asset/bomb app/output/02/bomb-app.html");
        webView.setBackgroundColor(Color.TRANSPARENT);
        setupScale(webView);

    }

    private void setupScale(WebView webView) {
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
    }

    private void setUpListeners() {

        deactiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(countDownTimer!=null)
                countDownTimer.cancel();
                //deaktywuj
                webView.loadUrl("file:///android_asset/bomb app/output/02/bomb-app.html");

                try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot stop but it does not matter now", e);
                }

            }
        });

        countDownTimer= new CountDownTimer(60000*2, 1000) {

            public void onTick(long millisUntilFinished) {
                timer.setText(String.format("%.2f", currentBeaconDistance)+ "m\nSeconds Remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                //wybuch
                timer.setText("Bomba wybuchła BOOOM!");
                webView.loadUrl("file:///android_asset/bomb app/output/05/bomb-app_05.html");

                try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot stop but it does not matter now", e);
                }
            }
        }.start();
    }

    private void setUpView() {
        timer  = (TextView) findViewById(R.id.timer);
        deactiveBtn = (Button) findViewById(R.id.deactiveBtn);
        webView = (WebView) findViewById(R.id.webView);
        //webView.loadUrl("file:///android_asset/bomb app/output/04/bomb-app_02.html");
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupBluetooth();
    }

    @Override
    protected void onStop() {

        // Should be invoked in #onStop.
        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot stop but it does not matter now", e);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // When no longer needed. Should be invoked in #onDestroy.
        beaconManager.disconnect();
        super.onDestroy();
    }


}

