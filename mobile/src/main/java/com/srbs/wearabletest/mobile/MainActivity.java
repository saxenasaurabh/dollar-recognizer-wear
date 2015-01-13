package com.srbs.wearabletest.mobile;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
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
    /**
     * Name for extra intent message passed from
     * {@link com.srbs.wearabletest.wearabletest.ListenerServiceFromWear}
     */
    protected static final String INTENT_EXTRA_MESSAGE_NAME =
            "com.srbs.wearabletest.wearabletest.points";

    private TextView textView; // TextView showing received list of points.
    private WebView dollarRecognizerWebView; // WebView loading dollar.js
    private Node mNode; // the connected device to send the message to
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private String LOGGER_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init WebView.
        dollarRecognizerWebView = (WebView)this.findViewById(R.id.webView);
        dollarRecognizerWebView.getSettings().setJavaScriptEnabled(true);
        dollarRecognizerWebView.addJavascriptInterface(new JavaScriptHandler(this), "MyHandler");
        dollarRecognizerWebView.loadUrl("file:///android_asset/index.html");

        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        // Get message from intent and respond with result.
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.INTENT_EXTRA_MESSAGE_NAME);
        textView = (TextView) findViewById(R.id.textView);
        if (message != null && !message.isEmpty()) {
            onNewMessage(message);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Read message from intent and call `onNewMessage` to recognize it.
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        String message = intent.getStringExtra(MainActivity.INTENT_EXTRA_MESSAGE_NAME);
        if (message != null && !message.isEmpty()) {
            onNewMessage(message);
        }
    }

    /**
     * Runs javascript:recognize(message) defined in assets/index.html and updates textView with
     * the list of points.
     * @param message
     */
    private void onNewMessage(String message) {
        dollarRecognizerWebView.loadUrl("javascript:recognize(" + message + ")");
        textView.setText(message);
    }

    /**
     * Builds byte array from name and score and sends to wearable.
     * The first Float.SIZE/8 (4 bytes) store the size and the rest store the name of the recognized
     * character.
     * @param name
     * @param score
     */
    protected void setResult(String name, float score) {
        int size = Float.SIZE/8 + name.length();
        ByteBuffer buffer = ByteBuffer.allocate(size).putFloat(score);
        buffer.put(name.getBytes());
        sendMessage(buffer.array());
    }

    /**
     * Sends the byte array to the wearable using {@link com.google.android.gms.wearable.MessageApi}
     * @param bytes
     */
    private void sendMessage(final byte[] bytes) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                // TODO(saurabh): Clean this up.
                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), PATH_RECOGNIZER_RESULT, bytes).await();
                }
            }
        }).start();
    }

    /*
     * Resolve the node = the connected device to send the message to
     */
    private void resolveNode() {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                Log.d(LOGGER_TAG, "Connected nodes: " + nodes.toString());
                for (Node node : nodes.getNodes()) {
                    mNode = node;
                }
            }
        });
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
}
