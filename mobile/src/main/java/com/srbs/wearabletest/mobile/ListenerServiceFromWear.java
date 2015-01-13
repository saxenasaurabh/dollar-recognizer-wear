package com.srbs.wearabletest.mobile;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class ListenerServiceFromWear extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        /*
         * Receive the message from wear
         */
        if (messageEvent.getPath().equals(MainActivity.PATH_POINTS)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra(MainActivity.INTENT_EXTRA_MESSAGE_NAME, new String(messageEvent.getData()));
            startActivity(startIntent);
        }
    }
}