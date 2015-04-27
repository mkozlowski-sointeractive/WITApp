package pl.sointeractive.doskasowania;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                        if (proximity == Utils.Proximity.IMMEDIATE && b.getMajor()==major && b.getMinor()==minor) {
                                Toast.makeText(GameActivity.this,"BOMBA",Toast.LENGTH_LONG).show();
                                webView.loadUrl("file:///android_asset/find.html");
                                deactiveBtn.setVisibility(View.VISIBLE);

                            Log.e(TAG, "entered in (IMMEDIATE)minor " + b.getMinor());
                            break;

                        }
                        else if(b.getMinor()==major && b.getMinor()==minor) {
                            break;
                        }
                        else {
                             Toast.makeText(GameActivity.this,"FIND",Toast.LENGTH_LONG).show();

                            webView.loadUrl("file:///android_asset/search.html");
                            deactiveBtn.setVisibility(View.INVISIBLE);

                        }

                    }
                    isproccesing=false;
                }

            }
        });
    }

    private void setUpWebView() {

        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/search.html");
        webView.setBackgroundColor(Color.TRANSPARENT);

    }

    private void setUpListeners() {
        deactiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                webView.loadUrl("file:///android_asset/deactive.html");

                try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot stop but it does not matter now", e);
                }



            }
        });


        countDownTimer= new CountDownTimer(60000*2, 1000) {

            public void onTick(long millisUntilFinished) {
                timer.setText("seconds remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                timer.setText("Bomba wybuchla BOOOM!");
                webView.loadUrl("file:///android_asset/boom.html");

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Should be invoked in #onStart.
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

