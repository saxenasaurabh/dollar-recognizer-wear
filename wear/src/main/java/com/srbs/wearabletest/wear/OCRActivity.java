package com.srbs.wearabletest.wear;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see
 */
public class OCRActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    /**
     * TODO(saurabh) This should be in some shared project.
     * Used by {@link com.google.android.gms.wearable.MessageApi}
     * for sending list of points from wearable to mobile.
     */
    public static final String PATH_POINTS = "/path-points";
    /**
     * TODO(saurabh) This should be in some shared project.
     * Used by {@link com.google.android.gms.wearable.MessageApi}
     * for sending result from mobile to wearable.
     */
    public static final String PATH_RECOGNIZER_RESULT = "/recognizer-result";

    private static final String LOGGER_TAG = OCRActivity.class.getSimpleName();
    protected SharedPreferences prefs;
    private GestureDetector mDetector;
    private DismissOverlayView mDismissOverlay;

    Node mNode; // the connected device to send the message to
    GoogleApiClient mGoogleApiClient;
    protected static final String RESULT = "com.srbs.wearabletest.wearabletest.result";
    private boolean mResolvingError=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        String timerWaitPrefKey = getResources().getString(R.string.timer_wait_preference_key);
        String distThresholdPrefKey = getResources().getString(R.string.dist_threshold_preference_key);
        prefs.edit().putLong(timerWaitPrefKey, getResources().getInteger(R.integer.timer_wait_default)).commit();
        prefs.edit().putLong(distThresholdPrefKey, getResources().getInteger(R.integer.dist_threshold_default)).commit();

        setContentView(R.layout.activity_main);

        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //UI elements with a simple CircleImageView
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
                if (mDismissOverlay != null) {
                    Log.d(LOGGER_TAG, mDismissOverlay.toString());
                    mDismissOverlay.setIntroText(R.string.long_press_intro);
                    mDismissOverlay.showIntroIfNecessary();
                }
            }
        });

        // Configure a gesture detector
        mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent ev) {
                mDismissOverlay.show();
            }
        });
    }

    /**
     * Build flattened string from list of points and send to mobile handheld to recognize.
     * @param points
     */
    protected void recognize(List<Point> points) {
        if (points.isEmpty()) return;
        List pointsFlattened = new ArrayList(points.size() * 2);
        String pointsList = points.get(0).x + "," + points.get(0).y;
        for (int i = 1; i < points.size(); i++) {
            pointsList += ", " + points.get(i).x + "," + points.get(i).y;
        }
        pointsList = "[" + pointsList + "]";
        Log.d(LOGGER_TAG, "Trying to recognize: " + pointsList);
        sendMessage(pointsList);
    }

    /**
     * Send message to mobile handheld
     */
    private void sendMessage(String pointList) {
        if (mNode != null && mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, mNode.getId(), PATH_POINTS, pointList.getBytes()).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e("TAG", "Failed to send message with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        } else {
            //Improve your code
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    /*
     * Resolve the node = the connected device to send the message to
     */
    private void resolveNode() {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                for (Node node : nodes.getNodes()) {
                    mNode = node;
                }
            }
        });
    }

    // Capture long presses
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    @Override
    public void onConnected(Bundle bundle) {
        resolveNode();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * Get result from intent message and display in textView.
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        final TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(intent.getStringExtra(OCRActivity.RESULT));
    }
}
