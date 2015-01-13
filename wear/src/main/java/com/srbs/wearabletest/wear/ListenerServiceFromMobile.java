package com.srbs.wearabletest.wear;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;

/**
 * Created by srbs on 1/11/15.
 */
public class ListenerServiceFromMobile extends WearableListenerService {
    private String LOGGER_TAG = ListenerServiceFromMobile.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOGGER_TAG, "Message received from mobile");
        /*
         * Receive the message from wear
         */
        if (messageEvent.getPath().equals(OCRActivity.PATH_RECOGNIZER_RESULT)) {
            byte[] data = messageEvent.getData();
            String name;
            float score;
            // Extract score and name from byte array. Assumes score is float type and is placed
            // ahead of name in the array.
            int numBytesInFloat = Float.SIZE/8;
            score = ByteBuffer.wrap(data, 0, numBytesInFloat).getFloat();
            name = new String(data, numBytesInFloat, data.length - numBytesInFloat);
            Log.d(LOGGER_TAG, "Name: " + name);

            Intent startIntent = new Intent(this, OCRActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra(OCRActivity.RESULT, "Name: " + name + " Score: " + score);
            startActivity(startIntent);
        }
    }
}
