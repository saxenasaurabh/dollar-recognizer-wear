package com.srbs.wearabletest.mobile;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * Created by srbs on 1/6/15.
 */
public class JavaScriptHandler {
    static final String LOGGER_TAG = JavaScriptHandler.class.getSimpleName();
    MainActivity parentActivity;
    public JavaScriptHandler(MainActivity activity) {
        parentActivity = activity;
    }

    /**
     * See window.MyHandler.setResult called from recognize(points) in assets/index.html.
     * @param name
     * @param score
     */
    @JavascriptInterface
    public void setResult(String name, float score) {
        Log.d(LOGGER_TAG, "Result: " + name);
        Log.d(LOGGER_TAG, "Score: " + score);
        parentActivity.setResult(name, score);
    }
}